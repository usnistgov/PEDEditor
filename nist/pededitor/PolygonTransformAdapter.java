package gov.nist.pededitor;

import java.awt.geom.*;

/** Simple abstract class that partly implements the PolygonTransform
    interface. */
abstract public class PolygonTransformAdapter
    extends Transform2DAdapter
    implements PolygonTransform {
    abstract public Point2D.Double transform(double x, double y)
        throws UnsolvableException;
    abstract public Transform2D createInverse()
        throws NoninvertibleTransformException;
    abstract public void preConcatenate(Transform2D other);
    abstract public void concatenate(Transform2D other);

    public Rectangle2D.Double inputBounds() {
        return Duh.bounds(inputVertices());
    }
    public Rectangle2D.Double outputBounds() {
        return Duh.bounds(outputVertices());
    }

    /** Verify the consistency of this transform's internal state;
        throw an IllegalStateException on failure. */
    public void check() {
        check(this);
    }

    /** Verify the consistency of xform's internal state; throw an
        IllegalStateException on failure. */
    static public void check(PolygonTransform xform) {
        try {
            Point2D.Double[] ivs = xform.inputVertices();
            Point2D.Double[] ovs = xform.outputVertices();

            int cnt = ivs.length;
            if (cnt != ovs.length) {
                throw new IllegalStateException
                    (xform + ": number of input vertices (" + ivs.length
                     + ") does not equal number of output vertices ("
                     + ovs.length + ")");
            }
    
            for (int i = 0; i < cnt; ++i) {
                Point2D.Double iv = ivs[i];
                Point2D.Double ov = xform.transform(iv);
                Point2D.Double ov2 = ovs[i];
                if (!nearlyEqual(ov, ov2)) {
                    throw new IllegalStateException
                        ("In " + xform + ":\n" +
                         "Vertex " + i + " consistency check failure: " + iv + " => " + ov +
                         " != " + ov2);
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

    public String toString() {
        return toString(this);
    }

    public static String toString(PolygonTransform xform) {
        Point2D.Double[] ivs = xform.inputVertices();
        Point2D.Double[] ovs = xform.outputVertices();
        StringBuilder out = new StringBuilder(xform.getClass().getCanonicalName());
        for (int i = 0; i < ivs.length; ++i) {
            out.append(Duh.toString(ivs[i]) + ":" + Duh.toString(ovs[i]));
            if (i + 1 < ivs.length) {
                out.append(", ");
            }
        }
        out.append(")");
        return out.toString();
    }

}
