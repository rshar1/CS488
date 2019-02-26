package receiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import common.RUDPSocket;

public class Receiver2 {

	
	final static int ownPort = 8888;
	final static int targetPort = 7777;
	static String host = "localhost";

	public static void main(String[] args) throws IOException, InterruptedException {
	        File file = new File("./testReceived.jpg");
	        File file2 = new File("./test2.jpg");
	        FileOutputStream fis = new FileOutputStream(file);
	        FileInputStream fis2 = new FileInputStream(file2);
	        
	        byte[] data = new byte[1024];
	        byte[] data2 = new byte[1024];
	        
	        
	        System.out.println("Receiver: connection built. about to receive.");
	        	try(RUDPSocket socket = new RUDPSocket(ownPort))
	        	{
	        		socket.acceptConnection();
	        		int numRead;
	        		int numRead2;
	        		do
	        		{
	        			numRead = socket.getInputStream().read(data);
	        			numRead2 = fis2.read(data2);
	        			if((numRead != -1))
	        			{
	        				fis.write(data,0, numRead);
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
	        
	        fis.close();
	        fis2.close();
	}
	
	
}
