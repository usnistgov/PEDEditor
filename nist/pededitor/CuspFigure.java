package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** A class for pairing a possibly smoothed polyline with its
    associated BasicStroke. */
public abstract class GeneralPolyline {
    protected ArrayList<Point2D.Double> points;
    protected CompositeStroke stroke = null;
    protected Color color = null;
    protected double lineWidth = 1.0;

    /** Set the line width for this polyline. The CompositeStroke may
        further modify the chosen line width further for some or all
        of the BasicStroke elements (for example, railroad ties tend
        to be much wider than the basic line width). */
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    public double getLineWidth() {
        return lineWidth;
    }

    public GeneralPolyline() {
        points = new ArrayList<Point2D.Double>();
    }

    public GeneralPolyline(Point2D.Double[] points,
                           CompositeStroke stroke,
                           double lineWidth) {
        this.points = new ArrayList(Arrays.asList(points));
        this.stroke = stroke;
        this.lineWidth = lineWidth;
    }

    /** @return a new GeneralPolyline of the given type. */
    static public GeneralPolyline create
        (int smoothingType, Point2D.Double[] points,
         CompositeStroke stroke, double lineWidth) {
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

    /** @return this's corresponding Path2D.Double. */
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

    /** @return either GeneralPolyline.LINEAR or
        GeneralPolyline.CUBIC_SPLINE. */
    abstract public int getSmoothingType();

    public void draw(Graphics2D g) {
        draw(g, getPath());
    }

    public void draw(Graphics2D g, Path2D path) {
        Color oldColor = g.getColor();
        Color color = getColor();
        if (color != null) {
            g.setColor(color);
        }

        stroke.draw(g, path, lineWidth);

        if (color != null) {
            g.setColor(oldColor);
        }
    }


    /** draw the path of this GeneralPolyline. The coordinates for
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
        stroke.draw(g, getPath(xform), scale * lineWidth);
    }

    public boolean isClosed() {
        return false;
    }

    /** @return null unless this polyline has been assigned a
        stroke. */
    public CompositeStroke getStroke() {
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


    /** curveTo() seems to determine its linearization granularity
        without regard to the transform defined for the Graphics2D, or
        at least without regard to deviations from the default
        transform. This results in unacceptable image quality if the
        path is decribed in standardPage coordinates (which are in
        [0,1]) and then transformed into a much larger range such as
        [0,800].

        A possible work-around would be to blow up standardPage by the
        maximum of the 4 scaling and shear coefficients (to insure
        that curveTo's polyline approximation has sufficient
        resolution) and then correspondingly shrink the final
        transformation.
     */
    void draw(Graphics2D g, Path2D path, double strokeScale) {
        Color oldColor = g.getColor();
        Color color = getColor();
        if (color != null) {
            g.setColor(color);
        }

        stroke.draw(g, path, lineWidth * strokeScale);

        if (color != null) {
            g.setColor(oldColor);
        }
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
    public void setStroke(CompositeStroke stroke) {
        this.stroke = stroke;
    }

    public Point2D.Double[] getPoints() {
        return points.toArray(new Point2D.Double[0]);
    }

    public Point2D.Double get(int vertexNo) {
        return (Point2D.Double) points.get(vertexNo).clone();
    }

    /** Add the point to the end of the polyline. */
    public void add(Point2D.Double point) {
        points.add(point);
    }

    /** Add the point to the polyline in the given position. */
    public void add(int index, Point2D.Double point) {
        points.add(index, point);
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

    /** Replace the given vertex, which must exist. */
    public void set(int vertexNo, Point2D.Double point) {
        points.set(vertexNo, point);
    }

    public Point2D.Double tail() {
        int size = points.size();
        return (size == 0) ? null : points.get(size-1);
    }

    /** @return the number of segments in the line. */
    public int size() {
        return points.size();
    }

    public String toString(AffineTransform at) {
        return "Not implemented";
    }

    public static final int LINEAR = 0;
    public static final int CUBIC_SPLINE = 1;
}
