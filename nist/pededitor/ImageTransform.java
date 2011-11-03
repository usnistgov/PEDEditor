package gov.nist.pededitor;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/** This module provides a method to transform an input image to an
    output image. The xform object should support #inverse (whose
    output should support the #call(x,y) method), #max_output_x, and
    #max_output_y. */
public class ImageTransform {

    /** run() with default size and black background. */
    public static BufferedImage run(PolygonTransform xform,
                                    BufferedImage imageIn) {
        return run(xform, imageIn, Color.BLACK);
    }

    /** run() with output image size just large enough to hold
        xform.outputBounds(). */
    public static BufferedImage run(PolygonTransform xform,
                                    BufferedImage imageIn,
                                    Color background) {
        Rectangle2D.Double b = xform.outputBounds();
        int width = (int) Math.ceil(b.x + b.width);
        int height = (int) Math.ceil(b.y + b.height);
        return run(xform, imageIn, background, new Dimension(width, height));
    }

    /**  The scale of the output is 1 pixel = 1 unit. The minimum x and
         y values are 0 and 0. If those values are not suitable, then
         concatenate xform with an affine transformation as needed. */
    public static BufferedImage run(PolygonTransform xform,
                                    BufferedImage input,
                                    Color background,
                                    Dimension size) {
        // Forget about alpha channel for now.

        int width = size.width;
        int height = size.height;

        StopWatch s = new StopWatch();
        s.start();

        BufferedImage output = new BufferedImage(width, height,
                                                 BufferedImage.TYPE_INT_RGB);
        Transform2D xformi;

        try {
            // We actually want the inverse transformation, to measure the
            // color of the input image at each pixel of the output image.
            xformi = xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }

        Rectangle2D.Double ib = xform.inputBounds();
        double ipixels = (ib.width+1) * (ib.height+1);
        int sampleCnt = (int) Math.round
            (Math.max(3, Math.min(11, 3 * Math.sqrt(ipixels / (width * height)))));
        int samplesPerPixel = sampleCnt * sampleCnt;
        double inWidth = input.getWidth();
        double inHeight = input.getHeight();
        int totalSampleCnt = 0;

        // Transform a pixel's worth of points at once for better
        // speed. (This wouldn't be necessary in C++, which has stack
        // objects with zero allocation overhead. The color
        // manipulation, too, could be done much nicer in C++ as
        // zero-overhead four-byte objects.)
        double points[] = new double[samplesPerPixel * 2];
        int backRGB = background.getRGB();
        

        // random() turns out to slow things down considerably, so reuse
        // old random values -- we don't need perfection here.
        double randCache[] = new double[0x1000];
        for (int i = 0; i < randCache.length; ++i) {
            randCache[i] = Math.random();
        }
        int randCachePos = 0;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // The image is over-sampled (with respect to the size
                // of an output pixel) to reduce aliasing: output
                // pixels are divided into sampleCnt x sampleCnt
                // sub-pixels, and the colors of those sub-pixels are
                // averaged to obtain the color of the output pixel.
                // (The value of sampleCnt setting is pretty
                // arbitrary: up to 11 times smaller than an output
                // pixel if the output image is much smaller than the
                // input one, and as little as 3 times smaller than
                // the output pixel if the output image is as big or
                // bigger than the input image.)

                // Each sub-pixel is sampled not in its center, but in
                // a uniformly random location; this randomization
                // should reduce systematic aliasing artifacts.
                {
                    int pos = 0;
                    for (int xsub = 0; xsub < sampleCnt; ++xsub) {
                        for (int ysub = 0; ysub < sampleCnt; ++ysub) {
                            points[pos++] = x +
                                (xsub + randCache[(++randCachePos) & 0xfff]) / sampleCnt;
                            points[pos++] = y +
                                (ysub + randCache[(++randCachePos) & 0xfff]) / sampleCnt;
                        }
                    }
                }

                try {
                    xformi.transform(points, 0, points, 0, samplesPerPixel);
                    // Now points[] contains the (x,y) coordinates of
                    // points in the input image whose colors should
                    // be averaged to set the color for this pixel in
                    // the output image.
                    totalSampleCnt += samplesPerPixel;
                    int r = 0;
                    int g = 0;
                    int b = 0;
                    for (int pos = 0; pos < points.length; pos += 2) {
                        double xd = points[pos];
                        double yd = points[pos+1];
                        int rgb = (xd >= 0. && xd < inWidth && yd >= 0. && yd < inHeight)
                            ? input.getRGB((int) xd, (int) yd) : backRGB;
                        r += (rgb >> 16) & 255;
                        g += (rgb >> 8) & 255;
                        b += rgb & 255;
                    }

                    // Round values over 1/2 up in the integer
                    // division
                    int half = samplesPerPixel / 2;
                    r = (r + half) / samplesPerPixel;
                    g = (g + half) / samplesPerPixel;
                    b = (b + half) / samplesPerPixel;

                    output.setRGB(x, y, (r << 16) + (g << 8) + b);
                } catch (UnsolvableException e) {
                    output.setRGB(x, y, backRGB);
                }
            }
        }

        System.err.println(totalSampleCnt + " samples used.");
        s.ping();

        return output;
    }

    /** Just a test harness. */
    public static void main(String[] args) {
        BufferedImage input;
        if (args.length != 1) {
            System.err.println("Usage: java ImageTransform <filename>");
            return;
        }
        try {
            input = ImageIO.read(new File(args[0]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (input == null) {
            System.err.println("Read of " + args[0] + " failed.");
            System.exit(2);
        }

        double[][] coords = {{0,4}, {4,4}, {2,5}, {2, 0}}; // kite_reverse
        Point2D.Double[] points = Duh.toPoint2DDoubles(coords);
        Duh.sort(points, true);
        QuadToRect xform = new QuadToRect();
        // RectToQuad xform = new RectToQuad();
        xform.setVertices(points);

        System.out.println(xform);

        {
            Rectangle2D.Double inb = xform.inputBounds();
            Affine af = new Affine();
            af.setToScale((inb.x + inb.width)/input.getWidth(),
                          (inb.y + inb.height)/input.getHeight());
            xform.preConcatenate(af);
        }

        int outWidth = 800;
        int outHeight = 600;

        System.out.println(xform);

        {
            Rectangle2D.Double outb = xform.outputBounds();
            double outScale = Math.min(outWidth/(outb.x + outb.width),
                                       outHeight/(outb.y + outb.height));
            Affine af = new Affine();
            af.setToScale(outScale, outScale);
            xform.concatenate(af);
        }

        System.out.println(xform);
        xform.check();

        BufferedImage output = run(xform, input);
        try {
            ImageIO.write(output, "jpg", new File("test-out.jpg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
