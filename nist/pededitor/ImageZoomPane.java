package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;

public class ImageZoomPane extends ImagePane {
    private static final long serialVersionUID = 5357695613573941516L;

    /** The x offset into the image to display in the center of this pane. */
    protected int zoomX = 0;
    /** The y offset into the image to display in the center of this pane. */
    protected int zoomY = 0;

    protected int preferredWidth = 800;
    protected int preferredHeight = 600;

    /** (Optional) Fixed image of a crosshairs to superimpose in the
        center of this pane. It helps if that image includes an alpha
        channel... */
    protected Image crosshairs = null;

    public ImageZoomPane() {
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
    }

    @Override
	public void setImage(BufferedImage image) {
        this.image = image;
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        setPreferredSize
            (new Dimension
             (Math.min(preferredWidth, 2 * w - 1),
              Math.min(preferredHeight, 2 * h -1)));
        repaint();
    }

    @Override
	public void paintComponent(Graphics g) {
        Rectangle drawHere = g.getClipBounds();
        g.setColor(Color.WHITE);
        ((Graphics2D) g).fill(drawHere);
        if (image != null) {
            int cx = getWidth()/2;
            int cy = getHeight()/2;
            g.drawImage(image, cx-zoomX, cy-zoomY, null);
            if (crosshairs != null) {
                ((Graphics2D) g).drawImage(crosshairs,
                            cx - crosshairs.getWidth(null)/2,
                            cy - crosshairs.getHeight(null)/2,
                            null);
            }
        }
    }
}
