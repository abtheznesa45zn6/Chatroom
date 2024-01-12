package server;

import shared.ValidityChecker;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;

public class ServerGUI extends JFrame implements ValidityChecker {
    private ConnectionListener server;

    private int currentPanel;
    private String currentGroup;

    private String selectedItemInTabbedPanel;
    private final Database database = Database.getInstance();

    public ServerGUI(ConnectionListener server) {
        this.server = server;
    }

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

        updateRäume();
        updateAlleBenutzerInServer();

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newPanel = tabbedPane.getSelectedIndex();

                if (currentPanel != tabbedPane.getSelectedIndex()) {
                    switch (newPanel) {
                        case 0 -> updateBenutzer(currentGroup);
                        case 1 -> updateRäume();
                        case 2 -> updateAlleBenutzerInServer();
                        default -> throw new IllegalStateException();
                    };
                }
                currentPanel = newPanel;
            }
        });


        // ComboBoxes, one per tab
        DefaultComboBoxModel<String> räumeComboBoxModel = new DefaultComboBoxModel<>();
        String[] räumeAufgaben = { "Raum erstellen", "Raumname ändern", "Raum löschen"};
        for (String aufgaben : räumeAufgaben) {
            räumeComboBoxModel.addElement(aufgaben);
        }

        DefaultComboBoxModel<String> benutzerComboBoxModel = new DefaultComboBoxModel<>();
        String[] benutzerAufgaben = { "Verwarnen", "Kicken", "Bannen"};
        for (String aufgaben : benutzerAufgaben) {
            benutzerComboBoxModel.addElement(aufgaben);
        }

        DefaultComboBoxModel<String> serverComboBoxModel = new DefaultComboBoxModel<>();
        String[] serverAufgaben = { "Servername setzen", "Verwarnen", "Kicken", "Bannen"};
        for (String aufgaben : serverAufgaben) {
            serverComboBoxModel.addElement(aufgaben);
        }

        // initiale Einstellung
        aufgabenComboBox.setModel(räumeComboBoxModel);

        // list listeners
        räumeList.addListSelectionListener(tabbedPaneListListener(räumeList, räumeComboBoxModel));
        benutzerList.addListSelectionListener(tabbedPaneListListener(benutzerList, benutzerComboBoxModel));
        serverListAlleBenutzer.addListSelectionListener(tabbedPaneListListener(serverListAlleBenutzer, serverComboBoxModel));

        räumeList.addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {

                Object nullableGroup = räumeList.getSelectedValue();
                if (nullableGroup == null){
                    return;
                }
                else {
                    String group = nullableGroup.toString();
                    currentGroup = group;
                }
            }
        });
    }

    private ListSelectionListener tabbedPaneListListener(JList<String> jList, DefaultComboBoxModel<String> comboBoxModel) {
        return listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {

                Object nullableSelectedValue = jList.getSelectedValue();
                if (nullableSelectedValue == null){
                    return;
                }
                else {
                    setSelectedItem(nullableSelectedValue.toString());
                    aufgabenComboBox.setModel(comboBoxModel);
                }
            }
        };
    }


    private void updateRäume() {
        List<String> groups = database.getAllGroups();

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String group : groups) {
            listModel.addElement(group);
        }
        räumeList.setModel(listModel);
    }

    private void updateBenutzer(String group) {
        if (group == null) {return;}
        List<String> users = database.getUsersInGroup(group);
        setJListToUsers(benutzerList, users);
    }

    private void updateAlleBenutzerInServer() {
        List<String> users = database.getAllUsers();
        setJListToUsers(serverListAlleBenutzer, users);
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
        selectedItem.setText(selected);
        selectedItemInTabbedPanel = selected;
    }
}
