/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/** Class describing a ruler whose tick marks describe values from a
    LinearAxis. */
@JsonSerialize(include = Inclusion.NON_DEFAULT)
class LinearRuler implements BoundedParameterizable2D, Decorated {
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
    double fontSize;

    /** Fudge factor is used for kludgy historical reasons. It would
        be nicer if I could get rid of the old save files that use the
        "fontSize" property, and maybe I could... I 'd probably need to
        use some name other than 'fontSize', though. */
    private final double FONT_SIZE_FUDGE_FACTOR = 1.2;

    @JsonProperty("fontSize") void setJSONFontSize(double v) {
        fontSize = v * FONT_SIZE_FUDGE_FACTOR;
    }
    @JsonProperty("fontSize") double getJSONFontSize() {
        return fontSize / FONT_SIZE_FUDGE_FACTOR;
    }

    @JsonProperty double lineWidth;
    protected Color color = null;

    /** Indicate where labels are to be anchored: NONE: No labels;
        LEFT: at the tip of the left-side tick (or left of the spine,
        if there are no left-side ticks); RIGHT: at the tip of the
        right-side tick (or right of the spine, if there are no
        right-side ticks). */
    @JsonProperty LabelAnchor labelAnchor;

    LinearAxis axis;

    /** Multiply the axis values by multiplier. Normally multiplier =
        1.0, but multiplier = 100.0 for rulers that show percentages
        without a percent sign. */
    @JsonProperty double multiplier = 1.0;

    /** If true, use pow(10, x) for tick marks. */
    @JsonProperty boolean displayLog10 = false;

    /** To simplify axis rotations, textAngle indicates the angle in
        radians of the text relative to the ray from startPoint to
        endPoint. So for a vertical upwards-pointing axis, textAngle =
        0 would mean that lines of text flow upwards. */
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

    /** If NaN, compute the number of big ticks automatically
        (generally recommended). If zero, there are no big ticks.
        Otherwise, the fixed delta for big ticks. */
    @JsonProperty double bigTickDelta = Double.NaN;

    /** If NaN, compute the number of small ticks automatically
        (generally recommended). If zero, there are no small ticks.
        Otherwise, the fixed delta for small ticks. */
    @JsonProperty double tickDelta = Double.NaN;

    /** If true, put an arrow at the end of the ruler. */
    @JsonProperty boolean startArrow = false;
    /** If true, put an arrow at the end of the ruler. */
    @JsonProperty boolean endArrow = false;

    /** If true, don't draw ticks too close to the starting point.
        Suppressing ticks or labels near the start may be needed to
        stop rulers and their decorations from overlapping. */
    @JsonProperty boolean suppressStartTick = false;
    /** Like suppressStartTick, but for the label. */
    @JsonProperty boolean suppressStartLabel = false;
    @JsonProperty boolean suppressEndTick = false;
    @JsonProperty boolean suppressEndLabel = false;

    @JsonProperty TickType tickType = TickType.NORMAL;

    /** Normally, you'd want to paint the spine of this axis, but if
        you don't, set drawSpine to false. (One possible reason to set
        it to false: when anti-aliasing is enabled, drawing the same
        line twice can yield an inferior rendering.) */
    @JsonProperty boolean drawSpine = true;

    /** Define the following only if you don't want to use the
        automatically selected tick range. The start tick is assumed
        to be a big tick. */
    @JsonProperty("tickStart") Double tickStartD = null;
    @JsonProperty("tickEnd") Double tickEndD = null;

    /** Used during serialization. */
    @JsonProperty("axis") String getAxisName() {
        return (axis == null) ? null : (String) (axis.name);
    }

    @JsonProperty("axis") void setJSONAxisName(String name) {
        axisName = name;
    }

    /** Used to temporarily store the name of the axis during
        deserialization. */
    transient String axisName = null;

    /** clone() copies but does not clone the axis field, because the
        axis is considered a relation of the ruler instead of an owned
        field. */
    @Override public LinearRuler clone() {
        LinearRuler o = new LinearRuler();
        o.startPoint = (Point2D.Double) startPoint.clone();
        o.endPoint = (Point2D.Double) endPoint.clone();
        o.fontSize = fontSize;
        o.lineWidth = lineWidth;
        o.axis = axis;
        o.labelAnchor = labelAnchor;
        o.multiplier = multiplier;
        o.displayLog10 = displayLog10;
        o.textAngle = textAngle;
        o.tickRight = tickRight;
        o.tickLeft = tickLeft;
        o.maxBigTicks = maxBigTicks;
        o.tickPadding = tickPadding;
        o.bigTickDelta = bigTickDelta;
        o.tickDelta = tickDelta;
        o.startArrow = startArrow;
        o.endArrow = endArrow;
        o.suppressStartTick = suppressStartTick;
        o.suppressStartLabel = suppressStartLabel;
        o.suppressEndTick = suppressEndTick;
        o.suppressEndLabel = suppressEndLabel;
        o.tickType = tickType;
        o.drawSpine = drawSpine;
        o.tickStartD = tickStartD;
        o.tickEndD = tickEndD;
        o.axisName = axisName;
        return o;
    }

    /** Functions required for the CurveParameterization interface. */
    @JsonIgnore @Override public BoundedParam2D getParameterization() {
        return BezierParam2D.create(startPoint, endPoint);
    }

    /** @return null unless this polyline has been assigned a
        color. */
    public Color getColor() {
        return color;
    }

    /** Set the color. Use null to indicate that the color should be
        the same as whatever was last chosen for the graphics
        context. */
    public void setColor(Color color) {
        this.color = color;
    }

    /** The multiplier is handled differently depending on whether
        displayLog10 is set. If it is set, then a multiplier value of
        100 means that the value -1 is treated like 1 (i.e., 10^-1 is
        treated as 10%). If it is not set, then the multiplier really
        is a multiplier. */
    private double mungeValue(double d) {
        if (displayLog10) {
            return d + Math.log10(multiplier);
        } else {
            return d * multiplier;
        }
    }

    /** Start of range of logical values covered by this axis. */
    double getStart() {
        return mungeValue(axis.value(startPoint));
    }

    /** End of range of logical values covered by this axis. */
    double getEnd() {
        return mungeValue(axis.value(endPoint));
    }

    CuspFigure spinePolyline() {
        return new CuspFigure
            (new CuspInterp2D(startPoint, endPoint),
             StandardStroke.SOLID, lineWidth);
    }

    /** Return { xWeight, yWeight }, which depend upon the value of
     * labelAnchor and textAngle. */
    double[] weights() {
        double quadrant = textAngle / (Math.PI / 2) + 0.01;
        quadrant -= 4 * Math.floor(quadrant / 4);

        double xWeight;
        double yWeight;

        // If the text is parallel or perpendicular to the ruler, then
        // the anchor belongs in the middle (0.5) of one of the two
        // dimensions. For text at other angles, the anchor belongs at
        // a corner (that may not be ideal, but at least it guarantees
        // that the label will not cross the spine of the ruler).

        // These values are correct for right-side text.
        if (quadrant < 0.02) {
            xWeight = 0.5;
            yWeight = 0.0;
        } else if (quadrant < 1) {
            xWeight = 0.0;
            yWeight = 0.0;
        } else if (quadrant < 1.02) {
            xWeight = 0.0;
            yWeight = 0.5;
        } else if (quadrant < 2) {
            xWeight = 0.0;
            yWeight = 1.0;
        } else if (quadrant < 2.02) {
            xWeight = 0.5;
            yWeight = 1.0;
        } else if (quadrant < 3) {
            xWeight = 1.0;
            yWeight = 1.0;
        } else if (quadrant < 3.02) {
            xWeight = 1.0;
            yWeight = 0.5;
        } else  {
            xWeight = 1.0;
            yWeight = 0.0;
        }

        if (labelAnchor == LabelAnchor.LEFT) {
            xWeight = 1.0 - xWeight;
            yWeight = 1.0 - yWeight;
        }

        return new double[] { xWeight, yWeight };
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
        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        xform.concatenate(originalToSquarePixel);
        xform.transform(startPoint, pageStartPoint);
        xform.transform(endPoint, pageEndPoint);

        Stroke oldStroke = g.getStroke();
        double scaledLineWidth = scale * lineWidth;
        // CAP_SQUARE is not appropriate at endpoints that have arrows
        // (the arrows get ugly square noses), but it is appropriate
        // for endpoints that have no arrows, and no cap type is
        // appropriate for lines that have an arrow at just one of the
        // two ends. The solution is to use CAP_BUTT and move the
        // endpoint to simulate CAP_SQUARE when necessary.
        g.setStroke(new BasicStroke((float) scaledLineWidth,
                                    BasicStroke.CAP_BUTT,
                                    BasicStroke.JOIN_MITER));
        
        if (drawSpine) {
            Point2D.Double vec = Geom.normalize
                (new Point2D.Double
                 (pageEndPoint.x - pageStartPoint.x,
                  (pageEndPoint.y - pageStartPoint.y)));
            Point2D.Double psp = (Point2D.Double) pageStartPoint.clone();
            Point2D.Double pep = (Point2D.Double) pageEndPoint.clone();
            if (vec != null) {
                vec.x *= scaledLineWidth / 2;
                vec.y *= scaledLineWidth / 2;
                if (!startArrow) {
                    // Extend the endpoint to simulate CAP_SQUARE.
                    psp.x -= vec.x;
                    psp.y -= vec.y;
                }
                if (!endArrow) {
                    // Extend the endpoint to simulate CAP_SQUARE.
                    pep.x += vec.x;
                    pep.y += vec.y;
                }
            }
            g.draw(new Line2D.Double(psp, pep));
        }

        double start = getStart();
        double end = getEnd();
        if (start == end) {
            g.setStroke(oldStroke);
            return; // Weird corner case.
        }

        double astart = Math.min(start, end);
        double aend = Math.max(start, end);

        double ws[] = weights();
        double xWeight = ws[0];
        double yWeight = ws[1];

        Font oldFont = g.getFont();
        g.setFont(oldFont.deriveFont((float) (fontSize * scale)));
        FontMetrics fm = g.getFontMetrics();

        Rectangle2D digitBounds = fm.getStringBounds("8", g);

        double distance = pageStartPoint.distance(pageEndPoint);
        Point2D.Double pageDelta
            = new Point2D.Double(pageEndPoint.x - pageStartPoint.x,
                                 pageEndPoint.y - pageStartPoint.y);
        double theta = Math.atan2(pageDelta.y, pageDelta.x);

        // m = page distance divided by logical distance.

        double m = distance / (aend - astart);
        double minBigTickDelta = RulerTick.roundCeil(digitBounds.getHeight() / m);

        // Construct an approximation of the largest string that is
        // likely to appear as a label in order to ascertain its size.
        // For example, if the format includes 3 characters before the
        // decimal and 2 after, the largest should be no larger than
        // '888.88'. We'll also add two spaces to insure there is some
        // space between ticks.

        String longestLabel;

        RulerTick rt = new RulerTick();
        if (tickStartD != null) {
            rt.merge(tickStartD);
        } else {
            rt.mergeHigh(start);
        }
        rt.mergeHigh(end);

        if (displayLog10) {
            double minPow10 = Math.ceil(astart - 1e-6);
            double maxPow10 = Math.floor(aend + 1e-6);
            String lo = LogRulerTick.pow10String(minPow10);
            String hi = LogRulerTick.pow10String(maxPow10);
            longestLabel = (lo.length() > hi.length()) ? lo : hi;
        } else {
            RulerTick rt2 = rt.clone();
            rt2.merge(minBigTickDelta);
            longestLabel = rt2.longestString();
        }
        
        Rectangle2D labelBounds = fm.getStringBounds("  " + longestLabel, g);
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

        double bigTickD = bigTickDelta;
        if (Double.isNaN(bigTickDelta)) {
            bigTickD = RulerTick.roundCeil(minBigTickDelta);
            if (labelAnchor != LabelAnchor.NONE && displayLog10
                && bigTickD < 1) {
                // I can only format log 10 labels for integer powers
                // of 10.
                bigTickD = 1;
            }
        }
        rt.merge(bigTickD);

        // tickD = change in logical coordinates between
        // neighboring ticks.
        double tickD = Double.isNaN(tickDelta)
            ? RulerTick.nextSmallerRound(bigTickD)
            : tickDelta;

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
            ? (Geom.length(tickVOffset) * 4)
            : (3 * scale * lineWidth);
        double minTickDelta = minTickPageDelta / m;

        double clearDistance = Math.abs(tickLength * 2 / m);

        /* Minimum value allowed for ticks. */
        double tickStart;
        /* Actual minimum tick value. */
        double actualTickStart;
        if (tickStartD != null) {
            actualTickStart = tickStart = tickStartD;
        } else {
            tickStart = astart;
            actualTickStart = tickD
                * Math.ceil((tickStart - 1e-6 * (aend - astart)) / tickD);
        }

        double tickEnd;
        if (tickEndD != null) {
            tickEnd = tickEndD;
        } else {
            tickEnd = aend;
        }

        Point2D.Double tmpPoint = new Point2D.Double();

        /* Set suppressStartTick if startArrow is set. */
        boolean sst = startArrow || suppressStartTick;
        boolean set = endArrow || suppressEndTick;

        if (Math.abs(tickD) >= minTickDelta && tickD != 0 && tickStart != tickEnd) {
            // Draw small ticks (if requested)

            double smallTickEnd = tickEnd + 1e-6 * (tickEnd - tickStart);

            for (double logical = actualTickStart;
                 (logical < smallTickEnd) == (tickD > 0);
                 logical += tickD) {
                if ((sst && Math.abs(logical - astart) < clearDistance)
                    || (set && Math.abs(logical -aend) < clearDistance)) {
                    continue;
                }
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


        if (bigTickD != 0 && tickStart != tickEnd) {
            // Draw big ticks and labels (if requested)

            actualTickStart = (tickStartD != null) ? tickStartD
                : (bigTickD * Math.ceil((tickStart - 1e-6 * (aend - astart)) / bigTickD));

            String formatString = displayLog10 ? null : rt.formatString();
            AffineTransform oldTransform = g.getTransform();

            double bigTickEnd = tickEnd
                + 1e-6 * (tickEnd - tickStart);
            for (double logical = actualTickStart;
                 (logical < bigTickEnd) == (bigTickD > 0);
                 logical += bigTickD) {
                Point2D.Double location
                    = toPhysical(logical, pageStartPoint, pageEndPoint);

                if (!((sst && Math.abs(logical - astart) < clearDistance)
                      || (set && Math.abs(logical - aend) < clearDistance))) {

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

                if (!(labelAnchor == LabelAnchor.NONE ||
                      (suppressStartLabel && logical < tickStart + clearDistance) ||
                      (suppressEndLabel && logical > bigTickEnd - clearDistance))) {
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
                    String s = displayLog10
                        ? LogRulerTick.pow10String(logical)
                        : String.format(formatString, logical).trim();
                    s = " " + ContinuedFraction.fixMinusZero(s) + " ";
                    LabelDialog.drawString
                        (g, s, anchor.x, anchor.y, xWeight, yWeight);
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

    void reflect() {
        switch (labelAnchor) {
        case LEFT:
            labelAnchor = LabelAnchor.RIGHT;
            break;
        case RIGHT:
            labelAnchor = LabelAnchor.LEFT;
            break;
        default:
            break;
        }
        boolean tickTemp = tickLeft;
        tickLeft = tickRight;
        tickRight = tickTemp;
    }

    public void neaten(AffineTransform toPage) {
        double theta = textAngle
            +  Geom.transformRadians(toPage,
                    Geom.toAngle(Geom.aMinusB(endPoint, startPoint)));
        if (theta != MathWindow.normalizeRadians180(theta)) {
            textAngle = Geom.normalizeRadians(Math.PI + textAngle);
        }
    }
}
