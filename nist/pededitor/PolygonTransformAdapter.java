/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;

/** Simple abstract class that partly implements the PolygonTransform
    interface. */
abstract public class PolygonTransformAdapter
    implements PolygonTransform {
    @Override abstract public PolygonTransformAdapter clone();

    @Override public String toString() {
        return toString(this);
    }

    public static String toString(PolygonTransform xform) {
        Point2D.Double[] ivs = xform.getInputVertices();
        Point2D.Double[] ovs = xform.getOutputVertices();
        StringBuilder res = new StringBuilder(xform.getClass().getCanonicalName());
        res.append(")");
        for (int i = 0; i < ivs.length; ++i) {
            res.append(Geom.toString(ivs[i]) + ":" + Geom.toString(ovs[i]));
            if (i + 1 < ivs.length) {
                res.append(", ");
            }
        }
        res.append(")");
        return res.toString();
    }

}
