package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;
import java.text.*;

/** Class to hold information about the third axis of a ternary
    diagram. */
public class ThirdTernaryAxisInfo extends AxisInfo {
    public ThirdTernaryAxisInfo(NumberFormat format) {
        super(format);
        name = "Z";
    }

    public ThirdTernaryAxisInfo() {
        this(new DecimalFormat("##0.0"));
    }

    public double value(double px, double py) {
        return 100.0 - px - py;
    }
}
