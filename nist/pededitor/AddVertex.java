/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Undoable that holds information about adding a vertex. */
public class AddVertex implements Undoable {
    Point2D.Double p;
    Interp2DDecoration d;
    int index;
    boolean smoothed;
    
    public AddVertex(Interp2DDecoration d, int index,
            Point2D p, boolean smoothed) {
        this.d =d;
        this.index = index;
        this.p = new Point2D.Double(p.getX(), p.getY());
        this.smoothed = smoothed;
    }

    @Override public void undo() {
        d.getCurve().remove(index);
    }

    @Override public void execute() {
        Interp2D curve = d.getCurve();
        curve.add(index, p);
        if (curve instanceof Smoothable)
            ((Smoothable) curve).setSmoothed(index, smoothed);
    }
}

/** Return an Undoable that "does what you want" when you try to
     * add a point to dec. See addCommand(Point2D).
    Undoable addCommand(Interp2DDecoration dec, Point2D prin) {
        if (isDuplicate(prin) || principalToStandardPage == null) {
            return new NoOp(); // Adding the same point twice causes problems.
        }
        Interp2DHandle vhand = getInterp2DHandle();
        Interp2D curve = dec.getCurve();
        if (curve.size() == curve.maxSize()) {
            return new MoveVertex(vhand, prin);
        } else {
            // Add a new point to the currently selected curve.
            return new AddVertex(curve, vertexInsertionIndex(), prin);
        }
    }
 */
