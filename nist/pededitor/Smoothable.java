/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public interface Smoothable {
    boolean isSmoothed(int vertexNo);
    void setSmoothed(int vertexNo, boolean value);
    default void toggleSmoothed(int vertexNo) {
        setSmoothed(vertexNo, !isSmoothed(vertexNo));
    }
}
