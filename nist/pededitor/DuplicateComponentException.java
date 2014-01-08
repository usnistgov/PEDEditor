/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Exception type for unsolvable transforms */
public class DuplicateComponentException extends Exception {
    private static final long serialVersionUID = 7023826949460454058L;

    DuplicateComponentException() {}

    DuplicateComponentException(String message) {
        super(message);
    }
}
