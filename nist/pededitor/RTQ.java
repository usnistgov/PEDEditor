package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;

public class RTQ {
   public static void main(String[] args) {
      double[][][] vertexSets = 
         {{{3,1}, {0,2}, {2,3}, {1,0}}, // rotated square
          {{8,3}, {4,5}, {7,5}, {3,3}}, // trapezoid
          {{0,4}, {4,4}, {2,5}, {2, 0}}, // kite_reverse
          // {{3,0.5},{4,5},{2,3},{0,2}}, // Transforming a concave
          // quadrilateral causes an exception (as intended)
          {{4,0}, {0,2}, {2,8}, {9,7}}};
      
      for (int i = 0; i < vertexSets.length; ++i) {
         double[][] vs = vertexSets[i];
         Point2D.Double[] points = new Point2D.Double[vs.length];

         for (int j = 0; j < vs.length; ++j) {
            points[j] = new Point2D.Double(vs[j][0],vs[j][1]);
         }
         
         RectToQuad.sort(points);
         RectToQuad xform = new RectToQuad();

         xform.setVertices(points);
         xform.setX(3); xform.setY(2); xform.setW(4); xform.setH(4);

         System.out.println("xform is " + xform);
         xform.check();
         System.out.println("Inverting.");

         QuadToRect xform2 = (QuadToRect) xform.createInverse();

         System.out.println("xfrm2 is " + xform2);
         xform2.check();
         System.out.println("Check passed.");
      }
   }
}