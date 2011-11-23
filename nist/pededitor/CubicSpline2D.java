package gov.nist.pededitor;

import java.awt.geom.*;
import java.awt.*;
import java.util.*;

/** Class to handle 1D cubic spline interpolations for open curves
    ("polylines" in Java terms). For higher dimensions, just combine
    multiple 1D cubic splines.
 */
final public class CubicSpline2D {
    /** Information about where the curve comes closest to a given point. */
    static public class DistanceInfo {
        public DistanceInfo(double t, Point2D.Double point, double distance) {
            this.t = t;
            this.point = new Point2D.Double(point.x, point.y);
            this.distance = distance;
        }

        public String toString() {
            return "DistanceInfo[t = " + t + ", p = " + point + ", d = "
                + distance + "]";
        }

        /** t in [0,1] is the parameterization variable value. */
        public double t;
        /** Point of closest approach. */
        public Point2D.Double point;
        /** Distance at closest approach. */
        public double distance;
    }

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

    public String toString() {
        return super.toString() + "\n" + "x: " + xSpline + "y: " + ySpline;
    }

    /** @return a guess for the point on the spline with t in [t0, t1]
        that is closest to p.

        The estimate is computed by assuming the segment is linear in
        order to determine the t value in [t0, t1] where the curve
        approaches most closely, and then computing the actual value
        of the spline at that t value.
    */
    public DistanceInfo closePointGuess(Point2D p, double t0, double t1) {
        Point2D.Double p0 = value(t0);
        Point2D.Double p1 = value(t1);
    
        double dx = p1.getX() - p0.getX();
        double dy = p1.getY() - p0.getY();
        double dx2 = p.getX() - p0.getX();
        double dy2 = p.getY() - p0.getY();
        double dot = dx * dx2 + dy * dy2;
        double p0p1LengthSq = dx * dx + dy * dy;

        if (p0p1LengthSq == 0 || t0 == t1) {
            return closePointInfo(p, (t0 + t1) / 2);
        }
        dot /= p0p1LengthSq;

        if (dot < 0) {
            return closePointInfo(p, t0);
        } else if (dot > 1) {
            return closePointInfo(p, t1);
        } else {
            return closePointInfo(p, t0 + (t1 - t0) * dot);
        }
    }

    /** @return DistanceInfo for value(t). */
    DistanceInfo closePointInfo(Point2D p, double t) {
        Point2D.Double p2 = value(t);
        return new DistanceInfo(t, p2, p.distance(p2));
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

        double d = Duh.distance(p, bounds);

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

    /** @return a DistanceInfo object for a point on the spline curve
        whose distance from p is no more than maxError greater than
        the minimum distance between any point on the spline curve and
        p.

        Performance should be good even when maxError is small --
        O(segmentCnt * log(curveBoundingBoxSize/maxError)), I think --
        unless either maxError is so small that double-precision
        arithmetic errors begin to dominate, or the radius of
        curvature of the spline curve at the nearest point equals the
        distance to p.

        @param maxIterations If maxError is not achieved within this
        number of iterations, then run just a bit longer and return
        the best estimate so far.
    */
    public DistanceInfo closePoint(Point2D p, double maxError,
                                   int maxIterations) {
        System.out.println("closePoint(" + p + ", " + maxError + ")");

        // Initialize bestCandidate with an arbitrary point.
        // bestCandidate.distance represents the lowest proven upper
        // bound on the distance from p to the spline curve.

        DistanceInfo bestCandidate = closePointGuess(p, 0.0, 1.0);

        ArrayList<double[]> candidateRanges =
            new ArrayList<double[]>();
        candidateRanges.add(new double[] { 0.0, 1.0 });

        ArrayList<double[]> newCandidateRanges;

        // Keep bisecting [0,1] until the difference between upper and
        // lower bounds on the distance from the curve to point is
        // less than maxError..

        int iterations = 0;

        while (true) {
            newCandidateRanges = new ArrayList<double[]>();

            // The global lower bound equals the minimum of the lower
            // bounds on all candidate ranges, which is not known
            // until every element of candidateRanges has been
            // examined, but lowerBound holds a conservative lower
            // bound on distance to point for all ranges examined so
            // far.
            double lowerBound = 0;
            boolean lowerBoundSet = false;

            for (double[] range: candidateRanges) {
                ++iterations;
                double t0 = range[0];
                double t1 = range[1];

                double thisLowerBound = distanceLowerBound(p, t0, t1);

                if (!lowerBoundSet) {
                    lowerBound = thisLowerBound;
                    lowerBoundSet = true;
                } else if (thisLowerBound < lowerBound) {
                    lowerBound = thisLowerBound;
                }

                if (thisLowerBound > bestCandidate.distance) {
                    // Discard this segment, because its closest point
                    // cannot be as close as bestCandidate.
                    continue;
                }

                DistanceInfo di = closePointGuess(p, t0, t1);
                // di.distance is an upper bound on distance.
                if (di.distance < bestCandidate.distance) {
                    bestCandidate = di;
                }

                double middle = (t0 + t1) / 2;
                newCandidateRanges.add(new double[] {t0, middle});
                newCandidateRanges.add(new double[] {middle, t1});
            }

            if (bestCandidate.distance - lowerBound < maxError
                || iterations >= maxIterations) {
                if (iterations >= maxIterations) {
                    System.err.println("Reached maximum iterations count.");
                }
                System.out.println
                    ("Found a solution " + bestCandidate
                     + " within " + (bestCandidate.distance - lowerBound)
                     + " of ideal after trying  " + iterations + " ranges.");
                return bestCandidate;
            }

            candidateRanges = newCandidateRanges;
        }
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
