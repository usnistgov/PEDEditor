/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;


/** A ButtonsPanel whose buttons generate DelimiterEvents. */
public class DelimiterPalettePanel extends ButtonsPanel {
    private static final long serialVersionUID = -7265299416396566730L;
    DelimiterPalette palette;

    ArrayList<DelimiterEventListener> delimiterEventListeners
        = new ArrayList<DelimiterEventListener>();

    class DelimiterAction extends AbstractAction {
        private static final long serialVersionUID = -7205094586111632209L;
        Delimiter delimiter;

        DelimiterAction(String label, Delimiter delimiter) {
            super(label);
            this.delimiter = delimiter;
        }

        @Override public void actionPerformed(ActionEvent e) {
            for (DelimiterEventListener listener: delimiterEventListeners) {
                listener.actionPerformed
                    (new DelimiterEvent(DelimiterPalettePanel.this, delimiter));
            }
        }
    }


    /** Enable "listen" to receive DelimiterEvent events when the user
        presses one of the buttons in the palette. */
    public void addListener(DelimiterEventListener listen) {
        delimiterEventListeners.add(listen);
    }

    public void removeListener(DelimiterEventListener listen) {
        delimiterEventListeners.remove(listen);
    }

    /** Create a panel full of buttons with the appropriate labels that
        emit the appropriate string when pressed.

        @param colCnt Maximum number of buttons per row, or 0 if no limit.
    */
    public DelimiterPalettePanel(DelimiterPalette palette, int colCnt) {
        this(palette, colCnt, null);
    }

    /** Create a panel full of buttons with the appropriate labels that
        emit the appropriate string when pressed.

        @param colCnt Maximum number of buttons per row, or 0 if no limit.

        @param font Font to use for the buttons.
    */
    public DelimiterPalettePanel(DelimiterPalette palette, int colCnt, Font font) {
        super(colCnt, font);
        this.palette = palette;
        int cnt = palette.size();

        for (int i = 0; i < cnt; ++i) {
            String label = palette.getLabel(i);
            if (label == null) {
                newSet();
            } else {
                addButton(new JButton(new DelimiterAction(label, palette.get(i))));
            }
        }
    }
}
