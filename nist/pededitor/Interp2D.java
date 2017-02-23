/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

/** An interface for curves generated from control points. */
public interface Interp2D extends TransformableParameterizable2D, Cloneable {

    Interp2D clone();
    @JsonIgnore Shape getShape();
    Point2D.Double[] getPoints();
    <T extends Point2D> void setPoints(List<T> points);

    default void transform(AffineTransform xform) {
        int size = this.size();
        for (int i = 0; i < size; ++i) {
            Point2D.Double p = get(i);
            xform.transform(p, p);
            set(i, p);
        }
    }

    @Override Interp2D createTransformed(AffineTransform xform);

    @Override default BoundedParam2D getParameterization(
            AffineTransform xform) {
        return createTransformed(xform).getParameterization();
    }


    /* Return control point #i. For closed curves, control points may
       be counted twice (such as the 0th point also being the point #
       size()). */
    Point2D.Double get(int vertexNo);
    /** Add the control point in the given position. */
    void add(int index, Point2D point);
    /** Remove the given control point. */
    void remove(int vertexNo);
    /** Replace the given control point, which must exist. */
    void set(int vertexNo, Point2D point);
    /* Return the number of control points without duplication (so for
       closed curves, the return trip to point #0 does not count). */
    int size();
    /* Return whether this curve is closed or not. */
    boolean isClosed();
    void setClosed(boolean closed);
    /* Return the maximum number of control points usable with this
     * object, or -1 for no maximum. */
    default int maxSize() {
        return -1;
    }
    /* Return the minimum number of control points usable with this
       object. */
    default int minSize() {
        return 1;
    }

    /* @param round If round is false, then when stroke is drawn, a
        cap setting of ROUND is changed to BUTT, and a join setting of
        ROUND is changed to MITER. */
    default void draw(Graphics2D g, StandardStroke lineStyle, double lineWidth,
            Color color, boolean round) {
        drawOutline(g, lineStyle, lineWidth, color, round);
    }

    default void drawOutline(Graphics2D g, StandardStroke lineStyle,
            double lineWidth, Color color, boolean round) {
        Color oldColor = g.getColor();
        if (color != null) {
            g.setColor(color);
        }

        if (lineWidth == 0) {
            return;
        }
        if (lineStyle == null) {
            throw new IllegalArgumentException("draw(): null lineStyle in " + this);
        }
        Shape shape = getShape();
        if (shape != null) {
            lineStyle.getStroke().draw(g, shape, lineWidth, round);
        }
        g.setColor(oldColor);
    }

    default void fill(Graphics2D g, Paint paint) {
        Paint oldPaint = null;
        try {
            oldPaint = g.getPaint();
            g.setPaint(paint);
            g.fill(getShape());
        } finally {
            g.setPaint(oldPaint);
        }
    }

    /** @return an array of all intersections between segment and
        this. */
    default Point2D.Double[] segIntersections(Line2D segment) {
        BoundedParam2D c = getParameterization();
        double[] ts = c.segIntersections(segment);
        Point2D.Double[] res = new Point2D.Double[ts.length];
        for (int i = 0; i < ts.length; ++i) {
            res[i] = c.getLocation(ts[i]);
        }
        return res;
    }

    /** @return an array of all intersections between segment and
        this. */
    default Point2D.Double[] lineIntersections(Line2D segment) {
        BoundedParam2D c = getParameterization();
        double[] ts = c.lineIntersections(segment);
        Point2D.Double[] res = new Point2D.Double[ts.length];
        for (int i = 0; i < ts.length; ++i) {
            res[i] = c.getLocation(ts[i]);
        }
        return res;
    }

    default Point2D.Double getLocation(double d) {
        return getParameterization().getLocation(d);
    }

    default Point2D.Double[] transformPoints(AffineTransform xform) {
        int cnt = size();
        Point2D.Double[] res = new Point2D.Double[cnt];
        for (int i = 0; i < cnt; ++i) {
            res[i] = new Point2D.Double();
            xform.transform(get(i), res[i]);
        }
        return res;
    }

    /** Add a new control point past the last one. */
    default int add(Point2D point) {
        int s = size();
        add(s, point);
        return s;
    }
    /** Remove the last point added. */
    default void remove() {
        remove(size() - 1);
    }

    /** @return the number of segments in this drawing. That equals
        the number of vertices minus 1 for open curves and closed
        curves with just 1 vertex, or the number of vertices for
        closed curves with at least 2 vertices. */
    @JsonIgnore default int getSegmentCnt() {
        int s = size();
        return (s >= 2 && isClosed()) ? s : (s - 1);
    }

    /** Return the point where this curve starts. */
    @JsonIgnore default Point2D.Double getStart() {
        return (size() == 0) ? null
            : (Point2D.Double) get(0).clone();
    }

    /** Return the point where this curve ends. Closed curves
        end where they start. */
    @JsonIgnore default Point2D.Double getEnd() {
        if (isClosed()) {
            return getStart();
        }

        int s = size();
        if (s == 0) {
            return null;
        }

        return (Point2D.Double) get(s-1);
    }

    default Point2D.Double getLocation(int i) {
        return get(i % size());
    }

    /* When something (such as a tie line) is anchored to
       getLocation(t), and one of the curve's control points is
       removed, you have to replace the old t value with its closest
       approximation on the new curve. Return that new t value. */
    default double newTIfVertexRemoved(double oldT, int vertexNo) {
        int oldVertexCnt = size();

        if (oldVertexCnt <= 2)
            return 0;

        if (vertexNo == oldVertexCnt - 1 && !isClosed())
            // Reset t values greater than vertexNo-1 to vertexNo-1.
            return (oldT > vertexNo-1) ? (vertexNo - 1) : oldT;

        // t values on previous segments that don't touch the deleted
        // vertex are left alone; t values on later segments that don't
        // touch the deleted vertex are decremented by 1; and the two
        // segments that touch the deleted vertex are combined into a
        // single segment number newSeg.

        int segCnt = getSegmentCnt();

        Point2D point = get(vertexNo);
        int prevSeg = (vertexNo > 0) ? (vertexNo - 1)
            : isClosed() ? (segCnt-1) : -1;
        Point2D previous = get((prevSeg >= 0) ? prevSeg : 0);
        int nextSeg = vertexNo;
        Point2D next = get
            ((!isClosed()  && (vertexNo == oldVertexCnt - 1))
             ? vertexNo
             : (vertexNo + 1));
        int newSeg = (vertexNo > 0) ? (vertexNo - 1)
            : isClosed() ? (segCnt - 2)
            : 0;

        // T values for segments prevSeg and nextSeg should be
        // combined into a single segment newSeg.

        double dist1 = point.distance(previous);
        double dist2 = point.distance(next);
        double splitT = dist1 / (dist1 + dist2);

        int segment = (int) Math.floor(oldT);
        double frac = oldT - segment;
        return (segment == prevSeg) ? (newSeg + frac * splitT)
            : (segment == nextSeg) ? (newSeg + splitT + frac * (1 - splitT))
            : (segment > vertexNo) ? (oldT-1) : oldT;
    }

    /* When something (such as a tie line) is anchored to
       getLocation(t), and a new control point is added, you have to
       replace the old T value with its closest approximation on the
       new curve. Return that new t value. */
    default double newTIfVertexAdded(double oldT, int vertexNo, Point2D point) {
        int segment = (int) Math.floor(oldT);
        if (segment >= vertexNo)
            return oldT + 1;
        if (segment != vertexNo - 1)
            return oldT;
        double frac = oldT - segment;
        int segCnt = getSegmentCnt();

        double dist1 = (vertexNo == 0) ? 0
            : point.distance(get(vertexNo-1));
        double dist2 = (vertexNo == segCnt) ? 0
            : point.distance(get(vertexNo));

        // For old segment vertexNo-1, map the t range [0, splitT] to
        // new segment vertexNo-1 range [0,1], and map the t range
        // (splitT, 1] to new segment vertexNo range [0,1]. If
        // vertexNo == segCnt-1 then segment vertexNo-1 never existed
        // before, so it doesn't matter what splitT value we use.
        double splitT = dist1 / (dist1 + dist2);
        return segment +
            ((frac <= splitT) ? (frac / splitT)
             : (1 + (frac - splitT) / (1.0 - splitT)));
    }

    /** @return An array of the indexes of the control points adjacent
       to the control point at the given index. This will contain up
       to 2 index values, but may contain less if this is an endpoint
       of an open curve or if size() &lt; 3. */
    default int[] adjacentVertexes(int vertexNo) {
        int s = size();
        if (s <= 1) {
            return new int[0];
        } else if (s >= 3 && isClosed()) {
            return new int[] { (vertexNo + s - 1) % s, (vertexNo + 1) % s };
        } else if (vertexNo == 0) {
            return new int[] {1};
        } else if (vertexNo == s - 1) {
            return new int[] {s-2};
        } else {
            return new int[] {vertexNo - 1, vertexNo + 1};
        }
    }

    /** Return true if this point looks like an endpoint of the curve. */
    default boolean isEndpoint(int vertexNo) {
        int s = size();
        return s <= 2 || (!isClosed() && (vertexNo == 0 || vertexNo == s-1));
    }

    default int tToIndex(double t) {
        return (int) Math.round(t);
    }

    default double indexToT(int index) {
        return index;
    }

    /** Return true if this is the t value of a vertex. */
    default boolean tIsVertex(double t) {
        return indexToT(tToIndex(t)) == t;
    }
}
