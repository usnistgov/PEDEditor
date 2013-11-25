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


    /** Return a 2-element array { pow10High, pow10Low } representing
        the exponents of ten needed to show the most and least
        significant figures of v, respectively. If v is not an even
        multiple of a power of 10, use 3 significant digits.

        Return null if v equals 0. */
    public static int[] pow10Range(double v) {
        if (v == 0) {
            return null;
        }

        if (v < 0) {
            v = -v;
        }

        // Bump up v's magnitude a tiny bit so afterward we only have
        // to worry about rounding down, not up.
        v *= 1 + 1e-12;

        // Greatest power of ten with absolute value equal to or less than v.
        int pow10High = (int) Math.floor(Math.log10(v));
        // Greatest power of ten with absolute value equal to or less
        // than the least significant digit of v.
        int pow10Low = pow10High;
        for (pow10Low = pow10High; ; --pow10Low) {
            if (pow10Low == pow10High - 6) {
                // Fall back on 3 significant digits.
                pow10Low = pow10High - 2;
                break;
            }
            double vScaled = v / Math.pow(10, pow10Low);
            vScaled -= Math.floor(vScaled);
            if (vScaled < 1e-8) {
                break; // Nearly perfect approximation.
            }
        }

        return new int[] { pow10High, pow10Low };
    }

    /** Return a 2-element array { pow10High, pow10Low } representing
        the exponents of ten needed to show the most and least
        significant figures of all terms. If any of those values do
        not represent even powers of 10, then use 3 significant
        digits.

        Return [0,0] if all values equal 0. */
    public static int[] pow10Range(double... terms) {
        if (terms.length == 0) {
            return null;
        }
        int[] limits = pow10Range(terms[0]);
        for (int i = 1; i < terms.length; ++i) {
            double v = terms[i];
            int[] limits2 = pow10Range(v);
            if (limits2 == null) {
                continue;
            }
            if (limits == null) {
                limits = limits2;
            } else {
                limits[0] = Math.max(limits[0], limits2[0]);
                limits[1] = Math.min(limits[1], limits2[1]);
            }
        }

        return (limits == null) ? new int[2] : limits;
    }

    /** Return a Formatter format string with sufficient
        precision and capacity to display all values in range [min,
        max] with the given step size. For example,
        formatString(-37.001, 5, 0.1) equals "%5.1f" since that format
        can represent all the values {-37.0, -36.9, ..., 5.0}.
    */
    public static String formatString(double... terms) {
        int[] limits = pow10Range(terms);
        if (limits == null) {
            limits = new int[] {0,0};
        }

        int maxPow10 = limits[0];
        int minPow10 = limits[1];

        boolean minus = false;
        for (double d: terms) {
            if (d < 0) {
                minus = true;
            }
        }
        int minusPlace = minus ? 1 : 0;

        if (maxPow10 < -4 || minPow10 > 5 || maxPow10 > 7 ) {
            // Allow 4 extra spots for the E, the exponent sign, and
            // two digits of exponent.
            int decimalPlace = (maxPow10 > minPow10) ? 1 : 0;
            return "%" + (minusPlace + decimalPlace + 4
                          + 1 + maxPow10 - minPow10)
                + '.' + (maxPow10 - minPow10) + 'E';
        } else {
            int units = 1 + Math.max(0, maxPow10);
            int decimals = Math.max(0, -minPow10);
            int decimalPlace = decimals > 0 ? 1 : 0;
            return "%"
                + (minusPlace + units + decimalPlace + decimals)
                + '.' + decimals + 'f';
        }
    }

    /** Return a string that is about as wide as any that might be
        returned by formatString(terms) applied to any value in the given range. */
    public static String longestString(double... terms) {
        int[] limits = pow10Range(terms);
        if (limits == null) {
            return "0";
        }
        String format = formatString(terms);
        int maxPow10 = limits[0];
        int minPow10 = limits[1];
        StringBuilder res= new StringBuilder();

        boolean minus = false;
        for (double d: terms) {
            if (d < 0) {
                minus = true;
            }
        }
        if (minus) {
            res.append('-');
        }
        if (maxPow10 >= 0) {
            for (int i = maxPow10; i >= 0; --i) {
                res.append('8');
            }
            if (minPow10 < 0) {
                res.append('.');
                for (int i = -1; i >= minPow10; --i) {
                    res.append('8');
                }
            }
        } else {
            res.append("0.");
            int i;
            for (i = -1; i > maxPow10; --i) {
                res.append('0');
            }
            for (; i >= minPow10; --i) {
                res.append('8');
            }
        }
        return String.format(format, Double.parseDouble(res.toString()));
    }

    public static void main(String[] args) {
        for (double d: new double[]
            {1e4, 1e-2, 2.5, 2.5237489220, -2e17, 3e-12 }) {
                int[] limits = pow10Range(d);
                System.out.println(d);
                for (int i: limits) {
                    System.out.println(i);
                }
                String format = formatString(d);
                System.out.println(format);
                System.out.println(String.format(format, d));
                System.out.println(longestString(d));
                System.out.println();
            }
    }
}
