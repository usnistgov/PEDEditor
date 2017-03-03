/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BadPdfFormatException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

/** Wrapper class for conversion of PED files to PDF files. */
public class PEDToPDF {
    public final static String PED_DIR = "/ebdata/ped";
    public final static String PED_DIR2 = "/ebdata/pedw";

    /** Load the PED file with the given filename, crop it, guess
        diagram components, remove the x3 and y3 keys (which are used
        to guess diagram components). */
    public static Diagram loadAndFix(String filename, boolean crop) throws IOException {
        Diagram d = Diagram.loadFrom(new File(filename));
        Rectangle2D bounds = new Rectangle2D.Double(-0.5, -0.5, 2.0, 2.0);
        if (crop && d.crop(bounds)) {
            System.err.println(filename + " did not fit in the normal page bounds.");
            d.computeMargins();
        }
        if (!d.guessComponents(true)) {
            d.addTag("WARN missing diagram component");
        }
        d.removeKey("diagram code");
        d.removeKey("x3");
        d.removeKey("y3");
        return d;
    }

    /** Return an unsorted list of all PED files in the given directory. */
    static ArrayList<String> getInputFilenames0(String dir)
        throws DirectoryIteratorException, IOException {
        PathMatcher m = FileSystems.getDefault().getPathMatcher
            ("glob:**.ped");
        ArrayList<String> res = new ArrayList<>();

        try  (DirectoryStream<Path> stream
              = Files.newDirectoryStream(Paths.get(dir))) {
                for (Path file: stream) {
                    if (m.matches(file)) {
                        res.add(file.toString());
                    }
                }
            } catch (IOException | DirectoryIteratorException x) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(x);
        }
        return res;
    }

    /** Return an unsorted list of all PED files in the given directory. */
    static ArrayList<String> getInputFilenames(String dir)
        throws DirectoryIteratorException, IOException {
        ArrayList<String> res = getInputFilenames0(dir);
        Collections.sort(res, new MixedIntegerStringComparator());
        return res;
    }

    /** Return an unsorted list of all PED files in the given directory. */
    static ArrayList<String> getInputFilenames1(String dir)
        throws DirectoryIteratorException, IOException {
        ArrayList<String> res = getInputFilenames0(dir);
        Collections.sort(res);
        return res;
    }

    public void combinePEDs(List<String> peds) {
        combinePEDs(peds, 0);
    }

    public static void combinePEDs(List<String> peds, int diagramsPerDocument) {
        Document doc = null;
        PdfCopy copy = null;
        int inFileNo = -1;
        int outFileCnt = 0;
        String ofn = null;

        for (String filename: peds) {
            System.out.println("Reading " + filename);
            ++inFileNo;
            if (inFileNo == 0
                || (diagramsPerDocument > 0
                    && inFileNo % diagramsPerDocument == 0)) {
                ++outFileCnt;
                if (doc != null) {
                    doc.close();
                }
                ofn = (diagramsPerDocument == 0) ? "/ebdata/pdf/combined.pdf"
                    : String.format("/ebdata/pdf/combined%04d.pdf", outFileCnt);
                System.out.println("Starting " + ofn);
                doc = new Document(PageSize.LETTER);
                try {
                    copy = new PdfCopy(doc, new FileOutputStream(ofn));
                } catch (Exception e) {
                    System.err.println(e);
                    return;
                }
                doc.open();
            }

            try {
                Diagram d = loadAndFix(filename, true);
                copy.addPage(copy.getImportedPage
                             (new PdfReader(DiagramPDF.toPDFByteArray(d)),
                              1));
                int pedpos = filename.indexOf("\\ped\\");
                String pedout = filename.substring(0, pedpos) + "\\ped2\\"
                    + filename.substring(pedpos + 5);
                System.out.println(filename + " -> " + ofn);
                System.out.println(filename + " -> " + pedout);
                d.saveAsPED(Paths.get(pedout));
            } catch (IOException | BadPdfFormatException x) {
                System.err.println(filename + ": " + x);
            }
        }
        doc.close();
    }

    public static void loadAndSave(String filename, String outdir) {
        try {
            Diagram d = loadAndFix(filename, false);
            int pedpos = filename.lastIndexOf("\\");
            String ofn = outdir + "\\" + filename.substring(pedpos + 1);
            System.out.println(filename + " -> " + ofn);
            d.saveAsPED(Paths.get(ofn));
        } catch (IOException x) {
            System.err.println(filename + ": " + x);
        }
    }

    /** Convert all files under PED_DIR to PDFs. Also fix the files
        and place the fixed files in the ped2 directory. */
    public static void convertAll() {
        try {
            List<String> peds = getInputFilenames(PED_DIR);
            // int i = peds.indexOf("\\eb\\ped\\13125.ped");
            // peds = peds.subList(i+1, peds.size());
            combinePEDs(peds, 100);
                
            System.out.println("Batch conversion complete.");
        } catch (IOException | DirectoryIteratorException x) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(x);
            return;
        }
    }

    /** Like convertAll, but doesn't create PDFs, just fixes the files
        in /ebdata/pedw and places the output in /ebdata/pedw2. */
    public static void convertAll2() {
        ArrayList<String> filenames;

        try {
            filenames = getInputFilenames1(PED_DIR2);
        } catch (IOException | DirectoryIteratorException x) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(x);
            return;
        }

        for (String filename: filenames) {
            try {
                Diagram d = loadAndFix(filename, true);
                int pedpos = filename.indexOf("\\pedw\\");
                String pedout = filename.substring(0, pedpos) + "\\pedw2\\"
                    + filename.substring(pedpos + 6);
                System.out.println(filename + " -> " + pedout);
                d.saveAsPED(Paths.get(pedout));
            } catch (IOException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
            }
        }
        System.out.println("Batch conversion complete.");
    }

    /** Convert all files under PED_DIR to PDFs. Also fix the files
        and place the fixed files in the ped2 directory. */
    public static void fixAll(String inDir, String outDir) {
        try {
            for (String filename: getInputFilenames(inDir)) {
                loadAndSave(filename, outDir);
            }
            System.out.println("Batch conversion complete.");
        } catch (IOException | DirectoryIteratorException x) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(x);
            return;
        }
    }

    public static void oldMain(String[] args) {
        if (args.length == 2) {
            String ifn = args[0];
            String ofn = args[1];
            try {
                DiagramPDF.saveAsPDF(loadAndFix(ifn, true), new File(ofn));
                System.out.println(ifn + " -> " + ofn + " conversion complete.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.toString());
            }
        } else if (args.length == 0) {
            convertAll();
        } else {
            System.err.println("Expected 0 or 2 arguments");
        }
    }

    public static void alternateMain(String[] args) {
        convertAll2();
    }

    public static void main(String[] args) {
        oldMain(args);
    }
}
