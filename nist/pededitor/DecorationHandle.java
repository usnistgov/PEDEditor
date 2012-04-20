package gov.nist.pededitor;

import java.awt.geom.*;

/** Interface for the combination of a Decoration and a point. This is needed for many operations; for example, if you are going to move a rectangle from one spot to another, you need to know which point 

Interface that permits common operations to be applied to
    different kinds of graphics objects */
public interface DecorationHandle {
    /** @return the new SelectionHandle that should result from the removal
        of this object, or null if the removal of this object should
        leave the current selection empty. */
    DecorationHandle remove();

    void move(Point2D dest);

    /** Copy this selection, placing the copy at dest. Return the
        SelectionHandle object that represents the copy. */
    DecorationHandle copy(Point2D dest);
    Point2D.Double getLocation();
    boolean isEditable();
    void edit();
    Decoration getDecoration();
}
