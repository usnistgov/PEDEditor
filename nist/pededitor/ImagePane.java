package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;

import javax.swing.JPanel;

public class ImagePane extends JPanel {
    private static final long serialVersionUID = -5288040395450118276L;
    protected BufferedImage image;

    public void setImage(BufferedImage image) {
        this.image = image;
        setPreferredSize(new Dimension(image.getWidth(null),
                                       image.getHeight(null)));
        revalidate();
        repaint();
    }

    public void paint(Graphics g) {
        Rectangle drawHere = g.getClipBounds();
        g.setColor(Color.LIGHT_GRAY);
        ((Graphics2D) g).fill(drawHere);
        if (image != null) {
            g.drawImage(image, 0, 0, null);
        }
    }

    public BufferedImage getImage() {
        return image;
    }
}
