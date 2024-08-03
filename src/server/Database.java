package server;

import org.sqlite.SQLiteDataSource;
import shared.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * benutzt Nutzername als unique identifier
 * benutzt Raumname als unique identifier → kann zu Bugs führen, da der Name änderbar ist
 */
class Database implements ValidityChecker {
    private final PwAuthenticator passwordAuthenticator = new PwAuthenticator();
    private final SQLiteDataSource dataSource;
    private final Semaphore semaphoreSQLite;


    // groupAndThread: Speichert ClientHandler nach den Gruppen ab, in denen der dazugehörige User ist
    final Map<String, HashSet<ClientHandler>> groupAndThread = Collections.synchronizedMap(new HashMap<>());
    final Set<ClientHandler> allThreads = Collections.synchronizedSet(new HashSet<>());

    final Map<String, ArrayList<Message>> messages = Collections.synchronizedMap(new HashMap<>());
    Map<String, Set<String>> privateGroups = Collections.synchronizedMap(new HashMap<>());


    private Database() {
        this.semaphoreSQLite = new Semaphore(1);
        this.dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:userAndGroup.db");
        createDatabaseIfNotExists();
        createPublicGroup("global");
    }

    public static Database getInstance() {
        return NestedSingletonHelper.databaseSingleton;
    }



    private static class NestedSingletonHelper {
        public static Database databaseSingleton = new Database();
    }



    public void addThreadToAllThreads(ClientHandler clientHandler) {
        allThreads.add(clientHandler);
        addThreadToEveryGroup(clientHandler);
    }

    private void addThreadToEveryGroup(ClientHandler clientHandler) {
        String user = clientHandler.getAngemeldeterNutzer();
        getGroupsForUser(user).forEach(group -> addThreadToGroup(clientHandler, group));
    }




    // alle SQL Methoden sind von ChatGPT
    private void createDatabaseIfNotExists() {
        try (Connection connection = DriverManager.getConnection(dataSource.getUrl());
             Statement statement = connection.createStatement()) {
            // Create Server table
            String createServerTableQuery = "CREATE TABLE IF NOT EXISTS server (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "server_name TEXT UNIQUE);";
            statement.executeUpdate(createServerTableQuery);

            // Insert a default server name or check if an entry exists and insert if not
            String insertDefaultServerQuery = "INSERT OR IGNORE INTO server (server_name) VALUES (?)";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertDefaultServerQuery)) {
                insertStatement.setString(1, "DefaultServerName"); // Set your default server name here
                insertStatement.executeUpdate();
            }

            // Create users table
            String createUserTableQuery = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE," +
                    "nickname TEXT," +
                    "password TEXT," +
                    "is_banned INTEGER DEFAULT 0);"; // Adding IS_BANNED attribute with default value 0 (false)
            statement.executeUpdate(createUserTableQuery);

            // Create groups table
            String createGroupTableQuery = "CREATE TABLE IF NOT EXISTS groups (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "group_name TEXT UNIQUE," +
                    "is_private INTEGER DEFAULT 0);";
            statement.executeUpdate(createGroupTableQuery);

            // Create user_groups table for the relationship
            String createUserGroupsTableQuery = "CREATE TABLE IF NOT EXISTS user_groups (" +
                    "user_id INTEGER," +
                    "group_id INTEGER," +
                    "PRIMARY KEY (user_id, group_id)," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE);";
            statement.executeUpdate(createUserGroupsTableQuery);
        } catch (SQLException e) {
            e.printStackTrace(); // Handle the exception according to your needs
        }
    }



    public List<String> getUsersInGroup(String group) {
        List<String> usersInGroup = new ArrayList<>();

        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                String query = "SELECT users.username FROM users " +
                        "JOIN user_groups ON users.id = user_groups.user_id " +
                        "JOIN groups ON user_groups.group_id = groups.id " +
                        "WHERE groups.group_name = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, group);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            String username = resultSet.getString("username");
                            usersInGroup.add(username);
                        }
                    }
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }
        return usersInGroup;
    }

    public List<String> getGroupsForUser(String username) {
        List<String> userGroups = new ArrayList<>();
        try {
            semaphoreSQLite.acquire();
        } catch (InterruptedException e) {
            return null;
        }
        try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
            String query = "SELECT groups.group_name FROM users " +
                    "JOIN user_groups ON users.id = user_groups.user_id " +
                    "JOIN groups ON user_groups.group_id = groups.id " +
                    "WHERE users.username = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String groupName = resultSet.getString("group_name");
                        userGroups.add(groupName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle the exception according to your needs
        } finally {
            semaphoreSQLite.release();
        }


        userGroups.addAll(getAllPrivateGroupsForUser(username));

        return userGroups;
    }

    private List<String> getAllPrivateGroupsForUser(String username) {
        var list = new ArrayList<String>();
            for (String groupName : privateGroups.keySet()) {
                if (groupName.contains(username)) {
                    list.add(groupName);
                }
            }

        return list;
    }

    public List<String> getAllUsers() {
        List<String> allUsers = new ArrayList<>();

        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                String query = "SELECT username FROM users";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                     ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String username = resultSet.getString("username");
                        allUsers.add(username);
                    }
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally-block to ensure it's released even if an exception occurs
        }

        return allUsers;
    }

    public boolean isBanned(String username) {
        boolean isBanned = false;
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl());
                 PreparedStatement statement = connection.prepareStatement("SELECT IS_BANNED FROM users WHERE username = ?")
            ) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        // Retrieve the IS_BANNED value (assuming it's stored as 0 for false and 1 for true)
                        isBanned = resultSet.getInt("IS_BANNED") == 1;
                    }
                }
            }
            } catch (InterruptedException | SQLException e) {
                e.printStackTrace();
            return true; // im Falle eines Errors lieber nicht zulassen
            } finally {
                semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
            }
            return isBanned;
        }


    void removeThreadFromAll(ClientHandler thread) {
        allThreads.remove(thread);

        synchronized (groupAndThread) {
            for (Map.Entry<String, HashSet<ClientHandler>> entry : groupAndThread.entrySet()) {

                HashSet<ClientHandler> listOfThreads = entry.getValue();

                listOfThreads.removeIf(handler -> handler.equals(thread));
            }
        }
    }

    public HashSet<ClientHandler> getAllThreads() {
        return new HashSet<>(allThreads);
    }

    public boolean isValidPassword(String username, String password) {
        boolean isValid = false;

        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                String query = "SELECT password FROM users WHERE username = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, username);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            String storedPassword = resultSet.getString("password");
                            isValid = validatePassword(password, storedPassword);
                        }
                    }
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally-block to ensure it's released even if an exception occurs
        }

        return isValid;
    }

    private boolean validatePassword(String inputPassword, String storedPassword) {
        return passwordAuthenticator.authenticate(inputPassword.toCharArray(), storedPassword);
    }

    public void setUserPassword(String username, String newPassword) {
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                // Hash the new password before storing it in the database
                String hashedPassword = passwordAuthenticator.hash(newPassword.toCharArray());

                // Update the password in the 'users' table
                String updatePasswordQuery = "UPDATE users SET password = ? WHERE username = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updatePasswordQuery)) {
                    preparedStatement.setString(1, hashedPassword);
                    preparedStatement.setString(2, username);
                    preparedStatement.executeUpdate();
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally-block to ensure it's released even if an exception occurs
        }
    }

    boolean isRegistriert(String username) {
        return getAllUsers().contains(username);
    }

    void addMessage(Message message) {
        String group = message.getStringAtIndex(0);

        // temporary storage
        messages.computeIfAbsent(group, k -> new ArrayList<>()).add(message);

        addMessageToSQLite(message);
    }

    private void addMessageToSQLite(Message message) {
        assert(message instanceof TextMessage || message instanceof PictureMessage || message instanceof PDFMessage);
    }

    private void addThreadToGroup(ClientHandler clientHandler, String group) {
        groupAndThread.computeIfAbsent(group, k -> new HashSet<>()).add(clientHandler);
    }

    private void removeThreadFromGroup(ClientHandler clientHandler, String group) {
        groupAndThread.computeIfPresent(group, (k, v) -> {
            v.remove(clientHandler);
            return v;
        });
    }

    // ChatGPT
    private boolean addUserToGroup(String user, String group) {
        try {
            semaphoreSQLite.acquire();
        } catch (InterruptedException e) {
            return false;
        }

        try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
            // Get user and group IDs
            int userId = getUserIdByUsername(connection, user);
            int groupId = getGroupIdByName(connection, group);

            // Insert into user_groups table
            String query = "INSERT INTO user_groups (user_id, group_id) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, userId);
                preparedStatement.setInt(2, groupId);
                preparedStatement.executeUpdate();
            }
            Logger.logAddUserToGroup(user, group);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            semaphoreSQLite.release();
        }
        return false;
    }

    private boolean removeUserFromPublicGroup(String user, String group) {
        try {
            semaphoreSQLite.acquire();
        } catch (InterruptedException e) {
            return false;
        }

        try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
            // Get user and group IDs
            int userId = getUserIdByUsername(connection, user);
            int groupId = getGroupIdByName(connection, group);

            // Insert into user_groups table
            String query = "DELETE FROM user_groups WHERE user_id = ? AND group_id = ?"; // TODO [SQLITE_ERROR] SQL error or missing database (near "(": syntax error)
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, userId);
                preparedStatement.setInt(2, groupId);
                preparedStatement.executeUpdate();
            }
            Logger.logRemoveUserFromGroup(user, group);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            semaphoreSQLite.release();
        }
        return false;
    }

    private int getUserIdByUsername(Connection connection, String username) throws SQLException {
        String query = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);

            try (var resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }
        throw new SQLException("User not found: " + username);
    }

    private int getGroupIdByName(Connection connection, String groupName) throws SQLException {
        String query = "SELECT id FROM groups WHERE group_name = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, groupName);

            try (var resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }
        throw new SQLException("Group not found: " + groupName);
    }

    public void addUser(String username, String password) {
        addNewUser(username);
        setUserPassword(username, password);
    }

    private void addNewUser(String username) {
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                // Check if the user already exists
                if (userExists(connection, username)) {
                    System.out.println("User already exists: " + username);
                    return;
                }

                // Insert into users table
                String query = "INSERT INTO users (username) VALUES (?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, username);
                    preparedStatement.executeUpdate();
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
        } finally {
            semaphoreSQLite.release();
        }
    }

    private boolean userExists(Connection connection, String username) throws SQLException {
        String query = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);

            try (var resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<String> getUsernamesInGroup(String group) {
        List<String> usernamesInGroup = new ArrayList<>();

        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                // Retrieve usernames in the specified group
                String query = "SELECT users.username FROM users " +
                        "JOIN user_groups ON users.id = user_groups.user_id " +
                        "JOIN groups ON user_groups.group_id = groups.id " +
                        "WHERE groups.group_name = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, group);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            String username = resultSet.getString("username");
                            usernamesInGroup.add(username);
                        }
                    }
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }

        return usernamesInGroup;
    }

    public ArrayList<String> getNicknamesAsList() {
        ArrayList<String> nicknamesAsList = new ArrayList<>();
        HashMap<String, String> nicknamesAsMap = getNicknamesAsMap();

        if (nicknamesAsMap != null) {
            nicknamesAsMap.forEach((username, nickname) -> {
                nicknamesAsList.add(username);
                nicknamesAsList.add(nickname);
            });
        }
        return nicknamesAsList;
    }


    public HashMap<String, String> getNicknamesAsMap() {
        HashMap<String, String> usersWithNicknames = new HashMap<>();

        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                String query = "SELECT username, nickname FROM users";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                     ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet != null) {
                        while (resultSet.next()) {
                            String username = resultSet.getString("username");
                            String nickname = resultSet.getString("nickname");
                            if (nickname!=null){
                                usersWithNicknames.put(username, nickname);
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }
        return usersWithNicknames;
    }


    boolean addUserAndThreadToGroup(String user, String group) {

        // Der User wird der Gruppe in SQLite hinzugefügt
        if (addUserToGroup(user, group)) {
            // Wenn das erfolgreich war, wird der Thread des Users der Gruppe hinzugefügt
            getAllThreads().stream()
                    .filter(clientHandler -> clientHandler.getAngemeldeterNutzer().equals(user))
                    .forEach((clientHandler -> addThreadToGroup(clientHandler, group)));
            return true;
        }
        return false;
    }

    void addUserAndThreadToGroup(ClientHandler thread, String user, String group) {
        addThreadToGroup(thread, group);
        addUserToGroup(user, group);
    }

    public void removeUserAndThreadFromPublicGroup(ClientHandler clientHandler, String user, String group) {
        removeThreadFromGroup(clientHandler, group);
        removeUserFromPublicGroup(user, group);
    }


    HashSet<ClientHandler> getThreadsOfGroup(String group) {
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

    public boolean currentUserIsInGroup(String username, String group) {
        try {
            semaphoreSQLite.acquire();
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                String query = "SELECT 1 FROM users " +
                        "JOIN user_groups ON users.id = user_groups.user_id " +
                        "JOIN groups ON user_groups.group_id = groups.id " +
                        "WHERE users.username = ? AND groups.group_name = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, username);
                    preparedStatement.setString(2, group);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next();
                    }
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            semaphoreSQLite.release();
        }
    }

    public boolean setNicknameForUser(String nickname, String username) {
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                // Check if the user exists
                if (!userExists(connection, username)) {
                    System.out.println("User not found: " + username);
                    return false;
                }

                // Update the nickname
                String updateNicknameQuery = "UPDATE users SET nickname = ? WHERE username = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateNicknameQuery)) {
                    preparedStatement.setString(1, nickname);
                    preparedStatement.setString(2, username);

                    preparedStatement.executeUpdate();
                    return true;
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }
    }

    List<String> getAllPublicGroups() {
        List<String> allGroups = new ArrayList<>();

        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
                String query = "SELECT group_name FROM groups";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                     ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String groupName = resultSet.getString("group_name");
                        allGroups.add(groupName);
                    }
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }

        return allGroups;
    }

    public String getServerName() {
        String serverName = null;
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl());
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT server_name FROM server WHERE id = 1")) {

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        serverName = resultSet.getString("server_name");
                    }
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle the exception according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }
        return serverName;
    }

    public void setServerName(String newName) {
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl());
                 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE server SET server_name = ? WHERE id = 1")) {

                // Set the parameter for the prepared statement
                preparedStatement.setString(1, newName);

                // Execute the update
                int rowsAffected = preparedStatement.executeUpdate();

                // Check if the update was successful
                if (rowsAffected > 0) {
                    System.out.println("Server name updated successfully to '" + newName + "'.");
                } else {
                    System.out.println("Failed to update server name.");
                }

            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle the exception according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }
    }

    public void deleteGroup(String groupName) {
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl());
                 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM groups WHERE group_name = ?")) {

                // Set the parameter for the prepared statement
                preparedStatement.setString(1, groupName);

                // Execute the update
                int rowsAffected = preparedStatement.executeUpdate();

                // Check if the deletion was successful
                if (rowsAffected > 0) {
                    System.out.println("Group '" + groupName + "' deleted successfully.");
                } else {
                    System.out.println("Group '" + groupName + "' not found or deletion failed.");
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }
    }

    public void changeNameOfGroup(String group, String newName) {
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl());
                 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE groups SET group_name = ? WHERE group_name = ?")) {

                // Set the parameters for the prepared statement
                preparedStatement.setString(1, newName);
                preparedStatement.setString(2, group);

                // Execute the update
                int rowsAffected = preparedStatement.executeUpdate();

                // Check if the update was successful
                if (rowsAffected > 0) {
                    System.out.println("Group name changed from '" + group + "' to '" + newName + "' successfully.");
                } else {
                    System.out.println("Group '" + group + "' not found or update failed.");
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally block to ensure it's released even if an exception occurs
        }
    }

    public void createPublicGroup(String groupName) {
        if (!checkValidityOfPublicGroup(groupName)){return;}
        try {
            semaphoreSQLite.acquire(); // Acquire the permit before accessing the shared resource
            try (Connection connection = DriverManager.getConnection(dataSource.getUrl());
                 PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO groups (group_name) VALUES (?)")) {

                // Set the parameter for the prepared statement
                preparedStatement.setString(1, groupName);

                // Execute the insert
                int rowsAffected = preparedStatement.executeUpdate();

                // Check if the insertion was successful
                if (rowsAffected > 0) {
                    System.out.println("Group '" + groupName + "' created successfully.");
                } else {
                    System.out.println("Group creation failed.");
                }
            }
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace(); // Handle exceptions according to your needs
        } finally {
            semaphoreSQLite.release(); // Release the permit in a finally-block to ensure it's released even if an exception occurs
        }
    }




    public void addToPrivateGroup(ClientHandler clientHandler, String groupName, String user) {
        addThreadToGroup(clientHandler, groupName);
        privateGroups.computeIfAbsent(groupName, k -> new HashSet<>()).add(user);
    }

    public boolean isPrivateGroup(String groupName) {
        return privateGroups.containsKey(groupName);
    }

    public boolean privateGroupContainsUser(String group, String user) {
        return privateGroups.get(group).contains(user);
    }

    public void removePrivateGroup(String group) {
        privateGroups.remove(group);
        groupAndThread.remove(group);
        System.out.println(group+" deleted");
    }
}