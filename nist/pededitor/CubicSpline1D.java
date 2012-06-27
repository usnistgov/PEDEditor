package gov.nist.pededitor;

import java.util.*;

/** Class to handle 1D cubic spline interpolations for open curves
    ("polylines" in Java terms) parameterized at arbitary t intervals.
    This is adapted from the procedure suggested in Chapter 3.7.5 of
    the 3rd edition of _Numerical Recipes_.

    Declared final for speed.

    TODO: BezierParam2D already contains a lot of this functionality.
    Remove duplication. (Also, its t domain, [0, (#vertexes -1)],
    differs from the parameterization used here, [0, 1].)
 */
final public class CubicSpline1D {

    /** coefficients[n][d] represents the d-th degree coefficient of
        the cubic polynomial over the segment of the spline that
        connects ys[n] to ys[n+1]. */

    double[][] coefficients = new double[0][4];
    double[] ys;
    double[] xs;

    /** @return a new CubicSpline1D object containing only the portion
        that includes cnt vertexes starting with vertex #start. The
        existing polynomials over that subset are kept, meaning that
        the resulting object will generally not be a natural spline
        (that is, it will generally not have zero second derivative at
        its endpoints). */
    public CubicSpline1D copyOfRange(int start, int cnt) {
        CubicSpline1D output = new CubicSpline1D();
        output.coefficients = new double[cnt - 1][4];
        for (int i = 0; i < cnt - 1; ++i) {
            for (int j = 0; j < 4; ++j) {
                output.coefficients[i][j] = coefficients[start+i][j];
            }
        }

        output.xs = Arrays.copyOfRange(xs, start, start + cnt);
        output.ys = Arrays.copyOfRange(ys, start, start + cnt);
        return output;
    }

    /** Return an array of the second derivative values d2y/dx2 at
        each of the data points. This function is derived from the one
        called "sety2" in Numerical Recipes.
    */
    double[] computeSecondDerivatives(final double[] xs, final double[] ys) {
        // Second derivatives (d2y/dx2) at (y[i], x[i]).
        double[] d2s = new double[xs.length];
        double[] us = new double[xs.length];

        int n = xs.length;
        if (xs.length != ys.length) {
            throw new IllegalArgumentException
                ("length(ys) (" + ys.length + ") != length(xs) ("
                 + xs.length + ")");
        }

        // Use the natural spline: second
        // derivative is 0 at the endpoints.
        d2s[0] = us[0] = 0;

        for (int i = 1; i < n - 1; ++i) {
            double sig = (xs[i] - xs[i-1])/(xs[i+1] - xs[i-1]);
            double p = sig * d2s[i-1] + 2.0;
            d2s[i] = (sig - 1.0) / p;
            double tmpu = (ys[i+1] - ys[i]) / (xs[i+1] - xs[i]) -
                (ys[i] - ys[i-1]) / (xs[i] - xs[i-1]);
            us[i] = (6.0 * tmpu/(xs[i+1] - xs[i-1]) - sig * us[i-1]) / p;
        }

        // Use the natural spline: second
        // derivative is 0 at the endpoints.
        d2s[n-1] = 0.0;

        for (int i = n - 2; i >= 0; --i) {
            d2s[i] = d2s[i] * d2s[i+1] + us[i];
        }

        return d2s;
    }

    CubicSpline1D () {}

    /** The xs and ys arrays are parallel. The xs array should be
        increasing. */
    public CubicSpline1D (final double[] xs, final double[] ys) {
        if (xs.length != ys.length) {
            throw new IllegalArgumentException
                ("length(xs) (" + xs.length + ") != length(ys) ("
                 + ys.length + ")");
        }
        int cnt = xs.length;
        this.xs = Arrays.copyOf(xs, cnt);
        this.ys = Arrays.copyOf(ys, cnt);

        if (cnt < 2) {
            return;
        }

        double[] d2s = computeSecondDerivatives(xs, ys);
        coefficients = new double[cnt-1][4];
        for (int i = 0; i < cnt - 1; ++i) {
            double deltax = xs[i+1] - xs[i];
            double deltay = ys[i+1] - ys[i];

            double[] coefs = coefficients[i];

            coefs[0] = ys[i];

            // Evaluating Numerical Recipes' equation 3.3.5 at x =
            // x[i] yields

            // A = 1, B = 0,

            // dy/dx = deltay / deltax + (1/3) deltax d2s[i] - (1/6) deltax d2s[i+1]

            // dx/dt = deltax


            // d^ky/dt^k = (d^ky/dx^k) deltax^k
            // (Chain rule for f(x(t)) where x(t) = xs[i] + deltax t)

            // dydx = deltay/deltax + deltax * (d2s[i] / 3.0 - d2s[i+1] / 6.0)
            // dydt = dydx * deltax
            double dydt = deltay - deltax * deltax * (d2s[i] / 3.0 + d2s[i+1] / 6.0);
            // dydt(0) = coefs[1]
            coefs[1] = dydt;

            double dy2dx2 = d2s[i];
            double dy2dt2 = dy2dx2 * deltax * deltax;
            // dy2dt2(0) = 2 coefs[2]
            coefs[2] = dy2dt2 / 2;

            // dy3dx3 = (d2s[i+1] - d2s[i]) / deltax;
            // dy3dt3 = dy3dx3 * deltax * deltax * deltax
            double dy3dt3 = (d2s[i+1] - d2s[i]) * deltax * deltax;
            coefs[3] = dy3dt3 / 6.0;
        }
    }

    public double getVertex(int vertexNo) {
        return ys[vertexNo];
    }

    public double value(int segment, double t) {
        double[] poly = coefficients[segment];
        return poly[0] + t * (poly[1] + t * (poly[2] + t * poly[3]));
    }

    /** @return the number of segments in this spline, which equals
        the number of endpoints minus one. */
    public int segmentCnt() {
        return ys.length - 1;
    }

    /* Parameterize the entire curve as t in [0,1] and return the
       value of the curve at the given t value */
    public double value(double t) {
        int cnt = coefficients.length;

        if (t < 0 || cnt == 0) {
            return ys[0];
        }

        if (t >= 1) {
            return ys[cnt];
        }

        t *= cnt;
        double segment = Math.floor(t);

        return value((int) segment, t - segment);
    }

    /* Return (dx/dt) at t. */
    public double derivative(double t) {
        int cnt = coefficients.length;

        if (cnt == 0) {
            return Double.NaN;
        }

        if (t < 0) {
            return derivative(0);
        }

        t *= cnt;
        double segment = Math.floor(t);

        if (t >= cnt) {
            // Return the slope at the last point in the curve.
            double[] last = coefficients[cnt - 1];
            return last[3] * 3 + last[2] * 2 + last[1];
        }

        return slope((int) segment, t - segment);
    }

    public double slope(int segment, double t) {
        double[] poly = coefficients[segment];
        return poly[1] + t * (2 * poly[2] + t * 3 * poly[3]);
    }

    /* Parameterize the entire curve as t in [0,1] and return the
       SegmentAndT corresponding to the given t value */
    public SegmentAndT getSegment(double t) {
        int cnt = coefficients.length;

        if (cnt == 0) {
            throw new IllegalArgumentException("No spline segments exist");
        }

        if (t >= 1) {
            return new SegmentAndT(cnt-1, 1.0);
        }

        t *= cnt;
        double segment = Math.floor(t);

        return new SegmentAndT((int) segment, t - segment);
    }

    /* @return the gross change in y over the given segment. If y
       changes from increasing to decreasing within the segment
       (y'(t) changes sign), then this will exceed |y(1) - y(0)|. */
    public double segmentLength(int segment) {
        return segmentLength(segment, 0., 1.);
    }

    /* @return the gross change in y (so for a round trip from and
       back to a single point, the return value is not zero but
       instead twice the one-way distance) in the given segment
       for t in [t0, t1]. */
    public double segmentLength(int segment, double t0, double t1) {
        double[] poly = coefficients[segment];
        double[] zeroes = quadraticFormula
            (poly[3] * 3, poly[2] * 2, poly[1]);

        int zmin = 0;
        while (zmin < zeroes.length && zeroes[zmin] <= t0) {
            ++zmin;
        }

        int zmax = zeroes.length;
        while (zmax > zmin && zeroes[zmax-1] >= t1) {
            --zmax;
        }

        double length = 0;
        double vlo = value(segment, t0);
        double hi;
        double vhi;
        if (zmax > zmin) {
            hi = zeroes[zmin];
            vhi = value(segment, hi);
            length = Math.abs(vhi - vlo);
            ++zmin;
            vlo = vhi;

            if (zmax > zmin) {
                hi = zeroes[zmin];
                vhi = value(segment, hi);
                length += Math.abs(vhi - vlo);
                vlo = vhi;
            }
        }

        vhi = value(segment, t1);
        length += Math.abs(vhi - vlo);

        return length;
    }


    /** Return the set of 4 cubic Bezier control points that map y(t)
        for the given segment. */
    public void bezier(int segment, double[] controlPoints) {
        double[] coefs = coefficients[segment];
        cubicToBezier(coefs[0], coefs[1], coefs[2], coefs[3], controlPoints);
        // Insure that the second endpoint has zero error.
        controlPoints[3] = ys[segment + 1];
    }


    /** Convert the cubic function k + kt t + kt2 t^2 + kt3 t^3 into a
        set of 4 cubic Bezier control points that represents the same
        function. */
    public static void cubicToBezier(double k, double kt, double kt2, double kt3,
                              double[] controlPoints) {
        // The formula for a Bezier is (1-t)^3 P0 + 3(1-t)^2 t P1 +
        // 3(1-t)t^2 P2 + t^3 P3. Putting that into a matrix and
        // computing the inverse yielded

        controlPoints[0] = k;
        controlPoints[1] = k + kt / 3;
        controlPoints[2] = k + (2.0/3) * kt + kt2 / 3;
        controlPoints[3] = k + kt + kt2 + kt3;
    }


    /** Convert the set of 4 cubic Bezier control points into 4
        coefficients of a cubic polynomial. */
    public static void bezierToCubic(double p0, double p1, double p2, double p3,
                                     double[] coefficients) {
        coefficients[0] = p0;
        coefficients[1] = 3 * (p1 - p0);
        coefficients[2] = 3 * (p2 - 2 * p1 + p0);
        coefficients[3] = p3 - p0 + 3 * (p1 - p2);
    }

    /** @return 
     * @return a CubicSpline1D object that corresponds to the cubic
        Bezier curve that starts at p0 at t = 0, ends at p3 at t = 1,
        and has the control points p1 and p2 in between. */
    public static CubicSpline1D getBezierInstance (double p0, double p1, double p2,
                                     double p3) {
        double[] coefs = new double[4];
        bezierToCubic(p0, p1, p2, p3, coefs);
        CubicSpline1D output = new CubicSpline1D();
        output.coefficients = new double[][] { coefs };
        output.xs = new double[] { 0, 1 };
        output.ys = new double[] { p0, p3 };
        return output;
    }

    /** Convert the position of the parameterized cubic Bezier curve
        at the given t value. */
    public static double bezier(double t, double[] controlPoints) {
        double u = 1 - t;
        double uSq = u * u;
        double tSq = t * t;

        return u * uSq * controlPoints[0]
            + 3 * uSq * t * controlPoints[1]
            + 3 * u * tSq * controlPoints[2]
            + t * tSq * controlPoints[3];
    }
    
    @Override
	public String toString() {
        StringBuilder out = new StringBuilder(super.toString() + "\n"); 
        for (int segment = 0; segment < segmentCnt(); ++segment) {
            out.append(segment + ": ");
            out.append(Polynomial.toString(coefficients[segment]));
            out.append("\n");
        }
        for (int i = 0; i < ys.length; ++i) {
            if (i > 0) {
                out.append(" - ");
            }
            out.append("(" + xs[i] + ", " + ys[i] + ")");
        }
        if (ys.length > 0) {
            out.append("\n");
        }
       return out.toString();
    }

    /** @return an array of all real solutions to a x^2 + b x + c = 0
        in ascending order */
    public static double[] quadraticFormula(double a, double b, double c) {
        if (a == 0) {
            if (b == 0) {
                return new double[0];
            } else {
                return new double[] { -c/b };
            }
        }

        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return new double[0];
        } else if (discriminant == 0) {
            return new double[] { -b / 2 / a };
        } else {
            double dsqrt = Math.sqrt(discriminant);

            // Use the stable formula listed in the Wikipedia article
            // on "loss of significance" (the standard (-b +/-
            // sqrt(disc)) formulation is unstable)

            double x1 = (b < 0) ? ((-b + dsqrt) / (2 * a))
                : ((-b - dsqrt) / (2 * a));
            double x2 = c / a / x1;
            return ((x1 < x2)
                    ? new double[] { x1, x2 }
                    : new double[] { x2, x1 });
        }
    }

    /** Return the polynomial x(t) that covers the given segment. */
    public double[] getPoly(int segment) {
        return (double[]) coefficients[segment].clone();
    }


    /** @return a 2-element array holding the minimum and maximum
        values of spline(t) for t in [t0, t1] within the given
        segment. */
    public double[] getBounds(int segment, double t0, double t1) {
        double[] poly = coefficients[segment];
        double[] extrema = quadraticFormula
            (poly[3] * 3, poly[2] * 2, poly[1]);

        double max = Math.max(value(segment, t0), value(segment, t1));
        double min = Math.min(value(segment, t0), value(segment, t1));
        for (double t : extrema) {
            if (t <= t0 || t >= t1) {
                continue;
            }
            max = Math.max(max, value(segment, t));
            min = Math.min(min, value(segment, t));
        }

        return new double[] { min, max };
    }


    /** @return a 2-element array holding the minimum and maximum
        values of the entire spline curve, or null if this spline is
        empty. */
    public double[] getBounds() {
        int cnt = segmentCnt();
        if (cnt == -1) {
            return null;
        }

        double min = ys[0];
        double max = ys[0];

        for (int segNo = 0; segNo < cnt; ++segNo) {
            double[] segRange = getBounds(segNo, 0.0, 1.0);
            if (segRange[0] < min) {
                min = segRange[0];
            }
            if (segRange[1] > max) {
                max = segRange[0];
            }
        }

        return new double[] { min, max };
    }

    /** Dumb helper function to return p(x) where coefficients[i] is
     * the coefficient of the ith power of x. */
    public double computePoly(double x, double[] coefficients) {
        double result = 0;
        for (int i = coefficients.length - 1; i >= 0; --i) {
            result = result * x + coefficients[i];
        }
        return result;
    }


    /** @return a 2-element array {minimum, maximum} holding the range
        of y values covered by the entire spline curve for t values in
        [t0, t1], or null if no spline points were defined.. */
    public double[] range(double t0, double t1) {
        int cnt = segmentCnt();

        if (cnt == -1) {
            return null;
        } else if (cnt == 0) {
            return new double[] { ys[0], ys[0] };
        }

        SegmentAndT s0 = getSegment(t0);
        SegmentAndT s1 = getSegment(t1);

        if (s0.segment == s1.segment) {
            return getBounds(s0.segment, s0.t, s1.t);
        } else {
            double[] r = getBounds(s0.segment, s0.t, 1.0);
            double[] r2;
            int s;

            for (s = s0.segment + 1; s < s1.segment; ++s) {
                r2 = getBounds(s, 0.0, 1.0);
                r[0] = Math.min(r[0], r2[0]);
                r[1] = Math.max(r[1], r2[1]);
            }

            r2 = getBounds(s1.segment, 0.0, s1.t);
            r[0] = Math.min(r[0], r2[0]);
            r[1] = Math.max(r[1], r2[1]);

            return r;
        }
    }

    
    /** @return a 2-element array {minimum, maximum} holding the range
        of values covered by the entire spline curve. */
    public double[] range() {
        return range(0.0, 1.0);
    }


    /* @return a 2-element array holding the minimum and maximum
       values of dspline(t)/dt for t in [t0, t1] within the given
       segment. */
    public double[] derivativeRange(int segment, double t0, double t1) {
        double[] poly = coefficients[segment];

        double[] deriv = new double[] { poly[1], poly[2] * 2, poly[3] * 3 };

        double derv1 = computePoly(t0, deriv);
        double derv2 = computePoly(t1, deriv);

        double min = Math.min(derv1, derv2);
        double max = Math.max(derv1, derv2);

        if (poly[3] != 0) {
            // extremum of derivative:

            // d^2 poly / dt^2 = poly[3] * 6 * t + poly[2] * 2
            double t = -poly[2] / poly[3] / 3;
            if (t > t0 && t < t1) {
                double derv = computePoly(t, deriv);

                min = Math.min(min, derv);
                max = Math.max(max, derv);
            }
        }

        return new double[] { min, max };
    }


    /** @return a 2-element array {minimum, maximum} holding the range
        of values covered by the entire spline curve for t values in [t0, t1]. */
    public double[] derivativeRange(double t0, double t1) {
        int cnt = segmentCnt();

        if (cnt == -1) {
            return null;
        } else if (cnt == 0) {
            return new double[] { 0, 0 };
        }

        SegmentAndT s0 = getSegment(t0);
        SegmentAndT s1 = getSegment(t1);

        if (s0.segment == s1.segment) {
            return derivativeRange(s0.segment, s0.t, s1.t);
        } else {
            double[] r = derivativeRange(s0.segment, s0.t, 1.0);
            double[] r2;
            int s;

            for (s = s0.segment + 1; s < s1.segment; ++s) {
                r2 = derivativeRange(s, 0.0, 1.0);
                r[0] = Math.min(r[0], r2[0]);
                r[1] = Math.max(r[1], r2[1]);
            }

            r2 = derivativeRange(s1.segment, 0.0, s1.t);
            r[0] = Math.min(r[0], r2[0]);
            r[1] = Math.max(r[1], r2[1]);

            return r;
        }
    }


    /** Just a test harness */
    public static void main(String[] args) {
        double[] xs = { 0, 0.5, 1.0, 1.5 };
        double[] ys = { 3.1, 5.5, 5.5, 7.9 };
        // double[] xs = { 0, 2.4, 2.5, 4.8 };
        // double[] ys = { 3.1, 5.5, 5.6, 7.9 };
        CubicSpline1D c = new CubicSpline1D(xs, ys);
        System.out.println(c);

        for (int i = 0; i < c.segmentCnt(); ++i) {
            for (double j = 0; j < 1.01; j += 0.25) {
                System.out.println("value(" + i + ", " + j + ") = "
                                   + Arrays.toString(Polynomial.taylor(j, c.coefficients[i])));
            }
        }

        for (double t = 0; t < 1.0001; t += 0.125) {
            System.out.println("value(" + t + ") = " + c.value(t));
        }
    }
}
