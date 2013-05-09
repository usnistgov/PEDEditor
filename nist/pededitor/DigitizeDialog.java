package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/** GUI for selecting where data should come from/go to and which
    variables should be input/output. */
public class DigitizeDialog extends JDialog {

    private static final long serialVersionUID = -665966463000978347L;

    public static enum SourceType { FILE, CLIPBOARD };

    public static class VariablePanel extends JPanel {
        private static final long serialVersionUID = 3321061886394879274L;

        JLabel variableLabel = new JLabel();
        VariableSelector variable = new VariableSelector();

        {
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            variableLabel.setLabelFor(variable);
            add(variableLabel);
            add(variable);
        }

        VariablePanel(String text) {
            variableLabel.setText(text);
        }
    }

    JLabel sourceLabel = new JLabel("Source");
    JRadioButton fileSource = new JRadioButton("File");
    JRadioButton clipboardSource = new JRadioButton("Clipboard");
    ButtonGroup sources = new ButtonGroup();
    JButton okButton =  new JButton(new AbstractAction("OK") {
            private static final long serialVersionUID = -1119773633681425069L;

            @Override public void actionPerformed(ActionEvent e) {
                normalExit();
            }
        });
    VariablePanel[] variablePanels = {
        new VariablePanel("Column 1"),
        new VariablePanel("Column 2") };
    transient boolean pressedOK = false;
    transient boolean packed = false;

    {
        fileSource.setSelected(true);
        sources.add(fileSource);
        sources.add(clipboardSource);

        GridBagUtil gb = new GridBagUtil(getContentPane());
        gb.centerAndEndRow(sourceLabel);
        gb.addWest(fileSource);
        gb.endRowWith(clipboardSource);

        gb.centerAndEndRow(new JLabel("Variables"));
        for (int i = 0; i < variablePanels.length - 1; ++i) {
            gb.addWest(variablePanels[i]);
        }
        gb.endRowWith(variablePanels[variablePanels.length-1]);

        gb.centerAndEndRow(okButton);
        getRootPane().setDefaultButton(okButton);
    }

    public int getVariableCount() {
        return 2;
    }

    public VariablePanel getPanel(int columnNo) {
        if (columnNo < 0 || columnNo >= getVariableCount()) {
            throw new RuntimeException("Invalid column number " + columnNo);
        }
        return variablePanels[columnNo];
    }

    public LinearAxis getVariable(int columnNo, ArrayList<LinearAxis> axes) {
        return getPanel(columnNo).variable.getSelected(axes);
    }

    public void setSelectedVariable(int columnNo, LinearAxis axis) {
        getPanel(columnNo).variable.setSelected(axis);
    }

    public SourceType getSourceType() {
        return fileSource.isSelected() ? SourceType.FILE
            : SourceType.CLIPBOARD;
    }

    public void setSourceType(SourceType v) {
        if (v == SourceType.FILE) {
            fileSource.setSelected(true);
        } else {
            clipboardSource.setSelected(true);
        }
    }

    public JLabel getSourceLabel() {
        return sourceLabel;
    }

    public void normalExit() {
        pressedOK = true;
        setVisible(false);
    }

    /** Show the dialog as document-modal. Return false if the dialog
        was closed abnormally. */
    public boolean showModal() {
        if (!packed) {
            pack();
            packed = true;
        }
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        setVisible(true);
        return pressedOK;
    }

    public void setAxes(ArrayList<LinearAxis> axes) {
        for (VariablePanel p: variablePanels) {
            p.variable.setAxes(axes);
        }
    }
}
