/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** DecorationHandle that really just identifies a random point on a
    Interp2DDecoration. Kind of a hack, and misleading because
    it doesn't really implement the interface. */
class NullParameterizableHandle implements BoundedParam2DHandle {
    Interp2DDecoration dec;
    double t;
    Point2D.Double point;

    public NullParameterizableHandle(Interp2DDecoration dec, double t,
                                     Point2D.Double point) {
        this.dec = dec;
        this.t = t;
        this.point = point;
    }

    @Override public BoundedParam2D getParameterization() {
        return getDecoration().getParameterization();
    }

    @Override public double getT() {
        return t;
    }

    @Override public Interp2DDecoration getDecoration() {
        return dec;
    }

    @Override public Point2D.Double getLocation() {
        return (Point2D.Double) point.clone();
    }

    @Override public DecorationHandle move(Point2D dest) {
        return null;
    }

    @Override public DecorationHandle copy(Point2D dest) {
        return null;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[" + dec + ", " + t + "]@"
            + Geom.toString(getLocation());
    }
}
