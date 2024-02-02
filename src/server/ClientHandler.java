package server;

import shared.*;

import java.net.Socket;
import java.util.*;


class ClientHandler extends AbstractClass implements ValidityChecker {

    public ClientHandler(Socket socket) {
        super(socket);
    }
    private final Database database = Database.getInstance();
    private ServerGUI serverGUI = ServerGUI.getInstance();

    @Override
    protected void dingeTun() {
      listenAndExecute();
      database.removeThreadFromAllGroups(this);
    }


    protected void ausführenVon(Message message) {
        if (message == null) {return;}

        Logger.log(message);
        serverGUI.receiveMessage(message);

        switch (message.getAktion()) {
            case REGISTRIEREN -> registrieren(message);
            case ANMELDEN -> anmelden(message);
            case ABMELDEN -> abmelden();
            case TEXT_MESSAGE -> receiveTextMessage(message);
            case PDF_MESSAGE -> receivePDFMessage(message);
            case PICTURE_MESSAGE -> receivePictureMessage(message);
            case GET_MESSAGES_FROM -> getMessagesFrom(message);
            case SET_PASSWORD -> changePassword(message);
            case GET_GROUPS -> sendGroupsOfLoggedInUser();
            case GET_USER_LIST -> sendUserList(message);
            case SET_NICKNAME -> setNickname(message);
            case GET_ALL_NICKNAMES -> sendAllNicknames();
            case REQUEST_PRIVATE_CHAT -> requestPrivateChat(message);
            default -> throw new IllegalStateException("Wrong enum: " + message.getAktion());
        }
    }


    private void registrieren(Message message) {
        String user = message.getStringAtIndex(0);
        String password = message.getStringAtIndex(1);

        if (!checkValidityOfName(user)) {
            sendMessage(ServerBefehl.FEEDBACK, "Name nicht erlaubt");
        }
        if (database.isRegistriert(user)) {
            sendMessage(ServerBefehl.FEEDBACK, "Name bereits vorhanden");
        } else {
            database.addUser(user, password);
            sendMessage(ServerBefehl.FEEDBACK, "Registration erfolgreich");
            Logger.logVerwaltung("Benutzer " + user + "hat sich registriert.");
            sendeAllenAktivenClientsDieNutzerliste();
        }
    }

    private void sendeAllenAktivenClientsDieNutzerliste() {
        var threads = database.getAllThreads();
        for (ClientHandler thread : threads) {
            thread.sendAllUsers();
        }
    }

    private void anmelden(Message message) {
        String user = message.getStringAtIndex(0);
        String password = message.getStringAtIndex(1);

        if (database.isBanned(user)) {
            sendMessage(ServerBefehl.FEEDBACK, "Benutzer ist gebannt");
            return;
        }
        else

        if (database.isRegistriert(user)) {
            if (database.isValidPassword(user, password)) {

                if (angemeldet) {
                    abmelden();
                }

                database.addThread(this);

                // zur default-Gruppe hinzufügen
                database.addUserAndThreadToGroup(this, user, "global");

                sendGroupsOfLoggedInUser();
                sendAllUsers();
                sendAllNicknames();
                sendServername();

                angemeldet = true;
                angemeldeterNutzer = user;
                sendMessage(ServerBefehl.ANMELDEN_ERFOLGREICH, user);
                sendMessage(ServerBefehl.FEEDBACK, "Anmeldung erfolgreich");
                System.out.println("client " + user + " angemeldet.");
            } else {
                sendMessage(ServerBefehl.FEEDBACK, "Falsches Passwort");
            }

        } else {
            sendMessage(ServerBefehl.FEEDBACK, "Name nicht vorhanden");
        }

    }




    private void abmelden() {
        angemeldet = false;
        database.removeThreadFromAllGroups(this);
    }



    private void receiveTextMessage(Message message) {
        String group = message.getStringAtIndex(0);
        String angemeldeterNutzer = message.getStringAtIndex(1);
        String text = message.getStringAtIndex(2);

        //if (!this.angemeldeterNutzer.equals(angemeldeterNutzer)) {return;}

        if (database.currentUserIsInGroup(angemeldeterNutzer, group) || group.contains(privateChatIndicator)) {
            TextMessage messageToSend = new TextMessage(ServerBefehl.TEXT_MESSAGE, new String[] {group, angemeldeterNutzer, text});
            database.addMessage(messageToSend);
            sendMessageToAllClientsInGroup(messageToSend);
        }
        else {
            sendMessage(ServerBefehl.FEEDBACK, angemeldeterNutzer + " nicht in Gruppe " + group);
        }
    }

    private void receivePDFMessage(Message message) {
        receiveDataMessage(message);
    }

    private void receivePictureMessage(Message message) {
        receiveDataMessage(message);
    }

    private void receiveDataMessage(Message message) {
        /*
        String group = message.getStringAtIndex(0);
        String angemeldeterNutzer = message.getStringAtIndex(1);
        String data = message.getStringAtIndex(2);
         */
        database.addMessage(message);
        sendMessageToAllClientsInGroup(message);
    }

    private void sendMessageToAllClientsInGroup(Message message) {
        String group = message.getStringAtIndex(0);

        HashSet<ClientHandler> users = database.getThreadsOfGroup(group);
        if (users != null) {
            for (ClientHandler serverThread : users) {
                serverThread.sendMessageToClient(message);
            }
        }
    }

    private void sendTextMessageToAllClients(Message message) {
        HashSet<ClientHandler> clients = database.getAllThreads();
        for (ClientHandler client : clients) {
            sendMessage(message);
        }
    }

    private void sendMessageToClient(Message message) {
        sendMessage(message);
    }

    private void getMessagesFrom(Message message) {
        String group = message.getStringAtIndex(0);
        ArrayList<Message> messages = database.messages.get(group);

        if (messages != null) {
            for (Message msgObject : messages) {
            sendMessage(msgObject);
            }
        }
    }

    private void changePassword(Message message) {
        String user = message.getStringAtIndex(0);
        String password = message.getStringAtIndex(1);
        String neuesPasswort = message.getStringAtIndex(2);

        if (database.isValidPassword(user, password)) {
            database.setUserPassword(user, neuesPasswort);
            sendMessage(ServerBefehl.FEEDBACK, "Passwort wurde geändert");
        } else {
            sendMessage(ServerBefehl.FEEDBACK, "Passwort ist falsch");
        }
    }

    /**
     * Sendet an den angemeldeten Client alle Gruppen, in denen er sich befindet
     */
    public void sendGroupsOfLoggedInUser() {
        List<String> groups = database.getGroupsForUser(angemeldeterNutzer);
        sendMessage(ServerBefehl.RECEIVE_GROUPS, groups.toArray(new String[0]));
    }


    private void sendUserList(Message message) {
        String group = message.getStringAtIndex(0);
        String user = message.getStringAtIndex(1);
        if (this.angemeldeterNutzer.equals(user)){
            ArrayList<String> users = new ArrayList<>();
            users.add(group); // first element of String[] is the group

            users.addAll(database.getUsernamesInGroup(group));

            sendMessage(ServerBefehl.RECEIVE_USER_LIST_OF_GROUP, users.toArray(new String[0]));
        }
    }

    private void setNickname(Message message) {
        String user = message.getStringAtIndex(0);
        String nickname = message.getStringAtIndex(1);
        if (user.equals(angemeldeterNutzer)) {
            if (database.setNicknameForUser(nickname, user)){
                Message messageToSend = new Message(ServerBefehl.SET_NICKNAME, new String[]{user, nickname});
                sendMessageToClient(messageToSend);
                System.out.printf("Nickname von Nutzer %s wurde zu %s ist geändert.\n", angemeldeterNutzer, nickname);
            }
        }
        System.out.printf("Nickname von %s zu ändern ist gescheitert.\n", angemeldeterNutzer);
    }

    private void sendAllUsers() {
        ArrayList<String> users = new ArrayList<>(database.getAllUsers());

        sendMessage(ServerBefehl.RECEIVE_COMPLETE_USER_LIST, users.toArray(new String[0]));
    }

    private void sendAllNicknames() {
        ArrayList<String> users = new ArrayList<>(database.getNicknamesAsList());

        sendMessage(ServerBefehl.RECEIVE_NICKNAME_LIST, users.toArray(new String[0]));
    }

    private void requestPrivateChat(Message message) {
        String group = message.getStringAtIndex(0);
        String empfänger = message.getStringAtIndex(1);

        if (database.createPrivateGroup(this, group, angemeldeterNutzer, empfänger)) {
            //sendGroupsOfLoggedInUser();
        }
        else {
            //sendMessage(ServerBefehl.FEEDBACK, angemeldeterNutzer+" fragt eine private Verbindung an");
        }

        database.getThreadsOfGroup(group).forEach(ClientHandler::sendGroupsOfLoggedInUser);

    }

    private void sendServername() {
        sendMessage(ServerBefehl.SERVERNAME_SETZEN, database.getServerName());
    }

    public void verbindungTrennenWennUserEquals(String user) {
        if (angemeldeterNutzer.equals(user)) {
            this.beenden(this);
        }
    }
}


