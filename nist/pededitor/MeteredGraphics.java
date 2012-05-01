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
import java.awt.geom.PathIterator;
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
        g = (Graphics2D) orig.g.create();
        if (orig.bounds != null) {
            bounds = Duh.createRectangle2DDouble(orig.bounds);
        }
    }

    @Override public void draw(Shape s) {
        fill(getStroke().createStrokedShape(s));
    }

    public Rectangle2D.Double getBounds() {
        if (bounds == null) {
            return null;
        }
        return (Rectangle2D.Double) bounds.clone();
    }

    @Override public void addRenderingHints(Map<?, ?> arg0) {
        g.addRenderingHints(arg0);
    }

    @Override public void clip(Shape arg0) {
        g.clip(arg0);
    }

    @Override public void drawGlyphVector
        (GlyphVector arg0, float arg1, float arg2) {
        // TODO Measure?
        // g.drawGlyphVector(arg0, arg1, arg2);
    }

    @Override public boolean drawImage(Image arg0, AffineTransform arg1,
                                       ImageObserver arg2) {
        // TODO Measure?
        // drawImage(arg0, arg1, arg2);
    	return false;
    }

    @Override public void drawImage(BufferedImage arg0,
                                    BufferedImageOp arg1, int arg2,
                                    int arg3) {
        // TODO Measure?
        // drawImage(arg0, arg1, arg2, arg3);
    }

    @Override public void drawRenderableImage
        (RenderableImage arg0, AffineTransform arg1) {
        // TODO Auto-generated method stub
    }

    @Override public void drawRenderedImage
        (RenderedImage arg0, AffineTransform arg1) {
        // TODO Measure?
    }

    @Override public void drawString(String arg0, int arg1, int arg2) {
        drawString(arg0, (float) arg1, (float) arg2);
    }

    @Override public void drawString(String str, float x, float y) {
        // Should actually draw the string rather than
        // trusting getStringBounds() to be correct, but whatever.
        FontMetrics fm = getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(str, g);
        fill(new Rectangle2D.Double
             (bounds.getX() + x, bounds.getY() + y,
              bounds.getWidth(), bounds.getHeight()));
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    @Override public void fill(Shape s) {
        PathIterator pit = s.getPathIterator(getTransform(), getAccuracy());
        float[] coords = new float[6];
        for (; !pit.isDone(); pit.next()) {
            int pointCount = 0;
            switch (pit.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                pointCount = 1;
                break;
            case PathIterator.SEG_LINETO:
                pointCount = 1;
                break;
            case PathIterator.SEG_QUADTO:
                pointCount = 2;
                break;
            case PathIterator.SEG_CUBICTO:
                pointCount = 3;
                break;
            default:
                pointCount = 0;
                break;
            }

            for (int i = 0; i < pointCount; ++i) {
                float x = coords[i*2];
                float y = coords[i*2+1];
                if (bounds == null) {
                    bounds = new Rectangle2D.Double(x, y, 0, 0);
                } else {
                    bounds.add(x, y);
                }
            }
        }
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

    @Override public void translate(int arg0, int arg1) {
        g.translate(arg0, arg1);
    }

    @Override public void translate(double arg0, double arg1) {
        g.translate(arg0, arg1);
    }

    @Override public void clearRect(int arg0, int arg1, int arg2, int arg3) {
    }

    @Override public void clipRect(int arg0, int arg1, int arg2, int arg3) {
        g.clipRect(arg0, arg1, arg2, arg3);
    }

    @Override public void copyArea(int arg0, int arg1, int arg2, int arg3, int arg4,
                                           int arg5) {
        // TODO I'm not sure how to handle this.
    }

    @Override public Graphics create() {
        return new MeteredGraphics(this);
    }

    @Override public void dispose() {
        g.dispose();
        bounds = null;
        im = null;
        g = null;
    }

    @Override public void drawArc(int arg0, int arg1, int arg2, int arg3, int arg4,
                                          int arg5) {
        // TODO Auto-generated method stub
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, ImageObserver arg3) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, Color arg3,
                                               ImageObserver arg4) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, int arg3,
                                               int arg4, ImageObserver arg5) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, int arg3,
                                               int arg4, Color arg5, ImageObserver arg6) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, int arg3,
                                               int arg4, int arg5, int arg6, int arg7, int arg8, ImageObserver arg9) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override public boolean drawImage(Image arg0, int arg1, int arg2, int arg3,
                                               int arg4, int arg5, int arg6, int arg7, int arg8, Color arg9,
                                               ImageObserver arg10) {
        // TODO Auto-generated method stub
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

    @Override public void drawRoundRect(int x, int y, int width, int height,
                                        int arg4, int arg5) {
        drawRect(x, y, width, height);
    }

    @Override public void fillArc(int arg0, int arg1, int arg2, int arg3, int arg4,
                                          int arg5) {
        // TODO Auto-generated method stub
                
    }

    @Override public void fillOval(int arg0, int arg1, int arg2, int arg3) {
        fillRect(arg0, arg1, arg2, arg3);
    }

    @Override public void fillRect(int arg0, int arg1, int arg2, int arg3) {
        fill(new Rectangle2D.Double(arg0, arg1, arg2, arg3));
    }

    @Override public void fillRoundRect(int arg0, int arg1, int arg2, int arg3, int arg4,
                                                int arg5) {
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
        // TODO Auto-generated method stub
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

    @Override public void drawString
        (AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float) x, (float) y);
    }

    @Override public void drawString
        (AttributedCharacterIterator iterator, float x, float y) {
        // TODO Measure?
    }
}