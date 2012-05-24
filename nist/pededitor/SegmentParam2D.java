package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Parameterize a line segment. */
public class SegmentParam2D
    extends Parameterization2DAdapter {

    /** @param p0 The position at time t=0, even if t0 != 0.

        @param pEnd The position at time t=1, even if t1 != 1.

        @param t0 The minimum t value (normally 0).

        @param t1 The minimum t value (normally 1).
    */
    public SegmentParam2D
        (Point2D p0, Point2D pEnd, double t0, double t1) {
        super(p0, pEnd, t0, t1);
    }

    @Override public SegmentParam2D clone() {
        return new SegmentParam2D(p0, pEnd, t0, t1);
    }
        
    @Override public SegmentParam2D derivative() {
        Point2D.Double g = getDerivative(0);
        return new SegmentParam2D(g, g, t0, t1);
    }

    @Override public CurveDistanceRange distance(Point2D p) {
        CurveDistance res =
            CurveDistance.pointSegmentDistance(p, getStart(), getEnd());
        if (t1 > t0) {
            res.t = t0 + res.t / (t1 - t0);
        } else {
            res.t = t0;
        }
        return new CurveDistanceRange(res);
    }

    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, double maxIterations) {
        return distance(p);
    }

    @Override public Rectangle2D.Double getBounds() {
        Rectangle2D r = (new Line2D.Double(getLocation(t0), getLocation(t1)))
            .getBounds2D();
        return new Rectangle2D.Double(r.getX(), r.getY(), 
                                      r.getWidth(), r.getHeight());
    }
        
    @Override public Point2D.Double getDerivative(double t) {
        return new Point2D.Double(pEnd.x - p0.x, pEnd.y - p0.y);
    }

    @Override public Point2D.Double getLocation(double t) {
        return new Point2D.Double
            (p0.x + (pEnd.x - p0.x) * t, p0.y + (pEnd.y - p0.y) * t);
    }

    @Override public double[] segIntersections(Line2D segment) {
        double t = Duh.segmentIntersectionT
            (p0, pEnd, segment.getP1(), segment.getP2());
        if (inDomain(t)) {
            return new double[] { t };
        } else {
            return new double[0];
        }
    }

    @Override public double[] lineIntersections(Line2D segment) {
        double t = Duh.lineIntersectionT
            (p0, pEnd, segment.getP1(), segment.getP2());
        if (inDomain(t)) {
            return new double[] { t };
        } else {
            return new double[0];
        }
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder
            (getClass().getSimpleName() + "[" + Duh.toString(getStart()) + ", "
             + Duh.toString(getEnd()));
        if (getMinT() != 0 || getMaxT() != 1) {
            s.append(" t in [" + getMinT() + ", " + getMaxT() + "]");
        }
        s.append("]");
        return s.toString();
    }
}
