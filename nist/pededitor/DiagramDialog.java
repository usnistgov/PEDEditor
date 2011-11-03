package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** GUI for selecting a DiagramType. */
public class DiagramDialog extends JDialog
    implements ActionListener  {
    DiagramType selectedDiagram = null;
    boolean pressedOK = false;
    JButton okButton;
    protected List<DiagramSelectionEventListener> diagramSelectedListeners
        = new ArrayList<DiagramSelectionEventListener>();

    abstract class DiagramDialogAction extends AbstractAction {
        DiagramDialogAction(String name) {
            super(name);
        }
    }

    DiagramType getSelectedDiagram() {
        return selectedDiagram;
    }

    public synchronized void addDiagramSelectionEventListener
        (DiagramSelectionEventListener listener) {
        diagramSelectedListeners.add(listener);
    }
    public synchronized void removeDiagramSelectionEventListener
        (DiagramSelectionEventListener listener) {
        diagramSelectedListeners.remove(listener);
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
                @Override
                    public void actionPerformed(ActionEvent e) {
                    DiagramDialog.this.pressedOK = true;
                    DiagramSelectionEvent se
                        = new DiagramSelectionEvent(DiagramDialog.this);
                    for (DiagramSelectionEventListener l : diagramSelectedListeners) {
                        l.diagramSelected(se);
                    }
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

    /** Listen for OK button press events, which indicate that the
        user has accepted a diagram type. To find out what that type
        is, call getSelectedDiagram(). */
    void addSelectionListener(ActionListener listener) {
        okButton.addActionListener(listener);
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