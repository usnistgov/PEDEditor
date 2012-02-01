package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** A label and anGUI for selecting a DiagramType. */
class StepDialog extends JDialog {
    JButton okButton;
    JLabel label = new JLabel();

    public JLabel getLabel() {
        return label;
    }

    public Action getAction() {
        return okButton.getAction();
    }

    StepDialog(Frame owner, String title, AbstractAction action) {
        super(owner, title, false);
        okButton = new JButton(action);

        JPanel pane = (JPanel) getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        pane.add(label);
        pane.add(okButton);
    }
}