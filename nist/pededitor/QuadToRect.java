package gov.nist.pededitor;

import java.awt.geom.*;

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

    /** @return the inverse transformation as a RectToQuad. */
    public QuadrilateralTransform createInverse() {
        RectToQuad inv = new RectToQuad();
        inv.copyFieldsFrom(this);
        inv.xform = xform.createInverse();
        return inv;
    }

    public Point2D.Double[] outputVertices() {
        return rectVertices();
    }

    public Point2D.Double[] inputVertices() {
        return quadVertices();
    }

    /** @return a transformation from the unit square to the input
     * quadrilateral */
    public RectToQuad squareToDomain() {
        RectToQuad output = new RectToQuad();
        output.setVertices(inputVertices());
        return output;
    }

    protected void update() {
        super.update();
        ((AffineXYInverse) xform).includeInRange(x + w/2, y + h/2);
        // This could be turned off if speed becomes critical, but
        // right now transformation updates are not that frequent.
        check();
    }

    public void concatenate(Transform2D other) {
        transformRect(other);
    }

    public void preConcatenate(Transform2D other) {
        try {
            transformQuad(other.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }
}
