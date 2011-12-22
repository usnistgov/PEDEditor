package gov.nist.pededitor;

import java.awt.geom.*;

/** Abstract class containing elements common to RectToQuad and
    QuadToRect. */
abstract public class RectToQuadCommon
    extends PolygonTransformAdapter
    implements QuadrilateralTransform {
    abstract public Transform2D createInverse();
    abstract public void preConcatenate(Transform2D other);
    abstract public void concatenate(Transform2D other);
    abstract public Transform2D squareToDomain();
    abstract public RectToQuadCommon clone();

    public Point2D.Double transform(double x, double y)
        throws UnsolvableException {
        return xform.transform(x,y);
    }

    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff, int numPts)
        throws UnsolvableException {
        xform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /** X offset of rectangle */
    protected double x = 0;
    /** Y offset of rectangle */
    protected double y = 0;
    /** Width of rectangle */
    protected double w = 1;
    /** Height of rectangle */
    protected double h = 1;

    protected AffineXYCommon xform = null;

    /** X offset of rectangle */
    public double getX() { return x; }
    /** Y offset of rectangle */
    public double getY() { return y; }
    /** Width of rectangle */
    public double getW() { return w; }
    /** Height of rectangle */
    public double getH() { return h; }

    // These attributes define the output (for RectToQuad) or input (for
    // QuadToRect) rectangle. llx is the x value of the "lower left"
    // vertex, ury is the y value of the "upper right" vertex (whichever
    // vertex is diagonal from the "lower left" vertex), and so on)

    protected double llx, lly, ulx, uly, urx, ury, lrx, lry;

    public double getLlx() { return llx; }
    public double getLly() { return lly; }
    public double getUlx() { return ulx; }
    public double getUly() { return uly; }
    public double getUrx() { return urx; }
    public double getUry() { return ury; }
    public double getLrx() { return lrx; }
    public double getLry() { return lry; }

    public void setLlx(double v) { llx = v; update(); }
    public void setLly(double v) { lly = v; update(); }
    public void setUlx(double v) { ulx = v; update(); }
    public void setUly(double v) { uly = v; update(); }
    public void setUrx(double v) { urx = v; update(); }
    public void setUry(double v) { ury = v; update(); }
    public void setLrx(double v) { lrx = v; update(); }
    public void setLry(double v) { lry = v; update(); }
    public void setW(double v) { w = v; update(); }
    public void setH(double v) { h = v; update(); }
    public void setHeight(double v) { setH(v); }
    public void setWidth(double v) { setW(v); }
    public void setX(double v) { x = v; update(); }
    public void setY(double v) { y = v; update(); }


    public void setRectangle(Rectangle2D rect) {
        x = rect.getX();
        y = rect.getY();
        w = rect.getWidth();
        h = rect.getHeight();
        update();
    }


    Rectangle2D.Double getRectangle() {
        return new Rectangle2D.Double(x, y, w, h);
    }


    public void setVertices(Point2D.Double[] vertices) {
        llx = vertices[0].x;
        lly = vertices[0].y;
        ulx = vertices[1].x;
        uly = vertices[1].y;
        urx = vertices[2].x;
        ury = vertices[2].y;
        lrx = vertices[3].x;
        lry = vertices[3].y;
        update();
    }


    protected void copyFieldsFrom(RectToQuadCommon src) {
        llx = src.llx;
        lly = src.lly;
        ulx = src.ulx;
        uly = src.uly;
        urx = src.urx;
        ury = src.ury;
        lrx = src.lrx;
        lry = src.lry;
        x = src.x;
        y = src.y;
        w = src.w;
        h = src.h;
        update();
    }


    /** @return the four vertices of the quadrilateral (LL, UL, UR,
     * LR). */
    protected Point2D.Double[] quadVertices() {
        Point2D.Double[] output =
            {new Point2D.Double(llx,lly),
             new Point2D.Double(ulx,uly),
             new Point2D.Double(urx,ury),
             new Point2D.Double(lrx,lry)};
        return output;
    }


    /** @return the four vertices of the rectangle (LL, UL, UR,
     * LR). */
    protected Point2D.Double[] rectVertices() {
        Point2D.Double[] output =
            {new Point2D.Double(x,y),
             new Point2D.Double(x,y+h),
             new Point2D.Double(x+w,y+h),
             new Point2D.Double(x+w,y)};
        return output;
    }


    public String toString(boolean verbose) {
        if (verbose) {
            return toString() + " =\n" + xform.toString();
        } else {
            return toString();
        }
    }


    /** Update xform's parameters to reflect changes made here. */
    void update() {

        // The transformation equals

        // LL(1 - (x - @x)/@w)(1 - (y - @y)/@h) +
        // UL(1 - (x - @x)/@w)(y - @y)/@h +
        // LR(1 - (y - @y)/@h)(x - @x)/@w
        // UR((x - @x)/@w)(y - @y)/@h +
 
        // Let XW = 1 + @x/@w
        // Let YH = 1 + @y/@h
        // 
        // | -  | 1          | x      | y      | xy    |
        // | LL | XW YH      | - YH/w | - XW/h | 1/wh  |
        // | UL | XW (-@y/h) | @y/hw  | XW/h   | -1/wh |
        // | LR | YH (-@x/w) | YH/w   | @x/hw  | -1/wh |
        // | UR | (@x@y/wh)  | -@y/wh | -@x/wh | 1/wh  |

        double xw = 1 + x/w;
        double yh = 1 + y/h;

        double xk = llx*xw*yh - ulx*xw*y/h - lrx*yh*x/w + urx*x*y/w/h;
        double xkx = -llx*yh/w + ulx*y/w/h + lrx*yh/w - urx*y/w/h;
        double xky = -llx*xw/h + lrx*x/w/h + ulx*xw/h - urx*x/w/h;
        double xkxy = (llx+urx-ulx-lrx)/w/h;

        double yk = lly*xw*yh - uly*xw*y/h - lry*yh*x/w + ury*x*y/w/h;
        double ykx = -lly*yh/w + uly*y/w/h + lry*yh/w - ury*y/w/h;
        double yky = -lly*xw/h + lry*x/w/h + uly*xw/h - ury*x/w/h;
        double ykxy = (lly+ury-uly-lry)/w/h;

        xform.set(xk, xkx, xky, xkxy, yk, ykx, yky, ykxy);
    }

    /** Apply the given transformation to the quadrilateral's vertices,
        with the effect of changing the transformation that this object
        represents. */
    public void transformQuad(Transform2D other) {
        try {
            Point2D.Double[] points = quadVertices();
            for (int i = 0; i < points.length; ++i) {
                points[i] = other.transform(points[i]);
            }
            setVertices(points);
        } catch (UnsolvableException e) {
            throw new RuntimeException(e);
        }
    }

    /** Apply the given transformation to the rectangle, with the
        effect of changing the transformation that this object
        represents. If the rectangle is no longer just a rectangle
        after transformation, though, then that is not supported. */
    public void transformRect(Transform2D other) {
        if (other instanceof Affine) {
            Affine oa = (Affine) other;
            if (oa.getShearX() == 0 && oa.getShearY() == 0) {
                double xs = oa.getScaleX();
                double ys = oa.getScaleY();
                double tx = oa.getTranslateX();
                double ty = oa.getTranslateY();
                setRectangle(new Rectangle2D.Double(x * xs + tx, y * ys + ty,
                                                        w * xs, h * ys));
                return;
            }
        }
        throw new IllegalArgumentException
            ("implemented only for Affine transformations\n"
             + "with no shear, not for\n"
             + other);
    }
}
