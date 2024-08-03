package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static client.PrivateChatStatus.*;

public class PrivateChatGUI extends JFrame {
    private final Client client;
    ClientGUI clientGUI;
    private final String group;
    private final String empfaenger;

    PrivateChatGUI(Client client, ClientGUI clientGUI, String group, String empfaenger) {
        this.client = client;
        this.clientGUI = clientGUI;
        this.group = group;
        this.empfaenger = empfaenger;

        init();
    }

    private JPanel mainPanel;
    private JButton keineVerbindungButton;
    private JPanel keineVerbindungCard;
    private JPanel aufbauenVerbindungCard;
    private JPanel aufgebauteVerbindungCard;
    private JTextArea chatAusgabe;
    private JTextField chatEingabe;
    private JButton buttonSenden;
    private JScrollPane chatAusgabeScrollPane;

    private void init() {
        setTitle("Private Verbindung mit "+ empfaenger);
        setContentPane(mainPanel);
        setWindowSize();
        setLocationRelativeTo(null);
        setResizable(true);

        initKeineVerbindungCard(); //Card 1
        initAufgebauteVerbindungCard(); //Card 3

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        clientGUI.removePrivateGroup(group);
                        client.sendRemovePrivateGroup(group);
                    }
                });
    }

    private void setWindowSize() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int w = (d.width - getSize().width) / 5;
        int h = (d.height - getSize().height) / 4;
        setSize(w, h);
    }

    private void initKeineVerbindungCard() {
        keineVerbindungButton.addActionListener(e -> {
            setCard(VERBINDUNGSAUFBAU);
            clientGUI.groupAndChat.put(group, this);
            client.requestPrivateChat(group, empfaenger);
        });
    }

    private void initAufgebauteVerbindungCard() {
        buttonSenden.addActionListener(e -> {
            String textMessage = chatEingabe.getText();
            chatEingabe.setText("");

            client.sendTextMessage(group, textMessage);
        });
    }

    void show(PrivateChatStatus status) {
        setCard(status);
        setVisible(true);
    }

    private void setCard(PrivateChatStatus status) {
        keineVerbindungCard.setVisible(false);
        aufbauenVerbindungCard.setVisible(false);
        aufgebauteVerbindungCard.setVisible(false);
        switch (status) {
            case KEINE_VERBINDUNG -> keineVerbindungCard.setVisible(true);
            case VERBINDUNGSAUFBAU -> aufbauenVerbindungCard.setVisible(true);
            case VERBINDUNG_AUFGEBAUT -> aufgebauteVerbindungCard.setVisible(true);
        }
    }

    JTextArea getChatAusgabe() {
        return chatAusgabe;
    }

    void chatStarten() {
        System.out.println("chatStarten "+group);
        setCard(VERBINDUNG_AUFGEBAUT);
    }
}