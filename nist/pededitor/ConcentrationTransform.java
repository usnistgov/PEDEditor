/* Eric Boesch, NIST Materials Measurement Laboratory, 2016. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Class to perform concentration transformations.

    A concentration is a vector <x_1, ..., x_n> such that each x_i >=
    0 and sum_{i=0}^{i=n-1} x_i <= 1. There is an implicit x_{n+1}
    element that equals one minus the sum of all other values.
 */
interface ConcentrationTransform {
    ConcentrationTransform createInverse();
    /** Transform the given coordinates in place. */
    void transform(double[] values);
    int componentCnt();
}
