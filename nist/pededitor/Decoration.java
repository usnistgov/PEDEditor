package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;

/** Interface that permits common operations to be applied to
    different kinds of graphics objects */
public interface Decoration {
    void draw(Graphics2D g, double scale);
    /** @return the new SelectionHandle that should result from the removal
        of this object, or null if the removal of this object should
        leave the current selection empty. */
    DecorationHandle remove();
    void setLineWidth(double lineWidth);
    double getLineWidth();
    void setLineStyle(StandardStroke lineStyle);
    Color getColor();
    void setColor(Color color);
    DecorationHandle[] getHandles();

    /* getMovementHandles() returns a subset of the handles of this
     decoration that is sufficient to permit it to move. If this
     decoration is defined in terms of positions on another
     decoration, then the returned list should be empty, because
     moving that other decoration will move this with it. If this
     decoration has a fixed orientation but many potential grabbing
     points, then the list should contain just one handle, since
     moving that one will move the others along with it. */
    DecorationHandle[] getMovementHandles();
}
