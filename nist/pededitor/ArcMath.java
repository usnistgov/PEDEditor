/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;

/** Utilities for doing math on arcs, circles, and ellipses. */
public class ArcMath {
    static double PI_OVER_180 = Math.PI/180;

    /** @param arc Either an Arc2D or an Ellipse2D. */
    public static Point2D.Double getLocation(RectangularShape arc, double deg) {
        return new Point2D.Double(
                arc.getX() + arc.getWidth() / 2 * (1 + Math.cos(deg * PI_OVER_180)),
                arc.getY() + arc.getHeight() / 2 * (1 + Math.sin(deg * PI_OVER_180)));
    }

    public static Point2D.Double getDerivative(RectangularShape arc, double deg) {
        return new Point2D.Double(
                -arc.getWidth() / 2 * PI_OVER_180 * Math.sin(deg * PI_OVER_180),
                arc.getHeight() / 2 * PI_OVER_180 * Math.cos(deg * PI_OVER_180));
    }

    /** Return angle values for the locations of intersections of arc and segment.

        @param isLine If true, segment is a line; if false, assume it
        is a segment.that ends at the given positions.
    */
    public static double[] segIntersections(Arc2D.Double arc, Line2D segment,
            boolean isLine) {
        double t0 = arc.start;
        double t1 = arc.start + arc.extent;
        double sdx = segment.getX2() - segment.getX1();
        double sdy = segment.getY2() - segment.getY1();
        if (sdx == 0 && sdy == 0) {
            // The segment is a point, so the claim that segment
            // doesn't intersect the curve is either true or within
            // an infinitesimal distance of being true, and we don't
            // guarantee infinite precision, so just return nothing.
            return new double[0];
        }
        boolean swapxy = Math.abs(sdx) < Math.abs(sdy);
        if (swapxy) {
            segment = new Line2D.Double
                (segment.getY1(), segment.getX1(),
                 segment.getY2(), segment.getX2());
            double tmp = sdx;
            sdx = sdy;
            sdy = tmp;
        }

        // Now the segment (with x and y swapped if necessary) has
        // slope with absolute value less than 1. That reduces the
        // number of corner cases and helps avoid numerical
        // instability.

        double m = sdy/sdx; // |m| <= 1
        double b = segment.getY1() - m * segment.getX1();

        // y = mx + b

        double minx = Math.min(segment.getX1(), segment.getX2());
        double maxx = Math.max(segment.getX1(), segment.getX2());

        ArrayList<Double> output = new ArrayList<>();

        double[] qcs = quadraticCoefs(arc);

        if (swapxy) {
            qcs = new double[] {qcs[0], qcs[2], qcs[1], qcs[4], qcs[3]};
        }
        double c = qcs[0];
        double cx = qcs[1];
        double cy = qcs[2];
        double cxx = qcs[3];
        double cyy = qcs[4];

        // Substitute y = mx + b into the ellipse equation.

        // That is,

        // c + cx x + cy (mx + b) + cxx x^2 + cyy (mx+b)^2 = 0

        // (c + b cy + b^2 cyy) +
        // x (cx + m cy + 2 b m cyy) +
        // x^2 (cxx + cyy m^2) = 0

        double poly[] = new double[] {
            c + b * cy + b * b * cyy,
            cx + m * cy + 2 * m * b * cyy,
            cxx + m * m * cyy};

        for (double x: Polynomial.solve(poly)) {
            double y = m * x + b;
            double t = toAngle(arc,
                    swapxy ? new Point2D.Double(y,x)
                            : new Point2D.Double(x,y));

            if (!Geom.degreesInRange(t, t0, t1)) {
                continue;
            }

            if (!isLine) {
                if (x < minx || x > maxx) {
                    // Bounds error: the segment domain is x in [minx,
                    // maxx].
                    continue;
                }
            }

            output.add(t - 360 * Math.floor((t - t0) / 360));
        }

        double[] o = new double[output.size()];
        for (int i = 0; i < o.length; ++i) {
            o[i] = output.get(i);
        }
        return o;
    }

    /** Return [c, cx, cy, cxx, cyy] for the ellipse equation
        c + cx x + cy y + cxx x^2 + cyy y^2 = 0 */
    static double[] quadraticCoefs(RectangularShape shape) {
        double rx = shape.getWidth() / 2;
        double ry = shape.getHeight() / 2;
        double centx = shape.getX() + rx;
        double centy = shape.getY() + ry;
        // (x/rx - centx/rx)^2 + (y/ry - centy/ry)^2 = 1

        // (1/rx)^2 x^2 - (2 centx / rx^2) x + centx^2/rx^2 +
        // (1/ry)^2 y^2 - (2 centy / ry^2) y + centy^2/ry^2 - 1 = 0

        double rxx = 1/(rx*rx);
        double ryy = 1/(ry*ry);
        
        return new double[]
            { centx * centx * rxx + centy * centy * ryy - 1,
              -2 * centx * rxx, -2 * centy * ryy, rxx, ryy };
    }

    public static double area(Arc2D.Double arc) {
        double t0 = arc.start;
        double t1 = arc.start + arc.extent;
        // Convert t to radians. It won't affect the result because y is
        // integrated with respect to x, not with respect to t.

        t0 *= PI_OVER_180;
        t1 *= PI_OVER_180;

        // y(t) = c_y + r_y sin t

        // x(t) = c_x + r_x cos t

        // x'(t) = - r_x sin t

        // integral(t0,t1) y(t) x'(t) =

        // integral(t0, t1) (-r_x c_y sin t - r_x r_y sin^2 t) =

        // eval(t0,t1) -r_x r_y (t - sin t cos t) / 2 + r_x c_y cos t

        double rx = arc.width / 2;
        double ry = arc.height / 2;
        double cy = arc.y + ry;

        double e0 = rx * (cy * Math.cos(t0) - ry * (t0 - Math.sin(t0) * Math.cos(t0)));
        double e1 = rx * (cy * Math.cos(t1) - ry * (t1 - Math.sin(t1) * Math.cos(t1)));
        return e1 - e0;
    }

    /**
     * Return an ellipse that passes through the given points. Two points are
     * assumed to identify the ends of a diameter of a circle. Three points
     * define a circle. Four points define an ellipse with axes that are
     * horizontal and vertical.
     */
        public static <T extends Point2D> Ellipse2D.Double ellipse(List<T> points)
        throws UnsolvableException {
        // A conic whose x^2 coefficient is nonzero (as is the case
        // for all arcs) can be expressed as

        // C0 + C1 x + C2 y + C3 y^2 + C4 xy + x^2

        // For now, I will assume C4 is always zero.

        int size = points.size();
        
        if (size == 1) {
            return new Ellipse2D.Double(points.get(0).getX(), points.get(0).getY(), 0, 0);
        } else if (size == 2) {
            return circle(points.get(0), points.get(1));
        }

        if (size < 2 || size > 4) {
            throw new IllegalArgumentException
                ("ArcInterp2D.ellipse() requires 2-4 points, not " 
                 + size + " points.");
        }

        Matrix rhs = new Matrix(size, 1);
        Matrix lhs = new Matrix(size, size);
        double cxy = 0;
        double cyy = 1; // If size == 3
        
        for (int i = 0; i < size; ++i) {
            Point2D p = points.get(i);
            double x = p.getX();
            double y = p.getY();
            double rh = -x*x; // right-hand side for this row.
            lhs.set(i, 0, 1.0);
            lhs.set(i, 1, x);
            lhs.set(i, 2, y);
            if (size > 3) {
                lhs.set(i, 3, y*y);
            } else {
                rh -= cyy * y*y;
            }
            if (size > 4) {
                lhs.set(i, 4, x*y);
            } else {
                cxy = 0;
            }
            rhs.set(i, 0, rh);
        }

        Matrix s = null;
        try {
            s = lhs.solve(rhs);
        } catch (RuntimeException x) {
            throw new UnsolvableException
                (points.toString() + " do not define an ellipse");
        }

        double c = s.get(0, 0);
        double cx = s.get(1, 0);
        double cy = s.get(2, 0);

        if (size >= 4)
            cyy = s.get(3,0);
        if (size >= 5)
            cxy = s.get(4,0);

        // (x - centx)^2 / rx^2 + (y - centy)^2 / ry^2 - 1 = 0

        // We're solving coefficients of

        // (x - centx)^2 + (y - centy)^2 (rx^2 / ry^2) - rx^2 = 0

        if (cxy != 0) {
            double disc = cxy * cxy - 4 * cyy;
            if (disc >= 0) {
                throw new UnsolvableException(
                        "These points define a parabola or hyperbola, "
                        + "not an ellipse");
            }

            throw new UnsupportedOperationException
                ("TODO Missing support for 5-point ellipses...");
        } else {
            // cxx = 1
            // cx = -2 x0
            // cyy = rx^2 / ry^2
            // cy = -2 y0 (rx^2/ry^2) = -2 y0 cyy
            // c = x0^2 + cyy y0^2 - rx^2
         double centx = -cx / 2;
            double centy = - cy / cyy / 2;
            double rx = Math.sqrt(centx * centx + cyy * centy * centy - c);
            double ry = rx / Math.sqrt(cyy);
            return new Ellipse2D.Double(centx - rx, centy - ry,
                                        rx * 2, ry * 2);
        }
    }

    /** Return a circle that passes through both points and has its
        center at the midpoint of p1p2. */
    static Ellipse2D.Double circle(Point2D p1, Point2D p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double r = Math.sqrt(dx*dx + dy*dy) / 2;
        double x = (p1.getX() + p2.getX()) / 2 - r;
        double y = (p1.getY() + p2.getY()) / 2 - r;
        return new Ellipse2D.Double(x, y, r*2, r*2);
    }

    /** The length of the arc on an ellipse with major and minor radii
        r1 and r2 is somewhere between the length of the same arc on a
        circle of radius r1 and on a circle of radius r2. */
    public static Estimate length(Arc2D.Double arc) {
        double len1 = arc.width * arc.extent * Math.PI / 360;
        double len2 = arc.height * arc.extent * Math.PI / 360;
        Estimate res = new Estimate((len1 + len2) / 2);
        res.setLowerBound(Math.min(len1, len2));
        res.setUpperBound(Math.max(len1, len2));
        return res;
    }

    /** @return the maximum Y value for points on an arc starting at
        angle t0+offset and ending at angle t1+offset, all in
        degrees. */
    static double arcMaxY(double t0, double t1, double offset) {
        if (t1 - t0 >= 360)
            return 1.0;
        t0 += offset;
        t1 += offset;
        t0 -= Math.floor(t0/360) * 360;
        t1 -= Math.floor(t1/360) * 360;
        if (Geom.degreesInRange(90, t0, t1))
            return 1.0;
        return Math.max(Math.sin(t0 * Math.PI / 180), Math.sin(t1 * Math.PI / 180));
    }

    public static Rectangle2D.Double getBounds(Arc2D.Double arc) {
        double t0 = arc.start;
        double t1 = arc.start + arc.extent;
        
        double maxX = arcMaxY(t0, t1, 90);
        double minX = -arcMaxY(t0, t1, -90);
        double maxY = arcMaxY(t0, t1, 0);
        double minY = -arcMaxY(t0, t1, 180);
        double cx = arc.getCenterX();
        double cy = arc.getCenterY();
        double rx = arc.getWidth() / 2;
        double ry = arc.getHeight() / 2;
                
        return new Rectangle2D.Double(cx + rx * minX, cy + ry * minY,
                rx * (maxX - minX), ry * (maxY - minY));
    }

    /** Return the maximum value of xc * x + yc * y for all points on arc. */
    public static double[] getBounds(Arc2D.Double arc, double xc, double yc) {
        return new double[] {
            -getMax(arc, -xc, -yc), getMax(arc, xc, yc) };
    }

    /** @return the maximum value of f(t) = x(t) * xc + y(t) * yc. */
    static double getMax(Arc2D.Double arc, double xc, double yc) {
        return getMax0(arc, xc, yc)
            + xc * arc.getCenterX() + yc * arc.getCenterY();
    }

    /** @return the maximum value of f(t) = x(t) * xc + y(t) * yc,
        ignoring the offset of the arc center. */
    static double getMax0(Arc2D.Double arc, double xc, double yc) {
        double t0 = arc.start;
        double t1 = arc.start + arc.extent;
        double sxc = arc.width / 2 * xc; // scaled xc
        double syc = arc.height / 2 * yc; // scaled yc
        // Now the problem is to find the maximum value of sxc * x +
        // syc * y over an arc on the unit circle.

        double circleMax = Math.sqrt(sxc * sxc + syc * syc);
        if (arc.extent == 360) {
            return circleMax;
        }

        double deg = Math.atan2(syc, sxc) * 180 / Math.PI + 180;
        if (Geom.degreesInRange(deg, t0, t1))
            return circleMax;

        // Rotate the system so circleMax corresponds to an angle of
        // zero. The maximum value of the function occurs at the
        // endpoint nearest to angle zero.

        t0 = Math.abs(t0 - deg);
        t1 = Math.abs(t1 - deg);
        t0 -= Math.floor(t0/360) * 360;
        t1 -= Math.floor(t1/360) * 360;
        return circleMax * Math.cos(Math.min(t0, t1) * Math.PI / 180);
    }

    private static CurveDistance circleDistance(Ellipse2D circle, Point2D p) {
        assert(circle.getWidth() == circle.getHeight());
        double dx = p.getX() - circle.getCenterX();
        double dy = p.getY() - circle.getCenterY();
        double r = circle.getWidth() / 2;
        double t = (r == 0) ? 0
            : (dx == 0 && dy == 0) ? 0
            : (Math.atan2(dy, dx) * 180 / Math.PI);
        Point2D pNear = getLocation(circle, t);
        return new CurveDistance(t, pNear, pNear.distance(p));
    }

    static Ellipse2D.Double toEllipse(Arc2D arc) {
        return new Ellipse2D.Double(arc.getX(), arc.getY(), arc.getWidth(), arc.getHeight());
    }

    static Arc2D.Double toArc(RectangularShape arc,
            double start, double extent) {
        return new Arc2D.Double(arc.getX(), arc.getY(),
                arc.getWidth(), arc.getHeight(), start, extent,
                Arc2D.OPEN);
    }

    public static CurveDistanceRange distance(Ellipse2D ellipse, Point2D p) {
        if (ellipse.getWidth() == ellipse.getHeight()) {
            return new CurveDistanceRange(circleDistance(ellipse, p));
        }

        if (ellipse.getHeight() == 0) {
            Point2D.Double p0 = new Point2D.Double
                (ellipse.getX(), ellipse.getY());
            Point2D.Double p1 = new Point2D.Double
                (p0.x + ellipse.getWidth(), p0.y);
            double d0 = p0.distance(p);
            double d1 = p1.distance(p);
            if (d1 <= d0) {
                return new CurveDistanceRange(0, p1, d1, d1);
            } else {
                return new CurveDistanceRange(180, p0, d0, d0);
            }
        } else if (ellipse.getWidth() == 0) {
            Point2D.Double p0 = new Point2D.Double
                (ellipse.getX(), ellipse.getY());
            Point2D.Double p1 = new Point2D.Double
                (p0.x, p0.y + ellipse.getHeight());
            double d0 = p0.distance(p);
            double d1 = p1.distance(p);
            if (d1 <= d0) {
                return new CurveDistanceRange(90, p1, d1, d1);
            } else {
                return new CurveDistanceRange(-90, p0, d0, d0);
            }
        }

        //  Rescale to turn the ellipse into a unit circle.
        // Multiply the distance of the point to the unit circle by
        // the ellipse's minor axis radius to obtain a lower bound on
        // the true distance.

        Point2D xpoint = new Point2D.Double
            ((p.getX() - ellipse.getCenterX()) / (ellipse.getWidth() / 2),
             (p.getY() - ellipse.getCenterY()) / (ellipse.getHeight() / 2));
        CurveDistance cd = circleDistance(new Ellipse2D.Double(-1, -1, 2, 2), xpoint);
        Point2D.Double prettyClose = getLocation(ellipse, cd.t);
        double dist = prettyClose.distance(p);
        double smallerRadius = Math.min(ellipse.getWidth(), ellipse.getHeight())
            / 2;
        return new CurveDistanceRange(cd.t, prettyClose, dist,
                cd.distance * smallerRadius);
    }

    static CurveDistance distance(RectangularShape arc, Point2D p, double deg) {
        Point2D.Double pt = getLocation(arc, deg);
        return new CurveDistance(deg, pt, pt.distance(p));
    }

    public static CurveDistanceRange distance(Arc2D.Double arc, Point2D p) {
        double t0 = arc.start;
        double t1 = arc.start + arc.extent;

        // Consider three candidates for the closest point: t0, t1,
        // and the ellipseDistance result.

        // In addition to the ellipseDistance lower bound on the
        // minimum distance, also consider the distance from the
        // bounding box of the (t0,t1) arc to p.
        
        System.out.println("distance(" + Geom.toString(p) + ", " + t0 + ", " + t1 + ")"); // UNDO

        CurveDistance cd = distance(arc, p, t0).minWith(distance(arc, p, t1));
        double minDist = Geom.distance(p, getBounds(arc));
        CurveDistanceRange res = new CurveDistanceRange(cd, minDist);

        CurveDistanceRange fed = distance(toEllipse(arc), p);
        fed.t -= Math.floor((fed.t - t0) / 360) * 360;
        if (Geom.degreesInRange(fed.t, t0, t1)) {
            res.add(fed);
        }
        return res;
    }

    /** Convert p to an angle like Arc2D#setAngleStart() does. No, not
        quite like that: it seems to reverse the y angle. */
    static double toAngle(RectangularShape arc, Point2D p) {
        if (arc.getWidth() == 0 || arc.getHeight() == 0)
            return 0;
        double dx = (p.getX() - arc.getCenterX()) / arc.getWidth();
        double dy = (p.getY() - arc.getCenterY()) / arc.getHeight();
        return Math.atan2(dy, dx) * 180 / Math.PI;
    }

    /** Return deg coerced to range [-180, 180). */
    static double coerce180(double deg) {
        return deg - Math.floor((deg + 180) / 360) * 360;
    }

    // TODO The point on an ellipse nearest to the point is always in the
    // same quadrant of the ellipse as that point, and there is always
    // only one nearest point, with distances strictly increasing on
    // each side of that point. That means the optimum can be computed
    // in logarithmic time using a bracketing search such as golden
    // section.
}
