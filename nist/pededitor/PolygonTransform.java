/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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

    static boolean nearlyEqual(double v1, double len1, double v2, double len2) {
        double maxError = Math.min(Math.abs(len1) + Math.abs(v1),
                Math.abs(len2) + Math.abs(v2)) * 0.5e-8;
        return Math.abs(len1 - len2) <= maxError && Math.abs(v1 - v2) <= maxError;
    }

    static double relativeError100(double v1, double len1,
            double v2, double len2) {
        return Math.min(Math.max(Math.abs(v1 + len1), Math.abs(v1)),
                Math.max(Math.abs(v2 + len2), Math.abs(v2)));
    }

    static Point2D.Double errorBounds(Rectangle2D r1, Rectangle2D r2,
            double relativeError) {
        double ex = relativeError100(r1.getX(), r1.getWidth(),
                r2.getX(), r2.getWidth());
        double ey = relativeError100(r1.getY(), r1.getHeight(),
                r2.getY(), r2.getHeight());
        return new Point2D.Double(ex * relativeError, ey * relativeError);
    }

    static boolean nearlyEqual(Point2D.Double[] v1, Point2D.Double[] v2,
                                       double relativeError) {
        if (v1.length != v2.length)
            return false;
        Point2D.Double errorBounds = errorBounds(
                Geom.bounds(v1), Geom.bounds(v2), relativeError);
        for (int i = 0; i < v1.length; ++i) {
            if (Math.abs(v1[i].x - v2[i].x) > errorBounds.x ||
                    Math.abs(v1[i].y - v2[i].y) > errorBounds.y)
                return false;
        }
        return true;
    }

    /** Return false unless the distance in both the X and Y
        dimensions between every pair of corresponding vertices is
        always no more than relativeError times the maximum coordinate
        value in the corresponding dimension. */
    default boolean nearlyEquals(Object other0, double relativeError) {
        if (this == other0) return true;
        if (other0 == null || getClass() != other0.getClass()) return false;
        PolygonTransform other = (PolygonTransform) other0;
        return nearlyEqual(getInputVertices(), other.getInputVertices(), relativeError)
            && nearlyEqual(getOutputVertices(), other.getOutputVertices(), relativeError);
    }
}
