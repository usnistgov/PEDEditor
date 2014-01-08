/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

/** Simple dialog to display HTML. */
public class HTMLDialog extends JDialog {
    private static final long serialVersionUID = 2081369025744823207L;

    public HTMLDialog(JFrame parent, String filename, String title) {
        super(parent, title);
        URL aboutURL = parent.getClass().getResource(filename);
        if (aboutURL == null) {
            throw new Error("File " + filename + " not found");
        }
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        try {
            editorPane.setPage(aboutURL);
            String s = editorPane.getText();
            editorPane.setContentType("text/html");
            System.err.println(s);
            editorPane.setText(s);
        } catch (IOException e) {
            throw new Error(e);
        }
        JScrollPane editorScrollPane = new JScrollPane(editorPane);
        editorScrollPane.setPreferredSize(new Dimension(500, 500));

        getContentPane().add(editorScrollPane);
        pack();
    }
}
