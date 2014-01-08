/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Class and utility routines for figuring out natural spacing and
    formatting of tick marks and tick mark labels.
 */

public class RulerTick implements Cloneable {
    Integer pow10High = null;
    Integer pow10Low = null;
    boolean haveMinus = false;

    @Override public RulerTick clone() {
        RulerTick res = new RulerTick();
        res.pow10High = pow10High;
        res.pow10Low = pow10Low;
        res.haveMinus = haveMinus;
		return res;
    }

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

    /** Require this number format to permit representation of
        values at least as large as v in absolute value, but do not
        require sufficient precision to represent v precisely. */
    public void mergeHigh(double v) {
        merge(v, false);
    }

    /** Require this number format to permit representation of v
        precisely, unless v is not an even multiple of powers of 10.
        In that case, require the format to represent v to 3
        significant digits. */
    public void merge(double v) {
        merge(v, true);
    }

    void merge(double v, boolean mergeLow) {
        if (v == 0) {
            return;
        }

        if (v < 0) {
            haveMinus = true;
            v = -v;
        }

        // Bump up v's magnitude a tiny bit so afterward we only have
        // to worry about rounding down, not up.
        v *= 1 + 1e-12;

        // Greatest power of ten with absolute value equal to or less than v.
        int high = (int) Math.floor(Math.log10(v));
        if (pow10High == null || high > pow10High) {
            pow10High = high;
        }

        if (mergeLow) {
            // Greatest power of ten with absolute value equal to or less
            // than the least significant digit of v.
            int low = high;
            for (low = high; ; --low) {
                if (low == high - 6) {
                    // Fall back on 3 significant digits.
                    low = high - 2;
                    break;
                }
                double vScaled = v / Math.pow(10, low);
                vScaled -= Math.floor(vScaled);
                if (vScaled < 1e-8) {
                    break; // Nearly perfect approximation.
                }
            }

            if (pow10Low == null || low < pow10Low) {
                pow10Low = low;
            }
        }
    }

    /** Return a Formatter format string with sufficient precision and
        capacity to represent all mergeHigh() values precisely and
        values at least as large as all merge() values imprecisely.
    */
    public String formatString() {
        if (pow10High == null) {
            return "%d";
        }
        // Knowing whether a minus sign is needed is hard, so just
        // assume one is needed.

        int minusPlace = haveMinus ? 1 : 0;
        int low = (pow10Low == null) ? pow10High : pow10Low;

        if (pow10High < -4 || low > 5 || pow10High > 7 ) {
            // Use floating point. Allow 4 extra spots for the E, the
            // exponent sign, and two digits of exponent.
            int decimalPlace = (pow10High > low) ? 1 : 0;
            return "%" + (minusPlace + decimalPlace + 4
                          + 1 + pow10High - low)
                + '.' + (pow10High - low) + 'E';
        } else {
            int units = 1 + Math.max(0, pow10High);
            int decimals = Math.max(0, -low);
            int decimalPlace = decimals > 0 ? 1 : 0;
            return "%"
                + (minusPlace + units + decimalPlace + decimals)
                + '.' + decimals + 'f';
        }
    }

    /** Return a string that is as wide as any that might be returned
        by String.format(formatString(), v) for any value v in the
        range passed in through merge() or mergeHigh(). */
    public String longestString() {
        if (pow10High == null) {
            return "0";
        }

        StringBuilder res= new StringBuilder();

        int low = (pow10Low == null) ? pow10High : pow10Low;

        if (haveMinus) {
            res.append('-');
        }
        if (pow10High >= 0) {
            for (int i = pow10High; i >= 0; --i) {
                res.append('8');
            }
            if (low < 0) {
                res.append('.');
                for (int i = -1; i >= low; --i) {
                    res.append('8');
                }
            }
        } else {
            res.append("0.");
            int i;
            for (i = -1; i > pow10High; --i) {
                res.append('0');
            }
            for (; i >= low; --i) {
                res.append('8');
            }
        }
        return String.format(formatString(),
                             Double.parseDouble(res.toString()));
    }

    public static void main(String[] args) {
        for (double d: new double[]
            {1e4, 1e-2, 2.5, 2.5237489220, -2e17, 3e-12 }) {
            RulerTick rt = new RulerTick();
            rt.merge(d);
            System.out.println(d + ":" + rt.pow10Low + ", " + rt.pow10High);
            String format = rt.formatString();
            System.out.println(format);
            System.out.println(String.format(format, d));
            System.out.println(rt.longestString());
            System.out.println();
        }
    }
}
