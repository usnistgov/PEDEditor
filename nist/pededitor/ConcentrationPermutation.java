/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;
import java.util.Arrays;

/** Concentration transformation that permutes the concentrations. */
public class ConcentrationPermutation
    implements SlopeConcentrationTransform {
    public int[] fromIndexes;

    /** @arg fromIndexes An array whose ith value indicates which
     * original index the ith element should be pulled from. For
     * examples, new ConcentrationPermutation(2, 0, 1) replaces
     * the 0th element with the 2nd, the 1st with the 0th, and the 2nd
     * with the 1st. */
    public ConcentrationPermutation(int... fromIndexes) {
        boolean used[] = new boolean[fromIndexes.length];
        this.fromIndexes = Arrays.copyOf(fromIndexes, fromIndexes.length);
        for (int d: fromIndexes) {
            if (d < 0 || d >= fromIndexes.length) {
                throw new IllegalArgumentException("Out of range value " + d);
            }
            if (used[d]) {
                throw new IllegalArgumentException("Repeated value " + d);
            }
            used[d] = true;
        }
    }

    @Override public ConcentrationPermutation createInverse() {
        int[] toIndexes = new int[fromIndexes.length];
        for (int i = 0; i < fromIndexes.length; ++i) {
            toIndexes[fromIndexes[i]] = i;
        }
        return new ConcentrationPermutation(toIndexes);
    }

    @Override public void transform(double[] values) {
        double[] oldValues = Arrays.copyOf(values, values.length);
        for (int i = 0; i < fromIndexes.length; ++i) {
            values[i] = oldValues[fromIndexes[i]];
        }
    }

    @Override public int componentCnt() {
        return fromIndexes.length;
    }

    @Override public Point2D.Double transformSlope(double x, double y,
            double dx, double dy) {
        if (componentCnt() == 2) {
            // Component order is RIGHT, LEFT.
            double[] deltas = {dx, -dx};
            transform(deltas);
            // In a binary diagram, swapping components has no effect on dy.
            return new Point2D.Double(deltas[0], dy);
        } else {
            // Component order is RIGHT, TOP, LEFT.
            double[] deltas = {dx, dy, -dx-dy};
            transform(deltas);
            return new Point2D.Double(deltas[0], deltas[1]);
        }
    }
}
