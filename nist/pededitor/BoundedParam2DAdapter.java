/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Class that supports computing distances from a point to a curve
    with a defined derivative. */
abstract public class BoundedParam2DAdapter extends Param2DAdapter
    implements BoundedParam2D {
    // Cache potentially expensive computations.
    transient Rectangle2D bounds = null;

    @Override abstract protected BoundedParam2D computeDerivative();

    @Override public BoundedParam2D derivative() {
        if (deriv == null) {
            deriv = computeDerivative();
        }
        return (BoundedParam2D) deriv;
    }

    @Override public Rectangle2D.Double getBounds() {
        if (bounds == null) {
            bounds = getBounds(getMinT(), getMaxT());
        }
        return (bounds == null) ? null : (Rectangle2D.Double) bounds.clone();
    }

    @Override public CurveDistanceRange distance(Point2D p) { 
        return distance(p, getMinT(), getMaxT());
    }

    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, int maxSteps) {
        return distance(p, maxError, maxSteps, getMinT(), getMaxT());
    }

    @Override public double[] getLinearFunctionBounds (double xc, double yc) {
        return getBounds(xc, yc, getMinT(), getMaxT());
    }

    @Override public double[] segIntersections(Line2D segment) {
        return segIntersections(segment, getMinT(), getMaxT());
    }

    @Override public double[] lineIntersections(Line2D segment) {
        return lineIntersections(segment, getMinT(), getMaxT());
    }

    @Override public BoundedParam2D[] subdivide() {
        return subdivide(getMinT(), getMaxT());
    }

    @Override public Point2D.Double getStart() {
        return getLocation(getMinT());
    }

    @Override public Point2D.Double getEnd() {
        return getLocation(getMaxT());
    }

    public boolean inDomain(double t) {
        return t >= getMinT() && t <= getMaxT(); }

    @Override public Estimate length() {
        return length(getMinT(), getMaxT());
    }

    @Override public Estimate length(double absoluteError,
                                          double relativeError, int maxSteps) {
        return length(absoluteError, relativeError, maxSteps, getMinT(),
                      getMaxT());
    }

    @Override public double area() {
        return area(getMinT(), getMaxT());
    }

}
