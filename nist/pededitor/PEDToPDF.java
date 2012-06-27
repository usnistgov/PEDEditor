package gov.nist.pededitor;

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

import javax.swing.JOptionPane;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BadPdfFormatException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

/** Wrapper class for conversion of PED files to PDF files. */
public class PEDToPDF {

    /** If the base filename contains a dot, then remove the last dot
        and everything after it. Otherwise, return the entire string.
        Modified from coobird's suggestion on Stack Overflow. */
    public static String removeExtension(String s) {
        String separator = System.getProperty("file.separator");
        int lastSeparatorIndex = s.lastIndexOf(separator);
        int extensionIndex = s.lastIndexOf(".");
        return (extensionIndex <= lastSeparatorIndex) ? s
            : s.substring(0, extensionIndex);
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            String ifn = args[0];
            String ofn = args[1];
            try {
                Diagram.loadFrom(new File(ifn))
                    .saveAsPDF(new File(ofn));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.toString());
            }
        } else if (args.length == 0) {
            String defaultDir = "/eb/ped";

            PathMatcher m = FileSystems.getDefault().getPathMatcher
                ("glob:**.ped");
            ArrayList<String> matches = new ArrayList<>();

            try  (DirectoryStream<Path> stream
                  = Files.newDirectoryStream(Paths.get(defaultDir))) {
                    for (Path file: stream) {
                        if (m.matches(file)) {
                            matches.add(file.toString());
                        }
                    }
                } catch (IOException | DirectoryIteratorException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
            }
            Collections.sort(matches, new MixedIntegerStringComparator());

            int diagramsPerDocument = 100;
            Document doc = null;
            PdfCopy copy = null;
            int inFileNo = -1;
            int outFileCnt = 0;
            String ofn = null;

            for (String filename: matches) {
                System.out.println("Reading " + filename);
                ++inFileNo;
                if (inFileNo % diagramsPerDocument == 0) {
                    ++outFileCnt;
                    if (doc != null) {
                        doc.close();
                    }
                    ofn = String.format("/eb/pdf/combined%04d.pdf", outFileCnt);
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
                    Diagram d = Diagram.loadFrom(new File(filename));
                    copy.addPage(copy.getImportedPage
                                 (new PdfReader(d.toPDFByteArray()),
                                  1));
                    System.out.println(filename + " -> " + ofn);
                } catch (IOException | BadPdfFormatException x) {
                    System.err.println(filename + ": " + x);
                }
            }
            doc.close();
        } else {
            System.err.println("Expected 0 or 2 arguments");
        }
    }
}
