package gov.nist.pededitor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** A frame that holds a scroll pane that displays an image. That's
    pretty much it. */
public class ImageScrollFrame extends JFrame {

    protected JPanel contentPane;
    protected JScrollPane scrollPane;
    protected ImagePane imagePane;
    protected int preferredWidth = 800;
    protected int preferredHeight = 600;

    public ImageScrollFrame() {
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLocation(0, 0);
      contentPane = new JPanel(new BorderLayout());
      setContentPane(contentPane);
      imagePane = newImagePane();
      scrollPane = new JScrollPane(imagePane);
      scrollPane.setPreferredSize
          (new Dimension(preferredWidth, preferredHeight));
      contentPane.add(scrollPane, BorderLayout.CENTER);
    }

    /** Allow subclasses to use alternative component types. */
    protected ImagePane newImagePane() {
        return new ImagePane();
    }

    public ImagePane getImagePane() {
        return imagePane;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    void setFilename(String filename) {
        try {
            BufferedImage im = ImageIO.read(new File(filename));
            if (im == null) {
                throw new IOException(filename + ": unknown image format");
            }
            setTitle(filename);
            setImage(im);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setImage(BufferedImage im) {
        int w = im.getWidth();
        int h = im.getHeight();

        if (w < preferredWidth && h < preferredHeight) {
            scrollPane.setPreferredSize(null);
        } else {
            scrollPane.setPreferredSize
                (new Dimension(preferredWidth, preferredHeight));
        }
        imagePane.setImage(im);
    }

    public BufferedImage getImage() {
        return imagePane.getImage();
    }

    public static void printHelp() {
        System.err.println("Usage: java ImageScrollFrame [<filename>]");
        System.err.println("\nDisplay an image in a scrolling window. That's all.");
    }

    /** Test code. */
    public static void main(String[] args) {
        EventQueue.invokeLater(new ArgsRunnable(args) {
                public void run() {
                    if (args.length != 1) {
                        printHelp();
                        System.exit(2);
                    }
                    
                    try {
                        ImageScrollFrame frame = new ImageScrollFrame();
                        frame.setFilename(args[0]);
                        frame.pack();
                        frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }
}
