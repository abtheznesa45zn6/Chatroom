package server;

import shared.*;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// https://hyperskill.org/learn/step/15639


class ClientHandler extends AbstractClass implements ValidityChecker {

    public ClientHandler(Socket socket) {
        super(socket);
    }
    private Database database = Database.getInstance();

    @Override
    protected void dingeTun() {
      listenAndExecute();
    }


    protected void ausführenVon(Message message) {
        if (message == null) {return;}

        switch (message.getAktion()) {
            case REGISTRIEREN -> registrieren(message);
            case ANMELDEN -> anmelden(message);
            case TEXT_MESSAGE -> receiveTextMessage(message);
            case GET_MESSAGES_FROM -> getMessagesFrom(message);
            case GET_USER_LIST -> sendUserList(message);
            case SET_PASSWORD -> changePassword(message);
            default -> throw new IllegalStateException("Wrong enum: " + message.getAktion());
        };
    }

    private void registrieren(Message message) {
        System.out.println("reg");
        String user = message.getStringAtIndex(0);
        String password = message.getStringAtIndex(1);

        if (checkValidityOfName(user)) {
            sendMessage(ServerBefehl.FEEDBACK, "Name nicht erlaubt");
        }
        if (database.isRegistriert(user)) {
            sendMessage(ServerBefehl.FEEDBACK, "Name bereits vorhanden");
        } else {
            database.setUserAndPassword(user, password);
            sendMessage(ServerBefehl.FEEDBACK, "Registration erfolgreich");
            System.out.println("client " + user + " registriert.");
        }
    }

    private void anmelden(Message message) {
        String user = message.getStringAtIndex(0);
        String password = message.getStringAtIndex(1);

        if (database.isRegistriert(user)) {
            if (database.getPasswordForUser(user).equals(password)) {

                if (angemeldet = true) {
                    abmelden();
                }

                // Der neue User/Thread wird der default Gruppe "global" hinzugefügt
                database.addThreadToGroup(this, "global");
                database.addThreadToGroup(this, "Off Topic");
                database.addThreadToGroup(this, "Testgruppe");

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

        if (database.currentThreadIsInGroup(this, group)) {
            Message messageToSend = new Message(ServerBefehl.TEXT_MESSAGE, new String[] {group, angemeldeterNutzer, text});
            database.addMessage(messageToSend);
            sendTextMessageToAllClientsInGroup(messageToSend);
        }
        else {
            sendMessage(ServerBefehl.FEEDBACK, angemeldeterNutzer + " nicht in Gruppe " + group);
        }
    }

    private void sendTextMessageToAllClientsInGroup(Message message) {
        String group = message.getStringAtIndex(0);

        HashSet<ClientHandler> users = database.getUsersOfGroup(group);
        for (ClientHandler serverThread : users) {
            serverThread.sendTextMessageToClient(message);
        }
    }

    private void sendTextMessageToClient (Message message) {
        sendMessage(message);
    }


    private void getMessagesFrom(Message message) {
        String group = message.getStringAtIndex(0);
        ArrayList<Message> messages = database.messages.get(group);

        write(messages.size());
        for (Message msgObject : messages) {
            sendMessage(msgObject);
        }
    }

    private void sendUserList(Message message) {
        write(database.getUserSet().size()); // Write the size of the keyset
        for (String key : database.getUserSet()) {
            write(key);
        }
    }

    private void changePassword(Message message) {
        String user = message.getStringAtIndex(0);
        String password = message.getStringAtIndex(1);
        String neuesPasswort = readText();
        String altesPasswort = database.getPasswordForUser(user);

        if (altesPasswort == null) {
            sendMessage(ServerBefehl.FEEDBACK, "Benutzername nicht vorhanden");
        }

        if(altesPasswort.equals(password))
        {
            database.setPasswordForUser(neuesPasswort, user);
            sendMessage(ServerBefehl.FEEDBACK, "Passwort wurde geändert");
        }
        else {
            sendMessage(ServerBefehl.FEEDBACK, "Passwort ist falsch");
        }
    }
}


