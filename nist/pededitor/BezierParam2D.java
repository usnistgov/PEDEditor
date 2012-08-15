package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

/** Parameterize a Bezier curve of arbitary degree. */
abstract public class BezierParam2D extends Param2DAdapter {
    /** Polynomial x(t) coefficients */
    final double[] xCoefficients;
    /** Polynomial y(t) coefficients */
    final double[] yCoefficients;
    final Point2D.Double p0;
    final Point2D.Double pEnd;

    Param2D deriv = null;

    /** Bezier control points. #0 is the start and #(length-1) is the
        end, but intermediate control points usually do not lie on the
        curve. */
    final Point2D.Double[] points;

    /** @param points The array of Bezier control points.
    */
    public BezierParam2D(Point2D[] points) {
        this.points = Duh.deepCopy(points);
        int len = points.length;
        double[] xs = new double[len];
        double[] ys = new double[len];
        for (int i = 0; i < len; ++i) {
            xs[i] = points[i].getX();
            ys[i] = points[i].getY();
        }
        xCoefficients = bezierToPoly(xs);
        yCoefficients = bezierToPoly(ys);
        p0 = new Point2D.Double(points[0].getX(), points[0].getY());
        pEnd = new Point2D.Double(points[len - 1].getX(), points[len - 1].getY());
    }

    public static BoundedParam2D create(Point2D... points) {
        return createUnbounded(points).createSubset(0, 1);
    }

    public static Param2D createUnbounded(Point2D... points) {
        switch (points.length) {
        case 4:
            return new CubicParam2D(points);
        case 3:
            return new QuadParam2D(points);
        case 2:
            return new SegmentParam2D(points[0], points[1]);
        case 1:
            return new SegmentParam2D(points[0], points[0]);
        default:
            throw new IllegalArgumentException
                ("create() can only handle 1-4 control points");
        }
    }

    @Override public Param2D createTransformed(AffineTransform xform) {
        Point2D.Double[] xpoints = new Point2D.Double[points.length];
        int i=-1;
        for (Point2D.Double point: points) {
            ++i;
            xform.transform(point, xpoints[i] = new Point2D.Double());
        }
        return createUnbounded(xpoints);
    }

    protected void init(Point2D[] points) {
    }

    public int getDegree() {
        return Math.max(xCoefficients.length - 1, 0);
    }

    @Override public Point2D.Double getLocation(double t) {
        return new Point2D.Double
            (Polynomial.evaluate(t, xCoefficients),
             Polynomial.evaluate(t, yCoefficients));
    }
        
    @Override public Point2D.Double getDerivative(double t) {
        return new Point2D.Double
            (Polynomial.evaluateDerivative(t, xCoefficients),
             Polynomial.evaluateDerivative(t, yCoefficients));
    }

    public Point2D.Double[] getControlPoints() {
        return Duh.deepCopy(points);
    }

    public double[] getXPolynomial() {
        return Arrays.copyOf(xCoefficients, xCoefficients.length);
    }

    public double[] getYPolynomial() {
        return Arrays.copyOf(yCoefficients, yCoefficients.length);
    }

    /** Convert the set of 1-D Bezier values "bezs" to the corresponding
        poynomial "poly". */
    public static void bezierToPoly(double[] bezs, double[] poly) {
        switch (bezs.length) {
        case 0:
            return;
        case 1:
            poly[0] = bezs[0];
            return;
        case 2:
            {
                double b0 = bezs[0];
                double b1 = bezs[1];
                poly[0] = b0;
                poly[1] = b1 - b0;
                return;
            }
        case 3:
            {
                double b0 = bezs[0];
                double b1 = bezs[1];
                double b2 = bezs[2];
                poly[0] = b0;
                poly[1] = 2 * (b1 - b0);
                poly[2] = b2 + b0 - 2 * b1;
                return;
            }
        case 4:
            {
                double b0 = bezs[0];
                double b1 = bezs[1];
                double b2 = bezs[2];
                double b3 = bezs[3];
                poly[0] = b0;
                poly[1] = 3 * (b1 - b0);
                poly[2] = 3 * (b2 - 2 * b1 + b0);
                poly[3] = b3 - b0 + 3 * (b1 - b2);
                return;
            }
        default:
            // Higher values are solvable, but I don't need them.
            throw new IllegalArgumentException
                ("bezierToPoly(" + Arrays.toString(bezs) + ",...) only accepts "
                 + "array length 0-4");
        }
    }

    public static double[] polyToBezier(double[] poly) {
        double[] res = new double[poly.length];
        polyToBezier(poly, res);
        return res;
    }

    public static double[] bezierToPoly(double[] bez) {
        double[] res = new double[bez.length];
        bezierToPoly(bez, res);
        return res;
    }

    /** Convert the quadratic poynomial "poly" to the corresponding set
        of 1-D Bezier points. */
    public static void polyToBezier(double[] poly, double[] bezs) {
        switch (poly.length) {
        case 0:
            return;
        case 1:
            bezs[0] = poly[0];
            return;
        case 2:
            {
                double k = poly[0];
                double kt = poly[1];
                bezs[0] = k;
                bezs[1] = k + kt;
                return;
            }
        case 3:
            { // Inverse of quad bezierToPoly matrix
                double k = poly[0];
                double kt = poly[1];
                double kt2 = poly[2];
                bezs[0] = k;
                bezs[1] = k + kt / 2;
                bezs[2] = k + kt + kt2;
                return;
            }
        case 4:
            { // Inverse of cubic bezierToPoly matrix
                double k = poly[0];
                double kt = poly[1];
                double kt2 = poly[2];
                double kt3 = poly[3];
                bezs[0] = k;
                bezs[1] = k + kt / 3;
                bezs[2] = k + (2.0/3) * kt + kt2 / 3;
                bezs[3] = k + kt + kt2 + kt3;
                return;
            }
        default:
            // Higher values are solvable, but I don't need them.
            throw new IllegalArgumentException
                ("polyToBezier() only accepts array length 0-4");
        }
    }

    @Override protected Param2D computeDerivative() {
        double[] xd = Polynomial.derivative(xCoefficients);
        polyToBezier(xd, xd);
        double[] yd = Polynomial.derivative(yCoefficients);
        polyToBezier(yd, yd);
        return createUnbounded(Duh.merge(xd, yd));
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

    public double[] segIntersections
        (Line2D segment, double t0, double t1, boolean isLine) {
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

        double[] xcs = xCoefficients;
        double[] ycs = yCoefficients;

        if (swapxy) {
            double[] temp = xcs;
            xcs = ycs;
            ycs = temp;
        }

        // Solve y(t) = m x(t) + b, which is equivalent to the
        // polynomial m x(t) - y(t) + b = 0, for t.

        double poly[] = new double[xcs.length];
        for (int i = 0; i < xcs.length; ++i) {
            poly[i] = m * xcs[i] - ycs[i];
        }
        poly[0] += b;

        for (double t: Polynomial.solve(poly)) {
            if (t < t0 || t > t1) {
                continue;
            }

            if (!isLine) {
                double x = Polynomial.evaluate(t, xcs);

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

    @Override public Rectangle2D.Double getBounds(double t0, double t1) {
        double[] xBounds = Polynomial.getBounds(xCoefficients, t0, t1);
        double[] yBounds = Polynomial.getBounds(yCoefficients, t0, t1);
        return new Rectangle2D.Double
            (xBounds[0], yBounds[0],
             xBounds[1] - xBounds[0], yBounds[1] - yBounds[0]);
    }

    @Override public double[] getBounds
        (double xc, double yc, double t0, double t1) {
        int s = xCoefficients.length;
        double[] poly = new double[s];
        for (int i = 0; i < s; ++i) {
            poly[i] = xCoefficients[i] * xc + yCoefficients[i] * yc;
        }
        return Polynomial.getBounds(poly, t0, t1);
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName() + "[");
        boolean first = true;
        for (Point2D p: points) {
            if (!first) {
                s.append(", ");
            }
            s.append(Duh.toString(p));
            first = false;
        }
        s.append("]");
        return s.toString();
    }
}
