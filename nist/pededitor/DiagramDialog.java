/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** GUI for selecting a DiagramType. */
public class DiagramDialog extends JDialog
    implements ActionListener  {
    private static final long serialVersionUID = -1082463709970796523L;
    DiagramType selectedDiagram = null;

    abstract class Action extends AbstractAction {
        private static final long serialVersionUID = -7296210099594024294L;

        Action(String name) {
            super(name);
        }
    }

    DiagramType getSelectedDiagram() {
        return selectedDiagram;
    }

    DiagramDialog(Frame owner) {
        this(owner, null);
    }

    DiagramDialog(Frame owner, BufferedImage image) {
        super(owner, "Select Diagram Type", false);

        DiagramType[] diagrams = DiagramType.values();
        ButtonGroup group = new ButtonGroup();

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new GridLayout(0,1));
        
        for (DiagramType diagram : diagrams) {
            JButton button = new JButton
                (diagram.getDescription(),
                 diagram.getIcon());
            button.setActionCommand(diagram.toString());
            button.setHorizontalAlignment(SwingConstants.LEFT);
            group.add(button);
            radioPanel.add(button);
            button.addActionListener(this);
        }

        if (image == null) {
            setContentPane(radioPanel);
        } else {
            JPanel pane = (JPanel) getContentPane();
            pane.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 10));
            pane.add(radioPanel);

            ScaledImagePane imagePane = new ScaledImagePane();
            imagePane.setPreferredSize(new Dimension(350, 500));
            imagePane.setImage(image);

            pane.add(imagePane);
        }
    }

    /** Show the dialog as document-modal, and return the DiagramType
        selected. Return null if the dialog was closed abnormally. */
    public DiagramType showModal() {
        pack();
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        setVisible(true);
        return getSelectedDiagram();
    }

    @Override public void actionPerformed(ActionEvent e) {
        String what = e.getActionCommand();
        for (DiagramType diagram: DiagramType.values()) {
            if (diagram.toString() == what) {
                selectedDiagram = diagram;
                DiagramDialog.this.setVisible(false);
            }
        }
    }

    public static void main(String[] args) {
        DiagramType t = (new DiagramDialog(null)).showModal();
        System.out.println("You selected " + t);
    }
   
}
