package receiver;

import common.RUDPServerSocket;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    try(RUDPServerSocket serversocket = new RUDPServerSocket(ownPort);
        FileInputStream fis = new FileInputStream("./test2.txt");
        FileOutputStream fos = new FileOutputStream("./testReceived.txt"))
    {
      RUDPSocket socket = serversocket.accept();

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
      while((numRead = fis.read(data)) != -1)
      {
        writeInt(socket.getOutputStream(), numRead);
        socket.getOutputStream().write(Arrays.copyOf(data, numRead));
      }
      writeInt(socket.getOutputStream(), -1);
      receiverEnd.join();
      socket.close();

    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Receiver: finished.");

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
