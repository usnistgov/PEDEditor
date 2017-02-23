/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.ArrayList;
import java.util.Arrays;

/* A list of Undoables that in aggregate itself acts as an Undoable. */

public class UndoableList implements Undoable {
    ArrayList<Undoable> commands = new ArrayList<>();

    UndoableList(Undoable... steps) {
        commands.addAll(Arrays.asList(steps));
    }

    public void add(Undoable command) {
        commands.add(command);
    }
    
    @Override public void execute() {
        for (Undoable command: commands) {
            command.execute();
        }
    }
    
    @Override public void undo() {
        for (int i = commands.size() - 1; i>=0; --i) {
            commands.get(i).undo();
        }
    }
}
