/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Component;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenu;


/** PED BasicEditor popup menu. */
@SuppressWarnings("serial")
public class RightClickMenu extends BasicRightClickMenu {
    JMenu mnEdit = new JMenu("Edit");
    JMenu mnEditSel = new JMenu("Edit selection");
    { mnEditSel.setMnemonic(KeyEvent.VK_E); }
    JMenu mnEditNear = new JMenu("Edit nearest item");
    { mnEditNear.setMnemonic(KeyEvent.VK_E); }
    JMenu mnLayer = null;
    JMenu mnDecorations;
    JLabel coordinates = new JLabel();

    static private void addit(JMenu m, Object... os) {
        for (Object obj: os) {
            if (obj instanceof Action) {
                m.add((Action) obj);
            } else {
                m.add((Component) obj);
            }
        }
    }

    public RightClickMenu(BasicEditor editor) {
        super(editor);
        EditFrame ef = getEditFrame();
        add(ef.actDeselect);

        addit(mnEdit, ef.actUndo, ef.actRedo, ef.actPaste, ef.actCutAll);

        addit(mnEditSel,
            ef.actColor,
              ef.actCopy,
              ef.actCut,
              ef.actCutRegion,
              ef.actRemoveSelection,
              ef.actMoveSelection,
              ef.actMovePoint,
              ef.actEditSelection,
              ef.actResetToDefault,
                ef.actMakeDefault);

        addit(mnEditNear, ef.actColor, ef.actCopy, ef.actCut, ef.actCutRegion,
                ef.actRemoveSelection, ef.actEditSelection,
                ef.actResetToDefault, ef.actMakeDefault);

        addSeparator();
        add(new PositionMenu(getEditor()));
        add(ef.mnJump);
        add(ef.mnStep);

        JMenu mnView = new JMenu("View");
        mnView.setMnemonic(KeyEvent.VK_V);
        mnView.add(ef.actZoomIn);
        mnView.add(ef.actZoomOut);
        mnView.add(ef.actCenterMouse);
        add(mnView);

        addSeparator();
        // If one does shift right-click, then the following two items
        // are identical, which can get a bit confusing.
        add(ef.actAddVertex);
        add(ef.actAddAutoPositionedVertex);
        add(mnDecorations = ef.createDecorationsMenu());
        add(mnEdit);
        add(mnEditSel);
        add(mnEditNear);
        mnLayer = ef.createLayerMenu();
        add(mnLayer);
        addSeparator();
        coordinates = createCoordinatesLabel();
        add(ef.actCopyStatusBar);
        add(coordinates);

        setHasSelection(false);
    }

    @Override public void setHasSelection(boolean b) {
        if (getEditor().isEditable()) {
            mnEditSel.setVisible(b);
            mnEditNear.setVisible(!b);
        }
    }

    @Override public void setCoordinates(String s) {
        coordinates.setText(mungeCoordinates(s));
    }

    @Override public void setEditable(boolean b) {
        mnDecorations.setVisible(b);
        setHasSelection(getEditor().getSelection() != null);
        if (!b) {
            mnEditSel.setVisible(false);
            mnEditNear.setVisible(false);
        }
    }
}
