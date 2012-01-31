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
            output.lineTo(p0.x, p0.y);
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

        Point2D.Double p0 = new Point2D.Double();
        Point2D.Double xpt = new Point2D.Double();
        at.transform(points.get(0), p0);
        output.moveTo(p0.x, p0.y);

        for (int i = 1; i < size; ++i) {
            at.transform(points.get(i), xpt);
            output.lineTo(xpt.x, xpt.y);
        }

        if (isClosed() && size > 1) {
            output.lineTo(p0.x, p0.y);
        }
        return output;
    }

    @Override
    public int getSmoothingType() {
        return GeneralPolyline.LINEAR;
    }

    @Override
    public Point2D.Double getGradient(int segmentNo, double t) {
        if (points.size() < 2) {
            return null;
        }

        Point2D.Double p1 = points.get(segmentNo);
        Point2D.Double p2 = get(segmentNo + 1);
        return new Point2D.Double(p2.x - p1.x, p2.y - p1.y);
    }

    @Override
    public Point2D.Double getLocation(int segmentNo, double t) {
        if (points.size() == 0) {
            return null;
        }

        Point2D.Double p1 = points.get(segmentNo);
        Point2D.Double p2 = points.get((segmentNo + 1) % size());
        return new Point2D.Double(p1.x + (p2.x - p1.x) * t,
                                  p1.y + (p2.y - p1.y) * t);
    }

    @Override public double[] segmentIntersectionTs(Line2D segment) {
        ArrayList<Double> output = new ArrayList<Double>();
        Point2D s1 = segment.getP1();
        Point2D s2 = segment.getP2();
        int segCnt = getSegmentCnt();
        Point2D.Double p1 = get(0);

        double oldT2 = -1.0;

        for (int i = 0; i < segCnt; ++i) {
            Point2D.Double p2 = get(i+1);
            double t = Duh.segmentIntersectionT(p1, p2, s1, s2);
            p1 = p2;
            // Convert t value within segment to t value within
            // whole curve.
            double t2 = (t + i) / segCnt;
            if (t < 0 /* No intersection */ || t2 <= oldT2 /* Repeat */) {
                continue;
            }
            oldT2 = t2;
            output.add(t2);
        }

        double[] o = new double[output.size()];
        for (int i = 0; i < o.length; ++i) {
            o[i] = output.get(i);
        }
        return o;
    }

    @Override public double[] lineIntersectionTs(Line2D line) {
        ArrayList<Double> output = new ArrayList<Double>();
        Point2D s1 = line.getP1();
        Point2D s2 = line.getP2();
        int segCnt = getSegmentCnt();
        Point2D.Double p1 = get(0);

        double oldT2 = -1.0;

        for (int i = 0; i < segCnt; ++i) {
            Point2D.Double p2 = get(i+1);
            double t = Duh.lineIntersectionT(p1, p2, s1, s2);
            p1 = p2;
            // Convert t value within segment to t value within
            // whole curve.
            double t2 = (t + i) / segCnt;
            if ((t < 0 || t > 1 /* No intersection */) || t2 <= oldT2 /* Repeat */) {
                continue;
            }
            oldT2 = t2;
            output.add(t2);
        }

        double[] o = new double[output.size()];
        for (int i = 0; i < o.length; ++i) {
            o[i] = output.get(i);
        }
        return o;
    }

    /* Parameterize the entire curve as t in [0,1] and return the
       location corresponding to the given t value */
    @Override public Point2D.Double getLocation(double t) {
        int segCnt = getSegmentCnt();

        if (t < 0 || segCnt <= 0) {
            return (Point2D.Double) points.get(0).clone();
        }

        if (t >= 1) {
            return (Point2D.Double)
                points.get(isClosed() ? 0 : points.size() - 1).clone();
        }

        t *= segCnt;
        double segment = Math.floor(t);

        return getLocation((int) segment, t - segment);
    }

    @Override
    public CurveDistance distance(Point2D p) {
        // For closed polylines, don't forget the segment
        // connecting the last vertex to the first one.
        int segCnt = getSegmentCnt();
        Point2D.Double nearPoint = null;
        CurveDistance minDist = null;

        for (int i = 0; i < segCnt; ++i) {
            CurveDistance dist = CurveDistance.pointSegmentDistance
                (p, points.get(i), points.get((i+1) % points.size()));
            if (minDist == null || dist.distance < minDist.distance) {
                // Convert the parameterized t-value within the
                // segment to a parameterized t-value for the entire
                // polyline.
                dist.t = segmentToT(i, dist.t);
                minDist = dist;
            }
        }

        return minDist;
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
