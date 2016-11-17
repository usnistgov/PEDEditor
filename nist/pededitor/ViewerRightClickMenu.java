/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import javax.swing.JLabel;


/** PED Viewer popup menu. */
@SuppressWarnings("serial")
public class ViewerRightClickMenu extends BasicRightClickMenu {
    BasicEditor mEditor;
    JLabel coordinates;

    public ViewerRightClickMenu(BasicEditor editor) {
        super(editor);
        EditFrame ef = getEditFrame();
        add(ef.actDeselect);
        add(getEditFrame().actSelectNearestPoint);
        add(getEditFrame().actSelectNearestCurve);
        add(ef.mnJump);
        add(ef.mnStep);
        addSeparator();
        coordinates = createCoordinatesLabel();
        add(ef.actCopyStatusBar);
        add(coordinates);
    }

    @Override public void setCoordinates(String s) {
        coordinates.setText(mungeCoordinates(s));
    }
}
