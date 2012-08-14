package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/** Offset the t values in a CurveBoundedParam2D by a fixed value.
    If offset = 0.5, for example, then getMinT() will return 0.5 more
    than the original curve's getMinT() does, and getLocation(0.5)
    returns the same value as the original curve's getLocation(0)
    would, and so on.
 */

public class OffsetParam2D implements BoundedParam2D {
    BoundedParam2D c;
    double offset;

    /** Warning: c will be incorporated into this object, so later
        modifications to this object may modify c and vice versa. If
        you don't like that, then clone c first. */
    public OffsetParam2D(BoundedParam2D c, double offset) {
        this.c = c;
        this.offset = offset;
    }

    public BoundedParam2D getContents() { return c; }
    @Override public double getMinT() { return c.getMinT() + offset; }
    @Override public double getMaxT() { return c.getMaxT() + offset; }
    @Override public double getNextVertex(double t) {
        return c.getNextVertex(t - offset) + offset;
    }
    @Override public double getLastVertex(double t) {
        return c.getLastVertex(t - offset) + offset;
    }
    @Override public Point2D.Double getLocation(double t) {
        return c.getLocation(t - offset);
    }
    @Override public Point2D.Double getDerivative(double t) {
        return c.getDerivative(t - offset);
    }
    @Override public Point2D.Double getStart() { return c.getStart(); }
    @Override public Point2D.Double getEnd() { return c.getEnd(); }

    @Override public CurveDistanceRange distance(Point2D p) {
        return addOffset(c.distance(p));
    }

    @Override public CurveDistance distance(Point2D p, double t) {
        return addOffset(c.distance(p, t - offset));
    }

    @Override public CurveDistanceRange distance(Point2D p, double maxError,
                                            int maxSteps) {
        return addOffset(c.distance(p, maxError, maxSteps));
    }

    @Override public OffsetParam2D derivative() {
        return new OffsetParam2D(c.derivative(), offset);
    }

    @Override public Rectangle2D.Double getBounds() {
        return c.getBounds();
    }

    @Override public double[] getBounds(double xc, double yc) {
        return c.getBounds(xc, yc);
    }

    @Override public double[] segIntersections(Line2D segment) {
        double[] res = c.segIntersections(segment);
        for (int i = 0; i < res.length; ++i) {
            res[i] += offset;
        }
        return res;
    }

    @Override public double[] lineIntersections(Line2D segment) {
        double[] res = c.lineIntersections(segment);
        for (int i = 0; i < res.length; ++i) {
            res[i] += offset;
        }
        return res;
    }

    @Override public BoundedParam2D[] subdivide() {
        BoundedParam2D[] parts = c.subdivide();
        BoundedParam2D[] res = new BoundedParam2D[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            res[i] = new OffsetParam2D(parts[i], offset);
        }
        return res;
    }

    @Override public OffsetParam2D createSubset
        (double minT, double maxT) {
        return new OffsetParam2D
            (c.createSubset(minT - offset, maxT - offset), offset);
    }

    CurveDistance addOffset(CurveDistance cd) {
        if (cd == null) {
            return null;
        }
        cd.t += offset;
        return cd;
    }

    CurveDistanceRange addOffset(CurveDistanceRange cd) {
        if (cd == null) {
            return null;
        }
        cd.t += offset;
        return cd;
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName() + "[" + c);
        if (offset != 0) {
            s.append(" t+" + offset);
        }
        s.append("]");
        return s.toString();
    }

    public static class DistanceIndex {
        CurveDistance distance;
        int index;
        
        public DistanceIndex(CurveDistance d, int i) {
            distance = d;
            index = i;
        }
    }

    /** Return the distance from "p" to the nearest curve in "params".
     Attaches the index of the nearest curve (so if res.index == 0
     then the zeroth element of params is nearest, to within the given
     error limits).

     When you just want to find the distance to the nearest of many
     curves, this method may be much more efficient than calling
     distance() on each curve separately and taking the minimum of
     those results, because extra effort to improve precision is only
     made for curves that are still candidates to be the nearest one.

     @see BoundedParam2Ds.distance(BoundedParam2D, p, maxError,
     maxSteps).
    */
    public static DistanceIndex distance
        (ArrayList<BoundedParam2D> params, Point2D p,
         double maxError, int maxSteps) {
        ArrayList<OffsetParam2D> oparams = separate(params);
        CurveDistance dist = BoundedParam2Ds.distance
            (oparams, p, maxError, maxSteps);
        if (dist == null) {
            return null;
        }
        int i = index(oparams, dist);
        return new DistanceIndex(dist, i);
    }

    /** Convert the inputs so that their domains are separate. This is
        used by BoundedParam2Ds.distance() to allow one to
        distinguish which input a CurveDistance comes from. */
    static <T extends BoundedParam2D> ArrayList<OffsetParam2D> separate(Iterable<T> ps) {
        ArrayList<OffsetParam2D> res = new ArrayList<>();
        double offset = 0;
        for (BoundedParam2D p: ps) {
            OffsetParam2D op = new OffsetParam2D(p, offset - p.getMinT());
            offset = op.getMaxT() + 1;
            res.add(op);
        }
        return res;
    }

    /** Undo the offsetting done by the separate() method. Return the
        index into the array of the curve that the given point lies on.

        @param d (In-out parameter) This object's t value will be
        changed back to be correct for the curve's original domain.
    */
    static int index(ArrayList<OffsetParam2D> ps, CurveDistance d) {
        double t = d.t;
        int i = 0;
        for (OffsetParam2D p: ps) {
            if (t <= p.getMaxT()) {
                if (t < p.getMinT()) {
                    throw new IllegalStateException(d.t + " is not in the domain");
                }
                d.t -= p.offset;
                return i;
            }
            ++i;
        }
        throw new IllegalStateException(d.t + " is above the domain");
    }

    @Override public OffsetParam2D createTransformed(AffineTransform xform) {
        return new OffsetParam2D(c.createTransformed(xform), offset);
    }
}
