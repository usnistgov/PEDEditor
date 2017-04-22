/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.awt.Graphics;

import javax.swing.AbstractAction;

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
        ef.hideImages.setVisible(false);
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
              ef.actAddAutoPositionedVertex,
              ef.actAddVertex,
              ef.actColor,
              ef.actCopy,
              ef.actCopyAndPaste,
              ef.actCut,
              ef.actCutAll,
              ef.actCutRegion,
              ef.actEditSelection,
              ef.actIsotherm,
              ef.actLeftArrow,
              ef.actMakeDefault,
              ef.actMovePoint,
              ef.actMoveSelection,
              ef.actMoveSelection,
              ef.actPaste,
              ef.actRedo,
              ef.actRemoveSelection,
              ef.actResetToDefault,
              ef.actRightArrow,
              ef.actRuler,
              ef.actText,
              ef.actTieLine,
              ef.actUndo
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
            
        removeImage();
        setEditable(false);
        // Page X and Page Y are only useful during editing.
        for (String var: new String[] {"page X", "page Y"}) {
            try {
                removeVariable(var);
            } catch (CannotDeletePrincipalVariableException
                     |NoSuchVariableException e1) {
                // OK, let it be
            }
        }
    }

    @Override protected void resizeEditFrame(int otherEditorCnt) {
        // Use default settings instead of expanding height to fill screen.
    }

    /** Standard installation message for Macintosh, on which file associations don't work. */
    @Override void doFileAssociationsBrokenMessage() {
        doFileAssociationsBrokenMessage
            ("On Macintosh systems, you may view downloaded diagrams (.PEDV files) manually "
                    + "using the <code>File/Open</code> menu option, or you may have this program "
                    + "automatically scan for newly downloaded diagrams "
                    + "for as long as it remains open using the "
                    + "<code>File/Monitor Directory</code> option. "
                    + "<p>At any time, you can "
                    + "uninstall, run, or create a shortcut for the Viewer by opening the Java Control "
                    + "Panel's General tab and and pressing the \"View...\" button.",
             fallbackTitle());
    }

    @Override public String mimeType() {
        return "application/x-pedviewer";
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
        // Do nothing.
    }

    @Override public String[] pedFileExtensions() {
        return new String[] {"ped", "pedv"};
    }

    @Override public String[] launchPEDFileExtensions() {
        return new String[] {"pedv"};
    }

    @Override String fallbackTitle() {
        return "PED Viewer";
    }

    @Override String successfulAssociationMessage() {
        return fallbackTitle()
            + " has been installed. For uninstall instructions, "
            + "see PED Viewer help menu.";
    }

    @Override String failedAssociationMessage(boolean haveOptions) {
        String res = fallbackTitle() + " could not register as the handler for "
            + "PED Viewer diagrams (.PEDV files).";
        if (haveOptions) {
            res +=
                "You may still view downloaded diagrams (.PEDV files) manually "
                + "using the <code>File/Open</code> menu option, or you may have this program "
                + "automatically scan for newly downloaded diagrams "
                + "for as long as it remains open using the "
                + "<code>File/Monitor Directory</code> option. "
                + "<p>You can reopen the PED Viewer at "
                + "any time by clicking on the desktop shortcut if available or clicking "
                + "on the same link you used to run this program in the first place. "
                + "<p>For more information, please contact phase3@ceramics.org.";
        }
        return res;
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
