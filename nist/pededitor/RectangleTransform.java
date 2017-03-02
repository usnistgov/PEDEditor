/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Transform a Rectangle2D into another Rectangle2D using independent
    linear functions for the x- and y-axes. This is equivalent to the
    set of affine transformations that have 0 shear. */
public class RectangleTransform extends AffinePolygonTransform {
    private static final long serialVersionUID = 3262190356272005695L;

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

    @Override public RectangleTransform clone() {
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

    @Override public RectangleTransform createInverse() {
        return new RectangleTransform(output, input);
    }

    @Override @JsonIgnore public Point2D.Double[] getInputVertices() {
        return Geom.toPoint2DDoubles(input);
    }

    @Override @JsonIgnore public Point2D.Double[] getOutputVertices() {
        return Geom.toPoint2DDoubles(output);
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

    @Override public void preConcatenate(Transform2D other) {
        concatSub(other, output);
    }

    @Override public void concatenate(Transform2D other) {
        try {
            concatSub(other.createInverse(), input);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override public void preConcatenate(AffineTransform other) {
        concatSub(other, output);
    }

    @Override public void concatenate(AffineTransform other) {
        try {
            concatSub(other.createInverse(), input);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @JsonProperty("input") public Rectangle2D.Double inputRectangle() {
        return (Rectangle2D.Double) input.clone();
    }

    static Rectangle2D.Double normalize(Rectangle2D.Double rect) {
        return new Rectangle2D.Double
            ((rect.width < 0) ? (rect.x + rect.width) : rect.x,
             (rect.height < 0) ? (rect.y + rect.height) : rect.y,
             Math.abs(rect.width), Math.abs(rect.height));
    }

    @JsonProperty("output") public Rectangle2D.Double outputRectangle() {
        return (Rectangle2D.Double) output.clone();
    }

    @Override public Rectangle2D.Double inputBounds() {
        return normalize(input);
    }

    @Override public Rectangle2D.Double outputBounds() {
        return normalize(output);
    }

    /** Scale the output rectangle by sx along the x axis and sy along
        the y axis. */
    public void scaleOutput(double sx, double sy) {
        output.x *= sx;
        output.width *= sx;
        output.y *= sy;
        output.height *= sy;
        update();
    }

    @Override public String toString() {
        return PolygonTransformAdapter.toString(this) + "(" + super.toString() + ")";
    }
}
