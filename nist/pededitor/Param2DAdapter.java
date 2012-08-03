package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Parameterize a Bezier curve of arbitary degree. */
abstract public class Param2DAdapter
    implements Param2D {
    Param2D deriv = null;

    abstract protected Param2D computeDerivative();

    @Override public Param2D derivative() {
        if (deriv == null) {
            deriv = computeDerivative();
        }
        return deriv;
    }

    @Override public double getLastVertex(double t) { return 0; }
    @Override public double getNextVertex(double t) { return 1; }
    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, int maxSteps, double t0, double t1) {
        return BoundedParam2Ds.distance
            (new Param2DBounder(this, t0, t1), p, maxError, maxSteps);
    }
    @Override public CurveDistance distance(Point2D p, double t) {
        Point2D.Double pt = getLocation(t);
        return new CurveDistance(t, pt, pt.distance(p));
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
