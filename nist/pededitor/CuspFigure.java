/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/** A class for pairing a CuspInterp2D with its color, stroke, fill,
    and/or line width. */
public class CuspFigure implements BoundedParameterizable2D, Decorated {
    CuspInterp2D curve;
    protected StandardStroke stroke = null;
    protected Color color = null;
    protected double lineWidth = 1.0;

    /** Used only during deserialization of the old PED format. */
    protected String jsonShape = null;
    protected boolean jsonClosed = false;

    /** Only closed curves can be filled. */
    protected StandardFill fill = null;

    static private int minFreeId = 0;
    /** Used only during serialization and deserialization. */
    protected int jsonId = -1;

    /** Set the line width for this polyline. The StandardStroke may
        further modify the chosen line width further for some or all
        of the BasicStroke elements (for example, railroad ties tend
        to be much wider than the basic line width). */
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    public double getLineWidth() {
        return lineWidth;
    }

    @JsonIgnore public boolean isFilled() {
        return fill != null;
    }

    @JsonIgnore public boolean isDegenerate() {
        return size() < 1 || (isFilled() && size() <= 2);
    }

    @JsonProperty("id") int getJSONId() {
        if (jsonId == -1) {
            jsonId = minFreeId;
            ++minFreeId;
        }
        return jsonId;
    }

    @JsonProperty("id") void setJSONId(int id) {
        if (id >= minFreeId) {
            minFreeId = id + 1;
        }

        jsonId = id;
    }

    /** Used only for deserialization of the old PED format. */
    @JsonProperty("shape") void setJsonShape(String shape) {
        if (curve != null) {
            int s = curve.size();
            boolean smoothed = shape.equals("cubic spline");
            for (int i = 0; i < s; ++i) {
                curve.setSmoothed(i, smoothed);
            }
        } else {
            jsonShape = shape;
        }
    }

    /** Used only for deserializing an obsolete version of the JSON
        PED format. */
    @JsonProperty("points") void setPoints(Point2D.Double[] points) {
        setCurve(new CuspInterp2D(points, "cubic spline".equals(jsonShape),
                                  jsonClosed));
        jsonShape = null;
        jsonClosed = false;
    }

    public CuspFigure() { }
    public CuspFigure(CuspInterp2D curve) {
        this.curve = curve;
    }

    public CuspFigure(CuspInterp2D curve,
                      StandardStroke stroke) {
        this.curve = curve;
        this.stroke = stroke;
    }

    public CuspFigure(CuspInterp2D curve,
                           StandardStroke stroke,
                           double lineWidth) {
        this(curve, stroke);
        this.lineWidth = lineWidth;
    }

    public CuspFigure(CuspInterp2D curve, StandardFill fill) {
        this.curve = curve;
        this.fill = fill;
    }

    public void draw(Graphics2D g, boolean round) {
        draw(g, getLineWidth(), round);
    }

    public void draw(Graphics2D g, double lineWidth, boolean round) {
        StandardFill fill = getFill();
        if (fill != null) {
            curve.fill(g, getPaint());
        } else {
            Color oldColor = null;
            Color color = getColor();
            if (color != null) {
                oldColor  = g.getColor();
                g.setColor(color);
            }
            curve.draw(g, getStroke(), lineWidth, getColor(), round);
            if (oldColor != null) {
                g.setColor(oldColor);
            }
        }
    }

    /** @return null unless this polyline has been assigned a
        stroke. */
    @JsonProperty("lineStyle") public StandardStroke getStroke() {
        return stroke;
    }

    /** @return null unless this polyline has been assigned a fill. */
    @JsonProperty("fillStyle") public StandardFill getFill() {
        return fill;
    }

    /** @return a copy of "stroke" with its line width and dash
        pattern lengths scaled by a factor of "scaled". */
    public static BasicStroke scaledStroke(BasicStroke stroke, double scaled) {
        float scale = (float) scaled;
        float[] dashes = stroke.getDashArray();

        if (dashes != null) {
            dashes = (float[]) dashes.clone();
            for (int i = 0; i < dashes.length; ++i) {
                dashes[i] *= scale;
            }
        }
        return new BasicStroke(stroke.getLineWidth() * scale,
                               stroke.getEndCap(), stroke.getLineJoin(),
                               stroke.getMiterLimit(), dashes,
                               stroke.getDashPhase() * scale);
    }

    /** @return null unless this polyline has been assigned a
        color. */
    public Color getColor() {
        return color;
    }

    /** Set the color. Use null to indicate that the color should be
        the same as whatever was last chosen for the graphics
        context. */
    public void setColor(Color color) {
        this.color = color;
    }

    /** Set the stroke. If not null, this unsets the fill. */
    @JsonProperty("lineStyle") public void setStroke(StandardStroke stroke) {
        this.stroke = stroke;
        if (stroke != null) {
            this.fill = null;
        }
    }

    /** Set the fill. If not null, this unsets the stroke. */
    @JsonProperty("fillStyle") public void setFill(StandardFill fill) {
        this.fill = fill;
        if (fill != null) {
            this.stroke = null;
        }
    }

    @JsonIgnore public Paint getPaint() {
        Color c = getColor();
        if (c == null) {
            c = Color.BLACK;
        }
        return getFill().getPaint(c, 1.0);
    }

    public String toJSONString() {
        try {
            return getClass().getCanonicalName()
                + (new ObjectMapper()).writeValueAsString(this);
        } catch (Exception e) {
            System.err.println(e);
            return getClass().getCanonicalName() + "[ERROR]";
        }
    }

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
            ("  stroke: " + getStroke()
             + " lineWidth: " + getLineWidth());
        if (getColor() != null) {
            res.append("  color: " + getColor());
        }
        res.append("]");
        return res.toString();
    }

    @JsonIgnore public boolean isClosed() {
        return curve.isClosed();
    }

    @JsonIgnore public void setClosed(boolean closed) {
        curve.setClosed(closed);
    }

    /** Modifications to this method's return value will modify this
        object. I don't see a good alternative to that. */
    public CuspInterp2D getCurve() { return curve; }
    /** Future modifications to the object passed in will modify this
        object. I don't see a good alternative to that,
        performance-wise. */
    public void setCurve(CuspInterp2D curve) { this.curve = curve; }

    public CuspFigure createSmoothed(boolean smoothed) {
        CuspFigure res = new CuspFigure
            (new CuspInterp2D(curve.getPoints(), smoothed, curve.isClosed()),
             getStroke(), getLineWidth());
        res.setFill(getFill());
        return res;
    }

    public CuspFigure createTransformed(AffineTransform xform) {
        CuspFigure res = new CuspFigure
            (curve.createTransformed(xform));
        res.setFill(getFill());
        res.setStroke(getStroke());
        res.setLineWidth(getLineWidth());
        res.setColor(getColor());
        return res;
    }

    @JsonIgnore public int getSegmentCnt() { 
        return curve.getSegmentCnt();
    }

    // Duplicates of some of the curve's methods
    final public int size() { return curve.size(); }
    final public void add(Point2D point) { curve.add(point); }
    final public void remove() { curve.remove(); }
    final public void add(int index, Point2D point) { curve.add(index, point); }
    final public void set(int index, Point2D point) { curve.set(index, point); }
    final public void remove(int index) { curve.remove(index); }
    final public Point2D.Double get(int index) { return curve.get(index); }
    @JsonIgnore final public Point2D.Double[] getPoints() {
        return curve.getPoints();
    }
    final public Point2D.Double getLocation(double d) { 
        return curve.getLocation(d);
    }
    @JsonIgnore final public Path2D.Double getPath() { return curve.getPath(); }
    @Override @JsonIgnore
    final public BoundedParam2D getParameterization() {
        return curve.getParameterization();
    }
    final public Point2D.Double[] segIntersections(Line2D segment) {
        return curve.segIntersections(segment);
    }
    final public Point2D.Double[] lineIntersections(Line2D segment) {
        return curve.lineIntersections(segment);
    }

    /** For testing purposes only; could be safely deleted. */
    public static void main(String[] args) {
        String filename = "/eb/polyline-test.json";

        Point2D[] points1 = new Point2D[]
            { new Point2D.Double(3.1, 5.7),
              new Point2D.Double(0.0, 0.1) };
        Point2D[] points2 = new Point2D[]
            { points1[0], points1[1],
              new Point2D.Double(4.5, 1.2),
              new Point2D.Double(9.1, 10.1) };

        CuspInterp2D pol = new CuspInterp2D
                (points2,
                 new boolean[]
                 { true, true, false, true },
                 true);
        
        CuspFigure o = new CuspFigure(pol, null, 1.3);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(filename), o);
            CuspFigure o2 = mapper.readValue(new File(filename),
                                               CuspFigure.class);
            System.out.println(o2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
