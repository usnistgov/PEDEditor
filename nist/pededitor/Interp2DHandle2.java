/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Class for a handle (control point) of a PointsDecoration with extra information about the specific point chosen. */
public class Interp2DHandle2 extends Interp2DHandle {
    public double exactT;

    // It'd be better working exclusively in page space, but sometimes
    // we're stuck storing a coordinate in principal space that's just
    // the transform of a coordinate in page space. It's not on the
    // curve if parameterized in principal space.
    Point2D.Double p = null;

    /** If true, a control point added here should be added at index
        instead of after index, for example if exactT is between
        points index and (index-1). */
    public boolean beforeThis = false;

    public Interp2DHandle2(Interp2DHandle h) {
        super(h.getDecoration(), h.index);
    }

    public Interp2DHandle2(Interp2DDecoration d, ParamPointInfo info,
            Point2D p) {
        this(d, info.index, info.t, info.beforeIndex);
        this.p = new Point2D.Double(p.getX(), p.getY());
    }

    public Interp2DHandle2(Interp2DDecoration d, int index, double exactT,
            boolean beforeThis) {
        super(d, index);
        this.exactT = exactT;
        this.beforeThis = beforeThis;
    }

    @Override public Interp2DHandle2 clone() {
        Interp2DHandle2 res = new Interp2DHandle2(this);
        res.exactT = exactT;
        res.beforeThis = beforeThis;
        if (p != null)
            res.p = new Point2D.Double(p.x, p.y);
        return res;
    }

    @Override public boolean equals(Object other0) {
        if (!super.equals(other0))
            return false;
        Interp2DHandle2 other = (Interp2DHandle2) other0;
        return super.equals(other) && exactT == other.exactT
            && beforeThis == other.beforeThis;
    }

    @Override public double getT() {
        return exactT;
    }

    /** Return a handle for the index instead of this particular point. */
    public Interp2DHandle indexHandle() {
        return getDecoration().createHandle(index);
    }

    @Override public Interp2DHandle moveHandle(double dx, double dy) {
        return indexHandle().moveHandle(dx, dy);
    }

    @Override public Point2D.Double getLocation() {
        return (p == null) ? null :
            new Point2D.Double(p.x, p.y);
    }

    @Override public Interp2DHandle2 copyFor(Decoration d) {
        throw new UnsupportedOperationException();
    }
}
