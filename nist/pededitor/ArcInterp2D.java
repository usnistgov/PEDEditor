/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ArcInterp2D extends PointsInterp2D {
    protected boolean closed = true;
    transient protected boolean endsSwapped = false;

    public boolean hasSwappedEnds() {
        getParameterization();
        return endsSwapped;
    }

    @Override public void setClosed(boolean b) {
        closed = b;
        if (size() <= 2)
            return;
        super.setClosed(b);
    }

    public ArcInterp2D() { }

    /** @param closed If false, this is an arc or a circle or ellipse,
        not the whole thing. The first point is at one end, the last
        point is at the other; and if there are three or more points,
        the second point is on the arc as well. */
    public <T extends Point2D> ArcInterp2D(List<T> points,
            boolean closed) {
        super(points);
        setClosed(closed);
    }

    /** @param closed If false, this is an arc or a circle or ellipse,
        not the whole thing. The first point is at one end, the last
        point is at the other; and if there are three or more points,
        the second point is on the arc as well. */
    public <T extends Point2D> ArcInterp2D(List<T> points) {
        this(points, true);
    }

    @Override public RectangularShape getShape() {
        try {
            RectangularShape res = getShape2();
            if (res instanceof Arc2D.Double) {
                Arc2D.Double arc = (Arc2D.Double) res;

                // I have to mirror the arc
                // to make Graphics2D draw what I want.
                arc.start = -(arc.start + arc.extent);
                return arc;
            }
            return res;
        } catch (UnsolvableException e) {
            return null;
        }
    }

    @JsonIgnore public RectangularShape getShape2() throws UnsolvableException
    {
        return isClosed() ? ArcMath.ellipse(points) : arc(points);
    }

    @Override public boolean isClosed() {
        return size() <= 2 || closed;
    }

    @Override public int minSize() {
        return 1;
    }

    @Override public int maxSize() {
        return 4;
    }

    static <T extends Point2D> Arc2D.Double arc(List<T> points)
        throws UnsolvableException {
        return arcAndSwapping(points).arc;
    }

    public static class ArcAndBool {
        Arc2D.Double arc;
        boolean hasSwappedEndpoints;
    }

    static <T extends Point2D> ArcAndBool arcAndSwapping(List<T> points)
        throws UnsolvableException {
        Ellipse2D.Double el = ArcMath.ellipse(points);
        Arc2D.Double a = new Arc2D.Double(el.getBounds2D(), 0, 0,
                                            Arc2D.OPEN);
        ArcAndBool res = new ArcAndBool();
        res.arc = a;
        Point2D p0 = points.get(0);
        double a0 = ArcMath.toAngle(a, p0);
        Point2D p1 = points.get(1);
        double a1 = ArcMath.toAngle(a, p1);
        Point2D p2 = points.get(points.size() - 1);
        double a2 = ArcMath.toAngle(a, p2);
        res.hasSwappedEndpoints = !Geom.degreesInRange(a1, a0, a2);
        if (res.hasSwappedEndpoints) {
            a.start = a2;
            a.extent = a0 - a2;
        } else {
            a.start = a0;
            a.extent = a2 - a0;
        }
        a.extent -= Math.floor(a.extent / 360) * 360;
        return res;
    }

    @Override public ArcInterp2D clone() {
        return new ArcInterp2D(points, isClosed());
    }

    protected ArcPointInfo fix(ArcPointInfo info) {
        if (hasSwappedEnds()) {
            info.beforeIndex = !info.beforeIndex;
            int tmp = info.lastIndex;
            info.lastIndex = info.nextIndex;
            info.nextIndex = tmp;
        }
        return info;
    }

    @Override @JsonIgnore public ArcParam2D getParameterization() {
        if (param == null) {
            try {
                param = new ArcParam2D(this);
                Point2D start = param.getStart();
                endsSwapped = !isClosed()
                    && (start.distanceSq(points.get(0))
                            > start.distanceSq(points.get(size()-1)));
            } catch (UnsolvableException e) {
                return null;
            }
        }
        return (ArcParam2D) param;
    }

    @Override public double indexToT(int index) {
        return toT(points.get(index));
    }

    public double toT(Point2D p) {
        ArcParam2D param = getParameterization();
        double minT = param.getMinT();
        double deg = ArcMath.toAngle(param.arc, p);
        return deg - Math.floor((deg - minT) / 360) * 360;
    }

    @Override public int tToIndex(double t) {
        if (size() <= 1) {
            return 0;
        }
        int nearestIndex = -1;
        double leastDistance = 1e6;
        for (int i = 0; i < size(); ++i) {
            double distance = Math.abs(ArcMath.coerce180(t - indexToT(i)));
            if (distance < leastDistance) {
                leastDistance = distance;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    static class ArcPointInfo extends ParamPointInfo {
        int nextIndex = -1;
        int lastIndex = -1;
    }

    @Override public ArcPointInfo info(double t) {
        ArcParam2D param = getParameterization();
        if (param == null)
            return null;

        // Figure out the closest control points on both sides and
        // which is closest overall.

        double minT = param.getMinT();
        double maxT = param.getMaxT();

        // Index of the control point with the least t value greater
        // than t. "Greater than t" means it's not t and either the
        // path is closed or its projection on the arc is between t and maxT.
        // "Least" means least rotation in the direction of increasing
        // angle starting from t.
        int nextIndex = -1;
        double minDirectedDist = 360;

        // Index of the control point with the greatest t less than or
        // equal to t. "Less than or equal" means either the path is
        // closed or its projection on the arc is between t and minT.
        int lastIndex = -1;
        double maxDirectedDist = 0;

        for (int i = 0; i < size(); ++i) {
            double ct = indexToT(i);
            if (!Geom.degreesInRange(ct, minT, maxT))
                continue; // Ignore points outside the arc.

            // Coerce the signed angular distance to [0, 360).
            double directedDist = ct - Math.floor((ct - t) / 360) * 360;

            if (ct != t
                    && (isClosed() || Geom.degreesInRange(ct, t, maxT))
                    && directedDist < minDirectedDist) {
                nextIndex = i;
                minDirectedDist = directedDist;
            }

            if ((isClosed() || Geom.degreesInRange(ct, minT, t))
                    && directedDist >= maxDirectedDist) {
                lastIndex = i;
                maxDirectedDist = directedDist;
            }
        }

        ArcPointInfo res = new ArcPointInfo();
        res.t = t;
        res.lastIndex = lastIndex;
        res.nextIndex = nextIndex;

        if (nextIndex == -1) {
            if (lastIndex == -1)
                return null;
            res.index = lastIndex;
        } else {
            res.beforeIndex = true;
            if (lastIndex >= 0) {
                Point2D p = getLocation(t);
                Point2D pn = get(nextIndex);
                Point2D pl = get(lastIndex);

                res.beforeIndex = p.distanceSq(pn) < p.distanceSq(pl);
            }
            res.index = res.beforeIndex ? nextIndex : lastIndex;
        }
        return fix(res);
    }

    @Override public double getNearestVertex(double t) {
        ParamPointInfo info = info(t);
        return (info == null) ? Double.NaN : indexToT(info.index);
    }

    /** Return the index of the control point that is on or inside the
        arc and for which the arc from the control point proceeding
        counterclockwise to t is shortest. */
    @Override public double getLastVertex(double t) {
        return info(t).lastIndex;
    }

    /** Return the index of the control point that is on or inside the
        arc and for which the arc from the control point proceeding
        counterclockwise to t is shortest. */
    @Override public double getNextVertex(double t) {
        return info(t).nextIndex;
    }

    @Override public ArcInterp2D createTransformed(AffineTransform xform) {
        return new ArcInterp2D(Arrays.asList(transformPoints(xform)),
                isClosed());
    }

    public static void main(String[] args) throws UnsolvableException {
        ArcMath.ellipse(Arrays.asList(new Point2D.Double[]
                { new Point2D.Double(7.0, 3.0),
                  new Point2D.Double(11.0, 3.0),
                  new Point2D.Double(9.0, 5.0) }));
    }
 }
