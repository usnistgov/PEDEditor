/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.text.NumberFormat;

import javax.swing.JTextField;

/** A text field for entering numbers. Percent signs and fractions are
    allowed. The value displayed to the user may leave out some
    trailing decimals in the input value, but if the user does not
    alter the text, then the value returned will exactly equal the
    original value, including the hidden digits. */
class NumberField extends JTextField {
    private static final long serialVersionUID = 6579379135556549548L;

    public NumberFormat format = null;
    String oldString = null;
    double oldV = 0;
    boolean mIsPercentage = false;

    public NumberField(int size) {
        super(size);
    }

    /** Calling setPercentage() removes your format setting (if you
        want to specify the format AND you want to show percentages,
        then just choose a percentage format.). */
    public void setPercentage(boolean b) {
        format = null;
        mIsPercentage = b;
        refresh();
    }

    public boolean isPercentage() {
        return mIsPercentage;
    }

    public void setValue(double v) {
        String s = (format == null)
            ? ContinuedFraction.toString(v, isPercentage())
            : format.format(v);
        setValueAndText(v, s);
    }

    @Override public void setText(String s) {
        // Setting oldString to null guarantees that getValue() will
        // obtain its value by parsing getText(), not by using oldV.
        oldString = null;
        super.setText(s);
    }

    /** Set both the text of the field and its exact value. For
        instance, setValueAndText(0.239, "0.24") will display "0.24"
        but cause getValue() to return 0.239 unless the user changes
        the displayed value. */
    public void setValueAndText(double v, String s) {
        oldV = v;
        oldString = s;
        super.setText(s);
    }

    /** If you never call setFormat(), or if you call setFormat(null),
        then values will be formatted with
        ContinuedFraction.toString(). Whether percentages are
        displayed will be determined by the setting of
        isPercentage(). */
    public void setFormat(NumberFormat f) {
        format = f;
        refresh();
    }

    /** Update the field text to reflect formatting changes. */
    void refresh() {
        try {
            setValue(getValue());
        } catch (NumberFormatException x) {
            // That's OK; leave the value in the text box as-is.
        }
    }

    public double getValue() throws NumberFormatException {
        return getText().trim().equals(oldString) ? oldV
            : ContinuedFraction.parseDouble(getText());
    }
}
