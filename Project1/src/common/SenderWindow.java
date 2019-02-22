package common;


public class SenderWindow {

  class Frame {
    private RUDPPacket packet;
    private boolean isAcked = false;
    private int inc;
  }


  private CircularBuffer<Frame> bufferQueue;


  void acceptAck(int num) {
    // todo ack a message (mark akced)
	  for(Frame f:bufferQueue)
	  {
		  if(f.packet.getAckNum() == num)
			  f.isAcked = true;
			  
	  }
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
