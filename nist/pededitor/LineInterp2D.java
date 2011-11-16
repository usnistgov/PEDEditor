package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** Specialization of GeneralPolyline for vanilla polylines. */
public class Polyline extends GeneralPolyline {
    public Polyline(Point2D.Double[] points, BasicStroke stroke) {
        super(points, stroke);
    }

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
        return output;
    }

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

    public int getSmoothingType() {
        return GeneralPolyline.LINEAR;
    }

    public String toString() {
        int size = points.size();
        if (size == 0) {
            return super.toString();
        }
        StringBuilder buf = new StringBuilder(super.toString() + "[");
        for (int i = 0; i < size; ++i) {
            if (i > 0) {
                buf.append(" - ");
            }
            buf.append(points.get(i).toString());
        }
        buf.append("]");
        return buf.toString();
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
