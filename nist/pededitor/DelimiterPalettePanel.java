/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;


/** A JPanel filled with JButtons that generate DelimiterEvents when
    pressed. This is a rote copy of StringPalettePanel.java, and it
    might be better style if the duplicate elements were merged, but
    eh. */
public class DelimiterPalettePanel extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = -7265299416396566730L;
	DelimiterPalette palette;
    int columnCnt = 0;
    ArrayList<DelimiterEventListener> delimiterEventListeners
        = new ArrayList<DelimiterEventListener>();

    class DelimiterAction extends AbstractAction {
        /**
		 * 
		 */
		private static final long serialVersionUID = -7205094586111632209L;
		Delimiter delimiter;

        DelimiterAction(String label, Delimiter delimiter) {
            putValue(Action.NAME, label);
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
        this.palette = palette;
        int cnt = palette.size();

        GridBagLineWrap gb = null;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        GridBagConstraints wholeRow = new GridBagConstraints();
        wholeRow.anchor = GridBagConstraints.WEST;
        wholeRow.gridwidth = GridBagConstraints.REMAINDER;
        wholeRow.insets = new Insets(3, 3, 3, 3);
        GridBagWrapper gb0 = new GridBagWrapper(this);

        for (int i = 0; i <= cnt; ++i) {
            String label = (i < cnt) ? palette.getLabel(i) : null;
            if (label == null) {
                gb = null;
            } else {
                if (gb == null) {
                    JPanel subpanel = new JPanel();
                    gb0.add(subpanel, wholeRow);
                    gb = new GridBagLineWrap(subpanel, gbc, colCnt);
                }
                JButton b = new JButton
                    (new DelimiterAction(label, palette.get(i)));
                if (font != null) {
                    b.setFont(font);
                }
                b.setFocusable(false);
                gb.add(b);
            }
        }
    }
}
