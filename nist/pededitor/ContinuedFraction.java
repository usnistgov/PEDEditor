package gov.nist.pededitor;

/** @author Eric Boesch */

public class ContinuedFraction {
    public long numerator;
    public long denominator;

    static class Extra {
        long numerator;
        long denominator;

        // lastFrac, the frac value of the last step, is a quality
        // indicator: a value much closer to 0 than 1 indicates a good
        // fit.
        double lastFrac;

        Extra(long n, long d, double f) {
            numerator = n;
            denominator = d;
            lastFrac = f;
        }
    }

    public ContinuedFraction(long numerator, long denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public ContinuedFraction() {
        numerator = 0;
        denominator = 0;
    }

    ContinuedFraction(Extra extra) {
        numerator = extra.numerator;
        denominator = extra.denominator;
    }

    public double toDouble() {
        return ((double) numerator) / denominator;
    }

    /** @return a fractional approximation of the value d, or null if
        no approximation that satisfies maxError and maxMinAtor
        exists.

       @param maxRelativeError Terminate successfully if during any
       step after step #0,

           | error * denominator * stepNo | < maxRelativeError

       If the floating point value is truly the closest approximation
       of a fraction, then low limits such as 0.00001 are usually
       achievable.

       (With infinite precision, the probability of the above test
       eventually passing during some step would equal 1, but
       realistically, and especially if maxMinAtor is small, the
       probability will be a small multiple of maxRelativeError.)

       @param maxMinAtor If nonzero, then return null (failure) if
       during any step,

           min(|numerator|, denominator) > maxMinAtor

       @param maxDenominator If nonzero, return null (failure) if the
       denominator exceeds this value.
    */
    static public ContinuedFraction create
        (double x, double maxRelativeError, int maxMinAtor,
         long maxDenominator) {
        double oldError = 0.0;

        for (int steps = 0; ; ++steps) {
            // Starting from scratch each time is not the way to
            // achieve good performance, but it is easier to understand.

            Extra f = createBySteps(x, steps);

            long minAtor = Math.min(Math.abs(f.numerator), f.denominator);

            if ((maxMinAtor != 0 && minAtor > maxMinAtor)
                || (maxDenominator != 0 && f.denominator > maxDenominator)) {
                return null;
            }

            double error = Math.abs(((double) f.numerator) / f.denominator - x);
            if (error == 0) {
                return new ContinuedFraction(f);
            }

            if (steps > 0 && error >= oldError) {
                // The limit of numerical accuracy was reached, but
                // error requirements were not met.
                return null;
            }
            oldError = error;

            if (steps > 0 && Math.abs(f.lastFrac) * steps <= maxRelativeError) {
                return new ContinuedFraction(f);
            }
        }
    }

    /**
       @return the ContinuedFraction that is obtained after continuing
       the fraction for "steps" steps. */
    static Extra createBySteps(double x, int steps) {
        long i = (x < 0) ? (long) Math.ceil(x) : (long) Math.floor(x);
        double frac = x - i;

        if (steps == 0 || frac == 0) {
            return new Extra(i, 1, frac);
        }

        Extra output = createBySteps(1.0 / frac, steps-1);
        i = i * output.numerator + output.denominator;
        output.denominator = output.numerator;
        output.numerator = i;
        if (output.denominator < 0) {
            output.numerator *= -1;
            output.denominator *= -1;
        }
        return output;
    }

    /** Return true if this fraction equals and looks better as a
        terminating decimal. For example, 7/50 looks nicer as 0.14
        (true), but 1/8 looks nicer than 0.125 (false). */
    public boolean looksLikeDecimal() {
        if (Math.abs(numerator) <= 1) {
            return false;
        }

        int twos = 0;
        long deno = denominator;
        while (deno % 2 == 0) {
            ++twos;
            deno /= 2;
        }
        int fives = 0;
        while (deno % 5 == 0) {
            ++fives;
            deno /= 5;
        }

        return (deno == 1 && fives >= 1 && fives * 2 >= twos);
    }

    public String toString() {
        return numerator + "/" + denominator;
    }
}