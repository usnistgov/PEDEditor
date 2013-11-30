package gov.nist.pededitor;

import java.awt.geom.Point2D;

public class SideConcentrationTransform {
    public Side[] sides;
    public ConcentrationTransform xform;

    public SideConcentrationTransform(Side[] sides,
                                      ConcentrationTransform xform) {
        this.sides = sides;
        this.xform = xform;
    }

    public SideConcentrationTransform inverse() {
        return new SideConcentrationTransform(sides, xform.inverse());
    }

    public double[] toValues(Point2D p) {
        int len = sides.length;
        double[] res = new double[len];
        for (int i = 0; i < len; ++i) {
            double d = 0;
            switch (sides[i]) {
            case RIGHT:
                d = p.getX();
                break;
            case TOP:
                d = p.getY();
                break;
            case BOTTOM:
                d = 1 - p.getY();
                break;
            case LEFT:
                switch (len) {
                case 3:
                    d = 1 - p.getY() - p.getX();
                    break;
                case 2:
                    d = 1 - p.getX();
                    break;
                default:
                    throw new IllegalArgumentException("Wrong dimension");
                }
            }
            res[i] = d;
        }
        return res;
    }

    public Point2D.Double transform(Point2D p) {
        double[] values = toValues(p);
        xform.transform(values);
        if (xform.componentCnt() == 2) {
            return new Point2D.Double(values[0], p.getY());
        } else {
            return new Point2D.Double(values[0], values[1]);
        }
    }
}
