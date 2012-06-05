package gov.nist.pededitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

// The annotations below for deserializing this GeneralPolyline into
// its appropriate subtype were recommended on Programmer Bruce's
// blog, "Deserialize JSON with Jackson into Polymorphic Types". -- EB

/** A class for pairing the anchor points of a possibly smoothed
    polyline with its associated color, StandardStroke, and the line
    width multiplier to use with the StandardStroke. */
@JsonTypeInfo(
              use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "shape")
@JsonSubTypes({
        @Type(value=Polyline.class, name = "polyline"),
            @Type(value=SplinePolyline.class, name = "cubic spline") })
public abstract class GeneralPolyline implements Parameterizable2D {
    protected ArrayList<Point2D.Double> points;
    protected StandardStroke stroke = null;
    protected Color color = null;
    protected double lineWidth = 1.0;

    /** Only closed curves can be filled. */
    protected boolean filled = false;

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

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public boolean isFilled() {
        return filled;
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

    public GeneralPolyline() {
        points = new ArrayList<Point2D.Double>();
    }

    public GeneralPolyline(Point2D[] points,
                           StandardStroke stroke,
                           double lineWidth) {
        this.points = new ArrayList<Point2D.Double>(points.length);
        for (Point2D p: points) {
            this.points.add(new Point2D.Double(p.getX(), p.getY()));
        }
        this.stroke = stroke;
        this.lineWidth = lineWidth;
    }

    /** @return a new GeneralPolyline of the given type. */
    public static GeneralPolyline create
        (int smoothingType, Point2D.Double[] points,
         StandardStroke stroke, double lineWidth) {
        switch (smoothingType) {
        case LINEAR:
            return new Polyline(points, stroke, lineWidth);
        case CUBIC_SPLINE:
            return new SplinePolyline(points, stroke, lineWidth);
        default:
            throw new IllegalArgumentException
                ("Unknown smoothingType value " + smoothingType);
        }
    }

    /** @return a new GeneralPolyline that is almost a clone of this,
        but whose smoothingType is as given. */
    public GeneralPolyline nearClone(int smoothingType) {
        GeneralPolyline output
            = create(smoothingType, getPoints(),
                     getStroke(), getLineWidth());
        output.setColor(getColor());
        output.setClosed(isClosed());
        return output;
    }

    public GeneralPolyline clone() {
        GeneralPolyline output
            = create(getSmoothingType(), getPoints(),
                     getStroke(), getLineWidth());
        output.setColor(getColor());
        output.setClosed(isClosed());
        return output;
    }

    /** @return a new GeneralPolyline that is like this one, but xform
        has been applied to its control points. Note that the smooth
        of the transform is generally not the same as the transform of
        the smoothing. */
    public GeneralPolyline createTransformed(AffineTransform xform) {
        GeneralPolyline output = clone();
        Point2D.Double[] points = getPoints();

        for (Point2D.Double point: points) {
            xform.transform(point, point);
        }
        output.setPoints(Arrays.asList(points));
        return output;
    }

    /** @return this's corresponding Path2D.Double. */
    @JsonIgnore
    abstract public Path2D.Double getPath();

    /** @return a Path2D.Double that corresponds to this polyline's
        coordinates with the given transformation applied afterwards.
        If you are using logical coordinates that are not merely a
        rotation or uniform (along both axes) rescaling of the image
        you wish to see, then in order to obtain appropriate line
        widths, transform the path using this method in preference to
        applying the transform() method to your Graphics2D object, and
        define your CompositeStroke line width to fit the dimensions of
        the transformed space. */
    abstract public Path2D.Double getPath(AffineTransform at);

    /** @return an array of all intersections between segment and
        this. */
    public Point2D.Double[] segIntersections(Line2D segment) {
        Parameterization2D c = getParameterization();
        double[] ts = c.segIntersections(segment);
        Point2D.Double[] output = new Point2D.Double[ts.length];
        for (int i = 0; i < ts.length; ++i) {
            output[i] = c.getLocation(ts[i]);
        }
        return output;
    }

    /** @return an array of all intersections between segment and
        this. */
    public Point2D.Double[] lineIntersections(Line2D segment) {
        Parameterization2D c = getParameterization();
        double[] ts = c.lineIntersections(segment);
        Point2D.Double[] output = new Point2D.Double[ts.length];
        for (int i = 0; i < ts.length; ++i) {
            output[i] = c.getLocation(ts[i]);
        }
        return output;
    }

    /** @return either GeneralPolyline.LINEAR or
        GeneralPolyline.CUBIC_SPLINE. */
    @JsonIgnore abstract public int getSmoothingType();

    public void draw(Graphics2D g) {
        draw(g, getPath());
    }

    public void draw(Graphics2D g, Path2D path) {
        draw(g, path, lineWidth);
    }

    public void draw(Graphics2D g, Path2D path, double lineWidth) {
        Color oldColor = g.getColor();
        Color color = getColor();
        if (color != null) {
            g.setColor(color);
        }

        if (isClosed() && isFilled()) {
            g.fill(path);
        } else {
            if (lineWidth == 0) {
                throw new IllegalStateException("Zero line width");
            }
            stroke.getStroke().draw(g, path, lineWidth);
        }

        if (color != null) {
            g.setColor(oldColor);
        }
    }

    /* Do not alter the object returned by this method. Clone it if
       you need to make changes to a copy. */
    @Override @JsonIgnore
        public Parameterization2D getParameterization() {
        return new PathParam2D(getPath());
    }

    public Point2D.Double getLocation(double d) {
        return getParameterization().getLocation(d);
    }

    /** Draw the path of this GeneralPolyline. The coordinates for
        this path should be defined in the "Original" coordinate
        system, but line widths are defined with respect to the
        "SquarePixel" coordinate system. Also, the output is scaled by
        "scale" before being drawn.
    */
    public void draw(Graphics2D g,
                     AffineTransform originalToSquarePixel,
                     double scale) {
        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        xform.concatenate(originalToSquarePixel);
        draw(g, getPath(xform), scale * lineWidth);
    }


    /** Draw the path of this GeneralPolyline. The output is scaled by
        "scale" before being drawn.
    */
    public void draw(Graphics2D g, double scale) {
        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        draw(g, getPath(xform), scale * lineWidth);
    }

    public boolean isClosed() {
        return false;
    }

    abstract public void setClosed(boolean closed);

    /** @return null unless this polyline has been assigned a
        stroke. */
    @JsonProperty("lineStyle") public StandardStroke getStroke() {
        return stroke;
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

    /** Set the stroke. Use null to indicate that the stroke should be
        the same as whatever was last chosen for the graphics
        context. */
    @JsonProperty("lineStyle") public void setStroke(StandardStroke stroke) {
        this.stroke = stroke;
    }

    public Point2D.Double[] getPoints() {
        return Duh.deepCopy(points.toArray(new Point2D.Double[0]));
    }

    public void setPoints(Collection<Point2D.Double> points) {
        this.points = new ArrayList<Point2D.Double>
            (Arrays.asList(Duh.deepCopy(points.toArray
                                        (new Point2D.Double[0]))));
    }

    /* Return control point #i. For closed curves, control points may
       be recounted (such as the 0th point being counted as the next
       one after the last). */
    public Point2D.Double get(int vertexNo) {
        return (Point2D.Double) points.get(vertexNo % points.size()).clone();
    }

    /** Add the point to the end of the polyline. */
    public void add(Point2D point) {
        add(points.size() - 1, point);
    }

    /** Add the point to the polyline in the given position. */
    public void add(int index, Point2D point) {
        points.add(index, new Point2D.Double(point.getX(), point.getY()));
    }

    /** Remove the last point added. */
    public void remove() {
        if (points.size() > 0) {
            points.remove(points.size()-1);
        }
    }

    /** Remove the given vertex. */
    public void remove(int vertexNo) {
        points.remove(vertexNo);
    }

    /* Parameterize the entire curve as t in [0,1] and return the
       SegmentAndT corresponding to the given t value */
    public SegmentAndT getSegment(double t) {
        int segCnt = getSegmentCnt();

        if (segCnt == 0) {
            return new SegmentAndT(0, 0.0);
        }

        if (t >= 1) {
            return new SegmentAndT(segCnt-1, 1.0);
        }

        t *= segCnt;
        double segment = Math.floor(t);

        return new SegmentAndT((int) segment, t - segment);
    }

    /** Replace the given vertex, which must exist. */
    public void set(int vertexNo, Point2D point) {
        points.set(vertexNo, new Point2D.Double(point.getX(), point.getY()));
    }

    /* Return the number of control points without duplication (so for
       closed curves, the return trip to point #0 does not count). */
    public int size() {
        return points.size();
    }

    /** @return the number of segments in this drawing. That equals
        the number of vertices minus 1 for open curves and closed
        curves with just 1 vertex, or the number of vertices for
        closed curves with at least 2 vertices. */
    @JsonIgnore
    public int getSegmentCnt() {
        int size = points.size();
        return (size >= 2 && isClosed()) ? size : (size - 1);
    }

    /** Return the point where this polyline starts. */
    @JsonIgnore public Point2D.Double getStart() {
        return (points.size() == 0) ? null
            : (Point2D.Double) points.get(0).clone();
    }

    /** Return the point where this polyline ends. Closed curves
        end where they start. */
    @JsonIgnore public Point2D.Double getEnd() {
        if (isClosed()) {
            return getStart();
        }

        if (points.size() == 0) {
            return null;
        }

        return (Point2D.Double) points.get(points.size() - 1).clone();
    }

    public Point2D.Double getLocation(int i) {
        return points.get(i % points.size());
    }


    @Override
    public String toString() {
        try {
            return getClass().getCanonicalName()
                + (new ObjectMapper()).writeValueAsString(this);
        } catch (Exception e) {
            System.err.println(e);
            return getClass().getCanonicalName() + "[ERROR]";
        }
    }

    public static final int LINEAR = 0;
    public static final int CUBIC_SPLINE = 1;

    /** For testing purposes only; could be safely deleted. */
    public static void main(String[] args) {
        String filename = "/eb/polyline-test.json";

        GeneralPolyline o = new Polyline
            (new Point2D.Double[] { new Point2D.Double(3.1, 5.7),
                                    new Point2D.Double(0.0, 0.1) },
                null, 1.3);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(filename), o);
            GeneralPolyline o2 = mapper.readValue(new File(filename),
                                               GeneralPolyline.class);
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
