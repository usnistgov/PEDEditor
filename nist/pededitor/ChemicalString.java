/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Utility class for manipulation of chemical compounds, particularly
    in string form. */
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
        Weights and Isotopic Composition" table at
        http://www.nist.gov/pml/data/comp.cfm (retrieved 12 May 2013
        and parsed with the script element_weights.pl. For elements
        with no official atomic weight, the number of the most stable
        isotope is used.
    */
    final static double[] weights =
    { 0, 1.00794, 4.002602, 6.941, 9.012182, 10.811, 12.0107, 14.0067, 
      15.9994, 18.9984032, 20.1797, 22.98976928, 24.3050, 26.9815386, 28.0855, 30.973762, 
      32.065, 35.453, 39.948, 39.0983, 40.078, 44.955912, 47.867, 50.9415, 
      51.9961, 54.938045, 55.845, 58.933195, 58.6934, 63.546, 65.38, 69.723, 
      72.64, 74.92160, 78.96, 79.904, 83.798, 85.4678, 87.62, 88.90585, 
      91.224, 92.90638, 95.96, 98, 101.07, 102.90550, 106.42, 107.8682, 
      112.411, 114.818, 118.710, 121.760, 127.60, 126.90447, 131.293, 132.9054519, 
      137.327, 138.90547, 140.116, 140.90765, 144.242, 145, 150.36, 151.964, 
      157.25, 158.92535, 162.500, 164.93032, 167.259, 168.93421, 173.054, 174.9668, 
      178.49, 180.94788, 183.84, 186.207, 190.23, 192.217, 195.084, 196.966569, 
      200.59, 204.3833, 207.2, 208.98040, 209, 210, 222, 223, 
      226, 227, 232.03806, 231.03588, 238.02891, 237, 244, 243, 
      247, 247, 251, 252, 257, 258, 259, 262, 
      265, 268, 271, 272, 270, 276, 281, 280, 
      285, 284, 289, 288, 293, 292, 294 };

    final static String element =
        "(?:(?:H|He|Li|Be|B|C|N|O|F|Ne|Na|Mg|Al|Si|P|S|Cl|Ar|K|Ca|Sc|Ti|V|Cr|Mn|Fe|Co|Ni|Cu|Zn|Ga|Ge|As|Se|Br|Kr|Rb|Sr|Y|Zr|Nb|Mo|Tc|Ru|Rh|Pd|Ag|Cd|In|Sn|Sb|Te|I|Xe|Cs|Ba|La|Ce|Pr|Nd|Pm|Sm|Eu|Gd|Tb|Dy|Ho|Er|Tm|Yb|Lu|Hf|Ta|W|Re|Os|Ir|Pt|Au|Hg|Tl|Pb|Bi|Po|At|Rn|Fr|Ra|Ac|Th|Pa|U|Np|Pu|Am|Cm|Bk|Cf|Es|Fm|Md|No|Lr|Rf|Db|Sg|Bh|Hs|Mt|Ds|Uun|Rg|Uuu|Cn|Uub|Uut|Fl|Uuq|Uup|Lv|Uuh|Uus|Uuo)"
        + "(?![a-z]))";

    final static String ion = "[\u207a\u207b]"; // superscript+ or superscript-
    final static String subscript = "(?:\\.\\d+|\\d+\\.\\d*|\\d+/\\d+|\\d+)%?";
    final static String elementCount =
        "(" + element + ")" + ion + "*(" + subscript + ")?" + ion + "*"
        + "(?![a-z])";

    final static String subscriptNeeded = "((?:" + element + "|[])])" + ion + "*)"
        + "(" + subscript + ")";
    final static String subscriptNeededReplacement = "$1<sub>$2</sub>";
    static Pattern subscriptNeededPattern = null;
    static Pattern subscriptPattern = null;
    static Pattern elementCountPattern = null;
    final static String quoted = "\"\\s*([^\"]+)\"";
    static Pattern quotedPattern = null;

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
        number, or Double.NaN if no standard weight is defined for that
        element. */
    public static double elementWeight(int i) {
        double v;
        return (i >= weights.length || ((v = weights[i]) == 0))
            ? Double.NaN
            : v;
    }

    /** Return the standard atomic weight for the given element
        symbol, or Double.NaN if no standard weight is defined for
        that element. */
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
       elemental grouping detection is dodgy -- it doesn't actually
       match up the parentheses -- and may return false positives.) */
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

    public static class Match {
        /** Inclusive index of beginning of match into input
            CharSequence -- like String#substring(). */
        int beginIndex;
        /** Exclusive index of end of match into input CharSquence --
            like String#substring(). */
        int endIndex;
        boolean mIsWholeStringMatch;
        Map<String,Double> composition;

        public String within(String s) {
            return s.substring(beginIndex, endIndex);
        }

        public boolean isWholeStringMatch() {
            return mIsWholeStringMatch;
        }

        public void setWholeStringMatch(boolean b) {
            mIsWholeStringMatch = b;
        }

        /** Merge o into this. */
        public void merge(Match o) {
            merge(o, 1.0);
        }

        public boolean isEmpy() {
            return beginIndex == endIndex;
        }

        /** Merge o times count into this. */
        public void merge(Match o, double count) {
            for (Map.Entry<String, Double> pair:
                     o.composition.entrySet()) {
                String key = pair.getKey();
                Double d = composition.get(key);
                if (d == null) {
                    d = (double) 0;
                }
                double newCount = d + pair.getValue() * count;
                if (newCount == 0) {
                    continue;
                }
                composition.put(key, newCount);
            }
        }

        /** Return this Match expressed as a Hill order chemical
            formula. */
        @Override public String toString() {
            return hillSystemFormula(composition);
        }

        /** Return this Match expressed as a Hill order chemical
            formula. */
        public double getWeight() {
            return weight(composition);
        }
    }

    /** Return the Hill system chemical formula. */
    public static String hillSystemFormula(Map<String,Double> composition) {
        String[] elements = composition.keySet().toArray(new String[0]);
        hillSort(elements);
        StringBuilder res = new StringBuilder();
        for (String element: elements) {
            res.append(element);
            double quantity = composition.get(element);
            if (Math.abs(quantity-1) > 1e-10) {
                res.append(ContinuedFraction.toString(quantity, false));
            }
        }
        return res.toString();
    }

    /** Return the molecular weight, or Double.NaN if that is not
        defined (either because of nonexistent symbols or because one
        or more elements has undefined standard weight. */
    public static double weight(Map<String,Double> composition) {
        double tot = 0;
        for (Map.Entry<String,Double> entry: composition.entrySet()) {
            String element = entry.getKey();
            double quantity = entry.getValue();
            tot += elementWeight(element) * quantity;
        }
        return tot;
    }

    /** Return an array of all maximal-length chemical formulas that
        appear in the given string. Chemical formulas must start and
        end at word boundaries, except that leading non-English
        letters are ignored. This is because of text such as
        "&alpha;Fe". */
    public static Match[] embeddedFormulas(CharSequence s) {
        ArrayList<Match> res = new ArrayList<>();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch) && ch <= 'z') {
                Match m = composition(s.subSequence(i, s.length()));
                if (m == null) {
                    for (; i < s.length()
                             && Character.isLetterOrDigit(s.charAt(i));
                         ++i) {
                    }
                    continue;
                } else {
                    m.beginIndex += i;
                    m.endIndex += i;
                    res.add(m);
                    i = m.endIndex;
                }
            }
        }
        return res.toArray(new Match[0]);
    }

    /** Like composition(), but also accept formulas enclosed in
       double-quotation marks to indicate that the oxidation state is
       unknown. beginIndex in the return value (if the return value is
       non-null) indicates the start of the actual formula,
       discounting the quotation marks. beginIndex will be nonzero if
       and only if the formula was quoted. */
    public static Match maybeQuotedComposition(CharSequence s) {
        if (s.length() > 0 && s.charAt(0) == '\"') {
            // Grab the quoted portion of this pattern.
            if (quotedPattern == null) {
                try {
                    quotedPattern = Pattern.compile(quoted);
                } catch (PatternSyntaxException e) {
                    throw new IllegalStateException("Pattern '" + quoted
                                                    + "' could not compile: " + e);
                }
            }
            Matcher quotedMatcher = quotedPattern.matcher(s);
            if (!quotedMatcher.lookingAt()) {
                return null;
            }

            int start = quotedMatcher.start(1);
            CharSequence oldS = s;
            s = s.subSequence(start, quotedMatcher.end(1));
            
            Match c = composition(s);
            if (c != null) {
                c.beginIndex += start;
                c.endIndex += start;
                if (c.isWholeStringMatch()) {
                    String str = oldS.subSequence(quotedMatcher.end(),
                                                  oldS.length()).toString();
                    c.setWholeStringMatch(str.trim().isEmpty());
                }
            }
            return c;
        } else {
            return composition(s);
        }
    }

    /** I need a Scanner variant that keeps track of the number of
        characters scanned and that doesn't rely on a fixed
        delimiter. */
    static class SimpleScanner implements Cloneable {
        final static String whitespace = "\\s*";
        static Pattern whitespacePattern = null;

        CharSequence s;
        int pos;

        SimpleScanner(CharSequence s) {
            this.s = s;
            this.pos = 0;
        }

        SimpleScanner(CharSequence s, int pos) {
            this.s = s;
            this.pos = pos;
        }

        @Override public SimpleScanner clone() {
            return new SimpleScanner(s, pos);
        }

        boolean hasNext() {
            return s.length() > 0;
        }

        boolean skip(Pattern p) {
            Matcher m = p.matcher(s);
            if (m.lookingAt()) {
                skip(m.end());
                return true;
            }
            return false;
        }

        void skip(int charCnt) {
            pos += charCnt;
            s = s.subSequence(charCnt, s.length());
        }

        boolean skip(char ch) {
            if (s.charAt(0) == ch) {
                skip(1);
                return true;
            }
            return false;
        }

        boolean skipWhitespace() {
            if (whitespacePattern == null) {
                whitespacePattern = Pattern.compile(whitespace);
            }
            return skip(whitespacePattern);
        }

        int getPosition() {
            return pos;
        }

        CharSequence getSequence() {
            return s;
        }

        String next(Pattern p) {
            Matcher m = p.matcher(s);
            if (m.lookingAt()) {
                String res = getSequence().subSequence(0, m.end()).toString();
                skip(m.end());
                return res;
            }
            return null;
        }
    }

    /** Read the longest leading sequence of s that can be interpreted
        as a chemical formula, and return a Match indicating the
        number of elements of each type. Return null if no leading
        sequence resembles a compound name. Note that spaces are not
        ignored. The beginIndex field will always equal 0 for this
        call.

        <composition> = ((<spaces>|)(<number>|)(<spaces>|)
          <simple_composition>(<spaces>|)[+:,[[:space:]]])*
          ((<spaces>|)(<number>|)(<spaces>|)
          <simple_composition>(<spaces>|)

        Detail: if the comma or no-delimiter approaches are used, then
        a number must start the next expression.

        So composition() understands expressions like

          5 (KF)2OH + 2 HCl

        Otherwise this behaves like simpleComposition. */
    public static Match composition(CharSequence s) {
        Match res = new Match();
        res.composition = new HashMap<String,Double>();

        SimpleScanner oldScan = new SimpleScanner(s);
        Pattern numberPattern = getSubscriptPattern(); 

        for (;;) {
            if (!oldScan.hasNext()) {
                res.setWholeStringMatch(true);
                break;
            }
            SimpleScanner scan = oldScan.clone();
            boolean numberNeeded = false;
            if (scan.getPosition() > 0) {
                // This isn't the first chemical formula, so a join
                // character is allowed, but not required.
                if (!(scan.skip('+') || scan.skip(':'))) {
                    numberNeeded = true;
                    scan.skip(',');
                }
            }

            scan.skipWhitespace();
            String num = scan.next(numberPattern);
            double count = 1.0;
            if (num != null) {
                try {
                    count = ContinuedFraction.parseDouble(num);
                } catch (NumberFormatException e) {
                    break;
                }
                scan.skipWhitespace();
            } else if (numberNeeded) {
                break;
            }
            
            Match simple = simpleComposition(scan.getSequence());
            if (simple == null) {
                break;
            }
            scan.skip(simple.endIndex);
            res.merge(simple, count);
            scan.skipWhitespace();
            oldScan = scan;
        }

        res.endIndex = oldScan.getPosition();
        return (res.endIndex > 0) ? res : null;
    }

    /** Sub-function of composition() for parsing the element names,
        parentheses, and subscripts in chemical formulas. So
        "Ca(NO3)2.5" would be understood, but anything more
        complicated would not be. */
    static Match simpleComposition(CharSequence s) {
        Match res = new Match();
        HashMap<String,Double> compo = new HashMap<>();
        res.composition = compo;

        String leftDelimiters = "([";
        String rightDelimiters = ")]";

        for (;;) {
            if (s.length() == 0) {
                res.setWholeStringMatch(true);
                break;
            }
            char ch = s.charAt(0);
            int delimiterNo = leftDelimiters.indexOf((int) ch);

            if (delimiterNo >= 0) {
                char rightDelimiter = rightDelimiters.charAt(delimiterNo);
                s = s.subSequence(1, s.length());
                Match submatch = composition(s);
                if (submatch == null) {
                    break;
                }
                s = s.subSequence(submatch.endIndex, s.length());
                if (s.length() == 0 || s.charAt(0) != rightDelimiter) {
                    break;
                }
                s = s.subSequence(1, s.length());
                res.endIndex += 2 + submatch.endIndex;
                // Successfully matched a parenthesized subcompound.

                // Now try to match a trailing subscript (as in
                // "(SO2)3")
                double count = 1.0;
                Matcher subscriptMatcher = getSubscriptPattern().matcher(s);
                if (subscriptMatcher.lookingAt()) {
                    // Successfully matched a trailing subscript
                    String substr = subscriptMatcher.group();
                    try {
                        count = ContinuedFraction.parseDouble(substr);
                    } catch (NumberFormatException e) {
                        break;
                    }
                    s = s.subSequence(subscriptMatcher.end(), s.length());
                    res.endIndex += subscriptMatcher.end();
                }

                // Merge this sub-compound with the existing
                // composition.
                res.merge(submatch, count);
                continue;
            }

            Matcher elementCountMatcher = getElementCountPattern().matcher(s);
            if (elementCountMatcher.lookingAt()) {
                // Successfully matched an element and possible count,
                // such as "Si" or "Si2".
                int charCount = elementCountMatcher.end();
                res.endIndex += charCount;
                s = s.subSequence(charCount, s.length());
                String element = elementCountMatcher.group(1);
                String countStr = elementCountMatcher.group(2);
                double count = 1;
                if (countStr != null) {
                    try {
                        count = ContinuedFraction.parseDouble(countStr);
                    } catch (NumberFormatException e) {
                        break;
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

            if (Character.isLetter(ch)) {
                // If the character immediately after a matching
                // portion is a letter or digit, then it invalidates
                // the whole match. For instance, it would be wrong to
                // extract the chemical formula "BO" from the string
                // "BORON".
                return null;
            }

            // Return the portion of the input that matched.
            break;
        }

        return (res.endIndex > 0) ? res : null;
    }

    public static void main(String[] args) {
        BufferedReader read
            = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("Formula: ");
            String line;
            try {
                line = read.readLine();
            } catch (IOException e) {
                break;
            }
            System.out.println("Auto-subscripted: " + autoSubscript(line));
            Match match = maybeQuotedComposition(line);
            if (match == null) {
                System.out.println("No leading compound found");
            } else {
                System.out.println((match.isWholeStringMatch() ? "Full" : "Partial") + " match found.");
                System.out.println("Hill Order: " + match);
                System.out.println("Weight: " + match.getWeight());
                System.out.println("Trimmed = " + match.within(line));
            }
        }
    }
}
