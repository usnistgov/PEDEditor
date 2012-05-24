package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Collection;

/** Specialization of GeneralPolyline for vanilla polylines. */
public class Polyline extends GeneralPolyline {
    private boolean closed = false;

    /** The parameterization is cached for efficiency. This class and
        its subclasses should reset param to null whenever a change
        invalidates the cached version. */
    protected transient Parameterization2D param = null;

    public Polyline() {
    }

    public Polyline(Point2D[] points,
                    StandardStroke stroke,
                    double lineWidth) {
        super(points, stroke, lineWidth);
    }

    @Override public void set(int vertexNo, Point2D point) {
        param = null;
        super.set(vertexNo, point);
    }

    @Override public void add(Point2D point) {
        param = null;
        super.add(point);
    }

    /** Add the point to the polyline in the given position. */
    @Override public void add(int index, Point2D point) {
        param = null;
        super.add(index, point);
    }

    /** Remove the last point added. */
    @Override public void remove() {
        param = null;
        super.remove();
    }

    /** Remove the given vertex. */
    @Override public void remove(int vertexNo) {
        param = null;
        super.remove(vertexNo);
    }

    @Override public void setPoints(Collection<Point2D.Double> points) {
        super.setPoints(points);
        param = null;
    }

    @Override public void setClosed(boolean closed) {
        if (closed != this.closed) {
            this.closed = closed;
            param = null;
        }
    }

    @Override public boolean isClosed() {
        return closed;
    }

    @Override public Path2D.Double getPath() {
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

        if (isClosed() && size > 1) {
            output.closePath();
        }
        return output;
    }

    @Override public Path2D.Double getPath(AffineTransform at) {
        int size = points.size();
        Path2D.Double output = new Path2D.Double();
        if (size == 0) {
            return output;
        }

        Point2D.Double p0 = new Point2D.Double();
        Point2D.Double xpt = new Point2D.Double();
        at.transform(points.get(0), p0);
        output.moveTo(p0.x, p0.y);

        for (int i = 1; i < size; ++i) {
            at.transform(points.get(i), xpt);
            output.lineTo(xpt.x, xpt.y);
        }

        if (isClosed() && size > 1) {
            output.closePath();
        }
        return output;
    }

    @Override public Parameterization2D getParameterization() {
        if (param == null) {
            param = super.getParameterization();
        }
        return param;
    }

    @Override public int getSmoothingType() {
        return GeneralPolyline.LINEAR;
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
