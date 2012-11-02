package gov.nist.pededitor;

/** Exception type for unsolvable transforms */
public class DuplicateComponentException extends Exception {
    private static final long serialVersionUID = 7023826949460454058L;

    DuplicateComponentException() {}

    DuplicateComponentException(String message) {
        super(message);
    }
}
