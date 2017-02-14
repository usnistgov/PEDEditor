/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.codehaus.jackson.annotate.JsonProperty;

import gov.nist.pededitor.DecorationHandle.Type;

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class SourceImage implements Decoration {
    public PolygonTransform originalToPrincipal;
    protected transient PolygonTransform principalToOriginal;

    protected double alpha = 0.0;
    protected BackgroundImageType backgroundType = BackgroundImageType.NONE;
    protected String filename;
    protected byte[] bytes;
    protected transient BufferedImage image = null;

    public BufferedImage getImage() throws IOException {
        return image; // XXX TODO
    }

    public double getAlpha() { return alpha; }

    public void setAlpha(double alpha) { this.alpha = alpha; }

    static void draw(Graphics2D g, BufferedImage im, float alpha) {
        draw(g, im, alpha, 0, 0);
    }

    public static void draw(Graphics2D g, BufferedImage im, float alpha,
            int x, int y) {
        Composite oldComposite = g.getComposite();
        try {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                            alpha));
            g.drawImage(im, x, y, null);
        } finally {
            g.setComposite(oldComposite);
        }
    }

    public BufferedImage getImage(double alpha) throws IOException {
        BufferedImage img = getImage();
        if (alpha == 1) {
            return img;
        }

        return image; // XXX TODO
    }

    public void setBackgroundType(BackgroundImageType value) {
        if (value == null) {
            value = BackgroundImageType.NONE;
        }
        backgroundType = value;
    }

    public BackgroundImageType getBackgroundType() {
        return backgroundType;
    }

    /** @return the binary contents of the original image. Changing
        the array contents is not safe. */
    @JsonProperty("bytes")
    protected byte[] getBytesUnsafe() throws IOException {
        return bytes;
    }

    /** Scale the diagram by the given amount, placing the upper-left
        corner in position (0,0), but don't actually draw the diagram.
        Instead, just return the portion of originalImage, adjusted to
        the current background image alpha (mixed with a background of
        pure white), that would sit in the background of the cropRect
        portion of the scaled diagram.

        TODO: the way fade() works here is doubtful. It makes more
        sense to use an RGBA image type than to use RGB and mix the
        color with pure white according to the alpha value. Only real
        RGBA allows bitmaps to be layered in nontrivial ways.
    */
    BufferedImage transform(
            Rectangle cropBounds, Affine xform,
            ImageTransform.DithererType dither, double alpha,
            Color backColor, int imageType) throws IOException {
        BufferedImage res = new BufferedImage(
                cropBounds.width, cropBounds.height, imageType);
        Graphics2D g = res.createGraphics();
        g.setPaint(backColor);
        g.fill(cropBounds);

        BufferedImage input = getImage();
        if (alpha > 0) {
            try {
                input = getImage();
            } catch (IOException x) {
                // Oh well, treat original image as blank.
            }
        }

        if (input == null) {
            return res;
        }

        // Create the transformed, cropped, and faded image of the
        // original diagram.
        PolygonTransform originalToCrop = originalToPrincipal.clone();
        originalToCrop.preConcatenate(xform);

        // Shift the transform so that location (cropBounds.x,
        // cropBounds.y) is mapped to location (0,0).

        originalToCrop.preConcatenate
            (new Affine(AffineTransform.getTranslateInstance
                        ((double) -cropBounds.x, (double) -cropBounds.y)));

        System.out.println("Resizing original image (" + dither + ")...");
        BufferedImage img = ImageTransform.run(originalToCrop, input,
                backColor, cropBounds.getSize(), dither, imageType);
        draw(g, img, (float) alpha);
        return res;
    }

    @Override public void draw(Graphics2D g, double scale) {
        // TODO Auto-generated method stub
    }

    @Override public Color getColor() {
        return null;
    }

    @Override public void setColor(Color color) {
    }

    @Override public DecorationHandle[] getHandles(Type type) {
        // TODO Auto-generated method stub
        return new DecorationHandle[0];
    }

    @Override public void transform(AffineTransform xform) {
        // TODO Auto-generated method stub
    }

    @Override public String typeName() {
        return "image";
    }
}
