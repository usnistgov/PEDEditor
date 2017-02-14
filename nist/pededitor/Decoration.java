/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

interface Decoration {
    default void draw(Graphics2D g) { draw(g, 1.0); };
    void draw(Graphics2D g, double scale);

    default void draw(Graphics2D g, AffineTransform xform, double scale) {
        createTransformed(xform).draw(g, scale);
    }

    default void setLineWidth(double lineWidth) {}
    default double getLineWidth() { return 0; }
    default void setLineStyle(StandardStroke lineStyle) {}
    default StandardStroke getLineStyle() { return null; }
    
    Color getColor();
    void setColor(Color color);
    DecorationHandle[] getHandles(DecorationHandle.Type type);

    /** Make the necessary changes to perform a reflection on this
        Decoration, EXCLUDING the transform of the movement handles.  */
    default void reflect() {}

    /** Change this diagram to make it look nicer on the page after
        the toPage transform is performed. Page Space is (x, y) where
        y increases as you go down the page. For example, the
        decoration might be changed so that upside down and backwards
        text is put right side up. */
    default void neaten(AffineTransform toPage) {}

    /** createTransformed() transforms the control points (and
        possibly angle for Angled decorations), which normally has a
        different effect from just transforming the output image. */
    void transform(AffineTransform xform);

    /** Like transform(), but creates a new object instead of overwriting this one. */
    Decoration createTransformed(AffineTransform xform);

    default List<Decoration> requiredDecorations() {
        return new ArrayList<Decoration>();
    }

    String typeName();
}
