/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public class OverflowException extends Exception {
    private static final long serialVersionUID = -2243013464366737879L;

    OverflowException() {}

    OverflowException(String message) {
        super(message);
    }
}
