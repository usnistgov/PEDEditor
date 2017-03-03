/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Class describing a ruler whose tick marks describe values from a
    LinearAxis. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
interface SegmentInterp2D extends Interp2D {

    @Override default int size() {
        return 2;
    }

    @Override default int minSize() {
        return 2;
    }

    @Override default int maxSize() {
        return 2;
    }

    void setStart(Point2D p);
    void setEnd(Point2D p);

    @Override default void set(int index, Point2D point) {
        Point2D.Double p = new Point2D.Double(point.getX(), point.getY());
        switch (index) {
        case 0:
            setStart(p);
        	break;
        case 1:
            setEnd(p);
        	break;
        default:
        	throw new IllegalArgumentException("index = " + index);
        }
    }

    @Override default Point2D.Double[] getPoints() {
        return new Point2D.Double[] { getStart(), getEnd() };
    }

    @Override default Point2D.Double get(int index) {
        switch (index) {
        case 0:
            return (Point2D.Double) getStart();
        case 1:
            return (Point2D.Double) getEnd();
        default:
        	throw new IllegalArgumentException("index = " + index);
        }
    }
    
    @Override SegmentInterp2D clone();
    
    @Override
    default SegmentInterp2D createTransformed(AffineTransform xform) {
        SegmentInterp2D res = clone();
        res.transform(xform);
        return res;
    }
    
    @Override default Shape getShape() {
        return new Line2D.Double(getStart(), getEnd());
    }

    @Override default void add(int index, Point2D point) {
        throw new UnsupportedOperationException();
    }

    @Override default void remove(int vertexNo) {
        throw new UnsupportedOperationException();
    }
    
    @Override default boolean isClosed() {
        return false;
    }

    @Override default void setClosed(boolean closed) {
        throw new UnsupportedOperationException();
    }

    @Override default <T extends Point2D> void setPoints(List<T> points) {
        int size = points.size();
        if (size != 2)
            throw new IllegalArgumentException("point.size() == " + size);
        setStart(points.get(0));
        setEnd(points.get(0));
    }

    @Override default BoundedParam2D getParameterization() {
        return BezierParam2D.create(getStart(), getEnd());
    }
}
