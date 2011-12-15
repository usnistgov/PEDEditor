package gov.nist.pededitor;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.io.*;
import java.text.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.plaf.basic.BasicHTML;

/** A test of displaying styled rotated text on the screen. */
public class RotatedHTMLTestPane extends JPanel {
    @Override
        public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        String str = "<html><p>H<sub>2</sub>SO<sub>4</sub> "
            + "<font color=\"red\">100&deg;C</font>"
            + "(&alpha; - &beta; &prime; <font size=\"+1\">&Delta;</font>)</font></p>"
            + "<p>The lazy dog <u>got run over</u>.</p></html>";
        
        JLabel bogus = new JLabel(str);
        bogus.setFont(new Font(null, 0, 32));
        bogus.setForeground(Color.BLUE);
        View view =  (View) bogus.getClientProperty("html");

        AffineTransform oldxform = g2.getTransform();

        Dimension bounds = getSize();
        double cx = bounds.width / 2;
        double cy = bounds.height / 2;

        for (double theta = 0.0; theta < Math.PI * 2 - 1e-6; theta += Math.PI / 4) {
            g2.translate(cx, cy);
            g2.rotate(theta, 0, 0);
            view.paint(g, new Rectangle(10, 10, 500, 500));
            g2.setTransform(oldxform);
        }


        // System.out.println(view);
        // Utilities.paintComposedText(g, g.getClipBounds(), (GlyphView) view);
    }
}
