package sender;

import common.RUDPSocket;

/**
 * This just demonstrates that if both hosts attempt to close at the same time, they wont block each
 * other
 */
public class SenderCloseTest {

  final static int ownPort = 7777;
  final static int targetPort = 8888;
  static String host = "localhost";

  public static void main(String[] args) {

    byte[] data = new byte[1024];

    System.out.println("Sender: connection built. about to send.");

    try(RUDPSocket socket = new RUDPSocket(ownPort))
    {
      socket.connect(host, targetPort);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Sender: finished.");
  }


}
