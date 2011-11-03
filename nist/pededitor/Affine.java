package gov.nist.pededitor;

import java.awt.geom.*;

/** Just a Transform2D-implementing wrapper around
 * awt.geom.AffineTransform. */
public class Affine extends AffineTransform implements Transform2D {

   /**
	 * 
	 */
	private static final long serialVersionUID = -867608180933463982L;

/** Identical to superclass constructor. */
   public Affine() {
      super();
   }

   /** Identical to superclass constructor. */
   public Affine(AffineTransform Tx) {
      super(Tx);
   }

   /** Identical to superclass constructor. */
   public Affine(double[] flatmatrix) {
      super(flatmatrix);
   }

   /** Identical to superclass constructor. */
   public Affine(double m00, double m10, double m01, double m11,
                 double m02, double m12) {
      super(m00,  m10, m01, m11, m02, m12);
   }

   public Point2D.Double transform(double x, double y) {
      Point2D.Double point = new Point2D.Double(x,y);
      transform(point, point);
      return point;
   }

   public Point2D.Double transform(Point2D.Double p) {
      return transform(p.x, p.y);
   }

   public Affine createInverse() throws NoninvertibleTransformException {
      return new Affine(super.createInverse());
   }

   public void concatenate(Transform2D other) {
      AffineTransform at = (AffineTransform) other;
      super.concatenate(at);
   }

   public void preConcatenate(Transform2D other) {
      AffineTransform at = (AffineTransform) other;
      super.preConcatenate(at);
   }

    /** This transformation never throws an UnsolvableException. */
    public boolean transformNeverThrows() {
        return true;
    }
    
    public Affine clone() {
    	return new Affine(this); 
    }
}
