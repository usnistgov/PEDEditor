package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** A class for pairing a possibly smoothed polyline with its
    associated BasicStroke. */
public abstract class GeneralPolyline {
    protected ArrayList<Point2D.Double> points;
    protected BasicStroke stroke = null;
    protected Color color = null;

    public GeneralPolyline() {
        points = new ArrayList<Point2D.Double>();
    }

    public GeneralPolyline(Point2D.Double[] points, BasicStroke stroke) {
        this.points = new ArrayList(Arrays.asList(points));
        this.stroke = stroke;
    }

    abstract public Path2D.Double getPath();
    abstract public int getSmoothingType();

    public void draw(Graphics2D g) {
        Color oldColor = g.getColor();
        Stroke oldStroke = g.getStroke();

        Color color = getColor();
        if (color != null) {
            g.setColor(color);
        }
        if (stroke != null) {
            g.setStroke(stroke);
        }
        g.draw(getPath());
        if (color != null) {
            g.setColor(oldColor);
        }
        if (stroke != null) {
            g.setStroke(oldStroke);
        }
    }

    public boolean isClosed() {
        return false;
    }

    /** @return null unless this polyline has been assigned a
        stroke. */
    public BasicStroke getStroke() {
        return stroke;
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
    public void setStroke(BasicStroke stroke) {
        this.stroke = stroke;
    }

    public Point2D.Double[] getPoints() {
        return points.toArray(new Point2D.Double[0]);
    }

    public void add(Point2D.Double point) {
        points.add(point);
    }

    /** Remove the last point added. */
    public void remove() {
        points.remove(points.size()-1);
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

    public static int LINEAR = 0;
    public static int CUBIC_SPLINE = 1;
}
