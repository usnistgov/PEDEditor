package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class Parameterization2Ds {
    static final boolean debug = false;
    /** Return the t value that is closest to t that is also in the
        domain of c. */
    public static double constrainToDomain(Parameterization2D c, double t) {
        double minT = c.getMinT();
        double maxT = c.getMinT();
        return (t < minT) ? minT
            : (t > maxT) ? maxT : t;
    }
        
    public static double getNearestVertex(Parameterization2D c, double t) {
        Point2D p = c.getLocation(t);
        if (p == null) {
            return 0;
        }

        double t1 = c.getLastVertex(t);
        double t2 = c.getNextVertex(t);
        return
            (t2 > c.getMaxT()
             || p.distanceSq(c.getLocation(t1)) <= p.distanceSq(c.getLocation(t2)))
            ? t1 : t2;
    }

    /** If either the derivative() function is not reliable or its
        getBounds() can return values with large relative error, then
        use distanceLowerBound0() instead. */
    public static double distanceLowerBound
        (Parameterization2D c, Point2D p) {
        if (debug) {
            double d0 = distanceLowerBound0(c, p);
            double d1 = distanceLowerBound1(c, p);
            System.out.println("distanceLowerBound((" + c.getMinT() + ", " + c.getMaxT() + ") , " + p
                               + ") = max(" + d0 + ", " + d1 + ")");
        }
        return Math.max(distanceLowerBound0(c, p),
                        distanceLowerBound1(c, p));
    }

    static <T extends Parameterization2D> CurveDistanceRange distance
        (Iterable<T> ocps, Point2D p,
         double maxError, double maxIterations) {
    	ArrayList<Parameterization2D> cps = new ArrayList<>();
    	for (Parameterization2D cp: ocps) {
    		cps.add(cp);
    	}

        if (debug) {
            System.err.println("Sections are: ");
            for (Parameterization2D cp: cps) {
                System.err.println(cp);
            }
            System.err.println();
        }

        if (cps.size() == 0) {
            return null;
        }

        CurveDistanceRange minDist = null;
        for (int step = 0;; ++step) {
            if (debug) {
                System.err.println("Step " + step);
            }

            if (minDist != null) {
                // Initialize minDistance to an exact lower bound. The
                // lower bound might no longer be exact after the
                // following loop finishes).
                minDist.minDistance = minDist.distance;
            }

            // Compute the proper minDist value for this stage.
            ArrayList<CurveDistanceRange> cds = new ArrayList<>();
            for (Parameterization2D cp: cps) {
                CurveDistanceRange dist = cp.distance(p);
                cds.add(dist);

                if (debug) {
                    System.err.println("[" + cp.getMinT() + ", " + cp.getMaxT()
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
                System.err.println("Step " + step + ": dist = " + minDist
                                   + ", cutoff = " + cutoffDistance);
            }

            // Create a list of curves whose distance from p may be
            // less than cutoffDistance. Bisect those curves to
            // improve accuracy and reduce the difference between the
            // upper and lower bounds on the minimum distance from p
            // to the curves.

            ArrayList<Parameterization2D> ncps = new ArrayList<>();
            for (int segNo = 0; segNo < cps.size(); ++segNo) {
                Parameterization2D cp = cps.get(segNo);
                CurveDistanceRange dist = cds.get(segNo);

                if (debug) {
                    System.err.println("Seg#" + segNo + "[" + cp.getMinT() + ", " + cp.getMaxT()
                                       + "]: " + dist);
                }

                if (dist.minDistance >= cutoffDistance) {
                    continue;
                }

                if (debug) {
                    System.err.println("Bisecting [" + cp.getMinT() + ", "
                                       + cp.getMaxT() + "]");
                }

                // Subdivide this curve to improve approximation accuracy.
                Parameterization2D[] divisions = cp.subdivide();
                for (Parameterization2D param: divisions) {
                    ncps.add(param);
                }
            }

            maxIterations -= cps.size();
            if (ncps.size() == 0 /* Error <= maxError */
                || maxIterations < 0 /* Too many iterations */) {
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
        // possible if at least one of the segments passes above pn,
        // and at least one of the segments passes below pn.

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

        // Find the minimum distance from pn to b and -b.
        double minDist = Math.min
            (Duh.distance(pn, b),
             Duh.distance(pn,
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
            System.out.println("dlb1(" + Duh.toString(pn) + ", "
                               + Duh.toString(b) + ") = " + minDist);
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
        at that time.

        See OffsetParam2D.distance(ArrayList<Parameterization2D,
        Point2D, double, double) for a more efficient way to measure
        the distance to the nearest of several curves.
    */
    public static CurveDistanceRange distance
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

    public static Parameterization2D[] subdivide(Parameterization2D param) {
        double middle = (param.getMinT() + param.getMaxT()) / 2;
        Parameterization2D[] res = { param.clone(), param.clone() };
        res[0].setMaxT(middle);
        res[1].setMinT(middle);
        return res;
    }
}
