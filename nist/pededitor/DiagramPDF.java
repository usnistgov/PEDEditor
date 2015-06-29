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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.io.StreamUtil;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

/** Class to add save-as-PDF capabilities to a Diagram. */
public class DiagramPDF {
    static class FontMap extends DefaultFontMapper {
        Diagram d;
        
        FontMap(Diagram d) {
            this.d = d;
        }

        @Override public BaseFont awtToPdf(java.awt.Font font) {
            String filename = d.fontNameToFilename(font.getFontName());
            if (filename == null) {
                return super.awtToPdf(font);
            } else {
                return getItextFont(filename, font.getSize2D());
            }
        }
    }

    public static void saveAsPDF(Diagram d, File file) throws FileNotFoundException {
        saveAsPDF(d, new Document(PageSize.LETTER), file);
    }

    public static void saveAsPDF(Diagram d, Document doc, File file)
        throws FileNotFoundException {
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
        } catch (DocumentException e) {
            throw new FileNotFoundException();
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

    static PdfGraphics2D createGraphics(Diagram d, PdfContentByte cb,
                                        float w, float h, boolean onlyShapes) {
        PdfGraphics2D res = onlyShapes ?
            new PdfGraphics2D(cb, w, h, onlyShapes) :
            new PdfGraphics2D(cb, w, h, new FontMap(d));
        res.setFont(d.getFont());
        return res;
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

        // Use onlyShapes because of a bug in automatic boldtext in
        // iText 5.5.6. That's too bad, because everything else about
        // using real fonts seems to work. -- EB 6/29/2015
        Graphics2D g2 = createGraphics
            (d, cb, (float) bounds.width, (float) bounds.height, true);
        d.paintDiagram(g2, d.deviceScale(g2, bounds), null);
        g2.dispose();
        cb.addTemplate(tp, doc.left(), doc.bottom());
    }

    @JsonIgnore static public BaseFont getItextFont
        (String filename, float font_size) {
        InputStream is = (new DiagramPDF()).getClass()
            .getResourceAsStream(filename);
        if (is == null) {
            throw new IllegalStateException
                ("Could not locate font '" + filename + "'");
        }
        byte[] bytes;
        try {
            bytes = StreamUtil.inputStreamToArray(is);
            return BaseFont.createFont(filename, BaseFont.IDENTITY_H, true, true,
                                       bytes, null);
        } catch (IOException | DocumentException x) {
            throw new RuntimeException(x);
        }
    }
}
