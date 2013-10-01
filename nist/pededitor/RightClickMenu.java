package gov.nist.pededitor;


/** PED Editor popup menu. */
@SuppressWarnings("serial")
public class RightClickMenu extends BasicRightClickMenu {
    Editor mEditor;

    public RightClickMenu(Editor editor) {
        super(editor);
        EditFrame ef = getEditFrame();
        add(ef.mnDeselect.getAction());
        addSeparator();
        add(ef.mnJump);
        add(ef.mnMove);
        add(new PositionMenu(getEditor()));
        addSeparator();
        add(ef.actMoveSelection);
        add(ef.actMovePoint);
        add(ef.actMoveRegion);
        addSeparator();
        add(ef.actAddVertex);
        add(ef.actAddAutoPositionedVertex);
        addSeparator();
        add(ef.actText);
        add(ef.actLeftArrow);
        add(ef.actRightArrow);
        addSeparator();
        add(ef.actCopyStatusBar);
        add(ef.actCenterMouse);
    }
}
