package common;

import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.*;

public class RUDPPacketTest {

  @Test
  public void simpleToAndFromBytes() {

    RUDPPacket rudpPacket = new RUDPPacket(5, 10);
    try {
      RUDPPacket packetReturned = RUDPPacket.fromBytes(rudpPacket.toBytes());
      assertTrue(!packetReturned.hasData());
      assertEquals(packetReturned.getSequenceNumber(), 5);
      assertEquals(packetReturned.getAckNum(), 10);
      assertTrue(!packetReturned.isConnectAttempt());
    } catch (IOException exc) {
      fail();
    }
  }

  @Test
  public void dataConversionTest() throws IOException {
    RUDPPacket packet = new RUDPPacket(150, 200);
    byte[] buf = new byte[100];
    for (int i = 0; i < buf.length; i++) {
      buf[i] = (byte) i;
    }

    packet.setData(buf);
    RUDPPacket convertedPacket = RUDPPacket.fromBytes(packet.toBytes());
    assertTrue(convertedPacket.hasData());
    assertEquals(convertedPacket.getSequenceNumber(), packet.getSequenceNumber());
    assertEquals(convertedPacket.getAckNum(), packet.getAckNum());
    assertEquals(packet.getDataLength(), packet.getData().length);
    assertTrue(!convertedPacket.isConnectAttempt());

    for (int i = 0; i < buf.length; i++) {
      assertEquals(convertedPacket.getData()[i], buf[i]);
    }

  }


  @Test
  public void setData() {
    RUDPPacket rudpPacket = new RUDPPacket(10, 20);

    byte[] oversizedbuffer = new byte[2000];

    try {
      rudpPacket.setData(oversizedbuffer);
      fail();
    } catch (IllegalArgumentException exc) {

    }

  }
}
