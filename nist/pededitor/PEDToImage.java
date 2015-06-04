/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.io.File;
import java.io.IOException;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;

import javax.imageio.ImageIO;

/** Wrapper class for conversion of PED files to JPEG files. */
public class PEDToImage {

    static void help() {
        System.err.println
            ("Usage:\n\n"
             + "    java -jar PEDToImage.jar [-nomargin] <PED file> <PDF file>\n"
             + "           -nomargin: Omit margins from PDF file\n\n"
             + "         or\n\n"
             + "    java -jar PEDToImage.jar <PED file> <image file> <width> <height>\n\n"
             + "Supported image formats include GIF, JPEG, and PNG.");
        System.exit(1);
    }

    public static void main(String[] args) {
        int baseIndex = 0;
        boolean haveMargins = true;
        if (baseIndex < args.length && "-nomargin".equals(args[baseIndex])) {
            haveMargins = false;
            ++baseIndex;
        }
        if (args.length - baseIndex < 2) {
            help();
        }

        String ifn = args[baseIndex++];
        String ofn = args[baseIndex++];
        String ext = BasicEditor.getExtension(ofn);
        File ofh = new File(ofn);

        if (ext == null) {
            throw new IllegalArgumentException
                ("Output file '" + ofn + "' missing extension (such as .jpg, .gif, .png, or .pdf)");
        }

        boolean isPDF = ext.equalsIgnoreCase("pdf");

        if (isPDF) {
            Document doc = new Document(PageSize.LETTER);
            if (baseIndex < args.length) {
                help();
            }
            if (!haveMargins) {
                doc.setMargins(0f, 0f, 0f, 0f);
            }

            Diagram d;
            try {
                d = Diagram.loadFrom(new File(ifn));
            } catch (IOException x) {
                throw new IllegalArgumentException
                    ("Invalid input file '" + args[0] + "': " + x);
            }
            
            DiagramPDF.saveAsPDF(d, doc, ofh);
            return;
        }

        if (args.length != 4) {
            help();
        }

        if (!isPDF) {
            String[] imageExts = ImageIO.getReaderFileSuffixes();

            boolean foundMatch = false;
            for (String x: imageExts) {
                if (ext.equalsIgnoreCase(x)) {
                    ext = x;
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                throw new IllegalArgumentException
                    ("Unsupported image format '" + ext + "'");
            }
        }
        
        int width;
        try {
            width = Integer.parseInt(args[2]);
        } catch (NumberFormatException x) {
            throw new IllegalArgumentException
                ("Invalid width value '" + args[2] + "'");
        }
        int height;
        try {
            height = Integer.parseInt(args[3]);
        } catch (NumberFormatException x) {
            throw new IllegalArgumentException
                ("Invalid height value '" + args[3] + "'");
        }
        Diagram d;
        try {
            d = Diagram.loadFrom(new File(ifn));
        } catch (IOException x) {
            throw new IllegalArgumentException
                ("Invalid input file '" + args[0] + "': " + x);
        }

        try {
            d.saveAsImage(ofh, ext, width, height);
        } catch (IOException x) {
            throw new IllegalArgumentException
                ("Invalid output file '" + args[1] + "': " + x);
        }
    }
}
