package gov.nist.pededitor;

/** GUI for selecting a label string and an anchoring position for
    that label. */
public class Tabify {
    /* Convert every set of 8 consecutive spaces into tabs. */
    static String tabify(String s) {
        StringBuilder res = new StringBuilder();
        int spaceCnt = 0;
        System.out.println("Tabifying " + s);
        for (int i = 0, len = s.length(); i < len; ++i) {
        	char ch = s.charAt(i);
            if (ch == ' ') {
                if (++spaceCnt == 8) {
                    res.append('\t');
                    spaceCnt = 0;
                }
            } else {
                for (; spaceCnt > 0; --spaceCnt) {
                    res.append(' ');
                }
                res.append(ch);
            }
        }
        return res.toString();
    }
}
