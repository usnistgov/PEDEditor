/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;

/** Simple object to store information about a single point on a
    parameterized curve and its distance from something. */
public class CurveDistanceRange extends CurveDistance {
    /** Guaranteed lower bound on a distance estimate. Contrast with
        the regular distance field, which is an upper bound (though it
        is presumably correct for the specific point stored in the
        point field). */
    double minDistance;

    /** Create a CurveDistanceRange from this CurveDistance, assuming
        minDistance = c.distance. */
    public CurveDistanceRange(CurveDistance c) {
        this(c, c.distance);
    }

    /** Create a CurveDistanceRange from this CurveDistance and the
        given minDistance value */
    public CurveDistanceRange(CurveDistance c, double minDistance) {
        super(c.t, c.point, c.distance);
        this.minDistance = minDistance;
    }

    public CurveDistanceRange(double t, Point2D point, double distance,
                              double minDistance) {
        super(t, point, distance);
        this.minDistance = minDistance;
    }

    /** Assuming both CurveDistanceRanges are accurate and the
        'distance' field contains a minimum known distance while the
        'minDistance' field contains a lower bound on the possible
        distances, merge them to create a result at least as accurate
        as either one. */

    public void add(CurveDistanceRange other) {
        if (other == null)
            return;
        if (other.distance < distance) {
            this.t = other.t;
            this.point = other.point;
            this.distance = other.distance;
        }
        minDistance = Math.max(minDistance, other.minDistance);
    }

    /** @return other if other is not null and its distance is less
        than this, or this otherwise. */
    public static CurveDistanceRange min(CurveDistanceRange c1, CurveDistanceRange c2) {
        return (c2 == null) ? c1
            : (c1 == null) ? c2
            : new CurveDistanceRange((c1.distance <= c2.distance) ? c1 : c2,
                                     Math.min(c1.minDistance, c2.minDistance));
    }

    @Override public String toString() {
        if (minDistance == distance)  {
            return getClass().getSimpleName() + "[t = " + t + ", p = " + point + ", "
                + " d = " + distance + "]";
        } else {
            return getClass().getSimpleName() + "[t = " + t + ", p = " + point + ", "
                + " d in [" + minDistance + ", " + distance + "]]";
        }
    }
}
