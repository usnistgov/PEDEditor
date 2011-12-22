package gov.nist.pededitor;

import org.codehaus.jackson.annotate.JsonProperty;

public class AnchoredLabel {
    double x;
    double y;
    double xWeight;
    double yWeight;
    String text;

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

    public double getX() { return x; }
    public double getY() { return y; }
    public String getString() { return text; }
    @JsonProperty("xWeight") public double getXWeight() { return xWeight; }
    @JsonProperty("yWeight") public double getYWeight() { return yWeight; }

    public String toString() {
        return "'" + text + "' x: " + x  + " y: " + y + " wx: " + xWeight
            + " wy: " + yWeight;
    }
}