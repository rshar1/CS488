package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A bi-directional communication protocol. This socket is the interface for the application layer
 * to send and receive data reliably to another end-system also using this protocol. This class
 * stores the buffer, and ensures that all messages are acked as well as sending it's own acks.
 */
public class RUDPSocket implements AutoCloseable {

  /**
   * The different states a connection can be in. An RUDP socket remains connected to the same remote
   * host until one of them closes the connection. It does not attempt a reconnection
   */
  enum STATUS {
    CONNECTED, CONNECTING, DISCONNECTED;
  }

  /**
   * The output stream that allows the application layer to write data to the sender window and send it
   * If there is no room in the sender window, the write methods will wait.
   */
  private class RUDPOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
      // check if window space available
      // if true, send it and add else thread sleep
      if (isClosed()) {
        throw new IOException("Resource is closed");
      }

      byte[] bArr = new byte[1];
      bArr[0] = (byte) b;
      send(bArr);

    }

    @Override
    public void write(byte[] bytes) throws IOException {
      // check if window space available
      // if true, send it and add else thread sleep
      int i = 0;

      while (bytes.length - i > 0) {
        int dataLength = Math.min(RUDPPacket.MAX_DATA_SIZE, bytes.length - i);
        byte[] data = Arrays.copyOfRange(bytes, i, i + dataLength);

        send(data);
        i += dataLength;
      }

    }

  }

  /**
   * The input stream allows the application layer to read the data from the receiver window in the
   * correct order.
   */
  public class RUDPInputStream extends InputStream {

    private byte[] buf;
    private int currIndex;

    public RUDPInputStream() {
      super();
      buf = null;
      currIndex = 0;
    }

    @Override
    public int read() throws IOException {
      byte res;
      while (this.buf == null || (currIndex >= buf.length)) {
        try {
          this.currIndex = 0;
          this.buf = receiver.receivePackets(true);
        } catch (IllegalArgumentException e) {
          return -1;
        }

      }

      res = this.buf[currIndex++];
      return (0xff & res);
    }

    @Override
    public int read(byte[] buf) throws IOException {

      for (int i = 0; i < buf.length; i++) {

        while (this.buf == null || (currIndex >= this.buf.length)) {

          try {
            this.currIndex = 0;
            this.buf = receiver.receivePackets(false);
          } catch (IllegalArgumentException e) {
            if (i > 0) {
              return i;
            } else {
              return -1;
            }
          }

          if (this.buf == null) {
            return i;
          }

        }

        buf[i] = this.buf[currIndex++];

      }

      return buf.length;

    }

  }

  // The UDP socket that the data is sent through
  private DatagramSocket socket;
  private int sourcePort;
  private STATUS status = STATUS.DISCONNECTED;

  private RUDPAddress remoteAddress;

  private boolean isFromServer;
  private RUDPServerSocket serverSocket;

  // This contains the packets that were sent out, and are now awaiting an ack
  private SenderWindow sender;

  // This collects the packets coming in so that the data can be returned to the application
  private ReceiverWindow receiver;

  private AtomicInteger sequenceNum;

  // This is more than 2 times larger than the BUFF_SIZE
  static final int MAX_SEQUENCE_NUM = 25;

  // The thread that will continuously listen for any received packets and process them accordingly
  private Thread listeningThread;

  private InputStream m_InputStream;
  private OutputStream m_OutputStream;

  private boolean disconnectingSoon = false;
  
  public static final double RELIABILITY = 0.9; // the closer to 0 the more packets dropped;

  public RUDPSocket(int sourcePort) throws SocketException {
    this.sourcePort = sourcePort;
    this.socket = new DatagramSocket(sourcePort);
    this.socket.setSoTimeout(100);
    this.status = STATUS.DISCONNECTED;
    this.sequenceNum = new AtomicInteger(0);
    this.isFromServer = false;

    // Make a thread that listens for any packets coming to the socket
    Runnable receiver = new Runnable() {
      @Override
      public void run() {
        while (!isClosed()) {

          byte[] buf = new byte[915];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          try {
            socket.receive(packet);
            processPacket(packet);
          } catch (SocketTimeoutException exc) {
            if (sender != null) {
              try {
                sender.timeOut(socket, remoteAddress);
              } catch (IOException ioexc) {
                System.out.println("Error resending packet");
              }
            }
          } catch (IOException exc) {
            System.out.println("Error receiving packing");
          }
        }
      }
    };

    this.listeningThread = new Thread(receiver);
    this.listeningThread.start();

  }

  RUDPSocket(DatagramSocket socket, RUDPServerSocket serverSocket) {
    this.sourcePort = socket.getLocalPort();
    this.socket = socket;
    this.status = STATUS.DISCONNECTED;
    this.sequenceNum = new AtomicInteger(0);

    // todo should we set up a timer to timeout sent packets?


    this.isFromServer = true;
    this.serverSocket = serverSocket;
  }

  /**
   * Begins a new connection with another RUDP socket
   *
   * @param address the remote address to connect to
   * @param port the remote port to connect to
   * @throws IllegalStateException if this socket is already connected to a remote host
   */
  public boolean connect(String address, int port) throws IOException {

    if (status != STATUS.DISCONNECTED) {
      throw new IllegalStateException("Cannot connect to multiple hosts");
    }

    this.remoteAddress = new RUDPAddress(address, port);
    beginConnectionRequest();

    while (status != STATUS.CONNECTED) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException exc) {
        // todo error
      }
    }

    return true;
  }

  STATUS getStatus() {
    return status;
  }

  public boolean isClosed() {
    return this.socket.isClosed();
  }

  @Override
  public void close() throws Exception {

    if (this.m_OutputStream != null) {
      this.m_OutputStream.close();
    }
    if (this.m_InputStream != null) {
      this.m_InputStream.close();
    }

    if (!this.disconnectingSoon) {
      RUDPPacket myPacket = new RUDPPacket(getAndUpdateSequenceNum(), 0);
      myPacket.setFinished();

      System.out.println("Closing with: " + myPacket.toString());


      // todo spinning lock can be changed to monitor
      while (!this.sender.isEmpty() && !this.disconnectingSoon) {
        Thread.sleep(100);
      }


      // Once the sender has sent all its packets, we will send a packet indicating that we want to
      // end the connection

      socket.send(myPacket.convertPacket(this.remoteAddress));
      sender.addPacket(myPacket);

      // when the senderwindow is empty, the other host must have acked the finish packet
      // todo spinning lock can be improved
      while (!this.sender.isEmpty() && !this.disconnectingSoon) {
        Thread.sleep(100);
      }

    }
    if (this.isFromServer) {
      serverSocket.closeChild(this);
    } else {
      this.socket.close();
    }

  }

  void processPacket(DatagramPacket packet) {

    RUDPPacket rPacket;
    try {
      rPacket = RUDPPacket.fromBytes(packet.getData());
    } catch (IOException exc) {
      System.out.println("Not an RUDP packet");
      return;
    }

    System.out.println("Processing a packet: " + rPacket);

    switch (status) {

      case CONNECTED:
    	double rand = Math.random();
    	if(rand<this.RELIABILITY)
    	{
    		if (rPacket.isAck()) {
    			this.processAck(rPacket.getAckNum());
    		}
    		try {
    			if (!rPacket.isAck() && this.receiver.processReceivedPacket(rPacket)) {
    				sendAck(false, rPacket.getSequenceNumber());
    			}
    			if (rPacket.isFinished()) {
    				this.disconnectingSoon = true;
    				System.out.println("Preparing for disconnection");
    			}
    		} catch (IllegalArgumentException exc) {

    		}
    	}
    	else
    		System.out.println("Packet purpously dropped with sequence num:" + rPacket.getSequenceNumber());
        break;
      case CONNECTING:
        if (rPacket.isAck() && rPacket.isConnectAttempt()) {
          this.processAck(rPacket.getAckNum());
          this.status = STATUS.CONNECTED;
          sendAck(false, rPacket.getSequenceNumber());
        } else if (rPacket.isAck()) {
          this.processAck(rPacket.getAckNum());
          this.status = status.CONNECTED;
        }

        this.receiver = new ReceiverWindow(rPacket.getSequenceNumber());

        break;
      case DISCONNECTED:
        this.remoteAddress = new RUDPAddress(packet.getAddress(), packet.getPort());
        this.sequenceNum.set(0);

        RUDPPacket response = new RUDPPacket(sequenceNum.get(),
            rPacket.getSequenceNumber());
        response.setAck(true);
        response.setConnectAttempt(true);

        this.status = STATUS.CONNECTING;
        this.sender = new SenderWindow();
        try {
          System.out.println("Sending packet" + response.toString());
          socket.send(response.convertPacket(this.remoteAddress));
          sender.addPacket(response);
        } catch (IOException exc) {
          // todo there was an error sending
        }
        break;
      default:
        System.out.println("Status undefined");
        System.exit(-1);

    }


  }

  private void beginConnectionRequest() throws IOException {
    System.out.println("Attempting connection");
    // The one that initiates the connection starts with a sequence number of 10. This isn't necessary
    this.sequenceNum.set(10);

    RUDPPacket rudpPacket = new RUDPPacket(getAndUpdateSequenceNum(), 0);
    rudpPacket.setConnectAttempt(true);

    status = STATUS.CONNECTING;

    this.sender = new SenderWindow();

    System.out.println("Sending a packet" + rudpPacket);
    socket.send(rudpPacket.convertPacket(this.remoteAddress));
    sender.addPacket(rudpPacket);

  }

  public InputStream getInputStream() throws IOException {
    if (this.getStatus() != STATUS.CONNECTED) {
      return null;
    }

    if (this.m_InputStream == null) {
      this.m_InputStream = new RUDPInputStream();
    }

    return this.m_InputStream;
  }

  public OutputStream getOutputStream() throws IOException {
    if (this.getStatus() != STATUS.CONNECTED) {
      return null;
    }

    if (this.m_OutputStream == null) {
      this.m_OutputStream = new RUDPOutputStream();
    }

    return this.m_OutputStream;
  }

  private void processAck(int ackNum) {
    this.sender.acceptAck(ackNum);
  }

  private void send(byte[] data) throws IOException {
    RUDPPacket myPacket = new RUDPPacket(getAndUpdateSequenceNum(), 0);
    myPacket.setData(data);
    sender.addPacket(myPacket);
    //todo check if any acks need to be sent with this packet. (OPTIMIZATON)
    System.out.println("Sending a packet: " + myPacket.toString());
    this.socket.send(myPacket.convertPacket(this.remoteAddress));

  }

  private void sendAck(boolean connectionAttempt, int ackNum) {
    RUDPPacket myPacket = new RUDPPacket(sequenceNum.get(), ackNum);
    myPacket.setAck(true);
    myPacket.setConnectAttempt(connectionAttempt);
    try {
      System.out.println("Sending Packet" + myPacket.toString());
      this.socket.send(myPacket.convertPacket(this.remoteAddress));
    } catch (IOException exc) {
      // todo error
    }

  }

  void timeout() {
    if (this.remoteAddress == null) {
      // todo this socket is for a server but hasn't received a request yet
      return;
    }
    try {
      sender.timeOut(this.socket, this.remoteAddress);
    } catch (IOException exc) {
      System.out.println("IOException while timing out");
    }
  }

  private int getAndUpdateSequenceNum() {
    return sequenceNum.getAndUpdate((n) -> (n + 1) % MAX_SEQUENCE_NUM);
  }

  RUDPAddress getRemoteAddress() {
    return this.remoteAddress;
  }

}
