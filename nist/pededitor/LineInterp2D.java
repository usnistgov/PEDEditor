package gov.nist.pededitor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

/** Specialization of GeneralPol for vanilla polylines. */
public class LineInterp2D extends PointsInterp2D {
    public LineInterp2D(Point2D[] points, boolean closed) {
        super(points, closed);
    }

    // This constructor is redundant, but it allows me to tell Jackson
    // that the "points" field of the JSON file is filled with
    // Point2D.Doubles.
    public LineInterp2D(@JsonProperty("points") Point2D.Double[] points,
               @JsonProperty("closed") boolean closed) {
        super(points, closed);
    }

    /** Shorthand to create a line segment. */
    public LineInterp2D(Point2D a, Point2D b) {
        super(new Point2D[] { a, b }, false);
    }

    /** Shorthand to create a line segment. */
    public LineInterp2D(Line2D segment) {
        this(segment.getP1(), segment.getP2());
    }

    @Override public Path2D.Double getPath() {
        return createPath(points, isClosed());
    }

    /** Create a straight (polyline) Path2D.Double connecting the
        given set of points, returning to the starting point if closed
        is true. */
    static public Path2D.Double createPath
        (List<Point2D.Double> points, boolean closed) {
        int size = points.size();
        Path2D.Double res = new Path2D.Double();
        if (size == 0) {
            return res;
        }

        Point2D.Double p0 = points.get(0);
        res.moveTo(p0.x, p0.y);

        for (int i = 1; i < size; ++i) {
            Point2D.Double p = points.get(i);
            res.lineTo(p.x, p.y);
        }

        if (closed && size > 1) {
            res.closePath();
        }
        return res;
    }

    @Override public LineInterp2D createTransformed(AffineTransform xform) {
        return new LineInterp2D(transformPoints(xform), isClosed());
    }
}
