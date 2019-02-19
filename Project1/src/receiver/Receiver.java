package receiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import common.RUDPSocket;

public class Receiver {

	
	final static int ownPort = 8888;
	final static int targetPort = 7777;
	static String host = "localhost";

	public static void main(String[] args) throws IOException, InterruptedException {
	        File file = new File("./Resource/copy_1_tcp.jpg");
	        FileOutputStream fis = new FileOutputStream(file);
	        
	        byte[] data = new byte[1024];
	        
	        //socket.setSoTimeout(10000);
	        System.out.println("Receiver: connection built. about to receive.");
	        
	        	try(RUDPSocket socket = new RUDPSocket(ownPort);)
	        	{
	        		while(socket.getInputStream().read(data) != -1)
	        		{
	        			fis.write(data);
	        		}
	        	}
	        	catch(Exception e)
		        {
	        		//todo
		        }
	        System.out.println("Receiver: finished.");
	        
	        fis.close();
		}
	
	
}
