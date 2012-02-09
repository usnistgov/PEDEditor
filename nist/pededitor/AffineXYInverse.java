package gov.nist.pededitor;

import java.awt.geom.Point2D;

/** The inverse of class AffineXY (@see AffineXY.java). */
public class AffineXYInverse extends AffineXYCommon
   implements Transform2D {

    protected Point2D.Double aRangePoint = null;
    public void setxk(double xk) { this.xk = xk; preferredSolution = -1; }
    public void setxkx(double xkx) { this.xkx = xkx; preferredSolution = -1; }
    public void setxky(double xky) { this.xky = xky; preferredSolution = -1; }
    public void setxkxy(double xkxy) { this.xkxy = xkxy; preferredSolution = -1; }
    public void setyk(double yk) { this.yk = yk; preferredSolution = -1; }
    public void setykx(double ykx) { this.ykx = ykx; preferredSolution = -1; }
    public void setyky(double yky) { this.yky = yky; preferredSolution = -1; }
    public void setykxy(double ykxy) { this.ykxy = ykxy; preferredSolution = -1; }
    public void set(double xk, double xkx, double xky, double xkxy,
                    double yk, double ykx, double yky, double ykxy) {
        super.set(xk, xkx, xky, xkxy, yk, ykx, yky, ykxy);
        preferredSolution = -1;
    }

    /** Preferred solution number: 0 or 1 depending on whether the
     * first or second solution returned by solve_equations is
     * preferred, or -1 if unknown either because includeInRange has not
     * been called yet or the preferred solution has not been recomputed
     * since the coefficients were changed.. */
    protected int preferredSolution = -1;

    public AffineXY createInverse() {
        AffineXY inv = new AffineXY();
        inv.copyFieldsFrom(this);
        return inv;
    }

    @Override public AffineXYInverse clone() {
        AffineXYInverse output = new AffineXYInverse();
        output.copyFieldsFrom(this);
        return output;
    }

    /** If the xy coefficients are nonzero, then points in the AffineXY
        domain will normally have zero or two preimages. include_in_range()
        asserts that the portion of the preimage that includes the given
        point should be included in the range. */
    public void includeInRange(double x, double y) {
        aRangePoint = new Point2D.Double(x,y);
        preferredSolution = -1;
    }

    public Point2D.Double transform(double x, double y)
        throws UnsolvableException {
        if (preferredSolution == -1) {
            // Initialize preferredSolution value.
            computePreferredSolution();
        }
        Point2D.Double[] solutions =
            solveEquations(xk - x, xkx, xky, xkxy, yk - y, ykx, yky, ykxy);
        return solutions[preferredSolution];
    }

    private static Point2D.Double[] transpose(Point2D.Double[] points,
                                           boolean doswap) {
        if (doswap) {
            for (Point2D.Double point : points) {
                double tmp = point.x;
                point.x = point.y;
                point.y = tmp;
            }
        }
        return points;
    }

    static Point2D.Double[] solveEquations
        (double k1, double kx1, double ky1, double kxy1,
         double k2, double kx2, double ky2, double kxy2)
        throws UnsolvableException {

        if (kxy1 != 0) {
            if (Math.abs(kxy2) < Math.abs(kxy1)) {
                // Swap *1 <=> *2.

                double tmp;
                tmp = k1; k1 = k2; k2 = tmp;
                tmp = kx1; kx1 = kx2; kx2 = tmp;
                tmp = ky1; ky1 = ky2; ky2 = tmp;
                tmp = kxy1; kxy1 = kxy2; kxy2 = tmp;
            }

            // Apply the operator
            // EQUATION1 := EQUATION1 - kxy1/kxy2 EQUATION2

            // which will transform the first equation so that kxy1 becomes zero

            double rat = -kxy1 / kxy2;
            k1 += rat * k2;
            kx1 += rat * kx2;
            ky1 += rat * ky2;
            kxy1 = 0;
        }

        // Now kxy1 == 0.

        if (ky1 == 0) {
            // Instead of solving for y in terms of x, we can read out
            // the value of x directly and then solve for y.

            // 0 = k1 + kx1 x

            if (kx1 == 0) {
                if (k1 != 0) {
                    return new Point2D.Double[0];
                }

                // The first equation is 0=0, which means we have
                // infinitely many solutions unless the second equation is
                // 0 = 1 or the like.

                if (kx2 == 0 && ky2 == 0 && kxy2 == 0 && k2 != 0) {
                    return new Point2D.Double[0];
                }

                throw new InfiniteSolutionsException();
            }

            double x = -k1 / kx1;

            // 0 = k2 + kx2 x + ky2 y + kxy2 x y

            double y = -(k2 + kx2 * x) / (ky2 + x * kxy2);

            Point2D.Double[] points = {new Point2D.Double(x,y)};

            return points;
        }

        boolean swapxy = false;

        if (Math.abs(kx1) > Math.abs(ky1)) {
            // The system is better-conditioned if we swap x and y.
            swapxy = true;
            double tmp;

            tmp = kx1;
            kx1 = ky1;
            ky1 = tmp;
            tmp = kx2;
            kx2 = ky2;
            ky2 = tmp;
        }

        // Solve for y in terms of x: y = mx + b
        double b = -k1 / ky1;
        double m = -kx1 / ky1;

        // Now substitute in the second equation.

        // 0 = k2 + kx2 x + ky2 (mx + b) + kxy2 x (mx + b)

        // 0 = a x^2 + b2 x + c (b2 because the name b is already taken)
        double a = kxy2 * m;
        double b2 = kx2 + m * ky2 + kxy2 * b;
        double c = k2 + b * ky2;

        if (a == 0) {
            if (b2 == 0) {
                if (c != 0) {
                    return new Point2D.Double[0];
                }
                throw new InfiniteSolutionsException();
            } else {
                double x = -c/b2;
                Point2D.Double[] points = {new Point2D.Double(x, m*x + b)};
                return transpose(points, swapxy);
            }
        }

        // Use the stable version of the quadratic formula listed in
        // the Wikipedia article on "loss of significance".

        double discriminant = b2 * b2 - 4 * a * c;
        if (discriminant < 0) {
            return new Point2D.Double[0];
        } else if (discriminant == 0) {
            double x = -b2 / 2 / a;
            Point2D.Double[] points = {new Point2D.Double(x, m*x + b)};
            return transpose(points, swapxy);
        } else {
            double dsqrt = Math.sqrt(discriminant);
            double x1 = (b2 < 0) ? ((-b2 + dsqrt) / (2 * a))
                : ((-b2 - dsqrt) / (2 * a));
            double x2 = c / a / x1;
            Point2D.Double[] points =
                {new Point2D.Double(x1, m*x1 + b),
                 new Point2D.Double(x2, m*x2 + b)};
            return transpose(points, swapxy);
        }
    }

    /** Compute the number of the preferred solution (which generally
     * means choosing whether to use the plus or minus term in the
     * quadratic formula). */
    protected void computePreferredSolution()
        throws UnsolvableException {
        if (aRangePoint == null) {
            if (isAffine()) {
                // There is only one solution, so use that one.
                preferredSolution = 0;
                return;
            }
            throw new UnsolvableException
                ("AffineXYInverse.transform() is ambiguous " +
                 "(call includeInRange() to fix)");
        }

        Point2D.Double aDomainPoint =
            createInverse().transform(aRangePoint.x, aRangePoint.y);
        double xp = aDomainPoint.x;
        double yp = aDomainPoint.y;
        Point2D.Double[] points = solveEquations
            (xk - xp, xkx, xky, xkxy, yk - yp, ykx, yky, ykxy);

        // Of the two solutions (assuming there are two solutions), the
        // preferred solution is the one that maps (xp, yp) back to
        // aRangePoint. For well-behaved transforms that are close to
        // being affine in the region of interest, the other set of
        // solutions is not wanted.

        double minDist = 0;
        boolean haveMinDist = false;
        int i = 0;

        // The minimum distance between the closest of the one or two
        // solutions and aRangePoint should be very small, because
        // aRangePoint should *be* one of the two solutions.

        for (Point2D.Double point : points) {
            double dist = aRangePoint.distance(point);
            if (!haveMinDist || dist < minDist) {
                haveMinDist = true;
                preferredSolution = i;
                minDist = dist;
            }
            ++i;
        }
    }
}
