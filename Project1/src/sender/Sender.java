package sender;

import java.io.FileInputStream;

import common.RUDPSocket;
import java.util.Arrays;

/**
 * This is the sender for a uni-directional file transfer. It connects to the receiver, and ends
 * the connection when it is done sending the data
 */
public class Sender {

	final static int ownPort = 7777;
	final static int targetPort = 8888;
	static String host = "localhost";

	public static void main(String[] args) {
	        
	        byte[] data = new byte[1024];
	        
	        System.out.println("Sender: connection built. about to send.");
	        
	        	try(RUDPSocket socket = new RUDPSocket(ownPort);
						FileInputStream fis = new FileInputStream("./text.txt"))
	        	{
	        		socket.connect(host, targetPort);
	        		int numRead;
	        		while((numRead = fis.read(data)) != -1)
	        		{
	        			socket.getOutputStream().write(Arrays.copyOf(data, numRead));
	        		}
	        	}
	        	catch(Exception e)
		        {
	        		e.printStackTrace();
		        }
	        System.out.println("Sender: finished.");
		}

}
