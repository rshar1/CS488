package common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * The sender window contains the packets that have been sent out. It keeps track of which ones
 * have been acked by the receiver and if they should be removed.
 */
public class SenderWindow {

  private class Frame {
    private RUDPPacket packet;
    private boolean isAcked = false;
    private int inc = 0;
  }
  
  private static final int TIMEOUT_NUM = 2;
  private static final int BUFF_SIZE = 12;
  private CircularBuffer<Frame> bufferQueue = new CircularBuffer<>(BUFF_SIZE);

	/**
	 * This will set the ack for the given sequence number to true
	 * @param num
	 */
  synchronized void acceptAck(int num) {
	  for(Frame f:bufferQueue)
	  {
		  if(f.packet.getSequenceNumber() == num) {
				f.isAcked = true;
				this.slideWindow();
				return;
			}
			  
	  }

  }

	/**
	 * Resends all the packets that have reached the time out threshold
	 * @param socket the socket to send the packets out on
	 * @param rudpAddress the address and port that the packets should be resent to on timeout
	 * @throws IOException
	 */
  void timeOut(DatagramSocket socket, RUDPAddress rudpAddress) throws IOException
  {
	  for(Frame f:this.bufferQueue)
	  {
		  f.inc++;
		  if(f.inc >= TIMEOUT_NUM)
		  {
			  f.inc = 0;
			  DatagramPacket p = f.packet.convertPacket(rudpAddress);
			  System.out.println("Sending packet " + f.packet);
			  socket.send(p);
		  }
	  }
	  
  }

  boolean isEmpty()
  {
	  return this.bufferQueue.size() == 0;
  }

  boolean windowAvailable() {
	  if(this.bufferQueue.size() == this.BUFF_SIZE)
		  return false;
    return true;
  }

  synchronized void addPacket(RUDPPacket packet) {
	  Frame f = new Frame();
	  f.packet = packet;
	  while (!windowAvailable()) {
	  	try {
	  		this.wait();
			} catch (InterruptedException exc) {
	  		System.out.println("Add packet interrupted");
			}
		}

	  this.bufferQueue.add(f);
  }

  private void slideWindow() {
	  while(!this.bufferQueue.isEmpty() &&  this.bufferQueue.peek().isAcked)
	  {
		  this.bufferQueue.remove();
			notify();
	  }
  }

}
