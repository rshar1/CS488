package sender;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import common.RUDPSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        FileOutputStream fos = new FileOutputStream("./test2Received.txt"))
    {
      socket.connect(host, targetPort);

      Thread receiverEnd = new Thread(() -> {

        try {
          int dataLength;
          while ((dataLength = readInt(socket.getInputStream())) > -1) {
            byte[] incomingData = new byte[dataLength];
            for (int i = 0; i < incomingData.length; i++) {
              incomingData[i] = (byte) socket.getInputStream().read();
            }
            fos.write(incomingData, 0, dataLength);
          }
        } catch (IOException exc) {
          exc.printStackTrace();
        }

      });

      receiverEnd.start();
      byte[] data = new byte[1024];
      int numRead;
      while ((numRead = fis.read(data)) != -1) {
        writeInt(socket.getOutputStream(), numRead);
        socket.getOutputStream().write(Arrays.copyOf(data, numRead));
      }
      writeInt(socket.getOutputStream(), -1);
      receiverEnd.join();

    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Sender: finished.");

  }

  /*
    Disclaimer by Raami: I got the code in readInt and writeInt from a Google CodeU Project that I contributed to.
    I continue to use this file as a reference for serialization. Please reference link:
    https://github.com/rshar1/codeu_project_2017/blob/master/src/codeu/chat/util/Serializers.java
 */
  static int readInt(InputStream inputStream) throws IOException {

    int value = 0;

    for (int i = 0; i < 4; i++) {
      value = (value << 8) | inputStream.read();
    }

    return value;

  }

  static void writeInt(OutputStream out, int num) throws IOException {

    byte[] bytes = new byte[4];
    int index = 0;
    for (int i = 24; i >= 0; i -= 8) {
      bytes[index++] = (byte) (0xFF & (num >>> i));
    }

    out.write(bytes);
  }


}
