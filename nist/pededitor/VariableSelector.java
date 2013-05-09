package gov.nist.pededitor;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComboBox;

/** A simple ComboBox to list variables. */

public class VariableSelector extends JComboBox<String> {
    private static final long serialVersionUID = 1038468375401505149L;

    public void setAxes(ArrayList<LinearAxis> axes) {
        String[] variables = new String[axes.size()];
        removeAllItems();
        int i = -1;
        for (Axis axis: axes) {
            ++i;
            variables[i] = (String) axis.name;
        }
        Arrays.sort(variables);

        for (String s: variables) {
            addItem(s);
        }
    }

    public void setSelected(LinearAxis axis) {
        String name = (String) axis.name;
        int cnt = getItemCount();
        for (int i = 0; i < cnt; ++i) {
            String s = getItemAt(i);
            if (s.equals(name)) {
                setSelectedIndex(i);
                return;
            }
        }
        throw new RuntimeException("Axis '" + name + "' not found");
    }

    public LinearAxis getSelected(ArrayList<LinearAxis> axes) {
        String s = getItemAt(getSelectedIndex());
        int cnt = getItemCount();
        for (int i = 0; i < cnt; ++i) {
            LinearAxis axis = axes.get(i);
            if (s.equals((String) axis.name)) {
                return axis;
            }
        }
        return null;
    }
}
