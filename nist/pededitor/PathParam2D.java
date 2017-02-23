/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** Param2D that can be constructed from a Shape or PathIterator. The
    individual segments of the PathIterator are converted into
    OffsetParam2Ds.

    An initial moveTo followed by a lineTo is deleted. That permits
    the t values to line up, so the numbering of the points used to
    generate a path may be identical to the numbering of the
    parameterization. So a t value of 5.7 refers to a point on
    segments.get(5), and it's partway between points[5] and points[6].

    For example, suppose the path consists of a moveTo followed by a
    lineTo, another lineTo, and then a final moveTo. A PointParam2D is
    initially added to the "segments" list to mark the destination of
    the first moveTo, just in case the path ends right away, but the
    PointParam2D is later removed. The SegmentParam2D that represents
    the first line segment starts at the moveTo destination at t = 0
    (so the moveTo destination is still remembered even after the
    PointParam2D segment is removed), and ends at the lineTo
    destination at t = 2. The second segment, corresponding to the
    second lineTo, is assigned an offset of 1, so it ranges from t=1
    to t=2, overlapping with the previous segment at t=1, but that's
    OK because the two segments overlap at the same point. The second
    moveTo is assigned an offset of 3, so it goes from t=3 to t=3. The
    parameterization arbitrarily treats t values greater than 2 but
    less than 3 as representing that last moveTo point as well. It all
    works out so that getLocation(0), getLocation(1), getLocation(2),
    and getLocation(3) represent the 4 control points. t values in (i,
    i+1) represent locations on segment #i; a t value of 0 represents
    a point on segment #0; and the t value (segments.size())
    represents the endpoint of segment #segments.size()-1).
 */
public class PathParam2D extends Param2DAdapter
    implements Param2D, Iterable<OffsetParam2D> {
    ArrayList<OffsetParam2D> segments = new ArrayList<>();
    double t0 = 0;
    double t1 = 0;

    PathParam2D(double t0, double t1) {
        this.t0 = t0;
        this.t1 = t1;
    }

    /** Create a BoundedParam2D that follows the given path. */
    public static Param2DBounder create(PathIterator pit) {
        PathParam2D p = new PathParam2D(pit);
        return new Param2DBounder(p, p.t0, p.t1);
    }

    /** Create a BoundedParam2D that follows shape's outline as
        returned by getPathIterator(). */
    public static Param2DBounder create(Shape shape) {
        return create(shape.getPathIterator(null));
    }

    public PathParam2D(PathIterator pit) {
        /** position of last moveTo */
        Point2D.Double lastMove = null;
        /** Current position */
        Point2D.Double p = null;
        double[] coords = new double[6];

        for (int pointCnt = 0; !pit.isDone(); ++pointCnt, pit.next()) {
            Point2D.Double op = p;
            BoundedParam2D segment;
            int offsetAdjustment = 0;

            switch (pit.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                lastMove = p = new Point2D.Double(coords[0], coords[1]);
                segment = new PointParam2D(p);
                if (pointCnt > 0) {
                    // Imagine this moveTo was preceded by a lineTo.
                    // The t value segments.size() already represents
                    // the point that ends the lineTo, so we need to
                    // use a different t value, segments.size() + 1,
                    // to mark the destination of the moveTo. So add
                    // an extra 1 to the offset of this moveTo
                    // segment.

                    offsetAdjustment = 1;
                }
                break;
            case PathIterator.SEG_LINETO:
                p = new Point2D.Double(coords[0], coords[1]);
                segment = BezierParam2D.create(op, p);
                break;
            case PathIterator.SEG_QUADTO:
                p = new Point2D.Double(coords[2], coords[3]);
                segment = BezierParam2D.create
                    (op, new Point2D.Double(coords[0], coords[1]), p);
                break;
            case PathIterator.SEG_CUBICTO:
                p = new Point2D.Double(coords[4], coords[5]);
                segment = BezierParam2D.create
                    (op,
                     new Point2D.Double(coords[0], coords[1]),
                     new Point2D.Double(coords[2], coords[3]),
                     p);
                break;
            case PathIterator.SEG_CLOSE:
                p = lastMove;
                segment = BezierParam2D.create(op, p);
                break;
            default:
                throw new IllegalStateException
                    ("Unrecognized segment type " + pit.currentSegment(coords));
            }

            if (pointCnt == 1) { // Delete the initial moveto segment.
                segments.remove(0);
            }

            segments.add(new OffsetParam2D
                         (segment, segments.size() + offsetAdjustment));
        }

        if (!segments.isEmpty()) {
            t1 = segments.get(segments.size() - 1).getMaxT();
        }
    }

    public PathParam2D(Shape shape) {
        this(shape.getPathIterator(null));
    }

    class Iterator implements java.util.Iterator<OffsetParam2D> {
        double t0;
        double t1;
        int segNo;
        int firstSegNo;
        int lastSegNo;

        Iterator(double t0, double t1) {
            this.t0 = t0;
            this.t1 = t1;
            segNo = firstSegNo = getSegmentNo(t0);
            lastSegNo = (t1 == 0) ? -1 : getSegmentNo(t1);
        }

        /** @return the next segment of this path. */
        @Override public OffsetParam2D next() {
            OffsetParam2D seg = segments.get(segNo);
            double minT = (segNo == firstSegNo) ? t0 : seg.getMinT();
            double maxT = (segNo == lastSegNo) ? t1 : seg.getMaxT();
            if (minT != seg.getMinT() || maxT != seg.getMaxT()) {
                seg = seg.createSubset(minT, maxT);
            }
            ++segNo;
            return seg;
        }

        @Override public boolean hasNext() {
            return segNo <= lastSegNo;
        }

        @Override public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class Subset implements Iterable<OffsetParam2D> {
        double t0;
        double t1;

        Subset(double t0, double t1) {
            this.t0 = t0;
            this.t1 = t1;
        }

        @Override public Iterator iterator() {
            return new Iterator(t0, t1);
        }
    }

    /** Return an Iterable over a set of BoundedParam2D objects that
        include every OffsetParam2D, bounded as necessary, that covers
        [t0, t1]. */
    public Subset subsetIterable(double t0, double t1) {
        return new Subset(t0, t1);
    }

    @Override public Iterator iterator() {
        return new Iterator(t0, t1);
    }

    /** Return the segment that t belongs to. If t lies on the joint
        between two segments, return the segment on the left. */
    int getSegmentNo(double t) {
        return (t == 0) ? 0 : ((int) Math.ceil(t) - 1);
    }

    /** Return the segment that t belongs to. If t lies on the joint
        between two segments, return the segment on the left. */
    OffsetParam2D getSegment(double t) {
        return segments.get((t == 0) ? 0 : ((int) Math.ceil(t) - 1));
    }

    @Override public Point2D.Double getLocation(double t) {
        return getSegment(t).getLocation(t);
    }

    public CurveDistance distance(Point2D.Double p, double t) {
        return getSegment(t).distance(p, t);
    }

    @Override public Point2D.Double getDerivative(double t) {
        return getSegment(t).getDerivative(t);
    }

    @Override public double getNextVertex(double t) {
        return Math.floor(t) + 1;
    }

    @Override public double getLastVertex(double t) {
        return Math.floor(t);
    }

    /** Return the distance between p and s, or return a distance of 0
        if s.contains(p). */
    static public CurveDistanceRange distance
        (Shape s, Point2D p, double maxError, int maxSteps) {
        if (s.contains(p)) {
            return new CurveDistanceRange
                (Double.NaN, new Point2D.Double(p.getX(), p.getY()), 0, 0);
        } else {
            return borderDistance(s, p, maxError, maxSteps);
        }
    }

    /** Return the distance from p to the closest point on the border
        of s. */
    static public CurveDistanceRange borderDistance
        (Shape s, Point2D p, double maxError, int maxSteps) {
        return create(s).distance(p, maxError, maxSteps);
    }

    /** This probably isn't what you want, because there are no a
        priori bounds on the error of its result. */
    @Override public CurveDistanceRange distance
        (Point2D p, double t0, double t1) {
        CurveDistanceRange minDist = null;
        for (BoundedParam2D segment: subsetIterable(t0, t1)) {
            CurveDistanceRange dist = segment.distance(p);
            minDist = CurveDistanceRange.min(minDist, dist);
        }

        return minDist;
    }

    @Override protected PathParam2D computeDerivative() {
        PathParam2D output = new PathParam2D(t0, t1);
        for (OffsetParam2D segment: this) {
            output.segments.add(segment.derivative());
        }
        return output;
    }

    @Override public Rectangle2D.Double getBounds(double t0, double t1) {
        Rectangle2D res = null;
        for (BoundedParam2D segment: subsetIterable(t0, t1)) {
            Rectangle2D b = segment.getBounds();
            if (res == null) {
                res = b;
            } else {
                res.add(b);
            }
        }
        return (res == null) ? null
            : new Rectangle2D.Double
            (res.getX(), res.getY(), 
             res.getWidth(), res.getHeight());
    }

    @Override public double[] getBounds
        (double xc, double yc, double t0, double t1) {
        double min = Double.NaN;
        double max = Double.NaN;

        for (BoundedParam2D segment: subsetIterable(t0, t1)) {
            double[] thisb = segment.getLinearFunctionBounds(xc, yc);
            double thismin = thisb[0];
            double thismax = thisb[1];
            if (Double.isNaN(min) || thismin < min) {
                min = thismin;
            }
            if (Double.isNaN(max) || thismax > max) {
                max = thismax;
            }
        }
        return new double[] { min, max };
    }

    /** Compute the distance from p to this curve to within maxError
        of the correct value, unless it takes more than maxSteps
        to compute. In that case, just return the best estimate known
        at that time. */
    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, int maxSteps,
         double t0, double t1) {
        ArrayList<BoundedParam2D> segs = new ArrayList<>();
        for (BoundedParam2D segment: subsetIterable(t0, t1)) {
            segs.add(segment);
        }
        return BoundedParam2Ds.distance
            (segs, p, maxError, maxSteps);
    }

    @Override public double[] segIntersections
        (Line2D segment, double t0, double t1) {
        ArrayList<Double> res = new ArrayList<>();
        for (BoundedParam2D c: subsetIterable(t0, t1)) {
            for (double t: c.segIntersections(segment)) {
                if (res.size() == 0 || res.get(res.size() -1) < t) {
                    res.add(t);
                }
            }
        }
        double[] res0 = new double[res.size()];
        for (int i = 0; i < res.size(); ++i) {
            res0[i] = res.get(i);
        }
        return res0;
    }

    @Override public double[] lineIntersections
        (Line2D segment, double t0, double t1) {
        ArrayList<Double> res = new ArrayList<>();
        for (BoundedParam2D c: subsetIterable(t0, t1)) {
            for (double t: c.lineIntersections(segment)) {
                if (res.size() == 0 || res.get(res.size() -1) < t) {
                    res.add(t);
                }
            }
        }
        double[] res0 = new double[res.size()];
        for (int i = 0; i < res.size(); ++i) {
            res0[i] = res.get(i);
        }
        return res0;
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName() + "[");
        boolean first = true;
        for (OffsetParam2D c: this) {
            if (!first) {
                s.append(", ");
            }
            s.append(c);
            first = false;
        }
        s.append("]");
        return s.toString();
    }

    @Override public BoundedParam2D createSubset(double t0, double t1) {
        if (Math.ceil(t1) <= t0 + 1) {
            // This is a subset of a single segment, so for efficiency
            // access that segment directly.
            return getSegment(t1).createSubset(t0, t1);
        } else {
            return super.createSubset(t0, t1);
        }
    }

    @Override public BoundedParam2D[] subdivide(double t0, double t1) {
        if (Math.ceil(t1) <= t0 + 1) {
            return super.subdivide(t0, t1);
        } else if (t1 - t0 > 8) {
            // Divide roughly in two
            return subdivide(t0, Math.rint((t1 + t0) / 2), t1);
        } else {
            ArrayList<BoundedParam2D> res = new ArrayList<>();
            int seg0 = (int) Math.floor(t0);
            int seg1 = getSegmentNo(t1);
            res.add(createSubset(t0, Math.floor(t0) + 1));
            for (int seg = seg0 + 1; seg < seg1; ++seg) {
                res.add(createSubset(seg, seg + 1));
            }
            res.add(createSubset(seg1, t1));
        }
        return segments.toArray(new BoundedParam2D[0]);
    }

    @Override public BoundedParam2D[] curvedSegments(double t0, double t1) {
        ArrayList<BoundedParam2D> res = new ArrayList<>();

        // TODO Collapse contiguous curved sections. Shouldn't be
        // hard, but I want to debug it this way first.
        for (OffsetParam2D seg: segments) {
            double minT = Math.max(t0, seg.getMinT());
            double maxT = Math.max(t1, seg.getMaxT());
            if (minT < maxT) {
                for (BoundedParam2D bp: seg.curvedSegments(minT, maxT)) {
                    res.add(bp);
                }
            }
        }
        return res.toArray(new BoundedParam2D[0]);
    }

    @Override public BoundedParam2D[] straightSegments(double t0, double t1) {
        ArrayList<BoundedParam2D> res = new ArrayList<>();
        for (OffsetParam2D seg: segments) {
            double minT = Math.max(t0, seg.getMinT());
            double maxT = Math.min(t1, seg.getMaxT());

            if (minT < maxT) {
                for (BoundedParam2D bp: seg.straightSegments(minT, maxT)) {
                    res.add(bp);
                }
            }
        }
        return res.toArray(new BoundedParam2D[0]);
    }

    @Override public PathParam2D createTransformed(AffineTransform xform) {
        PathParam2D res = new PathParam2D(t0, t1);
        for (OffsetParam2D segment: segments) {
            res.segments.add(segment.createTransformed(xform));
        }
        return res;
    }

    @Override public Estimate length(double t0, double t1) {
        Estimate total = new Estimate(0);
        for (BoundedParam2D seg: subsetIterable(t0, t1)) {
            total.add(seg.length());
        }
        return total;
    }

    @Override public double area(double t0, double t1) {
        double total = 0;
        for (BoundedParam2D seg: subsetIterable(t0, t1)) {
            total += seg.area();
        }
        return total;
    }

    static class CurveLength {
        BoundedParam2D curve;
        Estimate length;
        CurveLength(BoundedParam2D curve, Estimate length) {
            this.curve = curve;
            this.length = length;
        }
    }

    @Override public Estimate length(double absoluteError,
                                          double relativeError, int maxSteps,
                                          double t0, double t1) {
        Precision p = new Precision();
        p.absoluteError = absoluteError;
        p.relativeError = relativeError;

        Estimate roughLen = new Estimate(0);
        ArrayList<CurveLength> cls = new ArrayList<>();
        for (BoundedParam2D seg: subsetIterable(t0, t1)) {
            Estimate len = seg.length();
            cls.add(new CurveLength(seg, len));
            roughLen.add(len);
        }

        if (p.closeEnough(roughLen)) {
            return roughLen;
        }

        if (cls.size() == 1) {
            return cls.get(0).curve.length(absoluteError, relativeError, maxSteps);
        }

        if (roughLen.relativeError() > 0.8) {
            // This is a poor solution, but it's also a situation that
            // does not happen for me, so I won't optimize it.
            return super.length(absoluteError, relativeError, maxSteps, t0, t1);
        }

        // The allowable total absolute error is at least this much:
        double maxError = p.maxError(roughLen.lowerBound);
        relativeError = Math.max(relativeError, maxError / roughLen.upperBound);

        NumericEstimate res = new NumericEstimate(0);

        // Handle the segments whose relative error is already
        // acceptable immediately. That might free up more error
        // allowance for the others.

        // Curves whose length estimates are not precise enough will
        // be handled later.
        ArrayList<CurveLength> laters = new ArrayList<>();
        Estimate remainderLength = new Estimate(0);
        for (CurveLength cl: cls) {
            if (cl.length.relativeError() <= relativeError) {
                res.add(cl.length);
            } else {
                laters.add(cl);
                remainderLength.add(cl.length);
            }
        }
        cls = laters;

        // Sort the segments in decreasing order of length, and
        // estimate the longer segments' lengths first. Imagine if you
        // have two segments, one 100x longer than the other: a
        // precise measurement of the longer segment makes a very
        // crude (in terms of relative error) measurement of the
        // shorter one allowable, but a precise measurement of the
        // shorter segment hardly improves the error allowance on the
        // longer one at all.

        Collections.sort(cls, new Comparator<CurveLength>() {
                @Override
				public int compare(CurveLength a, CurveLength b) {
                    return (int) Math.signum(a.length.value - b.length.value);
                }
           });

        for (int i = 0; i < cls.size(); ++i) {
            CurveLength cl = cls.get(i);
            double errorLeft = maxError - res.width() / 2;
            if (errorLeft <= 0) {
                // Error allowance used up prematurely. This shouldn't
                // happen often.
                while (i < cls.size()) {
                    res.add(cls.get(i).length);
                    return res;
                }
            }
            // The expected length of this segment as a fraction of
            // the length of all remaining segments.
            double remainderFraction = cl.length.value / remainderLength.value;
            double thisAbsoluteError = errorLeft * remainderFraction;
            res.add(cl.curve.length(thisAbsoluteError, 0,
                                    maxSteps - res.sampleCnt));
            remainderLength.upperBound -= cl.length.upperBound;
            remainderLength.lowerBound -= cl.length.lowerBound;
            remainderLength.value -= cl.length.value;
        }
        return res;
    }
}
