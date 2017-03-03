package gov.nist.pededitor;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ShowHTML {
    public static void show(String htmlFile, JFrame parent) {
        InputStream in = parent.getClass()
            .getResourceAsStream(htmlFile);
        if (in == null) {
            JOptionPane.showMessageDialog
                (parent, "File not found: " + htmlFile);
            return;
        }
        try {
            File temp = File.createTempFile("ped", ".html");
            temp.deleteOnExit();
            try (OutputStream out = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
            		out.write(buffer, 0, len);
                    }
                    Desktop.getDesktop().browse(temp.toURI());
                }
        } catch (IOException x) {
            System.err.println(x);
        }
    }
}
