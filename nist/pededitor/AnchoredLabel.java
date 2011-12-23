package gov.nist.pededitor;

import org.codehaus.jackson.annotate.JsonProperty;

public class AnchoredLabel {
    double x;
    double y;
    double xWeight;
    double yWeight;
    String text;
    double angle = 0.0;

    /** A multiple of a standard font size (so 1.0 = normal). */
    double fontSize;

    public AnchoredLabel
        (@JsonProperty("string") String text,
         @JsonProperty("xWeight") double xWeight,
         @JsonProperty("yWeight") double yWeight) {
        this.text = text;
        this.xWeight = xWeight;
        this.yWeight = yWeight;
    }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setXWeight(double xWeight) { this.xWeight = xWeight; }
    public void setYWeight(double yWeight) { this.yWeight = yWeight; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setFontSize(double fontSize) { this.fontSize = fontSize; }
    public void setText(String text) { this.text = text; }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getText() { return text; }
    public double getAngle() { return angle; }
    public double getFontSize() { return fontSize; }
    @JsonProperty("xWeight") public double getXWeight() { return xWeight; }
    @JsonProperty("yWeight") public double getYWeight() { return yWeight; }

    public String toString() {
        return "'" + text + "' x: " + x  + " y: " + y + " wx: " + xWeight
            + " wy: " + yWeight + " angle: " + angle + " fontSize: " + fontSize;
    }
}