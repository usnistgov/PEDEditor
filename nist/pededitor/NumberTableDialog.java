/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Generic dialog that presents several rows of labels and
    corresponding text boxes and returns the array of text box text
    values once the user presses "OK". */
public class NumberTableDialog extends JDialog {
    private static final long serialVersionUID = -8184443211996144705L;

    boolean pressedOK = false;
    JButton okButton;
    NumberField[][] table;
    JPanel panelBeforeOK = new JPanel();

    NumberTableDialog(Frame owner, int rowCnt, int colCnt, String[] rowNames,
                      String[] columnNames, String intro) {
        super(owner, "Edit values", false);
        table = new NumberField[rowCnt][colCnt];
        GridBagUtil gb = new GridBagUtil(getContentPane());

        if (intro != null) {
            gb.centerAndEndRow(new JLabel(intro));
            gb.endRowWith(Box.createVerticalStrut(12 /* pixels */));
        }

        if (columnNames != null) {
            if (rowNames != null) {
                gb.addWest(new JLabel());
            }
            for (int j = 0; j < columnNames.length; ++j) {
                JLabel f = new JLabel(columnNames[j]);
                if (j < columnNames.length - 1) {
                    gb.addWest(f);
                } else {
                    gb.endRowWith(f);
                }
            }
        }

        for (int i = 0; i < rowCnt; ++i) {
            if (rowNames != null) {
                gb.addEast(new JLabel(rowNames[i]));
            }
            for (int j = 0; j < colCnt; ++j) {
                NumberField f = new NumberField(20);
                table[i][j] = f;
                if (j < colCnt - 1) {
                    gb.addWest(f);
                } else {
                    gb.endRowWith(f);
                }
            }
        }

        gb.endRowWith(panelBeforeOK);

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

    NumberTableDialog(Frame owner, double[][] data, String[] rowNames,
                      String[] columnNames, String intro) {
        this(owner, data.length, data[0].length, rowNames, columnNames, intro);
        int rowCnt = data.length;
        int colCnt = data[0].length;
        for (int i = 0; i < rowCnt; ++i) {
            for (int j = 0; j < colCnt; ++j) {
                setValueAt(i,j,data[i][j]);
            }
        }
    }

    NumberTableDialog(Frame owner, String[][] data, String[] rowNames,
                      String[] columnNames, String intro) {
        this(owner, data.length, data[0].length, rowNames, columnNames, intro);
        int rowCnt = data.length;
        int colCnt = data[0].length;
        for (int i = 0; i < rowCnt; ++i) {
            for (int j = 0; j < colCnt; ++j) {
                setValueAt(i,j,data[i][j]);
            }
        }
    }

    public int getRowCount() {
        return table.length;
    }

    public int getColumnCount() {
        return table[0].length;
    }

    public double getValueAt(int row, int col) {
        return table[row][col].getValue();
    }

    public String getTextAt(int row, int col) {
        return table[row][col].getText();
    }

    public void setValueAt(int row, int col, double v) {
        table[row][col].setValue(v);
    }

    public void setValueAt(int row, int col, String v) {
        table[row][col].setText(v);
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

    /** Return true if at least one entry includes a percent sign. */
    public boolean havePercentage() {
        for (int i = 0; i < getRowCount(); ++i) {
            for (int j = 0; j < getColumnCount(); ++j) {
                if (table[i][j].getText().contains("%")) {
                    return true;
                }
            }
        }
        return false;
    }

    public double[][] getValues() {
        double[][] res = new double[getRowCount()][getColumnCount()];
        for (int i = 0; i < getRowCount(); ++i) {
            for (int j = 0; j < getColumnCount(); ++j) {
                res[i][j] = getValueAt(i,j);
            }
        }
        return res;
    }

    public void setValues(double[][] values) {
        for (int i = 0; i < values.length; ++i) {
            for (int j = 0; j < values[0].length; ++j) {
                setValueAt(i, j, values[i][j]);
            }
        }
    }

    /** Show the dialog as document-modal, and return the selected
        values. Return null if the dialog was closed abnormally. */
    public double[][] showModal() throws NumberFormatException {
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        pack();
        setVisible(true);
        if (pressedOK) {
            return getValues();
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
                 new String[] { "Row one", "Row two" },
             new String[] { "Column one", "Column two", "Column three" }, "Intro");
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
