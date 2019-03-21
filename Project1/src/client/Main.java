package client;

import common.RUDPSocket;
import ftp.FTPMessages;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

  private static int id;

  public static void main(String[] args) {

    Scanner sc = new Scanner(System.in);

    System.out.println("Enter a unique id: ");
    id = sc.nextInt();

    mainMenu();

    System.out.println("Signing out. Goodbye user #" + id);

  }

  private static void receiveFile() {

    try (RUDPSocket socket = new RUDPSocket()) {

      System.out.println("Before connect");
      socket.connect("localhost" , 1177);

      InputStream in = socket.getInputStream();
      OutputStream out = socket.getOutputStream();

      System.out.println("Connected");
      FTPMessages.writeInt(out, FTPMessages.RECEIVE);
      FTPMessages.writeInt(out, id);

      int fileNameSize = FTPMessages.readInt(in);
      if (fileNameSize > 0) {

        byte[] fileName = new byte[fileNameSize];
        int numRead = in.read(fileName);
        if (numRead < fileNameSize) {
          System.out.println("This should not have happened");
        } else {

          try (OutputStream fout = new FileOutputStream(new String(fileName))) {

            int dataLength;
            while ((dataLength = FTPMessages.readInt(in)) > -1) {
              byte[] incomingData = new byte[dataLength];
              for (int i = 0; i < incomingData.length; i++) {
                incomingData[i] = (byte) socket.getInputStream().read();
              }
              fout.write(incomingData, 0, dataLength);
            }
          }

          System.out.println("File saved with name: " + new String(fileName));

        }

      } else {
        System.out.println("Invalid file size");
      }

    } catch (SocketException exc) {

    } catch (Exception exc) {

    }

  }

  private static void sendFile() {

    Scanner sc = new Scanner(System.in);
    System.out.println("What file would you like to send? ");

    String fileToSend = sc.nextLine();

    File file = new File(fileToSend);

    if (!file.exists()) {

      System.out.println("That file doesn't exist");
      return;
    }

    System.out.println("Enter client id of recipient: ");

    int recipientId = sc.nextInt();

    try (RUDPSocket socket = new RUDPSocket();
        InputStream in = new FileInputStream(file);) {

      socket.connect("localhost", 1177);

      InputStream sin = socket.getInputStream();
      OutputStream out = socket.getOutputStream();

      FTPMessages.writeInt(out, FTPMessages.SEND);
      FTPMessages.writeInt(out, recipientId);

      if (FTPMessages.readInt(sin) == FTPMessages.BEGINTRANSFER) {

        // send the file name
        byte[] fileName = file.getName().getBytes();
        FTPMessages.writeInt(out, fileName.length);
        out.write(fileName);

        // send the file contents
        int numRead;
        byte[] data = new byte[1024];
        while((numRead = in.read(data)) != -1)
        {
          FTPMessages.writeInt(out, numRead);
          out.write(Arrays.copyOf(data, numRead));
        }
        FTPMessages.writeInt(out, -1);

      } else {
        System.out.println("The server didn't accept request to send");
      }


    } catch (SocketException exc) {
      System.out.println("Error setting up socket");
      exc.printStackTrace();
    } catch (FileNotFoundException exc) {
      System.out.println("File not found");
    } catch (IOException exc) {
      System.out.println("IOException");
    } catch (Exception exc) {
      exc.printStackTrace();
    }

  }

  private static void changeUsername() {
    Scanner sc = new Scanner(System.in);
    System.out.println("Current Username: " + id);
    System.out.print("Enter a new user id: ");
    id = sc.nextInt();
    System.out.println("User id changed to: " + id);
  }

  private static void mainMenu() {

    Scanner sc = new Scanner(System.in);
    System.out.println("Welcome to RUDP File Transfer Application");
    System.out.println("User Id: " + id);

    int user = 1;

    while (user != 0) {
      System.out.println("\n\n-----------------------------\n\n");
      System.out.println("What would you like to do? ");
      System.out.println("1 - Send a file");
      System.out.println("2 - Receive a file");
      System.out.println("3 - Change username");
      System.out.println("0 - Exit");
      System.out.print("Enter your choice: ");
      user = sc.nextInt();

      switch (user) {
        case 1:
          sendFile();
          break;
        case 2:
          receiveFile();
          break;
        case 3:
          changeUsername();
          break;
        case 0:
          break;
        default:
          System.out.println("Invalid choice");
      }
    }

  }

}
