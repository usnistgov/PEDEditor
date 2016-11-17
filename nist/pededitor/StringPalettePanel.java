/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;


/** A ButtonsPanel whose buttons generate StringEvents. */
public class StringPalettePanel extends ButtonsPanel {
    private static final long serialVersionUID = 4572788761165949707L;
    StringPalette palette;

    class StringAction extends AbstractAction {
        private static final long serialVersionUID = 3954492051851940113L;

        int index;

        StringAction(Object label, int index) {
            putValue((label instanceof Icon) ? Action.SMALL_ICON
                     : Action.NAME,
                     label);
            this.index = index;
        }

        @Override public void actionPerformed(ActionEvent e) {
            for (StringEventListener listener: stringEventListeners) {
                listener.actionPerformed
                    (new StringEvent(StringPalettePanel.this, palette.get(index)));
            }
        }
    }

    ArrayList<StringEventListener> stringEventListeners
        = new ArrayList<StringEventListener>();

    /** Enable "listen" to receive StringEvent events when the user
        presses one of the buttons in the palette. */
    public void addListener(StringEventListener listen) {
        stringEventListeners.add(listen);
    }

    public void removeListener(StringEventListener listen) {
        stringEventListeners.remove(listen);
    }

    /** Create a panel full of buttons with the appropriate labels that
        emit the appropriate string when pressed.

        @param colCnt Maximum number of buttons per row, or 0 if no limit.
    */
    public StringPalettePanel(StringPalette palette, int colCnt) {
        this(palette, colCnt, null);
    }

    /** Create a panel full of buttons with the appropriate labels that
        emit the appropriate string when pressed.

        @param colCnt Maximum number of buttons per row, or 0 if no limit.

        @param font Font to use for the buttons.
    */
    public StringPalettePanel(StringPalette palette, int colCnt, Font font) {
        super(colCnt, font);
        this.palette = palette;
        int cnt = palette.size();

        for (int i = 0; i < cnt; ++i) {
            Object label = palette.getLabel(i);
            if (label == null) {
                newSet();
            } else {
                addButton(new JButton(new StringAction(label, i)));
            }
        }
    }
}
