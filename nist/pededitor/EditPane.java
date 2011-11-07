package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

public class EditPane extends ImagePane {

    protected ArrayList<Point> vertices = new ArrayList<Point>();
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

    class EditMouseAdapter extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            vertices.add(new Point(e.getX(), e.getY()));
            recomputeSpline();
            EditPane.this.repaint();
        }

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
            if (vertices.size() >= 1) {
                EditPane.this.repaint();
            }
        }
    }

    protected void recomputeSpline() {
        int cnt = vertices.size();
        if (cnt > 1) {
            spline = (new CubicSpline2D(vertices.toArray(new Point[0]))).path();
        }
    }

    /** @return the edit points that have been selected */
    protected ArrayList<Point> getVertices() {
        return vertices;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (diagramOutline != null) {
            ((Graphics2D) g).draw(diagramOutline);
        }

        int cnt = vertices.size();

        if (cnt == 0) {
            return;
        }
        
        Point mpos = null;
        try {
            mpos = getMousePosition();
        } catch (HeadlessException e) {
            mpos = null;
        }

        Graphics2D g2d = (Graphics2D) g;
            spline = (new CubicSpline2D(vertices.toArray(new Point[0]))).path();
        
        if (mpos != null) {
            try {
                vertices.add(new Point(mpos.x, mpos.y));
                g.setColor(Color.RED);
                g2d.draw((new CubicSpline2D
                          (vertices.toArray(new Point[0]))).path());
            } finally {
                vertices.remove(cnt);
            }
        }

        if (cnt >= 2) {
            g.setColor(Color.GREEN);
            g2d.draw(spline);
        }
    }
}
