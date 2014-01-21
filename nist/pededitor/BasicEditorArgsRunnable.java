/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.*;

/**
   You have to do the EventQueue.invokeLater() thing when setting up
   Swing GUIs to insure they run on the GUI thread. This class can
   help you save the args from static void main(String[] args) in a
   way that makes them accessible in that code. */
public abstract class BasicEditorArgsRunnable implements Runnable {
    public BasicEditorCreator ec;
    public String[] args;
        
    public BasicEditorArgsRunnable(BasicEditorCreator ec, String[] args) {
        this.ec = ec;
        this.args = Arrays.copyOf(args, args.length);
    }

    @Override public void run() {
        ec.run().run(args);
    }
};
