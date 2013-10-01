package gov.nist.pededitor;


/** PED Viewer popup menu. */
@SuppressWarnings("serial")
public class ViewerRightClickMenu extends BasicRightClickMenu {
    Editor mEditor;

    public ViewerRightClickMenu(Editor editor) {
        super(editor);
        EditFrame ef = getEditFrame();
        add(ef.mnDeselect.getAction());
        add(ef.mnJump);
        add(ef.mnMove);
        add(getEditFrame().actSelectNearestPoint);
        add(getEditFrame().actSelectNearestCurve);
        add(ef.actCopyStatusBar);
        add(ef.actCenterMouse);
    }
}
