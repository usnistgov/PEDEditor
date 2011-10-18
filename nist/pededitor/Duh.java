package gov.nist.pededitor;

import java.awt.geom.*;

/*** Trivial min/max helper functions. */

public class Duh {

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
      // There are fancy solutions to in-place rotation, but KISS is
      // good enough here.
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
}