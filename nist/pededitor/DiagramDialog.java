package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

/** Class for selecting a diagram type. */
public class DiagramDialog extends JDialog
    implements ActionListener  {
    DiagramType selectedDiagram = null;
    boolean pressedOK = false;

    abstract class DiagramDialogAction extends AbstractAction {
        DiagramDialogAction(String name) {
            super(name);
        }
    }

    DiagramDialog(Frame owner, int vertexCnt, DiagramType selected) {
        super(owner, "Select Diagram Type", true);

        DiagramType[] diagrams = (vertexCnt == -1)
            ? DiagramType.values() : DiagramType.values(vertexCnt);
        ButtonGroup group = new ButtonGroup();
        JPanel radioPanel = (JPanel) getContentPane();
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
        
        radioPanel.add(new JButton(new DiagramDialogAction("OK") {

			@Override
			public void actionPerformed(ActionEvent e) {
                DiagramDialog.this.pressedOK = true;
                DiagramDialog.this.dispose();
			}
        }));
    }

    /** Show a dialog with the given frame as owner.

        @return the user's choice of diagram type, or null if no choice was selected.
    */
    public static DiagramType show(Frame owner) {
        return show(owner, DiagramType.values()[0]);
    }

    /** Show a dialog with the given frame as owner and the given
        initially selected diagram type.

        @return the user's choice of diagram type, or null if no choice was selected.
    */
    public static DiagramType show(Frame owner, DiagramType selected) {
        return show(owner, selected, -1);
    }

    /** Show a dialog with the given frame as owner and the given
        initially selected diagram type. Only show diagrams that are
        consistent with the given number of selected vertices.

        @return the user's choice of diagram type, or null if no choice was selected.
    */
    public static DiagramType show(Frame owner, DiagramType selected,
                                   int vertexCnt) {
        DiagramDialog dialog = new DiagramDialog(owner, vertexCnt, selected);
        dialog.pack();
        dialog.setVisible(true);
        return dialog.pressedOK ? dialog.selectedDiagram : null;
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
        DiagramType t = show(null);
        System.out.println("You selected " + t);
    }
   
}