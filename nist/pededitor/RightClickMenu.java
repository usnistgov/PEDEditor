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
    JMenu mnEditSel = new JMenu("Edit selection");
    { mnEditSel.setMnemonic(KeyEvent.VK_E); }
    JMenu mnEditNear = new JMenu("Edit nearest item");
    { mnEditNear.setMnemonic(KeyEvent.VK_E); }
    JMenu mnLayer = null;
    JMenu mnDecorations;
    JLabel coordinates = new JLabel();

    public RightClickMenu(BasicEditor editor) {
        super(editor);
        EditFrame ef = getEditFrame();
        add(ef.actDeselect);

        for (Object obj: new Object[]
            { ef.actColor,
              ef.actCopy,
              ef.actCut,
              ef.actCutRegion,
              ef.actRemoveSelection,
              ef.actMoveSelection,
              ef.actMovePoint,
              ef.actPaste,
              ef.actEditSelection,
              ef.actResetToDefault,
              ef.actMakeDefault }) {
            if (obj instanceof Action) {
                mnEditSel.add((Action) obj);
            } else {
                mnEditSel.add((Component) obj);
            }
        }

        for (Object obj : new Object[] { ef.actColor, ef.actCut, ef.actRemoveSelection, ef.actEditSelection,
                ef.actResetToDefault, ef.actMakeDefault }) {
            if (obj instanceof Action) {
                mnEditNear.add((Action) obj);
            } else {
                mnEditNear.add((Component) obj);
            }
        }

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
