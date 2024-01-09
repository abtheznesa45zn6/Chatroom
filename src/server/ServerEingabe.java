/**

import java.io.IOException;
import java.util.Scanner;

// extra Thread, um Server über Konsole zu schreiben
// ich weiß nicht, ob das nötig ist

public class ServerEingabe extends Thread {


    public void run() {
            Scanner keyboard = new Scanner(System.in);
            while (true) {
                String eingabe = keyboard.nextLine().toLowerCase();
                switch (eingabe) {
                    case "stop":
                        activeServer.stop();
                        return;

                    case "close":
                        try {
                            //Server.serverIsRunning = false;
                            ConnectionListener.serverSocket.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return;

                    case "start":
                        activeServer.start();
                        return;

                }

            }
        }

    /** Thread t = new Thread(new Runnable() {
        public void run() {
            System.out.println("Run, Forrest, run!");
        }
    });
     */


  //  }
