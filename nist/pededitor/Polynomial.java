/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.QuadCurve2D;
import java.util.Arrays;

/** Simple utilities for polynomials encoded as an array of their
    coefficients. */
public class Polynomial {
    public static double[] derivative(double[] poly) {
        int degree = poly.length - 1;
        if (degree <= 0) {
            return new double[0];
        }

        double[] res = new double[degree];

        for (int i = 0; i < degree; ++i) {
            res[i] = poly[i+1] * (i+1);
        }

        return res;
    }

    /** Return the integral of poly. */
    public static double[] integral(double[] poly) {
        int degree = degree(poly);
        if (degree == -1) {
            return new double[0];
        }
        double[] res = new double[degree + 2];

        for (int i = 0; i <= degree; ++i) {
            res[i+1] = poly[i] / (i+1);
        }

        return res;
    }

    /** Return the product of the two polynomials. */
    public static double[] times(double[] poly1, double[] poly2) {
        int d1 = degree(poly1);
        int d2 = degree(poly2);
        if (d1 < 0 || d2 < 0) {
            return new double[0];
        }
        double[] res = new double[d1 + d2 + 1];
        for (int i = 0; i <= d1; ++i) {
            for (int j = 0; j <= d2; ++j) {
                res[i+j] += poly1[i] * poly2[j];
            }
        }
        return trim(res);
    }

    public static String toString(double[] poly) {
        return toString(poly, "t");
    }

    public static String toString(double[] poly, String var) {
        if (poly == null) {
            return "null";
        }

        int degree = poly.length - 1;

        StringBuilder out = new StringBuilder();

        boolean printed = false;

        for (int i = degree; i >=0; --i) {
            double c = poly[i];
            if (c == 0) {
                continue;
            }

            if (printed) {
                if (c < 0) {
                    out.append(" - ");
                    c = -c;
                } else {
                    out.append(" + ");
                }
            }
            out.append(c);
            if (i > 1) {
                out.append(" " + var + "^" + i);
            } else if (i == 1) {
                out.append(" " + var);
            }
            printed = true;
        }

        return printed ? out.toString() : "0";
    }

    /** Helper function to evaluate the polynomial with given
        coefficients at the given x value, returning poly[0] + poly[1]
        * x + poly[2] * x^2 + ... */
    public static double evaluate(double x, double[] poly) {
        double result = 0;
        for (int i = poly.length - 1; i >= 0; --i) {
            result = result * x + poly[i];
        }
        return result;
    }

    /** Equivalent to evaluate(x, derivative(poly)). */
    public static double evaluateDerivative(double x, double[] poly) {
        double result = 0;
        for (int i = poly.length - 1; i >= 1; --i) {
            result = result * x + poly[i] * i;
        }
        return result;
    }

    /** Equivalent to evaluate(x, integral(poly)). */
    public static double evaluateIntegral(double x, double[] poly) {
        double result = 0;
        for (int i = poly.length - 1; i >= 0; --i) {
            result = (result + poly[i] / (i+1)) * x;
        }
        return result;
    }

    /** Evaluate the integral over the given range. */
    public static double evaluateIntegral(double x1, double x2, double[] poly) {
        return evaluateIntegral(x2, poly) - evaluateIntegral(x1, poly);
    }

    public static double[] taylor(double x, double[] poly) {
        double[] res = new double[poly.length];
        int degree = poly.length - 1;
        for (int i = 0; i <= degree; ++i) {
            res[i] = evaluate(x, poly);
            poly = derivative(poly);
        }
        return res;
    }

    /** Return the actual degree of this polynomial (ignoring zero
        terms), or -1 for a zero polynomial. */
    public static int degree(double[] poly) {
        for (int d = poly.length - 1; d >= 0; --d) {
            if (poly[d] != 0) {
                return d;
            }
        }
        return -1;
    }

    /** Remove all zero coefficients that do not have higher-order
        nonzero coefficients and return the result (so for example
        0x^2 + 5x + 0 becomes simply 5x + 0). This will simply return
        its input (not even cloning it) unless poly[poly.length-1] ==
        0. */
    public static double[] trim(double[] poly) {
        int d = degree(poly);
        if (d == poly.length - 1) {
            return poly;
        }
        double[] res = new double[d + 1];
        for (int i = 0; i <= d; ++i) {
            res[i] = poly[i];
        }
        return res;
    }

    /** Return an array of the zeros of "poly" in order from least to
        greatest. Currently throws IllegalArgumentException on
        polynomials of degree 4 or greater. */
    public static double[] solve(double[] poly) {
        int d = degree(poly);
        if (d <= 0) {
            return new double[0];
        }
        double[] res = new double[d];
        int rootCnt;

        switch (d) {
        case 3:
            rootCnt = CubicCurve2D.solveCubic(poly, res);
            break;
        case 2:
            rootCnt = QuadCurve2D.solveQuadratic(poly, res);
            break;
        case 1:
            res[0] = -poly[0]/poly[1];
            rootCnt = 1;
            break;
        case 0:
        case -1:
            rootCnt = 0;
            break;
        default:
            throw new IllegalArgumentException
                ("Cannot solve polynomial " + Arrays.toString(poly)
                 + " because its degree exceeds 3");
        }

        if (rootCnt > 1) {
            Arrays.sort(res, 0, rootCnt);
        }
        if (rootCnt == d) {
            return res;
        } else {
            return Arrays.copyOf(res, rootCnt);
        }
    }

    /** @return a 2-element array holding the minimum and maximum
        values of the polynomial "poly" for t in [t0, t1]. Currently
        throws IllegalArgumentException on polynomials of degree 5 or more. */
    public static double[] getBounds(double[] poly, double t0, double t1) {
        double v0 = evaluate(t0, poly);
        double v1 = evaluate(t1, poly);
        double min = Math.min(v0, v1);
        double max = Math.max(v0, v1);

        for (double zero: solve(derivative(poly))) {
            if (t0 <= zero && zero <= t1) {
                double v = evaluate(zero, poly);
                max = Math.max(max, v);
                min = Math.min(min, v);
            }
        }

        return new double[] { min, max };
    }
}
