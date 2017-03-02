/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TieLine implements Decoration, Cloneable {
    /** Number of tie lines. Lines are painted only on the interior of
        the tie line region; the angle at which
        outerEdge(ot1)innerEdge(it1) and outerEdge(ot2)innerEdge(it2)
        meet is sectioned into lineCnt+1 equal parts. */
    public int lineCnt;
    public StandardStroke lineStyle;
    public double lineWidth = 1.0;
    protected Color color = null;

    @Override public TieLine clone() {
        TieLine res = new TieLine(lineCnt, lineStyle);
        res.lineStyle = lineStyle;
        res.it1 = it1;
        res.it2 = it2;
        res.ot1 = ot1;
        res.ot2 = ot2;
        res.color = color;
        res.lineWidth = lineWidth;
        res.innerEdge = innerEdge;
        res.outerEdge = outerEdge;
        return res;
    }

    /** Each tie line starts at innerEdge somewhere along [it1, it2]
        (the two values may be equal for triangular tie line regions)
        and ends somewhere along outerEdge along [ot1, ot2]. */

    @JsonProperty("innerT1") public double it1 = -1.0;
    @JsonProperty("innerT2") public double it2 = -1.0;
    @JsonIgnore public Interp2DDecoration innerEdge;

    /** Used only during JSON deserialization. Later, use getInnerID()
        instead. */
    int innerId = -1;

    @JsonProperty("outerT1") public double ot1 = -1.0;
    @JsonProperty("outerT2") public double ot2 = -1.0;
    @JsonIgnore public Interp2DDecoration outerEdge;

    /** Used only during JSON deserialization. Later, use getOuterID()
        instead. */
    int outerId = -1;

    public TieLine() { }

    public TieLine(int lineCnt, StandardStroke lineStyle) {
        this.lineCnt = lineCnt;
        this.lineStyle = lineStyle;
    }

    @JsonCreator
    TieLine(@JsonProperty("lineCnt") int lineCnt,
            @JsonProperty("lineStyle") StandardStroke lineStyle,
            @JsonProperty("innerId") int innerId,
            @JsonProperty("innerT1") double it1,
            @JsonProperty("innerT2") double it2,
            @JsonProperty("outerId") int outerId,
            @JsonProperty("outerT1") double ot1,
            @JsonProperty("outerT2") double ot2) {
        this.lineCnt = lineCnt;
        this.lineStyle = lineStyle;

        this.innerId = innerId;
        this.it1 = it1;
        this.it2 = it2;

        this.outerId = outerId;
        this.ot1 = ot1;
        this.ot2 = ot2;
    }

    /** @return null unless this polyline has been assigned a
        color. */
    @Override public Color getColor() {
        return color;
    }

    // TODO All that TieLine requires for the inner and outer edge is
    // a Param2D. However, the ID is attached to the Decoration.

    // The essence of what the TieLine needs is something that
    // supports getJSONId() and getParameterization().

    /** Set the color. Use null to indicate that the color should be
        the same as whatever was last chosen for the graphics
        context. */
    @Override public void setColor(Color color) {
        this.color = color;
    }

    /** Used during JSON serialization. */
    @JsonProperty int getInnerId() {
        return (innerEdge == null) ? -1 : ((HasJSONId) innerEdge).getJsonId();
    }

    /** Used during JSON serialization. */
    @JsonProperty int getOuterId() {
        return (outerEdge == null) ? -1 : ((HasJSONId) outerEdge).getJsonId();
    }

    public Point2D.Double getInnerEdge(double t) {
        return innerEdge.getParameterization().getLocation(t);
    }

    public Point2D.Double getOuterEdge(double t) {
        return outerEdge.getParameterization().getLocation(t);
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
        return Math.abs(Geom.signedArea(i1, i2, o2, o1)) <
            Math.abs(Geom.signedArea(i1, i2, o1, o2));
    }

    @JsonIgnore public Path2D.Double getPath() {
        Path2D.Double output = new Path2D.Double();

        boolean twisted = isTwisted();
        BoundedParam2D innerParam = innerEdge.getParameterization();
        BoundedParam2D outerParam = outerEdge.getParameterization();
        AdaptiveRombergIntegral innerLength = Param2Ds.lengthIntegral
            (innerParam,
             Math.min(it1,it2),
             Math.max(it1,it2));
        AdaptiveRombergIntegral outerLength = Param2Ds.lengthIntegral
            (outerParam,
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

    @Override public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    @Override public double getLineWidth() {
        return lineWidth;
    }

    @Override public void setLineStyle(StandardStroke lineStyle) {
        if (lineStyle != null) {
            this.lineStyle = lineStyle;
        }
    }

    /** For legacy deserialization, equivalent to setLineStyle(). */
    @Deprecated @JsonProperty public void setStroke(StandardStroke lineStyle) {
        setLineStyle(lineStyle);
    }

    @Override public StandardStroke getLineStyle() {
        return lineStyle;
    }

    /** Create a new TieLine linked to a transformed version of the
        curve this tie line is connected to. */
    @Override public void transform(AffineTransform xform) {
        innerEdge = innerEdge.createTransformed(xform);
        outerEdge = outerEdge.createTransformed(xform);
    }

    /** Create a new TieLine linked to a transformed version of the
        curve this tie line is connected to. */
    @Override public TieLine createTransformed(AffineTransform xform) {
        TieLine res = clone();
        res.transform(xform);
        return res;
    }

    @Override public void draw(Graphics2D g) {
        lineStyle.getStroke().draw(g, getPath(), lineWidth);
    }

    @Override public void draw(Graphics2D g, double scale) {
        TieLine dt = createTransformed(AffineTransform.getScaleInstance(
                        scale, scale));
        dt.setLineWidth(scale * getLineWidth());
        dt.draw(g);
    }

    @Override public TieLineHandle[] getHandles(DecorationHandle.Type type) {
        ArrayList<TieLineHandle> output = new ArrayList<>();
        for (TieLineHandle.Type handle: TieLineHandle.Type.values()) {
            output.add(new TieLineHandle(this, handle));
        }
        return output.toArray(new TieLineHandle[0]);
    }

    @Override public String toString() {
        return "TieLines[lineCnt=" + lineCnt + ", lineStyle = " + lineStyle
            + ", lineWidth = " + lineWidth
            + ", inner = " + innerEdge + ",  outer = " + outerEdge
            + ", ot1 = " + ot1 + ", ot2 = " + ot2
            + ", it1 = " + it1 + ", it2 = " + it2 + "]";
    }

    @Override public List<Decoration> requiredDecorations() {
        ArrayList<Decoration> res = new ArrayList<>();
        if (innerEdge instanceof Decoration) {
            res.add((Decoration) innerEdge);
        }
        if (outerEdge instanceof Decoration && innerEdge != outerEdge) {
            res.add((Decoration) outerEdge);
        }
        return res;
    }

    @Override public String typeName() {
        return "tie line";
    }

    @Override
    public void transform(SlopeTransform2D xform) throws UnsolvableException {
        innerEdge = innerEdge.clone();
        innerEdge.transform(xform);
        outerEdge = outerEdge.clone();
        outerEdge.transform(xform);
    }
}
