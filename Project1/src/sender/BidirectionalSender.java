package sender;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import common.RUDPSocket;
import java.io.IOException;
import java.util.Arrays;

/**
 * A bi-directional sender. Initiates a connection with the receiver and begins sending data. It
 * will also receive data and store it into a file.
 */
public class BidirectionalSender {

  final static int ownPort = 7777;
  final static int targetPort = 8888;
  static String host = "localhost";

  public static void main(String[] args) {

    System.out.println("Sender: connection built. about to send.");

    try(RUDPSocket socket = new RUDPSocket(ownPort);
        FileInputStream fis = new FileInputStream("./test.txt");
        FileOutputStream fos = new FileOutputStream("./testReceived.txt"))
    {
      socket.connect(host, targetPort);

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
    System.out.println("Sender: finished.");

  }

}
