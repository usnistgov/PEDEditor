package gov.nist.pededitor;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import javax.imageio.ImageIO;

/** This module provides a method to transform an input image to an
    output image. The xform object should support #inverse (whose
    output should support the #call(x,y) method), #max_output_x, and
    #max_output_y. */
public class ImageTransform {
    static final ForkJoinPool mainPool = new ForkJoinPool();

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

    static class RecursiveTransformer extends RecursiveAction {
        /**
		 * 
		 */
        private static final long serialVersionUID = -2489117155798819683L;

        final public BufferedImage input;
        final public Rectangle outputBounds;
        final public BufferedImage output;
        final public Color background;
        final int sampleCnt;
        /** Inverse transformation, to measure the color of the input
            image at each pixel of the output image. */
        final Transform2D inverseTransform;

        RecursiveTransformer(BufferedImage input,
                             BufferedImage output, Rectangle outputBounds,
                             Transform2D inverseTransform, Color background,
                             int sampleCnt) {
            this.input = input;
            this.inverseTransform = inverseTransform;
            this.output = output;
            this.outputBounds = outputBounds;
            this.background = background;
            this.sampleCnt = sampleCnt;
        }

        protected void compute() {
            if (outputBounds.width >= 2
            		&& outputBounds.width * outputBounds.height > 300 * 300) {
                RecursiveTransformer leftHalf
                    = new RecursiveTransformer
                    (input, output,
                     new Rectangle(outputBounds.x, outputBounds.y,
                                   outputBounds.width/2, outputBounds.height),
                     inverseTransform, background, sampleCnt);
                RecursiveTransformer rightHalf
                    = new RecursiveTransformer
                    (input, output,
                     new Rectangle(outputBounds.x + outputBounds.width/2,
                                   outputBounds.y,
                                   (outputBounds.width + 1)/2,
                                   outputBounds.height),
                     inverseTransform, background, sampleCnt);
                invokeAll(leftHalf, rightHalf);
            } else {
                run(input, output, outputBounds, inverseTransform,
                    background, sampleCnt);
            }
        }
    }

    static double[] randCache = null;

    static void initializeRandCache() {
        // random() turns out to slow things down considerably, so reuse
        // old random values -- we don't need perfection here.

        if (randCache == null) {
            randCache = new double[0x1000];
            for (int i = 0; i < randCache.length; ++i) {
                randCache[i] = Math.random();
            }
        }
    }


    static boolean requiresThreading(Rectangle rect) {
        return rect.width * rect.height > 100 * 100;
    }

    /** @param sampleCnt number of samples to take per pixel */
    static void run(BufferedImage input,
             BufferedImage output, Rectangle outputBounds,
             Transform2D inverseTransform, Color background,
             int sampleCnt) {
        int samplesPerPixel = sampleCnt * sampleCnt;
        double inWidth = input.getWidth();
        double inHeight = input.getHeight();

        // Transform a pixel's worth of points at once for better
        // speed. (This wouldn't be necessary in C++, which has stack
        // objects with zero allocation overhead. The color
        // manipulation, too, could be done much nicer in C++ as
        // zero-overhead four-byte objects.)
        double points[] = new double[samplesPerPixel * 2];
        int backRGB = background.getRGB();
        

        if (randCache == null) {
            randCache = new double[0x1000];
            for (int i = 0; i < randCache.length; ++i) {
                randCache[i] = Math.random();
            }
        }

        initializeRandCache();
        int randCachePos = 0;

        int xMax = outputBounds.x + outputBounds.width;
        int yMax = outputBounds.y + outputBounds.height;

        for (int x = outputBounds.x; x < xMax; ++x) {
            for (int y = outputBounds.y; y < yMax; ++y) {
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
                    inverseTransform.transform(points, 0, points, 0,
                                               samplesPerPixel);
                    // Now points[] contains the (x,y) coordinates of
                    // points in the input image whose colors should
                    // be averaged to set the color for this pixel in
                    // the output image.
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
    }

    /**  The scale of the output is 1 pixel = 1 unit. The minimum x and
         y values are 0 and 0. If those values are not suitable, then
         preConcatenate xform with an affine transformation as needed. */
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
            (Math.max(2, Math.min(11, 2 * Math.sqrt(ipixels / (width * height)))));
        Rectangle outputBounds = new Rectangle(0, 0, size.width, size.height);
        mainPool.invoke(new RecursiveTransformer(input, output, outputBounds,
                                                 xformi, background, sampleCnt));
        s.ping();
        return output;
    }

    /**  This is the quick and dirty version of run(). */
    public static BufferedImage runFast(PolygonTransform xform,
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

        double inWidth = input.getWidth();
        double inHeight = input.getHeight();
        int backRGB = background.getRGB();

        Point2D.Double p = new Point2D.Double(0,0);

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                int rgb;
                p.x = x + 0.5;
                p.y = y + 0.5;

                try {
                    p = xformi.transform(p);
                    double xd = p.x;
                    double yd = p.y;
                    rgb = (xd >= 0 && xd < inWidth && yd >= 0 && yd < inHeight)
                        ? input.getRGB((int) xd, (int) yd) : backRGB;
                } catch (UnsolvableException e) {
                    rgb = backRGB;
                }
                output.setRGB(x, y, rgb);
            }
        }
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
            xform.concatenate(af);
        }

        // int outWidth = 800;
        // int outHeight = 600;

        int outWidth = 4800;
        int outHeight = 3600;

        System.out.println(xform);

        {
            Rectangle2D.Double outb = xform.outputBounds();
            double outScale = Math.min(outWidth/(outb.x + outb.width),
                                       outHeight/(outb.y + outb.height));
            Affine af = new Affine();
            af.setToScale(outScale, outScale);
            xform.preConcatenate(af);
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
