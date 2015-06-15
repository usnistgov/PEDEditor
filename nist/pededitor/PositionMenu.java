/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/** Position submenu of the popup menu. */
@SuppressWarnings("serial")
public class PositionMenu extends JMenu {
    BasicEditor mEditor;

    public PositionMenu(BasicEditor editor) {
        super("Position");
        setMnemonic(KeyEvent.VK_P);
        mEditor = editor;
        add(getEditFrame().actAutoPosition);
        add(getEditFrame().actNearestGridPoint);
        add(getEditFrame().actNearestPoint);
        add(getEditFrame().actNearestCurve);
        add(getEditFrame().actSelectNearestPoint);
        add(getEditFrame().actSelectNearestCurve);
    }

    public BasicEditor getParentEditor() {
        return mEditor;
    }

    protected EditFrame getEditFrame() {
        return getParentEditor().getEditFrame();
    }


    void add(AbstractAction act) {
        add(new JMenuItem(act));
    }
}
