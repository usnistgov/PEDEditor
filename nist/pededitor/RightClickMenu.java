package gov.nist.pededitor;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/** Popup menu that pops up when the user right-clicks on a point in
    the diagram. */
@SuppressWarnings("serial")
public class RightClickMenu extends JPopupMenu {
    Editor mEditor;

    public RightClickMenu(Editor editor) {
        mEditor = editor;
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

        addPopupMenuListener(new PopupMenuListener() {
                @Override public void popupMenuCanceled(PopupMenuEvent e) {
                    System.out.println("Canceled."); // UNDO
                    getEditor().rightClick = null;
                }
                @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }
                @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }
            });
    }

    public void show(Editor.MousePress mp) {
        if (getEditor().rightClick == null) {
            getEditor().rightClick = mp;
        }
        show(mp.e.getComponent(), mp.e.getX(), mp.e.getY());
    }

    public Editor getEditor() {
        return mEditor;
    }

    protected EditFrame getEditFrame() {
        return getEditor().getEditFrame();
    }
}
