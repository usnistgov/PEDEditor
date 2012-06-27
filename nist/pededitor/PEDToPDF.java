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

import javax.swing.JOptionPane;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;

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
            Diagram d = new Diagram();
            String ifn = args[0];
            String ofn = args[1];
            try {
                d.openDiagram(new File(ifn));
                d.saveAsPDF(new File(ofn));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.toString());
            }
        } else if (args.length == 0) {
            String defaultDir = "/eb/ped";

            PathMatcher m = FileSystems.getDefault().getPathMatcher
                ("glob:**.ped");
            Diagram d = new Diagram();

            Document doc = new Document(PageSize.LETTER);
            PdfWriter writer = null;
            try {
                writer = PdfWriter.getInstance(doc, new FileOutputStream("/eb/pdf/combined.pdf"));
            } catch (Exception e) {
                System.err.println(e);
                return;
            }
            System.out.println("Opening...");
            doc.open();

            try  (DirectoryStream<Path> stream
            		= Files.newDirectoryStream(Paths.get(defaultDir))) {
                    int cnt = 0;
                    for (Path file: stream) {
                        if (!m.matches(file)) {
                            continue;
                        }
                        try {
                            d.openDiagram(file.toFile());
                            String ofn = removeExtension(file.toString()) + ".pdf";
                            d.appendToPDF(doc, writer);
                            System.out.println(file + " -> " + ofn + " OK");
                            ++cnt;
                            if (cnt == 20) {
                                break;
                            }
                        } catch (IOException | DirectoryIteratorException x) {
                            // IOException can never be thrown by the iteration.
                            // In this snippet, it can only be thrown by newDirectoryStream.
                            System.err.println(file + ": " + x);
                        }
                    }

                    System.out.println("Closing.");
                    doc.close();
                } catch (IOException | DirectoryIteratorException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
            }
        } else {
            System.err.println("Expected 0 or 2 arguments");
        }
    }
}
