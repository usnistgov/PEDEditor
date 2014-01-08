/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;


/** This class' purpose is described in its constructor docs. */
public class Rescale {
    public double width;
    public double height;
    public double t;

    /** If there exists a quantity t such that the width of the scaled
        object equals xMargin + t * xSlope and the height equals
        yMargin + t * ySlope, and the width and height cannot exceed
        the given maxima, then determine the maximum value of t that
        satisfies those requirements, as well as the resulting width
        and height. */
    Rescale(double xSlope, double xMargin, double maxWidth,
            double ySlope, double yMargin, double maxHeight) {
        double x = (maxWidth - xMargin) / xSlope;
        double y = (maxHeight - yMargin) / ySlope;
        t = Math.min(x,y);
        width = xMargin + t * xSlope;
        height = yMargin + t * ySlope;
    }

    @Override public String toString() {
        return "t = " + t + ", w = " + width + ", h = " + height;
    }

    /** Verbose substitute for (new Rescale()) that may be useful when
        debugging. */
    public static Rescale createRescale
        (double xSlope, double xMargin, double maxWidth,
         double ySlope, double yMargin, double maxHeight) {
        System.out.println
            ("x = " + xMargin + " + " + xSlope + " t <= " + maxWidth + ", "
             + "y = " + yMargin + " + " + ySlope + " t <= " + maxHeight);
        Rescale output = new Rescale
            (xSlope, xMargin, maxWidth,
             ySlope, yMargin, maxHeight);
        System.out.println(output);
        return output;
    }
}
