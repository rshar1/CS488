package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * A bi-directional communication protocol. This socket is the interface for the application layer
 * to send and receive data reliably to another end-system also using this protocol.
 * This class stores the buffer, and ensures that all messages are acked as well as sending it's own
 * acks.
 */
public class RUDPSocket implements AutoCloseable {

  enum STATUS {
    CONNECTED, CONNECTING, DISCONNECTED;
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


  public RUDPSocket(int sourcePort) throws SocketException {
    this.sourcePort = sourcePort;
    this.socket = new DatagramSocket(sourcePort);
    this.status = STATUS.DISCONNECTED;

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
          } catch (IOException exc) {
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


}
