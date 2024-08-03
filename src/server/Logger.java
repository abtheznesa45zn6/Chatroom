package server;

import org.slf4j.LoggerFactory;
import shared.Message;
import java.net.Socket;

public class Logger {


    private static final org.slf4j.Logger LOG_1 = LoggerFactory.getLogger("Kommunikation");
    private static final org.slf4j.Logger LOG_2 = LoggerFactory.getLogger("Verbindung");
    private static final org.slf4j.Logger LOG_3 = LoggerFactory.getLogger("Verwaltung");

    public static void log(Message message) {
        Logger.LOG_1.info(message.toString());
    }

    public static void logVerbindungsaufbau(Socket socket) {
        logVerbindung("Verbindung mit Client wird aufgebaut. Socket InetAddress: "+socket.getInetAddress());
    }

    public static void logVerbindungstrennung(Socket socket, String angemeldeterNutzer) {
        logVerbindung("Verbindung mit Client wurde unterbrochen. Socket InetAddress: "+socket.getInetAddress()+" \n Zuletzt angemeldeter Nutzername: "+angemeldeterNutzer);
    }

    private static void logVerbindung(String s) {
        Logger.LOG_2.info(s);
    }

    public static void logVerwaltung(String s) {
        LOG_3.info(s);
    }

    public static void logServer(String s) {
        LOG_3.info(s);
    }

    public static void logAddUserToGroup(String user, String group) {
        LOG_3.info("User {} wurde zum Raum {} hinzugefügt.", user, group);
    }
    public static void logRemoveUserFromGroup(String user, String group) {
        LOG_3.info("User {} wurde von Raum {} entfernt.", user, group);
    }
}
