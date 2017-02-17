/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/** Interp2DDecorationAdapter with a field to store an Interp2D. */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class DecorationHasInterp2D extends Interp2DDecorationAdapter {
    Interp2D curve = null;

    public DecorationHasInterp2D() { }
    public DecorationHasInterp2D(Interp2D curve) {
        this.curve = curve;
    }

    public DecorationHasInterp2D(Interp2D curve,
            StandardStroke stroke) {
        super(stroke);
        this.curve = curve;
    }

    public DecorationHasInterp2D(Interp2D curve, StandardStroke stroke,
            double lineWidth) {
        super(stroke, lineWidth);
        this.curve = curve;
    }

    public DecorationHasInterp2D(Interp2D curve, StandardFill fill) {
        super(fill);
        this.curve = curve;
    }
    
    @Override public BoundedParam2D getParameterization(
            AffineTransform xform) {
        return getCurve().getParameterization(xform);
    }

    @JsonIgnore public boolean isClosed() {
        return getCurve().isClosed();
    }

    @JsonIgnore public void setClosed(boolean closed) {
        getCurve().setClosed(closed);
    }

    /** Modifications to this method's return value will modify this
        object. I don't see a good alternative to that. */
    @Override public Interp2D getCurve() { return curve; }
    
    /** Future modifications to the object passed in will modify this
        object. I don't see a good alternative to that,
        performance-wise. */
    public void setCurve(Interp2D curve) { this.curve = curve; }

    @Override public String toString() {
        StringBuilder res = new StringBuilder
            (getClass().getSimpleName() + "[\n");
        if (getCurve() != null) {
            res.append("  curve: " + getCurve() + "\n");
        }
        if (getFill() != null) {
            res.append("  fill: " + getFill() + "\n");
        }
        res.append
            ("  lineStyle: " + getLineStyle()
             + " lineWidth: " + getLineWidth());
        if (getColor() != null) {
            res.append("  color: " + getColor());
        }
        res.append("]");
        return res.toString();
    }

    @Override void copyFrom(Interp2DDecoration other) {
        setCurve(other.getCurve().clone());
        super.copyFrom(other);
    }

    @Override public void transform(AffineTransform xform) {
        getCurve().transform(xform);
    }
    
    @JsonIgnore @Override public BoundedParam2D getParameterization() {
        return getCurve().getParameterization();
    }

    @Override public DecorationHasInterp2D clone() {
        DecorationHasInterp2D res = new DecorationHasInterp2D();
        res.copyFrom(this);
        return res;
    }
    @Override public String typeName() {
        return "generic curve";
    }
}
