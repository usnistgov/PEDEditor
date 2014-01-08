/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Information about a markup's delimiter and label. Example:
    new Delimiter("<sub>", "</sub>", "T<sub>sub</sub>"). */
public class Delimiter {
    protected String start;
    protected String end;
    protected String label;
    
    public Delimiter(String start, String end, String label) {
        this.start = start;
        this.end = end;
        this.label = label;
    }
    public String getStart() { return start; }
    public String getEnd() { return end; }
    public String getLabel() { return label; }
}
