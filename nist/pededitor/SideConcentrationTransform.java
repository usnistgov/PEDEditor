/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

public class SideConcentrationTransform implements SlopeTransform2D {
    public Side[] sides;
    public SlopeConcentrationTransform xform;

    public SideConcentrationTransform(Side[] sides,
            SlopeConcentrationTransform xform) {
        this.sides = sides;
        this.xform = xform;
    }

    @Override public SideConcentrationTransform createInverse() {
        return new SideConcentrationTransform(sides, xform.createInverse());
    }

    @Override public SideConcentrationTransform clone() {
        return new SideConcentrationTransform(sides, xform.clone());
    }

    public double[] toValues(double x, double y) {
        int len = sides.length;
        double[] res = new double[len];
        for (int i = 0; i < len; ++i) {
            double d = 0;
            switch (sides[i]) {
            case RIGHT:
                d = x;
                break;
            case TOP:
                d = y;
                break;
            case BOTTOM:
                d = 1 - y;
                break;
            case LEFT:
                switch (len) {
                case 3:
                    d = 1 - x - y;
                    break;
                case 2:
                    d = 1 - x;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong dimension");
                }
            }
            res[i] = d;
        }
        return res;
    }

    @Override public Point2D.Double transform(double x, double y) {
        double[] values = toValues(x,y);
        xform.transform(values);
        if (xform.componentCnt() == 2) {
            return new Point2D.Double(values[0], y);
        } else {
            return new Point2D.Double(values[0], values[1]);
        }
    }

    @Override public Point2D.Double transformSlope(double x, double y, double dx, double dy) {
        return xform.transformSlope(x, y, dx, dy);
    }
}
