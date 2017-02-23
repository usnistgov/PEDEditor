/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Undoable that does nothing. */
public class NoOp implements Undoable {
    @Override public void undo() {
    }

    @Override public void execute() {
    }
}
