/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

interface Interp2DDecoration extends Decoration,
                                     TransformableParameterizable2D, HasJSONId {
    Interp2D getCurve();
    
    default Interp2DHandle createHandle(int index) {
        return new Interp2DHandle(this, index);
    }

    @JsonProperty("fillStyle") void setFill(StandardFill fill)
        throws UnsupportedOperationException;

    /** @return null unless this has been assigned a fill. */
    @JsonProperty("fillStyle") StandardFill getFill();

    @JsonIgnore boolean isFilled();

    @Override Interp2DDecoration createTransformed(AffineTransform xform);

    /** Return true if this object is invisible. */
    @Override @JsonIgnore default boolean isDegenerate() {
        return getCurve().size() < 1 || (isFilled() && getCurve().size() <= 2);
    }

    @Override default DecorationHandle[] getHandles(DecorationHandle.Type type) {
        int size = getCurve().size();
        Interp2DHandle[] res = new Interp2DHandle[size];
        for (int j = 0; j < size; ++j) {
            res[j] = createHandle(j);
        }
        return res;
    }

    /** This should not usually be called directly. It exists to allow
        Interp2DHandle to respond polymorphically to
        move(). */
    default Interp2DHandle move(Interp2DHandle handle, double dx, double dy) {
        Point2D.Double p = getCurve().get(handle.index);
        getCurve().set(handle.index, new Point2D.Double(p.x + dx, p.y + dy));
        return handle;
    }

    /** Use true for points that look like circles and for rounded
     * caps and mitres. Use False for points that look like squares
     * and for angular caps and mitres. */
    @JsonIgnore default void setRoundedStroke(boolean b) {}
    @JsonIgnore default boolean isRoundedStroke() {return false; }

    default @Override void draw(Graphics2D g, double scale) {
        Interp2DDecoration dt = createTransformed(
                AffineTransform.getScaleInstance(scale, scale));
        dt.setLineWidth(dt.getLineWidth() * scale);
        dt.draw(g);
    }

    default @Override void draw(Graphics2D g, AffineTransform xform,
        double scale) {
        // Combine two transforms into one to avoid egregious time-wasting.
        AffineTransform xform2 = AffineTransform.getScaleInstance(scale,
                scale);
        xform2.concatenate(xform);
        Interp2DDecoration dt = createTransformed(xform2);
        dt.setLineWidth(dt.getLineWidth() * scale);
        dt.draw(g);
    }

    @Override default void draw(Graphics2D g) {
        boolean round = isRoundedStroke();
        StandardFill fill = getFill();
        if (fill != null) {
            Color c = getColor();
            if (c == null) {
                c = Color.BLACK;
            }
            getCurve().fill(g, getFill().getPaint(c, 1.0));
        } else {
            Color oldColor = null;
            Color color = getColor();
            if (color != null) {
                oldColor  = g.getColor();
                g.setColor(color);
            }
            getCurve().draw(g, getLineStyle(), getLineWidth(), getColor(), round);
            if (oldColor != null) {
                g.setColor(oldColor);
            }
        }
    }

    default String toJSONString() {
        try {
            return getClass().getCanonicalName()
                + (new ObjectMapper()).writeValueAsString(this);
        } catch (Exception e) {
            System.err.println(e);
            return getClass().getCanonicalName() + "[ERROR]";
        }
    }
    
    @Override Interp2DDecoration clone();
}
