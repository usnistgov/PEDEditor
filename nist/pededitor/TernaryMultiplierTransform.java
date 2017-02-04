/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Specialization of MultiplierConcentrationTransform for ternary diagrams. */
public class TernaryMultiplierTransform extends MultiplierConcentrationTransform
    implements SlopeConcentrationTransform {
    public TernaryMultiplierTransform(double a, double b, double c) {
        super(a,b,c);
    }

    public TernaryMultiplierTransform(double[] cs) {
        super(cs);
        if (cs.length != 3) {
            throw new IllegalArgumentException("Expected array of length 3");
        }
    }

    @Override public TernaryMultiplierTransform clone() {
        return (TernaryMultiplierTransform) super.clone();
    }

    @Override public TernaryMultiplierTransform createTransform(double... cs) {
        return new TernaryMultiplierTransform(cs);
    }

    @Override public TernaryMultiplierTransform createInverse() {
        return (TernaryMultiplierTransform) super.createInverse();
    }

    public Point2D.Double transform(Point2D p) {
        double[] vs = { p.getX(), p.getY() };
        transform(vs);
        return new Point2D.Double(vs[0], vs[1]);
    }

    /** Given a line or curve whose slope at the given point is as
        given, return the slope of the transformation of that line or
        curve at the transformed point. This is useful for adjusting the
        angles of arrows and text inside a diagram. */
    @Override public Point2D.Double transformSlope(double x, double y,
                                         double dx, double dy) {
        // As x and y increase, z decreases. The overall effect on the
        // dx and dy is...
        double cx = cs[0];
        double cy = cs[1];
        double cz = cs[2];

        double z = 1 - x - y;
        double dz = -(dx + dy);

        double qx = x * cx;
        double qy = y * cy;
        double qz = z * cz;
        double denom = qx + qy + qz;
        double dqx = dx * cx;
        double dqy = dy * cy;
        double dqz = dz * cz;
        double dDenom = dqx + dqy + dqz;
        double xformdx = (dqx * denom - qx * dDenom)/(denom * denom);
        double xformdy = (dqy * denom - qy * dDenom)/(denom * denom);
        return new Point2D.Double(xformdx, xformdy);
    }
}
