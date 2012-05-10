package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collections;

/** Simple object to store information about a single point on a
    parameterized curve and its distance from something. */
public class CurveDistance implements Comparable<CurveDistance> {
    public CurveDistance(double t, Point2D point, double distance) {
        this.t = t;
        this.point = new Point2D.Double(point.getX(), point.getY());
        this.distance = distance;
    }

    public String toString() {
        return "CurveDistance[t = " + t + ", p = " + point + ", d = "
            + distance + "]";
    }

    /** t in [0,1] is the curve parameterization variable value. */
    public double t;
    /** Point on the curve closest to the target object. */
    public Point2D.Double point;
    /** Distance at closest approach. */
    public double distance;

    public int compareTo(CurveDistance other) {
        return (distance < other.distance) ? -1
            : (distance > other.distance) ? 1 : 0;
    }

    /** @return other if other is not null and its distance is less
        than this, or this otherwise. */
    public CurveDistance minWith(CurveDistance other) {
        return (other == null) ? this
            : (distance <= other.distance) ? this : other;
    }

    /** @return other if other is not null and its distance is greater
        than this, or this otherwise. */
    public CurveDistance maxWith(CurveDistance other) {
        return (other == null) ? this
            : (distance >= other.distance) ? this : other;
    }

    public static CurveDistance pointSegmentDistance 
        (Point2D p, Point2D l1, Point2D l2) {
        
        double dx = l2.getX() - l1.getX();
        double dy = l2.getY() - l1.getY();
        double dx2 = p.getX() - l1.getX();
        double dy2 = p.getY() - l1.getY();
        double dot = dx * dx2 + dy * dy2;
        double l1l2LengthSq = dx * dx + dy * dy;

        CurveDistance output;

        if (dot < 0 || l1l2LengthSq == 0) {
            output = new CurveDistance(0.0, l1, 0.0);
        } else {
            dot /= l1l2LengthSq;

            output = (dot > 1.0) ? new CurveDistance(1.0, l2, 0.0)
                : new CurveDistance(dot,
                                    new Point2D.Double(l1.getX() + dx * dot,
                                                       l1.getY() + dy * dot), 0.0);
        }

        output.distance = p.distance(output.point);
        return output;
    }

    public static CurveDistance distance(Point2D p1, Point2D p2) {
        return new CurveDistance(0.0, p2, p1.distance(p2));
    }

    private static CurveDistance quadDistance
        (Point2D p, Point2D q0, Point2D q1, Point2D q2, double t) {

        double q0x = q0.getX();
        double q1x = q1.getX();
        double q2x = q2.getX();
        
        double x = (1-t) * ((1-t) * q0x + t * q1x) + t * ((1-t) * q1x + t * q2x);

        double q0y = q0.getY();
        double q1y = q1.getY();
        double q2y = q2.getY();
        
        double y = (1-t) * ((1-t) * q0y + t * q1y) + t * ((1-t) * q1y + t * q2y);

        Point2D.Double q = new Point2D.Double(x, y);
        return new CurveDistance(t, q, q.distance(p));
    }

    /** Given the curve

        C = { (x,y) | y = xSqCoef * x^2 }

        return the x values of the (or a) point q in C such that
        distance(p,q) is locally minimized. From one to three
        solutions will be returned, in order from nearest to
        farthest. */
    public static XAndDistanceSq[] parabolaNearestXAndDistanceSqs(Point2D p, double xSqCoef) {
        // The distance is minimized when the square of the distance
        // is minimized. The square of the distance is locally
        // minimized at all point where the derivative of the distance
        // with respect to x equals 0.

        // distance^2(p, C(x)) = (x - p.x)^2 + (xSqCoef * x^2 - p.y)^2

        double[] roots = new double[3];
        double px = p.getX();
        double py = p.getY();

        // Coefficients of quartic polynomial in x for the square of the distance.
        double[] quartic = {px * px + py * py, -2 * px, 1 - 2 * xSqCoef * py, 0, xSqCoef * xSqCoef};
        // cubic = (dquartic/dx) / 2
        double[] cubic = {-px, (1 - 2 * xSqCoef * p.getY()), 0, 2 * xSqCoef * xSqCoef };

        int rootCnt = CubicCurve2D.solveCubic(cubic, roots);
        if (rootCnt < 1) {
            throw new IllegalStateException("What? There is always at least "
                                            + "one nearest point on a parabola");
        }

        ArrayList<XAndDistanceSq> xad = new ArrayList<>();
        for (int i = 0; i < rootCnt; ++i) {
            xad.add(new XAndDistanceSq(roots[i], Polynomial.evaluate(roots[i], quartic)));
        }
        Collections.sort(xad);
        return xad.toArray(new XAndDistanceSq[0]);
    }

    /** @return the up to 3 local minima of the distance from p to the parabola y = x * x * xSqCoef. */
    public static CurveDistance[] parabolaNearest(Point2D p, double xSqCoef) {
        XAndDistanceSq[] xads = parabolaNearestXAndDistanceSqs(p, xSqCoef);
        CurveDistance[] output = new CurveDistance[xads.length];
        for (int i = 0; i < xads.length; ++i) {
            XAndDistanceSq xad = xads[i];
            output[i] = new CurveDistance
                (xad.x,
                 new Point2D.Double(xad.x, xad.x * xad.x * xSqCoef),
                 Math.sqrt(xad.distanceSq));
        }
        return output;
    }

    /** Just a helper class that parabolaNearest uses for sorting. */
    static private class XAndDistanceSq implements Comparable<XAndDistanceSq> {
        double x;
        double distanceSq;

        XAndDistanceSq(double x, double distanceSq) {
            this.x = x;
            this.distanceSq = distanceSq;
        }

        public int compareTo(XAndDistanceSq other) {
            return (distanceSq < other.distanceSq) ? -1
                : (distanceSq == other.distanceSq) ? 0 : 1;
        }
    }

    /** @return the distance between p and the quadratic Bezier curve
        defined by the given control points. */
    public static CurveDistance pointQuadDistance
        (Point2D p, Point2D q0, Point2D q1, Point2D q2) {
        // The formula for a quadratic Bezier is (1-t)^2 q0 + 2t(1-t)
        // q1 + t^2 q2, or

        // q0 + 2 (q1 - q0) t + (q0 - 2q1 + q2) t^2

        // In addition, WLOG we can shift all points by <-px, -py>
        // (causing the point of comparison to appear at the origin)

        double px = p.getX();
        double py = p.getY();

        double ax = q0.getX() - 2 * q1.getX() + q2.getX();
        double bx = 2 * (q1.getX() - q0.getX());
        double cx = q0.getX() - px;

        double ay = q0.getY() - 2 * q1.getY() + q2.getY();
        double by = 2 * (q1.getY() - q0.getY());
        double cy = q0.getY() - py;

        if (ax == 0 && ay == 0) {
            // This is a straight segment from q0 to q2.
            return pointSegmentDistance(p, q0, q2);
        }


        // A quadratic Bezier is a parabolic segment. The axis of the
        // parabola points in the direction <ax, ay>. At the cusp, the
        // velocity of the parabola's parameterization is
        // perpendicular to its axis, so we have

        // <2ax t + bx, 2ay t + by> . < ax, ay > = 0

        // 2 ax^2 t + ax bx + 2ay^2 t + ay by = 0

        double tCusp = -(ax * bx + ay * by) / 2 / (ax * ax + ay * ay);

        double xCusp = ax * tCusp * tCusp + bx * tCusp + cx;
        double yCusp = ay * tCusp * tCusp + by * tCusp + cy;

        // The velocity of the parameterization at time tCusp equals
        // the parabola's constant sideways (perpendicular to the
        // parabola's axis) sweep.

        double xSweep = 2 * ax * tCusp + bx;
        double ySweep = 2 * ay * tCusp + by;

        // Check the point at t = 0.
        CurveDistance nearest = new CurveDistance(0, q0, q0.distance(p));
        CurveDistance atT1 = new CurveDistance(1, q2, q2.distance(p));
        if (atT1.distance < nearest.distance) {
            nearest = atT1;
        }

        if (xSweep == 0 && ySweep == 0) {
            // The three points are colinear. The curve traces out a
            // path that is parabolic in time but linear in space,
            // like a ball thrown straight up that just falls straight
            // back down again.

            double dot = -xCusp * ax - yCusp * ay;
            if (dot <= 0) {
                // The point of nearest approach is the cusp.
                if (tCusp >= 0 && tCusp <= 1) {
                    return quadDistance(p, q0, q1, q2, tCusp);
                } else {
                    return nearest;
                }
            } else {
                // The nearest approach occurs twice: once at tCusp -
                // deltaT, and once at tCusp + deltaT. The closest
                // approach for t in [0,1] is the former if tCusp <
                // 0.5, and the latter if tCusp > 0.5.

                double deltaT = dot / (ax * ax + ay * ay);
                double t = tCusp + (tCusp > 0.5 ? -1 : 1) * deltaT;
                if (t > 0 && t < 1) {
                    return quadDistance(p, q0, q1, q2, 0);
                } else {
                    return nearest;
                }
            }
        }

        // The usual case: this is a true parabola. Perform a change
        // of basis so that the parabola's axis becomes the Y axis
        // (and the parabola points upwards), and the sweep direction
        // becomes the X axis (sweeping to the right).

        // (xCusp, yCusp) -> (0,0)

        // (xCusp + ax/axisLength, yCusp + ay/axisLength) -> (0,1)

        // (xCusp + xSweep/sweepLength, yCusp + ySweep/sweepLength) -> (1,0)

        // This is satisfied by the inverse of the following matrix:

        // [[xSweep / sweepLength, ax/axisLength], [ySweep / sweepLength, ay/axisLength], [xCusp, yCusp]]

        Point2D.Double axis = new Point2D.Double(ax, ay);
        Point2D.Double axisBasisVector = Duh.normalize(axis);
        Point2D.Double sweep = new Point2D.Double(xSweep, ySweep);
        Point2D.Double sweepBasisVector = Duh.normalize(sweep);

        AffineTransform inverseParabolaBasisXform = new AffineTransform
            (sweepBasisVector.x, axisBasisVector.x, sweepBasisVector.y, axisBasisVector.y, xCusp, yCusp);
        AffineTransform parabolaBasisXform;
        
        try {
            parabolaBasisXform = inverseParabolaBasisXform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException("Normal coordinates aren't really");
        }

        Point2D.Double transformedPoint = new Point2D.Double(0,0);
        parabolaBasisXform.transform(transformedPoint, transformedPoint);

        // When the x value changes by |sweep| relative to the cusp,
        // the y value changes by |axis|. When the x value changes by
        // 1 relative to the cusp, the y value changes by
        // |axis|/(|sweep|^2).

        double sweepLen = Duh.length(sweep);
        double xSqCoefficient = Duh.length(axis) / (sweepLen * sweepLen);

        CurveDistance[] nearests = parabolaNearest(transformedPoint, xSqCoefficient);

        // The nearest points are sorted in increasing order of
        // distance.
        for (CurveDistance cd: nearests) {
            // Of the three fields of cd, the distance value is
            // already correct, while the point and t values need to
            // be transformed back to the original system.

            if (cd.distance >= nearest.distance) {
                break;
            }

            // Transform cd.t back to the original system. The t value
            // was converted to put the cusp at 0 (when it was at
            // tCusp) and to have a sweep rate of 1 length unit per
            // time unit (when the original sweep rate was sweepLen).
            cd.t = tCusp + cd.t / sweepLen;

            if (cd.t >= 0 && cd.t <= 1) {
                // The t value is in range, and the distance is less
                // than the previously known best. So cd really is the
                // point of nearest approach.

                // Transform cd.point back to the original system.
                inverseParabolaBasisXform.transform(cd.point, cd.point);

                // Even the so-called original system wasn't really original; we shifted by <-px, -py>, so undo that.
                cd.point.x += px;
                cd.point.y += py;

                return cd;
            }
        }

        // The point of nearest approach was either t=0 or t=1.
        return nearest;
    }

    public static void main(String[] args) {
        Point2D.Double[] q =
        { new Point2D.Double(0, 5),
          new Point2D.Double(1,3),
          new Point2D.Double(2,5) };
        Point2D.Double[] ps =
            { new Point2D.Double(1,0), // t = 0.5, p = (1,4), d = 4
              new Point2D.Double(-0.5, 3.25), // t = 0.25, p = (0.5, 4.25), d = sqrt(2)
              new Point2D.Double(-3.5, 1.5),
              new Point2D.Double(13, 1.5), // t = 1
              new Point2D.Double(0.9, 4.1), // a bit less than t = 0.5
              new Point2D.Double(0.999, 5.49), // a bit more than t = 0
              new Point2D.Double(1.001, 5.49) // a bit less than t = 1
            };
        for (Point2D p : ps) {
            CurveDistance cd = pointQuadDistance
                (p, q[0], q[1], q[2]);
            System.out.println(p + " -> " + cd);
        }
    }
}
