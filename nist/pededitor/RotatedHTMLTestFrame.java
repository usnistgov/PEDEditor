package gov.nist.pededitor;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.*;

/** A test of displaying styled rotated text on the screen. */
public class RotatedHTMLTestFrame extends JFrame {

    public RotatedHTMLTestFrame() {
        // JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(new RotatedHTMLTestPane());
        getContentPane().setPreferredSize(new Dimension(600, 400));
    }

    /** Test code. */
    public static void main(String[] args) {
        EventQueue.invokeLater(new ArgsRunnable(args) {
                public void run() {
                    try {
                        RotatedHTMLTestFrame frame
                            = new RotatedHTMLTestFrame();
                        frame.pack();
                        frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }
}
