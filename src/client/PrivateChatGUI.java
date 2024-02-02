package client;

import shared.ValidityChecker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static client.PrivateChatStatus.*;

public class PrivateChatGUI extends JFrame {
    private Client client;
    ClientGUI clientGUI;
    private String group;
    private String empfänger;


    PrivateChatGUI(Client client, ClientGUI clientGUI, String group, String empfänger) {
        this.client = client;
        this.clientGUI = clientGUI;
        this.group = group;
        this.empfänger = empfänger;

        init();
    }

    private PrivateChatGUI() {
    }

    private JPanel mainPanel;
    private JButton keineVerbindungButton;
    private JPanel keineVerbindungCard;
    private JPanel aufbauenVerbindungCard;
    private JPanel aufgebauteVerbindungCard;
    private JTextArea chatAusgabe;
    private JScrollPane chatAusgabeScrollPane;
    private JTextField chatEingabe;
    private JButton buttonSenden;
    private JPanel cards;

    public static void main(String[] args) {
        PrivateChatGUI privateChatGUI = new PrivateChatGUI();
        privateChatGUI.init();
        privateChatGUI.start(VERBINDUNG_AUFGEBAUT);
    }

    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    int w = (d.width - getSize().width) / 5;
    int h = (d.height - getSize().height) / 4;


    void start(PrivateChatStatus status) {
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

    private void init() {
        setTitle("Private Verbindung mit "+empfänger);
        setContentPane(mainPanel);
        setSize(w, h);
        setLocationRelativeTo(null);
        setResizable(true);

        initCard1();
        initCard2();
        initCard3();

        // Window close operation
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        clientGUI.groupAndChat.remove(group);
                    }
                });
    }

    private void initCard1() {
        keineVerbindungButton.addActionListener(e -> {
            setCard(VERBINDUNGSAUFBAU);
            clientGUI.groupAndChat.put(group, this);
            client.requestPrivateChat(group, empfänger);
        });
    }

    private void initCard2() {
    }

    private void initCard3() {
        buttonSenden.addActionListener(e -> {
            String textMessage = chatEingabe.getText();
            chatEingabe.setText("");

            client.sendTextMessage(group, textMessage);
        });
    }

    JTextArea getChatAusgabe() {
        return chatAusgabe;
    }

    void chatStarten() {
        System.out.println("chatStarten "+group);
        setCard(VERBINDUNG_AUFGEBAUT);
    }
}






