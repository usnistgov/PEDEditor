/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.*;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties
({"lineWidth", "lineStyle"})
public class Arrow extends TransformedShape implements Cloneable {

    /** @param scale The "scale" of the arrow is a bit arbitrary, but
        it should be roughly equal to the line width of any line that
        is attached to the arrow. For legacy reasons, accepts the
        "scale" property being called "size" instead.
        
        @param angle In radians.
    */
    public Arrow(@JsonProperty("x") double x,
                 @JsonProperty("y") double y,
                 @JsonProperty("size") double scale,
                 @JsonProperty("angle") double angle) {
        super(x, y, scale, angle);
    }

    public Arrow() {
    }

    void addArrow(Path2D.Double res) {
        addArrow(res, x, y, scale, angle);
    }

    /** For some reason, the clone() method is declared final in
        Path2D.Double, so I had to give my clone method a different
        name. */
    @Override public Arrow clone() {
        Arrow res = new Arrow(getX(), getY(), getScale(), getAngle());
        res.setColor(getColor());
        return res;
    }

    @Override @JsonIgnore public Point2D.Double getLocation() {
        return new Point2D.Double(x,y);
    }

    @JsonIgnore public Shape getShape() {
        Path2D.Double res = new Path2D.Double();
        addArrow(res);
        return res;
    }

    static public Shape getShape(double x, double y, double scale,
            double angle) {
        Path2D.Double res = new Path2D.Double();
        addArrow(res, x, y, scale, angle);
        return res;
    }

    /** Add an arrow to the path "res". */
    static public void addArrow
        (Path2D.Double res, double x, double y,
         double scale, double theta) {
        AffineTransform xform = AffineTransform.getTranslateInstance(x,y);
        xform.rotate(theta);
        xform.scale(scale, scale);

        // Sharp, pointy arrows ordinarily look best, but those can be
        // difficult to coordinate with the segments that form the
        // arrow shafts.

        // Line segments end with r = 0.5 * linewidth semicircles, and
        // if an arrowpoint ends precisely where the line segment
        // does, the bulbous tip of the line sticks out past the
        // arrowpoint in a really ugly way. Somehow deciding to end
        // the line segment early if there is an arrow attached,
        // however, becomes a logical mess. Extending the arrow far
        // past the bulb to keep it sharp implies that the true end of
        // the arrow is well past the position its (x,y) value
        // implies. Solution: extend the arrow just enough to hide the
        // semicircle, which requires blunting the tip slightly.

        // The arrow extends from the coordinate (-7.0 times the line
        // width in the arrow's pointing dimension, 3.2 times the line
        // width in the perpendicular dimension) to (0.1779, 0.47),
        // the point of tangency from (-7.0, 3.2) to the circle of
        // radius 0.5 centered at the origin. From there, the arrow
        // smoothly turns around a quadratic Bezier to the blunted tip
        // at (0.5, 0), and then symmetrically reverses course to draw
        // the other half of the arrow.

        double[] points =
            { -4.5, 0.0, // Back of arrow's spine
              -7.0, 3.2, // Top left barb
              0.1778999, 0.467281, // Tangency with r=0.5 circle at (0,0)
              0.5, 0.3446534, // Control point -- intersection of
                              // previous segment and line { x = 0.5 }
              0.5, 0, // Blunted tip of arrow
              0.5, -0.3446534, // And then the arrow's other side
              0.1778999, -0.467281,
              -7.0, -3.2 };
        xform.transform(points, 0, points, 0, points.length / 2);

        int p = 0;
        res.moveTo(points[p], points[p+1]); p += 2;
        res.lineTo(points[p], points[p+1]); p += 2;
        res.lineTo(points[p], points[p+1]); p += 2;
        res.quadTo(points[p], points[p+1],
                      points[p+2], points[p+3]);
        p += 4;
        res.quadTo(points[p], points[p+1],
                      points[p+2], points[p+3]);
        p += 4;
        res.lineTo(points[p], points[p+1]); p += 2;
        if (p != points.length) {
            throw new IllegalStateException();
        }
        res.closePath();
    }

    @Override public void draw(Graphics2D g) {
        g.fill(getShape(x, y, scale, angle));
    }

    @Override public void draw(Graphics2D g, double scale) {
        g.fill(getShape(x * scale, y * scale, scale * this.scale, angle));
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[(" + x + ", " + y + ") s=" + scale
            + " ang=" + angle + "]";
    }
}
