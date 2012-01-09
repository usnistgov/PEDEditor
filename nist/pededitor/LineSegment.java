package gov.nist.pededitor;

import java.awt.geom.*;

/** Trivial class representing a line segment. TODO Replacing
    LineSegment with Line2D everywhere is probably a good idea. */
public class LineSegment {
    public Point2D p1;
    public Point2D p2;

    public LineSegment(Point2D p1, Point2D p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /** @return a new LineSegment with the x and y coordinates
        switched. */
    public LineSegment transpose() {
        return new LineSegment
            (new Point2D.Double(p1.getY(), p1.getX()),
             new Point2D.Double(p2.getY(), p2.getX()));
    }

    public String toString() {
        return "LineSegment[" + p1 + " - " + p2 + "]";
    }
}
