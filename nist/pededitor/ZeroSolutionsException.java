package gov.nist.pededitor;

/** Exception type for inverse transforms that have no solutions */
public class ZeroSolutionsException extends UnsolvableException {
   ZeroSolutionsException() {}
   ZeroSolutionsException(String message) {
      super(message);
   }
}
