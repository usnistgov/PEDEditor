/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Rectangle;

/** Simple interface for parallel tasks applied to each pixel of an image. */
public interface RectangleProcessor {
    /* Return an estimate of the time that run() will take. The units are kind of arbitrary. */
    public double estimatedRunTime(Rectangle outputBounds);
    public void run(Rectangle outputBounds);
}
