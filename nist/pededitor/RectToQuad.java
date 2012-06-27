package gov.nist.pededitor;

import java.awt.geom.*;

/** Transform a rectangles with sides parallel to the coordinate axes
to an arbitrary quadrilaterals. These transformations have to be
non-affine in general: three point translations define an affine
transformation, so how do you make the fourth point fit? By adding an
extra xy dependency to both the x and y coordinates. */
public class RectToQuad extends RectToQuadCommon {

    public RectToQuad() {
        xform = new AffineXY();
    }

    public RectToQuad(RectToQuad other) {
        xform = new AffineXY();
        copyFieldsFrom(other);
    }

    @Override public RectToQuad clone() {
        return new RectToQuad(this);
    }

    /** @return the inverse transformation as a QuadToRect object. */
    @Override
	public QuadrilateralTransform createInverse() {
        QuadToRect inv = new QuadToRect();
        inv.copyFieldsFrom(this);
        return inv;
    }

    @Override
	public Point2D.Double[] getInputVertices() {
        return rectVertices();
    }

    @Override
	public Point2D.Double[] getOutputVertices() {
        return quadVertices();
    }

    // Returns a transformation from the unit square to the input rectangle
    @Override
	public Affine squareToDomain() {
        return new Affine(w, 0, 0, h, x, y);
    }

    @Override
	protected void update() {
        super.update();
        // This could be turned off if speed becomes critical, but
        // right now transformation updates are not that frequent.
        check();
    }

    @Override
	public void preConcatenate(Transform2D other) {
        transformQuad(other);
    }

    @Override
	public void concatenate(Transform2D other) {
        try {
            transformRect(other.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }
}
