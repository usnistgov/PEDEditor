/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

interface CurveCloseable {
    boolean isClosed();
    void setClosed(boolean b);
}
