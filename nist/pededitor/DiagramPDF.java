/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

/** Class to add save-as-PDF capabilities to a Diagram. */
public class DiagramPDF {
    public static void saveAsPDF(Diagram d, File file) {
        saveAsPDF(d, new Document(PageSize.LETTER), file);
    }

    public static void saveAsPDF(Diagram d, Document doc, File file) {
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
        } catch (Exception e) {
            System.err.println(e);
            return;
        }

        doc.open();
        appendToPDF(d, doc, writer);
        doc.close();
    }

    public static byte[] toPDFByteArray(Diagram d) {
        Document doc = new Document(PageSize.LETTER);
        /*
          // TODO Decide whether to do this or not.
           if (pageBounds.width > pageBounds.height * 1.05) {
              doc = doc.rotate();
           }
        */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer;
        try {
            writer = PdfWriter.getInstance(doc, baos);
        } catch (DocumentException e) {
            e.printStackTrace();
            return null;
        }

        doc.open();
        appendToPDF(d, doc, writer);
        doc.close();
        return baos.toByteArray();
    }

    public static void appendToPDF(Diagram d, Document doc, PdfWriter writer) {
        String title = d.getTitle();
        if (title != null) {
            try {
                doc.add(new Paragraph(title));
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }

        float topMargin = (float) (72 * 0.5);
        Rectangle2D.Double bounds = new Rectangle2D.Double
            (doc.left(), doc.bottom(), doc.right() - doc.left(),
             doc.top() - doc.bottom() - topMargin);

        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp = cb.createTemplate((float) bounds.width, (float) bounds.height);
        
        @SuppressWarnings("unused")
            Graphics2D g2 = false
            ? tp.createGraphics((float) bounds.width, (float) bounds.height,
                                new DefaultFontMapper())
            : tp.createGraphicsShapes((float) bounds.width, (float) bounds.height);

        g2.setFont(d.getFont());
        d.paintDiagram(g2, d.deviceScale(g2, bounds), null);
        g2.dispose();
        cb.addTemplate(tp, doc.left(), doc.bottom());
    }
}
