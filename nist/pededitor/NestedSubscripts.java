package gov.nist.pededitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NestedSubscripts {
    static Pattern subPattern = null;
    static Pattern unsubPattern = null;
    static HashMap<Character, Character> subMap = null;
    static HashMap<Character, Character> supMap = null;

    @SuppressWarnings("serial")
	private static void init() {
        if (subPattern != null) {
            return;
        }
        String sub = "<(sub|sup)[^>]*>";
        if (subPattern == null) {
            try {
                subPattern = Pattern.compile(sub);
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException("Pattern '" + sub
                                                + "' could not compile: " + e);
            }
        }

        String unsub = "</su[bp][^>]*>";
        if (unsubPattern == null) {
            try {
                unsubPattern = Pattern.compile(unsub);
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException("Pattern '" + unsub
                                                + "' could not compile: " + e);
            }
        }
        subMap = new HashMap<Character, Character>() {{
                put('0', '\u2080');
                put('1', '\u2081');
                put('2', '\u2082');
                put('3', '\u2083');
                put('4', '\u2084');
                put('5', '\u2085');
                put('6', '\u2086');
                put('7', '\u2087');
                put('8', '\u2088');
                put('9', '\u2089');
                put('+', '\u208a');
                put('-', '\u208b');
                put('(', '\u208d');
                put(')', '\u208e');
                put('x', '\u2093');
            }};
        supMap = new HashMap<Character, Character>() {{
                put('0', '\u2070');
                put('1', '\u00b9');
                put('2', '\u00b2');
                put('3', '\u00b3');
                put('4', '\u2074');
                put('5', '\u2075');
                put('6', '\u2076');
                put('7', '\u2077');
                put('8', '\u2078');
                put('9', '\u2079');
                put('+', '\u207a');
                put('-', '\u207b');
                put('(', '\u207d');
                put(')', '\u207e');
            }};
    }

    /** Convert all digits in nested sub/superscripts into their
        Unicode equivalents. Swing needs this because it can't
        understand nested sub/superscripts on its own. */
    public static String unicodify(CharSequence s0) {
        CharSequence s = s0;
        init();
        StringBuilder res = new StringBuilder();
        ArrayList<Boolean> subStack = new ArrayList<>();
        boolean warnTooDeep = true;
        boolean warnUnrenderable = true;

        while (s.length() > 0) {
            int nestingLevel = subStack.size();
            Matcher subMatcher = subPattern.matcher(s);
            if (subMatcher.lookingAt()) {
                if (nestingLevel == 0) {
                    res.append(subMatcher.group());
                }
                s = s.subSequence(subMatcher.end(), s.length());
                subStack.add("sub".equals(subMatcher.group(1)));
                continue;
            }
            Matcher unsubMatcher = unsubPattern.matcher(s);
            if (unsubMatcher.lookingAt()) {
                if (nestingLevel == 1) {
                    res.append(unsubMatcher.group());
                }
                if (nestingLevel == 0) {
                    System.err.println("Sub/superscript end tags outnumber start tags in "
                                       + s0);
                    return s0.toString();
                }
                subStack.remove(subStack.size() - 1);
                s = s.subSequence(unsubMatcher.end(), s.length());
                continue;
            }
            char ch = s.charAt(0);
            if (nestingLevel <= 1) {
                res.append(ch);
            } else if (nestingLevel >= 3) {
                if (warnTooDeep) {
                    System.err.println("Cannot render triple-nested sub/superscripts in " + s0);
                    warnTooDeep = false;
                }
                res.append(ch);
            } else {
                HashMap<Character, Character> map
                    = subStack.get(nestingLevel - 1) ? subMap : supMap;
                Character outch = map.get(ch);
                if (outch != null) {
                    res.append(outch);
                } else {
                    res.append(ch);
                    if (warnUnrenderable) {
                        System.err.println
                            ("Cannot render double-nested sub/superscript "
                             + "character '" + ch + "'");
                        warnUnrenderable = false;
                    }
                }
            }
            s = s.subSequence(1, s.length());
        }
        if (subStack.size() > 0) {
            System.err.println("Unbalanced start/end sub/superscript tags in "
                               + s0);
        }
        return res.toString();
    }
}
