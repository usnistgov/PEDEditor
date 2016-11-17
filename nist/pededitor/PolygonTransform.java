/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(
              use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "transform")
@JsonSubTypes({
        @Type(value=RectangleTransform.class, name = "RectangleTransform"),
        @Type(value=TriangleTransform.class, name = "TriangleTransform"),
        @Type(value=QuadToRect.class, name = "QuadToRect"),
        @Type(value=QuadToQuad.class, name = "QuadToQuad") })
interface PolygonTransform extends Transform2D {
        
    /** The polygon's input vertices. */
    @JsonProperty("input") Point2D.Double[] getInputVertices();
    /** The polygon's output vertices. */
    @JsonProperty("output") Point2D.Double[] getOutputVertices();

    default Rectangle2D.Double inputBounds() {
        return Geom.bounds(getInputVertices());
    }

    default Rectangle2D.Double outputBounds() {
        return Geom.bounds(getOutputVertices());
    }
        
    @Override PolygonTransform clone();
    
    /** Verify the internal state consistency. Throw
        IllegalStateException on failure. */
    default void check() {
        try {
            Point2D.Double[] ivs = getInputVertices();
            Point2D.Double[] ovs = getOutputVertices();

            int cnt = ivs.length;
            if (cnt != ovs.length) {
                throw new IllegalStateException
                    (this + ": number of input vertices (" + ivs.length
                            + ") does not equal number of output vertices ("
                            + ovs.length + ")");
            }
    
            for (int i = 0; i < cnt; ++i) {
                Point2D.Double iv = ivs[i];
                Point2D.Double ov = transform(iv);
                Point2D.Double ov2 = ovs[i];
                if (!nearlyEqual(ov, ov2)) {
                    throw new IllegalStateException
                        ("In " + this + ":\n"
                                + "Vertex " + i + " consistency check failure: " + iv
                                + " => " + ov + " != " + ov2);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** @return true if either the absolute or relative difference of
        a and b is small (<1e-6) */
    static boolean nearlyEqual(Point2D.Double a, Point2D.Double b) {
        return a.distanceSq(b) / (a.x * a.x + a.y * a.y + b.x * b.x + b.y * b.y + 1.0)
            < 1e-12;
    }
}
