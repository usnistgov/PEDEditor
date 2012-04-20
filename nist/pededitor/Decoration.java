package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.*;

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
    Rectangle2D getBounds();
    Rectangle2D getBounds(AffineTransform xform);
    DecorationHandle[] getHandles();
}
