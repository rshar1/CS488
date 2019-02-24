package common;


public class ReceiverWindow {

		  class Frame {
		    private RUDPPacket packet;

		    private Frame() {
		    	this(null);
				}

				private Frame(RUDPPacket packet) {
		    	this.packet = packet;
				}

		    boolean isPlaceholder() {
		    	return packet == null;
				}
		  }
		  
		  private static final int BUFF_SIZE = 12;
		  private CircularBuffer<Frame> bufferQueue = new CircularBuffer<>(BUFF_SIZE);

		  private int currSequenceNum;

	/**
	 * Constructs a new receiver window with the initial expected sequence number as the head.
	 * This allows space to be reserved for any expected packets.
	 * @param expectedSequenceNum
	 */
		ReceiverWindow(int expectedSequenceNum) {
			this.currSequenceNum = expectedSequenceNum;

			// Fills the window with placeholders
			for (int i = 0; i < BUFF_SIZE; i++) {
				Frame f = new Frame();
				bufferQueue.add(f);
			}

		}

	synchronized boolean processReceivedPacket(RUDPPacket packet) {
		// Check if the packet has any data
		// If it has data, try to add it to the receiver buffer in the right place
		int seqNum = packet.getSequenceNumber();
		int tailSeqNum = (currSequenceNum + BUFF_SIZE) % (RUDPSocket.MAX_SEQUENCE_NUM);

		if (currSequenceNum < tailSeqNum || currSequenceNum > tailSeqNum) {

			int adjustedIndex =
					(RUDPSocket.MAX_SEQUENCE_NUM - currSequenceNum + seqNum) % RUDPSocket.MAX_SEQUENCE_NUM;
			Frame m_Frame = bufferQueue.get(adjustedIndex);

			if (m_Frame.isPlaceholder()) {
				m_Frame.packet = packet;
				this.notify();
			}

			if (packet.hasData() || packet.isFinished())
				return true;
			else {
				return false;
			}

		} else {
			// This should never happen as long as we follow the rule of how window size relates
			// to max sequence number.
			throw new IllegalArgumentException("The window size is too small for the max window size");
		}

		// todo return true if an ack should be sent for this packet, otherwise false

	}

	synchronized byte[] receivePackets(boolean block) {
		// Check if the head of the queue is a placeholder. If it is, then block depending on the value of block.
		// If there is data, then remove the packet from the queue and return the application data
		// that was in the packet. Keep looping through until we find some data

		slideWindow();

		while (bufferQueue.peek().isPlaceholder()) {
			if (block) {
				try {
					this.wait();
				} catch (InterruptedException exc) {
					// todo nothing
				}
			} else {
				return null;
			}
		}

		Frame f = bufferQueue.poll();

		// todo should it be an illegalArgumentException???
		if(f.packet.isFinished() && !f.packet.hasData()) 
    	{
    		throw new IllegalArgumentException("Finished");
    	}
		
		byte[] ret = f.packet.getData();
		f.packet = null;
		currSequenceNum = (currSequenceNum + 1) % RUDPSocket.MAX_SEQUENCE_NUM;
		bufferQueue.add(f);
		slideWindow();

		return ret;

	}

	// Slides the window so that the head is either a placeholder or is containing some data.
	// This is to remove any packets that were stored such as an ack from the other end host
	private void slideWindow() {

		Frame frame;
		while (!(frame = bufferQueue.peek()).isPlaceholder()) {
			// check if it contains any data
			if (!frame.packet.hasData() && !frame.packet.isFinished()) {
				bufferQueue.poll();
				frame.packet = null;
				currSequenceNum = (currSequenceNum + 1) % RUDPSocket.MAX_SEQUENCE_NUM;
				bufferQueue.add(frame);
			} else {
				return;
			}
		}

	}

}
