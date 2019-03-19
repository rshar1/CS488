package receiver;

import common.RUDPServerSocket;
import java.io.FileOutputStream;

import common.RUDPSocket;

/**
 * This is a uni-directional receiver. That is, it will receive data from the port, and write it to
 * the file. It will then close when the sender closes its connection.
 */
public class Receiver {

	
	final static int ownPort = 8888;

	public static void main(String[] args) {
	        byte[] data = new byte[1024];
	        
	        
	        System.out.println("Receiver: connection built. about to receive.");
	        	try(RUDPServerSocket serverSocket = new RUDPServerSocket(ownPort);
								FileOutputStream fis = new FileOutputStream("./testReceived.txt"))
	        	{
	        		RUDPSocket socket = serverSocket.accept();
	        		int numRead;
	        		while((numRead = socket.getInputStream().read(data)) != -1)
	        		{
	        			fis.write(data,0, numRead);
	        		}
	        		socket.close();
	        	}
	        	catch(Exception e)
		        {
		        	e.printStackTrace();
		        }
	        System.out.println("Receiver: finished.");
	}
	
	
}
