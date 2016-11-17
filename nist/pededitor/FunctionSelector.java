/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.function.DoubleUnaryOperator;

import javax.swing.JComboBox;

/** A simple ComboBox to list variables. */

public class FunctionSelector extends JComboBox<String> {
    private static final long serialVersionUID = -6731864317879982175L;

    {
        for (StandardDoubleUnaryOperator f: StandardDoubleUnaryOperator.values()) {
            addItem("<html>" + f.getText());
        }
    }

    public FunctionSelector() {}

    public void setSelected(StandardDoubleUnaryOperator f) {
        String name = "<html>" + f.getText();
        int cnt = getItemCount();
        for (int i = 0; i < cnt; ++i) {
            String s = getItemAt(i);
            if (s.equals(name)) {
                setSelectedIndex(i);
                return;
            }
        }
        throw new RuntimeException("Function '" + f + "' not found");
    }

    public DoubleUnaryOperator getSelected() {
        Object obj = getSelectedItem();
        if (obj == null) {
            return null;
        }
        String s = (String) obj;
        for (StandardDoubleUnaryOperator f: StandardDoubleUnaryOperator.values()) {
            if (s.equals("<html>" + f.getText())) {
                return f;
            }
        }

        return null;
    }
}
