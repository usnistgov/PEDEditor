/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;

/** Simple abstract class that partly implements the Transform2D
    interface. */
abstract public class Transform2DAdapter implements Transform2D {
    @Override
	abstract public Point2D.Double transform(double x, double y)
        throws UnsolvableException;
    @Override
	abstract public Transform2D createInverse()
        throws NoninvertibleTransformException;
    @Override
	abstract public void preConcatenate(Transform2D other);
    @Override
	abstract public void concatenate(Transform2D other);
    @Override
        abstract public Transform2D clone();

    @Override
	public Point2D.Double transform(Point2D.Double point)
        throws UnsolvableException {
        return transform(point.x, point.y);
    }

    @Override
	public Point2D.Double transform(Point2D point)
        throws UnsolvableException {
        return transform(point.getX(), point.getY());
    }


    /** Transform many points at once */
    @Override
	public void transform(double[] srcPts, int srcOff,
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

    /** Better safe than sorry: if not overridden, then assume the
        transform may throw an UnsolvableException. */
    @Override
	public boolean transformNeverThrows() {
        return false;
    }

    /** If not overridden, assume the transform is not affine. */
    @Override
	public boolean isAffine() {
        return false;
    }
}
