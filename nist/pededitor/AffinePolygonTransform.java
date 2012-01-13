package gov.nist.pededitor;

import java.awt.geom.*;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonIgnoreProperties
    ({"scaleX", "scaleY", "shearX", "shearY", "translateX", "translateY",
      "identity", "determinant", "type", "flatMatrix" })
public abstract class AffinePolygonTransform extends Affine
                                              implements PolygonTransform {
        
    /** The polygon's input vertices. */
    public abstract Point2D.Double[] getInputVertices();
    /** The polygon's output vertices. */
    public abstract Point2D.Double[] getOutputVertices();
    public abstract Rectangle2D.Double inputBounds();
    public abstract Rectangle2D.Double outputBounds();
    public abstract AffinePolygonTransform clone();
}
