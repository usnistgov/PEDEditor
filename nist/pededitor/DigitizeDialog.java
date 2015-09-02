/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.function.DoubleUnaryOperator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/** GUI for selecting where data should come from/go to and which
    variables should be input/output. */
public class DigitizeDialog extends JDialog {

    private static final long serialVersionUID = -665966463000978347L;

    public static enum SourceType { FILE, CLIPBOARD };

    public static class VariablePanel extends JPanel {
        private static final long serialVersionUID = 3321061886394879274L;

        JLabel variableLabel = new JLabel();
        VariableSelector variable = new VariableSelector();
        FunctionSelector function = new FunctionSelector();

        {
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            variableLabel.setLabelFor(variable);
            add(variableLabel);
            add(variable);
            JLabel lab = new JLabel("Transform");
            lab.setLabelFor(function);
            add(lab);
            add(function);
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
    JCheckBox commented
        = new JCheckBox("Include comments in output");
    JLabel sigLabel = new JLabel("Sig. figs");
    JTextField sigValue = new JTextField("16");
    {
        sigValue.setToolTipText
            ("More is better if you want to reimport these values later.");
    }
    
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

        gb.centerAndEndRow(commented);
        gb.addEast(sigLabel);
        gb.endRowWith(sigValue);
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

    /** Set to false to hide the "Include comments in output" checkbox. */
    public void setCommentedCheckboxVisible(boolean v) {
        commented.setVisible(v);
    }

    public boolean isCommented() {
        return commented.isSelected();
    }

    public void setExport(boolean v) {
        setCommentedCheckboxVisible(v);
        sigLabel.setVisible(v);
        sigValue.setVisible(v);
    }

    public int getSigFigs() {
        try {
            int x = Integer.parseInt(sigValue.getText());
            return (x > 16) ? 16
                : (x < 1) ? 1 : x;
        } catch (NumberFormatException x) {
            return 16;
        }
    }

    public LinearAxis getVariable(int columnNo, ArrayList<LinearAxis> axes) {
        return getPanel(columnNo).variable.getSelected(axes);
    }

    public void setVariable(int columnNo, LinearAxis axis) {
        getPanel(columnNo).variable.setSelected(axis);
    }

    public DoubleUnaryOperator getFunction(int columnNo) {
        return getPanel(columnNo).function.getSelected();
    }

    public void setFunction(int columnNo, StandardDoubleUnaryOperator f) {
        getPanel(columnNo).function.setSelected(f);
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

    /* defaults are the values that are to be initiially selected for
       the different variable columns. But if the previously selected
       values, if any, for these columns are still accessible, then
       those previous values take precedence over v1 and v2. */
    public void setAxes(ArrayList<LinearAxis> axes,
                        LinearAxis[] defaults) {
        int columnNo = -1;
        for (VariablePanel p: variablePanels) {
            ++columnNo;
            LinearAxis v = getVariable(columnNo, axes);
            if (v == null) {
                v = defaults[columnNo];
            }
            p.variable.setAxes(axes);
            setVariable(columnNo, v);
        }
    }
}
