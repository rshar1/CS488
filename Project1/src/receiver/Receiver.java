package receiver;

import java.io.FileOutputStream;

import common.RUDPSocket;

/**
 * This is a uni-directional receiver. That is, it will receive data from the port, and write it to
 * the file. It will then close when the sender closes its connection.
 */
public class Receiver {

	
	final static int ownPort = 8888;
	final static int targetPort = 7777;
	static String host = "localhost";

	public static void main(String[] args) {
	        byte[] data = new byte[1024];
	        
	        
	        System.out.println("Receiver: connection built. about to receive.");
	        	try(RUDPSocket socket = new RUDPSocket(ownPort);
								FileOutputStream fis = new FileOutputStream("./testReceived.txt"))
	        	{
	        		socket.acceptConnection();
	        		int numRead;
	        		while((numRead = socket.getInputStream().read(data)) != -1)
	        		{
	        			fis.write(data,0, numRead);
	        		}
	        	}
	        	catch(Exception e)
		        {
		        	e.printStackTrace();
		        }
	        System.out.println("Receiver: finished.");
	}
	
	
}
