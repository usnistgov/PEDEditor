/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/** Graphics2D wrapper that keeps track of the bounds of all items
    drawn to it... or at least as many items as needed by
    paintDiagram(). You use it to find out the smallest possible
    margins of a diagram. This is not yet a complete
    implementation. */


public class MeteredGraphics extends Graphics2D {
    BufferedImage im;
    Graphics2D g;

    /** The bounds are stored in post-transformation coordinates, but
        getBounds() converts them back to user coordinates. */
    Rectangle2D.Double bounds;
    double accuracy = 0.1;

    public MeteredGraphics() {
        im = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        g = im.createGraphics();
    }

    public MeteredGraphics(MeteredGraphics orig) {
        im = orig.im;
        accuracy = orig.accuracy;
        g = (Graphics2D) orig.g.create();
        if (orig.bounds != null) {
            bounds = Geom.createRectangle2DDouble(orig.bounds);
        }
    }

    @Override public void addRenderingHints(Map<?,?> hints) {
        g.addRenderingHints(hints);
    }

    @Override public void clip(Shape arg0) {
        g.clip(arg0);
    }

    @Override public void draw(Shape s) {
        fill(getStroke().createStrokedShape(s));
    }

    @Override public void draw3DRect(int x, int y, int width, int height,
            boolean raised) {
        drawRect(x, y, width, height);
    }

    @Override public void drawGlyphVector(GlyphVector g, float x, float y) {
        fill(g.getOutline(x, y));
    }

    @Override public void drawImage(BufferedImage arg0,
                                    BufferedImageOp arg1, int arg2,
                                    int arg3) {
        System.err.println("Ignoring drawImage#2");
        // TODO Measure
    }

    @Override public boolean drawImage(Image arg0, AffineTransform arg1,
                                       ImageObserver arg2) {
        System.err.println("Ignoring drawImage#1");
        // TODO Measure
        return false;
    }

    @Override public void drawRenderableImage
        (RenderableImage arg0, AffineTransform arg1) {
        System.err.println("Ignoring drawRenderableImage#1");
        // TODO Measure
    }

    @Override public void drawRenderedImage
        (RenderedImage arg0, AffineTransform arg1) {
        System.err.println("Ignoring drawRenderedImage#1");
        // TODO Measure
    }

    @Override public void drawString
        (AttributedCharacterIterator iterator, float x, float y) {
        System.err.println("Ignoring drawString#z");
        // TODO Measure
    }

    @Override public void drawString
        (AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float) x, (float) y);
    }

    @Override public void drawString(String str, float x, float y) {
        drawGlyphVector
            (getFont().createGlyphVector(getFontRenderContext(), str),
             x, y);
    }

    @Override public void drawString(String arg0, int arg1, int arg2) {
        drawString(arg0, (float) arg1, (float) arg2);
    }

    @Override public void fill(Shape s) {
        Rectangle2D.Double b = PathParam2D.create(
                s.getPathIterator(getTransform(), getAccuracy()))
            .getBounds();
        if (b == null) {
            return;
        }
        if (bounds == null) {
            bounds = b;
        } else {
            bounds.add(b);
        }
    }

    @Override public void fill3DRect(int x, int y, int width, int height,
            boolean raised) {
        fillRect(x, y, width, height);
    }

    @Override public Color getBackground() {
        return g.getBackground();
    }

    @Override public Composite getComposite() {
        return g.getComposite();
    }

    @Override public GraphicsConfiguration getDeviceConfiguration() {
        return g.getDeviceConfiguration();
    }

    @Override public FontRenderContext getFontRenderContext() {
        return g.getFontRenderContext();
    }

    @Override public Paint getPaint() {
        return g.getPaint();
    }

    @Override public Object getRenderingHint(Key arg0) {
        return g.getRenderingHint(arg0);
    }

    @Override public RenderingHints getRenderingHints() {
        return g.getRenderingHints();
    }

    @Override public Stroke getStroke() {
        return g.getStroke();
    }

    @Override public AffineTransform getTransform() {
        return g.getTransform();
    }

    @Override public boolean hit(Rectangle arg0, Shape arg1, boolean arg2) {
        return g.hit(arg0, arg1, arg2);
    }

    @Override public void rotate(double arg0) {
        g.rotate(arg0);
    }

    @Override public void rotate(double arg0, double arg1, double arg2) {
        g.rotate(arg0, arg1, arg2);
    }

    @Override public void scale(double arg0, double arg1) {
        g.scale(arg0, arg1);
    }

    @Override public void setBackground(Color arg0) {
        g.setBackground(arg0);
    }

    @Override public void setComposite(Composite arg0) {
        g.setComposite(arg0);
    }

    @Override public void setPaint(Paint arg0) {
        g.setPaint(arg0);
    }

    @Override public void setRenderingHint(Key arg0, Object arg1) {
        g.setRenderingHint(arg0, arg1);
    }

    @Override public void setRenderingHints(Map<?, ?> arg0) {
        g.setRenderingHints(arg0);
    }

    @Override public void setStroke(Stroke arg0) {
        g.setStroke(arg0);
    }

    @Override public void setTransform(AffineTransform arg0) {
        g.setTransform(arg0);
    }

    @Override public void shear(double arg0, double arg1) {
        g.shear(arg0, arg1);
    }

    @Override public void transform(AffineTransform arg0) {
        g.transform(arg0);
    }

    @Override public void translate(double arg0, double arg1) {
        g.translate(arg0, arg1);
    }

    @Override public void translate(int arg0, int arg1) {
        g.translate(arg0, arg1);
    }

    @Override public void clearRect(int arg0, int arg1, int arg2, int arg3) {
    }

    @Override public void clipRect(int arg0, int arg1, int arg2, int arg3) {
        g.clipRect(arg0, arg1, arg2, arg3);
    }

    @Override public void copyArea(int arg0, int arg1, int arg2, int arg3, int arg4,
                                           int arg5) {
        System.err.println("Ignoring copyArea()");
        // TODO I'm not sure how to handle this.
    }

    @Override public Graphics create() {
        return new MeteredGraphics(this);
    }

    @Override public Graphics create(int x, int y, int width, int height) {
        MeteredGraphics res = new MeteredGraphics(this);
        res.clipRect(x, y, width, height);
        res.translate(x, y);
        return res;
    }

    @Override public void dispose() {
        g.dispose();
        bounds = null;
        im = null;
        g = null;
    }

    @Override public void drawArc(int arg0, int arg1, int arg2, int arg3, int arg4,
                                          int arg5) {
        System.err.println("Ignoring drawArc()");
        // TODO Auto-generated method stub
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, ImageObserver arg3) {
        System.err.println("Ignoring drawImage#5()");
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, Color arg3,
                                               ImageObserver arg4) {
        System.err.println("Ignoring drawImage#6()");
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, int arg3,
                                               int arg4, ImageObserver arg5) {
        System.err.println("Ignoring drawImage#7()");
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, int arg3,
                                               int arg4, Color arg5, ImageObserver arg6) {
        System.err.println("Ignoring drawImage#8()");
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, int arg3,
            int arg4, int arg5, int arg6, int arg7, int arg8, ImageObserver arg9) {
        System.err.println("Ignoring drawImage#9()");
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, int arg3,
            int arg4, int arg5, int arg6, int arg7, int arg8, Color arg9,
            ImageObserver arg10) {
        System.err.println("Ignoring drawImage#10()");
        return false;
    }

    @Override public void drawLine(int arg0, int arg1, int arg2, int arg3) {
        draw(new Line2D.Double(arg0, arg1, arg2, arg3));
    }


    @Override public void drawOval(int arg0, int arg1, int arg2, int arg3) {
        draw(new Rectangle2D.Double(arg0, arg1, arg2, arg3));
    }

    @Override public void drawPolyline(int[] xs, int[] ys, int n) {
        if (n == 0) {
            return;
        }

        Path2D path = new Path2D.Double();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; ++i) {
            path.lineTo(xs[i], ys[i]);
        }            
            
        draw(path);
    }

    @Override public void drawPolygon(int[] xs, int[] ys, int n) {
        draw(new Polygon(xs, ys, n));
    }

    @Override public void fillPolygon(int[] xs, int[] ys, int n) {
        fill(new Polygon(xs, ys, n));
    }

    @Override public void fillPolygon(Polygon p) {
        fill((Shape) p);
    }

    @Override public void drawRoundRect(int x, int y, int width, int height,
                                        int arg4, int arg5) {
        drawRect(x, y, width, height);
    }

    @Override public void fillArc(int arg0, int arg1, int arg2, int arg3, int arg4,
                                          int arg5) {
        System.err.println("Ignoring fillArc#2()");
    }

    @Override public void fillOval(int arg0, int arg1, int arg2, int arg3) {
        fillRect(arg0, arg1, arg2, arg3);
    }

    @Override public void fillRect(int arg0, int arg1, int arg2, int arg3) {
        fill(new Rectangle2D.Double(arg0, arg1, arg2, arg3));
    }

    @Override public void fillRoundRect(int arg0, int arg1, int arg2, int arg3,
            int arg4, int arg5) {
        fillRect(arg0, arg1, arg2, arg3);
    }

    @Override public Shape getClip() {
        return g.getClip();
    }

    @Override public Rectangle getClipBounds() {
        return g.getClipBounds();
    }

    @Override public Color getColor() {
        return g.getColor();
    }

    @Override public Font getFont() {
        return g.getFont();
    }

    @Override public FontMetrics getFontMetrics(Font arg0) {
        return g.getFontMetrics(arg0);
    }

    @Override public void setClip(Shape arg0) {
        g.setClip(arg0);
    }

    @Override public void setClip(int arg0, int arg1, int arg2, int arg3) {
        g.setClip(arg0, arg1, arg2, arg3);
    }

    @Override public void setColor(Color arg0) {
        g.setColor(arg0);
    }

    @Override public void setFont(Font arg0) {
        g.setFont(arg0);
    }

    @Override public void setPaintMode() {
        g.setPaintMode();
    }

    @Override public void setXORMode(Color arg0) {
        g.setXORMode(arg0);
    }

    @Override public void drawChars(char[] data, int offset, int length,
                                    int x, int y) {
        StringBuilder b = new StringBuilder();
        for (int i = offset; i < offset+length; ++i) {
            b.append(data[i]);
        }
        drawString(b.toString(), x, y);
    }

    @Override public void drawBytes(byte[] data, int offset, int length,
                                    int x, int y) {
        StringBuilder b = new StringBuilder();
        for (int i = offset; i < offset+length; ++i) {
            b.append((char) data[i]);
        }
        drawString(b.toString(), x, y);
    }

    public Rectangle2D.Double getBounds() {
        if (bounds == null) {
            return null;
        }
        return (Rectangle2D.Double) bounds.clone();
    }

    @Override public FontMetrics getFontMetrics() {
        return g.getFontMetrics();
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }
}
