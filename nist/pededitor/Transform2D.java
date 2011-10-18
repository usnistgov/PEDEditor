package gov.nist.pededitor;

import java.awt.geom.*;

interface Transform2D {
   /** @return the inverse transform. */
   Transform2D createInverse() throws NoninvertibleTransformException;

   /** Change this transformation into one that represents the
    * composition of this transformation with the other
    * transformation. May not actually work, and should probably throw
    * some kind of exception in that case. */
   void concatenate(Transform2D other);

   /** @return the transformation of (x,y) as a Point2D.Double */
   Point2D.Double transform(double x, double y) throws UnsolvableException;
   Point2D.Double transform(Point2D.Double point) throws UnsolvableException;
}
