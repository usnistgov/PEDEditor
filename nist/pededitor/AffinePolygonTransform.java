/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.Arrays;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties
    ({"scaleX", "scaleY", "shearX", "shearY", "translateX", "translateY",
      "identity", "determinant", "type", "flatMatrix" })
public abstract class AffinePolygonTransform extends Affine
                                              implements PolygonTransform {
    private static final long serialVersionUID = 7329000844756906085L;

    /** The polygon's input vertices. */
    @Override public abstract Point2D.Double[] getInputVertices();
    /** The polygon's output vertices. */
    @Override public abstract Point2D.Double[] getOutputVertices();
    @Override public abstract Rectangle2D.Double inputBounds();
    @Override public abstract Rectangle2D.Double outputBounds();
    @Override public abstract AffinePolygonTransform clone();

    @Override public boolean equals(Object other0) {
        if (this == other0) return true;
        if (other0 == null || getClass() != other0.getClass()) return false;
        AffinePolygonTransform other = (AffinePolygonTransform) other0;

        return Arrays.equals(getInputVertices(), other.getInputVertices())
            && Arrays.equals(getOutputVertices(), other.getOutputVertices());
    }
}
