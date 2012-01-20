package gov.nist.pededitor;

import org.codehaus.jackson.annotate.JsonProperty;

/** Class to hold definitions of a text or HTML string anchored to a
    location in space and possibly drawn at an angle. */
public class AnchoredLabel {

    /** x position of the anchor point */
    double x;
    /** y position of the anchor point */
    double y;

    /** Text positioning relative to the anchor point. 0.0 = The
        anchor point lies along the left edge of the text block in
        baseline coordinates (if the text is rotated, then this edge
        may not be on the left in physical coordinates; for example,
        if the text is rotated by an angle of PI/2, then this will be
        the top edge in physical coordinates); 0.5 = the anchor point
        lies along the vertical line (in baseline coordinates) that
        bisects the text block; 1.0 = the anchor point lies along the
        right edge (in baseline coordinates) of the text block */
    double xWeight;

    /** Text positioning relative to the anchor point. 0.0 = The
        anchor point lies along the top edge of the text block in
        baseline coordinates (if the text is rotated, then this edge
        may not be on top in physical coordinates; for example, if the
        text is rotated by an angle of PI/2, then this will be the
        right edge in physical coordinates); 0.5 = the anchor point
        lies along the horizontal line (in baseline coordinates) that
        bisects the text block; 1.0 = the anchor point lies along the
        bottom edge (in baseline coordinates) of the text block */
    double yWeight;

    /** The actual string to display. (This may be HTML or something
        else as opposed to plain text.) */
    String text;

    /** Printing angle in clockwise radians. 0 represents
        left-to-right text; Math.PI/2 represents text where lines
        extend downwards. */
    double angle = 0.0;

    /** A multiple of a standard font size (1.0 = normal), where the
        standard font size is defined by the application. */
    double fontSize;

    boolean boxed = false;
    boolean opaque = false;

    public AnchoredLabel
        (@JsonProperty("string") String text,
         @JsonProperty("xWeight") double xWeight,
         @JsonProperty("yWeight") double yWeight) {
        this.text = text;
        this.xWeight = xWeight;
        this.yWeight = yWeight;
    }

    @Override
    public AnchoredLabel clone() {
        AnchoredLabel output = new AnchoredLabel(text, xWeight, yWeight);
        output.setX(getX());
        output.setY(getY());
        output.setAngle(getAngle());
        output.setFontSize(getFontSize());
        output.setBoxed(isBoxed());
        output.setOpaque(isOpaque());
        return output;
    }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setXWeight(double xWeight) { this.xWeight = xWeight; }
    public void setYWeight(double yWeight) { this.yWeight = yWeight; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setFontSize(double fontSize) { this.fontSize = fontSize; }
    public void setText(String text) { this.text = text; }
    /** If true, draw a box around the label. */
    public void setBoxed(boolean boxed) { this.boxed = boxed; }
    /** If true, erase the label's background before drawing. */
    public void setOpaque(boolean opaque) { this.opaque = opaque; }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getText() { return text; }
    public double getAngle() { return angle; }
    public double getFontSize() { return fontSize; }
    @JsonProperty("xWeight") public double getXWeight() { return xWeight; }
    @JsonProperty("yWeight") public double getYWeight() { return yWeight; }
    /** If true, draw a box around the label. */
    public boolean isBoxed() { return boxed; }
    /** If true, erase the label's background before drawing. */
    public boolean isOpaque() { return opaque; }

    public String toString() {
        return "'" + text + "' x: " + x  + " y: " + y + " wx: " + xWeight
            + " wy: " + yWeight + " angle: " + angle + " fontSize: " + fontSize
            + " boxed: " + boxed + " opaque: " + opaque;
    }
}