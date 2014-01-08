/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;

/** BasicEdtor with "Save as PDF" menu. */
public class Editor extends BasicEditor {
    public Editor() {
        addSaveAsPDF();
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

        DiagramPDF.saveAsPDF(this, file);
    }

    /** Launch the application. */
    public static void main(String[] args) {
        try {
            // Work-around for a bug that affects EB's PC as of 7.0_3.
            System.setProperty("sun.java2d.d3d", "false");
            // TODO UNDO?
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception x) {
            // Success is not critical.
            System.err.println("System settings: " + x);
        }
        EventQueue.invokeLater(new ArgsRunnable(args) {
                @Override public void run() {
                    if (args.length == 1 && args[0].charAt(0) == '-') {
                        printHelp();
                        System.exit(2);
                    }

                    try {
                        Editor app = new Editor();
                        app.run(args);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }

}
