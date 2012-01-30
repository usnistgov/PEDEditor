package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import org.codehaus.jackson.annotate.JsonIgnore;

/** Specialization of GeneralPolyline for open cubic splines. */
public class SplinePolyline extends GeneralPolyline {
    private boolean closed = false;

    /** Reset spline to null whenever modifying this. */
    protected CubicSpline2D spline = null;

    public SplinePolyline() {
    }

    public SplinePolyline(Point2D.Double[] points,
                          StandardStroke stroke,
                          double lineWidth) {
        super(points, stroke, lineWidth);
    }

    @Override public void set(int vertexNo, Point2D point) {
        spline = null;
        points.set(vertexNo, new Point2D.Double(point.getX(), point.getY()));
    }

    @Override public void setClosed(boolean closed) {
        if (closed != this.closed) {
            this.closed = closed;
            spline = null;
        }
    }

    @Override public boolean isClosed() {
        return closed;
    }

    @Override
    public Path2D.Double getPath() {
        return getSpline().path();
    }

    @JsonIgnore public CubicSpline2D getSpline() {
        if (spline == null) {
            spline = new CubicSpline2D(points.toArray(new Point2D.Double[0]),
                                       isClosed());
        }
        return spline;
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

    @Override public double[] segmentIntersectionTs(Line2D segment) {
        return getSpline().intersectionTs(segment);
    }

    /** @return an array of all intersections between segment and
        this. */
    @Override public Point2D.Double[] segmentIntersections(Line2D segment) {
        return getSpline().intersections(segment);
    }

    @Override public double[] lineIntersectionTs(Line2D line) {
        return getSpline().lineIntersectionTs(line);
    }

    /** @return an array of all intersections between line and
        this. */
    @Override public Point2D.Double[] lineIntersections(Line2D line) {
        return getSpline().lineIntersections(line);
    }

    @Override
    public Path2D.Double getPath(AffineTransform at) {
        return getSpline(at).path();
    }

    @Override
    public int getSmoothingType() {
        return GeneralPolyline.CUBIC_SPLINE;
    }

    @Override
    public Point2D.Double getGradient(int segmentNo, double t) {
        return getSpline().gradient(segmentNo, t);
    }

    @Override
    public Point2D.Double getLocation(int segmentNo, double t) {
        if (points.size() == 0) {
            return null;
        }

        return getSpline().value(segmentNo, t);
    }

    @Override
    public Point2D.Double getLocation(double t) {
        return getSpline().value(t);
    }

    @Override
    public CurveDistance distance(Point2D p) {
        return getSpline().curveDistance(p, 1e-9, 200);
    }
}
