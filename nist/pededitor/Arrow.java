/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.geom.*;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties
    ({"bounds2D", "bounds", "windingRule", "currentPoint"})
public class Arrow extends Path2D.Double implements Decorated {
    private static final long serialVersionUID = -3704208186216534922L;

    @JsonProperty public double x;
    @JsonProperty public double y;
    @JsonProperty public double size;
    @JsonIgnore public double theta;

    Color color = null;

    /** @param size The "size" of the arrow is a bit arbitrary, but
        it should be roughly equal to the line width of any line that
        is attached to the arrow.
    */
    public Arrow(@JsonProperty("x") double x,
                 @JsonProperty("y") double y,
                 @JsonProperty("size") double size,
                 @JsonProperty("angle") double theta) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.theta = theta;
        addArrow();
    }

    void addArrow() {
        addArrow(this, x, y, size, theta);
    }

    /** @return null unless this has been assigned a color. */
    public Color getColor() {
        return color;
    }

    public double getAngle() {
        return theta;
    }

    public void setAngle(double theta) {
        this.theta = theta;
        reset();
        addArrow();
    }

    /** Set the color. Use null to indicate that the color should be
        the same as whatever was last chosen for the graphics
        context. */
    public void setColor(Color color) {
        this.color = color;
    }

    /** For some reason, the clone() method is declared final in
        Path2D.Double, so I had to give my clone method a different
        name. */
    public Arrow clonus() {
        Arrow output = new Arrow(x, y, size, theta);
        output.setColor(getColor());
        return output;
    }

    /** Add an arrow to the path "output". */
    static public void addArrow
        (Path2D.Double output, double x, double y,
         double size, double theta) {
        AffineTransform xform = AffineTransform.getTranslateInstance(x,y);
        xform.rotate(theta);
        xform.scale(size, size);

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
        output.moveTo(points[p], points[p+1]); p += 2;
        output.lineTo(points[p], points[p+1]); p += 2;
        output.lineTo(points[p], points[p+1]); p += 2;
        output.quadTo(points[p], points[p+1],
                      points[p+2], points[p+3]);
        p += 4;
        output.quadTo(points[p], points[p+1],
                      points[p+2], points[p+3]);
        p += 4;
        output.lineTo(points[p], points[p+1]); p += 2;
        if (p != points.length) {
            throw new IllegalStateException();
        }
        output.closePath();
    }
}
