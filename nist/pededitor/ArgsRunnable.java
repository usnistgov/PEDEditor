/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.*;

/**
   You have to do the EventQueue.invokeLater() thing when setting up
   Swing GUIs to insure they run on the GUI thread. This class can
   help you save the args from static void main(String[] args) in a
   way that makes them accessible in that code. */
public abstract class ArgsRunnable implements Runnable {
    public String[] args;
        
    public ArgsRunnable(String[] args) {
        this.args = Arrays.copyOf(args, args.length);
    }

    @Override
	abstract public void run();
};
