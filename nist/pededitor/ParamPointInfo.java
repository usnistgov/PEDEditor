/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Information about a point on a parameterized curve built up from
    control points. */
public class ParamPointInfo {
    public ParamPointInfo() {
    }
    
    public ParamPointInfo(double t, int index, boolean beforeIndex) {
        this.t = t;
        this.index = index;
        this.beforeIndex = beforeIndex;
    }
    
    public double t = Double.NaN;
    /** Index of the closest control point. */    
    public int index = -1;
    /** True if t appears between index and the previous control
        point, false if t appears after, programmer's choice if an
        actual control point. */
    public boolean beforeIndex = false;
}
