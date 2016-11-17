/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

public class EditPane extends JPanel {
    private static final long serialVersionUID = 2995661544080625928L;

    protected String filename = null;
    protected EditFrame parentFrame;
    protected Shape diagramOutline = null;

    EditPane(EditFrame parentFrame) {
        this.parentFrame = parentFrame;
        EditMouseAdapter adapt = new EditMouseAdapter();
        addMouseListener(adapt);
        addMouseMotionListener(adapt);
        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
    }

    public EditFrame getParentFrame() {
        return parentFrame;
    }

    class EditMouseAdapter extends MouseAdapter {
        @Override public void mouseMoved(MouseEvent e) {
            repaintMaybe();
        }

        @Override public void mouseDragged(MouseEvent e) {
            repaintMaybe();
        }

        @Override public void mouseExited(MouseEvent e) {
            repaintMaybe();
        }

        final void repaintMaybe() {
            EditPane.this.repaint();
        }
    }

    @Override public void paintComponent(Graphics g) {
        super.paintComponent(g);
        getParentFrame().getEditor().paintEditPane(g);
    }
}
