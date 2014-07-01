/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class GridBagWrapper {
    public Container parent;
    public GridBagLayout gbl;

    public GridBagWrapper(Container parent) {
        this(parent, new GridBagLayout());
    }

    GridBagWrapper(Container parent, GridBagLayout gbl) {
        this.parent = parent;
        this.gbl = gbl;
        parent.setLayout(gbl);
    }

    /** Use this only if you know that parent has already been
        assigned a GridBagLayout and you want to retrieve it. */
    public static GridBagWrapper getLayout(Container parent) {
        return new GridBagWrapper(parent, (GridBagLayout) parent.getLayout());
    }

    void add(Component child, GridBagConstraints gbc) {
        gbl.setConstraints(child, gbc);
        parent.add(child);
    }
}
