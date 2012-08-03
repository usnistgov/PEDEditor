package gov.nist.pededitor;

/** Exception type for unsolvable transforms */
public class FailedToConvergeException extends Exception {
    private static final long serialVersionUID = -1594368491990023912L;

    FailedToConvergeException() {}

    FailedToConvergeException(String message) {
        super(message);
    }
}
