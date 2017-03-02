/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import com.fasterxml.jackson.annotation.JsonIgnore;

interface Transform2D {
    /** @return the inverse transform. */
    default Transform2D createInverse()
        throws NoninvertibleTransformException {
        throw new NoninvertibleTransformException("createInverse()");
    }

    /** Change this transformation into one that represents the
        composition of this transformation with the other
        transformation. */
    default void preConcatenate(Transform2D other) {
        throw new UnsupportedOperationException();
    }
    

    /** Change this transformation into one that represents the
        composition of the other transformation with this
        transformation. */
    default void concatenate(Transform2D other) {
        throw new UnsupportedOperationException();
    }

    /** @return the transformation of (x,y) as a Point2D.Double */
    Point2D.Double transform(double x, double y) throws UnsolvableException;

    default Point2D.Double transform(Point2D.Double point)
        throws UnsolvableException {
        return transform(point.x, point.y);
    }

    default Point2D.Double transform(Point2D point)
        throws UnsolvableException {
        return transform(point.getX(), point.getY());
    }

    /** Transform many points at once */
    default void transform(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts)
        throws UnsolvableException {
        int twice = numPts * 2;
        for (int i = 0; i < twice; i += 2) {
            Point2D.Double outpt =
                transform(srcPts[srcOff + i], srcPts[srcOff + i + 1]);
            dstPts[dstOff + i] = outpt.x;
            dstPts[dstOff + i + 1] = outpt.y;
        }
    }

    /** @return true if this transform never throws an
        UnsolvableException. */
    default boolean transformNeverThrows() {
        return false; // Override otherwise
    }

    /** @return true if this is an affine transformation of some
        kind. */
    @JsonIgnore default boolean isAffine() {
        return false; // Override otherwise
    }

    Transform2D clone();
}
