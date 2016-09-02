/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/** Class to hold information about an axis whose value equals

    (ax + by + c). */
public class LinearFunction {

    double a;
    double b;
    double c;

    /** Create a LinearAxisInfo for the formula a*x + b*y + c. */
    public LinearFunction(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override public LinearFunction clone() {
        return new LinearFunction(a,b,c);
    }

    public double value(double px, double py) {
        return a * px + b * py + c;
    }

    /** Analogous to AffineTransform.deltaTransform(). Return
        value(Point(x0 + dx, y0 + dy)) - value(Point(x0, y0)). The
        value is independent of x0 and y0 because the axis is
        linear. */
    public double deltaValue(double dx, double dy) {
        return a * dx + b * dy;
    }

    public double getA() { return a; }
    public double getB() { return b; }
    public double getC() { return c; }

    public void setA(double v) { a = v; }
    public void setB(double v) { b = v; }
    public void setC(double v) { c = v; }

    /** Change this linear axis to be the one that results from
        applying the current function to the transformed x and y
        values. 
     * @return */
    public void concatenate(AffineTransform xform) {
        double newA = a * xform.getScaleX() + b * xform.getShearY();
        double newB = a * xform.getShearX() + b * xform.getScaleY();
        double newC = a * xform.getTranslateX() + b * xform.getTranslateY()
            + c;
        a = newA;
        b = newB;
        c = newC;
    }

    public Point2D.Double gradient() {
        return new Point2D.Double(a, b);
    }

    /** Return the gradient of this axis in the space whose transform
        to the space in which this axis' coordinates are defined is
        toPrincipal. */
    public Point2D.Double gradient(AffineTransform toPrincipal) {
        if (toPrincipal == null) {
            return null;
        }
        return new Point2D.Double
            (s2pValue(new Point2D.Double(1,0), toPrincipal),
             s2pValue(new Point2D.Double(0,1), toPrincipal));
    }

    private double s2pValue(Point2D.Double d, AffineTransform toPrincipal) {
        toPrincipal.deltaTransform(d, d);
        return deltaValue(d.x, d.y);
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "['"
            + a + " x + " + b + " y + " + c + "]";
    }
}
