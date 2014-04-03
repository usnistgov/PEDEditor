/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;


/** Class to specify the requested precision of a numeric estimate.
*/
public class Precision implements Cloneable {

    /** Maximum acceptable relative error. Estimates of relative and
        absolute error are based on a comparison of the estimate
        computed using <code>2<sup>n</sup> + 1</code> points with the
        estimate computed using <code>2<sup>n-1</sup> + 1</code>
        points. The accuracy limit of double-precision floating point
        is about 10**-15, but the achievable accuracy for your
        particular function may be much less.

        The integration stops if either the relative or absolute error
        threshold is reached.
    */
    public double relativeError = 1e-10;

    /** Maximum acceptable absolute error. The integration stops if
        either the relative or absolute error threshold is reached.
    */
    public double absoluteError = 1e-20;

    /** Use at most the given number of sample x values to
        estimate the integral of <code>f()</code>. */
    public int maxSampleCnt = 65537;
    /** Use at least the given number of sample x values to estimate
        the integral of <code>f()</code>. If the least number of
        usable samples greater than or equal to minSampleCnt is
        greater than maxSampleCnt, then maxSampleCnt may be
        exceeded. */
    public int minSampleCnt = 5;

    public Precision() {}

    public Precision(Precision other) {
        relativeError = other.relativeError;
        absoluteError = other.absoluteError;
        maxSampleCnt = other.maxSampleCnt;
        minSampleCnt = other.minSampleCnt;
    }

    @Override public Precision clone() {
        return new Precision(this);
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[rel = " + relativeError
            + ", abs = " + absoluteError + ", " + minSampleCnt
            + " <= # samples <= " + maxSampleCnt + "]";
    }

    /* Return the maximum acceptable error in a measurement equal to
       v. */
    public double maxError(double v) {
        return Math.max(absoluteError, Math.abs(v * relativeError));
    }

    /** Return true if value1 is close enough to value2 to satisfy
        these precision requirements. */
    public boolean closeEnough(double value1, double value2) {
        return maxError(value1) >= Math.abs(value1 - value2);
    }

    public boolean closeEnough(Estimate est) {
        double mid = (est.lowerBound + est.upperBound) / 2;
        return closeEnough(est.lowerBound, mid);
    }

    /** Return true if est's entire range is within an acceptable
        distance of v. */
    public boolean closeEnough(Estimate est, double v) {
        double m = maxError(v);
        return est.lowerBound >= v - m && est.upperBound <= v + m;
    }
}
