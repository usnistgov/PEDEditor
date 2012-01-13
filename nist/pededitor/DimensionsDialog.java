package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Dialog to ask for the length of two axes. */
public class DimensionsDialog extends JDialog {
    boolean pressedOK = false;
    JButton okButton;
    JTextField[] textFields;
    String[] dimensions = {"100.0", "100.0"};

    abstract class DimensionDialogAction extends AbstractAction {
        DimensionDialogAction(String name) {
            super(name);
        }
    }

    DimensionsDialog(Frame owner, String[] labels) {
        super(owner, "Select Axis Lengths", false);

        JPanel contentPane = (JPanel) getContentPane();

        JPanel rows = new JPanel(new GridLayout(0, 2, 5, 0));
        textFields = new JTextField[labels.length];

        String filename = "dimensionshelp.html";
        URL helpURL = getClass().getResource(filename);
        if (helpURL == null) {
            throw new Error("File " + filename + " not found");
        }
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        try {
            editorPane.setPage(helpURL);
        } catch (IOException e) {
            throw new Error(e);
        }
        contentPane.add(editorPane, BorderLayout.PAGE_START);

        for (int i = 0; i < labels.length; ++i) {
            JLabel label = new JLabel(labels[i]);
            JTextField text = new JTextField("100.0");
            textFields[i] = text;
            label.setLabelFor(text);
            rows.add(label);
            rows.add(text);
        }
        contentPane.add(rows, BorderLayout.CENTER);

        okButton = new JButton(new DimensionDialogAction("OK") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    DimensionsDialog.this.pressedOK = true;
                    setVisible(false);
                }
            });

        contentPane.add(okButton, BorderLayout.PAGE_END);
    }

    DimensionsDialog(Frame owner) {
        this(owner, new String[] {"X Axis", "Y Axis"});
    }

    /** Set the dimensions strings. The user may change these values
        (that's the point of presenting the dialog). */
    public void setDimensions(String[] dimensions) {
        for (int i = 0; i < dimensions.length; ++i) {
            textFields[i].setText(dimensions[i]);
        }
    }

    /** Show the dialog as document-modal, and return the selected
        dimensions (as strings). Return null if the dialog was closed
        abnormally. */
    public String[] showModal() {
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        pack();
        setVisible(true);
        if (pressedOK) {
            String[] output = new String[textFields.length];
            for (int i = 0; i < textFields.length; ++i) {
                output[i] = textFields[i].getText();
            }
            return output;
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        String[] dimensions = (new DimensionsDialog(null)).showModal();
        System.out.println(Arrays.toString(dimensions));
    }
   
}