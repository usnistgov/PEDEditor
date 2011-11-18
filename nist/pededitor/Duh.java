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

    public static Point toPoint(Point2D.Double point) {
        return new Point((int) Math.round(point.x), (int) Math.round(point.y));
    }

    public static Point toPoint(double x, double y) {
        return new Point((int) Math.round(x), (int) Math.round(y));
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

    /** @return the point on segment p1p2 that is nearest to p0. */
    public static Point2D.Double nearestPointOnSegment
        (Point2D p0, Point2D p1, Point2D p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dx2 = p0.getX() - p1.getX();
        double dy2 = p0.getY() - p1.getY();
        double dot = dx * dx2 + dy * dy2;
        if (dot < 0) {
            return (Point2D.Double) p1.clone();
        }

        double p1p2LengthSq = dx * dx + dy * dy;
        dot /= p1p2LengthSq;

        // Now dot equals the ratio of the distance between p1 and the
        // projection of p0 onto p1p2 to the distance between p1 and
        // p2.

        if (dot < 1.0) {
            return new Point2D.Double(p1.getX() + dx * dot, p1.getY() + dy * dot);
        } else {
            return (Point2D.Double) p2.clone();
        }
    }
}
