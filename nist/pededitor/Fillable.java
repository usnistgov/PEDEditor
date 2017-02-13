/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public interface Fillable {
    StandardFill getFill();
    void setFill(StandardFill fill);
}
