package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Collection;

import org.codehaus.jackson.annotate.JsonIgnore;

/** Specialization of GeneralPolyline for open cubic splines. */
public class SplinePolyline extends GeneralPolyline {
    private boolean closed = false;

    /** The spline is cached for efficiency. This class and its
        subclasses should reset spline to null whenever a change
        invalidates the cached version. */
    protected transient CubicSpline2D spline = null;
    /** Param is also cached. */
    protected transient BoundedParam2D param = null;

    public SplinePolyline() {
    }

    public SplinePolyline(Point2D.Double[] points,
                          StandardStroke stroke,
                          double lineWidth) {
        super(points, stroke, lineWidth);
    }

    @Override public void set(int vertexNo, Point2D point) {
        param = null;
        spline = null;
        super.set(vertexNo, point);
    }

    @Override public void add(Point2D point) {
        param = null;
        spline = null;
        super.add(point);
    }

    /** Add the point to the polyline in the given position. */
    @Override public void add(int index, Point2D point) {
        param = null;
        spline = null;
        super.add(index, point);
    }

    /** Remove the last point added. */
    @Override public void remove() {
        param = null;
        spline = null;
        super.remove();
    }

    /** Remove the given vertex. */
    @Override public void remove(int vertexNo) {
        param = null;
        spline = null;
        super.remove(vertexNo);
    }

    @Override public void setPoints(Collection<Point2D.Double> points) {
        param = null;
        spline = null;
        super.setPoints(points);
    }

    @Override public void setClosed(boolean closed) {
        if (closed != this.closed) {
            this.closed = closed;
            param = null;
            spline = null;
        }
    }

    @Override public boolean isClosed() {
        return closed;
    }

    @Override public Path2D.Double getPath() {
        return getSpline().path();
    }

    @JsonIgnore public CubicSpline2D getSpline() {
        if (spline == null) {
            spline = new CubicSpline2D(points.toArray(new Point2D.Double[0]),
                                       isClosed());
            param = null;
        }
        return spline;
    }

    @Override public BoundedParam2D getParameterization() {
        if (param == null) {
            param = super.getParameterization();
        }
        return param;
    }

    public CubicSpline2D getSpline(AffineTransform at) {
        Point2D.Double[] xpoints = Duh.deepCopy
            (points.toArray(new Point2D.Double[0]));
        Point2D.Double xpt = new Point2D.Double();
        for (Point2D.Double point : xpoints) {
            at.transform(point, xpt);
            point.setLocation(xpt.x, xpt.y);
        }
        return new CubicSpline2D(xpoints, isClosed());
    }

    @Override public Path2D.Double getPath(AffineTransform at) {
        return getSpline(at).path();
    }

    @Override public int getSmoothingType() {
        return GeneralPolyline.CUBIC_SPLINE;
    }
}
