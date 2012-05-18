package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Parameterize a cubic Bezier curve. */
public class CubicParam2D extends BezierParam2D {
    public CubicParam2D(Point2D p0, Point2D p1, Point2D p2,
                                   Point2D pEnd, double t0, double t1) {
        this(new Point2D[] {p0, p1, p2, pEnd}, t0, t1);
    }

    /** @param points The array of 4 Bezier control points.

        @param t0 The minimum t value (normally 0).

        @param t1 The minimum t value (normally 1).
    */
    public CubicParam2D(Point2D[] points, double t0, double t1) {
        super(points, t0, t1);
    }

    @Override public CubicParam2D clone() {
        return new CubicParam2D(points, t0, t1);
    }

    /** distances from points to cubic Bezier curves cannot be solved
        analytically. Instead, compute the distance to the quadratic
        approximation of this Bezier curve and return a
        CurveDistanceRange. */
    @Override public CurveDistance distance(Point2D p) {
        double mid = (getMinT() + getMaxT()) / 2;
        double guessT
            = (new QuadParam2D(points[0],
                                          getLocation(mid), points[3], t0, t1))
            .distance(p).t;
        CurveDistance dist = distance(p, guessT);
        double distlb = Parameterization2Ds.distanceLowerBound(this, p);
        return new CurveDistanceRange(dist.t, dist.point, dist.distance,
                                      distlb);
    }
}
