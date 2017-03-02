/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

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

    @Override public Point2D.Double getLocation() {
        return new Point2D.Double(x,y);
    }

    @Override public Decoration getDecoration() {
        return null;
    }

    @Override
    public DecorationHandle copyFor(Decoration other) {
        return new NullDecorationHandle(x,y);
    }

    @Override
    public Double getLocation(AffineTransform xform) {
        return DecorationHandle.simpleLocation(this, xform);
    }

    @Override
    public DecorationHandle moveHandle(double dx, double dy) {
        return null;
    }

    @Override
    public DecorationHandle copy(double dx, double dy) {
        return null;
    }
}
