package client;

import shared.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientGUI extends JFrame implements ValidityChecker {

    private Client client;

    private String currentGroup = "global";

    private HashMap<String, String> nicknameMap = new HashMap<>();

    Set<String> privateGroups = new HashSet<>();
    HashMap<String, PrivateChatGUI> groupAndChat = new HashMap<>();

    ClientGUI(Client client) throws HeadlessException {
        this.client = client;
    }


    private JPanel mainPanel;
    private JPanel ebene1Links;
    private JPanel ebene1Rechts;
    private JPanel ebene2LinksOben;
    private JPanel ebene2LinksUnten;
    private JPanel ebene2RechtsOben;
    private JPanel ebene2RechtsUnten;
    private JTextArea chatAusgabe;
    private JTextField chatEingabe;
    private JScrollPane chatAusgabeScrollPane;
    private JTextField setNicknameTextField;
    private JButton setNicknameButton;
    private JTabbedPane tabbedPane1;
    private JTextArea textAreaBenutzer;
    private JTextArea textAreaRäume;
    private JMenu benutzer;
    private JButton buttonSenden;
    private JLabel verbindung;
    private JLabel status;
    private JList<String> listRäume;
    private JLabel labelNickname;
    private JPanel card2;
    private JPanel card1;
    private JTextField benutzernameTextField;
    private JPasswordField passwordTextField;
    private JButton anmeldenButton;
    private JButton registrierenButton;
    private JTextArea feedbackTextArea;
    private JPanel cardObenRechts;
    private JButton abmeldenButton;
    private JList<String> listBenutzer;
    private JLabel verbindungLabel;
    private JLabel statusLabel;
    private JLabel currentGroupLabel;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    int w = (d.width - getSize().width) / 2;
    int h = (d.height - getSize().height) / 2;
    int k = 20;

    void start() {
        setContentPane(mainPanel);
        setSize(w, h);
        setLocationRelativeTo(null);
        setResizable(true);

        // Window close operation
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        client.sendAbmeldenMessage();
                        client.beenden(client);
                    }
                });

        initMainFrameComponents();
        setVisible(true);
    }

    private void initMainFrameComponents() {
        addMenu();
        ebene1Links.setPreferredSize(new Dimension(w/2, h));
        ebene1Rechts.setPreferredSize(new Dimension(w/2, h));

        ebene2LinksOben.setPreferredSize(new Dimension(w/2-k, h/6));
        ebene2LinksUnten.setPreferredSize(new Dimension(w/2-k, h/8*6-50));
        initLinks();

        ebene2RechtsOben.setPreferredSize(new Dimension(w/2-k, h/6));
        ebene2RechtsUnten.setPreferredSize(new Dimension(w/2-k, h/8*6-50));
        initRechts();
    }

    private void addMenu() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu profil = new JMenu("Profil");
        benutzer = new JMenu("Benutzer");
        JMenu raum = new JMenu("Raum");
        JMenu optionen = new JMenu("Optionen");
        JMenu senden = new JMenu("Senden");
        menuBar.add(profil);
        menuBar.add(benutzer);
        menuBar.add(raum);
        menuBar.add(optionen);

        JMenuItem profilPasswortÄndern = new JMenuItem("Passwort ändern");
        profil.add(profilPasswortÄndern);

        JMenuItem sendenJPG = new JMenuItem("JPG");
        senden.add(sendenJPG);
        JMenuItem sendenPDF = new JMenuItem("PDF");
        senden.add(sendenPDF);
        menuBar.add(senden);

        sendenJPG.addActionListener(event -> {
            FileSenderGUI sendJPGDialog = new FileSenderGUI(client, currentGroup, true);
            sendJPGDialog.start();
        });

        sendenPDF.addActionListener(event -> {
            FileSenderGUI sendPDFBildDialog = new FileSenderGUI(client, currentGroup,false);
            sendPDFBildDialog.start();
        });

        profilPasswortÄndern.addActionListener(event -> {
            PasswordChangeGUI passwordChangeGUI = new PasswordChangeGUI(client);
            passwordChangeGUI.start();
        });
    }

    private void initLinks() {

        resetStatus();

        int rows = 15;
        int columns = 40;
        chatAusgabe.setRows(rows);
        chatAusgabe.setColumns(columns);

        chatAusgabeScrollPane.setPreferredSize(new Dimension((int)(w/2.2), h/8*4));

        chatEingabe.setPreferredSize(new Dimension((int)(w/2), h/20));
    }


    private void initRechts() {
        //setNicknameTextField.setPreferredSize(new Dimension(w/10, h/26));


        //rechts unten: feedbackTextArea
        //feedbackTextArea.setPreferredSize();
        feedbackTextArea.setMaximumSize(new Dimension(w-k, h/4));


        // Listeners
        anmeldenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                anmeldenOderRegistrieren(ClientGUI.this::anmelden);
            }
        });

        registrierenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                anmeldenOderRegistrieren(ClientGUI.this::registrieren);
            }
        });

        setNicknameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nickname = getAndReset(setNicknameTextField);

                if (!checkValidityOfNickname(nickname)) {
                    userBenachritigen("Nickname ungültig");
                }
                else {
                    client.setNicknameTo(nickname);
                }
            }
        });

        buttonSenden.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String textMessage = getAndReset(chatEingabe);

                if (client.isAngemeldet()) {
                    if (checkValidityOfText(textMessage)) {
                        client.sendTextMessage(currentGroup, textMessage);
                    }
                    else {
                        userBenachritigen("Nachricht ungültig");
                    }
                }
                else {
                    userBenachritigen("Du bist nicht angemeldet.");
                }

            }
        });

        abmeldenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.sendAbmeldenMessage();
                client.setAngemeldet(false);
                card2.setVisible(false);
                card1.setVisible(true);
            }
        });


        listRäume.addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {

                Object nullableGroup = listRäume.getSelectedValue();
                if (nullableGroup == null){
                    return;
                }
                else {
                    String group = nullableGroup.toString();
                        System.out.println("Selected: " + group);
                        chatAusgabe.setText("");
                        currentGroup = group;
                        currentGroupLabel.setText("Raum: "+group);
                        client.getMessagesFrom(group);
                        client.getUserList(group);
                }
            }
        });

    }

    private void anmeldenOderRegistrieren(BiConsumer<String, String> biConsumer) {
        String benutzer = benutzernameTextField.getText();
        //TODO passwordTextField.getPassword());
        String password = passwordTextField.getText();

        if (!checkValidityOfName(benutzer)) {
            userBenachritigen("Benutzername ungültig");
        }
        if (!checkValidityOfPassword(password)) {
            userBenachritigen("Passwort ungültig");
        }
        if (checkValidityOfName(benutzer) && checkValidityOfPassword(password)) {
            biConsumer.accept(benutzer, password);
        }
    }

    private void anmelden(String benutzer, String password) {
        client.anmelden(benutzer, password);
    }

    private void registrieren(String benutzer, String password) {
        client.registrieren(benutzer, password);
    }

    private String getAndReset(JTextComponent textArea) {
        String input = textArea.getText();
        textArea.setText("");
        return input;
    }

    private void userBenachritigen(String feedback) {
        addFeedback(feedback);
    }

    private void addTextMessageToCurrentChat(TextMessage message) {
        appendToChat(chatAusgabe, message);
    }

    private void addTextMessageToPrivateChat(TextMessage message) {
        PrivateChatGUI privateChatGUI = groupAndChat.get(message.getGroup());
        if (privateChatGUI != null) {
            JTextArea chat = privateChatGUI.getChatAusgabe();
            appendToChat(chat, message);
        }
    }

    private void appendToChat(JTextArea chat, TextMessage message){
        chat.append(
                String.format(
                        "\n[%s] %s: %s",
                        message.getTime().format(formatter),
                        nicknameOfUser(message.getUser()),
                        message.getText()));
    }

    private String nicknameOfUser(String user) {
        String nullableNickname = nicknameMap.get(user);
        if (nullableNickname == null) {
            return user;
        }
        else {
            return nullableNickname;
        }
    }

    void resetStatus(){
        statusLabel.setText("zu keinem Server verbunden.");
        //verbindungLabel.setText("nicht angemeldet.");
        verbindungLabel.setText("");
    }


    // API für Client

    void setPrivateGroups(Set<String> pg) {
        privateGroups = pg;
    }

    void abmelden() {
        resetStatus();
        card2.setVisible(false);
        card1.setVisible(true);
    }

    void showMessageInGUI(TextMessage message) {
        String group = message.getGroup();

        if (isCurrentGroup(group)) {
            addTextMessageToCurrentChat(message);
        }

        if (privateGroups.contains(message.getGroup())){
            addTextMessageToPrivateChat(message);
        }
    }


    void showMessageInGUI(PictureMessage message) {
        String group = message.getStringAtIndex(0);
        String picture = message.getStringAtIndex(1);

        String path = "messages/" + group + "/" + message.getTime().toInstant(ZoneOffset.UTC).getEpochSecond() + ".jpg";
        writeFileFromString(Paths.get(path),picture);

        if (isCurrentGroup(group)) {
            var abc = new DesktopOpenFile(path);
        }
        System.out.println(Paths.get(path).getFileName());
    }

    void showMessageInGUI(PDFMessage message) {
        String group = message.getStringAtIndex(0);
        String pdf = message.getStringAtIndex(1);

        String path = "messages/" + group + "/" + message.getTime().toInstant(ZoneOffset.UTC).getEpochSecond() + ".pdf";

        // only write and open new messages
        if (!Files.exists(Paths.get(path))) {
            writeFileFromString(Paths.get(path),pdf);

            if (isCurrentGroup(group)) {
                var abc = new DesktopOpenFile(path);
            }
        }
    }

    private static void writeFileFromString(Path outputPath, String data) {

        byte[] decodedBytes = Base64.getDecoder().decode(data);

        // Create parent directories if they don't exist
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            e.printStackTrace();
            return; // Stop execution if directory creation fails
        }

        // Write the file
        try {
            Files.write(outputPath, decodedBytes);
            System.out.println("File written successfully to: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private boolean isCurrentGroup(String group) {
        return currentGroup.equals(group);
    }


    void setNickname(String user) {
        labelNickname.setText(user);
        card2.setVisible(true);
        card1.setVisible(false);
    }

    void updateRooms (ArrayList<String> rooms) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        HashSet<String> newPrivateGroups = new HashSet<>();

        for (String group : rooms) {
            if (group.contains(privateChatIndicator)){
                newPrivateGroups.add(group);
            }
            else {
                listModel.addElement(group);
            }
        }
        privateGroups = newPrivateGroups;
        listRäume.setModel(listModel);
    }

    void updateUsersOfGroup(ArrayList<String> users, String group) {
        if(currentGroup.equals(group)){
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String user : users) {
                String line = String.format("%s (%s)", nicknameOfUser(user), user);
                listModel.addElement(line);
            }
            listBenutzer.setModel(listModel);
        }
    }

    void updateUserList(ArrayList<String> users) {
        for (String user : users) {
            // falls es der Nutzer selbst ist, nicht hinzufügen
            // beim ersten Mal ist der Nutzername nur im benutzernameTextField vorhanden
            if (user.equals(client.getAngemeldeterNutzer()) || user.equals(benutzernameTextField.getText())) {
                continue;
            }

            JMenu jMenu = new JMenu(user);
            JMenuItem privaterChat = new JMenuItem("Privater Chat");
            jMenu.add(privaterChat);

            privaterChat.addActionListener(event -> {
                String clickedUser = jMenu.getText();

                PrivateChatGUI privateChatGUI = new PrivateChatGUI(client, this, getPrivateGroupNameOfGroupWithUser(clickedUser), clickedUser);
                if (hasPrivateGroupWith(clickedUser)) {
                    privateChatGUI.start(PrivateChatStatus.VERBINDUNG_AUFGEBAUT);
                }
                else {
                    privateChatGUI.start(PrivateChatStatus.KEINE_VERBINDUNG);
                }
            });

            benutzer.add(jMenu);
        }
    }

    private String getPrivateGroupNameOfGroupWithUser(String user1) {
        String user2 = client.getAngemeldeterNutzer();
        return Stream.of(user1, user2)
                .sorted()
                .collect(Collectors.joining(" & ", privateChatIndicator, ""));
    }

    private boolean hasPrivateGroupWith(String user) {
        return privateGroups.contains(getPrivateGroupNameOfGroupWithUser(user));
    }


    void addFeedback (String feedbackLine) {
        feedbackTextArea.append("\n"+feedbackLine);
        removeExcessLines(feedbackTextArea);
    }
    // von ChatGPT
    private static void removeExcessLines(JTextArea textArea) {
        int lineCount = textArea.getLineCount();
        int MAX_LINES = 12;

        // Check if the number of lines exceeds the maximum
        if (lineCount > MAX_LINES) {
            try {
                // Get the start offset of the second line
                int startOffset = textArea.getLineStartOffset(lineCount - MAX_LINES);

                // Remove lines up to the start offset
                textArea.replaceRange("", 0, startOffset);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateNicknameList(HashMap<String, String> updatedMap) {
        // update own nickname if changed
        String nullableNickname = updatedMap.get(client.getAngemeldeterNutzer());
        if (nullableNickname!=null && !(nullableNickname.equals(nicknameMap.get(client.getAngemeldeterNutzer())))){
            labelNickname.setText(nullableNickname);
            userBenachritigen("Nickname wurde geändert zu "+nullableNickname);
        }

        // update nickname list
        nicknameMap = updatedMap;
    }

    public void setServerName(String newName) {
        statusLabel.setText("verbunden mit Server "+newName+".");
    }

    public void erstelleRaum(String groupName) {
        addFeedback("Ein neuer Raum namens "+groupName+" wurde erstellt.");
    }

    public void aendereRaumname(String oldName, String newName) {
        addFeedback("Der Raum namens +"+oldName+" wurde in"+newName+" umbenannt.");
    }

    public void loescheRaum(String geloeschterRaum) {
        addFeedback("Der Raum +"+geloeschterRaum+" wurde gelöscht.");
    }

    public void verwarnen(String warnedUser) {
        if (client.getAngemeldeterNutzer().equals(warnedUser)) {
            addFeedback("Du wurdest verwarnt.");
        }
        else {
            addFeedback("Nutzer "+warnedUser+" wurde verwarnt.");
        }
    }

    public void kicken(String kickedUser) {
        if (client.getAngemeldeterNutzer().equals(kickedUser)) {
            addFeedback("Du wurdest vom Server gekickt.");
        }
        else {
            addFeedback("Nutzer "+kickedUser+" wurde vom Server gekickt.");
        }
    }

    public void bannen(String bannedUser) {
        if (client.getAngemeldeterNutzer().equals(bannedUser)) {
            addFeedback("Du wurdest vom Server gebannt.");
        }
        else {
            addFeedback("Nutzer "+bannedUser+" wurde gebannt.");
        }
    }

    public void removePrivateGroup(String group) {
        if (groupAndChat.containsKey(group)) {
            PrivateChatGUI privateChatGUI = groupAndChat.get(group);
            if (privateChatGUI != null) {
                privateChatGUI.dispose();
            }
            groupAndChat.remove(group);
            privateGroups.remove(group);
            userBenachritigen("Die "+group+" wurde geschlossen.");
        }
    }

    public void verbindungStarten(String group) {
        groupAndChat.get(group).chatStarten();
    }
}
