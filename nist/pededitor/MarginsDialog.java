/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

/** GUI for selecting where data should come from/go to and which
    variables should be input/output. */
public class MarginsDialog extends JDialog {

    private static final long serialVersionUID = -9088557283490153600L;
    
    public AffineTransform toPage;
    public AffineTransform fromPage;
    List<LinearAxis> axes;

    /** The variable selectors for the horizontal and vertical axes,
        respectively. */
    VariableSelector[] variables = { new VariableSelector(),
                                   new VariableSelector() };
    /** table[0] is the row that holds the min and max values of the
        horizontal variable, and table[1] holds the min and max values
        of the vertical variable. */
    NumberField[][] table = new NumberField[2][2];
    JButton okButton =  new JButton(new AbstractAction("OK") {
            private static final long serialVersionUID = 1836544202745825507L;

            @Override public void actionPerformed(ActionEvent e) {
                    MarginsDialog.this.pressedOK = true;
                    setVisible(false);
            }
        });
    transient boolean pressedOK = false;

    public MarginsDialog(Frame owner, int numberFieldSize) {
        super(owner, "Set diagram bounds", false);
        GridBagUtil gb = new GridBagUtil(getContentPane());

        gb.addWest(new JLabel());
        gb.addWest(new JLabel("Minimum"));
        gb.endRowWith(new JLabel("Maximum"));

        int rowCnt = table.length;
        for (int i = 0; i < rowCnt; ++i) {
            for (int j = 0; j < table[i].length; ++j) {
                table[i][j] = new NumberField(numberFieldSize);
            }
            gb.addEast(variables[i]);
            variables[i].addItemListener(e -> variableChanged(e));
            gb.addWest(table[i][0]);
            gb.endRowWith(table[i][1]);
        }

        gb.centerAndEndRow(okButton);
        getRootPane().setDefaultButton(okButton);
        setResizable(false);
    }

    public static void orderRange(double[] range) {
        if (range[0] > range[1]) {
            double tmp = range[0];
            range[0] = range[1];
            range[1] = tmp;
        }
    }

    /** Make the range of values in the number field for varNo
        correspond to pageMin...pageMax. For example, if varNo == 1
        and getVariable(1) == "T", then the range of T values
        presented to the user will be the T values that correspond to
        the page Y values pageMin and pageMax respectively -- but swap
        min T and max T if they end up out of order, with min greater
        than max. */
    void setPageRange(int varNo, double pageMin, double pageMax) {
        double[] range = {pageMin, pageMax};
        for (int i = 0; i < 2; ++i) {
            Double d = pageToAxisValue(range[i], getVariable(varNo));
            if (d == null) {
                return;
            }
            range[i] = d;
        }
        orderRange(range);
        for (int i = 0; i < 2; ++i) {
            getNumberField(varNo, i).setValue(range[i]);
        }
    }

    void variableChanged(ItemEvent e) {
        int varNo = (e.getSource() == variables[0]) ? 0 : 1;
        String name = (String) e.getItem();
        LinearAxis axis = null;
        for (LinearAxis axis1: axes) {
            if (name.equals((String) axis1.name)) {
                axis = axis1;
                break;
            }
        }
        if (axis == null) {
            return;
        }

        try {

            if (e.getStateChange() == ItemEvent.DESELECTED) {
                // Store the min and max page X or Y (for varNo == 0 or 1
                // respectively) values in the number fields.
                for (int col = 0; col < 2; ++col) {
                    getNumberField(varNo, col).setValue(pageValue(varNo, col, axis));
                }
            } else { // ItemEvent.SELECTED

                // If the number fields are not empty, assume they are
                // page values, and convert the range they represent to axis values.

                double[] range = new double[2];
                for (int col = 0; col < 2; ++col) {
                    range[col] = getNumberField(varNo, col).getValue();
                }

                setPageRange(varNo, range[0], range[1]);
            }
        } catch (NumberFormatException x) {
            // Do nothing
        }
    }
    
    public LinearAxis getVariable(int varNo) {
        return getVariableSelector(varNo).getSelected(axes);
    }

    public VariableSelector getVariableSelector(int varNo) {
        return variables[varNo];
    }

    public void setVariable(int varNo, LinearAxis axis) {
        variables[varNo].setSelected(axis);
    }

    /** Return the number field corresponding to the value of var
        varNo (0 = page X, 1 = page Y) column col (0 = min value
        number field, 1 = max). */
    public NumberField getNumberField(int varNo, int col) {
        return table[varNo][col];
    }

    /** Return the standard page coordinate (x for varNo == 0, y for
        varNo == 1) corresponding to the value of var varNo's column col
        (0 = min value number field, 1 = max). */
    public double pageValue(int varNo, int col)
        throws NumberFormatException {
        return pageValue(varNo, col, getVariable(varNo));
    }

    double pageValue(int varNo, int col, LinearAxis axis)
        throws NumberFormatException {
        Point2D.Double g = axis.gradient(fromPage);
        double m = (varNo == 0) ? g.x : g.y;
        Point2D.Double pOrigin = new Point2D.Double(0,0);
        fromPage.transform(pOrigin, pOrigin);
        double b = axis.applyAsDouble(pOrigin);
        double axisV = getNumberField(varNo, col).getValue();

        // m pageV + b = axisV ; solve for pageV
        return (axisV - b) / m;
    }

    public double[] pageRange(int varNo) throws NumberFormatException {
        double[] range = { pageValue(varNo, 0), pageValue(varNo, 1) };
        orderRange(range);
        return range;
    }

    public double[] pageXRange() throws NumberFormatException {
        return pageRange(0);
    }

    public double[] pageYRange() throws NumberFormatException {
        return pageRange(1);
    }

    public Double pageToAxisValue(double v, LinearAxis axis) {
        if (axis == null) {
            return null;
        } else {
            Point2D.Double vp = new Point2D.Double(v, v);
            return axis.applyAsDouble(fromPage.transform(vp, vp));
        }
    }

    /** Show the dialog as document-modal. Return false if the dialog
        was closed abnormally. */
    public boolean showModal() {
        pack();
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        setVisible(true);
        return pressedOK;
    }

    public double[] getRange(int varNo) throws NumberFormatException {
        return new double[] { getNumberField(varNo, 0).getValue(),
                              getNumberField(varNo, 1).getValue() };
    }

    /* defaults are the values that are to be initiially selected for
       the different variable columns. But if the previously selected
       values, if any, for these columns are still accessible, then
       those previous values take precedence over v1 and v2. */
    public void setAxes(ArrayList<LinearAxis>[] axes,
                        LinearAxis[] defaults) {
        int varNo = -1;
        for (VariableSelector vs: variables) {
            ++varNo;
            vs.setAxes(axes[varNo]);
            LinearAxis v = vs.getSelected(axes[varNo]);
            if (v == null) {
                vs.setSelected(defaults[varNo]);
            }
        }
    }
}
