/* Eric Boesch, NIST Materials Measurement Laboratory, 2016. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

public class SwapXY implements SlopeTransform2D {
    @Override public Point2D.Double transformSlope(double x, double y,
            double dx, double dy) {
        return new Point2D.Double(dy, dx);
    }
    
    @Override public double transformAngle(Point2D p, double theta) {
        return Geom.normalizeRadians(Math.PI/2 - theta);
    }

    @Override public SwapXY createInverse() {
        return this;
    }
    
    @Override public SlopeTransform2D clone() {
        return this;
    }

    @Override public Point2D.Double transform(double x, double y) {
        return new Point2D.Double(y,x);
    }
}
