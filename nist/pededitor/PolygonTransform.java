package gov.nist.pededitor;

import java.awt.geom.*;

interface PolygonTransform extends Transform2D {
   /** The four input vertices of the quadrilateral. */
   Point2D.Double[] inputVertices();
   /** The four output vertices of the quadrilateral. */
   Point2D.Double[] outputVertices();
   Rectangle2D.Double inputBounds();
   Rectangle2D.Double outputBounds();
}
