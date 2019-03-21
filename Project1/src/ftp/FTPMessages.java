package ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FTPMessages {

  public final static int SEND = 0;
  public final static int RECEIVE = 1;

  public final static int BEGINTRANSFER = 1;

  public static int readInt(InputStream inputStream) throws IOException {

    int value = 0;

    for (int i = 0; i < 4; i++) {
      value = (value << 8) | inputStream.read();
    }

    return value;

  }

  public static void writeInt(OutputStream out, int num) throws IOException {

    for (int i = 24; i >= 0; i -= 8) {
      out.write(0xFF & (num >>> i));
    }
  }


}
