package common;

import common.SenderWindow.Frame;

public class ReceiverWindow {

		  class Frame {
		    private RUDPPacket packet;
		  }
		  
		  private static final int BUFF_SIZE = 12;
		  private CircularBuffer<Frame> bufferQueue = new CircularBuffer<>(BUFF_SIZE);
		  
		  
		  void addPacket(RUDPPacket packet) {
			    // todo
				  Frame f = new Frame();
				  f.packet = packet;
				  this.bufferQueue.add(f);
			  }
		  
		  //todo receiver oerder
}
