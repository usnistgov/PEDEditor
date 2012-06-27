package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.codehaus.jackson.annotate.JsonProperty;

import Jama.Matrix;

/** Transform a triangle into any other triangle. All that is needed
    is an affine transformation. */
public class TriangleTransform extends AffinePolygonTransform {

    private static final long serialVersionUID = 1768608728396588446L;

    public static final double UNIT_TRIANGLE_HEIGHT = Math.sqrt(3.0) / 2.0;

    public static final Point2D.Double[] equilateralTriangleVertices() {
        return new Point2D.Double[]
            { new Point2D.Double(0.,0.),
              new Point2D.Double(0.5, UNIT_TRIANGLE_HEIGHT),
              new Point2D.Double(1.0,0.) };
    }

    /** Default: transform from an equilateral triangle with base from
        (0,0) to (1,0) into the same triangle. */
    Point2D.Double[] inputVerts = equilateralTriangleVertices();
    Point2D.Double[] outputVerts = equilateralTriangleVertices();

    public TriangleTransform(TriangleTransform other) {
        inputVerts = Duh.deepCopy(other.inputVerts);
        outputVerts = Duh.deepCopy(other.outputVerts);
        update();
    }

    /** @return a new TriangleTransform that represents the affine
        transform that transforms the three input vertices inpts[] into
        the three output vertices outpts[] */
    public TriangleTransform(@JsonProperty("input") Point2D.Double[] inpts,
                             @JsonProperty("output") Point2D.Double[] outpts) {
        setInputVertices(inpts);
        setOutputVertices(outpts);
    }

    @Override
	public TriangleTransform clone() {
        return new TriangleTransform(this);
    }

    /** Update the underlying affine transformation after changes to
        inputVerts or outputVerts to maintain the requirement that
        transform(inputVerts) always equals outputVerts. */
    protected void update() {
        Matrix ins = new Matrix(3,3,1.0);
        Matrix outs = new Matrix(2,3);
        for (int c = 0; c < 3; ++c) {
            Point2D.Double in = inputVerts[c];
            Point2D.Double out = outputVerts[c];
            ins.set(0,c,in.x);
            ins.set(1,c,in.y);
            outs.set(0,c,out.x);
            outs.set(1,c,out.y);
        }
        Matrix m = ins.solveTranspose(outs);
        setTransform(m.get(0,0), m.get(0,1),
                     m.get(1,0), m.get(1,1),
                     m.get(2,0), m.get(2,1));
    }

    @Override
	public TriangleTransform createInverse() {
        return new TriangleTransform(outputVerts, inputVerts);
    }

    @Override
	public Point2D.Double[] getInputVertices() {
        return Duh.deepCopy(inputVerts);
    }

    @Override
	public Point2D.Double[] getOutputVertices() {
        return Duh.deepCopy(outputVerts);
    }

    public void setInputVertices(Point2D.Double[] inputVertices) {
        if (inputVertices.length != 3) {
            throw new IllegalArgumentException("inputVertices.length " + inputVertices.length + " != 3");
        }
        inputVerts = Duh.deepCopy(inputVertices);
        update();
    }

    public void setOutputVertices(Point2D.Double[] outputVertices) {
        if (outputVertices.length != 3) {
            throw new IllegalArgumentException("outputVertices.length " + outputVertices.length + " != 3");
        }
        outputVerts = Duh.deepCopy(outputVertices);
        update();
    }

    private void concatSub(Transform2D other, Point2D.Double[] points) {
        for (Point2D.Double point : points) {
            try {
                Point2D.Double newpt = other.transform(point);
                point.setLocation(newpt.x, newpt.y);
            } catch (UnsolvableException e) {
                throw new RuntimeException("Could not compute " + other +
                                           ".transform(" + point + ")");
            }
        }
        update();
    }

    private void concatSub(AffineTransform other, Point2D.Double[] points) {
        Point2D.Double xpt = new Point2D.Double();
        for (Point2D.Double point : points) {
            other.transform(point, xpt);
            point.setLocation(xpt.x, xpt.y);
        }
        update();
    }

    @Override
	public void preConcatenate(Transform2D other) {
        concatSub(other, outputVerts);
    }

    @Override
	public void concatenate(Transform2D other) {
        try {
            concatSub(other.createInverse(), inputVerts);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
	public void preConcatenate(AffineTransform other) {
        concatSub(other, outputVerts);
    }

    @Override
	public void concatenate(AffineTransform other) {
        try {
            concatSub(other.createInverse(), inputVerts);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
	public Rectangle2D.Double inputBounds() {
        return Duh.bounds(inputVerts);
    }

    @Override
	public Rectangle2D.Double outputBounds() {
        return Duh.bounds(outputVerts);
    }

    @Override
	public String toString() {
        return PolygonTransformAdapter.toString(this) + "(" + super.toString() + ")";
    }

    public void check() {
        PolygonTransformAdapter.check(this);
    }
}
