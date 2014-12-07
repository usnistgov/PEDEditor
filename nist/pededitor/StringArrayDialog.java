/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Generic dialog that presents several rows of labels and
    corresponding text boxes and returns the array of text box text
    values once the user presses "OK". */
public class StringArrayDialog extends JDialog {
    private static final long serialVersionUID = 4234603969579529313L;

    boolean pressedOK = false;
    JTextField[] textFields;
    JPanel panelBeforeOK = new JPanel();

    protected void add(JComponent c, GridBagLayout gb,
                       GridBagConstraints gbc) {
        gb.setConstraints(c, gbc);
        getContentPane().add(c);
    }

    StringArrayDialog(Frame owner, String[] labels, String[] strings,
                      String intro) {
        super(owner, "Edit values", false);

        GridBagUtil gb = new GridBagUtil(getContentPane());

        textFields = new JTextField[labels.length];

        if (intro != null) {
            gb.centerAndEndRow(new JLabel(intro));
        }

        for (int i = 0; i < labels.length; ++i) {
            JLabel label = new JLabel(labels[i]);
            JTextField text = new JTextField(20);
            if (strings != null && strings[i] != null) {
                text.setText(strings[i]);
            }
            textFields[i] = text;
            label.setLabelFor(text);

            gb.addWest(label);
            gb.endRowWith(text);
        }

        AbstractAction okAction = new AbstractAction("OK") {
                private static final long serialVersionUID = 979439543555230448L;

                @Override public void actionPerformed(ActionEvent e) {
                    StringArrayDialog.this.pressedOK = true;
                    setVisible(false);
                }
            };
        JButton okButton = new JButton(okAction);
        getRootPane().setDefaultButton(okButton);
        gb.endRowWith(panelBeforeOK);
        gb.centerAndEndRow(okButton);
        setResizable(false);
    }

    public void setTextAt(int index, String value) {
        textFields[index].setText(value);
    }

    public String getTextAt(int index) {
        return textFields[index].getText();
    }

    /** Set all fields at once. The user may change these values
        (that's the point of presenting the dialog). */
    public void setValues(String[] values) {
        for (int i = 0; i < values.length; ++i) {
            textFields[i].setText(values[i]);
        }
    }

    /** Show the dialog as document-modal, and return the selected
        values (as strings). Return null if the dialog was closed
        abnormally. */
    public String[] showModalStrings() {
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        pack();
        setVisible(true);
        if (pressedOK) {
            String[] output = new String[textFields.length];
            for (int i = 0; i < textFields.length; ++i) {
                output[i] = getTextAt(i);
            }
            return output;
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        StringArrayDialog dog = new StringArrayDialog
            (null, new String[] { "Row one", "Row two", "Row three" },
             new String[] { "Value one", "Value two", "Value three" },
             "<html><body width=\"100 px\">"
            + "This is some text, and some more text. "
            + "This is some text, and some more text. "
            + "</body></html>"
             );
        String[] values = dog.showModalStrings();
        System.out.println(Arrays.toString(values));
    }
}
