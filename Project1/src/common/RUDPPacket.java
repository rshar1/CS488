package common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The RUDPPacket contains the headers and payload for a reliable datagram. This packet class
 * also has some methods to easily transfer the packet to byte format and vice versa so as to send
 * it through the socket.
 * The receiving end, can also convert the incoming packet back into an RUDPPacket and perform
 * the necessary checks to ensure reliability.
 */
public class RUDPPacket {

  private int sequenceNumber; // 4 bytes
  private int ackNum;         // 4 bytes

  private boolean ack;
  private boolean connectAttempt; // 1 byte
  private byte[] data;            // 900 bytes max

  public RUDPPacket(int sequenceNumber, int ackNum) {
    this.sequenceNumber = sequenceNumber;
    this.ackNum = ackNum;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public int getAckNum() {
    return ackNum;
  }

  public boolean isConnectAttempt() {
    return connectAttempt;
  }

  public void setConnectAttempt(boolean connectAttempt) {
    this.connectAttempt = connectAttempt;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public byte[] toBytes() {

    byte[] res = null;

    try (ByteArrayOutputStream out = new ByteArrayOutputStream(905);
        DataOutputStream dOut = new DataOutputStream(out)) {

      dOut.writeInt(sequenceNumber);
      dOut.writeInt(ackNum);
      dOut.writeBoolean(connectAttempt);
      dOut.write(data);

      res = out.toByteArray();

    } catch (IOException exc) {
      // todo error converting
    }

    return res;
  }

  public static RUDPPacket fromBytes(byte[] bytes) throws IOException {

    RUDPPacket packet = null;

    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(in)) {

      int sequenceNumber = din.readInt();
      int ackNum = din.readInt();
      boolean connectAttempt = din.readBoolean();
      byte[] data = new byte[900];
      int dataLength = din.read(data);

      packet = new RUDPPacket(sequenceNumber, ackNum);
      packet.connectAttempt = connectAttempt;
      packet.data = data;

    }

    return packet;
  }

  public boolean isAck() {
    return ack;
  }

  public void setAck(boolean ack) {
    this.ack = ack;
  }
}
