/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Interface for the combination of a Decoration and a location on
    it. For example, to display a dragged object, you need to know
    both the object and where on the object you grabbed it. */
interface DecorationHandle {
    // CONTROL_POINT handles affect the way the Decoration is
    // displayed.

    // SELECTION handles are points the user is likely to choose when the
    // user wants to select the item.

    // MOVE handles are a sufficient set of handles to transform in
    // order to move the item. The set of MOVE handles is normally a
    // subset of or equal to the set of CONTROL_POINT handles.

    // There is often considerable or even total overlap between the
    // various kinds of handles.

    // Example #1: No matter what type of handle you request, rulers
    // have exactly two handles, at the two ends. (Though a SELECTION
    // handle in the middle might be a good idea!)

    // Example #2: For a linear fit (not yet implemented), the
    // SELECTION handles might be the ends of the fit lines, and the
    // CONTROL_POINT handles would be the data that the fit is
    // generated from, so the two sets of handles might have zero
    // overlap.

    // Example #3: Labels have two SELECTION handles, one at the
    // assigned anchor and one in the middle (if the anchor is not in
    // the middle), but only one MOVE handle, since moving one handle
    // also moves the other one.

    static enum Type { CONTROL_POINT, SELECTION };

    DecorationHandle moveHandle(double dx, double dy);

    /** Copy this selection, placing the copy at dest. Return the
        SelectionHandle object that represents the copy. */
    DecorationHandle copy(double dx, double dy);
    @JsonIgnore Point2D.Double getLocation();

    /**
     * Assuming other is a transformed version of getDecoration(),
     * return the corresponding handle for that decoration, or null if
     * not applicable. */
    DecorationHandle copyFor(Decoration other);

    /**
     * Return the locations where this handle would be if
     * transform(xform) were called on its underlying decoration.
     * Depending on the decoration, it may be computable quickly as
     * xform.transform(getLocation(), ...) or it may require actually
     * transforming the decoration. */
    Point2D.Double getLocation(AffineTransform xform);
    @JsonIgnore Decoration getDecoration();

    /**
     * Implementation of getLocation(xform) that works if transforming
     * the handle location has the same effect as transforming the
     * decoration. */
    static Point2D.Double simpleLocation(DecorationHandle hand,
            AffineTransform xform) {
        Point2D.Double res = hand.getLocation();
        if (res == null) {
            return null;
        }
        xform.transform(res, res);
        return res;
    }

    static Point2D.Double slowLocation(DecorationHandle hand,
            AffineTransform xform) {
        hand = hand.copyFor(hand.getDecoration().createTransformed(xform));
        return (hand == null) ? null : hand.getLocation();
    }
}
