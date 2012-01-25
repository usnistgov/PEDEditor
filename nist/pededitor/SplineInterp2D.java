package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** Specialization of GeneralPolyline for open cubic splines. */
public class SplinePolyline extends GeneralPolyline {
    private boolean closed = false;

    public SplinePolyline() {
    }

    public SplinePolyline(Point2D.Double[] points,
                          StandardStroke stroke,
                          double lineWidth) {
        super(points, stroke, lineWidth);
    }

    @Override public void setClosed(boolean value) {
        closed = value;
    }

    @Override public boolean isClosed() {
        return closed;
    }

    @Override
    public Path2D.Double getPath() {
        return getSpline().path();
    }

    public CubicSpline2D getSpline() {
        return new CubicSpline2D(points.toArray(new Point2D.Double[0]),
                                 isClosed());
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
    public Point2D.Double[] segmentIntersections(Line2D segment) {
        return getSpline().intersections(segment);
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
        if (points.size() == 0) {
            return null;
        }

        return getSpline(new AffineTransform()).gradient(segmentNo, t);
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
}
