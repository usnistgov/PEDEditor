/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.awt.Graphics;

import javax.jnlp.IntegrationService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class Viewer extends Editor {

    public Viewer() {
        init();
    }

    @Override public Viewer createNew() {
        return new Viewer();
    }

    private void init() {
        alwaysConvertLabels = true;
        setRightClickMenu(new ViewerRightClickMenu(this));
        
        // Cut out all the functions that the viewer doesn't need.
        EditFrame ef = editFrame;
        ef.setAlwaysOnTop(true);
        ef.setNewDiagramVisible(false);
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
              ef.actIsotherm,
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
    }

    @Override void setFileAssociations(boolean askExit) {
        setFileAssociations
            (askExit, "application/x-pedviewer", new String[] { "pedv" });
    }

    @Override public void paintEditPane(Graphics g) {
        if (paintCnt == 0) {
            editFrame.setAlwaysOnTop(false);
        }
        super.paintEditPane(g);
    }

    @Override public void open() {
        showOpenDialog(editFrame, openPEDFilesDialog(editFrame));
    }

    @Override public void run() {
        open();
    }

    @Override String[] pedFileExtensions() {
        return new String[] {"ped", "pedv"};
    }

    @Override String fallbackTitle() {
        return "Phase Equilibria Diagram Viewer";
    }

    @Override void setFileAssociations(boolean askExit, String mime, String[] exts) {
        try {
            IntegrationService is
                = (IntegrationService) ServiceManager.lookup("javax.jnlp.IntegrationService");
            if (askExit) {
                if (is.requestAssociation(mime, exts)) {
                    JOptionPane.showMessageDialog
                        (editFrame,
                         fallbackTitle() + " has been installed. For uninstall instructions, see PED Viewer help menu.",
                         "Installation successful",
                         JOptionPane.PLAIN_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog
                        (editFrame,
                         fallbackTitle() + " may not have successfully registered as the handler for  " +
                         "PED Viewer diagrams. If you are unable to view diagrams, please contact " +
                         "phase3@ceramics.org");
                }
                System.exit(0);
            }
        } catch (UnavailableServiceException x) {
            // OK, ignore this error.
        }
    }

    public static void main(String[] args) {
        SingleInstanceBasicEditor.main
            (new BasicEditorCreator() {
                    @Override public BasicEditor run() {
                        return new Viewer();
                    }
                }, args);
    }
}
