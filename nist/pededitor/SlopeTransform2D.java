/* Eric Boesch, NIST Materials Measurement Laboratory, 2016. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

interface SlopeTransform2D extends Transform2D {
        Point2D.Double transformSlope(double x, double y,
            double dx, double dy);
    
    /** Like transformSlope(), but applied to an angle in radians. */
    default double transformAngle(Point2D p, double theta) {
        Point2D.Double p2 = transformSlope
            (p.getX(), p.getY(),
             Math.cos(theta), Math.sin(theta));
        return Math.atan2(p2.y, p2.x);
    }

    @Override SlopeTransform2D createInverse();
    @Override SlopeTransform2D clone();
}
