/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

class TieLineHandle implements DecorationHandle {
    static enum Type { INNER1, INNER2, OUTER1, OUTER2 };

    TieLine decoration;
    /** A tie line is not a point object. It has up to four
        corners that can be used as handles to select it. */
    Type handle;

    TieLineHandle(TieLine decoration, Type handle) {
        this.decoration = decoration;
        this.handle = handle;
    }

    @Override public TieLineHandle copy(double dx, double dy) {
        throw new UnsupportedOperationException
            ("Tie lines cannot be copied.");
    }

    @Override public Point2D.Double getLocation() {
        switch (handle) {
        case INNER1:
            return getDecoration().getInner1();
        case INNER2:
            return getDecoration().getInner2();
        case OUTER1:
            return getDecoration().getOuter1();
        case OUTER2:
            return getDecoration().getOuter2();
        }

        return null;
    }

    @Override public TieLineHandle moveHandle(double dx, double dy) {
        // Tie line movement happens indirectly: normally,
        // everything at a key point moves at once, which means
        // that the control point that delimits the tie line moves
        // with it. No additional work is required here.
        return this;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        if (getClass() != TieLineHandle.class) return false;

        TieLineHandle cast = (TieLineHandle) other;
        return handle == cast.handle
            && getDecoration().equals(cast.getDecoration());
    }

    @Override public TieLine getDecoration() {
        return decoration;
    }

    @Override
    public DecorationHandle copyFor(Decoration other) {
        return new TieLineHandle((TieLine) other, handle);
    }

    @Override
    public Double getLocation(AffineTransform xform) {
        return DecorationHandle.slowLocation(this, xform);
    }
}
