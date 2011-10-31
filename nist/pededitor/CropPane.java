package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.util.*;

public class CropPane extends ImagePane {

    protected ArrayList<Point> vertices = new ArrayList<Point>();
    protected String filename = null;
    protected CropFrame parentFrame;

    private static final long serialVersionUID = -7787299467082484939L;

    CropPane(CropFrame parentFrame) {
        this.parentFrame = parentFrame;
        CropMouseAdapter adapt = new CropMouseAdapter();
        addMouseListener(adapt);
        addMouseMotionListener(adapt);
        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
    }

    class CropMouseAdapter extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (vertices.size() == 4) {
                vertices.remove(3);
            }
            vertices.add(new Point(e.getX(), e.getY()));
            CropPane.this.repaint();
            parentFrame.verticesChanged();
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
            int cnt = vertices.size();
            if (cnt >= 1 && cnt <= 4) {
                CropPane.this.repaint();
            }
        }
    }

    public void setImage(BufferedImage image) {
        super.setImage(image);
        vertices = new ArrayList<Point>();
    }

    /** @return the crop points that have been selected */
    protected ArrayList<Point> getVertices() {
        return getVertices(vertices);
    }

    static ArrayList<Point> sortedVertices(ArrayList<Point> points) {
        int cnt = points.size();
        if (cnt < 2) {
            return points;
        }

        Point2D.Double[] pts = new Point2D.Double[cnt];
        for (int i = 0; i < cnt; i++) {
            Point p = points.get(i);
            pts[i] = new Point2D.Double((double) p.x, (double) p.y);
        }

        int[] indices = Duh.sortIndices(pts, true);
        ArrayList<Point> output = new ArrayList<Point>(cnt);

        for (int i = 0; i < cnt; ++i) {
            output.add(points.get(indices[i]));
        }

        return output;
    }

    protected ArrayList<Point> getVertices(ArrayList<Point> points) {
        int cnt = points.size();

        if (points.size() != 3) {
            return sortedVertices(points);
        }

        // If the projection of P2 onto P0P1 lies in P0P1's middle
        // third, then count it as a triangle; otherwise, count it as
        // a rectangle.
        Point p0 = points.get(0);
        Point p1 = points.get(1);
        Point p2 = points.get(2);

        int x1 = p1.x - p0.x;
        int y1 = p1.y - p0.y;
        int x2 = p2.x - p0.x;
        int y2 = p2.y - p0.y;
        double dotProd = x1 * x2 + y1 * y2;
        double projLengthRatio = dotProd / (x1 * x1 + y1 * y1);

        if (projLengthRatio >= 0.33 && projLengthRatio <= 0.67) {
            // Looks like a triangle.
            return sortedVertices(points);
        }

        // Looks like three legs of a rectangle.

        double prjx1 = projLengthRatio * x1;
        double prjy1 = projLengthRatio * y1;

        // Projection of P0P2 onto a line perpendicular to P0P1.
        double normx = x2 - prjx1;
        double normy = y2 - prjy1;

        ArrayList<Point> rectangle = new ArrayList<Point>(4);
        rectangle.add(p0);
        rectangle.add(p1);
        rectangle.add(Duh.toPoint(p0.x + normx, p0.y + normy));
        rectangle.add(Duh.toPoint(p1.x + normx, p1.y + normy));

        return sortedVertices(rectangle);
    }

    static void paintPoly(Graphics g, ArrayList<Point> points) {
        int[] xs = new int[points.size()];
        int[] ys = new int[points.size()];
        int i = 0;
        for (Point p : points) {
            xs[i] = p.x;
            ys[i] = p.y;
            ++i;
        }

        g.drawPolygon(xs, ys, xs.length);
    }

    public void paint(Graphics g) {
        super.paint(g);
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

        if (mpos != null) {
            // If 1-3 vertices have already been chosen, display in
            // red the cropping region that would result from adding
            // the current mouse position.

            g.setColor(Color.RED);
            if (cnt == 1) {		
                Point p = vertices.get(0);
                g.drawLine(p.x, p.y, mpos.x, mpos.y);
            } else if (cnt <= 4) {
                ArrayList<Point> verticesPlus1 = (ArrayList<Point>) vertices.clone();
                if (cnt == 4) {
                    verticesPlus1.remove(3);
                }
                verticesPlus1.add(new Point(mpos.x, mpos.y));
                paintPoly(g, getVertices(verticesPlus1));
            }
        }

        if (vertices.size() >= 3) {
            // Display in green the current cropping region.
            g.setColor(Color.GREEN);
            paintPoly(g, getVertices(vertices));
        }
    }
    
}
