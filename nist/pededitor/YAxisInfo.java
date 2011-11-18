package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;
import java.text.*;

/** Class to hold information about the X axis. */
public class YAxisInfo extends AxisInfo {
    public YAxisInfo(NumberFormat format) {
        super(format);
        name = "Y";
    }

    public YAxisInfo() {
        this(new DecimalFormat("##0.0"));
    }

    public double value(double px, double py) {
        return py;
    }
}
