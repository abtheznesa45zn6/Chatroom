package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PasswordChangeGUI extends JFrame {

    private Client client;
    private JLabel oldPasswordLabel;
    private JPasswordField oldPasswordField;
    private JLabel newPasswordLabel;
    private JPasswordField newPasswordField;
    private JButton okButton;


    public PasswordChangeGUI(Client client) {
        this.client = client;
    }

    public void start(){

        setTitle("Passwort ändern");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(300, 150);
        setLocationRelativeTo(null);

        oldPasswordLabel = new JLabel("Altes Passwort:");
        oldPasswordField = new JPasswordField(20);
        newPasswordLabel = new JLabel("Neues Passwort:");
        newPasswordField = new JPasswordField(20);
        okButton = new JButton("OK");

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (client.isAngemeldet()) {
                    changePassword();
                }
            }
        });

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.add(oldPasswordLabel);
        panel.add(oldPasswordField);
        panel.add(newPasswordLabel);
        panel.add(newPasswordField);
        panel.add(new JLabel()); // Empty label for spacing
        panel.add(okButton);

        add(panel);

        setVisible(true);
    }

    private void changePassword() {
        char[] oldPassword = oldPasswordField.getPassword();
        char[] newPassword = newPasswordField.getPassword();

        client.changePassword(new String(oldPassword), new String(newPassword));

        dispose();
    }
}