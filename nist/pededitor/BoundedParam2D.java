package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public interface Parameterization2D {
    double getMinT();
    double getMaxT();

    /** It may or may not be allowed to increase the maximum t value,
        but it should always be allowed to reduce the maximum t value
        (as long as it is not less than the minimum). */
    void setMaxT(double t);
    /** It may or may not be allowed to reduce the minimum t value,
        but it should always be allowed to increase the minimum t value
        (as long as it does not exceed the maximum). */
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
    Point2D.Double getGradient(double t);

    Point2D.Double getStart();
    Point2D.Double getEnd();

    /** Return the distance between p and this curve. If the distance
        may not be the true minimum, then a CurveDistanceRange is
        returned whose minDistance field will be less than the
        distance value. */
    CurveDistance distance(Point2D p);

    /** Compute the distance from p to this curve to within maxError
        of the correct value, unless it takes more than maxIterations
        to compute. In that case, just return the best estimate known
        at that time. */
    CurveDistance distance(Point2D p, double maxError, double maxIterations);

    /** Return the distance between p and getLocation(t). */
    CurveDistance distance(Point2D p, double t);

    /** Return the minimum distance between p and any vertex of the
        curve. */
    CurveDistance vertexDistance(Point2D p);

    /** Return the derivative of this curve with respect to t. */
    Parameterization2D derivative();

    /** Return conservative bounds for this curve for t in [minT, maxT]. */
    Rectangle2D.Double getBounds();

    /** @return an array of t values where segment intersects this. */
    double[] segIntersections(Line2D segment);

    /** @return an array of t values where the line through segment
        intersects this. */
    double[] lineIntersections(Line2D segment);

    Parameterization2D clone();
}
