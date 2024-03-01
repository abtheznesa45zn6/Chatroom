package client;

import shared.PDFMessage;
import shared.PictureMessage;
import shared.ServerBefehl;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class FileSenderGUI extends JFrame {

    private JButton chooseFileButton;
    private JButton sendFileButton;
    private JTextField fileNameTextField;
    private Client client;
    private String currentGroup;
    private boolean isPicture;


    public FileSenderGUI(Client client, String currentGroup, boolean isPicture) {
        this.client = client;
        this.currentGroup = currentGroup;
        this.isPicture = isPicture;
    }

    public void start() {
        setTitle("Choose a file to send");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 150);
        setLocationRelativeTo(null);

        chooseFileButton = new JButton("Choose File");
        sendFileButton = new JButton("Send File");
        fileNameTextField = new JTextField(20);
        fileNameTextField.setEditable(true);

        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseFile(isPicture);
            }
        });

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });

        JPanel panel = new JPanel();
        panel.add(chooseFileButton);
        panel.add(fileNameTextField);
        panel.add(sendFileButton);

        add(panel);

        setVisible(true);
    }

    private void chooseFile(boolean isPicture) {
        FileNameExtensionFilter filter;
        if (isPicture) {
            filter = new FileNameExtensionFilter("JPG Files", "jpg");
        }
        else {
            filter = new FileNameExtensionFilter("PDF Files", "pdf");

        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            fileNameTextField.setText(selectedFile.getAbsolutePath());
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
        }

    }

    private void sendFile(){
        File file = new File(fileNameTextField.getText());
        System.out.println(file.getTotalSpace());
        if (file.exists()) {

            byte[] bytes;
            try {
                bytes = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            String data = Base64.getEncoder().encodeToString(bytes);

            if (isPicture) {
                client.sendMessage(new PictureMessage(ServerBefehl.PICTURE_MESSAGE, new String[]{currentGroup, data}));
            }
            else {
                client.sendMessage(new PDFMessage(ServerBefehl.PDF_MESSAGE, new String[]{currentGroup, data}));
            }

            System.out.println("File sent successfully!");
            this.dispose();
        }
        else {
            System.out.println("File couldn't be sent");
        }
    }
}