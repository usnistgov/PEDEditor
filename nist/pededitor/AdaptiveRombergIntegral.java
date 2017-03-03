/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

/** Adaptive Romberg integration which stores the calculated data
    points in an unbalanced binary tree. Compared to regular Romberg,
    this performs especially well for highly skewed data sets
    (integral(e^x dx) evaluated over [-100, 0] for example). In terms
    of the number of data points evaluated, this also performs
    comparably to regular Romberg for less skewed data sets, but
    regular Romberg has low time overhead and negligible space
    overhead, while adaptive Romberg has more of both, so adaptive
    Romberg is more suited to functions that are more expensive to
    evaluate.

    The leaves each contain the same number of data points (17 by
    default, but you can use the alternate constructor to specify some
    other value).

    TODO: Create a version where you can integrate with bounds other
    than the originally define lo-hi range, leveraging previously
    evaluated points.
*/
public class AdaptiveRombergIntegral {
    double lo;
    double hi;
    AdaptiveRombergIntegral left = null;
    AdaptiveRombergIntegral right = null;
    AdaptiveRombergIntegral parent = null;
    DoubleUnaryOperator f;
    NumericEstimate estimate;
    // bestLeaf is the leaf of this subtree that has the highest
    // efficiency() -- that is, the highest ratio of estimate.width()
    // to estimate.sampleCnt().
    AdaptiveRombergIntegral bestLeaf;
    double[] ys = null;

    // Currently all leaves get filled with exactly maxLeafSize samples.
    int maxLeafSize;

    public AdaptiveRombergIntegral(DoubleUnaryOperator f, double lo, double hi) {
        this(f, lo, hi, 17);
    }
    
    // leafSize must equal a value of the form (2**i+1) that is at
    // least 5 -- so 5, 9, 17, 33, ... are the allowed values. (The
    // value 0 is for internal use.)
    public AdaptiveRombergIntegral(DoubleUnaryOperator f, double lo, double hi,
                                   int maxLeafSize) {
        if (maxLeafSize < 5) {
            throw new IllegalArgumentException("maxLeafSize must be at least 9");
        }
        if (((maxLeafSize - 1) & (maxLeafSize - 2)) != 0) {
            throw new IllegalArgumentException
                ("maxLeafSize must have the form 2**n + 1");
        }

        if (lo > hi) {
            throw new IllegalArgumentException
                ("Lower limit " + lo + " must be less than or equal to upper limit " + hi);
        }
            
        this.f = f;
        this.lo = lo;
        this.hi = hi;
        this.maxLeafSize = maxLeafSize;
        estimate = NumericEstimate.bad(0);
        bestLeaf = this;
    }

    /** Construct a new object that is a parent of left and right. */
    AdaptiveRombergIntegral(AdaptiveRombergIntegral left, 
                                   AdaptiveRombergIntegral right) {
        assert(left.hi == right.lo);
        lo = left.lo;
        hi = right.hi;
        f = left.f;
        this.left = left;
        this.right = right;
        left.parent = this;
        right.parent = this;
        estimate = new NumericEstimate(0);
        updateEstimate();
    }

    public NumericEstimate integral() {
        return estimate.clone();
    }

    public DoubleUnaryOperator getFunction() {
        return f;
    }

    public final boolean isLeaf() {
        return left == null;
    }

    public boolean closeEnough(Precision p) {
        return p.closeEnough(estimate);
    }

    void updateEstimate() {
        AdaptiveRombergIntegral it = this;
        do {
            if (it.isLeaf()) {
                it.estimate = integral(it.ys, it.lo, it.hi);
                it.estimate.sampleCnt = it.ys.length -1;
            } else {
                it.estimate.copyFrom(it.left.estimate);
                it.estimate.add(it.right.estimate);
                it.bestLeaf =
                    (it.left.efficiency() > it.right.efficiency())
                    ? it.left.bestLeaf
                    : it.right.bestLeaf;
            }
            it = it.parent;
        } while (it != null);
    }

    /** A good guess is that the extent to which each additional
        sample will reduce the width of the estimate is proportional
        to the width divided by the number of samples already used.
        (If the width is very narrow already, then there is little to
        gain; and if very many samples were already used, then it is
        likely that a few more will lead to little improvement.)
    */
    final double efficiency() {
        return isLeaf() ? estimate.width() / estimate.sampleCnt :
            bestLeaf.efficiency();
    }

    /** Add a parent whose right child has the same width as this, and
        return the number of new samples required. */
    int expandRight() {
        AdaptiveRombergIntegral rightSibling
            = new AdaptiveRombergIntegral(f, hi, hi + (hi - lo), maxLeafSize);
        int res = rightSibling.refine();
        new AdaptiveRombergIntegral(this, rightSibling);
        return res;
    }

    /** Return true if this is a left child. */
    boolean isLeft() {
        return parent != null && parent.left == this;
    }

    /** Return true if this is a right child. */
    boolean isRight() {
        return parent != null && parent.right == this;
    }

    /** Expand this leaf; return the number of new samples that were
        needed. If the return value is 0, that means this leaf was
        split in two.

        @param preferToSplit If preferToSplit is false and maxLeafSize
        has not yet been reached, then add additional samples to this
        leaf instead of splitting. If true, this leaf will be split no
        matter what, and no additional samples will be added unless
        that is necessary to avoid having the leaf size drop below the
        minimum.

        Most of the time, preferToSplit should be false, because
        splitting prematurely just reduces accuracy except in unusual
        circumstances, such as when drilling down to improve the
        accuracy of interpolation.
    */
    int expand(boolean preferToSplit) {
        if (ys == null) {
            ys = sample(f, lo, hi, 5);
            updateEstimate();
            return 5;
        }
        int cnt = ys.length;
        if (cnt < maxLeafSize && (!preferToSplit || cnt < 5)) {
            ys = nextStep(ys, f, lo, hi);
            updateEstimate();
            return cnt / 2;
        }
        double mid = (lo + hi) / 2;
        left = new AdaptiveRombergIntegral(f, lo, mid, maxLeafSize);
        left.parent = this;
        right = new AdaptiveRombergIntegral(f, mid, hi, maxLeafSize);
        right.parent = this;
        int midi = cnt/2;
        left.ys = Arrays.copyOfRange(ys, 0, midi + 1);
        right.ys = Arrays.copyOfRange(ys, midi, cnt);
        ys = null;
        if (left.ys.length < 5) {
            // Refine these leaves to bring them back up to the
            // minimum of 5 data points.
            return left.refine() + right.refine();
        }
            
        left.updateEstimate();
        right.updateEstimate();
        return 0;
    }

    /* Compute the integral between the lower and upper limits to the
       given precision. */
    public NumericEstimate integral(Precision p) {
        int sampleCnt = 0;
        NumericEstimate.Status status = NumericEstimate.Status.OK;
        while (!closeEnough(p)
               && estimate.status !=
               NumericEstimate.Status.TOO_SMALL_STEP_SIZE) {
            if (sampleCnt >= p.maxSampleCnt) {
                status = NumericEstimate.Status.TOO_MANY_STEPS;
                break;
            }
            if (efficiency() == 0) {
                status = NumericEstimate.Status.TOO_SMALL_STEP_SIZE;
                break;
            }
            sampleCnt += refine();
        }
        NumericEstimate est = integral();
        est.sampleCnt = sampleCnt;
        est.status = status;
        return est;
    }

    /** Add a few more samples to improve the estimate quality. Return
        the number of new samples added. */
    public int refine() {
        for (;;) {
            int sampleCnt = bestLeaf.expand(false);
            if (sampleCnt > 0) {
                return sampleCnt;
            }
        }
    }

    /* Return the Romberg estimate of an integral over [lo, hi] that
       uses the given array of reguarly distributed sample y values,
       with f(lo)=ys[0] and f(hi)=ys[ys.length-1]. */
    public static NumericEstimate integral(double[] ys, double lo, double hi) {
        return integral(ys, lo, hi, 0, ys.length);
    }

    /* Return the Romberg estimate of an integral over [lo, hi] that
       uses a slice of the given array of reguarly distributed sample
       y values, with f(lo)=ys[startIndex] and
       f(hi)=ys[endIndex-1]. */
    public static NumericEstimate integral(double[] ys, double lo, double hi,
                                           int startIndex, int endIndex) {
        double width = hi - lo;
        if (width == 0) {
            return new NumericEstimate(0);
        } else if (width < 0) {
            throw new IllegalArgumentException
                ("Lower bound " + lo + " exceeds upper bound " + hi);
        }
        int cnt = endIndex-startIndex;
        if (cnt < 2 || ((cnt-1) & (cnt-2)) != 0) {
            throw new IllegalArgumentException("Romberg sample cnt " + cnt
                                               + " does not have form (1 + 2<<i)");
        }

        double ylo = ys[startIndex];
        double yhi = ys[endIndex-1];
        double total = (ylo + yhi)/2;
        int stepLength = cnt - 1;

        // 0th trapezoid approximation.
        NumericEstimate res = new NumericEstimate(total * stepLength);
        res.sampleCnt = 2;
        res.lowerBound = Math.min(ylo, yhi) * stepLength;
        res.upperBound = Math.max(ylo, yhi) * stepLength;

        // estimates[0] contains the trapezoid approximation;
        // estimates[1] (once it is created) contains the Simpson
        // approximation; and so on up to the
        // estimates[estimates.length-1] which contains the
        // (estimates.length-1)th Romberg approximation.
        double[] estimates = new double[RombergIntegral.sampleCntToSplit(cnt, false)+1];
        estimates[0] = res.value;

        for (int split = 1, sampleCnt=1;
             res.sampleCnt < cnt;
             split++, stepLength>>=1, sampleCnt<<=1) {
            int halfStep = stepLength>>1;
            
            // Compute the (split)th trapezoid approximation.
            int x = startIndex + halfStep;
            for (int i = 0; i < sampleCnt; ++i, x += stepLength) {
                total += ys[x];
            }
            res.sampleCnt += sampleCnt;

            // Unshift the new trapezoid estimate onto the front of
            // the estimates list.
            for (int i = split; i > 0; --i) {
                estimates[i] = estimates[i-1];
            }
            estimates[0] = total * halfStep;
            
            // Update the estimates array with the new data.
            int pow4 = 4;
            for (int approxLevel = 1; approxLevel <= split; ++approxLevel, pow4<<=2) {
                estimates[approxLevel] = estimates[approxLevel-1]
                    + (estimates[approxLevel-1]-estimates[approxLevel])/(pow4 - 1);
            }

            double estimate = estimates[split];
            double oldEstimate = res.value;
            double error = Math.abs(estimate - oldEstimate);
            res.value = estimate;
            res.lowerBound = estimate - error;
            res.upperBound = estimate + error;
        }
        res.times(width / (endIndex - startIndex - 1));
        return res;
    }

    static double[] sample(DoubleUnaryOperator f, double lo, double hi,
                                   int sampleCnt) {
        double[] res = new double[sampleCnt];
        for (int i = 0; i < res.length; ++i) {
            res[i] = f.applyAsDouble(lo + (double) i/(sampleCnt - 1) * (hi - lo));
        }
        return res;
    }

    /** Split the given data set (roughly doubling the number of
        samples) and return the new sample set. The x values of both
        the input "ys" and the return value are evenly distributed
        starting at lo and ending at hi.

        If ys == null or ys.length==0 then the sample set for the
        initial (zeroth) split will be returned. */
    static double[] nextStep(double[] ys, DoubleUnaryOperator f, double lo, double hi) {
        if (ys == null || ys.length == 0) {
            return new double[] { f.applyAsDouble(lo), f.applyAsDouble(hi) };
        }

        double[] res = new double[ys.length*2 - 1];
        for (int i = 0; i < ys.length; ++i) {
            res[i*2] = ys[i];
        }

        double xStep = (hi - lo) / (ys.length - 1);
        double x = lo + xStep / 2;
        for (int i = 0; i < ys.length - 1; ++i, x += xStep) {
            res[i*2 + 1] = f.applyAsDouble(x);
        }
        return res;
    }

    public AdaptiveRombergIntegral getRoot() {
        AdaptiveRombergIntegral it = this;
        while (it.parent != null) {
            it = it.parent;
        }
        return it;
    }

    /* Return the number of samples in each leaf node. */
    public int leafSampleCnt() {
        AdaptiveRombergIntegral it = this;
        while (!it.isLeaf()) {
            it = it.left;
        }
        return it.ys.length;
    }

    /* Return the number of leaves of this subtree. */
    public int leafCnt() {
        AdaptiveRombergIntegral it = this;
        int tot = 1;
        while (!it.isLeaf()) {
            tot += it.right.leafCnt();
            it = it.left;
        }
        return tot;
    }

    public NumericEstimate getEstimate() {
        return estimate.clone();
    }

    /** Return a string represting how the domain is adaptively
        partitioned. */
    public String domainTree() {
        if (isLeaf()) {
            return "[" + String.format("%6g", lo) + ", "
                + String.format("%6g", hi) + "]";
        } else {
            return "[" + left.domainTree() + ", " + right.domainTree() + "]";
        }
    }

    /** Return the leaf (or a leaf, if v coincides with an interval
        endpoint) that contains v, or null if v is outside the
        domain. */
    public AdaptiveRombergIntegral leafContaining(double x) {
        if (x < lo || x > hi) {
            return null;
        }

        AdaptiveRombergIntegral it = this;
        while (!it.isLeaf()) {
            it = (it.left.hi >= x) ? it.left : it.right;
        }
        return it;
    }

    /** Return the integral of f over [getRoot().lo, this.lo]. */
    NumericEstimate leftSideIntegral() {
        NumericEstimate res = new NumericEstimate(0);
        AdaptiveRombergIntegral it = this;
        for (AdaptiveRombergIntegral par = it.parent; par != null; it = par) {
            if (par.right == it) {
                res.add(par.left.estimate);
            }
        }
        return res;
    }

    public double getLowerLimit() {
        return lo;
    }

    public double getUpperLimit() {
        return hi;
    }

    /** The sample count returned for entire trees is always low by
        one. If you have 100 leaves of 9 samples apiece, then that's
        really just (7*100) internal sample values plus 99 samples
        shared between adjacent leaves plus the very first and very
        last sample, for a total of 801 samples. The best
        approximation for this that treats all subtrees the same is
        just to count each leaf as having one less sample than it
        really does, or 8*100 = 800 instead of the correct value of
        801. */
    public int getSampleCnt() {
        return estimate.sampleCnt;
    }

    @Override public String toString() {
        StringBuilder res = new StringBuilder(getClass().getSimpleName() + "[");
        res.append("[" + lo + ", " + hi + "]");
        int lc = leafCnt();
        if (lc > 1) {
            res.append(" (" + lc + " leaves)");
        }
        res.append(" " + estimate.toString());
        res.append(" " + efficiency());
        res.append("]");
        return res.toString();
    }

    public static void main(String[] args) {
        Precision p = new Precision();
        p.relativeError = 1e-10;
        p.absoluteError = 1e-16;
        AdaptiveRombergIntegral ig;
        NumericEstimate est;

        DoubleUnaryOperator quad = x -> 1 + 5 * x + 2 * x* x;
        System.out.println((new AdaptiveRombergIntegral(quad, 0, 10, 17)).integral(p));
        
        DoubleUnaryOperator gaussian =
            x -> Math.exp(-x*x/2)/Math.sqrt(2 * 3.14159265358979);
        System.out.println((new AdaptiveRombergIntegral(gaussian, 0, 4, 17)).integral(p));

        ig = new AdaptiveRombergIntegral(gaussian, 0, 10, 17);
        est = ig.integral(p);
        System.out.println(est);
        double expected = 0.5;
        double actual = est.value;
        boolean ok = Math.abs(expected - actual) < 1e-10;
        System.out.println("Test result: " + (ok ? "OK" : "FAILED"));
        System.out.println(ig.getRoot());
        // System.out.println(ig.getRoot().domainTree());

        double[] ys=sample(gaussian, -4, 12, 513);
        System.out.println(integral(ys, 0, 4, 128, 257));

    }
}
