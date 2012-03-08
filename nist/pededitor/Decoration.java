package gov.nist.pededitor;

import java.awt.Color;
import java.awt.geom.*;

/** Interface that permits common operations to be applied to
    different kinds of graphics objects */
public interface Selectable {
    /** @return the new selection that should result from the removal
        of this object, or null if the removal of this object should
        leave the current selection empty. */
    Selectable remove();
    void move(Point2D dest);

    /** Copy this selection, placing the copy at dest. Return the
        Selectable object that represents the copy. */
    Selectable copy(Point2D dest);
    Point2D.Double getLocation();
    boolean isEditable();
    void edit();
    void setLineWidth(double lineWidth);
    double getLineWidth();
    void setLineStyle(StandardStroke lineStyle);
    Color getColor();
    void setColor(Color color);
}
