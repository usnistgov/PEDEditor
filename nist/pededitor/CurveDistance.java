package gov.nist.pededitor;

import java.awt.geom.*;
import java.awt.*;
import java.util.*;

/** Simple object to store information about a single point on a
    parameterized curve and its distance from something. */
public class CurveDistance {
    public CurveDistance(double t, Point2D point, double distance) {
        this.t = t;
        this.point = new Point2D.Double(point.getX(), point.getY());
        this.distance = distance;
    }

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
