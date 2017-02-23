/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

import Jama.Matrix;

public class ArcInterp2D extends PointsInterp2D {
    protected boolean closed;
    protected boolean reversed = false;

    public ArcInterp2D(boolean closed) {
        setClosed(closed);
    }

    /** @return true if the control points are oriented clockwise
        around the ellipse, or false otherwise. Also arbitrarily true
        if there are fewer than three control points. If there are
        four or more control points with no consistent orientation,
        may return true or false.
    */
    boolean clockwiseControlPoints() {
        return clockwiseControlPoints(points);
    }
    
    static <T extends Point2D> boolean clockwiseControlPoints(List<T> points) {
        return (points.size() <= 2) ? true
            : (Geom.signedArea(points.get(1), points.get(0), points.get(2)) < 0);
    }

    @Override public void setClosed(boolean b) {
        super.setClosed(b);
        closed = b;
    }

    public ArcInterp2D() { }
    public <T extends Point2D> ArcInterp2D(List<T> points,
                                           boolean closed) {
        super(points);
        setClosed(closed);
    }

    @Override public RectangularShape getShape() {
        try {
            return getShape2();
        } catch (UnsolvableException e) {
            return null;
        }
    }

    @JsonIgnore
    public RectangularShape getShape2() throws UnsolvableException {
        return isClosed() ? ellipse(points) : arc(points);
    }

    @JsonIgnore @Override public boolean isClosed() {
        return closed;
    }

    @Override public int minSize() {
        return 1;
    }

    @Override public int maxSize() {
        return 4;
    }

    /** Return [c, cx, cy, cxx, cyy] for the ellipse equation
        c + cx x + cy y + cxx x^2 + cyy y^2 = 0 */
    static double[] quadraticCoefs(RectangularShape shape) {
        double rx = shape.getWidth() / 2;
        double ry = shape.getHeight() / 2;
        double centx = shape.getX() + rx;
        double centy = shape.getY() + ry;
        // (x/rx - centx/rx)^2 + (y/ry - centy/ry)^2 = 1

        // (1/rx)^2 x^2 - (2 centx / rx^2) x + centx^2/rx^2 +
        // (1/ry)^2 y^2 - (2 centy / ry^2) y + centy^2/ry^2 - 1 = 0

        double rxx = 1/(rx*rx);
        double ryy = 1/(ry*ry);
        
        return new double[]
            { centx * centx * rxx + centy * centy * ryy - 1,
              -2 * centx * rxx, -2 * centy * ryy, rxx, ryy };
    }

    static <T extends Point2D> Ellipse2D.Double ellipse(List<T> points)
        throws UnsolvableException {
        // A conic whose x^2 coefficient is nonzero (as is the case
        // for all arcs) can be expressed as

        // C0 + C1 x + C2 y + C3 y^2 + C4 xy + x^2

        // For now, I will assume C4 is always zero.

        int size = points.size();
        
        if (size == 1) {
            return new Ellipse2D.Double(points.get(0).getX(), points.get(0).getY(), 0, 0);
        } else if (size == 2) {
            return circle(points.get(0), points.get(1));
        }

        if (size < 2 || size > 4) {
            throw new IllegalArgumentException
                ("ArcInterp2D.ellipse() requires 2-4 points, not " 
                 + size + " points.");
        }

        Matrix rhs = new Matrix(size, 1);
        Matrix lhs = new Matrix(size, size);
        double cxy = 0;
        double cyy = 1; // If size == 3
        
        for (int i = 0; i < size; ++i) {
            Point2D p = points.get(i);
            double x = p.getX();
            double y = p.getY();
            double rh = -x*x; // right-hand side for this row.
            lhs.set(i, 0, 1.0);
            lhs.set(i, 1, x);
            lhs.set(i, 2, y);
            if (size > 3) {
                lhs.set(i, 3, y*y);
            } else {
                rh -= cyy * y*y;
            }
            if (size > 4) {
                lhs.set(i, 4, x*y);
            } else {
                cxy = 0;
            }
            rhs.set(i, 0, rh);
        }

        Matrix s = null;
        try {
            s = lhs.solve(rhs);
        } catch (RuntimeException x) {
            throw new UnsolvableException
                (points.toString() + " do not define an ellipse");
        }

        double c = s.get(0, 0);
        double cx = s.get(1, 0);
        double cy = s.get(2, 0);

        if (size >= 4)
            cyy = s.get(3,0);
        if (size >= 5)
            cxy = s.get(4,0);

        // (x - centx)^2 / rx^2 + (y - centy)^2 / ry^2 - 1 = 0

        // We're solving coefficients of

        // (x - centx)^2 + (y - centy)^2 (rx^2 / ry^2) - rx^2 = 0

        if (cxy != 0) {
            double disc = cxy * cxy - 4 * cyy;
            if (disc >= 0) {
                throw new UnsolvableException(
                        "These points define a parabola or hyperbola, "
                        + "not an ellipse");
            }

            throw new UnsupportedOperationException
                ("TODO Missing support for 5-point ellipses...");
        } else {
            // cxx = 1
            // cx = -2 x0
            // cyy = rx^2 / ry^2
            // cy = -2 y0 (rx^2/ry^2) = -2 y0 cyy
            // c = x0^2 + cyy y0^2 - rx^2
         double centx = -cx / 2;
            double centy = - cy / cyy / 2;
            double rx = Math.sqrt(centx * centx + cyy * centy * centy - c);
            double ry = rx / Math.sqrt(cyy);
            return new Ellipse2D.Double(centx - rx, centy - ry,
                                        rx * 2, ry * 2);
        }
    }

    /** Return a circle that passes through both points and has its
        center at the midpoint of p1p2. */
    static Ellipse2D.Double circle(Point2D p1, Point2D p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double r = Math.sqrt(dx*dx + dy*dy) / 2;
        double x = (p1.getX() + p2.getX()) / 2 - r;
        double y = (p1.getY() + p2.getY()) / 2 - r;
        return new Ellipse2D.Double(x, y, r*2, r*2);
    }

    static <T extends Point2D> Arc2D.Double arc(List<T> points) throws UnsolvableException {
        Ellipse2D.Double el = ellipse(points);
        Arc2D.Double res = new Arc2D.Double(el.getBounds2D(), 0, 0,
                                            Arc2D.OPEN);
        Point2D p1 = points.get(0);
        Point2D p2 = points.get(points.size()-1);

        if (clockwiseControlPoints(points)) {
            res.setAngles(p2, p1);
        } else {
            res.setAngles(p1, p2);
        }
        return res;
    }

    @Override public ArcInterp2D clone() {
        return new ArcInterp2D(points, isClosed());
    }

    @Override @JsonIgnore public ArcParam2D getParameterization() {
        if (param == null) {
            try {
                param = new ArcParam2D(this);
            } catch (UnsolvableException e) {
                return null;
            }
        }
        return (ArcParam2D) param;
    }

    @Override public int tToIndex(double t) {
        return getParameterization().tToIndex(t);
    }

    @Override public double indexToT(int index) {
        return getParameterization().indexToT(index);
    }

    @Override public ArcInterp2D createTransformed(AffineTransform xform) {
        return new ArcInterp2D(Arrays.asList(transformPoints(xform)),
                isClosed());
    }
    
    public static void main(String[] args) throws UnsolvableException {
        ellipse(Arrays.asList(new Point2D.Double[]
                { new Point2D.Double(7.0, 3.0), new Point2D.Double(11.0, 3.0), new Point2D.Double(9.0, 5.0) }));
    }
 }
