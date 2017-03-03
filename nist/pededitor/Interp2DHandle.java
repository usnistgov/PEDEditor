/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Class for a handle (control point) of a PointsDecoration. */
public class Interp2DHandle implements BoundedParam2DHandle {
    Interp2DDecoration decoration;
    int index; // index of control point this handle controls


    public Interp2DHandle(Interp2DDecoration decoration,
            int index) {
        this.decoration = decoration;
        this.index = index;
    }

    @Override public Interp2DHandle moveHandle(double dx, double dy) {
        return getDecoration().move(this, dx, dy);
    }

    /** Copy this selection, placing the copy at dest. Return the
        SelectionHandle object that represents the copy. */
    @Override public Interp2DHandle copy(double dx, double dy) {
        Interp2DDecoration dec = (Interp2DDecoration) decoration
                .createTransformed(AffineTransform.getTranslateInstance(dx, dy));
        return new Interp2DHandle(dec, index);
    }
        
    @Override public Point2D.Double getLocation() {
        return decoration.getCurve().get(index);
    }
    
    @Override public Interp2DDecoration getDecoration() {
        return decoration;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Interp2DHandle cast = (Interp2DHandle) other;
        return index == cast.index
            && getDecoration().equals(cast.getDecoration());
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[" + getDecoration() + ", "
            + index + "]";
    }

    @Override @JsonIgnore public double getT() {
        return index;
    }

    @Override @JsonIgnore public BoundedParam2D getParameterization() {
        return getDecoration().getParameterization();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int i) {
        index = i;
    }

    public Interp2D getCurve() {
        return getDecoration().getCurve();
    }

    @Override
    public DecorationHandle copyFor(Decoration other) {
        return ((Interp2DDecoration) other).createHandle(index);
    }

    @Override
    public Double getLocation(AffineTransform xform) {
        return DecorationHandle.simpleLocation(this,  xform);
    }
}
