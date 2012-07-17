package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/** GUI for changing tickLeft/tickRight, startArrow, endArrow,
    labelAnchor, and multiplier. */

public class RulerDialog extends JDialog {
    private static final long serialVersionUID = -2793746548149999804L;

    abstract class Action extends AbstractAction {
        private static final long serialVersionUID = -6218060076708914216L;

        Action(String name) {
            super(name);
        }
    }

    class LabelAnchorAction extends AbstractAction {
        private static final long serialVersionUID = -3652297861974269074L;

        LinearRuler.LabelAnchor labelAnchor;

        LabelAnchorAction(String name, LinearRuler.LabelAnchor anchor) {
            super(name);
            this.labelAnchor = anchor;
        }

        @Override public void actionPerformed(ActionEvent e) {
            RulerDialog.this.labelAnchor = labelAnchor;
        }
    }

    class LabelAnchorButton extends JRadioButton {
        private static final long serialVersionUID = 7463710339083034076L;

        LabelAnchorButton(String caption, LinearRuler.LabelAnchor anchor) {
            super(new LabelAnchorAction(caption, anchor));
            setSelected(anchor == RulerDialog.this.labelAnchor);
            labelAnchorGroup.add(this);
        }
    }

    JCheckBox tickLeft = new JCheckBox("Left-side tick marks");
    JCheckBox tickRight = new JCheckBox("Right-side tick marks");
    JCheckBox tickTypeV = new JCheckBox("V-shaped tick marks");

    JCheckBox startArrow = new JCheckBox("Start arrow");
    JCheckBox endArrow = new JCheckBox("End arrow");

    JCheckBox suppressStartTick = new JCheckBox("Suppress start tick");
    JCheckBox suppressStartLabel = new JCheckBox("Suppress start label");
    JCheckBox suppressEndTick = new JCheckBox("Suppress end tick");
    JCheckBox suppressEndLabel = new JCheckBox("Suppress end label");
    JCheckBox suppressSmallTicks = new JCheckBox("Suppress small ticks");

    JCheckBox showPercentages = new JCheckBox("Use percentages in labels");
    JTextField tickPadding = new JTextField("0", 10);
    JTextField textAngle = new JTextField("0", 10);
    JComboBox<String> variable;

    boolean pressedOK = false;
    LinearRuler.LabelAnchor labelAnchor = null;
    ButtonGroup labelAnchorGroup = new ButtonGroup();
    JButton okButton = new JButton(new Action("OK") {
            private static final long serialVersionUID = 54912002834702747L;

            @Override public void actionPerformed(ActionEvent e) {
                pressedOK = true;
                setVisible(false);
            }
        });

    RulerDialog(Frame owner, String title) {
        super(owner, "Edit Ruler", true);
        JPanel contentPane = (JPanel) getContentPane();
        GridBagUtil cpgb = new GridBagUtil(contentPane);

        cpgb.addWest(new JLabel("Tick labels:"));

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);
            gb.addWest
                (new LabelAnchorButton
                 ("None", LinearRuler.LabelAnchor.NONE));
            gb.addWest
                (new LabelAnchorButton
                 ("On left", LinearRuler.LabelAnchor.LEFT));
            gb.endRowWith
                (new LabelAnchorButton
                 ("On right", LinearRuler.LabelAnchor.RIGHT));
            cpgb.endRowWith(panel);
        }

        {
            variable = new JComboBox<String>();
            JLabel variableLabel = new JLabel("Display variable/component:");
            variableLabel.setLabelFor(variable);
            cpgb.addWest(variableLabel);
            cpgb.endRowWith(variable);
        }

        JLabel textAngleLabel = new JLabel
            ("Label angle with respect to axis:");
        textAngleLabel.setLabelFor(textAngle);
        cpgb.addWest(textAngleLabel);
        cpgb.addWest(textAngle);
        cpgb.endRowWith(new JLabel("degrees"));

        JLabel tickPaddingLabel = new JLabel("Extra padding between ticks:");
        tickPaddingLabel.setLabelFor(tickPadding);
        cpgb.addWest(tickPaddingLabel);
        cpgb.endRowWith(tickPadding);
        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);
            gb.addWest(tickPaddingLabel);
            gb.endRowWith(tickPadding);
            cpgb.endRowWith(panel);
        }

        cpgb.addWest(tickLeft);
        cpgb.endRowWith(tickRight);
        cpgb.addWest(tickTypeV);
        cpgb.endRowWith(showPercentages);

        cpgb.addWest(startArrow);
        cpgb.endRowWith(endArrow);
        cpgb.addWest(suppressStartTick);
        cpgb.endRowWith(suppressStartLabel);
        cpgb.addWest(suppressEndTick);
        cpgb.endRowWith(suppressEndLabel);
        cpgb.endRowWith(suppressSmallTicks);

        cpgb.centerAndEndRow(okButton);
        getRootPane().setDefaultButton(okButton);
        pack();
    }

    RulerDialog(Frame owner, String title, LinearRuler ruler,
                ArrayList<LinearAxis> axes) {
        this(owner, title);
        set(ruler, axes);
    }

    public void set(LinearRuler ruler, ArrayList<LinearAxis> axes) {
        labelAnchor = ruler.labelAnchor;

        String[] variables = new String[axes.size()];
        variable.removeAllItems();
        int i = -1;
        for (Axis axis: axes) {
            ++i;
            variables[i] = (String) axis.name;
        }
        Arrays.sort(variables);

        i = -1;
        String name = (String) ruler.axis.name;
        for (String s: variables) {
            ++i;
            variable.addItem(s);
            if (s.equals(name)) {
                variable.setSelectedIndex(i);
            }
        }

        tickLeft.setSelected(ruler.tickLeft);
        tickRight.setSelected(ruler.tickRight);
        tickTypeV.setSelected(ruler.tickType == LinearRuler.TickType.V);
        startArrow.setSelected(ruler.startArrow);
        endArrow.setSelected(ruler.endArrow);
        suppressStartTick.setSelected(ruler.suppressStartTick);
        suppressStartLabel.setSelected(ruler.suppressStartLabel);
        suppressEndTick.setSelected(ruler.suppressEndTick);
        suppressEndLabel.setSelected(ruler.suppressEndLabel);

        boolean showPct;

        if (ruler.multiplier == 1.0) {
            showPct = false;
        } else if (ruler.multiplier == 100.0) {
            showPct = true;
        } else {
            // Multiplier values other than 1 or 100 are not supported
            // by this dialog.
            throw new IllegalStateException("Multiplier = " + ruler.multiplier);
        }
        showPercentages.setSelected(showPct);

        suppressSmallTicks.setSelected(ruler.tickDelta == 0);
        textAngle.setText(String.format("%7.3f", ruler.textAngle * (-180 / Math.PI)));
        tickPadding.setText(ContinuedFraction.toString(ruler.tickPadding,
                                                       false));
    }

    /** Show the dialog as document-modal. If the dialog is closed
        normally, then update "dest" with the new values and return
        true. Otherwise, make no changes to "dest" and return false. */
    public boolean showModal(LinearRuler dest, ArrayList<LinearAxis> axes) {
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        set(dest, axes);
        setVisible(true);
        if (!pressedOK) {
            return false;
        }

        dest.tickLeft = tickLeft.isSelected();
        dest.tickRight = tickRight.isSelected();
        dest.startArrow = startArrow.isSelected();
        dest.endArrow = endArrow.isSelected();
        dest.suppressStartTick = suppressStartTick.isSelected();
        dest.suppressStartLabel = suppressStartLabel.isSelected();
        dest.suppressEndTick = suppressEndTick.isSelected();
        dest.suppressEndLabel = suppressEndLabel.isSelected();
        dest.multiplier = showPercentages.isSelected() ? 100.0 : 1.0;
        dest.labelAnchor = labelAnchor;
        dest.tickType = tickTypeV.isSelected() ? LinearRuler.TickType.V
            : LinearRuler.TickType.NORMAL;
        String padStr = tickPadding.getText();
        try {
            dest.tickPadding = ContinuedFraction.parseDouble(padStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog
                (getOwner(),
                 "Warning: could not parse tick padding value '"
                 + padStr + "'");
        }

        String angleStr = textAngle.getText();
        try {
            double degangle = ContinuedFraction.parseDouble(angleStr);
            double odegangle = -dest.textAngle * 180 / Math.PI;
            if (Math.abs(degangle - odegangle) > 1e-3) {
                dest.textAngle = -degangle * Math.PI / 180;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog
                (getOwner(),
                 "Warning: could not parse text angle value '"
                 + angleStr + "'");
        }

        String name = (String) variable.getSelectedItem();
        for (LinearAxis axis: axes) {
            if (name.equals(axis.name)) {
                dest.axis = axis;
                break;
            }
        }
        if (suppressSmallTicks.isSelected()) {
            dest.tickDelta = 0;
        } else if (dest.tickDelta == 0) {
            dest.tickDelta = Double.NaN;
        }
        return true;
    }
}
