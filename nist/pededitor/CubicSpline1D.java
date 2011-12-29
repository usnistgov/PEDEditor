package gov.nist.pededitor;

import java.util.*;

/** Class to handle 1D cubic spline interpolations for open curves
    ("polylines" in Java terms). For higher dimensions, just combine
    multiple 1D cubic splines.

    Declared final for speed.
 */
final public class CubicSpline1D {

    static class SegmentAndT {
        SegmentAndT(int segment, double t) {
            this.segment = segment;
            this.t = t;
        }

        public String toString() {
            return "SegmentAndT[" + segment + ", " + t + "]";
        }

        int segment;
        double t;
    }

    /** coefficients[n][d] represents the d-th degree coefficient of
        the cubic polynomial over the segment of the spline that
        connects ys[n] to ys[n+1]. */

    double[][] coefficients = new double[0][4];
    double[] ys;

    public CubicSpline1D (double[] ys) {
        int cnt = ys.length;
        this.ys = Arrays.copyOf(ys, cnt);

        if (cnt < 2) {
            return;
        }

        // diag is the main diagonal: diag[i] = m[i][i]
        double[] diag = new double[cnt];
        diag[0] = diag[cnt-1] = 2;
        for (int i = 1; i < cnt - 1; ++i) {
            diag[i] = 4;
        }

        // The technique used here is taken from MathWorld's "Cubic
        // Spline" discussion. Non-closed cubic splines are solvable
        // as a tridiagonal matrix that looks like

        // [ 2 1 0 0 0 ] D0 = 3 (y1-y0)
        // [ 1 4 1 0 0 ] D1 = 3 (y2-y0)
        // [ 0 1 4 1 0 ] D2 = 3 (y3-y1)
        // [ 0 0 1 4 1 ] D3 = 3 (y4-y2)
        // [ 0 0 0 1 2 ] D4 = 3 (y4-y3)

        // where D_i = the first derivative at each point

        // Solving this matrix takes linear time.

        // below is the row below the main diagonal: below[i] =
        // m[i][i-1]
        double[] below = new double[cnt];
        for (int i = 1; i < cnt; ++i) {
            below[i] = 1;
        }

        // above is the row above the main diagonal: above[i] =
        // m[i][i+1]. However, we can abstract it out, because the
        // initial values are all ones, and the final values are all
        // zeroes.

        // double[] above = new double[cnt];
        // for (int i = 0; i < cnt-1; ++i) {
        //     above[i] = 1;
        // }

        // constant is the constant column: constant[i] = m[i][cnt]
        double[] constant = new double[cnt];
        for (int i = 1; i < cnt - 1; ++i) {
            constant[i] = -3 * (ys[i+1] - ys[i-1]);
        }
        constant[0] = -3 * (ys[1] - ys[0]);
        constant[cnt-1] = -3 * (ys[cnt-1] - ys[cnt-2]);

        // Solve from the bottom up, turning the upper diagonal into
        // zeroes. Once we get up to the zeroth row, we will have a
        // linear equation we can solve for D0 -- no lower diagonal,
        // zero upper diagonal -- and we can start going forwards to
        // solve D1 ... Dn.

        for (int r = cnt-2; r >= 0; --r) {
            // Row(r) = Row(r) - (above[r]/diag[r+1]) * Row(r+1)

            // This sets above[r] to zero. However, we know above[r]
            // already equals 1.

            double rat = - 1.0 / diag[r+1];
            constant[r] += rat * constant[r+1];
            diag[r] += rat * below[r+1];
        }

        double[] ds = new double[cnt];
        ds[0] = -constant[0] / diag[0];
        for (int i = 1; i < cnt; ++i) {
            ds[i] = (-constant[i] - below[i] * ds[i-1]) / diag[i];
        }

        coefficients = new double[cnt-1][4];

        for (int i = 0; i < cnt-1; ++i) {

            // Look at the Math World write-up to understand what's
            // done here.

            double[] poly = new double[4];
            poly[0] = ys[i];
            poly[1] = ds[i];
            poly[2] = 3 * (ys[i+1] - ys[i]) - 2 * ds[i] - ds[i+1];
            poly[3] = 2 * (ys[i] - ys[i+1]) + ds[i] + ds[i+1];

            coefficients[i] = poly;
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
       slope of the curve at the given t value */
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

    /* Parameterize the entire curve as t in [0,1] and return the
       slope of x(t) at the given t value */
    public double slope(double t) {
        int cnt = coefficients.length;

        if (cnt == 0) {
            return Double.NaN;
        }

        if (t < 0) {
            return slope(0);
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
       function of the curve at the given t value */
    SegmentAndT getSegment(double t) {
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
    
    public String toString() {
        StringBuilder out = new StringBuilder(super.toString() + "\n"); 
        for (int segment = 0; segment < segmentCnt(); ++segment) {
            double[] coefs = coefficients[segment];
            out.append(segment + ": " + coefs[3] + " t^3 + " +
                       coefs[2] + " t^2 + " +
                       coefs[1] + " t + " + coefs[0] + "\n");
        }
        for (int i = 0; i < ys.length; ++i) {
            if (i > 0) {
                out.append(" - ");
            }
            out.append(ys[i]);
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


    /* @return a 2-element array holding the minimum and maximum
       values of spline(t) for t in [t0, t1] within the given
       segment. */
    public double[] range(int segment, double t0, double t1) {
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
            return range(s0.segment, s0.t, s1.t);
        } else {
            double[] r = range(s0.segment, s0.t, 1.0);
            double[] r2;
            int s;

            for (s = s0.segment + 1; s < s1.segment; ++s) {
                r2 = range(s, 0.0, 1.0);
                r[0] = Math.min(r[0], r2[0]);
                r[1] = Math.max(r[1], r2[1]);
            }

            r2 = range(s1.segment, 0.0, s1.t);
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
        double[] ys = { 3.1, 5.5, 5.5, 7.9 };
        // double[] ys = { 3.1, 5.5, 3.1 };
        CubicSpline1D c = new CubicSpline1D(ys);
        System.out.println(c);

        for (int i = 0; i < c.segmentCnt(); ++i) {
            for (double j = 0; j < 1.01; j += 0.25) {
                System.out.println("value(" + i + ", " + j + ") = " + c.value(i,j));
            }
        }

        for (double t = 0; t < 1.0001; t += 0.125) {
            System.out.println("value(" + t + ") = " + c.value(t));
        }
    }
}
