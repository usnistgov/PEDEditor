package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

public class CropPane extends ImagePane {

    static final boolean DOWNWARDS_Y = true;

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
            addVertex(vertices, new Point(e.getX(), e.getY()));
            CropPane.this.repaint();
            parentFrame.setSelectionReady(getSelection() != null);
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
            // It's a bit of a waste to repaint every time the mouse
            // moves, but it would be more of a waste to write special
            // code to figure out whether the repaint is needed.
            CropPane.this.repaint();
        }
    }

    public void setImage(BufferedImage image) {
        super.setImage(image);
        vertices = new ArrayList<Point>();
    }

    /** @return the selection region, or null if the selection region
        is invalid */
    public Polygon getSelection() {
        return getSelection(vertices);
    }

    /** Same as getSelection(), but with a different return type. */
    protected ArrayList<Point> getVertices() {
        Polygon p = getSelection();
        if (p == null) {
            return null;
        }

        ArrayList<Point> output = new ArrayList<Point>();
        for (int i = 0; i < p.npoints; ++i) {
            output.add(new Point(p.xpoints[i], p.ypoints[i]));
        }

        return output;
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

        int[] indices = Duh.sortIndices(pts, DOWNWARDS_Y);
        ArrayList<Point> output = new ArrayList<Point>(cnt);

        for (int i = 0; i < cnt; ++i) {
            output.add(points.get(indices[i]));
        }

        return output;
    }

    public DiagramType apparentDiagramType() {
        return apparentDiagramType(vertices);
    }

    public static DiagramType apparentDiagramType(ArrayList<Point> points) {
        int cnt = points.size();

        // Don't be too clever; the effort isn't worth it.

        if (cnt < 3 || cnt > 4) {
            return null;
        } else if (cnt == 4) {
            return DiagramType.BINARY;
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
            return DiagramType.TERNARY;
        }

        return DiagramType.BINARY;
    }


    /** @return A rectangular Polygon, with vertical sides, with p0
        and p1 as opposite diagonals of that rectangle. */
    public static Polygon toRectangle(Point p0, Point p1) {
        int minx = Math.min(p0.x, p1.x);
        int maxx = Math.max(p0.x, p1.x);
        int miny = Math.min(p0.y, p1.y);
        int maxy = Math.max(p0.y, p1.y);
        int[] xs = {minx, minx, maxx, maxx};
        int[] ys = {maxy, miny, miny, maxy};
        return new Polygon(xs, ys, 4);
    }

    /** @return A rectangular Polygon with p0p1 as side and p2 lying
        on the opposite side (or about as close to it as integer
        arithmetic permits). */
    public static Polygon toRectangle(Point p0, Point p1, Point p2) {
        int x1 = p1.x - p0.x;
        int y1 = p1.y - p0.y;
        int x2 = p2.x - p0.x;
        int y2 = p2.y - p0.y;

        double dotProd = x1 * x2 + y1 * y2;
        double projLengthRatio = dotProd / (x1 * x1 + y1 * y1);
        double prjx1 = projLengthRatio * x1;
        double prjy1 = projLengthRatio * y1;

        // Projection of P0P2 onto a line perpendicular to P0P1.
        double normx = x2 - prjx1;
        double normy = y2 - prjy1;

        Point[] points =
            { p0,
              p1, 
              Duh.toPoint(p0.x + normx, p0.y + normy),
              Duh.toPoint(p1.x + normx, p1.y + normy) };

        return Duh.sortToPolygon(points, DOWNWARDS_Y);
    }

    /** Assuming the points were entered in order (that is,
        points.get(points.size()-1) was entered most recently, convert
        the set of clicked-on points to the vertices of a polygon
        representing the selection. Return null if the set cannot be
        translated into a selection. */
    protected Polygon getSelection(ArrayList<Point> points) {
        int cnt = points.size();

        DiagramType diagramType = parentFrame.getDiagramType();

        boolean rectangular = (diagramType == DiagramType.BINARY
                               || diagramType == DiagramType.OTHER);
        boolean quadrilateral = rectangular
            || (diagramType == DiagramType.TERNARY_BOTTOM);
        boolean triangular = !quadrilateral;

        if (cnt == 0) {
            if (rectangular) {
                Dimension dim = getPreferredSize();
                return toRectangle(new Point(0,0), 
                                   new Point(dim.width, dim.height));
            } else {
                return null;
            }
        }

        if (cnt == 1) {
            return null;
        }

        if (cnt == 2) {
            if (rectangular) {
                return toRectangle(points.get(0), points.get(1));
            } else if (triangular) {
                return toTriangle(points.get(0), points.get(1), DOWNWARDS_Y);
            } else {
                return null;
            }
        }

        if (cnt == 3) {
            if (rectangular) {
                return toRectangle(points.get(0), points.get(1), 
                                   points.get(2));
            } else if (triangular) {
                return Duh.sortToPolygon(points, DOWNWARDS_Y);
            } else {
                return null;
            }
        }

        if (cnt == 4) {
            if (quadrilateral) {
                return Duh.sortToPolygon(points, DOWNWARDS_Y);
            } else {
                return null;
            }
        }
        
        return null;
    }


    /** Infer the third point of an equilateral triangle from the
        first two points. There are two candidates for the third
        point; the correct candidate in order to create an upright
        triangle, instead of one standing on its head, is the one that
        causes two out of three of the vertices to be below the
        triangle's center. */
    static Polygon toTriangle(Point p0, Point p1, boolean yAxisPointsDownwards) {
        int x1 = p1.x - p0.x;
        int y1 = p1.y - p0.y;

        // cs stands for Center of Segment p0p1.
        double csx = (p0.x + p1.x) / 2.0;
        double csy = (p0.y + p1.y) / 2.0;

        /* The third segment is a distance of h away from the center
         of the segment p0p1, and the displacement is perpendicular to
         p0p1, but that still leaves two choices. Try one of those
         choices, and if it turns out to be the wrong one, then we
         know the right one is in the opposite direction. */

        double h = Math.sqrt(3.0) / 2.0;
        double heightx = y1 * h;
        double heighty = -x1 * h;

        double p2y = csy + heighty;

        double cy = (p0.y + p1.y + p2y) / 3.0;
        int numVerticesWithYLessThanCenter = 0;
        if (p0.y < cy) {
            ++numVerticesWithYLessThanCenter;
        }
        if (p1.y < cy) {
            ++numVerticesWithYLessThanCenter;
        }
        if (p2y < cy) {
            ++numVerticesWithYLessThanCenter;
        }

        double p2x;
        if ((numVerticesWithYLessThanCenter == 1) !=
            yAxisPointsDownwards) {
            // Try the other direction instead
            heightx = -heightx;
            heighty = -heighty;
            p2y = csy + heighty;
        }
        p2x = csx + heightx;

        Point[] points = { p0, p1, Duh.toPoint(p2x, p2y) };
        return Duh.sortToPolygon(points, DOWNWARDS_Y);
    }

    /** Context-sensitive vertex addition or substitution routine;
        this may add a vertex, or it may replace the last vertex added
        if there is not room for additional vertices in this diagram
        type.

        @return true if the vertex was successly added or substituted,
        or false if the operation could not be performed. */
    public boolean addVertex(ArrayList<Point> vertices, Point p) {
        DiagramType diagramType = parentFrame.getDiagramType();
        if (diagramType == null) {
            return false;
        }
        if (vertices.size() < diagramType.getVertexCnt()) {
            vertices.add(p);
            return true;
        }
        vertices.set(vertices.size()-1, p);
        return true;
    }

    public void paint(Graphics g) {
        super.paint(g);

        DiagramType diagramType = parentFrame.getDiagramType();
        if (diagramType == null)
            return;
        
        int cnt = vertices.size();
        int maxCnt = diagramType.getVertexCnt();

        // Paint the selection that would result from clicking on the
        // current mouse position, if there is one, first, so that it
        // does not obscure the more important outline of the current
        // actual selection.

        Point mpos = null;
        try {
            mpos = getMousePosition();
        } catch (HeadlessException e) {
            mpos = null;
        }

        boolean markedVertices = false;

        if (mpos != null) {
            ArrayList<Point> newVertices = (ArrayList<Point>) vertices.clone();
            if (addVertex(newVertices, mpos)) {
                Polygon poly = getSelection(newVertices);
                if (poly != null) {
                    g.setColor(Color.RED);
                    ((Graphics2D) g).draw(poly);
                    markedVertices = true;
                }
            }
        }

        // Paint the actual selection, if there is one.

        Polygon poly = getSelection(vertices);
        if (poly != null) {
            g.setColor(Color.GREEN);
            ((Graphics2D) g).draw(poly);
            markedVertices = true;
        }

        if (vertices.size() > 0 && !markedVertices) {
            // Just show a polyline connecting all clicked-on points
            // and the current mouse position, if any.

            g.setColor(Color.RED);
            ArrayList<Point> newPoints = (ArrayList<Point>) vertices.clone();
            if (mpos != null) {
                newPoints.add(mpos);
            }

            int[] xs = new int[newPoints.size()];
            int[] ys = new int[newPoints.size()];
            int i = 0;
            for (Point p : newPoints) {
                xs[i] = p.x;
                ys[i] = p.y;
                ++i;
            }

            g.drawPolyline(xs, ys, xs.length);
        }
    }
}
