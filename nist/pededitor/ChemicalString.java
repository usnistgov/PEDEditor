package gov.nist.pededitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** GUI for selecting a label string and an anchoring position for
    that label. */
public class ChemicalString {
    final static String[] elements =
    { null, "H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds", "Rg", "Cn", "Uut", "Uuq", "Uup", "Uuh", "Uus", "Uuo" };

    static Map<String,Integer> symbolToNumberMap = new HashMap<>();

    static {
        for (int i = 1; i < elements.length; ++i) {
            String s = elements[i];
            if (s != null) {
                symbolToNumberMap.put(s, i);
            }
        }
        // Temporary symbols names that may still be in use
        symbolToNumberMap.put("Uun", 110);
        symbolToNumberMap.put("Uuu", 111);
        symbolToNumberMap.put("Uub", 112);
    }

    /** These standard atomic weight values come from the NIST "Atomic
        Weights and Isotopic Composition" table (retrieved 30 May
        2012). */
    final static double[] weights =
    { 0, 1.007947, 4.0026022, 6.9412, 9.0121823, 10.8117, 12.01078, 14.00672, 
      15.99943, 18.99840325, 20.17976, 22.989769282, 24.30506, 26.98153868, 28.08553, 30.9737622, 
      32.0655, 35.4532, 39.9481, 39.09831, 40.0784, 44.9559126, 47.8671, 50.94151, 
      51.99616, 54.9380455, 55.8452, 58.9331955, 58.69344, 63.5463, 65.382, 69.7231, 
      72.641, 74.921602, 78.963, 79.9041, 83.7982, 85.46783, 87.621, 88.905852, 
      91.2242, 92.906382, 95.962, 0, 101.072, 102.905502, 106.421, 107.86822, 
      112.4118, 114.8183, 118.7107, 121.7601, 127.603, 126.904473, 131.2936, 132.90545192, 
      137.3277, 138.905477, 140.1161, 140.907652, 144.2423, 0, 150.362, 151.9641, 
      157.253, 158.925352, 162.5001, 164.930322, 167.2593, 168.934212, 173.0545, 174.96681, 
      178.492, 180.947882, 183.841, 186.2071, 190.233, 192.2173, 195.0849, 196.9665694, 
      200.592, 204.38332, 207.21, 208.980401, 0, 0, 0, 0, 
      0, 0, 232.038062, 231.035882, 238.028913 };

    final static String element =
        "(?:(?:H|He|Li|Be|B|C|N|O|F|Ne|Na|Mg|Al|Si|P|S|Cl|Ar|K|Ca|Sc|Ti|V|Cr|Mn|Fe|Co|Ni|Cu|Zn|Ga|Ge|As|Se|Br|Kr|Rb|Sr|Y|Zr|Nb|Mo|Tc|Ru|Rh|Pd|Ag|Cd|In|Sn|Sb|Te|I|Xe|Cs|Ba|La|Ce|Pr|Nd|Pm|Sm|Eu|Gd|Tb|Dy|Ho|Er|Tm|Yb|Lu|Hf|Ta|W|Re|Os|Ir|Pt|Au|Hg|Tl|Pb|Bi|Po|At|Rn|Fr|Ra|Ac|Th|Pa|U|Np|Pu|Am|Cm|Bk|Cf|Es|Fm|Md|No|Lr|Rf|Db|Sg|Bh|Hs|Mt|Ds|Rg|Cn|Uun|Uuu|Uub|Uuq|Uuh)"
        + "(?![a-z]))";

    final static String ion = "[\u207a\u207b]"; // superscript+ or superscript-
    final static String subscript = "\\d+(?:\\.\\d+)?";
    final static String elementCount =
        "(" + element + ")" + ion + "*(" + subscript + ")?" + ion + "*"
        + "(?![a-z])";

    final static String subscriptNeeded = "((?:" + element + "|\\))" + ion + "*)"
        + "(" + subscript + ")";
    final static String subscriptNeededReplacement = "$1<sub>$2</sub>";
    static Pattern subscriptNeededPattern = null;
    static Pattern subscriptPattern = null;
    static Pattern elementCountPattern = null;

    // The following patterns need work.
    final static String elementWithCount = "(" + element + ")" + ion + "*"
        + "(<sub>" + subscript + "</sub>)?" + ion + "*";
    final static String simpleCompound = "(?<![A-Za-z]>("+ elementWithCount + ")+";

    /** Return the element number corresponding to the given element
        symbol, or 0 if the symbol is not defined. */
    public static int symbolToNumber(String symbol) {
        Integer i = symbolToNumberMap.get(symbol);
        return (i == null) ? 0 : i;
    }

    /** Return the standard atomic weight for the given element
        number, or 0 if no standard weight is defined for that
        element. */
    public static double elementWeight(int i) {
        return (i > weights.length) ? 0 : weights[i];
    }

    /** Return the standard atomic weight for the given element
        symbol, or 0 if no standard weight is defined for that
        element. */
    public static double elementWeight(String symbol) {
        return elementWeight(symbolToNumber(symbol));
    }

    public static Pattern getSubscriptPattern() {
        if (subscriptPattern == null) {
            try {
                subscriptPattern = Pattern.compile(subscript);
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException("Pattern '" + subscript
                                                + "' could not compile: " + e);
            }
        }
        return subscriptPattern;
    }

    public static Pattern getElementCountPattern() {
        if (elementCountPattern == null) {
            try {
                elementCountPattern = Pattern.compile(elementCount);
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException("Pattern '" + elementCount
                                                + "' could not compile: " + e);
            }
        }
        return elementCountPattern;
    }

    /** Sort the elements into Hill order. Hill order is alphabetical
        unless carbon is present; then the rule is carbon, then
        hydrogen (if present), then alphabetical order for the
        rest. */
    public static void hillSort(String[] elements) {
        int cpos = -1;
        int hpos = -1;
        for (int i = 0; i < elements.length; ++i) {
            String s = elements[i];
            if ("C".equals(s)) {
                cpos = i;
            }
            if ("H".equals(s)) {
                hpos = i;
            }
        }

        int sortStart;
        if (cpos >= 0) {
            String tmp = elements[0];
            elements[0] = elements[cpos];
            elements[cpos] = tmp;

            if (hpos >= 0) {
                if (hpos == 0) {
                    hpos = cpos;
                }
                tmp = elements[1];
                elements[1] = elements[hpos];
                elements[hpos] = tmp;
                sortStart = 2;
            } else {
                sortStart = 1;
            }
        } else {
            sortStart = 0;
        }

        Arrays.sort(elements, sortStart, elements.length, null);
    }

    /* Add HTML subscript markers to numbers that appear to be
       subscripts of either elements or elemental groupings. (The
       elemental grouping detection is a bit dodgy and may return
       false positives.) */
    public static String autoSubscript(String s) {
        if (subscriptNeededPattern == null) {
            try {
                subscriptNeededPattern = Pattern.compile(subscriptNeeded);
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException("Pattern '" + subscriptNeeded
                                                + "' could not compile: " + e);
            }
        }
        return subscriptNeededPattern.matcher(s)
            .replaceAll(subscriptNeededReplacement);
    }

    static class Match {
        int length;
        Map<String,Double> composition;
    }

    /** Read the longest leading sequence of s that can be interpreted
        as a chemical name, and return a Match indicating the number
        of elements of each type. Return null if no leading sequence
        resembles a compound name. Note that spaces are not
        ignored. */
    public static Match composition(CharSequence s) {
        Match res = new Match();
        HashMap<String,Double> compo = new HashMap<>();
        res.composition = compo;

        while (s.length() > 0) {
            char ch = s.charAt(0);

            if (ch == '(') {
                s = s.subSequence(1, s.length());
                Match submatch = composition(s);
                if (submatch == null) {
                    return null;
                }
                s = s.subSequence(submatch.length, s.length());
                if (s.length() == 0 || s.charAt(0) != ')') {
                    return (res.length > 0) ? res : null;
                }
                s = s.subSequence(1, s.length());
                res.length += 2 + submatch.length;
                // Successfully matched a parenthesized subcompound.

                // Now try to match a trailing subscript (as in
                // "(SO2)3")
                double count = 1.0;
                Matcher subscriptMatcher = getSubscriptPattern().matcher(s);
                if (subscriptMatcher.lookingAt()) {
                    // Successfully matched a trailing subscript
                    String substr = subscriptMatcher.group();
                    try {
                        count = Double.parseDouble(substr);
                    } catch (Exception e) {
                        throw new IllegalStateException
                            ("'" + substr + "' does not parse as a double!");
                    }
                    s = s.subSequence(subscriptMatcher.end(), s.length());
                    res.length += subscriptMatcher.end();
                }

                // Merge this sub-compound with the existing
                // composition.
                for (Map.Entry<String, Double> pair:
                         submatch.composition.entrySet()) {
                    String key = pair.getKey();
                    Double d = compo.get(key);
                    if (d == null) {
                        d = (double) 0;
                    }
                    double newCount = d + pair.getValue() * count;
                    if (newCount == 0) {
                        continue;
                    }
                    compo.put(key, newCount);
                }
                continue;
            }

            Matcher elementCountMatcher = getElementCountPattern().matcher(s);
            if (elementCountMatcher.lookingAt()) {
                // Successfully matched an element and possible count,
                // such as "Si" or "Si2".
                int charCount = elementCountMatcher.end();
                res.length += charCount;
                s = s.subSequence(charCount, s.length());
                String element = elementCountMatcher.group(1);
                String countStr = elementCountMatcher.group(2);
                double count = 1;
                if (countStr != null) {
                    try {
                        count = Double.parseDouble(countStr);
                    } catch (Exception e) {
                        throw new IllegalStateException
                            ("'" + countStr + "' does not parse as a double!");
                    }
                }

                Double d = compo.get(element);
                if (d == null) {
                    d = 0.0;
                }
                double newCount = d + count;
                if (newCount > 0) {
                    compo.put(element, d + count);
                }
                continue;
            }

            break;
        }

        return (res.length > 0) ? res : null;
    }

    public static void main(String[] args) {
        BufferedReader read
            = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("Formula: ");
            String line;
            try {
                line = read.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                break;
            }
            System.out.println("Auto-subscripted: " + autoSubscript(line));
            Match match = composition(line);
            if (match == null) {
                System.out.println("No leading compound found");
            } else {
                Map<String,Double> c = match.composition;
                int s = c.size();
                String[] elements = new String[s];
                int i = 0;
                for (String element: c.keySet()) {
                    elements[i++] = element;
                }
                Arrays.sort(elements);

                System.out.println("Composition: ");
                for (String element: elements) {
                    System.out.print(element + c.get(element));
                }
                System.out.println();

                System.out.println("Remainder = " + line.substring(match.length));
            }
        }
    }
}
