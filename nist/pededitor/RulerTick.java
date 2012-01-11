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
        precision and capacity to display all values in range [min,
        max] with the given absolute precision. For example,
        formatString(-37.001, 5, 0.1) equals "%5.1f" since that format can
        represent all the values {-37.0, -36.9, ..., 5.0}.
    */
    public static String formatString(double min, double max, double precision) {
        int[] limits = digitSpaceNeeded(min, max, precision);
        int before = limits[0];
        int after = limits[1];

        int width = before + after;
        if (after > 0) {
            ++width;
        }
        return "%" + width + '.' + after + 'f';
    }

    /** Return a 2-element array { before, after } representing the
        number of characters before and after the decimal point that
        are needed to display all values in range [min, max] to the
        given absolute precision. */
    public static int[] digitSpaceNeeded(double min, double max,
                                         double precision) {
        int[] limits = digitSpaceNeeded(precision);
        int after = limits[1];

        limits = digitSpaceNeeded(min);
        int before = limits[0];

        limits = digitSpaceNeeded(max);
        limits[0] = Math.max(before, limits[0]);
        limits[1] = after;

        return limits;
    }
}
