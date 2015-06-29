/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.AbstractAction;

/** BasicEditor plus "Save as PDF" menu. */
public class Editor extends SingleInstanceBasicEditor {
    public Editor() {
        addSaveAsPDF();
    }

    @Override public Editor createNew() {
        return new Editor();
    }

    @SuppressWarnings("serial")
	abstract class Action extends AbstractAction {
        Action(String name) {
            super(name);
        }
    }

    void addSaveAsPDF() {
        @SuppressWarnings("serial")
		Action act = new Action("PDF") {
                {
                    putValue(MNEMONIC_KEY, KeyEvent.VK_F);
                    putValue(SHORT_DESCRIPTION,
                             "Save diagram in Adobe PDF format");
                }

                @Override public void actionPerformed(ActionEvent e) {
                    saveAsPDF();
                }
            };
        editFrame.addAlphabetized(editFrame.mnSaveAs, act, 1);
    }

    public void saveAsPDF() {
        File file = showSaveDialog("pdf");
        if (file == null || !verifyOverwriteFile(file)) {
            return;
        }

        try {
            DiagramPDF.saveAsPDF(this, file);
        } catch (FileNotFoundException e) {
            showError("Could not save file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SingleInstanceBasicEditor.main
            (new BasicEditorCreator() {
                    @Override public BasicEditor run() {
                        return new Editor();
                    }
                }, args);
    }
}
