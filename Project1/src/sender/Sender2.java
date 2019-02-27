package sender;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import common.RUDPSocket;
import java.util.Arrays;

/**
 *	A bi-directional sender. Initiates a connection with the receiver and begins sending data.
 *	It will also receive data and store it into a file. WARNING: WE DID NOT IMPLEMENT AN EXIT.
 *  IT WILL CONTINUOUSLY WAIT FOR MORE DATA FROM THE RECEIVER.
 */
public class Sender2 {

	final static int ownPort = 7777;
	final static int targetPort = 8888;
	static String host = "localhost";

	public static void main(String[] args) {
	        
	        byte[] data = new byte[1024];
	        byte[] data2 = new byte[1024];
	        
	        System.out.println("Sender: connection built. about to send.");
	        
	        	try(RUDPSocket socket = new RUDPSocket(ownPort);
						FileInputStream inputFile = new FileInputStream("./test.txt");
						FileOutputStream outputFile = new FileOutputStream("./test2Received.txt"))
	        	{
	        		socket.connect(host, targetPort);
	        		int numRead;
	        		int numRead2;
	        		do
	        		{
	        			numRead = inputFile.read(data);
	        			numRead2 = socket.getInputStream().read(data2);
	        			if(numRead != -1)
						{
	        				socket.getOutputStream().write(Arrays.copyOf(data, numRead));
						}
	        			if(numRead2 != -1)
	        			{
	        				outputFile.write(data2,0, numRead2);
	        			}
	        		}while((numRead != -1) || (numRead2 != -1));
	        	}
	        	catch(Exception e)
		        {
	        		e.printStackTrace();
		        }
	        System.out.println("Sender: finished.");
		}

}
