package gov.nist.pededitor;

import java.awt.geom.*;
import java.awt.*;
import java.util.*;

/** Class to handle 1D cubic spline interpolations for open curves
    ("polylines" in Java terms). For higher dimensions, just combine
    multiple 1D cubic splines.
 */
final public class CubicSpline2D {
    CubicSpline1D xSpline;
    CubicSpline1D ySpline;

    public <T extends Point2D> CubicSpline2D (T[] points) {
        int cnt = points.length;
        double[] xs = new double[cnt];
        double[] ys = new double[cnt];
        for (int i = 0; i < cnt; ++i) {
            Point2D p = points[i];
            xs[i] = p.getX();
            ys[i] = p.getY();
        }

        xSpline = new CubicSpline1D(xs);
        ySpline = new CubicSpline1D(ys);
    }

    public int segmentCnt() {
        return xSpline.segmentCnt();
    }

    public Point2D.Double value(double t) {
        return new Point2D.Double(xSpline.value(t), ySpline.value(t));
    }

    public Point2D.Double value(int segment, double t) {
        return new Point2D.Double(xSpline.value(segment, t),
                                  ySpline.value(segment, t));
    }

    public Point2D.Double getVertex(int vertexNo) {
        return new Point2D.Double(xSpline.getVertex(vertexNo),
                                  ySpline.getVertex(vertexNo));
    }

    /** @return an upper bound on the length of the curve between
        point #segment and point #(segment+1). */
    public double maxSegmentLength(int segment) {
        double d1 = xSpline.segmentLength(segment);
        double d2 = ySpline.segmentLength(segment);
        return Math.abs(d1) + Math.abs(d2);
    }

    /** @return a set of segments that should suffice to draw a
        PolyLine that is never more than maxError away from the actual
        cubic spline curve.

        That the error will be below that threshold isn't 100%
        guaranteed, because I didn't do the math all the way
        through. */
    public Point2D.Double[] samplePoints(double maxError) {
        if (maxError <= 0) {
            throw new IllegalArgumentException("maxError must be positive");
        }

        ArrayList<Point2D.Double> output =
            new ArrayList<Point2D.Double>();

        int cnt = xSpline.segmentCnt();
        double me2 = maxError * maxError / 16;

        for (int segment = 0; segment < cnt; ++segment) {
            samplePoints(output, segment, 0., 1., me2);
        }

        /** We have to add the last point manually; see the comments
            for the other samplePoints() below. */
        output.add(value(1.));
        return output.toArray(new Point2D.Double[0]);
    }

    /** Recursively, adaptively add enough points to output to keep
        the square of the error term under maxError2. The last point
        (at t1) is not added, because it is assumed that it will be
        added by the next section.
    */
    protected void samplePoints(ArrayList<Point2D.Double> output,
                                int segment, double t0, double t1,
                                double maxError2) {
        double delta = t1 - t0;
        double t01 = t0 + delta/3;
        double t02 = t0 + 2*delta/3;
        Point2D.Double v0 = value(segment, t0);
        Point2D.Double v01 = value(segment, t01);
        Point2D.Double v02 = value(segment, t02);
        Point2D.Double v1 = value(segment, t1);

        double me2 = v01.distanceSq(Duh.mean(v0,v02)) +
            v02.distanceSq(Duh.mean(v01,v1));

        if (me2 > maxError2) {
            samplePoints(output, segment, t0, t01, maxError2);
            samplePoints(output, segment, t01, t02, maxError2);
            samplePoints(output, segment, t02, t1, maxError2);
        } else {
            output.add(v0);
            output.add(v01);
            output.add(v02);
        }
    }

    /** Convert this spline into a Path2D. */
    Path2D.Double path() {
        double[] bezx = new double[4];
        double[] bezy = new double[4];
        Path2D.Double path = new Path2D.Double();
        int cnt = segmentCnt();
        if (cnt < 0) {
            return path;
        }

        Point2D.Double p = getVertex(0);
        path.moveTo(p.x, p.y);
        for (int segment = 0; segment < cnt; ++segment) {
            xSpline.bezier(segment, bezx);
            ySpline.bezier(segment, bezy);
            path.curveTo(bezx[1],bezy[1],bezx[2],bezy[2],bezx[3],bezy[3]);
        }

        return path;
    }

    public String toString() {
        return super.toString() + "\n" + "x: " + xSpline + "y: " + ySpline;
    }

    /** Just a test harness */
    public static void main(String[] args) {
        Point[] points = {
            new Point(1,20), new Point(2,10) };
        CubicSpline2D c = new CubicSpline2D(points);
        System.out.println(c);
        Point2D.Double[] ptds = c.samplePoints(1.0);
        for (Point2D.Double p : ptds) {
            System.out.println(p);
        }
    }
}
