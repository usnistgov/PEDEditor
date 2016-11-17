/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Stupid class to marginally simplify use of GridBagLayout in a
    simple consistent style. */
public class GridBagUtil extends GridBagWrapper {
    public static final Insets insets = new Insets(0, 3, 0, 3);

    public static final GridBagConstraints east = new GridBagConstraints();
    public static final GridBagConstraints west = new GridBagConstraints();
    public static final GridBagConstraints northwest = new GridBagConstraints();
    public static final GridBagConstraints endRow = new GridBagConstraints();
    public static final GridBagConstraints endRowCentered
        = new GridBagConstraints();

    static {
        east.anchor = GridBagConstraints.EAST;
        east.insets = insets;

        west.anchor = GridBagConstraints.WEST;
        west.insets = insets;

        northwest.anchor = GridBagConstraints.NORTHWEST;
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

    /** Use this only if you know that parent has already been
        assigned a GridBagLayout and you want to retrieve it. */
    public static GridBagUtil getLayout(Container parent) {
        return new GridBagUtil(parent, (GridBagLayout) parent.getLayout());
    }

    GridBagUtil(Container parent, GridBagLayout gbl) {
        super(parent, gbl);
    }

    void addEast(Component child) {
        add(child, east);
    }

    void addWest(Component child) {
        add(child, west);
    }

    void addNorthwest(Component child) {
        add(child, northwest);
    }

    void endRowWith(Component child) {
        add(child, endRow);
    }

    void centerAndEndRow(Component child) {
        add(child, endRowCentered);
    }
}
