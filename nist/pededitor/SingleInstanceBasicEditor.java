/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JMenuItem;

public class SingleInstanceBasicEditor extends BasicEditor {
    static ArrayList<BasicEditor> openEditors = new ArrayList<>();

    public SingleInstanceBasicEditor() {
        setExitOnClose(false);
        init();
        openEditors.add(this);
    }

    void closeAll() {
        // Duplicate openEditors so we don't end up iterating through a
        // list that is simultaneously being modified.
        for (BasicEditor e: new ArrayList<>(openEditors)) {
            e.close();
        }
    }

    @Override public void close() {
        super.close();
        openEditors.remove(this);
    }

    @SuppressWarnings("serial") private void init() {
        JMenuItem mnExitAll = new JMenuItem
            (new Action("Exit all") {
                    @Override public void actionPerformed(ActionEvent e) {
                        closeAll();
                    }
                });
        mnExitAll.setMnemonic(KeyEvent.VK_A);
        editFrame.mnFile.add(mnExitAll);
    }

    static class Runnable extends ArgsRunnable {
        BasicEditorCreator ec;
        Runnable(BasicEditorCreator ec, String[] args) {
            super(args);
            this.ec = ec;
        }

        @Override public void run() {
            BasicEditor.main(ec, args);
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
            sis.addSingleInstanceListener(listen);
            listen.newActivation(args);
        } catch(UnavailableServiceException x) {
            // I guess we're not running via JWS...
            BasicEditor.main(ec, args);
        }
    }
}
