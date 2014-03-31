/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Adaptive Romberg integration which remembers previous results.
    Compared to regular Romberg, this may be useful for highly skewed
    data sets (e^x over [-100, 0] for example) or for repeated
    integrations over roughly the same domain.

    It would be cool to use Romberg integration of arbitrary degree,
    but it's simpler if each leaf uses the same degree, and then you
    don't have to have each integration interval split exactly in
    half. If your function is so smooth that higher-order Romberg
    estimates do converge much faster, then unless each evaluation is
    very expensive, you probably don't need the speed boost the higher
    powers provide anyway.
*/
public class AdaptiveRombergIntegral {
    double lo;
    double hi;
    AdaptiveRombergIntegral left = null;
    AdaptiveRombergIntegral right = null;
    AdaptiveRombergIntegral parent = null;
    RealFunction f;
    NumericEstimate estimate;
    double[] ys = null;

    // LEAF_SIZE must equal a value of the form (2**i+1) that is
    // at least 5 -- so 5, 9, 17, 33, ... are the allowed values.
    static final int LEAF_SIZE = 9;

    public AdaptiveRombergIntegral(RealFunction f, double lo, double hi) {
        this.f = f;
        this.lo = lo;
        this.hi = hi;
        ys = sample(f, lo, hi, LEAF_SIZE);
        estimate = integral(ys, lo, hi);
    }

    /** Construct a new object that is a parent of left and right. */
    public AdaptiveRombergIntegral(AdaptiveRombergIntegral left,
                                   AdaptiveRombergIntegral right) {
        assert(left.hi == right.lo);
        lo = left.lo;
        hi = right.hi;
        f = left.f;
        this.left = left;
        this.right = right;
        left.parent = this;
        right.parent = this;
        updateEstimate(false);
    }

    public NumericEstimate integral() {
        return estimate.clone();
    }

    public RealFunction getFunction() {
        return f;
    }

    public final boolean isLeaf() {
        return left == null;
    }

    public boolean closeEnough(Precision p) {
        return p.closeEnough(estimate);
    }

    void updateEstimate(boolean recurse) {
        // It can get expensive if we have to back all the way up the
        // tree every time we change a leaf. Unfortunately, not
        // backing up every time can lead to bugs if you're not
        // careful.
        estimate = left.estimate.clone();
        estimate.add(right.estimate);
        if (recurse && parent != null) {
            parent.updateEstimate(recurse);
        }
    }

    /** A good guess is that the extent to which each additional
        sample will reduce the width of the estimate is proportional
        to the width divided by the number of samples already used.
        (If the width is very narrow already, then there is little to
        gain; and if very many samples were already used, then it is
        likely that a few more will lead to little improvement.)
    */
    double efficiency() {
        return estimate.width() / estimate.sampleCnt;
    }

    /** Add a parent whose right child has the same width as this. */
    void expandRight() {
        AdaptiveRombergIntegral rightSibling
            = new AdaptiveRombergIntegral(f, hi, hi + (hi - lo));
        new AdaptiveRombergIntegral(this, rightSibling);
    }

    /** Return true if this is a left child. */
    boolean isLeft() {
        return parent != null && parent.left == this;
    }

    /** Return true if this is a right child. */
    boolean isRight() {
        return parent != null && parent.left == this;
    }

    /** Incrementally improve the accuracy of this estimate. Return
        the number of new samples that were needed. */
    public int refine() {
        int oldSampleCnt = estimate.sampleCnt;
        refine(efficiency() / 2);
        updateEstimate(true);
        return estimate.sampleCnt - oldSampleCnt;
    }

    void refine(double minEfficiency) {
        if (efficiency() <= minEfficiency) {
            return;
        }
        if (isLeaf()) {
            // Expand this leaf node.
            double mid = (lo + hi) / 2;
            left = new AdaptiveRombergIntegral(f, lo, mid);
            left.parent = this;
            right = new AdaptiveRombergIntegral(f, mid, hi);
            right.parent = this;
        } else {
            // The average efficiency of the children is always
            // greater than or equal to the efficiency of the parent.
            // So if the parent's efficiency exceeds the threshold, at
            // least one of the children's will, too, recursively
            // guaranteeing that we will reach one or more leaf nodes
            // to expand.
            left.refine(minEfficiency);
            right.refine(minEfficiency);
        }
        assert(estimate.width() > 0);
        updateEstimate(false);
        assert(estimate.width() > 0);
    }

    public NumericEstimate integral(Precision p) {
        while (!closeEnough(p)
               && estimate.status !=
               NumericEstimate.Status.TOO_SMALL_STEP_SIZE) {
            if (estimate.sampleCnt > p.maxSampleCnt) {
                NumericEstimate e = integral();
                e.status = NumericEstimate.Status.TOO_MANY_STEPS;
                return e;
            }
            assert(estimate.width() > 0);
            refine();
        }
        return integral();
    }

    static double[] sample(RealFunction f, double lo, double hi,
                                   int sampleCnt) {
        double[] res = new double[sampleCnt];
        for (int i = 0; i < res.length; ++i) {
            res[i] = f.value(lo + (double) i/(sampleCnt - 1) * (hi - lo));
        }
        return res;
    }

    /** Function that just returns the closest of the data point
        values that were passed into its constructor. */
    static class StepFunction implements RealFunction {
        double[] dataPoints;
        double lo, hi;

        public StepFunction(double[] dataPoints, double lo, double hi) {
            this.dataPoints = dataPoints;
            this.lo = lo;
            this.hi = hi;
        }

        @Override public double value(double x) {
            int sampleNo = (int) Math.rint((dataPoints.length - 1) * (x - lo)/(hi - lo));
            return dataPoints[sampleNo];
        }
    }

    static NumericEstimate integral(double[] ys, double lo, double hi) {
        Precision p = new Precision();
        p.absoluteError = p.relativeError = 0;
        p.minSampleCnt = p.maxSampleCnt = ys.length;

        // It's kind of a hack, but it allows me to call
        // RombergIntegral instead of writing a new variant:
        NumericEstimate res = RombergIntegral.integral
            (new StepFunction(ys, lo, hi), lo, hi, p);
        if (res.status == NumericEstimate.Status.TOO_MANY_STEPS) {
            // That's OK; I designed the precision rules to force that to happen.
            res.status =  NumericEstimate.Status.OK;
        }
        return res;
    }

    public static void main(String[] args) {
        Precision p = new Precision();
        p.relativeError = 1e-10;
        p.absoluteError = 1e-16;
        RealFunction gaussian = new RealFunction() {
                @Override public double value(double x) {
                    return Math.exp(-x*x/2)/Math.sqrt(2 * 3.14159265358979);
                }
            };

        AdaptiveRombergIntegral ig = new AdaptiveRombergIntegral
            (gaussian, 0, 400);
        NumericEstimate res = ig.integral(p);
        System.out.println(res);
        double expected = 0.5;
        double actual = res.value;
        boolean ok = Math.abs(expected - actual) < 1e-10;
        System.out.println("Test result: " + (ok ? "OK" : "FAILED"));
    }
}
