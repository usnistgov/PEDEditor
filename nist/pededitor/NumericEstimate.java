package gov.nist.pededitor;

public class NumericEstimate extends Estimate implements Cloneable {

    public enum Status {
        OK /* Calculation succeeded */,
        TOO_SMALL_STEP_SIZE /* Loss of accuracy: step size became too
                             * small */,
        TOO_MANY_STEPS /* Loss of accuracy: maximum number of
                           * doublings of the step count reached
                           * before accuracy target achieved. */,
    };

    public NumericEstimate(double d) {
        super(d);
    }

    public NumericEstimate(NumericEstimate other) {
        super(other);
        status = other.status;
        sampleCnt = other.sampleCnt;
    }

    /** Return a bad estimate with unlimited error possible on both sides. */
    static NumericEstimate bad(double d) {
        NumericEstimate res = new NumericEstimate(d);
        res.lowerBound = Double.NEGATIVE_INFINITY;
        res.upperBound = Double.POSITIVE_INFINITY;
        res.status = Status.TOO_MANY_STEPS;
        return res;
    }

    public Status status = Status.OK;
    public int sampleCnt = 0;

    @Override public NumericEstimate clone() {
        return new NumericEstimate(this);
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
        res.append(" = " + status + ", " + sampleCnt + " pts.");
        res.append("]");
        return res.toString();
    }

    public void add(NumericEstimate other) {
        if (other == null)
            return;
        super.add(other);
        if (status == Status.OK
            || other.status == Status.TOO_SMALL_STEP_SIZE) {
            status = other.status;
        }
        sampleCnt += other.sampleCnt;
    }
};
