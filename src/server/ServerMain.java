package server;

import java.util.Scanner;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println(ServerMain.class.getClassLoader().getResource("/logback.xml"));

        ConnectionListener server = new ConnectionListener();
        server.start();
        Logger.logServer("Server gestartet");

        ServerGUI serverGUI = ServerGUI.getInstance();
        ServerGUI.server = server;
        serverGUI.init();

        try {
            server.join();
        } catch (InterruptedException ignored) {
            server.interrupt();
        }

        Logger.logServer("Server beendet");
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
                case "test" -> {
                    for (String group : Database.getInstance().getGroupsForUser("ask")) {
                        System.out.println("User is in group: " + group);
                    }
                    System.out.println("vollständig ausgegeben");
                }
                default -> System.out.printf("%s?\n", eingabe);
            }
        }
    }
}
