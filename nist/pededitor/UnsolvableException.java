/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Exception type for unsolvable transforms */
public class UnsolvableException extends Exception {
    private static final long serialVersionUID = -1594368491990023912L;

    UnsolvableException() {}

    UnsolvableException(String message) {
        super(message);
    }
}
