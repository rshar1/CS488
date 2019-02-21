package common;


public class SenderWindow {

  class Frame {
    private RUDPPacket packet;
    private boolean isAcked;
    private int inc;
  }


  private CircularBuffer<Frame> bufferQueue;


  void acceptAck(int num) {
    // todo append this ack number to an RUDP packet that needs to be sent out
  }

  void getAcks() {

  }

  boolean windowAvailable() {
    // todo return true if there is a free window space
    return false;
  }

  void addPacket(RUDPPacket packet) {
    // todo
  }

  void slideWindow() {

  }

}
