/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import gov.nist.pededitor.DecorationHandle.Type;

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class SourceImage implements Decoration {
    /** Transform from original coordinates to principal coordinates.
        Original coordinates are (x,y) positions within a scanned
        image. Principal coordinates are either the natural (x,y)
        coordinates of a Cartesian graph or binary diagram (for
        example, y may equal a temperature while x equals the atomic
        fraction of the second diagram component), or the fraction
        of the right and top components respectively for a ternary
        diagram. */
    protected PolygonTransform transform = null;
    protected transient Transform2D reverseTransform = null;

    protected double alpha = 0.0;
    protected transient double oldAlpha = 0.0;
    protected String filename;
    protected byte[] bytes;
    protected transient BufferedImage image = null;
    protected transient boolean triedToLoad = false;

    public SourceImage() {}

    @Override
    public SourceImage clone() {
        SourceImage res = new SourceImage();
        res.alpha = alpha;
        res.filename = filename;
        res.bytes = bytes;
        res.triedToLoad = false;
        res.transform = transform.clone();
        res.transformedImages = transformedImages;
        return res;
    }

    public SourceImage(BufferedImage image) {
        this.image = image; // TODO bytes...
    }

    /** Because rescaling an image is slow, keep a cache of locations
        and sizes that have been rescaled. All of these images have
        had oldTransform applied to them; if a new transform is attempted,
        the cache gets emptied. */
    protected transient ArrayList<ScaledCroppedImage> transformedImages
        = new ArrayList<>();

    @JsonIgnore public BufferedImage getImage() {
        if (triedToLoad || image != null)
            return image;
        triedToLoad = true;
        try {
            if (bytes == null) {
                if (filename == null) {
                    return null;
                }
                bytes = Files.readAllBytes(Paths.get(filename));
            }

            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException x) {
            // No better option than to live with it.
            bytes = null;
        }
        return image;
    }

    public double getAlpha() { return alpha; }

    public void setAlpha(double alpha) {
        if (alpha != this.alpha) {
            oldAlpha = this.alpha;
            this.alpha = alpha;
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        bytes = null;
        image = null;
        transformedImages = new ArrayList<>();
        triedToLoad = false;
    }

    public void setTransform(PolygonTransform xform) {
        this.transform = xform.clone();
        reverseTransform = null;
    }

    public PolygonTransform getTransform() {
        return transform.clone();
    }

    /** @return the inverse transform of p. */
    public Point2D.Double inverseTransform(Point2D p) throws UnsolvableException {
        if (reverseTransform == null) {
            if (transform == null)
                return null;
            try {
                reverseTransform = transform.createInverse();
            } catch (NoninvertibleTransformException e) {
                System.err.println("This transform is not invertible");
                System.exit(2);
            }
        }
        return reverseTransform.transform(p);
    }

    /* Like setAlpha, but trying to set the value to what it already
       is causes it to change to oldAlpha. If oldAlpha also equals its
       current value, it changes to invisible (0) if it didn't used to
       be, and to 1.0 (opaque) if it used to be invisible. This allows
       control-H to switch between the image being hidden or not.. */
    public double toggleAlpha(double value) {
        double oa = alpha;
        if (value == alpha)
            value = oldAlpha;
        if (value == alpha) {
            if (value > 0) {
                value = 0;
            } else {
                value = 1.0;
            }
        }
        oldAlpha = oa;
        alpha = value;
        return alpha;
    }

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

    void emptyCache() {
        transformedImages = new ArrayList<>();
    }

    @Override
    public void draw(Graphics2D g, AffineTransform xform, double scale) {
        if (alpha == 0)
            return;
        try {
            AffineTransform xform2 = AffineTransform.getScaleInstance(scale, scale);
            xform2.concatenate(xform);
            Rectangle clip = g.getClip().getBounds();
            BufferedImage image2 = transform(clip, xform2, ImageTransform.DithererType.FAST,
                    alpha);
            g.drawImage(image2, clip.x, clip.y, null); // UNDO
        } catch (IOException x) {
            // OK, fall through
        }
    }

    public BufferedImage getImage(double alpha) throws IOException {
        BufferedImage img = getImage();
        if (alpha == 1) {
            return img;
        }

        return image; // XXX TODO
    }

    /** @param imageBounds the rectangle to crop the image into
        in unscaled coordinates, independent of the view bounds.

        @param viewBounds The region that is actually visible right
        now, in scaled coordinates. This will typically correspond to
        the clipping region of the Graphics2D object.

        imageBounds and viewBounds are considered separately for
        caching purposes. Scaling is expensive, and viewBounds can
        change rapidly (as when you move the scrollbar), so caching a
        version of the image that is larger than viewBounds but not
        larger than imageBounds, and clipping it to the view region,
        can be faster than recomputing from scratch each time. */
    ScaledCroppedImage getScaledOriginalImage(
            AffineTransform principalToAlignedPage,
            double scale, Rectangle viewBounds, Rectangle2D imageBoundsD) {
        AffineTransform principalToScaledPage = (AffineTransform) principalToAlignedPage.clone();
        principalToScaledPage.preConcatenate(AffineTransform.getScaleInstance(scale,  scale));
        imageBoundsD = Geom.createScaled(imageBoundsD, scale);
        int x = (int) Math.floor(imageBoundsD.getX());
        int x2 = (int) Math.ceil(imageBoundsD.getX() + imageBoundsD.getWidth());
        int y = (int) Math.floor(imageBoundsD.getY());
        int y2 = (int) Math.ceil(imageBoundsD.getY() + imageBoundsD.getHeight());
        Rectangle imageBounds = new Rectangle(x, y, x2-x, y2-y);

        Rectangle imageViewBounds = imageBounds.intersection(viewBounds);

        // Attempt to work around a bug where Rectangle#intersection
        // returns negative widths or heights.
        if (imageViewBounds.width <= 0 || imageViewBounds.height <= 0) {
            return null;
        }

        int totalMemoryUsage = 0;
        int maxScoreIndex = -1;
        int maxScore = 0;

        if (transformedImages == null) {
            transformedImages = new ArrayList<ScaledCroppedImage>();
        }
        int cnt = transformedImages.size();

        for (int i = cnt - 1; i>=0; --i) {
            ScaledCroppedImage im = transformedImages.get(i);
            if (Math.abs(1.0 - scale / im.scale) < 1e-6
                && (imageViewBounds == null
                    || im.cropBounds.contains(imageViewBounds))) {
                // Found a match.

                // Promote this image to the front of the LRU queue (last
                // position in the ArrayList).
                transformedImages.remove(i);
                transformedImages.add(im);
                return im;
            }

            // Lower scores are better. Penalties are given for memory
            // usage and distance back in the queue (implying the
            // image has not been used recently).

            int mu = im.getMemoryUsage();
            totalMemoryUsage += mu;

            int thisScore = mu * (cnt - i);
            if (thisScore > maxScore) {
                maxScore = thisScore;
                maxScoreIndex = i;
            }
        }

        // Save memory if we're at the limit.

        int totalMemoryLimit = 20000000; // Limit is 20 megapixels total.
        int totalImageCntLimit = 50;
        if (totalMemoryUsage > totalMemoryLimit) {
            transformedImages.remove(maxScoreIndex);
        } else if (cnt >= totalImageCntLimit) {
            // Remove the oldest image.
            transformedImages.remove(0);
        }

        // Create a new ScaledCroppedImage that is big enough to hold
        // all of a medium-sized scaled image and that is also at
        // least several times the viewport size if the scaled image
        // is big enough to need to be cropped.

        // Creating a cropped image that is double the viewport size
        // in both dimensions is near optimal in the sense that for a
        // double-sized cropped image, if the user drags the mouse in
        // a fixed direction, the frequency with which the scaled
        // image has to be updated times the approximate cost of each
        // update is minimized.

        Dimension maxCropSize = new Dimension
            (Math.max(2000, viewBounds.width * 2),
             Math.max(1500, viewBounds.height * 2));

        Rectangle cropBounds = new Rectangle();

        if (imageBounds.width * 3 <= maxCropSize.width * 4) {
            // If allowing a little extra space beyond the normal
            // maximum can make cropping unnecessary, then do it.
            cropBounds.x = 0;
            cropBounds.width = imageBounds.width;
        } else {
            int margin1 = (maxCropSize.width - imageViewBounds.width) / 2;
            int margin2 = margin1;

            int ivmin = imageViewBounds.x;
            int ivmax = ivmin + imageViewBounds.width;
            int immax = imageBounds.x + imageBounds.width;

            int extra = margin1 - ivmin;
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 += extra;
                margin1 -= extra;
            }

            extra = margin2 - (immax - ivmax);
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 -= extra;
                margin1 += extra;
            }

            cropBounds.x = imageViewBounds.x - margin1;
            cropBounds.width = imageViewBounds.width + margin1 + margin2;
        }

        if (imageBounds.height * 3 <= maxCropSize.height  * 4) {
            // If allowing a little extra space beyond the normal
            // maximum can make cropping unnecessary, then do it.
            cropBounds.y = 0;
            cropBounds.height = imageBounds.height;
        } else {
            int margin1 = (maxCropSize.height - imageViewBounds.height) / 2;
            int margin2 = margin1;

            int ivmin = imageViewBounds.y;
            int ivmax = ivmin + imageViewBounds.height;
            int immax = imageBounds.y + imageBounds.height;

            int extra = margin1 - ivmin;
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 += extra;
                margin1 -= extra;
            }

            extra = margin2- (immax - ivmax);
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 -= extra;
                margin1 += extra;
            }

            cropBounds.y = imageViewBounds.y - margin1;
            cropBounds.height = imageViewBounds.height + margin1 + margin2;
        }

        ScaledCroppedImage im = new ScaledCroppedImage();
        im.scale = scale;
        im.imageBounds = imageBounds;
        im.cropBounds = cropBounds;
        ImageTransform.DithererType dither
            = (cropBounds.getWidth() * cropBounds.getHeight() > 3000000)
            ? ImageTransform.DithererType.FAST
            : ImageTransform.DithererType.GOOD;

        try {
            im.croppedImage = transform(cropBounds, principalToScaledPage, dither, 1.0);
        } catch (IOException e) {
            // TODO Auto-generated catch block // UNDO
            e.printStackTrace();
        }
        transformedImages.add(im);
        return im;
    }

    /** @return the original binary content of the image file. Changing
        the array contents is not safe. */
    @JsonProperty("bytes")
    protected byte[] getBytesUnsafe() throws IOException {
        return bytes;
    }

    /** Set the binary content of the image file. Changing
        the array contents is not safe. */
    @JsonProperty("bytes")
    protected void setBytesUnsafe(byte[] bytes) {
        this.bytes = bytes;
        image = null;
        transformedImages = new ArrayList<>();
        triedToLoad = false;
    }

    /** Apply transform to the image, then apply
     principalToScaledPage, then translate the upper-left corner of
     cropRect to position (0,0). Return the portion of the image that
     intersects cropRect with its alpha value multiplied by alpha. Any
     part of the returned image not covered by the translated input
     image is assigned an alpha of 0. Return null if the image could
     not be generated or it would be completely transparent.
    */
    synchronized BufferedImage transform(Rectangle cropRect,
            AffineTransform principalToScaledPage,
            ImageTransform.DithererType dither, double alpha)
        throws IOException {
        PolygonTransform xform = transform.clone();
        xform.preConcatenate(new Affine(principalToScaledPage));
        return transform(getImage(), cropRect, xform, dither, alpha);
    }

    /** Apply transform to the image, then apply
     principalToScaledPage, then translate the upper-left corner of
     cropRect to position (0,0). Return the portion of the image that
     intersects cropRect with its alpha value multiplied by alpha. Any
     part of the returned image not covered by the translated input
     image is assigned an alpha of 0. Return null if the image could
     not be generated or it * would be completely transparent.
    */
    public static BufferedImage transform(BufferedImage input,
            Rectangle cropRect, PolygonTransform xform,
            ImageTransform.DithererType dither, double alpha) {
        if (input == null || alpha == 0)
            return null;

        PolygonTransform toCrop = xform.clone();

        // Shift the transform so that location (cropRect.x,
        // cropRect.y) is mapped to location (0,0).
        toCrop.preConcatenate(new Affine(
                        AffineTransform.getTranslateInstance(
                        -cropRect.x, -cropRect.y)));

        System.out.println("Resizing original image (" + dither + ")...");
        BufferedImage img = ImageTransform.run(toCrop, input,
                null, cropRect.getSize(), dither, BufferedImage.TYPE_INT_ARGB);
        if (alpha == 1) {
            return img;
        }

        BufferedImage res = new BufferedImage(cropRect.width,
                cropRect.height, BufferedImage.TYPE_INT_ARGB);
        draw(res.createGraphics(), img, (float) alpha);
        return res;
    }

    @Override public void draw(Graphics2D g, double scale) {
        // TODO Auto-generated method stub
    }

    @Override @JsonIgnore public Color getColor() {
        return null;
    }

    @Override public void setColor(Color color) {
    }

    @Override @JsonIgnore public DecorationHandle[] getHandles(Type type) {
        // TODO Auto-generated method stub
        return new DecorationHandle[0];
    }

    @Override public void transform(AffineTransform xform) {
        PolygonTransform xform2 = getTransform();
        xform2.preConcatenate(new Affine(xform));
        setTransform(xform2);
    }

    @Override public String typeName() {
        return "image";
    }

    @Override
    public SourceImage createTransformed(AffineTransform xform) {
        SourceImage res = clone();
        res.transform(xform);
        return res;
    }
}
