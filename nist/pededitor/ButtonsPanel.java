/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JPanel;


/** A JPanel filled with GridBagLineWraps. When you add a JComponent,
    it just gets pasted flush left onto the last row of components
    until you exceed colCnt elements, then a new row is started. */
public class ButtonsPanel extends JPanel {
    private static final long serialVersionUID = -6195796167537079682L;
    int colCnt;
    Font font; 
    GridBagLineWrap gb;
    GridBagWrapper gb0 = new GridBagWrapper(this);
    GridBagConstraints wholeRow = new GridBagConstraints();
    {
        wholeRow.anchor = GridBagConstraints.WEST;
        wholeRow.gridwidth = GridBagConstraints.REMAINDER;
        wholeRow.insets = new Insets(3, 3, 3, 3);
    }

    /** Create a panel full of buttons with the appropriate labels that
        emit the appropriate string when pressed.

        @param colCnt Maximum number of buttons per row, or 0 if no limit.
    */
    public ButtonsPanel(int colCnt) {
        this(colCnt, null);
    }

    /** Create a panel full of buttons with the appropriate labels that
        emit the appropriate string when pressed.

        @param colCnt Maximum number of buttons per row, or 0 if no limit.

        @param font Font to use for the buttons.
    */
    public ButtonsPanel(int colCnt, Font font) {
        this.colCnt = colCnt;
        this.font = font;
    }

    /** Start a new set of buttons on a new row. */
    public void newSet() {
        gb = null;
    }

    public void addButton(JButton widget) {
        widget.setFocusable(false);
        if (font != null) {
            widget.setFont(font);
        }
        if (gb == null) {
            JPanel subpanel = new JPanel();
            gb0.add(subpanel, wholeRow);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gb = new GridBagLineWrap(subpanel, gbc, colCnt);
        }
        gb.add(widget);
    }
}
