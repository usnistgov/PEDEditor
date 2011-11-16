package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

public class EditPane extends ImagePane {

    protected String filename = null;
    protected EditFrame parentFrame;
    protected Path2D spline;
    protected Shape diagramOutline = null;

    EditPane(EditFrame parentFrame) {
        this.parentFrame = parentFrame;
        EditMouseAdapter adapt = new EditMouseAdapter();
        addMouseListener(adapt);
        addMouseMotionListener(adapt);
        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void setDiagramOutline(Shape diagramOutline) {
        this.diagramOutline = diagramOutline;
        repaint();
    }

    public Shape getDiagramOutline() {
        return diagramOutline;
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

        Point mpos = null;
        try {
            mpos = getMousePosition();
        } catch (HeadlessException e) {
            mpos = null;
        }
        getParentFrame().getParentEditor().paintEditPane(g, mpos);
    }
}
