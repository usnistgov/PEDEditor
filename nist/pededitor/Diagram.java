/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JLabel;
import javax.swing.text.View;

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
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/** Main class for Phase Equilibria Diagrams and their presentation,
    but not including GUI elements such as menus and windows. */
public class Diagram extends Observable implements Printable {
    static ObjectMapper objectMapper = null;
    protected static final DecimalFormat STANDARD_PERCENT_FORMAT
        = new DecimalFormat("##0.00%");

    public Interp2DHandle removeVertex(Interp2DHandle hand) {
        Interp2DDecoration d = hand.getDecoration();
        Interp2D path = d.getCurve();
        if (path.size() == path.minSize()) {
            removeDecoration(d);
            return null;
        }
        
        int oldVertexCnt = path.size();
        int index = hand.index;

        if (oldVertexCnt >= 2) {
            ArrayList<Double> segments = getPathSegments(d);

            // While deleting this vertex, adjust t values that
            // reference this segment.

            for (int i = 0; i < segments.size(); ++i) {
                double newT = path.newTIfVertexRemoved
                    (segments.get(i), index);
                segments.set(i, newT);
            }

            path.remove(index);
            setPathSegments(d, segments);
            if (index > 0 && index < path.size()) {
                Point2D.Double previous = path.get(index - 1);
                Point2D.Double next = path.get(index);
                if (previous.equals(next)) {
                    return removeVertex(d.createHandle(index));
                }
            }
            if (d.isDegenerate()) {
                removeDecoration(d);
                return null;
            }
            propagateChange();
            return d.createHandle((index > 0) ? (index - 1) : 0);
        } else {
            removeDecoration(d);
            return null;
        }
    }
    
    /** Apply the NIST MML PED standard binary diagram axis style. */
    static LinearRuler defaultBinaryRuler() {
        LinearRuler r = new LinearRuler();
        r.fontSize = normalRulerFontSize();
        r.lineWidth = STANDARD_LINE_WIDTH;
        r.tickPadding = 3.0;
        r.drawSpine = true;
        return r;
    }


    /** Apply the NIST MML PED standard ternary diagram axis style. */
    static LinearRuler defaultTernaryRuler() {
        LinearRuler r = new LinearRuler();
        r.fontSize = normalRulerFontSize();
        r.lineWidth = STANDARD_LINE_WIDTH;
        r.tickPadding = 3.0;
        r.multiplier = 100.0;

        r.tickType = LinearRuler.TickType.V;
        r.suppressStartTick = true;
        r.suppressEndTick = true;

        r.drawSpine = true;
        return r;
    }

    class PathAndT {
        Interp2DDecoration path;
        double t;

        PathAndT(Interp2DDecoration interp2DDecoration, double t) {
            this.path = interp2DDecoration;
            this.t = t;
        }

        PathAndT(Interp2DHandle hand) {
            this(hand.getDecoration(), hand.getT());
        }

        @Override public String toString() {
            return "PathAndT[" + path + ", " + t + "]";
        }
    }


    // The graphics driver grid fits the label views (at a pixel
    // level, unrelated to the grid the program displays) at the font
    // size of the buttons they were created from. Grid-fitting throws
    // off the font metrics, but the bigger the font size, the less
    // effect grid-fitting has, and the less error it induces. So make
    // the Views big enough that grid-fitting is largely irrelevant.

    private static final int VIEW_MAGNIFICATION = 8; // = 100 px / 12.5 px
    // The value 4290 below is a meaningless historical accident.
    protected static final double BASE_SCALE = 4920.0 / VIEW_MAGNIFICATION;
    static protected final String defaultFontName = "DejaVu LGC Sans PED";
    static protected final Map<String,String> fontFiles
        = new HashMap<String, String>() {
        private static final long serialVersionUID = -4018269657447031301L;

        {
            put("DejaVu LGC Sans PED", "DejaVuLGCSansPED.ttf");
            put("DejaVu LGC Serif PED", "DejaVuLGCSerifPED.ttf");
            put("DejaVu LGC Sans GRUMP", "DejaVuLGCSansGRUMP.ttf");
        }
    };

    // embeddedFont is initialized when needed.
    protected Font embeddedFont = null;
    protected Map<String,String> keyValues = null;
    protected Set<String> tags = new HashSet<>();

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
    /** Bounds of the entire page in standardPage space. */
    protected Rectangle2D.Double pageBounds;
    protected DiagramType diagramType = null;
    protected ArrayList<Decoration> decorations;
    /** In pixel mode, one-point lines are shown as squares at the
        normal line width and line ends; CAP_SQUARE and JOIN_MITER are
        the default settings for lines; and the grid length is one
        screen unit. */
    protected boolean pixelMode = false;

    @JsonProperty protected String[/* Side */] diagramComponents = null;

    /** All elements contained in diagram components, sorted into Hill
        order. Only trustworthy if componentElements != null. */
    protected transient String[] diagramElements = null;
    /** Diagram components expressed as vectors of element quantities.
        The element quantities are parallel to the diagramElements[]
        array. Set this to null whenever diagramComponents changes. */
    protected transient double[/* Side */][/* elementNo */]
        componentElements = null;

    protected String originalFilename;
    protected byte[] originalImageBytes;

    protected BackgroundImageType backgroundType = BackgroundImageType.NONE;
    protected ArrayList<LinearAxis> axes = new ArrayList<>();

    protected double labelXMargin = Double.NaN;
    protected double labelYMargin = Double.NaN;
    protected double boxedLabelXMargin = Double.NaN;
    protected double boxedLabelYMargin = Double.NaN;
    protected boolean percentagePreferred = true;

    static final double STANDARD_LINE_WIDTH = 0.0024;
    static final double STANDARD_FONT_SIZE = 12.5;
    static final double STANDARD_LABEL_BOX_WIDTH = 0.0010;
    static final int STANDARD_RULER_FONT_SIZE = 15;
    protected String filename;
    private boolean usingWeightFraction = false;
    /** suppressUpdateCnt represents a count of the number of
        currently active orders to turn off the whole notification
        system. If suppressUpdateCnt > 0, changes are treated like no
        change. There are two reasons you may want to do this: 1) you
        are pushing through a whole bunch of changes and the diagram
        may temporarily be in an invalid state in the meantime, so you
        turn notifications off and then notify at the end; 2) you are
        making transient changes that will be undone later. */
    transient int suppressUpdateCnt = 0;
    transient boolean saveNeeded = false;
    
    /** True if the program already tried and failed to load the image
        named originalFilename. */
    protected transient boolean triedToLoadOriginalImage = false;
    protected transient BufferedImage originalImage;


    /** If an UpdateSuppressor object is created, then all changes are
        treated like no change at all, until the object is closed
        again. */
    class UpdateSuppressor implements AutoCloseable {
        public UpdateSuppressor() {
            ++suppressUpdateCnt;
        }

        @Override public void close() {
            --suppressUpdateCnt;
        }
    }

    public Diagram() {
        init();
    }

    /** @return the filename that has been assigned to the PED format
        diagram output. */
    @JsonIgnore public String getFilename() {
        return filename;
    }

    private void init() {
        originalToPrincipal = null;
        originalFilename = null;
        principalToOriginal = null;
        setPrincipalToStandardPage(null);
        pageBounds = null;
        decorations = new ArrayList<>();
        removeAllTags();
        removeAllVariables();
        diagramComponents = new String[Side.values().length];
        componentElements = null;
        filename = null;
        saveNeeded = false;
        embeddedFont = null;
        keyValues = new TreeMap<>();
    }

    /** Initialize/clear almost every field except diagramType. */
    void clear() {
        init();
    }

    @JsonIgnore public boolean isPercentagePreferred() {
        return percentagePreferred;
    }

    public void setPercentagePreferred(boolean v) {
        percentagePreferred = v;
        propagateChange();
    }

    @JsonSerialize(include = Inclusion.NON_DEFAULT)
    public boolean isPixelMode() {
        return pixelMode;
    }

    public void setPixelMode(boolean v) {
        pixelMode = v;
        propagateChange();
    }

    CuspFigure idToCurve(int id) {
        for (CuspFigure path: paths()) {
            if (path.getJSONId() == id) {
                return path;
            }
        }
        System.err.println("No curve found with id " + id + ".");
        return null;
    }

    public Point2D.Double[] diagramVertices() {
        return (principalToStandardPage == null) ? null
            : principalToStandardPage.getInputVertices();
    }

    /** Like diagramVertices(), but don't trust getInputVertices();
        instead assume all diagram component values in the range
        0-100% are OK. */
    Point2D.Double[] expansiveDiagramVertices() {
        if (isTernary()) {
            return new Point2D.Double[]
                { new Point2D.Double(1, 0),
                  new Point2D.Double(0, 1),
                  new Point2D.Double(0, 0) };
        } else {
            Rectangle2D.Double r =
                ((RectangleTransform) principalToStandardPage).inputRectangle();
            double bogoMin = Math.min(0, Math.min(r.x, r.x + r.width));
            double bogoMax = Math.max(1, Math.max(r.x, r.x + r.width));
            return new Point2D.Double[]
                { new Point2D.Double(bogoMin, r.y),
                  new Point2D.Double(bogoMin, r.y + r.height),
                  new Point2D.Double(bogoMax, r.y + r.height),
                  new Point2D.Double(bogoMax, r.y) };
        }
    }

    /** @return the shape of the core graph, which, except for
        free-form diagrams, is usually smaller than the entire page,
        but not always -- the page may have been resized, the diagram
        may have been resized (after which, confusingly, the original
        diagram size is still returned by this), and for trapezoidal
        ternary diagrams, the core graph will be the entire triangle,
        possibly extending beyond the top of the page. */
    public Path2D diagramShape() {
        Path2D.Double res = new Path2D.Double();
        int pointCnt = 0;
        for (Point2D.Double p: expansiveDiagramVertices()) {
            ++pointCnt;
            if (pointCnt > 1) {
                res.lineTo(p.x, p.y);
            } else {
                res.moveTo(p.x, p.y);
            }
        }
        res.closePath();
        return res;
    }

    /** @return the diagram's shape, as in diagramShape(), transformed
        by xform. */
    public Path2D diagramShape(AffineTransform xform) {
        Path2D.Double res = new Path2D.Double();
        int pointCnt = 0;
        for (Point2D.Double p: expansiveDiagramVertices()) {
            ++pointCnt;
            xform.transform(p, p);
            if (pointCnt > 1) {
                res.lineTo(p.x, p.y);
            } else {
                res.moveTo(p.x, p.y);
            }
        }
        res.closePath();
        return res;
    }

    public String[] getTags() {
        return tags.toArray(new String[0]);
    }

    public boolean containsTag(String tag) {
        return tags.contains(tag);
    }

    public void removeAllTags() {
        while (tags.size() > 0) {
            removeTag(tags.iterator().next());
        }
    }

    public void removeAllVariables() {
        while (axes.size() > 0) {
            remove(axes.get(0));
        }
    }

    public void setTags(String[] newTags) {
        removeAllTags();
        for (String tag: newTags) {
            addTag(tag);
        }
    }

    public void addTag(String tag) {
        propagateChange();
        tags.add(tag);
    }

    public void removeTag(String tag) {
        propagateChange();
        tags.remove(tag);
    }

    public void removeVariable(String name) throws CannotDeletePrincipalVariableException, NoSuchVariableException {
        for (LinearAxis axis: axes) {
            if (axis.name.equals(name)) {
                if (axis == getXAxis() || axis == getYAxis()) {
                    throw new CannotDeletePrincipalVariableException(axis);
                }
                remove(axis);
                return;
            }
        }
        throw new NoSuchVariableException(name);
    }

    @JsonIgnore public void setSaveNeeded(boolean b) {
        if (b) {
            propagateChange();
        } else {
            saveNeeded = false;
        }
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
        propagateChange();
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
        propagateChange();
    }

    /** Remove the given key from the keyValues field, and return its
        value, or null if absent. */
    public String removeKey(String key) {
        String output = keyValues.remove(key);
        propagateChange();
        return output;
    }

    /** Return true if p1 and p2's
        transformations to the standard page are very close. */
    boolean principalCoordinatesMatch(Point2D p1, Point2D p2) {
        return principalCoordinatesMatch(p1, p2, pageMatchDistance());
    }

    double pageMatchDistance() {
        return 0.25e-6 * pagePerimeter();
    }

    /** Return true if the distance between p1 and p2's
        transformations to the standard page is less than
        threshold. */
    public boolean principalCoordinatesMatch(Point2D p1, Point2D p2,
            double threshold) {
        Point2D.Double page1 = principalToStandardPage.transform(p1);
        Point2D.Double page2 = principalToStandardPage.transform(p2);
        return page1.distanceSq(page2) < threshold * threshold;
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

    /** setChanged() and then notifyObservers() */
    public void propagateChange1() {
        if (suppressUpdateCnt > 0) {
            return;
        }
        setChanged();
        notifyObservers(null);
    }

    /** Like propagateChange1(), but also set saveNeeded = true. */
    public void propagateChange() {
        if (suppressUpdateCnt > 0) {
            return;
        }
        saveNeeded = true;
        propagateChange1();
    }

    Rectangle scaledPageBounds(double scale) {
        return new Rectangle((int) 0, 0,
                             (int) Math.ceil(pageBounds.width * scale),
                             (int) Math.ceil(pageBounds.height * scale));
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

    public void paintBackground(Graphics2D g, double scale, Color color) {
        // Draw a box the size of the page.
        if (color != null && pageBounds.width > 0) {
            g.setColor(color);
            g.fill(scaledPageBounds(scale));
        }
    }

    protected void applyRenderingHints(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** Paint the diagram.

        @param backColor If not null, paint pageBounds this color
        before all other painting is done.

        @param clip If true, clip everything outside pageBounds. If
        false, even items outside the page bounds may still be seen
        (which may be useful during editing). */
    public void paintDiagram(Graphics2D g, double scale, Color backColor,
            boolean clip) {
        Shape oldClip = null;
        try {
            oldClip = g.getClip();
            if (clip) {
                g.clip(scaledPageBounds(scale));
            }
            applyRenderingHints(g);
            paintBackground(g, scale, backColor);

            ArrayList<Decoration> decorations = getDecorations();

            for (Decoration d: decorations) {
                draw(g, d, scale);
            }
        } finally {
            g.setClip(oldClip);
        }
    }

    public void draw(Graphics2D g, Decoration d, double scale) {
        g.setColor(thisOrBlack(d.getColor()));
        AffineTransform toPage = getPrincipalToAlignedPage();

        if (d instanceof Interp2DDecoration)
            ((Interp2DDecoration) d).setRoundedStroke(!isPixelMode());
        d.draw(g, toPage, scale);
    }

    /** Add a new vertex to path, located at point, and inserted as
        vertex vertexNo. Return true if successful. */
    public boolean add(Interp2DDecoration path, int vertexNo,
                    Point2D.Double point, boolean smoothed) {
        Interp2D interp = path.getCurve();
        if (interp.size() == interp.maxSize()) {
            return false;
        }
        
        ArrayList<Double> segments = getPathSegments(path);
        
        if (path.getCurve().size() > 0) {
            int segCnt = interp.getSegmentCnt();

            double dist1 = (vertexNo == 0) ? 0
                : point.distance(path.getCurve().get(vertexNo-1));
            double dist2 = (vertexNo == segCnt) ? 0
                : point.distance(path.getCurve().get(vertexNo));

            // For old segment vertexNo-1, map the t range [0, splitT] to
            // new segment vertexNo-1 range [0,1], and map the t range
            // (splitT, 1] to new segment vertexNo range [0,1]. If
            // vertexNo == segCnt-1 then segment vertexNo-1 never existed
            // before, so it doesn't matter what splitT value we use.
            double splitT = dist1 / (dist1 + dist2);

            for (int i = 0; i < segments.size(); ++i) {
                double t = segments.get(i);
                int segment = (int) Math.floor(t);
                double frac = t - segment;
                if (segment >= vertexNo) {
                    ++t;
                } else if (segment == vertexNo-1) {
                    if (frac <= splitT) {
                        t = segment + frac / splitT;
                    } else {
                        t = (segment + 1) + (frac - splitT) / (1.0 - splitT);
                    }
                }
                segments.set(i, t);
            }
        }

        interp.add(vertexNo, point);
        if (interp instanceof Smoothable)
            ((Smoothable) interp).setSmoothed(vertexNo, smoothed);
        setPathSegments(path, segments);
        propagateChange();
        return true;
    }

    /** Make a list of t values that appear in this Diagram
        object and that refer to locations on the given path. */
    ArrayList<Double> getPathSegments(Interp2DDecoration path) {
        ArrayList<Double> res = new ArrayList<>();

        // Tie Lines' limits are defined by t values.
        for (TieLine tie: tieLines()) {
            if (tie.innerEdge == path) {
                res.add(tie.it1);
                res.add(tie.it2);
            }
            if (tie.outerEdge == path) {
                res.add(tie.ot1);
                res.add(tie.ot2);
            }
        }

        return res;
    }

    /** You can change the segments returned by getPathSegments() and
        call setPathSegments() to make corresponding updates to the
        fields from which they came. */
    void setPathSegments(Interp2DDecoration path, ArrayList<Double> segments) {
        int index = 0;
        for (Iterator<TieLine> it = tieLines().iterator(); it.hasNext();) {
            TieLine tie = it.next();
            if (tie.innerEdge == path) {
                tie.it1 = segments.get(index++);
                tie.it2 = segments.get(index++);
            }
            if (tie.outerEdge == path) {
                tie.ot1 = segments.get(index++);
                tie.ot2 = segments.get(index++);
            }
            if (tie.it1 == tie.it2 && tie.ot1 == tie.ot2) {
                it.remove();
            }
        }
    }

    /** Remove decorations that are 'like' the given decoration by
        some arbitrary standard ("do what the user wants"). */
    public void removeLikeThis(Decoration d) {
        if (d instanceof AnchoredLabel) {
            /* Remove all labels that share the same text. */
            AnchoredLabel label = (AnchoredLabel) d;
            String s = label.getText();
            for (Iterator<AnchoredLabel> it = labels().iterator(); it.hasNext();) {
                AnchoredLabel l2 = it.next();
                if (l2.getText().equals(s)) {
                    it.remove();
                }
            }
            return;
        }
        removeDecoration(d);
    }

    /** Add an arrow with the given location, line width, and angle
        (expressed in principal coordinates). */
    public void addArrow(Point2D prin, double lineWidth, double theta) {
        addDecoration(new Arrow(prin.getX(), prin.getY(), lineWidth, theta));
    }

    public void addDecoration(Decoration d) {
        decorations.add(d);
        propagateChange();
    }
        
    public void add(TieLine tie) {
        addDecoration(tie);
    }

    public void add(LinearRuler ruler) {
        addDecoration(ruler);
    }

    public void add(LinearAxis axis) {
        String name = (String) axis.name;
        // Insert in order sorted by name.
        for (int i = 0; ; ++i) {
            if (i == axes.size()
                || name.compareTo((String) axes.get(i).name) < 0) {
                axes.add(i, axis);
                propagateChange();
                return;
            }
        }
    }

    public void rename(LinearAxis axis, String name) {
        axis.name = name;
        propagateChange();
    }

    /** Rename the LinearAxis with the given name. If it's a diagram
        component, update that as well to maintain consistency. Rulers
        that use the axis oldName will still refer to the axis under
        its new name. */
    public void renameVariable(String oldName, String newName)
        throws DuplicateComponentException, NoSuchVariableException {
        // If the user wants to rename or add a component, they should
        // use Chemistry/Components instead. The only time it makes
        // sense to redirect to that is if the old value is a component.

        for (Side side: Side.values()) {
            Axis axis = getAxis(side);
            if (axis != null && axis.name.equals(oldName)
                && (isTernary() || diagramComponents[side.ordinal()] != null)) {
                setDiagramComponent(side, newName);
                return;
            }
        }

        if (newName == null || "".equals(newName)) {
            return;
        }
        for (Axis axis: getAxes()) {
            if (axis.name.equals(newName)) {
                throw new DuplicateComponentException(
                        "A variable named " + axis.name + " already exists");
            }
        }

        for (LinearAxis axis: getAxes()) {
            if (axis.name.equals(oldName)) {
                rename(axis, newName);
                return;
            }
        }
        
        throw new NoSuchVariableException(oldName);
    }
    
    void renameVariableMissingOK(String oldName, String newName)
        throws DuplicateComponentException {
        try {
            renameVariable(oldName, newName);
        } catch (NoSuchVariableException x) {
            // That's fine
        }
    }

    /* Swap the names of the axes named name1 and name2. Rulers that
       refer to those axes will continue to refer to the same axis as
       before, but using the new name. If only one of the two named
       axes exist, then it will be renamed. */
    public void swapVariableNames(String name1, String name2) {
        try {
            String arbitrary = "Blezmogon93";
            renameVariableMissingOK(name1, arbitrary);
            renameVariableMissingOK(name2, name1);
            renameVariableMissingOK(arbitrary, name2);
        } catch (DuplicateComponentException x) { // Should never happen.
            throw new IllegalStateException(x);
        }
    }


    public void remove(LinearAxis axis) {
        // Remove all rulers that depend on this axis.
        for (Iterator<LinearRuler> it = rulers().iterator(); it.hasNext();) {
            LinearRuler ruler = it.next();
            if (ruler.axis == axis) {
                it.remove();
            }
        }

        for (int i = 0; i < axes.size(); ) {
            if (axes.get(i) == axis) {
                axes.remove(i);
            } else {
                ++i;
            }
        }
        propagateChange();
    }

    public void remove(CuspFigure path) {
        // Remove all associated tie lines.
        for (int i = decorations.size() - 1; i >= 0; --i) {
            Decoration d = decorations.get(i);
            if (d instanceof TieLine) {
                TieLine tie = (TieLine) d;
                if (tie.innerEdge == path || tie.outerEdge == path) {
                    decorations.remove(i);
                }
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

    /** See the documentation for the componentElements field. Returns
        a 2D array indexed by [Side][elementNo] that indicates the
        number of atoms of elementNo present in the component for the
        given Side. */
    @JsonIgnore double[][] getComponentElements() {
        if (componentElements != null || diagramType == null) {
            return componentElements;
        }

        ArrayList<StringComposition> scs = new ArrayList<>();

        for (Side side: Side.values()) {
            String dc = HtmlToText.htmlToText(diagramComponents[side.ordinal()]);
            if (dc == null) {
                continue;
            }
            ChemicalString.Match m = ChemicalString.maybeQuotedComposition(dc);
            if (m == null) {
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

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + s + ", " + d + "]";
        }
    }

    /** Assuming that the principal coordinates are defined as mole
        fractions (or in the case of binary diagrams, the X coordinate
        only is define as mole percent), return the mole fractions of
        the various diagram components at point prin, or null if the
        fractions could not be determined.
    */
    protected SideDouble[] componentFractions(Point2D prin) {
        if (prin == null || diagramType == null) {
            return null;
        }
        double x = prin.getX();
        double y = prin.getY();

        LinearAxis leftAxis = getLeftAxis();
        if (isTernary()) {
            double leftFraction = (leftAxis != null) ? leftAxis.applyAsDouble(x,y) :
                (1 - x - y);
            return new SideDouble[] {
                new SideDouble(Side.RIGHT, x),
                new SideDouble(Side.TOP, y),
                new SideDouble(Side.LEFT, leftFraction) };
        } else if (diagramComponents[Side.RIGHT.ordinal()] != null) {
            double leftFraction = (leftAxis != null) ? leftAxis.applyAsDouble(x,y) :
                (1 - x);
            return new SideDouble[] {
                new SideDouble(Side.RIGHT, x),
                new SideDouble(Side.LEFT, leftFraction) };
        } else {
            return null;
        }
    }

    protected Side[] sidesThatCanHaveComponents() {
        if (isTernary()) {
            return new Side[] { Side.RIGHT, Side.TOP, Side.LEFT };
        } else {
            return new Side[] { Side.RIGHT, Side.LEFT };
        }
    }

    protected boolean componentsSumToOne(SideDouble[] sds) {
        double sum = 0;
        for (SideDouble sd: sds) {
            sum += sd.d;
        }
        return Math.abs(1 - sum) < 1e-4;
    }

    /** Assuming that diagram's principal coordinates are mole
        fractions, return the weight fractions of the various diagram
        components at point prin, or null if the fractions could not
        be determined. */
    protected SideDouble[] componentWeightFractions(Point2D prin) {
        SideDouble[] res = componentFractions(prin);
        if (res == null || !componentsSumToOne(res)) {
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
        fractions, return the mole fractions of the various diagram
        components at point prin, or null if the fractions could not
        be determined. */
    protected SideDouble[] componentMoleFractions(Point2D prin) {
        SideDouble[] res = componentFractions(prin);
        if (res == null || !componentsSumToOne(res)) {
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

    // Projection of a point onto the nearest point in a set, and offset from that projection.
    static class ProjectionAndOffset {
        Point2D.Double projection;
        Point2D.Double offset;
        boolean interior; // true only if if in the open interior of
                          // the set, not on the border.

        ProjectionAndOffset() {}

        /** Treat point as if it is an interior point, which means
            offset is zero. */
        ProjectionAndOffset(Point2D point) {
            projection = new Point2D.Double(point.getX(), point.getY());
            offset = new Point2D.Double();
            interior = true;
        }
    }

    /** Conversions between mole and weight fraction produce ugly
        results when fractions exceed 100% or are less than 0%, as is
        often the case for labels outside the core diagram. To reduce
        this effect, treat such labels as the sum of the nearest point
        within the diagram (the projection) and an offset from that
        point, and perform the conversion by converting the projection
        and then adding the unchanged offset. */
    ProjectionAndOffset projectOntoDiagram(Point2D prin) {
        double x = prin.getX();
        double y = prin.getY();
        Shape diagramPage = diagramShape(principalToStandardPage);
        Point2D.Double page = principalToStandardPage.transform(prin);
        CurveDistanceRange cdist = PathParam2D.borderDistance
            (diagramPage, page, 1e-8, 20);
        boolean contained = diagramPage.contains(page);
        boolean interior = (cdist.minDistance > 0) && contained;
        Point2D.Double projection;
        Point2D.Double offset;
        if (contained) {
            projection = new Point2D.Double(x, y);
            offset = new Point2D.Double(0,0);
        } else {
            projection = standardPageToPrincipal.transform(cdist.point);
            offset = new Point2D.Double(x - projection.x, y - projection. y);
        }
        ProjectionAndOffset res = new ProjectionAndOffset();
        res.projection = projection;
        res.offset = offset;
        res.interior = interior;
        return res;
    }

    public SideConcentrationTransform moleToWeightTransform() {
        Side[] sides = sidesWithComponents();
        if (sides == null) {
            return null;
        }
        int len = sides.length;
        double weights[] = new double[len];
        for (int i = 0; i < len; ++i) {
            double wt = componentWeight(sides[i]);
            if (wt == 0) {
                // A value of zero indicates that the component's
                // weight could not be determined.
                return null;
            }
            weights[i] = wt;
        }
        return new SideConcentrationTransform
            (sides,
             (len == 2)
             ? new BinaryMultiplierTransform(weights)
             : new TernaryMultiplierTransform(weights));
    }

    public SideConcentrationTransform weightToMoleTransform() {
        SideConcentrationTransform res = moleToWeightTransform();
        return (res != null) ? res.createInverse() : null;
    }

    /** Convert the given point from mole fraction to weight fraction.
        If this is a binary diagram, then the Y component of the
        return value will equal the Y component of the input value. If
        the conversion cannot be performed for some reason, then
        return null. */
    public Point2D.Double moleToWeightFraction(Point2D p) {
        return transform(p, moleToWeightTransform(), true);
    }

    Point2D.Double transform(Point2D p, SideConcentrationTransform xform, 
            boolean stopAtBorders) {
        try {
            return transform(p, (Transform2D) xform, stopAtBorders);
        } catch (UnsolvableException x) {
            throw new IllegalStateException(x); // Will never happen
        }
    }

    /** Transform the given point with the given transformation, with
     * an important exception: points outside the diagram are
     * transformed by projecting onto the closest point within
     * diagram, transforming that, and applying the inverse of the
     * projection vector afterwards. 
     * @throws UnsolvableException */
    Point2D.Double transform(Point2D p, Transform2D xform, 
            boolean stopAtBorders) throws UnsolvableException {
        if (xform == null) {
            return null;
        }
        ProjectionAndOffset pao = stopAtBorders
            ? projectOntoDiagram(p)
            : new ProjectionAndOffset(p);
        Point2D.Double proj = pao.projection;
        Point2D.Double offs = pao.offset;
        proj = xform.transform(proj);
        return new Point2D.Double(proj.x + offs.x, proj.y + offs.y);
    }

    /** Inverse of moleToWeightFraction(). */
    public Point2D.Double weightToMoleFraction(Point2D p) {
        return transform(p, weightToMoleTransform(), true);
    }

    /** Apply xform to the positions and angles of all decorations, then
        transform the diagram corners.

        @param stopAtBorders if true, handle points outside the
        diagram differently: don't change the angle, just translate
        the decoration position by a vector that equals the difference
        between the closest point in the diagram p and
        xform.transform(p). If false, apply xform to the entire
        diagram and to all angles. */
    public boolean transformDiagram(SlopeTransform2D xform,
            boolean stopAtBorders) throws UnsolvableException {
        for (DecorationHandle hand: movementHandles()) {
            Point2D.Double p = hand.getLocation();
            Decoration d = hand.getDecoration();
            Point2D.Double newP = transform(p, xform, stopAtBorders);

            if (d instanceof Angled) {
                ProjectionAndOffset pao = stopAtBorders
                    ? projectOntoDiagram(p)
                    : new ProjectionAndOffset(p);
                if (!pao.interior && (d instanceof AnchoredLabel)) {
                    AnchoredLabel label = (AnchoredLabel) d;
                    String text = label.getText();
                    if (MoleWeightString.hasAtomic(text)
                        || MoleWeightString.hasMole(text)
                        || MoleWeightString.hasWeight(text)) {
                        // Assume this is a title whose positioning in
                        // the diagram should remain fixed.
                        continue;
                    }
                    if (principalToStandardPage.transform(p)
                        .distanceSq(principalToStandardPage.transform(newP)) > 1e-3) {
                        // If the label moves, it might overlap the axes, so
                        // make it opaque.
                        label.setOpaque(true);
                    }
                }
                if (pao.interior) { // Only modify angles of points inside the diagram.
                    Angled a = (Angled) d;
                    double theta = a.getAngle();
                    a.setAngle(xform.transformAngle(p, theta));
                }
            }
            hand.move(newP);
        }
        transformDiagramCorners(xform);

        return true;
    }

    /** Change the boundary of the core diagram, without changing how
        points are transformed. Basically this means converting
        principalToStandardPage from one kind of
        AffinePolygonTransform to another one that has the same effect
        but with different vertices.
     
        @throws UnsolvableException */
    void transformDiagramCorners(Transform2D xform) throws UnsolvableException {
        AffinePolygonTransform p2s = principalToStandardPage;
        if (p2s instanceof TriangleTransform) {
            Point2D.Double[] dvs = diagramVertices();
            int cnt = dvs.length;
            Point2D.Double[] resInputs = new Point2D.Double[cnt];
            Point2D.Double[] resOutputs = new Point2D.Double[cnt];
            for (int i = 0; i < cnt; ++i) {
                Point2D.Double  px = xform.transform(dvs[i]);
                resInputs[i] = px;
                resOutputs[i] = p2s.transform(px);
            }
            setPrincipalToStandardPage(
                    new TriangleTransform(resInputs, resOutputs));
        } else {
            RectangleTransform oldXform
                =  (RectangleTransform) p2s;
            Rectangle2D.Double inr = oldXform.inputRectangle();
            Point2D.Double i1 = xform.transform
                (new Point2D.Double(inr.x, inr.y));
            Point2D.Double i2 = xform.transform
                (new Point2D.Double(inr.x + inr.width, inr.y + inr.height));
            Point2D.Double o1 = p2s.transform(i1);
            Point2D.Double o2 = p2s.transform(i2);
            setPrincipalToStandardPage(
                    new RectangleTransform(
                            new Rectangle2D.Double(i1.x, i1.y, i2.x - i1.x, i2.y - i1.y),
                            new Rectangle2D.Double(o1.x, o1.y, o2.x - o1.x, o2.y - o1.y)));
        }
    }


    /** Globally convert all coordinates from mole fraction to weight
        fraction, if the information necessary to do so is available.
        Return true if the conversion was carried out.

        Bugs/limitations:

        1. Curves can be distorted, because control points are
        translated, not the curves themselves. This would be hard to
        fix. Isotherms in converted diagrams may no longer lie on the
        curves they describe unless the isotherm is located at a
        control point, and the isotherms may no longer be tangent to
        them either.

        @param convertLabels If true, attempt to convert appearances of
        variants of the words "mole" and "atomic" to "weight" in all
        labels.
    */
    public boolean moleToWeightFraction(boolean convertLabels) {
        SideConcentrationTransform xform = moleToWeightTransform();
        if (xform == null) {
            return false;
        }
        if (convertLabels) {
            for (AnchoredLabel label: labels()) {
                label.setText(MoleWeightString.moleToWeight(label.getText()));
            }
        }
        setUsingWeightFraction(true);
        try {
            boolean res = transformDiagram(xform, true);
            computeMargins();
            return res;
        } catch (UnsolvableException e) {
            throw new IllegalStateException(); // Should never happen
        }
    }

    /** @return true if all diagram components are single elements: O, not O2 or NaCl. */
    @JsonIgnore public boolean isAtomic() {
        double[][] componentElements = getComponentElements();
        if (componentElements == null) {
            return true;
        }
        for (Side side: Side.values()) {
            double[] quants = componentElements[side.ordinal()];
            if (quants == null) {
                continue;
            }
            int nonzeroCnt = 0;
            int onesCnt = 0;
            for (double d: quants) {
                if (d > 0) {
                    nonzeroCnt++;
                }
                if (Math.abs(d - 1) < 1e-6) {
                    onesCnt++;
                }
            }

            if (nonzeroCnt > 1 || nonzeroCnt > onesCnt) {
                return false;
            }
        }

        return true;
    }

    /** Globally convert all coordinates from weight fraction to mole
        fraction, if the information necessary to do so is available.
        Return true if the conversion was carried out.

        @see moleToWeightFraction() for bugs/limitations.

        @param convertLabels If true, attempt to convert appearances of
        variants of the word "weight" to variants of either "mole" or
        "atomic", depending on whether all diagram components consist
        of just a single element. */
    public boolean weightToMoleFraction(boolean convertLabels) {
        SideConcentrationTransform xform = weightToMoleTransform();
        if (xform == null) {
            return false;
        }
        if (convertLabels) {
            boolean isAtom = isAtomic();
            for (AnchoredLabel label: labels()) {
                String s = label.getText();
                label.setText(isAtom ? MoleWeightString.weightToAtomic(s)
                              : MoleWeightString.weightToMole(s));
            }
        }
        setUsingWeightFraction(false);
        try {
            boolean res = transformDiagram(xform, true);
            computeMargins();
            return res;
        } catch (UnsolvableException e) {
            throw new IllegalStateException(); // Should never happen
        }
    }

    public void swapXY() {
        if (principalToStandardPage == null) {
            return;
        }
        if (isTernary())
            throw new IllegalArgumentException(
                    "Cannot swap X and Y axes of ternary diagrams");
        for (String comp: diagramComponents) {
            if (comp != null) {
                throw new IllegalArgumentException(
                        "Cannot swap X and Y axes for diagram with "
                        + "defined component '" + comp + "'");
            }
        }

        // transformDiagram() changes principalToStandardPage, so copy
        // in and out before that happens.
        Rectangle2D.Double in = ((RectangleTransform)
                principalToStandardPage).inputRectangle();
        {
            double x = in.x;
            in.x = in.y;
            in.y = x;
            double w = in.width;
            in.width = in.height;
            in.height = w;
        }
        Rectangle2D.Double out = ((RectangleTransform)
                principalToStandardPage).outputRectangle();
        {
            double x = out.x;
            out.x = out.y + out.height;
            out.y = x + out.width;
            double w = out.width;
            out.width = -out.height;
            out.height = -w;
        }
        double xRange[] = getRange(getXAxis());
        double yRange[] = getRange(getYAxis());

        try {
            transformDiagram(new SwapXY(), false);
        } catch (UnsolvableException e) {
            throw new IllegalStateException(); // Should never happen
        }

        // Swap the X and Y components of all axes.
        for (LinearAxis axis: getAxes()) {
            double a= axis.getA();
            axis.setA(axis.getB());
            axis.setB(a);
        }

        setPrincipalToStandardPage(new RectangleTransform(in, out));
        for (Decoration d: getDecorations()) {
            d.reflect();
            d.neaten(principalToStandardPage);
        }

        setPageBounds(Geom.bounds(
                        IntStream.range(0,2)
                        .mapToObj(i -> principalToStandardPage.transform
                                (new Point2D.Double(yRange[i], xRange[i])))
                        .toArray(size -> new Point2D.Double[size])));

        swapVariableNames("page X", "page Y");
        swapVariableNames("X", "Y");
    }

    public void swapDiagramComponents(Side side1, Side side2) {
        LinearAxis axis1 = getAxis(side1);
        if (axis1 == null)
            throw new IllegalArgumentException(
                    "No component defined for side " + side1);
        LinearAxis axis2 = getAxis(side2);
        if (axis2 == null)
            throw new IllegalArgumentException(
                    "No component defined for side " + side2);
        int i1 = -1;
        int i2 = -1;
        Side[] sides = sidesThatCanHaveComponents();
        for (int i = 0; i < sides.length; ++i) {
            Side side = sides[i];
            if (side == side1) {
                i1 = i;
            } else if (side == side2) {
                i2 = i;
            }
        }
        int[] permutation = new int[isTernary() ? 3 : 2];
        for (int i = 0; i < permutation.length; ++i) {
            permutation[i] = i;
        }
        permutation[i1] = i2;
        permutation[i2] = i1;
        String name1 = (String) axis1.name;
        String name2 = (String) axis2.name;
        axis1.name = name2;
        axis2.name = name1;
        diagramComponents[side1.ordinal()] = name2;
        diagramComponents[side2.ordinal()] = name1;

        SideConcentrationTransform xform = new SideConcentrationTransform(
                sides, new ConcentrationPermutation(permutation));
        try {
            transformDiagram(xform, false);
        } catch (UnsolvableException e) {
            throw new IllegalStateException(); // Should never happen
        }

        for (LinearRuler r: rulers()) {
            if (r.axis == axis1) {
                r.axis = axis2;
            } else if (r.axis == axis2) {
                r.axis = axis1;
            }
        }

        for (Decoration d: getDecorations()) {
            d.reflect();
            d.neaten(principalToStandardPage);
        }
        componentElements = null; // Invalidate cached copy
        computeMargins();
    }

    /* Return true if all sides that could have components do have
       them, those components' compositions are known, and those
       components sum to 100%. */
    boolean haveComponentCompositions() {
        return sidesWithComponents() != null;
    }

    /* Return null unless all sides that could have components do have
       them, those components' compositions are known, and those
       components sum to 100%. Otherwise, return an array of those
       sides. */
    Side[] sidesWithComponents() {
        double[][] componentElements = getComponentElements();
        double a = 0;
        double b = 0;
        double c = 0;
        Side[] res =  sidesThatCanHaveComponents();
        for (Side side: res) {
            if (componentElements[side.ordinal()] == null) {
                return null;
            }
            LinearAxis axis = getAxis(side);
            if (axis == null) {
                return null;
            }
            a += axis.getA();
            b += axis.getB();
            c += axis.getC();
        }

        return (Math.abs(a) < 1e-4 && Math.abs(b) < 1e-4 && Math.abs(c-1) < 1e-4)
            ? res : null;
    }

    static class PointAndError {
        Point2D.Double point;
        double error;
    }

    static class OrderByXY implements Comparator<Point2D.Double> {
        @Override public int compare(Point2D.Double a, Point2D.Double b) {
            double ax = a.x;
            double bx = b.x;
            return (ax < bx) ? -1 : (ax > bx) ? 1
                : (a.y < b.y) ? -1 : (a.y > b.y) ? 1 : 0;
        }
    }

    @JsonIgnore String[] getDiagramElements() {
        getComponentElements();
        return diagramElements;
    }

    class DecorationIterator<T extends Decoration> implements Iterator<T> {
        int lastIndex = -1;
        int index;
        Class<? extends Decoration> klass;

        public DecorationIterator(T singleton) {
            this.klass = singleton.getClass();
            index = nextIndex(0);
        }

        @Override public final boolean hasNext() {
            return index >= 0;
        }

        @SuppressWarnings("unchecked") @Override public final T next() {
            if (index < 0) {
                throw new NoSuchElementException();
            }
            lastIndex = index;
            index = nextIndex(index+1);
            return (T) decorations.get(lastIndex);
        }

        @Override public final void remove() {
            if (lastIndex < 0) {
                throw new IllegalStateException();
            }
            if (index > 0) {
                --index;
            }
            decorations.remove(lastIndex);
            propagateChange();
        }

        private final int nextIndex(int startIndex) {
            for (int i = startIndex; i < decorations.size(); ++i) {
                Decoration d = decorations.get(i);
                if (klass.isInstance(d)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public Iterable<Arrow> arrows() {
        return () -> new DecorationIterator<Arrow>(new Arrow());
    }

    public Iterable<AnchoredLabel> labels() {
        return () -> new DecorationIterator<AnchoredLabel>(new AnchoredLabel());
    }

    public Iterable<CuspFigure> paths() {
        return () -> new DecorationIterator<CuspFigure>(new CuspFigure());
    }

    public Iterable<TieLine> tieLines() {
        return () -> new DecorationIterator<TieLine>(new TieLine());
    }

    public Iterable<LinearRuler> rulers() {
        return () -> new DecorationIterator<LinearRuler>(new LinearRuler());
    }

    @JsonIgnore boolean isTernary() {
        return diagramType != null && diagramType.isTernary();
    }

    public void setDiagramComponent(Side side, String str)
        throws DuplicateComponentException {
        componentElements = null;
        LinearAxis axis = getAxis(side);
        if (str != null && str.isEmpty()) {
            str = null;
        }

        if (str != null) {
            for (Side aSide: Side.values()) {
                if (aSide != side
                    && str.equals(diagramComponents[aSide.ordinal()])) {
                    throw new DuplicateComponentException(
                            "A component named " + str + " already exists");
                }
            }
        }

        diagramComponents[side.ordinal()] = str;

        if (axis != null) {
            if (str != null) {
                rename(axis, str);
                setPercentageDisplay(axis, true);
            }
            return;
        }

        if (str == null) {
            // Nothing to do.
            return;
        }

        axis = defaultAxis(side);
        axis.format = STANDARD_PERCENT_FORMAT;
        axis.name = str;
        add(axis);
    }

    /** Like getRange(), but simply return max - min. */
    double length(ToDoubleFunction<Point2D> ax) {
        return length(ax, pageBounds);
    }

    /** Like getRange(), but simply return max - min. */
    double length(ToDoubleFunction<Point2D> ax, Rectangle2D pageBounds) {
        double[] range = getRange(ax, pageBounds);
        return range[1] - range[0];
    }

    /** Return { min, max } representing the range of values that ax
        can take within the standard page. Assumes that the extremes
        are represented by corners of the page. */
    public double[] getRange(ToDoubleFunction<Point2D> ax) {
        return getRange(ax, pageBounds);
    }

    /** Return { min, max } representing the range of values that ax
        can take within the pageBounds region of the standard page.
        Assumes that the extremes are represented by corners of the
        page. */
    public double[] getRange(ToDoubleFunction<Point2D> ax, Rectangle2D pageBounds) {
        if (principalToStandardPage == null || pageBounds == null) {
            return new double[] { 0, 0 };
        }
        int pointCnt = 0;
        double min = 0;
        double max = 0;
        for (double x: new double[]
            { pageBounds.getMinX(), pageBounds.getMaxX() }) {
            for (double y: new double[]
                { pageBounds.getMinY(), pageBounds.getMaxY() }) {
                double v = ax.applyAsDouble(standardPageToPrincipal.transform(x,y));
                ++pointCnt;
                if (pointCnt == 1) {
                    min = max = v;
                } else {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        }
        return new double[] { min, max };
    }

    /** Return the plain text of all labels, tags, key values, and
        diagram components. Duplicates are removed. */
    @JsonIgnore public String[] getAllText() {
        TreeSet<String> lines = new TreeSet<>();
        for (AnchoredLabel label: labels()) {
            lines.add(HtmlToText.htmlToText(label.getText()));
        }
        for (String s: tags) {
            lines.add(s.trim());
        }
        for (String s: keyValues.values()) {
            lines.add(s.trim());
        }
        for (String s: diagramComponents) {
            if (s != null) {
                lines.add(s);
            }
        }
        return lines.toArray(new String[0]);
    }

    /** Return a multi-line comma-separated-values string of the
        coordinates for all control points of the given curve,
        expressed in terms of variables v1 and v2 */
    public String coordinates(Interp2DDecoration path, LinearAxis v1, LinearAxis v2) {
        StringBuilder sb = new StringBuilder();
        for (Point2D.Double point: path.getCurve().getPoints()) {
            sb.append(v1.applyAsDouble(point) + ", " + v2.applyAsDouble(point));
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Return the coordinates for all labels that match text. */
    public ArrayList<Point2D.Double> labelCoordinates(String text) {
        ArrayList<Point2D.Double> res = new ArrayList<>();
        for (AnchoredLabel label: labels()) {
            if (text.equals(label.getText())) {
                res.add(new Point2D.Double(label.getX(), label.getY()));
            }
        }
        Collections.sort(res, new OrderByXY());
        return res;
    }

    Point2D.Double transform(Point2D.Double p, LinearAxis v1, LinearAxis v2) {
        return new Point2D.Double(v1.applyAsDouble(p), v2.applyAsDouble(p));
    }

    Point2D.Double transform(Point2D.Double p, DoubleUnaryOperator f1, DoubleUnaryOperator f2) {
        return new Point2D.Double(f1.applyAsDouble(p.getX()), f2.applyAsDouble(p.getY()));
    }

    /** Return the coordinates of all labels and curves, expressed in
        terms of f1(v1) and f2(v2) */
    @JsonIgnore public String allCoordinatesToString
        (LinearAxis v1, DoubleUnaryOperator f1, LinearAxis v2, DoubleUnaryOperator f2,
         boolean addComments, int sigFigs) {
        ArrayList<String> groupStartTags = new ArrayList<>();
        ArrayList<ArrayList<Point2D.Double>> rawCoordinateGroups
            = new ArrayList<>();

        TreeSet<String> labelTexts = new TreeSet<>();
        for (AnchoredLabel label: labels()) {
            labelTexts.add(label.getText());
        }

        for (String labelText: labelTexts) {
            String plaintext = HtmlToText.htmlToText(labelText);
            String groupStartTag = null;
            if (addComments) {
                StringBuilder s = new StringBuilder();
                s.append("# Label ");
                for (int i = 0; i < plaintext.length(); ++i) {
                    char ch = plaintext.charAt(i);
                    if (i == 6 || ch < ' ') {
                        s.append("...");
                        break;
                    }
                    s.append(ch);
                }
                s.append("\n");
                groupStartTag = s.toString();
            }
            groupStartTags.add(groupStartTag);
            rawCoordinateGroups.add(labelCoordinates(labelText));
        }

        for (CuspFigure path: paths()) {
            String groupStartTag = null;
            if (addComments) {
                groupStartTag = "# " + path.getLineStyle() + " LINE\n";
            }
            groupStartTags.add(groupStartTag);
            rawCoordinateGroups.add
                (new ArrayList<Point2D.Double>
                 (Arrays.asList(path.getCurve().getPoints())));
        }

        StringBuilder sb = new StringBuilder();
        if (addComments) {
            sb.append("# ");
            sb.append(getProvisionalTitle());
            sb.append("\n");
            sb.append("# ");
            LinearAxis[] axes = { v1, v2 };
            for (int i = 0; i < axes.length; ++i) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append((String) axes[i].name);
                DoubleUnaryOperator f = (i == 0) ? f1 : f2;
                if (f instanceof StandardDoubleUnaryOperator
                    && f != StandardDoubleUnaryOperator.IDENTITY) {
                    sb.append(" ");
                    sb.append(((StandardDoubleUnaryOperator) f).getText());
                }
            }
            sb.append("\n\n");
        }
        int i = -1;
        for (ArrayList<Point2D.Double> g: rawCoordinateGroups) {
            ++i;
            String s = groupStartTags.get(i);
            if (s != null) {
                sb.append(s);
            } else if (i > 0) {
                sb.append("\n");
            }
            sb.append(toString(g, v1, f1, v2, f2, sigFigs));
        }
        return sb.toString();
    }

    /** Expand all margins by the given factor. */
    void expandMargins(double factor) {
        double width = pageBounds.width;
        double height = pageBounds.height;
        pageBounds.x -= width * factor;
        pageBounds.width += 2 * width * factor;
        pageBounds.y -= height * factor;
        pageBounds.height += 2 * height * factor;
        propagateChange();
    }

    String toString(Iterable<Point2D.Double> g,
                    LinearAxis v1, DoubleUnaryOperator f1,
                    LinearAxis v2, DoubleUnaryOperator f2,
                    int sigFigs) {
        StringBuilder sb = new StringBuilder();
        String format = "%." + sigFigs + "g";
        for (Point2D.Double p: g) {
            p = transform(transform(p, v1, v2), f1, f2);
            sb.append(String.format(format, p.getX()) + ", "
                      + String.format(format, p.getY()) + '\n');
        }
        return sb.toString();
    }

    /** Return all chemical formulas converted to Hill order.
        Duplicates are removed. */
    @JsonIgnore public String[] getAllFormulas() {
        TreeSet<String> res = new TreeSet<>();
        for (String line: getAllText()) {
            for (ChemicalString.Match m: ChemicalString.embeddedFormulas(line)) {
                res.add(m.toString());
            }
        }
        return res.toArray(new String[0]);
    }

    LinearAxis getAxis(Side side) {
        switch (side) {
        case RIGHT:
            return getXAxis();
        case TOP:
            return getYAxis();
        case LEFT:
            return getLeftAxis();
        default:
            break;
        }
        return null;
    }

    /* Return true if this diagram has been marked as using weight
       percent coordinates. (That only matters for diagrams for which
       diagram components are defined.) */
    public boolean isUsingWeightFraction() {
        return usingWeightFraction;
    }

    /* Indicate whether this diagram uses weight percent coordinates.
       Note that calling setUsingWeightFraction() only claims that the
       existing coordinates are ALREADY defined using weight percents.
       If you want to convert from weight to mole percent or vice
       versa, call weightToMoleFraction or moleToWeightFraction. */
    public void setUsingWeightFraction(boolean b) {
        if (b != usingWeightFraction) {
            usingWeightFraction = b;
            propagateChange();
        }
    }

    public void setPercentageDisplay(Axis axis, boolean isPercentage) {
        if (!isPercentage) {
            axis.format = new DecimalFormat("0.0000");
        } else {
            axis.format = STANDARD_PERCENT_FORMAT;
        }
        for (LinearRuler r: rulers()) {
            if (r.axis == axis) {
                r.multiplier = isPercentage ? 100 : 1;
            }
        }
        fixAxisFormat(axis);
    }

    
    /** Don't display proportion values less than 0 or greater than 1,
        but allow a little bit of fudge factor. */
    boolean isProportion(double v) {
        return v >= -0.02 && v <= 1.02;
    }


    /** Return a pretty description of the given point that is
        specified in principal coordinates. */
    String principalToPrettyString(Point2D.Double prin) {
        if (prin == null) {
            return null;
        }

        StringBuilder status = new StringBuilder();

        Point2D.Double mole = isUsingWeightFraction()
            ? weightToMoleFraction(prin) : prin;
        Point2D.Double weight = isUsingWeightFraction()
            ? prin : moleToWeightFraction(mole);
        boolean haveBoth = mole != null && weight != null;

        String compound = molePercentToCompound(mole);
        if (compound != null) {
            status.append(ChemicalString.autoSubscript(compound) + ": ");
        }

        // If any single component value is out of bounds, then all
        // component values are invalid.
        boolean suppressComponents = false;
        for (LinearAxis axis: axes) {
            if (isComponentAxis(axis) && !isProportion(axis.applyAsDouble(prin))) {
                suppressComponents = true;
                break;
            }
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

            if ((isComponentAxis(axis) && suppressComponents)
                    || (axis.isPercentage() && !isProportion(axis.applyAsDouble(prin)))) {
                status.append("--");
            } else if (haveBoth && isComponentAxis(axis)) {
                status.append(withFraction(axis, mole));
                status.append("/");
                status.append(withFraction(axis, weight));
            } else {
                status.append(withFraction(axis, prin));
            }
        }

        return status.toString();
    }

    String withFraction(LinearAxis axis, Point2D.Double p) {
        String res = axis.applyAsString(p);

        if (axisIsFractional(axis)) {
            // Express values in fractional terms if the decimal
            // value is a close approximation to a fraction.
            double d = axis.applyAsDouble(p);
            ContinuedFraction f = approximateFraction(d);
            if (f != null && f.numerator != 0 && f.denominator > 1) {
                res = res + " (" + f + ")";
            }
        }
        return res;
    }

    @JsonIgnore public LinearAxis getXAxis() {
        // This program doesn't explicitly define the X- and Y-axes,
        // so this isn't as simple as it could be. However, it is
        // forbidden to delete the X or Y axis variables, so at least
        // we can be sure that X- and Y-axes exist.

        String name = diagramComponents[Side.RIGHT.ordinal()];
        if (name != null) {
            for (LinearAxis axis: axes) {
                if (name.equals(axis.name)) {
                    return axis;
                }
            }
        }

        LinearAxis res = null;
        for (LinearAxis axis: axes) {
            if (axis.isXAxis()) {
                if ("page X".equals(axis.name)) {
                    res = axis;
                } else {
                    return axis;
                }
            }
        }

        if (res == null) {
            throw new IllegalStateException("No X axis found.");
        }
        
        return res;
    }

    @JsonIgnore public LinearAxis getYAxis() {
        String name = diagramComponents[Side.TOP.ordinal()];
        if (name != null) {
            for (LinearAxis axis: axes) {
                if (name.equals(axis.name)) {
                    return axis;
                }
            }
        }

        LinearAxis res = null;
        for (LinearAxis axis: axes) {
            if (axis.isYAxis()) {
                if ("page Y".equals(axis.name)) {
                    res = axis;
                } else {
                    return axis;
                }
            }
        }

        if (res == null) {
            throw new IllegalStateException("No Y axis found.");
        }
        
        return res;
    }

    /** @return an array of all axes whose gradient on the page is
        horizontal to within numeric error limits. */
    @JsonIgnore public LinearAxis[] getPageXAxes() {
        return getPageAxes(1, 0);
    }

    /** @return an array of all axes whose gradient on the page is
        vertical to within numeric error limits. */
    @JsonIgnore public LinearAxis[] getPageYAxes() {
        return getPageAxes(0, 1);
    }

    LinearAxis[] getPageAxes(double dx, double dy) {
        assert(standardPageToPrincipal != null);
        Point2D.Double vec = new Point2D.Double(dx, dy);
        ArrayList<LinearAxis> res = new ArrayList<>();
        double maxSineSq = 1e-10;
        for (LinearAxis axis: axes) {
            if (Geom.sineSq(vec, pageGradient(axis)) < maxSineSq) {
                res.add(axis);
            }
        }
        LinearAxis[] res2 = res.toArray(new LinearAxis[0]);
        Arrays.sort(res2);
        
        // Put Page X / Page Y first if they exist.
        int i = -1;
        for (LinearAxis axis: res2) {
            ++i;
            if (((String) axis.name).startsWith("Page ")) {
                LinearAxis tmp = res2[0];
                res2[0] = axis;
                res2[i] = tmp;
                break;
            }
        }
        return res2;
    }

    public boolean isLeftAxis(LinearAxis axis) {
        return axis.equals(getLeftAxis());
    }

    public boolean isXAxis(LinearAxis axis) {
        return axis.equals(getXAxis());
    }

    public boolean isYAxis(LinearAxis axis) {
        return axis.equals(getYAxis());
    }

    /** Return true if this axis measures a diagram component
        concentration (in either weight or mole fraction) */
    boolean isComponentAxis(LinearAxis axis) {
        return isXAxis(axis) ? (diagramComponents[Side.RIGHT.ordinal()] != null)
            : isYAxis(axis) ? (diagramComponents[Side.TOP.ordinal()] != null)
            : isLeftAxis(axis) ? (diagramComponents[Side.LEFT.ordinal()] != null)
            : false;
    }

    @JsonIgnore public LinearAxis getLeftAxis() {
        String name = diagramComponents[Side.LEFT.ordinal()];
        if (name == null) {
            name = "Left";
        }

        for (LinearAxis axis: axes) {
            if (name.equals(axis.name)) {
                return axis;
            }
        }
        
        return null;
    }

    public String getDiagramComponent(Side side) {
        return diagramComponents[side.ordinal()];
    }

    public boolean axisIsFractional(LinearAxis axis) {
        return (isXAxis(axis) && getDiagramComponent(Side.RIGHT) != null)
            || (isYAxis(axis) && getDiagramComponent(Side.TOP) != null)
            || (isLeftAxis(axis) && getDiagramComponent(Side.LEFT) != null);
    }

    public void add(AnchoredLabel label) {
        addDecoration(label);
    }

    /** @param xWeight Used to determine how to justify rows of text. */
    View toView(String str, double xWeight, Color textColor) {
        return toView(str, xWeight, textColor, 0);
    }

    /** @param width If not zero, the value (in pixels) to specify for
        the width CSS attribute. */
    View toView(String str, double xWeight, Color textColor, double width) {
        String style
            = "<style type=\"text/css\"><!--"
            + " body { font-size: 100 pt; } "
            + " sub { font-size: 75%; } "
            + " sup { font-size: 75%; } "
            + " --></style>";

        str = NestedSubscripts.unicodify(str);

        StringBuilder sb = new StringBuilder("<html><head>");
        sb.append(style);
        sb.append("</head><body");
        if (width > 0) {
            sb.append(" style=\"width:" + width + "px\"");
        }
        sb.append(">");

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
        str = sb.toString();

        JLabel bogus = new JLabel(str);
        Font f = getFont();
        bogus.setFont(f);
        bogus.setForeground(thisOrBlack(textColor));
        return (View) bogus.getClientProperty("html");
    }

    public DiagramType getDiagramType() {
        return diagramType;
    }

    public void setDiagramType(DiagramType t) {
        this.diagramType = t;
        propagateChange();
    }

    double principalToPageAngle(double theta) {
        return Geom.transformRadians(principalToStandardPage, theta);
    }

    double pageToPrincipalAngle(double theta) {
        return Geom.transformRadians(standardPageToPrincipal, theta);
    }

    Point2D.Double pageGradient(LinearAxis axis) {
        return axis.gradient(standardPageToPrincipal);
    }

    static class HandleAndDistance implements Comparable<HandleAndDistance> {
        DecorationHandle handle;
        double distance;
        public HandleAndDistance(DecorationHandle de, double di) {
            handle = de;
            distance = di;
        }
        @Override public int compareTo(HandleAndDistance other) {
            return (distance < other.distance) ? -1
                : (distance > other.distance) ? 1 : 0;
        }

        @Override public String toString() {
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

        @Override public int compareTo(DecorationDistance other) {
            return (distance.distance < other.distance.distance) ? -1
                : (distance.distance > other.distance.distance) ? 1 : 0;
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + decoration + ", "
                + distance + "]";
        }
    }

    /** Remove the given handle. Return what should become the new
     * selection if the old selection is the handle that was removed
     * (this will be null if removing the handle means removing the
     * entire decoration) */
    public DecorationHandle removeHandle(DecorationHandle h) {
        Decoration d = h.getDecoration();
        if (!(d instanceof Interp2DDecoration)) {
            removeDecoration(d);
            return null;
        } else {
            return removeVertex((Interp2DHandle) h);
        }
    }

    void removeDecoration(Decoration d) {
        int layer = getLayer(d);
        if (layer < 0)
            throw new IllegalArgumentException("Decoration not found in list: " + d);
        removeDecoration(layer);
    }

    void removeDecorationIfFound(Decoration d) {
        int layer = getLayer(d);
        if (layer >= 0)
            removeDecoration(layer);
    }

    public void removeDecoration(int layer) {
        Decoration d = decorations.remove(layer);

        // Also remove decorations that require this decoration, and
        // if necessary, remove decorations that require *those*
        // decorations.
        ArrayList<Decoration> dependents = new ArrayList<>();
        for (Decoration d2: decorations) {
            for (Decoration rd: d2.requiredDecorations()) {
                if (rd == d) {
                    dependents.add(d2);
                    break;
                }
            }
        }

        for (Decoration d2: dependents) {
            removeDecorationIfFound(d2);
        }
        propagateChange();
    }

    /** @return a list of DecorationHandles such that moving all of
        those handles will move every decoration in the diagram. */
    ArrayList<DecorationHandle> movementHandles() {
        ArrayList<DecorationHandle> res = new ArrayList<>();
        for (Decoration d: getDecorations()) {
            for (DecorationHandle h: d.getHandles(DecorationHandle.Type.MOVE)) {
                res.add(h);
            }
        }
        return res;
    }

    public double pagePerimeter() {
        return 2 * (pageBounds.width + pageBounds.height);
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
            for (DecorationHandle h: d.getHandles(DecorationHandle.Type.SELECTION)) {
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

        ArrayList<DecorationHandle> res = new ArrayList<>();
        for (HandleAndDistance h: hads) {
            res.add(h.handle);
        }

        return res;
    }

    /** Like nearestHandles(p), but return only handles of the given type.

        @param oneHandleOnly If true, return only the closest handle. */
    @SuppressWarnings("unchecked")
    <T> ArrayList<T> nearestHandles(Point2D.Double p, T handleType, boolean oneHandleOnly) {
        Point2D.Double pagePoint = principalToStandardPage.transform(p);

        ArrayList<HandleAndDistance> hads = new ArrayList<>();
        Class<? extends Object> c = handleType.getClass();
        for (Decoration d: getDecorations()) {
            HandleAndDistance nearestHandle = null;
            for (DecorationHandle h: d.getHandles(DecorationHandle.Type.SELECTION)) {
                if (!c.isInstance(h)) {
                    continue;
                }
                Point2D.Double p2 = h.getLocation();
                p2 = principalToStandardPage.transform(p2);
                HandleAndDistance had = new HandleAndDistance
                    (h, pagePoint.distanceSq(p2));

                if (oneHandleOnly) {
                    if (nearestHandle == null
                        || had.distance < nearestHandle.distance) {
                        nearestHandle = had;
                    }
                } else {
                    hads.add(had);
                }
            }
            if (nearestHandle != null) {
                hads.add(nearestHandle);
            }
        }

        Collections.sort(hads);

        ArrayList<T> res = new ArrayList<>();
        for (HandleAndDistance h: hads) {
            res.add((T) h.handle);
        }

        return res;
    }

    /** @return a list of all key points in the diagram. Key points
        that are not decoration handles are cast to type
        NullDecorationHandle just to wrap the Point (hacky). Some
        duplication is likely.

        @param includeSmoothingPoints If true, include smoothed
        internal control points. If false, exclude such points.

 */
    public ArrayList<DecorationHandle> keyPointHandles(
            DecorationHandle.Type type) {
        ArrayList<DecorationHandle> res = new ArrayList<>();
        for (Point2D.Double p: intersections()) {
            res.add(new NullDecorationHandle(p));
        }

        for (Point2D.Double p: principalToStandardPage.getInputVertices()) {
            res.add(new NullDecorationHandle(p));
        }

        res.addAll(getDecorationHandles(type));
        // Add all segment midpoints.
        for (Line2D.Double s: getLineSegments()) {
            res.add(new NullDecorationHandle
                    ((s.getX1() + s.getX2()) / 2,
                     (s.getY1() + s.getY2()) / 2));
        }

        return res;
    }

    public ArrayList<Point2D.Double> keyPoints(DecorationHandle.Type type) {
        ArrayList<Point2D.Double> res = new ArrayList<>();
        for (DecorationHandle h: keyPointHandles(type)) {
            res.add(h.getLocation());
        }
        return res;
    }

    /** @return a list of all possible selections. Note that
        modifications to the output will affect this object. */
    @JsonProperty("decorations") ArrayList<Decoration> getDecorations() {
        return decorations;
    }

    @JsonProperty("decorations") void setJSONDecorations(Decoration[] objects) {
        decorations = new ArrayList<>(Arrays.asList(objects));
        if (!axes.isEmpty()) {
            linkAxesAndRulers();
        }
    }

    void linkAxesAndRulers() {
        for (LinearRuler r: rulers()) {
            if (r.axis != null) {
                continue;
            }
            String name = r.axisName;
            if (name == null) {
                throw new IllegalStateException
                    ("No axis name assigned yet for " + r);
            }
            for (LinearAxis axis: axes) {
                if (axis.name.equals(name)) {
                    r.axis = axis;
                    r.axisName = null;
                    break;
                }
            }
            if (r.axisName != null) {
                throw new IllegalStateException
                    ("Unknown axis name '" + r.axisName + "'");
            }
        }
    }

    /** @return all decoration handles. */
    @JsonIgnore ArrayList<DecorationHandle> getDecorationHandles(
            DecorationHandle.Type type) {
        ArrayList<DecorationHandle> res = new ArrayList<>();

        for (Decoration selectable: getDecorations()) {
            res.addAll(Arrays.asList(selectable.getHandles(type)));
        }
        return res;
    }


    /** @return all point intersections involves curves and/or line
        segments. */
    List<Point2D.Double> intersections() {
        ArrayList<Point2D.Double> res = new ArrayList<>();
        Line2D.Double[] segs = getLineSegments();
        BoundedParam2D[] straights = getStraightSegments();
        BoundedParam2D[] curves = getCurvedSegments();

        int cs = curves.length;
        for (int i = 0; i < cs; ++i) {
            BoundedParam2D curve = curves[i];
            for (Line2D segment: segs) {
                for (double t: curve.segIntersections(segment)) {
                    res.add(curve.getLocation(t));
                }
            }
            
            for (int j = i+1; j < cs; ++j) {
                BoundedParam2D curve2 = curves[j];
                try {
                    for (Point2D.Double p: BoundedParam2Ds.intersections
                             (curve, curve2, 1e-9, 80)) {
                        res.add(p);
                    }
                } catch (FailedToConvergeException x) {
                    System.err.println(x);
                    // That's OK.
                }
            }
        }

        for (BoundedParam2D straight: straights) {
            for (Line2D segment: segs) {
                for (double t: straight.segIntersections(segment)) {
                    res.add(straight.getLocation(t));
                }
            }
        }

        return res.stream().map(p -> standardPageToPrincipal.transform(p)).collect(Collectors.toList());
    }

    /** @return an array of all curved segments defined for this
        diagram in page coordinates. */
    @JsonIgnore public BoundedParam2D[] getCurvedSegments() {
        ArrayList<BoundedParam2D> res = new ArrayList<>();

        for (Decoration d: getDecorations()) {
            BoundedParam2D bp = getStandardPageParameterization(d);
            if (bp == null)
                continue;
            for (BoundedParam2D ls: bp.curvedSegments()) {
                res.add(ls);
            }
        }

        return res.toArray(new BoundedParam2D[0]);
    }

    /** @return an array of all curved segments defined for this
        diagram in page coordinates. */
    @JsonIgnore public BoundedParam2D[] getStraightSegments() {
        ArrayList<BoundedParam2D> res = new ArrayList<>();

        for (Decoration d: getDecorations()) {
            BoundedParam2D bp = getStandardPageParameterization(d);
            if (bp == null)
                continue;
            for (BoundedParam2D ls: bp.straightSegments()) {
                res.add(ls);
            }
        }

        return res.toArray(new BoundedParam2D[0]);
    }

    /** @return an array of all straight line segments defined for
        this diagram in page coordinates. */
    @JsonIgnore public Line2D.Double[] getLineSegments() {
        BoundedParam2D[] segments = getStraightSegments();
        Line2D.Double[] res = new Line2D.Double[segments.length];
        for (int i = 0; i < segments.length; ++i) {
            BoundedParam2D bp = segments[i];
            res[i] = new Line2D.Double(bp.getStart(), bp.getEnd());
        }
        return res;
    }

    /* Return the DecorationDistance for the curve or ruler whose
       outline comes closest to pagePoint. This routine operates
       entirely in standard page space, both internally and in terms
       of the input and output values. */
    DecorationDistance nearestCurve(Point2D pagePoint) {
        ArrayList<Decoration> decs = new ArrayList<>();
        ArrayList<BoundedParam2D> params = new ArrayList<>();
        for (Decoration dec: getDecorations()) {
            BoundedParam2D b = getStandardPageParameterization(dec);
            if (b != null) {
                if (b.getMinT() == b.getMaxT()) {
                    // That's a point, not a curve.
                    continue;
                }
                decs.add(dec);
                params.add(b);
            }
        }

        if (params.size() == 0) {
            return null;
        }

        OffsetParam2D.DistanceIndex di
            = OffsetParam2D.distance(params, pagePoint, 1e-6, 2000);
        return new DecorationDistance(decs.get(di.index), di.distance);
    }

    /** Toggle the closed/open status of curve #pathNo. Throws
        IllegalArgumentException if that curve is filled, since you
        can't turn off closure for a filled curve. */
    public void toggleCurveClosure(CurveClosable dec)
        throws IllegalArgumentException {
        dec.setClosed(!dec.isClosed());
        propagateChange();
    }

    /** principalToStandardPage shifted to put the pageBounds corner
        at (0,0). */
    Affine getPrincipalToAlignedPage() {
        Affine xform = new Affine
            (AffineTransform.getTranslateInstance(-pageBounds.x, -pageBounds.y));
        xform.concatenate(principalToStandardPage);
        return xform;
    }

    /** @return the name of the image file that this diagram was
        digitized from converted to an absolute path, or null if this
        diagram is not known to be digitized from a file. */
    @JsonIgnore public String getAbsoluteOriginalFilename() {
        if (filename == null || originalFilename == null) {
            return originalFilename;
        }

        Path op = Paths.get(originalFilename);
        if (op.isAbsolute()) {
            return originalFilename;
        }

        // Convert originalFilename from a relative path starting from
        // the directory that filename belongs to into an absolute
        // path.

        Path absolute = Paths.get(Paths.get(filename).getParent().toString(),
                                  originalFilename).toAbsolutePath();
        try {
            return absolute.toRealPath().toString();
        } catch (IOException x) {
            return absolute.toString(); // Settle for the unreal path.
        }
    }

    /** @return the name of the image file that this diagram was
        digitized from, or null if this diagram is not known to be
        digitized from a file. */
    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setTitle(String title) {
        put("title", title);
    }

    @JsonIgnore public String getTitle() {
        return get("title");
    }

    public String removeTitle() {
        return removeKey("title");
    }

    /** Like getTitle, but make up a title if no official title has been assigned. */
    @JsonIgnore public String getProvisionalTitle() {
        String titleStr = getTitle();
        if (titleStr != null) {
            return titleStr;
        }

        StringBuilder titleBuf = new StringBuilder();
        if (diagramType != null) {
            titleBuf.append(diagramType);
        }

        String str = systemName();
        if (str != null) {
            if (titleBuf.length() > 0) {
                titleBuf.append(" ");
            }
            titleBuf.append(str);
        }

        str = getFilename();
        if (str != null) {
            str = Paths.get(str).getFileName().toString();
            if (titleBuf.length() > 0) {
                titleBuf.append(" ");
            }
            titleBuf.append(str);
        }

        str = getOriginalFilename();
        if (str != null) {
            str = Paths.get(str).getFileName().toString();
            if (titleBuf.length() > 0) {
                titleBuf.append(" ");
            }
            titleBuf.append(str);
        }

        return titleBuf.length() > 0 ? titleBuf.toString() : fallbackTitle();
    }

    String fallbackTitle() {
        return "PED Editor";
    }

    /** @return the system name if known, with components sorted into
        alphabetical order, or null otherwise.

        This might not be an actual system name if the diagram
        components are not principal components, but whatever. */
    public String systemName() {
        Side[] sides = null;
        if (diagramType == null) {
            return null;
        }

        if (isTernary()) {
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
        if (filename == null) {
            setBackgroundType(BackgroundImageType.NONE);
        }
        originalFilename = filename;
        relativizeOriginalFilename();
        propagateChange();
    }

    /** Remove the association between this diagram and the original
        image, including the originalToPrincipal transform. */
    public void detachOriginalImage() {
        originalToPrincipal = null;
        principalToOriginal = null;
        setOriginalFilename(null);
        originalImage = null;
        originalImageBytes = null;
        propagateChange();
    }

    public void setFilename(String fn) {
        if ((filename == null && fn == null)
            || (filename != null && filename.equals(fn))) {
            return;
        }
        String ofn = getAbsoluteOriginalFilename();
        filename = fn;
        setOriginalFilename(ofn);
        propagateChange();
    }

    /** Convert originalFilename to be relative to filename if
        possible. */
    void relativizeOriginalFilename() {
        if (filename == null || originalFilename == null) {
            return;
        }
        originalFilename = relativizeFilename(getAbsoluteOriginalFilename());
    }

    /** @return fn converted to be relative to the directory that
        contains getFilename(). For example, if
        getFilename.equals("a/b/c") and fn.equals("a/x/y") then
        "../x/y" would be returned. */
    public String relativizeFilename(String fn) {
        if (fn == null || filename == null) {
            return fn;
        }
        Path op = Paths.get(fn).toAbsolutePath();
        Path p = Paths.get(filename).toAbsolutePath();
        int pnc = p.getNameCount();
        int opnc = op.getNameCount();
        Path proot = p.getRoot();
        Path oproot = op.getRoot();

        if (proot != null || oproot != null) {
            if (!proot.equals(oproot)) {
                return fn;
            }
        }

        int commonCnt;
        for (commonCnt = 0;
             commonCnt < pnc-1 && commonCnt < opnc - 1;
             ++commonCnt) {
            if (!p.getName(commonCnt).equals(op.getName(commonCnt))) {
                break;
            }
        }
        int parentCnt = pnc - 1 - commonCnt;
        if (parentCnt == 0 || (commonCnt > 0 && parentCnt <= 3)) {
            ArrayList<String> names = new ArrayList<>();
            for (int i = commonCnt; i < pnc-1; ++i) {
                names.add("..");
            }
            names.add(op.subpath(commonCnt, opnc).toString());
            fn = Paths.get(names.get(0),
                           names.subList(1, names.size())
                           .toArray(new String[0]))
                .toString();
        }
        return fn;
    }

    protected static double normalFontSize() {
        return STANDARD_FONT_SIZE / BASE_SCALE;
    }

    protected static double normalRulerFontSize() {
        return STANDARD_RULER_FONT_SIZE / BASE_SCALE;
    }

    LinearAxis createLeftAxis() {
        LinearAxis axis = new LinearAxis
                (STANDARD_PERCENT_FORMAT,
                 -1.0,
                 isTernary() ? -1.0 : 0.0,
                 1.0);
        String name = diagramComponents[Side.LEFT.ordinal()];
        axis.name = (name == null) ? "Left" : name;
        return axis;
    }

    LinearAxis defaultAxis(Side side) {
        if (isTernary()) {
            NumberFormat format = STANDARD_PERCENT_FORMAT;
            LinearAxis axis;

            switch (side) {
            case RIGHT:
                axis = LinearAxis.createXAxis(format);
                axis.name = "Right";
                return axis;
            case LEFT:
                return createLeftAxis();
            case TOP:
                axis = LinearAxis.createYAxis(format);
                axis.name = "Top";
                return axis;
            default:
                return null;
            }
        } else {
            NumberFormat format = new DecimalFormat("0.0000");
            switch (side) {
            case LEFT:
                return createLeftAxis();
            case RIGHT:
                return LinearAxis.createXAxis(format);
            case TOP:
                return LinearAxis.createYAxis(format);
            default:
                return null;
            }
        }
    }

    public void setPrincipalToStandardPage(AffinePolygonTransform xform) {
        principalToStandardPage = xform;
        if (xform == null) {
            standardPageToPrincipal = null;
        } else {
            try {
                standardPageToPrincipal = principalToStandardPage.createInverse();
            } catch (NoninvertibleTransformException e) {
                System.err.println("Transform " + xform + " is not invertible");
                System.exit(2);
            }
        }
    }

    protected void initializeDiagram() {
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
        fixAxisFormats();
    }

    /** Invoked from the EditFrame menu */
    public void setAspectRatio(double aspectRatio) {
        Rectangle2D.Double bounds = principalToStandardPage.outputBounds();

        double oldValue = ((double) bounds.width) / bounds.height;
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
        propagateChange();
    }

    public double getMargin(Side side) {
        Rectangle2D.Double bounds = principalToStandardPage.outputBounds();

        switch (side) {
        case LEFT:
            return -pageBounds.x;
        case RIGHT:
            return pageBounds.getMaxX() - bounds.getMaxX();
        case TOP:
            return -pageBounds.y;
        case BOTTOM:
            return pageBounds.getMaxY() - bounds.getMaxY();
        }

        return 0;
    }


    /** Invoked from the EditFrame menu */
    public void setMargin(Side side, double margin) {
        double delta = margin - getMargin(side);

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

        setPageBounds(pageBounds);
    }

    static Diagram loadFrom(File file) throws IOException {
        Diagram res;

        try {
            ObjectMapper mapper = getObjectMapper();
            res = (Diagram) mapper.readValue(file, Diagram.class);
        } catch (Exception e) {
            throw new IOException("File load error: " + e);
        }

        res.setFilename(file.getAbsolutePath());
        res.finishDeserialization();
        return res;
    }

    static Diagram loadFrom(String jsonString) throws IOException {
        Diagram res;

        try {
            ObjectMapper mapper = getObjectMapper();
            res = (Diagram) mapper.readValue(jsonString, Diagram.class);
        } catch (Exception e) {
            throw new IOException("File load error: " + e);
        }

        res.finishDeserialization();
        return res;
    }

    static Diagram loadFrom(InputStream is) throws IOException {
        Diagram res;

        try {
            ObjectMapper mapper = getObjectMapper();
            res = (Diagram) mapper.readValue(is, Diagram.class);
        } catch (Exception e) {
            throw new IOException("File load error: " + e);
        }

        res.finishDeserialization();
        return res;
    }

    /** Final setup steps to be taken after a Diagram in JSON format
     * has been deserialized. */
    void finishDeserialization() {
        try {
            standardPageToPrincipal = principalToStandardPage.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }
        
        for (TieLine tie: tieLines()) {
            tie.innerEdge = idToCurve(tie.innerId);
            tie.outerEdge = idToCurve(tie.outerId);
        }
        if (pageBounds == null) {
            computeMargins();
        }
        setSaveNeeded(false);
    }

    /** Invoked from the EditFrame menu */
    public void openDiagram(File file) throws IOException {
        copyFrom(loadFrom(file));
    }

    public void openDiagram(String jsonString) throws IOException {
        copyFrom(loadFrom(jsonString));
    }

    public void openDiagram(InputStream is) throws IOException {
        copyFrom(loadFrom(is));
    }

    void copyFrom(Diagram d) throws IOException {
        setSaveNeeded(false);
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                clear();
                cannibalize(d);
            }
        propagateChange1();
    }

    public Rectangle2D.Double getPageBounds() {
        if (pageBounds == null) {
            return null;
        }
        return (Rectangle2D.Double) pageBounds.clone();
    }

    public void setPageBounds(Rectangle2D rect) {
        pageBounds = Geom.createRectangle2DDouble(rect);
        propagateChange();
    }

    /** Set the page size just big enough so everything fits. */
    public void computeMargins() {
        computeMargins(false);
    }

    /** Set the page size just big enough so everything fits.

        @param onlyExpand If false. eliminate all excess white space.
        If true, do not decrease the diagram size.
    */
    public void computeMargins(boolean onlyExpand) {
        if (pageBounds == null) {
            setPageBounds(new Rectangle2D.Double(0, 0, 1, 1));
            onlyExpand = false;
        }
        MeteredGraphics mg = new MeteredGraphics();
        double mscale = 10000;
        paintDiagram(mg, mscale, null, false);
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
        bounds = addMargins(bounds, defaultRelativeMargin());
        if (onlyExpand) {
            bounds.add(pageBounds);
        }
        setPageBounds(bounds);
    }

    /** Return the default relative margin to use with auto-fit margins and
        cropToSelection(). */
    public double defaultRelativeMargin() {
        return isPixelMode() ? 0 : (1.0 / 400);
    }

    /** Return the bounds of d on the standard page. */
    public Rectangle2D.Double bounds(Decoration d) {
        if (pageBounds == null) {
            setPageBounds(new Rectangle2D.Double(0, 0, 1, 1));
        }
        MeteredGraphics mg = new MeteredGraphics();
        double mscale = 10000;
        draw(mg, d, mscale);
        Rectangle2D.Double bounds = mg.getBounds();
        if (bounds == null) {
            return null;
        }
        bounds.x /= mscale;
        bounds.y /= mscale;
        bounds.width /= mscale;
        bounds.height /= mscale;
        bounds.x += pageBounds.x;
        bounds.y += pageBounds.y;
        return bounds;
    }

    /** Return a copy of r with a margin added on all sides that
        equals relativeMargin times the bounding rectangle's
        perimeter. */
    public static Rectangle2D.Double addMargins(Rectangle2D r,
                                                double relativeMargin) {
        double margin = (r.getWidth() + r.getHeight()) * 2 * relativeMargin;
        return new Rectangle2D.Double
            (r.getX() - margin, r.getY() - margin,
             r.getWidth() + 2 * margin, r.getHeight() + 2 * margin);
    }

    /** Copy non-transient data fields from other. Afterwards, it is
        unsafe to modify other, because the modifications may affect
        this as well. In other words, this is a shallow copy that
        destroys other. */
    void cannibalize(Diagram other) {
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                diagramType = other.diagramType;
                diagramComponents = other.diagramComponents;
                originalToPrincipal = other.originalToPrincipal;
                setPrincipalToStandardPage(other.principalToStandardPage);
                pageBounds = other.pageBounds;
                originalFilename = other.originalFilename;
                filename = other.filename;

                boolean haveBounds = (pageBounds != null);
                if (!haveBounds) {
                    pageBounds = new Rectangle2D.Double(0,0,1,1);
                }
                initializeDiagram();
                decorations = other.decorations;
                other.decorations = null;
                setFontName(other.getFontName());
                axes = other.axes;
                componentElements = null;
                setTags(other.getTags());
                setKeyValues(other.getKeyValues());
                if (!haveBounds) {
                    pageBounds = null;
                }
                setPixelMode(other.isPixelMode());
                setUsingWeightFraction(other.isUsingWeightFraction());
                setBackgroundType(other.getBackgroundType());
                originalImageBytes = other.originalImageBytes;
                other.originalImageBytes = null;
            }
        propagateChange1();
    }

    @JsonProperty("axes") ArrayList<LinearAxis> getAxes() {
        return axes;
    }

    /** Populate the Diagram object's "rulers" field from the
        individual axes' "rulers" fields, and set the individual axes'
        "rulers" fields to null. */
    @JsonProperty("axes") void setAxes(ArrayList<LinearAxis> axes) {
        for (LinearAxis axis: axes) {
            add(axis);
        }
        if (!decorations.isEmpty()) {
            linkAxesAndRulers();
        }
    }

    /** Return a Dimension whose proportions match the diagram's and
        which is not larger than width x height. */
    public Dimension bestFitSize(int width, int height) {
        Rescale r = new Rescale
            (pageBounds.width, 0, width,
             pageBounds.height, 0, height);
        return new Dimension((int) (r.width + 0.5), (int) (r.height + 0.5));
    }

    /** Return a BufferedImage of the diagram which is no larger than
        width x height. */
    public BufferedImage createImage(int width, int height) {
        return createImage(width, height, 0, false);
    }

    /** Return a BufferedImage of the diagram which is no larger than
        width x height. */
    public BufferedImage createImage(int width, int height,
            double alpha, boolean transparent) {
        if (width == 0 || height == 0) {
            throw new IllegalArgumentException(
                    "Cannot make image with width " + width
                    + " and height " + height);
        }
        Dimension size = bestFitSize(width, height);
        width = size.width;
        height = size.height;
        
        // Images in the editor are normally displayed without font
        // hinting, but saving at low resolution can cause font
        // hinting to significantly rearrange the positions of letters
        // in labels, which can be confusing. Writing the image at a
        // larger scale and downscaling the result reduces the
        // problem.
        int scale = Math.max(1, 400 / (width + height));
        BufferedImage res = createImageSub(width * scale, height * scale,
                    alpha, transparent);
        if (scale > 1) {
            res = ScaleImage.downscale(res, scale);
        }
        return res;
    }

    BufferedImage createImageSub(int width, int height,
            double alpha, boolean transparent) {
        BufferedImage res = null;
        Color backColor = transparent ? new Color(0, 0, 0, 0) :
            Color.WHITE;
        int imageType = transparent ? BufferedImage.TYPE_INT_ARGB
            : BufferedImage.TYPE_INT_RGB;
        if (alpha > 0) {
            Dimension size = new Dimension(width, height);
            try {
                res = transformOriginalImage(
                        new Rectangle(size), bestFitScale(size), 
                        ImageTransform.DithererType.GOOD, alpha, backColor,
                        imageType);
                backColor = null;
            } catch (IOException x) {
                // OK, fall through
            }
        }
        if (res == null) {
            res = new BufferedImage(width, height, imageType);
        }
        Graphics2D g = (Graphics2D) res.getGraphics();
        paintDiagram(g, bestFitScale(new Dimension(width, height)),
                backColor, false);
        return res;
    }

    /** Return the minimum scale that does not waste screen real
        estate, or 0 if that is not defined. */
    double bestFitScale(Dimension size) {
        if (pageBounds == null || size.width < 0) {
            return 0;
        }

        Rescale r = new Rescale(pageBounds.width, 0, (double) size.width,
                                pageBounds.height, 0, (double) size.height);
        return r.t;
    }

    public void saveAsImage(File file, String format, int width, int height)
        throws IOException {
        saveAsImage(file, format, width, height, 0, false);
    }

    /** Save the diagram as an image.

       @param transparent If true and the format supports it, the returned image may be partly transparent. Otherwise the image will be as if painted on a white background.

       @param alpha If nonzero, then the original image (assuming it
       exists) will be shown in the background of the returned image,
       with the given alpha channel value (0 = don't show background
       image at all, 0.5 = show at half normal alpha value, 1 = show
       background image at full alpha value). */
    public void saveAsImage(File file, String format, int width, int height,
                            double alpha, boolean transparent) throws IOException {
        BufferedImage save = createImage(width, height, alpha, transparent);
        ImageIO.write(save, format, file);
    }

    /** Return true if the save was successful. */
    public boolean saveAsPED(Path path) throws IOException {
        return saveAsPED(path, true);
    }

    /** @param updateFilename If true, set the diagram's filename to
        correspond to the new path. If false, leave the filename
        alone. The 'false' option is useful during autosaves, which
        should not alter the file's real name.

        @return true if the save was successful.
    */
    public boolean saveAsPED(Path path, boolean updateFilename)
        throws IOException {
        if (!haveDiagram()) {
            return false;
        }
        String oldFilename = getFilename();
        try (PrintWriter writer = new PrintWriter
             (Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            if (updateFilename) {
                // Reset the filename before saving. This will
                // re-relativize originalFilename so that it can still
                // be found using a relative path even if the new
                // filename is in a different directory from before.
                setFilename(path.toAbsolutePath().toString());
            }
            writer.print(toJsonString());
            setSaveNeeded(false);
            return true;
        } catch (IOException x) {
            if (updateFilename) {
                // Revert to the old filename;
                setFilename(oldFilename);
            }
            throw x;
        }
    }

    /** @return this diagram as a JSON string. */
    @Override public String toString() {

        try {
            return Tabify.tabify(getObjectMapper().writeValueAsString(this));
        } catch (IOException e) {
            return super.toString();
        }
    }

    /** @return this diagram as a JSON string. */
    public String toJsonString() throws IOException {
        return Tabify.tabify(getObjectMapper().writeValueAsString(this));
    }

    /** Remove every decoration that has at least one handle for which
        principalToStandardPage.transform(handle.getLocation()) lies
        outside r. Return true if at least one decoration was
        removed. */
    public boolean crop(Rectangle2D r) {
        boolean res = false;
        boolean finished = false;
        while (!finished) {
            finished = true;
            for (DecorationHandle hand: movementHandles()) {
                Point2D page = principalToStandardPage.transform
                    (hand.getLocation());
                if (Geom.distanceSq(page, r) > 1e-12) {
                    System.err.println("Removing handle " + hand
                                       + " at " + Geom.toString(page)
                                       + " outside  " + Geom.toString(r) + ")");
                    removeDecoration(hand.getDecoration());
                    finished = false;
                    res = true;
                    break;
                }
            }
        }
        return res;
    }

    /** Return the plain text of the chemical-formula label closest to
        prin and no farther than maxPageDist away in standard page
        coordinates, or null if none is found. */
    public String nearestChemical(Point2D.Double prin,
                                  double maxPageDist) {
        double maxDistanceSq = maxPageDist * maxPageDist;
        Point2D.Double page = principalToStandardPage
            .transform(prin);

        for (LabelHandle hand: nearestHandles(prin, new LabelHandle(), false)) {
            if (!hand.isCentered()) {
                continue;
            }
            Point2D.Double handPage = principalToStandardPage.transform
                (hand.getLocation());
            if (handPage.distanceSq(page) > maxDistanceSq) {
                break;
            }
            String text = HtmlToText.htmlToText(hand.getDecoration().getText());
            ChemicalString.Match match = ChemicalString
                .maybeQuotedComposition(text);
            if (match != null) {
                return match.within(text).trim();
            }
        }

        return null;
    }

    /** These rulers are defined by principal coordinates. That means
        getRuler(Side.LEFT) might return the left axis of a ternary
        diagram, for instance. Also, the "BOTTOM" axis represents the
        one with the least principal-coordinates value, but that might
        in fact lie at the top of the screen. Similarly, the "LEFT"
        axis has the least principal coordinates, but it might lie to
        the right of the "RIGHT" one. Finally if only one horizontal
        ruler exists, then the BOTTOM and TOP versions will be the
        same thing.

        Returns null if a suitable axis was not found. */
    public LinearRuler getRuler(Side side) {
        double cx = 0;
        double cy = 0;
        switch (side) {
        case LEFT:
            cx = -1;
            break;
        case RIGHT:
            cx = 1;
            if (isTernary()) {
                // For typical ternary diagrams, right-side rulers are
                // oriented along a line such the that quantity of the
                // right component plus the quantity of the top
                // component is maximized for the diagram domain.
                cy = 1;
            }
            break;
        case TOP:
            cy = 1;
            break;
        case BOTTOM:
            cy = -1;
            break;
        }

        LinearRuler res = null;
        double maxv = 0;
        for (LinearRuler r: rulers()) {
            Point2D.Double s = r.startPoint;
            Point2D.Double e = r.endPoint;
            double v = s.x * cx + s.y * cy + e.x * cx + e.y * cy;
            if (res == null || v > maxv) {
                res = r;
                maxv = v;
            }
        }

        if (res == null) {
            return null;
        }

        // OK, we know which ruler lies furthest in the direction this
        // ruler is supposed to lie in, but if that ruler isn't
        // oriented correctly then we still have to return null.

        // A roughly correct orientation requires that the dot product
        // of the delta with the coefficient vector be a lot smaller
        // than the cross product.

        Point2D.Double ray = Geom.aMinusB(res.endPoint, res.startPoint);
        double dot = cx * ray.x + cy * ray.y;
        double cross = cx * ray.y - cy * ray.x;
        if (Math.abs(dot * 4) > Math.abs(cross)) {
            return null;
        }

        return res;
    }

    /** Look at labels within the diagram in an attempt to guess what
     * the name of the diagram component is. For ternary diagrams,
     * look for compounds close to the appropriate corner (left,
     * right, or top). For binary diagrams, look for compound names
     * close to the lower left or lower right. */
    public String guessComponent(Side side) {
        LinearRuler left = getRuler(Side.LEFT);
        LinearRuler right = getRuler(Side.RIGHT);
        LinearRuler bottom = getRuler(Side.BOTTOM);

        // Lower left ... upper right corners. For ternary diagrams,
        // only one of the upper-left or upper-right corners may
        // exist, or they may be the same point (simply the top of the
        // diagram).
        Point2D.Double ll = null;
        Point2D.Double lr = null;
        Point2D.Double ul = null;
        Point2D.Double ur = null;

        double lh = 0; // height of left ruler
        double rh = 0; // height of right ruler
        double bw = 0; // width of bottom ruler
        if (bottom != null) {
            bw = bottom.endPoint.x - bottom.startPoint.x;
            if (bw > 0) {
                ll = bottom.startPoint;
                lr = bottom.endPoint;
            } else {
                lr = bottom.startPoint;
                ll = bottom.endPoint;
            }
            bw = Math.abs(bw);
        }

        if (left != null) {
            lh = left.endPoint.y - left.startPoint.y;
            if (lh > 0) {
                ll = left.startPoint;
                ul = left.endPoint;
            } else {
                ul = left.startPoint;
                ll = left.endPoint;
            }
            lh = Math.abs(lh);
        }

        if (right != null) {
            rh = right.endPoint.y - right.startPoint.y;
            if (rh > 0) {
                lr = right.startPoint;
                ur = right.endPoint;
            } else {
                ur = right.startPoint;
                lr = right.endPoint;
            }
            rh = Math.abs(rh);
        }

        double diagonal = 0;
        if (ur != null && ll != null) {
            diagonal = principalToStandardPage.transform(ll)
                .distance(principalToStandardPage.transform(ur));
        } else if (ul != null && lr != null) {
            diagonal = principalToStandardPage.transform(ul)
                .distance(principalToStandardPage.transform(lr));
        } else {
            return null;
        }
        double maxDist = diagonal / 11;

        Point2D.Double target;
        double margin = 0.03;

        switch (side) {

        case LEFT:
            target = Geom.toPoint2DDouble(ll);
            if (target == null) {
                return null;
            }
            if (left != null) {
                target.y -= lh * margin;
            }
            if (bottom != null) {
                target.x -= bw * margin;
            }
            return nearestChemical(target, maxDist);

        case RIGHT:
            target = Geom.toPoint2DDouble(lr);
            if (target == null) {
                return null;
            }
            if (right != null) {
                target.y -= lh * margin;
            }
            if (bottom != null) {
                target.x += bw * margin;
            }
            return nearestChemical(target, maxDist);

        case TOP:

            Point2D.Double ltarget = null;
            String lchem = null;
            if (left != null) {
                ltarget = Geom.toPoint2DDouble(ul);
                ltarget.y += lh * margin;
                lchem = nearestChemical(ltarget, maxDist);
            }

            if (right == null) {
                return lchem;
            }

            Point2D.Double rtarget = Geom.toPoint2DDouble(ur);
            rtarget.y += rh * margin;
            rtarget.x -= rh * margin;
            String rchem = nearestChemical(rtarget, maxDist);

            if (left == null) {
                return rchem;
            }

            // For some bottom partial ternary diagrams, the top
            // component is labeled just beyond the ends of the left
            // and right axes. If this is the case, nearestChemical()
            // should return the same thing on both sides.

            if (lchem != null && lchem.equals(rchem)) {
                return lchem;
            }

            // For others, the top compoonent is labeled near the
            // midpoint between those two.

            // For full ternary diagrams, checking the two different
            // positions above is a waste of time, but it shouldn't
            // hurt anything.
            return nearestChemical(Geom.midpoint(ltarget, rtarget), maxDist);
            
        default:
            throw new IllegalArgumentException("No such component 'BOTTOM'");
        }
    }

    /** Fill in all undefined diagram components with best guesses.
        Return false if it appears that some components still need to
        be added, or true otherwise.

        @param force If false, do not change components that have
        already been assigned nontrivial names (names like "Left" and
        "Right" are considered trivial). */
    public boolean guessComponents(boolean force) {
        String code;
        if (diagramType == DiagramType.OTHER
            || (((code = get("diagram code")) != null)
                && (code.equals("Q") || code.equals("P")))) {
            // Types OTHER, QUATERNARY, and PRESSURE/TEMPERATURE do
            // not have diagram components. Note that types Q and P
            // relate only to an obsolete version of this editor, so
            // you won't find an explanation of these items anywhere
            // in this editor's source code.
            return true;
        }

        boolean res = true;
        Side[] sides = isTernary()
            ? new Side[] { Side.LEFT, Side. RIGHT, Side.TOP }
        : new Side [] { Side.LEFT, Side. RIGHT };

        for (Side side: sides) {
            int i = side.ordinal();
            String oldName = diagramComponents[i];
            boolean componentExisted = oldName != null;
            boolean ok = !force && componentExisted
                && !oldName.toLowerCase().equals(side.toString().toLowerCase());
            if (!ok) {
                String c = guessComponent(side);
                if (c == null) {
                    res = false;
                } else {
                    try {
                        setDiagramComponent(side, c);
                    } catch (DuplicateComponentException x) {
                        c = null;
                    }
                }
            }
        }
        return res;
    }

    /** Invoked from the EditFrame menu */
    public void print(PrinterJob job, PrintRequestAttributeSet aset)
        throws PrinterException {
        /* Mucking with aset is discouraged at this point, because
         * it's too late to give the user the chance to change those
         * settings in a dialog. */
        job.print(aset);
    }

    @Override public int print(Graphics g0, PageFormat pf, int pageIndex)
         throws PrinterException {
        if (pageIndex != 0 || principalToStandardPage == null) {
            return Printable.NO_SUCH_PAGE;
        }
        Graphics2D g = (Graphics2D) g0;

        AffineTransform oldTransform = g.getTransform();

        Rectangle2D.Double bounds
            = new Rectangle2D.Double
            (pf.getImageableX(), pf.getImageableY(),
             pf.getImageableWidth(), pf.getImageableHeight());

        String title = getTitle();
        double deltaY = 0;
        double titleY = 0;
        if (title != null) {
            Rectangle2D tbox = g.getFontMetrics().getStringBounds(title, g);
            deltaY = tbox.getHeight();
            titleY = -tbox.getY();
        }

        g.translate(bounds.getX(), bounds.getY() + deltaY);
        if (title != null) {
            g.drawString(title, 0, (int) Math.round(titleY - deltaY));
        }
        g.setFont(getFont());
        double scale = Math.min((bounds.height - deltaY) / pageBounds.height,
                                bounds.width / pageBounds.width);
        paintDiagram(g, scale, null, true);
        g.setTransform(oldTransform);

        return Printable.PAGE_EXISTS;
    }

    /** @param segment A line on the standard page

        Return one of the vectors (which, inconsistently, is defined in
        principal coordinates) that passes through segment.getP1() and
        that is roughly parallel to segment, or null if no such line
        is close enough to parallel. A grid line is a line of zero
        change for a defined axis (from the "axes" variable). */
    Line2D.Double nearestGridLine(Line2D.Double segment,
                                  List<Point2D.Double> vectors) {
        Point2D source = segment.getP1();
        Point2D dest = segment.getP2();
        Point2D.Double pageDelta = Geom.aMinusB(dest, source);
        double deltaLength = Geom.length(pageDelta);

        // Tolerance is the maximum ratio of the distance between dest
        // and the projection to deltaLength. TODO A smarter approach
        // might allow for both absolute and relative errors.
        double tolerance = 0.06;

        double maxDist = deltaLength * tolerance;
        double maxDistSq = maxDist * maxDist;
        Line2D.Double res = null;
        
        for (Point2D.Double v: vectors) {
            principalToStandardPage.deltaTransform(v, v);
            Point2D.Double projection = Geom.nearestPointOnLine
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
            double t = Geom.lineSegmentIntersectionT
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

    /** Apply the given transform to all curve vertices, all label
        locations, all arrow locations, and all ruler start- and
        endpoints. */
    public void transformPrincipalCoordinates(Function<? super Point2D.Double,
                                              ? extends Point2D> xform) {
        for (DecorationHandle hand: movementHandles()) {
            hand.move(xform.apply(hand.getLocation()));
        }
        propagateChange();
    }

    /** Apply the given transform to all curve vertices, all label
        locations, all arrow locations, and all ruler start- and
        endpoints. */
    public void transformPrincipalCoordinates(AffineTransform trans) {
        for (CuspFigure path: paths()) {
            path.setCurve(path.getCurve().createTransformed(trans));
        }

        Point2D.Double tmp = new Point2D.Double();

        for (AnchoredLabel label: labels()) {
            tmp.x = label.x;
            tmp.y = label.y;
            trans.transform(tmp, tmp);
            label.x = tmp.x;
            label.y = tmp.y;
        }

        for (Arrow arrow: arrows()) {
            tmp.x = arrow.x;
            tmp.y = arrow.y;
            trans.transform(tmp, tmp);
            arrow.x = tmp.x;
            arrow.y = tmp.y;
        }

        for (LinearRuler ruler: rulers()) {
            trans.transform(ruler.startPoint, ruler.startPoint);
            trans.transform(ruler.endPoint, ruler.endPoint);
        }

        propagateChange();
    }

    /** Apply the given transform to all coordinates defined in
        principal coordinates, but apply corresponding and inverse
        transformations to all transforms to and from principal
        coordinates, with one exception: leave the x-, y-, and (for
        ternary diagrams) left axis alone. So the diagram looks the
        same as before except for axis ticks and status bar coordinate
        values for those two or three axes. For example, one might use
        this method to convert a binary diagram's y-axis from one
        temperature scale to another, or from the default range 0-1 to
        the range you really want. */
    public void invisiblyTransformPrincipalCoordinates(AffineTransform trans) {
        transformPrincipalCoordinates(trans);
        ArrayList<LinearAxis> mainAxes = new ArrayList<>();
        mainAxes.add(getXAxis());
        mainAxes.add(getYAxis());
        LinearAxis lax = getLeftAxis();
        if (lax != null) {
            mainAxes.add(lax);
        }

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
        for (Decoration d: decorations) {
            if (d instanceof Angled) {
                Angled a = (Angled) d;
                a.setAngle(principalToPageAngle(a.getAngle()));
            }
        }

        principalToStandardPage.concatenate(itrans);
        standardPageToPrincipal.preConcatenate(atrans);

        // Convert all axes from page to the new principal coordinates.
        for (LinearAxis axis: axes) {
            boolean skip = false;
            for (LinearAxis mainAxis: mainAxes) {
                if (axis == mainAxis) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            axis.concatenate(itrans);
        }

        for (Decoration d: decorations) {
            if (d instanceof Angled) {
                Angled a = (Angled) d;
                a.setAngle(pageToPrincipalAngle(a.getAngle()));
            }
        }

        fixAxisFormats();
    }

    /** This is crude and untested, and I'm pretty sure it can fail
        when two points get mapped to the same target, but even so, it's 
        sometimes useful. */
    public void snapToGrid() {
        ArrayList<Double> xs = new ArrayList<>();
        ArrayList<Double> ys = new ArrayList<>();
        for (DecorationHandle hand: movementHandles()) {
            Point2D p = hand.getLocation();
            xs.add(p.getX());
            ys.add(p.getY());
        }
        SnapToGrid snapX = new SnapToGrid(SnapToGrid.toDoubleArray(xs),
                                          length(getXAxis()) * 1e-8);
        SnapToGrid snapY = new SnapToGrid(SnapToGrid.toDoubleArray(ys),
                                          length(getYAxis()) * 1e-8);
        transformPrincipalCoordinates
            (p -> new Point2D.Double(snapX.snap(p.getX()), snapY.snap(p.getY())));
    }

    /** Make sure that the axis formats still make sense after a
        rescaling or change to the page bounds. */
    void fixAxisFormats() {
        for (Axis axis: getAxes()) {
            fixAxisFormat(axis);
        }
    }

    void fixAxisFormat(Axis axis) {
        double[] range = getRange(axis);
        boolean percentP = ((DecimalFormat) (axis.format)).getMultiplier() == 100;
        double max = Math.max(-range[0], range[1]);
        if (max == 0) {
            return;
        }
        if (percentP) {
            if (max < 0.0001) {
                axis.format = new DecimalFormat("0.000E0%");
            } else if (max < 0.1) {
                axis.format = new DecimalFormat("0.00000%");
            } else {
                axis.format = STANDARD_PERCENT_FORMAT;
            }
        } else {
            if (max < 0.01 || max > 1e7) {
                axis.format = new DecimalFormat("0.000E0");
            } else if (max < 1e4) {
                axis.format = new DecimalFormat("0.0000");
            } else {
                axis.format = new DecimalFormat("0");
            }
        }
        propagateChange();
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
        compounds with integer subscripts (that is, the components sum
        to 1), and point "prin" nearly equals a round fraction, then
        return the compound that "prin" represents. If it doesn't
        nearly equal a round fraction, then express the compound using
        floating point subscripts. */
    public String molePercentToCompound(Point2D.Double prin) {
        if (prin == null) {
            return null;
        }
        double[][] componentElements = getComponentElements();
        if (componentElements == null) {
            return null;
        }

        String[] diagramElements = getDiagramElements();

        SideDouble[] sds = componentFractions(prin);
        if (sds == null || sds.length == 0 || !componentsSumToOne(sds)) {
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

        boolean roundNumbers = true;
        long lcd = 1;
        for (double d: quantities) {
            ContinuedFraction f = approximateFraction(d);
            if (f == null) {
                roundNumbers = false;
                break;
            }
            fracs.add(f);
            try {
                lcd = ContinuedFraction.lcm(lcd, f.denominator);
            } catch (OverflowException e) {
                roundNumbers = false;
                break;
            }
        }

        StringBuilder res = new StringBuilder();
        if (roundNumbers) {
            // The formula subscripts should be the numerators of the mole
            // fractions when expressed using the common denominator,
            // divided by the greatest common factor of those numerators.

            long gcf = 0;
            for (int i = 0; i < eltCnt; ++i) {
                ContinuedFraction f = fracs.get(i);
                if (f.numerator == 0) {
                    continue;
                }
                long num = f.numerator * (lcd / f.denominator);
                gcf = (i == 0) ? num : ContinuedFraction.gcf(gcf, num);
            }

            for (int i = 0; i < eltCnt; ++i) {
                ContinuedFraction f = fracs.get(i);
                if (f.numerator == 0) {
                    continue;
                }
                long num = f.numerator * (lcd / f.denominator) / gcf;
                res.append(diagramElements[i]);
                if (num > 1) {
                    res.append(num);
                }
            }
        } else {
            // Express the formula in floating point.
            double denom = 0;
            for (double q: quantities) {
                denom += q;
            }
            for (int i = 0; i < eltCnt; ++i) {
                double q = quantities[i];
                if (q == 0) {
                    continue;
                }
                res.append(diagramElements[i]);
                res.append(String.format("%.3f", q/denom));
            }
        }
        return res.toString();
    }

    /** Draw a circle around each point in path. */
    void circleVertices(Graphics2D g, Interp2DDecoration cdec,
            double scale, boolean fill, double r) {
        for (Point2D.Double point: cdec.getCurve().getPoints()) {
            circleVertex(g, point, scale, fill, r);
        }
    }

    void circleVertex(Graphics2D g, Point2D.Double point, double scale,
                        boolean fill, double r) {
        Point2D.Double xpoint = new Point2D.Double();
        Affine p2d = principalToScaledPage(scale);

        Stroke oldStroke = g.getStroke();
        if (!fill) {
            g.setStroke(new BasicStroke((float) (r / 4)));
        }

        p2d.transform(point, xpoint);
        Shape shape = new Ellipse2D.Double
            (xpoint.x - r, xpoint.y - r, r * 2, r * 2);
        if (fill) {
            g.fill(shape);
        } else {
            g.draw(shape);
        }

        if (!fill) {
            g.setStroke(oldStroke);
        }
    }

    public void setLayer(Decoration d, int layer) {
        int oldLayer = getLayer(d);
        int cnt = decorations.size();
        if (oldLayer != -1) {
            decorations.remove(oldLayer);
        }
        if (layer < 0) {
            layer = 0;
        } else if (layer >= cnt) {
            layer = cnt - 1;
        }
        decorations.add(layer, d);
        propagateChange();
    }

    public int getLayer(Decoration d) {
        int i = -1;
        for (Decoration thisd: decorations) {
            ++i;
            if (d == thisd) {
                return i;
            }
        }
        return i;
    }

    static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT,
                                   true);
            objectMapper.setSerializationInclusion(Inclusion.NON_NULL);
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

            ser.addMixInAnnotations(Decoration.class,
                                    DecorationAnnotations.class);
            des.addMixInAnnotations(Decoration.class,
                                    DecorationAnnotations.class);
        }

        return objectMapper;
    }

    LinearRuler ternaryBottomRuler(double start /* Right */,
                                      double end /* Right */) {
        return ternaryBottomRuler(start, end, 0.0);
    }

    LinearRuler ternaryBottomRuler(double start, double end, double y) {
        LinearRuler r = defaultTernaryRuler();
        r.textAngle = 0;
        r.tickLeft = true;
        r.labelAnchor = LinearRuler.LabelAnchor.RIGHT;

        r.startPoint = new Point2D.Double(start, y);
        r.endPoint = new Point2D.Double(end, y);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        r.suppressStartTick = (diagramType != DiagramType.TERNARY_RIGHT)
            || (start < 1e-6);
        r.suppressEndTick = (diagramType != DiagramType.TERNARY_LEFT)
            || (end > 1 - 1e-6);
        r.axis = getXAxis();
        return r;
    }

    LinearRuler ternaryLeftRuler(double start /* Y */, double end /* Y */) {
        return ternaryLeftRuler(start, end, 0.0);
    }

    LinearRuler ternaryLeftRuler(double start, double end, double right) {
        LinearRuler r = defaultTernaryRuler();
        r.textAngle = Math.PI / 3;
        r.tickRight = true;
        r.labelAnchor = LinearRuler.LabelAnchor.LEFT;

        // Usual PED Data Center style leaves out the tick labels on
        // the left unless this is a top or left partial ternary
        // diagram.
        boolean showLabels = diagramType == DiagramType.TERNARY_LEFT
            || diagramType == DiagramType.TERNARY_TOP;
        if (showLabels) {
            r.labelAnchor = LinearRuler.LabelAnchor.LEFT;
            r.suppressStartLabel = diagramType != DiagramType.TERNARY_TOP;
            r.suppressEndLabel = diagramType != DiagramType.TERNARY_LEFT;
        } else {
            r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        }

        r.startPoint = new Point2D.Double(right, start);
        r.endPoint = new Point2D.Double(right, end);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        // The tick label for the bottom of the left ruler is
        // redundant with the tick label for the left end of the
        // bottom ruler unless this is a top partial ternary
        // diagram.
        r.suppressStartTick = (diagramType != DiagramType.TERNARY_TOP)
            || (start < 1e-6);
        r.suppressEndTick = (diagramType != DiagramType.TERNARY_LEFT)
            || (end > 1 - 1e-6);
        r.axis = getYAxis();
        return r;
    }

    LinearRuler ternaryRightRuler(double start /* Y */, double end /* Y */) {
        return ternaryRightRuler(start, end, 1.0 - Math.min(start, end));
    }

    LinearRuler ternaryRightRuler(double start, double end, double right) {
        LinearRuler r = defaultTernaryRuler();
        r.textAngle = Math.PI * 2 / 3;
        r.tickLeft = true;

        // The tick labels for the right ruler are redundant with the
        // tick labels for the left ruler unless this is a top or right
        // partial ternary diagram.
        boolean showLabels = diagramType == DiagramType.TERNARY_RIGHT
            || diagramType == DiagramType.TERNARY_TOP;
        if (showLabels) {
            r.labelAnchor = LinearRuler.LabelAnchor.RIGHT;
            r.suppressStartLabel = diagramType != DiagramType.TERNARY_TOP;
            r.suppressEndLabel = diagramType != DiagramType.TERNARY_RIGHT;
        } else {
            r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        }
        r.suppressStartTick = diagramType != DiagramType.TERNARY_TOP
            || (start < 1e-6);
        r.suppressEndTick = diagramType != DiagramType.TERNARY_RIGHT
            || (end > 1-1e-6);

        r.startPoint = new Point2D.Double(right, start);
        r.endPoint = new Point2D.Double(right - (end - start), end);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 1) > 1e-4);
        r.axis = getYAxis();
        return r;
    }

    LinearRuler binaryBottomRuler() {
        return binaryBottomRuler(0, 1, 0);
    }

    LinearRuler binaryBottomRuler(double left, double right, double y) {
        LinearRuler r = defaultBinaryRuler();
        r.textAngle = 0;
        r.tickLeft = true;
        r.labelAnchor = LinearRuler.LabelAnchor.RIGHT;
        r.startPoint = new Point2D.Double(left, y);
        r.endPoint = new Point2D.Double(right, y);
        r.axis = getXAxis();
        return r;
    }

    LinearRuler binaryTopRuler() {
        return binaryTopRuler(0, 1, 1);
    }

    LinearRuler binaryTopRuler(double left, double right, double y) {
        LinearRuler r = defaultBinaryRuler();
        r.textAngle = 0;
        r.tickRight = true;
        r.labelAnchor = LinearRuler.LabelAnchor.NONE;

        r.startPoint = new Point2D.Double(left, y);
        r.endPoint = new Point2D.Double(right, y);
        r.axis = getXAxis();
        return r;
    }

    LinearRuler binaryLeftRuler() {
        return binaryLeftRuler(0, 1, 0);
    }

    LinearRuler binaryLeftRuler(double bottom, double top, double x) {
        LinearRuler r = defaultBinaryRuler();
        r.textAngle = Math.PI / 2;
        r.tickRight = true;
        r.labelAnchor = LinearRuler.LabelAnchor.LEFT;

        r.startPoint = new Point2D.Double(x, bottom);
        r.endPoint = new Point2D.Double(x, top);
        r.axis = getYAxis();
        return r;
    }

    LinearRuler binaryRightRuler() {
        return binaryRightRuler(0, 1, 1);
    }

    LinearRuler binaryRightRuler(double bottom, double top, double x) {
        LinearRuler r = defaultBinaryRuler();
        r.textAngle = Math.PI / 2;
        r.tickLeft = true;
        r.labelAnchor = LinearRuler.LabelAnchor.NONE;

        r.startPoint = new Point2D.Double(x, bottom);
        r.endPoint = new Point2D.Double(x, top);
        r.axis = getYAxis();
        return r;
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
            if (Double.isNaN(w)) {
                return 0;
            }
            total += q * w;
        }
        return total;
    }

    static Color thisOrBlack(Color c) {
        return (c == null) ? Color.BLACK : c;
    }

    /** Return the X and Y range of the original central figure (such
        as the Cartesian graph inside a Binary diagram) intersected
        with the page range. */

    @JsonIgnore public Rectangle2D getPrincipalBounds() {
        double[] rangeX = getRange(LinearAxis.createXAxis());
        double[] rangeY = getRange(LinearAxis.createYAxis());
        Rectangle2D.Double pageBounds = new Rectangle2D.Double
            (rangeX[0], rangeY[0], rangeX[1] - rangeX[0], rangeY[1] - rangeY[0]);
        return principalToStandardPage.inputBounds().createIntersection
            (pageBounds);
    }

    /** If this returns false, then the diagram doesn't exist yet, and
        the edit window shows nothing but gray, though the Select
        Diagram window may be open and displaying an original image
        for digitization. */
    public boolean haveDiagram() {
        return principalToStandardPage != null;
    }

    public String getFontName() {
        return (embeddedFont == null) ? defaultFontName
            : getFont().getFontName();
    }

    /** Return the path to the font with the given name. */
    public String fontNameToFilename(String name) {
        return fontFiles.get(name);
    }

    /** Returns false if there is no effect because it's the same font
        as before and it was already loaded. */
    public boolean setFontName(String s) {
        if (embeddedFont != null && s.equals(getFontName())) {
            return false; // No change
        }

        String filename = fontFiles.get(s);
        if (filename == null) {
            throw new IllegalArgumentException("Unrecognized font name '" + s + "'");
        }
        embeddedFont = loadFont(filename, (float) STANDARD_FONT_SIZE);
        for (AnchoredLabel label: labels()) {
            label.setFont(embeddedFont);
        }
        propagateChange();
        return true;
    }

    /** Return the parameterization of obj (which is probably a
        Decoration or DecorationHandle) in standard page space if obj
        is an instance of TransformableParameterizable2D, or null
        otherwise. This transforms the object and then performs the
        parameterization, which may give a result different from
        obtaining the parameterization and transforming that.
    */
    BoundedParam2D getStandardPageParameterization(Object obj) {
        return (obj instanceof TransformableParameterizable2D)
            ? ((TransformableParameterizable2D) obj)
            .getParameterization(principalToStandardPage)
            : null;
    }

    /** Return the parameterization of obj (which is probably a
        Decoration or DecorationHandle) in principal space if obj is
        an instance of BoundedParameterizable2D, or null otherwise. */
    BoundedParam2D getPrincipalParameterization(Object obj) {
        return (obj instanceof BoundedParameterizable2D)
                ? ((BoundedParameterizable2D) obj).getParameterization()
                : null;
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

     /** Return the inverse transform from the (v0,v1) system to the
         principal coordinate system. Throws an exception if the
         inverse does not exist. */
    public Affine inverseTransform(LinearAxis v0, LinearAxis v1)
        throws NoninvertibleTransformException {
        Affine xform = new Affine
            (v0.getA(), v1.getA(),
             v0.getB(), v1.getB(),
             v0.getC(), v1.getC());

        return xform.createInverse();
    }

    /** Return true if the diagram has fixed aspect; that is, the
        length of a vector after transformation into screen
        coordinates does not depend on its orientation. */
    @JsonIgnore public boolean isFixedAspect() {
        if (principalToStandardPage == null) {
            return false;
        }
        Point2D.Double tmp = new Point2D.Double();
        tmp.x = 1.0;
        tmp.y = 0.0;
        double lenX = Geom.length
            (principalToStandardPage.deltaTransform(tmp, tmp));
        tmp.x = 0.0;
        tmp.y = 1.0;
        double lenY = Geom.length
            (principalToStandardPage.deltaTransform(tmp, tmp));
        return Math.abs((lenX - lenY) / lenY) < 1e-12;
    }

    /** Return the area in page space corresponding to 1 unit of area
        in principal space, times minus one (since graphs normally
        point up whereas page space points down.. */
    public double areaMultiplier() {
        Point2D.Double vx = new Point2D.Double(1, 0);
        principalToStandardPage.deltaTransform(vx, vx);
        Point2D.Double vy = new Point2D.Double(0, 1);
        principalToStandardPage.deltaTransform(vy, vy);
        return -Geom.crossProduct(vx, vy);
    }

    /** Return the length in page space corresponding to 1 unit of length
        in principal space. */
    public double lengthMultiplier() {
        if (!isFixedAspect()) {
            // Arc length is only defined for fixed aspect diagrams.
            return Double.NaN;
        }
        Point2D.Double vx = new Point2D.Double(1, 0);
        principalToStandardPage.deltaTransform(vx, vx);
        return Geom.length(vx);
    }

    @JsonIgnore public BufferedImage getOriginalImage() throws IOException {
        if (originalImage != null) {
            return originalImage;
        }

        byte[] imageBytes = getOriginalImageBytesUnsafe();
        if (imageBytes == null) {
            return null;
        }

        originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        return originalImage;
    }

    public BackgroundImageType getBackgroundType() {
        return backgroundType;
    }

    public synchronized void setBackgroundType(BackgroundImageType value) {
        if (value == null) {
            value = BackgroundImageType.NONE;
        }
        backgroundType = value;
    }

    /** @return the binary contents of the original image. Changing
        the array contents is not safe. */
    @JsonProperty("originalImageBytes")
    protected byte[] getOriginalImageBytesUnsafe() throws IOException {
        if (!triedToLoadOriginalImage && originalImageBytes == null) {
            triedToLoadOriginalImage = true;
        
            String filename = getAbsoluteOriginalFilename();
            if (filename == null) {
                return null;
            }

            originalImageBytes = Files.readAllBytes(Paths.get(filename));
            propagateChange();
        }

        return originalImageBytes;
    }

    /** The input array is stolen, not copied -- hence "unsafe". */ 
    @JsonProperty("originalImageBytes")
    protected void setOriginalImageBytesUnsafe(byte[] bytes) {
        originalImageBytes = bytes;
    }

    /** Scale the diagram by the given amount, placing the upper-left
        corner in position (0,0), but don't actually draw the diagram.
        Instead, just return the portion of originalImage, adjusted to
        the current background image alpha (mixed with a background of
        pure white), that would sit in the background of the cropRect
        portion of the scaled diagram.

        TODO: the way fade() works here is doubtful. It makes more
        sense to use an RGBA image type than to use RGB and mix the
        color with pure white according to the alpha value. Only real
        RGBA allows bitmaps to be layered in nontrivial ways.
    */
    synchronized BufferedImage transformOriginalImage(
            Rectangle cropBounds, double scale,
            ImageTransform.DithererType dither, double alpha,
            Color backColor, int imageType) throws IOException {
        BufferedImage input = null;
        if (alpha > 0) {
            try {
                input = getOriginalImage();
            } catch (IOException x) {
                // Oh well, treat original image as blank.
            }
        }

        if (input == null) {
            BufferedImage res = new BufferedImage(
                    cropBounds.width, cropBounds.height, imageType);
            Graphics2D g2d = res.createGraphics();
            g2d.setPaint(backColor);
            g2d.fill(cropBounds);
            return res;
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
        
        System.out.println("Resizing original image (" + dither + ")...");
        BufferedImage res = ImageTransform.run(originalToCrop, input,
                backColor, cropBounds.getSize(), dither, imageType);
        fade(res, res, alpha);

        return res;
    }

    /** Compress the brightness into the upper "frac" portion of the range
        0..255. */
    static int fade(int i, double frac) {
        int res = (int) (255 - (255 - i)*frac);
        return (res < 0) ? 0 : res;
    }

    static void fade(BufferedImage src, BufferedImage dest, double frac) {
        if (src == dest && frac == 1.0) {
            // Nothing to do.
            return;
        }

        int width = src.getWidth();
        int height = src.getHeight();
        if (dest.getType() == BufferedImage.TYPE_INT_ARGB) {
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    Color c = new Color(src.getRGB(x,y));
                    c = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                            (int) (c.getAlpha() * frac + 0.5));
                    dest.setRGB(x,y,c.getRGB());
                }
            }
        } else {
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

@JsonTypeInfo(
              use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "decoration")
@JsonSubTypes({
        @Type(value=CuspFigure.class, name = "curve"),
        @Type(value=AnchoredLabel.class, name = "label"),
        @Type(value=LinearRuler.class, name = "ruler"),
        @Type(value=Arrow.class, name = "arrow"),
        @Type(value=TieLine.class, name = "tie line"),
        @Type(value=SourceImage.class, name = "image")
            })
interface DecorationAnnotations extends Decoration {
}
