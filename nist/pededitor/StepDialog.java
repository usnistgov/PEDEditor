package gov.nist.pededitor;

import java.awt.Frame;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A label and anGUI for selecting a DiagramType. */
class StepDialog extends JDialog {
    private static final long serialVersionUID = -1250462809033476919L;

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