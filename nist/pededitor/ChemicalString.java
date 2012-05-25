package gov.nist.pededitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** GUI for selecting a label string and an anchoring position for
    that label. */
public class ChemicalString {
    // final static String element = "[A-Z][a-z]*";
    final static String element =
        "H|He|Li|Be|B|C|N|O|F|Ne|Na|Mg|Al|Si|P|S|Cl|Ar|K|Ca|Sc|Ti|V|Cr|Mn|Fe|Co|Ni|Cu|Zn|Ga|Ge|As|Se|Br|Kr|Rb|Sr|Y|Zr|Nb|Mo|Tc|Ru|Rh|Pd|Ag|Cd|In|Sn|Sb|Te|I|Xe|Cs|Ba|La|Ce|Pr|Nd|Pm|Sm|Eu|Gd|Tb|Dy|Ho|Er|Tm|Yb|Lu|Hf|Ta|W|Re|Os|Ir|Pt|Au|Hg|Tl|Pb|Bi|Po|At|Rn|Fr|Ra|Ac|Th|Pa|U|Np|Pu|Am|Cm|Bk|Cf|Es|Fm|Md|No|Lr|Rf|Db|Sg|Bh|Hs|Mt|Ds|Rg|Cn|Uun|Uuu|Uub|Uuq|Uuh";
    final static String ion = "[\u207a\u207b]"; // superscript+ or superscript-
    final static String subscript = "\\d+(?:\\.\\d+)?";
    final static String subscriptNeeded = "((?:(?:" + element + ")|\\))" + ion + "*)"
        + "(" + subscript + ")";
    final static String subscriptNeededReplacement = "$1<sub>$2</sub>";
    static Pattern subscriptNeededPattern = null;

    final static String elementWithCount = "(" + element + ")" + ion + "*"
        + "(<sub>" + subscript + "</sub>)?" + ion + "*";
    final static String simpleCompound = "(?<![A-Za-z]>("+ elementWithCount + ")+";

    // An extended compound, with parenthesized subunits, cannot be
    // defined in terms of a basic regular expression, because the
    // parentheses have to match up.

    final static String extendedCompound
        = "(" + simpleCompound; // TODO

    public static String autoSubscript(String s) {
        if (subscriptNeededPattern == null) {
            try {
                subscriptNeededPattern = Pattern.compile(subscriptNeeded);
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException("Pattern '" + subscriptNeeded
                                                + "' could not compile: " + e);
            }
        }
        return subscriptNeededPattern.matcher(s).replaceAll(subscriptNeededReplacement);
    }

    String[] embeddedFormulas(String s) {
        // TODO
        return null;
    }

    public static void main(String[] args) {
        BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("Formula: ");
            String line;
            try {
                line = read.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                break;
            }
            System.out.println("Formatted = " + autoSubscript(line));
        }
    }
}
