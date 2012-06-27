package gov.nist.pededitor;

import java.util.Comparator;

/** A string comparator that differs from the natural sorting order
    for strings as follows:

    1) Digits compare as less than any non-digit character.

    2) Where both strings have identical prefixes and both contain
    sets of digits, those sets of digits are ordered by their numeric
    value, so Foo010 > Foo9Zed

    3) A given numeric value with more leading zeroes compares as
    greater than the same value without leading zeroes: Foo020 > Foo20
*/

public class MixedIntegerStringComparator implements Comparator<String> {
    private static int integerEndPos(String s, int startPos) {
        int len = s.length();
        for (int i = startPos + 1; i < len; ++i) {
            if (!Character.isDigit(s.charAt(i))) {
                // System.out.println("iep(" + s + ", " + startPos + ") = " + i);
                return i;
            }
        }
        // System.out.println("iep(" + s + ", " + startPos + ") = " + len);
        return len;
    }

    @Override public int compare(String a, String b) {
        int la = a.length();
        int lb = b.length();
        // System.out.println(String.format("compare(\"%s\", \"%s\")", a, b));
        for (int p = 0; ; ++p) {
            // System.out.println("p = " + p);
            if (p == la) {
                return (p == lb) ? 0 : -1;
            } else if (p == lb) {
                return 1;
            }
            char cha = a.charAt(p);
            char chb = b.charAt(p);
            if (Character.isDigit(cha)) {
                if (!Character.isDigit(chb)) {
                    return -1;
                }
                int integerEndA = integerEndPos(a, p);
                int integerEndB = integerEndPos(b, p);
                int intA = Integer.parseInt(a.substring(p, integerEndA));
                int intB = Integer.parseInt(b.substring(p, integerEndB));
                if (intA != intB) {
                    return (intA < intB) ? -1 : 1;
                }
                if (integerEndA != integerEndB) {
                    return (integerEndA < integerEndB) ? -1 : 1;
                }
                p = integerEndA - 1;
            } else if (cha != chb) {
                return (cha > chb) ? 1: -1;
            }
        }
    }
}
