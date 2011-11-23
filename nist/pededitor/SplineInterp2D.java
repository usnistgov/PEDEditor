package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** Specialization of GeneralPolyline for open cubic splines. */
public class SplinePolyline extends GeneralPolyline {
    public SplinePolyline(Point2D.Double[] points, BasicStroke stroke) {
        super(points, stroke);
    }

    public Path2D.Double getPath() {
        return (new CubicSpline2D(points.toArray(new Point2D.Double[0]))).path();
    }

    public CubicSpline2D getSpline(AffineTransform at) {
        Point2D.Double[] xpoints = Duh.deepCopy
            (points.toArray(new Point2D.Double[0]));
        Point2D.Double xpt = new Point2D.Double();
        for (Point2D.Double point : xpoints) {
            at.transform(point, xpt);
            point.setLocation(xpt.x, xpt.y);
        }
        return new CubicSpline2D(xpoints);
    }

    public Path2D.Double getPath(AffineTransform at) {
        return getSpline(at).path();
    }

    public int getSmoothingType() {
        return GeneralPolyline.CUBIC_SPLINE;
    }
}
