/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.function.DoubleUnaryOperator;


/** Romberg scalar numerical integration -- numerically estimates the
    integral of a real-valued function using Romberg integration, a
    (possibly much) faster relative of Simpson's method. See {@link
    #main(String[]) main} for an example.

    Romberg integration uses progressively higher-degree polynomial
    approximations each time you double the number of sample points.
    For example, it uses a 2nd-degree polynomial approximation (as
    Simpson's method does) after one split (2**1 + 1 sample points),
    and it uses a 10th-degree polynomial approximation after five
    splits (2**5 + 1 sample points). Typically, this will greatly
    improve accuracy (compared to simpler methods) for smooth
    functions, while still being well-conditioned enough not to harm
    accuracy for non-smooth functions, though functions with
    asymptotes in the domain of integration are a problem.

    This is the basic version. More Romberg-related utilities and
    functions are found in AdaptiveRombergIntegral.java and
    AdaptiveRombergIntegralY.java
*/
public class RombergIntegral {

    /** Equivalent to {@link #integral(DoubleUnaryOperator, double, double,
        Precision) integral(f, lo, hi, new Precision()) } */
    public static NumericEstimate integral(DoubleUnaryOperator f, double lo, double hi) {
        return integral(f, lo, hi, new Precision());
    }

    /** Return the result of integrating the function f over the
     domain [lo,hi]. This never returns null and always returns an
     estimate, if res.Status != 0 then res.value may not be accurate
     to the relative and absolute error limits you requested.

     @param lo One of the limits of the integration domain. If lo &lt;
     hi then the two will be swapped, but they must both be finite.

     @param hi The other limit of the integration domain.

     @param p Requested precision of the result.
    */
    public static NumericEstimate integral(DoubleUnaryOperator f, double lo, double hi,
                                           Precision p) {
        double stepLength = hi - lo;
        if (stepLength == 0) {
            return new NumericEstimate(0);
        }
        // total is used to compute the trapezoid approximations. It
        // is a total of all f() values computed so far, except that,
        // f(hi) and f(lo) are assigned half as much weight as other
        // points, because that's how the trapezoid approximation
        // works.
        double ylo = f.applyAsDouble(lo);
        double yhi = f.applyAsDouble(hi);
        double total = (ylo + yhi)/2;


        // 0th trapezoid approximation.
        NumericEstimate res = new NumericEstimate(total * stepLength);
        res.sampleCnt = 2;
        res.lowerBound = Math.min(ylo, yhi) * stepLength;
        res.upperBound = Math.max(ylo, yhi) * stepLength;

        int maxSplit = sampleCntToSplit(p.maxSampleCnt, false);
        int minSplit = sampleCntToSplit(p.minSampleCnt, true);
        maxSplit = Math.max(maxSplit, minSplit);

        // estimates[0] contains the trapezoid approximation;
        // estimates[1] (once it is created) contains the Simpson
        // approximation; and so on up to the
        // estimates[estimates.length-1] which contains the
        // (estimates.length-1)th Romberg approximation.
        double[] estimates = new double[maxSplit+1];
        estimates[0] = res.value;

        for (int split = 1, sampleCnt=1;
             split <= maxSplit;
             split++, stepLength /=2, sampleCnt <<= 1) {
            // Don't let stepLength drop below the limits of numeric
            // precision. (This should prevent infinite loops, but not
            // loss of accuracy.)
            if (lo + stepLength/(sampleCnt*2) == lo
                || hi - stepLength/(sampleCnt*2) == hi) {
                res.status = NumericEstimate.Status.TOO_SMALL_STEP_SIZE;
                return res;
            }
            
            // Compute the (split)th trapezoid approximation.
            double x = lo + stepLength/2;
            for (int i = 0; i < sampleCnt; ++i, x += stepLength) {
                total += f.applyAsDouble(x);
            }
            res.sampleCnt += sampleCnt;

            // Unshift the new trapezoid estimate onto the front of
            // the estimates list.
            for (int i = split; i > 0; --i) {
                estimates[i] = estimates[i-1];
            }
            estimates[0] = total * stepLength / 2;
            
            // Update the estimates array with the new data.
            int pow4 = 4;
            for (int approxLevel = 1; approxLevel <= split; ++approxLevel, pow4<<=2) {
                estimates[approxLevel] = estimates[approxLevel-1]
                    + (estimates[approxLevel-1]-estimates[approxLevel])/(pow4 - 1);
            }

            // Is this estimate accurate enough?
            double estimate = estimates[split];
            double oldEstimate = res.value;
            double error = Math.abs(estimate - oldEstimate);
            res.value = estimate;
            res.lowerBound = estimate - error;
            res.upperBound = estimate + error;
            if (split >= minSplit && p.closeEnough(estimate, oldEstimate)) {
                return res;
            }
        }
        res.status = NumericEstimate.Status.TOO_MANY_STEPS;
        return res;
    }

    static int sampleCntToSplit(int sampleCnt, boolean roundUp) {
        if (sampleCnt <= 2) {
            return 0;
        }

        for (int split = 1; ; ++split) {
            int sc = (1 << split) + 1;
            if (sc == sampleCnt) {
                return split;
            } else if (sc > sampleCnt) {
                return split + (roundUp ? 0 : -1);
            }
        }
    }

    static int splitToSampleCnt(int split) {
        return 1 + (1 << split);
    }

    public static void main(String[] args) {
        Precision p = new Precision();
        p.relativeError = 1e-10;
        p.absoluteError = 1e-16;
        DoubleUnaryOperator gaussian =
            x-> Math.exp(-x*x/2)/Math.sqrt(2 * 3.14159265358979);
        NumericEstimate res = integral(gaussian, 0, 4, p);
        System.out.println(res);
        double expected = 0.49996832875817;
        double actual = res.value;
        boolean ok = Math.abs(expected - actual) < 1e-10;
        System.out.println("Test result: " + (ok ? "OK" : "FAILED"));
    }
}
