package gov.nist.pededitor;

import java.awt.image.BufferedImage;
import javax.swing.*;

/** A frame that holds a pane that displays a viewport into an image
    centered at position. */
public class ImageZoomFrame extends JFrame {
    private static final long serialVersionUID = 1239106959208277289L;

    protected ImageZoomPane contentPane;

    public ImageZoomFrame() {
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLocation(0, 0);
      contentPane = new ImageZoomPane();
      setContentPane(contentPane);
    }

    public ImageZoomPane getImageZoomPane() {
        return contentPane;
    }

    public void setImage(BufferedImage image) {
        contentPane.setImage(image);
    }

    /** Put the crosshairs at position (x,y) in the image. */
    public void setImageFocus(int x, int y) {
        contentPane.zoomX = x;
        contentPane.zoomY = y;
        contentPane.repaint();
    }
}
