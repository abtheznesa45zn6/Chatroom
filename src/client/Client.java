package client;

import shared.*;

import java.net.*;
import java.io.*;
import java.util.*;

class Client extends AbstractClass implements ValidityChecker {
    private Set<String> userList = new TreeSet<>();
    private String angemeldetesPasswort;
    private final ClientGUI clientGUI;
    private final static Map<String, ArrayList<Message>> messages = new HashMap<String, ArrayList<Message>>();

    Client(Socket socket) {
        super(socket);
        // GUI starten und anzeigen
        clientGUI = new ClientGUI(this);
        clientGUI.start();
    }
jk

    public static void main(String[] args) {

        final int port = 3141;
        final String host = "127.0.0.1";

        try (Socket socket = new Socket(host, port)) {
            Client client = new Client(socket);
            client.start();
            client.join();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException ignored) {

        } finally {
            System.out.println("Client main beendet");
        }
    }


    @Override
    public void dingeTun() {

        listenAndExecute();
        clientGUI.abmelden();
        clientGUI.dispose();

        System.out.println("Client beendet");
    }


    protected void ausführenVon(Message message) {
        if (message == null) {return;}

        switch (message.getAktion()) {
            case ANMELDEN_ERFOLGREICH -> nutzerAnmelden(message);
            case FEEDBACK -> feedback(message);
            case TEXT_MESSAGE -> receiveTextMessage(message);
            case PDF_MESSAGE -> receivePDFMessage(message);
            case PICTURE_MESSAGE -> receivePictureMessage(message);
            case RECEIVE_PUBLIC_GROUPS -> receivePublicGroups(message);
            case RECEIVE_OWN_GROUPS -> receiveOwnGroups(message);
            case RECEIVE_USER_LIST_OF_GROUP -> receiveUserList(message);
            case RECEIVE_COMPLETE_USER_LIST -> receiveCompleteUserList(message);
            case SET_NICKNAME -> updateNicknames(message);
            case RECEIVE_NICKNAME_LIST -> receiveNicknameList(message);
            case RAUM_ERSTELLEN -> erstelleRaum(message);
            case RAUMNAME_AENDERN -> aendereRaumname(message);
            case RAUM_LOESCHEN -> loescheRaum(message);
            case VERWARNEN -> verwarnen(message);
            case KICKEN -> kicken(message);
            case BANNEN -> bannen(message);
            case SERVERNAME_SETZEN -> setzeServername(message);
            case REMOVE_PRIVATE_CHAT -> removePrivateGroup(message);
            case VERBINDUNG_STARTEN -> verbindungStarten(message);
            default -> throw new IllegalStateException("Wrong enum: " + message.getAktion());
        };
    }



    void registrieren(String user, String password) {
        sendMessage(ServerBefehl.REGISTRIEREN, user, password);
    }

    void anmelden(String user, String password) {
        sendMessage(ServerBefehl.ANMELDEN, user, password);
    }
    void sendAbmeldenMessage() {
        sendMessage(ServerBefehl.ABMELDEN);
    }

    void setNicknameTo(String nickname) {
        sendMessage(ServerBefehl.SET_NICKNAME, angemeldeterNutzer, nickname);
    }



    void sendTextMessage(String group, String text) {
        sendMessage(ServerBefehl.TEXT_MESSAGE, group, angemeldeterNutzer, text);
    }

    void getMessagesFrom(String group) {
        sendMessage(ServerBefehl.GET_MESSAGES_FROM, group);
    }

    void getUserList(String group) {
        sendMessage(ServerBefehl.GET_USER_LIST, group, angemeldeterNutzer);
    }

    void changePassword(String altesPasswort, String neuesPasswort) {
        sendMessage(ServerBefehl.SET_PASSWORD, angemeldeterNutzer, altesPasswort, neuesPasswort);
        // TODO Passwort als char[] senden
    }

    private void nutzerAnmelden(Message message) {
        String user = message.getStringAtIndex(0);
        angemeldet = true;
        angemeldeterNutzer = user;
        clientGUI.setNickname(user);
        sendRequestForGroupList(user);
    }

    private void feedback(Message message) {
        String feedback = message.getStringAtIndex(0);
        clientGUI.addFeedback(feedback);
    }

    private void receiveTextMessage(Message message) {
        if (message instanceof TextMessage textMessage) {
            clientGUI.showMessageInGUI(textMessage);
        } else {
            throw new IllegalStateException();
        }
    }

    private void receivePictureMessage(Message message) {
        if (message instanceof PictureMessage pictureMessage) {
            clientGUI.showMessageInGUI(pictureMessage);
        } else {
            throw new IllegalStateException();
        }
    }

    private void receivePDFMessage(Message message) {
        if (message instanceof PDFMessage pdfMessage) {
            clientGUI.showMessageInGUI(pdfMessage);
        } else {
            throw new IllegalStateException();
        }
    }

    private void receivePublicGroups(Message message) {
        var groups = new ArrayList<String>(Arrays.asList(message.getStringArray()));
        clientGUI.updateGroupMenu(groups);
    }

    private void receiveOwnGroups(Message message) {
        var groups = new ArrayList<String>(Arrays.asList(message.getStringArray()));
        clientGUI.updateGroups(groups);
    }

    private void receiveUserList(Message message) {
        // erstes Element ist die Gruppe, die Elemente danach sind die dazugehörigen User
        var groupAndUsers = new ArrayList<String>(Arrays.asList(message.getStringArray()));
        String group = groupAndUsers.remove(0);
        var users = groupAndUsers;
        clientGUI.updateUsersOfGroup(users, group);
    }

    private void receiveCompleteUserList(Message message) {
        var users = new ArrayList<String>(Arrays.asList(message.getStringArray()));
        clientGUI.updateUserMenu(users);
    }


    private void updateNicknames(Message message) {
        String user = message.getStringAtIndex(0);
        String nickname = message.getStringAtIndex(1);
        // requests all nicknames
        sendMessage(ServerBefehl.GET_ALL_NICKNAMES);
    }


    private void receiveNicknameList(Message message) {
        HashMap<String, String> map = new HashMap<>();

        for (int i = 0; i < message.getStringArray().length; i += 2) {
            String username = message.getStringArray()[i];
            String nickname = message.getStringArray()[i+1];

            map.put(username, nickname);
        }
        clientGUI.updateNicknameList(map);
    }

    private void erstelleRaum(Message message) {
        String groupName = message.getStringAtIndex(0);
        clientGUI.erstelleRaum(groupName);
        sendRequestForGroupList(angemeldeterNutzer); //aktualisiert indirekt die Raumliste
    }

    private void aendereRaumname(Message message) {
        String oldName = message.getStringAtIndex(0);
        String newName = message.getStringAtIndex(1);
        clientGUI.aendereRaumname(oldName, newName);
        sendRequestForGroupList(angemeldeterNutzer); //aktualisiert indirekt die Raumliste
    }

    private void loescheRaum(Message message) {
        String geloeschterRaum = message.getStringAtIndex(0);
        clientGUI.loescheRaum(geloeschterRaum);
        sendRequestForGroupList(angemeldeterNutzer); //aktualisiert indirekt die Raumliste
    }

    private void verwarnen(Message message) {
        String warnedUser = message.getStringAtIndex(0);
        clientGUI.verwarnen(warnedUser);
    }

    private void kicken(Message message) {
        String kickedUser = message.getStringAtIndex(0);
        clientGUI.kicken(kickedUser);
        if (angemeldeterNutzer.equals(kickedUser)) {
            clientGUI.abmelden();
        }
    }

    private void bannen(Message message) {
        String bannedUser = message.getStringAtIndex(0);
        clientGUI.bannen(bannedUser);
        if (angemeldeterNutzer.equals(bannedUser)) {
            clientGUI.abmelden();
        }
    }

    private void setzeServername(Message message) {
        String newName = message.getStringAtIndex(0);
        clientGUI.setServerName(newName);
    }


    private void sendRequestForGroupList(String user) {
        sendMessage(ServerBefehl.GET_GROUPS, user);
    }

    void requestPrivateChat(String group, String empfänger) {
        sendMessage(ServerBefehl.REQUEST_PRIVATE_CHAT, group, empfänger);
    }

    void sendRemovePrivateGroup(String group) {
        sendMessage(ServerBefehl.REMOVE_PRIVATE_CHAT, group);
    }

    private void removePrivateGroup(Message message) {
        String group = message.getStringAtIndex(0);
        clientGUI.removePrivateGroup(group);
    }

    private void verbindungStarten(Message message) {
        String group = message.getStringAtIndex(0);
        clientGUI.verbindungStarten(group);
    }

    public void sendJoinGroup(String group) {
        sendMessage(ServerBefehl.JOIN_GROUP, group);
    }

    public void sendLeaveGroup(String group) {
        sendMessage(ServerBefehl.LEAVE_GROUP, group);
    }
}
