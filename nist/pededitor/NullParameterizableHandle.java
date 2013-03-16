package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** DecorationHandle that really just identifies a random point on a
    ParameterizableDecoration. Kind of a hack, and misleading because
    it doesn't really implement the interface. */
class NullParameterizableHandle implements BoundedParam2DHandle {
    ParameterizableDecoration dec;
    double t;
    Transform2D xform;

    public NullParameterizableHandle(ParameterizableDecoration dec, double t,
                                     Transform2D xform) {
        this.dec = dec;
        this.t = t;
        this.xform = xform;
    }

    @Override public BoundedParam2D getParameterization() {
        return getDecoration().getParameterization();
    }

    @Override public double getT() {
        return t;
    }

    @Override public ParameterizableDecoration getDecoration() {
        return dec;
    }

    @Override public Point2D.Double getLocation() {
        try {
            return xform.transform(getParameterization().getLocation(t));
        } catch (UnsolvableException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override public DecorationHandle remove() {
        return null;
    }

    @Override public void move(Point2D dest) {
    }

    @Override public DecorationHandle copy(Point2D dest) {
        return null;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[" + dec + ", " + t + "]@"
            + Duh.toString(getLocation());
    }
}
