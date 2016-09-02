/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.util.EnumSet;

/** Regenerate the diagram components list, and compare with the
    existing list to identify files that need updating. */
public class FindComplexComponents {
    public final static String DIR = "l:/internal/PEDataCenter/DiagramFiles";

    public static Diagram loadAndFix(String filename) throws IOException {
        Diagram d = Diagram.loadFrom(new File(filename));
        for (Side side: EnumSet.allOf(Side.class)) {
            try {
				d.setDiagramComponent(side, null);
			} catch (DuplicateComponentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        d.guessComponents(true);
        return d;
    }

    public static void checkAll(String dir) {
        try {
            for (String preFile: PEDToPDF.getInputFilenames0(dir)) {
                if (hasComplexFormula(preFile)) {
                    System.out.println(preFile);
                }
            }
        } catch (IOException | DirectoryIteratorException x) {
            System.err.println(x);
            return;
        }
    }

    /** Return true if any guessed components include the characters
        "/", "+", ",", ":", or "%". */
    public static boolean hasComplexFormula(String file) throws IOException {
        Diagram dPre = loadAndFix(file);
        for (Side side: EnumSet.allOf(Side.class)) {
            String s1 = dPre.getDiagramComponent(side);
            if (s1 != null
                && (s1.contains("+")
                    || s1.contains(":")
                    || s1.contains(",")
                    || s1.contains("%")
                    || s1.contains("/"))) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        switch (args.length) {
		case 1:
		    checkAll(args[0]);
		    break;

		case 0:
		    checkAll(DIR);
		    break;

		default:
		    System.err.println("Expected 0-1 arguments");
		    break;
		}
    }
}
