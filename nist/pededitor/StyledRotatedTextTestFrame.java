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
public class StyledRotatedTextTestFrame extends JFrame {

    public StyledRotatedTextTestFrame() {
        // JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(new StyledRotatedTextTestPane());
        getContentPane().setPreferredSize(new Dimension(600, 400));
    }

    /** Test code. */
    public static void main(String[] args) {
        EventQueue.invokeLater(new ArgsRunnable(args) {
                public void run() {
                    try {
                        StyledRotatedTextTestFrame frame
                            = new StyledRotatedTextTestFrame();
                        frame.pack();
                        frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }
}
