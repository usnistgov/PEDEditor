package gov.nist.pededitor;

import java.awt.geom.*;

/** Simple object to store information about a single point on a
    parameterized curve and its distance from something. */
public class CurveDistance implements Comparable<CurveDistance> {
    public CurveDistance(double t, Point2D point, double distance) {
        this.t = t;
        this.point = new Point2D.Double(point.getX(), point.getY());
        this.distance = distance;
    }

    @Override
	public String toString() {
        return "CurveDistance[t = " + t + ", p = " + point + ", d = "
            + distance + "]";
    }

    /** t in [0,1] is the curve parameterization variable value. */
    public double t;
    /** Point on the curve closest to the target object. */
    public Point2D.Double point;
    /** Distance at closest approach. */
    public double distance;

    @Override
	public int compareTo(CurveDistance other) {
        return (distance < other.distance) ? -1
            : (distance > other.distance) ? 1 : 0;
    }

    /** @return other if other is not null and its distance is less
        than this, or this otherwise. */
    public CurveDistance minWith(CurveDistance other) {
        return (other == null) ? this
            : (distance <= other.distance) ? this : other;
    }

    /** @return other if other is not null and its distance is less
        than this, or this otherwise. */
    public static CurveDistance min(CurveDistance c1, CurveDistance c2) {
        return (c2 == null) ? c1
            : (c1 == null) ? c2
            : (c1.distance <= c2.distance) ? c1 : c2;
    }

    /** @return other if other is not null and its distance is greater
        than this, or this otherwise. */
    public CurveDistance maxWith(CurveDistance other) {
        return (other == null) ? this
            : (distance >= other.distance) ? this : other;
    }

    public static CurveDistance pointSegmentDistance 
        (Point2D p, Point2D l1, Point2D l2) {
        
        double dx = l2.getX() - l1.getX();
        double dy = l2.getY() - l1.getY();
        double dx2 = p.getX() - l1.getX();
        double dy2 = p.getY() - l1.getY();
        double dot = dx * dx2 + dy * dy2;
        double l1l2LengthSq = dx * dx + dy * dy;

        CurveDistance output;

        if (dot < 0 || l1l2LengthSq == 0) {
            output = new CurveDistance(0.0, l1, 0.0);
        } else {
            dot /= l1l2LengthSq;

            output = (dot > 1.0) ? new CurveDistance(1.0, l2, 0.0)
                : new CurveDistance(dot,
                                    new Point2D.Double(l1.getX() + dx * dot,
                                                       l1.getY() + dy * dot), 0.0);
        }

        output.distance = p.distance(output.point);
        return output;
    }

    public static CurveDistance distance(Point2D p1, Point2D p2) {
        return new CurveDistance(0.0, p2, p1.distance(p2));
    }
}
