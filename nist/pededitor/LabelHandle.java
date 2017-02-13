/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

import org.codehaus.jackson.annotate.JsonIgnore;

public class LabelHandle implements DecorationHandle {
    static enum Type { CENTER, ANCHOR };

    AnchoredLabel decoration;
    /** Labels may be grabbed from the anchor or the center. */
    Type handle;

    @Override public AnchoredLabel getDecoration() {
        return decoration;
    }

    LabelHandle() {}

    LabelHandle(AnchoredLabel decoration, Type handle) {
        this.decoration = decoration;
        this.handle = handle;
    }

    public Type getType() {
        return handle;
    }

    @Override public LabelHandle move(Point2D dest) {
        getDecoration().move(getAnchorLocation(dest));
        return this;
    }

    @Override public LabelHandle copy(Point2D dest) {
        Point2D.Double destAnchor = getAnchorLocation(dest);
        AnchoredLabel label = (AnchoredLabel) getDecoration().copy(destAnchor);
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
}
