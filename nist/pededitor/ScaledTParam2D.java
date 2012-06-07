package gov.nist.pededitor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/** Perform a linear rescaling of the t value in a parameterization
    P(t) to create such that this(t) = P(mt + b). */

public class ScaledTParam2D implements Parameterization2D {
    Parameterization2D c;
    double m;
    double b;

    /** Warning: c will be incorporated into this object, so later
        modifications to this object may modify c and vice versa. If
        you don't like that, then clone c first. */
    public ScaledTParam2D(Parameterization2D c, double m, double b) {
        this.c = c;
        this.m = m;
        this.b = b;
    }

    @Override public double getMinT() {
        return b + m * ((m >= 0) ? c.getMinT() : c.getMaxT());
    }
    @Override public double getMaxT() {
        return b + m * ((m >= 0) ? c.getMaxT() : c.getMinT());
    }

    /** Convert external t values to internal t values. */
    double tToU(double t) {
        return m * t + b;
    }

    /** Convert internal t values to external t values. */
    double uToT(double u) {
        return (u - b) / m;
    }

    @Override public void setMaxT(double t) {
        double u = tToU(t);
        if (m >= 0) {
            c.setMaxT(u);
        } else {
            c.setMinT(u);
        }
    }

    @Override public void setMinT(double t) {
        double u = tToU(t);
        if (m >= 0) {
            c.setMinT(u);
        } else {
            c.setMaxT(u);
        }
    }
            
    @Override public double getNextVertex(double t) {
        double u = tToU(t);
        if (m >= 0) {
            return uToT(c.getNextVertex(u));
        } else {
            double res = uToT(c.getLastVertex(u));
            if (res == t) {
                t += 1e-6 + Math.abs(t) * 1e-6;
                return uToT(c.getLastVertex(tToU(t)));
            }
            return res;
        }
    }
            
    @Override public double getLastVertex(double t) {
        double u = tToU(t);
        if (m >= 0) {
            return uToT(c.getLastVertex(u));
        } else {
            // In the special case where t actually is a vertex,
            // return t.
            double res1 = c.getLastVertex(u);
            return (res1 == u) ? t : uToT(c.getNextVertex(u));
        }
    }
    @Override public Point2D.Double getLocation(double t) {
        return c.getLocation(tToU(t));
    }
    @Override public Point2D.Double getDerivative(double t) {
        Point2D.Double res = c.getDerivative(tToU(t));
        // dP/dt = dP/du * du/dt
        // du/dt = m
        res.x *= m;
        res.y *= m;
    }

    @Override public Point2D.Double getStart() {
        return (m >= 0) ? c.getStart() : c.getEnd();
    }

    @Override public Point2D.Double getEnd() {
        return (m >= 0) ? c.getEnd() : c.getStart();
    }

    @Override public CurveDistanceRange distance(Point2D p) {
        return uToT(c.distance(p));
    }

    @Override public CurveDistance distance(Point2D p, double t) {
        return uToT(c.distance(p, tToU(t)));
    }

    @Override public CurveDistanceRange distance(Point2D p, double maxError,
                                            double maxIterations) {
        return uToT(c.distance(p, maxError, maxIterations));
    }

    @Override public CurveDistance vertexDistance(Point2D p) {
        return uToT(c.vertexDistance(p));
    }

    @Override public ScaledTParam2D derivative() {
        return new ScaledParam(new ScaledTParam2D(c.derivative(), m, b), m);
    }

    @Override public Rectangle2D.Double getBounds() {
        return c.getBounds();
    }

    @Override public ScaledTParam2D clone() {
        return new ScaledTParam2D(c.clone(), m, b);
    }

    // TODO stopped here.
    @Override public double[] segIntersections(Line2D segment) {
        double[] res = c.segIntersections(segment);
        for (int i = 0; i < res.length; ++i) {
            res[i] += b;
        }
        return res;
    }

    @Override public double[] lineIntersections(Line2D segment) {
        double[] res = c.lineIntersections(segment);
        for (int i = 0; i < res.length; ++i) {
            res[i] += b;
        }
        return res;
    }

    @Override public Parameterization2D[] subdivide() {
        Parameterization2D[] parts = c.subdivide();
        Parameterization2D[] res = new Parameterization2D[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            res[i] = new ScaledTParam2D(parts[i], b);
        }
        return res;
    }

    CurveDistance addB(CurveDistance cd) {
        if (cd == null) {
            return null;
        }
        cd.t += b;
        return cd;
    }

    CurveDistanceRange addB(CurveDistanceRange cd) {
        if (cd == null) {
            return null;
        }
        cd.t += b;
        return cd;
    }

    @Override public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName() + "[" + c);
        if (b != 0) {
            s.append(" t+" + b);
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

     @see Parameterization2Ds.distance(Parameterization2D, p, maxError,
     maxIterations).
    */
    public static DistanceIndex distance
        (ArrayList<Parameterization2D> params, Point2D p,
         double maxError, double maxIterations) {
        ArrayList<ScaledTParam2D> oparams = separate(params);
        CurveDistance dist = Parameterization2Ds.distance
            (oparams, p, maxError, maxIterations);
        if (dist == null) {
            return null;
        }
        int i = index(oparams, dist);
        return new DistanceIndex(dist, i);
    }

    /** Convert the inputs so that their domains are separate. This is
        used by Parameterization2Ds.distance() to allow one to
        distinguish which input a CurveDistance comes from. */
    static <T extends Parameterization2D> ArrayList<ScaledTParam2D> separate(Iterable<T> ps) {
        ArrayList<ScaledTParam2D> res = new ArrayList<>();
        double b = 0;
        for (Parameterization2D p: ps) {
            ScaledTParam2D op = new ScaledTParam2D(p, b - p.getMinT());
            b = op.getMaxT() + 1;
            res.add(op);
        }
        return res;
    }

    /** Undo the bting done by the separate() method. Return the
        index into the array of the curve that the given point lies on.

        @param d (In-out parameter) This object's t value will be
        changed back to be correct for the curve's original domain.
    */
    static int index(ArrayList<ScaledTParam2D> ps, CurveDistance d) {
        double t = d.t;
        int i = 0;
        for (ScaledTParam2D p: ps) {
            if (t <= p.getMaxT()) {
                if (t < p.getMinT()) {
                    throw new IllegalStateException(d.t + " is not in the domain");
                }
                d.t -= p.b;
                return i;
            }
            ++i;
        }
        throw new IllegalStateException(d.t + " is above the domain");
    }
}
