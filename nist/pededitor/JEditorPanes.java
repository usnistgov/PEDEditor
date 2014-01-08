/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import javax.swing.JEditorPane;

public class JEditorPanes {
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
    public static void setHTMLResource(JEditorPane pane,
                                       String filename, Class<?> klass)
        throws IOException {
        InputStream in = klass.getResourceAsStream(filename);

        if (in == null) {
            throw new FileNotFoundException(filename);
        }

        pane.setContentType("text/html");
        pane.setText(streamToString(in));
    }
}
