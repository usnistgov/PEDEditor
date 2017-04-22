/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Interpolation where control points may be individually marked
    as smoothed or un-smoothed. */
public class CuspInterp2D extends PointsInterp2D
    implements Smoothable, CurveCloseable {
    protected ArrayList<Boolean> smoothed = new ArrayList<>();
    protected boolean closed;

    public CuspInterp2D(boolean closed) {
        this.closed = closed;
    }

    public <T extends Point2D> CuspInterp2D(List<T> points,
                        List<Boolean> smoothed,
                        boolean closed) {
        super(points);
        this.closed = closed;
        if (smoothed.size() != points.size()) {
            throw new IllegalArgumentException("smoothed.size() " + smoothed.size()
                                               + " != points.length " + points.size());
        }
        this.smoothed = new ArrayList<>(smoothed);
    }

    public CuspInterp2D
        (@JsonProperty("points") ArrayList<Point2D.Double> points,
         @JsonProperty("smoothed") ArrayList<Boolean> smoothed,
         @JsonProperty("closed") boolean closed) {
        this(points, (List<Boolean>) smoothed, closed);
    }

    public <T extends Point2D> CuspInterp2D(List<T> points,
                        boolean smoothed,
                        boolean closed) {
        super(points);
        this.closed = closed;
        this.smoothed = new ArrayList<>(points.size());
        for (int i = points.size(); i > 0; --i) {
            this.smoothed.add(smoothed);
        }
    }

    /** Shorthand to create a line segment. */
    public CuspInterp2D(Point2D a, Point2D b) {
        add(a);
        add(b);
    }

    /** Shorthand to create a line segment. */
    public CuspInterp2D(Line2D segment) {
        this(segment.getP1(), segment.getP2());
    }

    /** Return true if the curve is to be smoothed at t = #vertexNo.
        If the curve is not closed or if the curve contains two or
        fewer points, then the smoothing settings for the first and
        last points have no immediate effect. */
    @Override public final boolean isSmoothed(int vertexNo) {
        return smoothed.get
            ((vertexNo == smoothed.size() && isClosed()) ? 0 : vertexNo);
    }

    @JsonProperty("smoothed") ArrayList<Boolean> getSmoothed() {
        return smoothed;
    }

    @Override public final void setSmoothed(int vertexNo, boolean value) {
        if (value != isSmoothed(vertexNo)) {
            param = null;
            smoothed.set(vertexNo, value);
        }
    }

    int smoothedPointCnt() {
        int tot = 0;
        for (int i = 0; i < size(); ++i) {
            if (isSmoothed(i)) {
                ++tot;
            }
        }
        return tot;
    }

    /**
     * Return the least unsmoothed index &gt;= n, or size() if
     * vertices [n ... size()-1] are all smoothed. */
    int nextUnsmoothed(int n) {
        int s = smoothed.size();
        for (int i = n; i < s; ++i) {
            if (!smoothed.get(i)) {
                return i;
            }
        }
        return s;
    }

    /**
     * Return the greatest unsmoothed index &lt;= n, or -1 if vertices [0
     * ... n] are all smoothed. */
    int previousUnsmoothed(int n) {
        for (int i = n; i >= 0; --i) {
            if (!smoothed.get(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the indices of all vertices that are cusps, that is,
     * where the curve is not smooth in an interval around that
     * vertex. Endpoints and unsmoothed vertices are generally
     * cusps. */
    @JsonIgnore ArrayList<Integer> getCusps() {
            ArrayList<Integer> res = new ArrayList<>();
            int size = this.size();
            for (int j = 0; j < size; ++j) {
                if (isEndpoint(j) || !smoothed.get(j)) {
                    res.add(j);
                }
            }
            return res;
    }

    @Override @JsonIgnore public Path2D.Double getShape() {
        Point2D.Double temp;
        int s = size();
        Path2D.Double res = new Path2D.Double();
        if (s <= 2) {
            if (s >= 1) {
                temp = points.get(0);
                res.moveTo(temp.x, temp.y);
                if (s == 2) {
                    temp = points.get(1);
                }
                res.lineTo(temp.x, temp.y);
                // It seems that without the lineTo(), single-point
                // paths are invisible.
            }
            return res;
        }

        // In some ways it would be simpler to start drawing from the
        // first unsmoothed point, but that would spoil the
        // correspondence between the location of control point #i and
        // getParameterization().value(i).
        int nextUnsmoothed = nextUnsmoothed(0);
        if (nextUnsmoothed == s) { // Smooth entire curve.
            return new CubicSpline2D(points.toArray(new Point2D.Double[0]),
                    isClosed()).path();
        }

        temp = points.get(0);
        res.moveTo(temp.x, temp.y);
        int ss = 0; // Spline start no.
        int lastUnsmoothed;
        CubicSpline2D straddleSpline = null;

        // The last point and the first point may coincide, which
        // would require special handling for closed curves.
        boolean duplicateEnds = points.get(0).equals(points.get(s-1));
        int effectiveEnd = duplicateEnds ? s - 1 : s;
        int straddleOffset = -1;
        
        boolean oldLast = false;
        if (isClosed()) {
            if (nextUnsmoothed > 0) {
                // Smooth through point #0, which requires a spline that
                // straddles the loop back to #0.
                
                lastUnsmoothed = previousUnsmoothed(s-1);
                ArrayList<Point2D.Double> straddlePoints = new ArrayList<>();
                for (int i = lastUnsmoothed; i < effectiveEnd; ++i) {
                    straddlePoints.add(points.get(i));
                }
                straddleOffset = straddlePoints.size();
                for (int i = 0; i <= nextUnsmoothed; ++i) {
                    straddlePoints.add(points.get(i));
                }
                straddleSpline = new CubicSpline2D(
                        straddlePoints.toArray(new Point2D.Double[0]));
                straddleSpline.appendSplinesTo(res, straddleOffset,
                        straddlePoints.size() - 1);
                ss = nextUnsmoothed;
            } else {
                // Temporarily add the starting point as the endpoint
                // of the last spline.
                points.add(points.get(0));
                lastUnsmoothed = s;
            }
        } else {
            // Temporarily set smoothing for the last vertex to false.
            lastUnsmoothed = s-1;
            oldLast = smoothed.get(lastUnsmoothed);
            setSmoothed(lastUnsmoothed, false);
        }
        while (ss < lastUnsmoothed) {
            int se = nextUnsmoothed(ss+1);
            if (se == ss+1) {
                Point2D.Double p = points.get(se);
                res.lineTo(p.x, p.y);
            } else {
                CubicSpline2D spline = new CubicSpline2D(
                        points.subList(ss, se+1).toArray(new Point2D.Double[0]));
                spline.appendSplinesTo(res, 0, se - ss);
            }
            ss = se;
        }
        if (straddleSpline != null) {
            straddleSpline.appendSplinesTo(res, 0, straddleOffset);
            if (duplicateEnds) {
                Point2D.Double p = points.get(0);
                res.lineTo(p.x, p.y);
            }
        }
        if (points.size() > s) {
            // Remove the temporary duplicate of the starting point.
            points.remove(s);
        }
        if (isClosed()) {
            res.closePath();
        } else {
            // Reset the last vertex to its
            // original smoothing value.
            setSmoothed(lastUnsmoothed, oldLast);
        }
        return res;
    }

    @Override public void remove(int vertexNo) {
        super.remove(vertexNo);
        smoothed.remove(vertexNo);
    }

    /* Like add(vertexNo, point, false). */
    @Override public void add(int vertexNo, Point2D point) {
        add(vertexNo, point, false);
    }

    /* Add the given point as vertex #vertexNo. If smoothed is true,
       then use a smooth curve to connect the new vertex to the
       vertexes that preced and follow it. Otherwise, make no attempt
       to smooth the curve around the new point. */
    public void add(int vertexNo, Point2D point, boolean smoothed) {
        super.add(vertexNo, point);
        this.smoothed.add(vertexNo, smoothed);
    }

    @Override public CuspInterp2D createTransformed(AffineTransform xform) {
        return new CuspInterp2D(Arrays.asList(transformPoints(xform)),
                smoothed, isClosed());
    }

    @Override public String toString() {
        StringBuilder res = new StringBuilder(getClass().getSimpleName() + "[");
        int i = -1;
        for (Point2D point: points) {
            ++i;
            res.append(Geom.toString(point));
            res.append(isSmoothed(i) ? "-" : ",");
        }
        if (isClosed()) {
            res.append("close");
        }
        res.append("]");
        return res.toString();
    }

    @Override public CuspInterp2D clone() {
        return new CuspInterp2D(points, smoothed, isClosed());
    }

    @Override public boolean isClosed() {
        return closed;
    }

    @Override public void setClosed(boolean b) {
        super.setClosed(b);
        closed = b;
    }

}
