package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class StdOutErrRedirect
{
    static void run() {
        JFrame f = new JFrame();
        f.setTitle("Console");
        JTextPane textComponent = new JTextPane();
        JScrollPane sp = new JScrollPane(textComponent);
        sp.setPreferredSize(new Dimension(600, 400));
        f.getContentPane().add(sp);
        MessageConsole mc = new MessageConsole(textComponent);
        mc.redirectOut();
        mc.redirectErr(Color.RED, null);
        mc.setMessageLines(600);
        f.pack();
        f.setVisible(true);
        System.out.println("Standard output redirected.");
        System.err.println("Standard error redirected.");
    }
}
