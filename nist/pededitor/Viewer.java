package gov.nist.pededitor;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class Viewer extends Editor {
    @SuppressWarnings("serial")
	protected void init() {
        setRightClickMenu(new ViewerRightClickMenu(this));
        
        // Cut out all the functions that the viewer doesn't need.
        EditFrame ef = editFrame;
        ef.setNewDiagramVisible(false);
        ef.setOpenVisible(false);
        ef.setReloadVisible(false);
        ef.setEditable(false);
        ef.editingEnabled.setVisible(false);
        ef.mnTags.setVisible(false);
        ef.mnKeys.setVisible(false);
        ef.mnExportText.setVisible(false);
        ef.mnCopyFormulas.setVisible(false);
        ef.mnJumpToSelection.setVisible(false);
        ef.mnProperties.setVisible(false);
        ef.shortHelpFile = "viewhelp1.html";
        ef.helpAboutFile = "viewabout.html";
        ef.mnView.add(new Action("Hints..") {
                @Override public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog
                        (editFrame,
                         htmlify
                         ("<ol>"
                          + "<li>You can see more functions and their short-cut keys "
                          + "by right-clicking (pressing the right mouse button) "
                          + " while the mouse is inside the diagram."
                          + "<li>To zoom in, drag the mouse (move the mouse while holding down the left mouse button)."
                          + "<li>Hold down the <code>Shift</code> key while moving the "
                          + "mouse to make the mouse follow curves and special points."
                          + "</ol>"));
                }
            });

        for (AbstractAction act: new AbstractAction[]
            { (AbstractAction) ef.mnUnstickMouse.getAction(),
              ef.actColor,
              ef.actRemoveSelection,
              ef.actRemoveAll,
              ef.actMoveSelection,
              ef.actEditSelection,
              ef.actResetToDefault,
              ef.actMakeDefault,
              ef.actMovePoint,
              ef.actMoveRegion,
              ef.actAddVertex,
              ef.actAddAutoPositionedVertex,
              ef.actText,
              ef.actLeftArrow,
              ef.actRightArrow,
              ef.actRuler,
              ef.actTieLine,
              ef.actMoveSelection,
              ef.actCopy,
              ef.actCopyRegion
            }) {
            // Make these actions vanish from the interface.
            act.setEnabled(false);
            setVisible(act, false);
        }

        for (AbstractAction act: new AbstractAction[]
            { (AbstractAction) ef.mnUnstickMouse.getAction(),
                 ef.actAutoPosition,
                 ef.actNearestPoint,
                 ef.actNearestCurve,
            }) {
            // Remove the actions from the interface, but there's no
            // harm in leaving them enabled.
            setVisible(act, false);
        }
            
        detachOriginalImage();
        setEditable(false);
        try {
            removeVariable("page X");
        } catch (CannotDeletePrincipalVariableException
                 |NoSuchVariableException e1) {
            // OK, let it be
        }
        try {
            removeVariable("page Y");
        } catch (CannotDeletePrincipalVariableException
                 |NoSuchVariableException e1) {
            // OK, let it be
        }
        setSaveNeeded(false);
        int otherEditorCnt = Editor.getOpenEditorCnt() - 1;
        ef.setLocation(10 * otherEditorCnt, 10 * otherEditorCnt);
        initializeGUI();
        ef.setVertexInfoVisible(false);
        bestFit();
        ef.toFront();
    }
}
