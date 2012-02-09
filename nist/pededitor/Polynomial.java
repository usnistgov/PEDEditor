package gov.nist.pededitor;

/** Simple utilities for polynomials encoded as an array of their
    coefficients. */
public class Polynomial {
    public static double[] derivative(double[] poly) {
        int degree = poly.length - 1;
        if (degree <= 0) {
            return new double[0];
        }

        double[] output = new double[degree];

        for (int i = 0; i < degree; ++i) {
            output[i] = poly[i+1] * (i+1);
        }

        return output;
    }

    public static String toString(double[] poly) {
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
                out.append(" t^" + i);
            } else if (i == 1) {
                out.append(" t");
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

    public static double[] taylor(double x, double[] poly) {
        double[] output = new double[poly.length];
        int degree = poly.length - 1;
        for (int i = 0; i <= degree; ++i) {
            output[i] = evaluate(x, poly);
            poly = derivative(poly);
        }
        return output;
    }
}
