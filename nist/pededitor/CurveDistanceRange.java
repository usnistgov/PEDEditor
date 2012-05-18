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

    public CurveDistanceRange(double t, Point2D point, double distance,
                              double minDistance) {
        super(t, point, distance);
        this.minDistance = minDistance;
    }

    public String toString() {
        if (minDistance == distance)  {
            return getClass().getSimpleName() + "[t = " + t + ", p = " + point + ", "
                + " d = " + distance + "]";
        } else {
            return getClass().getSimpleName() + "[t = " + t + ", p = " + point + ", "
                + " d in [" + minDistance + ", " + distance + "]]";
        }
    }
}
