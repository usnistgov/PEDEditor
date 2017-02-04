/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.function.ToDoubleFunction;

import org.codehaus.jackson.annotate.JsonIgnore;

/** Simple class to hold information about an axis/variable. */
abstract public class Axis implements Comparable<Axis>, ToDoubleFunction<Point2D> {

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

        @param py the y value in principal coordinates

        @return the value of this variable corresponding to the given
        location in principal coordinates. */
    abstract public double applyAsDouble(double px, double py);

    /** Convenience variation of value(double, double). */
    @Override public double applyAsDouble(Point2D p) {
        return applyAsDouble(p.getX(), p.getY());
    }

    /** Same as value(), but return as a string formatted
        appropriately. */
    public String applyAsString(double px, double py) {
        return format.format(applyAsDouble(px, py));
    }

    /** Convenience variation of valueAsString(double, double). */
    public String applyAsString(Point2D p) {
        return format.format(applyAsDouble(p.getX(), p.getY()));
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "['" + name + "', fmt: "
            + format + "]";
    }

    @JsonIgnore public boolean isPercentage() {
        return applyAsString(0.5, 0.5).indexOf('%') >= 0;
    }

    @Override public int compareTo(Axis other) {
        return ((String) name).compareTo((String) other.name);
    }
}
