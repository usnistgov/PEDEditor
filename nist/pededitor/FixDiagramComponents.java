/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Paths;

/** Regenerate the diagram components list, and compare with the
    existing list to identify files that need updating. */
public class FixDiagramComponents {
    public final static String PRE_DIR = "/ebdata/pedw.20121212";
    public final static String POST_DIR = "/ebdata/DiagramClone/migration";
    public final static String OUT_DIR = "/ebdata/MigrationNew";

    /** Generate diagram components for all PEDs in dir1 and compare
        to the corresponding files in dir2. Print one line per
        mismatch:

            filename\tside\tdir1Component\tdir2Component
    */
    public static void compareAll(String dir1, String dir2, String dirOut) {
        try {
            for (String preFile: PEDToPDF.getInputFilenames(dir1)) {
                String name = (new File(preFile)).getName();
                String postFile = dir2 + "/" + name;
                String outFile = null;
                if (dirOut != null) {
                    outFile = dirOut + "/" + name;
                }
                try {
                    compare(preFile, postFile, outFile);
                } catch (Exception x) {
                    System.out.println("In file " + preFile + ": " + x);
                }
            }
        } catch (IOException | DirectoryIteratorException x) {
            System.err.println(x);
            return;
        }
    }

    public static void compare(String name) throws IOException {
        compare(PRE_DIR + "/" + name,
                POST_DIR + "/" + name,
                OUT_DIR + "/" + name);
    }

    public static void compare(String preFile, String postFile)
        throws IOException {
        compare(preFile, postFile, null);
    }

    /** Compare the guessed diagram components of preFile with the
        actual components of postFile, and save the fixed version of
        preFile to outFile. */
    public static void compare(String preFile, String postFile,
                                      String outFile) throws IOException {
        Diagram dPre = PEDToPDF.loadAndFix(preFile, true);
        Diagram dPost = Diagram.loadFrom(new File(postFile));
        boolean different = false;
        for (Side side: Side.values()) {
            String s1 = dPre.getDiagramComponent(side);
            String s2 = dPost.getDiagramComponent(side);
            if ((s1 != s2) && (s1 == null || !s1.equals(s2))) {
                System.out.println(preFile + "\t" + side + "\t" + s1 + "\t" + s2);
                different = true;
            }
        }
        if (different && outFile != null) {
            dPre.saveAsPED(Paths.get(outFile));
        }
    }

    public static void main(String[] args) {
        try {
            switch (args.length) {
            case 3:
                compare(args[0], args[1], args[2]);
                break;

            case 2:
                compare(args[0], args[1]);
                break;

            case 1:
                compare(args[0]);
                break;

            case 0:
                compareAll(PRE_DIR, POST_DIR, OUT_DIR);
                break;

            default:
                System.err.println("Expected 0-3 arguments");
                break;
            }

        } catch (IOException x) {
            System.err.println(x);
        }
    }
}
