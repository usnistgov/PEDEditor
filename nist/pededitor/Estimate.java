/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public class Estimate implements Cloneable {
    double value;
    double lowerBound;
    double upperBound;
        
    public Estimate() {}

    /** Exact estimate. */
    public Estimate(double d) {
        setExactValue(d);
    }

    public Estimate(Estimate other) {
        copyFrom(other);
    }

    public void copyFrom(Estimate other) {
        this.value = other.value;
        this.lowerBound = other.lowerBound;
        this.upperBound = other.upperBound;
    }

    /** Return a bad estimate with unlimited error possible on both sides. */
    static Estimate bad(double d) {
        Estimate res = new Estimate(d);
        res.lowerBound = Double.NEGATIVE_INFINITY;
        res.upperBound = Double.POSITIVE_INFINITY;
		return res;
    }

    /** Set the best guess for the actual value. This does not set the
        lower or upper bounds. */
    public void setValue(double value) {
        this.value = value;
    }

    /** Set the best guess and lower and upper bounds to value. */
    public void setExactValue(double value) {
        lowerBound = upperBound = this.value = value;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = Double.isNaN(lowerBound)
            ? Double.NEGATIVE_INFINITY : lowerBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = Double.isNaN(upperBound)
            ? Double.POSITIVE_INFINITY : upperBound;
    }

    public boolean isExact() {
        return lowerBound == upperBound;
    }

    public double relativeError() {
        return (lowerBound == upperBound)
            ? 0
            : Math.signum(lowerBound) != Math.signum(upperBound)
            ? 1 :
            Math.abs((upperBound - lowerBound) / (upperBound + lowerBound));
    }

    /** Return true if the range of possible error is infinite. */
    boolean isBad() {
        return (lowerBound == Double.NEGATIVE_INFINITY)
            || (upperBound == Double.POSITIVE_INFINITY);
    }

    /** Return true if d is contained in the range of possible
        values. */
    public boolean contains(double d) {
        return lowerBound <= d && upperBound >= d;
    }

    /** Return upperBound minus lowerBound. */
    public double width() {
        return upperBound - lowerBound;
    }

    public double getValue() {
        return value;
    }

    public void add(Estimate other) {
        if (other == null)
            return;
        value += other.value;
        lowerBound += other.lowerBound;
        upperBound += other.upperBound;
    }

    public void times(double d) {
        value *= d;
        lowerBound *= d;
        upperBound *= d;
        if (d < 0) {
            double tmp = lowerBound;
            lowerBound = upperBound;
            upperBound = tmp;
        }
    }

    @Override public Estimate clone() {
        return new Estimate(this);
    }

    @Override public String toString() {
        StringBuilder res = new StringBuilder(getClass().getSimpleName() + "[");
        if (isExact()) {
            res.append(value);
        } else {
            res.append(lowerBound == Double.NEGATIVE_INFINITY
                       ? "-inf" : Double.toString(lowerBound));
            res.append(", " + value + ", ");
            res.append(upperBound == Double.POSITIVE_INFINITY
                       ? "+inf" : Double.toString(upperBound));
        }
        res.append("]");
        return res.toString();
    }
}
