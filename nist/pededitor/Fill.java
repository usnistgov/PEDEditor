package gov.nist.pededitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

/** Fill type for black lines on a transparent background. */
public class Fill {

    /** Create a transparent hatching fill pattern (evenly spaced black lines).
        
        @param theta Line angle. 0 means horizontal; PI/4 means a 45 degree descending angle.

        @param lineWidth Line width in pixels.

        @param distance Approximate distance in pixels between lines' centers.

        @param crosshatch If true, superimpose crosshatching at an angle of (-theta).
    */


    public static TexturePaint createHatch(double theta, double lineWidth, double density, boolean crosshatch) {
        return createHatch(theta, lineWidth, density, crosshatch, CompositeStroke.getSolidLine());
    }

    public static TexturePaint createHatch(double theta, double lineWidth, double density, boolean crosshatch,
                                           CompositeStroke stroke) {
        theta = normalizeTheta(theta);
        boolean invertY = (theta < 0);
        if (invertY) {
            theta = -theta;
        }
        boolean transposeXY = Math.abs(theta) > Math.PI/4 + 1e-6;
        if (transposeXY) {
            theta = Math.PI/2 - theta;
        }

        Spec spec = createSpec(theta, lineWidth / density);
        BufferedImage im = toBlank(spec, transposeXY);
        Graphics2D g = createGraphics(im);

        if (crosshatch) {
            hatch(g, spec, stroke, lineWidth, transposeXY, false);
            hatch(g, spec, stroke, lineWidth, transposeXY, true);
        } else {
            hatch(g, spec, stroke, lineWidth, transposeXY, invertY);
        }

        return new TexturePaint(im, new Rectangle(0, 0, im.getWidth(), im.getHeight()));
    }

    /** Internal use. */
    protected static class Spec {
        Dimension box;
        /* Coordinates to draw the primary line diagonal across this box. */
        Line2D.Double line;
        /** Vertical offset between the primary line and the lines
            above and below it. */
        int yOffset;

        Spec(Dimension box, Line2D.Double line, int yOffset) {
            this.box = (Dimension) box.clone();
            this.line = (Line2D.Double) line.clone();
            this.yOffset= yOffset;
        }

        /* Mirror the y dimension about the box's midpoint. */
        void mirrorY() {
            line.y1 = box.height - line.y1;
            line.y2 = box.height - line.y2;
        }
    }

    /* Force theta into [-PI/2, PI/2) by adding an integer muliple of
       PI if necessary. For this class' purposes, there is no
       difference between an angle and that angle rotated by PI
       radians. */

    static double normalizeTheta(double theta) {
        theta -= Math.PI * Math.floor(0.5 + theta / Math.PI);
        double piOver2 = Math.PI / 2;
        return (theta < -piOver2 || theta >= piOver2) ? -piOver2 : theta;
    }

    /** @param theta Line angle. 0 means horizontal; PI/4 means a 45 degree descending angle.

        @param lineWidth Line width in pixels.

        @param distance Approximate distance in pixels between lines' centers.
    */
    static Spec createSpec(double theta, double distance) {
        /* Height approximately equals the distance between
           intersections of the hatch lines with the Y-axis. */
        int height = (int) Math.floor(0.5 + distance / Math.cos(theta));

        int maxWidth = (int) (100 + distance * 20);

        double slope = Math.tan(theta);

        if (slope * maxWidth < height / 2) {
            // Line is nearly horizontal; treat it as completely
            // horizontal
            return new Spec
                (new Dimension(1, height), 
                 new Line2D.Double(0, 0.5, 1, 0.5),
                 height);
        } else if (slope * maxWidth < height) {
            slope = height / maxWidth;
        }

        int width = (int) (0.5 + height / slope);

        ContinuedFraction frac = ContinuedFraction.create(slope, 0.001, 0, maxWidth / 5);
        if (frac != null) {
            /* It's possible to arrange the dimensions so the hatch
               lines tessellate nearly perfectly if the width is a
               multiple of denominator. */
            int den = (int) frac.denominator;
            // Round width to the nearest whole multiple of den.
            width = ((width + den/2) / den) * den;

            // Adjust height to correspond to width
            height = (int) (slope * width + 0.5);
        } else {
            slope = (double) height / width;
        }
        
        return new Spec
            (new Dimension(width, height),
             new Line2D.Double(0, 0.5 - 0.5 * slope, width, 0.5 + (width - 0.5) * slope),
             height);
    }

    static BufferedImage toBlank(Spec spec, boolean transpose) {
        int width = transpose ? spec.box.height : spec.box.width;
        int height = transpose ? spec.box.width : spec.box.height;
        return new BufferedImage
            (width, height,
             BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D createGraphics(BufferedImage im) {
        Graphics2D g = im.createGraphics();

        // Set the background color to transparent.
        g.setBackground(new Color(255, 255, 255, 0));
        g.clearRect(0, 0, im.getWidth(), im.getHeight());
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    static void hatch(Graphics2D g, Spec spec, CompositeStroke stroke,
                      double lineWidth, boolean transpose, boolean invertY) {
        Line2D.Double bl = spec.line;
        int height = transpose ? spec.box.width : spec.box.height;
        for (int offset = -1; offset <= 1; ++offset) {
            Line2D.Double line = new Line2D.Double
                (bl.x1, bl.y1 + offset * spec.yOffset,
                 bl.x2, bl.y2 + offset * spec.yOffset);
            if (transpose) {
                line = new Line2D.Double
                    (line.y1, line.x1, line.y2, line.x2);
            }
            if (invertY) {
                line.y1 = height - line.y1;
                line.y2 = height - line.y2;
            }
            stroke.draw(g, line, lineWidth);
        }
    }
}
