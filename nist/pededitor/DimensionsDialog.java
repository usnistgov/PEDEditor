package gov.nist.pededitor;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Dialog to ask for the length of two axes. */
public class DimensionsDialog extends JDialog {
    private static final long serialVersionUID = 2093995331175795482L;

    boolean pressedOK = false;
    JButton okButton;
    JTextField[] textFields;
    String[] dimensions = {"100.0", "100.0"};

    abstract class DimensionDialogAction extends AbstractAction {
        private static final long serialVersionUID = 2659325739020932406L;

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
        try {
            String helpText = ShowHTML.resourceFileString(filename, this);
            JEditorPane textPane = new JEditorPane();
            textPane.setEditable(false);
            textPane.setContentType("text/html");
            textPane.setText(helpText);
            contentPane.add(textPane, BorderLayout.PAGE_START);
        } catch (IOException x) {
            System.err.println(x);
        }

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
                private static final long serialVersionUID = 2235207000149359195L;

                @Override public void actionPerformed(ActionEvent e) {
                    DimensionsDialog.this.pressedOK = true;
                    setVisible(false);
                }
            });

        contentPane.add(okButton, BorderLayout.PAGE_END);
        getRootPane().setDefaultButton(okButton);
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
