package gov.nist.pededitor;

import java.awt.geom.*;

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
}
