package gov.nist.pededitor;

import java.awt.*;
import javax.swing.*;

/** A test of displaying styled rotated text on the screen. */
public class LineFillTestFrame extends JFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1620222527270248318L;

	public LineFillTestFrame() {
        // JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(new LineFillTestPane());
        getContentPane().setPreferredSize(new Dimension(600, 400));
    }

    /** Test code. */
    public static void main(String[] args) {
        EventQueue.invokeLater(new ArgsRunnable(args) {
                @Override
				public void run() {
                    try {
                        LineFillTestFrame frame
                            = new LineFillTestFrame();
                        frame.pack();
                        frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }
}
