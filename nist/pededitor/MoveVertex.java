/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Undoable that holds information about moving a vertex. */
public class MoveVertex implements Undoable {
    double dx;
    double dy;
    DecorationHandle h;

    public MoveVertex(DecorationHandle h, double dx, double dy) {
        this.h = h;
        this.dx = dx;
        this.dy = dy;
    }

    @Override public void undo() {
        move(-dx, -dy);
    }

    @Override public void execute() {
        move(dx, dy);
    }

    void move(double dx, double dy) {
        h.moveHandle(dx, dy);
    }
}
