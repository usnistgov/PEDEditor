/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;

public class SingleInstanceBasicEditor extends BasicEditor {

    @Override public SingleInstanceBasicEditor createNew() {
        return new SingleInstanceBasicEditor();
    }

    @Override public void lastWindowClosed() {
        if (exitIfLastWindowCloses) {
            super.lastWindowClosed();
        }
    }

    /** Launch the application. */
    public static void main(String[] args) {
        main(new BasicEditorCreator(), args);
    }

    public static void main(BasicEditorCreator ec, String[] args) {
        try {
            SingleInstanceService sis = (SingleInstanceService)
                ServiceManager.lookup("javax.jnlp.SingleInstanceService");
            BasicEditorSingleInstanceListener listen
                = new BasicEditorSingleInstanceListener(ec);
            // UNDO Temporarily disable shutdown because of bug 8189783
            listen.delay = 1000 * 1000 * 3600;
            sis.addSingleInstanceListener(listen);
            listen.newActivation(args);
        } catch(UnavailableServiceException x) {
            // I guess we're not running via JWS...
            BasicEditor.main(ec, args);
        }
    }

    @Override public void initializeGUI() {
        super.initializeGUI();
        editFrame.toFront();
    }
}
