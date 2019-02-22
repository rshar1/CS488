package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

/**
 * A bi-directional communication protocol. This socket is the interface for the application layer
 * to send and receive data reliably to another end-system also using this protocol.
 * This class stores the buffer, and ensures that all messages are acked as well as sending it's own
 * acks.
 */
public class RUDPSocket implements AutoCloseable {

  Semaphore noSpace;

  enum STATUS {
    CONNECTED, CONNECTING, DISCONNECTED;
  }


  private class RUDPOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
      // check if window space available
      // if true, send it and add else thread sleep

    }


    @Override
    public void write(byte[] bytes) {
      // check if window space available
      // if true, send it and add else thread sleep
    }


  }


  private class RUDPInputStream extends InputStream {

    @Override
    public int read() throws IOException {
      // todo
      return 0;
    }

    @Override
    public int read(byte[] buf) {
      // todo
      return 0;
    }


  }


  private DatagramSocket socket;
  private int sourcePort;
  private STATUS status = STATUS.DISCONNECTED;

  private InetAddress remoteAddress;
  private int remotePort;

  // This contains the packets that were sent out, and are now awaiting an ack
  private SenderWindow sender;

  // This collects the packets coming in so that the data can be returned to the application
  private ReceiverWindow receiver;

  private int sequenceNum;
  private static final int MAX_SEQUENCE_NUM = 25;


  public RUDPSocket(int sourcePort) throws SocketException {
    this.sourcePort = sourcePort;
    this.socket = new DatagramSocket(sourcePort);
    this.socket.setSoTimeout(10000);
    this.status = STATUS.DISCONNECTED;
    this.sequenceNum = 0;

    // Make a thread that listens for any packets coming to the socket
    Runnable receiver = new Runnable() {
      @Override
      public void run() {
        while (true) {

          byte[] buf = new byte[905];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          try {
            socket.receive(packet);
            processPacket(packet);
          } catch (SocketTimeoutException exc) {
            // todo timeout has been reached...do some work
            // increment the counters for the sent messsages
            // check if any messages have timed-out and must be resent
            // check if there is any room available in the sender window and if so, check if there
            // is any new application data to be sent. if so, send it and add to the sender window

          }
          catch (IOException exc) {
            System.out.println("Error receiving packing");
          }
        }
      }
    };

  }

  /**
   * Begins a new connection with another RUDP socket
   * @param address the remote address to connect to
   * @param port the remote port to connect to
   * @throws IllegalStateException if this socket is already connected to a remote host
   */
  public boolean connect(String address, int port) throws UnknownHostException {

    if (status != STATUS.DISCONNECTED) {
      throw new IllegalStateException("Cannot connect to multiple hosts");
    }

    this.remoteAddress = InetAddress.getByName(address);
    this.remotePort = port;
    beginConnectionRequest();

    if (status != STATUS.CONNECTED) {
      status = STATUS.DISCONNECTED;
      return false;
    }

    return true;
  }

  public STATUS getStatus() {
    return status;
  }

  public boolean isClosed() {
    return this.socket.isClosed();
  }

  @Override
  public void close() throws Exception {
    this.socket.close();
    // todo close any input streams and output streams that were returned by the class
  }

  private void processPacket(DatagramPacket packet) {

    RUDPPacket rPacket;
    try {
      rPacket = RUDPPacket.fromBytes(packet.getData());
    } catch (IOException exc) {
      System.out.println("Not an RUDP packet");
      return;
    }

    switch (status) {

      case CONNECTED:
        // todo process the packet from the remote address
        // If ack processAck
    	if(rPacket.isAck())
    	{
    		this.processAck(rPacket.getAckNum());
    	}



        break;
      case CONNECTING:
        // todo process the packet from  the connecting address
        System.out.println("Not yet implemented");
        break;
      case DISCONNECTED:
        // todo process this packet from a remote address looking to initiate a connection


        break;
      default:
        System.out.println("Status undefined");
        System.exit(-1);

    }


  }

  private void beginConnectionRequest() {
    socket.connect(remoteAddress, remotePort);

    RUDPPacket rudpPacket = new RUDPPacket(1, 0);
    rudpPacket.setConnectAttempt(true);

    byte[] payload = rudpPacket.toBytes();
    DatagramPacket packet = new DatagramPacket(payload, payload.length, remoteAddress, remotePort);

    status = STATUS.CONNECTING;

    // Send this to the SenderWindow

  }

  public InputStream getInputStream() throws IOException {
    // todo implement
    return null;
  }

  public OutputStream getOutputStream() throws IOException { 
    // todo implement
    return null;
  }

	

  public void processAck(int ackNum) {
	  // todo mark for removal from sender window
    // todo move the sender window forward while the head is acked
	  
	  this.sender.acceptAck(ackNum);
	  sender.slideWindow();
	  
  }

  public void send(byte[] data) {
    // todo
    // send packet
    // add to senderwindow
	  RUDPPacket myPacket = new RUDPPacket(sequenceNum,0);
	  sequenceNum++;
	  if(sequenceNum>=MAX_SEQUENCE_NUM)
		  sequenceNum = 0;
	  sender.addPacket(myPacket);
	  
  }

}
