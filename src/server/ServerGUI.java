package server;

import shared.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ServerGUI extends JFrame implements ValidityChecker {

    public static ServerGUI getInstance() {
        return ServerGUI.NestedSingletonHelper.serverGUISingleton;
    }

    private static class NestedSingletonHelper {
        public static ServerGUI serverGUISingleton = new ServerGUI();
    }

    static ConnectionListener server;
    private Pane currentPane;
    private String currentGroup;
    private String currentUser;
    private Aufgabe currentAufgabe;

    private enum Pane {
        BENUTZER,
        RÄUME,
        SERVER
    }

    private enum Aufgabe {
        RAUM_ERSTELLEN("Raum erstellen"),
        RAUMNAME_AENDERN("Raumname ändern"),
        RAUM_LOESCHEN("Raum löschen"),
        VERWARNEN("Verwarnen"),
        KICKEN("Kicken"),
        BANNEN("Bannen"),
        SERVERNAME_SETZEN("Servername setzen");

        private final String displayName;

        Aufgabe(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }


    private String selectedItemInTabbedPanel;
    private final Database database = Database.getInstance();

    private ServerGUI() {
    }

    public static void main(String[] args) {
        ServerGUI serverGUI = new ServerGUI();
        serverGUI.init();
    }
    
    private JPanel mainPanel;
    private JPanel ebene1Links;
    private JPanel ebene1Rechts;
    private JTextArea serverlogAusgabe;
    private JPanel ebene2RechtsOben;
    private JPanel ebene2RechtsUnten;
    private JTabbedPane tabbedPane;
    private JTextField aufgabenEingabe;
    private JComboBox<String> aufgabenComboBox;
    private JButton okButton;
    private JList<String> benutzerList;
    private JList<String> räumeList;
    private JLabel selectedItem;
    private JLabel feedbackLabel;
    private JList<String> serverListAlleBenutzer;
    private JList<String> serverList;
    private JButton zumRaumHinzufügenButton;
    private JLabel selectedUserLabel;
    private JLabel selectedGroupLabel;

    private DefaultComboBoxModel<String> räumeComboBoxModel;
    private DefaultComboBoxModel<String> benutzerComboBoxModel;
    private DefaultComboBoxModel<String> serverComboBoxModel;

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    int w = (d.width - getSize().width) / 2;
    int h = (d.height - getSize().height) / 2;
    int k = 20;


    void init() {
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(w, h);
        setLocationRelativeTo(null);
        setResizable(false);

        // Window close operation
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        server.interrupt();
                        dispose();
                    }
                });

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        ebene1Links.setPreferredSize(new Dimension(w/2-10, h));
        ebene1Rechts.setPreferredSize(new Dimension(w/2-10, h));
        initLinks();

        ebene2RechtsOben.setPreferredSize(new Dimension(w/2-k, h/2));
        ebene2RechtsUnten.setPreferredSize(new Dimension(w/2-k, h/3));
        initRechts();
    }

    private void initRechts() {
        initRäumeUndBenutzerTabbedPane();
    }

    private void initLinks() {

    }

    private void initRäumeUndBenutzerTabbedPane() {

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Pane newPane = getSelectedPane(tabbedPane);

                if (currentPane != newPane) {
                    switch (newPane) {
                        case BENUTZER -> updateBenutzerPane();
                        case RÄUME -> updateRäumePane();
                        case SERVER -> updateServerPane();
                        default -> throw new IllegalStateException();
                    };
                }
                currentPane = newPane;
            }
        });

        // ComboBoxes, one per tab
        räumeComboBoxModel = new DefaultComboBoxModel<>();
        Aufgabe[] räumeAufgaben = { Aufgabe.RAUM_ERSTELLEN, Aufgabe.RAUMNAME_AENDERN, Aufgabe.RAUM_LOESCHEN};
        for (Aufgabe aufgabe : räumeAufgaben) {
            räumeComboBoxModel.addElement(aufgabe.toString());
        }

        benutzerComboBoxModel = new DefaultComboBoxModel<>();
        Aufgabe[] benutzerAufgaben = { Aufgabe.VERWARNEN, Aufgabe.KICKEN, Aufgabe.BANNEN };
        for (Aufgabe aufgabe : benutzerAufgaben) {
            benutzerComboBoxModel.addElement(aufgabe.toString());
        }

        serverComboBoxModel = new DefaultComboBoxModel<>();
        Aufgabe[] serverAufgaben = { Aufgabe.SERVERNAME_SETZEN };
        for (Aufgabe aufgabe : serverAufgaben) {
            serverComboBoxModel.addElement(aufgabe.toString());
        }

        aufgabenComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Aufgabe aufgabe = stringToAufgabe(aufgabenComboBox.getSelectedItem());
                currentAufgabe = aufgabe;
                System.out.println("Selected Value: " + aufgabe);
            }

            private Aufgabe stringToAufgabe(Object selectedItem) {
                for (Aufgabe a : Aufgabe.values()) {
                    if (selectedItem.toString().equals(a.toString()))
                        return a;
                }
                throw new IllegalStateException();
            }
        });

        // list listeners
        räumeList.addListSelectionListener(setAufgabenComboBoxListener(räumeList, räumeComboBoxModel));
        benutzerList.addListSelectionListener(setAufgabenComboBoxListener(benutzerList, benutzerComboBoxModel));
        serverListAlleBenutzer.addListSelectionListener(setAufgabenComboBoxListener(serverListAlleBenutzer, benutzerComboBoxModel));
        serverList.addListSelectionListener(setAufgabenComboBoxListener(serverList, serverComboBoxModel));

        benutzerList.addListSelectionListener(setCurrentUserListener(benutzerList));
        serverListAlleBenutzer.addListSelectionListener(setCurrentUserListener(serverListAlleBenutzer));

        räumeList.addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {

                Object nullableGroup = räumeList.getSelectedValue();
                if (nullableGroup == null){
                    return;
                }
                else {
                    currentGroup = nullableGroup.toString();
                    updateBenutzerList(currentGroup);
                    selectedGroupLabel.setText(currentGroup);
                }
            }
        });

        // Button Listeners
        okButton.addActionListener(buttonListener -> {
            String userOrGroup = null;

            if (currentAufgabe == Aufgabe.RAUM_ERSTELLEN || currentAufgabe == Aufgabe.RAUM_LOESCHEN || currentAufgabe == Aufgabe.SERVERNAME_SETZEN){
                // userOrGroup ist nicht nötig, deswegen muss hier nicht returnt werden
            } else if (selectedItemInTabbedPanel == null) {return;}
            else {
                userOrGroup = selectedItemInTabbedPanel.split(disallowedSpaceCharacter)[0];
            }

            String eingabe = aufgabenEingabe.getText();
            aufgabenEingabe.setText("");

            switch (currentAufgabe) {
                case RAUM_ERSTELLEN -> raumErstellen(eingabe);
                case RAUMNAME_AENDERN -> raumnameAendern(userOrGroup, eingabe);
                case RAUM_LOESCHEN -> raumLoeschen(eingabe);
                case VERWARNEN -> verwarnen(userOrGroup);
                case KICKEN -> kicken(userOrGroup);
                case BANNEN -> bannen(userOrGroup);
                case SERVERNAME_SETZEN -> servernameSetzen(eingabe);
                default -> throw new IllegalStateException();
            }
        });

        zumRaumHinzufügenButton.addActionListener(buttonListener -> {
            if (currentUser == null || currentGroup == null) {return;}

            if (database.addUserAndThreadToGroup(currentUser, currentGroup)) {

                updateGroupsInClientsOfGroup(currentGroup);
                feedbackLabel.setText("User "+currentUser+" wurde zum Raum "+currentGroup+" hinzugefügt.");
                Logger.logVerwaltung("User "+currentUser+" wurde zum Raum "+currentGroup+" hinzugefügt.");
            }
            else {
                // TODO this is activated even if the user was added successfully
                feedbackLabel.setText("Das Hinzufügen ist fehlgeschlagen.");
            }
        });

        // initiale Einstellung
        updateBenutzerPane();
        aufgabenComboBox.setModel(benutzerComboBoxModel);
        currentAufgabe = Aufgabe.SERVERNAME_SETZEN;
    }

    private void updateGroupsInClientsOfGroup(String group) {
        database.getThreadsOfGroup(group).forEach(ClientHandler::sendGroupsOfLoggedInUser);
    }

    private void servernameSetzen(String newName) {
        Logger.logVerwaltung("Servername setzen auf "+newName);
        database.setServerName(newName);
        Message message = new Message(ServerBefehl.SERVERNAME_SETZEN, new String[]{newName});
        sendMessageToAllClients(message);
        updateServerPane();
    }

    private void bannen(String user) {
        Logger.logVerwaltung("Benutzer "+user+" bannen");
        Message message = new Message(ServerBefehl.BANNEN, new String[]{user});
        sendMessageToAllClients(message);

        HashSet<ClientHandler> clients = database.getAllThreads();
        for (ClientHandler client : clients) {
            client.sendMessage(message);
            client.verbindungTrennenWennUserEquals(user);
        }

        updateBenutzerPane();
        updateRäumePane();
    }

    private void kicken(String user) {
        Logger.logVerwaltung("Benutzer "+user+" kicken");
        Message message = new Message(ServerBefehl.KICKEN, new String[]{user});
        sendMessageToAllClients(message);
        updateBenutzerPane();
        updateRäumePane();

        HashSet<ClientHandler> clients = database.getAllThreads();
        for (ClientHandler client : clients) {
            client.sendMessage(message);
            client.verbindungTrennenWennUserEquals(user);
        }

        updateBenutzerPane();
        updateRäumePane();
    }

    private void verwarnen(String user) {
        Logger.logVerwaltung("Benutzer "+user+" verwarnen");
        Message message = new Message(ServerBefehl.VERWARNEN, new String[]{user});
        sendMessageToAllClients(message);
        updateBenutzerPane();
        updateRäumePane();
    }

    private void raumLoeschen(String group) {
        if (group.equals("global")){
            feedbackLabel.setText("Der Standard-Raum kann nicht gelöscht werden.");
        }
        else {
            Logger.logVerwaltung("Raum " + group + " löschen");
            database.deleteGroup(group);
            Message message = new Message(ServerBefehl.RAUM_LOESCHEN, new String[]{group});
            sendMessageToAllClientsInGroup(message, group);
            updateRäume();
        }
    }

    private void raumnameAendern(String group, String newName) {
        Logger.logVerwaltung("Raum "+group+" umbenennen in "+newName);
        database.changeNameOfGroup(group, newName);
        Message message = new Message(ServerBefehl.RAUMNAME_AENDERN, new String[]{group});
        sendMessageToAllClientsInGroup(message, group);
        updateRäume();
    }

    private void raumErstellen(String group) {
        Logger.logVerwaltung("Raum "+group+" erstellen");
        database.createPublicGroup(group);
        Message message = new Message(ServerBefehl.RAUM_ERSTELLEN, new String[]{group});
        sendMessageToAllClientsInGroup(message, group);
        updateRäume();
    }

    private void sendMessageToAllClientsInGroup(Message message, String group) {
        HashSet<ClientHandler> clients = database.getAllThreads();
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private void sendMessageToAllClients(Message message) {
        HashSet<ClientHandler> clients = database.getAllThreads();
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }


    private Pane getSelectedPane(JTabbedPane pane) {
        return switch (pane.getSelectedIndex()) {
            case 0 -> Pane.BENUTZER;
            case 1 -> Pane.RÄUME;
            case 2 -> Pane.SERVER;
            default -> throw new IllegalStateException();
        };
    }

    private ListSelectionListener setAufgabenComboBoxListener(JList<String> jList, DefaultComboBoxModel<String> comboBoxModel) {
        return listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {

                Object nullableSelectedValue = jList.getSelectedValue();
                if (nullableSelectedValue == null){
                    return;
                }
                else {
                    // delete this
                    setSelectedItem(nullableSelectedValue.toString());
                    aufgabenComboBox.setModel(comboBoxModel);
                }
            }
        };
    }

    private ListSelectionListener setCurrentUserListener(JList<String> jList) {
        return listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {

                Object nullableGroup = jList.getSelectedValue();
                if (nullableGroup == null){
                    return;
                }
                else {
                    String user = nullableGroup.toString();
                    currentUser = user.split(disallowedSpaceCharacter)[0];
                    selectedUserLabel.setText(currentUser);
                }
            }
        };
    }


    private void updateRäumePane() {
        updateRäume();
        updateBenutzerList(currentGroup);
        selectedGroupLabel.setText(currentGroup);
        selectedUserLabel.setText(currentUser);
    }

    private void updateRäume() {
        List<String> groups = database.getAllPublicGroups();

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String group : groups) {
            listModel.addElement(group);
        }
        räumeList.setModel(listModel);
        aufgabenComboBox.setModel(räumeComboBoxModel);
    }

    private void updateBenutzerList(String group) {
        if (group == null) {return;}
        List<String> users = database.getUsersInGroup(group);
        setJListToUsers(benutzerList, users);
    }

    private void updateBenutzerPane() {
        List<String> users = database.getAllUsers();
        setJListToUsers(serverListAlleBenutzer, users);
    }

    private void updateServerPane() {
        String serverName = database.getServerName();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(serverName);
        serverList.setModel(model);
    }

    private void setJListToUsers(JList<String> jList, List<String> users) {
        if (users == null) {return;}
        HashMap<String, String> usersAndNicknames = database.getNicknamesAsMap();

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String user : users) {
            String nickname = disallowedSpaceCharacter;
            if (usersAndNicknames.containsKey(user)){
                nickname = usersAndNicknames.get(user);
            }

            listModel.addElement(user+ disallowedSpaceCharacter +"("+nickname+")");
        }
        jList.setModel(listModel);
    }

    private void setSelectedItem(String selected) {
        selectedItemInTabbedPanel = selected;
        selectedItem.setText(selected);
        aufgabenEingabe.setText(selected);
    }

    void receiveMessage(Message message) {
        serverlogAusgabe.append(message.toString()+"\n");
    }
}
