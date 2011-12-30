package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** Specialization of GeneralPolyline for vanilla polylines. */
public class Polyline extends GeneralPolyline {
    private boolean closed = false;

    public Polyline() {
    }

    public Polyline(Point2D.Double[] points,
                    CompositeStroke stroke,
                    double lineWidth) {
        super(points, stroke, lineWidth);
    }

    @Override public void setClosed(boolean value) {
        this.closed = value;
    }

    @Override
    public Path2D.Double getPath() {
        int size = points.size();
        Path2D.Double output = new Path2D.Double();
        if (size == 0) {
            return output;
        }

        Point2D.Double p0 = points.get(0);
        output.moveTo(p0.x, p0.y);

        for (int i = 1; i < size; ++i) {
            Point2D.Double p = points.get(i);
            output.lineTo(p.x, p.y);
        }

        if (isClosed()) {
            // TODO If we do pen-up pen-down in the middle of a path,
            // then this won't work correctly.
            output.closePath();
        }
        return output;
    }

    @Override
    public Path2D.Double getPath(AffineTransform at) {
        int size = points.size();
        Path2D.Double output = new Path2D.Double();
        if (size == 0) {
            return output;
        }

        Point2D.Double xpt = new Point2D.Double();
        at.transform(points.get(0), xpt);
        output.moveTo(xpt.x, xpt.y);

        for (int i = 1; i < size; ++i) {
            at.transform(points.get(i), xpt);
            output.lineTo(xpt.x, xpt.y);
        }
        return output;
    }

    @Override
    public int getSmoothingType() {
        return GeneralPolyline.LINEAR;
    }

    @Override
    public Point2D.Double getGradient(int segmentNo, double t) {
        if (points.size() == 0) {
            return null;
        }

        Point2D.Double p1 = points.get(segmentNo);
        Point2D.Double p2 = points.get(segmentNo + 1);
        return new Point2D.Double(p2.x - p1.x, p2.y - p1.y);
    }

    public String toString(AffineTransform at) {
        int size = points.size();
        if (size == 0) {
            return super.toString();
        }
        StringBuilder buf = new StringBuilder(super.toString() + "[");
        Point2D.Double xpt = new Point2D.Double();
        for (int i = 0; i < size; ++i) {
            if (i > 0) {
                buf.append(" - ");
            }
            at.transform(points.get(i), xpt);
            buf.append(xpt.toString());
        }
        buf.append("]");
        return buf.toString();
    }
}
