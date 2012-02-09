package gov.nist.pededitor;

/** @author Eric Boesch */

public class ContinuedFraction {
    public long numerator;
    public long denominator;

    public ContinuedFraction() {
        numerator = 0;
        denominator = 0;
    }

    public ContinuedFraction(long numerator, long denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /** Internal-use class to keep track of the fraction plus the
        extra field lastFrac. */
    static class Extra {
        long numerator;
        long denominator;

        // lastFrac, the frac value of the last step, is a quality
        // indicator: values << 1 indicate a good fit, unless this is
        // the very first step and the value was very close to 0 to
        // begin with.

        double lastFrac;

        Extra(long n, long d, double f) {
            numerator = n;
            denominator = d;
            lastFrac = f;
        }
    }

    ContinuedFraction(Extra extra) {
        numerator = extra.numerator;
        denominator = extra.denominator;
    }

    public double toDouble() {
        return ((double) numerator) / denominator;
    }

    /** Return a fractional approximation of the value d, or null if
        no approximation that satisfies maxStepError is found before
        maxMinAtor or maxDenominator is exceeded.

       @param maxStepError This is the success criterion for accuracy.
       Return the current fractional approximation if during any
       iteration of the continued fraction, the integer part of the
       remainder is nonzero and the fractional part is less than
       (stepNo * maxStepError). For example, if maxStepError = 0.001
       and

           x= 4 + 1/(3 + 1/(5 + .000001))

       then this function will return (4 + 1/(3 + 1/5)) = 69/16.

       (With infinite precision, the probability that the above test
       would eventually succeed because of blind luck equals 1, but
       realistically, and especially if maxMinAtor is small, the
       probability will be a small multiple of maxStepError.)

       @param maxMinAtor If nonzero, then return null (failure) if
       during any iteration of the continued fraction,

           min(|numerator|, denominator) > maxMinAtor

       @param maxDenominator If nonzero, return null (failure) if
       during any step, the denominator exceeds this value.
    */
    static public ContinuedFraction create
        (double x, double maxStepError, int maxMinAtor,
         long maxDenominator) {
        double oldError = 0.0;

        for (int steps = 0; ; ++steps) {
            // Starting from scratch each time is inefficient but easy to follow.

            Extra f = createBySteps(x, steps);
            if (f == null) {
                return null;
            }

            long minAtor = Math.min(Math.abs(f.numerator), f.denominator);

            if ((maxMinAtor != 0 && minAtor > maxMinAtor)
                || (maxDenominator != 0 && f.denominator > maxDenominator)) {
                return null;
            }

            double error = Math.abs(((double) f.numerator) / f.denominator - x);

            if (steps > 0 && error >= oldError) {
                // The limit of numerical accuracy was reached, but
                // error requirements were not met.
                return null;
            }
            oldError = error;

            if (f.numerator != 0
                && Math.abs(f.lastFrac) * (steps+1) <= maxStepError) {
                return new ContinuedFraction(f);
            }
        }
    }

    /**
       @return the ContinuedFraction that is obtained after continuing
       the fraction for "steps" steps, or null if the fraction cannot
       be computed. */
    static Extra createBySteps(double x, int steps) {
        double di = (x < 0) ? Math.ceil(x) : Math.floor(x);
        if (di < Long.MIN_VALUE || di > Long.MAX_VALUE) {
            return null;
        }

        long i = (long) di;
        double frac = x - i;

        if (steps == 0 || frac == 0) {
            return new Extra(i, 1, frac);
        }

        Extra output = createBySteps(1.0 / frac, steps-1);
        if (output == null) {
            return null;
        }
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

    /** Extend Double.parseDouble() with fraction and percentage
        handling. */
    public static double parseDouble(String s)
        throws NumberFormatException {
        s = s.trim();
        double mul = 1.0;
        if (s.length() > 0 && s.charAt(s.length() - 1) == '%') {
            mul = 0.01;
            s = s.substring(0, s.length() -1);
        }

        try {
            return mul * Double.parseDouble(s);
        } catch (NumberFormatException e) {
            // Test for fraction format.
        }

        int p = s.indexOf('/');
        if (p <= 0) {
            throw new NumberFormatException
                ("Invalid number format '" + s + "'");
        }

        long num = Long.parseLong(s.substring(0, p));
        long den = Long.parseLong(s.substring(p + 1));

        if (den == 0) {
            throw new NumberFormatException("Zero denominator");
        }

        return mul * num / den;
    }

    /** Return a string representation of x as a fraction if x
        resembles a fraction and looks good as one, or a decimal or
        exponential format otherwise.

        @param showPercentage Show non-fractions as percentages, but
        leave fractions alone. */

    static String toString(double x, boolean showPercentage) {
        String suffix = showPercentage ? "%" : "";
        double xp = showPercentage ? (x * 100) : x;
        ContinuedFraction f = ContinuedFraction.create(x, 0.0000001, 1000, 0);

        if (f != null) {
            if (f.denominator == 1) {
                return xp + suffix;
            }
            if (!f.looksLikeDecimal()) {
                return f.toString();
            }

            
            int tens = 0;
            long pow10 = 1;
            while (pow10 % f.denominator != 0) {
                ++tens;
                pow10 *= 10;
            }
            if (showPercentage) {
                tens -= 2;
            }

            return String.format("%." + (tens > 0 ? tens : 0) + "f", xp)
                + suffix;
        }

        return String.format("%g", xp) + suffix;
    }
}