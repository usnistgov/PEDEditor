package gov.nist.pededitor;

import java.awt.geom.*;

interface PolygonTransform extends Transform2D {
    /** The polygon's input vertices. */
    Point2D.Double[] inputVertices();
    /** The polygon's output vertices. */
    Point2D.Double[] outputVertices();
    Rectangle2D.Double inputBounds();
    Rectangle2D.Double outputBounds();
}
