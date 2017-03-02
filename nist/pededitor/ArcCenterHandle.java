/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

public class ArcCenterHandle implements DecorationHandle {
    ArcDecoration decoration;

    ArcCenterHandle() {}

    ArcCenterHandle(ArcDecoration decoration) {
        this.decoration = decoration;
    }

    @Override public ArcDecoration getDecoration() {
        return decoration;
    }

    @Override public ArcCenterHandle moveHandle(double dx, double dy) {
        getDecoration().transform(
                AffineTransform.getTranslateInstance(dx, dy));
        return this;
    }

    @Override public ArcCenterHandle copy(double dx, double dy) {
        ArcDecoration d = getDecoration().clone();
        return new ArcCenterHandle(d).moveHandle(dx, dy);
    }

    @Override public Point2D.Double getLocation() {
        ArcParam2D param = (ArcParam2D)
                getDecoration().getParameterization();
        if (param == null) {
            return getDecoration().getCurve().get(0);
        }
        return new Point2D.Double(
                param.arc.getCenterX(),
                param.arc.getCenterY());
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        return getDecoration() == ((ArcCenterHandle) other).getDecoration();
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[" + getDecoration() + "]";
    }

    @Override public DecorationHandle copyFor(Decoration other) {
        return new ArcCenterHandle((ArcDecoration) other);
    }

    @Override
    public Double getLocation(AffineTransform xform) {
        return DecorationHandle.slowLocation(this, xform);
    }
}
