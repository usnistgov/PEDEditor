/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

class CroppedTransformedImage {

    PolygonTransform transform;
    /** The transformed image cropped to cropBounds. */
    BufferedImage croppedImage;
    /** The bounds of the portion of the scaled image that is stored
        in croppedImage. */
    Rectangle cropBounds;

    int getMemoryUsage() {
        return cropBounds.width * cropBounds.height;
    }

    @Override public String toString() {
        return "Transform: " + transform + " crop: " + cropBounds;
    }
    
}
