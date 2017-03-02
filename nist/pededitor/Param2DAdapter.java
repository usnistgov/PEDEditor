/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Class that supports computing distances from a point to a curve
    with a defined derivative. */
abstract public class Param2DAdapter
    implements Param2D {
    // Cache potentially expensive computations.
    transient Param2D deriv = null;

    abstract protected Param2D computeDerivative();

    @Override public Param2D derivative() {
        if (deriv == null) {
            deriv = computeDerivative();
        }
        return deriv;
    }

    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, int maxSteps, double t0, double t1) {
        return BoundedParam2Ds.distance
            (new Param2DBounder(this, t0, t1), p, maxError, maxSteps);
    }
    @Override public CurveDistance distance(Point2D p, double t) {
        Point2D.Double pt = getLocation(t);
        return new CurveDistance(t, pt, pt.distance(p));
    }

    public Estimate length(Precision p, double t0, double t1) {
        // Bezier curves of any reasonable degree are so well-behaved
        // that Romberg integration is a good choice.
        return RombergIntegral.integral
            (new Param2Ds.DLengthDT(this), t0, t1, p);
    }

    @Override public Estimate length
        (double absoluteError, double relativeError, int maxSampleCnt,
         double t0, double t1) {
        Precision p = new Precision();
        p.absoluteError = absoluteError;
        p.relativeError = relativeError;
        p.maxSampleCnt = maxSampleCnt;
        return length(p, t0, t1);
    }

    @Override public BoundedParam2D createSubset(double t0, double t1) {
        return new Param2DBounder(this, t0, t1);
    }

    @Override public Point2D.Double getDerivative(double t) {
        return derivative().getLocation(t);
    }

    @Override public BoundedParam2D[] subdivide(double t0, double t1) {
        return (t0 == t1)
            ? new BoundedParam2D[] { createSubset(t0, t1) }
        : subdivide(t0, (t0 + t1)/2, t1);
    }

    /** Like regular subdivide(), but specify that the region is to be
        broken up into [t0, t1] and [t1, t2]. */
    public final BoundedParam2D[] subdivide(double t0, double t1,
                                                double t2) {
        return new BoundedParam2D[]
            { createSubset(t0, t1), createSubset(t1,t2) };
    }
}
