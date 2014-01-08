/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

// The annotations below for deserializing this GeneralPolyline into
// its appropriate subtype were recommended on Programmer Bruce's
// blog, "Deserialize JSON with Jackson into Polymorphic Types". -- EB

/** A class for pairing the anchor points of a possibly smoothed
    polyline with its associated color, StandardStroke, and the line
    width multiplier to use with the StandardStroke. */
public abstract class PointsInterp2D extends Interp2D {
    protected ArrayList<Point2D.Double> points;

    public PointsInterp2D(boolean closed) {
        super(closed);
        points = new ArrayList<>();
    }

    public PointsInterp2D(Point2D[] points, boolean closed) {
        super(closed);
        if (points != null) {
            this.points = new ArrayList<>(points.length);
            for (Point2D p: points) {
                this.points.add(new Point2D.Double(p.getX(), p.getY()));
            }
        } else {
            this.points = new ArrayList<>();
        }
    }

    @Override public Point2D.Double[] getPoints() {
        return Duh.deepCopy(points.toArray(new Point2D.Double[0]));
    }

    @JsonIgnore public <T extends Point2D> void setPoints(Collection<T> points) {
        this.points = new ArrayList<Point2D.Double>
            (Arrays.asList(Duh.deepCopy(points.toArray
                                        (new Point2D.Double[0]))));
        param = null;
    }

    @JsonProperty("points") public void setPoints(Point2D.Double[] points) {
        this.points = new ArrayList<Point2D.Double>
            (Arrays.asList(Duh.deepCopy(points)));
        param = null;
    }

    /* Return control point #i. For closed curves, control points may
       be recounted (such as the 0th point being counted as the next
       one after the last). */
    @Override public Point2D.Double get(int vertexNo) {
        return (Point2D.Double) points.get(vertexNo % points.size()).clone();
    }

    /** Add the point to the polyline in the given position. */
    @Override public void add(int index, Point2D point) {
        points.add(index, new Point2D.Double(point.getX(), point.getY()));
        param = null;
    }

    /** Remove the given vertex. */
    @Override public void remove(int vertexNo) {
        points.remove(vertexNo);
        param = null;
    }

    /** Replace the given vertex, which must exist. */
    @Override public void set(int vertexNo, Point2D point) {
        points.set(vertexNo, new Point2D.Double(point.getX(), point.getY()));
        param = null;
    }

    /* Return the number of control points without duplication (so for
       closed curves, the return trip to point #0 does not count). */
    @Override public int size() {
        return points.size();
    }
}
