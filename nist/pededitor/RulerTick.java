package gov.nist.pededitor;

/** Class and utility routines for figuring out natural spacing and
    formatting of tick marks and tick mark labels. */

public class RulerTick {
    public double min;
    public double max;
    /** Change in value between ticks. */
    public double tickDelta;
    /** Change in value between labeled ticks. */
    public double textDelta;
    public String formatString;

    /** Return the greatest round number that is no larger than x plus
        a tiny fudge factor. Round numbers equal 1, 2, or 5 times a
        power of ten. */
    public static double roundFloor(double x) {
        if (x == 0) {
            return 0;
        }
        if (x < 0) {
            return -roundCeil(-x);
        }

        x *= 1.000001; // Fudge factor for roundoff error
        double leastPow10 = Math.pow(10,
                                     Math.floor(Math.log(x)/Math.log(10.0)));
        if (x < leastPow10 * 2)
            return leastPow10;
        if (x < leastPow10 * 5)
            return leastPow10 * 2;
        return leastPow10 * 5;
    }

    /** Return the least round number that is at least as great as x plus
        a tiny fudge factor. Round numbers equal 1, 2, or 5 times a
        power of ten. */
    public static double roundCeil(double x) {
        if (x == 0) {
            return 0;
        }
        return 1.0 / roundFloor(1.0 / x);
    }

    /** Return the next round number that is a multiple of this round
        number. */
    public static double nextLargerRound(double round) {
        double rv = roundCeil(round * 1.5);
        rv = divides(round, rv) ? rv : (round * 5.0);
        return rv;
    }

    /** Return the next smaller round number that divides this round
        number. */
    public static double nextSmallerRound(double round) {
        double rv = roundFloor(round * 0.8);
        rv = divides(rv, round) ? rv : (round / 5.0);
        return rv;
    }

    /**
     * Return true if (dividend/divisor) is an integer or very close to it.
     */
    public static boolean divides(double divisor, double dividend) {
        if (divisor == 0)
            return true;
        double r = dividend / divisor;
        return (Math.abs(r - Math.rint(r)) < 1e-10);
    }


    /** Return a 2-element array { before, after } representing the
        number of characters before and after the decimal point that
        are needed to display v with 1 significant digit. */
    public static int[] digitSpaceNeeded(double v) {
        if (v == 0) {
            return new int[] {1,0};
        }
        boolean isNegative = (v < 0);
        if (isNegative) {
            v = -v;
        }

        int pow10 = (int) Math.floor(Math.log10(v * 1.00001));
        int negint = isNegative ? 1 : 0;
        if (pow10 >= 0) {
            return new int[] {negint + pow10 + 1, 0};
        } else {
            return new int[] {negint + 1, -pow10};
        }
    }

    /** Return a fixed-point Formatter format string with sufficient
        precision and capacity to display all values to at least one
        significant digit. For example, formatString(-37, 5, 0.1)
        equals "%5.1f" since that format can represent all three
        values: "-37.0", " 5.0", and " 0.1".

        You can call this method with the three values (rulerMin,
        rulerMax, rulerStep) in order to determine a suitable format
        string for all multiples of rulerStep in range [rulerMin,
        rulerMax].
    */
    public static String formatString(double... values) {
        int[] limits = digitSpaceNeeded(values);
        int units = limits[0];
        int decimals = limits[1];

        int width = units + decimals;
        if (decimals > 0) {
            ++width;
        }
        return "%" + width + '.' + decimals + 'f';
    }

    public static int[] digitSpaceNeeded(double... values) {
        int decimals = 0;
        int units = 0;

        for (double value: values) {
            int[] limits = digitSpaceNeeded(value);
            units = Math.max(units, limits[0]);
            decimals = Math.max(decimals, limits[1]);
        }

        return new int[] { units, decimals };
    }
}
