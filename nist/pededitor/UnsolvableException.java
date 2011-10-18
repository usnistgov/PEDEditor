package gov.nist.pededitor;

/** Exception type for unsolvable transforms */
public class UnsolvableException extends Exception {
   UnsolvableException() {}
   UnsolvableException(String message) {
      super(message);
   }
}
