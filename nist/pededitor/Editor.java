package gov.nist.pededitor;

import javax.imageio.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import javax.swing.plaf.basic.BasicHTML;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.Timer;
import java.util.prefs.Preferences;
import java.io.*;
import java.awt.print.*;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.*;

// TODO Actually use the Selectable#move() methods!

// TODO (major, mandatory) Axes and user-defined variables.
// Some of the groundwork has been done, but there is a lot left.

// TODO (mandatory bug fix) Non-ASCII characters get lost during "save as PDF".

// TODO Text rendering issues: inaccurate text bounding boxes and
// incorrect spacing of space characters during diagram printing.
// These are likely to be especially problematic because they are in
// code that I do not control. The inaccurate text bounding boxes
// causes problems for the following items: multi-line label
// justification (minor) and curve breaking for text (major), and it
// also degrades image quality.

// TODO (mandatory) Label tags: diagram components, chemical formulas,
// temperatures. The only mandatory element so far is that these three
// elememts are associated with the diagram as a whole, not
// necessarily with specific locations or labels. A minimal
// implementation would involve manually adding diagram tags (which
// should be allowable anyhow). Optional: eutectic and peritectic
// points (which *would* have to be associated with specific points),
// user-defined.

// TODO (major, mandatory) Read GRUMP data.

// TODO (mandatory?, backwards compatibility) Duplicate existing
// program's smoothing algorithm when displaying GRUMP files.

// TODO (mandatory) Support tie lines. Per discussion with Chris, tie
// lines are defined by three or four endpoints, and all tie lines
// intersect at a single vanishing point, so quadrilateral tie lines
// are created by regularly sectioning the angle through that point
// and selectioning the region between the two boundary curves.

// TODO (mandatory) Paired squiggles that indicate elided portions of
// an axis

// TODO (mandatory) Chemical formula typing short-cuts (relative to
// entering the whole thing by hand as HTML) of some kind.
// Possibilities include 1) use of LaTeX syntax (or even just a
// severely limited subset of LaTex); 2) HTML input with special
// macros (character palette; press a button to turn subscripting on
// and another to turn it off; a set of buttons for more commonly-used
// special symbols); and 3) automatic subscripting for the majority of
// relatively simple formulas. Not all of those are mandatory, but it
// sounds like implementing at least one of them is.

// TODO (mandatory?) Curve section decorations (e.g. pen-up/pen-down).
// Not important for new diagrams as far as I can see, but may be
// required for backwards compatibility.

// TODO (helpful, not too hard) Allow labels to be moved.

// TODO (mandatory?, preexisting) Apply a gradient to all control
// points on a curve. Specifically, apply the following transformation
// to all points on the currently selected curve for which $variable
// is between v1 and v2: "$variable = $variable + k * ($variable - v1)
// / (v2 - v1)"

// TODO (important) Right-click popup menus. The program works just
// fine with using keyboard shortcuts instead of right-click menus,
// and I'd probably just go on using the keyboard shortcuts even if an
// alternative were provided, but forcing the users to remember
// shortcuts isn't very friendly, and you can't use the mouse to click
// on an ordinary menu if you're already using the mouse to identify
// the location the operation should apply to. A two-step process
// (select action, then selection location) is possible but would be
// slow at best and awkward to implement as well.

// TODO (important) More intuitive dot placement process (the black
// circles around interesting points). Currently a dot is printed at
// any curve or polyline that has only 1 vertex, but all label anchor
// points should be included in the vertex set, too, and label anchor
// points should not, in general, be dotted.

// TODO (important) Smarter diagram margin settings. Currently, the
// diagram fills a hard-coded portion of the page, regardless of
// whether the user needs more or less room than that. Allow the user
// to expand, shrink, and ideally recompute (that is, define margins
// in terms of the amount of screen space that is actually being used,
// instead of relative to the size of the core diagram)

// TODO (optional) Curve tags, including liquidus, solidus, and
// presumably user-defined tags too.

// TODO (major time-saver) Semi-automatically infer diagram location
// from composition where equation balancing is possible. Discarding
// ubiquitous elements like O, H, N, or C may be required.

// TODO (feature, harder) Allow detection of the intersections of two
// splines. (What makes this feature more desirable than it would be
// otherwise is that it would create consistency. As long as some
// kinds of intersections are detectable, people will sometimes forget
// that other kinds are not detectable and be confused when that does
// not work. (I did that myself at least once, even though I wrote the
// program.)

// TODO (optional) Better curve fitting. As I believe Don mentioned,
// following the control points too slavishly can yield over-fitting
// in which you end up mapping noise (experimental error, scanner
// noise, twitches in the hand of the digitizer or the person who drew
// the image in the first place, or whatever). Peter's program already
// does fitting, but forcing arbitrary curves only a cubic Bezier is
// too restrictive. (Consider Kriging?)

// Heuristics may be used to identify good cutoffs for fit quality. At
// the over-fitting end of the spectrum, if you find the sum of the
// squares of the fit error terms dropping only in roughly proportion
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
// making everything undoable would take work.

// TODO (easy enhancement) Support non-solid tie lines.

// TODO (requires a checkmark and fix of incorrect dimensions)
// Allow boxes around labels

// TODO (enhancement) Tiling fill patterns. These are important for
// general diagram drawing, but they seem to be less important for
// PEDs

// TODO (mandatory) "Symbol" type lines are not implemented.

// TODO (optional) User-defined line and point widths.

// TODO (preexisting in viewer) Periodic table integration and
// automatic conversion of diagrams from mole percent to weight
// percent

// TODO (optional) Standard line style shorthand in PED-format file.

// TODO (preexisting but not mandatory) Smart line dash lengths.
// Peter Schenk's PED Editor adjusts the segment length for dashed
// lines to insure that the dashes in dashed curves are always enough
// shorter than the curves themselves that at least two dashes are
// visible. It's a nice feature, but is it worth it to reproduce? The
// lengths of the dashes in dashed lines is proportional to their
// thickness, so you can especially short dashes by using especially
// thin lines already. (Using the sum of the chord lengths as a lower
// bound on the length of the whole would achieve the goal of insuring
// at least two dashes are visible, but would not guarantee that the
// dashes end neatly at both endpoints. (Java2D already has its own
// estimate of the path length -- and it would actually be better to
// use its estimate than to do a better but different estimate of
// one's own -- but I don't know how practical it would be to access
// that information.)

// TODO Allow copy and paste of curves.

// TODO (optional) When multiple curves share a vertex, allow the
// vertex to be moved in all curves instead of just one of them.

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
// it.)

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

// TODO (optional) Write GRUMP data. May not be practical because of
// format limitations.

// TODO (Don't do?) Define bounding rectangles for labels, instead of
// just identifying a corner or the center of the text. Bounding
// rectangles for text entry are useful if you're doing a lot of text
// that involves word wrap, but they are probably not needed for this
// application.

// TODO (minor bug (in Java itself?)) Fix or work around the bug (which I think
// is a Java bug, not an application bug) where scrolling the scroll
// pane doesn't cause the image to be redrawn.

// TODO (feature, easy) Use Robot to make sure that when the mouse is
// inside the diagram, zooming the image never causes you to lose your
// place. (For perspective, the program already keeps its location
// during zooming better than Adobe Reader does.)

// TODO (feature, lower priority, low difficulty) Add a "bring mouse
// and diagram under mouse to center" operation

// TODO (feature, lower priority) Zoom by mouse dragging would be nice
// too (there would have to be a minimum drag distance; maybe also
// require that there be no currently selected curve)

// TODO (feature, low difficulty) When choosing the nearest point, use
// a two-step process: first identify the nearest curve, then identify
// the nearest point on that curve.

// TODO (bug, medium severity) Sometimes the zoom window steals the
// focus, which is disorienting because you can't do anything until
// the focus is returned to the edit or crop window

// TODO Convert from wt% to mol%

// TODO Standard page coordinates become invalid if you stretch a
// diagram or modify margins (which should be easy to do). Perhaps it
// would be better to define everything, even page decorations, in
// principal coordinates, or to redefine standard page coordinates so
// the smallest rectangle circumscribing the diagram is identified
// with the unit square. The real problem here is only with ternary
// diagrams -- rectangles' principal coordinates are fine.

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class Editor implements CropEventListener, MouseListener,
                               MouseMotionListener, Printable {
    static ObjectMapper objectMapper = null;

    abstract class Action extends AbstractAction {
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

    /** Series of classes that implement the Movable interface so that
        different types of selections, such as vertices and labels,
        can be manipulated the same way. */
    class VertexSelection implements Selectable {
        int curveNo;
        int vertexNo;

        VertexSelection(int curveNo, int vertexNo) {
            this.curveNo = curveNo;
            this.vertexNo = vertexNo;
        }

        @Override
        public VertexSelection remove() {
            GeneralPolyline path = paths.get(curveNo);
            int oldVertexCnt = path.size();
            repaintEditFrame();

            if (oldVertexCnt >= 2) {
                ArrayList<SegmentAndT> segments = getPathSegments(path);

                // While deleting this vertex, adjust t values that
                // reference this segment. Previous segments that
                // don't touch point are left alone; following
                // segments that don't touch point have their
                // segmentNo decremented; and the two segments that
                // touch point are combined into a single segment
                // number newSeg. What a pain in the neck!

                if (oldVertexCnt == 2) {
                    for (SegmentAndT segment: segments) {
                        segment.segment = 0;
                        segment.t = 0;
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

                    for (SegmentAndT segment: segments) {
                        if (segment.segment == prevSeg) {
                            segment.segment = newSeg;
                            segment.t *= splitT;
                        } else if (segment.segment == nextSeg) {
                            segment.segment = newSeg;
                            segment.t = splitT + segment.t * (1 - splitT);
                        } else if (segment.segment > vertexNo) {
                            segment.segment--;
                        }
                    }
                }

                path.remove(vertexNo);
                setPathSegments(path, segments);
                return new VertexSelection
                    (curveNo,
                     (vertexNo > 0) ? (vertexNo - 1) : 0);
            } else {
                paths.remove(curveNo);
                return null;
            }
        }

        @Override public String toString() {
            return "VertexSelection[" + curveNo + ", " + vertexNo + "]";
        }

        @Override
        public void move(Point2D target) {
            GeneralPolyline path = paths.get(curveNo);
            Point2D.Double oldPos = path.get(vertexNo);
            path.set(vertexNo, target);
        }

        @Override
        public void copy(Point2D dest) {
            Point2D.Double delta = getLocation();
            delta.x = dest.getX() - delta.x;
            delta.y = dest.getY() - delta.y;
            GeneralPolyline path = paths.get(curveNo).clone();
            for (int i = 0; i < path.size(); ++i) {
                Point2D.Double point = path.get(i);
                point.x += delta.x;
                point.y += delta.y;
                path.set(i, point);
            }
            paths.add(path);
            repaintEditFrame();
        }

        @Override
        public Point2D.Double getLocation() {
            return paths.get(curveNo).get(vertexNo);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (this.getClass() != VertexSelection.class) return false;
            if (this.getClass() != other.getClass()) return false;

            VertexSelection cast = (VertexSelection) other;
            return this.curveNo == cast.curveNo
                && this.vertexNo == cast.vertexNo;
        }
    }


    class LabelSelection implements Selectable {
        int index;

        LabelSelection(int index) {
            this.index = index;
        }

        @Override
        public LabelSelection remove() {
            labels.remove(index);
            labelViews.remove(index);
            repaintEditFrame();
            return null;
        }

        @Override
        public void move(Point2D dest) {
            AnchoredLabel item = labels.get(index);
            item.setX(dest.getX());
            item.setY(dest.getY());
            repaintEditFrame();
        }

        @Override
        public void copy(Point2D dest) {
            AnchoredLabel item = labels.get(index).clone();
            item.setX(dest.getX());
            item.setY(dest.getY());
            add(item);
            repaintEditFrame();
        }

        @Override
        public Point2D.Double getLocation() {
            AnchoredLabel item = labels.get(index);
            return new Point2D.Double(item.getX(), item.getY());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (this.getClass() != LabelSelection.class) return false;
            if (this.getClass() != other.getClass()) return false;

            LabelSelection cast = (LabelSelection) other;
            return this.index == cast.index;
        }
    }


    class ArrowSelection implements Selectable {
        int index;

        ArrowSelection(int index) {
            this.index = index;
        }

        @Override
        public ArrowSelection remove() {
            arrows.remove(index);
            repaintEditFrame();
            return null;
        }

        @Override
        public void move(Point2D dest) {
            Arrow item = arrows.get(index);
            item.x = dest.getX();
            item.y = dest.getY();
        }

        @Override
        public void copy(Point2D dest) {
            Arrow item = arrows.get(index).clonus();
            item.x = dest.getX();
            item.y = dest.getY();
            arrows.add(item);
            repaintEditFrame();
        }

        @Override
        public Point2D.Double getLocation() {
            Arrow item = arrows.get(index);
            return new Point2D.Double(item.x, item.y);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (this.getClass() != ArrowSelection.class) return false;
            if (this.getClass() != other.getClass()) return false;

            ArrowSelection cast = (ArrowSelection) other;
            return this.index == cast.index;
        }
    }

    static enum TieLineHandle { INNER1, INNER2, OUTER1, OUTER2 };

    class TieLineSelection implements Selectable {

        /** Index into tieLines list. */
        int index;
        /** A tie line is not a point object. It has up to four
         corners that can be used as handles to select it. */
        TieLineHandle handle;

        TieLineSelection(int index, TieLineHandle handle) {
            this.index = index;
            this.handle = handle;
        }

        @Override
        public TieLineSelection remove() {
            tieLines.remove(index);
            repaintEditFrame();
            return null;
        }

        @Override
        public void move(Point2D dest) {
            // Tie line movement happens indirectly: normally,
            // everything at a key point moves at once, which means
            // that the control point that delimits the tie line moves
            // with it. No additional work is required here.
        }

        @Override
        public void copy(Point2D dest) {
            JOptionPane.showMessageDialog
                (editFrame, "Tie lines cannot be copied.");
        }

        @Override
        public Point2D.Double getLocation() {
            TieLine item = tieLines.get(index);
            switch (handle) {
            case INNER1:
                return item.getInner1();
            case INNER2:
                return item.getInner2();
            case OUTER1:
                return item.getOuter1();
            case OUTER2:
                return item.getOuter2();
            }

            return null;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (this.getClass() != TieLineSelection.class) return false;
            if (this.getClass() != other.getClass()) return false;

            TieLineSelection cast = (TieLineSelection) other;
            return index == cast.index && handle == cast.handle;
        }
    }

    
    /** Apply the NIST MML PED standard binary diagram axis style. */
    class DefaultBinaryRuler extends LinearRuler {
        DefaultBinaryRuler() {
            fontSize = normalFontSize();
            lineWidth = STANDARD_LINE_WIDTH;
            tickPadding = 3.0;

            drawSpine = false; // The ruler spine is already a curve
                               // in the diagram. Don't draw it twice.
        }
    }


    /** Apply the NIST MML PED standard ternary diagram axis style. */
    class DefaultTernaryRuler extends LinearRuler {
        DefaultTernaryRuler() {
            fontSize = normalFontSize();
            lineWidth = STANDARD_LINE_WIDTH;
            tickPadding = 3.0;
            drawSpine = false;

            tickType = LinearRuler.TickType.V;
            keepStartClear = true;
            keepEndClear = true;

            drawSpine = false; // The ruler spine is already a curve
                               // in the diagram. Don't draw it twice.
        }
    }

    class PathAndT {
        GeneralPolyline path;
        double t;
    }

    private static final String PREF_DIR = "dir";
    static protected double MOUSE_UNSTICK_DISTANCE = 30; /* pixels */
    static protected Image crosshairs = null;

    protected CropFrame cropFrame = new CropFrame();
    protected EditFrame editFrame = new EditFrame(this);
    protected ImageZoomFrame zoomFrame = new ImageZoomFrame();
    protected VertexInfoDialog vertexInfo = new VertexInfoDialog(editFrame);

    // TODO Allow inclusion of attribution data? (Debatable -- in the
    // larger digitization context, the attribution data would be
    // stored elsewhere, but if diagram files are flying around, some
    // people might like to insure that the attribution information is
    // attached to those files.)

    @JsonProperty protected PolygonTransform originalToPrincipal;
    protected PolygonTransform principalToOriginal;
    @JsonProperty protected AffinePolygonTransform principalToStandardPage;
    protected Affine standardPageToPrincipal;
    /** Bounds of the entire page in standardPage space. */
    protected Rectangle2D.Double pageBounds;
    /** Bounds of the core diagram (the central triangle or rectangle
        only) in the principal coordinate space. */
    protected DiagramType diagramType = null;
    protected double scale;
    protected ArrayList<GeneralPolyline> paths;
    protected ArrayList<AnchoredLabel> labels;
    protected ArrayList<Point2D.Double> labelCenters;
    protected ArrayList<View> labelViews;
    @JsonProperty protected ArrayList<Arrow> arrows;
    protected ArrayList<TieLine> tieLines;
    protected BufferedImage originalImage;
    protected String originalFilename;
    /** The item (vertex, label, etc.) that is selected, or null if nothing is. */
    protected Selectable selection;
    /** If the timer exists, the original image (if any) upon which
        the new diagram is overlaid will blink. */
    Timer imageBlinker = null;
    /** True if imageBlinker is enabled and the original image should
        be displayed in the background at this time. */
    boolean backgroundImageEnabled;

    protected ArrayList<LinearAxis> axes;
    /** principal coordinates are used to define rulers' startPoints
        and endPoints. */
    protected ArrayList<LinearRuler> rulers;
    protected LinearAxis xAxis = null;
    protected LinearAxis yAxis = null;
    protected LinearAxis zAxis = null;
    protected LinearAxis pageXAxis = null;
    protected LinearAxis pageYAxis = null;
    protected boolean preserveMprin = false;
    protected int paintSuppressionRequestCnt;

    /** mouseIsStuck is true if the user recently performed a
        point-selection operatiorn such as "nearest vertex" or
        "nearest point on curve" and the mouse has not yet been moved
        far enough to un-stick the mouse from that location. */
    protected boolean mouseIsStuck;
    protected ArrayList<ScaledCroppedImage> scaledOriginalImages;
    /** This is the darkened version of the original image, or null if
        no darkened version exists. At most one dark image is kept in
        memory at a time. */
    protected ScaledCroppedImage darkImage;
    protected double labelXMargin = 0;
    protected double labelYMargin = 0;

    static final double STANDARD_LINE_WIDTH = 0.0012;
    protected double lineWidth = STANDARD_LINE_WIDTH;
    protected StandardStroke lineStyle = StandardStroke.SOLID;

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
      + "Select the second inside corner (or just press \"OK\" right away "
      + "if the tie lines converge)."
      + "</p></div></html>" };

    StepDialog tieLineDialog = new StepDialog
        (editFrame, "Select Tie Line Display Region",
         new Editor.Action("Item selected") {
             @Override public void actionPerformed(ActionEvent e) {
                 tieLineCornerSelected();
             }
         });
    ArrayList<PathAndT> tieLineSelections;

    @JsonProperty protected String filename;

    // TODO Set saveNeeded
    boolean saveNeeded = false;

    /** Current mouse position expressed in principal coordinates.
     It's not always sufficient to simply read the mouse position in
     the window, because after the user jumps to a preselected point,
     the integer mouse position is not precise enough to express that
     location. */
    protected Point2D.Double mprin = null;

    public Editor() {
        zoomFrame.setFocusableWindowState(false);
        tieLineDialog.setFocusableWindowState(false);
        clear();
        getEditPane().addMouseListener(this);
        getEditPane().addMouseMotionListener(this);
        cropFrame.addCropEventListener(this);
    }

    /** @return the filename that has been assigned to the PED format
     * diagram output. */
    @JsonIgnore
    public String getFilename() {
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
        scale = 800.0;
        paths = new ArrayList<GeneralPolyline>();
        arrows = new ArrayList<Arrow>();
        tieLines = new ArrayList<TieLine>();
        labels = new ArrayList<AnchoredLabel>();
        labelViews = new ArrayList<View>();
        labelCenters = new ArrayList<Point2D.Double>();
        selection = null;
        axes = new ArrayList<LinearAxis>();
        rulers = new ArrayList<LinearRuler>();
        mprin = null;
        filename = null;
        saveNeeded = false;
        scaledOriginalImages = new ArrayList<ScaledCroppedImage>();
        vertexInfo.setAngle(0);
        vertexInfo.setSlope(0);
        vertexInfo.setLineWidth(lineWidth);
        mouseIsStuck = false;
        paintSuppressionRequestCnt = 0;
        setBackground(EditFrame.BackgroundImage.GRAY);
        tieLineDialog.setVisible(false);
        tieLineSelections = new ArrayList<PathAndT>();
    }

    @JsonProperty("curves")
    ArrayList<GeneralPolyline> getPaths() {
        return paths;
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

    @JsonIgnore
    VertexSelection getSelectedVertex() {
        return (selection instanceof VertexSelection)
            ? ((VertexSelection) selection)
            : null;
    }

    @JsonIgnore
    LabelSelection getSelectedLabel() {
        return (selection instanceof LabelSelection)
            ? ((LabelSelection) selection)
            : null;
    }

    @JsonIgnore
    TieLineSelection getSelectedTieLine() {
        return (selection instanceof TieLineSelection)
            ? ((TieLineSelection) selection)
            : null;
    }

    @JsonIgnore
    ArrowSelection getSelectedArrow() {
        return (selection instanceof ArrowSelection)
            ? ((ArrowSelection) selection)
            : null;
    }

    /** @return The currently selected GeneralPolyline, or null if no
        curve is selected. */
    @JsonIgnore
    public GeneralPolyline getActiveCurve() {
        VertexSelection sel = getSelectedVertex();
        return (sel == null)
            ? null
            : paths.get(sel.curveNo);
    }

    @JsonIgnore
    public Point2D.Double getActiveVertex() {
        VertexSelection sel = getSelectedVertex();
        return (sel == null)
            ? null
            : paths.get(sel.curveNo).get(sel.vertexNo);
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
        same location as the selection to mprin. */
    public void moveSelection() {
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

        moveSelection(mprin);

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

        selection.copy(mprin);
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
        same location as the selection to dest. */
    public void moveSelection(Point2D.Double dest) {
        Point2D.Double p = selection.getLocation();

        for (Selectable sel: selectables()) {
            if (principalCoordinatesMatch(p, sel.getLocation())) {
                sel.move(dest);
            }
        }
    }

    public void removeSelection() {
        if (selection != null) {
            selection = selection.remove();
        }
    }

    /** Cycle the currently active curve.

        @param delta if 1, then cycle forwards; if 0, then cycle backwards. */
    public void cycleActiveCurve(int delta) {
        if (paths.size() == 0) {
            // Nothing to do.
            return;
        }

        VertexSelection sel = getSelectedVertex();

        if (sel == null) {
            selection = sel = new VertexSelection((delta > 0) ? -1 : 0, -1);
        }

        sel.curveNo = (sel.curveNo + delta + paths.size()) % paths.size();
        sel.vertexNo = paths.get(sel.curveNo).size() - 1;
        repaintEditFrame();
    }

    public void editMargins() {
        System.out.println("TODO this");
        // TODO editMargins
    }

    public void editDiagramComponents() {
        System.out.println("TODO this");
        // TODO editDiagramComponents
    }

    /** @param scale A multiple of standardPage coordinates

        @return a transform from standard page coordinates to device
        coordinates. */
    Affine standardPageToDevice(double scale) {
        return Affine.getScaleInstance(scale, scale);
    }

    /** @param scale A multiple of standardPage coordinates

        @return a transform from principal coordinates to device
        coordinates. */
    Affine principalToScaledPage(double scale) {
        Affine output = standardPageToDevice(scale);
        output.concatenate(principalToStandardPage);
        return output;
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
        Point2D.Double newMprin = standardPageToPrincipal.transform(mousePage);

        if (mouseIsStuckAtSelection()) {
            moveSelection(newMprin);
        }
        mprin = newMprin;
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
        paintDiagram(g, scale, true);
    }


    /** Compute the scaling factor to apply to pageBounds (and
        standardPage coordinates) in order for xform.transform(scale *
        pageBounds) to fill deviceBounds as much as possible without
        going over.

        xxx This might be worthless because of the failure to take
        margins into account.
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


    /** Paint the diagram to the given graphics context.

        @param standardPageToDevice Transformation from standard page
        coordinates to device coordinates

        @param editing If true, then paint editing hints (highlight
        the currently active curve in green, show the consequences of
        adding the current mouse position in red, etc.). If false,
        show the final form of the diagram. This parameter should be
        false except while painting the editFrame. */
    public void paintDiagram(Graphics2D g, double scale, boolean editing) {
        if (principalToStandardPage == null
            || paintSuppressionRequestCnt > 0) {
            return;
        }

        EditFrame.BackgroundImage back = editFrame.getBackgroundImage();
        boolean showImage = tracingImage() && editing
            && back != EditFrame.BackgroundImage.NONE
            && (back == EditFrame.BackgroundImage.GRAY
                || backgroundImageEnabled);

        if (showImage) {
            ScaledCroppedImage im = getScaledOriginalImage();
            if (imageBlinker != null) {
                if (darkImage != null
                    && im.imageBounds.equals(darkImage.imageBounds)
                    && im.cropBounds.equals(darkImage.cropBounds)) {
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
                    unfade(src, darkImage.croppedImage);
                    im = darkImage;
                }
            }
            g.drawImage(im.croppedImage, im.cropBounds.x, im.cropBounds.y,
                        null);
        } else {
            // Draw a white box the size of the page.

            // TODO might need more work, but not a priority now

            // TODO Prohibit drawing outside the page, or expand the
            // page to accommodate such drawings?

            // TODO Shrink the diagram to the used space?

            if (editing) {
                g.setColor(Color.WHITE);
                g.fill(scaledPageBounds(scale));
            }
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.BLACK);

        int pathCnt = paths.size();
        if (pathCnt == 0) {
            return;
        }

        VertexSelection vsel = editing ? getSelectedVertex() : null;

        for (int i = 0; i < pathCnt; ++i) {
            if (vsel == null || vsel.curveNo != i) {
                GeneralPolyline path = paths.get(i);
                if (path.size() == 1) {
                    double r = path.getLineWidth() * 2 * scale;
                    circleVertices(g, path, scale, true, r);
                } else {
                    draw(g, path, scale);
                }
            }
        }

        if (false) {
            // TODO Delete dead code -- but sometimes it's helpful to
            // mark key points...

            Point2D.Double xpoint = new Point2D.Double();
            Affine p2d = principalToScaledPage(scale);
            for (Point2D.Double point: keyPoints()) {
                p2d.transform(point, xpoint);
                double r = 4.0;
                g.fill(new Ellipse2D.Double
                       (xpoint.x - r, xpoint.y - r, r * 2, r * 2));
            }
        }

        {
            LabelSelection sel = editing ? getSelectedLabel() : null;
            for (int i = 0; i < labels.size(); ++i) {
                boolean selected = (sel != null && i == sel.index);
                if (!selected) { // Draw selection later.
                    drawLabel(g, i, scale);
                }
            }
        }

        {
            ArrowSelection sel = editing ? getSelectedArrow() : null;
            for (int i = 0; i < arrows.size(); ++i) {
                Arrow arrow = arrows.get(i);
                boolean selected = (sel != null && i == sel.index);
                if (selected) {
                    g.setColor(Color.GREEN);
                }
                drawArrow(g, scale, arrow);
                g.setColor(Color.BLACK);
            }
        }

        {
            TieLineSelection sel = editing ? getSelectedTieLine() : null;
            for (int i = 0; i < tieLines.size(); ++i) {
                TieLine item = tieLines.get(i);
                boolean selected = (sel != null && i == sel.index);
                if (selected) {
                    g.setColor(Color.GREEN);
                }
                draw(g, item, scale);
                g.setColor(Color.BLACK);
            }
        }

        for (LinearRuler ruler: rulers) {
            ruler.draw(g, principalToStandardPage, scale);
        }

        if (vsel != null) {
            GeneralPolyline path = getActiveCurve();

            // Disable anti-aliasing for this phase because it
            // prevents the green line from precisely overwriting the
            // red line.

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_OFF);

            Point2D.Double extraVertex = mprin;
            if (mouseIsStuckAtSelection() && getSelectedVertex() != null) {
                // Show the point that would be added if the mouse
                // became unstuck.
                extraVertex = getMousePosition();
            }

            if (extraVertex != null && !isDuplicate(extraVertex)) {
                // Add the current mouse position to the path
                // immediately after the currently selected vertex,
                // and draw the curve that results from this addtion
                // in red. Then remove the extra vertex.

                g.setColor(Color.RED);
                path.add(vsel.vertexNo + 1, extraVertex);
                draw(g, path, scale);
                path.remove(vsel.vertexNo + 1);
            }

            g.setColor(Color.GREEN);
            draw(g, path, scale);
            double r = Math.max(path.getLineWidth() * scale * 1.5, 4.0);
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


        { // Draw the label selection, if any.
            LabelSelection sel = editing ? getSelectedLabel() : null;
            if (sel != null) {
                AnchoredLabel label = labels.get(sel.index);
                View normalView = labelViews.get(sel.index);
                labelViews.set(sel.index,
                               toView(label.getText(), label.getXWeight(),
                                      Color.GREEN));
                drawLabel(g, sel.index, scale);
                labelViews.set(sel.index, normalView);
            }
        }

    }

    /** @return a curve to which a vertex can be appended or inserted.
        If a curve was already selected, then that will be the return
        value; if no curve is selected, then start a new curve and
        return that. */
    @JsonIgnore
    public GeneralPolyline getCurveForAppend() {
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
        selection = new VertexSelection(paths.size() - 1, -1);
        return getActiveCurve();
    }

    public void deselectCurve() {
        selection = null;
        repaintEditFrame();
    }

    /** Start a new curve where the old curve ends. */
    public void addCusp() {
        if (mprin == null) {
            return;
        }

        add(mprin);
        deselectCurve();
        add(mprin);
    }

    public void deleteSymbol() {
        // TODO Symbol handling is an ugly hack that will have to be
        // consolidated later, probably with Arrow becoming a subclass
        // of something else.

        if (mprin == null) {
            return;
        }

        Point2D.Double nearPoint = null;
        Point2D.Double mousePage = new Point2D.Double();
        principalToStandardPage.transform(mprin, mousePage);

        // Square of the minimum distance of all key points examined
        // so far from mprin, as measured in standard page
        // coordinates.
        double minDistSq = 0;
        int indexOfMin = -1;

        int num = -1;

        for (Arrow arrow: arrows) {
            ++num;
            Point2D.Double symbolPage
                = principalToStandardPage.transform(arrow.x, arrow.y);
            double distSq = mousePage.distanceSq(symbolPage);
            if (indexOfMin == -1 || distSq < minDistSq) {
                indexOfMin = num;
                minDistSq = distSq;
            }
        }

        if (indexOfMin == -1) {
            return;
        }

        arrows.remove(indexOfMin);
        repaintEditFrame();
    }

    /** Add a dot. */
    public void addDot() {
        if (mprin == null) {
            return;
        }

        deselectCurve();
        add(mprin);
    }

    /** Update the tangency information to display the slope at the
        given vertex. For polylines, the slope will generally be
        undefined (having different values when coming from the left
        than when coming from the right) at interior vertices, but
        arbitarily choose the slope of the segment following the
        vertex if possible. */
    public void showTangent(int curveNo, int vertexNo) {
        if (vertexNo < 0) {
            return;
        }

        GeneralPolyline path = paths.get(curveNo);
        int vertexCnt = path.size();
        if (vertexCnt == 1) {
            return;
        }

        Point2D.Double g = null;

        if (vertexCnt >= 1) {
            g = (vertexNo + 1 == vertexCnt)
                ? path.getGradient(vertexNo - 1, 1.0)
                : path.getGradient(vertexNo, 0.0);
            if (g != null) {
                principalToStandardPage.deltaTransform(g, g);
            }
        }

        vertexInfo.setGradient(g);
        vertexInfo.setLineWidth(path.getLineWidth());
    }

    /** Return true if point p is the same as either the currently
        selected vertex or the vertex that immediately follows them.
        The reason to care is that SplinePolyline barfs if you pass
        the same vertex twice in a row. */
    boolean isDuplicate(Point2D p) {
        VertexSelection sel = getSelectedVertex();
        if (sel == null) {
            return false;
        }

        GeneralPolyline path = paths.get(sel.curveNo);

        return (sel.vertexNo >= 0 && p.equals(path.get(sel.vertexNo)))
            || (sel.vertexNo < path.size() - 1 && p.equals(path.get(sel.vertexNo + 1)));
    }

    /** Add a point to getActiveCurve(). */
    public void add(Point2D.Double point) {
        if (isDuplicate(point)) {
            return; // Adding the same point twice causes problems.
        }
        GeneralPolyline path = getCurveForAppend();
        VertexSelection sel = getSelectedVertex();

        add(paths.get(sel.curveNo), sel.vertexNo, point);
        ++sel.vertexNo;
        showTangent(sel.curveNo, sel.vertexNo);
        repaintEditFrame();
    }

    /** Add a new vertex to path, located at point, and inserted just
        after vertex vertexNo. */
    public void add(GeneralPolyline path, int vertexNo,
                    Point2D.Double point) {
        if (vertexNo == -1) {
            path.add(vertexNo + 1, point);
            return;
        }

        ArrayList<SegmentAndT> segments = getPathSegments(path);
        int segCnt = path.getSegmentCnt();

        double dist1 = point.distance(path.get(vertexNo));
        double dist2 = (vertexNo == segCnt) ? 0
            : point.distance(path.get(vertexNo + 1));

        // For old segment vertexNo, map the t range [0, splitT] to
        // new segment vertexNo range [0,1], and map the t range
        // (splitT, 1] to new segment vertexNo+1 range [0,1]. If
        // vertexNo == segCnt then segment vertexNo never existed
        // before, so it doesn't matter what splitT value we use.
        double splitT = dist1 / (dist1 + dist2);

        for (SegmentAndT segment: segments) {
            if (segment.segment > vertexNo) {
                segment.segment++;
            } else if (segment.segment == vertexNo) {
                if (segment.t <= splitT) {
                    segment.t /= splitT;
                } else {
                    segment.segment++;
                    segment.t = (segment.t - splitT) / (1.0 - splitT);
                }
            }
        }

        path.add(vertexNo + 1, point);
        setPathSegments(path, segments);
    }


    /** Make a list of SegmentAndT values that appear in this Editor
        object and that refer to locations on the given path. */
    ArrayList<SegmentAndT> getPathSegments(GeneralPolyline path) {
        ArrayList<SegmentAndT> output = new ArrayList<SegmentAndT>();

        // Tie Lines' limits are defined by t values.
        for (TieLine tie: tieLines) {
            if (tie.innerEdge == path) {
                output.add(path.getSegment(tie.it1));
                output.add(path.getSegment(tie.it2));
            }
            if (tie.outerEdge == path) {
                output.add(path.getSegment(tie.ot1));
                output.add(path.getSegment(tie.ot2));
            }
        }

        return output;
    }

    /** You can then change the segments returned by getPathSegments()
        and call setPathSegments() to make corresponding updates to
        the fields from which they came. */
    void setPathSegments(GeneralPolyline path,
                         ArrayList<SegmentAndT> segments) {

        ArrayList<Double> ts = new ArrayList<Double>();
        for (SegmentAndT segment: segments) {
            ts.add(path.segmentToT(segment.segment, segment.t));
        }

        int index = 0;
        for (TieLine tie: tieLines) {
            if (tie.innerEdge == path) {
                tie.it1 = ts.get(index++);
                tie.it2 = ts.get(index++);
            }
            if (tie.outerEdge == path) {
                tie.ot1 = ts.get(index++);
                tie.ot2 = ts.get(index++);
            }
        }
    }

    /** Print an arrow at the currently selected location that is
        tangent to the curve at that location and that points
        rightward (or downward if the curve is vertical). */
    public void addArrow(boolean rightward) {
        if (mouseIsStuckAtSelection() && getSelectedArrow() != null) {
            unstickMouse();
        }

        double theta = vertexInfo.getAngle();
        if (!rightward) {
            theta += Math.PI;
        }

        arrows.add(new Arrow(mprin.x, mprin.y, lineWidth, theta));
        repaintEditFrame();
    }

    void tieLineCornerSelected() {
        VertexSelection vsel = getSelectedVertex();
        if (vsel == null) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "You must select a vertex.");
            return;
        }

        PathAndT pat = new PathAndT();
        pat.path = paths.get(vsel.curveNo);
        pat.t = pat.path.segmentToT(vsel.vertexNo, 0.0);

        int oldCnt = tieLineSelections.size();
        System.out.println("OldCnt = " + oldCnt);

        if ((oldCnt == 1 || oldCnt == 3)
            && pat.path != tieLineSelections.get(oldCnt-1).path) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "This selection must belong to the same\n" +
                 "curve as the previous selection.");
            return;
        }

        tieLineSelections.add(pat);

        if (oldCnt < 3) {
            tieLineDialog.getLabel().setText(tieLineStepStrings[oldCnt + 1]);
            return;
        }

        tieLineDialog.setVisible(false);
        int lineCnt;

        while (true) {
            try {
                String lineCntStr = JOptionPane.showInputDialog
                    (editFrame,
                     "Number of tie lines to display (interior only):",
                     new Integer(10));
                if (lineCntStr == null) {
                    tieLineDialog.setVisible(false);
                    tieLineSelections = null;
                    return;
                }
                lineCnt = Integer.parseInt(lineCntStr);
                if (lineCnt <= 0) {
                    JOptionPane.showMessageDialog
                        (null, "Enter a positive integer.");
                    continue;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid number format.");
                continue;
            }
            break;
        }

        TieLine tie = new TieLine(lineCnt, lineStyle);
        tie.lineWidth = lineWidth;

        tie.innerEdge = tieLineSelections.get(0).path;
        tie.it1 = tieLineSelections.get(0).t;
        tie.it2 = tieLineSelections.get(1).t;

        tie.outerEdge = tieLineSelections.get(2).path;
        tie.ot1 = tieLineSelections.get(2).t;
        tie.ot2 = tieLineSelections.get(3).t;

        tieLines.add(tie);
        repaintEditFrame();
    }

    public void addTieLine() {
        tieLineSelections = new ArrayList<PathAndT>();
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

        Point2D.Double mousePage = principalToStandardPage.transform(mprin);

        int output = -1;
        double minDistance = 0.0;
        for (int i = 0; i < labels.size(); ++i) {
            double distance = labelCenters.get(i).distance(mousePage);
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
        if (mouseIsStuck) {
            unstickMouse();
        }

        Point2D.Double point;

        if (!select) {
            point = nearestPoint();
            if (point == null) {
                return;
            }
        } else {
            ArrayList<Selectable> points = nearestSelections();
            if (points.isEmpty()) {
                return;
            }

            Selectable sel = points.get(0);

            if (selection != null) {
                // Check if the old selection is one of the nearest
                // points. If so, then choose the next one after it. This
                // is to allow users to cycle through a set of overlapping
                // key points using the selection key. Select once for the
                // first item; select it again and get the second one,
                // then the third, and so on.
                for (int i = 0; i < points.size() - 1; ++i) {
                    if (selection.equals(points.get(i))) {
                        sel = points.get(i+1);
                        break;
                    }
                }
            }

            selection = sel;

            if (sel instanceof VertexSelection) {
                VertexSelection vsel = (VertexSelection) sel;
                showTangent(vsel.curveNo, vsel.vertexNo);
            }

            point = sel.getLocation();
        }

        moveMouse(point);
        mouseIsStuck = true;
    }

    /** In order to insert vertices "backwards", just reverse the
        existing set of vertices and insert the points in the normal
        forwards order. */
    public void reverseInsertionOrder() {
        VertexSelection vsel = getSelectedVertex();
        if (vsel == null) {
            return;
        }

        GeneralPolyline path = getActiveCurve();

        ArrayList<Point2D.Double> points
            = new ArrayList<Point2D.Double>();

        if (path.isClosed()) {
            // Leave vertex #0 in place, but reverse the order of the
            // others.
            points.add(path.get(0));

            for (int i = path.size() - 1; i >= 1; --i) {
                points.add(path.get(i));
            }

            if (vsel.vertexNo > 0) {
                vsel.vertexNo = path.size() - vsel.vertexNo;
            }
        } else {
            // Reverse the order of all vertices.

            for (int i = path.size() - 1; i >= 0; --i) {
                points.add(path.get(i));
            }

            vsel.vertexNo = path.size() - 1 - vsel.vertexNo;
        }

        for (TieLine tie: tieLines) {
            if (tie.innerEdge == path) {
                tie.it1 = 1.0 - tie.it1;
                tie.it2 = 1.0 - tie.it2;
            }
            if (tie.outerEdge == path) {
                tie.ot1 = 1.0 - tie.ot1;
                tie.ot2 = 1.0 - tie.ot2;
            }
        }
        
        path.setPoints(points);
        repaintEditFrame();
    }

    public void changeXUnits() {
        changeUnits(getXAxis());
    }

    public void changeYUnits() {
        changeUnits(getYAxis());
    }

    public void changeUnits(LinearAxis axis) {
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
        dog.setTitle("Change " + axis.name + " units");
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

    public void setComponent(int componentNum) {
        // TODO Do...
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

    /** Invoked from the EditFrame menu */
    public void addLabel() {
        if (mprin == null) {
            // TODO What if mprin is not defined?
            return;
        }

        if (mouseIsStuckAtSelection() && getSelectedLabel() != null) {
            unstickMouse();
        }

        AnchoredLabel t = (new LabelDialog(editFrame, "Add Label"))
            .showModal();
        if (t == null) {
            return;
        }

        t.setX(mprin.x);
        t.setY(mprin.y);
        add(t);
    }

    public void add(AnchoredLabel label) {
        labels.add(label);
        labelViews.add(toView(label.getText(), label.getXWeight()));
        labelCenters.add(new Point2D.Double());
        repaintEditFrame();
    }

    /** Invoked from the EditFrame menu */
    public void editLabel() {

        LabelSelection lsel = getSelectedLabel();
        if (lsel == null) {
            int nl = nearestLabelNo();
            if (nl == -1) {
                return;
            }
            selection = lsel = new LabelSelection(nl);
        }

        AnchoredLabel label = labels.get(lsel.index);
        LabelDialog dialog = new LabelDialog(editFrame, "Edit Label");
        dialog.setText(label.getText());
        dialog.setXWeight(label.getXWeight());
        dialog.setYWeight(label.getYWeight());
        dialog.setFontSize(label.getFontSize());
        dialog.setAngle(label.getAngle());

        repaintEditFrame();

        AnchoredLabel newLabel = dialog.showModal();
        if (newLabel == null) {
            return;
        }

        newLabel.setX(label.getX());
        newLabel.setY(label.getY());
        labels.set(lsel.index, newLabel);
        labelViews.set(lsel.index,
                       toView(newLabel.getText(), newLabel.getXWeight()));
    }

    View toView(String str, double xWeight) {
        return toView(str, xWeight, null);
    }

    /** @param xWeight Used to determine how to justify rows of text. */
    View toView(String str, double xWeight, Color textColor) {
        xWeight = 0.0;
        if (xWeight >= 0.75) {
            str = "<html><div align=\"right\">" + str + "</div></html>";
        } else if (xWeight >= 0.25) {
            str = "<html><div align=\"center\">" + str + "</div></html>";
        } else {
            str = "<html>" + str + "</html>";
            // str = "<html><div align=\"left\">" + str + "</div></html>";
        }

        JLabel bogus = new JLabel(str);
        if (textColor != null) {
            bogus.setForeground(textColor);
        }
        bogus.setFont(getEditPane().getFont());
        return (View) bogus.getClientProperty("html");
    }

    /** Regenerate the labelViews field from the labels field. */
    void initializeLabelViews() {
        labelViews = new ArrayList<View>();
        for (AnchoredLabel label: labels) {
            labelViews.add(toView(label.getText(), label.getXWeight()));
            labelCenters.add(new Point2D.Double());
        }
    }

    @JsonProperty ArrayList<AnchoredLabel> getLabels() {
        return labels;
    }

    @JsonProperty void setLabels(Collection<AnchoredLabel> labels) {
        this.labels = new ArrayList<AnchoredLabel>(labels);
    }

    public DiagramType getDiagramType() {
        return diagramType;
    }

    public void setDiagramType(DiagramType t) {
        this.diagramType = t;
    }


    /** Invoked from the EditFrame menu */
    public void setLineStyle(StandardStroke lineStyle) {
        this.lineStyle = lineStyle;

        GeneralPolyline path = getActiveCurve();

        if (path != null) {
            path.setStroke(lineStyle);
            repaintEditFrame();
        }

    }

    /** Move the mouse pointer so its position corresponds to the
        given location in principal coordinates. */
    void moveMouse(Point2D.Double point) {
        mprin = point;
        if (principalToStandardPage == null) {
            return;
        }

        ++paintSuppressionRequestCnt;
        Point mpos = Duh.floorPoint
            (principalToScaledPage(scale).transform(mprin));
        
        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();
        Point topCorner = spane.getLocationOnScreen();

        // For whatever reason (Java bug?), I need to add 1 to the x
        // and y coordinates if I want the output to satisfy

        // getEditPane().getMousePosition() == original mpos value.

        mpos.x += topCorner.x - view.x + 1;
        mpos.y += topCorner.y - view.y + 1;

        // TODO (mandatory bug fix) mouse jumping out of bounds...

        try {
            Robot robot = new Robot();
            robot.mouseMove(mpos.x, mpos.y);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        } finally {
            --paintSuppressionRequestCnt;
        }

        repaintEditFrame();
    }

    /** @return the location in principal coordinates of the key
        point closest (by page distance) to mprin. */
    Point2D.Double nearestPoint() {
        if (mprin == null) {
            return null;
        }
        Point2D.Double nearPoint = null;
        Point2D.Double xpoint = new Point2D.Double();
        principalToStandardPage.transform(mprin, xpoint);
        Point2D.Double xpoint2 = new Point2D.Double();

        // Square of the minimum distance from mprin of all key points
        // examined so far, as measured in standard page coordinates.
        double minDistSq = 0;

        for (Point2D.Double point: keyPoints()) {
            principalToStandardPage.transform(point, xpoint2);
            double distSq = xpoint.distanceSq(xpoint2);
            if (nearPoint == null || distSq < minDistSq) {
                nearPoint = point;
                minDistSq = distSq;
            }
        }

        return nearPoint;
    }

    /** @return a list of all selectable items located at the
        selectable point closest (by page distance) to mprin. */
    ArrayList<Selectable> nearestSelections() {
        if (mprin == null) {
            return null;
        }
        Point2D.Double nearPagePoint = null;
        Point2D.Double xpoint = new Point2D.Double();
        principalToStandardPage.transform(mprin, xpoint);
        Point2D.Double xpoint2 = new Point2D.Double();

        // Square of the minimum distance from mprin of all key points
        // examined so far, as measured in standard page coordinates.
        double minDistSq = 0;

        ArrayList<Selectable> sels = selectables();
        for (Selectable sel: sels) {
            Point2D.Double point = sel.getLocation();
            principalToStandardPage.transform(point, xpoint2);
            double distSq = xpoint.distanceSq(xpoint2);
            if (nearPagePoint == null || distSq < minDistSq) {
                nearPagePoint = (Point2D.Double) xpoint2.clone();
                minDistSq = distSq;
            }
        }

        ArrayList<Selectable> output = new ArrayList<Selectable>();
        if (nearPagePoint == null) {
            return output;
        }

        // Now that we know where the nearest point is, return all
        // selections that match that point.

        double tinyDistSq = 1e-12;

        for (Selectable sel: sels) {
            principalToStandardPage.transform(sel.getLocation(), xpoint2);
            if (nearPagePoint.distanceSq(xpoint2) < tinyDistSq) {
                output.add(sel);
            }
        }

        return output;
    }

    /** @return a list of all key points in the diagram. Some
        duplication is likely. */
    public ArrayList<Point2D.Double> keyPoints() {
        ArrayList<Point2D.Double> output = intersections();

        for (Selectable m: selectables()) {
            output.add(m.getLocation());
        }

        return output;
    }


    /** @return a list of all possible selections. */
    ArrayList<Selectable> selectables() {
        ArrayList<Selectable> output
            = new ArrayList<Selectable>();

        // Add vertices of all curves.
        for (int i = 0; i < paths.size(); ++i) {
            GeneralPolyline path = paths.get(i);
            for (int j = 0; j < path.size(); ++j) {
                output.add(new VertexSelection(i,j));
            }
        }

        // Add labels.
        for (int i = 0; i < labels.size(); ++i) {
            output.add(new LabelSelection(i));
        }

        // Add arrows.
        for (int i = 0; i < arrows.size(); ++i) {
            output.add(new ArrowSelection(i));
        }

        // Add tie lines.
        for (int i = 0; i < tieLines.size(); ++i) {
            for (TieLineHandle handle: TieLineHandle.values()) {
                output.add(new TieLineSelection(i, handle));
            }
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
                         pagePath.segmentIntersections(segment)) {
                    standardPageToPrincipal.transform(point, point);
                    output.add(point);
                }
            }
        }

        return output;
    }


    /** Like seekNearestPoint(), but instead select the point on a
        previously added segment that is nearest to the mouse pointer
        as measured by distance on the standard page.

        @param select If true, select the vertex preceding that segment.
    */
    public void seekNearestSegment(boolean select) {
        if (mouseIsStuck) {
            unstickMouse();
        }

        if (mprin == null) {
            return;
        }

        Point2D.Double mousePage = principalToStandardPage.transform(mprin);
        VertexSelection vsel = null;
        Point2D.Double gradient = null;

        CurveDistance minDist = null;
        boolean isCloserToNext = false;

        int curveNo = -1;
        for (GeneralPolyline path : paths) {
            ++curveNo;
            Point2D.Double point;

            GeneralPolyline pagePath
                = path.createTransformed(principalToStandardPage);
            CurveDistance dist = pagePath.distance(mousePage);

            if (minDist == null || dist.distance < minDist.distance) {
                minDist = dist;

                int vertexNo = pagePath.firstControlPointIndex(dist.t);
                isCloserToNext
                    = (pagePath.get(vertexNo).distanceSq(mousePage) >
                       pagePath.get(vertexNo + 1).distanceSq(mousePage));
                vsel = new VertexSelection(curveNo, vertexNo);

                // TODO Have to decide whether to use principal
                // coordinates for gradients or not, but continue to
                // use page coordinates for now.
                gradient = pagePath.getGradient(vertexNo, dist.t);
            }
        }

        if (minDist == null) {
            return;
        }

        if (select) {
            selection = vsel;
            if (isCloserToNext) {
                // pagePoint is closer to the next vertex than to this
                // one. Incrementing the selection number, possibly
                // cycling back to 0 for closed curves.
                vsel.vertexNo = (vsel.vertexNo + 1)
                    % paths.get(vsel.curveNo).size();

                // Incrementing vertexNo introduces a new problem: if
                // we added a new vertex at this moment, it would be
                // inserted after the second off the two neighboring
                // vertices vertex instead of between them. Reversing
                // the vertex order fixes this.
                reverseInsertionOrder();
            }
        }
        moveMouse(standardPageToPrincipal.transform(minDist.point));
        mouseIsStuck = true;
        vertexInfo.setGradient(gradient);
        vertexInfo.setLineWidth(paths.get(vsel.curveNo).getLineWidth());
    }

    public void toggleSmoothing() {
        GeneralPolyline oldPath = getActiveCurve();
        if (oldPath == null) {
            return;
        }

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

        paths.set(getSelectedVertex().curveNo, path);
        repaintEditFrame();
    }

    /** Toggle the closed/open status of the currently selected
        curve. */
    public void toggleCurveClosure() {

        GeneralPolyline path = getActiveCurve();
        if (path == null) {
            return;
        }
        path.setClosed(!path.isClosed());
        repaintEditFrame();
    }

    void draw(Graphics2D g, GeneralPolyline path, double scale) {
        path.draw(g, principalToStandardPage, scale);
    }

    void draw(Graphics2D g, TieLine tie, double scale) {
        tie.draw(g, principalToStandardPage, scale);
    }

    /** @return the name of the image file that this diagram was
        digitized from, or null if this diagram is not known to be
        digitized from a file. */
    public String getOriginalFilename() {
        return originalFilename;
    }

    void dontTrace() {
        originalFilename = null;
        originalToPrincipal = null;
        originalImage = null;
        zoomFrame.setVisible(false);
        editFrame.setTitle("Edit " + diagramType);
        editFrame.mnBackgroundImage.setEnabled(false);
    }

    public void setOriginalFilename(String filename) {
        originalFilename = filename;

        if (filename == null) {
            dontTrace();
            return;
        }

        // TODO I'd like to use Files here, but it was introduced in
        // Java 7 and I'm running Java 6.

        // if (Files.notExists(filename)) {
        if (false) {
            System.out.println("Warning: file '" + filename + "' not found");
            dontTrace();
            return;
        }

        try {
            BufferedImage im = ImageIO.read(new File(filename));
            if (im == null) {
                throw new IOException(filename + ": unknown image format");
            }
            originalImage = im;
            editFrame.setTitle("Edit " + diagramType + " " + filename);
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
            // Work-around for a bug that affects EB's PC as of 11/11.
            System.setProperty("sun.java2d.d3d", "false");
            // TODO UNDO
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
    }

    /** Start on a blank new diagram. */
    public void newDiagram() {
        if (!verifyNewDiagram()) {
            return;
        }

        diagramType = (new DiagramDialog(null)).showModal();
        newDiagram(null, null);
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
        boolean closeDiagramOutline = true;

        boolean tracing = (vertices != null);
        clear();
        setOriginalFilename(originalFilename);

        double leftMargin = 0.15;
        double rightMargin = 0.15;
        double topMargin = 0.15;
        double bottomMargin = 0.15;
        double maxPageHeight = 1.0;
        double maxPageWidth = 1.0;
        double aspectRatio = 1.0;

        Point2D.Double[] principalTrianglePoints =
            { new Point2D.Double(0.0, 0.0),
              new Point2D.Double(0.0, 100.0),
              new Point2D.Double(100.0, 0.0) };

        Rescale r = null;

        switch (diagramType) {
        case TERNARY_BOTTOM:
            {
                closeDiagramOutline = false;
                double height;

                double defaultHeight = !tracing ? 0.45
                    : (1.0
                       - (vertices[1].distance(vertices[2]) / 
                          vertices[0].distance(vertices[3])));
                Formatter f = new Formatter();
                f.format("%.1f", defaultHeight * 100);
                String initialHeight = f.toString();

                while (true) {
                    String heightS = (String) JOptionPane.showInputDialog
                        (null,
                         "Enter the visual height of the diagram\n" +
                         "as a percentage of the full triangle height:",
                         initialHeight);
                    if (heightS == null) {
                        heightS = initialHeight;
                    }
                    try {
                        height = Double.parseDouble(heightS);
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Invalid number format.");
                        continue;
                    }
                    break;
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
                      new Point2D.Double(100.0 - height, height),
                      new Point2D.Double(100.0, 0.0) };

                if (tracing) {
                    originalToPrincipal = new QuadToQuad(vertices, outputVertices);
                }

                r = new Rescale(1.0, leftMargin + rightMargin, maxPageWidth,
                                TriangleTransform.UNIT_TRIANGLE_HEIGHT * height/100,
                                topMargin + bottomMargin, maxPageHeight);

                double rx = r.width - rightMargin;
                double bottom = r.height - bottomMargin;

                Point2D.Double[] trianglePagePositions =
                    { new Point2D.Double(leftMargin, bottom),
                      new Point2D.Double((leftMargin + rx)/2,
                                         bottom - TriangleTransform.UNIT_TRIANGLE_HEIGHT * r.t),
                      new Point2D.Double(rx, bottom) };
                principalToStandardPage = new TriangleTransform
                    (principalTrianglePoints, trianglePagePositions);

                // Add the endpoints of the diagram.
                diagramOutline.add(outputVertices[1]);
                diagramOutline.add(outputVertices[0]);
                diagramOutline.add(outputVertices[3]);
                diagramOutline.add(outputVertices[2]);

                addTernaryBottomRuler(0.0, 100.0);
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
                Rectangle2D.Double principalBounds
                    = new Rectangle2D.Double(0.0, 0.0, 100.0, 100.0);
                if (tracing) {
                    // Transform the input quadrilateral into a rectangle
                    QuadToRect q = new QuadToRect();
                    q.setVertices(vertices);
                    q.setRectangle(principalBounds);
                    originalToPrincipal = q;
                }

                r = new Rescale(100.0, leftMargin + rightMargin, maxPageWidth,
                                100.0, topMargin + bottomMargin, maxPageHeight);

                principalToStandardPage = new RectangleTransform
                    (new Rectangle2D.Double(0.0, 0.0, 100.0, 100.0),
                     new Rectangle2D.Double(leftMargin, 1.0 - bottomMargin,
                                            100.0 * r.t, -100.0 * r.t));
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
                closeDiagramOutline = false;
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
                int otherVertices[] = { ov1, ov2 };

                double pageMaxes[];

                if (!tracing) {
                    pageMaxes = new double[] { 100.0, 100.0 };
                } else {
                    double angleSideLengths[] =
                        { vertices[angleVertex].distance(vertices[ov2]),
                          vertices[angleVertex].distance(vertices[ov1]) };
                    double maxSideLength = Math.max(angleSideLengths[0],
                                                    angleSideLengths[1]);
                    pageMaxes = new double[]
                        { 100.0 * angleSideLengths[0] / maxSideLength,
                          100.0 * angleSideLengths[1] / maxSideLength };
                }

                String pageMaxInitialValues[] = new String[pageMaxes.length];
                for (int i = 0; i < pageMaxes.length; ++i) {
                    Formatter f = new Formatter();
                    f.format("%.1f", pageMaxes[i]);
                    pageMaxInitialValues[i] = f.toString();
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
                            pageMaxes[i] = Double.parseDouble(pageMaxStrings[i]);
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Invalid number format.");
                        continue;
                    }
                    break;
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
                // coordinate represents the proportion of the
                // lower-right principal component, and the second
                // principal coordinate represents the proportion of
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
                        = new Point2D.Double(0, 100.0 - leftLength);
                    trianglePoints[RIGHT_VERTEX]
                        = new Point2D.Double(rightLength, 100.0 - rightLength);
                    addTernaryLeftRuler(100 - leftLength, 100.0);
                    addTernaryRightRuler(100 - rightLength, 100.0);
                    break;

                case TERNARY_RIGHT:
                    trianglePoints[LEFT_VERTEX]
                        = new Point2D.Double(100.0 - bottomLength, 0.0);
                    trianglePoints[TOP_VERTEX]
                        = new Point2D.Double(100.0 - rightLength, rightLength);
                    addTernaryBottomRuler(100.0 - bottomLength, 100.0);
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
                    (bounds.width, leftMargin + rightMargin, maxPageWidth,
                     bounds.height, topMargin + bottomMargin,
                     maxPageHeight);
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
                    (AffineTransform.getTranslateInstance
                     (leftMargin - minX, topMargin - minY));

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

                r = new Rescale(1.0, leftMargin + rightMargin, maxPageWidth,
                                TriangleTransform.UNIT_TRIANGLE_HEIGHT,
                                topMargin + bottomMargin,
                                maxPageHeight);
                double rx = r.width - rightMargin;
                double bottom = r.height - bottomMargin;

                Point2D.Double[] trianglePagePositions =
                    { new Point2D.Double(leftMargin, bottom),
                      new Point2D.Double((leftMargin + rx)/2, topMargin),
                      new Point2D.Double(rx, bottom) };
                principalToStandardPage = new TriangleTransform
                    (principalTrianglePoints, trianglePagePositions);
                diagramOutline.addAll
                    (Arrays.asList(principalToStandardPage.getInputVertices()));

                addTernaryBottomRuler(0.0, 100.0);
                addTernaryLeftRuler(0.0, 100.0);
                addTernaryRightRuler(0.0, 100.0);
                break;
            }
        }
        pageBounds = new Rectangle2D.Double(0.0, 0.0, r.width, r.height);

        if (diagramOutline.size() > 0) {
            // Insert the polyline outline of the diagram into the set
            // of paths.
            GeneralPolyline outline = GeneralPolyline.create
                (GeneralPolyline.LINEAR,
                 diagramOutline.toArray(new Point2D.Double[0]),
                 StandardStroke.SOLID,
                 STANDARD_LINE_WIDTH);
            outline.setClosed(closeDiagramOutline);
            paths.add(outline);
        }

        initializeDiagram();

        // xAxis etc. don't exist until initializeDiagram() is called,
        // so we can't assign them until now.
        switch (diagramType) {
        case TERNARY:
        case TERNARY_BOTTOM:
            {
                rulers.get(0).axis = xAxis;
                rulers.get(1).axis = yAxis;
                rulers.get(2).axis = yAxis;
                break;
            }
        case BINARY:
            {
                rulers.get(0).axis = xAxis;
                rulers.get(1).axis = xAxis;
                rulers.get(2).axis = yAxis;
                rulers.get(3).axis = yAxis;
                break;
            }
        case TERNARY_LEFT:
            {
                rulers.get(0).axis = xAxis;
                rulers.get(1).axis = yAxis;
                break;
            }
        case TERNARY_RIGHT:
            {
                rulers.get(0).axis = xAxis;
                rulers.get(1).axis = yAxis;
                break;
            }
        case TERNARY_TOP:
            {
                rulers.get(0).axis = yAxis;
                rulers.get(1).axis = yAxis;
                break;
            }
        }
    }

    protected double normalFontSize() {
        return 12.0 / 800;
    }

    protected void initializeDiagram() {

        if (diagramType.isTernary()) {
            NumberFormat pctFormat = new DecimalFormat("##0.0'%'");
            xAxis = LinearAxis.createXAxis(pctFormat);
            // Confusingly, the axis that goes from 0 at the bottom
            // left to 100 at the bottom right like a normal x-axis
            // and that is called "xAxis" actually ends at component
            // 'Z'.
            xAxis.name = "Z";
            yAxis = LinearAxis.createYAxis(pctFormat);
            zAxis = new LinearAxis(pctFormat, -1.0, -1.0, 100.0);
            zAxis.name = "X";

            axes.add(zAxis);
            axes.add(yAxis);
            axes.add(xAxis);
        } else {
            NumberFormat format = new DecimalFormat("##0.0");
            xAxis = LinearAxis.createXAxis(format);
            yAxis = LinearAxis.createYAxis(format);
            axes.add(xAxis);
            axes.add(yAxis);
        }

        try {
            standardPageToPrincipal = principalToStandardPage.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }

        {
            NumberFormat format = new DecimalFormat("0.000");
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

        // Force the editor frame image to be initialized.
        zoomBy(1.0);

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
        View em = toView("m", 0);
        labelXMargin = em.getPreferredSpan(View.X_AXIS) / 2.0;
        labelYMargin = em.getPreferredSpan(View.Y_AXIS) / 5.0;
        
        vertexInfo.setVisible(true);
        editFrame.setVisible(true);
    }

    /** Before starting a new diagram, give the user an opportunity to
        save the old diagram or to change their mind.

        @return false if the user changes their mind and a new diagram
        should not be started. */
    boolean verifyNewDiagram() {
        // TODO Just a stub.
        return true;
    }

    /** Invoked from the EditFrame menu */
    public void openDiagram() {
        if (!verifyNewDiagram()) {
            return;
        }

        File file = showOpenDialog("ped");
        if (file == null) {
            return;
        }
        filename = file.getAbsolutePath();

        try {
            ObjectMapper mapper = getObjectMapper();
            cannibalize((Editor) mapper.readValue(file, getClass()));
            for (TieLine tie: tieLines) {
                tie.innerEdge = idToCurve(tie.innerId);
                tie.outerEdge = idToCurve(tie.outerId);
            }
        } catch (Exception e) {
            // TODO More?
            JOptionPane.showMessageDialog
                (editFrame, "Error: " + e);

            // TODO only for testing...
            throw new Error(e);
        }
    }

    public Rectangle2D.Double getPageBounds() {
        return (Rectangle2D.Double) pageBounds.clone();
    }

    public void setPageBounds(Rectangle2D.Double rect) {
        pageBounds = (Rectangle2D.Double) rect.clone();
    }

    /** Copy data fields from other. Afterwards, it is unsafe to
        modify other, because the modifications may affect this as
        well. In other words, this is a shallow copy that destroys
        other. */
    void cannibalize(Editor other) {
        diagramType = other.diagramType;
        originalToPrincipal = other.originalToPrincipal;
        principalToStandardPage = other.principalToStandardPage;
        pageBounds = other.pageBounds;
        originalFilename = other.originalFilename;
        scale = other.scale;
        arrows = other.arrows;
        initializeDiagram();
        paths = other.paths;
        tieLines = other.tieLines;

        axes = other.axes;
        rulers = other.rulers;
        labels = other.labels;
        initializeLabelViews();
        selection = null;
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
        this.axes = axes;
        rulers = new ArrayList<LinearRuler>();

        for (LinearAxis axis: axes) {
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
            cropFrame.setFilename(filename);
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
        chooser.setDialogTitle("Save as " + ext.toUpperCase());
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
        Graphics2D g2 = tp.createGraphics((float) bounds.width, (float) bounds.height,
                                          new DefaultFontMapper());
        paintDiagram(g2, deviceScale(g2, bounds), false);
        g2.dispose();
        cb.addTemplate(tp, doc.left(), doc.bottom());
        doc.close();
    }

    /** Invoked from the EditFrame menu */
    public void saveAsSVG() {
        // TODO saveAsSVG (optional feature)
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
            JOptionPane.showMessageDialog(editFrame, "File saved.");
        } catch (Exception e) {
            // TODO More?
            JOptionPane.showMessageDialog
                (editFrame, "Error: " + e);

            // TODO only for testing...
            throw new Error(e);
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

        AffineTransform oldTransform = g.getTransform();

        Rectangle2D.Double bounds
            = new Rectangle2D.Double
            (pf.getImageableX(), pf.getImageableY(),
             pf.getImageableWidth(), pf.getImageableHeight());

        g.translate(bounds.getX(), bounds.getY());
        double scale = Math.min(bounds.height / pageBounds.height,
                                bounds.width / pageBounds.width);
        paintDiagram(g, scale, false);
        g.setTransform(oldTransform);

        return Printable.PAGE_EXISTS;
    }

    /** Invoked from the EditFrame menu */
    public void addVertexLocation() {
        // TODO (mandatory) Explicitly select label or vertex location.
    }


    /** Apply the given transform to all curve vertices, all label
        locations, all arrow locations, all ruler start and endpoints,
        and all axis definitions *except* for the x- and y-axis
        definitions. */
    public void transformPrincipalCoordinates(AffineTransform trans) {
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
        coordinates, with one exception: leave the x- and y-axes
        alone. So the diagram looks the same as before except for (1)
        principal component axis ticks and (2) principal coordinate
        values as indicated in the status bar. For example, one might
        use this method to convert a binary diagram's y-axis from one
        temperature scale to another, or from the default range 0-100
        to the range you really want. */
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

        principalToStandardPage.concatenate(itrans);
        standardPageToPrincipal.preConcatenate(atrans);

        for (LinearAxis axis: axes) {
            if (axis.isXAxis() || axis.isYAxis()) {
                continue;
            }
            axis.concatenate(itrans);
        }
    }

    @Override
        public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void unstickMouse() {
        mouseIsStuck = false;
        updateMousePosition();
    }

    @Override
        public void mousePressed(MouseEvent e) {
        if (principalToStandardPage == null) {
            return;
        }
        if (mouseIsStuckAtSelection() && getSelectedVertex() != null) {
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
    @Override
        public void mouseMoved(MouseEvent e) {
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
            boolean updateMprin = (mprin == null) || !mouseIsStuck;

            if (!updateMprin) {
                Point mprinScreen = Duh.floorPoint
                    (principalToScaledPage(scale).transform(mprin));
                if (mprinScreen.distance(mpos) >= MOUSE_UNSTICK_DISTANCE) {
                    mouseIsStuck = false;
                    updateMprin = true;
                }
            }

            if (updateMprin) {
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

        double sx = mpos.getX() + 0.5;
        double sy = mpos.getY() + 0.5;

        return standardPageToPrincipal.transform(sx/scale, sy/scale);
    }

    void updateStatusBar() {
        if (mprin == null) {
            return;
        }

        StringBuilder status = new StringBuilder("");

        boolean first = true;
        for (Axis axis : axes) {
            if (first) {
                first = false;
            } else {
                status.append(",  ");
            }
            status.append(axis.name.toString());
            status.append(" = ");
            status.append(axis.valueAsString(mprin.x, mprin.y));
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
        GeneralPolyline path = getActiveCurve();
        if (path != null) {
            path.setLineWidth(lineWidth);
            repaintEditFrame();
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
        bounds.width -= 2;
        bounds.height -= 2;
        // Rectangle bounds = spane.getViewportBorderBounds();
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

        ++paintSuppressionRequestCnt;

        // Keep the center of the viewport where it was if the center
        // was a part of the image.

        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();

        // Adjust the viewport to allow pagePoint in standard page
        // coordinates to remain located at offset viewportPoint from
        // the upper left corner of the viewport, to prevent the part
        // of the diagram that is visible from changing too
        // drastically when you zoom in and out. The viewport's
        // preferred size also sets constraints on the visible region,
        // so the diagram may not actually stay in the same place.

        Point2D.Double pagePoint = null;
        Point viewportPoint = null;

        if (mprin != null) {
            // Preserve the mouse position: transform mprin into
            // somewhere in the pixel viewportPoint.
            pagePoint = principalToStandardPage.transform(mprin);
            viewportPoint =
                new Point((int) Math.floor(pagePoint.x * oldScale) - view.x,
                          (int) Math.floor(pagePoint.y * oldScale) - view.y);
        } else {
            // Preserve the center of the viewport.
            viewportPoint = new Point(view.width / 2, view.height / 2);
            pagePoint =
                new Point2D.Double
                ((viewportPoint.x + view.x + 0.5) / oldScale,
                 (viewportPoint.y + view.y + 0.5) / oldScale);
        }

        getEditPane().setPreferredSize(scaledPageBounds(scale).getSize());

        if (pagePoint != null) {
            // Adjust viewport to preserve pagePoint => viewportPoint
            // relationship.

            Point screenPoint =
                new Point((int) Math.floor(pagePoint.x * scale),
                          (int) Math.floor(pagePoint.y * scale));

            Point viewPosition
                = new Point(Math.max(0, screenPoint.x - viewportPoint.x),
                            Math.max(0, screenPoint.y - viewportPoint.y));

            // Java 1.6_29 needs to be told twice which viewport to
            // use. It seems to get the Y scrollbar right the first
            // time, and the X scrollbar right the second time.
            preserveMprin = true;
            spane.getViewport().setViewPosition(viewPosition);
            spane.getViewport().setViewPosition(viewPosition);
            preserveMprin = false;
        }
        getEditPane().revalidate();
        --paintSuppressionRequestCnt;
        repaintEditFrame();
    }

    void customLineWidth() {
        // TODO Just a stub.
    }

    EditPane getEditPane() { return editFrame.getEditPane(); }

    static String format(double d, int decimalPoints) {
        Formatter f = new Formatter();
        f.format("%." + decimalPoints + "f", d);
        return f.toString();
    }

    /** Compress the brightness into the upper third of the range
        0..255. */
    static int fade(int i) {
        return 255 - (255 - i)/3;
    }

    /** Reverse the fade() transformation (as best one can). */
    static int unfade(int i) {
        return 255 - (255 - i)*3;
    }

    static void fade(BufferedImage image) { 
        int width = image.getWidth();
        int height = image.getHeight();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                Color c = new Color(image.getRGB(x,y));
                c = new Color(fade(c.getRed()),
                              fade(c.getGreen()),
                              fade(c.getBlue()));
                image.setRGB(x,y,c.getRGB());
            }
        }
    }

    static void unfade(BufferedImage src, BufferedImage dest) { 
        int width = src.getWidth();
        int height = src.getHeight();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                Color c = new Color(src.getRGB(x,y));
                c = new Color(unfade(c.getRed()),
                              unfade(c.getGreen()),
                              unfade(c.getBlue()));
                dest.setRGB(x,y,c.getRGB());
            }
        }
    }

    public void run(String filename) {
        editFrame.setTitle("Phase Equilibria Diagram Editor");
        editFrame.pack();
        editFrame.setVisible(true);
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
        AnchoredLabel label = labels.get(labelNo);
        View view = labelViews.get(labelNo);
        Point2D.Double point =
            principalToStandardPage.transform(label.getX(), label.getY());

        if (label.isOpaque()) {
            Color oldColor = g.getColor();
            g.setColor(Color.WHITE);
            boxHTML(g, view, scale * label.getFontSize(),
                    label.getAngle(),
                    point.x * scale, point.y * scale,
                    label.getXWeight(), label.getYWeight(), true);
            g.setColor(oldColor);
        }

        if (label.isBoxed()) {
            boxHTML(g, view, scale * label.getFontSize(),
                    label.getAngle(),
                    point.x * scale, point.y * scale,
                    label.getXWeight(), label.getYWeight(), false);
        }

        drawHTML(g, view, scale * label.getFontSize(),
                 label.getAngle(),
                 point.x * scale, point.y * scale,
                 label.getXWeight(), label.getYWeight(),
                 labelCenters.get(labelNo));
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
    void drawHTML(Graphics g, View view, double scale, double angle,
                  double ax, double ay,
                  double xWeight, double yWeight,
                  Point2D.Double labelCenter) {
        double width = view.getPreferredSpan(View.X_AXIS) + labelXMargin * 2;
        double height = view.getPreferredSpan(View.Y_AXIS) + labelYMargin * 2;

        Graphics2D g2d = (Graphics2D) g;
        double textScale = scale / 800.0;

        AffineTransform xform = AffineTransform.getRotateInstance(angle);
        xform.scale(textScale, textScale);
        Point2D.Double xpoint = new Point2D.Double();
        xform.transform
            (new Point2D.Double(width * xWeight, height * yWeight), xpoint);

        ax -= xpoint.x;
        ay -= xpoint.y;

        // Now (ax, ay) represents the (in baseline coordinates) upper
        // left corner of the text block expanded by the x- and
        // y-margins.

        if (labelCenter != null) {
            xform.transform(new Point2D.Double(width/2, height/2), xpoint);
            labelCenter.x = (xpoint.x + ax) / getScale();
            labelCenter.y = (xpoint.y + ay) / getScale();
        }

        // Displace (ax,ay) by (xMargin, yMargin) (again, in baseline
        // coordinates) in order to obtain the true upper left corner
        // of the text block.

        xform.transform(new Point2D.Double(labelXMargin, labelYMargin), xpoint);
        ax += xpoint.x;
        ay += xpoint.y;

        AffineTransform oldxform = g2d.getTransform();
        g2d.translate(ax, ay);
        g2d.transform(xform);
        view.paint(g, new Rectangle(0, 0,
                                    (int) Math.ceil(width), (int) Math.ceil(height)));
        g2d.setTransform(oldxform);
    }


    /** Create a box in the space that the given view would enclose.

        @param fill If true, make a solid box. If false, draw a box outline. */
    void boxHTML(Graphics g, View view, double scale, double angle,
                  double ax, double ay,
                 double xWeight, double yWeight, boolean fill) {
        double width = view.getPreferredSpan(View.X_AXIS) + labelXMargin * 2;
        double height = view.getPreferredSpan(View.Y_AXIS) + labelYMargin * 2;

        Graphics2D g2d = (Graphics2D) g;
        double textScale = scale / 800.0;

        AffineTransform xform = AffineTransform.getRotateInstance(angle);
        xform.scale(textScale, textScale);
        Point2D.Double xpoint = new Point2D.Double();
        xform.transform
            (new Point2D.Double(width * xWeight, height * yWeight), xpoint);

        ax -= xpoint.x;
        ay -= xpoint.y;

        // Now (ax, ay) represents the (in baseline coordinates) upper
        // left corner of the text block expanded by the x- and
        // y-margins.

        Path2D.Double path = new Path2D.Double();
        path.moveTo(ax, ay);
        xform.transform(new Point2D.Double(width, 0), xpoint);
        path.lineTo(ax + xpoint.x, ay + xpoint.y);
        xform.transform(new Point2D.Double(width, height), xpoint);
        path.lineTo(ax + xpoint.x, ay + xpoint.y);
        xform.transform(new Point2D.Double(0, height), xpoint);
        path.lineTo(ax + xpoint.x, ay + xpoint.y);
        path.closePath();

        if (fill) {
            g2d.fill(path);
        } else {
            g2d.draw(path);
        }
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
        }

        return objectMapper;
    }

    void drawArrow(Graphics2D g, double scale, Arrow ai) {
        Affine xform = principalToScaledPage(scale);
        Point2D.Double xpoint = xform.transform(ai.x, ai.y);
        Arrow arr = new Arrow(xpoint.x, xpoint.y, scale * ai.size, ai.theta);
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
        System.out.println("Resizing original image; please wait...");
        im.croppedImage = ImageTransform.run
            (originalToCrop, getOriginalImage(), Color.WHITE,
             new Dimension(cropBounds.width, cropBounds.height));
        fade(im.croppedImage);
        scaledOriginalImages.add(im);
        --paintSuppressionRequestCnt;
        editFrame.setCursor(oldCursor);
        return im;
    }

    void addTernaryBottomRuler(double start /* Z */, double end /* Z */) {
        LinearRuler r = new DefaultTernaryRuler() {{ // Component-Z axis
            xWeight = 0.5;
            yWeight = 0.0;
            textAngle = 0;
            tickLeft = true;
            labelAnchor = LinearRuler.LabelAnchor.RIGHT;
        }};

        r.startPoint = new Point2D.Double(start, 0.0);
        r.endPoint = new Point2D.Double(end, 0);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 100) > 1e-4);
        rulers.add(r);
    }

    void addTernaryLeftRuler(double start /* Y */, double end /* Y */) {
        LinearRuler r = new DefaultTernaryRuler() {{ // Left Y-axis
            xWeight = 1.0;
            yWeight = 0.5;
            textAngle = Math.PI / 3;
            tickRight = true;
            labelAnchor = LinearRuler.LabelAnchor.LEFT;
        }};

        r.startPoint = new Point2D.Double(0.0, start);
        r.endPoint = new Point2D.Double(0.0, end);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 100) > 1e-4);
        rulers.add(r);
    }

    void addTernaryRightRuler(double start /* Y */, double end /* Y */) {
        LinearRuler r = new DefaultTernaryRuler() {{ // Right Y-axis
            xWeight = 0.0;
            yWeight = 0.5;
            textAngle = Math.PI * 2 / 3;
            tickLeft = true;
            labelAnchor = LinearRuler.LabelAnchor.RIGHT;
        }};

        r.startPoint = new Point2D.Double(100 - start, start);
        r.endPoint = new Point2D.Double(100 - end, end);
        r.startArrow = Math.abs(start) > 1e-8;
        r.endArrow = (Math.abs(end - 100) > 1e-4);
        rulers.add(r);
    }

    void addBinaryBottomRuler() {
        rulers.add(new DefaultBinaryRuler() {{ // X-axis
            xWeight = 0.5;
            yWeight = 0.0;
            textAngle = 0;
            tickLeft = true;
            labelAnchor = LinearRuler.LabelAnchor.RIGHT;

            startPoint = new Point2D.Double(0.0, 0.0);
            endPoint = new Point2D.Double(100.0, 0.0);
        }});
    }

    void addBinaryTopRuler() {
        rulers.add(new DefaultBinaryRuler() {{ // X-axis
            xWeight = 0.5;
            yWeight = 1.0;
            textAngle = 0;
            tickRight = true;
            labelAnchor = LinearRuler.LabelAnchor.NONE;

            startPoint = new Point2D.Double(0.0, 100.0);
            endPoint = new Point2D.Double(100.0, 100.0);
        }});
    }

    void addBinaryLeftRuler() {
        rulers.add(new DefaultBinaryRuler() {{ // Left Y-axis
            xWeight = 1.0;
            yWeight = 0.5;
            textAngle = Math.PI / 2;
            tickRight = true;
            labelAnchor = LinearRuler.LabelAnchor.LEFT;

            startPoint = new Point2D.Double(0.0, 0.0);
            endPoint = new Point2D.Double(0.0, 100.0);
        }});
    }

    void addBinaryRightRuler() {
        rulers.add(new DefaultBinaryRuler() {{ // Right Y-axis
            xWeight = 0.0;
            yWeight = 0.5;
            textAngle = Math.PI / 2;
            tickLeft = true;
            labelAnchor = LinearRuler.LabelAnchor.NONE;

            startPoint = new Point2D.Double(100.0, 0.0);
            endPoint = new Point2D.Double(100.0, 100.0);
        }});
    }

    @JsonIgnore public Rectangle2D.Double getPrincipalBounds() {
        return principalToStandardPage.inputBounds();
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

class DecimalFormatAnnotations extends DecimalFormat {
    @Override @JsonProperty("pattern")
	public String toPattern() {
		return null;
	}
    DecimalFormatAnnotations(@JsonProperty("pattern") String pattern) {}
}

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
