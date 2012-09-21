package gov.nist.pededitor;

import java.awt.Color;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/** Class to hold definitions of a text or HTML string anchored to a
    location in space and possibly drawn at an angle. */
@JsonSerialize(include = Inclusion.NON_DEFAULT)
public class TransformedShape implements Decorated {

    /** x position of the anchor point */
    double x;
    /** y position of the anchor point */
    double y;

    /** Positioning relative to the anchor point. 0.0 = The
        anchor point lies along the left edge of the shape in
        baseline coordinates (if the shape is rotated, then this edge
        may not be on the left in physical coordinates; for example,
        if the shape is rotated by an angle of PI/2, then this will be
        the top edge in physical coordinates); 0.5 = the anchor point
        lies along the vertical line (in baseline coordinates) that
        bisects the shape; 1.0 = the anchor point lies along the
        right edge (in baseline coordinates) of the shape */
    double xWeight;

    /** Positioning relative to the anchor point. 0.0 = The
        anchor point lies along the top edge of the shape in
        baseline coordinates (if the shape is rotated, then this edge
        may not be on top in physical coordinates; for example, if the
        shape is rotated by an angle of PI/2, then this will be the
        right edge in physical coordinates); 0.5 = the anchor point
        lies along the horizontal line (in baseline coordinates) that
        bisects the shape; 1.0 = the anchor point lies along the
        bottom edge (in baseline coordinates) of the shape */
    double yWeight;

    /** Printing angle in clockwise radians. 0 represents
        left-to-right text; Math.PI/2 represents text where lines
        extend downwards. */
    double angle = 0.0;

    /** A multiple of a standard scale (1.0 = normal), where the
        standard scale is defined by the application. */
    double scale;

    Color color = null;

    public TransformedShape() {
    }

    public TransformedShape(double xWeight, double yWeight) {
        this.xWeight = xWeight;
        this.yWeight = yWeight;
    }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setXWeight(double xWeight) { this.xWeight = xWeight; }
    public void setYWeight(double yWeight) { this.yWeight = yWeight; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setScale(double scale) { this.scale = scale; }

    /** @return null unless this has been assigned a color. */
    public Color getColor() {
        return color;
    }

    /** Set the color. Use null to indicate that the color should be
        the same as whatever was last chosen for the graphics
        context. */
    public void setColor(Color color) {
        this.color = color;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public double getScale() { return scale; }
    @JsonProperty("xWeight") public double getXWeight() { return xWeight; }
    @JsonProperty("yWeight") public double getYWeight() { return yWeight; }
}
