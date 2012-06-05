package gov.nist.pededitor;

import java.awt.geom.*;
import java.text.*;

/** Simple class to hold information about an axis/variable. */
abstract public class Axis {

    public Axis() {
        format = NumberFormat.getInstance();
    }

    public Axis(NumberFormat format) {
        this.format = format;
    }

    /** Format to use for values of this kind. */
    public NumberFormat format;

    /** Name of this axis/variable -- normally a string, but not
        always. */
    public Object name = null;

    /** @param px the x value in principal coordinates

        @param py the y value in principal coordiantes

        @return the value of this variable corresponding to the given
        location in principal coordinates. */
    abstract public double value(double px, double py);

    /** Convenience variation of value(double, double). */
    public double value(Point2D p) {
        return value(p.getX(), p.getY());
    }

    /** Same as value(), but return as a string formatted
        appropriately. */
    public String valueAsString(double px, double py) {
        return format.format(value(px, py));
    }
}
