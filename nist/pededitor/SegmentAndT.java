/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

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
