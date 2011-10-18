package gov.nist.pededitor;

/** Exception type for inverse transforms that have an infinite number of solutions */
public class InfiniteSolutionsException extends UnsolvableException {
   InfiniteSolutionsException() {}
   InfiniteSolutionsException(String message) {
      super(message);
   }
}
