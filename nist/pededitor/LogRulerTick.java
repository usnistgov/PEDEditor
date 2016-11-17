/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Like RulerTick, but specifically for logarithmic axes. */
public class LogRulerTick {

    /** Return the greatest power of 10 that is no larger than 10 to
        the power of x plus a tiny fudge factor. */
    public static double roundFloor(double x) {
        return Math.pow(10, Math.floor(x + 1e-6));
    }

    /** Return the least power of 10 that is at least as great as 10 to
        the power of x minus a tiny fudge factor. */
    public static double roundCeil(double x) {
        return Math.pow(10, Math.ceil(x - 1e-6));
    }

    /** Return a string representation of 10^Math.round(x). */
    public static String pow10String(double x) {
        int p10 = (int) Math.round(x);
        if (p10 > 5 || p10 < -5) {
            return "1E" + p10;
        }
        StringBuilder s = new StringBuilder();
        if (p10 >= 0) {
            s.append('1');
            for (int i = 0; i < p10; ++i) {
                s.append('0');
            }
        } else {
            s.append("0.");
            for (int i = p10; i < -1; ++i) {
                s.append('0');
            }
            s.append('1');
        }
        return s.toString();
    }
}
