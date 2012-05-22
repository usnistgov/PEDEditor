package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Arrays;

/** Parameterize a quadratic Bezier curve in two dimensions. */
public class QuadParam2D extends BezierParam2D {
    /** Middle Bezier control point */
    Point2D.Double p1;

    /** @param p0 The position at time t=0, even if t0 != 0.

        @param p1 Middle Bezier control point.

        @param pEnd The position at time t=1, even if t1 != 1.

        @param t0 The minimum t value (normally 0).

        @param t1 The minimum t value (normally 1).
    */
    public QuadParam2D(Point2D p0, Point2D p1, Point2D pEnd,
                                  double t0, double t1) {
        this(new Point2D[] {p0, p1, pEnd}, t0, t1);
    }

    /** @param points The array of 3 Bezier control points.

        @param t0 The minimum t value (normally 0).

        @param t1 The minimum t value (normally 1).
    */
    public QuadParam2D(Point2D[] points, double t0, double t1) {
        super(points, t0, t1);
        Point2D ip1 = points[1];
        p1 = new Point2D.Double(ip1.getX(), ip1.getY());
    }

    @Override public QuadParam2D clone() {
        return new QuadParam2D(points, t0, t1);
    }

    /** @return the distance between p and the quadratic Bezier curve
        defined by the given control points. */
    @Override public CurveDistance distance(Point2D p) {
        // The formula for a quadratic Bezier is (1-t)^2 p0 + 2t(1-t)
        // p1 + t^2 pEnd, or

        // p0 + 2 (p1 - p0) t + (p0 - 2p1 + pEnd) t^2

        // In addition, WLOG we can shift all points by <-px, -py>
        // (causing the point of comparison to appear at the origin)

        double px = p.getX();
        double py = p.getY();

        double ax = p0.getX() - 2 * p1.getX() + pEnd.getX();
        double bx = 2 * (p1.getX() - p0.getX());
        double cx = p0.getX() - px;

        double ay = p0.getY() - 2 * p1.getY() + pEnd.getY();
        double by = 2 * (p1.getY() - p0.getY());
        double cy = p0.getY() - py;

        if (ax == 0 && ay == 0) {
            // This is a straight segment from p0 to pEnd.
            return (new SegmentParam2D (p0, pEnd, t0, t1))
                .distance(p);
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

        CurveDistance nearest = CurveDistance.min
            (distance(p, t0), distance(p, t1));

        if (xSweep == 0 && ySweep == 0) {
            // The three points are colinear. The curve traces out a
            // path that is parabolic in time but linear in space,
            // like a ball thrown straight up that just falls straight
            // back down again.

            double dot = -xCusp * ax - yCusp * ay;
            if (dot <= 0) {
                // The point of nearest approach is the cusp.
                if (inDomain(tCusp)) {
                    return distance(p, tCusp);
                } else {
                    return nearest;
                }
            } else {
                double rangeMid = (t0 + t1)/2;
                // The nearest approach occurs twice: once at tCusp -
                // deltaT, and once at tCusp + deltaT. The closest
                // approach for t in [t0,t1] is the former if tCusp <
                // rangeMid, and the latter if tCusp > rangeMid.

                double deltaT = dot / (ax * ax + ay * ay);
                double t = tCusp + (tCusp > rangeMid ? -1 : 1) * deltaT;
                if (inDomain(t)) {
                    return distance(p, t);
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

        for (double x: parabolaNearests(transformedPoint, xSqCoefficient)) {
            // Convert x values in the transformed system back to t
            // values in the original system. t = tCusp was
            // transformed into x = 0, and t = tCusp + 1 was
            // transformed into x = sweepLen.
            double t = tCusp + x / sweepLen;

            if (inDomain(t)) {
                nearest = CurveDistance.min(nearest, distance(p, t));
            }
        }

        // TODO UNDO
        System.out.println(this + ".distance" + Duh.toString(p) + " = " + nearest);

        return nearest;
    }

    /** Given

            f(x) = xSqCoef * x^2

        return all real x values such that p.distance(Point2D(x,
        f(x))) attains a local minimum. From one to three solutions
        will be returned. */
    static double[] parabolaNearests(Point2D p, double xSqCoef) {
        // The distance is minimized when the square of the distance
        // is minimized. The square of the distance is locally
        // minimized at all point where the derivative of the distance
        // with respect to x equals 0.

        // distance^2(p, C(x)) = (x - p.x)^2 + (xSqCoef * x^2 - p.y)^2

        double[] roots = new double[3];
        double px = p.getX();
        double py = p.getY();

        // Coefficients of quartic polynomial in x for the square of the distance.

        // double[] quartic = {px * px + py * py, -2 * px, 1 - 2 * xSqCoef * py, 0, xSqCoef * xSqCoef};

        // cubic = (dquartic/dx) / 2
        double[] cubic = {-px, (1 - 2 * xSqCoef * py), 0, 2 * xSqCoef * xSqCoef };

        int rootCnt = CubicCurve2D.solveCubic(cubic, roots);
        return Arrays.copyOf(roots, rootCnt);
    }

    private static void expect(double a, double b) {
        if (Math.abs(a - b) > 1e-6) {
            System.err.println("Expected " + b + ", got " + a);
        }
    }

    public static void main(String[] args) {
        Point2D.Double[] q =
        { new Point2D.Double(0, 5),
          new Point2D.Double(1,3),
          new Point2D.Double(2,5) };
        QuadParam2D param = new QuadParam2D
            (q[0], q[1], q[2], 0, 1);
            
        Point2D.Double[] ps =
            { new Point2D.Double(1,0), // t = 0.5, p = (1,4), d = 4
              new Point2D.Double(-0.5, 3.25), // t = 0.25, p = (0.5, 4.25), d = sqrt(2)
              new Point2D.Double(-3.5, 1.5),
              new Point2D.Double(13, 1.5), // t = 1
              new Point2D.Double(0.9, 4.1), // a bit less than t = 0.5
              new Point2D.Double(0.999, 5.49), // a bit more than t = 0
              new Point2D.Double(1.001, 5.49) // a bit less than t = 1
            };
            
        double[] ds =
            { 0.5,
              0.25,
              0.172035,
              1,
              0.4396933,
              0.0023800,
              0.9976199
            };
        for (int i = 0; i < ps.length; ++i) {
            Point2D p = ps[i];
            double expectedT = ds[i];
            CurveDistance cd = param.distance(p);
            System.out.println(p + " -> " + cd);
            expect(cd.t, expectedT);
        }
    }
}
