package gov.nist.pededitor;

import javax.imageio.ImageIO;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.View;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import Jama.Matrix;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;

import org.w3c.dom.DOMImplementation;

// TODO Investigate whether JavaFX is really a plausible alternative.

// TODO (bug) The decorations are messed up in the label dialog in XP.
// Check whether the same problem exists in Windows 7.

// TODO (bug) List user-defined variables loaded from files in the
// "delete" list (actually list everything except page X/page Y and
// the ternary diagram variables).

// TODO Allow users to adjust the text angle in the ruler edit dialog.

// TODO (mandatory, but not clear whose end -- mine or Prometheus' --
// where it should happen) HTML-to-plaintext conversion. Users who
// search for "H2SO4" find "H<sub>2</sub>SO<sub>4</sub>". An extra
// bonus, if done within the PED Editor, is that it makes the mole
// percent compound entry more flexible; you can take an existing
// label and compute its proper location without retyping. This is
// both (1) already solved by a number of different libraries and (2)
// not too hard to do from scratch in a somewhat slipshod way.

// TODO (optional) Auto-save the diagram at regular intervals.

// TODO Allow users to redefine the third component of a ternary, for
// situations where the ternary is actually a slice of a 4-component
// system and the three components do not actually sum to 100%.

// TODO (optional) Fill styles. (Right now the only fill style is
// "solid".) This is pretty easy.

// TODO (optional) Eutectic and peritectic points.

// TODO (mandatory?, preexisting) Apply a gradient to all control
// points on a curve. Specifically, apply the following transformation
// to all points on the currently selected curve for which $variable
// is between v1 and v2: "$variable = $variable + k * ($variable - v1)
// / (v2 - v1)"

// TODO (Optional) Make the GRUMP conversion font sans serif instead of serif.

// TODO (Optional) Fix symbol alignment for GRUMP font. (Symbols are
// almost centered already, but getting them exactly right would be
// easy. They are already correct in the PED font.)

// TODO (Optional) Make regular open symbols look as nice as the
// GRUMP-converted open symbols do. (JavaFX might fix this issue.)

// TODO (Optional) Better support for new double subscripts and
// changing font sizes within a single label. (JavaFX might fix this
// issue.)

// TODO (optional) You can't make tie lines that cross the "endpoint"
// of a closed curve. Fix this somehow.

// TODO: At this point, the rule that tie lines have to end at
// vertexes of the diagram is no longer needed and not difficult to
// eliminate. Tie lines ending on rulers without extra steps could
// also be enabled (and the extra steps are unintuitive).

// TODO (optional) For opaque and boxed labels, allow users to decide
// how much extra white space to include on each side.

// TODO (optional) Make it easier to edit multiple-line labels.

// TODO (Optional) Add font hints to make labels look better on screen
// (Probably a bad idea: it would increase margin errors in the image
// the digitizers look at)

// TODO (optional) Set of commonly used shapes (equilateral triangle,
// square, circle) whose scale and orientation are defined by
// selecting two points. Ideally the changes would be visible in real
// time as you move the mouse around. (Auto-positioning has made this
// slightly less important than before; rectangles and equilateral
// triangles -- though not squares -- are easy to do now.)

// TODO (optional) Support of GRUMP-style explicit assignment of ruler
// limits such as label skip, label density, and so on. The downside
// of this is that it would make the interface even more complicated,
// and people might get in the habit of doing explicit assignments
// even though the automatic ones are pretty good.

// TODO (optional) Allow the diagram domain and range to be expanded.
// Right now, you can expand the margins, change the aspect ratio, or
// rescale the axes, but it is awkward to extend a partial ternary to
// create a full ternary, for example. The 'move region' command is an
// OK work-around, at least for binary diagrams.

// TODO (optional) It should be easy to enable 'copy region' that is
// analogous to 'move region'. Maybe the user should just be asked
// whether to move a region or just a single curve.

// TODO (optional) Resize all labels at once by a given factor. This
// is more useful during conversion from GRUMP to PED fonts.

// TODO (optional) Curve tags, such as temperature, liquidus, solidus, and
// presumably user-defined tags too.

// TODO (optional) Point tags: pen-up, pen-down, temperature

// TODO (optional) As Chris suggested, allow input images to be
// rotated (currently images must be within about 45 degrees of the
// correct orientation). This is hardly a show-stopper; you can load
// the image in MS-Paint and rotate it in a minute, and Chris said
// that was a good solution.

// TODO (optional) Right-click popup menus. I don't think experienced
// users (the digitizers) would make much use of them, but occasional
// users might. Forcing the users to remember shortcuts isn't very
// friendly, and you can't use the mouse to click on an ordinary menu
// if you're already using the mouse to identify the location the
// operation should apply to.

// TODO (feature, a day or two) Allow detection of the intersections
// of two splines. (What makes this feature more desirable than it
// would be otherwise is that it would create consistency. As long as
// some kinds of intersections are detectable, people will sometimes
// forget that other kinds are not detectable and be confused when
// that does not work. (I did that myself at least once, even though I
// wrote the program.)

// TODO (optional) Better curve fitting. As I believe Don mentioned,
// following the control points too slavishly can yield over-fitting
// in which you end up mapping noise (experimental error, scanner
// noise, twitches in the hand of the digitizer or the person who drew
// the image in the first place, or whatever). Peter's program's
// fitting is simple but not great.

// Heuristics may be used to identify good cutoffs for fit quality. At
// the over-fitting end of the spectrum, if you find the sum of the
// squares of the fit error terms dropping only in rough proportion
// to the difference of the number of data points and the number of
// degrees of freedom in the fit, then that indicates that your fit
// method cannot detect any kind of pattern in the data at that degree
// of precision, so short of doing a perfect fit (smoothing), there is
// little point in trying to fit the data any more accurately than
// that. There may also be points further back in the curve where the
// slope of the sum of squares of the fit error terms as a function of
// the number of degrees of freedom in the fit definition becomes more
// shallow, and just before any such turn would be a good candidate
// for a fit. (For example, to fit a bumpy oval shape, one might find
// that a perfect ellipse (5 degrees of freedom in 2D) provides a much
// better fit than 4 degrees of freedom does and not much worse than 7
// degrees of freedom does, so the perfect ellipse is a good choice.)

// TODO "Undo" option. Any good drawing program has that feature, but
// making everything undoable would take a couple weeks. (Every time
// an operation that changes the diagram is completed, push the
// inverse operation onto a stack.)

// TODO (optional) More compact representation for symbol sets in the
// PED format.

// TODO (preexisting in viewer) Periodic table integration.

// TODO (optional) weight vs mole percent enhancements. If anybody
// actually wants to convert mole percent ternary diagrams to weight
// percent, then straight lines will have to be bent into curves. A
// ruler that shows weight percent would also be a nice feature, and
// so would the corresponding variables. Finally, text at an angle
// gets distorted, and that should really be fixed.

// TODO (preexisting but not mandatory) Smart line dash lengths. Peter
// Schenk's PED Editor adjusts the segment length for dashed lines to
// insure that the dashes in dashed curves are always enough shorter
// than the curves themselves that at least two dashes are visible.
// It's a nice feature, but is it worth it to reproduce? The lengths
// of the dashes in dashed lines is proportional to their thickness,
// so you can make especially short dashes by using especially thin
// lines already. (Using the sum of the chord lengths as a lower bound
// on the length of the whole would achieve the goal of insuring at
// least two dashes are visible, but would not guarantee that the
// dashes end neatly at both endpoints. (Java2D already has its own
// estimate of the path length -- and it would actually be better to
// use its estimate than to do a better but different estimate of
// one's own -- but I don't know how practical it would be to access
// that information.)

// TODO (optional) More general gradients, e.g. identify a polygon (or
// circle?) and a point in the interior of that polygon that is to be
// warped to some other point inside the polygon. The boundary should
// remain the same, and the warping should apply to the scanned input
// image as well. For polygonal regions, the transform used could be
// that the interior point is used to decompose the polygon into ( #
// of sides ) triangular regions, and each of those regions is
// subjected to the affine transformation that preserves its external
// edge while transforming the interior point to its new location.
// (The result of such a transformation might not be pretty; any line
// passing through the warped region could end up with sharp bends in
// it. However, if the line is defined by its endpoints only to begin
// with, you'll never see that.)

// TODO (major) Visual location of nearest point on scanned diagram. (The
// mouse can already be attracted to the nearest feature in the final
// version of the diagram.) Complicating factors include noise and
// specs in the scanned image, the need to infer the width of lines,
// and that the simplest smoothing algorithms for edge detection (such
// as Gaussian smoothing) tend to yield results that are biased
// towards centers of curvature. You can't just mindlessly apply basic
// edge-finding algorithms and have the answer pop right out. If the
// return value is off by even half of a line width, then the feature
// is almost worthless.

// TODO (major) Visual location of curves in the scanned diagram.
// Problems are much the same as for the previous section, plus more
// processor speed constraints. (A lot of vision problems could be
// stated in terms of optimization a function of the form
// function(photo(vector of features)) -- multidimensional
// optimization of the feature vector space where evaluating the
// function even once requires transforming thousands or millions of
// pixels of the scanned image. Multidimensional optimization of an
// ordinary function can be kind of expensive, but in this case
// computing the function just once, for a single feature vector,
// requires transforming thousands or millions of pixels. Duh,
// computer vision can be expensive, and how much of our own brains
// are dedicated to vision-related tasks?)

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class Editor implements CropEventListener, MouseListener,
                               MouseMotionListener, Printable {
    static ObjectMapper objectMapper = null;

    abstract class Action extends AbstractAction {
        private static final long serialVersionUID = 1834208008403586162L;

        Action(String name) {
            super(name);
        }
    }

    class ImageBlinker extends TimerTask {
        public void run() {
            backgroundImageEnabled = !backgroundImageEnabled;
            repaintEditFrame();
        }
    }

    class CloseListener extends WindowAdapter
                                implements WindowListener
    {
        public void windowClosing(WindowEvent e) {
            close();
        }
    }

    /** Series of classes that implement the Decoration and
        DecorationHandle interfaces so that different types of
        decorations, such as curves and labels, can be manipulated the
        same way. */

    class VertexHandle implements ParameterizableHandle {
        CurveDecoration decoration;
        int vertexNo;

        @Override public Parameterization2D getParameterization() {
            return getDecoration().getParameterization();
        }

        @Override public double getT() {
            return vertexNo;
        }

        public CurveDecoration getDecoration() {
            return decoration;
        }

        VertexHandle(int curveNo, int vertexNo) {
            this.decoration = new CurveDecoration(curveNo);
            this.vertexNo = vertexNo;
        }

        VertexHandle(CurveDecoration decoration, int vertexNo) {
            this.decoration = decoration;
            this.vertexNo = vertexNo;
        }

        @Override public boolean isEditable() { return false; }

        @Override public void edit() {
            throw new IllegalStateException("Can't edit " + this);
        }

        public GeneralPolyline getItem() {
            return getDecoration().getItem();
        }

        @Override
        public VertexHandle remove() {
            GeneralPolyline path = getItem();
            int oldVertexCnt = path.size();
            repaintEditFrame();
            saveNeeded = true;

            if (oldVertexCnt >= 2) {
                ArrayList<Double> segments = getPathSegments(path);

                // While deleting this vertex, adjust t values that
                // reference this segment. Previous segments that
                // don't touch point are left alone; following
                // segments that don't touch point have their
                // segmentNo decremented; and the two segments that
                // touch point are combined into a single segment
                // number newSeg. What a pain in the neck!

                if (oldVertexCnt == 2) {
                    for (int i = 0; i < segments.size(); ++i) {
                        segments.set(i, (double) 0);
                    }
                } else {
                    int segCnt = path.getSegmentCnt();

                    Point2D point = path.get(vertexNo);
                    int prevSeg = (vertexNo > 0) ? (vertexNo - 1)
                        : path.isClosed() ? (segCnt-1) : -1;
                    Point2D previous = path.get((prevSeg >= 0) ? prevSeg : 0);
                    int nextSeg = vertexNo;
                    Point2D next = path.get
                        ((!path.isClosed()  && (vertexNo == oldVertexCnt - 1))
                         ? vertexNo
                         : (vertexNo + 1));
                    int newSeg = (vertexNo > 0) ? (vertexNo - 1)
                        : path.isClosed() ? (segCnt - 2)
                        : 0;
                    // T values for segments prevSeg and nextSeg should be
                    // combined into a single segment newSeg.

                    double dist1 = point.distance(previous);
                    double dist2 = point.distance(next);
                    double splitT = dist1 / (dist1 + dist2);

                    for (int i = 0; i < segments.size(); ++i) {
                        double t = segments.get(i);
                        int segment = (int) Math.floor(t);
                        double frac = t - segment;
                        if (segment == prevSeg) {
                            t = newSeg + frac * splitT;
                        } else if (segment == nextSeg) {
                            t = newSeg + splitT + frac * (1 - splitT);
                        } else if (segment > vertexNo) {
                            --t;
                        }
                        segments.set(i, t);
                    }
                }

                path.remove(vertexNo);
                setPathSegments(path, segments);
                return new VertexHandle(decoration, 
                                        (vertexNo > 0) ? (vertexNo - 1) : 0);
            } else {
                getDecoration().remove();
                return null;
            }
        }

        @JsonIgnore public int getCurveNo() {
            return getDecoration().getCurveNo();
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + getCurveNo() + ", " + vertexNo + "]";
        }

        @Override
        public void move(Point2D target) {
            GeneralPolyline path = getItem();
            path.set(vertexNo, target);
            saveNeeded = true;
            repaintEditFrame();
        }

        @Override
        public VertexHandle copy(Point2D dest) {
            saveNeeded = true;
            Point2D.Double delta = getLocation();
            delta.x = dest.getX() - delta.x;
            delta.y = dest.getY() - delta.y;
            GeneralPolyline path = getItem().clone();
            for (int i = 0; i < path.size(); ++i) {
                Point2D.Double point = path.get(i);
                point.x += delta.x;
                point.y += delta.y;
                path.set(i, point);
            }
            paths.add(path);
            repaintEditFrame();
            return new VertexHandle(paths.size() - 1, vertexNo);
        }

        @Override
        public Point2D.Double getLocation() {
            return getItem().get(vertexNo);
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != VertexHandle.class) return false;

            VertexHandle cast = (VertexHandle) other;
            return vertexNo == cast.vertexNo
                && getDecoration().equals(cast.getDecoration());
        }
    }

    
    class CurveDecoration implements Decoration, Parameterizable2D {
        int curveNo;

        CurveDecoration(int curveNo) {
            this.curveNo = curveNo;
        }

        @Override public void setLineWidth(double lineWidth) {
            getItem().setLineWidth(lineWidth);
            saveNeeded = true;
            repaintEditFrame();
        }

        @Override public double getLineWidth() {
            return getItem().getLineWidth();
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            getItem().setStroke(lineStyle);
            saveNeeded = true;
            repaintEditFrame();
        }

        @Override public void setColor(Color color) {
            saveNeeded = true;
            getItem().setColor(color);
        }

        @Override public Color getColor() {
            return getItem().getColor();
        }

        public Path2D.Double getShape() {
            return getItem().getPath(principalToStandardPage);
        }

        GeneralPolyline getItem() {
            return paths.get(curveNo);
        }

        @Override public DecorationHandle remove() {
            removeCurve(curveNo);
            return null;
        }

        @Override public void draw(Graphics2D g, double scale) {
            GeneralPolyline item = getItem();
            if (item.size() == 1
                && item.getStroke() != StandardStroke.INVISIBLE) {
                // Draw a dot.
                double r = item.getLineWidth() * 2 * scale;
                circleVertices(g, item, scale, true, r);
            } else {
                item.draw(g, getPrincipalToAlignedPage(), scale);
            }
        }

        @Override public PathParam2D getParameterization() {
            return new PathParam2D(getShape());
        }

        public int getCurveNo() {
            return curveNo;
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + getCurveNo() + "]";
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != CurveDecoration.class) return false;

            CurveDecoration cast = (CurveDecoration) other;
            return this.curveNo == cast.curveNo;
        }

        @Override public DecorationHandle[] getHandles() {
            ArrayList<DecorationHandle> output = new ArrayList<>();
            GeneralPolyline path = getItem();
            for (int j = 0; j < path.size(); ++j) {
                output.add(new VertexHandle(this, j));
            }
            return output.toArray(new DecorationHandle[0]);
        }

        @Override public DecorationHandle[] getMovementHandles() {
            return getHandles();
        }

        /** Return the VertexHandle closest to path(t). */
        public VertexHandle getHandle(double t) {
            Parameterization2D c = getItem()
                .createTransformed(principalToStandardPage)
                .getParameterization();
            double ct = Parameterization2Ds.getNearestVertex(c, t);
            return new VertexHandle(this, (int) ct);
        }
    }

    static enum LabelHandleType { CENTER, ANCHOR };

    class LabelHandle implements DecorationHandle {
        LabelDecoration decoration;
        /** A tie line may be selected from its anchor or its center. */
        LabelHandleType handle;

        @Override public  LabelDecoration getDecoration() {
            return decoration;
        }

        LabelHandle(int index, LabelHandleType handle) {
            this.decoration = new LabelDecoration(index);
            this.handle = handle;
        }

        LabelHandle(LabelDecoration decoration, LabelHandleType handle) {
            this.decoration = decoration;
            this.handle = handle;
        }

        AnchoredLabel getItem() { return getDecoration().getItem(); }

        @Override public LabelHandle remove() {
            getDecoration().remove();
            return null;
        }

        @Override public void move(Point2D dest) {
            saveNeeded = true;
            Point2D.Double destAnchor = getAnchorLocation(dest);
            AnchoredLabel item = getItem();
            item.setX(destAnchor.getX());
            item.setY(destAnchor.getY());
            repaintEditFrame();
        }

        @Override public LabelHandle copy(Point2D dest) {
            saveNeeded = true;
            AnchoredLabel output = getItem().clone();
            Point2D.Double destAnchor = getAnchorLocation(dest);
            output.setX(destAnchor.getX());
            output.setY(destAnchor.getY());
            add(output);
            repaintEditFrame();
            return new LabelHandle(labels.size() - 1, handle);
        }

        @Override public Point2D.Double getLocation() {
            switch (handle) {
            case ANCHOR:
                return getDecoration().getAnchorLocation();
            case CENTER:
                return getDecoration().getCenterLocation();
            }
            return null;
        }

        @Override public boolean isEditable() { return true; }

        @Override public void edit() {
            editLabel(getDecoration().getIndex());
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != LabelHandle.class) {
                return false;
            }

            LabelHandle cast = (LabelHandle) other;
            return handle == cast.handle
                    && getDecoration().equals(cast.getDecoration());
        }

        public Point2D.Double getCenterLocation() {
            return getDecoration().getCenterLocation();
        }

        public Point2D.Double getAnchorLocation() {
            return getDecoration().getAnchorLocation();
        }

        /** @return the anchor location for a label whose handle is at
            dest. */
        public Point2D.Double getAnchorLocation(Point2D dest) {
            if (handle == LabelHandleType.ANCHOR) {
                return new Point2D.Double(dest.getX(), dest.getY());
            }

            // Compute the difference between the anchor
            // location and the center, and apply the same
            // difference to dest.
            Point2D.Double anchor = getAnchorLocation();
            Point2D.Double center = getCenterLocation();
            double dx = anchor.x - center.x;
            double dy = anchor.y - center.y;
            return new Point2D.Double(dest.getX() + dx, dest.getY() + dy);
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + getDecoration() + ", "
                + handle + "]";
        }
    }


    class LabelDecoration implements Decoration {
        int index;

        LabelDecoration(int index) {
            this.index = index;
        }

        public int getIndex() { 
            return index;
        }

        AnchoredLabel getItem() { return labels.get(index); }

        @Override public void draw(Graphics2D g, double scale) {
            drawLabel(g, index, scale);
        }

        @Override public DecorationHandle remove() {
            saveNeeded = true;
            labels.remove(index);
            labelViews.remove(index);
            repaintEditFrame();
            return null;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != LabelDecoration.class) return false;

            LabelDecoration cast = (LabelDecoration) other;
            return this.index == cast.index;
        }

        public Point2D.Double getCenterLocation() {
            return (Point2D.Double) labelCenters.get(index).clone();
        }

        public Point2D.Double getAnchorLocation() {
            AnchoredLabel item = getItem();
            return new Point2D.Double(item.getX(), item.getY());
        }

        @Override public void setLineWidth(double lineWidth) {
            // The capability to change box line width seems
            // unnecessary to me
        }

        @Override public double getLineWidth() {
            return 0.0;
            // What IS the line width for labels, anyhow?
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            // The capability to change box line style seems
            // unnecessary to me
        }

        @Override public void setColor(Color color) {
            saveNeeded = true;
            AnchoredLabel item = getItem();
            item.setColor(color);
            labelViews.set(index, toView(item));
        }

        @Override public Color getColor() { return getItem().getColor(); }

        @Override public DecorationHandle[] getHandles() {
            AnchoredLabel label = getItem();
            if (label.getXWeight() != 0.5 || label.getYWeight() != 0.5) {
                return new DecorationHandle[]
                    { new LabelHandle(this, LabelHandleType.ANCHOR),
                      new LabelHandle(this, LabelHandleType.CENTER) };
            } else {
                return new DecorationHandle[]
                    { new LabelHandle(this, LabelHandleType.ANCHOR) };
            }
        }

        /* Moving the anchor will move the center as well. */
        @Override public DecorationHandle[] getMovementHandles() {
            return new DecorationHandle[] { getHandles()[0] };
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + index
                + " (" + getItem().getText() + ")]";
        }
    }


    class ArrowDecoration implements Decoration, DecorationHandle {
        int index;

        ArrowDecoration(int index) {
            this.index = index;
        }

        Arrow getItem() { return arrows.get(index); }

        /*
        // Problem is, nobody really cares to follow the outline
        // of an arrow.

        public void getShape() {

            Point2D.Double p = principalToStandardPage.transform(arrow.x, arrow.y);
            return new Arrow
                (p.x, p.y, arrow.size, principalToPageAngle(arrow.theta));
        }
        */

        @Override public void draw(Graphics2D g, double scale) {
            Arrow arrow = getItem();
            Affine xform = principalToScaledPage(scale);
            Point2D.Double xpoint = xform.transform(arrow.x, arrow.y);
            Arrow arr = new Arrow
                (xpoint.x, xpoint.y, scale * arrow.size,
                 principalToPageAngle(arrow.theta));
            g.fill(arr);
        }

        @Override public ArrowDecoration remove() {
            saveNeeded = true;
            arrows.remove(index);
            repaintEditFrame();
            return null;
        }

        @Override public void move(Point2D dest) {
            saveNeeded = true;
            Arrow item = getItem();
            item.x = dest.getX();
            item.y = dest.getY();
            repaintEditFrame();
        }

        @Override public ArrowDecoration copy(Point2D dest) {
            saveNeeded = true;
            Arrow output = getItem().clonus();
            output.x = dest.getX();
            output.y = dest.getY();
            arrows.add(output);
            repaintEditFrame();
            return new ArrowDecoration(arrows.size() - 1);
        }

        @Override public boolean isEditable() { return false; }

        @Override public void edit() {
            throw new IllegalStateException("Can't edit " + this);
        }

        @Override public void setLineWidth(double lineWidth) {
            saveNeeded = true;
            getItem().size = lineWidth;
            repaintEditFrame();
        }

        @Override public double getLineWidth() {
            return getItem().size;
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            // Nothing to do here
        }

        @Override
        public Point2D.Double getLocation() {
            Arrow item = getItem();
            return new Point2D.Double(item.x, item.y);
        }

        @Override public void setColor(Color color) {
            saveNeeded = true;
            getItem().setColor(color);
        }

        @Override public Color getColor() { return getItem().getColor(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != ArrowDecoration.class) return false;

            ArrowDecoration cast = (ArrowDecoration) other;
            return this.index == cast.index;
        }

        @Override public ArrowDecoration getDecoration() {
            return this;
        }

        @Override public DecorationHandle[] getHandles() {
            return new DecorationHandle[] { this };
        }

        @Override public DecorationHandle[] getMovementHandles() {
            return getHandles();
        }
    }

    static enum TieLineHandleType { INNER1, INNER2, OUTER1, OUTER2 };

    class TieLineHandle implements DecorationHandle {
        TieLineDecoration decoration;
        /** A tie line is not a point object. It has up to four
         corners that can be used as handles to select it. */
        TieLineHandleType handle;

        TieLineHandle(TieLineDecoration decoration, TieLineHandleType handle) {
            this.decoration = decoration;
            this.handle = handle;
        }

        TieLineHandle(int index, TieLineHandleType handle) {
            this(new TieLineDecoration(index), handle);
        }

        TieLine getItem() { return getDecoration().getItem(); }

        @Override public boolean isEditable() { return true; }

        @Override public void edit() {
            TieLine item = getItem();
            int lineCnt = askNumberOfTieLines(item.lineCnt);
            if (lineCnt >= 0) {
                saveNeeded = true;
                item.lineCnt = lineCnt;
                repaintEditFrame();
            }
        }

        @Override public DecorationHandle remove() {
            return getDecoration().remove();
        }

        @Override public TieLineHandle copy(Point2D dest) {
            JOptionPane.showMessageDialog
                (editFrame, "Tie lines cannot be copied.");
            return this;
        }

        @Override
        public Point2D.Double getLocation() {
            switch (handle) {
            case INNER1:
                return getItem().getInner1();
            case INNER2:
                return getItem().getInner2();
            case OUTER1:
                return getItem().getOuter1();
            case OUTER2:
                return getItem().getOuter2();
            }

            return null;
        }

        @Override public void move(Point2D dest) {
            // Tie line movement happens indirectly: normally,
            // everything at a key point moves at once, which means
            // that the control point that delimits the tie line moves
            // with it. No additional work is required here.
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != TieLineHandle.class) return false;

            TieLineHandle cast = (TieLineHandle) other;
            return handle == cast.handle
                && getDecoration().equals(cast.getDecoration());
        }

        @Override public TieLineDecoration getDecoration() {
            return decoration;
        }
    }

    class TieLineDecoration implements Decoration {
        /** Index into tieLines list. */
        int index;

        TieLineDecoration(int index) {
            this.index = index;
        }

        TieLine getItem() { return tieLines.get(index); }

        @Override public void draw(Graphics2D g, double scale) {
            getItem().draw(g, getPrincipalToAlignedPage(), scale);
        }

        // I could return the tie line's outline, but nobody
        // cares. The inner and outer edges are already part of
        // the diagram, while the sides would be added if anyone
        // wants them.
        /*        @Override public void getShape() {

            return null;
            }
         */

        @Override public void setLineWidth(double lineWidth) {
            saveNeeded = true;
            getItem().lineWidth = lineWidth;
            repaintEditFrame();
        }

        @Override public double getLineWidth() {
            return getItem().lineWidth;
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            saveNeeded = true;
            getItem().stroke = lineStyle;
            repaintEditFrame();
        }

        @Override public DecorationHandle remove() {
            saveNeeded = true;
            tieLines.remove(index);
            repaintEditFrame();
            return null;
        }

        @Override public void setColor(Color color) {
            saveNeeded = true;
            getItem().setColor(color);
        }

        @Override public Color getColor() { return getItem().getColor(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != TieLineDecoration.class) return false;

            TieLineDecoration cast = (TieLineDecoration) other;
            return index == cast.index;
        }

        @Override public DecorationHandle[] getHandles() {
            ArrayList<TieLineHandle> output = new ArrayList<>();
            for (TieLineHandleType handle: TieLineHandleType.values()) {
                output.add(new TieLineHandle(this, handle));
            }
            return output.toArray(new TieLineHandle[0]);
        }

        /* Moving the curves these tie lines attach to will move the
           tie lines also, so return an empty array. */
        @Override public DecorationHandle[] getMovementHandles() {
            return new DecorationHandle[0];
        }
    }

    static enum RulerHandleType { START, END };

    class RulerHandle implements ParameterizableHandle {
        RulerDecoration decoration;
        /** Either end of the ruler may be used as a handle. */
        RulerHandleType handle; 

        RulerHandle(RulerDecoration decoration, RulerHandleType handle) {
            this.decoration = decoration;
            this.handle = handle;
        }

        RulerHandle(int index, RulerHandleType handle) {
            this(new RulerDecoration(index), handle);
        }

        LinearRuler getItem() { return getDecoration().getItem(); }

        @Override public Parameterization2D getParameterization() {
            return getDecoration().getParameterization();
        }

        @Override public double getT() {
            return (handle == RulerHandleType.START) ? 0 : 1;
        }

        @Override public boolean isEditable() { return true; }

        @Override public void edit() {
            LinearRuler item = getItem();
            if ((new RulerDialog(editFrame, "Edit Ruler", item))
                .showModal(item)) {
                saveNeeded = true;
                repaintEditFrame();
            }
        }

        @Override public DecorationHandle remove() {
            return getDecoration().remove();
        }

        @Override public void move(Point2D dest) {
            Point2D.Double d = new Point2D.Double(dest.getX(), dest.getY());

            switch (handle) {
            case START:
                getItem().startPoint = d;
                break;
            case END:
                getItem().endPoint = d;
                break;
            }

            saveNeeded = true;
            repaintEditFrame();
        }

        @Override public RulerHandle copy(Point2D dest) {
            saveNeeded = true;
            Point2D.Double d = new Point2D.Double(dest.getX(), dest.getY());
            LinearRuler r = getItem().clone();
            double dx = r.endPoint.x - r.startPoint.x;
            double dy = r.endPoint.y - r.startPoint.y;
            
            switch (handle) {
            case START:
                r.startPoint = d;
                r.endPoint = new Point2D.Double(d.x + dx, d.y + dy);
                break;
            case END:
                r.endPoint = d;
                r.startPoint = new Point2D.Double(d.x - dx, d.y - dy);
                break;
            }

            rulers.add(r);
            repaintEditFrame();
            return new RulerHandle(rulers.size() - 1, handle);
        }

        @Override public Point2D.Double getLocation() {
            switch (handle) {
            case START:
                return (Point2D.Double) getItem().startPoint.clone();
            case END:
                return (Point2D.Double) getItem().endPoint.clone();
            }

            return null;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != RulerHandle.class) return false;

            RulerHandle cast = (RulerHandle) other;
            return handle == cast.handle
                && getDecoration().equals(cast.getDecoration());
        }

        @Override public RulerDecoration getDecoration() {
            return decoration;
        }
    }

    class RulerDecoration implements Decoration, Parameterizable2D {
        /** Index into rulers list. */
        int index;

        RulerDecoration(int index) {
            this.index = index;
        }

        LinearRuler getItem() { return rulers.get(index); }

        @Override public void draw(Graphics2D g, double scale) {
            getItem().draw(g, getPrincipalToAlignedPage(), scale);
        }

        public Shape getShape() {
            LinearRuler item = getItem();
            Point2D.Double s = principalToStandardPage.transform(item.startPoint);
            Point2D.Double e = principalToStandardPage.transform(item.endPoint);
            return new Line2D.Double(s, e);
        }

        @Override public PathParam2D getParameterization() {
            return new PathParam2D(getShape());
        }

        @Override public void setLineWidth(double lineWidth) {
            saveNeeded = true;
            LinearRuler item = getItem();
            // Change fontSize too, to maintain a fixed ratio between
            // lineWidth and fontSize.
            double ratio  = lineWidth / item.lineWidth;
            item.lineWidth = lineWidth;
            item.fontSize *= ratio;
            repaintEditFrame();
        }

        @Override public double getLineWidth() {
            return getItem().lineWidth;
        }

        @Override public void setLineStyle(StandardStroke lineStyle) {
            // Nothing to do here
        }

        @Override public DecorationHandle remove() {
            saveNeeded = true;
            rulers.remove(index);
            repaintEditFrame();
            return null;
        }

        @Override public void setColor(Color color) {
            saveNeeded = true;
            getItem().setColor(color);
        }
        @Override public Color getColor() { return getItem().getColor(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            if (getClass() != RulerDecoration.class) return false;

            RulerDecoration cast = (RulerDecoration) other;
            return index == cast.index;
        }

        @Override public DecorationHandle[] getHandles() {
            ArrayList<RulerHandle> output = new ArrayList<>();
            for (RulerHandleType handle: RulerHandleType.values()) {
                output.add(new RulerHandle(this, handle));
            }
            return output.toArray(new DecorationHandle[0]);
        }

        @Override public DecorationHandle[] getMovementHandles() {
            return getHandles();
        }

        /** Return the VertexHandle closest to path(t). */
        public RulerHandle getHandle(double t) {
            return new RulerHandle
                (this,
                 (t <= 0.5) ? RulerHandleType.START : RulerHandleType.END);
        }
    }

    
    /** Apply the NIST MML PED standard binary diagram axis style. */
    class DefaultBinaryRuler extends LinearRuler {
        DefaultBinaryRuler() {
            fontSize = normalFontSize();
            lineWidth = STANDARD_LINE_WIDTH;
            tickPadding = 3.0;
            drawSpine = true;
        }
    }


    /** Apply the NIST MML PED standard ternary diagram axis style. */
    class DefaultTernaryRuler extends LinearRuler {
        DefaultTernaryRuler() {
            fontSize = normalFontSize();
            lineWidth = STANDARD_LINE_WIDTH;
            tickPadding = 3.0;
            multiplier = 100.0;

            tickType = LinearRuler.TickType.V;
            suppressStartTick = true;
            suppressEndTick = true;

            drawSpine = true;
        }
    }

    class PathAndT {
        GeneralPolyline path;
        double t;

        PathAndT(GeneralPolyline path, double t) {
            this.path = path;
            this.t = t;
        }

        public String toString() {
            return "PathAndT[" + path + ", " + t + "]";
        }
    }

    private static final String PREF_DIR = "dir";
    private static final double BASE_SCALE = 615.0;

    // The label views are grid-fitted at the font size of the buttons
    // they were created from. Grid-fitting throws off the font
    // metrics, but the bigger the font size, the less effect
    // grid-fitting has, and the less error it induces. So make the
    // Views big enough that grid-fitting is largely irrelevant.
    private static final int VIEW_MAGNIFICATION = 8;
    static protected double MOUSE_UNSTICK_DISTANCE = 30; /* pixels */
    static protected Image crosshairs = null;
    static protected final String defaultFontName = "DejaVu LGC Sans PED";
    static protected final Map<String,String> fontFiles
        = new HashMap<String, String>() {
	private static final long serialVersionUID = -4018269657447031301L;

        {
            put("DejaVu LGC Sans PED", "DejaVuLGCSansPED.ttf");
            put("DejaVu LGC Serif PED", "DejaVuLGCSerifPED.ttf");
            put("DejaVu LGC Serif GRUMP", "DejaVuLGCSerifGRUMP.ttf");
        }
    };

    // embeddedFont is initialized when needed.
    protected Font embeddedFont = null;

    protected CropFrame cropFrame = new CropFrame();
    protected EditFrame editFrame = new EditFrame(this);
    protected ImageZoomFrame zoomFrame = new ImageZoomFrame();
    protected VertexInfoDialog vertexInfo = new VertexInfoDialog(editFrame);
    protected LabelDialog labelDialog = null;
    protected JColorChooser colorChooser = null;
    protected JDialog colorDialog = null;
    protected Map<String,String> keyValues = null;
    protected Set<String> tags = new HashSet<>();

    LabelDialog getLabelDialog() {
        if (labelDialog == null) {
            labelDialog = new LabelDialog(editFrame, "Add label", getFont().deriveFont(16.0f));
        }
        return labelDialog;
    }

    /** Transform from original coordinates to principal coordinates.
        Original coordinates are (x,y) positions within a scanned
        image. Principal coordinates are either the natural (x,y)
        coordinates of a Cartesian graph or binary diagram (for
        example, y may equal a temperature while x equals the atomic
        fraction of the second diagram component), or the fraction
        of the right and top components respectively for a ternary
        diagram. */
    @JsonProperty protected PolygonTransform originalToPrincipal;
    protected PolygonTransform principalToOriginal;
    @JsonProperty protected AffinePolygonTransform principalToStandardPage;
    protected transient Affine standardPageToPrincipal;
    /** Bounds of the entire page in standardPage space. Use null to
        compute automatically instead. */
    protected Rectangle2D.Double pageBounds;
    protected DiagramType diagramType = null;
    protected transient double scale;
    protected ArrayList<GeneralPolyline> paths;
    protected ArrayList<AnchoredLabel> labels;
    protected transient ArrayList<Point2D.Double> labelCenters;
    protected transient ArrayList<View> labelViews;
    @JsonProperty protected ArrayList<Arrow> arrows;
    protected ArrayList<TieLine> tieLines;
    @JsonProperty protected String[/* Side */] diagramComponents = null;

    /** All elements contained in diagram components, sorted into Hill
        order. Only trustworthy if componentElements != null. */
    protected transient String[] diagramElements = null;
    /** Diagram components expressed as vectors of element quantities.
        The element quantities are parallel to the diagramElements[]
        array. Set this to null whenever diagramComponents changes. */
    protected transient double[/* Side */][/* elementNo */]
        componentElements = null;

    protected transient BufferedImage originalImage;
    protected String originalFilename;
    /** The item (vertex, label, etc.) that is selected, or null if nothing is. */
    protected DecorationHandle selection;
    /** If the timer exists, the original image (if any) upon which
        the new diagram is overlaid will blink. */
    transient Timer imageBlinker = null;
    /** True if imageBlinker is enabled and the original image should
        be displayed in the background at this time. */
    transient boolean backgroundImageEnabled;

    protected ArrayList<LinearAxis> axes;
    /** principal coordinates are used to define rulers' startPoints
        and endPoints. */
    protected ArrayList<LinearRuler> rulers;
    protected LinearAxis pageXAxis = null;
    protected LinearAxis pageYAxis = null;
    protected transient boolean preserveMprin = false;
    protected transient boolean isShiftDown = false;

    /** True unless selection instanceof VertexHandle and the next
        vertex to be inserted should be added to the curve before the
        currently selected vertex . */
    protected transient boolean insertBeforeSelection = false;

    protected transient int paintSuppressionRequestCnt;

    /** mouseIsStuck is true if the user recently performed a
        point-selection operatiorn such as "nearest vertex" or
        "nearest point on curve" and the mouse has not yet been moved
        far enough to un-stick the mouse from that location. */
    protected transient boolean mouseIsStuck;

    /** Because rescaling an image is slow, keep a cache of locations
        and sizes that have been rescaled. */
    protected ArrayList<ScaledCroppedImage> scaledOriginalImages;
    /** This is the darkened version of the original image, or null if
        no darkened version exists. At most one dark image is kept in
        memory at a time. */
    protected transient ScaledCroppedImage darkImage;
    /** Darkness value of darkImage. */
    protected transient double darkImageDarkness = 0;
    protected double labelXMargin = 0;
    protected double labelYMargin = 0;

    static final double STANDARD_LINE_WIDTH = 0.0024;
    static final int STANDARD_FONT_SIZE = 15;
    protected double lineWidth = STANDARD_LINE_WIDTH;
    protected transient StandardStroke lineStyle = StandardStroke.SOLID;

    static String[] tieLineStepStrings =
    { "<html><div width=\"200 px\"><p>"
      + "Use the 'L' or 'P' short-cut keys to select an outside "
      + "corner of the tie line "
      + "display region. Make sure to select not just the right "
      + "point, but also the specific curve that forms the "
      + "outside edge of the region."
      + "</p></div></html>",
      "<html><div width=\"200 px\"><p>"
      + "Select the second outside corner." 
      + "</p></div></html>",
      "<html><div width=\"200 px\"><p>"
      + "Select the convergence point, if the tie lines extend far enough "
      + "to converge, or one of the two inside corners otherwise."
      + "</p></div></html>",
      "<html><div width=\"200 px\"><p>"
      + "Select the second inside corner (or just press \"Item Selected\" "
      + "right away if the tie lines converge)."
      + "</p></div></html>" };

    StepDialog tieLineDialog = new StepDialog
        (editFrame, "Select Tie Line Display Region",
         new Editor.Action("Item selected") {
             private static final long serialVersionUID = -6676297149495177006L;

             @Override public void actionPerformed(ActionEvent e) {
                 tieLineCornerSelected();
             }
         });

    // When the user selects the "Add tie line" menu item,
    // tieLineCorners temporarily holds the corner locations until all
    // corners are specified and the tie line can actually be created.
    // (In retrospect, it would have been simpler to create the
    // TieLine with dummy values and fill the corners in as they are
    // added, but whatever, this works.)
    ArrayList<PathAndT> tieLineCorners;

    protected String filename;

    boolean saveNeeded = false;

    /** Current mouse position expressed in principal coordinates.
     It's not always sufficient to simply read the mouse position in
     the window, because after the user jumps to a preselected point,
     the integer mouse position is not precise enough to express that
     location. */
    protected Point2D.Double mprin = null;

    /** When the user presses 'p' or 'P' repeatedly to locate points
        close to a starting point, this holds the initial mouse
        location (in principal coordinates) so it is possible to
        identify what the next-closest point may be. */
    protected Point2D.Double principalFocus = null;

    public Editor() {
        zoomFrame.setFocusableWindowState(false);
        tieLineDialog.setFocusableWindowState(false);
        vertexInfo.setDefaultCloseOperation
            (WindowConstants.DO_NOTHING_ON_CLOSE);
        editFrame.setDefaultCloseOperation
            (WindowConstants.DO_NOTHING_ON_CLOSE);
        editFrame.addWindowListener(new CloseListener());
        clear();
        getEditPane().addMouseListener(this);
        getEditPane().addMouseMotionListener(this);
        cropFrame.setDefaultCloseOperation
            (WindowConstants.HIDE_ON_CLOSE);
        cropFrame.addCropEventListener(this);
    }

    /** @return the filename that has been assigned to the PED format
        diagram output. */
    @JsonIgnore public String getFilename() {
        return filename;
    }

    /** Initialize/clear almost every field except diagramType. */
    void clear() {
        originalToPrincipal = null;
        originalImage = null;
        originalFilename = null;
        principalToOriginal = null;
        principalToStandardPage = null;
        standardPageToPrincipal = null;
        pageBounds = null;
        scale = BASE_SCALE;
        paths = new ArrayList<>();
        arrows = new ArrayList<>();
        tieLines = new ArrayList<>();
        labels = new ArrayList<>();
        labelViews = new ArrayList<>();
        labelCenters = new ArrayList<>();
        selection = null;
        axes = new ArrayList<>();
        rulers = new ArrayList<>();
        diagramComponents = new String[Side.values().length];
        componentElements = null;
        mprin = null;
        filename = null;
        saveNeeded = false;
        scaledOriginalImages = null;
        vertexInfo.setAngle(0);
        vertexInfo.setSlope(0);
        vertexInfo.setLineWidth(lineWidth);
        mouseIsStuck = false;
        paintSuppressionRequestCnt = 0;
        setBackground(EditFrame.BackgroundImage.LIGHT_GRAY);
        tieLineDialog.setVisible(false);
        tieLineCorners = new ArrayList<>();
        embeddedFont = null;
        keyValues = new TreeMap<>();
        tags.clear();
        editFrame.removeAllTags();
        editFrame.removeAllVariables();
    }

    @JsonProperty("curves")
    ArrayList<GeneralPolyline> getPaths() {
        return paths;
    }

    public void close() {
        if (verifyCloseDiagram()) {
            System.exit(0);
        }
    }

    @JsonProperty("curves")
    void setPaths(Collection<GeneralPolyline> paths) {
        selection = null;
        this.paths = new ArrayList<GeneralPolyline>(paths);
    }

    @JsonProperty("tieLines")
    ArrayList<TieLine> getTieLines() {
        return tieLines;
    }

    GeneralPolyline idToCurve(int id) {
        for (GeneralPolyline path: paths) {
            if (path.getJSONId() == id) {
                return path;
            }
        }
        System.err.println("No curve found with id " + id + ".");
        return null;
    }

    @JsonIgnore VertexHandle getVertexHandle() {
        return (selection instanceof VertexHandle)
            ? ((VertexHandle) selection)
            : null;
    }

    @JsonIgnore CurveDecoration getSelectedCurve() {
        return (selection instanceof VertexHandle)
            ? ((VertexHandle) selection).getDecoration()
            : null;
    }

    @JsonIgnore LabelHandle getLabelHandle() {
        return (selection instanceof LabelHandle)
            ? ((LabelHandle) selection)
            : null;
    }

    @JsonIgnore LabelDecoration getSelectedLabel() {
        return (selection instanceof LabelHandle)
            ? ((LabelHandle) selection).getDecoration()
            : null;
    }

    @JsonIgnore TieLineHandle getTieLineHandle() {
        return (selection instanceof TieLineHandle)
            ? ((TieLineHandle) selection)
            : null;
    }

    @JsonIgnore TieLineDecoration getSelectedTieLine() {
        return (selection instanceof TieLineHandle)
            ? ((TieLineHandle) selection).getDecoration()
            : null;
    }

    @JsonIgnore RulerHandle getRulerHandle() {
        return (selection instanceof RulerHandle)
            ? ((RulerHandle) selection)
            : null;
    }

    @JsonIgnore RulerDecoration getRulerSelection() {
        return (selection instanceof RulerHandle)
            ? ((RulerHandle) selection).getDecoration()
            : null;
    }

    @JsonIgnore ArrowDecoration getSelectedArrow() {
        return (selection instanceof ArrowDecoration)
            ? ((ArrowDecoration) selection)
            : null;
    }

    /** @return The currently selected GeneralPolyline, or null if no
        curve is selected. */
    @JsonIgnore public GeneralPolyline getActiveCurve() {
        CurveDecoration sel = getSelectedCurve();
        return (sel == null) ? null : sel.getItem();
    }

    /** @return The currently selected vertex, or null if no curve is
        selected. */
    @JsonIgnore public Point2D.Double getActiveVertex() {
        VertexHandle handle = getVertexHandle();
        return (handle == null) ? null : handle.getLocation();
    }

    public String[] getTags() {
        return tags.toArray(new String[0]);
    }

    public void removeAllTags() {
        tags.clear();
        editFrame.removeAllTags();
    }

    public void removeAllVariables() {
        axes.clear();
        editFrame.removeAllVariables();
    }

    public void setTags(String[] newTags) {
        saveNeeded = true;
        removeAllTags();
        for (String tag: newTags) {
            addTag(tag);
        }
    }

    public void setVariables(String[] newTags) {
        saveNeeded = true;
        removeAllTags();
        for (String tag: newTags) {
            addTag(tag);
        }
    }

    public void addTag(String tag) {
        saveNeeded = true;
        tags.add(tag);
        editFrame.addTag(tag);
    }

    public void addTag() {
        String tag = JOptionPane.showInputDialog(editFrame, "Tag");
        if (tag != null && tags.add(tag)) {
            addTag(tag);
        }
    }

    public void removeTag(String tag) {
        saveNeeded = true;
        tags.remove(tag);
        editFrame.removeTag(tag);
    }

    public void removeVariable(String name) {
        for (Axis axis: axes) {
            if (axis.name.equals(name)) {
                axes.remove(axis);
                editFrame.removeVariable(name);
                for (int i = 0; i < rulers.size(); ) {
                    if (rulers.get(i).axis == axis) {
                        rulers.remove(i);
                    } else {
                        ++i;
                    }
                }
                repaintEditFrame();
                saveNeeded = true;
                return;
            }
        }
        throw new IllegalStateException("Variable '" + name + "' + not found.");
    }

    @JsonIgnore public void setSaveNeeded(boolean saveNeeded) {
        this.saveNeeded = saveNeeded;
    }

    @JsonIgnore public boolean getSaveNeeded() {
        return saveNeeded;
    }

    /** The returned value can be used to modify the
        internal key/value mapping. */
    @JsonProperty("keys") Map<String,String> getKeyValues() {
        return keyValues;
    }

    void setKeyValues(@JsonProperty("keys") Map<String, String> keyValues) {
        saveNeeded = true;
        this.keyValues = keyValues;
    }

    /** Return the value associated with this key in the keyValues
        field. */
    public String get(String key) {
        return keyValues.get(key);
    }

    /** Put a key/value pair in the keyValues field. */
    public void put(String key, String value) {
        keyValues.put(key, value);
        updateTitle();
    }

    public void put() {
        String[] labels = { "Key", "Value" };
        String[] values = { "", "" };
        StringArrayDialog dog = new StringArrayDialog
            (editFrame, labels, values, null);
        dog.setTitle("Add key/value pair");
        values = dog.showModal();
        if (values == null || "".equals(values[0])) {
            return;
        }

        put(values[0], values[1]);
    }


    public void listKeyValues() {
        Set<Map.Entry<String,String>> entries = keyValues.entrySet();
        String[] keys = new String[keyValues.size()];
        String[] values = new String[keyValues.size()];
        int ii = 0;
        for (Map.Entry<String, String> entry: entries) {
            keys[ii] = entry.getKey();
            values[ii] = entry.getValue();
            ++ii;
        }
            
        StringArrayDialog dog = new StringArrayDialog
                 (editFrame, keys, values,
                  "To delete a key, replace a value with the empty string.");
        dog.setTitle("Key/value pairs");
        values = dog.showModal();
        if (values == null || !isEditable()) {
            return;
        }

        for (int i = 0; i < keyValues.size(); ++i) {
            if ("".equals(values[i])) {
                removeKey(keys[i]);
            } else if (!values[i].equals(get(keys[i]))) {
                put(keys[i], values[i]);
            }
        }
    }


    /** Remove the given key from the keyValues field, and return its
        value, or null if absent. */
    public String removeKey(String key) {
        String output = keyValues.remove(key);
        saveNeeded = true;
        updateTitle();
        return output;
    }


    public synchronized void setBackground(EditFrame.BackgroundImage value) {
        // Turn blinking off
        if (imageBlinker != null) {
            imageBlinker.cancel();
        }
        imageBlinker = null;
        darkImage = null;

        if (value == EditFrame.BackgroundImage.BLINK) {
            imageBlinker = new Timer("ImageBlinker", true);
            imageBlinker.scheduleAtFixedRate(new ImageBlinker(), 500, 500);
            backgroundImageEnabled = true;
        }

        // The rest is handed in paintDiagram().

        repaintEditFrame();
    }

    /** Reset the location of all vertices and labels that have the
        same location as the selection to mprin.
        
        @param moveAll If true, all items located at the selected
        point will move moved. If false, only the selected item itself
        will be moved.
    */
    public void moveRegion() {
        VertexHandle vhand = getVertexHandle();

        if (vhand == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "Draw a curve to identify the boundary of the region to be moved.");
            return;
        }

        if (mprin == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "<html><p>Move the mouse to the target location. "
                 + "Use the 'R' shortcut key or keyboard menu controls "
                 + "instead of selecting the menu item using the mouse."
                 + "</p></html>"
                 );
            return;
        }

        if (mouseIsStuckAtSelection()) {
            // Unstick the mouse so we're not just moving the mouse
            // onto itself.
            unstickMouse();
        }

        GeneralPolyline path = vhand.getDecoration().getItem();
        Shape region = vhand.getDecoration().getShape();
        PathParam2D param = new PathParam2D(region);

        Point2D.Double delta = Duh.aMinusB(mprin, selection.getLocation());

        for (DecorationHandle hand: movementHandles()) {
            Point2D prin = hand.getLocation();
            Point2D page = principalToStandardPage.transform(prin);
            boolean inside = path.isClosed() && region.contains(page);
            if (!inside) {
                // Check if the point is very close to the path
                // border.
                CurveDistanceRange cdr = param.distance(page, 1e-6, 1000);
                inside = cdr != null && cdr.distance <= 1e-6;
            }

            if (inside) {
                hand.move(new Point2D.Double(prin.getX() + delta.x,
                                             prin.getY() + delta.y));
                saveNeeded = true;
            }
        }

        repaintEditFrame();
    }

    /** Reset the location of all vertices and labels that have the
        same location as the selection to mprin.
        
        @param moveAll If true, all items located at the selected
        point will move moved. If false, only the selected item itself
        will be moved.
    */
    public void moveSelection(boolean moveAll) {
        if (selection == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "You must select an item before you can move it.");
            return;
        }

        if (mprin == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "You must move the mouse to the target destination " +
                 "before you can move items.");
            return;
        }

        if (mouseIsStuckAtSelection()) {
            unstickMouse();
        }

        moveSelection(mprin, moveAll);
        mouseIsStuck = true;
    }

    /** Copy the selection, moving it to mprin. */
    public void copySelection() {

        if (selection == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "You must select an item before you can copy it.");
            return;
        }

        if (mprin == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "You must move the mouse to the target destination " +
                 "before you can copy.");
            return;
        }

        if (mouseIsStuckAtSelection()) {
            unstickMouse();
        }

        selection = selection.copy(mprin);
    }

    /** Edit the selection. */
    public void editSelection() {

        if (selection == null) {
            // Find the nearest editable item, and edit it.

            for (DecorationHandle handle: nearestHandles()) {
                if (handle.isEditable()) {
                    selection = handle;
                    break;
                }
            }

            if (selection == null) {
                JOptionPane.showMessageDialog
                    (editFrame, "There are no editable items.");
                return;
            }
        }

        if (!selection.isEditable()) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "This item does not have a special edit function.");
            return;
        }
        selection.edit();
    }

    /** Change the selection's color. */
    public void colorSelection() {

        if (selection == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "You must select an item before you can edit it.");
            return;
        }

        if (colorChooser == null) {
            colorChooser = new JColorChooser();
            colorDialog = JColorChooser.createDialog
            (editFrame, "Choose color", true,
             colorChooser, new ActionListener() {
                 @Override public void actionPerformed(ActionEvent e) {
                     Editor.this.selection.getDecoration().setColor(colorChooser.getColor());
                     repaintEditFrame();
                 }
             },
             null);
            colorDialog.pack();
        }

        Color c = thisOrBlack(selection.getDecoration().getColor());
        colorChooser.setColor(c);
        colorDialog.setVisible(true);
    }

    /** Return true if p1 and p2 are equal to within reasonable
        limits, where "reasonable limits" means the distance between
        their transformations to the standard page is less than
        1e-6. */
    boolean principalCoordinatesMatch(Point2D p1, Point2D p2) {
        Point2D.Double page1 = principalToStandardPage.transform(p1);
        Point2D.Double page2 = principalToStandardPage.transform(p2);
        return page1.distanceSq(page2) < 1e-12;
    }

    boolean mouseIsStuckAtSelection() {
        return mouseIsStuck && selection != null && mprin != null
            && principalCoordinatesMatch(selection.getLocation(), mprin);
    }

    /** Reset the location of all vertices and labels that have the
        same location as the selection to dest.

        @param moveAll If true, move all selectable items that have
        the same location as the selection to dest. If false, move
        only the selection itself. */
    public void moveSelection(Point2D.Double dest, boolean moveAll) {
        if (moveAll) {
            Point2D.Double p = selection.getLocation();

            for (DecorationHandle sel: getDecorationHandles()) {
                if (principalCoordinatesMatch(p, sel.getLocation())) {
                    sel.move(dest);
                }
            }
        } else {
            selection.move(dest);
        }
    }

    public void removeSelection() {
        if (selection != null) {
            selection = selection.remove();
        }
    }

    /** Cycle the currently active curve.

        @param delta if 1, then cycle forwards; if -1, then cycle
        backwards. */
    public void cycleActiveCurve(int delta) {
        if (paths.size() < 2) {
            return; // Nothing to do.
        }

        insertBeforeSelection = false;
        VertexHandle sel = getVertexHandle();

        if (sel == null) {
            selection = sel = new VertexHandle((delta > 0) ? -1 : 0, -1);
        }
        CurveDecoration csel = sel.getDecoration();

        csel.curveNo = (csel.curveNo + delta + paths.size()) % paths.size();
        sel.vertexNo = sel.getItem().size() - 1;
        repaintEditFrame();
    }

    /** Cycle the currently active vertex.

        @param delta if 1, then cycle forwards; if -1, then cycle
        backwards. */
    public void cycleActiveVertex(int delta) {
        VertexHandle sel = getVertexHandle();

        if (sel == null) {
            return; // Nothing to do.
        }

        GeneralPolyline path = getActiveCurve();
        int cnt = path.size();

        if (path.isClosed()) {
            sel.vertexNo = (sel.vertexNo + cnt + delta) % cnt;
        } else {
            sel.vertexNo += delta;
            if (sel.vertexNo > cnt - 1) {
                sel.vertexNo = cnt - 1;
                insertBeforeSelection = false;
            } else if (sel.vertexNo < 0) {
                sel.vertexNo = 0;
                insertBeforeSelection = true;
            }
        }

        repaintEditFrame();
    }

    /** @param scale A multiple of standardPage coordinates

        @return a transform from principal coordinates to device
        coordinates. In additon to the scaling, the coordinates are
        translated to make (0,0) the top left corner. */
    Affine standardPageToDevice(double scale) {
        Affine output = Affine.getScaleInstance(scale, scale);
        output.translate(-pageBounds.x, -pageBounds.y);
        return output;
    }

    /** @param scale A multiple of standardPage coordinates

        @return a transform from principal coordinates to device
        coordinates. In additon to the scaling, the coordinates are
        translated to make (0,0) the top left corner. */
    Affine principalToScaledPage(double scale) {
        Affine output = standardPageToDevice(scale);
        output.concatenate(principalToStandardPage);
        return output;
    }

    Affine scaledPageToPrincipal(double scale) {
        try {
            return principalToScaledPage(scale).createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("p2sp = " + principalToStandardPage);
            System.err.println("p2scp = " + principalToScaledPage(scale));
            throw new IllegalStateException("Transform at scale " + scale
                                            + " is not invertible");
        }
    }

    /** Move mprin, plus the selection if the mouse is stuck at the
        selection's location, the given distance.

        @param dx The change in x position, expressed in screen pixels
        @param dy The change in y position, expressed in screen pixels
    */
    public void move(int dx, int dy) {
        if (mprin == null) {
            return;
        }

        Point2D.Double mousePage = principalToStandardPage.transform(mprin);
        mousePage.x += dx / scale;
        mousePage.y += dy / scale;
        moveMouseAndMaybeSelection
            (standardPageToPrincipal.transform(mousePage));
    }

    /** Move the mouse to position p (defined in principal
        coordinates). If the mouse is stuck at the selection, then
        move everything at the mouse to the new position as well. */
    public void moveMouseAndMaybeSelection(Point2D.Double p) {
        if (mouseIsStuckAtSelection()) {
            moveSelection(p, true);
        }
        mprin = p;
        moveMouse(mprin);
        mouseIsStuck = true;
    }


    /** Make sure the mouse position and status bar are up to date and
        call repaint() on the edit frame. */
    public void repaintEditFrame() {
        editFrame.repaint();
    }

    Rectangle scaledPageBounds(double scale) {
        return new Rectangle((int) 0, 0,
                             (int) Math.ceil(pageBounds.width * scale),
                             (int) Math.ceil(pageBounds.height * scale));
    }


    /** Most of the information required to paint the EditPane is part
        of this object, so it's simpler to do the painting from
        here. */
    public void paintEditPane(Graphics g0) {
        updateMousePosition();
        Graphics2D g = (Graphics2D) g0;
        paintDiagram(g, scale, true, true);
    }


    /** Compute the scaling factor to apply to pageBounds (and
        standardPage coordinates) in order for xform.transform(scale *
        pageBounds) to fill deviceBounds as much as possible without
        going over.
    */
    double deviceScale(AffineTransform xform, Rectangle2D deviceBounds) {
        AffineTransform itrans;
        try {
            itrans = xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException("Transform " + xform
                                            + " is not invertible");
        }

        Point2D.Double delta = new Point2D.Double();
        itrans.deltaTransform
            (new Point2D.Double(pageBounds.width, pageBounds.height), delta);

        Rescale r = new Rescale(Math.abs(delta.x), 0.0, deviceBounds.getWidth(),
                                Math.abs(delta.y), 0.0, deviceBounds.getHeight());
        return r.t;
    }

    double deviceScale(Graphics2D g, Rectangle2D deviceBounds) {
        return deviceScale(g.getTransform(), deviceBounds);
    }

    static final double DEFAULT_BACKGROUND_IMAGE_DARKNESS = 1.0/3;

    double getBackgroundImageDarkness() {
        switch (editFrame.getBackgroundImage()) {
        case LIGHT_GRAY:
            return DEFAULT_BACKGROUND_IMAGE_DARKNESS;
        case DARK_GRAY:
            return 0.577; // Geometric mean of 1/3 and 1
        case BLACK:
            return 1.0;
        case BLINK:
            return 1.0;
        default:
            return Double.NaN;
        }
    }

    void paintBackgroundImage(Graphics2D g, double scale) {
        ScaledCroppedImage im = getScaledOriginalImage();
        double darkness = getBackgroundImageDarkness();
        if (darkness != DEFAULT_BACKGROUND_IMAGE_DARKNESS) {
            if (darkImage != null
                && im.imageBounds.equals(darkImage.imageBounds)
                && im.cropBounds.equals(darkImage.cropBounds)
                && darkImageDarkness == darkness) {
                // The cached image darkImage can be used.
                im = darkImage;
            } else {
                // Darken this image and cache it.
                darkImage = new ScaledCroppedImage();
                darkImage.imageBounds = (Rectangle) im.imageBounds.clone();
                darkImage.cropBounds = (Rectangle) im.cropBounds.clone();
                BufferedImage src = im.croppedImage;
                darkImage.croppedImage = new BufferedImage
                    (src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
                fade(src, darkImage.croppedImage, darkness / DEFAULT_BACKGROUND_IMAGE_DARKNESS);
                darkImageDarkness = darkness;
                im = darkImage;
            }
        }
        g.drawImage(im.croppedImage, im.cropBounds.x, im.cropBounds.y, null);
    }

    static Color toColor(AutoPositionType ap) {
        return ap == AutoPositionType.NONE ? Color.RED
            : ap == AutoPositionType.CURVE
            ? new Color(0xd87000) // Orange
            : new Color(0xb0c000); // Yellow
    }


    /** Show the result if the mouse point were added to the currently
        selected curve in red, and show the currently selected curve in
        green. */
    void paintSelectedCurve(Graphics2D g, double scale) {
                
        // Color in red the curve that would exist if the
        // current mouse position were added. Color in green
        // the curve that already exists.

        CurveDecoration csel = getSelectedCurve();
        GeneralPolyline path = csel.getItem();

        // Disable anti-aliasing for this phase because it
        // prevents the green line from precisely overwriting
        // the red line.

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_OFF);

        AutoPositionHolder ap = new AutoPositionHolder();
        Point2D.Double extraVertex
            = isShiftDown ? getAutoPosition(ap)
            : mouseIsStuckAtSelection()
            ? getMousePosition() // Show the point that would be added
                                 // if the mouse became unstuck.
            : mprin;

        if (extraVertex != null && !isDuplicate(extraVertex)) {
            // Add the current mouse position to the path next to the
            // currently selected vertex, and draw the curve that
            // results from this addition in red. Then remove the
            // extra vertex.

            boolean oldSaveNeeded = saveNeeded;
            Color oldColor = csel.getColor();
            csel.setColor(toColor(ap.position));

            int vertexNo = getVertexHandle().vertexNo
                + (insertBeforeSelection ? 0 : 1);
            path.add(vertexNo, extraVertex);
            csel.draw(g, scale);
            path.remove(vertexNo);
            csel.setColor(oldColor);
            saveNeeded = oldSaveNeeded;
        }

        csel.draw(g, scale);
        double r = Math.max(path.getLineWidth() * scale * 2.0, 4.0);
        circleVertices(g, path, scale, false, r);

        // Mark the active vertex specifically.
        Point2D.Double point = getActiveVertex();
        if (point != null) {
            Point2D.Double xpoint = new Point2D.Double();
            Affine p2d = principalToScaledPage(scale);
            p2d.transform(point, xpoint);
            g.fill(new Ellipse2D.Double
                   (xpoint.x - r, xpoint.y - r, r * 2, r * 2));
        }
    }


    /** Special label highlighting rules: show the label box, and if
        the anchor is not at the center, show the anchor as either a
        hollow circle (if not selected) or as a solid circle (if
        selected). */
    void paintSelectedLabel(Graphics2D g, double scale) {
        LabelHandle hand = getLabelHandle();
        LabelDecoration ldec = hand.getDecoration();
        AnchoredLabel label = ldec.getItem();

        boolean isBoxed = label.isBoxed();
        label.setBoxed(true);
        drawLabel(g, ldec.index, scale, true);
        label.setBoxed(isBoxed);
    }

    /** @param c The usual color, or null.

        @return A color that contrasts with c; specifically, magenta
        if c is null or greenish, or green otherwise. */
    Color getHighlightColor(Color c) {
        // Choices are magenta or green. Green is used unless the c is
        // already greenish.
        if (c == null) {
            return Color.GREEN;
        }

        // These numbers are made up, but reflect that fact that green
        // is perceived as being a brighter color than red, which is
        // perceived as being brighter than blue (compare 0xff00 to
        // 0xff0000 and 0xff: even the brightest pure blue, 0xff,
        // looks a bit dark).
        double brightness = (4 * c.getGreen() + 2 * c.getRed() + c.getBlue())
            / 7.0 / 256;
        double greenness = ((double) c.getGreen()) / 
            (c.getGreen() + 2.0 * c.getRed() + c.getBlue());
        return (brightness > 0.6 && greenness > 0.5) ? Color.MAGENTA
            : Color.GREEN;
    }

    public void paintDiagram(Graphics2D g, double scale) {
        paintDiagram(g, scale, false, true);
    }

    /** Paint the diagram to the given graphics context.

        @param scale Scaling factor to convert standard page
        coordinates to device coordinates.

        @param editing If true, then paint editing hints (highlight
        the currently active curve in green, show the consequences of
        adding the current mouse position in red, etc.). If false,
        show the final form of the diagram. This parameter should be
        false except while painting the editFrame.

        @param drawBackground If false, do not draw the background.

 */
    public void paintDiagram(Graphics2D g, double scale, boolean editing,
                             boolean drawBackground) {
        if (principalToStandardPage == null
            || paintSuppressionRequestCnt > 0) {
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

        if (drawBackground) {
            EditFrame.BackgroundImage back = editFrame.getBackgroundImage();
            boolean showBackgroundImage = editing && tracingImage()
                && back != EditFrame.BackgroundImage.NONE
                && (back != EditFrame.BackgroundImage.BLINK
                    || backgroundImageEnabled);

            if (showBackgroundImage) {
                paintBackgroundImage(g, scale);
            } else {
                // Draw a white box the size of the page.

                g.setColor(Color.WHITE);
                if (pageBounds.width > 0) {
                    g.fill(scaledPageBounds(scale));
                }
            }
        }

        boolean showSel = (selection != null) && editing;

        ArrayList<Decoration> decorations = getDecorations();

        Decoration sel = showSel ? selection.getDecoration() : null;
        for (Decoration decoration: decorations) {
            if (!decoration.equals(sel)) {
                g.setColor(thisOrBlack(decoration.getColor()));
                decoration.draw(g, scale);
            }
        }

        if (editing) {
            if (selection != null) {
                Decoration dec = selection.getDecoration();
                Color oldColor = dec.getColor();
                boolean oldSaveNeeded = saveNeeded;
                Color highlight = getHighlightColor(oldColor);

                g.setColor(highlight);
                dec.setColor(highlight);
                if (selection instanceof VertexHandle) {
                    paintSelectedCurve(g, scale);
                } else {
                    if (selection instanceof LabelHandle) {
                        paintSelectedLabel(g, scale);
                    } else {
                        sel.draw(g, scale);
                    }
                }
                dec.setColor(oldColor);
                saveNeeded = oldSaveNeeded;
            }

            if (getVertexHandle() == null && isShiftDown) {
                AutoPositionHolder ap = new AutoPositionHolder();
                Point2D.Double autop = getAutoPosition(ap);
                Point2D.Double gmp = getMousePosition();
                if (autop != null && gmp != null
                    && !principalCoordinatesMatch(autop, gmp)) {
                    // Paint a cross at autop.
                    Point2D.Double vPage = principalToScaledPage(scale)
                        .transform(autop);
                    int r = 7;
                    g.setColor(toColor(ap.position));
                    int ix = (int) vPage.x;
                    int iy = (int) vPage.y;
                    g.drawLine(ix, iy - r, ix, iy + r);
                    g.drawLine(ix -r, iy, ix+r, iy);
                }
            }
        }
    }

    /** @return a curve to which a vertex can be appended or inserted.
        If a curve was already selected, then that will be the return
        value; if no curve is selected, then start a new curve and
        return that. */
    @JsonIgnore public GeneralPolyline getCurveForAppend() {
        if (principalToStandardPage == null) {
            return null;
        }

        GeneralPolyline activeCurve = getActiveCurve();
        if (activeCurve != null) {
            return activeCurve;
        }

        paths.add(GeneralPolyline.create
                  (GeneralPolyline.LINEAR,
                   new Point2D.Double[0], lineStyle, lineWidth));
        selection = new VertexHandle(paths.size() - 1, -1);
        insertBeforeSelection = false;
        return getActiveCurve();
    }

    public void deselectCurve() {
        selection = null;
        repaintEditFrame();
    }

    /** Start a new curve where the old curve ends. */
    public void fill() {
        GeneralPolyline path = getActiveCurve();
        if (path == null) {
            JOptionPane.showMessageDialog
                (editFrame, "Fill settings can only be changed when a curve is selected.");
            return;
        }

        if (!path.isClosed()) {
            JOptionPane.showMessageDialog
                (editFrame, "Fill settings can only be changed for closed curves.");
            return;
        }

        path.setFilled(!path.isFilled());
        repaintEditFrame();
    }

    public void addCusp() {
        if (mprin == null) {
            return;
        }

        add(mprin);
        deselectCurve();
        add(mprin);
    }

    /** Update the tangency information to display the slope at the
        given vertex. */
    public void showTangent(ParameterizableHandle hand) {
        showTangent(hand.getDecoration(), hand.getT());
    }

    public void showTangent(Decoration dec, double t) {
    	Parameterization2D param = ((Parameterizable2D) dec).getParameterization();
        if (dec instanceof CurveDecoration) {
            CurveDecoration cdec = (CurveDecoration) dec;
            GeneralPolyline path = cdec.getItem();
            if (path instanceof Polyline && t == Math.floor(t)) {

                // For polylines, insertBeforeSelection gives a hint which
                // of the two segments adjoining a vertex is the one whose
                // derivative we want.
                t = Parameterization2Ds.constrainToDomain
                    (param, t + (insertBeforeSelection ? -0.5 : 0.5));
            }
        }

        Point2D.Double g = param.getDerivative(t);
        if (g != null) {
            vertexInfo.setDerivative(g);
        }

        double w = ((Decoration) dec).getLineWidth();
        if (w != 0) {
            vertexInfo.setLineWidth(w);
        }
    }

    /** Return true if point p is the same as either the currently
        selected vertex or the vertex that immediately follows them.
        The reason to care is that SplinePolyline barfs if you pass
        the same vertex twice in a row. */
    boolean isDuplicate(Point2D p) {
        VertexHandle vhand = getVertexHandle();
        if (vhand == null) {
            return false;
        }
        if (vhand.vertexNo == -1 || vhand.vertexNo > vhand.getItem().size()) {
            System.out.println("vhand = " + vhand);
        }
        if (vhand.getLocation().equals(p)) {
            return true;
        }

        GeneralPolyline path = vhand.getItem();
        return vhand.vertexNo < path.size() - 1 && p.equals(path.get(vhand.vertexNo + 1));
    }

    /** Add a point to getActiveCurve(). */
    public void add(Point2D.Double point) {
        if (isDuplicate(point)) {
            return; // Adding the same point twice causes problems.
        }
        getCurveForAppend();
        VertexHandle vhand = getVertexHandle();
        CurveDecoration csel = vhand.getDecoration();

        int addPos = vhand.vertexNo + (insertBeforeSelection ? -1 : 0 );
        add(csel.getItem(), addPos, point);
        if (!insertBeforeSelection) {
            ++vhand.vertexNo;
        }
        showTangent(vhand);
        repaintEditFrame();
    }

    /** Add a new vertex to path, located at point, and inserted just
        after vertex vertexNo. */
    public void add(GeneralPolyline path, int vertexNo,
                    Point2D.Double point) {
        saveNeeded = true;
        if (path.size() == 0) {
            path.add(0, point);
            return;
        }

        ArrayList<Double> segments = getPathSegments(path);
        int segCnt = path.getSegmentCnt();

        double dist1 = (vertexNo == -1) ? 0
            : point.distance(path.get(vertexNo));
        double dist2 = (vertexNo == segCnt) ? 0
            : point.distance(path.get(vertexNo + 1));

        // For old segment vertexNo, map the t range [0, splitT] to
        // new segment vertexNo range [0,1], and map the t range
        // (splitT, 1] to new segment vertexNo+1 range [0,1]. If
        // vertexNo == segCnt then segment vertexNo never existed
        // before, so it doesn't matter what splitT value we use.
        double splitT = dist1 / (dist1 + dist2);

        for (int i = 0; i < segments.size(); ++i) {
            double t = segments.get(i);
            int segment = (int) Math.floor(t);
            double frac = t - segment;
            if (segment > vertexNo) {
                ++t;
            } else if (segment == vertexNo) {
                if (frac <= splitT) {
                    t = segment + frac / splitT;
                } else {
                    t = (segment + 1) + (frac - splitT) / (1.0 - splitT);
                }
            }
            segments.set(i, t);
        }

        path.add(vertexNo + 1, point);
        setPathSegments(path, segments);
    }

    void removeCurve(int curveNo) {
        saveNeeded = true;
        GeneralPolyline path = paths.get(curveNo);

        // Remove all associated tie lines.
        for (int i = tieLines.size() - 1; i >= 0; --i) {
            TieLine tie = tieLines.get(i);
            if (tie.innerEdge == path || tie.outerEdge == path) {
                tieLines.remove(i);
            }
        }

        // If an incomplete tie line selection refers to this curve,
        // then stop selecting a tie line.

        for (PathAndT pat: tieLineCorners) {
            if (pat.path == path) {
                tieLineDialog.setVisible(false);
                tieLineCorners = null;
                break;
            }
        }

        paths.remove(curveNo);
    }

    /** Make a list of t values that appear in this Editor
        object and that refer to locations on the given path. */
    ArrayList<Double> getPathSegments(GeneralPolyline path) {
        ArrayList<Double> output = new ArrayList<>();

        // Tie Lines' limits are defined by t values.
        for (TieLine tie: tieLines) {
            if (tie.innerEdge == path) {
                output.add(tie.it1);
                output.add(tie.it2);
            }
            if (tie.outerEdge == path) {
                output.add(tie.ot1);
                output.add(tie.ot2);
            }
        }

        return output;
    }

    /** You can change the segments returned by getPathSegments() and
        call setPathSegments() to make corresponding updates to the
        fields from which they came. */
    void setPathSegments(GeneralPolyline path, ArrayList<Double> segments) {
        int index = 0;
        for (TieLine tie: tieLines) {
            if (tie.innerEdge == path) {
                tie.it1 = segments.get(index++);
                tie.it2 = segments.get(index++);
            }
            if (tie.outerEdge == path) {
                tie.ot1 = segments.get(index++);
                tie.ot2 = segments.get(index++);
            }
        }
    }

    /** Print an arrow at the currently selected location that is
        tangent to the curve at that location and that points
        rightward (or downward if the curve is vertical). */
    public void addArrow(boolean rightward) {
        saveNeeded = true;
        if (mouseIsStuckAtSelection() && getSelectedArrow() != null) {
            unstickMouse();
        }

        double theta = vertexInfo.getAngle();
        if (!rightward) {
            theta += Math.PI;
        }

        arrows.add
            (new Arrow(mprin.x, mprin.y, lineWidth,
                       pageToPrincipalAngle(theta)));
        repaintEditFrame();
    }

    void tieLineCornerSelected() {
        VertexHandle vhand = getVertexHandle();
        if (vhand == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "You must select a vertex.");
            return;
        }

        CurveDecoration csel = vhand.getDecoration();
        PathAndT pat = new PathAndT(csel.getItem(), vhand.vertexNo);

        int oldCnt = tieLineCorners.size();

        if ((oldCnt == 1 || oldCnt == 3)
            && pat.path != tieLineCorners.get(oldCnt-1).path) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "This selection must belong to the same\n" +
                 "curve as the previous selection.");
            return;
        }

        if ((oldCnt == 1) && pat.t == tieLineCorners.get(0).t) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "The second point cannot be the same as the first.");
            return;
        }

        tieLineCorners.add(pat);

        if (oldCnt < 3) {
            tieLineDialog.getLabel().setText(tieLineStepStrings[oldCnt + 1]);
            return;
        }

        tieLineDialog.setVisible(false);
        int lineCnt = askNumberOfTieLines(10);

        if (lineCnt <= 0) {
            tieLineDialog.setVisible(false);
            tieLineCorners = null;
            return;
        }

        TieLine tie = new TieLine(lineCnt, lineStyle);
        tie.lineWidth = lineWidth;

        tie.innerEdge = tieLineCorners.get(2).path;
        tie.it1 = tieLineCorners.get(2).t;
        tie.it2 = tieLineCorners.get(3).t;

        tie.outerEdge = tieLineCorners.get(0).path;
        tie.ot1 = tieLineCorners.get(0).t;
        tie.ot2 = tieLineCorners.get(1).t;

        selection = new TieLineHandle(tieLines.size(), TieLineHandleType.OUTER2);
        tieLines.add(tie);
        saveNeeded = true;
        repaintEditFrame();
    }

    int askNumberOfTieLines(int oldCount) {
        while (true) {
            try {
                String lineCntStr = JOptionPane.showInputDialog
                    (editFrame,
                     "Number of tie lines to display (interior only):",
                     new Integer(oldCount));
                if (lineCntStr == null) {
                    return -1;
                }
                int lineCnt = Integer.parseInt(lineCntStr);
                if (lineCnt > 0) {
                    return lineCnt;
                }

                JOptionPane.showMessageDialog
                    (editFrame, "Enter a positive integer.");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog
                    (editFrame, "Invalid number format.");
            }
        }
    }

    public void addRuler() {
        GeneralPolyline path = getActiveCurve();
        if (path == null || path.size() != 2) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "Before you can create a new ruler,\n"
                 + "you must create and select a curve\n"
                 + "consisting of exactly two vertices\n"
                 + "which will become the rulers' endpoints.\n");
            return;
        }

        Object[] choices = new Object[axes.size()];
        int i = -1;
        for (LinearAxis axis: axes) {
            ++i;
            choices[i] = axis.name;
        }

        String s = (String) JOptionPane.showInputDialog
            (editFrame,
             "Select variable to display:",
             "New ruler",
             JOptionPane.PLAIN_MESSAGE,
             null, choices, choices[0]);

        if (s == null) {
            return;
        }

        int choiceNo;
        for (choiceNo = 0; choiceNo < axes.size(); ++choiceNo) {
            if (s.equals(axes.get(choiceNo).name)) {
                break;
            }
        }

        if (choiceNo == axes.size()) {
            throw new IllegalStateException("Choice " + s + " is not on the list!");
        }

        LinearAxis axis = axes.get(choiceNo);

        LinearRuler ruler = new LinearRuler() {{
            fontSize = currentFontSize();
            tickPadding = 0.0;
            labelAnchor = LinearRuler.LabelAnchor.NONE;
            drawSpine = true;
        }};

        ruler.lineWidth = lineWidth;
        ruler.axis = axis;

        if (diagramType.isTernary()) {
            ruler.tickType = LinearRuler.TickType.V;
        }

        VertexHandle vhand = getVertexHandle();
        ruler.startPoint = path.get(1 - vhand.vertexNo);
        ruler.endPoint = path.get(vhand.vertexNo);

        if (!(new RulerDialog(editFrame, "Edit Ruler", ruler))
            .showModal(ruler)) {
            return;
        }

        removeCurve(vhand.getCurveNo());
        rulers.add(ruler);
        saveNeeded = true;

        selection = new RulerHandle(rulers.size() - 1, RulerHandleType.END);
        repaintEditFrame();
    }

    public void addVariable(LinearAxis axis) {
        axes.add(axis);
        editFrame.addVariable((String) axis.name);
        repaintEditFrame();
    }

    public void addVariable() {
        GeneralPolyline path = getActiveCurve();
        if (path == null || path.size() != 3) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "<html><p>To create a new variable, you must "
                 + "first select a curve consisting of exactly three "
                 + "points.</p></html>");
            return;
        }

        String[] values = { "Rupert", "0", "0", "1" };

        while (true) {
            StringArrayDialog dog = new StringArrayDialog
                (editFrame,
                 new String[] { "Variable name",
                                "Value at point #1",
                                "Value at point #2",
                                "Value at point #3" },
                 values,
                 "<html><body width=\"200 px\"><p>"
                 + "Enter the name of the new variable and its values "
                 + "at three different points. The variable must be a "
                 + "linear function (affine transformation) of the principal "
                 + "coordinates. Fractions and percentages are allowed."
                 + "</p></body></html>");
            dog.setTitle("Create new axis");
            values = dog.showModal();
            if (values == null) {
                return;
            }

            if ("".equals(values[0])) {
                JOptionPane.showMessageDialog
                    (null, "Please enter a variable name.");
                continue;
            }

            double[] dvs = new double[3];
            for (int i = 0; i < dvs.length; ++i) {
                try {
                    dvs[i] = ContinuedFraction.parseDouble(values[i+1]);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog
                        (null, "Invalid number format '" + values[i+1] + "'");
                    continue;
                }
            }

            Point2D.Double p0 = path.get(0);
            Point2D.Double p1 = path.get(1);
            Point2D.Double p2 = path.get(2);

            Matrix xform = new Matrix
                (new double[][] {{p0.x, p0.y, 1},
                                 {p1.x, p1.y, 1},
                                 {p2.x, p2.y, 1}});
            xform.print(8,2);
            try {
                Matrix m = xform.solve(new Matrix(dvs, 3));
                LinearAxis axis = new LinearAxis
                    (new DecimalFormat("0.0000"),
                     m.get(0,0), m.get(1,0), m.get(2,0));
                axis.name = values[0];
                addVariable(axis);
                return;
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog
                    (editFrame,
                     "These points are colinear and cannot be used to define an axis."
                     + e + ", " + xform);
                return;
            }
        }
    }

    static class StringComposition {
        /** Decomposition of s into (element name, number of atoms)
            pairs. */
        Map<String,Double> c;
        /** The side (or corner, for ternary diagrams) of the diagram
            that this diagram component belongs to. */
        Side side;
    };

    @JsonIgnore double[][] getComponentElements() {
        if (componentElements != null || diagramType == null) {
            return componentElements;
        }

        ArrayList<StringComposition> scs = new ArrayList<>();

        for (Side side: Side.values()) {
            String dc = diagramComponents[side.ordinal()];
            if (dc == null) {
                continue;
            }
            ChemicalString.Match m = ChemicalString.composition(dc);
            if (m == null) {
                System.out.println("Diagram component '" + dc + "' does not start\n"
                                   + "with a simple chemical formula.");
                continue;
            }
            StringComposition sc = new StringComposition();
            // sc.s = dc.substring(0, m.length);
            sc.c = m.composition;
            sc.side = side;
            scs.add(sc);
        }

        Map<String,Integer> elementIndexes = new HashMap<>();
        ArrayList<String> elements = new ArrayList<>();
        for (StringComposition sc: scs) {
            for (String element: sc.c.keySet()) {
                if (elementIndexes.containsKey(element)) {
                    continue;
                }
                // Don't worry about the value for now.
                elementIndexes.put(element, 0);
                elements.add(element);
            }
        }


        // Now we know which elements are present, in some order.
        // Rearrange to obtain Hill order.
        diagramElements = elements.toArray(new String[0]);
        ChemicalString.hillSort(diagramElements);
        for (int i = 0; i < diagramElements.length; ++i) {
            elementIndexes.put(diagramElements[i], i);
        }

        componentElements = new double[Side.values().length][];
        for (StringComposition sc: scs) {
            double[] compEls = new double[diagramElements.length];
            componentElements[sc.side.ordinal()] = compEls;
            for (Map.Entry<String, Double> pair: sc.c.entrySet()) {
                compEls[elementIndexes.get(pair.getKey())] = pair.getValue();
            }
        }
        return componentElements;
    }

    static class SideDouble {
        Side s;
        double d;

        SideDouble(Side s, double d) {
            this.s = s;
            this.d = d;
        }

        public String toString() {
            return getClass().getSimpleName() + "[" + s + ", " + d + "]";
        }
    }

    /** Assuming that the principal coordinates are defined as mole
        percents, return the mole fractions of the various diagram
        components at point prin, or null if the fractions could not
        be determined.
    */
    protected SideDouble[] componentFractions(Point2D prin) {
        if (prin == null || diagramType == null) {
            return null;
        }
        double x = prin.getX();
        double y = prin.getY();

        if (diagramType.isTernary()) {
            return new SideDouble[] {
                new SideDouble(Side.RIGHT, x),
                new SideDouble(Side.TOP, y),
                new SideDouble(Side.LEFT, 1 - x - y) };
        } else if (diagramComponents[Side.LEFT.ordinal()] != null
                   || diagramComponents[Side.RIGHT.ordinal()] != null) {
            return new SideDouble[] {
                new SideDouble(Side.RIGHT, x),
                new SideDouble(Side.LEFT, 1 - x) };
        } else {
            return null;
        }
    }

    protected Side[] sidesThatCanHaveComponents() {
        if (diagramType.isTernary()) {
            return new Side[] { Side.LEFT, Side.RIGHT, Side.TOP };
        } else {
            return new Side[] { Side.LEFT, Side.RIGHT };
        }
    }

    /** Assuming that diagram's principal coordinates are mole
        fractions, return the weight fractions of the various diagram
        components at point prin, or null if the fractions could not
        be determined. */
    protected SideDouble[] componentWeightFractions(Point2D prin) {
        SideDouble[] res = componentFractions(prin);
        if (res == null) {
            return null;
        }
        double totWeight = 0;
        for (SideDouble sd: res) {
            double cw = componentWeight(sd.s);
            if (cw == 0) {
                return null;
            }
            // Multiply the mole fraction by the compound's weight.
            // Later, dividing by the sum of all of these terms will
            // yield the weight fraction.
            cw *= sd.d;
            sd.d = cw;
            totWeight += cw;
        }
        for (SideDouble sd: res) {
            sd.d /= totWeight;
        }
        return res;
    }

    /** Assuming that diagram's principal coordinates are weight
        fractions, return the weight fractions of the various diagram
        components at point prin, or null if the fractions could not
        be determined. */
    protected SideDouble[] componentMoleFractions(Point2D prin) {
        SideDouble[] res = componentFractions(prin);
        if (res == null) {
            return null;
        }
        double totMole = 0;
        for (SideDouble sd: res) {
            double cw = componentWeight(sd.s);
            if (cw == 0) {
                return null;
            }
            // Divide the weight fraction by the compound's weight.
            // Later, dividing by the sum of all of these terms will
            // yield the mole fraction.
            double mf = sd.d / cw;
            sd.d = mf;
            totMole += mf;
        }
        for (SideDouble sd: res) {
            sd.d /= totMole;
        }
        return res;
    }

    /** Convert the given point from mole fraction to weight fraction.
        If this is a binary diagram, then the Y component of the
        return value will equal the Y component of the input value. If
        the conversion cannot be performed for some reason, then
        return null. */
    public Point2D.Double moleToWeightFraction(Point2D mole) {
        SideDouble[] sds = componentWeightFractions(mole);
        if (sds == null) {
            return null;
        }
        if (diagramType.isTernary()) {
            return new Point2D.Double(sds[0].d, sds[1].d);
        } else {
            return new Point2D.Double(sds[0].d, mole.getY());
        }
    }

    /** Convert the given point from mole fraction to weight fraction.
        If this is a binary diagram, then the Y component of the
        return value will equal the Y component of the input value. If
        the conversion cannot be performed for some reason, then
        return null. */
    public Point2D.Double weightToMoleFraction(Point2D weight) {
        SideDouble[] sds = componentMoleFractions(weight);
        if (sds == null) {
            return null;
        }
        if (diagramType.isTernary()) {
            return new Point2D.Double(sds[0].d, sds[1].d);
        } else {
            return new Point2D.Double(sds[0].d, weight.getY());
        }
    }

    /** Globally convert all coordinates from mole fraction to weight
        fraction, if the information necessary to do so is available.
        Return true if the conversion was carried out.

        IMPORTANT BUGS:

        1. In ternary diagrams, the conversion from mole percent to
        weight percent should distort many straight lines into curves.
        What this routine actually (and incorrectly) does is to
        convert the endpoints only, incorrectly drawing a straight
        line between them.

        2. Angle values are not converted. This is also wrong.
    */
    protected boolean moleToWeightFraction() {
        if (!haveComponentCompositions()) {
            return false;
        }

        for (DecorationHandle hand: movementHandles()) {
            hand.move(moleToWeightFraction(hand.getLocation()));
        }

        if (mprin != null) {
            moveMouse(moleToWeightFraction(mprin));
        }
        return true;
    }

    /** Globally convert all coordinates from weight fraction to mole
        fraction, if the information necessary to do so is available.
        Return true if the conversion was carried out.

        IMPORTANT BUGS:

        1. Angle values are not converted.
    */
    protected boolean weightToMoleFraction() {
        if (!haveComponentCompositions()) {
            return false;
        }

        for (DecorationHandle hand: movementHandles()) {
            hand.move(weightToMoleFraction(hand.getLocation()));
        }

        if (mprin != null) {
            moveMouse(weightToMoleFraction(mprin));
        }
        return true;
    }

    /* Return true if all sides that could have components do have
       them, and those components' composiitions are known. */
    boolean haveComponentCompositions() {
        double[][] componentElements = getComponentElements();
        for (Side side: sidesThatCanHaveComponents()) {
            if (componentElements[side.ordinal()] == null) {
                return false;
            }
        }

        return true;
    }

    /* Have the user enter a string; parse the string as a compound;
       and set the mouse position to the principal coordinates that
       correspond to that compound, if the compound can be expressed
       as a product of the diagram components. For binary diagrams,
       the Y coordinate will be left unchanged from its original
       value. */
    public void computeMolePercent() {
        double[][] componentElements = getComponentElements();

        ArrayList<Side> sides = new ArrayList<>();
        ArrayList<Side> badSides = new ArrayList<>();
        for (Side side: sidesThatCanHaveComponents()) {
            if (diagramComponents[side.ordinal()] == null) {
                JOptionPane.showMessageDialog
                    (editFrame,
                     "The " + side + " diagram component is not defined.\n"
                     + "Define it with the \"Properties/Components/Set "
                     + side.toString().toLowerCase() + " component\" menu item.");
                return;
            }
            if (componentElements[side.ordinal()] == null) {
                badSides.add(side);
            } else {
                sides.add(side);
            }
        }

        if (sides.size() < 2) {
            StringBuilder message = new StringBuilder
                ("The following diagram component(s) could not be parsed as "
                 + "compounds:\n");
            int sideNo = 0;
            for (Side side: badSides) {
                if (sideNo > 0) {
                    message.append(", ");
                }
                message.append(side.toString());
            }
            JOptionPane.showMessageDialog(editFrame, message.toString());
            return;
        }

        ChemicalString.Match m = null;
        String compound = null;
        for (;;) { // Keep trying until user aborts or enters valid input
            compound = JOptionPane.showInputDialog
                (editFrame,
                 "The string you enter will be placed in the clipboard.\n"
                 + "In Windows, you may press Control-V later to paste\n"
                 + "the text into a label's text box.\n"
                 + "Chemical formula:",
                 "Compute mole percent", JOptionPane.PLAIN_MESSAGE);
            if (compound == null) {
                return;
            }
            compound = compound.trim();
            if ("".equals(compound)) {
                return;
            }
            m = ChemicalString.composition(compound);
            if (m == null) {
                JOptionPane.showMessageDialog
                    (editFrame, "Parse error at start of formula");
                continue;
            } else if (m.length < compound.length()) {
                JOptionPane.showMessageDialog
                    (editFrame, "Parse error at <<HERE in '"
                     + compound.substring(0, m.length)
                     + "<<HERE" + compound.substring(m.length) + "'");
                continue;
            } else {
                break;
            }
        }

        try {
            StringSelection sel = new StringSelection(compound);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents
                (sel, sel);
        } catch (HeadlessException e) {
            // Can't set the clipboard? Don't worry about it.
        }

        // Map from elements to indexes into diagramElements.
        String[] diagramElements = getDiagramElements();
        int eltCnt = diagramElements.length;
        Map<String,Integer> elementIndexes = new HashMap<>();
        for (int i = 0; i < eltCnt; ++i) {
            elementIndexes.put(diagramElements[i], i);
        }

        double[][] quantities = new double[diagramElements.length]
            [sides.size()];

        for (int cn = 0; cn < sides.size(); ++cn) {
            Side side = sides.get(cn);
            double[] elts = componentElements[side.ordinal()];
            for (int j = 0; j < eltCnt; ++j) {
                quantities[j][cn] = elts[j];
            }
        }

        double[] coefs = new double[eltCnt];
        for (Map.Entry<String, Double> pair: m.composition.entrySet()) {
            String element = pair.getKey();
            Integer i = elementIndexes.get(element);
            if (i == null) {
                JOptionPane.showMessageDialog
                    (editFrame, "No parseable diagram component contains "
                     + element + ".");
                return;
            }
            coefs[i] = pair.getValue();
        }

        // Matrix that transforms a vector of quantities of each
        // component into a vector of quantities of each element.
        Matrix c2e = new Matrix(quantities);
        Matrix e = new Matrix(coefs, coefs.length);
        Matrix c = c2e.solve(e); // Vector of quantities of each diagram coefficient.

        double totalQuantity = 0;
        for (int compNo = 0; compNo < sides.size(); ++compNo) {
            double d = c.get(compNo, 0);
            if (d < 0) {
                // Round negative values up to zero.
                d = 0;
                c.set(compNo, 0, d);
            }
            totalQuantity += d;
        }

        Matrix compE = c2e.times(c); // computed element counts
        double totalError = 0; // total difference between computed
                               // and actual element counts
        double totalAtoms = 0;
        for (int i = 0; i < coefs.length; ++i) {
            double computed = compE.get(i,0);
            double wanted = e.get(i,0);
            totalError += Math.abs(computed - wanted);
            totalAtoms += Math.abs(wanted);
        }

        double maxRelativeError = 1e-4; // maximum ratio between total
                                        // error in element counts and
                                        // the total number of atoms
        if (totalError > totalAtoms * maxRelativeError) {
                JOptionPane.showMessageDialog
                    (editFrame,
                     "Showing the best fit, which has a relative error of "
                     + String.format("%.2f%%",
                                     100 * totalError / totalAtoms));
        }

        Point2D.Double fractions;

        if (diagramType.isTernary()) {
            fractions = new Point2D.Double(0,0);
        } else if (mprin != null) {
            fractions = new Point2D.Double(0,mprin.y);
        } else {
            Rectangle2D b = principalToStandardPage.outputBounds();
            fractions = new Point2D.Double(0, b.getMaxY());
        }

        for (int compNo = 0; compNo < sides.size(); ++compNo) {
            double fraction = c.get(compNo, 0) / totalQuantity;
            Side side = sides.get(compNo);

            switch (side) {
            case RIGHT:
                fractions.x = fraction;
                break;
            case TOP:
                fractions.y = fraction;
                break;
            case LEFT:
                // There's nothing to do here. If all 3
                // fractions are defined, then the RIGHT and TOP
                // values already define the coordinate. If only 2
                // fractions are defined, then the undefined
                // coordinate has already been set to the correct
                // value, which is zero (so if LEFT + RIGHT are
                // defined then TOP = 0, and if LEFT + TOP are
                // defined then RIGHT = 0).
                break;
            default:
                throw new IllegalStateException("Side " + side + " should not have an " +
                                                "associated component.");
            }
        }

        mouseIsStuck = true;
        moveMouse(fractions);
    }

    static class OrderByXY implements Comparator<Point2D.Double> {
        public int compare(Point2D.Double a, Point2D.Double b) {
            double ax = a.x;
            double bx = b.x;
            return (ax < bx) ? -1 : (ax > bx) ? 1
                : (a.y < b.y) ? -1 : (a.y > b.y) ? 1 : 0;
        }
    }

    public void copyCoordinatesToClipboard() {
        ArrayList<Point2D.Double> points = new ArrayList<>();
        GeneralPolyline path = getActiveCurve();
        if (path != null) {
            points.addAll(Arrays.asList(path.getPoints()));
        } else {
            LabelDecoration ldec = getSelectedLabel();
            if (ldec != null) {
                String text = ldec.getItem().getText();
                for (AnchoredLabel label: labels) {
                    if (text.equals(label.getText())) {
                        points.add(new Point2D.Double(label.getX(), label.getY()));
                    }
                }
                Collections.sort(points, new OrderByXY());
            } else {
            	JOptionPane.showMessageDialog
                	(editFrame, "You need to select a curve or label before using this function.");
            	return;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Point2D.Double point: points) {
            sb.append(point.x + ", " + point.y + "\n");
        }

        String res = sb.toString();
        
        try {
            StringSelection sel = new StringSelection(res);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents
                (sel, sel);
        } catch (HeadlessException e) {
            throw new IllegalArgumentException
                ("Can't call coordinatesToClipboard() in a headless environment:" + e);
        }
    }

    @JsonIgnore String[] getDiagramElements() {
        getComponentElements();
        return diagramElements;
    }

    public void addTieLine() {
        tieLineCorners = new ArrayList<PathAndT>();
        tieLineDialog.getLabel().setText(tieLineStepStrings[0]);
        tieLineDialog.pack();
        tieLineDialog.setVisible(true);
        tieLineDialog.toFront();
    }

    /** @return the index of the label that is closest to mprin, as
        measured by distance from the center of the label to mprin on
        the standard page. */
    int nearestLabelNo() {
        if (mprin == null) {
            return -1;
        }

        Affine toPage = getPrincipalToAlignedPage();
        Point2D.Double mousePage = toPage.transform(mprin);
        Point2D.Double centerPage = new Point2D.Double();

        int output = -1;
        double minDistance = 0.0;
        for (int i = 0; i < labels.size(); ++i) {
            toPage.transform(labelCenters.get(i), centerPage);
            double distance = centerPage.distance(mousePage);
            if (output == -1 || distance < minDistance) {
                output = i;
                minDistance = distance;
            }
        }

        return output;
    }

    /** Move the mouse to the nearest key point.

        @param select If true, exclude unselectable points, and select
        the point that is returned. */
    public void seekNearestPoint(boolean select) {
        if (mouseIsStuck && principalFocus == null) {
            unstickMouse();
        }

        Point2D.Double point;

        if (!select) {
            point = nearestPoint();
            if (point == null) {
                return;
            }
        } else {
            boolean haveFocus = (principalFocus != null);
            ArrayList<DecorationHandle> points = nearestHandles();
            if (points.isEmpty()) {
                return;
            }

            DecorationHandle sel = points.get(0);

            if (selection != null && haveFocus) {
                // Check if the old selection is one of the nearest
                // points. If so, then choose the next one after it. This
                // is to allow users to cycle through a set of overlapping
                // key points using the selection key. Select once for the
                // first item; select it again and get the second one,
                // then the third, and so on.

                int matchCount = 0;
                for (DecorationHandle handle: points) {
                    if (handle.equals(selection)) {
                        ++matchCount;
                    }
                }

                if (matchCount != 1) {
                    // I don't think this should ever happen.
                    // nearestHandles() does not return everything (it
                    // only returns the nearest handle for any given
                    // decoration), but if the nearest handle on any
                    // given decoration has changed, then
                    // principalFocus should probably have been set to
                    // null.
                    String error = "Selection " + selection + " equals "
                        + matchCount + " decorations.";
                    System.err.println(error);
                    for (DecorationHandle handle: points) {
                        if (matchCount == 0 || handle.equals(selection)) {
                            System.err.println(handle);
                        }
                    }
                    throw new IllegalStateException(error);
                } else if (matchCount == 1) {
                    for (int i = 0; i < points.size() - 1; ++i) {
                        if (selection.equals(points.get(i))) {
                            sel = points.get(i+1);
                            break;
                        }
                    }
                }
            }

            selection = sel;

            if (sel instanceof ParameterizableHandle) {
                showTangent((ParameterizableHandle) sel);
            }

            point = sel.getLocation();
        }

        moveMouse(point);
        mouseIsStuck = true;
    }

    public void scaleXUnits() {
        scaleUnits(getXAxis());
    }

    public void scaleYUnits() {
        scaleUnits(getYAxis());
    }

    public void scaleUnits(LinearAxis axis) {
        if (axis == null) {
            return;
        }

        String[] columnNames =
            {"Old " + axis.name + " value", "New " + axis.name + " value"};

        Object[][] data;

        boolean isX = axis.isXAxis();

        Rectangle2D.Double principalBounds = getPrincipalBounds();

        if (isX) {
            data = new Object[][]
                {{principalBounds.x, principalBounds.x},
                 {principalBounds.x+principalBounds.width,
                  principalBounds.x+principalBounds.width}};
        } else if (axis.isYAxis()) {
            data = new Object[][]
                {{principalBounds.y, principalBounds.y},
                 {principalBounds.y+principalBounds.height,
                  principalBounds.y+principalBounds.height}};
        } else {
            throw new IllegalStateException("Only x and y units are adjustable");
        }

        TableDialog dog = new TableDialog(editFrame, data, columnNames);
        dog.setTitle("Scale " + axis.name + " units");
        Object[][] output = dog.showModal();
        if (output == null) {
            return;
        }

        // Compute {m,b} such that y = mx + b. Note that here, y is
        // the new axis value and x is the old one -- this has nothing
        // to do with x-axis versus y-axis.

        double x1 = (Double) output[0][0];
        double x2 = (Double) output[1][0];
        double y1 = (Double) output[0][1];
        double y2 = (Double) output[1][1];

        if (x1 == x2 || y1 == y2) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "Please choose two different old values\n"
                 + "and two different new values.");
            return;
        }

        double m = (y2 - y1)/(x2 - x1);
        double b = y1 - m * x1;

        AffineTransform xform = isX
            ? new AffineTransform(m, 0.0, 0.0, 1.0, b, 0.0)
            : new AffineTransform(1.0, 0.0, 0.0, m, 0.0, b);

        invisiblyTransformPrincipalCoordinates(xform);
        repaintEditFrame();
    }

    public void setDiagramComponent(Side side) {
        String old = diagramComponents[side.ordinal()];
        if (old == null) {
            old = "";
        }

        String str = JOptionPane.showInputDialog
            (editFrame, side + " diagram component name:", old);
        if (str == null) {
            return;
        }
        saveNeeded = true;
        componentElements = null;

        // When updating a diagram component, you may also have to
        // update the corresponding axis name and format (since xx.x%
        // format is used with component axes).

        if (str.equals("")) {
            // Reset this axis to the default.
            diagramComponents[side.ordinal()] = null;
            LinearAxis axis = getAxis(side);
            if (axis != null) {
                axes.remove(axis);
            }
            axes.add(defaultAxis(side));
        } else {
            LinearAxis axis = getAxis(side);
            if (axis != null) {
                axis.name = str;
                axis.format = new DecimalFormat("##0.00%");
                for (LinearRuler r: rulers) {
                    if (r.axis == axis) {
                        // Show percentages on this axis.
                        r.multiplier = 100.0;
                    }
                }
            }
            diagramComponents[side.ordinal()] = (str.equals("") ? null : str);
        }
        updateTitle();
    }

    LinearAxis getAxis(Side side) {
        switch (side) {
        case RIGHT:
            return getXAxis();
        case TOP:
            return getYAxis();
        case LEFT:
            return getZAxis();
        }
        return null;
    }

    @JsonIgnore public LinearAxis getXAxis() {
        for (LinearAxis axis: axes) {
            if (axis.isXAxis()) {
                return axis;
            }
        }
        return null;
    }

    @JsonIgnore public LinearAxis getYAxis() {
        for (LinearAxis axis: axes) {
            if (axis.isYAxis()) {
                return axis;
            }
        }
        return null;
    }

    /*
    static boolean isZAxis(LinearAxis axis) {
        String name = axis.name;
        return name != null
            && name.equals(diagramComponents[Side.LEFT]);
    }
    */
            
    static boolean isZAxis(LinearAxis axis) {
        // TODO This approach doesn't work with ternary diagrams that
        // are sliced of more complex systems.
        return axis.getA() == -1.0 && axis.getB() == -1.0
            && axis.getC() == 1.0;
    }

    static boolean isPrimaryAxis(LinearAxis axis) {
        return (axis.isXAxis() || axis.isYAxis() || isZAxis(axis));
    }

    // Smells kludgy...
    static boolean isStandardAxis(LinearAxis axis) {
        if (isPrimaryAxis(axis)) {
            return true;
        }

        String name = (String) axis.name;
        return name.equals("page X") || name.equals("page Y");
    }

    @JsonIgnore public LinearAxis getZAxis() {
        for (LinearAxis axis: axes) {
            if (isZAxis(axis)) {
                return axis;
            }
        }
        return null;
    }

    public String getDiagramComponent(Side side) {
        return diagramComponents[side.ordinal()];
    }

    public boolean axisIsFractional(LinearAxis axis) {
        return (axis.isXAxis() && getDiagramComponent(Side.RIGHT) != null)
            || (axis.isYAxis() && getDiagramComponent(Side.TOP) != null)
            || (isZAxis(axis) && getDiagramComponent(Side.LEFT) != null);
    }

    /** Invoked from the EditFrame menu */
    public void addLabel() {
        if (principalToStandardPage == null) {
            return;
        }

        if (mprin == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "Position the mouse where the label belongs,\n"
                 + "then press the Enter short-cut key to add the label.");
            return;
        }

        if (mouseIsStuckAtSelection() && getSelectedLabel() != null) {
            unstickMouse();
        }

        LabelDialog dog = getLabelDialog();
        double fontSize = dog.getFontSize();
        dog.reset();
        dog.setTitle("Add Label");
        dog.setFontSize(fontSize);
        AnchoredLabel newLabel = dog.showModal();
        if (newLabel == null || "".equals(newLabel.getText())) {
            return;
        }

        newLabel.setAngle(pageToPrincipalAngle(newLabel.getAngle()));
        newLabel.setX(mprin.x);
        newLabel.setY(mprin.y);
        add(newLabel);
        selection = new LabelHandle(labels.size() - 1, LabelHandleType.ANCHOR);
        mouseIsStuck = true;
    }

    public void add(AnchoredLabel label) {
        saveNeeded = true;
        labels.add(label);
        labelViews.add(toView(label));
        labelCenters.add(new Point2D.Double());
        repaintEditFrame();
    }

    public void editLabel(int index) {
        AnchoredLabel label = (AnchoredLabel) labels.get(index).clone();
        label.setAngle(principalToPageAngle(label.getAngle()));
        LabelDialog dog = getLabelDialog();
        dog.setTitle("Edit Label");
        dog.set(label);
        AnchoredLabel newLabel = dog.showModal();
        if (newLabel == null) {
            return;
        }

        saveNeeded = true;
        newLabel.setAngle(pageToPrincipalAngle(newLabel.getAngle()));
        newLabel.setX(label.getX());
        newLabel.setY(label.getY());
        newLabel.setBaselineXOffset(label.getBaselineXOffset());
        newLabel.setBaselineYOffset(label.getBaselineYOffset());
        labels.set(index, newLabel);
        labelViews.set(index, toView(newLabel));
        repaintEditFrame();
    }

    /** @param xWeight Used to determine how to justify rows of text. */
    View toView(String str, double xWeight, Color textColor) {
        String style
            = "<style type=\"text/css\"><!--"
            + " body { font-size: 100 pt; } "
            + " sub { font-size: 75%; } "
            + " sup { font-size: 75%; } "
            + " --></style>";

        StringBuilder sb = new StringBuilder("<html><head");
        sb.append(style);
        sb.append("</head><body>");

        if (xWeight >= 0.67) {
            sb.append("<div align=\"right\">");
            sb.append(str);
            sb.append("</div>");
        } else if (xWeight >= 0.33) {
            sb.append("<div align=\"center\">");
            sb.append(str);
            sb.append("</div>");
        } else {
            sb.append(str);
        }
        sb.append("</body></html>");
        str = sb.toString();

        JLabel bogus = new JLabel(str);
        Font f = getFont();
        bogus.setFont(f.deriveFont((float) (f.getSize() * VIEW_MAGNIFICATION)));
        // bogus.setFont(f);
        bogus.setForeground(thisOrBlack(textColor));
        return (View) bogus.getClientProperty("html");
    }

    View toView(AnchoredLabel label) {
        return toView(label.getText(), label.getXWeight(), label.getColor());
    }

    /** Regenerate the labelViews field from the labels field. */
    void initializeLabelViews() {
        labelViews = new ArrayList<View>();
        for (AnchoredLabel label: labels) {
            labelViews.add(toView(label));
            labelCenters.add(new Point2D.Double());
        }
    }

    @JsonProperty ArrayList<AnchoredLabel> getLabels() {
        return labels;
    }

    @JsonProperty void setLabels(Collection<AnchoredLabel> labels) {
        saveNeeded = true;
        this.labels = new ArrayList<AnchoredLabel>(labels);
    }

    public DiagramType getDiagramType() {
        return diagramType;
    }

    public void setDiagramType(DiagramType t) {
        saveNeeded = true;
        this.diagramType = t;
    }

    double principalToPageAngle(double theta) {
        Point2D.Double p = new Point2D.Double(Math.cos(theta), Math.sin(theta));
        principalToStandardPage.deltaTransform(p, p);
        return Math.atan2(p.y, p.x);
    }

    double pageToPrincipalAngle(double theta) {
        Point2D.Double p = new Point2D.Double(Math.cos(theta), Math.sin(theta));
        standardPageToPrincipal.deltaTransform(p, p);
        return Math.atan2(p.y, p.x);
    }

    /** Invoked from the EditFrame menu */
    public void setLineStyle(StandardStroke lineStyle) {
        this.lineStyle = lineStyle;
        if (selection != null) {
            selection.getDecoration().setLineStyle(lineStyle);
        }
    }

    /** Move the mouse pointer so its position corresponds to the
        given location in principal coordinates. */
    void moveMouse(Point2D.Double point) {
        mprin = point;
        if (principalToStandardPage == null) {
            return;
        }

        Point mpos = Duh.floorPoint
            (principalToScaledPage(scale).transform(mprin));
        
        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();

        if (view.contains(mpos)) {
            Point topCorner = spane.getLocationOnScreen();

            // For whatever reason (Java bug?), I need to add 1 to the x
            // and y coordinates if I want the output to satisfy

            // getEditPane().getMousePosition() == original mpos value.

            mpos.x += topCorner.x - view.x + 1;
            mpos.y += topCorner.y - view.y + 1;

            try {
                ++paintSuppressionRequestCnt;
                Robot robot = new Robot();
                robot.mouseMove(mpos.x, mpos.y);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            } finally {
                --paintSuppressionRequestCnt;
            }

            repaintEditFrame();
        } else {
            Point2D.Double mousePage = principalToStandardPage
                .transform(mprin);
            if (pageBounds.contains(mousePage.x, mousePage.y)) {
                centerMouse();
            } else {
                // mprin can't be both off-screen and off-page at the
                // same time. Move the mouse to the middle of the page
                // instead.
                mousePage.x = pageBounds.x + pageBounds.width/2;
                mousePage.y = pageBounds.y + pageBounds.height/2;
                moveMouse(standardPageToPrincipal.transform(mousePage));
            }
        }

    }

    /** Put mprin and the mouse in the center of the screen (with
        some restrictions). */
    public void centerMouse() {
        if (mprin == null) {
            return;
        }
        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();
        setViewportRelation(mprin,
                            new Point(view.width/2, view.height/2));
    }

    /** Move the mouse to the selection and center it. */
    public void centerSelection() {
        if (selection == null) {
            return;
        }
        mprin = selection.getLocation();
        centerMouse();
        mouseIsStuck = true;
    }

    /** Adjust the viewport to place principal coordinates point prin
        at the given position within the viewport of the scroll pane,
        unless that would require bringing off-page regions into view.
    */
    public void setViewportRelation(Point2D prin,
                                    Point viewportPoint) {
        ++paintSuppressionRequestCnt;
        Affine xform = principalToScaledPage(scale);
        Point panePoint = Duh.floorPoint(xform.transform(prin));
        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();

        // Compute the view that would result from placing prin
        // exactly at viewportPoint.
        view.x = panePoint.x - viewportPoint.x;
        view.y = panePoint.y - viewportPoint.y;

        Rectangle spb = scaledPageBounds(scale);
        // If mapping prin to viewportPoint would bring off-page
        // regions into view, then adjust the mapping.

        if (view.width >= spb.width || view.x < 0) {
            // Never show off-page regions to the left.
            view.x = 0;
        } else if (view.x + view.width > spb.width) {
            // Don't show off-page regions to the right unless the
            // other rules take priority.
            view.x = spb.width - view.width;
        }

        if (view.height >= spb.height || view.y < 0) {
            view.y = 0;
        } else if (view.y + view.height > spb.height) {
            view.y = spb.height - view.height;
        }

        Point viewPosition = new Point(view.x, view.y);

        // Java 1.6_29 needs to be told twice which viewport to
        // use. It seems to get the Y scrollbar right the first
        // time, and the X scrollbar right the second time.
        preserveMprin = true;
        spane.getViewport().setViewPosition(viewPosition);
        spane.getViewport().setViewPosition(viewPosition);
        if (mprin != null) {
            moveMouse(mprin);
        }
        preserveMprin = false;
        --paintSuppressionRequestCnt;
        repaintEditFrame();
    }

    /** @return the location in principal coordinates of the key
        point closest (by page distance) to mprin. */
    Point2D.Double nearestPoint() {
        if (mprin == null) {
            return null;
        }
        return nearestPoint(principalToStandardPage.transform(mprin));
    }

    /** @return the location in principal coordinates of the key point
        closest (by page distance) to pagePoint, where pagePoint is
        expressed in standard page coordinates. */
    Point2D.Double nearestPoint(Point2D pagePoint) {
        if (pagePoint == null) {
            return null;
        }
        Point2D.Double nearPoint = null;
        Point2D.Double xpoint2 = new Point2D.Double();

        // Square of the minimum distance from mprin of all key points
        // examined so far, as measured in standard page coordinates.
        double minDistSq = 0;

        ArrayList<Point2D.Double> points = keyPoints();
        if (selection != null && !mouseIsStuckAtSelection()) {
            // Add the point on (the curve closest to pagePoint) that
            // is closest to selection.

            // In this case, we don't really need the precision that
            // nearestCurve() provides -- all we need is a good guess
            // for what the closest curve is -- but whatever...
            DecorationDistance nc = nearestCurve(pagePoint);
            if (nc != null) {
                if (nc.decoration instanceof Parameterizable2D) {
                    Parameterization2D param = ((Parameterizable2D) nc.decoration)
                        .getParameterization();
                    Point2D.Double selPage = principalToStandardPage
                        .transform(selection.getLocation());
                    CurveDistanceRange cdr = param.distance(selPage, 1e-6, 1000);
                    if (cdr != null) {
                        points.add(standardPageToPrincipal.transform(cdr.point));
                    }
                }
            }
        }
        
        for (Point2D.Double point: points) {
            principalToStandardPage.transform(point, xpoint2);
            double distSq = pagePoint.distanceSq(xpoint2);
            if (nearPoint == null || distSq < minDistSq) {
                nearPoint = point;
                minDistSq = distSq;
            }
        }

        return nearPoint;
    }

    static class HandleAndDistance implements Comparable<HandleAndDistance> {
        DecorationHandle handle;
        double distance;
        public HandleAndDistance(DecorationHandle de, double di) {
            handle = de;
            distance = di;
        }
        public int compareTo(HandleAndDistance other) {
            return (distance < other.distance) ? -1
                : (distance > other.distance) ? 1 : 0;
        }

        public String toString() {
            return "HaD[" + handle + ", " + distance + "]";
        }
    }

    static class DecorationDistance implements Comparable<DecorationDistance> {
        Decoration decoration;
        CurveDistance distance;

        public DecorationDistance(Decoration de, CurveDistance di) {
            decoration = de;
            distance = di;
        }

        public int compareTo(DecorationDistance other) {
            return (distance.distance < other.distance.distance) ? -1
                : (distance.distance > other.distance.distance) ? 1 : 0;
        }

        public String toString() {
            return getClass().getSimpleName() + "[" + decoration + ", "
                + distance + "]";
        }
    }

    /** @return a list of DecorationHandles such that moving all of
        those handles will move every decoration in the diagram. */
    ArrayList<DecorationHandle> movementHandles() {
        ArrayList<DecorationHandle> res = new ArrayList<>();
        for (Decoration d: getDecorations()) {
            for (DecorationHandle h: d.getMovementHandles()) {
                res.add(h);
            }
        }
        return res;
    }

    /** @return a list of DecorationHandles sorted by distance in page
        coordinates from point p (expressed in principal coordinates).
        Generally, only the closest DecorationHandle for each
        Decoration is included, though perhaps an exception should be
        made for VertexHandles. */
    ArrayList<DecorationHandle> nearestHandles(Point2D.Double p) {
        Point2D.Double pagePoint = principalToStandardPage.transform(p);

        ArrayList<HandleAndDistance> hads = new ArrayList<>();
        for (Decoration d: getDecorations()) {
            double minDistSq = 0;
            DecorationHandle nearestHandle = null;
            for (DecorationHandle h: d.getHandles()) {
                Point2D.Double p2 = h.getLocation();
                p2 = principalToStandardPage.transform(p2);
                double distSq = pagePoint.distanceSq(p2);
                if (nearestHandle == null || distSq < minDistSq) {
                    nearestHandle = h;
                    minDistSq = distSq;
                }
            }
            if (nearestHandle != null) {
                hads.add(new HandleAndDistance(nearestHandle, minDistSq));
            }
        }

        Collections.sort(hads);

        ArrayList<DecorationHandle> output = new ArrayList<>();
        for (HandleAndDistance h: hads) {
            output.add(h.handle);
        }

        return output;
    }

    /** @return a list of all DecorationHandles in order of their
        distance from principalFocus (if not null) or mprin (otherwise). */
    ArrayList<DecorationHandle> nearestHandles() {
        if (mprin == null) {
            return null;
        }
        if (principalFocus == null) {
            principalFocus = mprin;
        }

        return nearestHandles(principalFocus);
    }

    /** @return a list of all key points in the diagram. Some
        duplication is likely. */
    public ArrayList<Point2D.Double> keyPoints() {
        ArrayList<Point2D.Double> res = intersections();

        for (Point2D.Double p: principalToStandardPage.getInputVertices()) {
            res.add(p);
        }

        for (DecorationHandle m: getDecorationHandles()) {
            res.add(m.getLocation());
        }

        // Add all segment midpoints.
        for (Line2D.Double s: getAllLineSegments()) {
            res.add(new Point2D.Double((s.getX1() + s.getX2()) / 2,
                                       (s.getY1() + s.getY2()) / 2));
        }

        if (selection != null) {
            Point2D.Double p1 = selection.getLocation();
            Point2D.Double p2 = secondarySelectionLocation();
            if (p2 != null) {
                // Add the doublings of segment p1p2.
                res.add(new Point2D.Double(p1.getX() + (p1.getX() - p2.getX()),
                                           p1.getY() + (p1.getY() - p2.getY())));
                res.add(new Point2D.Double(p2.getX() + (p2.getX() - p1.getX()),
                                           p2.getY() + (p2.getY() - p1.getY())));
            }
        }
        return res;
    }

    /** @return a list of all possible selections. */
    @JsonIgnore ArrayList<Decoration> getDecorations() {
        ArrayList<Decoration> res = new ArrayList<>();

        for (int i = 0; i < paths.size(); ++i) {
            res.add(new CurveDecoration(i));
        }
        for (int i = 0; i < arrows.size(); ++i) {
            res.add(new ArrowDecoration(i));
        }
        for (int i = 0; i < tieLines.size(); ++i) {
            res.add(new TieLineDecoration(i));
        }
        for (int i = 0; i < rulers.size(); ++i) {
            res.add(new RulerDecoration(i));
        }
        for (int i = 0; i < labels.size(); ++i) {
            res.add(new LabelDecoration(i));
        }

        return res;
    }


    /** @return a list of all possible selections. */
    @JsonIgnore ArrayList<DecorationHandle> getDecorationHandles() {
        ArrayList<DecorationHandle> output = new ArrayList<>();

        for (Decoration selectable: getDecorations()) {
            output.addAll(Arrays.asList(selectable.getHandles()));
        }
        return output;
    }


    /** @return a list of all segment+segment and segment+curve
        intersections. curve+curve intersections are not detected at
        this time. */
    ArrayList<Point2D.Double> intersections() {
        ArrayList<Point2D.Double> output = new ArrayList<Point2D.Double>();
        Line2D.Double[] segments = getAllLineSegments();

        // Spline curves are defined in the page coordinates, and the
        // transformation of a spline fit in principal coordinates
        // does not equal the spline fit of the transformation, so
        // convert the segments to page coordinates to match the
        // splines.
        Line2D.Double[] pageSegments = new Line2D.Double[segments.length];
        for (int i = 0; i < segments.length; ++i) {
            pageSegments[i] = new Line2D.Double
                (principalToStandardPage.transform(segments[i].getP1()),
                 principalToStandardPage.transform(segments[i].getP2()));
        }

        for (GeneralPolyline path: paths) {
            GeneralPolyline pagePath = path.createTransformed
                (principalToStandardPage);

            for (Line2D segment: pageSegments) {
                for (Point2D.Double point:
                         pagePath.segIntersections(segment)) {
                    standardPageToPrincipal.transform(point, point);
                    output.add(point);
                }
            }
        }

        return output;
    }

    /* Return the DecorationDistance for the curve or ruler whose
       outline comes closest to pagePoint. This routine operates
       entirely in standard page space, both internally and in terms
       of the input and output values. */
    DecorationDistance nearestCurve(Point2D pagePoint) {
        ArrayList<Decoration> decs = new ArrayList<>();
        ArrayList<Parameterization2D> params = new ArrayList<>();
        for (Decoration dec: getDecorations()) {
            if (dec instanceof Parameterizable2D) {
                Parameterization2D param
                    = ((Parameterizable2D) dec).getParameterization();
                if (param.getMinT() == param.getMaxT()) {
                    // That's a point, not a curve. If the user wanted
                    // a point, they would have pressed 'p' instead.
                    continue;
                }
                decs.add(dec);
                params.add(param);
            }
        }

        if (params.size() == 0) {
            return null;
        }

        OffsetParam2D.DistanceIndex di
            = OffsetParam2D.distance(params, pagePoint, 1e-6, 2000);
        return new DecorationDistance(decs.get(di.index), di.distance);
    }


    /** Like seekNearestPoint(), but locate the nearest decoration
        outline (as measured by distance on the standard page) instead
        of the nearest key point.

        @param select If true, select the nearer of the two
        DecorationHandles that neighbor the selected point on the
        curve. */
    public void seekNearestCurve(boolean select) {
        if (mouseIsStuck) {
            unstickMouse();
        }

        if (mprin == null) {
            return;
        }

        Point2D.Double mousePage = principalToStandardPage.transform(mprin);
        DecorationDistance dist = nearestCurve(mousePage);

        if (dist == null) {
            return;
        }

        DecorationHandle handle = null;

        Decoration dec = dist.decoration;
        CurveDistance minDist = dist.distance;
        double t = minDist.t;
        Parameterization2D c
          = ((Parameterizable2D) dec).getParameterization();
        int vertex = (int) Parameterization2Ds.getNearestVertex(c, t);
        boolean closerToNext = (t < vertex);

        if (dec instanceof CurveDecoration) {
            CurveDecoration cdec = (CurveDecoration) dec;
            handle = cdec.getHandle(t);
        } else if (dec instanceof RulerDecoration) {
            RulerDecoration rdec = (RulerDecoration) dec;
            handle = rdec.getHandle(t);
        } else {
            throw new IllegalStateException("Huh? " + dec);
        }

        if (select) {
            selection = handle;
            if (selection instanceof VertexHandle) {
                GeneralPolyline path = getActiveCurve();
                insertBeforeSelection = closerToNext
                    || (path.size() >= 2 && t == 0);
            }
        }
        moveMouse(standardPageToPrincipal.transform(minDist.point));
        mouseIsStuck = true;
        showTangent(dec, t);
    }

    public void toggleSmoothing() {
        GeneralPolyline oldPath = getActiveCurve();
        if (oldPath == null) {
            return;
        }

        saveNeeded = true;
        GeneralPolyline path = oldPath.nearClone
            (oldPath.getSmoothingType() == GeneralPolyline.LINEAR
             ? GeneralPolyline.CUBIC_SPLINE : GeneralPolyline.LINEAR);

        // Switch over all tie lines defined using the old path to use
        // the new one instead. T values remain unchanged.

        for (TieLine tie: tieLines) {
            if (tie.innerEdge == oldPath) {
                tie.innerEdge = path;
            }
            if (tie.outerEdge == oldPath) {
                tie.outerEdge = path;
            }
        }

        paths.set(getVertexHandle().getCurveNo(), path);
        repaintEditFrame();
    }

    /** Toggle the closed/open status of the currently selected
        curve. */
    public void toggleCurveClosure() {

        GeneralPolyline path = getActiveCurve();
        if (path == null) {
            return;
        }
        saveNeeded = true;
        path.setClosed(!path.isClosed());
        repaintEditFrame();
    }

    /** principalToStandardPage shifted to put the pageBounds corner
        at (0,0). */
    Affine getPrincipalToAlignedPage() {
        Affine xform = new Affine
            (AffineTransform.getTranslateInstance(-pageBounds.x, -pageBounds.y));
        xform.concatenate(principalToStandardPage);
        return xform;
    }

    void draw(Graphics2D g, GeneralPolyline path, double scale) {
        path.draw(g, getPrincipalToAlignedPage(), scale);
    }

    void draw(Graphics2D g, TieLine tie, double scale) {
        tie.draw(g, getPrincipalToAlignedPage(), scale);
    }

    /** @return the name of the image file that this diagram was
        digitized from, or null if this diagram is not known to be
        digitized from a file. */
    public String getOriginalFilename() {
        return originalFilename;
    }

    /** Assert that this diagram should not be represented as the
        traced version of some other diagram image. Disable the
        zoomFrame, hide the background image in editFrame, and so
        on. */
    void dontTrace() {
        originalFilename = null;
        originalToPrincipal = null;
        originalImage = null;
        zoomFrame.setVisible(false);
        updateTitle();
        editFrame.mnBackgroundImage.setEnabled(false);
    }

    public void setTitle(String title) {
        put("title", title);
    }

    @JsonIgnore public String getTitle() {
        return get("title");
    }

    void updateTitle() {
        StringBuilder titleBuf = new StringBuilder(isEditable() ? "Edit " : "View ");

        String titleStr = getTitle();
        if (titleStr != null) {
            titleBuf.append(titleStr);
        } else {
            titleBuf.append(diagramType);

            String str = systemName();
            if (str != null) {
                titleBuf.append(" ");
                titleBuf.append(str);
            }

            str = getOriginalFilename();
            if (str == null) {
                str = getFilename();
            }
            if (str != null) {
                titleBuf.append(" ");
                titleBuf.append(str);
            }
        }

        editFrame.setTitle(titleBuf.toString());
    }

    /** @return the system name if known, with components sorted into
        alphabetical order, or null otherwise.

        This might not be an actual system name if the diagram
        components are not principal components, but whatever. */
    public String systemName() {
        Side[] sides = null;
        if (diagramType.isTernary()) {
            sides = new Side[] {Side.LEFT, Side.RIGHT, Side.TOP};
        } else {
            sides = new Side[] {Side.LEFT, Side.RIGHT};
        }

        ArrayList<String> comps = new ArrayList<String>();
        for (Side side: sides) {
            String str = getDiagramComponent(side);
            if (str == null) {
                return null;
            } else {
                comps.add(str);
            }
        }

        if (comps != null) {
            Collections.sort(comps);
        }

        StringBuilder str = null;
        for (String comp: comps) {
            if (str == null) {
                str = new StringBuilder();
            } else {
                str.append("-");
            }
            str.append(comp);
        }

        return str.toString();
    }

    public void setOriginalFilename(String filename) {
        saveNeeded = true;
        originalFilename = filename;

        if (filename == null) {
            dontTrace();
            return;
        }

        if (Files.notExists(new File(filename).toPath())) {
            JOptionPane.showMessageDialog
                (editFrame, "Warning: original file '" + filename + "' not found");
            dontTrace();
            return;
        }

        try {
            BufferedImage im = ImageIO.read(new File(filename));
            if (im == null) {
                throw new IOException(filename + ": unknown image format");
            }
            originalImage = im;
            updateTitle();
            zoomFrame.setImage(getOriginalImage());
            initializeCrosshairs();
            zoomFrame.getImageZoomPane().crosshairs = crosshairs;
            editFrame.mnBackgroundImage.setEnabled(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "Original image unavailable: '" + filename + "': " +  e.toString());
            dontTrace();
        }
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        try {
            // Work-around for a bug that affects EB's PC as of 7.0_3.
            System.setProperty("sun.java2d.d3d", "false");
            // TODO UNDO?
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            throw new Error(e);
        }
        EventQueue.invokeLater(new ArgsRunnable(args) {
                public void run() {
                    if (args.length > 1) {
                        printHelp();
                        System.exit(2);
                    }

                    try {
                        Editor app = new Editor();
                        app.run(args.length == 1 ? args[0] : null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    public void cropPerformed(CropEvent e) {
        diagramType = e.getDiagramType();
        newDiagram(e.filename, Duh.toPoint2DDoubles(e.getVertices()));
        initializeGUI();
        saveNeeded = true;
    }

    /** Start on a blank new diagram. */
    public void newDiagram() {
        if (!verifyCloseDiagram()) {
            return;
        }

        DiagramType temp = (new DiagramDialog(null)).showModal();
        if (temp == null) {
            return;
        }

        diagramType = temp;
        newDiagram(null, null);
        saveNeeded = false;
    }

    /** Start on a blank new diagram.

        @param originalFilename If not null, the filename of the
        original image the diagram is to be scanned from.

        @param vertices If not null, the endpoints of the polygonal
        region that the user selected as the diagram boundary within
        the originalFilename image .
    */
    protected void newDiagram(String originalFilename,
                              Point2D.Double[] vertices) {
        ArrayList<Point2D.Double> diagramOutline
            = new ArrayList<Point2D.Double>();
        boolean tracing = (vertices != null);
        clear();
        setOriginalFilename(originalFilename);

        // It's easier to shrink than to enlarge a diagram, so start
        // with fairly generous margins.
        double leftMargin = 0.15;
        double rightMargin = 0.15;
        double topMargin = 0.15;
        double bottomMargin = 0.15;
        double maxDiagramHeight = 1.0;
        double maxDiagramWidth = 1.0;

        Point2D.Double[] principalTrianglePoints =
            { new Point2D.Double(0.0, 0.0),
              new Point2D.Double(0.0, 1.0),
              new Point2D.Double(1.0, 0.0) };

        Rescale r = null;

        switch (diagramType) {
        case TERNARY_BOTTOM:
            {
                double height;

                double defaultHeight = !tracing ? 0.45
                    : (1.0
                       - (vertices[1].distance(vertices[2]) / 
                          vertices[0].distance(vertices[3])));
                String initialHeight = String.format
                    ("%.1f%%", defaultHeight * 100);

                while (true) {
                    String heightS = (String) JOptionPane.showInputDialog
                        (null,
                         "Enter the visual height of the diagram\n" +
                         "as a fraction of the full triangle height:",
                         initialHeight);
                    if (heightS == null) {
                        heightS = initialHeight;
                    }
                    try {
                        height = ContinuedFraction.parseDouble(heightS);
                        break;
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Invalid number format.");
                    }
                }

                // The logical coordinates of the four legs of the
                // trapezoid. These are not normal Cartesian
                // coordinates, but ternary diagram concentration
                // values. The first coordinate represents the
                // percentage of the bottom right corner substance,
                // and the second coordinate represents the percentage
                // of the top corner substance. The two coordinate
                // axes, then, are not visually perpendicular; they
                // meet to make a 60 degree angle that is the bottom
                // left corner of the triangle.

                Point2D.Double[] outputVertices =
                    { new Point2D.Double(0.0, 0.0),
                      new Point2D.Double(0.0, height),
                      new Point2D.Double(1.0 - height, height),
                      new Point2D.Double(1.0, 0.0) };

                if (tracing) {
                    originalToPrincipal = new QuadToQuad(vertices, outputVertices);
                }

                r = new Rescale(1.0, 0.0, maxDiagramWidth,
                                TriangleTransform.UNIT_TRIANGLE_HEIGHT * height,
                                0.0, maxDiagramHeight);

                double rx = r.width;
                double bottom = r.height;

                Point2D.Double[] trianglePagePositions =
                    { new Point2D.Double(0.0, bottom),
                      new Point2D.Double(rx/2,
                                         bottom - TriangleTransform.UNIT_TRIANGLE_HEIGHT * r.t),
                      new Point2D.Double(rx, bottom) };
                principalToStandardPage = new TriangleTransform
                    (principalTrianglePoints, trianglePagePositions);

                // Add the endpoints of the diagram.
                diagramOutline.add(outputVertices[1]);
                diagramOutline.add(outputVertices[0]);
                diagramOutline.add(outputVertices[3]);
                diagramOutline.add(outputVertices[2]);

                addTernaryBottomRuler(0.0, 1.0);
                addTernaryLeftRuler(0.0, height);
                addTernaryRightRuler(0.0, height);
                break;
            }
        case BINARY:
            {
                addBinaryBottomRuler();
                addBinaryTopRuler();
                addBinaryLeftRuler();
                addBinaryRightRuler();
                // Fall through...
            }
        case OTHER:
            {
                if (diagramType == DiagramType.OTHER) {
                    leftMargin = rightMargin = topMargin = bottomMargin = 0.05;
                }
                Rectangle2D.Double principalBounds
                    = new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0);
                if (tracing) {
                    // Transform the input quadrilateral into a rectangle
                    QuadToRect q = new QuadToRect();
                    q.setVertices(vertices);
                    q.setRectangle(principalBounds);
                    originalToPrincipal = q;
                }

                r = new Rescale(1.0, 0.0, maxDiagramWidth,
                                1.0, 0.0, maxDiagramHeight);

                principalToStandardPage = new RectangleTransform
                    (new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0),
                     new Rectangle2D.Double(0.0, 1.0,
                                            1.0 * r.t, -1.0 * r.t));
                if (diagramType == DiagramType.BINARY) {
                    diagramOutline.addAll
                        (Arrays.asList(principalToStandardPage.getInputVertices()));
                }

                break;

            }

        case TERNARY_LEFT:
        case TERNARY_RIGHT:
        case TERNARY_TOP:
            {
                final int LEFT_VERTEX = 0;
                final int TOP_VERTEX = 1;
                final int RIGHT_VERTEX = 2;

                final int RIGHT_SIDE = 0;
                final int BOTTOM_SIDE = 1;
                final int LEFT_SIDE = 2;
                String sideNames[] = { "Right", "Bottom", "Left" };

                int angleVertex = diagramType.getTriangleVertexNo();
                int ov1 = (angleVertex+1) % 3; // Other Vertex #1
                int ov2 = (angleVertex+2) % 3; // Other Vertex #2

                double pageMaxes[];

                if (!tracing) {
                    pageMaxes = new double[] { 1.0, 1.0 };
                } else {
                    double angleSideLengths[] =
                        { vertices[angleVertex].distance(vertices[ov2]),
                          vertices[angleVertex].distance(vertices[ov1]) };
                    double maxSideLength = Math.max(angleSideLengths[0],
                                                    angleSideLengths[1]);
                    pageMaxes = new double[]
                        { angleSideLengths[0] / maxSideLength,
                          angleSideLengths[1] / maxSideLength };
                }

                String pageMaxInitialValues[] = new String[pageMaxes.length];
                for (int i = 0; i < pageMaxes.length; ++i) {
                    pageMaxInitialValues[i] = ContinuedFraction.toString
                        (pageMaxes[i], true);
                }
                DimensionsDialog dialog = new DimensionsDialog
                    (null, new String[] { sideNames[ov1], sideNames[ov2] });
                dialog.setDimensions(pageMaxInitialValues);
                dialog.setTitle("Select Screen Side Lengths");

                while (true) {
                    String[] pageMaxStrings = dialog.showModal();
                    if (pageMaxStrings == null) {
                        pageMaxStrings = (String[]) pageMaxInitialValues.clone();
                    }
                    try {
                        for (int i = 0; i < pageMaxStrings.length; ++i) {
                            pageMaxes[i] = ContinuedFraction.parseDouble(pageMaxStrings[i]);
                        }
                        break;
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Invalid number format.");
                    }
                }

                double[] sideLengths = new double[3];
                sideLengths[ov1] = pageMaxes[0];
                sideLengths[ov2] = pageMaxes[1];

                // One of the following 3 values will be invalid and
                // equal to 0, but we only use the ones that are
                // valid.
                double leftLength = sideLengths[LEFT_SIDE];
                double rightLength = sideLengths[RIGHT_SIDE];
                double bottomLength = sideLengths[BOTTOM_SIDE];

                // trianglePoints is to be set to a set of coordinates
                // of the 3 vertices of the ternary diagram expressed
                // in principal coordinates. The first principal
                // coordinate represents the fraction of the
                // lower-right principal component, and the second
                // principal coordinate represents the fraction of
                // the top principal component.

                // At this point, principal component lengths should
                // be proportional to page distances.

                // trianglePoints[] contains the key points of the
                // actual diagram (though only one of those key points
                // will be a diagram component).
                Point2D.Double[] trianglePoints
                    = Duh.deepCopy(principalTrianglePoints);

                switch (diagramType) {
                case TERNARY_LEFT:
                    trianglePoints[TOP_VERTEX] = new Point2D.Double(0, leftLength);
                    trianglePoints[RIGHT_VERTEX]
                        = new Point2D.Double(bottomLength, 0);
                    addTernaryBottomRuler(0.0, bottomLength);
                    addTernaryLeftRuler(0.0, leftLength);
                    break;

                case TERNARY_TOP:
                    trianglePoints[LEFT_VERTEX]
                        = new Point2D.Double(0, 1.0 - leftLength);
                    trianglePoints[RIGHT_VERTEX]
                        = new Point2D.Double(rightLength, 1.0 - rightLength);
                    addTernaryLeftRuler(1 - leftLength, 1.0);
                    addTernaryRightRuler(1 - rightLength, 1.0);
                    break;

                case TERNARY_RIGHT:
                    trianglePoints[LEFT_VERTEX]
                        = new Point2D.Double(1.0 - bottomLength, 0.0);
                    trianglePoints[TOP_VERTEX]
                        = new Point2D.Double(1.0 - rightLength, rightLength);
                    addTernaryBottomRuler(1.0 - bottomLength, 1.0);
                    addTernaryRightRuler(0.0, rightLength);
                    break;
                }

                // Add the endpoints of the diagram.
                diagramOutline.add(trianglePoints[ov1]);
                diagramOutline.add(trianglePoints[angleVertex]);
                diagramOutline.add(trianglePoints[ov2]);

                if (tracing) {
                    originalToPrincipal = new TriangleTransform(vertices,
                                                                trianglePoints);
                }

                // xform is fixed -- three vertices of an equilateral
                // triangle in principal coordinates transformed to an
                // equilateral triangle in Euclidean coordinates, with
                // the y-axis facing up -- -- and ignores the
                // particulars of this diagram. However, applying
                // xform to trianglePoints yields the correct
                // proportions (though not the scale) for the limit
                // points of the actual diagram.

                TriangleTransform xform = new TriangleTransform
                    (principalTrianglePoints,
                     TriangleTransform.equilateralTriangleVertices());
                Point2D.Double[] xformed = {
                    xform.transform(trianglePoints[0]),
                    xform.transform(trianglePoints[1]),
                    xform.transform(trianglePoints[2]) };

                // Rescale the principalToStandardPage transform

                // Reverse the direction of the y-axis to point
                // downwards, and rescale to fill the available
                // space as much as possible.

                Rectangle2D.Double bounds = Duh.bounds(xformed);
                r = new Rescale
                    (bounds.width, 0.0, maxDiagramWidth,
                     bounds.height, 0.0, maxDiagramHeight);
                xform.preConcatenate
                    (AffineTransform.getScaleInstance(r.t, -r.t));

                // Translate so the top vertex has y value = topMargin
                // and the leftmost vertex (either the left or,
                // possibly but unlikely, the top) has x value =
                // leftMargin.
                Point2D.Double top
                    = xform.transform(trianglePoints[TOP_VERTEX]);
                double minX = Math.min
                    (top.x, xform.transform(trianglePoints[LEFT_VERTEX]).x);
                double minY = top.y;
                xform.preConcatenate
                    (AffineTransform.getTranslateInstance(-minX, -minY));

                // Change the input vertices to be the actual
                // corners of the triangle in principal
                // coordinates and the output vertices to be the
                // translations of those points. This won't affect
                // the transform in most ways, but it will cause
                // getInputVertices(), getOutputVertices(),
                // inputBounds(), and outputBounds() to return
                // meaningful values.

                for (int i = 0; i < 3; ++i) {
                    xformed[i] = xform.transform(trianglePoints[i]);
                }

                principalToStandardPage = new TriangleTransform
                    (trianglePoints, xformed);

                break;
            }
        case TERNARY:
            {
                if (tracing) {
                    originalToPrincipal = new TriangleTransform
                        (vertices, principalTrianglePoints);
                }

                r = new Rescale(1.0, 0.0, maxDiagramWidth,
                                TriangleTransform.UNIT_TRIANGLE_HEIGHT,
                                0.0, maxDiagramHeight);
                Point2D.Double[] trianglePagePositions =
                    { new Point2D.Double(0.0, r.height),
                      new Point2D.Double(r.width/2, 0.0),
                      new Point2D.Double(r.width, r.height) };
                principalToStandardPage = new TriangleTransform
                    (principalTrianglePoints, trianglePagePositions);
                diagramOutline.addAll
                    (Arrays.asList(principalToStandardPage.getInputVertices()));

                addTernaryBottomRuler(0.0, 1.0);
                addTernaryLeftRuler(0.0, 1.0);
                addTernaryRightRuler(0.0, 1.0);
                break;
            }
        }

        pageBounds = new Rectangle2D.Double
            (-leftMargin, -topMargin, r.width + leftMargin + rightMargin,
             r.height + topMargin + bottomMargin);

        initializeDiagram();

        // xAxis etc. don't exist until initializeDiagram() is called,
        // so we can't assign them until now.
        switch (diagramType) {
        case TERNARY:
        case TERNARY_BOTTOM:
            {
                rulers.get(0).axis = getXAxis();
                rulers.get(1).axis = getYAxis();
                rulers.get(2).axis = getYAxis();
                break;
            }
        case BINARY:
            {
                rulers.get(0).axis = getXAxis();
                rulers.get(1).axis = getXAxis();
                rulers.get(2).axis = getYAxis();
                rulers.get(3).axis = getYAxis();
                break;
            }
        case TERNARY_LEFT:
            {
                rulers.get(0).axis = getXAxis();
                rulers.get(1).axis = getYAxis();
                break;
            }
        case TERNARY_RIGHT:
            {
                rulers.get(0).axis = getXAxis();
                rulers.get(1).axis = getYAxis();
                break;
            }
        case TERNARY_TOP:
            {
                rulers.get(0).axis = getYAxis();
                rulers.get(1).axis = getYAxis();
                break;
            }
        }
    }

    protected double normalFontSize() {
        return STANDARD_FONT_SIZE / BASE_SCALE;
    }

    protected double currentFontSize() {
        return normalFontSize() * lineWidth / STANDARD_LINE_WIDTH;
    }

    LinearAxis defaultAxis(Side side) {
        NumberFormat format = new DecimalFormat("0.0000");
        if (diagramType.isTernary()) {
            LinearAxis axis;

            switch (side) {
            case RIGHT:
                axis = LinearAxis.createXAxis(format);
                axis.name = "Right";
                return axis;
            case LEFT:
                axis = new LinearAxis(format, -1.0, -1.0, 1.0);
                axis.name = "Left";
                return axis;
            case TOP:
                axis = LinearAxis.createYAxis(format);
                axis.name = "Top";
                return axis;
            default:
                return null;
            }
        } else {
            switch (side) {
            case RIGHT:
                return LinearAxis.createXAxis(format);
            case TOP:
                return LinearAxis.createYAxis(format);
            default:
                return null;
            }
        }
    }

    protected void initializeDiagram() {

        boolean isTernary = diagramType.isTernary();
        editFrame.setAspectRatio.setEnabled(!isTernary);
        editFrame.setTopComponent.setEnabled(isTernary);

        if (isTernary) {
            axes.add(defaultAxis(Side.LEFT));
            axes.add(defaultAxis(Side.RIGHT));
            axes.add(defaultAxis(Side.TOP));
        } else {
            axes.add(defaultAxis(Side.RIGHT));
            axes.add(defaultAxis(Side.TOP));
        }

        try {
            standardPageToPrincipal = principalToStandardPage.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }

        {
            NumberFormat format = new DecimalFormat("0.0000");
            pageXAxis = LinearAxis.createFromAffine
                (format, principalToStandardPage, false);
            pageXAxis.name = "page X";
            pageYAxis = LinearAxis.createFromAffine
                (format, principalToStandardPage, true);
            pageYAxis.name = "page Y";
            axes.add(pageXAxis);
            axes.add(pageYAxis);
        }

        if (getOriginalFilename() != null) {
            try {
                principalToOriginal = (PolygonTransform)
                    originalToPrincipal.createInverse();
            } catch (NoninvertibleTransformException e) {
                System.err.println("This transform is not invertible");
                System.exit(2);
            }
        }

        setOriginalFilename(getOriginalFilename());

        View em = toView("n", 0, Color.BLACK);
        labelXMargin = em.getPreferredSpan(View.X_AXIS) / 3.0;
        // labelYMargin = em.getPreferredSpan(View.Y_AXIS) / 5.0;

        zoomBy(1.0);
    }

    /** Elements of openDiagram() that apply only if the GUI is
        actually meant to be visible. */
    protected void initializeGUI() {
        // Force the editor frame image to be initialized.

        editFrame.pack();
        Rectangle rect = editFrame.getBounds();
        if (tracingImage()) {
            zoomFrame.setLocation(rect.x + rect.width, rect.y);
            zoomFrame.setTitle("Zoom " + getOriginalFilename());
            zoomFrame.pack();
            zoomFrame.setVisible(true);
            Rectangle zrect = zoomFrame.getBounds();
            Rectangle vrect = vertexInfo.getBounds();
            vertexInfo.setLocation(zrect.x, zrect.y + zrect.height - vrect.height);
        } else {
            vertexInfo.setLocation(rect.x + rect.width, rect.y);
        }
        vertexInfo.setVisible(true);
        editFrame.setVisible(true);
    }

    /** Give the user an opportunity to save the old diagram or to
        change their mind before closing a diagram.

        @return false if the user changes their mind and the diagram
        should not be closed. */
    boolean verifyCloseDiagram() {
        if (!saveNeeded) {
            return true;
        }
        
        Object[] options = new Object[]
            {"Save and continue", "Do not save", "Cancel"};

        switch (JOptionPane.showOptionDialog
                (editFrame,
                 "This file has changed. Would you like to save it?",
                 "Confirm new diagram",
                 JOptionPane.YES_NO_CANCEL_OPTION,
                 JOptionPane.QUESTION_MESSAGE,
                 null,
                 options,
                 options[0])) {
        case JOptionPane.YES_OPTION:
            save();
            return true;

        case JOptionPane.NO_OPTION:
            return true;

        default:
            return false;
        }
    }


    /** Invoked from the EditFrame menu */
    public void setAspectRatio() {
        if (diagramType == null) {
            return;
        }

        Rectangle2D.Double bounds = principalToStandardPage.outputBounds();

        double oldValue = ((double) bounds.width) / bounds.height;
        double aspectRatio;

        while (true) {
            try {
                String aspectRatioStr = JOptionPane.showInputDialog
                    (editFrame,
                     "Enter the width-to-height ratio for the core diagram.\n"
                     + "(Most diagrams in the database uses a ratio of 80%.)",
                     ContinuedFraction.toString(oldValue, true));
                if (aspectRatioStr == null) {
                    return;
                }

                aspectRatio = ContinuedFraction.parseDouble(aspectRatioStr);
                if (aspectRatio <= 0) {
                    JOptionPane.showMessageDialog
                        (editFrame, "Enter a positive number.");
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog
                    (editFrame, "Invalid number format.");
            }
        }

        saveNeeded = true;
        double stretchFactor = aspectRatio / oldValue;
        ((RectangleTransform) principalToStandardPage).scaleOutput
            (stretchFactor, 1.0);
        try {
            standardPageToPrincipal = principalToStandardPage.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }

        pageBounds.x *= stretchFactor;
        pageBounds.width *= stretchFactor;
        getEditPane().setPreferredSize(scaledPageBounds(scale).getSize());
        getEditPane().revalidate();
        scaledOriginalImages = null;


        repaintEditFrame();
    }


    /** Invoked from the EditFrame menu */
    public void setMargin(Side side) {
        if (diagramType == null) {
            return;
        }

        Rectangle2D.Double bounds = principalToStandardPage.outputBounds();

        String standard;
        switch (diagramType) {
        case BINARY:
        case OTHER:
            standard = "core diagram height";
            break;
        case TERNARY:
            standard = "triangle side length";
            break;
        default:
            standard = "core diagram "
                + ((bounds.width >= bounds.height) ? "width" : "height");
            break;
        }

        double oldValue = 0.0;
        switch (side) {
        case LEFT:
            oldValue = -pageBounds.x;
            break;
        case RIGHT:
            oldValue = pageBounds.getMaxX() - bounds.getMaxX();
            break;
        case TOP:
            oldValue = -pageBounds.y;
            break;
        case BOTTOM:
            oldValue = pageBounds.getMaxY() - bounds.getMaxY();
            break;
        }

        String oldString = ContinuedFraction.toString(oldValue, true);
        double margin;

        while (true) {
            try {
                String marginStr = JOptionPane.showInputDialog
                    (editFrame,
                     "Margin size as a fraction of the " + standard + ":",
                     oldString);
                if (marginStr == null) {
                    return;
                }

                margin = ContinuedFraction.parseDouble(marginStr);
                break;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid number format.");
            }
        }

        saveNeeded = true;
        double delta = margin - oldValue;

        switch (side) {
        case LEFT:
            pageBounds.x = -margin;
            // Fall through
        case RIGHT:
            pageBounds.width += delta;
            break;
        case TOP:
            pageBounds.y = -margin;
            // Fall through
        case BOTTOM:
            pageBounds.height += delta;
            break;
        }

        getEditPane().setPreferredSize(scaledPageBounds(scale).getSize());
        getEditPane().revalidate();
        scaledOriginalImages = null;
        repaintEditFrame();
    }

    /** Invoked from the EditFrame menu */
    public void openDiagram() {
        if (!verifyCloseDiagram()) {
            return;
        }

        File file = showOpenDialog("ped");
        if (file == null) {
            return;
        }

        openDiagram(file);
        initializeGUI();
    }

    /** Invoked from the EditFrame menu */
    public void openDiagram(File file) {
        Editor ned;

        try {
            ObjectMapper mapper = getObjectMapper();
            ned = (Editor) mapper.readValue(file, getClass());
        } catch (Exception e) {
            JOptionPane.showMessageDialog
                (editFrame, "File load error: " + e);
            return;
        }

        clear();
        cannibalize(ned);
        filename = file.getAbsolutePath();
        for (TieLine tie: tieLines) {
            tie.innerEdge = idToCurve(tie.innerId);
            tie.outerEdge = idToCurve(tie.outerId);
        }
        updateTitle();
        if (pageBounds == null) {
            computeMargins();
        }
        saveNeeded = false;
    }


    /** Invoked from the EditFrame menu */
    public void reloadDiagram() {
        if (!verifyCloseDiagram()) {
            return;
        }

        String filename = getFilename();
        if (filename == null) {
            return;
        }

        openDiagram(new File(filename));
    }


    public Rectangle2D.Double getPageBounds() {
        if (pageBounds == null) {
            return null;
        }
        return (Rectangle2D.Double) pageBounds.clone();
    }


    public void setPageBounds(Rectangle2D rect) {
        saveNeeded = true;
        pageBounds = Duh.createRectangle2DDouble(rect);
        getEditPane().setPreferredSize(scaledPageBounds(scale).getSize());
        getEditPane().revalidate();
        repaintEditFrame();
    }


    public void computeMargins() {
        if (pageBounds == null) {
            setPageBounds(new Rectangle2D.Double(0, 0, 1, 1));
        }
        MeteredGraphics mg = new MeteredGraphics();
        double mscale = 10000;
        paintDiagram(mg, mscale, false, false);
        Rectangle2D.Double bounds = mg.getBounds();
        if (bounds == null) {
            return;
        }
        bounds.x /= mscale;
        bounds.y /= mscale;
        bounds.width /= mscale;
        bounds.height /= mscale;
        bounds.x += pageBounds.x;
        bounds.y += pageBounds.y;
        double margin = (bounds.width + bounds.height) / 200;
        bounds.width += margin * 2;
        bounds.height += margin * 2;
        bounds.x -= margin;
        bounds.y -= margin;
        setPageBounds(bounds);
    }


    /** Copy data fields from other. Afterwards, it is unsafe to
        modify other, because the modifications may affect this as
        well. In other words, this is a shallow copy that destroys
        other. */
    void cannibalize(Editor other) {
        diagramType = other.diagramType;
        diagramComponents = other.diagramComponents;
        originalToPrincipal = other.originalToPrincipal;
        principalToStandardPage = other.principalToStandardPage;
        pageBounds = other.pageBounds;
        originalFilename = other.originalFilename;
        scale = other.scale;
        arrows = other.arrows;

        boolean haveBounds = (pageBounds != null);
        if (!haveBounds) {
            pageBounds = new Rectangle2D.Double(0,0,1,1);
        }
        initializeDiagram();
        paths = other.paths;
        tieLines = other.tieLines;
        setFontName(other.getFontName());
        axes = other.axes;
        editFrame.removeAllVariables();
        for (LinearAxis axis: axes) {
            if (!isStandardAxis(axis)) {
                editFrame.addVariable((String) axis.name);
            }
        }
        rulers = other.rulers;
        labels = other.labels;
        initializeLabelViews();
        selection = null;
        componentElements = null;
        setTags(other.getTags());
        setKeyValues(other.getKeyValues());
        if (!haveBounds) {
            pageBounds = null;
        }
    }

    /** Populate the "rulers" fields of the axes, and then return the
        axes. */
    @JsonProperty("axes") ArrayList<LinearAxis>
    getSerializationReadyAxes() {
        for (LinearAxis axis: axes) {
            axis.rulers = new ArrayList<LinearRuler>();
        }

        // Make each ruler a child of its respective axis.
        for (LinearRuler r: rulers) {
            r.axis.rulers.add(r);
        }

        return axes;
    }

    /** Populate the Editor object's "rulers" field from the
        individual axes' "rulers" fields, and set the individual axes'
        "rulers" fields to null. */
    @JsonProperty("axes") void
    setAxesFromSerialization(ArrayList<LinearAxis> axes) {
        rulers = new ArrayList<LinearRuler>();

        for (LinearAxis axis: axes) {
            addVariable(axis);
            rulers.addAll(axis.rulers);
            axis.rulers = null;
        }
    }

    @JsonProperty("tieLines")
    void setTieLines(ArrayList<TieLine> tieLines) {
        this.tieLines = tieLines;
    }

    @JsonIgnore public BufferedImage getOriginalImage() {
        return originalImage;
    }

    /** Invoked from the EditFrame menu */
    public void openImage(String filename) {
        String title = (filename == null) ? "PED Editor" : filename;
        editFrame.setTitle(title);

        if (filename == null) {
            cropFrame.showOpenDialog();
        } else {
            try {
                cropFrame.setFilename(filename);
            } catch (IOException e) {
                JOptionPane.showMessageDialog
                    (editFrame, "Could not load file '" + filename + "': " + e);
            }
        }

        cropFrame.pack();
        cropFrame.setVisible(true);
    }

    /** @return a File if the user selected one, or null otherwise.

        @param ext the extension to use with this file ("pdf" for
        example). */
    public File showSaveDialog(String ext) {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String dir = prefs.get(PREF_DIR,  null);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as " + ext.toUpperCase());
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
        chooser.setFileFilter
            (new FileNameExtensionFilter(ext.toUpperCase(), ext));
        if (chooser.showSaveDialog(editFrame) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();
        if (getExtension(file) == null) {
            // Add the default extension
            file = new File(file.getAbsolutePath() + "." + ext);
        }

        prefs.put(PREF_DIR, file.getParent());
        return file;
    }

    /** @return a File if the user selected one, or null otherwise.

        @param ext the extension to use with this file ("pdf" for
        example). */
    public File showOpenDialog(String ext) {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String dir = prefs.get(PREF_DIR,  null);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open " + ext.toUpperCase() + " file");
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
        chooser.setFileFilter
            (new FileNameExtensionFilter(ext.toUpperCase(), ext));
        if (chooser.showOpenDialog(editFrame) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();
        if (getExtension(file) == null) {
            // Add the default extension
            file = new File(file.getAbsolutePath() + "." + ext);
        }

        prefs.put(PREF_DIR, file.getParent());
        return file;
    }

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

    /** Invoked from the EditFrame menu */
    public void saveAsPDF() {
        File file = showSaveDialog("pdf");
        if (file == null) {
            return;
        }

        saveAsPDF(file);
    }

    public void saveAsPDF(File file) {
        Document doc = new Document(PageSize.LETTER);
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
        } catch (Exception e) {
            System.err.println(e);
            return;
        }

        doc.open();

        Rectangle2D.Double bounds = new Rectangle2D.Double
            (doc.left(), doc.bottom(), doc.right() - doc.left(), doc.top() - doc.bottom());

        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp = cb.createTemplate((float) bounds.width, (float) bounds.height);
        
        Graphics2D g2 = false
            ? tp.createGraphics((float) bounds.width, (float) bounds.height,
                                new DefaultFontMapper())
            : tp.createGraphicsShapes((float) bounds.width, (float) bounds.height);

        g2.setFont(getFont());
        paintDiagram(g2, deviceScale(g2, bounds));
        g2.dispose();
        cb.addTemplate(tp, doc.left(), doc.bottom());
        doc.close();
    }

    /** Invoked from the EditFrame menu */
    public void saveAsSVG() {
        File file = showSaveDialog("svg");
        if (file == null) {
            return;
        }

        DOMImplementation imp = GenericDOMImplementation.getDOMImplementation();
        org.w3c.dom.Document doc = imp.createDocument("http://www.w3.org/2000/svg", "svg", null);
        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(doc);
        ctx.setComment("PED Document exported using Batik SVG Generator");
        ctx.setEmbeddedFontsOn(true);
        SVGGraphics2D svgGen = new SVGGraphics2D(ctx, true);
        paintDiagram(svgGen, BASE_SCALE);
        try {
            Writer wr = new FileWriter(file);
            svgGen.stream(wr);
            wr.close();
            JOptionPane.showMessageDialog
                (editFrame, "File saved.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(editFrame, "File save error: " + e);
        }
    }

    /** Invoked from the EditFrame menu */
    public void saveAsPED(String fn) {
        File file;
        if (fn == null) {
            file = showSaveDialog("ped");
            if (file == null) {
                return;
            }
            filename = file.getAbsolutePath();
        } else {
            file = new File(fn);
        }

        try {
            getObjectMapper().writeValue(file, this);
            saveNeeded = false;
            JOptionPane.showMessageDialog(editFrame, "File saved.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog
                (editFrame, "File save error: " + e);
        }
    }

    /** Invoked from the EditFrame menu */
    public void save() {
        saveAsPED(getFilename());
    }

    /** Invoked from the EditFrame menu */
    public void print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        if (job.printDialog()) {
            try {
                PrintRequestAttributeSet aset
                    = new HashPrintRequestAttributeSet();
                aset.add
                    ((pageBounds.width > pageBounds.height)
                     ? OrientationRequested.LANDSCAPE
                     : OrientationRequested.PORTRAIT);
                job.print(aset);
                JOptionPane.showMessageDialog
                    (editFrame, "Print job submitted.");
            } catch (PrinterException e) {
                System.err.println(e);
            }
        } else {
            JOptionPane.showMessageDialog(editFrame, "Print job canceled.");
        }
    }

    public int print(Graphics g0, PageFormat pf, int pageIndex)
         throws PrinterException {
        if (pageIndex != 0 || principalToStandardPage == null) {
            return Printable.NO_SUCH_PAGE;
        }
        Graphics2D g = (Graphics2D) g0;
        g.setFont(getFont());

        AffineTransform oldTransform = g.getTransform();

        Rectangle2D.Double bounds
            = new Rectangle2D.Double
            (pf.getImageableX(), pf.getImageableY(),
             pf.getImageableWidth(), pf.getImageableHeight());

        g.translate(bounds.getX(), bounds.getY());
        double scale = Math.min(bounds.height / pageBounds.height,
                                bounds.width / pageBounds.width);
        paintDiagram(g, scale);
        g.setTransform(oldTransform);

        return Printable.PAGE_EXISTS;
    }

    /** @param segment A line on the standard page

        Return a grid line (also on the standard page) that passes
        through segment.getP1() and that is roughly parallel to
        segment, or null if no such line is close enough to parallel.
        A grid line is a line of zero change for a defined axis (from
        the "axes" variable). */
    Line2D.Double nearestGridLine(Line2D.Double segment) {
        Point2D source = segment.getP1();
        Point2D dest = segment.getP2();
        Point2D.Double pageDelta = Duh.aMinusB(dest, source);
        double deltaLength = Duh.length(pageDelta);

        // Tolerance is the maximum ratio of the distance between dest
        // and the projection to deltaLength. TODO A smarter approach
        // might allow for both absolute and relative errors.
        double tolerance = 0.06;

        double maxDist = deltaLength * tolerance;
        double maxDistSq = maxDist * maxDist;

        ArrayList<Point2D.Double> vectors = new ArrayList<>();
        for (LinearAxis axis: axes) {
            // Add the line of no change for this axis. The line
            // of no change is perpendicular to the gradient.
            Point2D.Double g = axis.gradient();
            vectors.add(new Point2D.Double(g.y, -g.x));
        }

        if (vertexInfo != null) {
            // Add the line at the angle given in vertexInfo.
            double theta = vertexInfo.getAngle();
            Point2D.Double p = new Point2D.Double(Math.cos(theta), Math.sin(theta));
            standardPageToPrincipal.deltaTransform(p, p);
            vectors.add(p);
        }

        Line2D.Double res = null;
        
        for (Point2D.Double v: vectors) {
            principalToStandardPage.deltaTransform(v, v);
            Point2D.Double projection = Duh.nearestPointOnLine
                (pageDelta, new Point2D.Double(0,0), v);
            double distSq = pageDelta.distanceSq(projection);
            if (distSq < maxDistSq) {
                projection.x += source.getX();
                projection.y += source.getY();
                maxDistSq = distSq;
                res = new Line2D.Double(source, projection);
            }
        }

        if (res == null) {
            return null;
        }

        // Extend res to the limits of pageBounds.
        Rectangle2D b = pageBounds;

        Point2D.Double[] vertexes =
            { new Point2D.Double(b.getMinX(), b.getMinY()),
              new Point2D.Double(b.getMaxX(), b.getMinY()),
              new Point2D.Double(b.getMaxX(), b.getMaxY()),
              new Point2D.Double(b.getMinX(), b.getMaxY()) };
        double minT = Double.NaN;
        double maxT = Double.NaN;

        for (int i = 0; i < 4; ++i) {
            double t = Duh.lineSegmentIntersectionT
                (res.getP1(), res.getP2(),
                 vertexes[i], vertexes[(i+1) % 4]);
            if (Double.isNaN(t)) {
                continue;
            }
            if (Double.isNaN(minT)) {
                minT = t;
                maxT = t;
            } else {
                minT = Math.min(minT, t);
                maxT = Math.max(maxT, t);
            }
        }

        if (Double.isNaN(minT)) {
            // Apparently the line of the segment doesn't even
            // intersect the page. Weird.
            return null;
        }

        double x1 = res.getX1();
        double y1 = res.getY1();
        double dx = res.getX2() - x1;
        double dy = res.getY2() - y1;

        // Midpoints of segments are key points, but the midpoint of
        // the grid line is not really interesting. Double the
        // difference of minT and maxT to put the grid line's midpoint
        // on the edge of the diagram.

        double pastMax = maxT + (maxT - minT);

        return new Line2D.Double
            (x1 + minT * dx, y1 + minT * dy,
             x1 + pastMax * dx, y1 + pastMax * dy);
    }

    public DecorationHandle secondarySelection() {
        VertexHandle vh = getVertexHandle();
        if (vh != null) {
            GeneralPolyline path = vh.getItem();
            int size = path.size();
            if (size < 2) {
                return null;
            }
            int vertexNo = vh.vertexNo + (insertBeforeSelection ? -1 : 1);
            if (path.isClosed()) {
                vertexNo = (vertexNo + size) % size;
            } else if (vertexNo < 0) {
                vertexNo = 1;
            } else if (vertexNo >= size) {
                vertexNo = size - 2;
            }
            return new VertexHandle(vh.decoration, vertexNo);
        }
        RulerHandle rh = getRulerHandle();
        if (rh != null) {
            return new RulerHandle
                (rh.decoration,
                 (rh.handle == RulerHandleType.START) ? RulerHandleType.END
                 : RulerHandleType.START);
        }
        return null;
    }
    
    public Point2D.Double secondarySelectionLocation() {
        DecorationHandle h = secondarySelection();
        return (h == null) ? null : h.getLocation();
    }

    static enum AutoPositionType { NONE, CURVE, POINT };
    static class AutoPositionHolder {
        AutoPositionType position = AutoPositionType.NONE;
    }

    /** Return the point in principal coordinates that
        auto-positioning would move the mouse to. */
    @JsonIgnore public Point2D.Double getAutoPosition() {
        return getAutoPosition(null);
    }

    /** Return the point in principal coordinates that
        auto-positioning would move the mouse to.

        @param ap If not null, ap.position will be set to
        AutoPositionType.NONE, AutoPositionType.CURVE, or
        AutoPositionType.POINT to reflect whether the autoposition is
        the regular mouse position, the nearest curve, or the nearest
        key point.
    */
    @JsonIgnore public Point2D.Double getAutoPosition(AutoPositionHolder ap) {
        if (ap == null) {
            ap = new AutoPositionHolder();
        }
        ap.position = AutoPositionType.NONE;

        Point2D.Double mprin2 = getMousePosition();
        if (mprin2 == null) {
            return mprin;
        }
        Point2D.Double mousePage = principalToStandardPage.transform(mprin2);

        // Location to move to. If null, no good candidate has been found yet.
        Point2D.Double newPage = null;

        boolean oldSaveNeeded = saveNeeded;
        ArrayList<Point2D.Double> selections = new ArrayList<>();
        int oldSize = paths.size();

        if (selection != null) {
            selections.add(selection.getLocation());
        }
        Point2D.Double point2 = secondarySelectionLocation();
        if (point2 != null) {
            selections.add(point2);
        }

        try {
        for (Point2D.Double p: selections) {
            principalToStandardPage.transform(p, p);
            Line2D.Double gridLine = nearestGridLine
                (new Line2D.Double(p, mousePage));
            if (gridLine == null) {
                continue;
            }
            gridLine = Duh.transform(standardPageToPrincipal, gridLine);
            paths.add(new Polyline(new Point2D[] { gridLine.getP1(), gridLine.getP2() },
                                   StandardStroke.INVISIBLE, 0));
        }

        {
            Point2D.Double point = nearestPoint(mousePage);
            double keyPointDist = 1e6;

            if (point != null) {
                newPage = principalToStandardPage.transform(point);

                // Subtract keyPointPixelDist (converted to page
                // coordinates) from keyPointDist before comparing
                // with curves, in order to express the preference for
                // key points over curves when the mouse is close to
                // both.
                double keyPointPixelDist = 10;
                keyPointDist = newPage.distance(mousePage)
                    - keyPointPixelDist / scale;
                ap.position = AutoPositionType.POINT;
            }

            // Only jump to the nearest curve if it is at least three
            // times closer than the nearest key point.
            DecorationDistance nc;
            if (keyPointDist > 0
                && (nc = nearestCurve(mousePage)) != null
                && keyPointDist > 3 * nc.distance.distance) {
                ap.position = AutoPositionType.CURVE;
                newPage = nc.distance.point;
            }
        }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        double maxMovePixels = 50; // Maximum number of pixels to
                                        // move the mouse
        if (newPage == null
            || newPage.distance(mousePage) * scale > maxMovePixels) {
            ap.position = AutoPositionType.NONE;
            newPage = mousePage; // Leave the mouse where it is.
        }

        while (paths.size() > oldSize) {
            paths.remove(paths.size() - 1);
        }
        saveNeeded = oldSaveNeeded;

        return standardPageToPrincipal.transform(newPage);
    }

    /** Invoked from the EditFrame menu */
    public void autoPosition() {
        if (mouseIsStuck) {
            unstickMouse();
        }

        mprin = getAutoPosition();
        if (mprin != null) {
            moveMouse(mprin);
            mouseIsStuck = true;
            repaintEditFrame();
        }
    }

    /** Invoked from the EditFrame menu */
    public void enterPosition() {
        String[] labels = new String[axes.size()];
        String[] oldValues = new String[axes.size()];
        int i = -1;
        LinearAxis xAxis = getXAxis();
        LinearAxis yAxis = getYAxis();

        // Fill in default values.
        for (Axis axis: axes) {
            ++i;
            labels[i] = (String) axis.name;
            oldValues[i] = null;
            if ((axis == xAxis || axis == yAxis) && mprin != null) {
                oldValues[i] = axis.valueAsString(mprin.x, mprin.y);
            }
        }
        StringArrayDialog dog = new StringArrayDialog
            (editFrame, labels, oldValues,
             "<html><body width=\"200 px\"><p>"
             + "Enter exactly two values. Fractions and percentages are "
             + "allowed."
             + "</p></body></html>");
        dog.setTitle("Set mouse position");
        String[] values = dog.showModal();
        if (values == null) {
            return;
        }

        ArrayList<LinearAxis> axs = new ArrayList<LinearAxis>();
        ArrayList<Double> vs = new ArrayList<Double>();

        try {
            i = -1;
            for (String str: values) {
                ++i;
                if (str == null) {
                    continue;
                }
                str = str.trim();
                if (str.equals("")) {
                    continue;
                }
                LinearAxis axis = axes.get(i);
                if (str.equals(oldValues[i])) {
                    // Never mind the sig figs in the displayed value;
                    // assume the user meant the value to remain
                    // exactly as it was, down to the last bit. (This
                    // assumption, of course, may be wrong, but it's
                    // more likely to be right, and if it is right, it
                    // may prevent a situation where a vertex that was
                    // supposed to be right on a line ends up being
                    // not quite there.)
                    vs.add(axis.value(mprin));
                } else {
                    vs.add(ContinuedFraction.parseDouble(str));
                }
                axs.add(axis);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(editFrame, e.getMessage());
            return;
        }

        if (vs.size() != 2) {
            JOptionPane.showMessageDialog
                (editFrame, "You did not enter exactly two values.");
            return;
        }

        // Solve the linear system to determine the pair of principal
        // coordinates that corresponds to this pair of
        // whatever-coordinates.
        LinearAxis ax0 = axs.get(0);
        LinearAxis ax1 = axs.get(1);
        Affine xform = new Affine
            (ax0.getA(), ax1.getA(),
             ax0.getB(), ax1.getB(),
             ax0.getC(), ax1.getC());

        try {
            Affine xformi = xform.createInverse();
            Point2D.Double newMprin = xformi.transform(vs.get(0), vs.get(1));
            Point2D.Double newMousePage = principalToStandardPage
                .transform(newMprin);
            if (Duh.distance(newMousePage, pageBounds) > 1e-6) {
                JOptionPane.showMessageDialog
                    (editFrame,
                     "<html><body width = \"300 px\""
                     + "<p>The coordinates you selected lie beyond the edge "
                     + "of the page. "
                     + "Maybe you left out a % sign, or maybe you need "
                     + "to adjust the margins using the "
                     + "<code><u>L</u>ayout/<u>M</u>argins</code> "
                     + "menu selections.</p>"
                     + "</body></html>");
                return;
            }

            mprin = newMprin;
            moveMouse(newMprin);
            mouseIsStuck = true;
        } catch (NoninvertibleTransformException e) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "The two variables you entered cannot be\n"
                 + "combined to identify a position.");
            return;
        }
    }


    /** Apply the given transform to all curve vertices, all label
        locations, all arrow locations, and all ruler start- and
        endpoints. */
    public void transformPrincipalCoordinates(AffineTransform trans) {
        saveNeeded = true;
        for (GeneralPolyline path: paths) {
            Point2D.Double[] points = path.getPoints();
            trans.transform(points, 0, points, 0, points.length);
            path.setPoints(Arrays.asList(points));
        }

        Point2D.Double tmp = new Point2D.Double();

        for (AnchoredLabel label: labels) {
            tmp.x = label.x;
            tmp.y = label.y;
            trans.transform(tmp, tmp);
            label.x = tmp.x;
            label.y = tmp.y;
        }

        for (Arrow arrow: arrows) {
            tmp.x = arrow.x;
            tmp.y = arrow.y;
            trans.transform(tmp, tmp);
            arrow.x = tmp.x;
            arrow.y = tmp.y;
        }

        for (LinearRuler ruler: rulers) {
            trans.transform(ruler.startPoint, ruler.startPoint);
            trans.transform(ruler.endPoint, ruler.endPoint);
        }
    }

    /** Apply the given transform to all coordinates defined in
        principal coordinates, but apply corresponding and inverse
        transformations to all transforms to and from principal
        coordinates, with one exception: leave the x-, y-, and z-axes
        alone. So the diagram looks the same as before except for (1)
        principal component axis ticks and (2) principal coordinate
        values as indicated in the status bar. For example, one might
        use this method to convert a binary diagram's y-axis from one
        temperature scale to another, or from the default range 0-1 to
        the range you really want. */
    public void invisiblyTransformPrincipalCoordinates(AffineTransform trans) {
        transformPrincipalCoordinates(trans);

        Affine atrans = new Affine(trans);
        Affine itrans;
        try {
            itrans = atrans.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException("Transform " + trans
                                            + " is not invertible");
        }

        if (originalToPrincipal != null) {
            originalToPrincipal.preConcatenate(atrans);
            principalToOriginal.concatenate(itrans);
        }

        // Convert all angles from principal to page coordinates.
        for (Arrow item: arrows) {
            item.setAngle(principalToPageAngle(item.getAngle()));
        }
        for (AnchoredLabel item: labels) {
            item.setAngle(principalToPageAngle(item.getAngle()));
        }

        principalToStandardPage.concatenate(itrans);
        standardPageToPrincipal.preConcatenate(atrans);

        // Convert all angles from page to the new principal coordinates.
        for (LinearAxis axis: axes) {
            if (axis.isXAxis() || axis.isYAxis() || isZAxis(axis)) {
                continue;
            }
            axis.concatenate(itrans);
        }

        for (Arrow item: arrows) {
            item.setAngle(pageToPrincipalAngle(item.getAngle()));
        }
        for (AnchoredLabel item: labels) {
            item.setAngle(pageToPrincipalAngle(item.getAngle()));
        }

    }

    @Override
        public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void unstickMouse() {
        mouseIsStuck = false;
        principalFocus = null;
        updateMousePosition();
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.isShiftDown()) {
            autoPosition();
        }
        addVertex();
    }

    public void addVertex() {
        if (principalToStandardPage == null) {
            return;
        }
        if (mouseIsStuckAtSelection() && getVertexHandle() != null) {
            unstickMouse();
        }
        add(mprin);
        mouseIsStuck = true;
    }

    /** @return true if diagram editing is enabled, or false if the
        diagram is read-only. */
    @JsonIgnore
    public boolean isEditable() {
        return true;
    }

    /** @return true if the diagram is currently being traced from
        another image. */
    boolean tracingImage() {
        return originalImage != null;
    }

    /** The mouse was moved in the edit window. Update the coordinates
        in the edit window status bar, repaint the diagram, and update
        the position in the zoom window,. */
    @Override public void mouseMoved(MouseEvent e) {
        isShiftDown = e.isShiftDown();
        repaintEditFrame();
    }

    /** Update mprin to reflect the mouse's current position unless
        mouseIsStuck is true and mprin is less than
        MOUSE_UNSTICK_DISTANCE away from the mouse position. That
        restriction insures that a slight hand twitch will not cause
        the user to lose their place after an operation such as
        seekNearestPoint(). */
    public void updateMousePosition() {
        if (principalToStandardPage == null) {
            return;
        }

        Point mpos = getEditPane().getMousePosition();
        if (mpos != null && !preserveMprin) {
            if (!mouseIsStuck || mprin == null
                || (principalToScaledPage(scale).transform(mprin)
                    .distance(mpos) >= MOUSE_UNSTICK_DISTANCE)) {
                mouseIsStuck = false;
                principalFocus = null;
                mprin = getMousePosition();
            }
        }

        updateStatusBar();
    }

    /** Return the actual mouse position in principal coordinates. For
        most purposes, you should use mprin instead of this function.
        If mouseIsStuck is true, then getMousePosition() will be
        different from mprin, but mprin (the location that the mouse
        is notionally stuck at, as opposed to the mouse pointer's true
        location) is more useful. */
    Point2D.Double getMousePosition() {
        if (principalToStandardPage == null) {
            return null;
        }

        Point mpos = getEditPane().getMousePosition();
        if (mpos == null) {
            return null;
        }

        // If we treat the mouse as being located precisely at the
        // midpoint of the pixel, then the mouse position will
        // typically end up equaling an exact fractional value when
        // expressed in principal coordinates, and the coordinates
        // display might misrepresent what was basically a mouse click
        // at an inexact location as an exact value such as "5/8". To
        // prevent this, treat the mouse as being located at a very
        // non-round number position that is only roughly in the
        // middle of the pixel. That makes it very likely that if a
        // position is displayed as an exact fraction such as "5/8",
        // it really means that the value was explicitly entered to be
        // at that exact position.
        final double ABOUT_HALF = 0.48917592383497298103;

        double sx = mpos.getX() + ABOUT_HALF;
        double sy = mpos.getY() + ABOUT_HALF;

        return scaledPageToPrincipal(scale).transform(sx,sy);
    }

    ContinuedFraction approximateFraction(double d) {
        // Digitization isn't precise enough to justify guessing
        // ratios. Only show fractions that are almost exact.
        ContinuedFraction f = ContinuedFraction.create(d, 0.000001, 0, 90);

        if (f != null) {
            return f;
        }

        return f;
    }

    /** If the diagram has a full set of diagram components defined as
        compounds with integer subscripts, and point "prin" nearly
        equals a round fraction, then return the compound that "prin"
        represents. */
    public String molePercentToCompound(Point2D.Double prin) {
        double[][] componentElements = getComponentElements();
        if (componentElements == null) {
            return null;
        }

        String[] diagramElements = getDiagramElements();

        SideDouble[] sds = componentFractions(prin);
        if (sds == null || sds.length == 0) {
       		return null;
        }
        for (SideDouble sd: sds) {
            if (componentElements[sd.s.ordinal()] == null) {
                // Can't do it without a complete set of diagram
                // components that can be parsed to compounds.
                return null;
            }
        }

        int eltCnt = diagramElements.length;
        double[] quantities = new double[eltCnt];
        for (SideDouble sd: sds) {
            // Vector of element quantities for this component
            double[] compel = componentElements[sd.s.ordinal()];
            // Quantity of this component
            double d = sd.d;
            // If d is slightly out of bounds, move it in-bounds. If d
            // is very small, then reduce it to zero.

            if (d > 1 - 1e-4) {
                if (d > 1 + 1e-4) {
                    return null;
                } else {
                    d = 1;
                }
            } else if (d < 1e-4) {
                if (d < -1e-4) {
                    return null;
                } else {
                    d = 0;
                }
            }

            for (int i = 0; i < eltCnt; ++i) {
                quantities[i] += d * compel[i];
            }
        }

        ArrayList<ContinuedFraction> fracs = new ArrayList<>(eltCnt);

        long lcd = 1;
        for (double d: quantities) {
            ContinuedFraction f = approximateFraction(d);
            if (f == null) {
                return null;
            }
            fracs.add(f);
            try {
                lcd = ContinuedFraction.lcm(lcd, f.denominator);
            } catch (OverflowException e) {
                return null;
            }
        }

        StringBuilder res = new StringBuilder();
        for (int i = 0; i < eltCnt; ++i) {
            ContinuedFraction f = fracs.get(i);
            long num = f.numerator * (lcd / f.denominator);
            if (num == 0) {
                continue;
            }
            res.append(diagramElements[i]);
            if (num > 1) {
                res.append(num);
            }
        }
        return res.toString();
    }

    void updateStatusBar() {
        if (mprin == null) {
            return;
        }

        StringBuilder status = new StringBuilder("");

        String compound = molePercentToCompound(mprin);
        if (compound != null) {
            status.append(ChemicalString.autoSubscript(compound) + ": ");
        }

        boolean first = true;
        for (LinearAxis axis : axes) {
            if (first) {
                first = false;
            } else {
                status.append(",  ");
            }
            status.append(ChemicalString.autoSubscript(axis.name.toString()));
            status.append(" = ");
            status.append(axis.valueAsString(mprin.x, mprin.y));

            if (axisIsFractional(axis)) {
                // Express values in fractional terms if the decimal
                // value is a close approximation to a fraction.
                double d = axis.value(mprin.x, mprin.y);
                ContinuedFraction f = approximateFraction(d);
                if (f != null && f.numerator != 0 && f.denominator > 1) {
                    status.append(" (" + f + ")");
                }
            }
        }
        editFrame.setStatus(status.toString());
            
        if (tracingImage() && mprin != null) {
            try {
                // Update image zoom frame.

                Point2D.Double orig = principalToOriginal.transform(mprin);
                zoomFrame.setImageFocus((int) Math.floor(orig.x),
                                        (int) Math.floor(orig.y));
            } catch (UnsolvableException ex) {
                // Ignore the exception
            }
        }
    }

    /** Set the default line width for lines added in the future.
        Values are relative to standard page units, which typically
        means 0.001 represents one-thousandth of the longer of the two
        page dimensions. */
    void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        if (selection != null) {
            selection.getDecoration().setLineWidth(lineWidth);
        }
    }

    /** Equivalent to setScale(getScale() * factor). */
    void zoomBy(double factor) {
        setScale(getScale() * factor);
    }

    /** Adjust the scale so that the page just fits in the edit frame. */
    void bestFit() {
        if (pageBounds == null) {
            return;
        }

        JScrollPane spane = editFrame.getScrollPane();
        Dimension bounds = spane.getSize(null);
        bounds.width -= 3;
        bounds.height -= 3;
        Rescale r = new Rescale(pageBounds.width, 0, (double) bounds.width,
                                pageBounds.height, 0, (double) bounds.height);
        setScale(r.t);
    }

    /** @return the screen scale of this image relative to a standard
        page (in which the longer of the two axes has length 1). */
    double getScale() { return scale; }

    /** Set the screen display scale; max(width, height) of the
        displayed image will be "value" pixels, assuming that the
        default transform for the screen uses 1 unit = 1 pixel. */
    void setScale(double value) {
        double oldScale = scale;
        scale = value;
        if (principalToStandardPage == null) {
            return;
        }

        // Keep the center of the viewport where it was if the center
        // was a part of the image.

        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();

        // Adjust the viewport to allow prin in principal coordinates
        // to remain located at offset viewportPoint from the upper
        // left corner of the viewport, to prevent the part of the
        // diagram that is visible from changing too drastically when
        // you zoom in and out. The viewport's preferred size also
        // sets constraints on the visible region, so the diagram may
        // not actually stay in the same place.

        Point2D.Double prin;
        Point viewportPoint = null;

        if (mprin != null) {
            prin = mprin;
            Point2D.Double mousePage = principalToScaledPage(oldScale)
                .transform(mprin);
            viewportPoint =
                new Point((int) Math.floor(mousePage.x * oldScale) - view.x,
                          (int) Math.floor(mousePage.y * oldScale) - view.y);
        } else {
            // Preserve the center of the viewport.
            viewportPoint = new Point(view.x + view.width / 2,
                                      view.y + view.height / 2);
            prin = scaledPageToPrincipal(oldScale).transform(viewportPoint);
        }

        getEditPane().setPreferredSize(scaledPageBounds(scale).getSize());
        getEditPane().revalidate();
        setViewportRelation(prin, viewportPoint);
    }

    double getLineWidth() {
        GeneralPolyline path = getActiveCurve();
        if (path != null) {
            return path.getLineWidth();
        }
        return lineWidth;
    }

    void customLineWidth() {
        while (true) {
            String str = (String) JOptionPane.showInputDialog
                (null,
                 "Line width in page X/Y units:",
                 String.format("%.5f", getLineWidth()));
            if (str == null) {
                return;
            }
            try {
                setLineWidth(ContinuedFraction.parseDouble(str));
                return;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid number format.");
            }
        }
    }

    EditPane getEditPane() { return editFrame.getEditPane(); }

    static String format(double d, int decimalPoints) {
        Formatter f = new Formatter();
        f.format("%." + decimalPoints + "f", d);
        return f.toString();
    }

    /** Compress the brightness into the upper "frac" portion of the range
        0..255. */
    static int fade(int i, double frac) {
        int res = (int) (255 - (255 - i)*frac);
        return (res < 0) ? 0 : res;
    }

    static void fade(BufferedImage src, BufferedImage dest, double frac) { 
        int width = src.getWidth();
        int height = src.getHeight();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                Color c = new Color(src.getRGB(x,y));
                c = new Color(fade(c.getRed(), frac),
                              fade(c.getGreen(), frac),
                              fade(c.getBlue(), frac));
                dest.setRGB(x,y,c.getRGB());
            }
        }
    }

    public void run(String filename) {
        editFrame.setTitle("Phase Equilibria Diagram Editor");
        editFrame.pack();
        if (filename != null) {
            String lcase = filename.toLowerCase();
            int index = lcase.lastIndexOf(".ped");
            if (index >= 0 && index == lcase.length() - 4) {
                openDiagram(new File(filename));
                initializeGUI();
            } else {
                openImage(filename);
            }
        } else {
            initializeGUI();
        }
    }

    public static void initializeCrosshairs() {
        if (crosshairs == null) {
            URL url =
                Editor.class.getResource("images/crosshairs.png");
            try {
                crosshairs = ImageIO.read(url);
            } catch (IOException e) {
                throw new Error("Could not load crosshairs at " + url);
            }
            if (crosshairs == null) {
                throw new Error("Resource " + url + " not found");
            }
        }
    } 

    public static void printHelp() {
        System.err.println("Usage: java Editor [<filename>]");
    }

    @Override
        public void mouseClicked(MouseEvent e) {
        // Auto-generated method stub
    }

    @Override
        public void mouseReleased(MouseEvent e) {
        // Auto-generated method stub
    }

    @Override
        public void mouseEntered(MouseEvent e) {
        // Auto-generated method stub
    }

    boolean isStraight(GeneralPolyline path) {
        return path.size() < 2 || path.getSmoothingType() == GeneralPolyline.LINEAR;
    }

    /** @return an array of all straight line segments defined for
        this diagram. */
    @JsonIgnore
    public Line2D.Double[] getAllLineSegments() {
        ArrayList<Line2D.Double> output
            = new ArrayList<Line2D.Double>();
         
        for (GeneralPolyline path : paths) {
            if (path.getSmoothingType() == GeneralPolyline.LINEAR) {
                Point2D.Double[] points = path.getPoints();
                for (int i = 1; i < points.length; ++i) {
                    output.add(new Line2D.Double(points[i-1], points[i]));
                }
                if (points.length >= 3 && path.isClosed()) {
                    output.add(new Line2D.Double(points[points.length-1], points[0]));
                }
            } else if (path.size() == 2) {
                // A smooth path between two points is a segment.
                output.add(new Line2D.Double(path.get(0), path.get(1)));
            }
        }

        for (LinearRuler ruler: rulers) {
            GeneralPolyline path = ruler.spinePolyline();
            output.add(new Line2D.Double(path.get(0), path.get(1)));
        }

        return output.toArray(new Line2D.Double[0]);
    }

    /** Draw a circle around each point in path. */
    void circleVertices(Graphics2D g, GeneralPolyline path, double scale,
                        boolean fill, double r) {
        Point2D.Double xpoint = new Point2D.Double();
        Affine p2d = principalToScaledPage(scale);

        Stroke oldStroke = g.getStroke();
        if (!fill) {
            g.setStroke(new BasicStroke((float) (r / 4)));
        }

        for (Point2D.Double point: path.getPoints()) {
            p2d.transform(point, xpoint);
            Shape shape = new Ellipse2D.Double
                (xpoint.x - r, xpoint.y - r, r * 2, r * 2);
            if (fill) {
                g.fill(shape);
            } else {
                g.draw(shape);
            }
        }

        if (!fill) {
            g.setStroke(oldStroke);
        }
    }

    @Override
        public void mouseExited(MouseEvent e) {
        mprin = null;
        repaintEditFrame();
    }

    /* Draw the label defined by the given label and view combination
       to the given graphics context while mulitplying the font size
       and position by scale. */
    public void drawLabel(Graphics g, int labelNo, double scale) {
        drawLabel(g, labelNo, scale, false);
    }

    /* @param Draw the label defined by the given label and view combination
       to the given graphics context while mulitplying the font size
       and position by scale. */
    void drawLabel(Graphics g0, int labelNo, double scale,
                          boolean circleAnchor) {
        AnchoredLabel label = labels.get(labelNo);
        if (label.getFontSize() == 0) {
            return;
        }

        Graphics2D g = (Graphics2D) g0;
        View view = labelViews.get(labelNo);
        Affine toPage = getPrincipalToAlignedPage();
        Point2D.Double point = toPage.transform(label.getX(), label.getY());
        double angle = principalToPageAngle(label.getAngle());

        AffineTransform l2s = labelToScaledPage
            (view, scale * label.getFontSize(), angle,
             point.x * scale, point.y * scale,
             label.getXWeight(), label.getYWeight(),
             label.getBaselineXOffset(), label.getBaselineYOffset());

        Path2D.Double path = htmlBox(l2s);

        if (label.isOpaque()) {
            Color oldColor = g.getColor();
            g.setColor(Color.WHITE);
            g.fill(path);
            g.setColor(oldColor);
        }

        if (label.isBoxed()) {
            g.draw(path);
        }

        if (circleAnchor
            && (label.getXWeight() != 0.5 || label.getYWeight() != 0.5)) {
            // Mark the anchor with a circle -- either a solid circle
            // if the selection handle is the anchor, or a hollow
            // circle if the selection handle is the label's center.

            double r = Math.max(scale * 2.0 / BASE_SCALE, 4.0);
            Point2D.Double p = new Point2D.Double
                (label.getXWeight(), label.getYWeight());
            l2s.transform(p, p);
            Ellipse2D circle = new Ellipse2D.Double
                (p.x - r, p.y - r, r * 2, r * 2);
            if (getLabelHandle().handle == LabelHandleType.CENTER) {
                g.draw(circle);
            } else {
                g.fill(circle);
            }
        }

        Point2D.Double centerPage = new Point2D.Double();
        htmlDraw(g, view, scale * label.getFontSize(),
                 angle, point.x * scale, point.y * scale,
                 label.getXWeight(), label.getYWeight(),
                 label.getBaselineXOffset(), label.getBaselineYOffset(),
                 centerPage);

        try {
            labelCenters.set(labelNo,
                             toPage.createInverse().transform(centerPage));
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException(toPage + " is not invertible");
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

       @param labelCenter (output parameter) If this parameter is not
       null, then it will be reset to the standard page coordinates of
       the center of the label.
    */
    void htmlDraw(Graphics g, View view, double scale, double angle,
                  double ax, double ay,
                  double xWeight, double yWeight,
                  double baselineXOffset, double baselineYOffset,
                  Point2D.Double labelCenter) {
        scale /= VIEW_MAGNIFICATION;
        double baseWidth = view.getPreferredSpan(View.X_AXIS);
        double baseHeight = view.getPreferredSpan(View.Y_AXIS);
        double width = baseWidth + labelXMargin * 2;
        double height = baseHeight + labelYMargin * 2;

        Graphics2D g2d = (Graphics2D) g;
        double textScale = scale / BASE_SCALE;

        AffineTransform baselineToPage = AffineTransform.getRotateInstance(angle);
        baselineToPage.scale(textScale, textScale);
        Point2D.Double xpoint = new Point2D.Double();
        baselineToPage.transform
            (new Point2D.Double(width * xWeight, height * yWeight), xpoint);

        ax -= xpoint.x;
        ay -= xpoint.y;

        Point2D.Double baselineOffset = new Point2D.Double
            (baselineXOffset, baselineYOffset);
        baselineToPage.transform
            (baselineOffset, baselineOffset);

        ax += baselineOffset.x;
        ay += baselineOffset.y;

        // Now (ax, ay) represents the (in baseline coordinates) upper
        // left corner of the text block expanded by the x- and
        // y-margins.

        if (labelCenter != null) {
            baselineToPage.transform(new Point2D.Double(width/2, height/2), xpoint);
            labelCenter.x = (xpoint.x + ax) / getScale();
            labelCenter.y = (xpoint.y + ay) / getScale();
        }

        {
            // Displace (ax,ay) by (labelXMargin, labelYMargin) (again, in baseline
            // coordinates) in order to obtain the true upper left corner
            // of the text block.

            baselineToPage.transform
                (new Point2D.Double(labelXMargin, labelYMargin), xpoint);
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

    /** @return a transformation that maps the unit square to the
        outline of this label in scaled page space. */
    AffineTransform labelToScaledPage
        (View view, double scale, double angle,
         double ax, double ay, double xWeight, double yWeight,
         double baselineXOffset, double baselineYOffset) {
        double width = view.getPreferredSpan(View.X_AXIS) + labelXMargin * 2;
        double height = view.getPreferredSpan(View.Y_AXIS) + labelYMargin * 2;
        double textScale = scale / BASE_SCALE / VIEW_MAGNIFICATION;

        AffineTransform res = AffineTransform.getTranslateInstance(ax, ay);
        res.rotate(angle);
        res.scale(textScale, textScale);
        res.translate(baselineXOffset, baselineYOffset);
        res.scale(width, height);
        res.translate(-xWeight, -yWeight);
        return res;
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

    static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT,
                                   true);
            SerializationConfig ser = objectMapper.getSerializationConfig();
            DeserializationConfig des = objectMapper.getDeserializationConfig();

            ser.addMixInAnnotations(Point2D.class, Point2DAnnotations.class);
            des.addMixInAnnotations(Point2D.class, Point2DAnnotations.class);

            ser.addMixInAnnotations(Rectangle2D.class,
                                    Rectangle2DAnnotations.class);
            des.addMixInAnnotations(Rectangle2D.class,
                                    Rectangle2DAnnotations.class);

            ser.addMixInAnnotations(Rectangle2D.Double.class,
                                    Rectangle2DDoubleAnnotations.class);
            des.addMixInAnnotations(Rectangle2D.Double.class,
                                    Rectangle2DDoubleAnnotations.class);

            ser.addMixInAnnotations(BasicStroke.class,
                                    BasicStrokeAnnotations.class);
            des.addMixInAnnotations(BasicStroke.class,
                                    BasicStrokeAnnotations.class);

            ser.addMixInAnnotations(DecimalFormat.class,
                                    DecimalFormatAnnotations.class);
            des.addMixInAnnotations(DecimalFormat.class,
                                    DecimalFormatAnnotations.class);

            ser.addMixInAnnotations(NumberFormat.class,
                                    NumberFormatAnnotations.class);
            des.addMixInAnnotations(NumberFormat.class,
                                    NumberFormatAnnotations.class);

            ser.addMixInAnnotations(Color.class,
                                    ColorAnnotations.class);
            des.addMixInAnnotations(Color.class,
                                    ColorAnnotations.class);
        }

        return objectMapper;
    }

    void drawArrow(Graphics2D g, double scale, Arrow ai) {
        Affine xform = principalToScaledPage(scale);
        Point2D.Double xpoint = xform.transform(ai.x, ai.y);
        Arrow arr = new Arrow
            (xpoint.x, xpoint.y, scale * ai.size,
             principalToPageAngle(ai.theta));
        g.fill(arr);
    }

    synchronized ScaledCroppedImage getScaledOriginalImage() {
        JScrollPane spane = editFrame.getScrollPane();
        Rectangle viewBounds = spane.getViewport().getViewRect();
        Rectangle imageBounds = scaledPageBounds(scale);
        Rectangle imageViewBounds = viewBounds.intersection(imageBounds);

        // Attempt to work around a bug where Rectangle#intersection
        // returns negative widths or heights.
        if (imageViewBounds.width <= 0 || imageViewBounds.height <= 0) {
            imageViewBounds = null;
        }

        int totalMemoryUsage = 0;
        int maxScoreIndex = -1;
        int maxScore = 0;

        if (scaledOriginalImages == null) {
            scaledOriginalImages = new ArrayList<ScaledCroppedImage>();
        }
        int cnt = scaledOriginalImages.size();

        for (int i = cnt - 1; i>=0; --i) {
            ScaledCroppedImage im = scaledOriginalImages.get(i);
            if (Math.abs(1.0 - scale / im.scale) < 1e-6
                && (imageViewBounds == null
                    || im.cropBounds.contains(imageViewBounds))) {
                // Found a match.

                // Promote this image to the front of the LRU queue (last
                // position in the ArrayList).
                scaledOriginalImages.remove(i);
                scaledOriginalImages.add(im);
                return im;
            }

            // Lower scores are better. Penalties are given for memory
            // usage and distance back in the queue (implying the
            // image has not been used recently).

            int mu = im.getMemoryUsage();
            totalMemoryUsage += mu;

            int thisScore = mu * (cnt - i);
            if (thisScore > maxScore) {
                maxScore = thisScore;
                maxScoreIndex = i;
            }
        }

        // Save memory if we're at the limit.

        int totalMemoryLimit = 20000000; // Limit is 20 megapixels total.
        int totalImageCntLimit = 50;
        if (totalMemoryUsage > totalMemoryLimit) {
            scaledOriginalImages.remove(maxScoreIndex);
        } else if (cnt >= totalImageCntLimit) {
            // Remove the oldest image.
            scaledOriginalImages.remove(0);
        }

        // Create a new ScaledCroppedImage that is big enough to hold
        // all of a medium-sized scaled image and that is also at
        // least several times the viewport size if the scaled image
        // is big enough to need to be cropped.

        // Creating a cropped image that is double the viewport size
        // in both dimensions is near optimal in the sense that for a
        // double-sized cropped image, if the user drags the mouse in
        // a fixed direction, the frequency with which the scaled
        // image has to be updated times the approximate cost of each
        // update is minimized.

        Dimension maxCropSize = new Dimension
            (Math.max(2000, viewBounds.width * 2),
             Math.max(1500, viewBounds.height * 2));

        Rectangle cropBounds = new Rectangle();

        if (imageBounds.width * 3 <= maxCropSize.width * 4) {
            // If allowing a little extra space beyond the normal
            // maximum can make cropping unnecessary, then do it.
            cropBounds.x = 0;
            cropBounds.width = imageBounds.width;
        } else {
            int margin1 = (maxCropSize.width - imageViewBounds.width) / 2;
            int margin2 = margin1;

            int ivmin = imageViewBounds.x;
            int ivmax = ivmin + imageViewBounds.width;
            int immax = imageBounds.x + imageBounds.width;

            int extra = margin1 - ivmin;
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 += extra;
                margin1 -= extra;
            }

            extra = margin2 - (immax - ivmax);
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 -= extra;
                margin1 += extra;
            }

            cropBounds.x = imageViewBounds.x - margin1;
            cropBounds.width = imageViewBounds.width + margin1 + margin2;
        }

        if (imageBounds.height * 3 <= maxCropSize.height  * 4) {
            // If allowing a little extra space beyond the normal
            // maximum can make cropping unnecessary, then do it.
            cropBounds.y = 0;
            cropBounds.height = imageBounds.height;
        } else {
            int margin1 = (maxCropSize.height - imageViewBounds.height) / 2;
            int margin2 = margin1;

            int ivmin = imageViewBounds.y;
            int ivmax = ivmin + imageViewBounds.height;
            int immax = imageBounds.y + imageBounds.height;

            int extra = margin1 - ivmin;
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 += extra;
                margin1 -= extra;
            }

            extra = margin2- (immax - ivmax);
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 -= extra;
                margin1 += extra;
            }

            cropBounds.y = imageViewBounds.y - margin1;
            cropBounds.height = imageViewBounds.height + margin1 + margin2;
        }

        // Create the transformed, cropped, and faded image of the
        // original diagram.
        PolygonTransform originalToCrop = originalToPrincipal.clone();
        originalToCrop.preConcatenate(principalToScaledPage(scale));

        // Shift the transform so that location (cropBounds.x,
        // cropBounds.y) is mapped to location (0,0).

        originalToCrop.preConcatenate
            (new Affine(AffineTransform.getTranslateInstance
                        ((double) -cropBounds.x, (double) -cropBounds.y)));

        ScaledCroppedImage im = new ScaledCroppedImage();
        im.scale = scale;
        im.imageBounds = imageBounds;
        im.cropBounds = cropBounds;
        Cursor oldCursor = editFrame.getCursor();
        ++paintSuppressionRequestCnt;
        editFrame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        ImageTransform.DithererType dither
            = (cropBounds.getWidth() * cropBounds.getHeight() > 3000000)
            ? ImageTransform.DithererType.FAST
            : ImageTransform.DithererType.GOOD;
        System.out.println("Resizing original image (" + dither + ")...");
        im.croppedImage = ImageTransform.run
            (originalToCrop, getOriginalImage(), Color.WHITE,
             new Dimension(cropBounds.width, cropBounds.height), dither);
        fade(im.croppedImage, im.croppedImage, DEFAULT_BACKGROUND_IMAGE_DARKNESS);
        scaledOriginalImages.add(im);
        --paintSuppressionRequestCnt;
        editFrame.setCursor(oldCursor);
        return im;
    }

    void addTernaryBottomRuler(double start /* Z */, double end /* Z */) {
        saveNeeded = true;
        LinearRuler r = new DefaultTernaryRuler() {{ // Component-Z axis
            textAngle = 0;
            tickLeft = true;
            labelAnchor = LinearRuler.LabelAnchor.RIGHT;
        }};

        r.startPoint = new Point2D.Double(start, 0.0);
        r.endPoint = new Point2D.Double(end, 0);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        r.suppressStartTick = (diagramType != DiagramType.TERNARY_RIGHT);
        r.suppressEndTick = (diagramType != DiagramType.TERNARY_LEFT);
        rulers.add(r);
    }

    void addTernaryLeftRuler(double start /* Y */, double end /* Y */) {
        saveNeeded = true;
        LinearRuler r = new DefaultTernaryRuler() {{ // Left Y-axis
            textAngle = Math.PI / 3;
            tickRight = true;
            labelAnchor = LinearRuler.LabelAnchor.LEFT;
        }};

        // Usual PED Data Center style leaves out the tick labels on the left unless this is a top or left
        // partial ternary diagram.
        boolean showLabels = diagramType == DiagramType.TERNARY_LEFT

            || diagramType == DiagramType.TERNARY_TOP;
        if (showLabels) {
            r.labelAnchor = LinearRuler.LabelAnchor.RIGHT;
            r.suppressEndLabel = diagramType != DiagramType.TERNARY_RIGHT;
        } else {
            r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        }


        r.startPoint = new Point2D.Double(0.0, start);
        r.endPoint = new Point2D.Double(0.0, end);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        // The tick label for the bottom of the left ruler is
        // redundant with the tick label for the left end of the
        // bottom ruler unless this is a top partial ternary
        // diagram.
        r.suppressStartLabel = (diagramType != DiagramType.TERNARY_TOP);
        r.suppressStartTick = (diagramType != DiagramType.TERNARY_TOP);
        r.suppressEndLabel = (diagramType != DiagramType.TERNARY_TOP);
        r.suppressEndTick = (diagramType != DiagramType.TERNARY_LEFT);
        rulers.add(r);
    }

    void addTernaryRightRuler(double start /* Y */, double end /* Y */) {
        saveNeeded = true;
        LinearRuler r = new DefaultTernaryRuler() {{ // Right Y-axis
            textAngle = Math.PI * 2 / 3;
            tickLeft = true;
                }};

        // The tick labels for the right ruler are redundant with the
        // tick labels for the left ruler unless this is a top or right
        // partial ternary diagram.
        boolean showLabels = diagramType == DiagramType.TERNARY_RIGHT
            || diagramType == DiagramType.TERNARY_TOP;
        if (showLabels) {
            r.labelAnchor = LinearRuler.LabelAnchor.RIGHT;
            r.suppressEndLabel = diagramType != DiagramType.TERNARY_RIGHT;
        } else {
            r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        }
        r.suppressStartTick = diagramType != DiagramType.TERNARY_TOP;
        r.suppressEndTick = diagramType != DiagramType.TERNARY_RIGHT;

        r.startPoint = new Point2D.Double(1 - start, start);
        r.endPoint = new Point2D.Double(1 - end, end);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        rulers.add(r);
    }

    void addBinaryBottomRuler() {
        saveNeeded = true;
        rulers.add(new DefaultBinaryRuler() {{ // X-axis
            textAngle = 0;
            tickLeft = true;
            labelAnchor = LinearRuler.LabelAnchor.RIGHT;

            startPoint = new Point2D.Double(0.0, 0.0);
            endPoint = new Point2D.Double(1.0, 0.0);
        }});
    }

    void addBinaryTopRuler() {
        saveNeeded = true;
        rulers.add(new DefaultBinaryRuler() {{ // X-axis
            textAngle = 0;
            tickRight = true;
            labelAnchor = LinearRuler.LabelAnchor.NONE;

            startPoint = new Point2D.Double(0.0, 1.0);
            endPoint = new Point2D.Double(1.0, 1.0);
        }});
    }

    void addBinaryLeftRuler() {
        saveNeeded = true;
        rulers.add(new DefaultBinaryRuler() {{ // Left Y-axis
            textAngle = Math.PI / 2;
            tickRight = true;
            labelAnchor = LinearRuler.LabelAnchor.LEFT;

            startPoint = new Point2D.Double(0.0, 0.0);
            endPoint = new Point2D.Double(0.0, 1.0);
        }});
    }

    void addBinaryRightRuler() {
        saveNeeded = true;
        rulers.add(new DefaultBinaryRuler() {{ // Right Y-axis
            textAngle = Math.PI / 2;
            tickLeft = true;
            labelAnchor = LinearRuler.LabelAnchor.NONE;

            startPoint = new Point2D.Double(1.0, 0.0);
            endPoint = new Point2D.Double(1.0, 1.0);
        }});
    }

    /** Return the weight of the given component computed as a product
        of the quantities and standard weights of its individual
        elements, or 0 if the weight could not be computed, either
        because the component is not known, it could not be converted
        to a compound, or the compound includes elements for which no
        standard weight is defined. */
    public double componentWeight(Side side) {
        double[][] componentElements = getComponentElements();
        double[] ces = componentElements[side.ordinal()];
        if (ces == null) {
            return 0;
        }
        String[] elements = getDiagramElements();
        double total = 0;
        for (int i = 0; i < elements.length; ++i) {
            double q = ces[i];
            if (q == 0) {
                continue;
            }
            double w = ChemicalString.elementWeight(elements[i]);
            if (w == 0) {
                return 0;
            }
            total += q * w;
        }
        return total;
    }

    static Color thisOrBlack(Color c) {
        return (c == null) ? Color.BLACK : c;
    }

    @JsonIgnore public Rectangle2D.Double getPrincipalBounds() {
        return principalToStandardPage.inputBounds();
    }

    public String getFontName() {
        return getFont().getFontName();
    }

    public void setFontName(String s) {
        if (embeddedFont != null && s.equals(getFontName())) {
            return; // No change
        }

        saveNeeded = true;
        String filename = fontFiles.get(s);
        if (filename == null) {
            throw new IllegalArgumentException("Unrecognized font name '" + s + "'");
        }
        embeddedFont = loadFont(filename, STANDARD_FONT_SIZE);
        getEditPane().setFont(embeddedFont);
        editFrame.setFontName(s);
    }

    public Font loadFont(String filename, float size) {
        InputStream is = getClass().getResourceAsStream(filename);
        if (is == null) {
            throw new IllegalStateException
                ("Could not locate font '" + filename + "'");
        }
        try {
            Font f = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            f = f.deriveFont(size);
            return f;
        } catch (IOException e) {
            throw new IllegalStateException
                ("Could not process font '" + filename
                 + "': " + e);
        } catch (FontFormatException e) {
            throw new IllegalStateException
                ("Could not process font '" + filename
                 + "': " + e);
        }
    }

    @JsonIgnore public Font getFont() {
        if (embeddedFont == null) {
            setFontName(defaultFontName);
        }
        return embeddedFont;
    }

	public void setFillStyle(StandardFill fill) {
		// TODO Auto-generated method stub
	}
}

// Annotations that are serialization hints for the Jackson JSON
// encoder

@JsonDeserialize(as=Point2D.Double.class)
abstract class Point2DAnnotations {
}

@JsonDeserialize(as=Rectangle2D.Double.class)
abstract class Rectangle2DAnnotations {
}

@SuppressWarnings("serial")
abstract class Rectangle2DDoubleAnnotations
    extends Rectangle2D.Double {
    @Override @JsonIgnore abstract public Rectangle getBounds();
    @Override @JsonIgnore abstract public Rectangle2D getBounds2D();
    @Override @JsonIgnore abstract public boolean isEmpty();
    @Override @JsonIgnore abstract public double getMinX();
    @Override @JsonIgnore abstract public double getMaxX();
    @Override @JsonIgnore abstract public double getMinY();
    @Override @JsonIgnore abstract public double getMaxY();
    @Override @JsonIgnore abstract public Rectangle2D getFrame();
    @Override @JsonIgnore abstract public double getCenterX();
    @Override @JsonIgnore abstract public double getCenterY();
}

class BasicStrokeAnnotations {
    BasicStrokeAnnotations
        (@JsonProperty("lineWidth") float lineWidth,
         @JsonProperty("endCap") int endCap,
         @JsonProperty("lineJoin") int lineJoin,
         @JsonProperty("miterLimit") float miterLimit,
         @JsonProperty("dashArray") float[] dashArray,
         @JsonProperty("dashPhase") float dashPhase) {}
}

@SuppressWarnings("serial")
class DecimalFormatAnnotations extends DecimalFormat {
    @Override @JsonProperty("pattern") public String toPattern() {
        return null;
    }
    DecimalFormatAnnotations(@JsonProperty("pattern") String pattern) {}
}

@SuppressWarnings("serial")
@JsonTypeInfo(
              use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "type")
@JsonSubTypes({
        @Type(value=DecimalFormat.class, name = "DecimalFormat") })
@JsonIgnoreProperties
    ({"groupingUsed", "parseIntegerOnly",
      "maximumIntegerDigits",
      "minimumIntegerDigits",
      "maximumFractionDigits",
      "minimumFractionDigits",
      "positivePrefix",
      "positiveSuffix",
      "negativePrefix",
      "negativeSuffix",
      "multiplier",
      "groupingSize",
      "decimalSeparatorAlwaysShown",
      "parseBigDecimal",
      "roundingMode",
      "decimalFormatSymbols",
      "currency"})
abstract class NumberFormatAnnotations extends NumberFormat {
}

@SuppressWarnings("serial")
@JsonIgnoreProperties
    ({"alpha", "red", "green", "blue",
            "colorSpace", "transparency"})
abstract class ColorAnnotations extends Color {
    ColorAnnotations(@JsonProperty("rgb") int rgb) {
        super(rgb);
    }
}

class ScaledCroppedImage {
    double scale;

    int getMemoryUsage() {
        return cropBounds.width * cropBounds.height;
    }

    /** The bounds of the entire image at this scale. */
    Rectangle imageBounds;
    /** The image of coveredRegion at this scale. */
    BufferedImage croppedImage;
    /** The bounds of the portion of the scaled image that is stored
        in croppedImage. */
    Rectangle cropBounds;

    boolean isCropped() {
        return cropBounds.width < imageBounds.width
            || cropBounds.height < imageBounds.height;
    }

    public String toString() {
        return "Scale: " + scale + " image: " + imageBounds
            + " crop: " + cropBounds;
    }
}
