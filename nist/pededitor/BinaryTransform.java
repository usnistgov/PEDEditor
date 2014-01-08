/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Class to perform concentration transformations.

    A concentration is a vector <x_1, ..., x_n> such that each x_i >=
    0 and sum_{i=0}^{i=n-1} x_i <= 1. There is an implicit x_{n+1}
    element that equals one minus the sum of all other values.

    The concentration transformations that are supported have the
    following form:

    1. Initially, each x_i (including for i=n+1) is transformed into c_i * x_i.

    2. All coefficients are divided by the sum of all x_i, so the
    resulting vector also appears as a concentration.
 */
public class BinaryTransform extends ConcentrationTransform {
    public BinaryTransform(double... cs) {
        super(cs);
        if (cs.length != 2) {
            throw new IllegalArgumentException("Expected array of length 3");
        }
    }

    @Override public BinaryTransform inverse() {
        double[] ics = new double[cs.length];
        for (int i = 0; i < cs.length; ++i) {
            ics[i] = 1/cs[i];
        }
        return new BinaryTransform(ics);
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
    public Point2D.Double transformSlope(double x, double y,
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

    /** Like transformSlope(), but applied to an angle. */
    public double transformAngle(Point2D p, double theta) {
        Point2D.Double p2 = transformSlope
            (p.getX(), p.getY(),
             Math.cos(theta), Math.sin(theta));
        return Math.atan2(p2.y, p2.x);
    }
}
