/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public class DoThenUndo implements AutoCloseable {
    public Undoable undoable;
    
    public DoThenUndo(Undoable undoable) {
        this.undoable = undoable;
        undoable.execute();
    }

    @Override public void close() {
        undoable.undo();
    }
}
