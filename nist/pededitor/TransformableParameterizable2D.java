/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.AffineTransform;

public interface TransformableParameterizable2D
    extends BoundedParameterizable2D {
    TransformableParameterizable2D createTransformed(AffineTransform xform);

    /* This should return the parameterization of the transformed
       control points, not the transform of the parameterization of
       the original control points. */
    BoundedParam2D getParameterization(AffineTransform xform);

    TransformableParameterizable2D clone();
}
