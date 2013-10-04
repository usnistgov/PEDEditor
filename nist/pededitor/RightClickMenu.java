package gov.nist.pededitor;

import javax.swing.JMenu;


/** PED Editor popup menu. */
@SuppressWarnings("serial")
public class RightClickMenu extends BasicRightClickMenu {
    Editor mEditor;
    JMenu mnEdit = new JMenu("Edit Selection");

    public RightClickMenu(Editor editor) {
        super(editor);
        EditFrame ef = getEditFrame();
        add(ef.actDeselect);
        mnEdit.setEnabled(false);
        mnEdit.add(ef.actCopy);
        mnEdit.add(ef.actCopyRegion);
        mnEdit.add(ef.actMoveSelection);
        mnEdit.add(ef.actMovePoint);
        mnEdit.add(ef.actMoveRegion);
        add(mnEdit);
        addSeparator();
        add(ef.mnJump);
        add(ef.mnMove);
        add(new PositionMenu(getEditor()));
        addSeparator();
        // If one does shift right-click, then the following two items
        // are identical, which can get a bit confusing.
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

    @Override public void setHasSelection(boolean b) {
        mnEdit.setEnabled(b);
    }

    @Override public void setEditable(boolean b) {
        mnEdit.setVisible(b);
    }
}
