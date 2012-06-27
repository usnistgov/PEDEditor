package gov.nist.pededitor;

public class SegmentAndT {
    SegmentAndT(int segment, double t) {
        this.segment = segment;
        this.t = t;
    }

    @Override
	public String toString() {
        return "SegmentAndT[" + segment + ", " + t + "]";
    }

    int segment;
    double t;
}
