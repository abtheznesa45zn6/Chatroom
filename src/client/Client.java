package client;

import shared.*;

import java.net.*;
import java.io.*;
import java.util.*;

class Client extends AbstractClass implements ValidityChecker {
    private Set<String> userList = new TreeSet<>();
    private String angemeldetesPasswort;
    private final MainFrame clientGUI;
    private final static Map<String, ArrayList<Message>> messages = new HashMap<String, ArrayList<Message>>();

    Client(Socket socket) {
        super(socket);
        // GUI starten und anzeigen
        clientGUI = new MainFrame(this);
    }


    public static void main(String[] args) {

        final int port = 3141;
        final String host = "localhost";

        try (Socket socket = new Socket(host, port)) {
            Client client = new Client(socket);
            client.start();
            client.join();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {

        } finally {
            System.out.println("Client main beendet");
        }
    }


    @Override
    public void dingeTun() {

        // Test Block
        try {
            out.writeObject(new Message(ServerBefehl.REGISTRIEREN, new String[]{"ask", "asdfa"}));
            out.writeObject(new Message(ServerBefehl.REGISTRIEREN, new String[]{"ask", "asd23424fa"}));
            out.writeObject(new Message(ServerBefehl.ANMELDEN, new String[]{"ask234", "asd23424a"}));
            out.writeObject(new Message(ServerBefehl.ANMELDEN, new String[]{"ask", "asdf2342423423523456a"}));
            out.writeObject(new Message(ServerBefehl.ANMELDEN, new String[]{"ask", "asdfa"}));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        listenAndExecute();

        System.out.println("Client beendet");
    }


    protected void ausführenVon(Message message) {
        if (message == null) {return;}

        switch (message.getAktion()) {
            case GET_MESSAGES_FROM -> getMessagesFrom(message);
            case GET_USER_LIST -> getUserList();
            case SET_PASSWORD -> changePassword(message);
            case ANMELDEN_ERFOLGREICH -> nutzerAnmelden(message);
            case FEEDBACK -> feedback(message);
            case TEXT_MESSAGE -> receiveTextMessage(message);
            default -> throw new IllegalStateException("Wrong enum: " + message.getAktion());
        };
    }



    //getNewMessagesFromGroupSinceTime(group, time)

    void registrieren(String user, String password) {
        sendMessage(ServerBefehl.REGISTRIEREN, user, password);
    }

    void anmelden(String user, String password) {
        sendMessage(ServerBefehl.ANMELDEN, user, password);
    }

    void setNicknameTo(String nickname) {
        sendMessage(ServerBefehl.SET_NICKNAME, angemeldeterNutzer, nickname);
    }



    void sendTextMessage(String group, String text) {
        sendMessage(ServerBefehl.TEXT_MESSAGE, group, angemeldeterNutzer, text);
    }

    void getMessagesFrom(Message message) {
        String group = message.getStringAtIndex(0);
        write(ServerBefehl.GET_MESSAGES_FROM, group);
        ArrayList<TextMessage> textMessages = new ArrayList<>();

        int sizeOfList = readInt();
        for (int i = 0; i < sizeOfList; i++) {
            TextMessage msgObject = readTextMessage();
            textMessages.add(msgObject);
        }
        // update the messages
    }

    void getUserList() {
        write(ServerBefehl.GET_USER_LIST, angemeldeterNutzer, angemeldetesPasswort);

        int keySetSize = readInt();
        for (int i = 0; i < keySetSize; i++) {
            String key = readText(); // Read each key as a UTF-encoded string
            userList.add(key);
        }
        // update the user list
    }

    protected TextMessage readTextMessage()  {
        try {
            return (TextMessage) in.readObject();

        } catch (IOException e) {
            System.out.println("Client Message readObject() IOException");
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            System.out.println("Client Message readObject() ClassNotFoundException");
            throw new RuntimeException(e);
        }
    }

    void changePassword(Message message) {
        String neuesPasswort = message.getStringAtIndex(0);
        sendMessage(ServerBefehl.SET_PASSWORD, angemeldeterNutzer, angemeldetesPasswort,neuesPasswort);
    }

    private void nutzerAnmelden(Message message) {
        String user = message.getStringAtIndex(0);
        angemeldet = true;
        angemeldeterNutzer = user;
        clientGUI.setNickname(user);
    }

    private void feedback(Message message) {
        String feedback = message.getStringAtIndex(0);
        clientGUI.addFeedback(feedback);
    }

    private void receiveTextMessage(Message message) {
        showMessageInGUI(message);
        saveMessageToCache(message);
    }

    private void saveMessageToCache(Message message) {
        String group = message.getStringAtIndex(0);
        List<Message> groupMessages = messages.computeIfAbsent(group, k -> new ArrayList<>());
        groupMessages.add(message);
    }

    private void showMessageInGUI(Message message) {
        clientGUI.showMessageInGUI(message);
    }

    private void getAllMessagesFromRoom () {

    }


    private void write (ServerBefehl aktion, String... strings) {
        try {
            out.writeObject(aktion);
            for (String string : strings) {
                out.writeObject(string);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    private void write (ServerBefehl aktion, String info1, String info2) {
        try {
            out.writeObject(new SystemMessage(aktion, info1, info2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(ServerBefehl aktion, String info1) {
        try {
            out.writeObject(new SystemMessage(aktion, info1));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
     */

    private void trenneVerbindung() {
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
