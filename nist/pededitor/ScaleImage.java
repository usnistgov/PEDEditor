/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.image.BufferedImage;

public class ScaleImage {
    /** Simple method to downscale an image by a constant factor. The
        values of all input pixels are averaged, weighting each
        according to their alpha value. */
    static BufferedImage downscale(BufferedImage input, int descale) {
        int widthIn = input.getWidth();
        int widthOut = widthIn / descale;
        int heightIn = input.getHeight();
        int heightOut = heightIn / descale;
        BufferedImage res = new BufferedImage(widthOut, heightOut,
                                              BufferedImage.TYPE_INT_ARGB);
        // Weight each pixel's RGB values according to their alpha values.
        for (int x = 0; x < widthOut; ++x) {
            for (int y = 0; y < heightOut; ++y) {
                int r = 0, g = 0, b = 0, a = 0;
                int x0max = (x+1) * descale;
                int y0max = (y+1) * descale;
                int samplesPerPixel = 0;
                for (int x0 = x * descale; x0 < x0max; ++x0) {
                    for (int y0 = y * descale; y0 < y0max; ++y0) {
                        ++samplesPerPixel;
                        int argb = input.getRGB(x0, y0);
                        // The (& 0xff) part below is necessary: it
                        // converts the result to an unsigned value!
                        int a1 = (argb >> 24) & 0xff;
                        r += a1 * ((argb >> 16) & 0xff);
                        g += a1 * ((argb >> 8) & 0xff);
                        b += a1 * (argb & 0xff);
                        a += a1;
                    }
                }

                if (a == 0) {
                    // The RGB values of a 100% transparent pixel are
                    // irrelevant.
                    res.setRGB(x, y, 0);
                } else {
                    int half = a / 2; // for rounding purposes
                    r = (r + half) / a;
                    g = (g + half) / a;
                    b = (b + half) / a;
                    a = (a + samplesPerPixel/2) / samplesPerPixel;
                    res.setRGB(x, y, (a << 24) +  (r << 16) + (g << 8) + b);
                }
            }
        }
        return res;
    }
}
