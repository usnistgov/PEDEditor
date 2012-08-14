package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Parameterize a line segment. */
public class SegmentParam2D extends Param2DAdapter
    implements Param2D {
    Point2D.Double p0;
    Point2D.Double pEnd;

    /** @param p0 The position at time t=0.
        @param pEnd The position at time t=1
    */
    public SegmentParam2D
        (Point2D p0, Point2D pEnd) {
        this.p0 = new Point2D.Double(p0.getX(), p0.getY());
        this.pEnd = new Point2D.Double(pEnd.getX(), pEnd.getY());
    }
        
    @Override protected SegmentParam2D computeDerivative() {
        Point2D.Double g = getDerivative(0);
        return new SegmentParam2D(g, g);
    }

    @Override public CurveDistanceRange distance
        (Point2D p, double t0, double t1) {
        CurveDistance res = CurveDistance.pointSegmentDistance
            (p, getLocation(t0), getLocation(t1));
        if (t1 > t0) {
            res.t = t0 + res.t / (t1 - t0);
        } else {
            res.t = t0;
        }
        return new CurveDistanceRange(res);
    }

    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, int maxSteps, double t0, double t1) {
        return distance(p, t0, t1);
    }

    @Override public Rectangle2D.Double getBounds(double t0, double t1) {
        Rectangle2D r = (new Line2D.Double(getLocation(t0), getLocation(t1)))
            .getBounds2D();
        return new Rectangle2D.Double(r.getX(), r.getY(), 
                                      r.getWidth(), r.getHeight());
    }

    @Override public double[] getBounds
        (double xc, double yc, double t0, double t1) {
        Point2D.Double p0 = getLocation(t0);
        Point2D.Double p1 = getLocation(t1);
        double d0 = p0.x * xc + p0.y * yc;
        double d1 = p1.x * xc + p1.y * yc;
        return new double[] { Math.min(d0,d1), Math.max(d0,d1) };
    }
        
    @Override public Point2D.Double getDerivative(double t) {
        return new Point2D.Double(pEnd.x - p0.x, pEnd.y - p0.y);
    }

    @Override public Point2D.Double getLocation(double t) {
        return new Point2D.Double
            (p0.x + (pEnd.x - p0.x) * t, p0.y + (pEnd.y - p0.y) * t);
    }

    @Override public double[] segIntersections
        (Line2D segment, double t0, double t1) {
        double t = Duh.segmentIntersectionT
            (p0, pEnd, segment.getP1(), segment.getP2());
        if (t >= t0 && t <= t1) {
            return new double[] { t };
        } else {
            return new double[0];
        }
    }

    @Override public double[] lineIntersections
        (Line2D segment, double t0, double t1) {
        double t = Duh.lineIntersectionT
            (p0, pEnd, segment.getP1(), segment.getP2());
        if (t >= t0 && t <= t1) {
            return new double[] { t };
        } else {
            return new double[0];
        }
    }

    @Override public SegmentParam2D createTransformed(AffineTransform xform) {
        return new SegmentParam2D
            (xform.transform(p0, new Point2D.Double()),
             xform.transform(pEnd, new Point2D.Double()));
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder
            (getClass().getSimpleName() + "[" + Duh.toString(p0) + ", "
             + Duh.toString(pEnd));
        s.append("]");
        return s.toString();
    }
}
