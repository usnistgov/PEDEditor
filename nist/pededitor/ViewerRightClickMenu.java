package gov.nist.pededitor;

import javax.swing.JLabel;


/** PED Viewer popup menu. */
@SuppressWarnings("serial")
public class ViewerRightClickMenu extends BasicRightClickMenu {
    Editor mEditor;
    JLabel coordinates;

    public ViewerRightClickMenu(Editor editor) {
        super(editor);
        EditFrame ef = getEditFrame();
        add(ef.actDeselect);
        add(getEditFrame().actSelectNearestPoint);
        add(getEditFrame().actSelectNearestCurve);
        add(ef.mnJump);
        add(ef.mnStep);
        add(ef.actCenterMouse);
        addSeparator();
        coordinates = createCoordinatesLabel();
        add(ef.actCopyStatusBar);
        add(coordinates);
    }

    @Override public void setCoordinates(String s) {
        coordinates.setText(mungeCoordinates(s));
    }
}
