package gov.nist.pededitor;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.Arrays;

/** Utility functions for working with points and polygons. */
public class Duh {

   static class ReverseAngleSort implements Comparator<Point2D.Double> {
      Point2D.Double center;

      protected ReverseAngleSort(Point2D.Double center) {
         this.center = center;
      }

      public int compare(Point2D.Double o1, Point2D.Double o2) {
         double diff = Duh.toAngle(Duh.aMinusB(o2, center)) -
            Duh.toAngle(Duh.aMinusB(o1, center));
         return (diff > 0) ? 1 : (diff == 0) ? 0 : -1;
      }
   }

    /** Sort a list of indices into an array of doubles into
        increasing order (by the double value they point to) */
   static class IndexSort implements Comparator<Integer> {
       double[] values;

       protected IndexSort(double[] values) {
           this.values = values;
       }

       public int compare(Integer i1, Integer i2) {
           double diff = values[i2] - values[i1];
           return (diff > 0) ? 1 : (diff == 0) ? 0 : -1;
       }
   }

    public static Point toPoint(Point2D point) {
        return new Point((int) Math.round(point.getX()),
                         (int) Math.round(point.getY()));
    }

    /** Convert point into a Point by taking the floor of x and y. */
    public static Point floorPoint(Point2D point) {
        return new Point((int) Math.floor(point.getX()),
                         (int) Math.floor(point.getY()));
    }

    public static Point toPoint(Point2D.Double point) {
        return new Point((int) Math.round(point.x), (int) Math.round(point.y));
    }

    public static Point toPoint(double x, double y) {
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    public static Point2D.Double toPoint2DDouble(Point2D p) {
        return new Point2D.Double(p.getX(), p.getY());
    }

    public static Point2D.Double[] toPoint2DDoubles(int[] arr) {
        if (arr.length % 2 != 0) {
            throw new IllegalArgumentException("Odd array size");
        }
        Point2D.Double[] output = new Point2D.Double[arr.length/2];
        int i = 0;
        for (int j = 0; j < arr.length; i++, j+=2) {
            output[i] = new Point2D.Double(arr[j], arr[j+1]);
        }
        return output;
    }

    public static Point2D.Double[] toPoint2DDoubles(Point[] arr) {
        Point2D.Double[] output = new Point2D.Double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            output[i] = new Point2D.Double(arr[i].x, arr[i].y);
        }
        return output;
    }

    public static Point[] toPoints(Point2D.Double[] arr) {
        Point[] output = new Point[arr.length];
        for (int i = 0; i < arr.length; i++) {
            output[i] = toPoint(arr[i]);
        }
        return output;
    }

    public static Point2D.Double[] toPoint2DDoubles(int[][] arr) {
        Point2D.Double[] output = new Point2D.Double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            output[i] = new Point2D.Double(arr[i][0], arr[i][1]);
        }
        return output;
    }

    public static Point2D.Double[] toPoint2DDoubles(double[][] arr) {
        Point2D.Double[] output = new Point2D.Double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            output[i] = new Point2D.Double(arr[i][0], arr[i][1]);
        }
        return output;
    }

    public static double minX (Point2D.Double[] points) {
        double rv = points[0].x;
        for (int i = 1; i < points.length; ++i) {
            double x = points[i].x;
            if (x < rv) {
                rv = x;
            }
        }
        return rv;
    }

    public static double minY (Point2D.Double[] points) {
        double rv = points[0].y;
        for (int i = 1; i < points.length; ++i) {
            double x = points[i].y;
            if (x < rv) {
                rv = x;
            }
        }
        return rv;
    }

    public static double maxX (Point2D.Double[] points) {
        double rv = points[0].x;
        for (int i = 1; i < points.length; ++i) {
            double x = points[i].x;
            if (x > rv) {
                rv = x;
            }
        }
        return rv;
    }

    public static double maxY (Point2D.Double[] points) {
        double rv = points[0].y;
        for (int i = 1; i < points.length; ++i) {
            double x = points[i].y;
            if (x > rv) {
                rv = x;
            }
        }
        return rv;
    }

    public static Point2D.Double mean(Point2D.Double[] points) {
        double totX = 0;
        double totY = 0;
        for (Point2D.Double p : points) {
            totX += p.x;
            totY += p.y;
        }
        return new Point2D.Double(totX / points.length, totY / points.length);
    }

    public static Point2D.Double mean(Point2D.Double a, Point2D.Double b) {
        return new Point2D.Double((a.x + b.x) / 2, (a.y + b.y) / 2);
    }

    public static String toString(Point2D.Double point) {
        return "(" + point.x + ", " + point.y + ")";
    }

    public static double toAngle(Point2D.Double ray) {
        return Math.atan2(ray.y, ray.x);
    }

    public static Point2D.Double aMinusB(Point2D.Double a, Point2D.Double b) {
        return new Point2D.Double(a.x - b.x, a.y - b.y);
    }

    @SuppressWarnings("unchecked")
        public static <T> T[] rotateRight(T[] arr, int amount) {
        T[] output = (T[]) new Object[arr.length];
        if (arr.length == 0) {
            return (T[]) new Object[0];
        }
        amount = amount % arr.length;

        if (amount < 0) {
            amount += arr.length;
        }

        int i = 0;
        for (; i < arr.length - amount; ++i) {
            output[i + amount] = arr[i];
        }

        amount -= arr.length;

        for (; i < arr.length; ++i) {
            output[i + amount] = arr[i];
        }

        return output;
    }

    public static <T> void rotateRightInPlace(T[] arr, int amount) {
        // Don't bother with the fancy in-place rotation methods for now.
        T[] dup = rotateRight(arr, amount);
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = dup[i];
        }
    }

    public static <T> void rotateLeftInPlace(T[] arr, int amount) {
        rotateRightInPlace(arr, -amount);
    }

    public static Rectangle2D.Double bounds(Point2D.Double[] points) {
        double mx = minX(points);
        double my = minY(points);
        return new Rectangle2D.Double(mx, my,
                                      maxX(points) - mx, maxY(points) - my);
    }

    public static Rectangle bounds(Point[] points) {
        if (points == null || points.length == 0) {
            return null;
        }
        int minx, maxx;
        minx = maxx = points[0].x;
        int miny, maxy;
        miny = maxy = points[0].y;
        for (int i = 1; i < points.length; ++i) {
            int x = points[i].x;
            int y = points[i].y;
            if (x < minx) minx = x;
            if (x > maxx) maxx = x;
            if (y < miny) miny = y;
            if (y > maxy) maxy = y;
        }

        return new Rectangle(minx, miny, maxx - minx + 1, maxy - miny + 1);
    }

    /** @return the distance between p and the nearest point on rect.
        The return value will be 0 if p is in rect. */
    public static double distanceSq(Point2D p, Rectangle2D rect) {
        double min;
        double max;
        double x0 = p.getX();
        double x1 = (x0 > rect.getMaxX()) ? rect.getMaxX()
            : (x0 < rect.getMinX()) ? rect.getMinX()
            : x0;
        double y0 = p.getY();
        double y1 = (y0 > rect.getMaxY()) ? rect.getMaxY()
            : (y0 < rect.getMinY()) ? rect.getMinY()
            : y0;

        double dx = x1 - x0;
        double dy = y1 - y0;

        return dx * dx + dy * dy;
    }

    /** @return the distance between p and the nearest point on rect.
        The return value will be 0 if p is in rect. */
    public static double distance(Point2D p, Rectangle2D rect) {
        return Math.sqrt(distanceSq(p, rect));
    }

    /* @see sortIndices()

       Sort the given vertices in place into clockwise order, starting
       with the lower-left corner. Do not modify the input array.

       @return the resulting array. */
    static Point2D.Double[] sortExternal(Point2D.Double[] points,
                                         boolean yAxisPointsDown) {
        int[] indices = sortIndices(points, yAxisPointsDown);
        Point2D.Double[] output = new Point2D.Double[points.length];
        for (int i = 0; i < points.length; ++i) {
            output[i] = points[i];
        }
        return output;
    }

    /* @see sortIndices()

       Sort the given vertices in place into clockwise order, starting
       with the lower-left corner. */
    static void sort(Point2D.Double[] points, boolean yAxisPointsDown) {
        Point2D.Double[] output = sortExternal(points, yAxisPointsDown);
        for (int i = 0; i < points.length; ++i) {
            points[i] = output[i];
        }
    }

    static int[] sortIndices(Point2D.Double[] points) {
        return sortIndices(points, true);
    }

    /** @return a list of index values into the points[] array sorted
        so that they represent the vertices of a polygon in clockwise
        order, starting with the lower left vertex (for upwards-point
        y axis, this means the vertex with the least (x+y) value). For
        good results, the points must form a convex
        (points.length)-gon after sorting.

        @param yAxisPointsDown If true, assume the positive Y axis
        points downwards for purposes of determining clockwise
        orientation and the lower-left vertex.
    */
    static int[] sortIndices(Point2D.Double[] points, boolean yAxisPointsDown) {
        int cnt = points.length;
        
        if (cnt <= 1) {
            int[] indices = new int[cnt];
            for (int i = 0; i < indices.length; ++i) {
                indices[i] = i;
            }
            return indices;
        }

        Point2D.Double center = mean(points);
        double mul = yAxisPointsDown ? -1.0 : 1.0;

        double[] angles = new double[cnt];
        Integer[] indicesI = new Integer[cnt];
        for (int i = 0; i < angles.length; ++i) {
            angles[i] = mul * Duh.toAngle(Duh.aMinusB(points[i], center));
            indicesI[i] = i;
        }
      
        Arrays.sort(indicesI, new IndexSort(angles));

        // Select the point with smallest X+Y.
        double minXPlusY = 0;
        int minIndex = 0;
        for (int i = 0; i < cnt; ++i) {
            Point2D.Double point = points[indicesI[i]];
            double xpy = point.x + mul * point.y;
            if (i == 0 || xpy < minXPlusY) {
                minXPlusY = xpy;
                minIndex = i;
            }
        }
        

        // Rotate the array left to put minIndex in position 0.
        rotateLeftInPlace(indicesI, minIndex);

        int[] indices = new int[cnt];
        for (int i = 0; i < cnt; ++i) {
            indices[i] = indicesI[i];
        }

        return indices;
    }

    /** Sort the points into clockwise order starting from the lower
        left, and return a Polygon of those points. */
    public static Polygon sortToPolygon(Point[] points,
                                        boolean yAxisPointsDown) {
        int cnt = points.length;
        int[] indices = sortIndices(toPoint2DDoubles(points), yAxisPointsDown);
        int[] xs = new int[cnt];
        int[] ys = new int[cnt];
        for (int i = 0; i < cnt; ++i) {
            Point p = points[indices[i]];
            xs[i] = p.x;
            ys[i] = p.y;
        }

        return new Polygon(xs, ys, cnt);
    }

    /** Sort the points into clockwise order starting from the lower
        left, and return a Polygon of those points. */
    public static Polygon sortToPolygon(ArrayList<Point> points,
                                        boolean yAxisPointsDown) {
        return sortToPolygon(points.toArray(new Point[0]), yAxisPointsDown);
    }

    /** @return a deep copy of the given array. */
    public static Point2D.Double[] deepCopy(Point2D.Double[] original) {
        int length = original.length;
        Point2D.Double[] output = new Point2D.Double[length];
        for (int i = 0; i < length; ++i) {
            Point2D.Double p = original[i];
            output[i] = new Point2D.Double(p.x, p.y);
        }
        return output;
    }

    /** @return the cross product v1 x v2. The returned value will be
        positive if v2 is between 0 and 180 degrees counterclockwise
        from v1. */
    static double crossProduct(Point2D v1, Point2D v2) {
        return v1.getX() * v2.getY() - v1.getY() - v2.getX();
    }

    /** @return the cross product a1a2 x b1b2. The returned value will be
        positive if b1b2 is between 0 and 180 degrees counterclockwise
        from a1a2. */
    static double crossProduct(Point2D a1, Point2D a2,
                               Point2D b1, Point2D b2) {
        return (a2.getX() - a1.getX()) * (b2.getY() - b1.getY())
            - (a2.getY() - a1.getY()) * (b2.getX() - b1.getX());
    }

    /** @return the cross product p1p2 x p1p3. The returned value will be
        positive if p1p3 is between 0 and 180 degrees counterclockwise
        from p1p2. */
    static double crossProduct(Point2D p1, Point2D p2, Point2D p3) {
        return (p2.getX() - p1.getX()) * (p3.getY() - p1.getY())
            - (p2.getY() - p1.getY()) * (p3.getX() - p1.getX());
    }

    /** @return the convex hull of inputs[], starting with the point
        with lowest y value and proceeding clockwise or
        counterclockwise depending on whether the Y axis points down
        or upwards.

        The algorithm used is adapted (and corrected, I hope) from the
        Wikipedia article "Graham Scan", which in turn is adapted from
        Segdewick and Wayne's <i>Algorithms, 4th edition</i>. */
    static public Point2D.Double[] convexHull(Point2D.Double[] inputs) {
        int cnt = inputs.length;
        if (cnt == 0) {
            return new Point2D.Double[0];
        }

        Point2D.Double lowestPoint = null;

        // Find the point with lowest y value, with lower x value as
        // tiebreaker.
        int indexOfLowest = -1;
        int index = 0;
        for (Point2D.Double point: inputs) {
            if (lowestPoint == null
                || point.y < lowestPoint.y
                || (point.y == lowestPoint.y && point.x < lowestPoint.x)) {
                lowestPoint = point;
                indexOfLowest = index;
            }
            ++index;
        }


        // Make an array of point indices into the inputs[] array.
        Integer[] pointIndices = new Integer[cnt];
        for (int i = 0; i < cnt; ++i) {
            pointIndices[i] = i;
        }

        // Swap the indexOfLowest point into position 0.
        pointIndices[indexOfLowest] = 0;
        pointIndices[0] = indexOfLowest;

        // Remove any duplicates of that lowest point to avoid
        // division by zero during the angle computation phase.
        int j = 1;
        for (int i = 1;  i < cnt; ++i) {
            Point2D.Double point = inputs[pointIndices[i]];
            if (!lowestPoint.equals(point)) {
                pointIndices[j++] = i;
            }
        }

        cnt = j;
        if (cnt == 1) {
            return new Point2D.Double[] {
                new Point2D.Double(lowestPoint.x, lowestPoint.y) };
        }

        // Sort the other points by a hopefully faster-to-compute
        // stand-in for the polar angle (that is also strictly
        // decreasing for angles between 0 and pi radians) through
        // lowestPoint. (This computation is also faster and more
        // sensitive to small changes in y value than the cosine.)
        double[] pseudoAngles = new double[cnt];
        for (int i = 1;  i < cnt; ++i) {
            Point2D.Double point = inputs[pointIndices[i]];
            double dx = point.x - lowestPoint.x;
            double dy = point.y - lowestPoint.y;
            double dx2 = dx * dx;
            pseudoAngles[i] = dx / (dy + Math.abs(dx));
        }
        Arrays.sort(pointIndices, 1, pointIndices.length,
                    new IndexSort(pseudoAngles));

        // When the minimum polar angle is shared among several
        // points, we can't allow the point among them that has the
        // greatest distance from lowestPoint to be discarded. One way
        // to avoid this is to keep only the farthest one and discard
        // all others right away.

        double minPseudoAngle = pseudoAngles[pointIndices[1]];
        int farthestIndex = -1;
        double maxDistance = 0;

        for (j = 1; j < cnt; ++j) {
            int jIndex = pointIndices[j];
            if (pseudoAngles[jIndex] != minPseudoAngle) {
                break;
            }
            Point2D.Double point = inputs[jIndex];
            double distance = (point.y - lowestPoint.y)
                + Math.abs(point.x - lowestPoint.x);

            if (distance > maxDistance) {
                farthestIndex = jIndex;
                maxDistance = distance;
            }
        }
        // If j < cnt, then j now equals the index into pointsIndex of
        // the index of the point whose pseudoAngle value is least
        // while still exceeding minPseudoAngle

        ArrayList<Point2D.Double> outputs
            = new ArrayList<Point2D.Double>();

        // Add lowestPoint and inputs[farthestIndex], which are
        // definitely part of the convex hull.
        outputs.add(lowestPoint);
        outputs.add(inputs[farthestIndex]);

        for (; j < cnt; ++j) {
            Point2D.Double point = inputs[pointIndices[j]];
            Point2D.Double last;
            Point2D.Double nextToLast;

            while (true) {
                last = outputs.get(outputs.size() - 1);
                nextToLast = outputs.get(outputs.size() - 2);
                if (crossProduct(nextToLast, last, point) <= 0) {
                    // From nextToLast to last to point is a straight
                    // or right turn, so "last" is no longer a
                    // candidate member of the convex hull.
                    outputs.remove(outputs.size() - 1);
                } else {
                    break;
                }
            }

            outputs.add(point);
        }

        return outputs.toArray(new Point2D.Double[0]);
    }
    

    /** @return the point on segment p1p2 that is nearest to p0. */
    public static Point2D.Double nearestPointOnSegment
        (Point2D p0, Point2D p1, Point2D p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dx2 = p0.getX() - p1.getX();
        double dy2 = p0.getY() - p1.getY();
        double dot = dx * dx2 + dy * dy2;
        double p1p2LengthSq = dx * dx + dy * dy;
        if (dot < 0 || p1p2LengthSq == 0) {
            return new Point2D.Double(p1.getX(), p1.getY());
        }

        dot /= p1p2LengthSq;

        // Now dot equals the ratio of the distance between p1 and the
        // projection of p0 onto p1p2 to the distance between p1 and
        // p2.

        if (dot < 1.0) {
            return new Point2D.Double(p1.getX() + dx * dot, p1.getY() + dy * dot);
        } else {
            return new Point2D.Double(p2.getX(), p2.getY());
        }
    }
    

    /** @return a point where segments a1a2 and b1b2 intersect, or
        null if no such point exists.

        The return value is undefined if tiny changes to the locations
        of one or more input points could yield major changes in the
        correct result, such as if the intersection lies at the
        endpoint of one or more segment or if the segments are
        parallel. Still, some minimal attempt is made to handle such
        cases correctly.
    */
    public static Point2D.Double segmentIntersection
        (Point2D a1, Point2D a2, Point2D b1, Point2D b2) {

        // Don't worry too much about the intractible instability
        // problems, but the problem of one or both of a1a2 and b1b2
        // being parallel or nearly parallel to the x or y axis is
        // important and sometimes tractible.

        if (crossProduct
            (new Point2D.Double(Math.abs(a2.getX() - a1.getX()),
                                Math.abs(a2.getY() - a1.getY())),
             new Point2D.Double(Math.abs(b2.getX() - b1.getX()),
                                Math.abs(b2.getY() - b1.getY()))) < 0) {
            // a1a2 has steeper slope than b1b2, which could cause
            // numerical instability problems later. Swap the two
            // pairs.

            Point2D tmpp;

            tmpp = a1;
            a1 = b1;
            b1 = tmpp;

            tmpp = a2;
            a2 = b2;
            b2 = tmpp;
        }

        if (crossProduct(a1, a2, b1, b2) == 0) {
            // Either the two segments are parallel, or at least one
            // of the segments has length 0. In that case, the
            // segments only intersect if they intersect at the
            // endpoint of one of the two segments.

            if (segmentDistance(a1, b1, b2) == 0) {
                return toPoint2DDouble(a1);
            }
            if (segmentDistance(a2, b1, b2) == 0) {
                return toPoint2DDouble(a2);
            }

            if (segmentDistance(b1, a1, a2) == 0) {
                return toPoint2DDouble(b1);
            }
            if (segmentDistance(b2, a1, a2) == 0) {
                return toPoint2DDouble(b2);
            }

            // Parallel segments for which neither segment's endpoints
            // lie on the other segment do not intersect.

            return null;
        }

        // a1a2 and b1b2 are non-parallel segments with positive
        // length, and b1b2 is steeper than a1a2. So a1a2 is not
        // vertical, and b1b2 is not horizontal.

        double m = (a2.getY() - a1.getY()) / (a2.getX() - a1.getX());
        double b = a2.getY() - m * a2.getX();

        // a1a2 lies on the line y = m x + b

        double q = (b2.getX() - b1.getX()) / (b2.getY() - b1.getY());
        double r = b2.getX() - q * b2.getY();

        // b1b2 lies on the line x = qy + r

        // Substitute (x = qy + r) into the first equation:

        // y = m (qy + r) + b

        // y(1 - mq) = mr + b

        double denom = 1 - m * q;

        if (denom == 0) {
            // It looks like the two lines are parallel after all --
            // limits of numerical precision must be at fault. In that
            // numerically unstable case, the answer is undefined and
            // we can just as well return "no intersection".

            return null;
        }

        double y = (m * r + b) / denom;
        double x = q * y + r;

        // (x,y) is on both segments if its x value is in [ax1, ax2]
        // and its y values is in [by1, by2] -- a formulation that
        // takes into account that a1a2 is not vertical and b1b2 is
        // not horizontal.

        if (((x >= a1.getX() && x <= a2.getX())
             || (x <= a1.getX() && x >= a2.getX()))
            && ((y >= b1.getY() && y <= b2.getY())
                || (y <= b1.getY() && y >= b2.getY()))) {
            return new Point2D.Double(x,y);
        } else {
            return null;
        }
    }
    

    /** @return the distance from p0 to the nearest point on segment
        p1p2. */
    public static double segmentDistance
        (Point2D p0, Point2D p1, Point2D p2) {
        Point2D.Double nearest = nearestPointOnSegment(p0, p1, p2);
        return nearest.distance(p0);
    }

    public static java.awt.geom.Point2D.Double[] argsToPoints(String[] args) {
        ArrayList<Point2D.Double> vertices
            = new ArrayList<Point2D.Double>();
        for (int i = 0; i < args.length; i+=2) {
            vertices.add(new Point2D.Double(Double.parseDouble(args[i]),
                                            Double.parseDouble(args[i+1])));
        }
        return vertices.toArray(new Point2D.Double[0]);
    }

    public static void hullTest(String[] args) {
        Point2D.Double[] hull = convexHull(argsToPoints(args));
        for (Point2D p: hull) {
            System.out.println(p);
        }
    }

    public static void segmentTest(String[] args) {
        Point2D.Double[] points = argsToPoints(args);
        int segCnt = points.length / 2;

        for (int i = 0; i < segCnt; ++i) {
            Point2D.Double a1 = points[i*2];
            Point2D.Double a2 = points[i*2 + 1];
            for (int j = i + 1; j < segCnt; ++j) {
                Point2D.Double b1 = points[j*2];
                Point2D.Double b2 = points[j*2 + 1];
                Point2D.Double p = Duh.segmentIntersection(a1,a2,b1,b2);
                if (p != null) {
                    System.out.println(a1 + " - " + a2 + " intersects "
                                       + b1 + " - " + b2 + " at " + p);
                }
            }
        }
    }

    public static void main(String[] args) {
        segmentTest(args);
    }
}
