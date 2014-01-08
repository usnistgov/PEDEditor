/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public class Tabify {
    /* Convert every set of 8 consecutive spaces that start a line into tabs. */
    static String tabify(String s) {
        StringBuilder res = new StringBuilder();
        int spaceCnt = 0;
        boolean lineStart = true;
        for (int i = 0, len = s.length(); i < len; ++i) {
        	char ch = s.charAt(i);
            if (lineStart && ch == ' ') {
                if (++spaceCnt == 8) {
                    res.append('\t');
                    spaceCnt = 0;
                }
            } else {
                for (; spaceCnt > 0; --spaceCnt) {
                    res.append(' ');
                }
                res.append(ch);
                lineStart = (ch == '\n');
            }
        }
        return res.toString();
    }
}
