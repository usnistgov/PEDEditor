package gov.nist.pededitor;

public class AnchoredLabel {
    double x;
    double y;
    double anchorX;
    double anchorY;
    String text;

    public AnchoredLabel(String text, double anchorX, double anchorY) {
        this.text = text;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getString() { return text; }
    public double getXWeight() { return anchorX; }
    public double getYWeight() { return anchorY; }

    public String toString() {
        return "'" + text + "' x: " + x  + " y: " + y + " wx: " + anchorX
            + " wy: " + anchorY;
    }
}