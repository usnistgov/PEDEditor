package gov.nist.pededitor;

import java.awt.geom.*;

/** Trivial class representing a line segment. */
public class LineSegment {
    public Point2D p1;
    public Point2D p2;

    public LineSegment(Point2D p1, Point2D p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public String toString() {
        return "LineSegment[" + p1 + " - " + p2 + "]";
    }
}
