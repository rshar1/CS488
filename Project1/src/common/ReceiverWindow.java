package common;


public class ReceiverWindow {

		  class Frame {
		    private RUDPPacket packet;
		  }
		  
		  private static final int BUFF_SIZE = 12;
		  private CircularBuffer<Frame> bufferQueue = new CircularBuffer<>(BUFF_SIZE);
		  
		  
		  void processReceivedPacket(RUDPPacket packet) {
			    // todo finish implementing
					// Check if the packet has any data
					// If it has data, try to add it to the receiver buffer in the right place
				  Frame f = new Frame();
				  f.packet = packet;
				  this.bufferQueue.add(f);
			  }

			byte[] receivePackets(boolean block) {
		  	// todo implement
				// Check if the head of the queue is a placeholder. If it is, then block depending on the value of block.
				// If there is data, then remove the packet from the queue and return the application data
				// that was in the packet. Keep looping through until we find some data
				return null;
			}
}
