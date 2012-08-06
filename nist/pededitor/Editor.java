package gov.nist.pededitor;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Shape;
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import Jama.Matrix;

import org.codehaus.jackson.annotate.JsonIgnore;

// TODO (optional) Warn users when there exists an autosave version of
// a file that is newer than the regular saved version.

// TODO (optional, easy?) Add "Print visible region" as an alternative
// to "Print". (You can always collapse the margins to the region you
// want to show and then print that, but that's slow and might be hard
// to undo.)

// TODO (mandatory, 2 days) Make regular open symbols look as nice as the
// GRUMP-converted open symbols do.

// TODO (mandatory, 1 week) Enable filling of regions with cusps. As
// things currently stand, it's not possible to fill a semicircle.

// TODO (mandatory) Allow different densities of dotted and dashed
// lines, just as is the case for railroad ties.

// TODO (mandatory, 1 day): At this point, the rule that tie lines
// have to end at vertexes of the diagram is no longer needed and not
// difficult to eliminate. Tie lines ending on rulers without extra
// steps could also be enabled (and the extra steps are unintuitive).

// TODO (optional but in my opinion super useful for general
// digitization tasks, 3 days) Currently the tangent dialog displays a
// slope value that is just the tangent of the angle and frankly is
// not very useful. The user could select the rise and run variables
// instead, so, for example, the slope could show the derivative of
// the liquidus with respect to composition. This would also
// facilitate drawing straight lines whose slopes are defined in terms
// of user variables.

// TODO (Optional) Allow shift-L and/or shift-A to cycle around likely
// candidates the same way that shift-P does. In other words, if
// shift-L doesn't give you what you want the first time, just try it
// again.

// TODO (optional, 2 weeks) Keep an undifferentiated list of
// decorations, which allows any decoration to be moved up or down on
// the screen.

// TODO (optional, 1 day) In the text dialog, link state-on buttons to
// state-offf buttons so that if a region is selected and a state-on
// button is pressed, then the state-on text is pasted before the
// selection and the state-off selection is pasted after it.

// TODO (mandatory?, preexisting, 1 week) Apply a gradient to all control
// points on a curve. Specifically, apply the following transformation
// to all points on the currently selected curve for which $variable
// is between v1 and v2: "$variable = $variable + k * ($variable - v1)
// / (v2 - v1)"

// TODO (optional, TAV not so important unless someone complains) You
// can't make tie lines that cross the "endpoint" of a closed curve.
// Fix this somehow.

// TODO (optional, 3 days) For opaque and boxed labels, allow users to decide
// how much extra white space to include on each side.

// TODO (optional) Grid lines might occasionally be nice. At one point
// I would have liked to have them

// TODO (optional, TV leave it alone and see if anyone complains) Make
// it easier to edit multiple-line labels.

// TODO (optional) Support of GRUMP-style explicit assignment of ruler
// limits such as label skip, label density, and so on. The downside
// of this is that it would make the interface even more complicated,
// and people might get in the habit of doing explicit assignments
// even though the automatic ones are pretty good.

// TODO (optional) It should be easy to enable 'copy region' that is
// analogous to 'move region'. Maybe the user should just be asked
// whether to move a region or just a single curve.

// TODO (optional) Resize all labels at once by a given factor. This
// is more useful during conversion from GRUMP to PED fonts.

// TODO (optional) Curve tags, such as temperature, liquidus, solidus, and
// presumably user-defined tags too.

// TODO (optional) Make cross sections of quaternary diagrams
// first-class.

// TODO (optional) As Chris suggested, allow input images to be
// rotated (currently images must be within about 45 degrees of the
// correct orientation). This is hardly a show-stopper; you can load
// the image in MS-Paint and rotate it in a minute, and Chris said
// that was a good solution.

// TODO (optional, 2 weeks) Right-click popup menus. I don't think
// experienced users (the digitizers) would make much use of them, but
// occasional users might. Forcing the users to remember shortcuts
// isn't very friendly, and you can't use the mouse to click on an
// ordinary menu if you're already using the mouse to identify the
// location the operation should apply to.

// TODO (optional) Other commonly used shapes (equilateral triangle,
// square, circle) whose scale and orientation are defined by
// selecting two points. Ideally the changes would be visible in real
// time as you move the mouse around. (Auto-positioning has made this
// slightly less important than before; squares, rectangles, and
// equilateral triangles are easy if one side is horizontal.)

// TODO (optional) Eutectic and peritectic points.

// TODO (optional) Allow the diagram domain and range to be expanded.
// Right now, you can expand the margins, change the aspect ratio, or
// rescale the axes, but it is awkward to extend a partial ternary to
// create a full ternary, for example. The 'move region' command is an
// OK work-around, at least for binary diagrams.

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

// TODO (2-3 weeks, optional) "Undo" option. Any good drawing program
// has that feature, but making everything undoable would take a
// couple weeks. (Every time an operation that changes the diagram is
// completed, push the inverse operation onto a stack.)

// TODO (optional) More compact representation for symbol sets in the
// PED format.

// TODO (preexisting in viewer) Periodic table integration.

// TODO (optional, TAV leave for now see if someone complains) weight
// vs mole percent enhancements. Text at an angle gets distorted, and
// that should really be fixed.

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

// TODO Investigate whether JavaFX is really a plausible alternative.
// (Answer: it's not quite ready, it seems.)

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
public class Editor extends Diagram
    implements CropEventListener, MouseListener, MouseMotionListener,
               Observer {
    abstract class Action extends AbstractAction {
        private static final long serialVersionUID = 1834208008403586162L;

        Action(String name) {
            super(name);
        }
    }

    class ImageBlinker extends TimerTask {
        @Override public void run() {
            backgroundImageEnabled = !backgroundImageEnabled;
            redraw();
        }
    }

    /** Return the path to use for autosave files. If getFilename()
        returns something useful, then switch the file name from
        /path/foo.ped to /path/#foo.ped# like emacs does. Otherwise use
        the name AUTOSAVE.ped. */
    Path getAutosave() {
        String fn = getFilename();
        if (fn != null) {
            return getAutosave(FileSystems.getDefault().getPath(fn));
        } else {
            return getAutosave(null);
        }
    }

    /** Return the autosave file corresponding to the given normal PED
        file. A value is returned regardless of whether the autosave
        file exists or not. If file is null, return the default
        autosave file. */
    Path getAutosave(Path file) {
        if (file == null) {
            String dir = getCurrentDirectory();
            if (dir == null) {
                return null;
            }
            return FileSystems.getDefault().getPath(dir, "AUTOSAVE.ped");
        }
        return FileSystems.getDefault().getPath
            (file.getParent().toString(), '#' + file.getFileName().toString());
    }

    class FileSaver extends TimerTask {
        @Override public void run() {
            if (saveNeeded) {
                Path file = getAutosave();
                try {
                    saveAsPED(file);
                    System.out.println("Saved '" + file + "'");
                    autosaveFile = file;
                } catch (IOException x) {
                    System.err.println("Could not save '" + file + "': " + x);
                }
            }
            fileSaver = null;
        }
    }

    class CloseListener extends WindowAdapter
                                implements WindowListener
    {
        @Override public void windowClosing(WindowEvent e) {
            close();
        }
    }

    public static boolean editable(DecorationHandle hand) {
        return hand instanceof LabelHandle
            || hand instanceof TieLineHandle
            || hand instanceof RulerHandle;
    }

    public void editSelection() {
        String errorTitle = "Cannot edit selection";
        if (selection == null) {
            // Find the nearest editable item, and edit it.

            for (DecorationHandle handle: nearestHandles()) {
                if (editable(handle)) {
                    selection = handle;
                    break;
                }
            }

            if (selection == null) {
                showError("There are no editable items.", errorTitle);
                return;
            }
        }

        if (selection instanceof LabelHandle) {
            editLabel(((LabelHandle) selection).getDecoration().getIndex());
        } else if (selection instanceof TieLineHandle) {
            edit((TieLineHandle) selection);
        } else if (selection instanceof RulerHandle) {
            edit((RulerHandle) selection);
        } else {
            showError("This item does not have a special edit function.",
                      errorTitle);
            return;
        }
    }

    public void edit(TieLineHandle hand) {
        TieLine item = hand.getItem();
        int lineCnt = askNumberOfTieLines(item.lineCnt);
        if (lineCnt >= 0) {
            item.lineCnt = lineCnt;
            propagateChange();
        }
    }

    public void edit(RulerHandle hand) {
        LinearRuler item = hand.getItem();

        if (getRulerDialog().showModal(item, axes)) {
            propagateChange();
        }
    }

    private static final String PREF_DIR = "dir";
    private static final long AUTO_SAVE_DELAY = 5 * 60 * 1000; // 5 minutes

    static final protected double MOUSE_UNSTICK_DISTANCE = 30; /* pixels */
    static protected Image crosshairs = null;

    protected CropFrame cropFrame = new CropFrame();
    protected EditFrame editFrame = new EditFrame(this);
    protected ImageZoomFrame zoomFrame = null;
    protected VertexInfoDialog vertexInfo = new VertexInfoDialog(editFrame);
    protected LabelDialog labelDialog = null;
    protected RulerDialog rulerDialog = null;
    protected JColorChooser colorChooser = null;
    protected JDialog colorDialog = null;

    LabelDialog getLabelDialog() {
        if (labelDialog == null) {
            labelDialog = new LabelDialog(editFrame, "Add label",
                                          getFont().deriveFont(16.0f));
        }
        return labelDialog;
    }

    RulerDialog getRulerDialog() {
        if (rulerDialog == null) {
            rulerDialog = new RulerDialog(editFrame, "Add ruler");
        }
        return rulerDialog;
    }

    protected transient double scale;
    protected transient BufferedImage originalImage;
    protected transient boolean editorIsPacked = false;

    /** The item (vertex, label, etc.) that is selected, or null if nothing is. */
    protected transient DecorationHandle selection;
    /** If the timer exists, the original image (if any) upon which
        the new diagram is overlaid will blink. */
    transient Timer imageBlinker = null;
    transient Timer fileSaver = null;
    /** True if imageBlinker is enabled and the original image should
        be displayed in the background at this time. */
    transient boolean backgroundImageEnabled;

    protected transient boolean preserveMprin = false;
    protected transient boolean isShiftDown = false;

    /** autosaveFile is null unless an autosave event happened and the
        resulting autosavefile has not been deleted by this program. */
    protected transient Path autosaveFile = null;

    /** True if the program already tried and failed to load the image
        named originalFilename. */
    protected transient boolean triedToLoadOriginalImage = false;

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

    protected transient double lineWidth = STANDARD_LINE_WIDTH;
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

    /** Current mouse position expressed in principal coordinates.
     It's not always sufficient to simply read the mouse position in
     the window, because after the user jumps to a preselected point,
     the integer mouse position is not precise enough to express that
     location. */
    protected transient Point2D.Double mprin = null;

    /** When the user presses 'p' or 'P' repeatedly to locate points
        close to a starting point, this holds the initial mouse
        location (in principal coordinates) so it is possible to
        identify what the next-closest point may be. */
    protected transient Point2D.Double principalFocus = null;

    public Editor() {
        init();
        tieLineDialog.setFocusableWindowState(false);
        vertexInfo.setDefaultCloseOperation
            (WindowConstants.DO_NOTHING_ON_CLOSE);
        editFrame.setDefaultCloseOperation
            (WindowConstants.DO_NOTHING_ON_CLOSE);
        editFrame.addWindowListener(new CloseListener());
        getEditPane().addMouseListener(this);
        getEditPane().addMouseMotionListener(this);
        cropFrame.setDefaultCloseOperation
            (WindowConstants.HIDE_ON_CLOSE);
        cropFrame.addCropEventListener(this);
        addObserver(this);
    }

    public void initializeZoomFrame() {
        if (zoomFrame != null)
            return;
        zoomFrame = new ImageZoomFrame();
        zoomFrame.setFocusableWindowState(false);
        Rectangle rect = editFrame.getBounds();
        zoomFrame.setLocation(rect.x + rect.width, rect.y);
        zoomFrame.pack();
    }

    private void init() {
        scale = BASE_SCALE;
        mprin = null;
        scaledOriginalImages = null;
        vertexInfo.setAngle(0);
        vertexInfo.setSlope(0);
        vertexInfo.setLineWidth(lineWidth);
        mouseIsStuck = false;
        paintSuppressionRequestCnt = 0;
        setBackgroundType(EditFrame.BackgroundImageType.LIGHT_GRAY);
        tieLineDialog.setVisible(false);
        tieLineCorners = new ArrayList<>();
        originalImage = null;
        triedToLoadOriginalImage = false;
    }

    @Override void clear() {
        selection = null;
        if (fileSaver != null) {
            fileSaver.cancel();
            fileSaver = null;
        }
        autosaveFile = null;
        super.clear();
        init();
    }

    public void close() {
        if (verifyCloseDiagram
            (new Object[] {"Save and quit", "Quit without saving", "Cancel"})) {
            System.exit(0);
        }
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

    @JsonIgnore RulerDecoration getSelectedRuler() {
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

    @Override public void addTag(String tag) {
        super.addTag(tag);
        editFrame.addTag(tag);
    }

    public void addTag() {
        String tag = JOptionPane.showInputDialog(editFrame, "Tag");
        if (tag != null && tags.add(tag)) {
            addTag(tag);
        }
    }

    @Override public void removeTag(String tag) {
        super.removeTag(tag);
        editFrame.removeTag(tag);
    }

    public void put() {
        String[] labels = { "Key", "Value" };
        String[] values = { "", "" };
        StringArrayDialog dog = new StringArrayDialog
            (editFrame, labels, values, null);
        dog.setTitle("Add key/value pair");
        values = dog.showModal();
        if (values == null || values[0].isEmpty()) {
            return;
        }

        put(values[0], values[1]);
    }


    public void listKeyValues() {
        Set<Map.Entry<String,String>> entries = keyValues.entrySet();
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog
                (editFrame, "No key/value pairs have been assigned yet.");
            return;
        }
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
            if (values[i].isEmpty()) {
                removeKey(keys[i]);
            } else if (!values[i].equals(get(keys[i]))) {
                put(keys[i], values[i]);
            }
        }
    }

    public synchronized void setBackgroundType
        (EditFrame.BackgroundImageType value) {
        // Turn blinking off
        if (imageBlinker != null) {
            imageBlinker.cancel();
        }
        imageBlinker = null;
        darkImage = null;

        if (value == EditFrame.BackgroundImageType.BLINK) {
            imageBlinker = new Timer("ImageBlinker", true);
            imageBlinker.scheduleAtFixedRate(new ImageBlinker(), 500, 500);
            backgroundImageEnabled = true;
        }

        // The rest is handed in paintDiagram().

        redraw();
    }

    /** Reset the location of all vertices and labels that have the
        same location as the selection to mprin.
        
        @param moveAll If true, all items located at the selected
        point will move moved. If false, only the selected item itself
        will be moved.
    */
    public void moveRegion() {
        VertexHandle vhand = getVertexHandle();
        String errorTitle = "Cannot move region";

        if (vhand == null) {
            showError
                ("Draw a curve to identify the boundary of the region "
                 + "to be moved.",
                 errorTitle);
            return;
        }

        if (mprin == null) {
            showError
                ("<html><p>Move the mouse to the target location. "
                 + "Use the 'R' shortcut key or keyboard menu controls "
                 + "instead of selecting the menu item using the mouse."
                 + "</p></html>",
                 errorTitle);
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
            }
        }

        propagateChange();
    }

    /** Reset the location of all vertices and labels that have the
        same location as the selection to mprin.
        
        @param moveAll If true, all items located at the selected
        point will move moved. If false, only the selected item itself
        will be moved.
    */
    public void moveSelection(boolean moveAll) {
        String errorTitle = "Cannot move selection";
        if (selection == null) {
            showError
                ("You must select an item before you can move it.",
                 errorTitle);
            return;
        }

        if (mprin == null) {
            showError
                ("You must move the mouse to the target destination " +
                 "before you can move items.",
                 errorTitle);
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
        String errorTitle = "Cannot copy selection";

        if (selection == null) {
            showError
                ("You must select an item before you can copy it.",
                 errorTitle);
            return;
        }

        if (mprin == null) {
            showError
                ("You must move the mouse to the target destination " +
                 "before you can copy.",
                 errorTitle);
            return;
        }

        if (mouseIsStuckAtSelection()) {
            unstickMouse();
        }

        selection = selection.copy(mprin);
    }

    /** Change the selection's color. */
    public void colorSelection() {
        String errorTitle = "Cannot change color";

        if (selection == null) {
            showError
                ("You must select an item before you can change its color.",
                 errorTitle);
            return;
        }

        if (colorChooser == null) {
            colorChooser = new JColorChooser();
            colorDialog = JColorChooser.createDialog
            (editFrame, "Choose color", true, colorChooser,
             new ActionListener() {
                 @Override public void actionPerformed(ActionEvent e) {
                     Editor.this.selection.getDecoration()
                         .setColor(colorChooser.getColor());
                 }
             },
             null);
            colorDialog.pack();
        }

        Color c = thisOrBlack(selection.getDecoration().getColor());
        colorChooser.setColor(c);
        colorDialog.setVisible(true);
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
        redraw();
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

        redraw();
    }

    /* The diagram has not changed, but it needs to be redrawn. The
       mouse cursor may have been recentered, the scale may have
       changed, an item may have been selected... */
    public void redraw() {
        if (suppressUpdateCnt > 0) {
            return;
        }
        setChanged();
        notifyObservers(null);
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

    /** Most of the information required to paint the EditPane is part
        of this object, so it's simpler to do the painting from
        here. */
    public void paintEditPane(Graphics g) {
        updateMousePosition();
        paintDiagramWithSelection((Graphics2D) g, scale);
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

            Color oldColor = csel.getColor();
            csel.setColor(toColor(ap.position));

            int vertexNo = getVertexHandle().vertexNo
                + (insertBeforeSelection ? 0 : 1);
            path.add(vertexNo, extraVertex);
            csel.draw(g, scale);
            path.remove(vertexNo);
            csel.setColor(oldColor);
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
        drawLabel(g, ldec.index, scale);

        if (label.getXWeight() != 0.5 || label.getYWeight() != 0.5) {
            // Mark the anchor with a circle -- either a solid circle
            // if the selection handle is the anchor, or a hollow
            // circle if the selection handle is the label's center.
            double r = Math.max(scale * 2.0 / BASE_SCALE, 4.0);
            Point2D.Double p = new Point2D.Double
                (label.getXWeight(), label.getYWeight());
            labelToScaledPage(ldec.index, scale).transform(p, p);
            Ellipse2D circle = new Ellipse2D.Double
                (p.x - r, p.y - r, r * 2, r * 2);
            if (getLabelHandle().handle == LabelHandleType.CENTER) {
                g.draw(circle);
            } else {
                g.fill(circle);
            }
        }

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

    /** Paint the diagram to the given graphics context. Highlight the
        selection, if any, and print the background image if
        necessary. Also indicate the autoselect point if the shift key
        is depressed.

        @param scale Scaling factor to convert standard page
        coordinates to device coordinates.
    */
    public void paintDiagramWithSelection(Graphics2D g, double scale) {
        if (principalToStandardPage == null
            || paintSuppressionRequestCnt > 0) {
            return;
        }

        if (labelViews.size() != labels.size()) {
            initializeLabelViews();
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

        { // Draw the background
            EditFrame.BackgroundImageType back = editFrame.getBackgroundImage();
            boolean showBackgroundImage = tracingImage()
                && back != EditFrame.BackgroundImageType.NONE
                && (back != EditFrame.BackgroundImageType.BLINK
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

        boolean showSel = selection != null;

        ArrayList<Decoration> decorations = getDecorations();

        Decoration sel = showSel ? selection.getDecoration() : null;
        for (Decoration decoration: decorations) {
            if (!decoration.equals(sel)) {
                g.setColor(thisOrBlack(decoration.getColor()));
                decoration.draw(g, scale);
            }
        }

        if (selection != null) { // Draw the selection
            try (UpdateSuppressor us = new UpdateSuppressor()) {
                    Decoration dec = selection.getDecoration();
                    Color oldColor = dec.getColor();
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
                }
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
        redraw();
    }

    public void setFill(StandardFill fill) {
        String errorTitle = "Cannot change fill settings";
        GeneralPolyline path = getActiveCurve();
        if (path == null) {
            showError
                ("Fill settings can only be changed when a curve is selected.",
                 errorTitle);
            return;
        }

        if (!path.isClosed()) {
            showError
                ("Fill settings can only be changed for closed curves.",
                 errorTitle);
            return;
        }

        setFill(path, fill);
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

    /** Update the tangency information to display the slope at the
        given vertex. */
    public void showTangent(BoundedParam2DHandle hand) {
        showTangent(hand.getDecoration(), hand.getT());
    }

    public void showTangent(Decoration dec, double t) {
        BoundedParam2D param = ((BoundedParameterizable2D) dec).getParameterization();
        if (dec instanceof CurveDecoration) {
            CurveDecoration cdec = (CurveDecoration) dec;
            GeneralPolyline path = cdec.getItem();
            if (path instanceof Polyline && t == Math.floor(t)) {

                // For polylines, insertBeforeSelection gives a hint which
                // of the two segments adjoining a vertex is the one whose
                // derivative we want.
                t = BoundedParam2Ds.constrainToDomain
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
        GeneralPolyline path = getCurveForAppend();
        VertexHandle vhand = getVertexHandle();
        int addPos = vhand.vertexNo + (insertBeforeSelection ? -1 : 0 );
        add(path, addPos, point);
        if (!insertBeforeSelection) {
            ++vhand.vertexNo;
        }
        showTangent(vhand);
    }

    @Override void removeCurve(int curveNo) {
        GeneralPolyline path = paths.get(curveNo);

        // If an incomplete tie line selection refers to this curve,
        // then stop selecting a tie line.

        for (PathAndT pat: tieLineCorners) {
            if (pat.path == path) {
                tieLineDialog.setVisible(false);
                tieLineCorners = null;
                break;
            }
        }
        super.removeCurve(curveNo);
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

        addArrow(mprin, lineWidth, pageToPrincipalAngle(theta));
    }

    void tieLineCornerSelected() {
        String errorTitle = "Invalid tie line selection";
        VertexHandle vhand = getVertexHandle();
        if (vhand == null) {
            showError("You must select a vertex.", errorTitle);
            return;
        }

        CurveDecoration csel = vhand.getDecoration();
        PathAndT pat = new PathAndT(csel.getItem(), vhand.vertexNo);

        int oldCnt = tieLineCorners.size();

        if ((oldCnt == 1 || oldCnt == 3)
            && pat.path != tieLineCorners.get(oldCnt-1).path) {
            showError
                ("This selection must belong to the same\n" +
                 "curve as the previous selection.",
                 errorTitle);
            return;
        }

        if ((oldCnt == 1) && pat.t == tieLineCorners.get(0).t) {
            showError
                ("The second point cannot be the same as the first.",
                 errorTitle);
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
        add(tie);
    }

    int askNumberOfTieLines(int oldCount) {
        String errorTitle = "Invalid tie line count";
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

                showError("Enter a positive integer.", errorTitle);
            } catch (NumberFormatException e) {
                showError("Invalid number format.", errorTitle);
            }
        }
    }

    public void addRuler() {
        String errorTitle = "Cannot create ruler";
        GeneralPolyline path = getActiveCurve();
        if (path == null || path.size() != 2) {
            showError
                ("Before you can create a new ruler,\n"
                 + "you must create and select a curve\n"
                 + "consisting of exactly two vertices\n"
                 + "which will become the rulers' endpoints.\n",
                 errorTitle);
            return;
        }

        LinearAxis axis = axes.get(0);

        LinearRuler r = new LinearRuler();
        r.fontSize = currentFontSize();
        r.tickPadding = 0.0;
        r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        r.drawSpine = true;
        r.lineWidth = lineWidth;
        r.axis = axis;

        if (diagramType.isTernary()) {
            r.tickType = LinearRuler.TickType.V;
        }

        VertexHandle vhand = getVertexHandle();
        r.startPoint = path.get(1 - vhand.vertexNo);
        r.endPoint = path.get(vhand.vertexNo);

        if (!getRulerDialog().showModal(r, axes)) {
            return;
        }

        removeCurve(vhand.getCurveNo());
        add(r);
        selection = new RulerHandle(rulers.size() - 1, RulerHandleType.END);
    }

    @Override public void rename(LinearAxis axis, String name) {
        if (axis.name != null) {
            editFrame.removeVariable((String) axis.name);
        }
        super.rename(axis, name);
        editFrame.addVariable(name);
    }

    @Override public void add(LinearAxis axis) {
        super.add(axis);
        editFrame.addVariable((String) axis.name);
    }

    @Override public void remove(LinearAxis axis) {
        RulerDecoration rdec = getSelectedRuler();
        if (rdec != null && axis == rdec.getItem().axis) {
            selection = null;
        }
        super.remove(axis);
        editFrame.removeVariable((String) axis.name);
    }

    public void addVariable() {
        String errorTitle = "Cannot add variable";
        GeneralPolyline path = getActiveCurve();
        if (path == null || path.size() != 3) {
            showError
                ("<html><p>To create a new variable, you must "
                 + "first select a curve consisting of exactly three "
                 + "points.</p></html>",
                 errorTitle);
            return;
        }

        String[] values = { "", "0", "0", "1" };

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
                    (editFrame, "Please enter a variable name.");
                continue;
            }

            double[] dvs = new double[3];
            boolean ok = true;
            boolean maybePercentage = true;
            for (int i = 0; i < dvs.length; ++i) {
                try {
                    dvs[i] = ContinuedFraction.parseDouble(values[i+1]);
                    maybePercentage = maybePercentage &&
                        (dvs[i] >= 0 && dvs[i] <= 1);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog
                        (editFrame, "Invalid number format '" + values[i+1] + "'");
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }

            Point2D.Double p0 = path.get(0);
            Point2D.Double p1 = path.get(1);
            Point2D.Double p2 = path.get(2);

            Matrix xform = new Matrix
                (new double[][] {{p0.x, p0.y, 1},
                                 {p1.x, p1.y, 1},
                                 {p2.x, p2.y, 1}});
            try {
                Matrix m = xform.solve(new Matrix(dvs, 3));
                DecimalFormat format =
                    (maybePercentage
                    && JOptionPane.showConfirmDialog
                    (editFrame,
                     "Display variable as a percentage?",
                     "Display format",
                     JOptionPane.YES_NO_OPTION)
                     == JOptionPane.YES_OPTION)
                    ? STANDARD_PERCENT_FORMAT
                    : new DecimalFormat("0.0000");
                LinearAxis axis = new LinearAxis
                    (format, m.get(0,0), m.get(1,0), m.get(2,0));
                axis.name = values[0];
                add(axis);
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

    @Override public void setUsingWeightFraction(boolean b) {
        super.setUsingWeightFraction(b);
        editFrame.setUsingWeightFraction(b);
    }

    @Override protected boolean moleToWeightFraction() {
        boolean res = super.moleToWeightFraction();
        if (res && mprin != null) {
            moveMouse(moleToWeightFraction(mprin));
        }
        return res;
    }

    @Override protected boolean weightToMoleFraction() {
        boolean res = super.weightToMoleFraction();
        if (res && mprin != null) {
            moveMouse(weightToMoleFraction(mprin));
        }
        return res;
    }

    public void computeFraction() {
        computeFraction(isUsingWeightFraction());
    }

    /** Have the user enter a string; parse the string as a compound;
        and set the mouse position to the principal coordinates that
        correspond to that compound, if the compound can be expressed
        as a product of the diagram components. For binary diagrams,
        the Y coordinate will be left unchanged from its original
        value.

        @param isWeight If true, use weight fractions. If false, use
        mole fractions.
    */
    public void computeFraction(boolean isWeight) {
        String errorTitle = "Could not compute component ratios";
        double[][] componentElements = getComponentElements();

        ArrayList<Side> sides = new ArrayList<>();
        ArrayList<Side> badSides = new ArrayList<>();
        for (Side side: sidesThatCanHaveComponents()) {
            if (diagramComponents[side.ordinal()] == null) {
                JOptionPane.showMessageDialog
                    (editFrame,
                     "The " + side + " diagram component is not defined.\n"
                     + "Define it with the \"Chemistry/Components/Set "
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
            int sideNo = -1;
            for (Side side: badSides) {
                ++sideNo;
                if (sideNo > 0) {
                    message.append(", ");
                }
                message.append(side.toString());
            }
            JOptionPane.showMessageDialog(editFrame, message.toString());
            return;
        }

        ChemicalString.Match m = null;
        String originalCompound = null;
        String compound = null;
        for (;;) { // Keep trying until user aborts or enters valid input
            originalCompound = JOptionPane.showInputDialog
                (editFrame,
                 "The string you enter will be placed in the clipboard.\n"
                 + "In Windows, you may press Control-V later to paste\n"
                 + "the text into a label's text box.\n"
                 + "Chemical formula:",
                 "Compute mole/weight fraction", JOptionPane.PLAIN_MESSAGE);
            if (originalCompound == null) {
                return;
            }
            originalCompound = originalCompound.trim();
            
            // Attempt to convert the input from HTML to regular text.
            // This should be mostly harmless even if it was regular
            // text to begin with, since normal chemical formulas
            // don't include <, >, or & anyhow.
            compound = htmlToText(originalCompound);
            if (compound.isEmpty()) {
                return;
            }
            m = ChemicalString.composition(compound);
            if (m == null) {
                JOptionPane.showMessageDialog
                    (editFrame, "Parse error in formula");
                continue;
            } else if (m.endIndex < compound.length()) {
                JOptionPane.showMessageDialog
                    (editFrame, "Parse error at <<HERE in '"
                     + compound.substring(0, m.endIndex)
                     + "<<HERE" + compound.substring(m.endIndex) + "'");
                continue;
            } else {
                break;
            }
        }

        try {
            StringSelection sel = new StringSelection(originalCompound);
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

        double componentsSum = 0;
        boolean sumIsKnown = true;
        for (Side side: sides) {
            LinearAxis axis = getAxis(side);
            if (axis == null) {
                sumIsKnown = false;
                break;
            }
            componentsSum += axis.value(fractions);
        }
        if (sumIsKnown && Math.abs(1 - componentsSum) > 1e-4) {
            showError("Components do not sum to 1", errorTitle);
            return;
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
                throw new IllegalStateException
                    ("Side " + side + " should not have an "
                     + "associated component.");
            }
        }

        if (isWeight) {
            fractions = moleToWeightFraction(fractions);
        }

        mouseIsStuck = true;
        if (!moveMouse(fractions)) {
            showError
                ("<html>You requested the coordinates<br>"
                 + principalToPrettyString(fractions)
                 + "<br>which lie outside the boundary of the diagram.</html>",
                 "Could not move mouse");
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
                showError
                    ("You must first select a curve or label whose\ncoordinates "
                     + "are to be copied.", "Cannot perform operation");
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

    public void copyAllTextToClipboard() {
        try {
            StringBuilder res = new StringBuilder();
            for (String s: getAllText()) {
                res.append(s);
                res.append("\n");
            }
            StringSelection sel = new StringSelection(res.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents
                (sel, sel);
        } catch (HeadlessException x) {
            throw new IllegalArgumentException
                ("Can't set clipboard in a headless environment:" + x);
        }
    }

    public void copyAllFormulasToClipboard() {
        try {
            StringBuilder res = new StringBuilder();
            for (String s: getAllFormulas()) {
                res.append(s);
                res.append("\n");
            }
            StringSelection sel = new StringSelection(res.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents
                (sel, sel);
        } catch (HeadlessException x) {
            throw new IllegalArgumentException
                ("Can't set clipboard in a headless environment:" + x);
        }
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
        return (mprin == null) ? -1 : nearestLabelNo(mprin);
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

            if (sel instanceof BoundedParam2DHandle) {
                showTangent((BoundedParam2DHandle) sel);
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
        propagateChange();
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
        str = str.trim();

        setDiagramComponent(side, str.isEmpty() ? null : str);
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
                 + "then press 't' to add the label.");
            return;
        }

        if (mouseIsStuckAtSelection() && getSelectedLabel() != null) {
            unstickMouse();
        }
        double x = mprin.x;
        double y = mprin.y;

        LabelDialog dog = getLabelDialog();
        double fontSize = dog.getFontSize();
        dog.reset();
        dog.setTitle("Add Label");
        dog.setFontSize(fontSize);
        AnchoredLabel newLabel = dog.showModal();
        if (newLabel == null || !check(newLabel)) {
            return;
        }

        newLabel.setAngle(pageToPrincipalAngle(newLabel.getAngle()));
        newLabel.setX(x);
        newLabel.setY(y);
        add(newLabel);
        selection = new LabelHandle(labels.size() - 1, LabelHandleType.ANCHOR);
        mouseIsStuck = true;
    }

    public boolean check(AnchoredLabel label) {
        if (label.getText().isEmpty()) {
            return false;
        }
        if (label.getFontSize() <= 0) {
            showError("Font size is not a positive number", "Cannot perform operation");
            return false;
        }
        return true;
    }

    public void editLabel(int index) {
        AnchoredLabel label = (AnchoredLabel) labels.get(index).clone();
        label.setAngle(principalToPageAngle(label.getAngle()));
        LabelDialog dog = getLabelDialog();
        dog.setTitle("Edit Label");
        dog.set(label);
        AnchoredLabel newLabel = dog.showModal();
        if (newLabel == null || !check(newLabel)) {
            return;
        }

        newLabel.setAngle(pageToPrincipalAngle(newLabel.getAngle()));
        newLabel.setX(label.getX());
        newLabel.setY(label.getY());
        newLabel.setBaselineXOffset(label.getBaselineXOffset());
        newLabel.setBaselineYOffset(label.getBaselineYOffset());
        labels.set(index, newLabel);
        labelViews.set(index, toView(newLabel));
        propagateChange();
    }

    /** Invoked from the EditFrame menu */
    public void setLineStyle(StandardStroke lineStyle) {
        this.lineStyle = lineStyle;
        if (selection != null) {
            selection.getDecoration().setLineStyle(lineStyle);
        }
    }

    /** Move the mouse pointer so its position corresponds to the
        given location in principal coordinates. Return true if it was
        possible to go to that location. */
    boolean moveMouse(Point2D.Double point) {
        mprin = point;
        if (principalToStandardPage == null) {
            return false;
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

            redraw();
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
                return false;
            }
        }

        return true;

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
        redraw();
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
                if (nc.decoration instanceof BoundedParameterizable2D) {
                    BoundedParam2D param = ((BoundedParameterizable2D) nc.decoration)
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
    @Override public ArrayList<Point2D.Double> keyPoints() {
        ArrayList<Point2D.Double> res = super.keyPoints();

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
        BoundedParam2D c
          = ((BoundedParameterizable2D) dec).getParameterization();
        int vertex = (int) BoundedParam2Ds.getNearestVertex(c, t);
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
        VertexHandle hand = getVertexHandle();
        if (hand == null) {
            return;
        }
        toggleSmoothing(hand.getCurveNo());
    }

    /** Toggle the closed/open status of the currently selected
        curve. */
    public void toggleCurveClosure() {
        VertexHandle hand = getVertexHandle();
        if (hand == null) {
            return;
        }
        try {
            toggleCurveClosure(hand.getCurveNo());
        } catch (IllegalArgumentException x) {
            showError(x.toString(), "Cannot perform operation");
        }
    }

    @Override public void setOriginalFilename(String filename) {
        if ((filename == null && originalFilename == null)
            || (filename != null && filename.equals(originalFilename))) {
            return;
        }

        super.setOriginalFilename(filename);
        originalImage = null;
        triedToLoadOriginalImage = false;
    }

    /** Launch the application. */
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
                @Override public void run() {
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

    @Override public void cropPerformed(CropEvent e) {
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                diagramType = e.getDiagramType();
                newDiagram(e.filename, Duh.toPoint2DDoubles(e.getVertices()));
                initializeGUI();
            }
        propagateChange();
    }

    /** Start on a blank new diagram. */
    public void newDiagram() {
        if (!verifyCloseDiagram()) {
            return;
        }

        setSaveNeeded(false);
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                DiagramType temp = (new DiagramDialog(null)).showModal();
                if (temp == null) {
                    return;
                }

                diagramType = temp;
                newDiagram(null, null);
            }

        propagateChange1();
    }

    @Override public void setSaveNeeded(boolean b) {
        if (b != saveNeeded) {
            super.setSaveNeeded(b);
            if (!b) {
                if (fileSaver != null) {
                    fileSaver.cancel();
                    fileSaver = null;
                }
            }
        }
    }

    @Override public void update(Observable o, Object arg) {
        if (!isEditable() || !saveNeeded) {
            return;
        }
        if (fileSaver == null) {
            fileSaver = new Timer("FileSaver", true);
            fileSaver.schedule(new FileSaver(), AUTO_SAVE_DELAY);
        }
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
        boolean tracing = (vertices != null);
        try (UpdateSuppressor d = new UpdateSuppressor()) {
                clear();
                setOriginalFilename(originalFilename);

                add(defaultAxis(Side.RIGHT));
                add(defaultAxis(Side.TOP));
                if (isTernary()) {
                    add(defaultAxis(Side.LEFT));
                }

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
                                (editFrame,
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
                                JOptionPane.showMessageDialog(editFrame, "Invalid number format.");
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
                            (editFrame, new String[] { sideNames[ov1], sideNames[ov2] });
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
                                showError(e.toString(), "Invalid number format.");
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

                        addTernaryBottomRuler(0.0, 1.0);
                        addTernaryLeftRuler(0.0, 1.0);
                        addTernaryRightRuler(0.0, 1.0);
                        break;
                    }
                }

                pageBounds = new Rectangle2D.Double
                    (-leftMargin, -topMargin, r.width + leftMargin + rightMargin,
                     r.height + topMargin + bottomMargin);

                {
                    NumberFormat format = new DecimalFormat("0.0000");
                    LinearAxis pageXAxis = LinearAxis.createFromAffine
                        (format, principalToStandardPage, false);
                    pageXAxis.name = "page X";
                    LinearAxis pageYAxis = LinearAxis.createFromAffine
                        (format, principalToStandardPage, true);
                    pageYAxis.name = "page Y";
                    add(pageXAxis);
                    add(pageYAxis);
                }

                initializeDiagram();
            }
        propagateChange();
    }

    protected double currentFontSize() {
        return normalFontSize() * lineWidth / STANDARD_LINE_WIDTH;
    }

    @Override protected void initializeDiagram() {
        super.initializeDiagram();
        boolean isTernary = diagramType.isTernary();
        editFrame.setAspectRatio.setEnabled(!isTernary);
        editFrame.setTopComponent.setEnabled(isTernary);
        bestFit();
    }

    protected void initializeGUI() {
        // Force the editor frame image to be initialized.

        if (!editorIsPacked) {
            editFrame.pack();
            editorIsPacked = true;
        }
        Rectangle rect = editFrame.getBounds();
        revalidateZoomFrame();

        if (tracingImage()) {
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
    boolean verifyCloseDiagram(Object[] options) {
        if (!saveNeeded) {
            return true;
        }

        switch (JOptionPane.showOptionDialog
                (editFrame,
                 "This file has changed. Would you like to save it?",
                 "Confirm close diagram",
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

    /** Give the user an opportunity to save the old diagram or to
        change their mind before closing a diagram.

        @return false if the user changes their mind and the diagram
        should not be closed. */
    boolean verifyCloseDiagram() {
        return verifyCloseDiagram
            (new Object[] {"Save and continue", "Do not save", "Cancel"});
    }

    /** If the file already exists, ask the user whether overwriting
        the file is OK, and return false if the user says no. If the
        file does not already exist, or the user confirms overwrite,
        then return true. */
    boolean verifyOverwriteFile(File file) {
        if (!Files.exists(file.toPath())) {
            return true;
        }
        return JOptionPane.showConfirmDialog
            (editFrame,
             "A file named '" + file.getName() + "' already exists. Overwrite the file?",
             "Confirm overwrite",
             JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
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

        super.setAspectRatio(aspectRatio);
        getEditPane().setPreferredSize(scaledPageBounds(scale).getSize());
        getEditPane().revalidate();
        scaledOriginalImages = null;
    }


    /** Invoked from the EditFrame menu */
    public void setMargin(Side side) {
        if (diagramType == null) {
            return;
        }

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
            Rectangle2D.Double bounds = principalToStandardPage.outputBounds();
            standard = "core diagram "
                + ((bounds.width >= bounds.height) ? "width" : "height");
            break;
        }

        String oldString = ContinuedFraction.toString(getMargin(side), true);
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
                JOptionPane.showMessageDialog(editFrame, "Invalid number format.");
            }
        }

        setMargin(side, margin);
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

        try {
            openDiagram(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog
                (editFrame, "File load error: " + e);
            return;
        }
    }

    @Override public void openDiagram(File file) throws IOException {
        super.openDiagram(file);
        initializeGUI();
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

        try {
            openDiagram(new File(filename));
        } catch (IOException e) {
            JOptionPane.showMessageDialog
                (editFrame, "File load error: " + e);
            return;
        }
    }

    @Override public void setPageBounds(Rectangle2D rect) {
        super.setPageBounds(rect);
        getEditPane().setPreferredSize(scaledPageBounds(scale).getSize());
        getEditPane().revalidate();
        scaledOriginalImages = null;
    }

    @Override void cannibalize(Diagram other) {
        removeAllVariables();
        removeAllTags();
        super.cannibalize(other);
        for (LinearAxis axis: axes) {
            editFrame.addVariable((String) axis.name);
        }
        selection = null;
    }

    @JsonIgnore public BufferedImage getOriginalImage() {
        if (originalImage != null) {
            return originalImage;
        }

        if (originalFilename == null || triedToLoadOriginalImage || !isEditable()) {
            return null;
        }

        triedToLoadOriginalImage = true;

        File originalFile = new File(originalFilename);
        if (Files.notExists(originalFile.toPath())) {
            JOptionPane.showMessageDialog
                (editFrame, "Warning: original file '" + originalFilename + "' not found");
            return null;
        }

        try {
            originalImage = ImageIO.read(originalFile);
            if (originalImage == null) {
                throw new IOException(filename + ": unknown image format");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog
                (editFrame,
                 "Original image unavailable: '" + filename + "': " +  e.toString());
        }

        return originalImage;
    }

    /** If the zoom frame is not needed, then then make sure it's null
        or invisible. Otherwise, make sure the zoom frame is non-null,
        initialized, visible, and shows the correct image. */
    void revalidateZoomFrame() {
        BufferedImage im = getOriginalImage();
        if (im != null) {
            initializeZoomFrame();
            zoomFrame.setImage(im);
            initializeCrosshairs();
            zoomFrame.getImageZoomPane().crosshairs = crosshairs;
            editFrame.mnBackgroundImage.setEnabled(true);
            zoomFrame.setTitle("Zoom " + getOriginalFilename());
            zoomFrame.pack();
            zoomFrame.setVisible(true);
        } else {
            if (zoomFrame != null) {
                zoomFrame.setVisible(false);
            }
            editFrame.mnBackgroundImage.setEnabled(false);
        }
    }

    @SafeVarargs public static <T> T[] concat(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static File openPEDFileDialog(Component parent) {
        return CropFrame.openFileDialog(parent, "PED files",
                                        new String[] {"ped"});
    }

    /** Return the default directory to save to and load from. */
    public static String getCurrentDirectory() {
        return Preferences.userNodeForPackage(Editor.class)
            .get(PREF_DIR,  null);
    }

    /** Set the default directory to save to and load from. */
    public static void setCurrentDirectory(String dir) {
        Preferences.userNodeForPackage(Editor.class)
            .put(PREF_DIR,  dir);
    }

    public static File openPEDOrImageFileDialog(Component parent) {
        String[] pedExts = { "ped" };
        String[] imageExts = ImageIO.getReaderFileSuffixes();
        String[] allExts = concat(pedExts, imageExts);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open PED or image file");
        String dir = getCurrentDirectory();
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
       chooser.setFileFilter
            (new FileNameExtensionFilter("PED and image files", allExts));
       chooser.addChoosableFileFilter
           (new FileNameExtensionFilter("PED files only", pedExts));
       chooser.addChoosableFileFilter
           (new FileNameExtensionFilter("Image files only", imageExts));
       if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
           File file = chooser.getSelectedFile();
           setCurrentDirectory(file.getParent());
           return file;
       } else {
           return null;
       }
    }

    public void showOpenDialog(Component parent) {
        File file = isEditable() ? openPEDOrImageFileDialog(parent)
            : openPEDFileDialog(parent);
        if (file == null) {
            return;
        }
        String ext = getExtension(file.getName());
        try {
            if (isEditable() && ext != null && !"ped".equalsIgnoreCase(ext)) {
                // This had better be an image file.
                cropFrame.setFilename(file.getAbsolutePath());
                cropFrame.pack();
                editFrame.setStatus("");
                clear();
                cropFrame.setVisible(true);
            } else {
                if (ext == null && Files.notExists(file.toPath())) {
                    // Add .ped extension.
                    ext = "ped";
                    file = new File(file.getAbsolutePath() + "." + ext);
                }
                if ("ped".equalsIgnoreCase(ext)) {
                    openDiagram(file);
                } else {
                    showError(parent,
                              "Unrecognized file extension (expected .ped)",
                              "File load error");
                }
            }
        } catch (IOException x) {
            showError(parent, "Could not load file: " + x,
                      "File load error");
        }
    }

    void showError(String mess, String title) {
        showError(editFrame, mess, title);
    }

    void showError(Component parent, String mess, String title) {
        JOptionPane.showMessageDialog
            (parent, mess, title, JOptionPane.ERROR_MESSAGE);
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
        String dir = getCurrentDirectory();
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
        if (getExtension(file.getName()) == null) {
            // Add the default extension
            file = new File(file.getAbsolutePath() + "." + ext);
        }

        setCurrentDirectory(file.getParent());
        return file;
    }

    /** @return a File if the user selected one, or null otherwise.

        @param ext the extension to use with this file ("pdf" for
        example). */
    public File showOpenDialog(String ext) {
        String dir = getCurrentDirectory();
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
        if (getExtension(file.getName()) == null) {
            // Add the default extension
            file = new File(file.getAbsolutePath() + "." + ext);
        }

        setCurrentDirectory(file.getParent());
        return file;
    }

    /** If the last section of the filename contains a dot, then
        return everything after that dot, converted to lower case.
        Otherwise, return null. */
    public static String getExtension(String s) {
        String separator = System.getProperty("file.separator");
        int lastSeparatorIndex = s.lastIndexOf(separator);
        int extensionIndex = s.lastIndexOf(".");
        return (extensionIndex <= lastSeparatorIndex) ? null
            : s.substring(extensionIndex + 1).toLowerCase();
    }

    /** Invoked from the EditFrame menu */
    public void saveAsPDF() {
        File file = showSaveDialog("pdf");
        if (file == null || !verifyOverwriteFile(file)) {
            return;
        }

        saveAsPDF(file);
    }

    /** Invoked from the EditFrame menu */
    public void saveAsSVG() {
        File file = showSaveDialog("svg");
        if (file == null || !verifyOverwriteFile(file)) {
            return;
        }

        try {
            saveAsSVG(file);
            JOptionPane.showMessageDialog
                (editFrame, "Saved '" + file.getAbsolutePath() + "'.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(editFrame, "File save error: " + e);
        }
    }

    public void saveAsPED() {
        File file = showSaveDialog("ped");
        if (file == null || !verifyOverwriteFile(file)) {
            return;
        }
        saveAsPEDGUI(file.toPath());
    }

    /** Like saveAsPED(), but handle both exceptions and success
        internally with warning or information dialogs. */
    public void saveAsPEDGUI(Path file) {
        try {
            saveAsPED(file);
            filename = file.toAbsolutePath().toString();
            JOptionPane.showMessageDialog
                (editFrame, "Saved '" + file.toAbsolutePath() + "'.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog
                (editFrame, "File save error: " + e);
        }
    }

    @Override public void saveAsPED(Path file) throws IOException {
        super.saveAsPED(file);
        // Now delete the auto-save file if it exists.
        if (autosaveFile != null) {
            try {
                if (Files.deleteIfExists(autosaveFile)) {
                    System.out.println("Deleted '" + autosaveFile + "'");
                }
            } catch (IOException|SecurityException x) {
                showError("Auto-save file '" + autosaveFile + "': " + x,
                          "Could not delete file");
            } finally {
                // No matter whether we succeeded or failed, do not
                // try again.
                autosaveFile = null;
            }
        }
    }

    /** Invoked from the EditFrame menu */
    public void save() {
        String filename = getFilename();
        if (filename == null) {
            saveAsPED();
        } else {
            saveAsPEDGUI(FileSystems.getDefault().getPath(filename));
        }
    }

    /** Invoked from the EditFrame menu */
    public void print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        if (job.printDialog()) {
            try {
                print(job);
                JOptionPane.showMessageDialog
                    (editFrame, "Print job submitted.");
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(editFrame, e.toString());
            }
        } else {
            JOptionPane.showMessageDialog(editFrame, "Print job canceled.");
        }
    }

    /** @param segment A line on the standard page

        Return a grid line (also on the standard page) that passes
        through segment.getP1() and that is roughly parallel to
        segment, or null if no such line is close enough to parallel.
        A grid line is a line of zero change for a defined axis (from
        the "axes" variable). */
    Line2D.Double nearestGridLine(Line2D.Double segment) {
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

        return nearestGridLine(segment, vectors);
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

        try (UpdateSuppressor us = new UpdateSuppressor()) {
                ArrayList<Point2D.Double> selections = new ArrayList<>();
                int oldSize = paths.size();

                if (selection != null) {
                    selections.add(selection.getLocation());
                }
                Point2D.Double point2 = secondarySelectionLocation();
                if (point2 != null) {
                    selections.add(point2);
                }

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
            } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

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
            redraw();
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
            double d = Duh.distance(newMousePage, pageBounds);
            if (d > 10) {
                showError
                    ("<html><body width = \"300 px\""
                     + "<p>The coordinates you selected lie far outside "
                     + "the page boundaries.  (Remember to use the percent "
                     + "sign when entering percentage values.)"
                     + "</body></html>",
                     "Coordinates out of bounds");
                return;
            } else if (d > 1e-6) {
                if (JOptionPane.showConfirmDialog
                    (editFrame,
                     "<html><body width = \"300 px\""
                     + "<p>The coordinates you selected lie beyond the edge "
                     + "of the page. Expand the page margins?"
                     + "</body></html>",
                     "Coordinates out of bounds",
                     JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    pageBounds.add(newMousePage);
                    setPageBounds(pageBounds);
                } else {
                    return;
                }
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

    @Override public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void unstickMouse() {
        mouseIsStuck = false;
        principalFocus = null;
        updateMousePosition();
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON2 
            || e.getButton() == MouseEvent.BUTTON3) {
            deselectCurve();
            return;
        }
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
    @JsonIgnore public boolean isEditable() {
        return true;
    }

    /** @return true if the diagram is currently being traced from
        another image. */
    boolean tracingImage() {
        return getOriginalImage() != null;
    }

    /** The mouse was moved in the edit window. Update the coordinates
        in the edit window status bar, repaint the diagram, and update
        the position in the zoom window,. */
    @Override public void mouseMoved(MouseEvent e) {
        isShiftDown = e.isShiftDown();
        redraw();
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

        if (mprin != null) {
            editFrame.setStatus(principalToPrettyString(mprin));
            
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
                (editFrame,
                 "Line width in page X/Y units:",
                 String.format("%.5f", getLineWidth()));
            if (str == null) {
                return;
            }
            try {
                setLineWidth(ContinuedFraction.parseDouble(str));
                return;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(editFrame, "Invalid number format.");
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
        if (filename != null) {
            String lcase = filename.toLowerCase();
            int index = lcase.lastIndexOf(".ped");
            if (index >= 0 && index == lcase.length() - 4) {
                try {
                    openDiagram(new File(filename));
                    initializeGUI();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog
                        (null, filename + ": " + e,
                         "File load error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
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

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}

    @Override public void mouseExited(MouseEvent e) {
        mprin = null;
        redraw();
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

    @Override public boolean setFontName(String s) {
        boolean res = super.setFontName(s);
        if (res) {
            getEditPane().setFont(embeddedFont);
            editFrame.setFontName(s);
        }
        return res;
    }

    public void setFillStyle(StandardFill fill) {
        // TODO Auto-generated method stub
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

    @Override public String toString() {
        return "Scale: " + scale + " image: " + imageBounds
            + " crop: " + cropBounds;
    }
}
