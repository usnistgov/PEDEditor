/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class MoleWeightString {
    /** Replace variants of the word "mole" or "atomic" with the
        corresponding variant of "weight". */
    public static String moleToWeight(String str) {
        return str.replace("Mole", "Weight")
            .replaceAll("\\bmole\\b", "weight")
            .replace("Mol.", "Wt.")
            .replaceAll("Mol\\b", "Wt")
            .replace("Atomic", "Weight")
            .replaceAll("\\batomic\\b", "weight")
            .replace("At.", "Wt.")
            .replace("At %", "Wt %")
            .replace("At&nbsp;%", "Wt&nbsp;%");
    }

    /** Replace variants of the word "weight" with the corresponding
        variant of "mole". */
    public static String weightToMole(String str) {
        return str.replace("Weight", "Mole")
            .replace("weight", "mole")
            .replace("Wt.", "Mol.")
            .replaceAll("Wt\\b", "Mol");
    }

    /** Replace variants of the word "weight" with the corresponding
        variant of "atomic". */
    public static String weightToAtomic(String str) {
        return str.replace("Weight", "Atomic")
            .replace("weight", "atomic")
            .replace("Wt.", "At.")
            .replaceAll("Wt\\b", "At");
    }

    static Pattern hm1 = Pattern.compile("\\bmole\\b");
    static Pattern hm2 = Pattern.compile("\\bmol\\.?\\b");
    static Pattern hw1 = Pattern.compile("\\bweight\\b");
    static Pattern hw2 = Pattern.compile("\\bwt\\.?\\b");
    static Pattern ha1 = Pattern.compile
        ("\\batomic\\b", Pattern.CASE_INSENSITIVE);
    static Pattern ha2 = Pattern.compile("\\bAt\\.?\\b");

    /** Does str contains a common variation of the word "mole"? */
    public static boolean hasMole(String str) {
        str = str.toLowerCase();
        return hm1.matcher(str).find() || hm2.matcher(str).find();
    }

    /** Does str contains a common variation of the word "weight"? */
    public static boolean hasWeight(String str) {
        str = str.toLowerCase();
        return hw1.matcher(str).find() || hw2.matcher(str).find();
    }

    /** Does str contains a common variation of the word "atomic"? */
    public static boolean hasAtomic(String str) {
        return ha1.matcher(str).find() || str.contains("At %")
            || ha2.matcher(str).find();
    }

    public static void main(String[] args) {
        BufferedReader read
            = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("String: ");
            String line;
            try {
                line = read.readLine();
            } catch (IOException e) {
                break;
            }
            if (hasMole(line) || hasAtomic(line)) {
                System.out.println("M->W: " + moleToWeight(line));
            }
            if (hasWeight(line)) {
                System.out.println("W->M: " + weightToMole(line));
                System.out.println("W->A: " + weightToAtomic(line));
            }
            System.out.println("Done.");
        }
    }
}
