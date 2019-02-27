package receiver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import common.RUDPSocket;

/**
 * Bi-directional Receiver. This will be accept a connection, and then it will receive a file, at
 * the same time as sending one back to the sender.
 */
public class BidirectionalReceiver {


  final static int ownPort = 8888;
  final static int targetPort = 7777;
  static String host = "localhost";

  public static void main(String[] args) {

    System.out.println("Receiver: connection built. about to send.");

    try(RUDPSocket socket = new RUDPSocket(ownPort);
        FileInputStream fis = new FileInputStream("./test2.txt");
        FileOutputStream fos = new FileOutputStream("./test2Received.txt"))
    {
      socket.acceptConnection();

      Thread receiverEnd = new Thread(() -> {

        try {
          byte[] incomingData = new byte[1024];
          int numRead;
          while ((numRead = socket.getInputStream().read(incomingData)) != -1) {
            for (int i = 0; i < numRead; i++) {
              if (incomingData[i] != -1) {
                fos.write(incomingData[i]);
              } else {
                System.out.println("-1 found");
                return;
              }
            }
          }
        } catch (IOException exc) {
          exc.printStackTrace();
          return;
        }

      });

      receiverEnd.start();
      byte[] data = new byte[1024];
      int numRead;
      while((numRead = fis.read(data)) != -1)
      {
        socket.getOutputStream().write(Arrays.copyOf(data, numRead));
      }
      socket.getOutputStream().write(-1);

      receiverEnd.join();

    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Receiver: finished.");

  }


}
