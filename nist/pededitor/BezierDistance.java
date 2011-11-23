package gov.nist.pededitor;

import java.awt.geom.*;
import java.awt.*;
import java.util.*;

public class BezierDistance {
    /** Information about where the curve comes closest to a given point. */
    public class DistanceInfo {
        /** t in [0,1] is the value of the Bezier curve
            parameterization variable. */
        public double t;
        /** Point of closest approach. */
        public Point2D.Double point;
        /** Distance at closest approach. */
        public double distance;
    }

    /** @return the distance between the point and the convex hull of
        the Bezier control points, which represents a lower bound on
        the distance between the point and the Bezier curve itself. */
    public static double distanceLowerBound(Point point,
                                            Point2D.Double[] controlPoints) {
        Point2D.Double[] hullPoints = Duh.convexHull(controlPoints);

        if (hullPoints.length == 1) {
            return hullPoints[0].distance(point);
        }

        // Figure out whether point is inside the convex hull.

        Path2D.Double path = new Path2D.Double();
        path.moveTo(hullPoints[0]);
        for (int i = 1; i < hullPoints.length; ++i) {
            path.lineTo(hullPoints[1]);
        }
        path.closePath();

        if (path.contains(point)) {
            return 0;
        }

        double minDistance = -1;
        for (int i = 1; i < hullPoints.length; ++i) {
            double distance = Duh.segmentDistance
                (point, hullPoints[i-1], hullPoints[i]);
            if (i == 1 || distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    /** @return an upper bound on the distance between the point and
        the portion of the Bezier curve defined by the given control
        points that lies in [t0, t1], using the super-sophisticated
        method of testing the midpoint of that region. */
    public static double distanceUpperBound(Point point,
                                            Point2D.Double[] controlPoints,
                                            double t0, double t1) {
        Point2D.Double[] hullPoints = Duh.convexHull(controlPoints);

        if (hullPoints.length == 1) {
            return hullPoints[0].distance(point);
        }

        // Use the Java2D library to figure out whether point is
        // inside the convex hull.

        Path2D.Double path = new Path2D.Double();
        path.moveTo(hullPoints[0]);
        for (int i = 1; i < hullPoints.length; ++i) {
            path.lineTo(hullPoints[1]);
        }
        path.closePath();

        if (path.contains(point)) {
            return 0;
        }

        double minDistance = -1;
        for (int i = 1; i < hullPoints.length; ++i) {
            double distance = Duh.segmentDistance
                (point, hullPoints[i-1], hullPoints[i]);
            if (i == 1 || distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    /** @return the nearest point on the Bezier curve, to within the
        given maximum error bounds. */
    public static DistanceInfo nearestPoint(Point point,
                                            Point2D.Double[] controlPoints,
                                            double maxError) {
    }
}