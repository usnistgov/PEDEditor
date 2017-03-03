/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LabelHandle implements DecorationHandle {
    static enum Type { CENTER, ANCHOR };

    Label decoration;
    /** Labels may be grabbed from the anchor or the center. */
    Type handle;

    @Override public Label getDecoration() {
        return decoration;
    }

    LabelHandle() {}

    LabelHandle(Label decoration, Type handle) {
        this.decoration = decoration;
        this.handle = handle;
    }

    public Type getType() {
        return handle;
    }

    @Override public LabelHandle moveHandle(double dx, double dy) {
        getDecoration().moveHandle(dx, dy);
        return this;
    }

    @Override public LabelHandle copy(double dx, double dy) {
        Label label = (Label) getDecoration().copy(dx, dy);
        return new LabelHandle(label, handle);
    }

    @Override public Point2D.Double getLocation() {
        switch (handle) {
        case ANCHOR:
            return getDecoration().getLocation();
        case CENTER:
            return getDecoration().getCenter();
        }
        return null;
    }

    /** Return true if this handle is at the center of the
     * label. */
    @JsonIgnore public boolean isCentered() {
        return (handle == Type.CENTER ||
                (getDecoration().getXWeight() == 0.5
                 && getDecoration().getYWeight() == 0.5));
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        LabelHandle cast = (LabelHandle) other;
        return handle == cast.handle
            && getDecoration().equals(cast.getDecoration());
    }

    /** @return the anchor location for a label whose handle is at
        dest. */
    public Point2D.Double getAnchorLocation(Point2D dest) {
        if (handle == Type.ANCHOR) {
            return new Point2D.Double(dest.getX(), dest.getY());
        }

        // Compute the difference between the anchor
        // location and the center, and apply the same
        // difference to dest.
        Point2D.Double anchor = getDecoration().getLocation();
        Point2D.Double center = getDecoration().getCenter();
        double dx = anchor.x - center.x;
        double dy = anchor.y - center.y;
        return new Point2D.Double(dest.getX() + dx, dest.getY() + dy);
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[" + getDecoration() + ", "
            + handle + "]";
    }

    @Override
    public DecorationHandle copyFor(Decoration other) {
        return new LabelHandle((Label) other, handle);
    }

    @Override
    public Double getLocation(AffineTransform xform) {
        // The anchor is translated by xform. The center's transformed position
        // is more complicated.
        if (handle == Type.ANCHOR) {
            return DecorationHandle.simpleLocation(this,  xform);
        } else {
            return DecorationHandle.slowLocation(this,  xform);
        }
    }
}
