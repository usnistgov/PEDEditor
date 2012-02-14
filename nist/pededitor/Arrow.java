package gov.nist.pededitor;

import java.awt.Color;
import java.awt.geom.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties
    ({"bounds2D", "bounds", "windingRule", "currentPoint"})
public class Arrow extends Path2D.Double {
    private static final long serialVersionUID = -3704208186216534922L;

    @JsonProperty public double x;
    @JsonProperty public double y;
    @JsonProperty public double size;
    @JsonProperty public double theta;

    Color color = null;

    /** @param size The "size" of the arrow is a bit arbitrary, but
        it should be roughly equal to the line width of any line that
        is attached to the arrow.
    */
    public Arrow(@JsonProperty("x") double x,
                 @JsonProperty("y") double y,
                 @JsonProperty("size") double size,
                 @JsonProperty("theta") double theta) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.theta = theta;
        addArrow(this, x, y, size, theta);
    }

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
        double[] points =
            { 0.0, 0.0,
              -2.56, -3.2,
              6.4, 0.0,
              -2.56, 3.2 };
        xform.transform(points, 0, points, 0, points.length / 2);
        output.moveTo(points[0], points[1]);
        for (int i = 1; 2*i < points.length; ++i) {
            output.lineTo(points[i*2], points[i*2+1]);
        }
        output.closePath();
    }
}
