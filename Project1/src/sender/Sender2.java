package sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import common.RUDPSocket;
import java.util.Arrays;

public class Sender2 {

	final static int ownPort = 7777;
	final static int targetPort = 8888;
	static String host = "localhost";

	public static void main(String[] args) throws IOException, InterruptedException {
	        File file = new File("./test.jpg");
	        File file2 = new File("./test2Received.jpg");
	        FileInputStream fis = new FileInputStream(file);
	        FileOutputStream fis2 = new FileOutputStream(file2);
	        
	        byte[] data = new byte[1024];
	        byte[] data2 = new byte[1024];
	        
	        System.out.println("Sender: connection built. about to send.");
	        
	        	try(RUDPSocket socket = new RUDPSocket(ownPort))
	        	{
	        		socket.connect(host, targetPort);
	        		int numRead;
	        		int numRead2;
	        		do
	        		{
	        			numRead = fis.read(data);
	        			numRead2 = socket.getInputStream().read(data2);
	        			if(numRead != -1)
						{
	        				socket.getOutputStream().write(Arrays.copyOf(data, numRead));
						}
	        			if(numRead2 != -1)
	        			{
	        				fis2.write(data2,0, numRead2);
	        			}
	        		}while((numRead != -1) || (numRead2 != -1));
	        	}
	        	catch(Exception e)
		        {
	        		e.printStackTrace();
		        }
	        System.out.println("Sender: finished.");
	        
	        fis.close();
	        fis2.close();
		}

}
