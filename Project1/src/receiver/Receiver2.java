package receiver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import common.RUDPSocket;

/**
 * Bi-directional Receiver. This will be accept a connection, and then it will receive a file, at the
 * same time as sending one back to the sender. WARNING: WE DID NOT IMPLEMENT AN EXIT.
 * IT WILL CONTINUOUSLY WAIT FOR MORE DATA FROM THE RECEIVER.
 */
public class Receiver2 {

	
	final static int ownPort = 8888;
	final static int targetPort = 7777;
	static String host = "localhost";

	public static void main(String[] args) {
	        
	        byte[] data = new byte[1024];
	        byte[] data2 = new byte[1024];
	        
	        System.out.println("Receiver: connection built. about to receive.");
	        	try(RUDPSocket socket = new RUDPSocket(ownPort);
						FileOutputStream outputFile = new FileOutputStream("./testReceived.txt");
						FileInputStream inputFile = new FileInputStream("./test2.txt"))
	        	{
	        		socket.acceptConnection();
	        		int numRead;
	        		int numRead2;
	        		do
	        		{
	        			numRead = socket.getInputStream().read(data);
	        			numRead2 = inputFile.read(data2);
	        			if((numRead != -1))
	        			{
	        				outputFile.write(data,0, numRead);
	        			}
	        			if((numRead2 != -1))
		        		{
		        			socket.getOutputStream().write(Arrays.copyOf(data2, numRead2));
		        		}
	        		}while((numRead != -1) || (numRead2 != -1));
	        	}
	        	catch(Exception e)
		        {
		        	e.printStackTrace();
		        }
	        System.out.println("Receiver: finished.");
	}
	
	
}
