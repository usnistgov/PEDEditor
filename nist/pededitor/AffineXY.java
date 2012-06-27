package gov.nist.pededitor;

import java.awt.geom.*;

/** The class AffineXY applies the following transformation:

(x,y) -> (? + ? x + ? y + ? xy, ? + ? x + ? y + ? xy) where the ?
stand for different constants.
*/
public class AffineXY extends AffineXYCommon implements Transform2D {

    @Override
	public Point2D.Double transform(double x, double y) {
        return new Point2D.Double
            (xk + x * (xkx + y * xkxy) + y * xky,
             yk + x * (ykx + y * ykxy) + y * yky);
    }

    @Override
	public Point2D.Double transform(Point2D.Double p) {
        return transform(p.x, p.y);
    }

    /** Transform many points at once */
    @Override
	public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff, int numPts) {
        int twice = numPts * 2;
        for (int i = 0; i < twice; i += 2) {
            double x = srcPts[srcOff + i];
            double y = srcPts[srcOff + i + 1];
            dstPts[dstOff + i] = xk + x * (xkx + y * xkxy) + y * xky;
            dstPts[dstOff + i + 1] = yk + x * (ykx + y * ykxy) + y * yky;
        }
    }

    @Override
	public AffineXYCommon createInverse() {
        AffineXYInverse inv = new AffineXYInverse();
        inv.copyFieldsFrom(this);
        return inv;
    }

    /** Always return true: this transformation never throws an
        UnsolvableException. */
    @Override
	public boolean transformNeverThrows() {
        return true;
    }

    @Override public AffineXY clone() {
        AffineXY output = new AffineXY();
        output.copyFieldsFrom(this);
        return output;
    }
};
