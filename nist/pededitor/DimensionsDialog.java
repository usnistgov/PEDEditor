/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Frame;
import java.util.Arrays;

/** Dialog to ask for the length of two axes. */
public class DimensionsDialog extends NumberColumnDialog {

    private static final long serialVersionUID = -1831860481483477895L;

    static final String intro = 
"<p>Enter the fraction of the domain that the region you selected " +
"covers. For example, for a lower right partial ternary diagram whose " +
"bottom axis covers the range from 30% to 100% of the bottom right " +
"component, enter 70% for the bottom side. " +

"<p>If you wish to use different scales for the two axes, you may need " +
"to enter the apparent axis length instead. For example, if the top " +
"component in the diagram ranges from only 0-1%, but it " +
"takes up as much room in the displayed image as if it ranged from " +
"0-10%, then enter 10% for the axis length. Later, you can rescale the " +
"Y axis (Properties/Scale/Y axis/top component) to " +
"change the top Y value from 10% to the true value of 1%. " +

"<p>You may enter values as decimals, fractions, or percentages, but if " +
"you use percentages, do not omit the percent sign.";
    DimensionsDialog(Frame owner, double[] values, String[] labels) {
        super(owner, values, labels, Stuff.htmlify(intro));
        setTitle("Select Axis Lengths");
    }

    public static void main(String[] args) {
        DimensionsDialog dog = new DimensionsDialog
            (null, new double[] {1, 0.5},  new String[] {"Right", "Bottom"});
        dog.setPercentage(true);
        double[] values = dog.showModalColumn();
        System.out.println(Arrays.toString(values));
    }
   
}
