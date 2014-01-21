/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.EventQueue;
import java.awt.Graphics;

import javax.swing.JDialog;
import javax.swing.JLabel;


/** Defer run until the wait dialog has been painted. */
class WaitDialog extends JDialog {
    private static final long serialVersionUID = 8897149707323096462L;

    Runnable run;

    public WaitDialog(Runnable run, String loadMessage) {
        setAlwaysOnTop(true);
        getContentPane().add(new JLabel(loadMessage));
        this.run = run;
    }

    @Override public void paint(Graphics g) {
        super.paint(g);
        if (run != null) {
            EventQueue.invokeLater(run);
            run = null;
        }
    }
}
