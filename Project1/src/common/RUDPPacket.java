package common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * The RUDPPacket contains the headers and payload for a reliable datagram. This packet class
 * also has some methods to easily transfer the packet to byte format and vice versa so as to send
 * it through the socket.
 * The receiving end, can also convert the incoming packet back into an RUDPPacket and perform
 * the necessary checks to ensure reliability.
 */
public class RUDPPacket {

  public static final int MAX_DATA_SIZE = 900;

  private int sequenceNumber; // 4 bytes
  private int ackNum;         // 4 bytes

  private boolean ack;
  private boolean connectAttempt; // 1 byte
  private byte[] data;            // 900 bytes max

  private int dataLength;       // 4 bytes
  private boolean finished;     // 1 byte

  public RUDPPacket(int sequenceNumber, int ackNum) {
    this.sequenceNumber = sequenceNumber;
    this.ackNum = ackNum;

    this.ack = false;
    this.connectAttempt = false;
    this.data = new byte[0];
    this.dataLength = 0;
    this.finished = false;
  }

  public boolean isFinished() {
	    return this.finished;
	  }
  
  public void setFinished() {
	    this.finished = true;
	  }
  
  public boolean hasData() {
    return this.dataLength > 0;
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
    if (data.length > MAX_DATA_SIZE) {
      throw new IllegalArgumentException("Data passed in is too large");
    }

    this.dataLength = data.length;
    this.data = data;
  }

  public byte[] toBytes() {

    byte[] res = null;

    try (ByteArrayOutputStream out = new ByteArrayOutputStream(915);
        DataOutputStream dOut = new DataOutputStream(out)) {

      dOut.writeInt(sequenceNumber);
      dOut.writeInt(ackNum);

      dOut.writeBoolean(ack);
      dOut.writeBoolean(connectAttempt);
      dOut.writeInt(dataLength);
      dOut.write(data);
      dOut.writeBoolean(finished);

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
      boolean ack = din.readBoolean();
      boolean connectAttempt = din.readBoolean();
      int dataLength = din.readInt();
      byte[] data = new byte[dataLength];
      din.read(data);
      boolean finished = din.readBoolean();

      packet = new RUDPPacket(sequenceNumber, ackNum);
      packet.ack = ack;
      packet.connectAttempt = connectAttempt;
      packet.data = data;
      packet.dataLength = dataLength;
      packet.finished = finished;

    }

    return packet;
  }

  public boolean isAck() {
    return ack;
  }

  public void setAck(boolean ack) {
    this.ack = ack;
  }
  
  public DatagramPacket convertPacket(RUDPAddress rudpAddress)
  {
	  byte[] payload = this.toBytes();
	  DatagramPacket datagramPacket = new DatagramPacket(payload, payload.length, rudpAddress.getHost(), rudpAddress.getPort());
	  return datagramPacket;
  }

  public int getDataLength() {
    return this.dataLength;
  }

  @Override
  public String toString() {
    return "RUDPPacket{" +
        "sequenceNumber=" + sequenceNumber +
        ", ackNum=" + ackNum +
        ", ack=" + ack +
        ", connectAttempt=" + connectAttempt +
        ", data=" + Arrays.toString(data) +
        ", dataLength=" + dataLength +
        ", finished=" + finished +
        '}';
  }

}
