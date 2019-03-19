import java.io.IOException;
import java.net.*;

public class Iperfer {

  private final String HOSTNAME;
  private final int PORT;
  private final int TIME;

  // store the current start time
  private long startTime;

  public Iperfer(String serverHostname, int port, int time) {
    this.HOSTNAME = serverHostname;
    this.PORT = port;
    this.TIME = time;
  }

  /**
   * Begins sending the packets
   */
  public void run() {
    this.startTime = System.currentTimeMillis();
    System.out.println("Connecting to host with address: " + this.HOSTNAME);
    System.out.println("Port: " + this.PORT);
    System.out.println("Duration: " + this.TIME);
    System.out.println("At time (millis): " + startTime);
    System.out.println("Which means " + (this.TIME - (System.currentTimeMillis() - this.startTime) / 1000.0) + " seconds left");


    // todo Edward: connect by tcp
    try {
    	
		Socket server = new Socket(this.HOSTNAME, this.PORT);
		byte[] data = new byte[1000];
		double sentB = 0;
		while(startTime+(this.TIME*1000) > System.currentTimeMillis())
		{
			server.getOutputStream().write(data);
			sentB+=1000;
		}
		server.close();
		double sentKB = sentB/1000;
		double rate = (sentKB/1000)/this.TIME;
		System.out.println("sent="+sentKB+" KB rate="+rate+" Mbps");
		
	} catch (Exception e) {
		System.out.println("Invalid argument");
		e.printStackTrace();
	}
    
  }

  public static void main(String[] args) {

    // Check if the arguments provided are valid
    String hostName = null;

    int port = 0;

    int time = 0;

    if (args.length != 6) {
      System.out.println("Error: missing or additional arguments");
      printUsage();
      System.exit(0);
    }

    for (int i = 0; i < args.length; i += 2) {

      String flag = args[i];
      String value = args[i + 1];

      if (flag.length() != 2 && flag.charAt(0) != '-') {
        System.out.println("Invalid argument");
        printUsage();
        System.exit(0);
      }

      switch (flag.charAt(1)) {
        case 'h':
          if (hostName != null) {
            System.out.println("Invalid arguments");
            printUsage();
            System.exit(0);
          }
          hostName = value;
          break;
        case 'p':
          if (port != 0) {
            System.out.println("Invalid arguments");
            printUsage();
            System.exit(0);
          }
          try {
            port = Integer.parseInt(value);
            if (port < 1024 || port > 65535) {
              throw new NumberFormatException("Out of port bounds");
            }
          } catch (NumberFormatException exc) {
            System.out.println("Error: port number must be in the range 1024 to 65535");
            System.exit(0);
          }
          break;
        case 't':
          if (time != 0) {
            System.out.println("Invalid arguments");
            printUsage();
            System.exit(0);
          }
          try {
            time = Integer.parseInt(value);
            if (time <= 0) {
              throw new NumberFormatException("Time must be greater than 0");
            }
          } catch (NumberFormatException exc) {
            System.out.println("Error: Time must be an integer greater than 0");
            System.exit(0);
          }
          break;
        default:
          System.out.println("Unknown flag provided");
          printUsage();
          System.exit(0);
      }



    }

    Iperfer iperfer = new Iperfer(hostName, port, time);

    iperfer.run();


  }

  public static void printUsage() {
    System.out.println("Usage: java Iperfer -h <server hostname> -p <server port> -t <time>");
    System.out.println("server hostname is the hostname or IP address of the iperf server which will consume data");
    System.out.println("server port is the port on which the remote host is waiting to consume data; the port should be in the range 1024 < server port < 65535");
    System.out.println("time is the duration in seconds for which data should be generated");
  }

}
