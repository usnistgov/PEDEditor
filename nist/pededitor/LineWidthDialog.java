/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/** GUI for entering a chemical formula. */
@SuppressWarnings("serial")
public class LineWidthDialog extends JDialog {
    protected JLabel descr = new JLabel
        ("Line width:");
    protected NumberField valueField = new NumberField(20);
    protected ButtonGroup unitsGroup = new ButtonGroup();
    protected JRadioButton pageUnits = new JRadioButton
        ("Page units");
    protected JRadioButton userUnits = new JRadioButton
        ("User units");
    protected JPanel unitsPanel = new JPanel();
    {
        pageUnits.setSelected(true);
        unitsGroup.add(pageUnits);
        unitsGroup.add(userUnits);
    }

    public boolean isUserUnits() {
        return userUnits.isSelected();
    }

    public void setUserUnits(boolean v) {
        (v ? userUnits : pageUnits).setSelected(true);
    }

    public void setUnitsVisible(boolean v) {
        unitsPanel.setVisible(v);
    }

    public NumberField getValueField() {
        return valueField;
    }

    public double getValue() throws NumberFormatException {
        return valueField.getValue();
    }

    public void setValue(double d) {
        valueField.setValue(d);
    }

    protected transient boolean pressedOK = false;
    protected JButton okButton =  new JButton
        (new AbstractAction("OK") {
                @Override public void actionPerformed(ActionEvent e) {
                    normalExit();
                }
            });
    { okButton.setFocusable(false); }

    public LineWidthDialog(JFrame parent) {
        super(parent, "Set line width");
        
        GridBagUtil gb = new GridBagUtil(getContentPane());

        gb.addEast(descr);
        gb.endRowWith(valueField);
        gb.endRowWith(unitsPanel);

        {
            GridBagUtil gb2 = new GridBagUtil(unitsPanel);
            gb2.addEast(pageUnits);
            gb2.endRowWith(userUnits);
        }
        gb.centerAndEndRow(okButton);
        getRootPane().setDefaultButton(okButton);
        setResizable(false);
    }

    public void normalExit() {
        pressedOK = true;
        setVisible(false);
    }

    /** Show the dialog as document-modal. Return true if the user
        pressed OK. */
    public boolean showModal() {
        pack();
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        pressedOK = false;
        setVisible(true);
        return pressedOK;
    }

    public static void main(String[] args) {
        LineWidthDialog dog = new LineWidthDialog(null);
        if (dog.showModal()) {
            System.out.println("You selected " + dog.getValue() + " and "
                               + dog.isUserUnits());
        }
    }
}
