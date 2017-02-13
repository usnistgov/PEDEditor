/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import org.codehaus.jackson.annotate.JsonIgnore;

public interface BoundedParameterizable2D {
    @JsonIgnore BoundedParam2D getParameterization();
}
