/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.ArrayList;

/** Support markup start and end markers (such as <sub> and </sub>)
    with associated labels. */
public class DelimiterPalette {
    Delimiter[] items;

    /** Construct a DelimiterPalette, using the given initializer array
        to generate the string/label pairs.

        Initializer format is

        String[][] initializer = {{"label", "start", "end"}, ...}

        In place of a string triple, nulls may be inserted as
        end-of-row markers.
    */
    public DelimiterPalette(String[][] initializer) {
        ArrayList<Delimiter> its = new ArrayList<>();
        for (String[] triple: initializer) {
            its.add((triple == null) ? null
                    : new Delimiter(triple[0], triple[1], triple[2]));
        }
        items = its.toArray(new Delimiter[0]);
    }

    /** Return the number of items, including nulls. */
    public int size() {
        return items.length;
    }

    public Delimiter get(int index) {
        return items[index];
    }

    public String getStart(int index) {
        return items[index].start;
    }

    public String getEnd(int index) {
        return items[index].end;
    }

    public String getLabel(int index) {
        return items[index].label;
    }
}
