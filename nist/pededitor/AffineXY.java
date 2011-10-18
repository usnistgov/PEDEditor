package gov.nist.pededitor;

import java.awt.geom.*;

/** The class AffineXY applies the following transformation:

(x,y) -> (? + ? x + ? y + ? xy, ? + ? x + ? y + ? xy) where the ?
stand for different constants.
*/
public class AffineXY extends AffineXYCommon implements Transform2D {

   public Point2D.Double transform(double x, double y) {
      return new Point2D.Double
         (xk + x * (xkx + y * xkxy) + y * xky,
          yk + x * (ykx + y * ykxy) + y * yky);
   }

   public Point2D.Double transform(Point2D.Double p) {
      return transform(p.x, p.y);
   }

   public AffineXYCommon createInverse() {
      AffineXYInverse inv = new AffineXYInverse();
      inv.copyFieldsFrom(this);
      return inv;
   }

   public void concatenate(Transform2D other) {
      throw new UnsupportedOperationException
         ("AffineXY.concatenate() implementation delayed pending need");
   }

   public String toString() {
      return toString("AffineXY");
   }
};
