package receiver;

import common.RUDPServerSocket;
import common.RUDPSocket;

/**
 * This just demonstrates that if both hosts attempt to close at the same time, they wont block each
 * other
 */
public class ReceiverCloseTest {

  final static int ownPort = 8888;
  final static int targetPort = 7777;
  static String host = "localhost";

  public static void main(String[] args) {
    byte[] data = new byte[1024];


    System.out.println("Receiver: connection built. about to receive.");
    try(RUDPServerSocket serversocket = new RUDPServerSocket(ownPort))
    {
      RUDPSocket socket = serversocket.accept();
      socket.close();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Receiver: finished.");
  }

}
