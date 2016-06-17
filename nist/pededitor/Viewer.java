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
        // Page X and Page Y are only useful during editing.
        for (String var: new String[] {"page X", "page Y"}) {
            try {
                removeVariable(var);
            } catch (CannotDeletePrincipalVariableException
                     |NoSuchVariableException e1) {
                // OK, let it be
            }
        }

        // ef.mnMonitor.setVisible(true); // Enable directory monitoring.
        
        setSaveNeeded(false);
    }

    @Override protected void resizeEditFrame(int otherEditorCnt) {
        // Use default settings instead of expanding height to fill screen.
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
        open();
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
            res = res + "<p>You can still view any .PEDV files you download " +
                "(using the \"View Diagram\" button of the PED Online Search) " +
                "by pressing \"Run Now\" " +
                "and selecting the file to display when the " +
                "\"Open PED/PEDV file\" dialog opens. Then, as long as you keep the PED " +
                "Viewer open, you can display additional diagrams using its " +
                "File/Open menu item. You can reopen the PED Viewer at " +
                "any time by clicking on the desktop shortcut if available or clicking " +
                "on the same link you used to run this program in the first place. " +
                "<p>For more information, please contact phase3@ceramics.org.";
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
