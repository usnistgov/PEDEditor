package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;
import java.text.*;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonManagedReference;
import org.codehaus.jackson.annotate.JsonProperty;

/** Class to hold information about an axis whose value equals

    (ax + by + c). */
public class LinearAxisInfo extends AxisInfo {

    double a;
    double b;
    double c;
    @JsonManagedReference public ArrayList<LinearRuler> rulers
        = new ArrayList<LinearRuler>();

    /** Create a LinearAxisInfo for the formula a*x + b*y + c. */
    public LinearAxisInfo(@JsonProperty("format") NumberFormat format,
                          @JsonProperty("a") double a,
                          @JsonProperty("b") double b,
                          @JsonProperty("c") double c) {
        super(format);
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /** Create a LinearAxisInfo for the formula a*x + b*y + c. */
    public LinearAxisInfo(double a, double b, double c) {
        this(getDefaultFormat(), a, b, c);
    }

    public LinearAxisInfo() {
        this(0.0, 0.0, 0.0);
    }

    public double value(double px, double py) {
        return a * px + b * py + c;
    }

    public double getA() { return a; }
    public double getB() { return b; }
    public double getC() { return c; }

    public void setA(double v) { a = v; }
    public void setB(double v) { b = v; }
    public void setC(double v) { c = v; }

    /** @return true if f(x,y) == x. */
    @JsonIgnore public boolean isXAxis() {
        return (a == 1 && b == 0 && c == 0);
    }

    /** @return true if f(x,y) == y. */
    @JsonIgnore public boolean isYAxis() {
        return (a == 0 && b == 1 && c == 0);
    }

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


    static NumberFormat getDefaultFormat() {
        return new DecimalFormat("##0.0");
    }

    static public LinearAxisInfo createXAxis(NumberFormat format) {
        LinearAxisInfo output = new LinearAxisInfo(format, 1.0, 0.0, 0.0);
        output.name = "X";
        return output;
    }

    static public LinearAxisInfo createXAxis() {
        return createXAxis(getDefaultFormat());
    }

    static public LinearAxisInfo createFromAffine
        (NumberFormat format, AffineTransform xform, boolean isY) {
        if (isY) {
            return new LinearAxisInfo(format, xform.getShearY(),
                                      xform.getScaleY(), xform.getTranslateY());
        } else {
            return new LinearAxisInfo(format, xform.getScaleX(),
                                      xform.getShearX(), xform.getTranslateX());
        }
    }

    static public LinearAxisInfo createYAxis(NumberFormat format) {
        LinearAxisInfo output = new LinearAxisInfo(format, 0.0, 1.0, 0.0);
        output.name = "Y";
        return output;
    }

    static public LinearAxisInfo createYAxis() {
        return createYAxis(getDefaultFormat());
    }
}
