/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;

/** A class for curves interpolated between control points. */
public abstract class Interp2D implements BoundedParameterizable2D {
    private boolean closed = false;
    protected transient BoundedParam2D param = null;

    /** @param closed If true, connect the last control point to the
        first control point. */
    public Interp2D(boolean closed) {
        this.closed = closed;
    }

    /** @return this's corresponding Path2D.Double. */
    @JsonIgnore abstract public Path2D.Double getPath();
    abstract public Point2D.Double[] getPoints();
    abstract public Interp2D createTransformed(AffineTransform xform);
    /* Return control point #i. For closed curves, control points may
       be recounted (such as the 0th point being counted as the next
       one after the last). */
    abstract public Point2D.Double get(int vertexNo);
    /** Add the point to the polyline in the given position. */
    abstract public void add(int index, Point2D point);
    /** Remove the given vertex. */
    abstract public void remove(int vertexNo);
    /** Replace the given vertex, which must exist. */
    abstract public void set(int vertexNo, Point2D point);
    /* Return the number of control points without duplication (so for
       closed curves, the return trip to point #0 does not count). */
    abstract public int size();

    public void draw(Graphics2D g, StandardStroke stroke, double lineWidth,
                     Color color, boolean round) {
        Color oldColor = g.getColor();
        if (color != null) {
            g.setColor(color);
        } else {
            color = g.getColor();
        }

        if (lineWidth == 0) {
            return;
        }
        if (stroke == null) {
            throw new IllegalArgumentException("draw(): null stroke in " + this);
        }
        stroke.getStroke().draw(g, getPath(), lineWidth, round);
        g.setColor(oldColor);
    }

    public void fill(Graphics2D g, Paint paint) {
        Paint oldPaint = null;
        try {
            oldPaint = g.getPaint();
            g.setPaint(paint);
            g.fill(getPath());
        } finally {
            g.setPaint(oldPaint);
        }
    }

    /** @return an array of all intersections between segment and
        this. */
    public Point2D.Double[] segIntersections(Line2D segment) {
        BoundedParam2D c = getParameterization();
        double[] ts = c.segIntersections(segment);
        Point2D.Double[] res = new Point2D.Double[ts.length];
        for (int i = 0; i < ts.length; ++i) {
            res[i] = c.getLocation(ts[i]);
        }
        return res;
    }

    /** @return an array of all intersections between segment and
        this. */
    public Point2D.Double[] lineIntersections(Line2D segment) {
        BoundedParam2D c = getParameterization();
        double[] ts = c.lineIntersections(segment);
        Point2D.Double[] res = new Point2D.Double[ts.length];
        for (int i = 0; i < ts.length; ++i) {
            res[i] = c.getLocation(ts[i]);
        }
        return res;
    }

    @Override @JsonIgnore public BoundedParam2D getParameterization() {
        if (param == null) {
            param = PathParam2D.create(getPath());
        }
        return param;
    }

    public Point2D.Double getLocation(double d) {
        return getParameterization().getLocation(d);
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        if (closed != this.closed) {
            this.closed = closed;
            param = null;
        }
    }

    /**  */
    public Point2D.Double[] transformPoints(AffineTransform xform) {
        int cnt = size();
        Point2D.Double[] res = new Point2D.Double[cnt];
        for (int i = 0; i < cnt; ++i) {
            res[i] = new Point2D.Double();
            xform.transform(get(i), res[i]);
        }
        return res;
    }

    /** Add the point to the end of the polyline. */
    public void add(Point2D point) {
        add(size(), point);
    }
    /** Remove the last point added. */
    public void remove() {
        remove(size() - 1);
    }

    /** @return the number of segments in this drawing. That equals
        the number of vertices minus 1 for open curves and closed
        curves with just 1 vertex, or the number of vertices for
        closed curves with at least 2 vertices. */
    @JsonIgnore public int getSegmentCnt() {
        int s = size();
        return (s >= 2 && isClosed()) ? s : (s - 1);
    }

    /** Return the point where this polyline starts. */
    @JsonIgnore public Point2D.Double getStart() {
        return (size() == 0) ? null
            : (Point2D.Double) get(0).clone();
    }

    /** Return the point where this polyline ends. Closed curves
        end where they start. */
    @JsonIgnore public Point2D.Double getEnd() {
        if (isClosed()) {
            return getStart();
        }

        int s = size();
        if (s == 0) {
            return null;
        }

        return (Point2D.Double) get(s-1).clone();
    }

    public Point2D.Double getLocation(int i) {
        return get(i % size());
    }

    @Override public String toString() {
        try {
            return getClass().getCanonicalName()
                + (new ObjectMapper()).writeValueAsString(this);
        } catch (Exception e) {
            System.err.println(e);
            return getClass().getCanonicalName() + "[ERROR]";
        }
    }
}
