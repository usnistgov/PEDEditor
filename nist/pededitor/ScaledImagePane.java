package gov.nist.pededitor;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

/** A simple JPanel to display an image rescaled to fit the preferred
    panel size. You should call setPreferredSize() to tell what that
    size is.

    This is not efficient, because the rescaling happens with every
    repaint. */
public class ScaledImagePane extends JPanel {
	private static final long serialVersionUID = -5722774110161790794L;
	protected BufferedImage image;

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public void paintComponent(Graphics g) {
        Rectangle drawHere = g.getClipBounds();
        g.setColor(Color.LIGHT_GRAY);
        ((Graphics2D) g).fill(drawHere);
        if (image != null) {
            Dimension d = getPreferredSize();
            int iw = image.getWidth();
            int ih = image.getHeight();
            double xScale = d.getWidth() / iw;
            double yScale = d.getHeight() / ih;
            double scale = Math.min(xScale, yScale);
            g.drawImage(image,
                        0, 0,
                        (int) Math.round(iw * scale), (int) Math.round(ih * scale),
                        0, 0,
                        iw-1, ih-1,
                        null);
        }
    }

    public BufferedImage getImage() {
        return image;
    }
}
