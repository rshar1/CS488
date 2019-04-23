#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h> //memset()
#include <unistd.h> //sleep()

//Socket stuff
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

//IP header (struct iphdr) definition
#include <linux/ip.h>
//TCP header (struct tcphdr) definition
#include <linux/tcp.h>

//Data to be sent (appended at the end of the TCP header)
#define DATA "datastring"

const int MSS = 1460;
const int wscale = 4;
const double client_bw = 1544000.0
const unsigned maxwindow = 15000 << wscale;
const double min_wait = 1.2 * 8 * 40 / client_bw;

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
  unsigned window;         // the current window size
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
                int is_ack,
                void* content,
                unsigned content_length,
                char syn,
                char fin,
                char rst,
                int window,
                int wscale) {
    // TODO implement send packet to construct the packet and send
    // it based to the provided socket

}

int read_packet(struct victim_connection *m_victim, int sock) {
    // TODO keep reading the socket for a relevent ip
    // update the sequence number
}

void handshake(struct victim_connection *m_victim, int sock) {
    // TODO IMPLEMENT
	unsigned send_seq = 1; //TODO random
    unsigned ack_nbr=1;
    int is_ack = 0;
    void* content = "";
    unsigned content_length = 0;
    char syn = 0;
    char fin = 0;
    char rst = 0;
    int window = 5840;
    int wscale = 0;
	send_packet(sock,m_victim,send_seq,ack_nbr,is_ack,
				content,content_length,syn,fin,rst,
				window,wscale);
	int read_seq = read_packet(m_victim, sock);
	send_seq +=1;
	ack_nbr = read_seq+1;
	send_packet(sock,m_victim,send_seq,ack_nbr,is_ack,
				content,content_length,syn,fin,rst,
				window,wscale);
	ack_nbr = read_seq+1;
	content="GET / HTTP/1.0\r\n\r\n";
	send_packet(sock,m_victim,send_seq,ack_nbr,is_ack,
				content,content_length,syn,fin,rst,
				window,wscale);
	m_victim->send_seq = send_seq + content_length;
}

int beginAttack() {

    // local variables
    int sock;
    struct victim_connection m_victim;  // TODO THIS ASSUMES ONE VICTIM

    // TODO Implement

    // Open the socket
    if((sock = socket(PF_INET, SOCK_RAW, IPPROTO_TCP)) < 0) {
      perror("Error while creating socket");
      exit(-1);
    }
    // Set up each connection struct
    m_victim.id = 2;
    m_victim.dst_addr = inet_addr("10.0.0.2");
    m_victim.dst_port = //TODO;
    m_victim.window = MSS;

    // Connect to each server using 3-way handshake
    handshake(sock, &m_victim);

    // Get the first ack for each connection
    


    // Begin the pace thread to observe overruns


    // begin the real attack

    // Go through each connection and send the next ack

    // for each connection increment the ack by the window size

    // increase the window size by mss as long as its less than the max

}

int main(int argc, char const *argv[]) {

  if (argc != 3) {
    printf("Error: Invalid parameters! \n");
    printf("Usage: %s <target hostname/IP> <target port>\n", argv[0]);
    exit(1);
  }

  u_int16_t src_port, dst_port;
  u_int32_t src_addr, dst_addr;
  dst_addr = inet_addr(argv[1]);
  dst_port = atoi(argv[2]);

  // TODO THIS SHOULD BE RECEIVED SOMEHOW
  src_addr = inet_addr("10.0.0.1");

  int sock, bytes_sent;
  struct tcphdr *tcpHdr;

  //TODO change this Initial guess for the SEQ field of the TCP header
  uint32_t initSeqGuess = 1138083240;

  //Data to be appended at the end of the tcp header
  char *data;

  //TCP header + data
  char packet[MSS];

  //Address struct to sendto()
  struct sockaddr_in addr_in;

  //Pseudo TCP header to calculate the TCP header's checksum
  struct pseudoTCPPacket pTCPPacket;

  //Pseudo TCP Header + TCP Header + data
  char *pseudo_packet;
  
  //Raw socket without the tcp headers
  if((sock = socket(PF_INET, SOCK_RAW, IPPROTO_TCP)) < 0) {
    perror("Error while creating socket");
    exit(-1);
  }

  //Populate address struct
  addr_in.sin_family = AF_INET;
  addr_in.sin_port = htons(dst_port);
  addr_in.sin_addr.s_addr = dst_addr;

  //Allocate mem for tcp headers and 0
  memset(packet, 0, MSS);

  tcpHdr = (struct tcphdr *) packet;
  data = (char *) (packet + sizeof(struct tcphdr));
  strcpy(data, DATA);

  //Populate tcpHdr
  tcpHdr->source = htons(src_port); //16 bit in nbp format of source port
  tcpHdr->dest = htons(dst_port); //16 bit in nbp format of destination port
  tcpHdr->seq = 0x0; //32 bit sequence number, initially set to zero
  tcpHdr->ack_seq = 0x0; //32 bit ack sequence number, depends whether ACK is set or not
  tcpHdr->doff = 5; //4 bits: 5 x 32-bit words on tcp header
  tcpHdr->res1 = 0; //4 bits: Not used
  tcpHdr->cwr = 0; //Congestion control mechanism
  tcpHdr->ece = 0; //Congestion control mechanism
  tcpHdr->urg = 0; //Urgent flag
  tcpHdr->ack = 0; //Acknownledge
  tcpHdr->psh = 0; //Push data immediately
  tcpHdr->rst = 0; //RST flag
  tcpHdr->syn = 1; //SYN flag
  tcpHdr->fin = 0; //Terminates the connection
  tcpHdr->window = htons(155);//0xFFFF; //16 bit max number of databytes 
  tcpHdr->check = 0; //16 bit check sum. Can't calculate at this point
  tcpHdr->urg_ptr = 0; //16 bit indicate the urgent data. Only if URG flag is set

  //Now we can calculate the checksum for the TCP header
  pTCPPacket.srcAddr = src_addr; //32 bit format of source address
  pTCPPacket.dstAddr = dst_addr; //32 bit format of source address
  pTCPPacket.zero = 0; //8 bit always zero
  pTCPPacket.protocol = IPPROTO_TCP; //8 bit TCP protocol
  pTCPPacket.TCP_len = htons(sizeof(struct tcphdr) + strlen(data)); // 16 bit length of TCP header

  //Populate the pseudo packet
  pseudo_packet = (char *) malloc((int) (sizeof(struct pseudoTCPPacket) + sizeof(struct tcphdr) + strlen(data)));
  memset(pseudo_packet, 0, sizeof(struct pseudoTCPPacket) + sizeof(struct tcphdr) + strlen(data));

  //Copy pseudo header
  memcpy(pseudo_packet, (char *) &pTCPPacket, sizeof(struct pseudoTCPPacket));
 
  //Calculate check sum: zero current check, copy TCP header + data to pseudo TCP packet, update check
  tcpHdr->check = 0;

  //Copy tcp header + data to fake TCP header for checksum
  memcpy(pseudo_packet + sizeof(struct pseudoTCPPacket), tcpHdr, sizeof(struct tcphdr) + strlen(data));

  //Set the TCP header's check field
  tcpHdr->check = (csum((unsigned short *) pseudo_packet, (int) (sizeof(struct pseudoTCPPacket) + sizeof(struct tcphdr) +  strlen(data))));

  printf("TCP Checksum: %d\n", (int) tcpHdr->check);

  //Finally, send packet
  if((bytes_sent = sendto(sock, packet, MSS, 0, (struct sockaddr *) &addr_in, sizeof(addr_in))) < 0) {
    perror("Error on sendto()");
  }
  else {
    printf("Success! Sent %d bytes.\n", bytes_sent);
  }

  while (1) {
    // TODO TECHNICALLY THE MAX PACKET SIZE FOR TCP IS 65535
    char buff[MSS];
    if (recvfrom(sock, buff, MSS, 0, NULL, NULL) < 0) {
      perror("Error on recv()");
    } else {
      // TODO WE SHOULD PARSE THE RESPONSE HERE
      struct sockaddr_in source_socket_address, dest_socket_address;
      struct iphdr *ip_packet = (struct iphdr *) buff;
      memset(&source_socket_address, 0, sizeof(source_socket_address));
      source_socket_address.sin_addr.s_addr = ip_packet->saddr;
      memset(&dest_socket_address, 0, sizeof(dest_socket_address));
      dest_socket_address.sin_addr.s_addr = ip_packet->daddr;

      if (source_socket_address.sin_addr.s_addr == dst_addr) {
       
        printf("Incoming packet: \n");
        printf("Packet size (bytes) %d\n", ntohs(ip_packet->tot_len));
        printf("Source Address: %s\n", (char *)inet_ntoa(source_socket_address.sin_addr));
        printf("Destination Address: %s\n", (char *)inet_ntoa(dest_socket_address.sin_addr));
        printf("Identification: %d\n\n", ntohs(ip_packet->id)); 
        break;
      } else {
        printf("Does not match\n");
      }
      
    }
  }
  close(sock);
  return 0;
}
