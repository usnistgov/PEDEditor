package gov.nist.pededitor;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/** A test of displaying styled rotated text on the screen. */
public class LineFillTestPane extends JPanel {
    private static final long serialVersionUID = 4350137998913831804L;


    TexturePaint tex = Fill.createHatch(0, 0.25, false, BasicStrokes.getDottedLine(),
    		Color.BLACK);
    // TexturePaint tex = Fill.createHatch(Math.PI / 2, 1, 0.25, false);
    // TexturePaint tex = StandardFill.V1_25.getFill();
    // TexturePaint tex = Fill.createHatch(Math.atan2(0.5, 1), 0.3, false, BasicStrokes.scaledStroke(BasicStrokes.getDottedLine(), 3.0f));

    @Override public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        // g2.setColor(Color.BLACK);
        g2.setPaint(tex);
        g2.fill(new Rectangle2D.Double(50, 50, 200, 200));
    }
}
