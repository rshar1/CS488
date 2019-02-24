package sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import common.RUDPSocket;
import java.util.Arrays;

public class Sender {

	final static int ownPort = 7777;
	final static int targetPort = 8888;
	static String host = "localhost";

	public static void main(String[] args) throws IOException, InterruptedException {
	        File file = new File("./test.txt");
	        FileInputStream fis = new FileInputStream(file);
	        
	        byte[] data = new byte[1024];
	        
	        System.out.println("Sender: connection built. about to send.");
	        
	        	try(RUDPSocket socket = new RUDPSocket(ownPort))
	        	{
	        		//socket.setSoTimeout(10000);
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
	        
	        fis.close();
		}

}
