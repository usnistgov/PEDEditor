package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Simple dialog to display HTML. */
public class HTMLDialog extends JDialog {

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
        } catch (IOException e) {
            throw new Error(e);
        }
        JScrollPane editorScrollPane = new JScrollPane(editorPane);
        editorScrollPane.setPreferredSize(new Dimension(500, 500));

        getContentPane().add(editorScrollPane);
        pack();
    }
}