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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class ClientGUI extends JFrame implements ValidityChecker {

    Client client;

    private String currentGroup = "global";

    private HashMap<String, String> nicknameMap = new HashMap<>();

    public ClientGUI(Client client) throws HeadlessException {
        this.client = client;
        init();
    }

    public ClientGUI() throws HeadlessException {
        this.client = null;
        init();
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
    private JButton buttonSenden;
    private JLabel verbindung;
    private JLabel status;
    private JList listRäume;
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
    private JList listBenutzer;
    private JLabel verbindungLabel;
    private JLabel statusLabel;

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    int w = (d.width - getSize().width) / 2;
    int h = (d.height - getSize().height) / 2;
    int k = 20;

    private void init() {
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(w, h);
        setLocationRelativeTo(null);
        setResizable(false);
        System.out.println(w);
        System.out.println(h);

        // Window close operation
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        client.sendAbmeldenMessage();
                        client.beenden(client);
                        dispose();
                    }
                });

        initMainFrameComponents();
        setVisible(true);
    }

    private void initMainFrameComponents() {
        addMenu();
        ebene1Links.setPreferredSize(new Dimension(w/2, h));
        ebene1Rechts.setPreferredSize(new Dimension(w/2, h));

        ebene2LinksOben.setPreferredSize(new Dimension(w/2-k, h/8));
        ebene2LinksUnten.setPreferredSize(new Dimension(w/2-k, h/8*7-50));
        initLinks();

        ebene2RechtsOben.setPreferredSize(new Dimension(w/2-k, h/8));
        ebene2RechtsUnten.setPreferredSize(new Dimension(w/2-k, h/8*7-50));
        initRechts();
    }

    private void addMenu() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu menu = new JMenu("Menu");
        JMenu benutzer = new JMenu("Benutzer");
        JMenu raum = new JMenu("Raum");
        JMenu optionen = new JMenu("Optionen");
        JMenu senden = new JMenu("Senden");
        menuBar.add(menu);
        menuBar.add(benutzer);
        menuBar.add(raum);
        menuBar.add(optionen);


        JMenuItem sendenJPG = new JMenuItem("JPG");
        senden.add(sendenJPG);
        JMenuItem sendenPDF = new JMenuItem("PDF");
        senden.add(sendenPDF);
        menuBar.add(senden);

        sendenJPG.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                FileSenderGUI sendJPGDialog = new FileSenderGUI(client, currentGroup, true);
            }
        });

        sendenPDF.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                FileSenderGUI sendPDFBildDialog = new FileSenderGUI(client, currentGroup,false);
            }
        });



    }

    private void initLinks() {

        resetStatus();

        int rows = 15;
        int columns = 40;
        chatAusgabe.setRows(rows);
        chatAusgabe.setColumns(columns);

        chatAusgabeScrollPane.setPreferredSize(new Dimension((int)(w/2.2), h/8*5));

        chatEingabe.setPreferredSize(new Dimension((int)(w/2.3), h/20));


        // Listeners

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

                if (client.getAngemeldet()) {
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
                        client.getMessagesFrom(group);
                        client.getUserList(group);
                }
            }
        });

    }

    private void anmeldenOderRegistrieren(BiConsumer<String, String> biConsumer) {
        String benutzer = benutzernameTextField.getText();
        //String password = Arrays.toString(passwordTextField.getPassword());
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
    private void nicknameÄndern(String nickname) {
        client.setNicknameTo(nickname);
    }

    private String getAndReset(JTextComponent textArea) {
        String input = textArea.getText();
        textArea.setText("");
        return input;
    }

    private void userBenachritigen(String feedback) {
        addFeedback(feedback);
    }

    private void addTextMessageToCurrentChat(Message message) {
        String user = message.getStringAtIndex(1);
        String text = message.getStringAtIndex(2);
        LocalDateTime time = message.getTime();

        // Formatting the time using DateTimeFormatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = time.format(formatter);


        chatAusgabe.append("\n"
                +"["
                +formattedTime
                +"]"
                +" "
                +nicknameOfUser(user)
                +": "
                +text);
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
        verbindungLabel.setText("nicht angemeldet.");
    }


    // API für Client

    void abmelden() {
        resetStatus();
        card2.setVisible(false);
        card1.setVisible(true);
    }

    void showMessageInGUI(TextMessage message) {
        String group = message.getStringAtIndex(0);

        if (isCurrentGroup(group)) {
            addTextMessageToCurrentChat(message);
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


    public void setNickname(String user) {
        labelNickname.setText(user);
        card2.setVisible(true);
        card1.setVisible(false);
    }

    void updateRooms (ArrayList<String> rooms) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String group : rooms) {
            listModel.addElement(group);
        }
        listRäume.setModel(listModel);
    }

    public void updateUsers(String group, ArrayList<String> users) {
        if(currentGroup.equals(group)){
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String user : users) {
                String line = nicknameOfUser(user) + " (" + user + ")";
                listModel.addElement(line);
            }
            listBenutzer.setModel(listModel);
        }
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
}
