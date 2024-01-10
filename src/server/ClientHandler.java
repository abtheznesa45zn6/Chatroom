package server;

import shared.*;

import java.net.Socket;
import java.util.*;

// https://hyperskill.org/learn/step/15639


class ClientHandler extends AbstractClass implements ValidityChecker {

    public ClientHandler(Socket socket) {
        super(socket);
    }
    private final Database database = Database.getInstance();

    @Override
    protected void dingeTun() {
      listenAndExecute();
      database.removeThreadFromAllGroups(this);
    }


    protected void ausführenVon(Message message) {
        if (message == null) {return;}

        switch (message.getAktion()) {
            case REGISTRIEREN -> registrieren(message);
            case ANMELDEN -> anmelden(message);
            case ABMELDEN -> abmelden();
            case TEXT_MESSAGE -> receiveTextMessage(message);
            case GET_MESSAGES_FROM -> getMessagesFrom(message);
            case SET_PASSWORD -> changePassword(message);
            case GET_GROUPS -> sendGroups(message);
            case SET_NICKNAME -> setNickname(message);
            default -> throw new IllegalStateException("Wrong enum: " + message.getAktion());
        }
    }

    private void registrieren(Message message) {
        String user = message.getStringAtIndex(0);
        String password = message.getStringAtIndex(1);

        if (checkValidityOfName(user)) {
            sendMessage(ServerBefehl.FEEDBACK, "Name nicht erlaubt");
        }
        if (database.isRegistriert(user)) {
            sendMessage(ServerBefehl.FEEDBACK, "Name bereits vorhanden");
        } else {
            database.setUserAndPassword(user, password);
            database.addUser(user);
            sendMessage(ServerBefehl.FEEDBACK, "Registration erfolgreich");
            System.out.println("client " + user + " registriert.");
        }
    }

    private void anmelden(Message message) {
        String user = message.getStringAtIndex(0);
        String password = message.getStringAtIndex(1);

        if (database.isRegistriert(user)) {
            if (database.getPasswordForUser(user).equals(password)) {

                if (angemeldet) {
                    abmelden(this);
                }

                // Der neue User/Thread wird der default Gruppe "global" hinzugefügt
                database.addGroup("global");
                database.addUserAndThreadToGroup(this, user, "global");

                // Test
                database.addUserAndThreadToGroup(this, user,"Off Topic");
                database.addUserAndThreadToGroup(this, user,"Testgruppe");
                database.addGroup("Testgruppe");
                database.addGroup("Off Topic");

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

        if (!this.angemeldeterNutzer.equals(angemeldeterNutzer)) {return;}

        if (database.currentUserIsInGroup(angemeldeterNutzer, group)) {
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
        String altesPasswort = database.getPasswordForUser(user);

        if (altesPasswort == null) {
            sendMessage(ServerBefehl.FEEDBACK, "Benutzername nicht vorhanden");
        }
        else {
            if(altesPasswort.equals(password)) {
                database.setPasswordForUser(neuesPasswort, user);
                sendMessage(ServerBefehl.FEEDBACK, "Passwort wurde geändert");
            }
            else {
                sendMessage(ServerBefehl.FEEDBACK, "Passwort ist falsch");
            }
        }
    }

    private void sendGroups(Message message) {
        String user = message.getStringAtIndex(0);
        List<String> groups = database.getGroupsForUser(user);
        sendMessage(ServerBefehl.RECEIVE_GROUPS, groups.toArray(new String[0]));
    }

    private void setNickname(Message message) {
        String nickname = message.getStringAtIndex(0);
        System.out.println("setting nick to "+nickname);
        //
        //
    }

}


