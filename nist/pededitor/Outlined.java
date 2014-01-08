/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Shape;

/** Interface that permits common operations to be applied to
    different kinds of graphics objects */
public interface Outlined extends Decoration {
    Shape getOutline();
    DecorationHandle nearestHandle(double t);
}
