/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.Arrays;

/** Support concentration transformations that have the following
    form:

    1. Initially, each x_i (including for i=n+1) is transformed into c_i * x_i.

    2. All coefficients are divided by the sum of all x_i, so the
    resulting vector also appears as a concentration.
 */
public class MultiplierConcentrationTransform
    implements ConcentrationTransform {
    public double[] cs;

    public MultiplierConcentrationTransform(double... cs) {
        this.cs = Arrays.copyOf(cs, cs.length);
        for (double c: cs) {
            if (c < -1e-4) {
                throw new IllegalArgumentException("Negative coefficient");
            }
        }
    }

    @Override public MultiplierConcentrationTransform clone() {
        return new MultiplierConcentrationTransform(cs);
    }
    
    public MultiplierConcentrationTransform createTransform(double... cs) {
        return new MultiplierConcentrationTransform(cs);
    }

    @Override public int componentCnt() {
        return cs.length;
    }

    @Override public MultiplierConcentrationTransform createInverse() {
        double[] ics = new double[cs.length];
        for (int i = 0; i < cs.length; ++i) {
            ics[i] = 1/cs[i];
        }
        return createTransform(ics);
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
    @Override public void transform(double[] values) {
        int vlen = values.length;
        double dot = dotProduct(values);
        for (int i = 0; i < vlen; ++i) {
            values[i] *= cs[i] / dot;
        }
    }
}
