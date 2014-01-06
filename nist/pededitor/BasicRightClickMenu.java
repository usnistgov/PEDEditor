package gov.nist.pededitor;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/** Shared features of the editor and viewer popup menus. */
@SuppressWarnings("serial")
public class BasicRightClickMenu extends JPopupMenu {
    BasicEditor mEditor;

    public BasicRightClickMenu(BasicEditor editor) {
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

    public BasicEditor getEditor() {
        return mEditor;
    }

    protected EditFrame getEditFrame() {
        return getEditor().getEditFrame();
    }

    /** Call this when the user selects or deselects something. */
    public void setHasSelection(boolean b) {
    }

    /** Call this when the user checks or unchecks the "Show Editing
        Options" box. */
    public void setEditable(boolean b) {
    }

    String mungeCoordinates(String s) {
        return "  " + s;
    }

    public JLabel createCoordinatesLabel() {
        JLabel res = new JLabel();
        res.setFont(res.getFont().deriveFont(Font.PLAIN));
        return res;
    }

    public void setCoordinates(String coord) {
    }
}
