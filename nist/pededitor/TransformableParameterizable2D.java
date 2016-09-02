/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;

public interface TransformableParameterizable2D
    extends BoundedParameterizable2D {
    TransformableParameterizable2D createTransformed(AffineTransform xform);
}
