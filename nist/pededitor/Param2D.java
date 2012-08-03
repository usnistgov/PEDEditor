package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/* Interface for curves in two dimensions parameterized by t. Many
   methods take t0,t1 parameters which indicate that the answer should
   apply only for the t in [t0,t1] subset of the curve. */
public interface Param2D {
    /* Return the t value of the vertex whose t value is least among
     those greater than t. A vertex is a location that was explicitly
     assigned to lie on the curve. */
    double getNextVertex(double t);
    /* Return the t value of the vertex whose t value is greatest
     among those less than or equal to t. A vertex is a location that
     was explicitly assigned to lie on the curve. */
    double getLastVertex(double t);

    Point2D.Double getLocation(double t);
    Point2D.Double getDerivative(double t);

    /** Return the distance between p and this curve. The "point"
        field holds an estimate of the closest point, and the
        "distance" and "t" fields holds the distance and
        parameterization t value for that point, respectively. The
        "minDistance" field holds a lower bound on the distance to the
        true closest point, which may be anywhere between 0 and
        "distance" (if "minDistance" = "distance" then the distance
        computation is exact to within precision limits). This
        computation should be fast; for high accuracy, a user should
        select distance(p, maxError, maxSteps) instead.
    */
    CurveDistanceRange distance(Point2D p, double t0, double t1);

    /** Compute the distance from p to this curve to within maxError
        of the correct value, unless it takes more than maxSteps
        to compute. In that case, just return the best estimate known
        at that time. */
    CurveDistanceRange distance(Point2D p, double maxError, int maxSteps,
                                double t0, double t1);

    /** Return the distance between p and getLocation(t). */
    CurveDistance distance(Point2D p, double t);

    /** Return the derivative of this curve with respect to t, or null
        if the derivative is undefined. */
    Param2D derivative();

    /** Return bounds for this curve. If the bounds cannot be computed
        exactly, then they should be wider than necessary instead of
        too narrow. */
    Rectangle2D.Double getBounds(double t0, double t1);

    /** Return {min,max} for the function f(x) = x(t) * xc + y(t) * yc. */
    double[] getBounds(double xc, double yc, double t0, double t1);

    /** @return an array of t values where segment intersects this. */
    double[] segIntersections(Line2D segment, double t0, double t1);

    /** @return an array of t values where the line through segment
        intersects this. */
    double[] lineIntersections(Line2D segment, double t0, double t1);

    /** Return a BoundedParam2D that covers the t in (t0,t1) range
        of this item. */
    BoundedParam2D createSubset(double t0, double t1);

    /** Divide [t0,t1] into a union of parts that are disjoint except
        possibly at their endpoints. Bisection is one obvious way to
        subdivide the object, but it might not be most efficient.
        Unless this is a single point, at least two objects should be
        returned. */
    BoundedParam2D[] subdivide(double t0, double t1);
}
