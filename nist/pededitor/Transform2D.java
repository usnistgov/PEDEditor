package gov.nist.pededitor;

import java.awt.geom.*;

import org.codehaus.jackson.annotate.JsonIgnore;

interface Transform2D {
    /** @return the inverse transform. */
    Transform2D createInverse() throws NoninvertibleTransformException;

    /** Change this transformation into one that represents the
        composition of this transformation with the other
        transformation. May not succeed, and should throw some kind of
        exception in that case. */
    void preConcatenate(Transform2D other);

    /** Change this transformation into one that represents the
        composition of the other transformation with this
        transformation. May not succeed, and should throw some kind of
        exception in that case. */
   void concatenate(Transform2D other);

    /** @return the transformation of (x,y) as a Point2D.Double */
    Point2D.Double transform(double x, double y) throws UnsolvableException;
    Point2D.Double transform(Point2D.Double point) throws UnsolvableException;
    Point2D.Double transform(Point2D point) throws UnsolvableException;

    /** Transform many points at once */
    void transform(double[] srcPts, int srcOff,
                   double[] dstPts, int dstOff, int numPts)
        throws UnsolvableException;

    /** @return true if this transform never throws an
        UnsolvableException. */
    boolean transformNeverThrows();

    /** @return true if this is an affine transformation of some
        kind. */
    @JsonIgnore boolean isAffine();

    Transform2D clone();
}
