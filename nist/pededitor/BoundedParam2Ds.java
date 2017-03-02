/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * This class primarily exists to provide the {@link #intersections}
 * and {@link #distance} methods. */
public class BoundedParam2Ds {
    static final boolean debug = false;
    /** Return the t value that is closest to t that is also in the
        domain of c. */
    public static double constrainToDomain(BoundedParam2D c, double t) {
        double minT = c.getMinT();
        double maxT = c.getMaxT();
        return (t < minT) ? minT
            : (t > maxT) ? maxT : t;
    }

    /** If either the derivative() function is not reliable or its
        getBounds() can return values with large relative error, then
        use distanceLowerBound0() instead. */
    public static double distanceLowerBound
        (BoundedParam2D c, Point2D p) {
        double d0 = distanceLowerBound0(c, p);
        double d1 = distanceLowerBound1(c, p);
        if (debug) {
            System.out.println("distanceLowerBound((" + c.getMinT() + ", " + c.getMaxT() + ") , " + p
                               + ") = max(" + d0 + ", " + d1 + ")");
        }
        return Math.max(d0, d1);
    }

    /** Determine the distance between the point and the closest of
        the curves in ocps.

        @see {@link OffsetParam2D#distance(ArrayList<BoundedParam2D>,
        Point2D)} for a more generally useful variant of distance()
        that also identifies which curve is closest. */
    static <T extends BoundedParam2D> CurveDistanceRange distance(
            Iterable<T> ocps, Point2D p, double maxError, double maxSteps) {
    	ArrayList<BoundedParam2D> cps = new ArrayList<>();
    	for (BoundedParam2D cp: ocps) {
    		cps.add(cp);
    	}

        if (debug) {
            System.out.println("Sections are: ");
            for (BoundedParam2D cp: cps) {
                System.out.println(cp);
            }
            System.out.println();
        }

        if (cps.size() == 0) {
            return null;
        }

        CurveDistanceRange minDist = null;
        for (int step = 0;; ++step) {
            if (debug) {
                System.out.println("Step " + step);
            }

            if (minDist != null) {
                // Initialize minDistance to an exact lower bound. The
                // lower bound might no longer be exact after the
                // following loop finishes).
                minDist.minDistance = minDist.distance;
            }

            // Compute the proper minDist value for this stage.
            ArrayList<CurveDistanceRange> cds = new ArrayList<>();
            for (BoundedParam2D cp: cps) {
                CurveDistanceRange dist = cp.distance(p);
                cds.add(dist);

                if (debug) {
                    System.out.println("[" + cp.getMinT() + ", " + cp.getMaxT()
                                       + "]: " + dist);
                }
                minDist = CurveDistanceRange.min(minDist, dist);
            }

            // CurveDistanceRanges whose minDist values exceed
            // cutoffDistance do not require further investigation --
            // either they're not nearest, or they are nearest but
            // represent an improvement of no more than maxError in
            // distance compared to the current best estimate.
            double cutoffDistance = minDist.distance - maxError;

            if (debug) {
                System.out.println("Step " + step + ": dist = " + minDist
                                   + ", cutoff = " + cutoffDistance);
            }

            // Create a list of curves whose distance from p may be
            // less than cutoffDistance. Bisect those curves to
            // improve accuracy and reduce the difference between the
            // upper and lower bounds on the minimum distance from p
            // to the curves.

            ArrayList<BoundedParam2D> ncps = new ArrayList<>();
            for (int segNo = 0; segNo < cps.size(); ++segNo) {
                BoundedParam2D cp = cps.get(segNo);
                CurveDistanceRange dist = cds.get(segNo);

                if (debug) {
                    System.out.println("Seg#" + segNo + "[" + cp.getMinT() + ", " + cp.getMaxT()
                                       + "]: " + dist);
                }

                if (dist.minDistance >= cutoffDistance) {
                    continue;
                }

                if (debug) {
                    System.out.println("Bisecting [" + cp.getMinT() + ", "
                                       + cp.getMaxT() + "]");
                }

                // Subdivide this curve to improve approximation accuracy.
                BoundedParam2D[] divisions = cp.subdivide();
                for (BoundedParam2D param: divisions) {
                    ncps.add(param);
                }
            }

            maxSteps -= cps.size();
            if (ncps.size() == 0 /* Error <= maxError */
                || maxSteps < 0 /* Too many iterations */) {
                return minDist;
            }

            cps = ncps;
        }
    }

    /** Compute the lower bound on the distance from p to f(t) for any
        t in [-deltaT, deltaT] given that f(0) = f0 and f'(t) in
        dfdtBounds for all t in [-deltaT, deltaT]. This implies that
        for all t in [-deltaT, deltaT] there exists a vector del in
        dfdtBounds such that f(t) = f0 + del * t. The "1" in the
        method name reflects that this this lower bound is computed
        using the bounds on the first derivative.
     * @return */
    public static double distanceLowerBound1
        (Point2D p, Point2D f0, double deltaT, Rectangle2D dfdtBounds) {
        if (deltaT == 0) {
            return p.distance(f0);
        }

        Point2D.Double pn = new Point2D.Double
            (p.getX() - f0.getX(), p.getY() - f0.getY());
        Rectangle2D.Double b = new Rectangle2D.Double
            (dfdtBounds.getX() * deltaT,
             dfdtBounds.getY() * deltaT,
             dfdtBounds.getWidth() * deltaT,
             dfdtBounds.getHeight() * deltaT);

        // Create a list of the segments connecting (-v) to (v) where
        // v is one of the four vertexes of b.
        Point2D.Double[] vertexes =
            { new Point2D.Double(b.x, b.y),
              new Point2D.Double(b.x + b.width, b.y),
              new Point2D.Double(b.x, b.y + b.height),
              new Point2D.Double(b.x + b.width, b.y + b.height) };
        ArrayList<Line2D.Double> segments = new ArrayList<>();
        for (Point2D.Double v: vertexes) {
            segments.add
                (new Line2D.Double(v, new Point2D.Double(-v.x, -v.y)));
        }

        // Figure out whether pn might equal f(t) for some t. That is
        // possible if at least one of the segments (or the horizontal
        // edges of b or -b) passes above pn, and at least one of the
        // segments (or one of the horizontal edges of b or -b) passes
        // below pn.

        boolean segmentAbove
            = (pn.x >= b.x && pn.x <= b.x+b.width && pn.y <= b.y+b.height)
            || (pn.x >= -b.x-b.width && pn.x <= -b.x && pn.y <= -b.y);
        boolean segmentBelow
            = (pn.x >= b.x && pn.x <= b.x+b.width && pn.y >= b.y)
            || (pn.x >= -b.x-b.width && pn.x <= -b.x && pn.y >= -b.y-b.height);

        for (Line2D.Double segment: segments) {
            if (pn.x == segment.x1 || pn.x == segment.x2 ||
                ((pn.x > segment.x1) == (pn.x < segment.x2))) {
                // pn's x range overlaps segment's.
                if (segment.x1 == segment.x2) {
                    if (pn.y == segment.y1 || pn.y == segment.y2 ||
                        ((pn.y > segment.y1) == (pn.y < segment.y2))) {
                        // segment is vertical and contains pn.
                        return 0;
                    }
                } else {
                    double y = (pn.x - segment.x1)
                        * (segment.y2 - segment.y1)/(segment.x2 - segment.x1);
                    if (y == pn.y) {
                        // segment contains pn.
                        return 0;
                    } else if (y > pn.y) {
                        segmentAbove = true;
                    } else {
                        segmentBelow = true;
                    }
                }
            }
        }

        if (segmentAbove && segmentBelow) {
            return 0;
        }

        // Find the minimum distance from pn to b and -b.
        double minDist = Math.min
            (Geom.distance(pn, b),
             Geom.distance(pn,
                          new Rectangle2D.Double(-b.getMaxX(), -b.getMaxY(),
                                                 b.getWidth(), b.getHeight())));

        // Find the minimum distance from pn to any segment.
        for (Line2D segment: segments) {
            double d = segment.ptSegDist(pn);
            if (d < minDist) {
                minDist = d;
            }
        }

        if (debug) {
            System.out.println
                ("dlb1(" + Geom.toString(p) + ", " + Geom.toString(f0) + ", " + deltaT + ", "
                 + Geom.toString(dfdtBounds) + ") = ");
            System.out.println("pn = " + Geom.toString(pn) + ", b = "
                               + Geom.toString(b) + ", dist = " + minDist);
        }
        return minDist;
    }

    /** Compute a lower bound on the minimum distance from c to p by
        computing the distance from p to the following set:

        { q | q = mid + dt * delta }

        where mid = (c.maxT() + c.minT())/2, dt <= (c.maxT() -
        c.minT()) / 2, and delta is a vector that is in the interior
        of c.derivative().getBounds().

        (The '1' in the name indicates that this lower bound is based
        on the first derivative of c.)
    */
   public static double distanceLowerBound1
        (BoundedParam2D c, Point2D p) {
        double centerT = (c.getMinT() + c.getMaxT()) / 2;
        Point2D.Double f0 = c.getLocation(centerT);
        double deltaT = centerT - c.getMinT();
        Rectangle2D.Double dfdtBounds = c.derivative().getBounds();
        return distanceLowerBound1(p, f0, deltaT, dfdtBounds);
    }

    /** Add each contiguous subset of c that is on the left side of
        divider to lefts and add each contiguous subset on the right side
        to rights. (The left side is the half-plane you get to by
        starting at divider.getP1(), moving to divider.getP2(), and
        making a left turn.

        If either "lefts" or "rights" is null, then the corresponding
        half-plane is simply discarded. */
    public static void intersectWithHalfPlane
        (BoundedParam2D c, Line2D divider,
         List<BoundedParam2D> lefts,
         List<BoundedParam2D> rights) {
        double[] ts = c.lineIntersections(divider);
        Point2D p1 = divider.getP1();
        Point2D p2 = divider.getP2();

        double t0 = c.getMinT();
        double t1 = c.getMaxT();

        boolean oldOnLeft = false;
        double startT = t0;
        double oldT = t0;

        // Some intersections are just points of tangency.

        // TODO If the curve goes from the right half-plane to touch
        // the divider and then goes back to the right half-plane,
        // then technically a single point should be added to "lefts",
        // though I'm not sure how much that matters.

        for (int i = 0; i <= ts.length; ++i) {
            double t;

            if (i < ts.length) {
                t = ts[i];
                if (t <= t0 || t >= t1) {
                    continue;
                }
            } else {
                t = t1;
            }

            double midT = (oldT + t) / 2;
            boolean onLeft = Geom.crossProduct(p1, p2, c.getLocation(midT)) >= 0;
            if (oldT != t0 && onLeft != oldOnLeft) {
                // The parameterization crossed the divider. Add the
                // segment [startT, oldT] on the old side of the
                // divider.
                List<BoundedParam2D> list = oldOnLeft ? lefts : rights;
                if (list != null) {
                    BoundedParam2D oldC = c.createSubset(startT, oldT);
                    list.add(oldC);
                }
                startT = oldT;
            }
            oldT = t;
            oldOnLeft = onLeft;
        }

        // Add the last segment.
        List<BoundedParam2D> list = oldOnLeft ? lefts : rights;
        if (list == null) {
            return;
        }

        if (startT == t0) {
            list.add(c);
        } else {
            list.add(c.createSubset(startT, c.getMaxT()));
        }
    }

    /** Simply apply intersectWithHalfPlane to each curve in cs. */
    public static void intersectWithHalfPlane
        (List<BoundedParam2D> cs, Line2D divider,
         List<BoundedParam2D> lefts,
         List<BoundedParam2D> rights) {
        for (BoundedParam2D c: cs) {
            intersectWithHalfPlane(c, divider, lefts, rights);
        }
    }

    /** @return a list of intersections between a and b. The error
        guarantee is supposed to be is as follows:

        1) No matter how small maxError is, the precision limits of
        double-precision arithmetic and the segmentIntersection()
        method still set upper limits on the precision of the answer.

        2) Both curve a and curve b must approach all alleged
        intersections (that is, intersections returned by the method)
        to a minimum distance of maxError or less. However, at this
        point I have no rule guaranteeing an upper bound on the number
        of alleged intersections that correspond to any one actual
        intersection, even if the input curves are well-behaved. (One
        way to identify a situation where multiple intersections are
        impossible is that over any intervals where the two curves'
        ranges of dy/dx values are disjoint, the two curves can only
        intersect once.)

        3) Define the "overlap diameter" of two curves as the minimum
        distance that one of the curves must be translated by before a
        given intersection disappears completely.

        For each actual intersection between the two curves for which
        1) the overlap diameter on either the left or right side of
        that intersection exceeds neither the precision afforded by
        double-precision arithmetic nor the precision limits of the
        segmentIntersection() method AND 2) the distance between the
        actual intersection and the alleged intersection is no more
        than maxError, the program must return an alleged intersection
        that corresponds to the actual intersection. The alleged
        intersection must have the property that there exists a curve
        connecting the alleged intersection to the actual intersection
        such that every point on that curve is within at most maxError
        distance of both curve A and curve B simultaneously. Speaking
        informally, it is permitted for the alleged intersection to
        lie an arbitrary distance from the actual intersection, but
        only if curves A and B follow each other very closely between
        the actual intersection and the alleged intersection.

        If the above conditions do not prevent it, then a single
        alleged intersection may correspond to multiple actual
        intersections. For example,

            y = 0

        intersects

            y = x(x - 0.1)(x + 0.1)

        three times, but if maxError = 0.001, then the algorithm may
        only return a single intersection located at any x value in
        the (roughly) [-0.13,0.13] interval and a y value in [-0.001,
        0.001], because the overlap diameter is less than the maxError
        value of 0.001 over that entire interval.

        @throws FailedToConvergeException if maxSteps is not enough to
        identify all intersections to the requested error threshold..
    */
    public static ArrayList<Point2D.Double> intersections(
            BoundedParam2D a, BoundedParam2D b,
            double maxError, int maxSteps)
        throws FailedToConvergeException {
        ArrayList<Point2D.Double> res = new ArrayList<>();
        int steps = intersections(res, a, b, maxError, maxSteps);
        if (steps > maxSteps) {
            throw new FailedToConvergeException
                ("Could not compute intersections to within " + maxError
                 + " accuracy within " + maxSteps + " steps for "
                 + a + " and " + b);
        }
        return res;
    }

    /** Like {@link #intersections(BoundedParam2D, BoundedParam2D,
        double, int)}, but appends the list of intersections to the
        given ArrayList and returns the number of steps actually
        needed. If the return value is greater than maxSteps then no
        error guarantee is provided. */
    public static int intersections(ArrayList<Point2D.Double> is,
            BoundedParam2D a, BoundedParam2D b,
            double maxError, int maxSteps) {
        Rectangle2D.Double ab = a.getBounds();
        Rectangle2D.Double bb = b.getBounds();
        if (debug) {
            System.out.println("Intersect " + a + ", " + b);
            System.out.println("a[" + a.getMinT() + ", " + a.getMaxT() + "], "
                               + Geom.toString(a.getStart()) + " - "
                               + Geom.toString(a.getEnd()) + ", "
                               + Geom.toString(ab));
            System.out.println("b[" + b.getMinT() + ", " + b.getMaxT() + "], "
                               + Geom.toString(b.getStart()) + " - "
                               + Geom.toString(b.getEnd()) + ", "
                               + Geom.toString(bb));
        }
        Rectangle2D isec = ab.createIntersection(bb);
        double iw = isec.getWidth();
        double ih = isec.getHeight();
        int stepCnt = 1;

        if (iw < 0 || ih < 0) { // No intersection
            return stepCnt;
        }

        Point2D.Double as = a.getStart();
        Point2D.Double ae = a.getEnd();

        // For the direction of the length dimension, choose the
        // vector from the start to the end of curve 'a'. The choice
        // of curve 'a' over curve 'b' is arbitrary, but during the
        // recursion we swap 'a' and 'b', so if one curve is a better
        // choice than the other, the better candidate will get its
        // chance either now or later.

        double lx = ae.x - as.x;
        double ly = ae.y - as.y;
        if (ly == 0) {
            // If it looks like the length vector may end up as a zero
            // vector, then use <1,0> instead.
            lx = 1;
        }
        {
            // Normalize the length vector. This makes life simpler
            // later.
            Point2D.Double normalLength = Geom.normalize
                (new Point2D.Double(lx, ly));
            lx = normalLength.x;
            ly = normalLength.y;
        }

        // Let the width vector <wx,wy> equal <-ly, lx>, perpendicular
        // to the length vector.

        double wx = -ly;
        double wy = lx;

        // lp has been normalized so that lx^2 + ly^2 = 1. The
        // coordinates transform matrix from x/y to length/width
        // coordinates is

        // [ lx ly]
        // [-ly lx]

        // so the inverse transfrom from length/width to x/y
        // coordinates is the matrix

        // [lx -ly]
        // [ly  lx]

        // Now find the bounds of curves 'a' and 'b' using rotated
        // rectangular bounding boxes whose edges are parallel to the
        // length and width basis vectors. Bounds expressed using
        // those vectors are generally tighter than simple xy bounds,
        // so they allow for faster convergence. In the extreme case
        // where curve 'a' is a line segment, the width is zero,
        // allows the intersection to be computed in a single step
        // using the width bound.

        double[] alb = a.getLinearFunctionBounds(lx, ly);
        double[] awb = a.getLinearFunctionBounds(wx, wy);
        if (awb[1] - awb[0] <= maxError) {
            double cw = (awb[0] + awb[1])/2;
            // Curve 'a' is within maxError/2 of being the line
            // segment (alb[0], cw) - (alb[1], cw) (defined in
            // (length, width) coordinate space)
            Line2D.Double aLine = new Line2D.Double
                (lx * alb[0] - ly * cw,
                 ly * alb[0] + lx * cw,
                 lx * alb[1] - ly * cw,
                 ly * alb[1] + lx * cw);
            for (double t: b.segIntersections(aLine)) {
                is.add(b.getLocation(t));
            }
            return stepCnt;
        }

        double[] bwb = b.getLinearFunctionBounds(wx, wy);
        double wmin = Math.max(awb[0], bwb[0]);
        double wmax = Math.min(awb[1], bwb[1]);
        double width = wmax - wmin;
        if (width < 0) {
            // The two bounding boxes do not intersect in the width
            // dimension
            return stepCnt;
        }

        double[] blb = b.getLinearFunctionBounds(lx, ly);
        double lmin = Math.max(alb[0], blb[0]);
        double lmax = Math.min(alb[1], blb[1]);
        double length = lmax - lmin;
        if (length < 0) {
            // The two bounding boxes do not intersect in the length
            // dimension
            return stepCnt;
        }

        Point2D.Double wp = new Point2D.Double(wx, wy);
        Point2D.Double lp = new Point2D.Double(lx, ly);
        double cl = lmin + length/2;
        Line2D.Double widthAxis = Geom.createRay
            (new Point2D.Double(cl * lx, cl * ly), wp);

        double[][] lbounds = { alb, blb };
        double[][] wbounds = { awb, bwb };
        @SuppressWarnings("unchecked")
            ArrayList<BoundedParam2D>[] wholes = new ArrayList[2];

        for (int i = 1; i >= 0; --i) {
            int you = 1 - i; // you is the index of the curve that is not i.
            BoundedParam2D ci = (i == 0) ? a : b;
            ArrayList<BoundedParam2D> curves = new ArrayList<>();
            ArrayList<BoundedParam2D> curves2;
            double l, w;

            // Intersect ci with 4 half-planes that define the sides
            // of cyou's bounding box.

            // Do not intersect ci with the 4 sides of the bounding
            // box of the intersection of ci and cyou, because
            // intersecting a line parallel to length with the line's
            // own zero-thickness bounding box might return nothing at
            // all because of roundoff errors.

            w = wbounds[you][0];
            Line2D.Double wminline = Geom.createRay
                (new Point2D.Double(w * wx, w * wy), lp);
            intersectWithHalfPlane(ci, wminline, curves, null);

            curves2 = new ArrayList<>();
            w = wbounds[you][1];
            Line2D.Double wmaxline = Geom.createRay
                (new Point2D.Double(w * wx, w * wy), lp);
            // I care only about the closed half-plane on the right
            // side of wmaxline. intersectWithHalfPlane() returns the
            // closed left half-plane and the open right half-plane,
            // and neither one of those is exactly what I want. To
            // obtain the closed right half-plane, grab the closed
            // left half-plane defined by the ray that points in the
            // opposite direction from wmaxline.
            intersectWithHalfPlane
                (curves,
                 new Line2D.Double(wmaxline.getP2(), wmaxline.getP1()),
                 curves2, null);
            curves = curves2;

            curves2 = new ArrayList<>();
            l = lbounds[you][0];
            Line2D.Double lminline = Geom.createRay
                (new Point2D.Double(l * lx, l * ly), wp);
            intersectWithHalfPlane
                (curves, new Line2D.Double(lminline.getP2(), lminline.getP1()),
                 curves2, null);
            curves = curves2;

            curves2 = new ArrayList<>();
            l = lbounds[you][1];
            Line2D.Double lmaxline = Geom.createRay
                (new Point2D.Double(l * lx, l * ly), wp);
            intersectWithHalfPlane(curves, lmaxline, curves2, null);
            curves = curves2;

            if (curves.size() == 0) {
                return stepCnt; // test "Z"
            }

            wholes[i] = curves;
        }

        double size = width + length;
        if (size <= maxError) {
            // Both curves intersect the intersection of the two
            // curves' bounds -- otherwise, test "Z" above would have
            // terminated the function -- so both curves approach the
            // center of the intersection box to within size/2
            // distance, which means it is allowed to mark an
            // intersection, though we don't know for sure that there
            // is one. In fact, typically the center of the bounding
            // box will exceed the minimum accuracy requirements by a
            // large margin.

            double cw = wmin + width/2;
            Point2D.Double p = new Point2D.Double
                    (cl * lx  - cw * ly, cl * ly + cw * lx);
            is.add(p);
            return stepCnt;
        }

        if (debug) {
            System.out.print("->a");
            for (BoundedParam2D p:  wholes[0]) {
                System.out.print("[" + p.getMinT() + ", " + p.getMaxT() + "]");
            }
            System.out.println();
            System.out.print("->b");
            for (BoundedParam2D p:  wholes[1]) {
                System.out.print("[" + p.getMinT() + ", " + p.getMaxT() + "]");
            }
            System.out.println();
        }

        double oldASize = (alb[1] - alb[0]) + (awb[1] - awb[0]);
        double oldBSize = (blb[1] - blb[0]) + (bwb[1] - bwb[0]);

        // If we couldn't rely on the bounds to be tight, we would
        // have to use some other test to determine whether the bounds
        // were tightened very much, but this should be OK for now.
        if (size * 1.3 <= oldASize || size * 1.3 <= oldBSize) {
            for (BoundedParam2D pa:  wholes[0]) {
                for (BoundedParam2D pb:  wholes[1]) {
                    // Swap a and b in the next recursion step.
                    if (stepCnt > maxSteps) {
                        return stepCnt;
                    }
                    stepCnt += intersections(is, pb, pa, maxError,
                                             maxSteps - stepCnt);
                }
            }
            return stepCnt;
        }

        // We're not converging fast enough, so bisect the
        // intersection region and process each half separately.

        @SuppressWarnings("unchecked")
            ArrayList<BoundedParam2D>[][] halves = new ArrayList[2][2];
        for (int i = 0; i < 2; ++i) {
            intersectWithHalfPlane
                (wholes[i], widthAxis,
                 halves[0][i] = new ArrayList<BoundedParam2D>(),
                 halves[1][i] = new ArrayList<BoundedParam2D>());
        }

        if (debug) {
            for (ArrayList<BoundedParam2D>[] half: halves) {
                System.out.println("Half: ");
                System.out.print("->a");
                for (BoundedParam2D p:  half[0]) {
                    System.out.print("[" + p.getMinT() + ", " + p.getMaxT() + "]");
                }
                System.out.println();
                System.out.print("->b");
                for (BoundedParam2D p:  half[1]) {
                    System.out.print("[" + p.getMinT() + ", " + p.getMaxT() + "]");
                }
                System.out.println();
            }
        }

        for (ArrayList<BoundedParam2D>[] half: halves) {
            for (BoundedParam2D pa:  half[0]) {
                for (BoundedParam2D pb:  half[1]) {
                    if (stepCnt > maxSteps) {
                        return stepCnt;
                    }
                    // Swap a and b in the next recursion step.
                    stepCnt +=  intersections(is, pb, pa, maxError,
                                              maxSteps - stepCnt);
                }
            }
        }
        return stepCnt;
    }

    /** Compute a lower bound on the minimum distance from c to p by
        computing the distance from p to c.getBounds(). (The "0"
        reflects that the bounds of the zeroth derivative -- that is,
        the function itself -- are used to determine the answer.) */
    public static double distanceLowerBound0(BoundedParam2D c, Point2D p) {
        return Geom.distance(p, c.getBounds());
    }

    /** Compute the distance from p to this curve to within maxError
        of the correct value, unless it takes more than maxSteps
        to compute. In that case, just return the best estimate known
        at that time.

        See OffsetParam2D.distance(ArrayList<BoundedParam2D,
        Point2D, double, double) for a more efficient way to measure
        the distance to the nearest of several curves.
    */
    public static CurveDistanceRange distance(BoundedParam2D c, Point2D p,
            double maxError, int maxSteps) {
        ArrayList<BoundedParam2D> cps = new ArrayList<>();
        cps.add(c);

        return distance(cps, p, maxError, maxSteps);
    }

    public static BoundedParam2D[] subdivide(BoundedParam2D c) {
        double middle = (c.getMinT() + c.getMaxT()) / 2;
        return new BoundedParam2D[]
            { c.createSubset(c.getMinT(), middle),
              c.createSubset(middle, c.getMaxT()) };
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        BoundedParam2D s1 = BezierParam2D.create
            (new Point2D.Double(0, 0), new Point2D.Double(1, 1));
        BoundedParam2D s2 = BezierParam2D.create
            (new Point2D.Double(0, 1), new Point2D.Double(2, 0));
        BoundedParam2D q1 = BezierParam2D.create
            (new Point2D.Double(0, 1),
             new Point2D.Double(0.5, -1),
             new Point2D.Double(1, 1));
        BoundedParam2D q2 = BezierParam2D.create
            (new Point2D.Double(0, 0),
             new Point2D.Double(0.5, 2),
             new Point2D.Double(1, 0));
        BoundedParam2D q3 = BezierParam2D.create
            (new Point2D.Double(0, 0.01),
             new Point2D.Double(0.5, 1.99),
             new Point2D.Double(1, 0.01));
        BoundedParam2D q4 = BezierParam2D.create
            (new Point2D.Double(0, 0.5),
             new Point2D.Double(0.5, 1.5),
             new Point2D.Double(1, 0.5));
        try {
            for (Point2D p: intersections(q1, q2, 1e-5, 20)) {
                System.out.println(Geom.toString(p));
            }
        } catch (FailedToConvergeException x) {
            System.err.println(x);
        }
    }

    static public AdaptiveRombergIntegral lengthIntegral(BoundedParam2D c) {
        DoubleUnaryOperator dsdt = new Param2Ds.DLengthDT(c);
        return new AdaptiveRombergIntegral(dsdt, c.getMinT(), c.getMaxT());
    }

    static public void verifyBounds(BoundedParam2D c) {
        Rectangle2D bounds = c.getBounds();
        ArrayList<Point2D.Double> points = new ArrayList<>();
        int pointCnt = 20;
        for (int i = 0; i < pointCnt; ++i) {
            double t = (c.getMaxT() - c.getMinT()) * i / pointCnt;
            Point2D.Double p = c.getLocation(t);
            points.add(p);
            if (!bounds.contains(p)) {
                throw new IllegalStateException("location(" + t + ") = " + Geom.toString(p)
                        + " out of bounds for " + c);
            }
        }
        Rectangle2D.Double bounds2 = Geom.bounds(points.toArray(new Point2D.Double[0]));
        if (bounds2.width * 0.8 < bounds.getWidth()
                || bounds2.height * 0.8 < bounds.getHeight()) {
            System.out.println("Suspect wide bounds: " +
                    Geom.toString(bounds) + ", verified " + Geom.toString(bounds2));
        }
    }
}
