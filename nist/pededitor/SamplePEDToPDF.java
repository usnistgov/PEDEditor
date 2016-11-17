/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.util.Collections;
import java.util.List;

/** Create a PDF from a set of randomly selected PEDs. */
public class SamplePEDToPDF {
    public static void main(String[] args) {
        List<String> peds;

        try {
            peds = PEDToPDF.getInputFilenames(PEDToPDF.PED_DIR);
        } catch (IOException | DirectoryIteratorException x) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(x);
            return;
        }
        Collections.shuffle(peds);
        peds = peds.subList(0, 200);
        Collections.sort(peds, new MixedIntegerStringComparator());
        PEDToPDF.combinePEDs(peds, 0);
        System.out.println("Conversion complete.");
    }
}
