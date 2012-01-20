package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;
import Jama.*;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/** Transform a Rectangle2D into another Rectangle2D using independent
    linear functions for the x- and y-axes. This is equivalent to the
    set of affine transformations that have 0 shear. */
public class RectangleTransform extends AffinePolygonTransform {

    Rectangle2D.Double input;
    Rectangle2D.Double output;

    public RectangleTransform(RectangleTransform other) {
        input = (Rectangle2D.Double) other.input.clone();
        output = (Rectangle2D.Double) other.output.clone();
        update();
    }

    public RectangleTransform() {
        input = new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0);
        output = new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0);
    }

    public RectangleTransform(@JsonProperty("input") Rectangle2D input,
                              @JsonProperty("output") Rectangle2D output) {
        this.input = new Rectangle2D.Double
            (input.getX(), input.getY(), input.getWidth(), input.getHeight());
        this.output = new Rectangle2D.Double
            (output.getX(), output.getY(), output.getWidth(), output.getHeight());
        update();
    }

    public RectangleTransform clone() {
        return new RectangleTransform(this);
    }

    /** Update the underlying affine transformation after changes to
        the input or output rectangles to maintain the requirement
        that transform(input) equals output. */
    protected void update() {
        double mx = output.width / input.width;
        double bx = output.x - input.x * mx;

        double my = output.height / input.height;
        double by = output.y - input.y * my;

        setTransform(mx, 0.0, 0.0, my, bx, by);
    }

    public RectangleTransform createInverse() {
        return new RectangleTransform(output, input);
    }

    @JsonIgnore public Point2D.Double[] getInputVertices() {
        return Duh.toPoint2DDoubles(input);
    }

    @JsonIgnore public Point2D.Double[] getOutputVertices() {
        return Duh.toPoint2DDoubles(output);
    }

    private void concatSub(Transform2D other, Rectangle2D.Double rect) {
        // Could try to be more subtle here...
        throw new RuntimeException("Could not concatenate");
    }

    private void concatSub(AffineTransform other, Rectangle2D.Double rect) {
        if (other.getShearX() != 0 || other.getShearY() != 0) {
            throw new RuntimeException("Could not apply shear");
        }
        rect.width *= other.getScaleX();
        rect.height *= other.getScaleY();
        rect.x = rect.x * other.getScaleX() + other.getTranslateX();
        rect.y = rect.y * other.getScaleY() + other.getTranslateY();
        update();
    }

    public void preConcatenate(Transform2D other) {
        concatSub(other, output);
    }

    public void concatenate(Transform2D other) {
        try {
            concatSub(other.createInverse(), input);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void preConcatenate(AffineTransform other) {
        concatSub(other, output);
    }

    public void concatenate(AffineTransform other) {
        try {
            concatSub(other.createInverse(), input);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @JsonProperty("input") public Rectangle2D.Double inputBounds() {
        return (Rectangle2D.Double) input.clone();
    }

    @JsonProperty("output") public Rectangle2D.Double outputBounds() {
        return (Rectangle2D.Double) output.clone();
    }

    public String toString() {
        return PolygonTransformAdapter.toString(this) + "(" + super.toString() + ")";
    }

    public void check() {
        PolygonTransformAdapter.check(this);
    }
}
