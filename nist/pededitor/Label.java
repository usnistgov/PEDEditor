/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JLabel;
import javax.swing.text.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Class to hold definitions of a text or HTML string anchored to a
    location in space and possibly drawn at an angle. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties
({"lineWidth", "lineStyle", "font"})
public class Label extends AnchoredTransformedShape {
    // The View size is independent of the resolution of the physical
    // device. Because the physical device might be high resolution, I
    // want the View to be high resolution, too.
    private static final int VIEW_MAGNIFICATION = 8; // = 100 px / 12.5 px
    static final double STANDARD_LABEL_BOX_WIDTH = 0.0010;
    
    static Font defaultFont = null;

    static class Margins {
        double x, y; // x margin, y margin
        double boxedX, boxedY; // x margin if boxed, y margin if boxed

        Margins(Font font) {
            View en = toView("n", 0, null, font);
            x = boxedX = en.getPreferredSpan(View.X_AXIS) 
                / 3.0 / VIEW_MAGNIFICATION;
            y = 0;
            boxedY = en.getPreferredSpan(View.Y_AXIS)
                / 8.0 / VIEW_MAGNIFICATION;
        }
    }


    private Font font;
    transient private View view = null;
    transient private Margins margins = null;
    /** The actual string to display. (This may be HTML or something
        else as opposed to plain text.) */
    private String text;
    boolean boxed = false;
    boolean opaque = false;
    boolean cutout = false;
    boolean autoWidth = false;

    public Label() {
    }

    public Label
        (@JsonProperty("text") String text,
         @JsonProperty("xWeight") double xWeight,
         @JsonProperty("yWeight") double yWeight) {
        super(xWeight, yWeight);
        this.text = text;
        font = defaultFont;
    }

    /** Make this like other. */
    public void copyFrom(Label other) {
        setAngle(other.getAngle());
        setAutoWidth(other.isAutoWidth());
        setBoxed(other.isBoxed());
        setColor(other.getColor());
        setColor(other.getColor());
        setCutout(other.isCutout());
        setFont(other.getFont());
        setOpaque(other.isOpaque());
        setScale(other.getScale());
        setText(other.getText());
        setX(other.getX());
        setXWeight(other.getXWeight());
        setY(other.getY());
        setYWeight(other.getYWeight());
        view = other.view;
        margins = other.margins;
    }

    @Override public Label clone() {
        Label res = new Label();
        res.copyFrom(this);
        return res;
    }

    Margins getMargins() {
        if (margins == null) {
            margins = new Margins(getFont());
        }
        return margins;
    }

    /* The actual font size is font's size times getScale() times
       Diagram.BASE_SCALE. Yeah, that dependency is a little ugly, and
       maybe Diagram should depend on Label instead of vice
       versa. */
    public void setFont(Font font) {
        if (font != this.font) {
            view = null;
            margins = null;
            this.font = font;
        }
    }

    public Font getFont() {
        return font;
    }

    public void setText(String text) {
        this.text = text;
        view = null;
    }
    public void setAutoWidth(boolean v) { autoWidth = v; }
    /** If true, draw a box around the label. */
    public void setBoxed(boolean boxed) { this.boxed = boxed; }
    /** If true, erase the label's background before drawing. */
    public void setOpaque(boolean opaque) { this.opaque = opaque; }
    public void setCutout(boolean v) { cutout = v; }

    public String getText() { return text; }

    public boolean isAutoWidth() { return autoWidth; }
   /** If true, draw a box around the label. */
    public boolean isBoxed() { return boxed; }
    /** If true, erase the label's background before drawing. */
    public boolean isOpaque() { return opaque; }
    public boolean isCutout() { return cutout; }

    @Override public void reflect() {
        setYWeight(1.0 - getYWeight());
        setText(SwapWhitespace.swap(getText()));
        view = null;
    }

    @Override public void neaten(AffineTransform toPage) {
        double theta = Geom.transformRadians(toPage, angle);
        if (theta != MathWindow.normalizeRadians180(theta)) {
            // Rotate the text 180 degrees so it's not pointing
            // to the left.
            angle = Geom.normalizeRadians(Math.PI + angle);
            setXWeight(1.0 - getXWeight());
            setYWeight(1.0 - getYWeight());
            setText(SwapWhitespace.swap(getText()));
            // Ideally I would also swap left and right padding (and
            // not just top and bottom padding), but that's more work.
            view = null;
        }
    }

    @Override public String toString() {
        return "'" + text + "' x: " + x  + " y: " + y + " wx: " + xWeight
            + " wy: " + yWeight + " angle: " + angle + " scale: " + scale
            + " boxed: " + boxed + " opaque: " + opaque + "autowidth: " + autoWidth;
    }

    // OBSOLESCENT
    @JsonProperty("fontSize") void setFontSize(double fontSize) {
        setScale(fontSize);
    }

    @Override public void setXWeight(double x) {
        super.setXWeight(x);
        view = null;
    }
    @Override public void setColor(Color color) {
        super.setColor(color);
        view = null;
    }

    @JsonIgnore public View getView() {
        if (view == null) {
            view = toView();
        }
        return view;
    }

    /** A metric for the size of d that compromises between minimum
        perimeter and minimum area, such that 3x0 and 1x1 are considered equally bad. */
    private static double size(View v) {
        Dimension2D d = dimension(v);
        double w = d.getWidth();
        double h = d.getHeight();
        return 9.0 * (w+h) * (w+h) - 5.0 * (w-h) * (w-h);
    }

    View toView() {
        String text = getText();
        double xw = getXWeight();
        Color c = getColor();
        View wideView = toView(text, xw, c, font);
        if (isAutoWidth()) {
            // Find the CSS width specifier that minimizes size(View)

            View thinView = toView(text, xw, c, font, 1);
            double thinWidth = dimension(thinView).getWidth();
            double wideWidth = dimension(wideView).getWidth();
            if (thinWidth == wideWidth) {
                return wideView;
            }
            View bestView = null;
            double leastSize = 0;
            for (double width = thinWidth; ; width *= 1.1) {
                View thisView = toView(text, xw, c, font, width);
                double size = size(thisView);
                Dimension2D dim = dimension(thisView);
                if (bestView == null || size < leastSize) {
                    leastSize = size;
                    bestView = thisView;
                }
                if (dim.getWidth() >= wideWidth) {
                    break;
                }
            }
            return bestView;
        } else {
            return wideView;
        }
    }

    /** @param xWeight Used to determine how to justify rows of text. */
    static View toView(String str, double xWeight, Color textColor, Font f) {
        return toView(str, xWeight, textColor, f, 0);
    }

    /** Convert htmlStr to a View.
        @param xWeight Determines how to justify rows of text.
    */
    static View toView(String htmlStr, double xWeight, Color textColor, Font f,
            double width) {
        String style
            = "<style type=\"text/css\"><!--"
            + " body { font-size: 100 pt; } "
            + " sub { font-size: 75%; } "
            + " sup { font-size: 75%; } "
            + " --></style>";

        htmlStr = NestedSubscripts.unicodify(htmlStr);

        StringBuilder sb = new StringBuilder("<html><head");
        sb.append(style);
        sb.append("</head><body");
        if (width > 0) {
            sb.append(" style=\"width:" + width + "px\"");
        }
        sb.append(">");

        if (xWeight >= 0.67) {
            sb.append("<div align=\"right\">");
            sb.append(htmlStr);
            sb.append("</div>");
        } else if (xWeight >= 0.33) {
            sb.append("<div align=\"center\">");
            sb.append(htmlStr);
            sb.append("</div>");
        } else {
            sb.append(htmlStr);
        }
        htmlStr = sb.toString();

        JLabel bogus = new JLabel(htmlStr);
        bogus.setFont(f);
        if (textColor == null) {
            textColor = Color.BLACK;
        }
        bogus.setForeground(textColor);
        return (View) bogus.getClientProperty("html");
    }
    
    @JsonIgnore public Point2D.Double getCenter() {
        Point2D.Double res = new Point2D.Double(0.5, 0.5);
        labelToScaledPage(null, 1.0).transform(res, res);
        return res;
    }

    Dimension2D dimension() {
        if (isCutout()) {
            // TODO Does it save time to cache the big font?
            Rectangle2D bounds = font.getStringBounds
                (getText(), new FontRenderContext(null, true, false));
            return new Dimension2DDouble(bounds);
        } else {
            return dimension(getView());
        }
    }

    static Dimension2D dimension(View view) {
        return new Dimension2DDouble(
                view.getPreferredSpan(View.X_AXIS) / VIEW_MAGNIFICATION,
                view.getPreferredSpan(View.Y_AXIS) / VIEW_MAGNIFICATION);
    }

    /** @return a transformation that maps the unit square to the
        outline of the label in scaled physical space. */
    AffineTransform labelToScaledPage(AffineTransform toPage, double scale) {
        Dimension2D dimension = dimension();
        Point2D.Double point = new Point2D.Double(getX(), getY());
        double angle = getAngle();
        if (toPage != null) {
            toPage.transform(point, point);
            angle = Geom.transformRadians(toPage, angle);
        }

        return labelToScaledPage(dimension.getWidth(), dimension.getHeight(),
                scale * getScale(), angle, point.x * scale, point.y * scale,
                getXWeight(), getYWeight(), isBoxed(), getMargins());
    }

    /** @return a transformation that maps the unit square to the
        outline of this label in scaled page space. */
    static AffineTransform labelToScaledPage
        (double width, double height, double scale, double angle,
         double ax, double ay, double xWeight, double yWeight,
         boolean boxed, Margins margins) {
        width += (boxed ? margins.boxedX : margins.x) * 2;
        height += (boxed ? margins.boxedY : margins.y) * 2;
        double textScale = scale / Diagram.BASE_SCALE;

        AffineTransform res = AffineTransform.getTranslateInstance(ax, ay);
        res.rotate(angle);
        res.scale(textScale, textScale);
        res.scale(width, height);
        res.translate(-xWeight, -yWeight);
        return res;
    }
        
    @Override public void draw(Graphics2D g, double scale) {
        draw(g, null, scale);
    }

    @Override public void draw(Graphics2D g, AffineTransform toPage, double scale) {
        double labelScale = scale * getScale();
        if (labelScale == 0) {
            return;
        }
        if (getFont() == null)
            setFont(g.getFont());

        AffineTransform l2s = labelToScaledPage(toPage, scale);
        Path2D.Double box = htmlBox(l2s);

        if (isOpaque()) {
            Color oldColor = g.getColor();
            g.setColor(Color.WHITE);
            g.fill(box);
            g.setColor(oldColor);
        }

        if (isBoxed()) {
            float width = (float) (STANDARD_LABEL_BOX_WIDTH * labelScale);
            g.setStroke(new BasicStroke(width));
            g.draw(box);
        }

        // TODO: Exploit the labelToScaledPage transformation in htmlDraw.
        View view = getView();
        Point2D.Double point = new Point2D.Double(getX(), getY());
        double angle = getAngle();
        if (toPage != null) {
            toPage.transform(point, point);
            angle = Geom.transformRadians(toPage, angle);
        }

        if (isCutout()) {
            Font oldFont = g.getFont();
            double fs = labelScale * normalFontSize();
            g.setFont(oldFont.deriveFont((float) fs));
            g.setColor(Color.WHITE);

            double lx = (isBoxed() ? margins.boxedX : margins.x)
                * labelScale / Diagram.BASE_SCALE;
            double ly = (isBoxed() ? margins.boxedY : margins.y)
                * labelScale / Diagram.BASE_SCALE;

            // TODO The box margins still don't match up...
            Shapes.drawString(g, getText(), point.x * scale, point.y * scale,
                    getXWeight(), getYWeight(), lx, ly, angle, true);
            Color color = getColor();
            if (color == null)
                color = Color.BLACK;
            g.setColor(color);
            Shapes.drawString(g, getText(), point.x * scale, point.y * scale,
                    getXWeight(), getYWeight(), lx, ly, angle);
            g.setFont(oldFont);
        } else {
            htmlDraw(g, view, labelScale, angle,
                    point.x * scale, point.y * scale,
                    getXWeight(), getYWeight(), getMargins());
        }
    }

    /**
       @param view The view.paint() method is used to perform the
       drawing (the decorated text to be encoded is implicitly
       included in this parameter)

       @param scale The text is magnified by this factor before being
       painted.

       @param angle The printing angle. 0 = running left-to-right (for
       English); rotated 90 degrees clockwise (running downwards)

       @param ax The X position of the anchor point

       @param ay The Y position of the anchor point

       @param xWeight 0.0 = The anchor point lies along the left edge
       of the text block in baseline coordinates (if the text is
       rotated, then this edge may not be on the left in physical
       coordinates; for example, if the text is rotated by an angle of
       PI/2, then this will be the top edge in physical coordinates);
       0.5 = the anchor point lies along the vertical line (in
       baseline coordinates) that bisects the text block; 1.0 = the
       anchor point lies along the right edge (in baseline
       coordinates) of the text block

       @param yWeight 0.0 = The anchor point lies along the top edge
       of the text block in baseline coordinates (if the text is
       rotated, then this edge may not be on top in physical
       coordinates; for example, if the text is rotated by an angle of
       PI/2, then this will be the right edge in physical
       coordinates); 0.5 = the anchor point lies along the horizontal
       line (in baseline coordinates) that bisects the text block; 1.0
       = the anchor point lies along the bottom edge (in baseline
       coordinates) of the text block

       This method appears to be almost free of side effects and side
       references to "this", except that initializeLabelMargins()
       calls getFont(), which it probably shouldn't do anyway because
       it should use g.getFont() instead.
    */
    void htmlDraw(Graphics g, View view, double scale, double angle,
                  double ax, double ay,
                  double xWeight, double yWeight, Margins margins) {
        scale /= VIEW_MAGNIFICATION;
        double baseWidth = view.getPreferredSpan(View.X_AXIS);
        double baseHeight = view.getPreferredSpan(View.Y_AXIS);
        double width = baseWidth + VIEW_MAGNIFICATION * margins.x * 2;
        double height = baseHeight + VIEW_MAGNIFICATION * margins.y * 2;

        Graphics2D g2d = (Graphics2D) g;
        double textScale = scale / Diagram.BASE_SCALE;

        AffineTransform baselineToPage = AffineTransform.getRotateInstance(angle);
        baselineToPage.scale(textScale, textScale);
        Point2D.Double xpoint = new Point2D.Double();
        baselineToPage.transform
            (new Point2D.Double(width * xWeight, height * yWeight), xpoint);

        ax -= xpoint.x;
        ay -= xpoint.y;

        // Now (ax, ay) represents the (in baseline coordinates) upper
        // left corner of the text block expanded by the x- and
        // y-margins.

        {
            // Displace (ax,ay) by (labelXMargin, labelYMargin) (again, in baseline
            // coordinates) in order to obtain the true upper left corner
            // of the text block.

            baselineToPage.transform
                (new Point2D.Double(VIEW_MAGNIFICATION * margins.x,
                                    VIEW_MAGNIFICATION * margins.y),
                 xpoint);
            ax += xpoint.x;
            ay += xpoint.y;
        }

        // Paint the view after creating a transform in which (0,0)
        // maps to (ax,ay)

        AffineTransform oldxform = g2d.getTransform();
        g2d.translate(ax, ay);
        g2d.transform(baselineToPage);

        // Don't pass a rectangle that is larger than necessary to
        // view.paint(), or else view.paint() will attempt to center
        // the label on its own, but it won't recenter the way we
        // want.
        Rectangle r = new Rectangle
            (0, 0, (int) Math.ceil(baseWidth), (int) Math.ceil(baseHeight));

        // The views seem to require non-null clip bounds for some
        // dumb reason, and the SVG uses no clip region, so... TODO
        // Still needed?!
        if (g.getClipBounds() == null) {
            g.setClip(-1000000, -1000000, 2000000, 2000000);
        }

        try {
            view.paint(g, r);
        } catch (NullPointerException e) {
            System.out.println("Clip = " + g2d.getClipBounds());
            System.out.println("R = " + r);
            throw(e);
        }
        g2d.setTransform(oldxform);
    }


    /** Create a box in the space that the transform of the unit
        square would enclose. */
    Path2D.Double htmlBox(AffineTransform xform) {
        Point2D.Double[] corners =
            { new Point2D.Double(0,0),
              new Point2D.Double(1,0),
              new Point2D.Double(1,1),
              new Point2D.Double(0,1) };
        xform.transform(corners, 0, corners, 0, corners.length);
        Path2D.Double res = new Path2D.Double();
        res.moveTo(corners[0].x, corners[0].y);
        for (int i = 1; i < corners.length; ++i) {
            res.lineTo(corners[i].x, corners[i].y);
        }
        res.closePath();
        return res;
    }
    
    protected static double normalFontSize() {
        return Diagram.STANDARD_FONT_SIZE / Diagram.BASE_SCALE;
    }

    @JsonIgnore @Override
    public LabelHandle[] getHandles(DecorationHandle.Type type) {
        if (getXWeight() != 0.5 || getYWeight() != 0.5) {
            return new LabelHandle[]
                { new LabelHandle(this, LabelHandle.Type.ANCHOR),
                  new LabelHandle(this, LabelHandle.Type.CENTER) };
        } else {
            return new LabelHandle[]
                { new LabelHandle(this, LabelHandle.Type.ANCHOR) };
        }
    }

    @Override public String typeName() {
        return "Label";
    }
}
