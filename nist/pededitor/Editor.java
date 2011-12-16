package gov.nist.pededitor;

import java.awt.*;

import javax.imageio.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.plaf.basic.BasicHTML;

import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.prefs.Preferences;
import java.io.*;
import java.awt.print.*;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.*;

// TODO (feature/usability improvement) Dot placement (the black
// circles around interesting points) is unintuitive. Currently a dot
// is printed at any curve or polyline that has only 1 vertex, but all
// label anchor points should be included in the vertex set, too, and
// label anchor points should not, in general, be dotted.

// TODO (feature/usability improvement) Entering curves and lines
// broken by labels is currently too difficult. (The easiest way to
// simplify these is to allow labels to have white backgrounds,
// erasing the lines drawn behind them, but we'll have to see whether
// the printer or "save as PDF" formats are smart enough to do that
// correctly.)

// TODO (feature -- preexisting functionality, but is it mandatory?)
// Peter Schenk's PED Editor adjusts the segment length for dashed
// lines to insure that dashed lines always have dashes in them, even
// if the curve is very short.

// TODO (mandatory bug fix) Save as PDF: currently, all special
// characters get lost during "save as PDF", probably because of a
// lack of support for these characters in the PDF font. Fix that.

// TODO (feature/usability improvement) Highlighting the previous
// curve when an existing curve is deleted is also unintuitive. It's
// probably better just to leave nothing highlighted.

// TODO (feature -- may need to think about this more in relation to
// margin adjustments) When the edit window is maximized but the image
// scale is smaller than the screen size, the window is not used
// effectively.

// TODO (feature -- important) Margin handling is currently
// inadequate; the diagram fills a predetermined portion of the page,
// and if the user doesn't need as much space on the sides and top and
// bottom as they get, or if they need more, then that's tough luck
// for them. Allow the user to expand, shrink, and ideally recompute
// (that is, define margins in terms of the amount of screen space
// that is actually being used, instead of relative to the size of the
// core diagram)

// TODO (major mandatory feature) Axes and user-defined variables.
// Some of the groundwork has been done, but there is a lot left.

// TODO (feature; as pre-existing functionality, this may be mandatory
// in the viewer but not in the editor) Integration with periodic
// table and automatic conversion of diagrams from mole percent to
// weight percent

// TODO (mandatory feature) Add label angle settings

// TODO (mandatory feature) Allow editing of existing labels

// TODO (mandatory bug fix, low difficulty) Anchors for angled text
// don't work right.

// TODO (major mandatory feature) Load and save as YAML

// TODO (major mandatory feature) (might be a separate program) Read
// GRUMP data.

// TODO (mandatory feature) Mark principal components, chemical
// formulas, eutectic and peritectic points (maybe general
// user-defined tags, too)

// TODO (mandatory feature) Allow some kind of typing short-cuts
// (relative to entering the whole thing by hand as HTML) when
// entering chemical formulas. Possibilities include 1) use of LaTeX
// syntax (or even just a severely limited subset of LaTex); 2) HTML
// input with special macros (press a button to turn subscripting on
// and another to turn it off; a set of buttons for more commonly-used
// special symbols); and 3) automatic subscripting for the majority of
// relatively simple formulas. Not all of those are mandatory, but it
// sounds like implementing at least ONE of the IS mandatory.

// TODO (mandatory feature) Attach tags to polylines and curves,
// including liquidus, solidus, and presumably user-defined tags too.

// TODO (feature) Permit automatic inference of diagram positioning of
// chemicals if the locations can be solved stochiometrically or even
// almost stochiometrically (by discarding ubiquitous elements like O,
// H, N, or C if they do not appear alone as principal components). I
// think this feature would recieve heavy use.

// TODO (mandatory feature -- preexisting functionality) apply a
// gradient to all the control points on a curve. Specifically, apply
// the following transformation to all points on the currently
// selected curve for which $variable is between v1 and v2: "$variable
// = $variable + k * ($variable - v1) / (v2 - v1)"

// TODO (feature) Extensions to the above. More general warping --
// identify a polygon (or circle?) and a point in the interior of that
// polygon that is to be warped to some other point inside the
// polygon. The boundary should remain the same, and the warping
// should apply to the scanned input image as well. For polygonal
// regions, the transform used could be that the interior point is
// used to decompose the polygon into ( # of sides ) triangular
// regions, and each of those regions is subjected to the affine
// transformation that preserves its external edge while transforming
// the interior point to its new location. (The result of such a
// transformation might not be pretty; any line passing through the
// warped region could end up with sharp bends in it.)

// TODO (mandatory compatibility feature) Smoothing: Replicate the existing
// program's smoothing algorithm exactly, because existing diagrams
// have to continue looking like they always did.

// TODO (strongly recommended feature, not too difficult) Better
// smoothing. Numerical Recipes recommends modifications to the cubic
// spline algorithm I currently use, and I expect those modifications
// would yield something that behaves reasonably in just about every
// case.

// TODO (feature, probably not appropriate for the first version)
// Better curve fitting. As I believe Don mentioned, following the
// control points too slavishly can yield over-fitting in which you
// end up mapping noise (experimental error, scanner noise, twitches
// in the hand of the digitizer or the person who drew the image in
// the first place, or whatever). Peter's program already does
// fitting, but forcing arbitrary curves only a cubic Bezier is too
// restrictive. (Consider Kriging?)

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

// TODO (feature) Allow loop smoothing and fitting. For many smoothing
// algorithms including those we have discussed, the formula for
// smoothing a continuous loop is slightly different from the one used
// to smooth an open curve, because for the continuous loop, you want
// the derivatives at the starting point to match up with the
// derivatives at the ending point.

// TODO (major feature, somewhat easier part) Semi-automated
// digitizing: identify the point nearest to the mouse that is on a
// feature of the scanned image. The mouse can already be attracted to
// the nearest feature in the final version of the diagram.
// (Complicating factors include noise and specs in the scanned image,
// the need to infer the width of lines, and that the simplest
// smoothing algorithms for edge detection (such as Gaussian
// smoothing) tend to yield results that are biased towards centers of
// curvature. You can't just mindlessly apply basic edge-finding
// algorithms and have the answer pop right out. If the return value
// is off by even half of a line width, then the feature is almost
// worthless..)

// TODO (major feature, somewhat harder part) Semi-automated
// digitizing: identify entire curves in the scanned image. Problems
// are much the same as for the previous section, with the additional
// issue that processing speed would be more of a problem. (A lot of
// vision problems could be stated in terms of optimization a function
// of the form function(photo(vector of features)) -- multidimensional
// optimization of the feature vector space where evaluating the
// function even once requires transforming thousands or millions of
// pixels of the scanned image. Multidimensional optimization of an
// ordinary function can be kind of expensive, but in this case
// computing the function just once, for a single feature vector,
// requires transforming thousands or millions of pixels. Duh,
// computer vision can be expensive, and how much of our own brains
// are dedicated to vision-related tasks?)

// TODO (feature) Add right-click popup menus. The program works just
// fine with using keyboard shortcuts instead of right-click menus,
// and I'd probably just go on using the keyboard shortcuts even if an
// alternative were provided, but forcing the users to remember
// shortcuts isn't very friendly, and you can't use the mouse to click
// on an ordinary menu if you're already using the mouse to identify
// the location the operation should apply to. A two-step process
// (select action, then selection location) is possible but would be
// slow at best and awkward to implement as well.

// TODO (basic almost mandatory speed improvement) Even with some
// optimizations already applied, zooming is still too slow when
// tracing an image at high zoom. Level 1 fix is to cache all
// previously generated zoom levels, so it's only slow when zooming to
// a higher magnification than ever previously seen.

// TODO (more advanced and less critical performance improvement) At
// very high zoom levels, blow up only a subset of the image instead
// of the whole thing. (If this isn't done, then blowing up traced
// images to, say, 20,000x20,000 -- even if you're actually just
// looking at a small fraction of the whole -- becomes impractical.)

// TODO (feature -- debatable whether it should be implemented or not)
// Write GRUMP data. Not all diagrams could be fully expressed as
// GRUMP. Grump only allows specific combinations of line widths and
// line styles; the program already allows 4 different line widths,
// and it would be easy to allow arbitrary widths. Also, some HTML
// data could not be translated.

// TODO (feature, low priority -- don't do?) Allow users to define bounding
// rectangles for labels, instead of just identifying a corner or the
// center of the text. Bounding rectangles for text entry are useful
// if you're doing a lot of text that involves word wrap, but they are
// probably not needed for this application.

// TODO (feature, fairly low difficulty assuming the Java library
// cubic equation solver, CubicCurve2D, is properly implemented to be
// numerically stable and handle degenerate cases) Allow detection of
// the intersections of line segments with splines.

// TODO (feature, harder) Allow detection of the intersections of two
// splines. (What makes this feature more desirable than it would be
// otherwise is that it would create consistency. I guarantee that as
// long as some kinds of intersections are detectable, people will
// forget that other kinds are not detectable. They will try to detect
// them and then get confused when they see an intersection on the
// screen that they can't jump to.)

// TODO (low severity bug) Fix or work around the bug (which I think
// is a Java bug, not an application bug) where scrolling the scroll
// pane doesn't cause the image to be redrawn.

// TODO (feature) Allow copy and paste of curves.

// TODO (feature, probably too easy to implement to be worth debating
// priority levels) In those situations where zooming the mouse causes
// you to lose your position in the diagram, have Robot move the mouse
// to the new location. (For perspective, the program already keeps
// its location during zooming better than Adobe Reader does.)

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

// TODO (actually don't do -- impractical to fix) Line joins may be
// less than gorgeous. Fat lines may look a bit ugly since they are
// not mitered with thinner lines. For example, if you have a fat line
// on one edge of a rectangle, and a thin line alone the other edge,
// the fat edge sticks out in a bit of a bulb. However, that would be
// hard to fix automatically, and it would also be hard to ask the
// user how to do it. Almost anything is liable to be wrong in some
// situation or other.

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class Editor implements CropEventListener, MouseListener,
                               MouseMotionListener, Printable {
    private static final String PREF_DIR = "dir";
    static protected Image crosshairs = null;

    protected CropFrame cropFrame = new CropFrame();
    protected EditFrame editFrame = new EditFrame(this);
    protected ImageZoomFrame zoomFrame = null;

    protected PolygonTransform originalToPrincipal;
    protected PolygonTransform principalToOriginal;
    protected Affine principalToStandardPage;
    protected Affine standardPageToPrincipal;
    protected Rectangle2D.Double pageBounds;
    protected DiagramType diagramType = null;
    protected double scale;
    protected ArrayList<GeneralPolyline> paths;
    protected ArrayList<AnchoredLabel> labels;
    protected ArrayList<View> labelViews;
    protected ArrayList<Double> labelAngles;
    protected int activeCurveNo;
    protected int activeVertexNo;
    protected ArrayList<AxisInfo> axes;
    protected AxisInfo xAxis = null;
    protected AxisInfo yAxis = null;
    protected AxisInfo zAxis = null;
    protected AxisInfo pageXAxis = null;
    protected AxisInfo pageYAxis = null;
    protected boolean preserveMprin = false;
    protected double lineWidth = 0.0012;
    protected CompositeStroke lineStyle = CompositeStroke.getSolidLine();

    /** Current mouse position expressed in principal coordinates.
     It's not always sufficient to simply read the mouse position in
     the window, because after the user jumps to a preselected point,
     the integer mouse position is not precise enough to express that
     location. */
    protected Point2D.Double mprin = null;

    public Editor() {
        clear();
        editFrame.getImagePane().addMouseListener(this);
        editFrame.getImagePane().addMouseMotionListener(this);
        cropFrame.addCropEventListener(this);
    }

    void clear() {
        originalToPrincipal = null;
        principalToOriginal = null;
        principalToStandardPage = null;
        standardPageToPrincipal = null;
        pageBounds = null;
        scale = 800.0;
        paths = new ArrayList<GeneralPolyline>();
        labels = new ArrayList<AnchoredLabel>();
        labelViews = new ArrayList<View>();
        labelAngles = new ArrayList<Double>();
        activeCurveNo = -1;
        activeVertexNo = -1;
        axes = new ArrayList<AxisInfo>();
        mprin = null;
    }

    /** This variable only determines the type of new curves; existing
        curves retain their originally assigned smoothing type. */
    protected int smoothingType = GeneralPolyline.LINEAR;

    ArrayList<GeneralPolyline> getPaths() {
        return paths;
    }

    public Point2D.Double getActiveVertex() {
        return (activeVertexNo == -1) ? null
            : getActiveCurve().get(activeVertexNo);
    }

    /** Remove the current vertex. */
    public void removeCurrentVertex() {
        if (activeCurveNo == -1) {
            return;
        }

        GeneralPolyline cur = paths.get(activeCurveNo);
        int oldVertexCnt = cur.size();

        if (oldVertexCnt > 1) {
            if (activeVertexNo == -1) {
                activeVertexNo = 0;
            }

            cur.remove(activeVertexNo);
            --activeVertexNo;
        } else {
            removeActiveCurve();
            
            if (oldVertexCnt == 0) {
                // Keep looking for a vertex to remove.
                removeCurrentVertex();
            }
        }

        repaintEditFrame();
    }

    /** Cycle the currently active curve.

        @param delta if 1, then cycle forwards; if 0, then cycle backwards. */
    public void cycleActiveCurve(int delta) {
        if (activeCurveNo == -1) {
            // Nothing to do.
            return;
        }

        if (activeVertexNo == -1) {
            // Delete empty curves.
            removeActiveCurve();
        }

        activeCurveNo = (activeCurveNo + delta + paths.size()) % paths.size();
        activeVertexNo = paths.get(activeCurveNo).size() - 1;
        repaintEditFrame();
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

    /** Move the location of the last curve vertex added so that the
        screen location changes by the given amount. */
    public void moveLastVertex(int dx, int dy) {
        Point2D.Double p = getActiveVertex();
        if (p != null) {
            principalToStandardPage.transform(p, p);
            p.x += dx / scale;
            p.y += dy / scale;
            standardPageToPrincipal.transform(p, p);
            getActiveCurve().set(activeVertexNo, p);
            repaintEditFrame();
        }
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
        if (principalToStandardPage == null) {
            return;
        }
        if (!tracingImage()) {
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

        for (int i = 0; i < pathCnt; ++i) {
            if (!editing || i != activeCurveNo) {
                GeneralPolyline path = paths.get(i);
                if (path.size() == 1) {
                    circleVertices(g, path, scale);
                } else {
                    draw(g, path, scale);
                }
            }
        }

        {
            int i = 0;
            for (AnchoredLabel label: labels) {
                drawHTML(g, labelViews.get(i), scale, labelAngles.get(i),
                         label.getX() * scale, label.getY() * scale,
                         label.getXWeight(), label.getYWeight());

                ++i;
            }
        }

        if (editing) {
            GeneralPolyline lastPath = paths.get(activeCurveNo);

            // Disable anti-aliasing for this phase because it
            // prevents the green line from precisely overwriting the
            // red line.

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_OFF);

            if (mprin != null && activeVertexNo != -1) {

                g.setColor(Color.RED);
                lastPath.add(activeVertexNo + 1, mprin);
                draw(g, lastPath, scale);
                lastPath.remove(activeVertexNo + 1);
            }

            g.setColor(Color.GREEN);
            draw(g, lastPath, scale);
            circleVertices(g, lastPath, scale);

            double r = 8.0; // Radius ~= r/72nds of an inch.
        }
    }

    /** @return The GeneralPolyline that is currently being edited. If
        there is none yet, then create it. */
    public GeneralPolyline getActiveCurve() {
        if (principalToStandardPage == null) {
            return null;
        }
        if (paths.size() == 0) {
            endCurve();
        }
        return paths.get(activeCurveNo);
    }

    /** Start a new curve. */
    void endCurve() {
        if (principalToStandardPage == null) {
            return;
        }
        if (paths.size() > 0) {
            if (paths.get(paths.size() - 1).size() == 0) {
                // Remove the old empty path.
                paths.remove(paths.size() - 1);
            }
        }

        paths.add(GeneralPolyline.create
                  (smoothingType, new Point2D.Double[0], lineStyle, lineWidth));
        activeCurveNo = paths.size() - 1;
        activeVertexNo = -1;
        repaintEditFrame();
    }

    /** Start a new curve where the old curve ends. */
    public void startConnectedCurve() {
        Point2D.Double vertex = getActiveVertex();
        endCurve();
        if (vertex != null) {
            add(vertex);
        }
    }

    /** Add a point to getActiveCurve(). */
    public void add(Point2D.Double point) {
        getActiveCurve().add(++activeVertexNo, point);
        repaintEditFrame();
    }

    /** Like add(), but instead add the previously added point that is
        nearest to the mouse pointer as measured by distance on the
        standard page. */
    public void addNearestPoint() {
        Point2D.Double nearPoint = nearestPoint();
        if (nearPoint == null) {
            return;
        }
        add(nearPoint);
        moveMouse(nearPoint);
    }

    /** Select the previously added point that is
        nearest to the mouse pointer as measured by distance on the
        standard page. */
    public void selectNearestPoint() {
        Point2D.Double nearPoint = nearestPoint();
        if (nearPoint == null) {
            return;
        }

        // Since a single point may belong to multiple curves, point
        // visitation order matters. Start with the point immediately
        // preceding the current point and work backwards. That means
        // visiting the current curve twice -- first visiting the
        // portion of the curve that precedes the current vertex, and
        // then, as the last step, visiting the rest (which means
        // re-visiting a few previously visited points, but that's
        // harmless).

        int pathCnt = paths.size();

        for (int i = 0; i <= pathCnt; ++i) {
            int pathNo = (activeCurveNo - i + pathCnt) % pathCnt;
            GeneralPolyline path = paths.get(pathNo);
            int vertexCnt = path.size();
            int startVertex = (i == 0) ? (activeVertexNo-1) : (vertexCnt-1);
            for (int vertexNo = startVertex; vertexNo >= 0; --vertexNo) {
                Point2D.Double vertex = path.get(vertexNo);
                if (vertex.x == nearPoint.x && vertex.y == nearPoint.y) {
                    activeCurveNo = pathNo;
                    activeVertexNo = vertexNo;
                    moveMouse(vertex);
                    return;
                }
            }
        }

        // This point wasn't in the list; it must be an intersection. Add it.
        add(nearPoint);
        moveMouse(nearPoint);
    }

    /** In order to insert vertices "backwards", just reverse the
        existing set of vertices and insert the points in the normal
        forwards order. */
    public void reverseInsertionOrder() {
        if (activeVertexNo < 0) {
            return;
        }

        GeneralPolyline oldCurve = paths.get(activeCurveNo);

        ArrayList<Point2D.Double> points
            = new ArrayList<Point2D.Double>();
        for (int i = oldCurve.size() - 1; i >= 0; --i) {
            points.add(oldCurve.get(i));
        }

        GeneralPolyline curve = GeneralPolyline.create
            (oldCurve.getSmoothingType(),
             points.toArray(new Point2D.Double[0]),
             oldCurve.getStroke(),
             oldCurve.getLineWidth());
        paths.set(activeCurveNo, curve);
        activeVertexNo = curve.size() - 1 - activeVertexNo;
        repaintEditFrame();
    }

    /** Invoked from the EditFrame menu */
    public void addLabel() {
        if (mprin == null) {
            // TODO What if mprin is not defined?
            return;
        }

        AnchoredLabel t = (new LabelDialog(editFrame)).showModal();
        if (t == null) {
            return;
        }

        Point2D.Double xpoint = principalToStandardPage.transform(mprin);
        t.setX(xpoint.x);
        t.setY(xpoint.y);
        labels.add(t);
        labelAngles.add(labelAngles.size() * Math.PI / 12);

        String str = "<html>" + t.getString() + "</html>";
        JLabel bogus = new JLabel(str);
        bogus.setFont(getEditPane().getFont());
        labelViews.add((View) bogus.getClientProperty("html"));

        repaintEditFrame();
    }

    /** Invoked from the EditFrame menu */
    public void setLabelAnchor() {
        // TODO setLabelAnchor (currently anchors can only be set when
        // the label is first created, which isn't too bad but is a
        // bit annoying)
    }

    /** Invoked from the EditFrame menu */
    public void setLabelAngle() {
        // TODO setLabelAngle
    }

    /** Invoked from the EditFrame menu */
    public void setLabelFont() {
        // TODO setLabelFont
    }

    /** Invoked from the EditFrame menu */
    public void setLineStyle(CompositeStroke lineStyle) {
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

        // TODO fix jumping out of bounds...

        try {
            Robot robot = new Robot();
            robot.mouseMove(mpos.x, mpos.y);
        } catch (AWTException e) {
            throw new RuntimeException(e);
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

        // Square of the minimum distance of all key points examined
        // so far from mprin, as measured in standard page
        // coordinates.
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

    /** @return a list of all segment intersections and user-defined
        points in the diagram. */
    public ArrayList<Point2D.Double> keyPoints() {
        ArrayList<Point2D.Double> output
            = new ArrayList<Point2D.Double>();

        // Check all explicitly selected points in this diagram.

        for (GeneralPolyline path : paths) {
            for (Point2D.Double point : path.getPoints()) {
                output.add(point);
            }
        }

        // Check all intersections of straight line segments.
        // Intersections of two curves, or one curve and one segment,
        // are not detected at this time.

        LineSegment[] segments = getAllLineSegments();
        for (int i = 0; i < segments.length; ++i) {
            LineSegment s1 = segments[i];
            for (int j = i + 1; j < segments.length; ++j) {
                LineSegment s2 = segments[j];
                Point2D.Double p = Duh.segmentIntersection
                    (s1.p1, s1.p2, s2.p1, s2.p2);
                if (p != null) {
                    output.add(p);
                }
            }
        }

        return output;
    }

    /** Like add(), but instead add the point on a previously added
        segment that is nearest to the mouse pointer as measured by
        distance on the standard page. */
    public void addNearestSegment() {
        if (mprin == null) {
            return;
        }

        /** Location of the mouse on the standard page: */
        Point2D.Double xpoint = principalToStandardPage.transform(mprin);

        Point2D.Double nearPoint = null;
        Point2D.Double xpoint2 = new Point2D.Double();
        Point2D.Double xpoint3 = new Point2D.Double();
        double minDist = 0;

        for (GeneralPolyline path : paths) {
            Point2D.Double point;

            if (path.getSmoothingType() == GeneralPolyline.CUBIC_SPLINE) {
                // Locate a point on the cubic spline that is nearly
                // closest to this one.

                CubicSpline2D.DistanceInfo di
                    = ((SplinePolyline) path).getSpline(principalToStandardPage)
                    .closePoint(xpoint, 1e-9, 200);

                if (nearPoint == null || (di != null && di.distance < minDist)) {
                    if (di.distance < 0) {
                        throw new IllegalStateException
                            (xpoint + " => " + path + " dist = " + di.distance + " < 0");
                    }
                    standardPageToPrincipal.transform(di.point, di.point);
                    nearPoint = di.point;
                    minDist = di.distance;
                }
            } else {
                // Straight connect-the-dots polyline.
                Point2D.Double[] points = path.getPoints();

                for (int i = 0; i < points.length - 1; ++i) {
                    principalToStandardPage.transform(points[i], xpoint2);
                    principalToStandardPage.transform(points[i+1], xpoint3);
                    point = Duh.nearestPointOnSegment
                        (xpoint, xpoint2, xpoint3);
                    double dist = xpoint.distance(point);
                    if (nearPoint == null || dist < minDist) {
                        standardPageToPrincipal.transform(point, point);
                        nearPoint = point;
                        minDist = dist;
                    }
                }
            }

        }

        add(nearPoint);
        moveMouse(nearPoint);
    }

    public void toggleSmoothing() {
        JCheckBoxMenuItem check = editFrame.getSmoothingMenuItem();
        if (check.getState()) {
            smoothingType = GeneralPolyline.CUBIC_SPLINE;
        } else {
            smoothingType = GeneralPolyline.LINEAR;
        }
        processCurveDiscontinuity();
    }

    /** If we have a selected polyline, then end it and start a new one. */
    void processCurveDiscontinuity() {
        if (activeCurveNo >= 0) {
            Point2D.Double vertex = getActiveVertex();
            if (paths.get(activeCurveNo).size() == 1) {

                // If we end the old curve after just one vertex, a
                // dot will print. It feels arbitrary to print a dot
                // when you toggle the smoothing status, so delete the
                // old curve before starting the new one.

                // TODO Maybe there is a better way to indicate which
                // points should be circled (use the space bar,
                // maybe?)

                removeActiveCurve();
            }
            endCurve();

            if (vertex != null) {
                add(vertex);
            }
        }
    }

    void removeActiveCurve() {
        if (activeCurveNo == -1)
            return;

        paths.remove(activeCurveNo);
        --activeCurveNo;
        if (activeCurveNo == -1) {
            activeCurveNo = paths.size() - 1;
        }

        if (activeCurveNo >= 0) {
            activeVertexNo = paths.get(activeCurveNo).size() - 1;
        } else {
            activeVertexNo = -1;
        }
    }

    void draw(Graphics2D g, GeneralPolyline path, double scale) {
        path.draw(g, principalToStandardPage, scale);
    }

    public String getFilename() {
        return cropFrame.getFilename();
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        try {
            // Work-around for a bug that affects EB's PC as of 11/11.
            System.setProperty("sun.java2d.d3d", "false");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
        Point[] vertices = e.getVertices();
        diagramType = e.getDiagramType();
        newDiagram(vertices);
    }

    /** Start on a blank new diagram. */
    public void newDiagram() {
        // TODO Check before scratching an existing diagram.
        diagramType = (new DiagramDialog(null)).showModal();
        newDiagram((Point2D.Double[]) null);
    }

    protected void newDiagram(Point2D.Double[] vertices) {
        ArrayList<Point2D.Double> diagramPolyline = 
            new ArrayList<Point2D.Double>();

        boolean tracing = (vertices != null);
        clear();

        if (tracing && zoomFrame == null) {
            zoomFrame = new ImageZoomFrame();
        }

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

        if (diagramType.isTernary()) {
            NumberFormat pctFormat = new DecimalFormat("##0.0'%'");
            xAxis = new XAxisInfo(pctFormat);
            xAxis.name = "Z";
            yAxis = new YAxisInfo(pctFormat);
            zAxis = new ThirdTernaryAxisInfo(pctFormat);
            zAxis.name = "X";
            axes.add(zAxis);
            axes.add(yAxis);
            axes.add(xAxis);
        } else {
            NumberFormat format = new DecimalFormat("##0.0");
            xAxis = new XAxisInfo(format);
            yAxis = new YAxisInfo(format);
            axes.add(xAxis);
            axes.add(yAxis);
        }

        switch (diagramType) {
        case TERNARY_BOTTOM:
            {
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
                        height = Double.valueOf(heightS);
                    } catch (Exception e) {
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
                diagramPolyline.add(outputVertices[1]);
                diagramPolyline.add(outputVertices[0]);
                diagramPolyline.add(outputVertices[3]);
                diagramPolyline.add(outputVertices[2]);

                break;
            }
        case BINARY:
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

                principalToStandardPage = new Affine
                    (r.t, 0.0,
                     0.0, -r.t,
                     leftMargin, 1.0 - bottomMargin);
                if (diagramType == DiagramType.BINARY) {
                    // Add the endpoints of the diagram.
                    for (Point2D.Double point:
                             new Point2D.Double[] {
                                 new Point2D.Double(0.0, 0.0),
                                 new Point2D.Double(0.0, 100.0),
                                 new Point2D.Double(100.0, 100.0),
                                 new Point2D.Double(100.0, 0.0),
                                 new Point2D.Double(0.0, 0.0)}) {
                        diagramPolyline.add(point);
                    }
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
                            pageMaxes[i] = Double.valueOf(pageMaxStrings[i]);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Invalid number format.");
                        continue;
                    }
                    break;
                }

                double[] sideLengths = new double[3];
                sideLengths[ov1] = pageMaxes[0];
                sideLengths[ov2] = pageMaxes[1];
                double leftLength = sideLengths[LEFT_SIDE];
                double rightLength = sideLengths[RIGHT_SIDE];
                double bottomLength = sideLengths[BOTTOM_SIDE];

                Point2D.Double[] trianglePoints
                    = Duh.deepCopy(principalTrianglePoints);

                switch (diagramType) {
                case TERNARY_LEFT:
                    trianglePoints[TOP_VERTEX] = new Point2D.Double(0, leftLength);
                    trianglePoints[RIGHT_VERTEX] = new Point2D.Double(bottomLength, 0);
                    break;
                case TERNARY_TOP:
                    trianglePoints[LEFT_VERTEX]
                        = new Point2D.Double(0, 100.0 - leftLength);
                    trianglePoints[RIGHT_VERTEX]
                        = new Point2D.Double(rightLength, 100.0 - rightLength);
                    break;
                case TERNARY_RIGHT:
                    trianglePoints[LEFT_VERTEX]
                        = new Point2D.Double(100.0 - bottomLength, 0.0);
                    trianglePoints[TOP_VERTEX]
                        = new Point2D.Double(100.0 - rightLength, rightLength);
                    break;
                }

                // Add the endpoints of the diagram.
                diagramPolyline.add(trianglePoints[ov1]);
                diagramPolyline.add(trianglePoints[angleVertex]);
                diagramPolyline.add(trianglePoints[ov2]);

                if (tracing) {
                    originalToPrincipal = new TriangleTransform(vertices,
                                                                trianglePoints);
                }

                TriangleTransform xform = new TriangleTransform
                    (principalTrianglePoints,
                     TriangleTransform.equilateralTriangleVertices());
                Point2D.Double[] xformed = {
                    xform.transform(trianglePoints[0]),
                    xform.transform(trianglePoints[1]),
                    xform.transform(trianglePoints[2]) };
                double baseWidth = xformed[RIGHT_VERTEX].x
                    - xformed[LEFT_VERTEX].x;
                double leftHeight = xformed[TOP_VERTEX].y
                    - xformed[LEFT_VERTEX].y;
                double rightHeight = xformed[TOP_VERTEX].y
                    - xformed[RIGHT_VERTEX].y;
                double baseHeight = Math.max(leftHeight, rightHeight);
                r = new Rescale(baseWidth, leftMargin + rightMargin, maxPageWidth,
                                baseHeight, topMargin + bottomMargin, maxPageHeight);
                double rx = r.width - rightMargin;
                double bottom = r.height - bottomMargin;

                Point2D.Double[] trianglePagePositions =
                    { new Point2D.Double(leftMargin, topMargin + r.t * leftHeight),
                      new Point2D.Double(leftMargin + r.t * (xformed[1].x - xformed[0].x),
                                         topMargin),
                      new Point2D.Double(maxPageWidth - rightMargin,
                                         topMargin + r.t * rightHeight) };
                principalToStandardPage = new TriangleTransform
                    (trianglePoints, trianglePagePositions);

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

                // Add the endpoints of the diagram.
                for (Point2D.Double point: principalTrianglePoints) {
                    diagramPolyline.add(point);
                }
                diagramPolyline.add(principalTrianglePoints[0]);

                break;
            }
        }
        pageBounds = new Rectangle2D.Double(0.0, 0.0, r.width, r.height);
        try {
            standardPageToPrincipal = principalToStandardPage.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }

        {
            NumberFormat format = new DecimalFormat("0.000");
            pageXAxis = new AffineXAxisInfo(principalToStandardPage, format);
            pageXAxis.name = "page X";
            pageYAxis = new AffineYAxisInfo(principalToStandardPage, format);
            pageYAxis.name = "page Y";
            axes.add(pageXAxis);
            axes.add(pageYAxis);
        }

        if (tracing) {
            try {
                principalToOriginal = (PolygonTransform)
                    originalToPrincipal.createInverse();
            } catch (NoninvertibleTransformException e) {
                System.err.println("This transform is not invertible");
                System.exit(2);
            }
        }

        // Force the editor frame image to be initialized.
        zoomBy(1.0);

        // Add the polyline outline of the diagram to the diagram.
        int oldSmoothingType = smoothingType;
        smoothingType = GeneralPolyline.LINEAR;
        CompositeStroke oldLineStyle = lineStyle;
        lineStyle = CompositeStroke.getSolidLine();
        for (Point2D.Double point: diagramPolyline) {
            add(point);
        }
        smoothingType = oldSmoothingType;
        lineStyle = oldLineStyle;
        endCurve();

        if (tracing) {
            editFrame.setTitle("Edit " + diagramType + " "
                               + cropFrame.getFilename());
            zoomFrame.setImage(cropFrame.getImage());
            initializeCrosshairs();
            zoomFrame.getImageZoomPane().crosshairs = crosshairs;
        } else {
            editFrame.setTitle("Edit " + diagramType);
        }
        editFrame.pack();
        Rectangle rect = editFrame.getBounds();
        if (tracing) {
            zoomFrame.setLocation(rect.x + rect.width, rect.y);
            zoomFrame.setTitle("Zoom " + cropFrame.getFilename());
            zoomFrame.pack();
            zoomFrame.setVisible(true);
        }
        editFrame.setVisible(true);
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
        // TODO addVertexLocation (mandatory feature)
    }

    @Override
        public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
        public void mousePressed(MouseEvent e) {
        if (principalToStandardPage == null) {
            return;
        }
        add(mprin);
    }

    /** @return true if the diagram is currently being traced from
        another image. */
    boolean tracingImage() {
        return originalToPrincipal != null;
    }

    /** The mouse was moved in the edit window. Update the coordinates
        in the edit window status bar, repaint the diagram, and update
        the position in the zoom window,. */
    @Override
        public void mouseMoved(MouseEvent e) {
        repaintEditFrame();
    }

    public void updateMousePosition() {
        if (principalToStandardPage == null) {
            return;
        }

        Point mpos = getEditPane().getMousePosition();
        if (mpos != null && !preserveMprin) {

            double sx = mpos.getX() + 0.5;
            double sy = mpos.getY() + 0.5;

            boolean updateMprin = (mprin == null);

            if (!updateMprin) {
                // Leave mprin alone if it is already accurate to the
                // nearest pixel. This allows "jump to point"
                // operations' accuracy to exceed the screen
                // resolution, which matters when zooming the screen
                // or viewing the coordinates in the status bar.

                Point mprinScreen = Duh.floorPoint
                    (principalToScaledPage(scale).transform(mprin));
                updateMprin = !mprinScreen.equals(mpos);
            }

            if (updateMprin) {
                mprin = standardPageToPrincipal.transform(sx/scale, sy/scale);
            }
        }

        updateStatusBar();
    }

    void updateStatusBar() {
        if (mprin == null) {
            return;
        }

        StringBuilder status = new StringBuilder("");

        boolean first = true;
        for (AxisInfo axis : axes) {
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
            
        if (tracingImage()) {
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

        getEditPane().setPreferredSize
            (new Dimension((int) Math.ceil(pageBounds.width * scale),
                           (int) Math.ceil(pageBounds.height * scale)));

        if (originalToPrincipal != null) {
            // Initialize the faded and transformed image of the original
            // diagram.
            PolygonTransform originalToScreen = originalToPrincipal.clone();
            originalToScreen.preConcatenate(principalToScaledPage(scale));
            BufferedImage output = ImageTransform.run
                (originalToScreen, cropFrame.getImage(), Color.WHITE,
                 getEditPane().getPreferredSize());
            fade(output);
            editFrame.setImage(output);
        }

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
        repaintEditFrame();
    }

    EditPane getEditPane() { return editFrame.getEditPane(); }

    static String format(double d, int decimalPoints) {
        Formatter f = new Formatter();
        f.format("%." + decimalPoints + "f", d);
        return f.toString();
    }

    protected void newDiagram(Point[] verticesIn) {
        if (zoomFrame != null) {
            zoomFrame.setVisible(false);
        }
        newDiagram((verticesIn == null) ? null
                   : Duh.toPoint2DDoubles(verticesIn));
    }

    /** Compress the brightness into the upper third of the range
        0..255. */
    static int fade1(int i) {
        return 255 - (255 - i)/3;
    }

    static void fade(BufferedImage image) { 
        int width = image.getWidth();
        int height = image.getHeight();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                Color c = new Color(image.getRGB(x,y));
                c = new Color(fade1(c.getRed()),
                              fade1(c.getGreen()),
                              fade1(c.getBlue()));
                image.setRGB(x,y,c.getRGB());
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
    public LineSegment[] getAllLineSegments() {
        ArrayList<LineSegment> output
            = new ArrayList<LineSegment>();
         
        for (GeneralPolyline path : paths) {
            if (path.getSmoothingType() != GeneralPolyline.LINEAR) {
                // Easy case -- this path is smoothed between exactly
                // 2 points, which means it is equivalent to a
                // segment.

                if (path.size() == 2) {
                    output.add(new LineSegment(path.get(0), path.get(1)));
                }

            } else {
                Point2D.Double[] points = path.getPoints();
                for (int i = 1; i < points.length; ++i) {
                    output.add(new LineSegment(points[i-1], points[i]));
                }
            }
        }

        return output.toArray(new LineSegment[0]);
    }

    /** Draw a circle around each point in path. */
    void circleVertices(Graphics2D g, GeneralPolyline path, double scale) {
        Point2D.Double xpoint = new Point2D.Double();
        Affine p2d = principalToScaledPage(scale);
        double r = path.getLineWidth() * 2 * scale;

        for (Point2D.Double point: path.getPoints()) {
            p2d.transform(point, xpoint);
            g.fill(new Ellipse2D.Double(xpoint.x - r, xpoint.y - r, r * 2, r * 2));
        }
    }

    @Override
        public void mouseExited(MouseEvent e) {
        mprin = null;
        repaintEditFrame();
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

       @param weightX 0.0 = The anchor point lies along the left edge
       of the text block in baseline coordinates (if the text is
       rotated, then this edge may not be on the left in physical
       coordinates; for example, if the text is rotated by an angle of
       PI/2, then this will be the top edge in physical coordinates);
       0.5 = the anchor point lies along the vertical line (in
       baseline coordinates) that bisects the text block; 1.0 = the
       anchor point lies along the right edge (in baseline
       coordinates) of the text block

       @param weightY 0.0 = The anchor point lies along the top edge
       of the text block in baseline coordinates (if the text is
       rotated, then this edge may not be on top in physical
       coordinates; for example, if the text is rotated by an angle of
       PI/2, then this will be the right edge in physical
       coordinates); 0.5 = the anchor point lies along the horizontal
       line (in baseline coordinates) that bisects the text block; 1.0
       = the anchor point lies along the bottom edge (in baseline
       coordinates) of the text block
    */
    void drawHTML(Graphics g, View view, double scale, double angle,
                  double ax, double ay,
                  double weightX, double weightY) {
        double width = view.getPreferredSpan(View.X_AXIS);
        double height = view.getPreferredSpan(View.Y_AXIS);

        Graphics2D g2d = (Graphics2D) g;
        scale /= 800.0;

        AffineTransform xform = AffineTransform.getRotateInstance(angle);
        xform.scale(scale, scale);
        Point2D.Double xpoint = new Point2D.Double();
        xform.transform
            (new Point2D.Double(width * weightX, height * weightY), xpoint);

        ax -= xpoint.x;
        ay -= xpoint.y;

        AffineTransform oldxform = g2d.getTransform();
        g2d.translate(ax, ay);
        g2d.transform(xform);
        view.paint(g, new Rectangle(0, 0,
                                    (int) Math.ceil(width), (int) Math.ceil(height)));
        g2d.setTransform(oldxform);
    }

}
