/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.geom.AffineTransform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
abstract public class Interp2DDecorationAdapter implements Interp2DDecoration {
    protected double lineWidth = 1.0;
    protected StandardStroke stroke = null;
    /** Only closed curves can be filled. */
    protected StandardFill fill = null;
    protected Color color = null;
    protected boolean roundedStroke = false;

    public Interp2DDecorationAdapter() { }
    public Interp2DDecorationAdapter(StandardStroke stroke) {
        this.stroke = stroke;
    }

    public Interp2DDecorationAdapter(StandardStroke stroke,
            double lineWidth) {
        this(stroke);
        this.lineWidth = lineWidth;
    }

    public Interp2DDecorationAdapter(StandardFill fill) {
        this.fill = fill;
    }

    @Override abstract public Interp2DDecoration clone();
    @Override public Interp2DDecoration createTransformed(
            AffineTransform xform) {
        Interp2DDecoration res = clone();
        res.transform(xform);
        return res;
    }

    /** Set the line width. The StandardStroke may further modify the
        chosen line width -- for example, railroad ties tend to be
        much wider than the basic line width. */
    @Override public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    @Override public double getLineWidth() {
        return lineWidth;
    }

    /** Set the line style. If not null, this unsets the fill. */
    @Override public void setLineStyle(StandardStroke stroke) {
        this.stroke = stroke;
        if (stroke != null) {
            this.fill = null;
        }
    }

    /** Set the fill. If not null, this unsets the stroke. 
     * @throws UnsupportedOperationException */
    @Override public void setFill(StandardFill fill)
        throws UnsupportedOperationException {
        this.fill = fill;
        if (fill != null) {
            if (!getCurve().isClosed()) {
                throw new UnsupportedOperationException();
            }
            stroke = null;
        }
    }

    /** @return null unless this has been assigned a fill. */
    @Override public StandardFill getFill() {
        return fill;
    }

    @Override @JsonIgnore public boolean isFilled() {
        return fill != null;
    }

    /** @return null unless this has been assigned a line style. */
    @Override public StandardStroke getLineStyle() {
        return stroke;
    }

    /** Set the color. Use null to indicate that the color should be
        the same as whatever was last chosen for the graphics
        context. */
    @Override public void setColor(Color color) {
        this.color = color;
    }

    /** @return null unless this has been assigned a color. */
    @Override public Color getColor() {
        return color;
    }

    @JsonIgnore @Override public boolean isRoundedStroke() {
        return roundedStroke;
    }

    @JsonIgnore @Override public void setRoundedStroke(boolean b) {
        roundedStroke = b;
    }

    /** Used only during serialization and deserialization. */
    protected int jsonId = -1;

    @JsonProperty("id") @Override public int getJsonId() {
        if (jsonId == -1) {
            jsonId = IdGenerator.getInstance().id();
        }
        return jsonId;
    }

    @Override @JsonProperty("id") public void setJsonId(int id) {
        IdGenerator.getInstance().idInUse(id);
        jsonId = id;
    }

    void copyFrom(Interp2DDecoration other) {
        setFill(other.getFill());
        setLineStyle(other.getLineStyle());
        setLineWidth(other.getLineWidth());
        setColor(other.getColor());
        setRoundedStroke(other.isRoundedStroke());
    }
}
