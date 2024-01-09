package server;

import java.util.Scanner;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("Server gestartet");

        ConnectionListener server = new ConnectionListener();
        server.start();

        listenForServerCommands(server);

        System.out.println("Server beendet");
    }

    private static void listenForServerCommands(ConnectionListener server) {
        Scanner keyboard = new Scanner(System.in);
        while (true) {
            String eingabe = keyboard.nextLine().toLowerCase().replace("\s", "");
            switch (eingabe) {
                case "interrupt" -> {
                    server.interrupt();
                    System.out.println("Server interrupted");
                    return;
                }
                default -> System.out.printf("%s?\n", eingabe);
            }
        }
    }
}
