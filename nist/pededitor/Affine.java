package gov.nist.pededitor;

import java.awt.geom.*;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/** Just a Transform2D-implementing wrapper around
 * awt.geom.AffineTransform. */
@JsonTypeInfo(
              use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "transform")
@JsonSubTypes({
        @Type(value=TriangleTransform.class, name = "TriangleTransform"),
        @Type(value=Affine.class, name = "Affine") })
@JsonIgnoreProperties
    ({"scaleX", "scaleY", "shearX", "shearY", "translateX", "translateY",
      "identity", "determinant", "type" })
public class Affine extends AffineTransform implements Transform2D {

    private static final long serialVersionUID = -867608180933463982L;

    /** Identical to superclass constructor. */
    public Affine() {
        super();
    }

    /** Identical to superclass constructor. */
    public Affine(AffineTransform Tx) {
        super(Tx);
    }

    /** Identical to superclass constructor. */
    public Affine(@JsonProperty("flatMatrix") double[] flatmatrix) {
        super(flatmatrix);
    }

    /** Identical to superclass constructor. */
    public Affine(double m00, double m10, double m01, double m11,
                  double m02, double m12) {
        super(m00,  m10, m01, m11, m02, m12);
    }

    public Point2D.Double transform(double x, double y) {
        Point2D.Double point = new Point2D.Double(x,y);
        transform(point, point);
        return point;
    }

    public Point2D.Double transform(Point2D.Double p) {
        return transform(p.x, p.y);
    }

    public Point2D.Double transform(Point2D p) {
        return transform(p.getX(), p.getY());
    }

    public Affine createInverse() throws NoninvertibleTransformException {
        return new Affine(super.createInverse());
    }

    public void preConcatenate(Transform2D other) {
        AffineTransform at = (AffineTransform) other;
        super.preConcatenate(at);
    }

    @JsonProperty("flatMatrix") double[] getFlatMatrix() {
        return new double[] { getScaleX(), getShearY(),
                              getShearX(), getScaleY(),
                              getTranslateX(), getTranslateY() };
    }

    /** Method defined just to avoid "method is ambiguous" error. */
    public void preConcatenate(Affine other) {
        super.preConcatenate((AffineTransform) other);
    }

    /** Method defined just to avoid "method is ambiguous" error. */
    public void concatenate(Transform2D other) {
        AffineTransform at = (AffineTransform) other;
        super.concatenate(at);
    }

    public void concatenate(Affine other) {
        super.concatenate((AffineTransform) other);
    }

    public static Affine getScaleInstance(double sx, double sy) {
        return new Affine(AffineTransform.getScaleInstance(sx, sy));
    }

    /** This transformation never throws an UnsolvableException. */
    public boolean transformNeverThrows() {
        return true;
    }

    /** This transformation never throws an UnsolvableException. */
    @Override
    public boolean isAffine() {
        return true;
    }
    
    public Affine clone() {
    	return new Affine(this); 
    }

    @Override
    public String toString() {
        return "Affine(" + super.toString() + ")";
    }
}
