/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;

/** Parameterize an arc of an ellipse along with the arc's control
    points. */
public class ArcParam2D extends BoundedParam2DAdapter
    implements Cloneable
{
    Arc2D.Double arc = null;
    /** The ts[] are NOT in reverseOrder if there are at least 3 t
        values and they are sorted counterclockwise -- that is, if you
        label the points of a circle with those t degree values, with
        0 corresponding to east and 90 corresponding to north, and you
        proceed counterclockwise from t_0 to t_1 to t_2, et cetera,
        you never end up crossing t values you already encountered.
        For instance, -10, 30, -20 is not in reverse order, because
        proceeding counterclockwise around the circle from 30 degrees
        to -20 degrees, you do not pass -10 degrees. */
    boolean reverseOrder = false;
    double[] ts;
    transient Arc2D.Double dummyArc = null;

    public ArcParam2D() { }

    /** Parameterize the given arc. t=0 corresponds to due east,
        regardless of where the arc starts, and t=90 corresponds to
        due north. */
    public ArcParam2D(Arc2D.Double arc) {
        this.arc = (Arc2D.Double) arc.clone();
        ts = new double[0];
    }

    @Override public ArcParam2D clone() {
        ArcParam2D res = new ArcParam2D(arc);
        res.reverseOrder = reverseOrder;
        res.ts = (double[]) ts.clone();
        return res;
    }

    public boolean isClosed() {
        return arc.extent == 360;
    }

    /** @return an unbounded parameterization of the ellipse
        containing the arc. */
    public ArcParam2D(ArcInterp2D ai) {
        if (ai.isClosed()) {
            arc = new Arc2D.Double(ai.getShape().getBounds2D(), 0, 360,
                                   Arc2D.OPEN);
        } else {
            arc = (Arc2D.Double) ai.getShape();
        }
        ts = new double[ai.size()];
        reverseOrder = ai.clockwiseControlPoints();
        int size = ai.size();
        double oldAngle = 0;
        for (int i = 0; i < size; ++i) {
            double angle = toAngle(ai.get(reverseOrder ? size - i - 1 : i));
            while (i > 0 && angle <= oldAngle) {
                angle += 360;
            }
            ts[i] = angle;
            oldAngle = angle;
        }
    }

    /** @return the number of control points. */
    int size() { return ts.length; }

    /** Convert p to an angle like Arc2D#setAngleStart() does. */
    double toAngle(Point2D p) {
        if (dummyArc == null) {
            dummyArc = (Arc2D.Double) arc.clone();
        }
        dummyArc.setAngleStart(p);
        return dummyArc.start;
    }

    /** Return the index of the control point that is on or inside the
        arc and for which the arc from the control point proceeding
        counterclockwise to t is shortest. */
    @Override public double getLastVertex(double t) {
        if (size() <= 2) {
            return 0;
        }
        double lastT = 0;
        double leastDistance = 1e6;
        for (int i = 0; i < size(); ++i) {
            if (!Geom.degreesInRange(ts[i], ts[0], ts[ts.length - 1])) {
                // Ignore control points that are not inside the arc.
                continue;
            }
            double distance = t - ts[i];
            distance -= 360 * Math.floor(distance / 360);
            if (distance < leastDistance) {
                leastDistance = distance;
                lastT = ts[i];
            }
        }
        return lastT;
    }

    /** Return the index of the control point that is on or inside the
        arc and for which the arc from the control point proceeding
        counterclockwise to t is shortest. */
    @Override public double getNextVertex(double t) {
        if (size() <= 2) {
            return 0;
        }
        double nextT = 0;
        double leastDistance = 1e6;
        for (int i = 0; i < size(); ++i) {
            if (!Geom.degreesInRange(ts[i], ts[0], ts[ts.length - 1])) {
                // Ignore control points that are not inside the arc.
                continue;
            }
            double distance = ts[i] - t;
            distance -= 360 * (Math.ceil(distance / 360) - 1);
            if (distance < leastDistance) {
                leastDistance = distance;
                nextT = ts[i];
            }
        }
        return nextT;
    }

    /** Convert a t value to an index into the original control point
        list. "Original" is important because the original list may
        have been reversed with respect to the ts[] array. */
    public int tToIndex(double t) {
        if (size() <= 1) {
            return 0;
        }
        int nearestIndex = -1;
        double leastDistance = 1e6;
        for (int i = 0; i < size(); ++i) {
            if (!Geom.degreesInRange(ts[i], ts[0], ts[ts.length - 1])) {
                // Ignore control points that are not inside the arc.
                continue;
            }
            double distance = ts[i] - t;
            distance -= 360 * Math.floor(distance / 360);
            if (distance > 180) {
                distance = 360 - distance;
            }
            if (distance < leastDistance) {
                leastDistance = distance;
                nearestIndex = i;
            }
        }
        if (reverseOrder) {
            nearestIndex = size() - 1 - nearestIndex;
        }
        return nearestIndex;
    }

    public double indexToT(int index) {
        return ts[index];
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

    /** @return a bounded parameterization of the arc. */
    public static BoundedParam2D create(ArcInterp2D ai) {
        ArcParam2D ep = new ArcParam2D(ai);
        return ep.createSubset(ep.arc.start, ep.arc.start + ep.arc.extent);
    }

    @Override public ArcParam2D createTransformed(AffineTransform xform) {
        if (xform.getShearX() != 0 || xform.getShearY() != 0) {
            throw new UnsupportedOperationException
                ("ArcParam2D.createTransformed() does not support shear transforms");
        }
        Point2D.Double upperLeft = new Point2D.Double(arc.x, arc.y);
        xform.transform(upperLeft, upperLeft);
        Point2D.Double size = new Point2D.Double(arc.width, arc.height);
        xform.deltaTransform(size, size);
        Arc2D.Double newArc = new Arc2D.Double
            (upperLeft.x, upperLeft.y, size.x, size.y, arc.start, arc.extent,
             arc.getArcType());
        return new ArcParam2D(newArc);
    }

    static double PI_OVER_180 = Math.PI/180;

    /** For consistency with Arc2D, t is an angle in degrees. */
    @Override public Point2D.Double getLocation(double t) {
        return getLocation(t, arc);
    }

    /** For consistency with Arc2D, t is an angle in degrees. */
    static public Point2D.Double getLocation(double t, RectangularShape arc) {
        return new Point2D.Double
            (arc.getX() + arc.getWidth() / 2 * (1 + Math.cos(t * PI_OVER_180)),
             arc.getY() + arc.getHeight() / 2 * (1 + Math.sin(t * PI_OVER_180)));
    }
        
    /** For consistency with Arc2D, t is an angle in degrees. */
    @Override public Point2D.Double getDerivative(double t) {
        return new Point2D.Double
            (-arc.width / 2 * PI_OVER_180 * Math.sin(t * PI_OVER_180),
             arc.height / 2 * PI_OVER_180 * Math.cos(t * PI_OVER_180));
    }

    /** The derivative of an arc is just another arc on a different
        ellipse, but the arc is offset by 90 degrees, so t=0
        corresponds to the top of the circle instead of the right
        tip. */
    @Override protected BoundedParam2D computeDerivative() {
        Arc2D.Double arcNew = new Arc2D.Double
            (-arc.width / 2 * PI_OVER_180,
             -arc.height / 2 * PI_OVER_180,
             arc.width, arc.height, arc.start + 90, arc.extent,
             arc.getArcType());
        return new OffsetParam2D(new ArcParam2D(arcNew), -90);
    }

    @Override public double[] segIntersections
        (Line2D segment, double t0, double t1) {
        return segIntersections(segment, t0, t1, false);
    }

    /** Return the t values for all intersections of this curve with segment. */
    @Override public double[] lineIntersections
        (Line2D segment, double t0, double t1) {
        return segIntersections(segment, t0, t1, true);
    }

    /** Return true if angle a is in [a1, a2]. */
    public static boolean degreeInRange(double a, double a1, double a2) {
        if (a2 == a1 + 360) {
            return true;
        }
        double d1 = a - a1;
        d1 -= 360 * Math.floor(d1 / 360);
        double d2 = a2 - a1;
        d2 -= 360 * Math.floor(d2 / 360);
        return d2 >= d1;
    }

    public double[] segIntersections(Line2D segment, double t0, double t1,
            boolean isLine) {
        double sdx = segment.getX2() - segment.getX1();
        double sdy = segment.getY2() - segment.getY1();
        if (sdx == 0 && sdy == 0) {
            // The segment is a point, so the claim that segment
            // doesn't intersect the curve is either true or within
            // an infinitesimal distance of being true, and we don't
            // guarantee infinite precision, so just return nothing.
            return new double[0];
        }
        boolean swapxy = Math.abs(sdx) < Math.abs(sdy);
        if (swapxy) {
            segment = new Line2D.Double
                (segment.getY1(), segment.getX1(),
                 segment.getY2(), segment.getX2());
            double tmp = sdx;
            sdx = sdy;
            sdy = tmp;
        }

        // Now the segment (with x and y swapped if necessary) has
        // slope with absolute value less than 1. That reduces the
        // number of corner cases and helps avoid numerical
        // instability.

        double m = sdy/sdx; // |m| <= 1
        double b = segment.getY1() - m * segment.getX1();

        // y = mx + b

        double minx = Math.min(segment.getX1(), segment.getX2());
        double maxx = Math.max(segment.getX1(), segment.getX2());

        ArrayList<Double> output = new ArrayList<>();

        double[] qcs = ArcInterp2D.quadraticCoefs(arc);

        if (swapxy) {
            qcs = new double[] {qcs[0], qcs[2], qcs[1], qcs[4], qcs[3]};
        }

        // Substitute y = mx + b into the ellipse equation.

        double poly[] = new double[] {
            qcs[0] + b * qcs[2],
            qcs[1] + m * qcs[2] + 2 * m * b * qcs[4],
            qcs[3] + m * m * qcs[4]};

        for (double x: Polynomial.solve(poly)) {
            double y = m * x + b;
            double t = toAngle(swapxy ? new Point2D.Double(y,x)
                               : new Point2D.Double(x,y));
            
            if (!degreeInRange(t, t0, t1)) {
                continue;
            }

            if (!isLine) {
                if (x < minx || x > maxx) {
                    // Bounds error: the segment domain is x in [minx,
                    // maxx].
                    continue;
                }
            }

            output.add(t);
        }

        double[] o = new double[output.size()];
        for (int i = 0; i < o.length; ++i) {
            o[i] = output.get(i);
        }
        return o;
    }

    @Override public double area(double t0, double t1) {
        // Convert t to radians. It won't affect the result because y is
        // integrated with respect to x, not with respect to t.
        
        t0 *= PI_OVER_180;
        t1 *= PI_OVER_180;
        
        // y(t) = c_y + r_y sin t

        // x(t) = c_x + r_x cos t

        // x'(t) = - r_x sin t

        // integral(t0,t1) y(t) x'(t) =

        // integral(t0, t1) (-r_x c_y sin t - r_x r_y sin^2 t) =

        // eval(t0,t1) -r_x r_y (t - sin t cos t) / 2 + r_x c_y cos t

        double rx = arc.width / 2;
        double ry = arc.height / 2;
        double cy = arc.y + ry;

        double e0 = rx * (cy * Math.cos(t0) - ry * (t0 - Math.sin(t0) * Math.cos(t0)));
        double e1 = rx * (cy * Math.cos(t1) - ry * (t1 - Math.sin(t1) * Math.cos(t1)));
        return e1 - e0;
    }

    /** The length of the arc on an ellipse with major and minor radii
        r1 and r2 is somewhere between the length of the same arc on a
        circle of radius r1 and on a circle of radius r2. */
    @Override public Estimate length(double t0, double t1) {
        double len1 = arc.width * arc.extent * Math.PI / 360;
        double len2 = arc.height * arc.extent * Math.PI / 360;
        Estimate res = new Estimate((len1 + len2) / 2);
        res.setLowerBound(Math.min(len1, len2));
        res.setUpperBound(Math.max(len1, len2));
        return res;
    }

    /** @return the maximum Y value for points on an arc starting at
        angle t0+offset and ending at angle t1+offset, all in
        degrees. */
    static double arcMaxY(double t0, double t1, double offset) {
        if (t1 - t0 >= 360)
            return 1.0;
        t0 += offset;
        t1 += offset;
        t0 -= Math.floor(t0/360) * 360;
        t1 -= Math.floor(t1/360) * 360;
        if (Geom.degreesInRange(90, t0, t1))
            return 1.0;
        return Math.max(Math.sin(t0 * Math.PI / 180), Math.sin(t1 * Math.PI / 180));
    }

    @Override public Rectangle2D.Double getBounds(double t0, double t1) {
        double maxX = arcMaxY(t0, t1, 90);
        double minX = -arcMaxY(t0, t1, -90);
        double maxY = arcMaxY(t0, t1, 0);
        double minY = -arcMaxY(t0, t1, 180);
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    @Override public double[] getBounds
        (double xc, double yc, double t0, double t1) {
        return new double[] {
            -getMax(-xc, -yc, t0, t1), getMax(xc, yc, t0, t1) };
    }

    /** @return the maximum value of f(t) = x(t) * xc + y(t) * yc. */
    double getMax(double xc, double yc, double t0, double t1) {
        double sxc = arc.width / 2 * xc; // scaled xc
        double syc = arc.height / 2 * yc; // scaled yc
        // Now the problem is to find the maximum value of sxc * x +
        // syc * y over an arc on the unit circle.

        double circleMax = Math.sqrt(sxc * sxc + syc * syc);
        if (arc.extent == 360) {
            return circleMax;
        }

        double deg = Math.atan2(syc, sxc) * 180 / Math.PI + 180;
        if (Geom.degreesInRange(deg, t0, t1))
            return circleMax;

        // Rotate the system so circleMax corresponds to an angle of
        // zero. The maximum value of the function occurs at the
        // endpoint nearest to angle zero.
        
        t0 = Math.abs(t0 - deg);
        t1 = Math.abs(t1 - deg);
        t0 -= Math.floor(t0/360) * 360;
        t1 -= Math.floor(t1/360) * 360;
        return circleMax * Math.cos(Math.min(t0, t1) * Math.PI / 180);
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName() + "[");
        boolean first = true;
        for (double t: ts) {
            if (!first) {
                s.append(", ");
            }
            s.append(t);
            first = false;
        }
        s.append("]");
        return s.toString();
    }

    static CurveDistance circleDistance(Point2D p, RectangularShape circle) {
        assert(circle.getWidth() == circle.getHeight());
        double dx = p.getX() - circle.getCenterX();
        double dy = p.getY() - circle.getCenterY();
        double r = circle.getWidth() / 2;
        double t = (r == 0) ? 0
            : (dx == 0 && dy == 0) ? 0
            : (Math.atan2(dy, dx) * 180 / Math.PI);
        Point2D pNear = getLocation(t, circle);
        return new CurveDistance(t, pNear, pNear.distance(p));
    }

    static CurveDistanceRange ellipseDistance(Point2D p,
                                                  RectangularShape ellipse) {
        if (ellipse.getWidth() == ellipse.getHeight()) {
            return new CurveDistanceRange(circleDistance(p, ellipse));
        }

        if (ellipse.getHeight() == 0) {
            Point2D.Double p0 = new Point2D.Double
                (ellipse.getX(), ellipse.getY());
            Point2D.Double p1 = new Point2D.Double
                (p0.x + ellipse.getWidth(), p0.y);
            double d0 = p0.distance(p);
            double d1 = p1.distance(p);
            if (d1 <= d0) {
                return new CurveDistanceRange(0, p1, d1, d1);
            } else {
                return new CurveDistanceRange(180, p0, d0, d0);
            }
        } else if (ellipse.getWidth() == 0) {
            Point2D.Double p0 = new Point2D.Double
                (ellipse.getX(), ellipse.getY());
            Point2D.Double p1 = new Point2D.Double
                (p0.x, p0.y + ellipse.getHeight());
            double d0 = p0.distance(p);
            double d1 = p1.distance(p);
            if (d1 <= d0) {
                return new CurveDistanceRange(90, p1, d1, d1);
            } else {
                return new CurveDistanceRange(-90, p0, d0, d0);
            }
        }

        // Find the range of possible distances by rescaling space to
        // turn the ellipse into a unit circle. Multiply the distance
        // of the point to the unit circle by the ellipse's minor and
        // major axis radii to obtain lower and upper
        // boundsrespectively on the distance of the point to the
        // ellipse.

        Point2D xpoint = new Point2D.Double
            ((p.getX() - ellipse.getCenterX()) / (ellipse.getWidth() / 2),
             (p.getY() - ellipse.getCenterY()) / (ellipse.getHeight() / 2));
        CurveDistance cd = circleDistance(xpoint,
                                          new Rectangle(-1, -1, 2, 2));
        return new CurveDistanceRange
            (cd.t, getLocation(cd.t, ellipse),
             cd.distance * Math.min(Math.abs(ellipse.getHeight()),
                                    Math.abs(ellipse.getWidth())) / 2,
             cd.distance * Math.max(Math.abs(ellipse.getHeight()),
                                    Math.abs(ellipse.getWidth())) / 2);
    }

    @Override public CurveDistanceRange distance
        (Point2D p, double t0, double t1) {
        CurveDistanceRange fed = ellipseDistance(p, arc);
        if (Geom.degreesInRange(fed.t, t0, t1))
            return fed;
        return new CurveDistanceRange(distance(p, t0).minWith(distance(p, t1)));
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

    // TODO The point on an ellipse nearest to the point is always in the
    // same quadrant of the ellipse as that point, and there is always
    // only one nearest point, with distances strictly increasing on
    // each side of that point. That means the optimum can be computed
    // in logarithmic time using a bracketing search such as golden
    // section.
}
