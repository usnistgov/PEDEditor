package gov.nist.pededitor;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.*;

import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonProperty;

/** Class describing a ruler whose tick marks describe values from a
    LinearAxis. */
class LinearRuler {
    public static enum LabelAnchor { NONE, LEFT, RIGHT };

    /** Most applications use straight tick marks, but ternary
        diagrams use V-shaped tick marks (probably because internal
        tick marks that are simple line segments would often be masked
        by diagram line segments that pass through the same
        points). */
    public static enum TickType { NORMAL, V };
    
    /** Location of the axis start point in original coordinates. */
    @JsonProperty Point2D.Double startPoint;
    /** Location of the axis end point in original coordinates. */
    @JsonProperty Point2D.Double endPoint;

    /** I recommend that fontSize be approximately 10 times the
        lineWidth -- 9X and 12X are fine, but 30X or 3X will look
        strange. */
    @JsonProperty double fontSize;
    @JsonProperty double lineWidth;

    /** Tick marks act as anchor points for their corresponding
        labels. As in AnchoredLabel, xWeight determines the positioning of
        the label relative to its anchor. */
    @JsonProperty double xWeight;
    /** Tick marks act as anchor points for their corresponding
        labels. As in AnchoredLabel, yWeight determines the positioning of
        the label relative to its anchor. */
    @JsonProperty double yWeight;


    /** Indicate where labels are to be anchored: NONE: No labels;
        LEFT: at the tip of the left-side tick (or left of the spine,
        if there are no left-side ticks); RIGHT: at the tip of the
        right-side tick (or right of the spine, if there are no
        right-side ticks). */
    @JsonProperty LabelAnchor labelAnchor;

    @JsonBackReference LinearAxis axis;

    /** Multiply the axis values by multiplier. Normally multiplier =
        1.0, but multiplier = 100.0 for rulers that show percentages
        without a percent sign. */
    @JsonProperty double multiplier = 1.0;

    /** To simplify axis rotations, textAngle indicates the angle of
        the text relative to the ray from startPoint to endPoint. So
        for a vertical upwards-pointing axis, textAngle = 0 would mean
        that lines of text flow upwards. */
    @JsonProperty double textAngle = 0.0;

    /** True if ticks should extend from the right side of the ruler
        (the side that is on your right as you face from startPoint to
        endPoint). */
    @JsonProperty boolean tickRight = false;
    /** True if ticks should extend from the left side of the ruler.
        If tickRight and tickLeft are both true, then you have normal
        two-sided tick marks. */
    @JsonProperty boolean tickLeft = false;

    /** If nonnegative, an upper limit on the number of big ticks to
        display, even if there is room for more. Big ticks are also
        text ticks, so this also represents a limit on text ticks.
        Also, small ticks only appear between big ticks, so this is a
        limit on small ticks as well. */
    @JsonProperty int maxBigTicks = -1;

    /** extra spacing to use for determining automatic tick spacing,
        expressed as a multiple of the height of a line of text. 0.0 =
        ticks packed about as tightly as legibility permits; 1.0 =
        ticks separated by an additional amount equal to the height of
        a line of text. */
    @JsonProperty double tickPadding = 0.0;

    /** If positive, the fixed delta for big ticks. If zero, there are
        no big ticks. If negative, compute the number of big ticks
        automatically. */
    @JsonProperty double bigTickDelta = -1.0;

    /** If positive, the fixed delta for small ticks. If zero, there are
        no small ticks. If negative, compute the number of small ticks
        automatically. */
    @JsonProperty double tickDelta = -1.0;

    /** If true, put an arrow at the end of the ruler. */
    @JsonProperty boolean startArrow = false;
    /** If true, put an arrow at the end of the ruler. */
    @JsonProperty boolean endArrow = false;

    /** If true, don't put ticks too close to the starting point.
        Reasons to omit ticks close to the start include that interior
        ticks and text might cross each other. Ticks are never
        included too close to the starting point if startArrow is
        true. */
    @JsonProperty boolean keepStartClear = false;

    /** If true, don't put ticks too close to the ending point. (This
        is always treated as true if endArrow is true.) */
    @JsonProperty boolean keepEndClear = false;

    @JsonProperty TickType tickType = TickType.NORMAL;

    /** Normally, you'd want to paint the spine of this axis, but if
        you don't, set drawSpine to false. (One possible reason to set
        it to false: when anti-aliasing is enabled, drawing the same
        line twice can yield an inferior rendering.) */
    @JsonProperty boolean drawSpine = true;

    /** clone() copies but does not clone the axis field, because the
        axis is considered a relation of the ruler instead of an owned
        field. */
    public LinearRuler clone() {
        LinearRuler o = new LinearRuler();
        o.startPoint = (Point2D.Double) startPoint.clone();
        o.endPoint = (Point2D.Double) endPoint.clone();
        o.xWeight = xWeight;
        o.yWeight = yWeight;
        o.fontSize = fontSize;
        o.textAngle = textAngle;
        o.lineWidth = lineWidth;
        o.tickRight = tickRight;
        o.tickLeft = tickLeft;
        o.startArrow = startArrow;
        o.endArrow = endArrow;
        o.labelAnchor = labelAnchor;
        o.tickType = tickType;
        o.axis = axis;
        o.multiplier = multiplier;
        return o;
    }

    /** Start of range of logical values covered by this axis. */
    double getStart() {
        return axis.value(startPoint) * multiplier;
    }
    /** End of range of logical values covered by this axis. */
    double getEnd() {
        return axis.value(endPoint) * multiplier;
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
        Point2D.Double pageStartPoint = new Point2D.Double();
        Point2D.Double pageEndPoint = new Point2D.Double();
        double start = getStart();
        double end = getEnd();
        if (start == end) {
            return; // Weird corner case.
        }

        double astart = Math.min(start, end);
        double aend = Math.max(start, end);

        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        xform.concatenate(originalToSquarePixel);
        xform.transform(startPoint, pageStartPoint);
        xform.transform(endPoint, pageEndPoint);

        Font oldFont = g.getFont();
        g.setFont(new Font(null, 0, (int) Math.ceil(fontSize * scale)));
        FontMetrics fm = g.getFontMetrics();

        Rectangle2D digitBounds = fm.getStringBounds("8", g);

        double distance = pageStartPoint.distance(pageEndPoint);
        Point2D.Double pageDelta
            = new Point2D.Double(pageEndPoint.x - pageStartPoint.x,
                                 pageEndPoint.y - pageStartPoint.y);
        double theta = Math.atan2(pageDelta.y, pageDelta.x);

        // m = page distance divided by logical distance.

        double m = distance / (aend - astart);
        double maxLabels = Math.floor(distance / digitBounds.getWidth());
        double minBigTickDelta = digitBounds.getHeight() / m;

        // Construct an approximation of the largest string that is
        // likely to appear as a label in order to ascertain its size.
        // For example, if the format includes 3 characters before the
        // decimal and 2 after, the largest should be no larger than
        // '888.88'. We'll also add two spaces to insure there is some
        // space between ticks.

        int[] limits = RulerTick.digitSpaceNeeded
            (start, end, RulerTick.roundCeil(minBigTickDelta));
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
        double padding = labelBounds.getHeight() * tickPadding;
        Rectangle2D.Double lb = new Rectangle2D.Double
            (labelBounds.getX(), labelBounds.getY(),
             labelBounds.getWidth() + padding,
             labelBounds.getHeight() + padding);
        Point2D.Double offset
            = new Point2D.Double(Math.cos(textAngle), Math.sin(textAngle));

        // Now we can compute minBigTickDelta more accurately. separate()
        // returns the necessary distance in physical coordinates
        // between label ticks; divide by m to convert to the
        // corresponding difference in logical coordinates.
        minBigTickDelta = separate(lb, offset) / m;

        if (maxBigTicks > 0) {
            minBigTickDelta = Math.max(minBigTickDelta,
                                    (aend - astart) / maxBigTicks);
        } else if (maxBigTicks == 0) {
            minBigTickDelta = (aend - astart) * 100; // Close enough to infinity
        }

        double bigTickD = (bigTickDelta > 0) ? bigTickDelta
            :  RulerTick.roundCeil(minBigTickDelta);

        // tickD = change in logical coordinates between
        // neighboring ticks.
        double tickD = (tickDelta > 0) ? tickDelta
            : RulerTick.nextSmallerRound(bigTickD);

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke((float) (scale * lineWidth)));
        
        if (drawSpine) {
            g.draw(new Line2D.Double(pageStartPoint, pageEndPoint));
        }

        g.setStroke(new BasicStroke((float) (scale * lineWidth),
                                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

        double tickLength = scale * lineWidth * 4;
        Point2D.Double tickOffset
            = new Point2D.Double(-pageDelta.y * tickLength / distance,
                                 pageDelta.x * tickLength / distance);
        Point2D.Double tickVOffset = new Point2D.Double
            (pageDelta.x * tickLength / distance / Math.sqrt(3),
             pageDelta.y * tickLength / distance / Math.sqrt(3));

        double minTickPageDelta = (tickType == TickType.V)
            ? (Duh.length(tickVOffset) * 4)
            : (3 * scale * lineWidth);
        double minTickDelta = minTickPageDelta / m;

        double clearDistance = Math.abs(scale * lineWidth * 8 / m);
        double tickStart = astart;
        if (keepStartClear || startArrow) {
            tickStart += clearDistance;
        }
        double tickEnd = aend;
        if (keepEndClear || endArrow) {
            tickEnd -= clearDistance;
        }

        if (tickD >= minTickDelta && tickDelta != 0) {
            long starti = (long) Math.ceil
                ((tickStart - 1e-6 * (aend - astart)) / tickD);
            long endi = (long) Math.floor
                ((tickEnd + 1e-6 * (aend - astart)) / tickD);
            Point2D.Double tmpPoint = new Point2D.Double();
            for (long i = starti; i <= endi; ++i) {
                double logical = i * tickD;
                Point2D.Double location
                    = toPhysical(logical, pageStartPoint, pageEndPoint);
                if (tickRight) {
                    if (tickType == TickType.V) {
                        tmpPoint.x = location.x + tickOffset.x + tickVOffset.x;
                        tmpPoint.y = location.y + tickOffset.y + tickVOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                        tmpPoint.x = location.x + tickOffset.x - tickVOffset.x;
                        tmpPoint.y = location.y + tickOffset.y - tickVOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                    } else {
                        tmpPoint.x = location.x + tickOffset.x;
                        tmpPoint.y = location.y + tickOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                    }
                }                
                if (tickLeft) {
                    if (tickType == TickType.V) {
                        tmpPoint.x = location.x - tickOffset.x - tickVOffset.x;
                        tmpPoint.y = location.y - tickOffset.y - tickVOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                        tmpPoint.x = location.x - tickOffset.x + tickVOffset.x;
                        tmpPoint.y = location.y - tickOffset.y + tickVOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                    } else {
                        tmpPoint.x = location.x - tickOffset.x;
                        tmpPoint.y = location.y - tickOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                    }
                }                
            }
        }

        // Double the tick length for the text ticks.
        tickOffset.x *= 2;
        tickOffset.y *= 2;
        tickVOffset.x *= 2;
        tickVOffset.y *= 2;

        String formatString = RulerTick.formatString(start, end, bigTickD);

        if (bigTickDelta != 0) {
            long starti = (long) Math.ceil
                ((tickStart - 1e-6 * (aend - astart)) / bigTickD);
            long endi = (long) Math.floor
                ((tickEnd + 1e-6 * (aend - astart)) / bigTickD);
            Point2D.Double tmpPoint = new Point2D.Double();
            AffineTransform oldTransform = g.getTransform();

            for (long i = starti; i <= endi; ++i) {
                double logical = i * bigTickD;
                Point2D.Double location
                    = toPhysical(logical, pageStartPoint, pageEndPoint);

                if (tickRight) {
                    if (tickType == TickType.V) {
                        tmpPoint.x = location.x + tickOffset.x + tickVOffset.x;
                        tmpPoint.y = location.y + tickOffset.y + tickVOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                        tmpPoint.x = location.x + tickOffset.x - tickVOffset.x;
                        tmpPoint.y = location.y + tickOffset.y - tickVOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                    } else {
                        tmpPoint.x = location.x + tickOffset.x;
                        tmpPoint.y = location.y + tickOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                    }
                }                
                if (tickLeft) {
                    if (tickType == TickType.V) {
                        tmpPoint.x = location.x - tickOffset.x - tickVOffset.x;
                        tmpPoint.y = location.y - tickOffset.y - tickVOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                        tmpPoint.x = location.x - tickOffset.x + tickVOffset.x;
                        tmpPoint.y = location.y - tickOffset.y + tickVOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                    } else {
                        tmpPoint.x = location.x - tickOffset.x;
                        tmpPoint.y = location.y - tickOffset.y;
                        g.draw(new Line2D.Double(location, tmpPoint));
                    }
                }                

                if (labelAnchor != LabelAnchor.NONE) {
                    // If there is a tick on the same side as the
                    // label, then use the tip of the tick (or the
                    // midpoint of the two tips of a V-type tick) as
                    // the label anchor. But if there is no tick on
                    // the same side as the label, displace the anchor
                    // a small distance away from the middle of the
                    // ruler spine. (There are two reasons for this:
                    // first, the middle of the ruler spine is half a
                    // line-width away from the ruler spine edge,
                    // while the tick-mark tip is the real edge of the
                    // tick-mark; second, it looks busier to have a
                    // number very close to a line segment than to
                    // have a number very close to just the tip of a
                    // line segment.)

                    double mul
                        = ((labelAnchor == LabelAnchor.LEFT)
                           ? (tickLeft ? -1.0 : -1.0/3)
                           : (tickRight ? 1.0 : 1.0/3));
                    Point2D.Double anchor = new Point2D.Double
                        (location.x + mul * tickOffset.x,
                         location.y + mul * tickOffset.y);

                    g.rotate(theta + textAngle, anchor.x, anchor.y);
                    LabelDialog.drawString
                        (g, " " + String.format(formatString, logical).trim() + " ",
                                           anchor.x, anchor.y, xWeight, yWeight);
                    g.setTransform(oldTransform);
                }
            }
        }

        if (startArrow) {
            g.fill(new Arrow
                   (pageStartPoint.x, pageStartPoint.y,
                    scale * lineWidth, theta + Math.PI));
        }

        if (endArrow) {
            g.fill(new Arrow
                   (pageEndPoint.x, pageEndPoint.y,
                    scale * lineWidth, theta));
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
