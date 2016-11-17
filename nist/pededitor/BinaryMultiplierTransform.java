/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Specialization of MultiplierConcentrationTransform for binary diagrams. */
public class BinaryMultiplierTransform extends MultiplierConcentrationTransform
    implements SlopeConcentrationTransform {
    public BinaryMultiplierTransform(double... cs) {
        super(cs);
        if (cs.length != 2) {
            throw new IllegalArgumentException("Expected array of length 2");
        }
    }

    @Override public BinaryMultiplierTransform createTransform(double... cs) {
        return new BinaryMultiplierTransform(cs);
    }

    @Override public BinaryMultiplierTransform createInverse() {
        return (BinaryMultiplierTransform) super.createInverse();
    }

    public double transform(double v) {
        double[] vs = {v};
        transform(vs);
        return vs[0];
    }

    /** Given a line or curve whose slope at the given point is as
        given, return the slope of the transformation of that line or
        curve at the transformed point. This is useful for adjusting the
        angles of arrows and text inside a diagram. */
    @Override public Point2D.Double transformSlope(double x, double y,
            double dx, double dy) {
        // dx is affected by component transformations, but dy is not.

        // As x increases, z decreases. The overall effect on dx is...
        double cx = cs[0];
        double cz = cs[1];

        double z = 1 - x;
        double dz = -(dx);

        double qx = x * cx;
        double qz = z * cz;
        double denom = qx + qz;
        double dqx = dx * cx;
        double dqz = dz * cz;
        double dDenom = dqx + dqz;
        double xformdx = (dqx * denom - qx * dDenom)/(denom * denom);
        return new Point2D.Double(xformdx, dy);
    }
}
