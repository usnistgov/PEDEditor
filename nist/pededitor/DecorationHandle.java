/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Interface for the combination of a Decoration and a location on
    it. For example, to display a dragged object, you need to know
    both the object and where on the object you grabbed it. */
public interface DecorationHandle {
    /** @return the new DecorationHandle that should result from the
        removal of this object, or null if the removal of this object
        should leave the current selection empty. */
    DecorationHandle remove();

    DecorationHandle move(Point2D dest);

    /** Copy this selection, placing the copy at dest. Return the
        SelectionHandle object that represents the copy. */
    DecorationHandle copy(Point2D dest);
    Point2D.Double getLocation();
    Decoration getDecoration();
}
