package gov.nist.pededitor;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.io.*;
import java.text.*;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.*;

/** A test of displaying styled rotated text on the screen. */
public class StyledRotatedTextTestPane extends JPanel {
    @Override
        public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

        String str= "    H2SO4\u0394\u2032 \u03B1";
        AttributedString ats = new AttributedString(str);
        makeDigitsSubscripts(ats);

        // Font f = new Font("Times",Font.BOLD, 24);
        // ats.addAttribute(TextAttribute.FONT, f, 0, str.length());

        AttributedCharacterIterator iter = ats.getIterator();
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout tl = new TextLayout(iter, frc);
        g2.setColor(Color.red);

        AffineTransform at = g2.getTransform();

        Dimension bounds = getSize();
        double cx = bounds.width / 2;
        double cy = bounds.height / 2;

        for (int rotate = 0; rotate < 8; ++rotate) {
            g2.setTransform(at);
            g2.rotate(rotate * Math.PI / 4, cx, cy);
            tl.draw(g2, (float) cx, (float) cy);
        }

        // new TextLayout(ats.getIterator(), g2.getFontRenderContext())
        // .draw(g2, (float) x, (float) y);
    }

    public static void makeDigitsSubscripts(AttributedString ats) {
    	String str = ats.toString();
        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);

            if (ch >= '0' && ch <= '9') {
                ats.addAttribute(
                                 TextAttribute.SUPERSCRIPT,
                                 new Integer(TextAttribute.SUPERSCRIPT_SUB),
                                 i, i +1);
            }
        }
    }
}
