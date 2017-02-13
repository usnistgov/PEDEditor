/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/** Class to hold a shape drawn at a given angle, scale, position,
    with a given color, anchored at a specific location within the
    shape's bounding box. */
@JsonSerialize(include = Inclusion.NON_DEFAULT)
abstract public class TransformedShape
    implements Angled, Cloneable, Decoration, DecorationHandle {

    /** x position of the anchor point */
    double x;
    /** y position of the anchor point */
    double y;

    /** angle in radians */
    double angle = 0.0;

    /** A multiple of a standard scale (1.0 = normal), where the
        standard scale is defined by the application. */
    double scale;

    Color color = null;

    public TransformedShape() { }

    public TransformedShape(double x,
                            double y,
                            double scale,
                            double angle) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.angle = angle;
    }

    @Override abstract public TransformedShape clone();

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    @Override public void setAngle(double angle) { this.angle = angle; }
    public void setScale(double scale) { this.scale = scale; }

    /** @return null unless this has been assigned a color. */
    @Override public Color getColor() {
        return color;
    }

    /** Set the color. Use null to indicate that the color should be
        the same as whatever was last chosen for the graphics
        context. */
    @Override public void setColor(Color color) {
        this.color = color;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    @Override @JsonIgnore public Point2D.Double getLocation() {
        return new Point2D.Double(getX(), getY());
    }
    @Override public double getAngle() { return angle; }
    public double getScale() { return scale; }

    @Override public void transform(AffineTransform xform) {
        Point2D.Double p = new Point2D.Double(getX(), getY());
        xform.transform(p, p);
        setX(p.x);
        setY(p.y);
        setAngle(Geom.transformRadians(xform, getAngle()));
    }

    @Override public TransformedShape move(Point2D dest) {
        setX(dest.getX());
        setY(dest.getY());
        return this;
    }

    @Override public TransformedShape copy(Point2D dest) {
        TransformedShape res = clone();
        res.move(dest);
        return res;
    }

    @Override public TransformedShape createTransformed(AffineTransform xform) {
        TransformedShape res = clone();
        res.transform(xform);
        return res;
    }

    @Override public DecorationHandle[] getHandles(DecorationHandle.Type type) {
        return new TransformedShape[] { this };
    }

    @JsonIgnore @Override public TransformedShape getDecoration() {
        return this;
    }

}
