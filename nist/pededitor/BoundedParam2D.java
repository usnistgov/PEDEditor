package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/* Interface for curves in two dimensions parameterized by t over the
   domain [getMinT(), getMaxT()]. */
public interface Parameterization2D {
    /** Return the maximum valid t value for this curve. */
    double getMinT();
    /** Return the minimum valid t value for this curve. */
    double getMaxT();

    /** Increasing the maximum t value may be prohibited, but
        decreasing the maximum t value to a value no less than the
        minimum is always allowed. */
    void setMaxT(double t);
    /** Decreasing the minimum t value may be prohibited, but
        increasing the minimum t value to a value no greater than
        the maximum is always allowed. */
    void setMinT(double t);

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

    /** Return getLocation(getMinT()). */
    Point2D.Double getStart();
    /** Return getLocation(getMaxT()). */
    Point2D.Double getEnd();

    /** Return the distance between p and this curve. The "point"
        field holds an estimate of the closest point, and the
        "distance" and "t" fields holds the distance and
        parameterization t value for that point, respectively. The
        "minDistance" field holds a lower bound on the distance to the
        true closest point, which may be anywhere between 0 and
        "distance" (if "minDistance" = "distance" then the distance
        computation is exact to within precision limits). This
        computation should be fast; for high accuracy, a user should
        select distance(p, maxError, maxIterations) instead.
    */
    CurveDistanceRange distance(Point2D p);

    /** Compute the distance from p to this curve to within maxError
        of the correct value, unless it takes more than maxIterations
        to compute. In that case, just return the best estimate known
        at that time. */
    CurveDistanceRange distance(Point2D p, double maxError, double maxIterations);

    /** Return the distance between p and getLocation(t). */
    CurveDistance distance(Point2D p, double t);

    /** Return the minimum distance between p and any vertex of the
        curve. */
    CurveDistance vertexDistance(Point2D p);

    /** Return the derivative of this curve with respect to t, or null
        if the derivative is undefined. */
    Parameterization2D derivative();

    /** Return bounds for this curve for t in [minT, maxT]. If the
        bounds cannot be computed exactly, then they should be wider
        than necessary instead of too narrow. */
    Rectangle2D.Double getBounds();

    /** @return an array of t values where segment intersects this. */
    double[] segIntersections(Line2D segment);

    /** @return an array of t values where the line through segment
        intersects this. */
    double[] lineIntersections(Line2D segment);

    Parameterization2D clone();

    /** Divide this object into a union of parts that are disjoint
        except possibly at their endpoints. Bisection is one obvious
        way to subdivide the object, but it might not be most
        efficient. Unless this is a single point, at least two objects
        should be returned. */
    Parameterization2D[] subdivide();
}
