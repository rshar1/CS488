package common;

import common.RUDPSocket.STATUS;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class RUDPServerSocket implements AutoCloseable {

  // The UDP socket that the data is sent through
  private DatagramSocket socket;
  private int sourcePort;

  // Keeps track of the connections currently waiting to be accepted
  private LinkedBlockingQueue<DatagramPacket> pendingConnections;
  private Set<RUDPAddress> pendingAddresses;

  // Keeps track of the sockets currently using this server socket
  private Hashtable<RUDPAddress, RUDPSocket> currentConnections;

  private Timer timer;

  // The thread that will continuously listen for any received packets and process them accordingly
  private Thread listeningThread;

  public static final double RELIABILITY = 0.9; // the closer to 0 the more packets dropped;


  public RUDPServerSocket(int sourcePort) throws SocketException {
    this.sourcePort = sourcePort;
    this.socket = new DatagramSocket(sourcePort);

    this.pendingConnections = new LinkedBlockingQueue<>();
    this.pendingAddresses = Collections.synchronizedSet(new HashSet<RUDPAddress>());

    this.currentConnections = new Hashtable<>();
    this.socket.setSoTimeout(100);

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
            // todo does server do anything at timeout??
          } catch (IOException exc) {
            System.out.println("Error receiving packing");
          }
        }
      }
    };

    this.listeningThread = new Thread(receiver);
    this.listeningThread.start();

    this.timer = new Timer();
    this.timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        synchronized (currentConnections) {
          for (RUDPSocket rudpSocket: currentConnections.values()) {
            rudpSocket.timeout();
          }
        }
      }
    }, 0, 1000);

  }

  public boolean isClosed() {
    return this.socket.isClosed();
  }

  @Override
  public void close() throws Exception {

    // todo what to do when server wants to close
    // i.e. before all the children sockets are done working.
    this.timer.cancel();
    this.socket.close();


  }

  private void processPacket(DatagramPacket packet) {

    RUDPAddress address = new RUDPAddress(packet.getAddress(), packet.getPort());

    System.out.println("Received a packet from " + address);

    if (currentConnections.containsKey(address)) {
      RUDPSocket socket = currentConnections.get(address);
      socket.processPacket(packet);
    } else if (!pendingAddresses.contains(address)) {
      pendingAddresses.add(address);
      pendingConnections.add(packet);
    }

  }

  public RUDPSocket accept() throws InterruptedException {
    // Accept connection will block until the three way handshake completes
    DatagramPacket packet = this.pendingConnections.take();
    RUDPAddress rudpAddress = new RUDPAddress(packet.getAddress(), packet.getPort());
    this.pendingAddresses.remove(rudpAddress);

    RUDPSocket rudpSocket = new RUDPSocket(this.socket, this);
    this.currentConnections.put(rudpAddress, rudpSocket);

    // todo if the requester never times out and sends another connection request, this will never return
    while (rudpSocket.getStatus() != STATUS.CONNECTED) {
      Thread.sleep(1000);
    }

    return rudpSocket;
  }

  void closeChild(RUDPSocket childSocket) {
    this.currentConnections.remove(childSocket.getRemoteAddress());
  }

}
