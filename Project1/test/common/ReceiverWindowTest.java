package common;

import static org.junit.Assert.*;

import org.junit.Test;

public class ReceiverWindowTest {

  // This tests that placeholders work properly if the sequence number starts at 0
  @Test
  public void testPlaceholders() {
    ReceiverWindow receiverWindow = new ReceiverWindow(0);

    RUDPPacket packet0 = new RUDPPacket(0, 0);
    byte[] b0 = {0};
    packet0.setData(b0);

    RUDPPacket packet1 = new RUDPPacket(1, 0);
    byte[] b1 = {1};
    packet1.setData(b1);

    RUDPPacket packet2 = new RUDPPacket(2, 0);
    byte[] b2 = {2};
    packet2.setData(b2);

    RUDPPacket packet3 = new RUDPPacket(3, 0);
    byte[] b3 = {3};
    packet3.setData(b3);

    receiverWindow.processReceivedPacket(packet0);
    receiverWindow.processReceivedPacket(packet3);

    assertEquals(b0, receiverWindow.receivePackets(false));
    assertNull(receiverWindow.receivePackets(false));

    receiverWindow.processReceivedPacket(packet2);
    assertNull(receiverWindow.receivePackets(false));

    receiverWindow.processReceivedPacket(packet1);
    assertEquals(b1, receiverWindow.receivePackets(false));
    assertEquals(b2, receiverWindow.receivePackets(false));
    assertEquals(b3, receiverWindow.receivePackets(false));
    assertNull(receiverWindow.receivePackets(false));

  }

  // Tests that placeholders work properly if the sequence number starts at 23, which is near the
  // top of the loop
  @Test
  public void testAdjustedReceiver() {
      ReceiverWindow receiverWindow = new ReceiverWindow(23);

      RUDPPacket packet0 = new RUDPPacket(23, 0);
      byte[] b0 = {12};
      packet0.setData(b0);

      RUDPPacket packet1 = new RUDPPacket(24, 0);
      byte[] b1 = {1};
      packet1.setData(b1);

      RUDPPacket packet2 = new RUDPPacket(0, 0);
      byte[] b2 = {2};
      packet2.setData(b2);

      RUDPPacket packet3 = new RUDPPacket(1, 0);
      byte[] b3 = {3};
      packet3.setData(b3);

      receiverWindow.processReceivedPacket(packet0);
      receiverWindow.processReceivedPacket(packet3);

      assertEquals(b0, receiverWindow.receivePackets(false));
      assertNull(receiverWindow.receivePackets(false));

      receiverWindow.processReceivedPacket(packet2);
      assertNull(receiverWindow.receivePackets(false));

      receiverWindow.processReceivedPacket(packet1);
      assertEquals(b1, receiverWindow.receivePackets(false));
      assertEquals(b2, receiverWindow.receivePackets(false));
      assertEquals(b3, receiverWindow.receivePackets(false));
      assertNull(receiverWindow.receivePackets(false));

  }

  @Test
  public void testOutOfBoundsPacket() {

    ReceiverWindow receiverWindow = new ReceiverWindow(23);

    RUDPPacket packet0 = new RUDPPacket(22, 0);
    byte[] b0 = {22};
    packet0.setData(b0);

    try {
      receiverWindow.processReceivedPacket(packet0);
      fail();
    } catch (IndexOutOfBoundsException exc) {

    }

  }

  // Test to make sure that packets that don't have data are ignored when the window slides
  @Test
  public void testSlidingWithAcks() {

    ReceiverWindow receiverWindow = new ReceiverWindow(0);

    RUDPPacket packet0 = new RUDPPacket(0, 0);
    byte[] b0 = {0};
    packet0.setData(b0);

    RUDPPacket packet1 = new RUDPPacket(1, 0);
    byte[] b1 = {1};

    RUDPPacket packet2 = new RUDPPacket(2, 0);
    byte[] b2 = {2};

    RUDPPacket packet3 = new RUDPPacket(3, 0);
    byte[] b3 = {3};
    packet3.setData(b3);

    receiverWindow.processReceivedPacket(packet0);
    receiverWindow.processReceivedPacket(packet3);

    assertEquals(b0, receiverWindow.receivePackets(false));
    assertNull(receiverWindow.receivePackets(false));

    receiverWindow.processReceivedPacket(packet2);
    assertNull(receiverWindow.receivePackets(false));

    receiverWindow.processReceivedPacket(packet1);
    assertEquals(b3, receiverWindow.receivePackets(false));
    assertNull(receiverWindow.receivePackets(false));

  }

}