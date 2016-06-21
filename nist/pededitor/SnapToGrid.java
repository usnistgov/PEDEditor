/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.ArrayList;
import java.util.Arrays;

/** Transform the input values into integers where possible, but don't
    map two different locations to the same target and preferably
    don't change relative distances by very much. This is analogous to
    automatic font hinting.
 */
public class SnapToGrid {

    /** The none-too-fancy solution used here (the method used for
     * automatic hinting in FontForge is probably better) is 1) Count
     * all input values that are within distance nearlyEqual to each
     * other as the same; 2) provisionally round each input value to
     * the nearest integer; 3) if two or more values are mapped to the
     * same output, then only map the one that is closest to that
     * output value; 4) use a piecewise linear map to transform all
     * other values (this means these other values will not be mapped
     * to integers). So if step (3) maps 2 1/5 -> 2 and 2 4/5 -> 3,
     * then 2 2/5 -> 2 1/3 in step 4.

     * The worst case stretch factor of this approach is unbounded if,
     * for example, 2.499 gets mapped to 2 and 2.501 gets mapped to 3.
     */

    double[] ins;
    double[] outs;

    /** Return the point to which d should be mapped. */
    public double snap(double d) {
        int cnt = ins.length;
        int idx = Arrays.binarySearch(ins, d);
        if (idx < 0)
            idx = -idx - 2;
        if (idx < 0)
            return outs[0] + d - ins[0];
        if (idx == cnt - 1)
            return outs[cnt-1] + d - ins[cnt-1];
        return outs[idx]
            + (d - ins[idx]) / (ins[idx+1] - ins[idx]) * (outs[idx+1] - outs[idx]);
    }
    
    /** Initialize the snap transform. Important note: the values[]
        array will be sorted during this process. */
    public SnapToGrid(double[] values, double nearlyEqual) {
        Arrays.sort(values);
        ArrayList<Double> vs = new ArrayList<>();
        int cnt = values.length;
        if (cnt == 0) {
            ins = outs = new double[0];
            return;
        }
        vs.add(values[0]);
        for (int i = 1; i < cnt; ++i) {
            double v = values[i];
            if (v - vs.get(vs.size() - 1) > nearlyEqual)
                vs.add(v);
        }
        cnt = vs.size();
        ArrayList<Double> inas = new ArrayList<>();
        ArrayList<Double> outas = new ArrayList<>();
        for (int i = 0; i < cnt; ++i) {
            double in = vs.get(i);
            double out = Math.rint(in);
            if (i > 0) {
                double in2 = vs.get(i-1);
                double out2 = Math.rint(in2);
                if (out == out2
                    && Math.abs(out - in2) <= Math.abs(out - in)) {
                    continue;
                }
            }
            if (i < cnt-1) {
                double in2 = vs.get(i+1);
                double out2 = Math.rint(in2);
                if (out == out2
                    && Math.abs(out - in2) < Math.abs(out - in)) {
                    continue;
                }
            }
            inas.add(in);
            outas.add(out);
        }
        ins = toDoubleArray(inas);
        outs = toDoubleArray(outas);
    }

    public static double[] toDoubleArray(ArrayList<Double> al) {
        int cnt = al.size();
        double[] res = new double[cnt];
        for (int i = 0; i < cnt; ++i) {
            res[i] = al.get(i);
        }
        return res;
    }

    public static void main(String[] args) {
        double[] ins = {0.3, 5.7, 2.1, 1.8, 5.70001};
        SnapToGrid g = new SnapToGrid(ins, 0.001);
        for (double d: ins) {
            System.out.println(d + " -> " + g.snap(d));
        }
    }
}    
