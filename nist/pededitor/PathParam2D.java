package gov.nist.pededitor;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/** CurveParameterization2D that can be constructed from a
    PathIterator. The individual segments of the PathIterator are
    converted into OffsetParameterization2Ds.

    An initial moveTo followed by a lineTo is deleted. That permits
    the t values to line up, so the numbering of the points used to
    generate a path may be identical to the numbering of the
    parameterization. So a t value of 5.7 refers to a point on
    segments.get(5), and it's partway between points[5] and points[6].

    For example, suppose the path consists of a moveTo followed by a
    lineTo, another lineTo, and then a final moveTo. A
    PointParameterization2D is initially added to the "segments" list
    to mark the destination of the first moveTo, just in case the path
    ends right away, but the PointParameterization2D is later removed.
    The SegmentParameterization2D that represents the first line
    segment starts at the moveTo destination at t = 0 (so the moveTo
    destination is still remembered even after the
    PointParameterization2D segment is removed), and ends at the
    lineTo destination at t = 2. The second segment, corresponding to
    the second lineTo, is assigned an offset of 1, so it ranges from
    t=1 to t=2, overlapping with the previous segment at t=1, but
    that's OK because the two segments overlap at the same point. The
    second moveTo is assigned an offset of 3, so it goes from t=3 to
    t=3. The parameterization arbitrarily treats t values greater than
    2 but less than 3 as representing that last moveTo point as well.
    It all works out so that getLocation(0), getLocation(1),
    getLocation(2), and getLocation(3) represent the 4 control points.
    t values in (i, i+1) represent locations on segment #i; a t value
    of 0 represents a point on segment #0; and the t value
    (segments.size()) represents the endPoint of segment #1
    segments.size() -1.
 */
public class PathParam2D
    implements Parameterization2D,
               Iterable<OffsetParam2D> {
    ArrayList<OffsetParam2D> segments = new ArrayList<>();
    double t0 = 0;
    double t1 = 0;

    public PathParam2D() {
    }

    public PathParam2D(PathIterator pit) {
        /** position of last moveTo */
        Point2D.Double lastMove = null;
        /** Current position */
        Point2D.Double p = null;
        double[] coords = new double[6];

        for (int pointCnt = 0; !pit.isDone(); ++pointCnt, pit.next()) {
            Point2D.Double op = p;
            Parameterization2D segment;
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
                segment = new SegmentParam2D(op, p, 0, 1);
                break;
            case PathIterator.SEG_QUADTO:
                p = new Point2D.Double(coords[2], coords[3]);
                segment = new QuadParam2D
                    (op, new Point2D.Double(coords[0], coords[1]), p, 0, 1);
                break;
            case PathIterator.SEG_CUBICTO:
                p = new Point2D.Double(coords[4], coords[5]);
                segment = new CubicParam2D
                    (op,
                     new Point2D.Double(coords[0], coords[1]),
                     new Point2D.Double(coords[2], coords[3]),
                     p, 0, 1);
                break;
            case PathIterator.SEG_CLOSE:
                p = lastMove;
                segment = new SegmentParam2D(op, p, 0, 1);
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
        int segNo = 0;

        Iterator() {}

        Iterator(int segNo) {
            this.segNo = segNo;
        }

        /** @return the next segment of this path. */
        @Override public OffsetParam2D next() {
            return segments.get(segNo++);
        }

        @Override public boolean hasNext() {
            return segNo < segments.size();
        }

        @Override public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override public Point2D.Double getStart() {
        return getLocation(getMinT());
    }

    @Override public Point2D.Double getEnd() {
        return getLocation(getMaxT());
    }

    @Override public CurveDistance distance(Point2D p, double t) {
        Point2D.Double pt = getLocation(t);
        return new CurveDistance(t, pt, pt.distance(p));
    }

    @SuppressWarnings("unchecked")
	@Override public PathParam2D clone() {
        PathParam2D output = new PathParam2D();
        output.segments = (ArrayList<OffsetParam2D>)
            segments.clone();
        output.t0 = t0;
        output.t1 = t1;
        return output;
    }

    @Override public Iterator iterator() {
        int segNo = (t0 == 0) ? 0 : ((int) Math.ceil(t0) - 1);
        return new Iterator(segNo);
    }

    @Override public double getMinT() { return t0; }
    @Override public double getMaxT() { return t1; }

    @Override public void setMinT(double t) {
        // Change the segment containing the old t0 value back to normal.
        OffsetParam2D segment = getSegment(t0);
        segment.setMinT(segment.offset);

        // Make the change.
        getSegment(t0).setMinT(t);
        t0 = t;
    }

    @Override public void setMaxT(double t) {
        // Change the segment containing the old t1 value back to normal.
        getSegment(t1).setMaxT((int) Math.ceil(t1));

        // Make the change.
        getSegment(t1).setMaxT(t);
        t1 = t;
    }

    /** Return the segment that t belongs to. If t lies on the joint
        between two segments, return the segment on the left. */
    OffsetParam2D getSegment(double t) {
        return segments.get((t == 0) ? 0 : ((int) Math.ceil(t) - 1));
    }

    @Override
	public Point2D.Double getLocation(double t) {
        return getSegment(t).getLocation(t);
    }

    public CurveDistance distance(Point2D.Double p, double t) {
        return getSegment(t).distance(p, t);
    }

    @Override
	public Point2D.Double getDerivative(double t) {
        return getSegment(t).getDerivative(t);
    }

    @Override public double getNextVertex(double t) {
        return Math.floor(t) + 1;
    }

    @Override public double getLastVertex(double t) {
        return Math.floor(t);
    }

    @Override public CurveDistanceRange distance(Point2D p) {
        CurveDistanceRange minDist = null;
        for (Parameterization2D segment: this) {
            CurveDistanceRange dist = segment.distance(p);
            minDist = CurveDistanceRange.min(minDist, dist);
        }

        return minDist;
    }

    @Override public CurveDistance vertexDistance(Point2D p) {
        CurveDistance minDist = null;

        for (OffsetParam2D segment: this) {
            CurveDistance dist;
            double minT = segment.getMinT();
            if (minT >= t0) {
                dist = segment.distance(p, minT);
                minDist = CurveDistance.min(minDist, dist);
            }
            double maxT = segment.getMaxT();
            if (maxT <= t1) {
                dist = segment.distance(p, maxT);
                minDist = CurveDistance.min(minDist, dist);
            }
        }

        return minDist;
    }

    @Override public PathParam2D derivative() {
        PathParam2D output = new PathParam2D();
        for (OffsetParam2D segment: this) {
            output.segments.add(segment.derivative());
        }
        return output;
    }

    @Override public Rectangle2D.Double getBounds() {
        Rectangle2D output = null;
        for (Parameterization2D segment: this) {
            Rectangle2D b = segment.getBounds();
            if (output == null) {
                output = b;
            } else {
                output.add(b);
            }
        }
        return new Rectangle2D.Double
            (output.getX(), output.getY(), 
             output.getWidth(), output.getHeight());
    }

    /** Compute the distance from p to this curve to within maxError
        of the correct value, unless it takes more than maxIterations
        to compute. In that case, just return the best estimate known
        at that time. */
    @Override
	public CurveDistanceRange distance(Point2D p, double maxError,
                                  double maxIterations) {
        ArrayList<Parameterization2D> segs = new ArrayList<>();
        for (Parameterization2D segment: this) {
            segs.add(segment);
        }
        return Parameterization2Ds.distance
            (segs, p, maxError, maxIterations);
    }

    @Override public double[] segIntersections(Line2D segment) {
        ArrayList<Double> res = new ArrayList<>();
        for (Parameterization2D c: this) {
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

    @Override public double[] lineIntersections(Line2D segment) {
        ArrayList<Double> res = new ArrayList<>();
        for (Parameterization2D c: this) {
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
        if (getMinT() != 0 || getMaxT() != 1) {
            s.append(" t in [" + getMinT() + ", " + getMaxT() + "]");
        }
        s.append("]");
        return s.toString();
    }

    @Override public Parameterization2D[] subdivide() {
        if (segments.size() == 1) {
            return segments.get(0).subdivide();
        } else {
            return segments.toArray(new OffsetParam2D[0]);
        }
    }
}
