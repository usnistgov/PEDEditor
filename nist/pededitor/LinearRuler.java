package gov.nist.pededitor;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.*;

/** Class describing a ruler decorating a linear axis. */
class LinearRuler {
    public static enum LabelAnchor
    { LABEL_NONE, LABEL_LEFT, LABEL_RIGHT, LABEL_MIDDLE };
    
    Point2D.Double startPoint;
    Point2D.Double endPoint;

    /** Tick marks act as anchor points for their corresponding
        labels. As in AnchoredLabel, xWeight determines the positioning of
        the label relative to its anchor. */
    double xWeight;
    /** Tick marks act as anchor points for their corresponding
        labels. As in AnchoredLabel, yWeight determines the positioning of
        the label relative to its anchor. */
    double yWeight;
    double fontSize;

    /** To simplify axis rotations, textAngle indicates the angle of
        the text relative to the ray from startPoint to endPoint. So
        for a vertical upwards-pointing axis, textAngle = 0 would mean
        that lines of text flow upwards. */
    double textAngle;

    double lineWidth;

    /** True if ticks should extend from the right side of the ruler.
        The right side is the side that would involve making a
        90-degree right hand turn if you started out oriented from
        startPoint to endPoint. */
    boolean tickRight;
    /** True if ticks should extend from the left side of the ruler. */
    boolean tickLeft;

    /** Indicate where labels are to be anchored: LABEL_NONE: No
        labels; LABEL_LEFT: at the tip of the left-side tick;
        LABEL_RIGHT: at the tip of the right-side tick. */
    LabelAnchor labelAnchor;

    // UNDO LinearAxisInfo axis;

    /** Start of range of logical values covered by this axis. */
    double getStart() {
        // UNDO return axis.value(startPoint);
        return 17.07;
    }
    /** End of range of logical values covered by this axis. */
    double getEnd() {
        // UNDO return axis.value(endPoint);
        return 18.3179;
    }

    /** Linear function mapping logical values to points in 2D. */
    Point2D.Double toPhysical(double logical,
                              Point2D startPoint, Point2D endPoint) {
        double start = getStart();
        double end = getEnd();
        double t = (logical - start)/(end - start);
        return new Point2D.Double
            (startPoint.getX() + (endPoint.getX() - startPoint.getX()) * t,
             startPoint.getY() + (endPoint.getY() - startPoint.getY()) * t);
    }

    /** draw this ruler to the given graphics context. The ruler's
        logical coordinates for this path should be defined in the
        "Original" coordinate system, but line width and font size are
        defined with respect to the "SquarePixel" coordinate system.
        Also, the output is scaled by "scale" before being drawn.
    */
    public void draw(Graphics2D g,
                     AffineTransform originalToSquarePixel,
                     double scale) {
        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        xform.concatenate(originalToSquarePixel);

        Font oldFont = g.getFont();
        g.setFont(new Font(null, 0, (int) Math.ceil(fontSize * scale)));
        FontMetrics fm = g.getFontMetrics();

        Rectangle2D digitBounds = fm.getStringBounds("8", g);
        Point2D.Double pageStartPoint = new Point2D.Double();
        Point2D.Double pageEndPoint = new Point2D.Double();

        xform.transform(startPoint, pageStartPoint);
        xform.transform(endPoint, pageEndPoint);
        double distance = pageStartPoint.distance(pageEndPoint);
        Point2D.Double pageDelta
            = new Point2D.Double(pageEndPoint.x - pageStartPoint.x,
                                 pageEndPoint.y - pageStartPoint.y);
        double theta = Math.atan2(pageDelta.y, pageDelta.x);

        // m = page distance divided by logical distance.
        double start = getStart();
        double end = getEnd();
        double m = distance / (end - start);
        double maxLabels = Math.floor(distance / digitBounds.getWidth());
        double minTextDelta = digitBounds.getHeight() / m;

        // Construct an approximation of the largest string that is
        // likely to appear as a label in order to ascertain its size.
        // For example, if the format includes 3 characters before the
        // decimal and 2 after, the largest should be no larger than
        // '888.88'. We'll also add two spaces to insure there is some
        // space between ticks.

        int[] limits = RulerTick.digitSpaceNeeded
            (start, end, RulerTick.roundCeil(minTextDelta));
        int units = limits[0];
        int decimals = limits[1];
        StringBuilder longestLabel = new StringBuilder("  ");
        for (int i = 0; i < units; ++i) {
            longestLabel.append('8');
        }
        if (decimals > 0) {
            longestLabel.append('.');
            for (int i = 0; i < decimals; ++i) {
                longestLabel.append('8');
            }
        }
        Rectangle2D labelBounds
            = fm.getStringBounds(longestLabel.toString(), g);
        Point2D.Double offset
            = new Point2D.Double(Math.cos(textAngle), Math.sin(textAngle));

        // Now we can compute minTextDelta more accurately. separate()
        // returns the distance in the final coordinate system we need
        // to have between label ticks, and then we convert back to
        // the corresponding distance in logical coordinates.
        minTextDelta = separate(labelBounds, offset) / m;
        double textDelta = RulerTick.roundCeil(minTextDelta);
        double tickDelta = RulerTick.nextSmallerRound(textDelta);

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke((float) (scale * lineWidth)));
        
        // Draw the spine.
        g.draw(new Line2D.Double(pageStartPoint, pageEndPoint));

        // Draw the minor ticks. CAP_SQUARE would work fine for this,
        // but with aliasing, you can see the back end of the ticks
        // sticking out the other side of the spine, so use CAP_BUTT
        // instead.

        g.setStroke(new BasicStroke((float) (scale * lineWidth),
                                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

        double tickLength = tickDelta * m * 0.2;
        Point2D.Double tickOffset
            = new Point2D.Double(-pageDelta.y * tickLength / distance,
                                 pageDelta.x * tickLength / distance);

        {
            long starti = (long) Math.ceil((start - 1e-6 * (end - start)) / tickDelta);
            long endi = (long) Math.floor((end + 1e-6 * (end - start)) / tickDelta);
            Point2D.Double tmpPoint = new Point2D.Double();
            for (long i = starti; i <= endi; ++i) {
                double logical = i * tickDelta;
                Point2D.Double location
                    = toPhysical(logical, pageStartPoint, pageEndPoint);
                if (tickRight) {
                    tmpPoint.x = location.x + tickOffset.x;
                    tmpPoint.y = location.y + tickOffset.y;
                    g.draw(new Line2D.Double(location, tmpPoint));
                }                
                if (tickLeft) {
                    tmpPoint.x = location.x - tickOffset.x;
                    tmpPoint.y = location.y - tickOffset.y;
                    g.draw(new Line2D.Double(location, tmpPoint));
                }                
            }
        }

        tickOffset.x *= 2;
        tickOffset.y *= 2;

        String formatString = " "
            + RulerTick.formatString(start, end, textDelta) + " ";

        {
            long starti = (long) Math.ceil((start - 1e-6 * (end - start)) / textDelta);
            long endi = (long) Math.floor((end + 1e-6 * (end - start)) / textDelta);
            Point2D.Double tmpPoint = new Point2D.Double();
            AffineTransform oldTransform = g.getTransform();

            for (long i = starti; i <= endi; ++i) {
                double logical = i * textDelta;
                Point2D.Double location
                    = toPhysical(logical, pageStartPoint, pageEndPoint);
                Point2D.Double anchor = null;
                if (tickRight) {
                    tmpPoint.x = location.x + tickOffset.x;
                    tmpPoint.y = location.y + tickOffset.y;
                    g.draw(new Line2D.Double(location, tmpPoint));
                    if (labelAnchor == LabelAnchor.LABEL_RIGHT) {
                        anchor = (Point2D.Double) tmpPoint.clone();
                    }
                }                
                if (tickLeft) {
                    tmpPoint.x = location.x - tickOffset.x;
                    tmpPoint.y = location.y - tickOffset.y;
                    g.draw(new Line2D.Double(location, tmpPoint));
                    if (labelAnchor == LabelAnchor.LABEL_LEFT) {
                        anchor = (Point2D.Double) tmpPoint.clone();
                    }
                }

                if (anchor == null) {
                    anchor = (Point2D.Double) location.clone();
                }

                g.rotate(theta + textAngle, anchor.x, anchor.y);
                LabelDialog.drawString(g, String.format(formatString, logical),
                                       anchor.x, anchor.y, xWeight, yWeight);
                g.setTransform(oldTransform);
            }
        }

        g.setStroke(oldStroke);
        g.setFont(oldFont);
    }

    /** @return the least value t such that a second rectangle r2 that
        is offset from r1's location by offset*t but is otherwise
        identical to r1 would not overlap r1. */
    public double separate(Rectangle2D r1, Point2D.Double offset) {
        // The two rectangles are separate if their x ranges do not
        // overlap OR their y ranges do not overlap.
        if (offset.x == 0 && offset.y == 0) {
            throw new IllegalArgumentException("Offset is 0");
        }

        double minT = (offset.x == 0) ? 1e199
            : r1.getWidth()/Math.abs(offset.x);

        if (offset.y != 0) {
            minT = Math.min(minT, r1.getHeight()/Math.abs(offset.y));
        }

        return minT;
    }
}
