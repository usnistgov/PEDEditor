/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import org.codehaus.jackson.annotate.JsonProperty;

/** Interpolation where control points may be individually marked
    as smoothed or un-smoothed. */
public class CuspInterp2D extends PointsInterp2D {
    protected ArrayList<Boolean> smoothed = new ArrayList<>();

    public CuspInterp2D(boolean closed) {
        super(null, closed);
    }

    public CuspInterp2D(Point2D.Double[] points,
                        ArrayList<Boolean> smoothed, 
                        boolean closed) {
        super(points, closed);
        if (smoothed.size() != points.length) {
            throw new IllegalArgumentException("smoothed.size() " + smoothed.size()
                                               + " != points.length " + points.length);
        }
        this.smoothed = new ArrayList<>(smoothed);
    }

    public CuspInterp2D
        (@JsonProperty("points") Point2D.Double[] points,
         @JsonProperty("smoothed") boolean[] smoothed,
         @JsonProperty("closed") boolean closed) {
        super(points, closed);
        this.smoothed = new ArrayList<>(smoothed.length);
        for (int i = 0; i < smoothed.length; ++i) {
            this.smoothed.add(smoothed[i]);
        }
    }

    public CuspInterp2D
        ( Point2D[] points,
          boolean[] smoothed,
          boolean closed) {
        super(points, closed);
        this.smoothed = new ArrayList<>(smoothed.length);
        for (int i = 0; i < smoothed.length; ++i) {
            this.smoothed.add(smoothed[i]);
        }
    }

    public CuspInterp2D(Point2D.Double[] points,
                        boolean smoothed,
                        boolean closed) {
        super(points, closed);
        this.smoothed = new ArrayList<>(points.length);
        for (int i = points.length; i > 0; --i) {
            this.smoothed.add(smoothed);
        }
    }

    /** Shorthand to create a line segment. */
    public CuspInterp2D(Point2D a, Point2D b) {
        this(new Point2D.Double[] { new Point2D.Double(a.getX(), a.getY()), new Point2D.Double(b.getX(), b.getY()) }, false, false);
    }

    /** Shorthand to create a line segment. */
    public CuspInterp2D(Line2D segment) {
        this(segment.getP1(), segment.getP2());
    }

    /** Return true if the curve is to be smoothed at t = #vertexNo.
        If the curve is not closed or if the curve contains two or
        fewer points, then the smoothing settings for the first and
        last points have no immediate effect. */
    public final boolean isSmoothed(int vertexNo) {
        return smoothed.get
            ((vertexNo == smoothed.size() && isClosed()) ? 0 : vertexNo);
    }

    /** Return true if this point looks like an endpoint of the curve. */
    public final boolean isEndpoint(int vertexNo) {
        int s = size();
        return s <= 2 || (!isClosed() && (vertexNo == 0 || vertexNo == s-1));
    }

    @JsonProperty("smoothed") ArrayList<Boolean> getSmoothed() {
        return smoothed;
    }

    public final void setSmoothed(int vertexNo, boolean value) {
        if (value != isSmoothed(vertexNo)) {
            param = null;
            smoothed.set(vertexNo, value);
        }
    }

    public final void toggleSmoothed(int vertexNo) {
        smoothed.set(vertexNo, !smoothed.get(vertexNo));
        param = null;
    }

    int nextClearBit(int n) {
        int s = smoothed.size();
        for (int i = n; i < s; ++i) {
            if (!smoothed.get(i)) {
                return i;
            }
        }
        return s;
    }

    int previousClearBit(int n) {
        for (int i = n; i >= 0; --i) {
            if (!smoothed.get(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override public Path2D.Double getPath() {
        Point2D.Double temp;
        int s = size();
        Path2D.Double res = new Path2D.Double();
        if (s <= 2) {
            if (s >= 1) {
            	temp = points.get(0);
                res.moveTo(temp.x, temp.y);
                if (s == 2) {
                	temp = points.get(1);
                    res.lineTo(temp.x, temp.y);
                }
            }
            return res;
        }

        int nextClear = nextClearBit(0);
        if (nextClear >= s) {
            return new CubicSpline2D
                (points.toArray(new Point2D.Double[0]), isClosed()).path();
        }

        temp = points.get(0);
        res.moveTo(temp.x, temp.y);
        int ss = 0; // Spline start no.
        int lastClear;
        CubicSpline2D straddleSpline = null;
        boolean oldLast = false;
        if (isClosed()) {
            if (nextClear > 0) {
                // Smooth through point #0, which requires a spline that
                // straddles the loop back to #0.
                lastClear = previousClearBit(s-1);
                if (lastClear == -1) {
                    lastClear = s;
                }
                ArrayList<Point2D.Double> straddlePoints = new ArrayList<>();
                for (int i = lastClear; i < s; ++i) {
                    straddlePoints.add(points.get(i));
                }
                int offset = s - lastClear;
                for (int i = 0; i <= nextClear; ++i) {
                    straddlePoints.add(points.get(i % s));
                }
                straddleSpline = new CubicSpline2D
                    (straddlePoints.toArray(new Point2D.Double[0]));
                straddleSpline.appendSplinesTo(res, offset, offset + nextClear);
                ss = nextClear;
            } else {
                // Temporarily add the starting point as the endpoint
                // of the last spline.
                points.add(points.get(0));
                lastClear = s;
            }
        } else {
            // Temporarily set smoothing for the last vertex to false.
            lastClear = s-1;
            oldLast = smoothed.get(lastClear);
            setSmoothed(lastClear, false);
        }
        while (ss < lastClear) {
            int se = nextClearBit(ss+1);
            if (se == ss+1) {
                Point2D.Double p = points.get(se);
                res.lineTo(p.x, p.y);
            } else {
                CubicSpline2D spline = new CubicSpline2D
                    (points.subList(ss, se+1).toArray(new Point2D.Double[0]));
                spline.appendSplinesTo(res, 0, se - ss);
            }
            ss = se;
        }
        if (straddleSpline != null) {
            straddleSpline.appendSplinesTo(res, 0, s - ss);
        }
        if (points.size() > s) {
            // Remove the temporary duplicate of the starting point.
            points.remove(s);
        }
        if (!isClosed()) {
            // Reset the last vertex to its
            // original smoothing value.
            setSmoothed(lastClear, oldLast);
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
        return new CuspInterp2D(transformPoints(xform), smoothed, isClosed());
    }

    @Override public String toString() {
        StringBuilder res = new StringBuilder(getClass().getSimpleName() + "[");
        int i = -1;
        for (Point2D point: points) {
            ++i;
            res.append(Duh.toString(point));
            res.append(isSmoothed(i) ? "-" : ",");
        }
        if (isClosed()) {
            res.append("close");
        }
        res.append("]");
        return res.toString();
    }
}
