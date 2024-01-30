package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

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

        setTitle("Password Change");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(300, 150);
        setLocationRelativeTo(null);

        oldPasswordLabel = new JLabel("Old Password:");
        oldPasswordField = new JPasswordField(20);
        newPasswordLabel = new JLabel("New Password:");
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

        // Perform password change logic here

        // For now, let's just print the passwords for demonstration purposes
        System.out.println("Old Password: " + new String(oldPassword));
        System.out.println("New Password: " + new String(newPassword));

        client.changePassword(new String(oldPassword), new String(newPassword));

        // Close the window
        dispose();
    }
}