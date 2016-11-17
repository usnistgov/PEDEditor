/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.TimerTask;

/** Task that gets called regularly to see if any BasicEditor objects
    are open. A slightly better solution would be to check after a
    given delay since the last non BasicEditor closed, but that takes a
    little more code. Instead, just check every 30 minutes whether
    any Editors are open. */
class BasicEditorShutdownChecker extends TimerTask {
    @Override public void run() {
        int cnt = BasicEditor.getOpenEditorCnt();
        if (cnt == 0) {
            System.exit(0);
        }
    }
}
