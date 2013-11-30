package gov.nist.pededitor;

import java.util.Arrays;

/** Class to perform concentration transformations.

    A concentration is a vector <x_1, ..., x_n> such that each x_i >=
    0 and sum_{i=0}^{i=n-1} x_i <= 1. There is an implicit x_{n+1}
    element that equals one minus the sum of all other values.

    The concentration transformations that are supported have the
    following form:

    1. Initially, each x_i (including for i=n+1) is transformed into c_i * x_i.

    2. All coefficients are divided by the sum of all x_i, so the
    resulting vector also appears as a concentration.
 */
public class ConcentrationTransform {
    public double[] cs;

    public ConcentrationTransform(double... cs) {
        this.cs = Arrays.copyOf(cs, cs.length);
        for (double c: cs) {
            if (c < -1e-4) {
                throw new IllegalArgumentException("Negative coefficient");
            }
        }
    }

    public int componentCnt() {
        return cs.length;
    }

    public ConcentrationTransform inverse() {
        double[] ics = new double[cs.length];
        for (int i = 0; i < cs.length; ++i) {
            ics[i] = 1/cs[i];
        }
        return new ConcentrationTransform(ics);
    }

    /** Multiply all values by the same constant so they sum to 1. */
    public void normalize(double[] values) {
        double sum = 0;
        for (double d: values) {
            sum += d;
        }
        if (sum != 0) {
            for (int i = 0; i < values.length; ++i) {
                values[i] /= sum;
            }
        }
    }

    /** @return the dot product of the values with the cs. If the last
        value is missing, fill it in with a value so they all sum to
        1. */
    public double dotProduct(double... values) {
        int clen = cs.length;
        int vlen = values.length;
        double dot = 0;
        for (int i = 0; i < vlen; ++i) {
            dot += cs[i] * values[i];
        }
        if (vlen == clen) {
            // do nothing
        } else if (vlen == clen - 1) {
            double sum = 0;
            for (double d: values) {
                sum += d;
            }
            dot += cs[clen-1] * (1 - sum);
        } else {
            throw new IllegalArgumentException
                ("Expected " + (clen-1)
                 + "or " + (clen) + " arguments");
        }
        return dot;
    }

    /** Transform the given coordinates in place. */
    public void transform(double[] values) {
        int vlen = values.length;
        double dot = dotProduct(values);
        for (int i = 0; i < vlen; ++i) {
            values[i] *= cs[i] / dot;
        }
    }
}
