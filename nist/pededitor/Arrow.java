package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties
    ({"bounds2D", "bounds", "windingRule", "currentPoint"})
public class Arrow extends Path2D.Double {
    @JsonProperty public double x;
    @JsonProperty public double y;
    @JsonProperty public double size;
    @JsonProperty public double theta;

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
