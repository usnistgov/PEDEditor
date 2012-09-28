package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Class to build a BoundedParam2D from a Param2D. */
public class Param2DBounder implements BoundedParam2D {
    private final double t0;
    private final double t1;

    private final Param2D c;

    // Cache potentially expensive computations.
    BoundedParam2D deriv = null;
    Rectangle2D bounds = null;

    Param2DBounder(Param2D c, double t0, double t1) {
        this.c = c;
        this.t0 = t0;
        this.t1 = t1;
    }

    public Param2D getUnboundedCurve() {
        return c;
    }

    @Override public double getMinT() { return t0; }
    @Override public double getMaxT() { return t1; }

    @Override public BoundedParam2D createSubset(double t0, double t1) {
        return new Param2DBounder(c, t0, t1);
    }

    @Override public double getNextVertex(double t) {
        return c.getNextVertex(t);
    }
    @Override public double getLastVertex(double t) {
        return c.getLastVertex(t);
    }
    @Override public Point2D.Double getLocation(double t) {
        return c.getLocation(t);
    }
    @Override public Point2D.Double getDerivative(double t) {
        return c.getDerivative(t);
    }
    @Override public CurveDistanceRange distance(Point2D p) { 
        return c.distance(p, t0, t1);
    }
    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, int maxSteps) {
        return c.distance(p, maxError, maxSteps, t0, t1);
    }

    @Override public CurveDistance distance(Point2D p, double t) {
        return c.distance(p, t);
    }

    @Override public BoundedParam2D derivative() {
        if (deriv == null) {
            deriv = new Param2DBounder(c.derivative(), t0, t1);
        }
        return deriv;
    }

    @Override public BoundedParam2D createTransformed
        (AffineTransform xform) {
        return new Param2DBounder(c.createTransformed(xform), t0, t1);
    }

    @Override public Rectangle2D.Double getBounds() {
        if (bounds == null) {
            bounds = c.getBounds(t0, t1);
        }
        return (bounds == null) ? null : (Rectangle2D.Double) bounds.clone();
    }

    @Override public double[] getBounds (double xc, double yc) {
        return c.getBounds(xc, yc, t0, t1);
    }

    @Override public double[] segIntersections(Line2D segment) {
        return c.segIntersections(segment, t0, t1);
    }

    @Override public double[] lineIntersections(Line2D segment) {
        return c.lineIntersections(segment, t0, t1);
    }

    @Override public BoundedParam2D[] subdivide() {
        return c.subdivide(t0,  t1);
    }

    @Override public Point2D.Double getStart() {
        return getLocation(getMinT());
    }

    @Override public Point2D.Double getEnd() {
        return getLocation(getMaxT());
    }

    public boolean inDomain(double t) { return t >= t0 && t <= t1; }

    @Override public String toString() {
        return getClass().getSimpleName() + "[t in [" + getMinT() + ", " + getMaxT() + "] " + c + "]";
    }
}
