package server;

import common.RUDPServerSocket;
import common.RUDPSocket;
import ftp.FTPMessages;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Hashtable;

public class Main {

  public static final int PORT = 1177;

  private static Hashtable<Integer, RUDPSocket> waitingReceivers = new Hashtable<>();
  private static Hashtable<Integer, ArrayDeque<RUDPSocket>> waitingSenders = new Hashtable<>();

  static class FTPTransfer implements Runnable {

    private RUDPSocket sender;
    private RUDPSocket receiver;

    FTPTransfer (RUDPSocket sender, RUDPSocket receiver) {
      this.sender = sender;
      this.receiver = receiver;
    }

    @Override
    public void run() {

      byte[] buf = new byte[1500];
      int numRead;

      try {

        FTPMessages.writeInt(this.sender.getOutputStream(), FTPMessages.BEGINTRANSFER);

        while ((numRead = this.sender.getInputStream().read(buf)) != -1) {
          this.receiver.getOutputStream().write(Arrays.copyOf(buf, numRead));
        }

        this.sender.close();
        this.receiver.close();

        System.out.println("CLOSING CONNECTION");

      } catch (Exception exc) {
        exc.printStackTrace();
      }
    }

  }


  public static void main(String[] args) {

    try (RUDPServerSocket serverSocket = new RUDPServerSocket(PORT)) {

      while (true) {
        RUDPSocket socket = serverSocket.accept();

        System.out.println("Accepted a connection");
        handleConnection(socket);
      }
    } catch (Exception exc) {
      System.out.println("Exception thrown");
      exc.printStackTrace();
    }

  }

  private static void handleConnection(RUDPSocket socket) throws IOException {

    System.out.println("Handling connection");
    // check if it a sender or a receiver
    InputStream in = socket.getInputStream();
    int senderOrReceiver = FTPMessages.readInt(in);

    if (senderOrReceiver == FTPMessages.SEND) {
      handleSender(socket);
    } else {
      handleReceiver(socket);
    }

  }

  private static void handleSender(RUDPSocket socket) throws IOException {

    System.out.println("Handling sender");
    InputStream in = socket.getInputStream();
    int recipient = FTPMessages.readInt(in);

    // Check if the recipient is waiting
    if (waitingReceivers.containsKey(recipient)) {
      // If the recipient is waiting begin transfer
      RUDPSocket receiver = waitingReceivers.remove(recipient);
      FTPTransfer transferRunnable = new FTPTransfer(socket, receiver);
      new Thread(transferRunnable).start();
    } else {
      // If the recipient isn't waiting do not begin the transfer
      if (!waitingSenders.containsKey(recipient)) {
        waitingSenders.put(recipient, new ArrayDeque<>());
      }
      waitingSenders.get(recipient).add(socket);
    }

  }

  private static void handleReceiver(RUDPSocket socket) throws IOException {
    System.out.println("Handling receiver");
    InputStream in = socket.getInputStream();
    int recipient = FTPMessages.readInt(in);

    // Check if the sender is waiting
    if (waitingSenders.containsKey(recipient)) {
      // If the sender is waiting begin transfer
      RUDPSocket sender = waitingSenders.get(recipient).poll();
      FTPTransfer transferRunnable = new FTPTransfer(sender, socket);
      new Thread(transferRunnable).start();
      if (waitingSenders.get(recipient).isEmpty()) {
        waitingSenders.remove(recipient);
      }

    } else {
      // If the recipient isn't waiting do not begin the transfer
      waitingReceivers.put(recipient, socket);
    }
  }

}
