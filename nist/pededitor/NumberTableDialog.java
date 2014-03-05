/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

/** Generic dialog that presents several rows of labels and
    corresponding text boxes and returns the array of text box text
    values once the user presses "OK". */
public class NumberTableDialog extends JDialog {
    private static final long serialVersionUID = -8184443211996144705L;

    boolean pressedOK = false;
    JButton okButton;
    NumberField[][] table;

    NumberTableDialog(Frame owner, double[][] data, String[] columnNames) {
        super(owner, "Edit values", false);
        table = new NumberField[data.length][data[0].length];
        GridBagUtil gb = new GridBagUtil(getContentPane());

        for (int j = 0; j < columnNames.length; ++j) {
            JLabel f = new JLabel(columnNames[j]);
            if (j < columnNames.length - 1) {
                gb.addWest(f);
            } else {
                gb.endRowWith(f);
            }
        }

        for (int i = 0; i < data.length; ++i) {
            int cols = data[i].length;
            for (int j = 0; j < cols; ++j) {
                NumberField f = new NumberField(20);
                f.setValue(data[i][j]);
                table[i][j] = f;
                if (j < cols - 1) {
                    gb.addWest(f);
                } else {
                    gb.endRowWith(f);
                }
            }
        }

        okButton = new JButton(new AbstractAction("OK") {
                private static final long serialVersionUID = -8082661716737814979L;

                @Override public void actionPerformed(ActionEvent e) {
                    NumberTableDialog.this.pressedOK = true;
                    setVisible(false);
                }
            });

        gb.centerAndEndRow(okButton);
        getRootPane().setDefaultButton(okButton);
    }

    public int getRowCount() {
        return table.length;
    }

    public int getColumnCount() {
        return table[0].length;
    }

    public double getValueAt(int i, int j) {
        return table[i][j].getValue();
    }

    /** Set whether to show the given field as a percentage. */
    public void setPercentage(int row, int col, boolean b) {
        table[row][col].setPercentage(b);
    }

    /** Set whether to show all fields as percentages. */
    public void setPercentage(boolean b) {
        for (int i = 0; i < getRowCount(); ++i) {
            for (int j = 0; j < getColumnCount(); ++j) {
                table[i][j].setPercentage(b);
            }
        }
    }

    /** Show the dialog as document-modal, and return the selected
        values (as strings). Return null if the dialog was closed
        abnormally. */
    public double[][] showModal() {
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        pack();
        setVisible(true);
        if (pressedOK) {
            double[][] output = new double[getRowCount()][getColumnCount()];
            for (int i = 0; i < getRowCount(); ++i) {
                for (int j = 0; j < getColumnCount(); ++j) {
                    output[i][j] = getValueAt(i,j);
                }
            }
            return output;
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        NumberTableDialog dog = new NumberTableDialog
            (null,
             new double[][]
                {{ 1.0/3, 0.5, 0.7 },
                 { 0.2, 0.411111356, 0.9234729835888 }},
             new String[] { "Column one", "Column two", "Column three" });
        dog.setPercentage(true);
        double[][] values = dog.showModal();
        if (values != null) {
            for (int i = 0; i < values.length; ++i) {
                for (int j = 0; j < values[i].length; ++j) {
                    System.out.format("%15.8g ", values[i][j]);
                }
                System.out.println();
            }
        } else {
            System.out.println("Canceled.");
        }
    }
}
