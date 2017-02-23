/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

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

    @Override default Interp2DHandle[] getHandles(DecorationHandle.Type type) {
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
    default Interp2DHandle move(Interp2DHandle handle, Point2D dest) {
        getCurve().set(handle.index, dest);
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

    default Interp2DHandle nearestHandle(double t,
            BooleanHolder beforeThisHandle) {
        double handleT = getParameterization().getNearestVertex(t);
        if (beforeThisHandle != null) {
            // We chose the lesser side of this handle if t <
            // handleT. But if we grabbed the 0th vertex exactly, we
            // probably want the lesser side, since extending the
            // curve is more common than inserting points inside it.
            beforeThisHandle.value = (t < handleT)
                || (t == 0 && getCurve().size() >= 2);
        }
        int index = getCurve().tToIndex(handleT);
        if (getCurve().isClosed()) {
            index = index % getCurve().size();
        }
        return new Interp2DHandle(this, index);
    }
}
