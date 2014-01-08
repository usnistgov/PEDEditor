/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** DecorationHandle that isn't actually the handle to anything. It
    has a location but that's it. */
public class NullDecorationHandle implements DecorationHandle {
    double x;
    double y;

    public NullDecorationHandle(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public NullDecorationHandle(Point2D p) {
        x = p.getX();
        y= p.getY();
    }

    @Override public DecorationHandle remove() {
        return null;
    }

    @Override public void move(Point2D dest) {
    }

    @Override public DecorationHandle copy(Point2D dest) {
        return null;
    }

    @Override public java.awt.geom.Point2D.Double getLocation() {
        return new Point2D.Double(x,y);
    }

    @Override public Decoration getDecoration() {
        return null;
    }
}
