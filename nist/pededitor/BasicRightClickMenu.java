package gov.nist.pededitor;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/** Shared features of the editor and viewer popup menus. */
@SuppressWarnings("serial")
public class BasicRightClickMenu extends JPopupMenu {
    Editor mEditor;

    public BasicRightClickMenu(Editor editor) {
        mEditor = editor;

        addPopupMenuListener(new PopupMenuListener() {
                @Override public void popupMenuCanceled(PopupMenuEvent e) {
                    getEditor().rightClick = null;
                }
                @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }
                @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }
            });
    }

    public Editor getEditor() {
        return mEditor;
    }

    protected EditFrame getEditFrame() {
        return getEditor().getEditFrame();
    }
}
