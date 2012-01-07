package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.JPanel;

public class EditPane extends JPanel {

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
        public void mouseMoved(MouseEvent e) {
            repaintMaybe();
        }

        public void mouseDragged(MouseEvent e) {
            repaintMaybe();
        }

        public void mouseExited(MouseEvent e) {
            repaintMaybe();
        }

        final void repaintMaybe() {
            EditPane.this.repaint();
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        getParentFrame().getParentEditor().paintEditPane(g);
    }
}
