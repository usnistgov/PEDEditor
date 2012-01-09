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
        @Type(value=TriangleTransform.class, name = "TriangleTransform"),
        @Type(value=QuadToRect.class, name = "QuadToRect"),
        @Type(value=QuadToQuad.class, name = "QuadToQuad") })
interface PolygonTransform extends Transform2D {
        
    /** The polygon's input vertices. */
    @JsonProperty("input") Point2D.Double[] getInputVertices();
    /** The polygon's output vertices. */
    @JsonProperty("output") Point2D.Double[] getOutputVertices();
    Rectangle2D.Double inputBounds();
    Rectangle2D.Double outputBounds();
    PolygonTransform clone();
}
