package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Specialization of ConcentrationTransform for ternary diagrams. */
public class TernaryTransform extends ConcentrationTransform {
    public TernaryTransform(double a, double b, double c) {
        super(a,b,c);
    }

    public TernaryTransform(double[] cs) {
        super(cs);
        if (cs.length != 3) {
            throw new IllegalArgumentException("Expected array of length 3");
        }
    }

    @Override public TernaryTransform inverse() {
        double[] ics = new double[cs.length];
        for (int i = 0; i < cs.length; ++i) {
            ics[i] = 1/cs[i];
        }
        return new TernaryTransform(ics);
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
    public Point2D.Double transformSlope(double x, double y,
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

    /** Like transformSlope(), but applied to an angle. */
    public double transformAngle(Point2D p, double theta) {
        Point2D.Double p2 = transformSlope
            (p.getX(), p.getY(),
             Math.cos(theta), Math.sin(theta));
        return Math.atan2(p2.y, p2.x);
    }
}
