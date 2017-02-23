/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** Undoable that holds information about moving a vertex. */
public class MoveVertex implements Undoable {
    Point2D src;
    Point2D dest;
    DecorationHandle h;

    public MoveVertex(DecorationHandle h, Point2D dest) {
        this.h = h;
        this.src = h.getLocation();
        this.dest = new Point2D.Double(dest.getX(), dest.getY());
    }

    @Override public void undo() {
        move(src);
    }

    @Override public void execute() {
        move(dest);
    }

    void move(Point2D where) {
        h.move(where);
    }
}
