/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;
import java.awt.*;
import java.util.*;

/** 2D cubic spline class. Differences in t values are proportional to
    chord distances. Open splines have zero second derivative at the
    endpoints; closed splines are approximated by computing an open
    spline with each end extended by 5 extra control points' worth of
    padding from the opposite end of the curve. */
final public class CubicSpline2D implements BoundedParameterizable2D {
    CubicSpline1D xSpline;
    CubicSpline1D ySpline;
    Path2D.Double cachedPath = null;

    public CubicSpline2D (Point2D[] points) {
        this(points, false);
    }

    protected CubicSpline2D() {
    }

    /** @return a CubicSpline2D object that corresponds to the cubic
        Bezier curve that starts at p0, ends at p3, and has the
        control points p1 and p2 in between (but as is typical for
        Bezier curves, does not usually pass through p1 or p2). */
    public static CubicSpline2D getBezierInstance
        (Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        CubicSpline2D output = new CubicSpline2D();
        output.xSpline = CubicSpline1D.getBezierInstance
            (p0.getX(), p1.getX(), p2.getX(), p3.getX());
        output.ySpline = CubicSpline1D.getBezierInstance
            (p0.getY(), p1.getY(), p2.getY(), p3.getY());
        return output;
    }

    public <T extends Point2D> CubicSpline2D (T[] points, boolean closed) {
        int cnt = points.length;

        if (closed && cnt >= 2) {
            // Pad the vertex set with extra points at each end from
            // the other end of the curve, to give the algorithm a
            // hint about how to wrap around at the endpoints. The
            // result won't be perfect (that is, the slope and second
            // derivatives won't match precisely), but it should be
            // close.

            if (points[0].equals(points[cnt-1])) {
                // If the last vertex repeats the first one, ignore
                // it.
                --cnt;
            }

            if (cnt > 1) {
                int padding = 5;

                ArrayList<Point2D> paddedPoints = new ArrayList<Point2D>();
                for (int i = 0; i < padding * 2 + cnt; ++i) {
                    int j = (((i - padding) % cnt) + cnt) % cnt;
                    paddedPoints.add(points[j]);
                }

                CubicSpline2D paddedSpline = new CubicSpline2D
                    (paddedPoints.toArray(new Point2D[0]), false);

                // Now remove all of the padding, but keep the extra
                // segment that connects the two endpoints.
                xSpline = paddedSpline.xSpline.copyOfRange(padding, cnt + 1);
                ySpline = paddedSpline.ySpline.copyOfRange(padding, cnt + 1);
                return;
            }
        }

        double[] xs = new double[cnt];
        double[] ys = new double[cnt];
        double[] ts = new double[cnt];
        double t = 0.0;

        // Per Numerical Recipes' suggestion, let the delta-t between
        // successive points equal the distance between them. (Letting
        // delta-t always equal 1 produces weird behavior such as
        // needlessly overshooting some points even when they lie
        // along a straight line if a long distance is followed by a
        // distance that is very short or zero.)

        for (int i = 0; i < cnt; ++i) {
            Point2D p = points[i];
            if (i > 0) {
                double d = p.distance(points[i-1]);
                if (d == 0) {
                    throw new IllegalArgumentException
                        ("Successive points must not coincide (#" + (i-1) + " and #" + i
                         + " at " + Geom.toString(points[i]) + ")");
                }
                t += d;
            }

            ts[i] = t;
            xs[i] = p.getX();
            ys[i] = p.getY();
        }

        xSpline = new CubicSpline1D(ts, xs);
        ySpline = new CubicSpline1D(ts, ys);
    }

    public int segmentCnt() {
        return xSpline.segmentCnt();
    }

    public Point2D.Double value(double t) {
        return new Point2D.Double(xSpline.value(t), ySpline.value(t));
    }

    public Point2D.Double derivative(double t) {
        return new Point2D.Double(xSpline.derivative(t), ySpline.derivative(t));
    }

    public Point2D.Double gradient(int segmentNo, double t) {
        if (segmentCnt() < 1) {
            return null;
        }
        return new Point2D.Double(xSpline.slope(segmentNo, t),
                                  ySpline.slope(segmentNo, t));
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

        double me2 = v01.distanceSq(Geom.mean(v0,v02)) +
            v02.distanceSq(Geom.mean(v01,v1));

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

    void appendSplinesTo(Path2D path, int start, int end) {
        double[] bezx = new double[4];
        double[] bezy = new double[4];
        for (int segment = start; segment < end; ++segment) {
            xSpline.bezier(segment, bezx);
            ySpline.bezier(segment, bezy);
            path.curveTo(bezx[1],bezy[1],bezx[2],bezy[2],bezx[3],bezy[3]);
        }
    }

    /** Convert this spline into a Path2D. */
    public Path2D.Double path() {
        if (cachedPath == null) {
            cachedPath = new Path2D.Double();
            int cnt = segmentCnt();
            if (cnt >= 0) {
                Point2D.Double p = getVertex(0);
                cachedPath.moveTo(p.x, p.y);
                appendSplinesTo(cachedPath, 0, cnt);
            }
        }

        return (Path2D.Double) cachedPath.clone();
    }

    @Override public BoundedParam2D getParameterization() {
        return PathParam2D.create(path());
    }

    /** @return the set of Bezier control points that define the given
        segment of this spline curve. */
    public Point2D.Double[] bezierPoints(int segment) {
        double[] bezx = new double[4];
        double[] bezy = new double[4];
        xSpline.bezier(segment, bezx);
        ySpline.bezier(segment, bezy);
        return new Point2D.Double[]
            { new Point2D.Double(bezx[0], bezy[0]),
              new Point2D.Double(bezx[1], bezy[1]),
              new Point2D.Double(bezx[2], bezy[2]),
              new Point2D.Double(bezx[3], bezy[3]) };
    }

    @Override public String toString() {
        return super.toString() + "\n" + "x: " + xSpline + "y: " + ySpline;
    }

    /** @return a guess for the point on the spline with t in [t0, t1]
        that is closest to p.

        The estimate is computed by assuming the segment is linear in
        order to determine the t value in [t0, t1] where the curve
        approaches most closely, and then computing the actual value
        of the spline at that t value.
    */
    public CurveDistance nearPointGuess(Point2D p, double t0, double t1) {
        Point2D.Double p0 = value(t0);
        Point2D.Double p1 = value(t1);
    
        double dx = p1.getX() - p0.getX();
        double dy = p1.getY() - p0.getY();
        double dx2 = p.getX() - p0.getX();
        double dy2 = p.getY() - p0.getY();
        double dot = dx * dx2 + dy * dy2;
        double p0p1LengthSq = dx * dx + dy * dy;

        if (p0p1LengthSq == 0 || t0 == t1) {
            return curveDistance(p, (t0 + t1) / 2);
        }
        dot /= p0p1LengthSq;

        if (dot < 0) {
            return curveDistance(p, t0);
        } else if (dot > 1) {
            return curveDistance(p, t1);
        } else {
            return curveDistance(p, t0 + (t1 - t0) * dot);
        }
    }

    /** @return CurveDistance for value(t). */
    CurveDistance curveDistance(Point2D p, double t) {
        Point2D.Double p2 = value(t);
        return new CurveDistance(t, p2, p.distance(p2));
    }

    /** @return the bounds of this curve. */
    public Rectangle2D.Double getBounds() {
        double[] xBounds = xSpline.getBounds();
        double[] yBounds = ySpline.getBounds();
        return new Rectangle2D.Double(xBounds[0], yBounds[0],
                                      xBounds[1] - xBounds[0],
                                      yBounds[1] - yBounds[0]);
    }

    /** @return a lower bound on the distance between p and spline(t)
        for t in [t0, t1]. */
    public double distanceLowerBound(Point2D p, double t0, double t1) {
        double[] xRange = xSpline.range(t0, t1);
        double[] yRange = ySpline.range(t0, t1);

        double minx = xRange[0];
        double maxx = xRange[1];
        double miny = yRange[0];
        double maxy = yRange[1];

        Rectangle2D.Double bounds = 
            new Rectangle2D.Double
            (minx, miny, maxx - minx, maxy - miny);

        double d = Geom.distance(p, bounds);

        if (d == 0) {
            return d;
        }

        // If you base the lower distance bounds on the rectangle
        // only, then the bounds provided for the following case are
        // poor in the critical region near the closest point:

        // point = (0,0)

        // spline is (x(t) = t, y(t) = t - 1)

        // Near the point of closest approach at (t = 0.5, x = 0.5, y
        // = -0.5), the error in the lower bound estimate above is
        // proportional to width of the interval [t0, t1]. The box
        // size you need to prove that error bounds are satisfied ends
        // up being proportional to the square root of the reciprocal
        // of your error bounds, which implies poor performance when
        // seeking high accuracy.

        // A refinement that can help in such cases is to examine the
        // derivative and the bounding box and use the two together to
        // set limits on the change in distance with respect to t.

        // ddistance/dt = (x dx/dt + y dy/dt) / sqrt(x^2 + y^2) (after
        // subtracting p.x from x and p.y from y)

        // For example, consider the range [t0 = 0.48, t1 = 0.50].
        // Within this region, the maximum absolute value of
        // ddistance/dt is when the first term is either most positive
        // or negative, and the divisor (sqrt(x^2 + y^2)) is as small
        // as possible. In phase 1, we already obtained an estimate of
        // the smallest possible value of the denominator, so focus on
        // the numerator.

        // So you've got

        // max(|ddistance/dt|) =

        // max(max(minx * mindx, maxx * maxdx) + max(miny * mindy, maxy * maxdy),
        // max(-maxx * mindx, -minx * maxdx) + max(-maxy * mindy, -miny * maxdy)) / mindist.

        // Then a lower bound on distance is obtained by starting with
        // the distance of the central point and subtracting the above
        // figure times (t1 - t0) / 2.

        // In the given example, you have

        // max(|ddistance/dt|) = max(0.51 - 0.49, -0.49 + 0.51) = 0.02

        // distance lower bound = 1/sqrt(2) - (0.01 * 0.02) / sqrt(2)

        // which provides a much tighter lower bound than sqrt(0.49^2
        // + 0.49^2), and changes the convergence rate from terrible
        // to something approaching linear. Big improvement!

        // If p happens to be the center of curvature for the point
        // closest to it, you'll still get poor convergence, but
        // that's an anomaly.

        double[] dxRange = xSpline.derivativeRange(t0, t1);
        double[] dyRange = ySpline.derivativeRange(t0, t1);

        double mindx = dxRange[0];
        double maxdx = dxRange[1];
        double mindy = dyRange[0];
        double maxdy = dyRange[1];

        // Adjust positions relative to p.
        minx -= p.getX();
        maxx -= p.getX();
        miny -= p.getY();
        maxy -= p.getY();

        double maxdddt
            = Math.max(Math.max(minx * mindx, maxx * maxdx)
                       + Math.max(miny * mindy, maxy * maxdy),
                       Math.max(-maxx * mindx, -minx * maxdx)
                       + Math.max(-maxy * mindy, -miny * maxdy))
            / d;

        double d2 = p.distance(value((t0 + t1) / 2))
            - ((t1 - t0) / 2) * maxdddt;

        // Use whichever estimate provides the most precise result.
        return Math.max(d, d2);
    }

    public SegmentAndT getSegment(double t) {
        return xSpline.getSegment(t);
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
