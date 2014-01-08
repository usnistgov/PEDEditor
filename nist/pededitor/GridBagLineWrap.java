/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;

/** Stupid class that supports only one kind of GridBagConstraint,
    with the exception that a new line is started if the number of
    elements in a single row would otherwise exceed getColumnCnt(),
    and the endRow() method exists to force a line break between the
    previous item added and the next item added.

    If you use LineWrapGridBag, then use it for all of the parent
    container's layout. This class also gets confused if you remove()
    components or call add() by some other path.
 */
public class GridBagLineWrap {
    GridBagWrapper gb;
    GridBagConstraints gbc;
    GridBagConstraints gbcEndRow;

    int colCnt = -1;
    int col = 0;

    /** @param parent The container that this LineWrapGridBag should
        manage.

        @param gbc The GridBagConstraints to use for a typical
        element. This GridBagConstraints should place the item in the
        next column to the right of the previous item.
       
        @param colCnt The grid bag will contain no more than colCnt
        columns. */
    public GridBagLineWrap(Container parent, GridBagConstraints gbc,
                           int colCnt) {
        this.colCnt = colCnt;
        gb = new GridBagWrapper(parent);
        this.gbc = (GridBagConstraints) gbc.clone();
        gbcEndRow = (GridBagConstraints) gbc.clone();
        gbcEndRow.gridwidth = GridBagConstraints.REMAINDER;
    }

    public void add(Container child) {
        if (col + 1 == colCnt) {
            gb.add(child, gbcEndRow);
            col = 0;
        } else {
            gb.add(child, gbc);
            ++col;
        }
    }

    public void endRow() {
        // Retroactively switch the GridBagConstraints for the
        // previous item to gbcEndRow.
        int cnt = gb.parent.getComponentCount();
        if (cnt > 0) {
            Component child = gb.parent.getComponent(cnt - 1);
            gb.parent.remove(cnt - 1);
            gb.add(child, gbcEndRow);
        }
        col = 0;
    }
}
