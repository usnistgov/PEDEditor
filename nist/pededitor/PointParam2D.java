package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Parameterize a single point over t in [0,0]. Not real interesting... */
public class PointParam2D implements BoundedParam2D {
    Point2D.Double p0;

    public PointParam2D(Point2D p0) {
        this.p0 = new Point2D.Double(p0.getX(), p0.getY());
    }

    @Override public PointParam2D clone() {
        return new PointParam2D(p0);
    }

    @Override public double getMinT() { return 0; }
    @Override public double getMaxT() { return 0; }

    @Override public Point2D.Double getLocation(double t) {
        return (Point2D.Double) p0.clone();
    }

    @Override public double getNextVertex(double t) { return 0; }
    @Override public double getLastVertex(double t) { return 0; }
        
    @Override public Point2D.Double getDerivative(double t) {
        return null;
    }

    @Override public CurveDistanceRange distance(Point2D p) {
        double d = p0.distance(p);
        return new CurveDistanceRange(0, p0, d, d);
    }

    @Override public Point2D.Double getStart() {
        return (Point2D.Double) p0.clone();
    }

    @Override public Point2D.Double getEnd() {
        return (Point2D.Double) p0.clone();
    }

    @Override public CurveDistance distance(Point2D p, double t) {
        return distance(p);
    }

    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, int maxSteps) {
        return distance(p);
    }

    @Override public BoundedParam2D derivative() {
        return null;
    }

    @Override public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(p0.x, p0.y, 0, 0);
    }

    @Override public double[] getBounds(double xc, double yc) {
        double d = p0.x * xc + p0.y * yc;
        return new double[] { d, d };
    }

    @Override public double[] segIntersections(Line2D segment) {
        return (segment.ptSegDist(p0) == 0)
            ? (new double[] { 0 }) : (new double[0]);
    }

    @Override public double[] lineIntersections(Line2D segment) {
        return (segment.ptLineDist(p0) == 0)
            ? (new double[] { 0 }) : (new double[0]);
    }

    @Override public PointParam2D[] subdivide() {
        // Can't subdivide a point.
        return new PointParam2D[] { this };
    }

    @Override public String toString() {
        return getClass().getSimpleName() + Duh.toString(getStart());
    }

	@Override public BoundedParam2D createSubset(double minT, double maxT) {
		return this;
	}

	@Override public BoundedParam2D createTransformed(AffineTransform xform) {
		return new PointParam2D(xform.transform(p0, new Point2D.Double()));
	}
}
