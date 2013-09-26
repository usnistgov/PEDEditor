package gov.nist.pededitor;

import javax.swing.JComboBox;

/** A simple ComboBox to list variables. */

public class FunctionSelector extends JComboBox<String> {
    private static final long serialVersionUID = -6731864317879982175L;

    {
        for (StandardRealFunction f: StandardRealFunction.values()) {
            addItem("<html>" + f.getText());
        }
    }

    public FunctionSelector() {}

    public void setSelected(StandardRealFunction f) {
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

    public RealFunction getSelected() {
        Object obj = getSelectedItem();
        if (obj == null) {
            return null;
        }
        String s = (String) obj;
        for (StandardRealFunction f: StandardRealFunction.values()) {
            if (s.equals("<html>" + f.getText())) {
                return f.getFunction();
            }
        }

        return null;
    }
}
