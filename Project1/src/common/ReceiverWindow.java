package common;

/**
 * This data structure stores the buffer for any received packets that contain data. It reserves
 * space for any packets that were not loaded in
 */
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

	/**
	 * Packets that arrive, that contain data, are processed for storage here. It is first determined,
	 * if the received packet should be stored in the queue. If it is supposed to be in the queue,
	 * it will be added, and an ack will be sent. If it is already in, the function will just
	 * determine that an ack should be sent, but will not store it again.
	 * If it is not supposed to be in the queue, there is some issue with the syncing of the sender
	 * queue. There are two reasons this may be.
	 * 1) The receiver received the packet and the ack got lost. The application layer then read the data.
	 * When the sender resends it, the receiver must send an ack so that the sender window can move forward
	 * 2) The receiver received teh packet and sent the ack to the sender. The sender then sent the next
	 * batch of data. Our receiver window may be full, however, since the receiver may not have
	 * read from the buffer yet. For that reason, we shouldn't send an ack so that the data will eventually
	 * be resent by the sender.
	 * @param packet
	 * @return true if an ack should be sent about this packet, false otherwise.
	 */
	synchronized boolean processReceivedPacket(RUDPPacket packet) {
		// Check if the packet has any data
		// If it has data, try to add it to the receiver buffer in the right place
		int seqNum = packet.getSequenceNumber();
		int tailSeqNum = (currSequenceNum + BUFF_SIZE) % (RUDPSocket.MAX_SEQUENCE_NUM);

		// this just ensures that the MAX sequence number is large enough for our buffer size
		if (currSequenceNum < tailSeqNum || currSequenceNum > tailSeqNum) {

			// This is the index number of this sequence number
			int adjustedIndex = getSeqIndexNum(seqNum);

			int replacement = (seqNum + BUFF_SIZE) % RUDPSocket.MAX_SEQUENCE_NUM;
			int replacementAdjustedIndex = getSeqIndexNum(replacement);

			// If the sequence number is not in our queue
			if (adjustedIndex >= BUFF_SIZE) {
				// Check if an ack should be sent or not
				if (replacementAdjustedIndex >= 0 && replacementAdjustedIndex < BUFF_SIZE && bufferQueue
						.get(replacementAdjustedIndex).isPlaceholder()) {
					return true;
				} else {
					return false;
				}
			}

			Frame m_Frame;
			try {
				m_Frame = bufferQueue.get(adjustedIndex);
			} catch (IndexOutOfBoundsException exc) {
				System.out.println("MAXSEQNUM: " + RUDPSocket.MAX_SEQUENCE_NUM);
				System.out.println("Curr Head: " + this.currSequenceNum);
				System.out.println("Seq num: " + seqNum);

				for (Frame frame : bufferQueue) {
					if (frame.packet != null) {
						System.out.println("Frame in buffer" + frame.packet.toString());
					} else {
						System.out.println("Placeholder");
					}
				}

				throw exc;
			}

			// Only packets that have data, or are a finish packet, should be acked. Only reason to
			// ack a message is to remove it from the sender's window.
			if (m_Frame.isPlaceholder()) {
				m_Frame.packet = packet;
				this.notify();
				if (packet.hasData() || packet.isFinished())
					return true;
			}

			return false;

		} else {
			// This should never happen as long as we follow the rule of how window size relates
			// to max sequence number.
			throw new IllegalArgumentException("The window size is too small for the max window size");
		}

	}

	/**
	 * The input stream in RUDPSocket uses this method to get the next batch of bytes.
	 * @param block if this method should block until data is received, or if it should just return
	 * a null byte array
	 * @return if block is true, then it will send data when there is some. if block is false, it may
	 * send data or it may send null. It all depends on the receiver window at that moment
	 */
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

		Frame f = bufferQueue.peek();

		// todo should it be an illegalArgumentException???
		if (f.packet.isFinished() && !f.packet.hasData()) {
			System.out.println("Found a finished packet");
			throw new IllegalArgumentException("Finished");
		} else if (f.packet.isFinished()) {
			byte[] ret = f.packet.getData();
			f.packet.setData(new byte[0]);
			return ret;
		} else if (!f.packet.hasData()) {
			bufferQueue.poll();
			f.packet = null;
			currSequenceNum = (currSequenceNum + 1) % RUDPSocket.MAX_SEQUENCE_NUM;
			bufferQueue.add(f);
			slideWindow();
			return null;
		} else {
			bufferQueue.poll();
			byte[] ret = f.packet.getData();
			f.packet = null;
			currSequenceNum = (currSequenceNum + 1) % RUDPSocket.MAX_SEQUENCE_NUM;
			bufferQueue.add(f);
			slideWindow();
			return ret;
		}

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

	private int getSeqIndexNum(int seqNum) {
		return (RUDPSocket.MAX_SEQUENCE_NUM - currSequenceNum + seqNum) % RUDPSocket.MAX_SEQUENCE_NUM;
	}

}
