package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

public class ConnectionListener extends Thread {

    @Override
    public void run() {

        final int port = 3141;
        final int timeout = 1000;
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            serverSocket.setSoTimeout(timeout);

            while (!this.isInterrupted()) {
                try {
                    ClientHandler clientHandler = new ClientHandler(serverSocket.accept());
                    clientHandler.start(); // einzelner Thread bearbeitet eine aufgebaute Verbindung
                } catch (SocketTimeoutException e) {
                    //System.out.println("timeout");
                } catch (IOException e) {
                    System.out.println("IOException von serverSocket.accept()");
                }
            }

    } catch (IOException e) {
            System.out.println("IOException beim Aufbau des Server Sockets");
        }
    }
}
