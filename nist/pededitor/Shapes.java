package gov.nist.pededitor;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class Shapes {

    static class PathAndPoint {
        Path2D.Double path;
        Point2D.Double point;

        PathAndPoint(Path2D.Double path, Point2D.Double point) {
            this.path = path;
            this.point = point;
        }
    }

    static ArrayList<PathAndPoint> getParts(PathIterator pit) {
        ArrayList<PathAndPoint> parts = new ArrayList<>();
        Path2D.Double path = null;
        double[] coords = new double[6];

        for (; !pit.isDone(); pit.next()) {
            switch (pit.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                if (path != null) {
                    path.closePath();
                }
                path = new Path2D.Double();
                path.moveTo(coords[0], coords[1]);
                parts.add(new PathAndPoint
                          (path, new Point2D.Double(coords[0], coords[1])));
                break;
            case PathIterator.SEG_LINETO:
                path.lineTo(coords[0], coords[1]);
                break;
            case PathIterator.SEG_QUADTO:
                path.quadTo(coords[0], coords[1], coords[2], coords[3]);
                break;
            case PathIterator.SEG_CUBICTO:
                path.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                break;
            case PathIterator.SEG_CLOSE:
                break;
            default:
                throw new IllegalStateException
                    ("Unrecognized segment type " + pit.currentSegment(coords));
            }
        }

        if (path != null) {
            path.closePath();
        }

        return parts;
    }

    public static Shape fillHoles(PathIterator pit) {
        ArrayList<PathAndPoint> parts = getParts(pit);

        // I see no easy way to improve over O(n^2)
        // performance. You can usually do
        // better with getBounds(), but I would rather not go through
        // the trouble.
        Path2D.Double res = new Path2D.Double();
        for (PathAndPoint part: parts) {
            Path2D.Double p = part.path;
            Point2D.Double point = part.point;
            boolean ok = true;
            for (PathAndPoint otherPart: parts) {
                Path2D.Double op = otherPart.path;
                if (p != op && op.contains(point)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                res.append(p, false);
            }
        }
        return res;
    }

    /** Return the outermost holes in the given path iterator. A
        circular boundary has no holes, but a white circle has a hole
        in the middle, while the rendering of the string '88' has 4
        holes, 2 per figure-8. Two concentric rings have only one
        outermost hole plus one inner hole. */
    public static Shape getHoles(PathIterator pit) {
        ArrayList<PathAndPoint> parts = getParts(pit);

        // I see no easy way to improve over O(n^2)
        // performance. You can usually do
        // better with getBounds(), but I would rather not go through
        // the trouble.
        Path2D.Double res = new Path2D.Double();
        for (PathAndPoint part: parts) {
            int containerCnt = 0;
            Path2D.Double p = part.path;
            Point2D.Double point = part.point;
            for (PathAndPoint otherPart: parts) {
                Path2D.Double op = otherPart.path;
                if (p != op && op.contains(point)) {
                    ++containerCnt;
                    if (containerCnt > 1) {
                        break;
                    }
                }
            }
            if (containerCnt == 1) {
                res.append(p, false);
            }
        }
        return res;
    }

    public static void drawString(Graphics g, String str,
                                  double x, double y,
                                  double xWeight, double yWeight,
                                  double xMargin, double yMargin,
                                  double angle) {
        drawString(g, str, x, y, xWeight, yWeight, xMargin, yMargin, angle, false);
    }

    public static void drawString(Graphics g, String str,
                                  double x, double y,
                                  double xWeight, double yWeight,
                                  double xMargin, double yMargin,
                                  double angle, boolean drawHoles) {
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform unrotate = AffineTransform.getRotateInstance(-angle);
        Point2D.Double unrotatedP = new Point2D.Double(x,y);
        unrotate.transform(unrotatedP, unrotatedP);
        x = unrotatedP.x;
        y = unrotatedP.y;
        AffineTransform oldXform = g2d.getTransform();
        g2d.rotate(angle);
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(str, g);
        x += xMargin -bounds.getX() - (bounds.getWidth() + 2 * xMargin) * xWeight;
        y += yMargin -bounds.getY() - (bounds.getHeight() + 2 * yMargin) * yWeight;
        if (drawHoles) {
            Shape outline = g2d.getFont().createGlyphVector
                (g2d.getFontRenderContext(), str).getOutline((float) x, (float) y);
            outline = Shapes.getHoles(outline.getPathIterator(g2d.getTransform()));
            g2d.setTransform(new AffineTransform());
            g2d.fill(outline);
        } else {
            g2d.drawString(str, (float) x, (float) y);
        }
        g2d.setTransform(oldXform);
    }
}
