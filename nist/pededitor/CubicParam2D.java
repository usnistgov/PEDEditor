/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Parameterize a cubic Bezier curve. */
public class CubicParam2D extends BezierParam2D {
    public CubicParam2D(Point2D p0, Point2D p1, Point2D p2,
                                   Point2D pEnd) {
        this(new Point2D[] {p0, p1, p2, pEnd});
    }

    /** @param points The array of 4 Bezier control points. */
    public CubicParam2D(Point2D[] points) {
        super(points);
    }

    /** Distances from points to cubic Bezier curves cannot be solved
        analytically. */
    @Override public CurveDistanceRange distance
        (Point2D p, double t0, double t1) {
        double mid = (t0 + t1) / 2;
        /* Choose a candidate t value using the quadratic
           approximation of this Bezier curve. */
        QuadParam2D quadApprox = new QuadParam2D
            (points[0], getLocation(mid), points[3]);
        double guessT = quadApprox.distance(p, t0, t1).t;
        CurveDistance dist = distance(p, guessT);
        double distlb = BoundedParam2Ds.distanceLowerBound
            (createSubset(t0, t1), p);
        return new CurveDistanceRange(dist, distlb);
    }
}
