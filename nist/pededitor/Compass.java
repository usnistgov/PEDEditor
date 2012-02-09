package gov.nist.pededitor;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/** Simple class to draw a circle with ticks and/or angle markings. */
public class Compass {
    double cx;
    double cy;
    double r;
    BasicStroke stroke;

    public Compass(double cx, double cy, double r) {
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        stroke = new BasicStroke((float) (r * 0.04),
                                 BasicStroke.CAP_ROUND,
                                 BasicStroke.JOIN_ROUND);
    }

    public void drawCircle(Graphics2D g2d) {
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(stroke);
        Shape shape = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        g2d.draw(shape);
        g2d.setStroke(oldStroke);
    }

    public void drawTick(Graphics2D g2d, double degrees, String text) {
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(stroke);
        double theta = degreesToTheta(degrees);
        double dx = r * Math.cos(theta);
        double dy = r * Math.sin(theta);

        double tickLength = 0.06;

        g2d.draw(new Line2D.Double
                 (cx + dx + dx * tickLength, cy + dy + dy * tickLength,
                  cx + dx - dx * tickLength, cy + dy - dy * tickLength));

        if (text != null) {
            LabelDialog.drawString(g2d, text, cx + dx * 1.28, cy + dy * 1.28,
                                   0.5, 0.5);
        }

        g2d.setStroke(oldStroke);
    }

    public void drawHand(Graphics2D g2d, double degrees) {
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke((float) (r * 0.08)));
        double theta = degreesToTheta(degrees);

        r *= 0.95;
        double dx = r * Math.cos(theta);
        double dy = r * Math.sin(theta);

        g2d.draw(new Line2D.Double(cx, cy, cx + dx, cy + dy));

        Path2D.Double arrow = new Path2D.Double();
        arrow.moveTo(cx + dx - dy * 0.1, cy + dy + dx * 0.1);
        arrow.lineTo(cx + dx + dy * 0.1, cy + dy - dx * 0.1);
        arrow.lineTo(cx + dx * 1.2, cy + dy * 1.2);
        arrow.closePath();

        g2d.fill(arrow);
        g2d.setStroke(oldStroke);
    }

    static double degreesToTheta(double deg) {
        return Math.PI * (-deg) / 180;
    }

    static double thetaToDegrees(double theta) {
        return - (theta / Math.PI) * 180;
    }

    public void drawTickedCircle(Graphics2D g2d) {
        drawCircle(g2d);
        for (int i = - 135; i <= 180; i += 45) {
            drawTick(g2d, (double) i, Integer.toString(i));
        }
    }
}
