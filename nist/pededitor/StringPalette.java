package gov.nist.pededitor;

import java.util.ArrayList;
import java.util.Arrays;

/** Support sets of strings with associated labels. */
public class StringPalette {
    String[] strings;
    Object[] labels;

    /** Construct a StringPalette, using the given initializer array
        to generate the string/label pairs.

        Possible initializers include
 
        Object[] initializer = {"string1", "string2", ...}

        for a set of strings whose labels equal the strings themselves

        or

        Object[][] initializer = {{"string1", "label1"}, {"string2", "label2"}, ...}

        for a set of string/label pairs

        or

        {new Object[] {"string1", "label1"},
         new Object[] {"string2", "label2"},
         "string3", ... }

         for two string/label pairs plus a string "string3" whose
         label is also "string3" or

        {new Object[][] {{"string1", "label1"}, {"string2", "label2"}},
         "string3"}

        for another, potentially more concise way to combine
        string/label pairs with ordinary strings.
    */
    public StringPalette(Object[] initializer) {
        ArrayList<String> strs = new ArrayList<String>();
        ArrayList<Object> labs = new ArrayList<Object>();
        for (Object o: initializer) {
            if (o == null) {
                strs.add(null);
                labs.add(null);
            } else if (o instanceof Object[][]) {
                // An array of {"string", "label"} pairs
                Object[][] sss = (Object[][]) o;
                for (Object[] ss: sss) {
                    if (ss == null) {
                        strs.add(null);
                        labs.add(null);
                    } else if (ss.length != 2) {
                        throw new IllegalArgumentException
                            (Arrays.toString(ss)
                             + " does not match {\"string\", \"label\"} format");
                    } else {
                        strs.add((String) ss[0]);
                        labs.add(ss[1]);
                    }
                }
            } else if (o instanceof Object[]) {
                // {"string", "label"} pair
                Object[] ss = (Object[]) o;
                if (ss == null) {
                    strs.add(null);
                    labs.add(null);
                } else if (ss.length != 2) {
                    throw new IllegalArgumentException
                        (Arrays.toString(ss)
                         + " does not match {\"string\", \"label\"} format");
                } else {
                    strs.add((String) ss[0]);
                    labs.add(ss[1]);
                }
            } else if (o instanceof String) {
                // Output string does double duty as a label.
                String s = (String) o;
                strs.add(s);
                labs.add(s);
            } else {
                throw new IllegalArgumentException
                    (o + " does not match \"string\", "
                     + "{\"string\", \"label\"}, or"
                     + "{{\"string\", \"label\"}, ... } format");
            }
        }

        strings = strs.toArray(new String[0]);
        labels = labs.toArray(new Object[0]);
    }

    /** Return the number of strings in this palette. */
    public int size() {
        return strings.length;
    }

    /** Return the literal string contained at position #index. */
    public String get(int index) {
        return strings[index];
    }

    /** Return a String or Icon that represents get(index). */
    public Object getLabel(int index) {
        return labels[index];
    }
}