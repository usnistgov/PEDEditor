/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.ArrayList;
import java.util.function.DoubleUnaryOperator;

/** Utilities to solve problems of the form integral_lo^x (f(u) du) =
    y. (For instance, if p(x) is the probability density at x, you
    might want to find the value med such that P(X < med) = 0.5, or if
    f(t) is the differential of the arc length of a parameterized
    function, you might want to find the t value where the arc length
    equals 1.)

    You can efficiently perform many integrations using the same lower
    bound and the same function by reusing the AdaptiveRombergIntegral
    object used in previous calls.
*/
public class AdaptiveRombergIntegralY {

    /** Return the value x such that the integral from lo to x of f
        equals y.

        @param extrapolate If true, allow the domain to be expanded
        beyond that of the input parameter r.
    */
    public static NumericEstimate integraly(AdaptiveRombergIntegral r,
                                            double y, Precision p, boolean extrapolate) {
        double maxError = p.maxError(y);
        Precision p1 = p.clone();
        // Allocate half of the total error allowance to the leaf.
        p1.absoluteError = maxError;
        p1.relativeError = 0;
        return integraly(r, new Stats(0, maxError), y, p1, extrapolate);
    }

    /* Return the x value of the given quantile (that is, the value x
     * such that the fraction of the area under f within the domain
     * [r.lo, r.hi] that lies to the left of x equals y). The
     * precision bounds apply to quantile of the returned x value, not
     * the x value itself. (For example, for a standard Gaussian
     * variable over the domain [-100, 100], 10 would be considered a
     * very accurate return value for quantile(1.0), because the
     * actual quantile of 10 is very close to 1.0 even if 10 is not
     * close to 100.)

     * p.relativeError and p.absoluteError are in this case almost
     * completely redundant, since we know relativeError ~=
     * absoluteError/q.
     */

    public static NumericEstimate quantile(AdaptiveRombergIntegral r,
                                     double q, Precision p) {
        if (q == 0) {
            return new NumericEstimate(0);
        }
        if (q < 0) {
            throw new IllegalArgumentException("quantile: q value " + q + " is negative");
        }
        Precision pTotal = new Precision();
        double relativeError = Math.max(p.relativeError, p.absoluteError/q);
        pTotal.relativeError = relativeError / 4;
        NumericEstimate total = r.integral(pTotal);
        if (!total.isOK()) {
            total.lowerBound = 0;
            total.upperBound = 1;
            total.value = (r.lo + r.hi)/2;
            return total;
        }
        if (total.isExact() && total.value == 0) {
            total.value = r.lo;
            return total;
        }
        relativeError -= total.relativeError();
        p = p.clone();
        p.maxSampleCnt -= total.sampleCnt;
        p.absoluteError = 0;
        p.relativeError = relativeError;
        NumericEstimate part = integraly(r, q * total.value, p, false);
        part.lowerBound /= total.upperBound;
        part.upperBound /= total.lowerBound;
        return part;
        
    }

    static class Stats {
        int sampleCnt;
        // Upper bound on the absolute error in integraly values
        // introduced above the leaf level.
        double maxNonleafWidth;
        Stats(int sampleCnt, double maxNonleafWidth) {
            this.sampleCnt = sampleCnt;
            this.maxNonleafWidth = maxNonleafWidth;
        }
        void add(int i) {
            sampleCnt += i;
        }
    }

    static NumericEstimate integraly
        (AdaptiveRombergIntegral r, Stats scnt, double y, Precision p,
         boolean extrapolate) {

        if (y < 0) {
            throw new IllegalArgumentException
                ("integraly() is not currently designed to handle integration of " +
                 "functions with negative values.");
        }

        if (r.getSampleCnt() == 0) {
            scnt.add(r.refine());
        }

        if (p.closeEnough(y, 0)) {
            NumericEstimate res = new NumericEstimate(0);
            res.value = r.lo;
            return res;
        }

        outer:
        for (;;) {
            r = r.getRoot();

            if (y > r.estimate.upperBound) {
                if (extrapolate) {
                    // Expand the domain rightwards.
                    if (r.estimate.value <= 0) {
                        throw new IllegalArgumentException
                            ("integraly() is not currently designed to handle integration of " +
                             "functions with negative values.");
                    }
                    if (r.parent == null) {
                        if (scnt.sampleCnt >= p.maxSampleCnt) {
                            NumericEstimate res = NumericEstimate.bad(r.hi);
                            res.sampleCnt = scnt.sampleCnt;
                            res.status = NumericEstimate.Status.TOO_MANY_STEPS;
                            return res;
                        } else {
                            scnt.add(r.expandRight());
                        }
                    }

                    r = r.parent;
                    continue;
                } else if (scnt.sampleCnt >= p.maxSampleCnt
                           || (scnt.sampleCnt >= p.minSampleCnt
                               && y >= r.estimate.value + 100 * r.estimate.width())) {
                    NumericEstimate res = r.estimate.clone();
                    res.status = NumericEstimate.Status.IMPOSSIBLE;
                    return res;
                } else {
                    // Maybe a better estimate will bring y back into range.
                    scnt.add(r.refine());
                }
            }

            NumericEstimate leftArea = new NumericEstimate(0);

            for (;;) {
                if (r.isLeaf()) {
                    if (leftArea.width() > scnt.maxNonleafWidth) {
                        scnt.add(refineBestLeftSibling(r));
                        continue outer;
                    }
                    NumericEstimate lfest = leafIntegraly(r, y - leftArea.value);
                    NumericEstimate combined = lfest.clone();
                    combined.addNoV(leftArea);
                    combined.sampleCnt = scnt.sampleCnt;
                    if (p.closeEnough(combined, y)) {
                        return combined;
                    } else if (scnt.sampleCnt >= p.maxSampleCnt) {
                        combined.status = NumericEstimate.Status.TOO_MANY_STEPS;
                        return combined;
                    } else {
                        scnt.add(r.expand(true));
                        continue;
                    }
                }

                NumericEstimate lest = r.left.estimate;
                if (lest.value >= y - leftArea.value) {
                    r = r.left;
                } else {
                    leftArea.add(lest);
                    r = r.right;
                }
            }
        }
    }

    static int refineBestLeftSibling(AdaptiveRombergIntegral r) {
        double maxEfficiency = 0;
        AdaptiveRombergIntegral best = null;

        for (AdaptiveRombergIntegral ancest = r;
             ancest.parent != null;
             ancest = ancest.parent) {
            if (ancest.isRight()) {
                AdaptiveRombergIntegral lsib = ancest.parent.left;
                if (lsib.efficiency() > maxEfficiency) {
                    maxEfficiency = lsib.efficiency();
                    best = lsib;
                }
            }
        }
        return best.refine();
    }

    /** Return {c,b,a} for the quadratic interpolation ax^2 + bx + c
        through the data points {(0,y0), (1,y1), (2,y2) */
    static double[] fitQuadTo(double y0, double y1, double y2) {
        // 0a + 0b + c = y0
        // 1a + 1b + c = a + b + y0 = y1       (2)
        // 4a + 2b + c = 4a + 2b + y0 = y2     (3)

        // (3) minus two times (2) equals
        // y2-2y1 = 2a - y0

        // a = (y0 + y2 - 2y1) / 2
        double c = y0;
        double a = (y0 + y2 - 2 * y1) / 2;
        double b = y1 - y0 - a;
        return new double[] {c, b, a};
    }

    /** Return simpson's estimate of the integral using the three data
        points {(0, y0), {1, y1), (2, y2)) */
    static double simpson(double y0, double y1, double y2) {
        return (y0 + y2 + 4 * y1)/3;
    }
    
    /** integraly for the 5 points {(xlo, ys[0]), (xlo + xStep,
        ys[1]), ..., (xlo + xStep * 4, ys[4])}. Also returns the
        leftover y part, if the integral over the entire region
        appears to be less than y. That is, res.d != 0 if and only if
        the answer appears to lie to the right of xlo + xStep * 4.
    */
    static NumericEstimate integraly5(double[] ys, int offset,
                                      double xlo, double y, double xStep) {
        double a = ys[offset];
        double b = ys[offset+1];
        double c = ys[offset+2];
        double d = ys[offset+3];
        double e = ys[offset+4];
        double[] crudeQuad = fitQuadTo(a,c,e);
        double yError = (Math.abs(Polynomial.evaluate(0.5, crudeQuad) - b)
                         + Math.abs(Polynomial.evaluate(1.5, crudeQuad) - d)) / 2;
        double lint = simpson(a,b,c) * xStep; // Left-half integral
        double xStart;
        double[] fineQuad;
        if (y > lint) {
            // Value appears to lie in the right half.
            xStart = xlo + 2 * xStep;
            fineQuad = fitQuadTo(c,d,e);
        } else {
            // Value appears to lie in the left half.
            lint = 0;
            xStart = xlo;
            fineQuad = fitQuadTo(a,b,c);
        }
        double[] cubic = Polynomial.integral(fineQuad);
        cubic[0] -= (y - lint) / xStep; // Set cubic(x) = scaled leftover y
        double[] roots = Polynomial.solve(cubic);

        if (roots.length == 0) {
            return NumericEstimate.bad(0);
        }

        // Find the root closest to the center of the interval [0,2]
        // (that is, closest to 1).
        double root = roots[0];
        for (int i = 1; i < roots.length; ++i) {
            double thisRoot = roots[i];
            if (Math.abs(thisRoot - 1) < Math.abs(root - 1)) {
                root = thisRoot;
            }
        }

        // Translate the x domain from [0,2] back into [xStart, xStart + 2*xStep]
        root = root * xStep + xStart;
        double yIntegralError = Math.abs(yError * (root - xlo));
        NumericEstimate est = new NumericEstimate(root);
        est.lowerBound = y - yIntegralError;
        est.upperBound = y + yIntegralError;
        return est;
    }

    static public NumericEstimate integraly(double[] ys, double lo, double hi,
                                            double y) {
        return integraly(ys, lo, hi, 0, ys.length, y);
    }

    static public NumericEstimate integraly
        (double[] ys, double lo, double hi, int startIndex, int endIndex,
         double y) {
        NumericEstimate res = new NumericEstimate(0);

        // Divide [lo,hi] in half until we are down to 5 data points,
        // then call integraly5 on them.

        while (endIndex - startIndex > 5) {
            double mid = (lo + hi) / 2;
            int midIndex = (startIndex + endIndex)/2;
            NumericEstimate left = AdaptiveRombergIntegral.integral
                (ys, lo, mid, startIndex, midIndex+1);
            if (y > res.value + left.value) {
                res.add(left);
                lo = mid;
                startIndex = midIndex;
            } else {
                hi = mid;
                endIndex = midIndex+1;
            }
        }
        NumericEstimate i5 = integraly5
            (ys, startIndex, lo, y - res.value, (hi - lo)/4);
        i5.addNoV(res);
        return i5;
    }
                                
    static NumericEstimate leafIntegraly(AdaptiveRombergIntegral r, double y) {
        return integraly(r.ys, r.lo, r.hi, y);
    }

    static NumericEstimate badIntegraly(AdaptiveRombergIntegral r) {
        NumericEstimate res = NumericEstimate.bad(r.estimate.value);
        res.sampleCnt = r.estimate.sampleCnt;
        return res;
    }

    public static void main(String[] args) {
        AdaptiveRombergIntegral ig;
        Precision p = new Precision();
        p.relativeError = 1e-10;
        p.absoluteError = 1e-16;

        Precision pInversion = new Precision();
        pInversion.relativeError = 1e-12;
        pInversion.absoluteError = 1e-16;

        DoubleUnaryOperator gaussian =
            x -> Math.exp(-x*x/2)/Math.sqrt(2 * 3.14159265358979);

        ig = new AdaptiveRombergIntegral(gaussian, 0, 5, 33);
        ArrayList<Double> testValues = new ArrayList<>();
        int valueCnt = 100;
        for (int i = 0; i < valueCnt; ++i) {
            testValues.add(i / 2.0 / valueCnt);
        }
        testValues.add(0.4999999999);
        int failCnt = 0;
        for (double quant: testValues) {
            NumericEstimate res = integraly(ig, quant, p, true);
            System.out.println(res);

            res = RombergIntegral.integral(gaussian, 0, res.value, pInversion);
            System.out.println(res);
            double expected = quant;
            double actual = res.value;

            // We did two steps, so the total error might be up to
            // twice the precision limits of the individual steps.
            boolean ok = p.closeEnough(expected, (expected + actual)/2);
            if (!ok) {
                ++failCnt;
            }
            System.out.println("Test result: " + (ok ? "OK" : "FAILED"));
        }
        System.out.println(ig);
        System.out.println(failCnt + " test(s) failed.");
        System.out.println(quantile(ig, 0.5, p));
    }
}
