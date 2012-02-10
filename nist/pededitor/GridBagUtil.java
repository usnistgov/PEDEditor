package gov.nist.pededitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/** Stupid class to marginally simplify use of GridBagLayout in a
    simple consistent style. */
public class GridBagUtil extends GridBagWrapper {
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

    public GridBagUtil(Container parent) {
        super(parent);
    }

    void addEast(Component child) {
        add(child, east);
    }

    void addWest(Component child) {
        add(child, west);
    }

    void endRowWith(Component child) {
        add(child, endRow);
    }

    void centerAndEndRow(Component child) {
        add(child, endRowCentered);
    }
}