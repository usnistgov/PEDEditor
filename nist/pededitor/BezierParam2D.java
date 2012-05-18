package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

/** Parameterize a Bezier curve of arbitary degree. */
abstract public class BezierParam2D
    extends Parameterization2DAdapter {
    /** Polynomial x(t) coefficients */
    double[] xCoefficients;
    /** Polynomial y(t) coefficients */
    double[] yCoefficients;

    /** Bezier control points. #0 is the start and #(length-1) is the
        end, but intermediate control points usually do not lie on the
        curve. */
    Point2D.Double[] points;

    /** @param points The array of Bezier control points.

        @param t0 The minimum t value (normally 0).

        @param t1 The minimum t value (normally 1).
    */
    public BezierParam2D(Point2D[] points, double t0, double t1) {
        super(points[0], points[points.length-1], t0, t1);
        this.points = Duh.deepCopy(points);
        init(points);
    }

    public static Parameterization2D create
        (Point2D[] points, double t0, double t1) {
        switch (points.length) {
        case 4:
            return new CubicParam2D(points, t0, t1);
        case 3:
            return new QuadParam2D(points, t0, t1);
        case 2:
            return new SegmentParam2D(points[0], points[1], t0, t1);
        case 1:
            return new SegmentParam2D(points[0], points[0], t0, t1);
        default:
            throw new IllegalArgumentException
                ("create() can only handle 1-4 control points");
        }
    }

    protected void init(Point2D[] points) {
        double[] xs = new double[points.length];
        double[] ys = new double[points.length];
        for (int i = 0; i < points.length; ++i) {
            xs[i] = points[i].getX();
            ys[i] = points[i].getY();
        }
        xCoefficients = new double[points.length];
        bezierToPoly(xs, xCoefficients);
        yCoefficients = new double[points.length];
        bezierToPoly(ys, yCoefficients);
    }

    public int getDegree() {
        return Math.max(xCoefficients.length - 1, 0);
    }

    @Override public Point2D.Double getLocation(double t) {
        return new Point2D.Double
            (Polynomial.evaluate(t, xCoefficients),
             Polynomial.evaluate(t, yCoefficients));
    }
        
    @Override public Point2D.Double getGradient(double t) {
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
            poly[0] = bezs[0];
            poly[1] = bezs[1] - bezs[0];
            return;
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
            { // Inverse of quad bezierToPoly matrix
                double k = poly[0];
                double kt = poly[1];
                double kt2 = poly[2];
                bezs[0] = k;
                bezs[1] = k + kt / 2;
                bezs[2] = k + kt + kt2;
                return;
            }
        case 3:
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

    @Override public Parameterization2D derivative() {
        double[] xd = Polynomial.derivative(xCoefficients);
        double[] yd = Polynomial.derivative(yCoefficients);
        return create(Duh.merge(xd, yd), getMinT(), getMaxT());
    }

    /** @return the distance between p and the quadratic Bezier curve
        defined by the given control points. */
    @Override abstract public CurveDistance distance(Point2D p);

    /** Return the t values for all intersections of this spline with segment. */
    @Override public double[] segIntersections(Line2D segment) {
        return segIntersections(segment, false);
    }

    /** Return the t values for all intersections of this spline with segment. */
    @Override public double[] lineIntersections(Line2D segment) {
        return segIntersections(segment, true);
    }

    public double[] segIntersections(Line2D segment, boolean isLine) {
        double sdx = segment.getX2() - segment.getX1();
        double sdy = segment.getY2() - segment.getY1();
        if (sdx == 0 && sdy == 0) {
            // The segment is a point, so the claim that segment
            // doesn't intersect the spline is either true or within
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

        // Let poly = ycs, equate poly(t) = mx + b, and
        // solve for x(t).

        // Equate ycs(t) = mx + b  and solve for x(t).

        double poly[] = (double[]) ycs.clone();

        poly[0] -= b;

        // Divide poly by m.
        for (int i = 0; i < poly.length; ++i) {
            poly[i] /= m;
        }

        // Now we have

        // xcs(t) = x
        // poly(t) = x

        // therefore

        // (poly - xcs) = 0

        // and with that we can solve for t.

        for (int i = 0; i < poly.length; ++i) {
            poly[i] -= xcs[i];
        }

        for (double t: Polynomial.solve(poly)) {
            if (!inRange(t)) {
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

    @Override public Rectangle2D.Double getBounds() {
        double[] xBounds = Polynomial.getBounds
            (xCoefficients, getMinT(), getMaxT());
        double[] yBounds = Polynomial.getBounds
            (yCoefficients, getMinT(), getMaxT());
        return new Rectangle2D.Double
            (xBounds[0], yBounds[0],
             xBounds[1] - xBounds[0], yBounds[1] - yBounds[0]);
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
        if (getMinT() != 0 || getMaxT() != 1) {
            s.append(" t in [" + getMinT() + ", " + getMaxT() + "]");
        }
        s.append("]");
        return s.toString();
    }
}
