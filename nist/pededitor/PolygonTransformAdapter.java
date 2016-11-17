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
        StringBuilder out = new StringBuilder(xform.getClass().getCanonicalName());
        for (int i = 0; i < ivs.length; ++i) {
            out.append(Geom.toString(ivs[i]) + ":" + Geom.toString(ovs[i]));
            if (i + 1 < ivs.length) {
                out.append(", ");
            }
        }
        out.append(")");
        return out.toString();
    }

}
