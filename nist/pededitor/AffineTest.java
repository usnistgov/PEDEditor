package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class AffineTest {
   public static void main(String[] args) {
      int pointCnt = 1000000;
      int repCnt = 100;

      if (false) {
         for (int i = 0; i < repCnt; ++i) {
            double[] inPoints = new double[pointCnt * 2];
            double[] outPoints = new double[pointCnt * 2];

            double[] ftrans = {3,1,7,5,1,4};

            AffineTransform transform = new AffineTransform(ftrans);
            for (int j = 0; j < inPoints.length; j+= 2) {
               inPoints[j] = j*1.7 + 3;
            }

            transform.transform(inPoints, 0, outPoints, 0, pointCnt);
         }
      } else {
         double[] ftrans = {3,1,7,5,1,4};
         AffineTransform transform = new AffineTransform(ftrans);

         double total = 0.0;

         Point2D.Double point = new Point2D.Double();

         for (int i = 0; i < repCnt * pointCnt; ++i) {
            point.setLocation(i*2.0, i*3.1);
            transform.transform(point, point);

            total += point.getX() + point.getY();
         }

         System.out.println(total);
      }
   }
}
