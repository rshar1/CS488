package common;

import java.net.DatagramSocket;

import common.SenderWindow.Frame;

public class SenderWindow {

  class Frame {
    private RUDPPacket packet;
    private boolean isAcked = false;
    private int inc = 0;
  }
  
  private static final int timeoutNum = 10;
  private static final int BUFF_SIZE = 12;
  private CircularBuffer<Frame> bufferQueue = new CircularBuffer<>(BUFF_SIZE);


  void acceptAck(int num) {
    // todo ack a message (mark akced)
	  for(Frame f:bufferQueue)
	  {
		  if(f.packet.getAckNum() == num)
			  f.isAcked = true;
			  
	  }
  }
  
  
  void timeOut(DatagramSocket socket)
  {
	  for(Frame f:this.bufferQueue)
	  {
		  f.inc++;
	  }
  }
  
  void getAcks() {
	  //?????????
  }

  boolean windowAvailable() {
    // todo return true if there is a free window space
	  if(this.bufferQueue.size() == this.BUFF_SIZE)
		  return false;
    return true;
  }

  void addPacket(RUDPPacket packet) {
    // todo
	  Frame f = new Frame();
	  f.packet = packet;
	  this.bufferQueue.add(f);
  }

  void slideWindow() {
	  while(this.bufferQueue.peek().isAcked)
	  {
		  this.bufferQueue.remove();
	  }
  }

}
