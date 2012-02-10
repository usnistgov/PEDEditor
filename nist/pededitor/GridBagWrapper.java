package gov.nist.pededitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class GridBagWrapper {
    public Container parent;
    public GridBagLayout gbl;

    public GridBagWrapper(Container parent) {
        this.parent = parent;

        gbl = new GridBagLayout();
        parent.setLayout(gbl);
    }

    void add(Component child, GridBagConstraints gbc) {
        gbl.setConstraints(child, gbc);
        parent.add(child);
    }
}