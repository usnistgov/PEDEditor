/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public interface Decoration {
    void draw(Graphics2D g, double scale);
    /** @return the new SelectionHandle that should result from the removal
        of this object, or null if the removal of this object should
        leave the current selection empty. */
    DecorationHandle remove();
    void setLineWidth(double lineWidth);
    double getLineWidth();
    void setLineStyle(StandardStroke lineStyle);
    StandardStroke getLineStyle();
    Color getColor();
    void setColor(Color color);
    DecorationHandle[] getHandles();

    /** Make the necessary changes to perform a reflection on this
        Decoration, EXCLUDING the transform of the movement handles.  */
    default void reflect() {}

    /** Change this diagram to make it look nicer on the page after
        the toPage transform is performed. Page Space is (x, y) where
        y increases as you go down the page. For example, the
        decoration might be changed so that upside down and backwards
        text is put right right side up. */
    default void neaten(AffineTransform toPage) {}

    /* getMovementHandles() returns a subset of the handles of this
     decoration that is sufficient to permit it to move. If this
     decoration is defined in terms of positions on another
     decoration, then the returned list should be empty, because
     moving that other decoration will move this with it. If this
     decoration has a fixed orientation but many potential grabbing
     points, then the list should contain just one handle, since
     moving that one will move the others along with it. */
    DecorationHandle[] getMovementHandles();

    /** Used during serialization. Some Decorations are just an
        interface between an underlying object and the manipulating
        environment, and in that case, serializing the underlying
        object (as opposed to serializing the Decoration itself) is
        enough (and if the Decoration is an inner class, then Jackson
        can't deserialize the inner class directly anyhow). If that is
        NOT the case, then it's perfectly legitimate just to have
        getSerializationObject() return "this". */
    Decorated getSerializationObject();
}
