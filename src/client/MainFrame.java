package client;

import shared.Message;
import shared.ValidityChecker;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class MainFrame extends JFrame implements ValidityChecker {

    Client client;

    private String currentGroup = "global";

    public MainFrame(Client client) throws HeadlessException {
        this.client = client;
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

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    int w = (d.width - getSize().width) / 2;
    int h = (d.height - getSize().height) / 2;
    int k = 20;

    private void init() {
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(w, h);
        setLocationRelativeTo(null);
        mainPanel.setBackground(Color.WHITE);
        System.out.println(w);
        System.out.println(h);

        // Window close operation
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        client.abmelden();
                        client.beenden(client);
                        dispose();
                    }
                });

        initMainFrameComponents();
        // setLayout(null); // sets absolute positioning of components
        setVisible(true);
    }

    private void initMainFrameComponents() {
        ebene1Links.setPreferredSize(new Dimension(w/2, h));
        ebene1Rechts.setPreferredSize(new Dimension(w/2, h));
        //ebene1Links.setBackground(Color.GREEN);
        //ebene1Rechts.setBackground(Color.RED);


        ebene2LinksOben.setPreferredSize(new Dimension(w/2-k, h/8));
        ebene2LinksUnten.setPreferredSize(new Dimension(w/2-k, h/8*7-50));
        //ebene2LinksOben.setBackground(Color.YELLOW);
        //ebene2LinksUnten.setBackground(Color.BLUE);
        initLinks();

        ebene2RechtsOben.setPreferredSize(new Dimension(w/2-k, h/8));
        ebene2RechtsUnten.setPreferredSize(new Dimension(w/2-k, h/8*7-50));
        //ebene2RechtsOben.setBackground(Color.YELLOW);
        //ebene2RechtsUnten.setBackground(Color.BLUE);
        initRechts();
    }

    private void initLinks() {

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

        textAreaBenutzer.setText("abs /n asdo \n anfdo");
        //textAreaRäume.setText("abs\nasdo\nanfdo");


        //rechts unten: feedbackTextArea
        //feedbackTextArea.setPreferredSize();
        feedbackTextArea.setMaximumSize(new Dimension(w-k, h/4));


        // Listeners
        anmeldenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                anmeldenOderRegistrieren(MainFrame.this::anmelden);
            }
        });

        registrierenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                anmeldenOderRegistrieren(MainFrame.this::registrieren);
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
                    labelNickname.setText(nickname);
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
                client.abmelden();
                card2.setVisible(false);
                card1.setVisible(true);
            }
        });


        listRäume.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String group = listRäume.getSelectedValue().toString();
                    if (group != null) {
                        System.out.println("Selected: " + group);
                        chatAusgabe.setText("");
                        currentGroup = group;
                        client.getMessagesFrom(group);
                    }
                }
            }
        });

    }

    private void anmeldenOderRegistrieren(BiConsumer<String, String> biConsumer) {
        String benutzer = benutzernameTextField.getText();
        String password = Arrays.toString(passwordTextField.getPassword());

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

    private void addOneMessageToCurrentChat(Message message) {
        String user = message.getStringAtIndex(1);
        String text = message.getStringAtIndex(2);
        LocalDateTime time = message.getTime();


        chatAusgabe.append("\n"
                +"["
                +time.getHour()
                +":"
                +time.getMinute()
                +"]"
                +" "
                +user
                +": "
                +text);
    }


    // API für Client

    void showMessageInGUIIfCurrentGroup(Message message) {
        String group = message.getStringAtIndex(0);

        if (currentGroup.equals(group)) {
            addOneMessageToCurrentChat(message);
        }
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


    void addFeedback (String feedbackLine) {
        feedbackTextArea.append("\n"+feedbackLine);
        removeExcessLines(feedbackTextArea);
    }

    // von ChatGPT
    private static void removeExcessLines(JTextArea textArea) {
        String text = textArea.getText();
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


}
