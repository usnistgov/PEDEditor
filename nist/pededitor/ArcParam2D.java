/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Parameterize an arc of an ellipse along with the arc's control
    points. */
public class ArcParam2D extends BoundedParam2DAdapter
    implements Cloneable {
    Arc2D.Double arc = null;

    public ArcParam2D() { }

    /** Parameterize the given arc. t=0 corresponds to due east,
        regardless of where the arc starts, and t=90 corresponds to
        due north. */
    public ArcParam2D(Arc2D.Double arc) {
        this.arc = (Arc2D.Double) arc.clone();
    }

    @Override public ArcParam2D clone() {
        return new ArcParam2D(arc);
    }

    public ArcParam2D(ArcInterp2D ai) throws UnsolvableException {
        if (ai.isClosed()) {
            arc = new Arc2D.Double(ai.getShape2().getBounds2D(), 0, 360,
                                   Arc2D.OPEN);
        } else {
            arc = (Arc2D.Double) ai.getShape2();
        }
    }

    /** Convert p to an angle relative to the arc center. As with
        Arc2D, angles for non-circular arcs use non-square framing
        rectangles. */
    double toAngle(Point2D p) {
        return ArcMath.toAngle(arc, p);
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

    /** @return a bounded parameterization of the arc.
     * @throws UnsolvableException */
    public static BoundedParam2D create(ArcInterp2D ai) throws UnsolvableException {
        ArcParam2D ep = new ArcParam2D(ai);
        return ep.createSubset(ep.arc.start, ep.arc.start + ep.arc.extent);
    }

    @Override public ArcParam2D createTransformed(AffineTransform xform) {
        if (xform.getShearX() != 0 || xform.getShearY() != 0) {
            throw new UnsupportedOperationException(
                    "ArcParam2D.createTransformed() does not support "
                    + "shear transforms");
        }
        Point2D.Double upperLeft = new Point2D.Double(arc.x, arc.y);
        xform.transform(upperLeft, upperLeft);
        Point2D.Double size = new Point2D.Double(arc.width, arc.height);
        xform.deltaTransform(size, size);
        if (size.x < 0 || size.y < 0) {
            throw new UnsupportedOperationException(
                    "ArcParam2D still doesn't support negative scaling createTransformed().");
        }
        Arc2D.Double newArc = new Arc2D.Double
            (upperLeft.x, upperLeft.y, size.x, size.y, arc.start, arc.extent,
             arc.getArcType());
        return new ArcParam2D(newArc);
    }

    /** For consistency with Arc2D, t is an angle in degrees. */
    @Override public Point2D.Double getLocation(double t) {
        return ArcMath.getLocation(arc, t);
    }

    @Override public Point2D.Double getDerivative(double deg) {
        return ArcMath.getDerivative(arc, deg);
    }

    /** The derivative of an arc is just another arc on a different
        ellipse, but the arc is offset by 90 degrees, so t=0
        corresponds to the top of the circle instead of the right
        tip. */
    @Override protected BoundedParam2D computeDerivative() {
        Arc2D.Double arcNew = new Arc2D.Double(
                -arc.width * Math.PI / 360,
                -arc.height * Math.PI / 360,
                arc.width, arc.height, arc.start + 90, arc.extent,
                arc.getArcType());
        return new OffsetParam2D(new ArcParam2D(arcNew), -90);
    }

    @Override public double[] segIntersections(Line2D segment,
            double t0, double t1) {
        return segIntersections(segment, t0, t1, false);
    }

    /** Return the t values for all intersections of this curve with segment. */
    @Override public double[] lineIntersections(Line2D segment,
            double t0, double t1) {
        return segIntersections(segment, t0, t1, true);
    }

    public double[] segIntersections(Line2D segment, double t0, double t1,
            boolean isLine) {
        return ArcMath.segIntersections(ArcMath.toArc(arc, t0, t1-t0),
                segment, isLine);
    }

    @Override public double area(double t0, double t1) {
        return ArcMath.area(ArcMath.toArc(arc, t0, t1-t0));
    }

    /** The length of the arc on an ellipse with major and minor radii
        r1 and r2 is somewhere between the length of the same arc on a
        circle of radius r1 and on a circle of radius r2. */
    @Override public Estimate length(double t0, double t1) {
        return ArcMath.length(ArcMath.toArc(arc, t0, t1-t0));
    }

    @Override public Rectangle2D.Double getBounds(double t0, double t1) {
        return ArcMath.getBounds(ArcMath.toArc(arc, t0, t1-t0));
    }

    @Override public double[] getBounds(double xc, double yc,
            double t0, double t1) {
        return ArcMath.getBounds(ArcMath.toArc(arc, t0, t1-t0),
                xc, yc);
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName() + "[");
        if (arc != null) {
            s.append(" arc: (" + arc.x + " + " + arc.width + ", " + arc.y + " + " + arc.height + " " + arc.start + " + "
                    + arc.extent);
        }
        s.append("]");
        return s.toString();
    }

    @Override public CurveDistanceRange distance(Point2D p,
            double t0, double t1) {
        return ArcMath.distance(ArcMath.toArc(arc, t0, t1-t0), p);
    }

    @Override public double getMinT() {
        return arc.start;
    }

    @Override public double getMaxT() {
        return arc.start + arc.extent;
    }

    @Override public BoundedParam2D[] straightSegments(double t0, double t1) {
        if ((arc.height == 0) != (arc.width == 0) && t0 < t1) {
            return new BoundedParam2D[] { thisOrSubset(t0, t1) };
        } else {
            return new BoundedParam2D[0];
        }
    }

    @Override public BoundedParam2D[] curvedSegments(double t0, double t1) {
        if (arc.height != 0 && arc.width != 0 && t0 < t1) {
            return new BoundedParam2D[] { thisOrSubset(t0, t1) };
        } else {
            return new BoundedParam2D[0];
        }
    }
    
    /** Return a subset BoundedParam2D that is only valid for t
        values in [minT, maxT]. minT must be greater than or equal to
        the old getMinT(), and maxT must be less than or equal to the
        old getMaxT(). */
    @Override public ArcParam2D createSubset(double minT, double maxT) {
        ArcParam2D res = clone();
        res.arc.start = minT;
        res.arc.extent = maxT - minT;
        return res;
    }
}
