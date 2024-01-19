package client;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class DesktopOpenFile {

    public DesktopOpenFile(String pathname) {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop != null && desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(new File(pathname));
            } else {
                System.err.println("Datei kann nicht angezeigt werden!");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
