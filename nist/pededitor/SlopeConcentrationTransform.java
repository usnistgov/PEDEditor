/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

interface SlopeConcentrationTransform extends ConcentrationTransform {
    /** Given a line or curve whose slope at the given point is as
        given, return the slope of the transformation of that line or
        curve at the transformed point. This is useful for adjusting the
        angles of arrows and text inside a diagram. */
    Point2D.Double transformSlope(double x, double y,
            double dx, double dy);
    
    /** Like transformSlope(), but applied to an angle. */
    default double transformAngle(Point2D p, double theta) {
        Point2D.Double p2 = transformSlope
            (p.getX(), p.getY(),
             Math.cos(theta), Math.sin(theta));
        return Math.atan2(p2.y, p2.x);
    }

    @Override SlopeConcentrationTransform createInverse();
    SlopeConcentrationTransform clone();
}
