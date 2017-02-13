/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

// The annotations below for deserializing this GeneralPolyline into
// its appropriate subtype were recommended on Programmer Bruce's
// blog, "Deserialize JSON with Jackson into Polymorphic Types". -- EB

/** An Interp2D paired with an ArrayList of points. */
abstract public class PointsInterp2D implements Interp2D {
    protected ArrayList<Point2D.Double> points;
    protected transient BoundedParam2D param = null;

    public PointsInterp2D() {
        points = new ArrayList<>();
    }

    public <T extends Point2D> PointsInterp2D(List<T> points) {
        setPoints(points);
    }

    @Override abstract public PointsInterp2D clone();

    @Override public Point2D.Double[] getPoints() {
        return Geom.deepCopy(points.toArray(new Point2D.Double[0]));
    }

    @Override @JsonIgnore public BoundedParam2D getParameterization() {
        if (param == null) {
            param = PathParam2D.create(getShape());
        }
        return param;
    }

    @Override public <T extends Point2D> void setPoints(List<T> points) {
        this.points = new ArrayList<Point2D.Double>
            (Arrays.asList(Geom.deepCopy(points.toArray
                                        (new Point2D.Double[0]))));
        param = null;
    }

    @JsonProperty("points") public void setPoints(Point2D.Double[] points) {
        setPoints(Arrays.asList(points));
    }

    public <T extends Point2D> void setPoints(T[] points) {
        setPoints(Arrays.asList(points));
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

    @Override public void setClosed(boolean closed) {
        if (closed != isClosed()) {
            param = null;
        }
        // The work of actually setting closed to the given value must
        // be done by a subclass.
    }

    @Override public String toString() {
        try {
            return getClass().getCanonicalName()
                + (new ObjectMapper()).writeValueAsString(this);
        } catch (Exception e) {
            System.err.println(e);
            return getClass().getCanonicalName() + "[ERROR]";
        }
    }
}
