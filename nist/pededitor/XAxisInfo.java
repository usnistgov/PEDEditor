package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;
import java.text.*;

/** Class to hold information about the X axis. */
public class XAxisInfo extends AxisInfo {
    public XAxisInfo(NumberFormat format) {
        super(format);
        name = "X";
    }

    public XAxisInfo() {
        this(new DecimalFormat("##0.0"));
    }

    public double value(double px, double py) {
        return px;
    }
}
