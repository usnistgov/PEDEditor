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

    public static String PROGRAM_TITLE = "PED Viewer";

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
                 ef.actNearestGridPoint,
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
                Object[] options = {"Run Now", "Finished"};
                int defaultIndex;
                String mess;
                String title;
                if (is.requestAssociation(mime, exts)) {
                    mess = fallbackTitle()
                        + " has been installed. For uninstall instructions, see PED Viewer help menu.";
                    title = "Installation successful";
                    defaultIndex = 1; // Default is exit
                } else {
                    mess = fallbackTitle() +
                        " could not register as the handler for  " +
                        "PED Viewer diagrams (.PEDV files). " +
                        "<p>You can still use this program\'s File/Open option to " +
                        "manually load and view those files. " +
                        "<p>For more information, please contact phase3@ceramics.org.";
                    title = "Installation partly successful";
                    defaultIndex = 0; // Default is continue
                }
                if (JOptionPane.showOptionDialog
                    (editFrame, htmlify(mess), title,
                     JOptionPane.YES_NO_OPTION,
                     JOptionPane.PLAIN_MESSAGE,
                     null, options, options[defaultIndex]) != JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
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
                    @Override public String getProgramTitle() {
                        return Viewer.PROGRAM_TITLE;
                    }
                }, args);
    }
}
