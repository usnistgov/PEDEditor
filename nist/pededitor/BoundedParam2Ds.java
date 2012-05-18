package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class Parameterization2Ds {
    public static double getNearestVertex(Parameterization2D c, double t) {
        Point2D p = c.getLocation(t);
        if (p == null) {
            return 0;
        }

        double t1 = c.getLastVertex(t);
        double t2 = c.getNextVertex(t);
        return (p.distanceSq(c.getLocation(t1))
                <= p.distanceSq(c.getLocation(t2))) ? t1 : t2;
    }

    /** If either the derivative() function is not reliable or its
        getBounds() can return values with large relative error, then
        don't use this, use distanceLowerBound0() instead. */
    public static double distanceLowerBound
        (Parameterization2D c, Point2D p) {
        return Math.max(distanceLowerBound0(c, p),
                        distanceLowerBound1(c, p));
    }

    public static CurveDistance distance
        (ArrayList<Parameterization2D> cps, Point2D p,
         double maxError, double maxIterations) {

        if (cps.size() == 0) {
            return null;
        }

        // Initialize minDist with the distance from an arbitrary
        // point.
        Parameterization2D c0 = cps.get(0);
        CurveDistance minDist = c0.distance(p, c0.getMinT());

        for (;;) {
            // Determine the new upper bound on distances, and discard
            // every curve that exceeds that upper bound.

            // Compute the proper minDist value for this stage.
            ArrayList<CurveDistance> cds = new ArrayList<>();
            for (Parameterization2D cp: cps) {
                CurveDistance dist = cp.distance(p);
                cds.add(dist);
                minDist = CurveDistance.min(minDist, dist);
            }

            double cutoffDistance = minDist.distance - maxError;

            // Create a list of curves whose distance from p may be
            // less than cutoffDistance. Bisect those curves to
            // improve accuracy and reduce the difference between the
            // upper and lower bounds on the minimum distance from p
            // to the curves.

            double dlb = minDist.distance; // distance lower bound

            ArrayList<Parameterization2D> ncps = new ArrayList<>();
            for (int segNo = 0; segNo < cps.size(); ++segNo) {
                Parameterization2D cp = cps.get(segNo);
                CurveDistance dist = cp.distance(p);

                // this distance lower bound
                double thisdlb = dist.distance;

                if (dist instanceof CurveDistanceRange) {
                    thisdlb = ((CurveDistanceRange) dist).minDistance;
                }

                dlb = Math.min(dlb, thisdlb);
                if (thisdlb >= cutoffDistance) {
                    continue;
                }

                // Bisect this curve to improve approxmation accuracy.
                double middle = (cp.getMinT() + cp.getMaxT()) / 2;

                {
                    Parameterization2D ncp = cp.clone();
                    ncp.setMaxT(middle);
                    ncps.add(ncp);
                }

                {
                    Parameterization2D ncp = cp.clone();
                    ncp.setMinT(middle);
                    ncps.add(ncp);
                }
            }

            maxIterations -= cps.size();
            if (ncps.size() == 0 /* Error <= maxError */
                || maxIterations < 0 /* Too many iterations */) {
                return new CurveDistanceRange
                    (minDist.t, minDist.point, minDist.distance, dlb);
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
        (Point2D p, Point2D f0, double deltaT,
         Rectangle2D dfdtBounds) {
        if (deltaT == 0) {
            return p.distance(f0);
        }

        Point2D.Double pn = new Point2D.Double
            (p.getX() - f0.getX(), p.getY() - f0.getY());
        Rectangle2D.Double b = new Rectangle2D.Double
            (dfdtBounds.getX() * deltaT,
             dfdtBounds.getY() * deltaT,
             dfdtBounds.getMaxX() * deltaT,
             dfdtBounds.getMaxY() * deltaT);

        if (b.contains(pn)) {
            return 0;
        }

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

        // Figure out whether pn might equal to f(t) for some t. That
        // is possible if at least one of the segments passes above
        // pn, and at least one of the segments passes below pn.

        boolean segmentAbove = false;
        boolean segmentBelow = false;

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

        // Find the minimum distance from pn to any segments.
        double minDist = -1;

        for (Line2D segment: segments) {
            double d = segment.ptSegDist(pn);
            if (minDist == -1 || d < minDist) {
                minDist = d;
            }
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
        (Parameterization2D c, Point2D p) {
        double centerT = (c.getMinT() + c.getMaxT()) / 2;
        Point2D.Double f0 = c.getLocation(centerT);
        double deltaT = centerT - c.getMinT();
        Rectangle2D.Double dfdtBounds = c.derivative().getBounds();
        return distanceLowerBound1(p, f0, deltaT, dfdtBounds);
    }

    /** Compute a lower bound on the minimum distance from c to p by
        computing the distance from p to c.getBounds(). (The "0"
        reflects that the bounds of the zeroth derivative -- that is,
        the function itself -- are used to determine the answer.) */
    public static double distanceLowerBound0
        (Parameterization2D c, Point2D p) {
        return Duh.distance(p, c.getBounds());
    }

    /** Compute the distance from p to this curve to within maxError
        of the correct value, unless it takes more than maxIterations
        to compute. In that case, just return the best estimate known
        at that time. */
    public static CurveDistance distance
        (Parameterization2D c, Point2D p,
         double maxError, double maxIterations) {
        ArrayList<Parameterization2D> cps = new ArrayList<>();
        cps.add(c);

        return distance(cps, p, maxError, maxIterations);
    }

    public static CurveDistance vertexDistance
        (Parameterization2D c, Point2D p) {
        double t = c.getMinT();
        double maxT = c.getMaxT();
        CurveDistance minDist = c.distance(p, t);
        do {
            double ot = t;
            t = c.getNextVertex(t);
            if (t == ot) {
                break;
            }
            minDist = minDist.minWith(c.distance(p,t));
        } while (t < maxT);
        return minDist;
    }

}
