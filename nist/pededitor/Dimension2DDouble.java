/* Eric Boesch, NIST Materials Measurement Laboratory, 2015. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Rectangle2D;

public class Dimension2DDouble extends java.awt.geom.Dimension2D {
    public double width;
    public double height;

    @Override public double getWidth() {
        return width;
    }

    @Override public double getHeight() {
        return height;
    }

    public Dimension2DDouble(Rectangle2D r) {
        width = r.getWidth();
        height = r.getHeight();
    }

    public Dimension2DDouble(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[" + width + ", " + height + "]";
    }
}
