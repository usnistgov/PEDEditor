/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

public class TieLine implements Decorated {
    /** Number of tie lines. Lines are painted only on the interior of
        the tie line region; the angle at which
        outerEdge(ot1)innerEdge(it1) and outerEdge(ot2)innerEdge(it2)
        meet is sectioned into lineCnt+1 equal parts. */
    public int lineCnt;
    public StandardStroke stroke;
    public double lineWidth = 1.0;
    protected Color color = null;

    /** Each tie line starts at innerEdge somewhere along [it1, it2]
        (the two values may be equal for triangular tie line regions)
        and ends somewhere along outerEdge along [ot1, ot2]. */

    @JsonProperty("innerT1") public double it1 = -1.0;
    @JsonProperty("innerT2") public double it2 = -1.0;
    @JsonIgnore public CuspFigure innerEdge;

    /** Used only during JSON deserialization. Later, use getInnerID()
        instead. */
    int innerId = -1;

    @JsonProperty("outerT1") public double ot1 = -1.0;
    @JsonProperty("outerT2") public double ot2 = -1.0;
    @JsonIgnore public CuspFigure outerEdge;

    /** Used only during JSON deserialization. Later, use getOuterID()
        instead. */
    int outerId = -1;

    public TieLine(int lineCnt, StandardStroke stroke) {
        this.lineCnt = lineCnt;
        this.stroke = stroke;
    }

    @JsonCreator
    TieLine(@JsonProperty("lineCnt") int lineCnt,
            @JsonProperty("lineStyle") StandardStroke stroke,
            @JsonProperty("innerId") int innerId,
            @JsonProperty("innerT1") double it1,
            @JsonProperty("innerT2") double it2,
            @JsonProperty("outerId") int outerId,
            @JsonProperty("outerT1") double ot1,
            @JsonProperty("outerT2") double ot2) {
        this.lineCnt = lineCnt;
        this.stroke = stroke;

        this.innerId = innerId;
        this.it1 = it1;
        this.it2 = it2;

        this.outerId = outerId;
        this.ot1 = ot1;
        this.ot2 = ot2;
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

    /** Used during JSON serialization. */
    @JsonProperty int getInnerId() {
        return (innerEdge == null) ? -1 : innerEdge.getJSONId();
    }

    /** Used during JSON serialization. */
    @JsonProperty int getOuterId() {
        return (outerEdge == null) ? -1 : outerEdge.getJSONId();
    }

    public Point2D.Double getInnerEdge(double t) {
        return innerEdge.getLocation(t);
    }

    public Point2D.Double getOuterEdge(double t) {
        return outerEdge.getLocation(t);
    }

    /** Return the location of endpoint #1 of the inner edge. */
    @JsonIgnore public Point2D.Double getInner1() {
        return getInnerEdge(it1);
    }

    /** Return the location of endpoint #2 of the inner edge. */
    @JsonIgnore public Point2D.Double getInner2() {
        return getInnerEdge(it2);
    }

    /** Return the location of endpoint #1 of the outer edge. */
    @JsonIgnore public Point2D.Double getOuter1() {
        return getOuterEdge(ot1);
    }

    /** Return the location of endpoint #2 of the outer edge. */
    @JsonIgnore public Point2D.Double getOuter2() {
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

    /** Return true if the inner and outer edges are oriented in
        opposite directions. If left alone, the first tie line will
        cross the last one, which is almost certainly not the
        intent.

        Sometimes the twists can't be taken out (in particular if i1i2
        crosses o1o2), but I think the best orientation is the one
        that creates a quadrilateral with the maximum area.
    */
    boolean isTwisted() {
        Point2D.Double i1 = getInnerEdge(Math.min(it1, it2));
        Point2D.Double i2 = getInnerEdge(Math.max(it1, it2));
        Point2D.Double o1 = getOuterEdge(Math.min(ot1, ot2));
        Point2D.Double o2 = getOuterEdge(Math.max(ot1, ot2));
        return Math.abs(Duh.signedArea(i1, i2, o2, o1)) <
            Math.abs(Duh.signedArea(i1, i2, o1, o2));
    }

    @JsonIgnore public Path2D.Double getPath() {
        Path2D.Double output = new Path2D.Double();

        boolean twisted = isTwisted();
        BoundedParam2D innerParam = innerEdge.getParameterization();
        BoundedParam2D outerParam = outerEdge.getParameterization();
        AdaptiveRombergIntegral innerLength = Param2Ds.lengthIntegral
            (innerParam.getUnboundedCurve(),
             Math.min(it1,it2),
             Math.max(it1,it2));
        AdaptiveRombergIntegral outerLength = Param2Ds.lengthIntegral
            (outerParam.getUnboundedCurve(),
             Math.min(ot1,ot2),
             Math.max(ot1,ot2));
        Precision p = new Precision();
        p.relativeError = 1e-3;
        p.absoluteError = 0;
        p.maxSampleCnt = 1000;

        for (int i = 0; i < lineCnt; ++i) {
            double quantile = (i + 1.0) / (lineCnt + 1);
            double innerT = AdaptiveRombergIntegralY.quantile
                (innerLength, quantile, p).value;
            double outerT = AdaptiveRombergIntegralY.quantile
                (outerLength,
                 twisted ? 1 - quantile: quantile,
                 p).value;

            Point2D.Double innerPoint = innerParam.getLocation(innerT);
            Point2D.Double outerPoint = outerParam.getLocation(outerT);
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
        TieLine res = new TieLine(lineCnt, stroke);
        res.it1 = it1;
        res.it2 = it2;
        res.ot1 = ot1;
        res.ot2 = ot2;
        res.lineWidth = lineWidth;
        res.innerEdge = innerEdge.createTransformed(xform);
        res.outerEdge = outerEdge.createTransformed(xform);
        res.setColor(getColor());
        return res;
    }

    /** Draw the path of this TieLine. The coordinates for
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

    @Override public String toString() {
        return "TieLines[lineCnt=" + lineCnt + ", stroke = " + stroke
            + ", lineWidth = " + lineWidth
            + ", inner = " + innerEdge + ",  outer = " + outerEdge
            + ", ot1 = " + ot1 + ", ot2 = " + ot2
            + ", it1 = " + it1 + ", it2 = " + it2 + "]";
    }
}
