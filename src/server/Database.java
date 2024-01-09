package server;

import shared.*;

import java.util.*;

class Database {

    public static Database getInstance() {
        return NestedSingletonHelper.databaseSingleton;
    }

    private static class NestedSingletonHelper{
        public static Database databaseSingleton = new Database();
    }



    // speichert Benutzername und Passwort
    final Map<String, String> userAndPassword = Collections.synchronizedMap(new HashMap<String, String>());

    // groupAndThread: Speichert ClientHandler nach den Gruppen ab, in denen der dazugehörige User ist
    final Map<String, HashSet<ClientHandler>> groupAndThread = Collections.synchronizedMap(new HashMap<String, HashSet<ClientHandler>>());

    final Map<String, ArrayList<Message>> messages = Collections.synchronizedMap(new HashMap<String, ArrayList<Message>>());




    void removeThreadFromAllGroups(ClientHandler clientHandler) {
        synchronized (groupAndThread) {
            for (Map.Entry<String, HashSet<ClientHandler>> entry : groupAndThread.entrySet()) {

                HashSet<ClientHandler> listOfThreads = entry.getValue();

                listOfThreads.removeIf(handler -> handler.equals(clientHandler));
            }
        }
    }

    // Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
    String getPasswordForUser(String username) {
        return userAndPassword.get(username);
    }

    void setPasswordForUser(String password, String username) {
        userAndPassword.computeIfPresent(username, (k, v) -> password);
    }

    void setUserAndPassword(String username, String password) {
        userAndPassword.put(username, password);
    }

    boolean isRegistriert(String username) {
        return userAndPassword.containsKey(username);
    }

    Set<String> getUserSet() {
        return userAndPassword.keySet();
    }

    void addMessage (Message message) {
        String group = message.getStringAtIndex(0);
        messages.computeIfAbsent(group, k -> new ArrayList<>()).add(message);
    }

    void addThreadToGroup(ClientHandler clientHandler, String group) {
        groupAndThread.computeIfAbsent(group, k -> new HashSet<>()).add(clientHandler);
    }

    HashSet<ClientHandler> getUsersOfGroup (String group) {
        return groupAndThread.get(group);
    }


    boolean currentThreadIsInGroup(ClientHandler clientHandler, String group) {
        if (groupAndThread.containsKey(group)) {
            return groupAndThread.get(group).contains(clientHandler);
        }
        else {
            return false;
        }
    }



}


// Group
// alle Nachrichten in einer LinkedList
// wenn eine neue Nachricht rein kommt, wird sie vorne gespeichert und hinten eine zu weak reference
// Stream -> neue Nachricht ->  write file -> observer update -> weak link

// Deque
// keep time of last element
// time > threshold -> remove item, read time of next
// bei jeder Anfrage, alle Nachrichten zu bekommen, überprüfen, ob Zeit des letzten LEments eingehalten ist

// LinkedHashSet
// first() last()