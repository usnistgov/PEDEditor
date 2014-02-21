/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class Viewer extends Editor {
    static ArrayList<BasicEditor> openEditors = new ArrayList<>();

    public Viewer() {
        openEditors.add(this);
    }

    void closeAll() {
        // Duplicate openEditors so we don't end up iterating through a
        // list that is simultaneously being modified.
        for (BasicEditor e: new ArrayList<>(openEditors)) {
            e.close();
        }
    }

    @Override public void close() {
        super.close();
        openEditors.remove(this);
    }

    @SuppressWarnings("serial") protected void init() {
        alwaysConvertLabels = true;
        setRightClickMenu(new ViewerRightClickMenu(this));
        
        // Cut out all the functions that the viewer doesn't need.
        EditFrame ef = editFrame;
        ef.setAlwaysOnTop(true);
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
        JMenuItem mnExitAll = new JMenuItem
            (new Action("Exit all") {
                    @Override public void actionPerformed(ActionEvent e) {
                        closeAll();
                    }
                });
        mnExitAll.setMnemonic(KeyEvent.VK_A);
        ef.mnFile.add(mnExitAll);
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
                          + "mouse to find special points and curves, which will be "
                          + "marked with a second pair of crosshairs."
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
        for (String var: new String[] {"page X", "page Y"}) {
            try {
                removeVariable(var);
            } catch (CannotDeletePrincipalVariableException
                     |NoSuchVariableException e1) {
                // OK, let it be
            }
        }
        setSaveNeeded(false);
        int otherEditorCnt = BasicEditor.getOpenEditorCnt() - 1;
        ef.setLocation(15 * otherEditorCnt, 15 * otherEditorCnt);
        initializeGUI();
        ef.setVertexInfoVisible(false);
        bestFit();
        ef.toFront();
    }

    @Override public void paintEditPane(Graphics g) {
        if (paintCnt == 0) {
            editFrame.setAlwaysOnTop(false);
        }
        super.paintEditPane(g);
    }
}
