/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Class to hold a shape drawn at a given angle, scale, position,
    with a given color, anchored at a specific location within the
    shape's bounding box. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
abstract public class AnchoredTransformedShape extends TransformedShape {

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

    public AnchoredTransformedShape() { }

    public AnchoredTransformedShape(double xWeight, double yWeight) {
        this.xWeight = xWeight;
        this.yWeight = yWeight;
    }

    public void setXWeight(double xWeight) { this.xWeight = xWeight; }
    public void setYWeight(double yWeight) { this.yWeight = yWeight; }
    public double getXWeight() { return xWeight; }
    public double getYWeight() { return yWeight; }

    @Override public void reflect() {
        setYWeight(1.0 - getYWeight());
    }
}
