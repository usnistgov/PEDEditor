package gov.nist.pededitor;

import java.awt.geom.*;

/** Transform a rectangles with sides parallel to the coordinate axes
to an arbitrary quadrilaterals. These transformations have to be
non-affine in general: three point translations define an affine
transformation, so how do you make the fourth point fit? By adding an
extra xy dependency to both the x and y coordinates. */
public class RectToQuad extends RectToQuadCommon {

   public RectToQuad() {
      xform = new AffineXY();
   }

   public RectToQuad(RectToQuad other) {
      xform = new AffineXY();
      copyFieldsFrom(other);
   }

   /** @return the inverse transformation as a QuadToRect object. */
   public QuadrilateralTransform createInverse() {
      QuadToRect inv = new QuadToRect();
      inv.copyFieldsFrom(this);
      return inv;
   }

   public Point2D.Double[] inputVertices() {
      return rectVertices();
   }

   public Point2D.Double[] outputVertices() {
      return quadVertices();
   }

   // Returns a transformation from the unit square to the input rectangle
   public Affine squareToDomain() {
     return new Affine(w, 0, 0, h, x, y);
   }

   protected void update() {
      super.update();
      // This could be turned off if speed becomes critical, but
      // right now transformation updates are not that frequent.
      check();
   }

   public String toString() {
      return toString("RectToQuad");
   }

   public void concatenate(Transform2D other) {
      transformQuad(other);
   }

   public void preConcatenate(Transform2D other) {
      try {
         transformRect(other.createInverse());
      } catch (NoninvertibleTransformException e) {
         throw new RuntimeException(e);
      }
   }
}
