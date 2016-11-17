/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;
import java.util.function.DoubleUnaryOperator;

/** Class that supports computing distances from a point to a curve
    with a defined derivative. */
public class Param2Ds {
    public static class DLengthDT implements DoubleUnaryOperator {
        Param2D p;

        public DLengthDT(Param2D p) {
            this.p = p;
        }

        @Override public double applyAsDouble(double t) {
            Point2D.Double d = p.getDerivative(t);
            if (d == null) {
                return 0;
            }
            double x = d.x;
            double y = d.y;
            return Math.sqrt(x*x + y*y);
        }
    }

    public static class DAreaDT implements DoubleUnaryOperator {
        Param2D p;
        Param2D d;

        public DAreaDT(Param2D p) {
            this.p = p;
            this.d = p.derivative();
        }

        @Override public double applyAsDouble(double t) {
            return p.getLocation(t).y * d.getLocation(t).x;
        }
    }

    static public AdaptiveRombergIntegral lengthIntegral
        (Param2D c, double lo, double hi) {
        DoubleUnaryOperator dsdt = new Param2Ds.DLengthDT(c);
        return new AdaptiveRombergIntegral(dsdt, lo, hi);
    }
}
