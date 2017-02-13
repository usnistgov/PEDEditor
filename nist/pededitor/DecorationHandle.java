/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

import org.codehaus.jackson.annotate.JsonIgnore;

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

    static enum Type { CONTROL_POINT, SELECTION, MOVE };

    DecorationHandle move(Point2D dest);

    /** Copy this selection, placing the copy at dest. Return the
        SelectionHandle object that represents the copy. */
    DecorationHandle copy(Point2D dest);
    @JsonIgnore Point2D.Double getLocation();
    @JsonIgnore Decoration getDecoration();
}
