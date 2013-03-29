package gov.nist.pededitor;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

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

    /** From Pavel Repin's suggestion on StackOverflow. */
    public static String streamToString(InputStream is) {
        try (Scanner s = new java.util.Scanner(is)) {
          s.useDelimiter("\\A");
          return s.hasNext() ? s.next() : "";
        }
    }

    /** Work-around for Java 7 Windows 64-bit bug that causes HTML
        files embedded in JAR files to be presented as text in a
        JEditorPane. */
    public static String resourceFileString(String file, Object parent)
        throws IOException {
        InputStream in = parent.getClass()
            .getResourceAsStream(file);

        if (in == null) {
            throw new FileNotFoundException(file);
        }

        return streamToString(in);
    }
}
