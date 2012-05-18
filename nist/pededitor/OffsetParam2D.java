package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Offset the t values in a CurveParameterization2D by a fixed value.
    If offset = 0.5, for example, then getMinT() will return 0.5 more
    than the original curve's getMinT() does, and getLocation(0.5)
    returns the same value as the original curve's getLocation(0)
    would, and so on.
 */

public class OffsetParam2D implements Parameterization2D {
    Parameterization2D c;
    double offset;

    /** Warning: c will be incorporated into this object, so later
        modifications to this object may modify c and vice versa. If
        you don't like that, then clone c first. */
    public OffsetParam2D(Parameterization2D c, double offset) {
        this.c = c;
        this.offset = offset;
    }

    @Override public double getMinT() { return c.getMinT() + offset; }
    @Override public double getMaxT() { return c.getMaxT() + offset; }
    @Override public void setMaxT(double t) { c.setMaxT(t - offset); }
    @Override public void setMinT(double t) { c.setMinT(t - offset); }
    @Override public double getNextVertex(double t) {
        return c.getNextVertex(t - offset) + offset;
    }
    @Override public double getLastVertex(double t) {
        return c.getLastVertex(t - offset) + offset;
    }
    @Override public Point2D.Double getLocation(double t) {
        return c.getLocation(t - offset);
    }
    @Override public Point2D.Double getDerivative(double t) {
        return c.getDerivative(t - offset);
    }
    @Override public Point2D.Double getStart() { return c.getStart(); }
    @Override public Point2D.Double getEnd() { return c.getEnd(); }

    @Override public CurveDistance distance(Point2D p) {
        return addOffset(c.distance(p));
    }

    @Override public CurveDistance distance(Point2D p, double t) {
        return addOffset(c.distance(p, t - offset));
    }

    @Override public CurveDistance distance(Point2D p, double maxError,
                                            double maxIterations) {
        return addOffset(c.distance(p, maxError, maxIterations));
    }

    @Override public CurveDistance vertexDistance(Point2D p) {
        return addOffset(c.vertexDistance(p));
    }

    @Override public OffsetParam2D derivative() {
        return new OffsetParam2D(c.derivative(), offset);
    }

    @Override public Rectangle2D.Double getBounds() {
        return c.getBounds();
    }

    @Override public OffsetParam2D clone() {
        return new OffsetParam2D(c.clone(), offset);
    }

    @Override public double[] segIntersections(Line2D segment) {
        double[] res = c.segIntersections(segment);
        for (int i = 0; i < res.length; ++i) {
            res[i] += offset;
        }
        return res;
    }

    @Override public double[] lineIntersections(Line2D segment) {
        double[] res = c.lineIntersections(segment);
        for (int i = 0; i < res.length; ++i) {
            res[i] += offset;
        }
        return res;
    }

    CurveDistance addOffset(CurveDistance cd) {
        cd.t += offset;
        return cd;
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName() + "[" + c);
        if (offset != 0) {
            s.append(" t+" + offset);
        }
        s.append("]");
        return s.toString();
    }
}
