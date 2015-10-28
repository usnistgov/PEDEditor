/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.List;
import java.util.Arrays;

import javax.swing.JComboBox;

/** A simple ComboBox to list variables. */

public class VariableSelector extends JComboBox<String> {
    private static final long serialVersionUID = 1038468375401505149L;

    public void setAxes(List<LinearAxis> axes) {
        String name = getSelectedName();
        removeAllItems();
        String[] variables = new String[axes.size()];
        int i = -1;
        for (Axis axis: axes) {
            ++i;
            variables[i] = (String) axis.name;
        }
        Arrays.sort(variables);

        for (String s: variables) {
            addItem(s);
        }

        if (name != null) {
            try {
                setSelected(name);
            } catch (RuntimeException x) {
                // OK if formerly selected name is no longer available
            }
        }
    }

    public void setSelected(String name) {
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

    public void setSelected(LinearAxis axis) {
        setSelected((String) axis.name);
    }

    public LinearAxis getSelected(List<LinearAxis> axes) {
        String name = getSelectedName();
        if (name == null) {
            return null;
        }
        for (LinearAxis axis: axes) {
            if (name.equals((String) axis.name)) {
                return axis;
            }
        }
        return null;
    }

    public String getSelectedName() {
        Object obj = getSelectedItem();
        return (obj == null) ? null : (String) obj;
    }
        
}
