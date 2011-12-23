package gov.nist.pededitor;

/** @author Eric Boesch */

public class ContinuedFraction {
    public long numerator;
    public long denominator;

    public ContinuedFraction(long numerator, long denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public ContinuedFraction() {
        numerator = 0;
        denominator = 0;
    }

    /** @return a fractional approximation of the value d, or null if
        no approximation that satisfies maxError and maxMinAtor
        exists.

       @param maxError If nonzero, this sets an upper limit on the
       absolute value of the approximation error times the numerator
       times the denominator. If the floating point value is truly an
       approximation of a fraction, then low maxError values such as
       0.0001 are usually achievable.

       @param maxMinAtor If nonzero, sets an upper limit on the
       minimum of the absolute value of the numerator and the
       denominator of a fractional representation. This reflects a
       sense that a fraction such as 6673/1 may be legitimate, and a
       fraction such as 3/295837 may be legitimate, but a fraction
       like 24983/47292 is more likely to be random noise.
    */
    static public ContinuedFraction create
        (double x, double maxRelativeError, double maxMinAtor) {
        double oldError = 0.0;

        for (int steps = 0; ; ++steps) {
            ContinuedFraction f = createBySteps(x, steps);
            double error = Math.abs(((double) f.numerator) / f.denominator - x);
            if (steps > 0 && error > oldError) {
                // The limit of numerical accuracy was reached without
                // error requirements being met.
                return null;
            }
            oldError = error;

            if (maxRelativeError != 0
                && error * Math.abs((double) f.numerator * f.denominator)
                                    > maxRelativeError) {
                continue;
            }

            if (maxMinAtor != 0 &&
                Math.min(Math.abs(f.numerator), Math.abs(f.denominator))
                > maxMinAtor) {
                return null;
            }

            return f;
        }
    }

    /**
       @return the ContinuedFraction that is obtained after continuing
       the fraction for "steps" steps. */
    static public ContinuedFraction createBySteps(double x, int steps) {
        long i = (x < 0) ? (long) Math.ceil(x) : (long) Math.floor(x);
        double frac = x - i;

        if (steps == 0 || frac == 0) {
            return new ContinuedFraction(i, 1);
        }

        ContinuedFraction output = createBySteps(1.0 / frac, steps-1);
        i = i * output.numerator + output.denominator;
        output.denominator = output.numerator;
        output.numerator = i;
        return output;
    }

    public boolean looksLikeDecimal() {
        if (Math.abs(numerator) <= 1) {
            return false;
        }

        int twos = 0;
        long deno = Math.abs(denominator);
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
}