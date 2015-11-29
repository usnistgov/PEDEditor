/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/** Class to hold definitions of a text or HTML string anchored to a
    location in space and possibly drawn at an angle. */
@JsonSerialize(include = Inclusion.NON_DEFAULT)
public class AnchoredLabel extends TransformedShape {

    /** Additional offset to apply to this anchor, expressed in
        baseline coordinates and normalized to match scale = 1. */
    double baselineXOffset = 0;
    double baselineYOffset = 0;

    /** The actual string to display. (This may be HTML or something
        else as opposed to plain text.) */
    String text;

    boolean boxed = false;
    boolean opaque = false;
    boolean cutout = false;
    boolean autoWidth = false;

    public AnchoredLabel() {
    }

    public AnchoredLabel
        (@JsonProperty("text") String text,
         @JsonProperty("xWeight") double xWeight,
         @JsonProperty("yWeight") double yWeight) {
        super(xWeight, yWeight);
        this.text = text;
    }

    @Override public AnchoredLabel clone() {
        AnchoredLabel output = new AnchoredLabel(text, xWeight, yWeight);
        output.setX(getX());
        output.setY(getY());
        output.setAngle(getAngle());
        output.setScale(getScale());
        output.setBoxed(isBoxed());
        output.setOpaque(isOpaque());
        output.setCutout(isCutout());
        output.setAutoWidth(isAutoWidth());
        output.setBaselineXOffset(getBaselineXOffset());
        output.setBaselineYOffset(getBaselineYOffset());
        output.setColor(getColor());
        return output;
    }

    public void setText(String text) { this.text = text; }
    /** If true, draw a box around the label. */
    public void setAutoWidth(boolean v) { autoWidth = v; }
    public void setBoxed(boolean boxed) { this.boxed = boxed; }
    /** If true, erase the label's background before drawing. */
    public void setOpaque(boolean opaque) { this.opaque = opaque; }
    public void setCutout(boolean v) { cutout = v; }
    public void setBaselineXOffset(double v) { baselineXOffset = v; }
    public void setBaselineYOffset(double v) { baselineYOffset = v; }

    public double getBaselineXOffset() { return baselineXOffset; }
    public double getBaselineYOffset() { return baselineYOffset; }
    public String getText() { return text; }
    /** If true, draw a box around the label. */
    public boolean isAutoWidth() { return autoWidth; }
    /** If true, draw a box around the label. */
    public boolean isBoxed() { return boxed; }
    /** If true, erase the label's background before drawing. */
    public boolean isOpaque() { return opaque; }
    public boolean isCutout() { return cutout; }

    @Override public String toString() {
        return "'" + text + "' x: " + x  + " y: " + y + " wx: " + xWeight
            + " wy: " + yWeight + " angle: " + angle + " scale: " + scale
            + " boxed: " + boxed + " opaque: " + opaque;
    }

    // OBSOLESCENT
    @JsonProperty("fontSize") void setFontSize(double fontSize) {
        setScale(fontSize);
    }
}
