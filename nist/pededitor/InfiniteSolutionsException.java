/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Exception type for inverse transforms that have an infinite number of solutions */
public class InfiniteSolutionsException extends UnsolvableException {
    private static final long serialVersionUID = -790745748792265891L;

    InfiniteSolutionsException() {}
    InfiniteSolutionsException(String message) {
        super(message);
    }
}
