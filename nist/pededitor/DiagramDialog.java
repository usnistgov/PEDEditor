package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

/** GUI for selecting a DiagramType. */
public class DiagramDialog extends JDialog
    implements ActionListener  {
	private static final long serialVersionUID = -1082463709970796523L;
	DiagramType selectedDiagram = null;
    boolean pressedOK = false;
    JButton okButton;

    abstract class DiagramDialogAction extends AbstractAction {
		private static final long serialVersionUID = -7296210099594024294L;

		DiagramDialogAction(String name) {
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
        this(owner, image, DiagramType.values()[0]);
    }

    DiagramDialog(Frame owner, BufferedImage image, DiagramType selected) {
        super(owner, "Select Diagram Type", false);
        selectedDiagram = selected;

        DiagramType[] diagrams = DiagramType.values();
        ButtonGroup group = new ButtonGroup();

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new GridLayout(0,1));
        
        for (DiagramType diagram : diagrams) {
            JRadioButton button = new JRadioButton
                (diagram.getDescription(),
                 diagram.getIcon(),
                 (diagram == selected));
            button.setActionCommand(diagram.toString());
            group.add(button);
            radioPanel.add(button);
            button.addActionListener(this);
        }

        okButton = new JButton(new DiagramDialogAction("OK") {
			private static final long serialVersionUID = -4517446754034705381L;

				@Override
                    public void actionPerformed(ActionEvent e) {
                    DiagramDialog.this.pressedOK = true;
                    DiagramDialog.this.setVisible(false);
                }
            });
        
        radioPanel.add(okButton);

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
        return pressedOK ? getSelectedDiagram() : null;
    }

    public void actionPerformed(ActionEvent e) {
        String what = e.getActionCommand();
        for (DiagramType diagram: DiagramType.values()) {
            if (diagram.toString() == what) {
                selectedDiagram = diagram;
            }
        }
    }

    public static void main(String[] args) {
        DiagramType t = (new DiagramDialog(null)).showModal();
        System.out.println("You selected " + t);
    }
   
}