package gov.nist.pededitor;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/** Generic dialog that presents several rows of labels and
    corresponding text boxes and returns the array of text box text
    values once the user presses "OK". */
public class TableDialog extends JDialog {
    private static final long serialVersionUID = -8184443211996144705L;

    boolean pressedOK = false;
    JButton okButton;
    JTable table;

    TableDialog(Frame owner, Object[][] data, String[] columnNames) {
        super(owner, "Edit values", false);

        JPanel contentPane = (JPanel) getContentPane();
        
        table = new JTable(data, columnNames) {
                private static final long serialVersionUID = -5653753441232401173L;

                @Override public Class<?> getColumnClass(int c) {
                    return Double.class;
                }
            };
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setPreferredScrollableViewportSize(new Dimension(350, 40));

        table.setFillsViewportHeight(true);


        JScrollPane scrollPane = new JScrollPane(table);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        okButton = new JButton(new AbstractAction("OK") {
                private static final long serialVersionUID = -8082661716737814979L;

                @Override public void actionPerformed(ActionEvent e) {
                    TableDialog.this.pressedOK = true;
                    setVisible(false);
                }
            });

        contentPane.add(okButton, BorderLayout.PAGE_END);
        getRootPane().setDefaultButton(okButton);
    }

    public int getRowCount() {
        return table.getRowCount();
    }

    public int getColumnCount() {
        return table.getColumnCount();
    }

    public Object getValueAt(int i, int j) {
        return table.getValueAt(i,j);
    }

    /** Show the dialog as document-modal, and return the selected
        values (as strings). Return null if the dialog was closed
        abnormally. */
    public Object[][] showModal() {
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        pack();
        setVisible(true);
        if (pressedOK) {
            Object[][] output = new Object[getRowCount()][getColumnCount()];
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
        TableDialog dog = new TableDialog
            (null,
             new Object[][]
                {{ "Value one", "Value two", "Value three" },
                 { "Value one-2", "Value two-2", "Value three-2" }},
             new String[] { "Column one", "Column two", "Column three" });
        Object[][] values = dog.showModal();
        System.out.println(Arrays.toString(values));
    }
}
