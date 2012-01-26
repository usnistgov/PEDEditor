package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

public class TieLine {
    /** Number of tie lines. Lines are painted only on the interior of
        the tie line region; the angle at which
        outerEdge(ot1)innerEdge(it1) and outerEdge(ot2)innerEdge(it2)
        meet is sectioned into numLines+1 equal parts. */
    public int numLines;
    public StandardStroke stroke;
    public double lineWidth = 1.0;
    /** Each tie line starts at innerEdge somewhere along [it1, it2]
        (the two values may be equal for triangular tie line regions)
        and ends somewhere along outerEdge along [ot1, ot2]. */

    public GeneralPolyline innerEdge;
    public GeneralPolyline outerEdge;
    public double it1, it2, ot1, ot2;

    public TieLine(@JsonProperty("numLines") int numLines,
                    @JsonProperty("lineStyle") StandardStroke stroke) {
        this.numLines = numLines;
        this.stroke = stroke;
    }

    public TieLine clone() {
        TieLine output = new TieLine(numLines, stroke);
        output.it1 = it1;
        output.it2 = it2;
        output.ot1 = ot1;
        output.ot2 = ot2;
        output.lineWidth = lineWidth;
        output.innerEdge = (GeneralPolyline) innerEdge.clone();
        output.outerEdge = (GeneralPolyline) outerEdge.clone();
        return output;
    }

    public Point2D.Double getInnerEdge(double t) {
        return innerEdge.getLocation(t);
    }

    public Point2D.Double getOuterEdge(double t) {
        return outerEdge.getLocation(t);
    }

    @JsonIgnore
    public Point2D.Double getInner1() {
        return getInnerEdge(it1);
    }

    @JsonIgnore
    public Point2D.Double getInner2() {
        return getInnerEdge(it2);
    }

    @JsonIgnore
    public Point2D.Double getOuter1() {
        return getOuterEdge(ot1);
    }

    @JsonIgnore
    public Point2D.Double getOuter2() {
        return getOuterEdge(ot2);
    }

    @JsonIgnore
    public Line2D.Double getEdge1() {
        return new Line2D.Double(getInner1(), getOuter1());
    }

    @JsonIgnore
    public Line2D.Double getEdge2() {
        return new Line2D.Double(getInner2(), getOuter2());
    }

    public Point2D.Double convergencePoint() {
        return Duh.lineIntersection(getOuter1(), getInner1(),
                                    getOuter2(), getInner2());
    }

    boolean isInverted() {
        Point2D.Double i1 = getInner1();
        Point2D.Double i2 = getInner2();
        Point2D.Double o1 = getOuter1();
        Point2D.Double o2 = getOuter2();

        double dot = (i2.x - i1.x) * (o2.x - o1.x)
            + (i2.y - i1.y) * (o2.y - o1.y);

        return (dot < 0);
    }

    public Path2D.Double getPath() {
        Path2D.Double output = new Path2D.Double();

        if (isInverted()) {
            // The inner and outer edges are oriented in opposite
            // directions. If we draw tie lines like this, they will
            // look like an hourglass, converging at a point between
            // the two edges instead of somewhere beyond them. Swap i1
            // and i2 to make the two edges face in the same
            // direction.
            double tmp = ot1;
            ot1 = ot2;
            ot2 = tmp;
        }

        Point2D.Double i1 = getInner1();
        Point2D.Double i2 = getInner2();
        Point2D.Double o1 = getOuter1();
        Point2D.Double o2 = getOuter2();
        Point2D.Double v = convergencePoint();

        // For triangular tie-line regions, i1.equals(i2) or
        // o1.equals(o2), but the midpoint of i1o1 never equals the
        // midpoint of i2o2.

        Point2D.Double mid1 = new Point2D.Double
            ((i1.x + o1.x) / 2, (i1.y + o1.y) / 2);
        Point2D.Double mid2 = new Point2D.Double
            ((i2.x + o2.x) / 2, (i2.y + o2.y) / 2);
        double theta1 = Math.atan2(mid1.y - v.y, mid1.x - v.x);
        double theta2 = Math.atan2(mid2.y - v.y, mid2.x - v.x);

        // Determine whether to proceed clockwise or counterclockwise
        // from theta1 to theta2. We'll do this by taking the midpoint
        // of the segment from the middle t value of the inner edge to
        // the middle t value of the outer edge.
        Point2D.Double iMid = getInnerEdge((it1 + it2) / 2);
        Point2D.Double oMid = getOuterEdge((ot1 + ot2) / 2);
        Point2D.Double midMid = new Point2D.Double
            ((iMid.x + oMid.x) / 2, (iMid.y + oMid.y) / 2);

        double thetaMid = Math.atan2(midMid.y - v.y, midMid.x - v.x);

        // The correct direction of rotation from theta1 to theta2
        // passes through thetaMid; the incorrect direction does not.
        final double twoPi = 2 * Math.PI;

        // Force theta2 >= theta1.
        double theta2Minus1 = theta2 - theta1;
        theta2Minus1 -= twoPi * Math.floor(theta2Minus1 / twoPi);
        theta2 = theta1 + theta2Minus1;

        double thetaMidMinus1 = thetaMid - theta1;
        thetaMidMinus1 -= twoPi * Math.floor(thetaMidMinus1 / twoPi);

        if (thetaMidMinus1 > theta2Minus1) {
            // Theta1 and theta2 are out of order. Swap them.
            double tmp = theta1;
            theta1 = theta2;
            theta2 = tmp;

            // Force theta2 >= theta1.
            theta2Minus1 = theta2 - theta1;
            theta2Minus1 -= twoPi * Math.floor(theta2Minus1 / twoPi);
            theta2 = theta1 + theta2Minus1;
        }

        double deltaTheta = (theta2 - theta1) / (numLines + 1);
        double theta = theta1;

        for (int i = 0; i < numLines; ++i) {
            theta += deltaTheta;
            Line2D.Double line = new Line2D.Double
                (v.x, v.y, v.x + Math.cos(theta), v.y + Math.sin(theta));

            Point2D.Double innerPoint = i1;
            if (!i1.equals(i2)) {
                for (double t: innerEdge.lineIntersectionTs(line)) {
                    if ((it1 < t) == (t < it2)) {
                        innerPoint = innerEdge.getLocation(t);
                        break;
                    }
                }
            }

            Point2D.Double outerPoint = o1;
            if (!o1.equals(o2)) {
                for (double t: outerEdge.lineIntersectionTs(line)) {
                    if ((ot1 < t) == (t < ot2)) {
                        outerPoint = outerEdge.getLocation(t);
                        break;
                    }
                }
            }

            output.moveTo(innerPoint.x, innerPoint.y);
            output.lineTo(outerPoint.x, outerPoint.y);
        }

        return output;
    }

    /** @return a new TieLines that is like this one, but xform has
        been applied to its control points. Note that the smooth of
        the transform is generally not the same as the transform of
        the smoothing. */
    public TieLine createTransformed(AffineTransform xform) {
        TieLine output = clone();
        output.innerEdge = innerEdge.createTransformed(xform);
        output.outerEdge = outerEdge.createTransformed(xform);
        return output;
    }


    /** Draw the path of this GeneralPolyline. The coordinates for
        this path should be defined in the "Original" coordinate
        system, but line widths are defined with respect to the
        "SquarePixel" coordinate system. Also, the output is scaled by
        "scale" before being drawn.
    */
    public void draw(Graphics2D g,
                     AffineTransform originalToSquarePixel,
                     double scale) {
        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        xform.concatenate(originalToSquarePixel);
        createTransformed(xform).draw(g, scale);
    }

    public void draw(Graphics2D g, double scale) {
        stroke.getStroke().draw(g, getPath(), scale * lineWidth);
    }

    public String toString() {
        return "TieLines[numlines=" + numLines + ", stroke = " + stroke
            + ", lineWidth = " + lineWidth
            + ", inner = " + innerEdge + ",  outer = " + outerEdge
            + ", ot1 = " + ot1 + ", ot2 = " + ot2
            + ", it1 = " + it1 + ", it2 = " + it2 + "]";
    }
}
