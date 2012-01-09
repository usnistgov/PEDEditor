package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;
import java.text.*;

/** Class to hold information about an axis whose value equals

    (ax + by + c). */
public class LinearAxisInfo extends AxisInfo {
    public double a;
    public double b;
    public double c;

    public LinearAxisInfo(NumberFormat format, double... coefs) {
        super(format);
        this.a = coefs[0];
        this.b = coefs[1];
        this.c = (coefs.length >= 3) ? coefs[2] : 0.0;
    }

    public LinearAxisInfo(double... coefs) {
        this(getDefaultFormat(), coefs);
    }

    public LinearAxisInfo() {
        this (0.0, 0.0, 0.0);
    }

    public double value(double px, double py) {
        return a * px + b * py + c;
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
