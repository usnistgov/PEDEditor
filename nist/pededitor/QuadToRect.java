package gov.nist.pededitor;

import java.awt.geom.*;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/** Transform a rectangles with sides parallel to the coordinate axes
to an arbitrary quadrilaterals. These transformations have to be
non-affine in general: three point translations define an affine
transformation, so how do you make the fourth point fit? By adding an
extra xy dependency to both the x and y coordinates. */
public class QuadToRect extends RectToQuadCommon
                                implements QuadrilateralTransform {

    public QuadToRect() {
        xform = new AffineXYInverse();
    }

    public QuadToRect(QuadToRect other) {
        xform = new AffineXYInverse();
        copyFieldsFrom(other);
    }

    public QuadToRect(@JsonProperty("input") Point2D.Double[] inpts,
                      @JsonProperty("output") Rectangle2D rect) {
        this();
        setVertices(inpts);
        setRectangle(rect);
    }

    @Override public QuadToRect clone() {
        return new QuadToRect(this);
    }

    /** @return the inverse transformation as a RectToQuad. */
    public QuadrilateralTransform createInverse() {
        RectToQuad inv = new RectToQuad();
        inv.copyFieldsFrom(this);
        inv.xform = xform.createInverse();
        return inv;
    }

    @JsonIgnore public Point2D.Double[] getOutputVertices() {
        return rectVertices();
    }

    @JsonProperty("input") public Point2D.Double[] getInputVertices() {
        return quadVertices();
    }

    @JsonProperty("output") public Rectangle2D.Double
        getOutputRectangle() {
        return getRectangle();
    }

    /** @return a transformation from the unit square to the input
     * quadrilateral */
    public RectToQuad squareToDomain() {
        RectToQuad output = new RectToQuad();
        output.setVertices(getInputVertices());
        return output;
    }

    protected void update() {
        super.update();
        ((AffineXYInverse) xform).includeInRange(x + w/2, y + h/2);
        // This could be turned off if speed becomes critical, but
        // right now transformation updates are not that frequent.
        check();
    }

    public void preConcatenate(Transform2D other) {
        transformRect(other);
    }

    public void concatenate(Transform2D other) {
        try {
            transformQuad(other.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }
}
