package gov.nist.pededitor;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Stupid class to marginally simplify use of GridBagLayout in a
    simple consistent style. */
public class GridBagUtil {
    private Insets insets = new Insets(0, 3, 0, 3);

    public static final GridBagConstraints east = new GridBagConstraints();
    public static final GridBagConstraints west = new GridBagConstraints();
    public static final GridBagConstraints endRow = new GridBagConstraints();
    public static final GridBagConstraints endRowCentered
        = new GridBagConstraints();

    {
        east.anchor = GridBagConstraints.EAST;
        east.insets = insets;

        west.anchor = GridBagConstraints.WEST;
        west.insets = insets;

        endRow.anchor = GridBagConstraints.WEST;
        endRow.gridwidth = GridBagConstraints.REMAINDER;
        endRow.insets = insets;

        endRowCentered.anchor = GridBagConstraints.CENTER;
        endRowCentered.gridwidth = GridBagConstraints.REMAINDER;
        endRowCentered.insets = insets;
    }

    public Container parent;
    public GridBagLayout gbl;

    public GridBagUtil(Container parent) {
        this.parent = parent;

        gbl = new GridBagLayout();
        parent.setLayout(gbl);
    }

    void add(Container child, GridBagConstraints gbc) {
        gbl.setConstraints(child, gbc);
        parent.add(child);
    }

    void addEast(Container child) {
        add(child, east);
    }

    void addWest(Container child) {
        add(child, west);
    }

    void endRowWith(Container child) {
        add(child, endRow);
    }

    void centerAndEndRow(Container child) {
        add(child, endRowCentered);
    }
}