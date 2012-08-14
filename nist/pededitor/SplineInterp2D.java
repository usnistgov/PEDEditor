package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Collection;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/** Specialization of GeneralPol for open cubic splines. */
public class SplineInterp2D extends PointsInterp2D {
    /** The spline is cached for efficiency. This class and its
        subclasses should reset spline to null whenever a change
        invalidates the cached version. */
    protected transient CubicSpline2D spline = null;

    public SplineInterp2D(Point2D[] points,
                     @JsonProperty("closed") boolean closed) {
        super(points, closed);
    }

    // This constructor is redundant, but it allows me to tell Jackson
    // that the "points" field of the JSON file is filled with
    // Point2D.Doubles.
    public SplineInterp2D(@JsonProperty("points") Point2D.Double[] points,
                     @JsonProperty("closed") boolean closed) {
        super(points, closed);
    }

    @Override public void set(int vertexNo, Point2D point) {
        spline = null;
        super.set(vertexNo, point);
    }

    @Override public void add(Point2D point) {
        spline = null;
        super.add(point);
    }

    /** Add the point to the polyline in the given position. */
    @Override public void add(int index, Point2D point) {
        spline = null;
        super.add(index, point);
    }

    /** Remove the last point added. */
    @Override public void remove() {
        spline = null;
        super.remove();
    }

    /** Remove the given vertex. */
    @Override public void remove(int vertexNo) {
        spline = null;
        super.remove(vertexNo);
    }

    @Override public <T extends Point2D> void setPoints(Collection<T> points) {
        spline = null;
        super.setPoints(points);
    }

    @Override public void setClosed(boolean closed) {
        if (closed != isClosed()) {
            super.setClosed(closed);
            spline = null;
        }
    }

    @Override public Path2D.Double getPath() {
        return getSpline().path();
    }

    @JsonIgnore public CubicSpline2D getSpline() {
        if (spline == null) {
            spline = new CubicSpline2D(points.toArray(new Point2D.Double[0]),
                                       isClosed());
        }
        return spline;
    }

    @Override public SplineInterp2D createTransformed(AffineTransform xform) {
        return new SplineInterp2D(transformPoints(xform), isClosed());
    }
}
