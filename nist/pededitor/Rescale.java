package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;

public class Rescale {
    public double width;
    public double height;
    public double t;

    /** If there exists a quantity t such that the width of the scaled
     object equals xMargin + t * xSlope and the height equals yMargin
     + t * ySlope, and the width and height cannot exceed the given
     maxima, then determine the maximum value of t that satisfies
     those requirements, as well as the resulting width and height. */
    Rescale(double xSlope, double xMargin, double maxWidth,
            double ySlope, double yMargin, double maxHeight) {
        double x = (maxWidth - xMargin) / xSlope;
        double y = (maxHeight - yMargin) / ySlope;
        t = Math.min(x,y);
        width = xMargin + t * xSlope;
        height = yMargin + t * ySlope;
    }
}
