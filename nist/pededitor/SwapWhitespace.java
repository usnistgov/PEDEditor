/* Eric Boesch, NIST Materials Measurement Laboratory, 2016. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Move vertical padding from the end to the front and vice versa.
    This is helpful after performing a reflection on a label that
    changes the anchoring so the padding is needed below instead of on
    top or vice versa. */
public class SwapWhitespace {
    static Pattern whitespacePattern = null;

    private static void init() {
        if (whitespacePattern != null) {
            return;
        }

        // Optional one or more <br>s at the front, then the body,
        // then optional one or more <br>s at the end. All lead <br>s
        // have an effect but only the second and subsequent <br>s at
        // the end have an effect.
        String whites = "\\A((?:<br>\\n?)+)?(.*?)(?:<br>\n?((?:<br>\n?)*))?\\z";
        
        try {
            whitespacePattern = Pattern.compile(whites);
        } catch (PatternSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String swap(CharSequence s) {
        init();

        Matcher matcher = whitespacePattern.matcher(s);
        if (!matcher.matches()) {
            throw new IllegalStateException("Failed to match in '" + s + "')");
        }
        
        StringBuilder res = new StringBuilder();
        String beforeWhitespace = matcher.group(1);
        String body = matcher.group(2);
        String afterWhitespace = matcher.group(3);
        if (afterWhitespace != null) {
            res.append(afterWhitespace);
        }
        res.append(body);
        if (beforeWhitespace != null) {
            res.append("<br>\n"); // First BR has no effect
            res.append(beforeWhitespace);
        }
        return res.toString();
    }
}
