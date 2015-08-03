/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import gov.nist.pededitor.EditFrame.BackgroundImageType;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.jnlp.IntegrationService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.AbstractAction;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.MenuElement;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.codehaus.jackson.annotate.JsonIgnore;

import Jama.Matrix;

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class BasicEditor extends Diagram
    implements CropEventListener, MouseListener, MouseMotionListener,
               Observer {
    static ArrayList<BasicEditor> openEditors = new ArrayList<>();

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
                    saveAsPED(file, false);
                    System.out.println("Saved '" + file + "'");
                    autosaveFile = file;
                    majorSaveNeeded = true;
                } catch (IOException x) {
                    System.err.println("Could not save '" + file + "': " + x);
                }
            }
            fileSaver = null;
        }
    }

    class CloseListener extends WindowAdapter
    {
        @Override public void windowClosing(WindowEvent e) {
            verifyThenClose();
        }
    }

    class MathWindowCloseListener extends WindowAdapter
    {
        @Override public void windowClosing(WindowEvent e) {
            editFrame.showMathWindow.setSelected(false);
        }
    }

    class ZoomWindowCloseListener extends WindowAdapter
    {
        @Override public void windowClosing(WindowEvent e) {
            zoomFrame.setState(Frame.ICONIFIED);
        }
    }

    public static boolean editable(DecorationHandle hand) {
        return hand instanceof LabelHandle
            || hand instanceof TieLineHandle
            || hand instanceof RulerHandle;
    }

    /** Set selection to the nearest DecorationHandle. Return true for
        success, false if that was not possible. */
    boolean selectSomething() {
        for (DecorationHandle handle: nearestHandles()) {
            setSelection(handle);
            return true;
        }

        showError("There is nothing to select.");
        return false;
    }

    public void editSelection() {
       boolean hadSelection = (selection != null);

        String errorTitle = "Cannot edit selection";
        if (selection == null) {
            // Find the nearest editable item, and edit it.

            for (DecorationHandle handle: nearestHandles()) {
                if (editable(handle)) {
                    setSelection(handle);
                    break;
                }
            }

            if (selection == null) {
                showError("There are no editable items.", errorTitle);
                return;
            }
        }

        if (selection instanceof LabelHandle) {
            editLabel(((LabelHandle) selection).getItem());
        } else if (selection instanceof TieLineHandle) {
            edit((TieLineHandle) selection);
        } else if (selection instanceof RulerHandle) {
            edit((RulerHandle) selection);
        } else {
            showError("This item does not have a special edit function.",
                      errorTitle);
        }
        if (!hadSelection) {
            clearSelection();
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

        if (getRulerDialog().showModal(item, axes, principalToStandardPage)) {
            propagateChange();
        }
    }

    private static final String PREF_DIR = "dir";
    private static final long AUTO_SAVE_DELAY = 5 * 60 * 1000; // 5 minutes

    static final protected double MOUSE_UNSTICK_DISTANCE = 30; /* pixels */
    static final protected double MOUSE_DRAG_DISTANCE = 80; /* pixels */
    static final protected String HOW_TO_SELECT
        = "You can select an item by positioning the mouse pointer within "
        + "the diagram, pressing the right mouse button to open a popup menu, "
        + "and selecting the 'Select nearest key point' (<code>Shift+Q</code>) "
        + "or 'Select nearest line/curve' (<code>Shift+W</code>) menu "
        + "options.";
    protected double mouseDragDistance = MOUSE_DRAG_DISTANCE;
    static protected Image crosshairs = null;

    protected CropFrame cropFrame = new CropFrame();
    protected EditFrame editFrame = new EditFrame(this);
    protected BasicRightClickMenu mnRightClick = new RightClickMenu(this);
    protected ImageZoomFrame zoomFrame = null;
    protected MathWindow mathWindow = new MathWindow(this);
    protected LabelDialog labelDialog = null;
    protected RulerDialog rulerDialog = null;
    protected JColorChooser colorChooser = null;
    protected JDialog colorDialog = null;
    protected FormulaDialog formulaDialog = null;
    protected CoordinateDialog coordinateDialog = null;
    protected DigitizeDialog digitizeDialog = null;
    protected ImageDimensionDialog imageDimensionDialog = null;
    protected LineWidthDialog lineWidthDialog = null;
    static JDialog waitDialog = null;

    @JsonIgnore public DecorationHandle getSelection() {
        return selection;
    }

    void setRightClickMenu(BasicRightClickMenu menu) {
        mnRightClick = menu;
    }

    BasicRightClickMenu getRightClickMenu() {
        return mnRightClick;
    }

    LabelDialog getLabelDialog() {
        if (labelDialog == null) {
            labelDialog = new LabelDialog(editFrame, "Add text",
                                          getFont().deriveFont(16.0f));
        }
        return labelDialog;
    }

    FormulaDialog getFormulaDialog() {
        if (formulaDialog == null) {
            formulaDialog = new FormulaDialog(editFrame);
        }
        formulaDialog.formula.selectAll();
        return formulaDialog;
    }

    CoordinateDialog getCoordinateDialog() {
        if (coordinateDialog == null) {
            coordinateDialog = new CoordinateDialog(editFrame);
        }
        return coordinateDialog;
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
    protected transient boolean smoothed = false;
    protected transient boolean showGrid = false;
    protected transient boolean majorSaveNeeded = false;
    protected transient Dimension oldFrameSize = null;
    protected transient boolean autoRescale = false;
    protected transient boolean allowRobotToMoveMouse = true;
    // This warning is more annoying than helpful.
    protected transient boolean alwaysConvertLabels = true;
    // Number of times paintEditPane() has been called.
    protected transient int paintCnt = 0;
    protected boolean mEditable = true;
    protected boolean exitOnClose = true;

    /** The item (vertex, label, etc.) that is selected, or null if nothing is. */
    protected transient DecorationHandle selection;
    /** If the timer exists, the original image (if any) upon which
        the new diagram is overlaid will blink. */
    transient Timer imageBlinker = null;
    transient Timer fileSaver = null;
    /** True if imageBlinker is enabled and the original image should
        be displayed in the background at this time. */
    transient boolean backgroundImageEnabled;
    protected transient BackgroundImageType backgroundType = null;
    protected transient BackgroundImageType oldBackgroundType = null;

    protected transient boolean preserveMprin = false;
    protected transient boolean isShiftDown = false;
    protected transient Point2D.Double statusPt = null;

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

    protected transient boolean exitIfLastWindowCloses = true;

    /** Because rescaling an image is slow, keep a cache of locations
        and sizes that have been rescaled. */
    protected transient ArrayList<ScaledCroppedImage> scaledOriginalImages;
    /** This is the darkened version of the original image, or null if
        no darkened version exists. At most one dark image is kept in
        memory at a time. */
    protected transient ScaledCroppedImage darkImage;
    /** Alpha value of darkImage. */
    protected transient double darkImageAlpha = 0;

    protected transient double lineWidth = STANDARD_LINE_WIDTH;
    protected transient StandardStroke lineStyle = StandardStroke.SOLID;
    protected transient Color color = Color.BLACK;

    static String[] tieLineStepStrings =
    { "<html><div width=\"200 px\"><p>"
      + "Use the 'Q' or 'W' short-cut keys to select an outside "
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
         new BasicEditor.Action("Item selected") {
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
    protected transient ArrayList<PathAndT> tieLineCorners;

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

    final static class MousePress {
        /** The mousePressed event that created this. */
        MouseEvent e;
        /** Principal coordinates of the point that will be added
            unless this ends up being a drag operation. */
        Point2D.Double prin;

        public MousePress(MouseEvent e, Point2D.Double prin) {
            this.e = e;
            this.prin = new Point2D.Double(prin.getX(), prin.getY());
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" + e + ", "
                + Geom.toString(prin) + "]";
        }
    }

    final static class MouseTravel {
        int lastX;
        int lastY;
        int travel = 0; /* In pixels */

        public MouseTravel(int x0, int y0) {
            lastX = x0;
            lastY = y0;
        }

        public void travel(MouseEvent e) {
            travel += Math.abs(e.getX() - lastX) + Math.abs(e.getY() - lastY);
            lastX = e.getX();
            lastY = e.getY();
        }

        public int getTravel() { return travel; }
    }

    /** Until the mouse is released, we can't tell whether the user is
        dragging a box to zoom or clicking to add a point. Use
        mousePress to store the mousePressed event and the position of
        the point that will be added if that's what we end up
        doing. */
    protected transient MousePress mousePress = null;
    protected transient MousePress rightClick = null;

    /** mouseTravel logs the distance in pixels that the mouse has
        moved either since the button was depressed (to discriminate
        between clicks and drags) or since the mouse was explicitly
        positioned (to determine whether to unstick the mouse from
        that position). */
    protected transient MouseTravel mouseTravel = null;

    /** Array of filename to open, if any. */
    File[] filesList = null;
    int fileNo = -1;

    public BasicEditor() {
        setEditable(true);
        setExitOnClose(false);
        init();
        tieLineDialog.setFocusableWindowState(false);
        mathWindow.setDefaultCloseOperation
            (WindowConstants.HIDE_ON_CLOSE);
        mathWindow.addWindowListener(new MathWindowCloseListener());
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
        zoomFrame.setDefaultCloseOperation
            (WindowConstants.DO_NOTHING_ON_CLOSE);
        zoomFrame.addWindowListener(new ZoomWindowCloseListener());
        Rectangle rect = editFrame.getBounds();
        zoomFrame.setLocation(rect.x + rect.width, rect.y);
        zoomFrame.pack();
    }

    private void init() {
        scale = BASE_SCALE;
        mprin = null;
        scaledOriginalImages = null;
        mathWindow.refresh();
        mathWindow.setLineWidth(lineWidth);
        mouseIsStuck = false;
        mouseTravel = null;
        mousePress = null;
        rightClick = null;
        principalFocus = null;
        paintSuppressionRequestCnt = 0;
        setBackgroundType((backgroundType == null)
                          ? BackgroundImageType.LIGHT_GRAY
                          : backgroundType);
        tieLineDialog.setVisible(false);
        tieLineCorners = new ArrayList<>();
        originalImage = null;
        triedToLoadOriginalImage = false;
        majorSaveNeeded = false;
    }

    @Override void clear() {
        clearSelection();
        if (fileSaver != null) {
            fileSaver.cancel();
            fileSaver = null;
        }
        autosaveFile = null;
        editFrame.setStatus(null);
        super.clear();
        if (zoomFrame != null) {
            zoomFrame.setVisible(false);
            zoomFrame.clear();
        }
        init();
    }

    public void verifyThenClose() {
        if (!isEditable() || verifyCloseDiagram()) {
            close();
        }
    }

    /* If there is another editor open or this window is blank, then
       close this one. If this is the last open editor, then just
       clear it instead of closing it. (From the user's perspective,
       the diagram is closed in either case.) */
    public void verifyThenCloseOrClear() {
        if (!isEditable() || verifyCloseDiagram()) {
            if (getOpenEditorCnt() > 1 || !haveDiagram()) {
                close();
            } else {
                clear();
            }
        }
    }


    /* If a different diagram window is open and this window does not
       have a diagram, then close it and return true; otherwise return
       false. */
    public boolean closeIfNotUsed() {
        if (getOpenEditorCnt() > 1
            && !haveDiagram()
            && (zoomFrame == null || !zoomFrame.isVisible())) {
            close();
            return true;
        }
        return false;
    }

    /** close() irrevocably closes this BasicEditor object. The results of
        any further method calls except isClosed() will be
        undefined.

        If this never gets called, then the open editor count will be wrong.
    */
    public void close() {
        if (!isClosed()) {
            if (cropFrame != null) {
                cropFrame.dispose();
                cropFrame = null;
            }
            if (zoomFrame != null) {
                zoomFrame.dispose();
                zoomFrame = null;
            }
            if (mathWindow != null) {
                mathWindow.dispose();
                mathWindow = null;
            }
            if (labelDialog != null) {
                labelDialog.dispose();
                labelDialog = null;
            }
            if (rulerDialog != null) {
                rulerDialog.dispose();
                rulerDialog = null;
            }
            if (colorDialog != null) {
                colorDialog.dispose();
                colorDialog = null;
            }
            if (coordinateDialog != null) {
                coordinateDialog.dispose();
                coordinateDialog = null;
            }
            if (formulaDialog != null) {
                formulaDialog.dispose();
                formulaDialog = null;
            }
            if (digitizeDialog != null) {
                digitizeDialog.dispose();
                digitizeDialog = null;
            }
            if (imageDimensionDialog != null) {
                imageDimensionDialog.dispose();
                imageDimensionDialog = null;
            }
            if (lineWidthDialog != null) {
                lineWidthDialog.dispose();
                lineWidthDialog = null;
            }
            if (tieLineDialog != null) {
                tieLineDialog.dispose();
                tieLineDialog = null;
            }
            if (mnRightClick != null) {
                mnRightClick = null;
            }

            if (editFrame != null) {
                editFrame.dispose();
                editFrame.parentEditor = null;
                editFrame = null;
            }
        }
        openEditors.remove(this);
        if (getOpenEditorCnt() == 0) {
            lastWindowClosed();
        }
    }

    @JsonIgnore public boolean isExitIfLastWindowCloses() {
        return isExitOnClose() || exitIfLastWindowCloses;
    }

    public void setExitIfLastWindowCloses(boolean v) {
        exitIfLastWindowCloses = v;
    }

    /** This gets called automatically when the last window closes;
        override it to change this behavior. */
    void lastWindowClosed() {
        if (isExitIfLastWindowCloses()) {
            System.exit(0);
        }
    }

    /** Close all windows, no questions asked. */
    public void exit() {
        // Duplicate openEditors so we don't end up iterating through a
        // list that is simultaneously being modified.
        for (BasicEditor e: new ArrayList<>(openEditors)) {
            e.close();
        }
        lastWindowClosed();
    }

    /** Close all windows, but have the user confirm any unsaved
        changes. */
    public void verifyExit() {

        // A single diagram that needs saving is handled differently
        // from two or more diagrams that need saving.
        BasicEditor needsSave = null;
        for (BasicEditor e: openEditors) {
            if (e.isSaveNeeded()) {
                if (needsSave == null) {
                    needsSave = e;
                } else {
                    // If two or more diagrams need saving, don't ask
                    // about saving each one, because then the options
                    // get hard to explain.
                    if (JOptionPane.showConfirmDialog
                        (null,
                         "There are several diagrams with unsaved changes. "
                         + "Exit anyway?",
                         "Confirm exit",
                         JOptionPane.OK_CANCEL_OPTION)
                        == JOptionPane.OK_OPTION) {
                        exit();
                    } else {
                        return;
                    }
                }
            }
        }

        if (needsSave != null) {
            needsSave.verifyThenClose();
        }
        exit();
    }

    @JsonIgnore public void setExitOnClose(boolean b) {
        exitOnClose = b;
    }

    @JsonIgnore public boolean isExitOnClose() {
        return exitOnClose;
    }

    public static int getOpenEditorCnt() {
        return openEditors.size();
    }

    @JsonIgnore public boolean isClosed() {
        return editFrame == null;
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
    @JsonIgnore public CuspFigure getSelectedCuspFigure() {
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

    @Override public void detachOriginalImage() {
        originalImage = null;
        super.detachOriginalImage();
    }

    public void setDefaultSettingsFromSelection() {
       boolean hadSelection = (selection != null);
        if (!hadSelection && !selectSomething()) {
            return;
        }
 
        Decoration dec = selection.getDecoration();
        StandardStroke ls = dec.getLineStyle();
        if (ls != null) {
            lineStyle = ls;
        }

        setColor(thisOrBlack(dec.getColor()));
        double lw = dec.getLineWidth();
        if (lw != 0) {
            lineWidth = lw;
        }
        LabelDecoration ldec = getSelectedLabel();
        if (ldec != null) {
            getLabelDialog().setFontSize(ldec.getLabel().getScale());
        }
        if (!hadSelection) {
            clearSelection();
        }
    }

    public void setColor(Color c) {
        color = c;
        editFrame.setColor(c);
        redraw();
    }

    public void resetSelectionToDefaultSettings() {
       boolean hadSelection = (selection != null);
        if (!hadSelection && !selectSomething()) {
            return;
        }
 
        Decoration dec = selection.getDecoration();
        dec.setLineStyle(lineStyle);
        dec.setColor(color);
        dec.setLineWidth(lineWidth);
        LabelDecoration ldec = getSelectedLabel();
        if (ldec != null) {
            ldec.getLabel().setScale(getLabelDialog().getFontSize());
            propagateChange();
        }
        if (!hadSelection) {
            clearSelection();
        }
    }

    public void put() {
        String[] labels = { "Key", "Value" };
        String[] values = { "", "" };
        StringArrayDialog dog = new StringArrayDialog
            (editFrame, labels, values, null);
        dog.setTitle("Add key/value pair");
        values = dog.showModalStrings();
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
        values = dog.showModalStrings();
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

    public synchronized void setBackgroundType(BackgroundImageType value) {
        if (backgroundType != oldBackgroundType) {
            oldBackgroundType = backgroundType;
        }
        backgroundType = value;

        // Turn blinking off
        if (imageBlinker != null) {
            imageBlinker.cancel();
        }
        imageBlinker = null;
        darkImage = null;

        if (value == BackgroundImageType.BLINK) {
            imageBlinker = new Timer("ImageBlinker", true);
            imageBlinker.scheduleAtFixedRate(new ImageBlinker(), 500, 500);
            backgroundImageEnabled = true;
        }
        editFrame.setBackgroundType(value);

        // The rest is handed in paintDiagram().

        redraw();
    }

    /* Like setBackgroundType(), but attempting to set the background
       to its current value causes it to revert to its previous value.
       This exists just to allow control-H to hide the background
       image and then uh-hide it. */
    public synchronized void toggleBackgroundType(BackgroundImageType value) {
        setBackgroundType(value == backgroundType ? oldBackgroundType : value);
    }

    /** A curve should be selected -- usually, a closed curve.
        Everything on or in the curve will be shifted by the vector
        that moves the selected vertex to mprin. */
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
                ("Move the mouse to the target location. "
                 + "Use the 'R' shortcut key or keyboard menu controls "
                 + "instead of selecting the menu item using the mouse.",
                 errorTitle);
            return;
        }

        if (mouseIsStuckAtSelection()) {
            // Unstick the mouse so we're not just moving the mouse
            // onto itself.
            setMouseStuck(false);
        }

        CuspFigure path = vhand.getDecoration().getItem();
        Shape region = vhand.getDecoration().getShape();
        Param2DBounder param = PathParam2D.create(region);

        Point2D.Double delta = Geom.aMinusB(mprin, selection.getLocation());

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

    /** Like moveRegion(), but copy instead. All handles must be
        inside the region for a decoration to be copied. */
    public void copyRegion() {
        VertexHandle vhand = getVertexHandle();
        String errorTitle = "Cannot copy region";

        if (vhand == null) {
            showError
                ("Draw a curve to identify the boundary of the region "
                 + "to be moved.",
                 errorTitle);
            return;
        }

        if (mprin == null) {
            showError("Move the mouse to the target location.", errorTitle);
            return;
        }

        if (mouseIsStuckAtSelection()) {
            // Unstick the mouse so we're not just moving the mouse
            // onto itself.
            setMouseStuck(false);
        }

        CuspFigure path = vhand.getDecoration().getItem();
        Shape region = vhand.getDecoration().getShape();
        Param2DBounder param = PathParam2D.create(region);

        Point2D.Double delta = Geom.aMinusB(mprin, selection.getLocation());

        for (Decoration d: new ArrayList<Decoration>(getDecorations())) {
            boolean inside = true;
            DecorationHandle hand = null;
            Point2D prin = null;
            for (DecorationHandle h: d.getHandles()) {
                hand = h;
                prin = hand.getLocation();
                Point2D page = principalToStandardPage.transform(prin);
                inside = path.isClosed() && region.contains(page);
                if (!inside) {
                    // Check if the point is very close to the path
                    // border.
                    CurveDistanceRange cdr = param.distance(page, 1e-6, 1000);
                    inside = cdr != null && cdr.distance <= 1e-6;
                }
                if (!inside) {
                    break;
                }
            }

            if (inside) {
                DecorationHandle s = hand.copy
                    (new Point2D.Double(prin.getX() + delta.x, prin.getY() + delta.y));
                if (hand.getDecoration().equals(vhand.getDecoration())) {
                    VertexHandle newHand = new VertexHandle
                        (((VertexHandle) s).getDecoration(), vhand.vertexNo);
                    setSelection(newHand);
                }
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
            setMouseStuck(false);
        }

        moveSelection(mprin, moveAll);
        setMouseStuck(true);
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
            setMouseStuck(false);
        }

        setSelection(selection.copy(mprin));
    }

    public void changeLayer(int delta) {
        boolean hadSelection = (selection != null);
        if (!hadSelection && !selectSomething()) {
            return;
        }
        changeLayer0(delta);
        if (!hadSelection) {
            clearSelection();
        }
    }

    /** Move the selection closer to the front(drawn later/positive
        delta) or back (drawn earlier/negative delta). If the selection
        cannot be raised or lowered by the given amount because there
        are not enough layers above/below it, then raise/lower it to
        the top/bottom. */
    void changeLayer0(int delta) {
        Decoration d = selection.getDecoration();
        int layer = getLayer(d);
        if (layer < 0) {
            throw new IllegalStateException
                ("Could not locate decoration " + d);
        }
        layer += delta;
        if (layer > 0 && layer < decorations.size() - 1) {
            // If the layer was raised or lowered just a little bit,
            // and it did not move the selection ahead of or behind
            // any other decoration that intersects this decoration's
            // bounds, then keep moving the layer up until it can go
            // no further or it passes in front of a decorations whose
            // bounds it intersects.
            for (Rectangle2D selectionBounds = bounds(d);
                 layer > 0 && layer < decorations.size() - 1;
                 layer += ((delta > 0) ? 1 : -1)) {
                Decoration jumpedPast = decorations.get(layer);
                if (Geom.distanceSq(selectionBounds, bounds(jumpedPast))
                    < 1e-12) {
                    break;
                }
            }
        }
        setLayer(d, layer);
    }

    /** Change the selection's color. */
    public void colorSelection() {
        boolean hadSelection = (selection != null);
        if (!hadSelection && !selectSomething()) {
            return;
        }

        if (colorChooser == null) {
            colorChooser = new JColorChooser();
            colorDialog = JColorChooser.createDialog
            (editFrame, "Choose color", true, colorChooser,
             new ActionListener() {
                 @Override public void actionPerformed(ActionEvent e) {
                     setColor(colorChooser.getColor());
                     BasicEditor.this.selection.getDecoration().setColor(color);
                 }
             },
             null);
            colorDialog.pack();
        }

        Color c = selection.getDecoration().getColor();
        if (c != null) {
            setColor(c);
        }
        colorChooser.setColor(color);
        colorDialog.setVisible(true);

        if (!hadSelection) {
            clearSelection();
        }
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
        propagateChange();
    }

    public void removeSelection() {
        boolean hadSelection = (selection != null);
        if (!hadSelection && !selectSomething()) {
            return;
        }
        setSelection(selection.remove());
        if (!hadSelection) {
            clearSelection();
        }
    }

    public void removeLikeSelection() {
        boolean hadSelection = (selection != null);
        if (!hadSelection && !selectSomething()) {
            return;
        }
        removeLikeThis(selection.getDecoration());
        clearSelection();
    }

    /** Select the vertex that comes after or before the currently
        selected vertex.

        @param rightward If rightward is true and the line tangent to
        this curve at the currently selected vertex is not vertical,
        then select the next vertex in the rightward direction. If
        rightward is true and the tangent is vertical, select the next
        vertex in the downward direction. If rightward is false,
        select the next vertex in the other direction (left or
        straight up). */
    public void shiftActiveVertex(boolean rightward) {
        VertexHandle sel = getVertexHandle();

        if (sel == null) {
            return; // Nothing to do.
        }

        CuspFigure path = getSelectedCuspFigure()
            .createTransformed(principalToStandardPage);
        int cnt = path.size();

        Point2D.Double g = path.getParameterization().getDerivative
            (sel.getT());

        if (g == null) {
            return; // Nothing to do.
        }

        // nextIsRight is true if the gradient points rightward or if
        // the gradient points straight up (to within numeric
        // error).
        boolean nextIsRight = g.getX() > 0 ||
            Math.abs(g.getX() * 1e12) < g.getY();
        int delta = (nextIsRight == rightward) ? 1 : -1;

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

        setSelection(sel);
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

    /** Like move(), but move in the given direction until you run
        into a line. If there are no lines past this one, then do
        nothing.
    */
    public void jump(int dx, int dy) {
        if (mprin == null) {
            return;
        }

        Point2D.Double mousePage = principalToStandardPage.transform(mprin);

        double bigBump = pageBounds.width + pageBounds.height +
                Math.abs(pageBounds.x) + Math.abs(pageBounds.y);
        double smallBump = bigBump * 1e-5;
        // Bump p1 over a bit so we don't end up hitting the same
        // point we started at. The bump should be small, but not
        // smaller than the precision of the intersection computation.

        // Bump p2 in the same direction a larger distance -- clear
        // outside the diagram, to make sure the segment isn't too
        // small.

        Line2D.Double seg = new Line2D.Double
            (mousePage.x + dx * smallBump, mousePage.y + dy * smallBump,
             mousePage.x + dx * bigBump, mousePage.y + dy * bigBump);
        Point2D p1 = seg.getP1();

        double minDistSq = 0;
        Decoration closeDec = null;
        double closeDecT = 0;
        Point2D.Double res = null;
        for (Decoration dec: getDecorations()) {
            BoundedParam2D b = getStandardPageParameterization(dec);
            if (b != null) {
                for (double t: b.segIntersections(seg)) {
                    Point2D.Double p = b.getLocation(t);
                    double distSq = p1.distanceSq(p);
                    if (res == null || distSq < minDistSq) {
                        minDistSq = distSq;
                        closeDec = dec;
                        closeDecT = t;
                        res = p;
                    }
                }
            }
        }

        if (res != null) {
            moveMouse(standardPageToPrincipal.transform(res));
            showTangent(closeDec, closeDecT);
            setMouseStuck(true);
        }
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
        setMouseStuck(true);
    }

    /** Most of the information required to paint the EditPane is part
        of this object, so it's simpler to do the painting from
        here. */
    public void paintEditPane(Graphics g) {
        if (++paintCnt == 1) {
            if (waitDialog != null) {
                waitDialog.dispose();
                waitDialog = null;
            }
        }
        updateMousePosition();
        Dimension size = editFrame.getSize();
        if (oldFrameSize == null
            || (size != null && !size.equals(oldFrameSize))) {
            oldFrameSize = size;
            if (autoRescale || scale < bestFitScale()) {
                boolean oart = allowRobotToMoveMouse;
                try {
                    allowRobotToMoveMouse = false;
                    bestFit();
                } finally {
                    allowRobotToMoveMouse = oart;
                }
            }
        }
        paintDiagramWithSelection((Graphics2D) g, scale);
    }

    static final double DEFAULT_BACKGROUND_IMAGE_ALPHA = 1.0/3;

    double getBackgroundImageAlpha() {
        switch (editFrame.getBackgroundImage()) {
        case LIGHT_GRAY:
            return DEFAULT_BACKGROUND_IMAGE_ALPHA;
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
        double alpha = getBackgroundImageAlpha();
        if (alpha != DEFAULT_BACKGROUND_IMAGE_ALPHA) {
            if (darkImage != null
                && im.imageBounds.equals(darkImage.imageBounds)
                && im.cropBounds.equals(darkImage.cropBounds)
                && darkImageAlpha == alpha) {
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
                fade(src, darkImage.croppedImage,
                     alpha / DEFAULT_BACKGROUND_IMAGE_ALPHA);
                darkImageAlpha = alpha;
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

    /** Return true if the user is currently selecting a region to
        zoom to. */
    boolean isZoomMode() {
        return mousePress != null && mouseTravel != null
            && mouseTravel.getTravel() >= mouseDragDistance;
    }


    /** Show the result if the mouse point were added to the currently
        selected curve in red, and show the currently selected curve in
        green. */
    void paintSelectedCurve(Graphics2D g, double scale) {
                
        // Color in red the curve that would exist if the
        // current mouse position were added. Color in green
        // the curve that already exists.

        CurveDecoration csel = getSelectedCurve();
        CuspFigure path = csel.getItem();
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                Color oldColor = csel.getColor();
                Color highlight = getHighlightColor(oldColor);

                StandardFill oldFill = path.getFill();
                if (oldFill != null) {
                    path.setStroke(StandardStroke.SOLID);
                }

                double originalLineWidth = path.getLineWidth();
                double highlightLineWidth = originalLineWidth;
                double highlightLineWidthPixels = highlightLineWidth * scale;
                double MAX_LINE_WIDTH_PIXELS = 2;
                if (highlightLineWidthPixels > MAX_LINE_WIDTH_PIXELS
                    || oldFill != null) {
                    // If somebody zooms in a lot, drawing the line at normal
                    // size can make it hard to figure out where to put the
                    // control points. Instead, draw the line as normal and
                    // then draw a thinner highlighted line inside it.
                    highlightLineWidth = MAX_LINE_WIDTH_PIXELS / scale;
                    csel.setLineWidth(highlightLineWidth);
                }

                // Disable anti-aliasing for this phase because it
                // prevents the green line from precisely overwriting
                // the red line.

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_OFF);

                AutoPositionHolder ap = new AutoPositionHolder();
                Point2D.Double extraVertex = mprin;
                if (isZoomMode() || !isEditable()) {
                    extraVertex = null;
                } else if (isShiftDown) {
                    extraVertex = statusPt = getAutoPosition(ap);
                } else if (mouseIsStuckAtSelection()) {
                    // Show the point that would be added if the mouse became
                    // unstuck.
                    extraVertex = getMousePrincipal();
                }

                if (extraVertex != null && !isDuplicate(extraVertex)) {
                    // Add the current mouse position to the path next to the
                    // currently selected vertex, and draw the curve that
                    // results from this addition in red. Then remove the
                    // extra vertex.

                    csel.setColor(toColor(ap.position));

                    int vip = vertexInsertionPosition();
                    path.getCurve().add(vip, extraVertex, smoothed);
                    csel.draw(g, scale);
                    path.remove(vip);
                }

                g.setColor(highlight);
                csel.setColor(highlight);
                csel.draw(g, scale);
                double r = Math.max(path.getLineWidth() * scale * 1.7, 4.0);
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

                csel.setLineWidth(originalLineWidth);
                csel.setColor(oldColor);
                path.setFill(oldFill);

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
            }
    }


    /** Mark every key point with a circle. */
    void highlightKeyPoints(Graphics2D g, double scale) {
        double r = 4.0;
        Point2D.Double xpoint = new Point2D.Double();
        Affine p2d = principalToScaledPage(scale);

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke((float) (r / 4)));

        for (Point2D.Double point: keyPoints(false)) {
            p2d.transform(point, xpoint);
            Shape shape = new Ellipse2D.Double
                (xpoint.x - r, xpoint.y - r, r * 2, r * 2);
            g.draw(shape);
        }

        g.setStroke(oldStroke);
    }


    /** Special label highlighting rules: show the label box, and if
        the anchor is not at the center, show the anchor as either a
        hollow circle (if not selected) or as a solid circle (if
        selected). */
    void paintSelectedLabel(Graphics2D g, double scale) {
        LabelHandle hand = getLabelHandle();
        LabelInfo labelInfo = hand.getItem();
        AnchoredLabel label = labelInfo.label;

        boolean isBoxed = label.isBoxed();
        // label.setBoxed(true);
        draw(g, labelInfo, scale);

        if (label.getXWeight() != 0.5 || label.getYWeight() != 0.5) {
            // Mark the anchor with a circle -- either a solid circle
            // if the selection handle is the anchor, or a hollow
            // circle if the selection handle is the label's center.
            double r = Math.max(scale * 2.0 / BASE_SCALE, 4.0);
            Point2D.Double p = new Point2D.Double
                (label.getXWeight(), label.getYWeight());
            labelToScaledPage(labelInfo, scale).transform(p, p);
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

    public void paintScreenBackground(Graphics2D g, double scale, Color color) {
        BackgroundImageType back = editFrame.getBackgroundImage();
        boolean showBackgroundImage = tracingImage()
            && back != BackgroundImageType.NONE
            && (back != BackgroundImageType.BLINK
                || backgroundImageEnabled);

        if (showBackgroundImage) {
            paintBackgroundImage(g, scale);
        } else {
            super.paintBackground(g, scale, Color.WHITE);
        }
    }

    public double gridStep(LinearAxis ax) {
        if (isTernary()) {
            return 0.1;
        } else if (isPixelMode()) {
            return 0.5;
        } else {
            return RulerTick.roundFloor(length(ax) / 15);
        }
    }
    
    public double gridStepX() {
        return gridStep(getXAxis());
    }

    public double gridStepY() {
        return gridStep(getYAxis());
    }
    
    public Point2D.Double nearestGridPoint(Point2D prin) {
        double gx = gridStepX();
        double x = prin.getX();
        x = Math.rint(x / gx) * gx;
        double gy = gridStepY();
        double y = prin.getY();
        y = Math.rint(y / gy) * gy;
        return new Point2D.Double(x,y);
    }

    public void paintGridLines(Graphics2D g, double scale,
                               LinearAxis lineAx, LinearAxis stepAx,
                               double dstep) {
        if (principalToStandardPage == null) {
            return;
        }
        double[] lineRange = getRange(lineAx);
        double[] stepRange = getRange(stepAx);

        double stepMin = dstep * Math.ceil(stepRange[0] / dstep);
        double stepMax = dstep * Math.floor(stepRange[1] / dstep) + dstep / 2;
        double lineMin = lineRange[0];
        double lineMax = lineRange[1];
        Affine toPage;

        try {
            toPage = inverseTransform(lineAx, stepAx);
        } catch (NoninvertibleTransformException x) {
            throw new IllegalStateException
                ("Axes " + lineAx + " and " + stepAx + " are not L.I.");
        }
        toPage.preConcatenate(principalToScaledPage(scale));
        g.setColor(Color.GRAY);
        for (double step = stepMin; step < stepMax; step += dstep) {
            Point2D.Double p1 = toPage.transform(lineMin, step);
            Point2D.Double p2 = toPage.transform(lineMax, step);
            g.drawLine((int) Math.round(p1.x), (int) Math.round(p1.y),
                       (int) Math.round(p2.x), (int) Math.round(p2.y));
        }
    }

    public void paintGridLines(Graphics2D g, double scale) {
        double xstep = isPixelMode() ? 1.0 : gridStepX();
        double ystep = isPixelMode() ? 1.0 : gridStepY();
        paintGridLines(g, scale, getXAxis(), getYAxis(), ystep);
        paintGridLines(g, scale, getYAxis(), getXAxis(), xstep);
        if (isTernary()) {
            paintGridLines(g, scale, getYAxis(), getLeftAxis(), xstep);
        }
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

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

        paintScreenBackground(g, scale, Color.WHITE);
        if (showGrid) {
            paintGridLines(g, scale);
        }

        boolean showSel = selection != null;
        statusPt = mprin;

        Decoration sel = showSel ? selection.getDecoration() : null;
        boolean isCurve = selection instanceof VertexHandle;
        for (int dn = 0; dn < decorations.size(); ++dn) {
            Decoration decoration = decorations.get(dn);
            if (decoration.equals(sel) && !isCurve) {
                try (UpdateSuppressor us = new UpdateSuppressor()) {
                        Color oldColor = sel.getColor();
                        Color highlight = getHighlightColor(oldColor);

                        g.setColor(highlight);
                        sel.setColor(highlight);
                        if (selection instanceof LabelHandle) {
                            paintSelectedLabel(g, scale);
                        } else {
                            sel.draw(g, scale);
                        }
                        sel.setColor(oldColor);
                    }
            } else {
                g.setColor(thisOrBlack(decoration.getColor()));
                decoration.draw(g, scale);
            }
        }

        if (isCurve) {
            paintSelectedCurve(g, scale);
        }

        // g.setColor(Color.GREEN);
        // highlightKeyPoints(g, scale);

        if (getVertexHandle() == null) {
            Point2D.Double gmp = getMousePrincipal();
            if (isShiftDown) {
                AutoPositionHolder ap = new AutoPositionHolder();
                Point2D.Double autop = getAutoPosition(ap);
                if (autop != null) {
                    statusPt = autop;
                    if (gmp != null && !principalCoordinatesMatch(autop, gmp)) {
                        g.setColor(toColor(ap.position));
                        paintCross(g, autop, scale);
                    }
                }
            } else if (mouseIsStuck && mprin != null) {
                g.setColor(new Color(0xb0c000));
                paintCross(g, mprin, scale);
            }

        }

        if (isZoomMode()) {
            Point mpos = getEditPane().getMousePosition();
            if (mpos != null) {
                g.setColor(Color.RED);
                g.draw(Geom.bounds
                       (new Point[] { mpos, mousePress.e.getPoint() }));
            }
        }

        if (statusPt != null) {
            editFrame.setStatus(principalToPrettyString(statusPt));
        }
    }

    /** Paint a crosshairs at principal coordinate p. */
    void paintCross(Graphics g, Point2D.Double p, double scale) {
        Point2D.Double vPage = principalToScaledPage(scale).transform(p);
        int r = 11;
        int ix = (int) vPage.x;
        int iy = (int) vPage.y;
        int r2 = 1;
        for (int offs = -r2; offs <= r2; ++offs) {
            g.drawLine(ix + offs, iy - r, ix + offs, iy - r2 - 1);
            g.drawLine(ix - r, iy + offs, ix - r2 - 1, iy + offs);
            g.drawLine(ix + offs, iy + r, ix + offs, iy + r2 + 1);
            g.drawLine(ix + r, iy + offs, ix + r2 + 1, iy + offs);
        }
    }

    public void clearSelection() {
        setSelection(null);
    }

    public void setSelection(DecorationHandle hand) {
        if (selection == hand) {
            return;
        }
        selection = hand;
        boolean haveSel = (hand != null);
        getEditFrame().actDeselect.setEnabled(haveSel);
        mnRightClick.setHasSelection(haveSel);
        if (selection != null) {
            showTangent(selection);
            editFrame.setColor(thisOrBlack(hand.getDecoration().getColor()));
        } else {
            editFrame.setColor(color);
        }
        redraw();
    }

    public void deselectCurve() {
        clearSelection();
    }

    public void setFill(StandardFill fill) {
        String errorTitle = "Cannot change fill settings";
        CuspFigure path = getSelectedCuspFigure();
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

    /** Toggle the highlighted vertex between the smoothed and
        un-smoothed states. */
    public void toggleCusp() {
        VertexHandle vhand = getVertexHandle();
        if (vhand == null) {
            return;
        }

        vhand.getItem().getCurve().toggleSmoothed(vhand.vertexNo);
        propagateChange();
    }

    /** Update the math window to show whatever information is
        available for the given handle. */
    public void showTangent(DecorationHandle hand) {
        if (hand instanceof BoundedParam2DHandle) {
            BoundedParam2DHandle bp = (BoundedParam2DHandle) hand;
            showTangent(bp.getDecoration(), bp.getT());
        } else {
            showTangent(hand.getDecoration());
        }
    }
            

    /** Return the slope at the given handle in terms of
        dStandardPageY/dStandardPageX. */
    public Point2D.Double derivative(BoundedParam2DHandle hand) {
        return derivative(hand.getDecoration(), hand.getT());
    }

    /** Return the slope at the given t value in terms of
        dStandardPageY/dStandardPageX. */
    public Point2D.Double derivative(Decoration dec, double t) {
        BoundedParam2D param = ((BoundedParameterizable2D) dec)
            .getParameterization();
        if (dec instanceof CurveDecoration) {
            CurveDecoration cdec = (CurveDecoration) dec;
            CuspFigure path = cdec.getItem();
            if (t == Math.floor(t) && !path.getCurve().isSmoothed((int) t)) {
                // Cusps have different slopes depending on whether
                // one approaches from the positive or negative t
                // direction. insertBeforeSelection gives a hint which
                // which side to approach from.
                t = BoundedParam2Ds.constrainToDomain
                    (param, t + 1e-10 * (insertBeforeSelection ? -1 : 1));
            }
        }

        return param.getDerivative(t);
    }

    public void showTangent(Decoration dec, double t) {
        Point2D.Double g = derivative(dec, t);
        if (g != null) {
            mathWindow.setScreenDerivative(g);
        }
        if (dec instanceof CurveDecoration && showLength()) {
            CuspFigure c = ((CurveDecoration) dec).getItem();
            BoundedParam2D b = getPrincipalParameterization(dec);
            Param2D uc = b.getUnboundedCurve();

            double t0, t1;
            double areaMul;
            if (c.curve.getStart().x > c.curve.getEnd().x) {
                areaMul = -1;
                t0 = t;
                t1 = b.getMaxT();
            } else {
                areaMul = 1;
                t0 = b.getMinT();
                t1 = t;
            }
            double area = uc.area(t0, t1) * areaMul;
            double totArea = b.area() * areaMul;
            double length = uc.length(0, 1e-6, 800, t0, t1).value;
            double totLength = b.length(0, 1e-6, 800).value;

            if (c.isClosed()) {
                mathWindow.setTotLengthLabel("Perimeter");
                mathWindow.setTotAreaLabel("Area");
                totArea = Math.abs(totArea);
            } else {
                mathWindow.setTotLengthLabel("Total length");
                mathWindow.setTotAreaLabel("Total \u222B");
            }

            mathWindow.setArea(area);
            mathWindow.setTotArea(totArea);
            mathWindow.setLength(length);
            mathWindow.setTotLength(totLength);
        }
            
        showTangentCommon(dec);
    }

    boolean showLength() {
        if (isTernary()) { return false; }
        for (String s: diagramComponents) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }

    boolean showArea() {
        return showLength();
    }

    public void showTangent(Decoration dec) {
        if (dec == null) {
            return;
        }
        if (dec instanceof Angled) {
            double theta = ((Angled) dec).getAngle();
            Point2D.Double p = new Point2D.Double(Math.cos(theta),
                                                  Math.sin(theta));
            mathWindow.setDerivative(p);
        }
        showTangentCommon(dec);
    }

    void showTangentCommon(Decoration dec) {
        double w = dec.getLineWidth();
        if (w != 0) {
            mathWindow.setLineWidth(w);
        }
    }

    /** Return true if, were point p inserted into the currently
        selected curve at the current position, it would be the same
        as the point preceding or following it. SplinePolyline barfs
        on smoothing between a series of points where the same point
        appears twice in a row, so inserting duplicate points is
        bad. */
    boolean isDuplicate(Point2D p) {
        VertexHandle vhand = getVertexHandle();
        if (vhand == null) {
            return false;
        }
        CuspFigure path = vhand.getItem();
        int vip = vertexInsertionPosition();
        int s = path.size();
        return (vip > 0 && principalCoordinatesMatch(p, path.get(vip-1), 1e-9))
            || (vip < s && principalCoordinatesMatch(p, path.get(vip), 1e-9))
            || (path.isClosed() && s > 1 &&
                ((vip == 0 && principalCoordinatesMatch(p, path.get(s-1), 1e-9))
                 || (vip == s && principalCoordinatesMatch(p, path.get(0), 1e-9))));
    }

    int vertexInsertionPosition() {
        return getVertexHandle().vertexNo + (insertBeforeSelection ? 0 : 1);
    }

    /** Add a point to getActiveCurve(), or create and select a new
        curve if no curve is currently selected. */
    public void add(Point2D.Double point) {
        if (isDuplicate(point) || principalToStandardPage == null) {
            return; // Adding the same point twice causes problems.
        }
        CuspFigure path = getSelectedCuspFigure();
        if (path == null) {
            // Start a new curve consisting of a single point, and
            // make it the new selection.
            path = new CuspFigure(new CuspInterp2D(false), lineStyle, lineWidth);
            CurveDecoration d = new CurveDecoration(path);
            decorations.add(d);
            add(path, 0, point, smoothed);
            d.setColor(color);
            setSelection(new VertexHandle(d, 0));
            insertBeforeSelection = false;
        } else {
            // Add a new point to the currently selected curve.
            VertexHandle vhand = getVertexHandle();
            add(path, vertexInsertionPosition(), point, smoothed);
            if (!insertBeforeSelection) {
                ++vhand.vertexNo;
            }
            showTangent(vhand);
        }
    }

    @Override public void remove(CuspFigure path) {

        // If an incomplete tie line selection refers to this curve,
        // then stop selecting a tie line.

        for (PathAndT pat: tieLineCorners) {
            if (pat.path == path) {
                tieLineDialog.setVisible(false);
                tieLineCorners = new ArrayList<>();
                break;
            }
        }
        super.remove(path);
    }

    /** Print an arrow at the currently selected location at the angle
        given in the math window, even if that value is hidden. When
        you choose a point on a line, the angle gets reset to be
        tangent to the curve at that location and pointing rightward
        (or downward if the curve is vertical).
    */
    public void addArrow(boolean rightward) {
        if (mprin == null) {
            showError("The mouse must be inside the diagram.");
            return;
        }

        if (mouseIsStuckAtSelection() && getSelectedArrow() != null) {
            setMouseStuck(false);
        }

        double theta = mathWindow.getAngle();
        if (Double.isNaN(theta)) {
            // It's easier to use angle 0 than to explain to users
            // what the problem is.
            theta = 0;
        }
        if (!rightward) {
            theta += Math.PI;
        }

        addArrow(mprin, lineWidth, pageToPrincipalAngle(theta));
        resetLastDecorationColor();
    }

    private void resetLastDecorationColor() {
        decorations.get(decorations.size()-1).setColor(color);
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
                ("This selection must belong to the same " +
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
            tieLineCorners = new ArrayList<>();
            return;
        }

        TieLine tie = new TieLine(lineCnt, lineStyle);
        tie.lineWidth = lineWidth;
        tie.setColor(color);

        tie.innerEdge = tieLineCorners.get(2).path;
        tie.it1 = tieLineCorners.get(2).t;
        tie.it2 = tieLineCorners.get(3).t;

        tie.outerEdge = tieLineCorners.get(0).path;
        tie.ot1 = tieLineCorners.get(0).t;
        tie.ot2 = tieLineCorners.get(1).t;

        TieLineDecoration d = new TieLineDecoration(tie);
        setSelection(new TieLineHandle(d, TieLineHandleType.OUTER2));
        addDecoration(d);
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
        CuspFigure path = getSelectedCuspFigure();
        if (path == null || path.size() != 2) {
            showError
                ("Before you can create a new ruler, "
                 + "you must create and select a curve "
                 + "consisting of exactly two vertices "
                 + "which will become the rulers' endpoints.",
                 errorTitle);
            return;
        }

        LinearRuler r = new LinearRuler();
        r.fontSize = rulerFontSize();
        r.tickPadding = 0.0;
        r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        r.drawSpine = true;
        r.lineWidth = lineWidth;
        r.setColor(color);


        if (isTernary()) {
            r.tickType = LinearRuler.TickType.V;
        }

        VertexHandle vhand = getVertexHandle();

        r.startPoint = path.get(1 - vhand.vertexNo);
        r.endPoint = path.get(vhand.vertexNo);
        Point2D.Double pageVec = Geom.aMinusB(r.endPoint, r.startPoint);
        principalToStandardPage.deltaTransform(pageVec, pageVec);
        if (pageVec.x < -1e-6) {
            // For rulers with an appreciable horizontal component,
            // make the left point the start.
            Point2D.Double p = r.startPoint;
            r.startPoint = r.endPoint;
            r.endPoint = p;
            Geom.invert(pageVec);
        }
        r.textAngle = Math.atan2(-pageVec.y, pageVec.x);

        LinearAxis axis = null;
        double maxCosSq = -1;

        // For the initial default variable, use a variable for which
        // pageVec is closest to parallel to the gradient. A
        // horizontal axis is not likely to be meant to measure
        // changes in the vertical variable!

        int axisNo = -1;
        for (LinearAxis axisTemp: axes) {
            ++axisNo;
            Point2D.Double pageGrad = pageGradient(axisTemp);
            double cosSq = Geom.cosSq(pageGrad, pageVec);
            if (((String) axisTemp.name).contains("page ")) {
                // An axis for the page coordinates? Not likely...
                cosSq /= 4;
            }
            // Prefer later axes to earlier ones as a tiebreaker,
            // mainly so "Right" is preferred over "Left".
            cosSq *= (10000 + axisNo);
            if (cosSq > maxCosSq) {
                axis = axisTemp;
                maxCosSq = cosSq;
            }
        }

        r.axis = axis;
        if (axis.isPercentage()) {
            r.multiplier = 100.0;
        }

        if (!getRulerDialog().showModal(r, axes, principalToStandardPage)) {
            return;
        }

        vhand.getDecoration().remove();
        RulerDecoration d = new RulerDecoration(r);
        addDecoration(d);
        setSelection(new RulerHandle(d, 1));
    }

    public void renameVariable(String name) {
        // If the user wants to rename or add a component, they should
        // use Chemistry/Components instead. The only time it makes
        // sense to redirect to that is if the old value is a component.

        for (Side side: Side.values()) {
            Axis axis = getAxis(side);
            if (axis != null && axis.name.equals(name)
                && (isTernary() || diagramComponents[side.ordinal()] != null)) {
                setDiagramComponent(side);
                return;
            }
        }

        String newName = JOptionPane.showInputDialog
            (editFrame, "New name for variable '" + name + "':");

        if (newName == null || "".equals(newName)) {
            return;
        }
        for (Axis axis: axes) {
            if (axis.name.equals(newName)) {
                showError("A variable with that name already exists.");
                return;
            }
        }

        for (LinearAxis axis: axes) {
            if (axis.name.equals(name)) {
                rename(axis, newName);
                return;
            }
        }
        
        throw new IllegalStateException("No such variable '" + name + "'");
    }

    @Override public void rename(LinearAxis axis, String name) {
        if (axis.name != null) {
            editFrame.removeVariable((String) axis.name);
        }
        super.rename(axis, name);
        editFrame.addVariable(name);
        mathWindow.refresh();
    }

    @Override public void add(LinearAxis axis) {
        super.add(axis);
        editFrame.addVariable((String) axis.name);
    }

    @Override public void remove(LinearAxis axis) {
        RulerDecoration rdec = getSelectedRuler();
        if (rdec != null && axis == rdec.getItem().axis) {
            clearSelection();
        }
        super.remove(axis);
        editFrame.removeVariable((String) axis.name);
    }

    public void addVariable() {
        String errorTitle = "Cannot add variable";
        CuspFigure path = getSelectedCuspFigure();
        if (path == null || path.size() != 3) {
            showError(
"To add a user variable, first select a curve consisting of three points "
+ "where the variable's value is known. The points must not all lie on the same "
+ "line."
+ "<p>For example, for a variable whose values range from 0 to 1, "
+ "you might click on two different points where the variable should equal 0, "
+ "plus a third point where the variable should equal 1."
+ "<p>User variables must vary at the same rate throughout the entire diagram "
+ "&mdash; that is, they must be linear functions of the screen position.",
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
                 htmlify("Enter the name of the new variable and its value "
                         + "at the three points you selected. Fractions are allowed. "
                         + "For percentages, do not omit the percent sign."));
            dog.setTitle("Create new axis");
            values = dog.showModalStrings();
            if (values == null) {
                return;
            }

            if ("".equals(values[0])) {
                JOptionPane.showMessageDialog
                    (editFrame, "Please enter a variable name.");
                continue;
            }

            boolean ok = true;
            for (LinearAxis axis: axes) {
                if (values[0].equals(axis.name)) {
                    showError("You cannot reuse the name of an existing variable.");
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }

            double[] dvs = new double[3];
            boolean maybePercentage = true;
            for (int i = 0; i < dvs.length; ++i) {
                try {
                    dvs[i] = ContinuedFraction.parseDouble(values[i+1]);
                    maybePercentage = maybePercentage &&
                        (dvs[i] >= 0 && dvs[i] <= 1);
                } catch (NumberFormatException e) {
                    showError("Invalid number format '" + values[i+1] + "'");
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
                     htmlify("Display variable as a percentage "
                             + "in the gray status bar at the bottom of "
                             + "the window?"),
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
                showError
                    ("These points are colinear and cannot be used to define an axis."
                     + e + ", " + xform);
                return;
            }
        }
    }

    public void togglePercentageDisplay(String varName) {
        for (Axis axis: axes) {
            if (varName.equals(axis.name)) {
                setPercentageDisplay(axis, !axis.isPercentage());
                return;
            }
        }
    }

    /** Toggle the initial setting for vertexes added in the future
        between the smoothed and un-smoothed states. */
    public void setSmoothed(boolean b) {
        smoothed = b;
        editFrame.setSmoothed(b);
    }

    /** Toggle whether to show grid lines at round (x,y) values. */
    public void setShowGrid(boolean b) {
        showGrid = b;
        redraw();
    }

    @Override public void setUsingWeightFraction(boolean b) {
        super.setUsingWeightFraction(b);
        editFrame.setUsingWeightFraction(b);
    }

    /** Set pixel mode on or off, but also make other related settings
        changes the user usually wants. */
    public void setPixelModeComplex(boolean b) {
        setPixelMode(b);
        if (b) {
            setGridLineWidth(1.0);
            setShowGrid(true);
        }
    }

    @Override public void setPixelMode(boolean b) {
        super.setPixelMode(b);
        editFrame.setPixelMode(b);
    }

    int convertLabels() {
        return alwaysConvertLabels ? JOptionPane.YES_OPTION
            : JOptionPane.showConfirmDialog
            (editFrame,
             "Convert diagram labels? The conversion "
             + "is not guaranteed to be correct or reversible.",
             "Convert diagram labels",
             JOptionPane.YES_NO_CANCEL_OPTION,
             JOptionPane.QUESTION_MESSAGE);
    }

    /** Open a dialog asking whether to convert labels, and pass the
        user's answer along to moleToWeightFraction(boolean). */
    public boolean moleToWeightFraction() {
         switch (convertLabels()) {
        case JOptionPane.YES_OPTION:
            return moleToWeightFraction(true);

         case JOptionPane.NO_OPTION:
             return moleToWeightFraction(false);

         default:
             return false;
         }
    }

    @Override public boolean moleToWeightFraction(boolean convertLabels) {
        boolean res = super.moleToWeightFraction(convertLabels);
        if (res && mprin != null) {
            moveMouse(moleToWeightFraction(mprin));
        }
        bestFit();
        return res;
    }

    /** Open a dialog asking whether to convert labels, and pass the
        user's answer along to weightToMoleFraction(boolean). */
    public boolean weightToMoleFraction() {
         switch (convertLabels()) {
        case JOptionPane.YES_OPTION:
            return weightToMoleFraction(true);

         case JOptionPane.NO_OPTION:
             return weightToMoleFraction(false);

         default:
             return false;
         }
    }

    @Override public boolean weightToMoleFraction(boolean convertLabels) {
        boolean res = super.weightToMoleFraction(convertLabels);
        if (res && mprin != null) {
            moveMouse(weightToMoleFraction(mprin));
        }
        bestFit();
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
        FormulaDialog dog = getFormulaDialog();
        dog.okButton.setText("Locate compound in diagram");
        String compound = dog.showModal(true);
        if (compound == null) {
            return;
        }

        String errorTitle = "Could not compute component ratios";
        String nonEditableError = "This diagram does not support that feature.";
        double[][] componentElements = getComponentElements();

        ArrayList<Side> sides = new ArrayList<>();
        ArrayList<Side> badSides = new ArrayList<>();
        for (Side side: sidesThatCanHaveComponents()) {
            if (diagramComponents[side.ordinal()] == null) {
                if (isEditable()) {
                    showError("The " + side.toString().toLowerCase()
                              + " diagram component is not defined. "
                              + "Define it with the \"Chemistry/Components/Set "
                              + side.toString().toLowerCase() + " component\" menu item.");
                } else {
                    showError(nonEditableError);
                }
                return;
            }
            if (componentElements[side.ordinal()] == null) {
                badSides.add(side);
            } else {
                sides.add(side);
            }
        }

        if (sides.size() < 2 + (isTernary() ? 1 : 0)) {
            if (!isEditable()) {
                showError(nonEditableError);
            } else {
                StringBuilder message = new StringBuilder
                    ("The following diagram component(s) were not successfully\n"
                     + "parsed as compounds:\n");
                int sideNo = -1;
                for (Side side: badSides) {
                    ++sideNo;
                    if (sideNo > 0) {
                        message.append(", ");
                    }
                    message.append(side.toString());
                }

                showError(message.toString());
            }
            return;
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
        for (Map.Entry<String, Double> pair: dog.getComposition().entrySet()) {
            String element = pair.getKey();
            Integer i = elementIndexes.get(element);
            if (i == null) {
                showError("This diagram does not contain "
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
            String s = "<p>Showing the best fit, which has a relative error of "
                + String.format("%.2f%%", 100 * totalError / totalAtoms) + ".";
            if (totalError > 0.02) {
                s = s + "<p>The formula you entered is not an approximate "
                    + "linear combination of the diagram components. "
                    + "Note that this program will not adjust the "
                    + "subscript of oxygen to balance the equation. "
                    + "So this fit is probably worthless.";
            }
            showError(s);
        }

        Point2D.Double fractions;

        if (isTernary()) {
            fractions = new Point2D.Double(0,0);
        } else if (mprin != null) {
            fractions = new Point2D.Double(0,mprin.y);
        } else {
            Rectangle2D b = principalToStandardPage.inputBounds();
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

        setMouseStuck(true);
        if (!moveMouse(fractions)) {
            showError
                ("You requested the coordinates<br>"
                 + principalToPrettyString(fractions)
                 + "<br>which lie outside the boundary of the diagram.",
                 "Could not move mouse");
        }
    }

    public String selectedCoordinatesToString
        (LinearAxis v1, RealFunction f1,
         LinearAxis v2, RealFunction f2, boolean addComments, int sigFigs) {
        CuspFigure path = getSelectedCuspFigure();
        if (path != null) {
            return toString(Arrays.asList(path.getPoints()), v1, f1, v2, f2, sigFigs);
        }

        LabelDecoration ldec = getSelectedLabel();
        if (ldec != null) {
            return toString(labelCoordinates(ldec.getLabel().getText()),
                            v1, f1, v2, f2, sigFigs);
        }

        nothingToExportError();
        return null;
    }

    /** Return a DigitizeDialog that can be polled for the specifics
     * of how the data are to be exported, or null if the user
     * canceled the operation. */
    DigitizeDialog exportDialog() {
        initializeDigitizeDialog();
        DigitizeDialog dog = digitizeDialog;
        for (int i = 0; i < dog.getVariableCount(); ++i) {
            if (dog.getVariable(i, axes).isPercentage()) {
                dog.setFunction(i, StandardRealFunction.TO_PERCENT);
            }
        }
            
        dog.setTitle("Export");
        dog.getSourceLabel().setText("Destination");
        dog.setExport(true);
        dog.pack();
        if (!dog.showModal()) {
            return null;
        }
        return dog;
    }

    /** Export the string s to the type of destination indicated in
        variable dig's state information. dig should already have had
        showModal() called on it. */

    public void exportString(String s, DigitizeDialog dig) {
        if (s == null) {
            return;
        }
        if (dig.getSourceType() == DigitizeDialog.SourceType.FILE) {
            File of = getExportFile();
            if (of == null) {
                return;
            }
            try (OutputStream ofs = new FileOutputStream(of)) {
                    try (OutputStreamWriter w = new OutputStreamWriter
                         (ofs, StandardCharsets.UTF_8)) {
                            w.write(s, 0, s.length());
                        }
                }
            catch (IOException x) {
                showError(x.toString(), "Write Failed");
                return;
            }
            JOptionPane.showMessageDialog
                (editFrame, "Saved '" + of + "'.");
        } else {
            copyToClipboard(s, false);
        }
    }

    public void exportAllCoordinates() {
        DigitizeDialog dig = exportDialog();
        if (dig == null) {
            return;
        }
        exportString(allCoordinatesToString
                     (dig.getVariable(0, axes), dig.getFunction(0),
                      dig.getVariable(1, axes), dig.getFunction(1),
                      dig.isCommented(), dig.getSigFigs()),
                     dig);
    }

    void nothingToExportError() {
        showError("You must first select a curve or label whose "
                  + "coordinates are to be copied. "
                  + HOW_TO_SELECT);
    }

    public void exportSelectedCoordinates() {
        if (selection == null) {
            nothingToExportError();
            return;
        }
        DigitizeDialog dig = exportDialog();
        if (dig == null) {
            return;
        }
        exportString(selectedCoordinatesToString
                     (dig.getVariable(0, axes), dig.getFunction(0),
                      dig.getVariable(1, axes), dig.getFunction(1),
                      dig.isCommented(), dig.getSigFigs()),
                     dig);
    }

    public void importCoordinates() {
        DigitizeDialog dig = importDialog();
        if (dig == null) {
            return;
        }

        String str = (dig.getSourceType() == DigitizeDialog.SourceType.FILE)
            ? importStringFromFile() : clipboardContents();
        if (str == null) {
            return;
        }

        copyCoordinatesFromString
            (str,
             dig.getVariable(0, axes), dig.getFunction(0),
             dig.getVariable(1, axes), dig.getFunction(1));
    }

    public static Point2D.Double[][] stringToCurves(String pointsStr)
        throws NumberFormatException {
        String[] lines = pointsStr.split("\r?\n");
        ArrayList<ArrayList<Point2D.Double>> res = new ArrayList<>();
        ArrayList<Point2D.Double> curve = new ArrayList<>();
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i].trim();
            int sharp = line.indexOf('#');
            if (sharp >= 0) {
                line = line.substring(0, sharp).trim();
            }
            if (line.equals("")) {
                if (curve.size() > 0) {
                    res.add(curve);
                    curve = new ArrayList<>();
                }
                continue;
            }
            String[] xAndYStr = line.split(",");
            if (xAndYStr.length != 2) {
                throw new NumberFormatException
                    ("Line '" + line + "' does not have format 'x,y'");
            }
            double[] xAndY = {0.0, 0.0};
            for (int j = 0; j < 2; ++j) {
                String s = xAndYStr[j];
                xAndY[j] = ContinuedFraction.parseDouble(s);
            }
            curve.add(new Point2D.Double(xAndY[0], xAndY[1]));
        }
        if (curve.size() > 0) {
            res.add(curve);
        }

        Point2D.Double[][] res2 = new Point2D.Double[res.size()][];
        for (int i = 0; i < res.size(); ++i) {
            res2[i] = res.get(i).toArray(new Point2D.Double[0]);
        }
        return res2;
    }

    public void copyCoordinatesFromString
        (String lines,
         LinearAxis v1, RealFunction f1,
         LinearAxis v2, RealFunction f2) {

        if (principalToStandardPage == null) {
            return;
        }

        Affine xformi;

        if (v1 == v2) {
            showError("Please choose two different variables.");
            return;
        }
        try {
            xformi = inverseTransform(v1, v2);
        } catch (NoninvertibleTransformException e) {
            showError("The combination of " + (String) v1.name + " and "
                      + (String) v2.name + " cannot be used to  "
                      + "uniquely identify points (the two variables "
                      + "are not linearly independent).");
            return;
        }

        boolean haveLabel = getSelectedLabel() != null;
        boolean haveCurve = getSelectedCuspFigure() != null;
           
        
        try {
            Point2D.Double[][] curves = stringToCurves(lines);

            Rectangle2D.Double bounds = null;
            for (Point2D.Double[] curve: curves) {
                for (Point2D.Double point: curve) {
                    point.setLocation(xformi.transform(transform(point, f1, f2)));
                    Point2D.Double page = principalToStandardPage.transform(point);
                    if (bounds == null) {
                        bounds = new Rectangle2D.Double(page.x, page.y, 0, 0);
                    } else {
                        bounds.add(page);
                    }
                }
            }

            boolean expand = false;
            if (bounds != null && !pageBounds.contains(bounds)) {
                if (JOptionPane.showConfirmDialog
                    (editFrame,
                     "Expand diagram so these points can fit?",
                     "Import data",
                     JOptionPane.OK_CANCEL_OPTION)
                        == JOptionPane.YES_OPTION) {
                    expand = true;
                } else {
                    return;
                }
            }
            
            for (Point2D.Double[] curve: curves) {
                if (!haveLabel && !haveCurve) {
                    deselectCurve();
                }
                for (Point2D.Double point: curve) {
                    if (haveLabel) {
                        selection.copy(point);
                    } else {
                        add(point);
                    }
                }
            }

            if (expand) {
                computeMargins(true);
            }
        } catch (NumberFormatException x) {
            showError(x.toString());
            return;
        }
    }
    
    static void copyToClipboard(String str, boolean ignoreFailure) {
        try {
            StringSelection sel = new StringSelection(str);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents
                (sel, sel);
        } catch (HeadlessException e) {
            if (!ignoreFailure) {
                throw new IllegalArgumentException
                    ("Can't call coordinatesToClipboard() in a headless environment:" + e);
            }
        }
    }
    
    String clipboardContents() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                .getData(DataFlavor.stringFlavor);
        } catch (HeadlessException e) {
            throw new IllegalArgumentException
                ("Can't call coordinatesToClipboard() in a headless environment:" + e);
        } catch (UnsupportedFlavorException e) {
            throw new IllegalStateException
                ("StringFlavor unsupported (internal:" + e);
        } catch (IOException e) {
            throw new IllegalStateException
                ("While getting clipboard: " + e);
        }
    }

    /** Ask the user the name of a file to export data to. Return null
        if they abort the process. */
    File getExportFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export");
        chooser.setFileFilter
            (new FileNameExtensionFilter("Text files",
                                         new String[] {"txt", "csv"}));
        String dir = getCurrentDirectory();
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
       if (chooser.showSaveDialog(editFrame) == JFileChooser.APPROVE_OPTION) {
           File file = chooser.getSelectedFile();
           if (getExtension(file.getName()) == null) {
               // Add the default extension
               file = new File(file.getAbsolutePath() + ".txt");
           }
           setCurrentDirectory(file.getParent());
           return file;
       } else {
           return null;
       }
    }
    
    /** Ask the user the name of a file to import data from. Return null
        if they abort the process. */
    File getImportFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import text or CSV");
        chooser.setFileFilter
            (new FileNameExtensionFilter("Text files",
                                         new String[] {"txt", "csv"}));
        String dir = getCurrentDirectory();
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
       if (chooser.showOpenDialog(editFrame) == JFileChooser.APPROVE_OPTION) {
           File file = chooser.getSelectedFile();
           setCurrentDirectory(file.getParent());
           return file;
       } else {
           return null;
       }
    }
    
    /** Ask the user the name of a UTF-8 file to import data from, and
        return the contents of that file. Return null if they abort
        the process. */
    String importStringFromFile() {
        File file = getImportFile();
        if (file == null) {
            return null;
        }

        try {
            return importStringFromFile(file);
        } catch (IOException e) {
            showError("Could not read file '" + file + "' : " + e);
            return null;
        }
    }

    /** Read the given UTF-8 file and return the contents as a string. */
    static String importStringFromFile(File file) throws IOException {
        StringBuilder res = new StringBuilder();
        for (String line: Files.readAllLines(file.toPath(),
                                             StandardCharsets.UTF_8)) {
            res.append(line);
            res.append('\n');
        }
        return res.toString();
    }

    private void initializeDigitizeDialog() {
        if (digitizeDialog == null) {
            digitizeDialog = new DigitizeDialog();
        }
        digitizeDialog.setAxes(axes, new LinearAxis[] {getXAxis(), getYAxis()});
    }
    
    /** Show a dialog asking the user where to import data in string
        form from, and return the dialog. Return null if the process
        is aborted. */
    DigitizeDialog importDialog() {
        initializeDigitizeDialog();
        DigitizeDialog dog = digitizeDialog;
        for (int i = 0; i < dog.getVariableCount(); ++i) {
            if (dog.getVariable(i, axes).isPercentage()) {
                dog.setFunction(i, StandardRealFunction.FROM_PERCENT);
            }
        }

        dog.setTitle("Import");
        dog.getSourceLabel().setText("Source");
        dog.setExport(false);
        dog.pack();
        if (!dog.showModal()) {
            return null;
        }
        return dog;
    }

    @Override public void expandMargins(double factor) {
        super.expandMargins(factor);
        bestFit();
    }

    @Override public void computeMargins(boolean onlyExpand) {
        super.computeMargins(onlyExpand);
        bestFit();
    }

    /** Copy the contents of the status bar to the clipboard. */
    public void copyPositionToClipboard() {
        if (mprin == null) {
            showError
                ("The mouse is outside the diagram.");
        }
        moveMouse(mprin);
        setMouseStuck(true);
        copyToClipboard(HtmlToText.htmlToText(principalToPrettyString(mprin)), false);
    }

    public void copyAllTextToClipboard() {
        StringBuilder res = new StringBuilder();
        for (String s: getAllText()) {
            res.append(s);
            res.append("\n");
        }
        copyToClipboard(res.toString(), false);
    }

    public void copyAllFormulasToClipboard() {
        StringBuilder res = new StringBuilder();
        for (String s: getAllFormulas()) {
            res.append(s);
            res.append("\n");
        }
        copyToClipboard(res.toString(), false);
    }

    public void addTieLine() {
        tieLineCorners = new ArrayList<>();
        tieLineDialog.getLabel().setText(tieLineStepStrings[0]);
        tieLineDialog.pack();
        tieLineDialog.setVisible(true);
        tieLineDialog.toFront();
    }

    /** Move the mouse to the nearest key point.

        @param select If true, exclude unselectable points, and select
        the point that is returned.

        @param key A String describing the key combination used to
        invoke this function; this parameter is used to display an
        error to the user if the mouse is off-screen.
    */
    public void seekNearestPoint(boolean select, String key) {
        if (mouseMissing(key)) {
            return;
        }
        if (mouseIsStuck && principalFocus == null) {
            setMouseStuck(false);
        }

        Point2D.Double point;

        if (!select) {
            point = nearestPoint(true);
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
                } else {
                    for (int i = 0; i < points.size() - 1; ++i) {
                        if (selection.equals(points.get(i))) {
                            sel = points.get(i+1);
                            break;
                        }
                    }
                }
            }

            setSelection(sel);

            if (sel instanceof VertexHandle) {
                VertexHandle hand = getVertexHandle();
                // You're more likely to want to add a point before
                // vertex #0 than to insert a point between vertex #0
                // and vertex #1.
                insertBeforeSelection = (hand.vertexNo == 0)
                    && (hand.getItem().size() >= 2);
            }

            point = sel.getLocation();
        }

        moveMouse(point);
        setMouseStuck(true);
    }

    /** Move the mouse to the nearest integer coordinates.

        @param key A String describing the key combination used to
        invoke this function; this parameter is used to display an
        error to the user if the mouse is off-screen.
    */
    public void seekNearestGridPoint(String key) {
        if (mouseMissing(key)) {
            return;
        }
        setMouseStuck(false);
        moveMouse(nearestGridPoint(mprin));
        setMouseStuck(true);
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
            {"Current " + axis.name + " value", "New " + axis.name + " value"};


        boolean isX = isXAxis(axis);
        if (!isX && !isYAxis(axis)) {
            throw new IllegalStateException("Only x and y units are adjustable");
        }

        Rectangle2D.Double principalBounds = getPrincipalBounds();

        Point2D.Double p1 = null;
        Point2D.Double p2 = null;

        CuspFigure path = getSelectedCuspFigure();
        if (path != null && path.size() == 2) {
            p1 = path.curve.getStart();
            p2 = path.curve.getEnd();
        } else {
            RulerDecoration rdec = getSelectedRuler();
            if (rdec != null) {
                BoundedParam2D p = rdec.getItem().getParameterization();
                p1 = p.getStart();
                p2 = p.getEnd();
            }
        }

        double v1 = isX ? principalBounds.x : principalBounds.y;
        double v2 = isX ? principalBounds.x + principalBounds.width
            : principalBounds.y + principalBounds.height;
        if (p1 != null) {
            if (isX) {
                v1 = p1.getX();
                v2 = p2.getX();
            } else {
                v1 = p1.getY();
                v2 = p2.getY();
            }
        }

        double[][] data = {{v1, v1}, {v2, v2}};

        NumberTableDialog dog = new NumberTableDialog
            (editFrame, data, null, columnNames,
             htmlify
             ("Enter two pairs of current and new values. For example, to convert "
              + "from degrees Celsius to degrees Kelvin, you might choose"
              + "<blockquote>(Current value: 0, New value: 273.15)</blockquote>"
              + "<blockquote>(Current value: 1, New value: 274.15)</blockquote>"
              + "<p>Fractions are allowed. "
              + "If you enter percentages, you must include the percent sign."));
        dog.setTitle("Scale " + axis.name + " units");
        dog.setPercentage(axis.isPercentage());
        double[][] output = dog.showModal();
        if (output == null) {
            return;
        }

        // Compute {m,b} such that y = mx + b. Note that here, y is
        // the new axis value and x is the old one -- this has nothing
        // to do with x-axis versus y-axis.

        double x1 = output[0][0];
        double x2 = output[1][0];
        double y1 = output[0][1];
        double y2 = output[1][1];

        if (x1 == x2 || y1 == y2) {
            showError("Please choose two different old values "
                      + "and two different new values.");
            return;
        }

        double m = (y2 - y1)/(x2 - x1);
        double b = y1 - m * x1;

        AffineTransform xform = isX
            ? new AffineTransform(m, 0.0, 0.0, 1.0, b, 0.0)
            : new AffineTransform(1.0, 0.0, 0.0, m, 0.0, b);

        invisiblyTransformPrincipalCoordinates(xform);
        resetPixelModeVisible();
        propagateChange();
    }

    public void scaleBoth() {
        BoundedParam2D b = getPrincipalParameterization(selection);
        double oldV = (b == null) ? 1 
            : b.length(0, 1e-9, 2000).value;
        if (oldV <= 0) {
            oldV = 1;
        }

        String[] columnNames = {"Current length", "New length"};
        double[][] data = {{oldV, oldV}};

        NumberTableDialog dog = new NumberTableDialog
            (editFrame, data, null, columnNames,
             htmlify
             ("Enter the current and new values. Both axes will be rescaled by "
              + "a factor of (new length)/(current length)."
              + "<p>Fractions are allowed. "
              + "If you enter percentages, you must include the percent sign."));
        dog.setTitle("Scale units");
        double[][] output = dog.showModal();
        if (output == null) {
            return;
        }

        oldV = output[0][0];
        double newV = output[0][1];

        if (oldV == 0 || newV == 0) {
            showError("Neither the old nor the new length may be zero.");
            return;
        }

        double m = newV / oldV;

        AffineTransform xform = AffineTransform.getScaleInstance(m, m);
        invisiblyTransformPrincipalCoordinates(xform);
        propagateChange();
    }

    public void setDiagramComponent(Side side) {
        String old = diagramComponents[side.ordinal()];
        FormulaDialog dog = getFormulaDialog();
        dog.setFormula((old == null) ? "" : old);
        dog.okButton.setText("Set " + side.toString().toLowerCase()
                             + " component");
        String str = dog.showModal(false);
        if (str == null) {
            return;
        }
        str = str.trim();

        try {
            setDiagramComponent(side, str.isEmpty() ? null : str);
            mathWindow.refresh();
        } catch (DuplicateComponentException x) {
            showError("Duplicate component '" + str + "'",
                      "Cannot change diagram component");
        }
    }


    /** Invoked from the EditFrame menu */
    public void addLabel() {
        if (principalToStandardPage == null) {
            return;
        }

        if (mprin == null) {
            showError
                ("Position the mouse where the label belongs, "
                 + "then press 't' to add the label.");
            return;
        }

        if (mouseIsStuckAtSelection() && getSelectedLabel() != null) {
            setMouseStuck(false);
        }

        LabelDialog dog = getLabelDialog();
        double fontSize = dog.getFontSize();
        dog.reset();
        dog.setFontSize(fontSize);
        finishAddLabel();
    }

    /** Invoked from the EditFrame menu */
    public void addIsotherm() {
        if (principalToStandardPage == null) {
            return;
        }

        if (mprin == null) {
            showError
                ("Position the mouse where the label belongs, "
                 + "then press 'i' to add the isotherm.");
            return;
        }

        if (mouseIsStuckAtSelection() && getSelectedLabel() != null) {
            setMouseStuck(false);
        }

        LabelDialog dog = getLabelDialog();
        double fontSize = dog.getFontSize();
        dog.reset();
        dog.setFontSize(fontSize);
        dog.setOpaqueLabel(true);
        seekNearestCurve(false, null);
        dog.setAngle(mathWindow.getAngle());
        finishAddLabel();
    }

    public void finishAddLabel() {
        double x = mprin.x;
        double y = mprin.y;

        LabelDialog dog = getLabelDialog();
        dog.setTitle("Add Label");
        AnchoredLabel newLabel = dog.showModal();
        if (newLabel == null || !check(newLabel)) {
            return;
        }

        newLabel.setAngle(pageToPrincipalAngle(newLabel.getAngle()));
        newLabel.setX(x);
        newLabel.setY(y);
        newLabel.setColor(color);
        LabelDecoration d = new LabelDecoration(new LabelInfo(newLabel));
        decorations.add(d);
        setSelection(new LabelHandle(d, LabelHandleType.ANCHOR));
        moveMouse(new Point2D.Double(x,y));
        setMouseStuck(true);
        propagateChange();
    }

    public boolean check(AnchoredLabel label) {
        if (label.getText().isEmpty()) {
            return false;
        }
        if (label.getScale() <= 0) {
            showError("Font size is not a positive number");
            return false;
        }
        return true;
    }

    public void editLabel(LabelInfo labelInfo) {
        AnchoredLabel label = labelInfo.label.clone();
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
        newLabel.setColor(label.getColor());
        newLabel.setBaselineXOffset(label.getBaselineXOffset());
        newLabel.setBaselineYOffset(label.getBaselineYOffset());

        labelInfo.setLabel(newLabel);
        moveMouse(label.getLocation());
        setMouseStuck(true);
        propagateChange();
    }

    /** Invoked from the EditFrame menu */
    public void setLineStyle(StandardStroke lineStyle) {
        this.lineStyle = lineStyle;
        if (selection != null) {
            selection.getDecoration().setLineStyle(lineStyle);
        }
    }

    /** Return a value that's small compared to the page size but
        still big enough to avoid getting swallowed by loss of
        precision. */
    double pagePrecision() {
        return 1e-10 * (pageBounds.width + pageBounds.height +
                        Math.abs(pageBounds.x) + Math.abs(pageBounds.y));
    }

    /** Return true if point pageP is inside pageR or nearly so. */
    boolean roughlyInside(Point2D pageP, Rectangle2D pageR) {
        if (pageR.contains(pageP)) {
            return true;
        }

        return Geom.distance(pageP, pageR) < pagePrecision();
    }

    /** Return null if principal coordinates point prin is not within
        the viewport or extremely close to it, or return the the
        point's location in scaled page coordinates if it is. */
    Point viewPosition(Point2D prin) {
        if (principalToStandardPage == null) {
            return null;
        }

        Point2D.Double pointd = principalToScaledPage(scale).transform(mprin);
        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();
        Point point = Geom.floorPoint(pointd);

        // If pointd is on the view's boundary, then bump it inside.
        // This avoids a potential infinite loop where the mouse is
        // placed precisely on the page border and the page border
        // corresponds with the view border, but because of round-off
        // error, the mouse position is determined to sit just
        // outside, so it's recomputed to put it just on the border,
        // and the cycle repeats.
        if (point.x < view.x && pointd.x + 1e-8 > view.x) {
            point.x = view.x;
        } else if (point.x >= view.x + view.width
                   && pointd.x - 1e-8 < view.x + view.width) {
            point.x = view.x + view.width - 1;
        }
        if (point.y < view.y && pointd.y + 1e-8 > view.y) {
            point.y = view.y;
        } else if (point.y >= view.y + view.height
                   && pointd.y - 1e-8 < view.y + view.height) {
            point.y = view.y + view.height - 1;
        }

        if (!view.contains(point)) {
            return null;
        }

        return point;
    }

    /** Move the mouse pointer so its position corresponds to the
        given location in principal coordinates. Return true if it was
        possible to go to that location. */
    boolean moveMouse(Point2D prin) {
        if (!allowRobotToMoveMouse) {
            return false;
        }
        mprin = new Point2D.Double(prin.getX(), prin.getY());
        mouseTravel = null;
        if (principalToStandardPage == null) {
            return false;
        }

        Point spoint = viewPosition(prin);

        if (spoint != null) {
            JScrollPane spane = editFrame.getScrollPane();
            Rectangle view = spane.getViewport().getViewRect();
            Point topCorner = spane.getLocationOnScreen();

            // For whatever reason (Java bug?), I need to add 1 to the x
            // and y coordinates if I want the output to satisfy

            // getEditPane().getMousePosition() == original mpos value.

            spoint.x += topCorner.x - view.x + 1;
            spoint.y += topCorner.y - view.y + 1;

            try {
                ++paintSuppressionRequestCnt;
                Robot robot = new Robot();
                robot.mouseMove(spoint.x, spoint.y);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            } finally {
                --paintSuppressionRequestCnt;
            }

            redraw();
        } else {
            Point2D.Double mousePage = principalToStandardPage
                .transform(mprin);
            if (roughlyInside(mousePage, pageBounds)) {
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
        centerPoint(mprin);
    }

    /** Place principal coordinates point prin in the center of the
        view, subject to the restriction that off-page space should be
        minimized within the view and mprin must be visible. */
    public void centerPoint(Point2D prin) {
        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();
        setViewportRelation(prin, new Point(view.width/2, view.height/2));
    }

    /** Move the mouse to the selection and center it. */
    public void centerSelection() {
        if (selection == null) {
            return;
        }
        mprin = selection.getLocation();
        centerMouse();
        setMouseStuck(true);
    }

    /** Adjust the viewport to place principal coordinates point prin
        as close to the given position within the viewport of the
        scroll pane as possible without bringing extra dead off-page
        space into view. */
    public void setViewportRelation(Point2D prin,
                                    Point viewportPoint) {
        ++paintSuppressionRequestCnt;
        Affine xform = principalToScaledPage(scale);
        Point panePoint = Geom.floorPoint(xform.transform(prin));
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

        // I haven't checked whether the double-call is still needed
        // since I made the scrollbars always-on.
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

    boolean mouseMissing(String key) {
        if (mprin == null) {
            showError("Place the mouse in the desired location and press " + key);
            return true;
        }
        return false;
    }

    /** @return the location in principal coordinates of the key
        point closest (by page distance) to mprin. */
    Point2D.Double nearestPoint(boolean includeSmoothingPoints) {
        if (mprin == null) {
            return null;
        }
        return nearestPoint(principalToStandardPage.transform(mprin),
                            includeSmoothingPoints);
    }

    /** @return the location in principal coordinates of the key point
        closest (by page distance) to pagePoint, where pagePoint is
        expressed in standard page coordinates. */
    Point2D.Double nearestPoint(Point2D pagePoint,
                                boolean includeSmoothingPoints) {
        if (pagePoint == null) {
            return null;
        }
        Point2D.Double nearPoint = null;
        Point2D.Double xpoint2 = new Point2D.Double();

        // Square of the minimum distance from mprin of all key points
        // examined so far, as measured in standard page coordinates.
        double minDistSq = 0;

        ArrayList<Point2D.Double> points = keyPoints(includeSmoothingPoints);
        if (selection != null && !mouseIsStuckAtSelection()) {
            // Add the point on (the curve closest to pagePoint) that
            // is closest to selection.

            // In this case, we don't really need the precision that
            // nearestCurve() provides -- all we need is a good guess
            // for what the closest curve is -- but whatever...

            // This feature XYZZY should also be available for auto-positioning.
            DecorationDistance nc = nearestCurve(pagePoint);
            if (nc != null) {
                BoundedParam2D b = getStandardPageParameterization(nc.decoration);
                if (b != null) {
                    Point2D.Double selPage = principalToStandardPage
                        .transform(selection.getLocation());
                    CurveDistanceRange cdr = b.distance(selPage, 1e-6, 1000);
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
            return new ArrayList<>();
        }
        if (principalFocus == null) {
            principalFocus = mprin;
        }

        return nearestHandles(principalFocus);
    }

    @Override public ArrayList<DecorationHandle> keyPointHandles
        (boolean includeSmoothingPoints) {
        ArrayList<DecorationHandle> res = super.keyPointHandles
            (includeSmoothingPoints);

        CurveDecoration cdec = getSelectedCurve();
        if (cdec != null) {
            // Diagram.keyPointHandles() omits interior control points
            // of smoothed arcs, but we want to include every control
            // point of the selected curve.
            for (DecorationHandle hand: cdec.getHandles()) {
                res.add(hand);
            }
        }

        if (selection != null) {
            Point2D.Double p1 = selection.getLocation();
            Point2D.Double p2 = secondarySelectionLocation();
            if (p2 != null) {
                // Add the doublings of segment p1p2.
                res.add(new NullDecorationHandle
                        (p1.getX() + (p1.getX() - p2.getX()),
                         p1.getY() + (p1.getY() - p2.getY())));
                res.add(new NullDecorationHandle
                        (p2.getX() + (p2.getX() - p1.getX()),
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
        curve.

        @param key A String describing the key combination used to
        invoke this function; this parameter is used to display an
        error to the user if the mouse is off-screen.
    */
    public void seekNearestCurve(boolean select, String key) {
        if (mouseMissing(key)) {
            return;
        }
        if (mouseIsStuck) {
            setMouseStuck(false);
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
            setSelection(handle);
            if (selection instanceof VertexHandle) {
                CuspFigure path = getSelectedCuspFigure();
                insertBeforeSelection = closerToNext
                    || (path.size() >= 2 && t == 0);
            }
        }
        moveMouse(standardPageToPrincipal.transform(minDist.point));
        setMouseStuck(true);
        showTangent(dec, t);
    }

    /** Toggle the closed/open status of the currently selected
        curve. */
    public void toggleCurveClosure() {
        VertexHandle hand = getVertexHandle();
        if (hand == null) {
            return;
        }
        try {
            toggleCurveClosure(hand.getItem());
        } catch (IllegalArgumentException x) {
            showError(x.toString());
        }
    }

    @Override public void setOriginalFilename(String filename) {
        boolean isNew = (filename != originalFilename)
            && (filename == null || !filename.equals(originalFilename));
        super.setOriginalFilename(filename);
        if (isNew) {
            originalImage = null;
            triedToLoadOriginalImage = false;
            revalidateZoomFrame();
        }
    }

    /** @param askExit If true and the file associations were
        successfully installed, then ask the user if they want to exit
        afterwards. */
    void setFileAssociations(boolean askExit) {
        setFileAssociations
            (askExit, "application/x-pededitor", new String[] { "ped" });
    }

    void setFileAssociations(boolean askExit, String mime, String[] exts) {
        try {
            IntegrationService is
                = (IntegrationService) ServiceManager.lookup("javax.jnlp.IntegrationService");
            if (askExit && is.requestAssociation(mime, exts)) {
                if (JOptionPane.showOptionDialog
                    (editFrame,
                     htmlify(fallbackTitle() + " has been installed. At any time, you can " +
                             "uninstall, run, or create a shortcut for it by opening the Java Control " +
                             "Panel's General tab and and pressing the \"View...\" " +
                             "button."),
                     "Installation successful",
                     JOptionPane.YES_NO_OPTION,
                     JOptionPane.QUESTION_MESSAGE,
                     null,
                     new Object[] { "Run Now", "Finished" },
                     null) != JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        } catch (UnavailableServiceException x) {
            // OK, ignore this error.
        }
    }

    public static void main(BasicEditorCreator ec, String[] args) {
        if (args.length == 1 && args[0].charAt(0) == '-') {
            printHelp();
            System.exit(2);
        }

        waitDialog = new WaitDialog
            (new BasicEditorArgsRunnable(ec, args),
             "Loading PED Editor...");
        waitDialog.setTitle("PED Editor");
        waitDialog.pack();
        waitDialog.setVisible(true);
    }

    /** Launch the application. */
    public static void main(String[] args) {
        main(new BasicEditorCreator(), args);
    }

    @Override public void cropPerformed(CropEvent e) {
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                diagramType = e.getDiagramType();
                newDiagram
                    (e.filename, Geom.toPoint2DDoubles(e.getVertices()));
                initializeGUI();
            }
        propagateChange();
    }

    void clearFileList() {
        filesList = null;
        editFrame.mnNextFile.setVisible(false);
        fileNo = -1;
    }

    public BasicEditor createNew() {
        return new BasicEditor();
    }

    public BasicEditor findEmptyEditor() {
        if (haveDiagram()) {
            return createNew();
        } else {
            return this;
        }
    }

    /** Start on a blank new diagram.

        @param overwrite This only affects behavior when a diagram is
        already open in this window. If overwrite is true, the new
        diagram will replace the old one; if false, the new diagram
        will open in a new window.
    */
    public void newDiagram(boolean overwrite) {
        if (overwrite) {
            if (!verifyCloseDiagram()) {
                return;
            }
        } else {
            if (haveDiagram()) {
                BasicEditor e = createNew();
                e.newDiagram(true);
                if (!e.isClosed()) {
                    e.initializeGUI();
                }
                return;
            }
            clearFileList();
            majorSaveNeeded = false;
            setSaveNeeded(false);
        }

        try (UpdateSuppressor us = new UpdateSuppressor()) {
                DiagramType temp = (new DiagramDialog(null)).showModal();
                if (temp == null) {
                    closeIfNotUsed();
                    return;
                }

                diagramType = temp;
                newDiagram(null, null);
            }

        closeIfNotUsed();
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

    NumberColumnDialog trapezoidDialog(double[] values) {
        String[] labels = {
            "Minimum amount of top component",
            "Maximum amount of top component",
            "Minimum amount of right component",
            "Maximum amount of right component" };
        return new NumberColumnDialog
            (editFrame, values, labels,
             htmlify
             ("Enter the range of compositions covered by this diagram. "
              + "Fractions are allowed. For percentages, do not omit the "
              +  "percent sign."));
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
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                clear();
                setOriginalFilename(originalFilename);
                revalidateZoomFrame();

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
                        double defaultHeight = !tracing ? 0.45
                            : (1.0
                               - (vertices[1].distance(vertices[2]) / 
                                  vertices[0].distance(vertices[3])));
                        double[] defaultValues = { 0, defaultHeight, 0, 1 };
                        NumberColumnDialog dog = trapezoidDialog(defaultValues);
                        dog.setPercentage(true);

                        double minTop, maxTop, minRight, maxRight;
                        boolean isPercent = true;

                        while (true) {
                            double[] values = null;
                            try {
                                values = dog.showModalColumn();
                            } catch (NumberFormatException x) {
                                showError(x.getMessage());
                                continue;
                            }
                            if (values == null) {
                                values = defaultValues;
                            } else {
                                isPercent = dog.havePercentage();
                            }

                            boolean retry = false;
                            for (int i = 0; i < values.length; ++i) {
                                double d= values[i];
                                if (d < 0 || d > 1) {
                                    showError
                                        ("Component density '" + dog.getTextAt(i) + "' "
                                         + "(equal to "
                                         + String.format("%.2f%%", d * 100) + ") is "
                                         + "not between 0% and 100%");
                                    retry = true;
                                    break;
                                }
                            }
                            if (retry) {
                                continue;
                            }

                            minTop = values[0];
                            maxTop = values[1];
                            minRight = values[2];
                            maxRight = values[3];

                            if (minTop >= maxTop) {
                                maxLessThanMinError
                                    ("the top component", minTop, maxTop);
                                continue;
                            }
                            if (minRight >= maxRight) {
                                maxLessThanMinError
                                    ("the right component", minRight, maxRight);
                                continue;
                            }
                            if (minTop + maxRight > 1 + 1e-12) {
                                showError
                                    ("The sum of the min top component and the "
                                     + "max right component ("
                                     + String.format("%.2f%%", minTop * 100) + " + "
                                     + String.format("%.2f%%", maxRight * 100)
                                     + ") cannot exceed 100%.");
                                continue;
                            }
                            if (maxTop - minTop >= maxRight - minRight - 1e-12) {
                                showError
                                    ("The difference between the maximum and "
                                     + "minimum top component concentrations "
                                     + " must be less than the difference between "
                                     + "the maximum and minimum right component "
                                     + "concentrations.");
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
                            { new Point2D.Double(minRight, minTop),
                              new Point2D.Double(minRight, maxTop),
                              new Point2D.Double(maxRight - (maxTop - minTop), maxTop),
                              new Point2D.Double(maxRight, minTop) };

                        if (tracing) {
                            originalToPrincipal = new QuadToQuad(vertices, outputVertices);
                        }

                        double width = maxRight - minRight;
                        double height = TriangleTransform.UNIT_TRIANGLE_HEIGHT
                            * (maxTop - minTop);
                        // The height of the triangle formed by
                        // extending the nonparallel legs of the
                        // trapezoid until they meet.
                        double triangleHt = TriangleTransform.UNIT_TRIANGLE_HEIGHT
                            * width;
                        r = new Rescale(width, 0.0, maxDiagramWidth,
                                        height, 0.0, maxDiagramHeight);

                        double rx = r.width;
                        double bottom = r.height;

                        Point2D.Double[] trianglePagePositions =
                            { new Point2D.Double(0.0, bottom),
                              new Point2D.Double(rx/2, bottom - triangleHt * r.t),
                              new Point2D.Double(rx, bottom) };
                        principalToStandardPage = new TriangleTransform
                            (new Point2D.Double[]
                                { new Point2D.Double(minRight, minTop),
                                  new Point2D.Double(minRight,
                                                     maxRight - minRight + minTop),
                                  new Point2D.Double(maxRight, minTop) },
                             trianglePagePositions);

                        LinearRuler rule = ternaryBottomRuler
                            (minRight, maxRight, minTop);
                        rule.startArrow = rule.endArrow = false;
                        add(rule);
                        rule = ternaryLeftRuler(minTop, maxTop, minRight);
                        rule.startArrow = false;
                        add(rule);
                        rule = ternaryRightRuler(minTop, maxTop, maxRight);
                        rule.startArrow = false;
                        add(rule);

                        for (Axis axis: getAxes()) {
                            setPercentageDisplay(axis, isPercent);
                        }

                        break;
                    }

                case BINARY:
                case OTHER:
                    {
                        CartesianDialog dog = new CartesianDialog(editFrame);
                        boolean other = (diagramType == DiagramType.OTHER);
                        dog.setUniformScale(other);
                        dog.setPixelModeVisible(other);

                        Rectangle2D.Double domain;
                        if (other && tracing) {
                            // For free-form diagrams, let the
                            // dimensions of the input determine the
                            // default dimensions of the output.
                            double width1 = vertices[1].distance(vertices[2]);
                            double width2 = vertices[0].distance(vertices[3]);
                            double height1 = vertices[0].distance(vertices[1]);
                            double height2 = vertices[2].distance(vertices[3]);
                            double wOverH = (width1 + width2) / (height1 + height2);
                            domain = new Rectangle2D.Double(0, 0, wOverH, 1);
                            dog.setUniformScale(true);
                        } else {
                            domain = new Rectangle2D.Double(0, 0, 1, 1);
                            if (!other) {
                                dog.setAspectRatio(0.9);
                            }
                        }
                        
                        dog.setRectangle(domain);
                        domain = dog.showModalRectangle();

                        if (tracing) {
                            // Transform the input quadrilateral into a rectangle
                            QuadToRect q = new QuadToRect();
                            q.setVertices(vertices);
                            q.setRectangle(domain);
                            originalToPrincipal = q;
                        }

                        if (dog.isUniformScale()) {
                            // Scale the domain uniformly to just fit
                            // inside a unit square. (This means the
                            // page margins will be sane relative to
                            // the size of the diagram.)
                            r = new Rescale(Math.abs(domain.width), 0.0, 1.0,
                                            Math.abs(domain.height), 0.0, 1.0);
                        } else {
                            // I just need to get the width and height
                            // correct.
                            r = new Rescale(dog.getAspectRatio(), 1.0, 1.0);
                        }
                        principalToStandardPage = new RectangleTransform
                            (domain,
                             new Rectangle2D.Double
                             (0, r.height, r.width, -r.height));

                        if (other) {
                            if (dog.isPixelMode()) {
                                setPixelModeComplex(true);
                                leftMargin = rightMargin = topMargin = bottomMargin = 0.0;
                            } else {
                                leftMargin = rightMargin = topMargin = bottomMargin = 0.05;
                            }
                        } else {
                            double bottom = domain.getY();
                            double top = bottom + domain.getHeight();
                            double left = domain.getX();
                            double right = left + domain.getWidth();
                            add(binaryBottomRuler(left, right, bottom));
                            add(binaryTopRuler(left, right, top));
                            add(binaryLeftRuler(bottom, top, left));
                            add(binaryRightRuler(bottom, top, right));
                        }

                        setPercentageDisplay(getXAxis(), dog.xIsPercentage());
                        setPercentageDisplay(getYAxis(), dog.yIsPercentage());

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

                        DimensionsDialog dog = new DimensionsDialog
                            (editFrame, pageMaxes,
                             new String[] { sideNames[ov1], sideNames[ov2] });
                        dog.setPercentage(true);
                        dog.setTitle("Select Triangle Side Lengths");
                        boolean isPercent = true;

                        while (true) {
                            try {
                                double[] pageMaxes2 = dog.showModalColumn();
                                if (pageMaxes2 != null) {
                                    pageMaxes = pageMaxes2;
                                    isPercent = dog.havePercentage();
                                }
                                break;
                            } catch (NumberFormatException e) {
                                showError(e.getMessage(), "Invalid number format.");
                                continue;
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
                            = Geom.deepCopy(principalTrianglePoints);

                        switch (diagramType) {
                        case TERNARY_LEFT:
                            trianglePoints[TOP_VERTEX] = new Point2D.Double(0, leftLength);
                            trianglePoints[RIGHT_VERTEX]
                                = new Point2D.Double(bottomLength, 0);
                            add(ternaryBottomRuler(0.0, bottomLength));
                            add(ternaryLeftRuler(0.0, leftLength));
                            break;

                        case TERNARY_TOP:
                            trianglePoints[LEFT_VERTEX]
                                = new Point2D.Double(0, 1.0 - leftLength);
                            trianglePoints[RIGHT_VERTEX]
                                = new Point2D.Double(rightLength, 1.0 - rightLength);
                            add(ternaryLeftRuler(1 - leftLength, 1.0));
                            add(ternaryRightRuler(1 - rightLength, 1.0));
                            break;

                        case TERNARY_RIGHT:
                            trianglePoints[LEFT_VERTEX]
                                = new Point2D.Double(1.0 - bottomLength, 0.0);
                            trianglePoints[TOP_VERTEX]
                                = new Point2D.Double(1.0 - rightLength, rightLength);
                            add(ternaryBottomRuler(1.0 - bottomLength, 1.0));
                            add(ternaryRightRuler(0.0, rightLength));
                            break;

                        default:
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

                        Rectangle2D.Double bounds = Geom.bounds(xformed);
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

                        for (Axis axis: getAxes()) {
                            setPercentageDisplay(axis, isPercent);
                        }

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

                        add(ternaryBottomRuler(0.0, 1.0));
                        add(ternaryLeftRuler(0.0, 1.0));
                        add(ternaryRightRuler(0.0, 1.0));
                        break;
                    }
                }

                pageBounds = new Rectangle2D.Double
                    (-leftMargin, -topMargin, r.width + leftMargin + rightMargin,
                     r.height + topMargin + bottomMargin);

                if (diagramType != DiagramType.OTHER) {
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

    private void maxLessThanMinError(String component, double min, double max) {
        showError("<html><div width=\"200 px\">"
                  + "The maximum amount of " + component + ", "
                  + String.format("%.2f%%", max * 100) + ", "
                  + "does not exceed the minimum amount "
                  + String.format("%.2f%%", min * 100) + ".");
    }

    protected double rulerFontSize() {
        return normalRulerFontSize() * lineWidth / STANDARD_LINE_WIDTH;
    }

    public double currentFontSize() {
        return getLabelDialog().getFontSize();
    }

    /** Return true unless something about the setup of this diagram
        makes it unsuitable for use with pixel mode. */
    boolean maybePixelMode() {
        if (isPixelMode())
            return true;
        if (isTernary() || !isFixedAspect())
            return false;
        LinearAxis[] axes =
            { LinearAxis.createXAxis(null), LinearAxis.createYAxis(null) };
        for (LinearAxis axis: axes) {
            long len = Math.round(length(axis));
            if (len < 2 || len > 256)
                return false;
        }
        return true;
    }

    void resetPixelModeVisible() {
        editFrame.pixelMode.setVisible(maybePixelMode());
    }
        
    @Override protected void initializeDiagram() {
        super.initializeDiagram();
        editFrame.setAspectRatio.setEnabled(!isTernary());
        editFrame.setTopComponent.setEnabled(isTernary());
        editFrame.scaleBoth.setEnabled(!isTernary());
        resetPixelModeVisible();
        bestFit();
    }

    protected void initializeGUI() {
        // Force the editor frame image to be initialized.

        if (!editorIsPacked) {
            editFrame.pack();
            editorIsPacked = true;
            int otherEditorCnt = getOpenEditorCnt();
            editFrame.setLocation(15 * otherEditorCnt, 15 * otherEditorCnt);
            openEditors.add(this);
        }
        Rectangle rect = editFrame.getBounds();
        revalidateZoomFrame();

        if (tracingImage()) {
            Rectangle zrect = zoomFrame.getBounds();
            Rectangle vrect = mathWindow.getBounds();
            mathWindow.setLocation(zrect.x, zrect.y + zrect.height - vrect.height);
        } else {
            mathWindow.setLocation(rect.x + rect.width, rect.y);
        }
        mathWindow.refresh();
        editFrame.setMathWindowVisible(false);
        editFrame.setStatus("");
        editFrame.setVisible(true);
        setColor(color);
        bestFit();
    }

    @JsonIgnore public boolean isSaveNeeded() {
        return saveNeeded || majorSaveNeeded;
    }
        
    /** Give the user an opportunity to save the old diagram or to
        change their mind before closing a diagram.

        @return false if the user changes their mind and the diagram
        should not be closed. */
    boolean verifyCloseDiagram(Object[] options) {
        if (!isSaveNeeded()) {
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
            (new Object[] {"Save and close", "Close without saving", "Cancel"});
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
                     "Enter the width-to-height ratio for the diagram.\n"
                     + "(Most diagrams in the database uses a ratio of 80%.)",
                     ContinuedFraction.toString(oldValue, true));
                if (aspectRatioStr == null) {
                    return;
                }

                aspectRatio = ContinuedFraction.parseDouble(aspectRatioStr);
                if (aspectRatio <= 0) {
                    showError("Enter a positive number.");
                } else if (aspectRatio >= 20) {
                    showError("That's a very big ratio. Maybe you need to include "
                              + "a percent sign in the value?");
                } else {
                    break; // OK value
                }
            } catch (NumberFormatException e) {
                showError("Invalid number format.");
            }
        }

        super.setAspectRatio(aspectRatio);
        bestFit();
        scaledOriginalImages = null;
    }

    void scaleEditPane() {
        getEditPane().setPreferredSize(scaledPageBounds(scale).getSize());
        getEditPane().revalidate();
        redraw();
    }


    /** Invoked from the EditFrame menu */
    public void cropToSelection() {
        Rectangle2D.Double sbounds = getSelectionBounds();
        if (sbounds == null) {
            return;
        }
        setPageBounds(sbounds);
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
                showError("Invalid number format.");
            }
        }

        setMargin(side, margin);
    }

    @Override public void openDiagram(File file) throws IOException {
        super.openDiagram(file);
        initializeGUI();
    }

    @Override public void openDiagram(String jsonString) throws IOException {
        super.openDiagram(jsonString);
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
            showError("File load error: " + e);
            return;
        }
    }

    @Override public void setPageBounds(Rectangle2D rect) {
        super.setPageBounds(rect);
        scaleEditPane();
        scaledOriginalImages = null;
    }

    @Override void cannibalize(Diagram other) {
        removeAllVariables();
        removeAllTags();
        super.cannibalize(other);
        for (LinearAxis axis: axes) {
            editFrame.addVariable((String) axis.name);
        }
        if (isPixelMode()) {
            setPixelModeComplex(true);
        }
        clearSelection();
    }

    @JsonIgnore public BufferedImage getOriginalImage() {
        if (originalImage != null) {
            return originalImage;
        }

        String ofn = getAbsoluteOriginalFilename();

        if (ofn == null || triedToLoadOriginalImage || !isEditable()) {
            return null;
        }

        triedToLoadOriginalImage = true;

        File originalFile = new File(ofn);
        boolean changedOriginal = false;
        do {
            try {
                if (Files.notExists(originalFile.toPath())) {
                    throw new FileNotFoundException(originalFile.toString());
                }
                originalImage = ImageIO.read(originalFile);
                if (originalImage == null) {
                    throw new IOException(filename + ": unknown image format");
                }
                if (changedOriginal) {
                    setOriginalFilename(originalFile.getName());
                }
                return originalImage;
            } catch (IOException e) {
                if (JOptionPane.showOptionDialog
                    (editFrame,
                     "Original image unavailable: '" + ofn + "':\n" +  e.toString() + "\n"
                     + "You may hide the original image or\ntry to locate a "
                     + "readable copy of this file.",
                     "Image load error",
                     JOptionPane.YES_NO_OPTION,
                     JOptionPane.WARNING_MESSAGE,
                     null,
                     new Object[] { "Hide original image", "Locate original image" },
                     null) == JOptionPane.NO_OPTION) {
                    originalFile = openImageFileDialog(editFrame);
                    if (originalFile == null) {
                        return null;
                    }
                    changedOriginal = true;
                } else {
                    return null;
                }
            }
        } while (true);
    }

    /** If the zoom frame is not needed, then then make sure it's null
        or invisible. Otherwise, make sure the zoom frame is non-null,
        initialized, visible, and shows the correct image. */
    void revalidateZoomFrame() {
        BufferedImage im = getOriginalImage();
        if (im != null) {
            editFrame.setBackgroundTypeEnabled(true);
            initializeZoomFrame();
            zoomFrame.setImage(im);
            initializeCrosshairs();
            zoomFrame.getImageZoomPane().crosshairs = crosshairs;
            editFrame.mnBackgroundImage.setEnabled(true);
            zoomFrame.setTitle
                ("Zoom " + getOriginalFilename());
            zoomFrame.pack();
            zoomFrame.setVisible(true);
        } else {
            if (zoomFrame != null) {
                zoomFrame.setVisible(false);
            }
            if (getOriginalFilename() == null) {
                editFrame.mnBackgroundImage.setEnabled(false);
            } else {
                editFrame.setBackgroundTypeEnabled(false);
                editFrame.mnBackgroundImage.setEnabled(true);
            }
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

    String[] pedFileExtensions() {
        return new String[] {"ped"};
    }

    public File[] openPEDFilesDialog(Component parent) {
        String what = String.join("/", Arrays.asList(pedFileExtensions())).toUpperCase();
        return openFilesDialog
            (parent, "Open " + what + "  File", what + " files", pedFileExtensions());
    }

    /** Return the default directory to save to and load from. */
    public static String getCurrentDirectory() {
        return Preferences.userNodeForPackage(BasicEditor.class)
            .get(PREF_DIR,  null);
    }

    /** Set the default directory to save to and load from. */
    public static void setCurrentDirectory(String dir) {
        Preferences.userNodeForPackage(BasicEditor.class)
            .put(PREF_DIR,  dir);
    }

    public File[] openPEDOrImageFilesDialog(Component parent) {
        String[] pedExts = pedFileExtensions();
        String[] imageExts = ImageIO.getReaderFileSuffixes();
        String[] allExts = concat(pedExts, imageExts);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open PED or image file");
        chooser.setMultiSelectionEnabled(true);
        String dir = getCurrentDirectory();
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
           File[] files = chooser.getSelectedFiles();
           setCurrentDirectory(files[0].getParent());
           return files;
       } else {
           return null;
       }
    }

    public static File[] openFilesDialog(Component parent, String title,
                                      String filterName, String[] suffixes) {
        Preferences prefs = Preferences.userNodeForPackage(CropFrame.class);
        String dir = prefs.get(PREF_DIR,  null);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setMultiSelectionEnabled(true);
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
        chooser.setFileFilter
            (new FileNameExtensionFilter(filterName, suffixes));
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
           File[] files = chooser.getSelectedFiles();
           if (files != null) {
               setCurrentDirectory(files[0].getParent());
           }
           return files;
        } else {
            return null;
        }
    }


    public static File[] openImageFilesDialog(Component parent) {
        return openFilesDialog
            (parent, "Open Image File", "Image files",
             ImageIO.getReaderFileSuffixes());
    }

    public static File openImageFileDialog(Component parent) {
        File[] res = openImageFilesDialog(parent);
        if (res != null && res.length == 1) {
            return res[0];
        }
        return null;
    }

    public void open() {
        showOpenDialog(editFrame);
    }

    public void showOpenDialog(Component parent) {
        File[] filesList = isEditable() ? openPEDOrImageFilesDialog(parent)
            : openPEDFilesDialog(parent);
        showOpenDialog(parent, filesList);
    }

    public void showOpenDialog(Component parent, File[] files) {
        if (files == null || files.length == 0) {
            return;
        }

        if (haveDiagram()) {
            BasicEditor e = createNew();
            e.initializeGUI();
            e.showOpenDialog(parent, files);
            return;
        }

        filesList = files;
        fileNo = 0;
        if (files.length > 1) {
            editFrame.mnNextFile.setVisible(true);
        }
        showOpenDialog(parent, files[0]);
    }

    /** Return true if the file extension is that of a PED file (or
        possibly a PED viewer file) */
    boolean isPED(File file) {
        if (file == null) {
            return false;
        }
        String ext = getExtension(file.getName());
        for (String ext1: pedFileExtensions()) {
            if (ext1.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }
        
    public void showOpenDialog(Component parent, File file) {
        if (file == null) {
            closeIfNotUsed();
            return;
        }

        if (haveDiagram()) {
            BasicEditor e = createNew();
            e.initializeGUI();
            e.showOpenDialog(e.editFrame, file);
            return;
        }
        boolean ped = isPED(file);
        String ext = getExtension(file.getName());
        String[] exts = pedFileExtensions();
        String mainPEDExt = exts[0];
        if (ext == null && Files.notExists(file.toPath())) {
            ped = true;
            file = new File(file.getAbsolutePath() + "." + mainPEDExt);
        }

        try {
            if (isEditable() && !ped) {
                // This had better be an image file.
                cropFrame.setFilename(file.getAbsolutePath());
                if (cropFrame.getDiagramType() == null) {
                    closeIfNotUsed();
                    return;
                }
                cropFrame.pack();
                editFrame.setStatus("");
                clear();
                cropFrame.refresh();
                cropFrame.setVisible(true);
            } else {
                if (ped) {
                    openDiagram(file);
                } else {
                    StringBuilder buf = new StringBuilder
                        ("Unrecognized file extension (expected ");
                    for (int i = 0; i < exts.length; ++i) {
                        if (i > 0) {
                            buf.append((i == (exts.length - 1))
                                       ? " or " : ", ");
                        }
                        buf.append("'.");
                        buf.append(exts[i]);
                        buf.append("'");
                    }
                    buf.append(")");
                    showError(parent, buf.toString(), "File load error");
                    closeIfNotUsed();
                }
            }
        } catch (IOException x) {
            showError(parent, "Could not load file: " + x,
                      "File load error");
            closeIfNotUsed();
        }
    }

    void showError(String mess, String title) {
        showError(editFrame, mess, title);
    }

    void showError(String mess) {
        showError(editFrame, mess, "Cannot perform operation");
    }

    static String htmlify(String mess) {
        return mess.startsWith("<html>") ? mess
            : ("<html><div width=\"250 px\"><p>" + mess);
    }

    static void showError(Component parent, String mess, String title) {
        JOptionPane.showMessageDialog
            (parent, htmlify(mess), title, JOptionPane.ERROR_MESSAGE);
    }

    public void openImage(String filename) {
        String title = (filename == null) ? "PED Editor" : filename;
        editFrame.setTitle(title);

        if (filename == null) {
            cropFrame.showOpenDialog();
        } else {
            try {
                cropFrame.setFilename(filename);
            } catch (IOException e) {
                showError("Could not load file '" + filename + "': " + e);
            }
        }

        cropFrame.pack();
        cropFrame.setVisible(true);
    }

    /** @return A File corresponding to getFilename() but with the
        extension replaced by ".ext". */
    public File defaultFile(String ext) {
        String s = getFilename();
        return (s == null) ? null : new File(removeExtension(s) + "." + ext);
    }

    /** @return a File if the user selected one, or null otherwise.

        @param ext the extension to use with this file ("pdf" for
        example). */
    public File showSaveDialog(String ext) {
        String dir = getCurrentDirectory();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save as " + ext.toUpperCase());
        File initial = defaultFile(ext);
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
        if (initial != null) {
            chooser.setSelectedFile(initial);
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
        return (extensionIndex <= lastSeparatorIndex + 1) ? null
            : s.substring(extensionIndex + 1).toLowerCase();
    }

    /** If the base filename contains a dot, then remove the last dot
        and everything after it. Otherwise, return the entire string.
        Modified from coobird's suggestion on Stack Overflow. */
    public static String removeExtension(String s) {
        String separator = System.getProperty("file.separator");
        int lastSeparatorIndex = s.lastIndexOf(separator);
        int extensionIndex = s.lastIndexOf(".");
        return (extensionIndex <= lastSeparatorIndex + 1) ? s
            : s.substring(0, extensionIndex);
    }

    void nothingToSave() {
        showError("You must create or load a diagram before "
                  + "you can save it.");
    }

    /** Invoked from the EditFrame menu */
    public void saveAsImage(String ext) {
        if (!haveDiagram()) {
            nothingToSave();
            return;
        }
        if (imageDimensionDialog == null) {
            imageDimensionDialog = new ImageDimensionDialog(editFrame);
            if (isPixelMode()) {
                imageDimensionDialog.setDimension
                    (new Dimension
                     ((int) Math.max(1, length(getXAxis())),
                      (int) Math.max(1, length(getYAxis()))));
            }
        }
        ImageDimensionDialog dog = imageDimensionDialog;
        dog.setShowOriginalImageVisible(tracingImage());
        boolean maybeTransparent = ext.equalsIgnoreCase("GIF")
            || ext.equalsIgnoreCase("PNG");
        dog.setTransparentVisible(maybeTransparent);

        Dimension size;
        try {
            size = dog.showModalDimension();
            if (size == null) {
                return;
            }
        } catch (NumberFormatException x) {
            showError(x.toString());
            return;
        }
        
        File file = showSaveDialog(ext);
        if (file == null || !verifyOverwriteFile(file)) {
            return;
        }

        try {
            saveAsImage(file, ext, size.width, size.height,
                        dog.isShowOriginalImage(),
                        maybeTransparent && dog.isTransparent());
        } catch (IOException x) {
            showError("File error: " + x);
        } catch (OutOfMemoryError x) {
            showError("Out of memory. You may either re-run Java with a "
                      + "larger heap size or try saving as a smaller image.");
        }
    }

    public void saveAsImage(File file, String format, int width, int height,
                            boolean showOriginalImage,
                            boolean transparent) throws IOException {
        if (!showOriginalImage) {
            saveAsImage(file, format, width, height, transparent);
            return;
        }
        BufferedImage im = transformOriginalImage(new Dimension(width, height));
        paintDiagram((Graphics2D) im.getGraphics(),
                     bestFitScale(new Dimension(width, height)), null);
        ImageIO.write(im, format, file);
    }
 
    public boolean saveAsPED() {
        if (!haveDiagram()) {
            nothingToSave();
            return false;
        }
        File file = showSaveDialog("ped");
        if (file == null || !verifyOverwriteFile(file)) {
            return false;
        }
        return saveAsPEDGUI(file.toPath());
    }

    /** Like saveAsPED(), but handle both exceptions and success
        internally with warning or information dialogs. */
    public boolean saveAsPEDGUI(Path file) {
        try {
            boolean saved = saveAsPED(file);
            if (saved) {
                JOptionPane.showMessageDialog
                    (editFrame, "Saved '" + getFilename() + "'.");
            }
            return saved;
        } catch (IOException e) {
            showError("File save error: " + e);
            return false;
        }
    }

    @Override public boolean saveAsPED(Path file) throws IOException {
        if (!haveDiagram()) {
            nothingToSave();
            return false;
        }
        if (super.saveAsPED(file)) {
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
            majorSaveNeeded = false;
            return true;
        } else {
            return false;
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
        PrintRequestAttributeSet aset 
            = new HashPrintRequestAttributeSet();
        /* Disabled at Will's request */
        /* aset.add
            ((pageBounds.width > pageBounds.height * 1.12)
             ? OrientationRequested.LANDSCAPE
             : OrientationRequested.PORTRAIT); */
        if (job.printDialog(aset)) {
            try {
                print(job, aset);
                JOptionPane.showMessageDialog
                    (editFrame, "Print job submitted.");
            } catch (PrinterException e) {
                showError(e.toString());
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

        if (mathWindow != null) {
            // Add the line at the angle given in mathWindow.
            double theta = mathWindow.getAngle();
            Point2D.Double p = new Point2D.Double(Math.cos(theta), Math.sin(theta));
            standardPageToPrincipal.deltaTransform(p, p);
            vectors.add(p);
        }

        return nearestGridLine(segment, vectors);
    }

    public DecorationHandle secondarySelection() {
        VertexHandle vh = getVertexHandle();
        if (vh != null) {
            CuspFigure path = vh.getItem();
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
            return new RulerHandle(rh.decoration, 1 - rh.handle);
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

    public Point2D.Double getAutoPosition(AutoPositionHolder ap) {
        DecorationHandle h = getAutoPositionHandle(ap);
        return (h == null) ? null : h.getLocation();
    }
        
    /** Return a DecorationHandle for the point in principal
        coordinates that auto-positioning would move the mouse to -- a
        nearby key point if possible, or a point on a nearby curve
        otherwise, or where the mouse already is otherwise, or null if
        the mouse is outside the panel containing the diagram. The
        DecorationHandle will be a normal DecorationHandle if it is a
        key point with exactly one decoration handle at that location;
        it will be a NullParameterizableHandle if it is a random
        location on a nearby curve; and it will be a
        NullDecorationHandle if it is a key point associated with zero
        or two or more decoration handles, or if nothing special was
        found nearby.

        @param ap If not null, ap.position will be set to
        AutoPositionType.NONE, AutoPositionType.CURVE, or
        AutoPositionType.POINT to reflect whether the autoposition is
        the regular mouse position, the nearest curve, or the nearest
        key point.
    */
    DecorationHandle getAutoPositionHandle(AutoPositionHolder ap) {
        if (ap == null) {
            ap = new AutoPositionHolder();
        }
        ap.position = AutoPositionType.NONE;

        Point2D.Double mprin2 = getMousePrincipal();
        if (mprin2 == null) {
            return (mprin == null) ? null : new NullDecorationHandle(mprin);
        }
        Point2D.Double mousePage = principalToStandardPage.transform(mprin2);

        DecorationHandle res = null;
        Point2D.Double newPage = null;
        double pageDist = 1e100;

        try (UpdateSuppressor us = new UpdateSuppressor()) {
                ArrayList<Point2D.Double> selections = new ArrayList<>();
                int oldSize = decorations.size();

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
                    gridLine = Geom.transform(standardPageToPrincipal, gridLine);
                    decorations.add
                        (new CurveDecoration
                         (new CuspFigure(new CuspInterp2D(gridLine),
                                         StandardStroke.INVISIBLE, 0)));
                }

                ArrayList<DecorationHandle> hands = keyPointHandles(false);
                if (hands != null) {
                    for (DecorationHandle h: hands) {
                        Point2D.Double pagePt = principalToStandardPage
                            .transform(h.getLocation());
                        double dist = pagePt.distance(mousePage);
                        if (dist < pageDist) {
                            newPage = pagePt;
                            res = h;
                            pageDist = dist;
                        }
                    }

                    final double OVERLAP_DISTANCE = 1e-10;
                    int parameterizableCnt = 0;
                    for (DecorationHandle h: hands) {
                        Point2D.Double pagePt = principalToStandardPage
                            .transform(h.getLocation());
                        if (pagePt.distance(newPage) > OVERLAP_DISTANCE) {
                            continue;
                        }
                        
                        // Two or more handles are in the same place,
                        // so do nitpicky stuff to get the
                        // parameterization right. If there's only one
                        // parameterizable handle, use it. If there
                        // are two or more, use neither of them,
                        // because it's ambiguous.

                        if (h instanceof BoundedParameterizable2D) {
                            ++parameterizableCnt;
                            if (parameterizableCnt > 1) {
                                // If there are two or more, use
                                // neither, because it's ambiguous.
                                res = new NullDecorationHandle(h.getLocation());
                                break;
                            } else {
                                // If there's only one parameterizable
                                // handle, use it.
                                res = h;
                            }
                        }
                    }

                    if (res != null) {

                        // Subtract keyPointPixelDist (converted to page
                        // coordinates) from keyPointDist before comparing
                        // with curves, in order to express the preference for
                        // key points over curves when the mouse is close to
                        // both.
                        double keyPointPixelDist = 10;
                        pageDist -= keyPointPixelDist / scale;
                        ap.position = AutoPositionType.POINT;
                    }

                    // Only jump to the nearest curve if it is at
                    // least three times closer than pageDist.

                    DecorationDistance nc;
                    if (pageDist > 0
                        && (nc = nearestCurve(mousePage)) != null
                        && pageDist > 3 * nc.distance.distance) {
                        ap.position = AutoPositionType.CURVE;
                        newPage = nc.distance.point;
                        pageDist = nc.distance.distance;
                        res = new NullParameterizableHandle
                            ((ParameterizableDecoration) nc.decoration,
                             nc.distance.t, standardPageToPrincipal);
                    }
                }

                double maxMovePixels = 50; // Maximum number of pixels to
                // move the mouse
                if (newPage == null
                    || pageDist * scale > maxMovePixels) {
                    ap.position = AutoPositionType.NONE;
                    newPage = mousePage; // Leave the mouse where it is.
                    res = new NullDecorationHandle(mprin2);
                }

                while (decorations.size() > oldSize) {
                    decorations.get(decorations.size()-1).remove();
                }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return res;
    }

    /** Invoked from the EditFrame menu */
    public void autoPosition() {
        if (mouseIsStuck) {
            setMouseStuck(false);
        }

        DecorationHandle h = getAutoPositionHandle(null);
        if (h != null) {
            moveMouse(h.getLocation());
            showTangent(h);
            setMouseStuck(true);
            redraw();
        }
    }

    /** Invoked from the EditFrame menu */
    public void enterPosition() {
        CoordinateDialog dog = getCoordinateDialog();
        dog.setTitle("Set mouse position");
        int cnt = dog.rowCnt();
        LinearAxis[] axes = { getXAxis(), getYAxis() };

        dog.setAxes(getAxes());
        for (int i = 0; i < cnt; ++i) {
            dog.setAxis(i, axes[i]);
            if (mprin != null) {
                dog.setValue(i, axes[i].value(mprin));
            } else {
                dog.setValue(i, "");
            }
        }
        
        if (!dog.showModal()) {
            return;
        }

        double[] vs = new double[2];

        try {
            for (int i = 0; i < dog.rowCnt(); ++i) {
                vs[i] = dog.getValue(i);
                axes[i] = dog.getAxis(i, getAxes());
            }
        } catch (NumberFormatException e) {
            showError(e.getMessage());
            return;
        }

        try {
            Affine xformi = inverseTransform(axes[0], axes[1]);
            // Solve the linear system to determine the pair of principal
            // coordinates that corresponds to this pair of
            // whatever-coordinates.
            Point2D.Double newMprin = xformi.transform(vs[0], vs[1]);
            Point2D.Double newMousePage = principalToStandardPage
                .transform(newMprin);
            double d = Geom.distance(newMousePage, pageBounds);
            if (d > 10) {
                showError
                    ("The coordinates you selected lie far outside "
                     + "the page boundaries.  (Remember to use the percent "
                     + "sign when entering percentage values.)",
                     "Coordinates out of bounds");
                return;
            } else if (d > 1e-6) {
                if (!isEditable()) {
                    showError("The point you selected lies beyond the edge "
                              + "of the diagram.");
                    return;
                } else if (JOptionPane.showConfirmDialog
                    (editFrame,
                     htmlify("The coordinates you selected lie beyond the "
                             + "edge of the page. Expand the page margins?"),
                     "Coordinates out of bounds",
                     JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    pageBounds.add(newMousePage);
                    setPageBounds(pageBounds);
                    bestFit();
                } else {
                    return;
                }
            }

            mprin = newMprin;
            moveMouse(newMprin);
            setMouseStuck(true);
        } catch (NoninvertibleTransformException e) {
            showError
                ("The two variables you entered cannot be "
                 + "combined to identify a position.");
            return;
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void setMouseStuck(boolean b) {
        mouseIsStuck = b;
        if (b) {
            mouseTravel = null;
        } else {
            principalFocus = null;
            updateMousePosition();
        }
    }

    protected EditFrame getEditFrame() {
        return editFrame;
    }

    public void setShiftDown(boolean b) {
        isShiftDown = b;
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() != MouseEvent.BUTTON1) {
           if (e.isShiftDown()) {
              mprin = getAutoPosition();
           }
           showPopupMenu(new MousePress(e,mprin));
        } else {
            Point2D.Double p;
            if (e.isShiftDown()) {
                p = getAutoPositionHandle(null).getLocation();
            } else {
                p = getVertexAddMousePosition(e.getPoint());
                if (isPixelMode()) {
                    p = nearestGridPoint(p);
                }
            }
            mousePress = new MousePress(e,p);
            mouseTravel = null;
        }
    }

    Point2D.Double getVertexAddMousePosition(Point panep) {
        if (mprin == null ||
            (getVertexHandle() != null &&
             mouseIsStuckAtSelection())) {
            // It doesn't make sense to add the same point twice,
            // so use the mouse's current position instead of the
            // stuck position.
            return paneToPrincipal(panep);
        } else {
            return mprin;
        }
    }

    public void addVertex() {
        if (principalToStandardPage == null || mprin == null) {
            return;
        }
        add(mprin = getVertexAddMousePosition(getEditPane().getMousePosition()));
        moveMouse(mprin);
        setMouseStuck(true);
    }

    /** @return true if diagram editing is enabled, or false if the
        diagram is read-only. */
    @JsonIgnore public boolean isEditable() {
        return mEditable;
    }

    @JsonIgnore public void setEditable(boolean b) {
        mEditable = b;
        editFrame.setEditable(b);
        mnRightClick.setEditable(b);
    }

    public void setTitle() {
        String str = (String) JOptionPane.showInputDialog
            (editFrame, "Title:", getTitle());
        if (str != null) {
            if ("".equals(str)) {
                removeTitle();
            } else {
                setTitle(str);
            }
        }
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
        if (rightClick != null) {
            return;
        }
        isShiftDown = e.isShiftDown();
        if (mouseTravel == null) {
            mouseTravel = new MouseTravel(e.getX(), e.getY());
        } else {
            mouseTravel.travel(e);
        }
        redraw();
    }

    /** Update mprin to reflect the mouse's current position unless
        mouseIsStuck is true and the mouse has traveled less than
        MOUSE_UNSTICK_DISTANCE pixels since the mouse got stuck. That
        restriction insures that a slight hand twitch will not cause
        the user to lose their place after an operation such as
        seekNearestPoint(). */
    public void updateMousePosition() {
        if (rightClick != null || mousePress != null) {
            return; // Don't update the mouse position while the
                   // right-click menu is active.
        }
        updateMousePosition(getEditPane().getMousePosition());
    }

    /** Update mprin to reflect the mouse being positioned at edit
        panel position mpos unless mouseIsStuck is true and the mouse
        has traveled less than MOUSE_UNSTICK_DISTANCE since it got
        stuck. That restriction insures that a slight hand twitch will
        not cause the user to lose their place after an operation such
        as seekNearestPoint(). */
    public void updateMousePosition(Point mpos) {
        if (principalToStandardPage == null) {
            return;
        }

        if (mpos != null && !preserveMprin) {
            if (!mouseIsStuck || mprin == null
                || (mouseTravel != null
                    && mouseTravel.getTravel() >= MOUSE_UNSTICK_DISTANCE)) {
                mouseIsStuck = false;
                principalFocus = null;
                mprin = paneToPrincipal(mpos);
            }
        }

        if (mprin != null) {
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

    Point2D.Double getMousePrincipal() {
        if (mousePress != null) {
            return mousePress.prin;
        } else if (rightClick != null) {
            return rightClick.prin;
        } else {
            return paneToPrincipal(getEditPane().getMousePosition());
        }
    }

    /** Convert a position on the edit pane to standard page
        coordinates. */
    Point2D.Double paneToStandardPage(Point2D mpos) {
        if (principalToStandardPage == null || mpos == null) {
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
        return new Point2D.Double
            (sx/scale + pageBounds.x, sy/scale + pageBounds.y);
    }

    /** Convert a position on the edit pane to principal
        coordinates. */
    Point2D.Double paneToPrincipal(Point mpos) {
        Point2D p = paneToStandardPage(mpos);
        return (p == null) ? null : standardPageToPrincipal.transform(p);
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

    /** Return the bounds of the selection on the standard page,
        augmented by a slight margin for error (1/400th of the
        bounding rectangle's perimeter). */
    @JsonIgnore Rectangle2D.Double getSelectionBounds() {
        if (selection == null) {
            showError("You must first select something to perform this operation");
            return null;
        }
        Rectangle2D.Double res = bounds(selection.getDecoration());
        if (res.width == 0 || res.height == 0) {
            showError("The selection's width and/or height is too small. ("
                      + Geom.toString(res) + ", " + selection.getDecoration() + ")");
            return null;
        }
        return res;
    }

    Dimension getViewportSize() {
        JScrollPane spane = editFrame.getScrollPane();
        Dimension size = spane.getViewport().getExtentSize();
        size.width -= 1;
        size.height -= 1;
        return size;
    }

    Rectangle getViewRect() {
        JScrollPane spane = editFrame.getScrollPane();
        return spane.getViewport().getViewRect();
    }

    void zoom(Point pane1, Point pane2) {
        Point2D.Double page1 = paneToStandardPage(pane1);
        Point2D.Double page2 = paneToStandardPage(pane2);
        mprin = null;
        zoom(Geom.bounds(new Point2D.Double[] { page1, page2 }));
    }

    void zoomToSelection() {
        zoom(getSelectionBounds());
    }

    void zoom(Rectangle2D pageBounds) {
        if (pageBounds == null) {
            return;
        }

        Dimension size = getViewportSize();
        Rescale r = new Rescale(pageBounds.getWidth(), 0, (double) size.width,
                                pageBounds.getHeight(), 0, (double) size.height);
        setScale(r.t);
        Point2D.Double center = new Point2D.Double
            (pageBounds.getCenterX(), pageBounds.getCenterY());
        standardPageToPrincipal.transform(center, center);

        // If mprin is in the new window, leave it be. If mprin is not
        // in the new window, move it to the center of the new window.
        if ((mprin == null)
            || !roughlyInside(principalToStandardPage.transform(mprin),
                              pageBounds)) {
            mprin = center;
            mouseIsStuck = false;
        }
        centerPoint(center);
        redraw();
    }

    /** Return the minimum scale that does not waste screen real
        estate, or 0 if that is not defined. */
    double bestFitScale() {
        return bestFitScale(getViewportSize());
    }

    /** Adjust the scale so that the page just fits in the edit frame. */
    void bestFit() {
        double s = bestFitScale();
        if (s > 0) {
            setScale(s);
            autoRescale = true;
        }
    }

    /** @return the screen scale of this image relative to a standard
        page (in which the longer of the two axes has length 1). */
    double getScale() { return scale; }

    /** Set the screen display scale; max(width, height) of the
        displayed image will be "value" pixels, assuming that the
        default transform for the screen uses 1 unit = 1 pixel. */
    void setScale(double value) {
        autoRescale = false;
        double oldScale = scale;
        scale = value;
        if (principalToStandardPage == null) {
            return;
        }

        // Keep the center of the viewport where it was if the center
        // was a part of the image.
        Rectangle view = getViewRect();

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
                new Point((int) Math.floor(mousePage.x) - view.x,
                          (int) Math.floor(mousePage.y) - view.y);
        } else {
            // Preserve the center of the viewport.
            viewportPoint = new Point(view.x + view.width / 2,
                                      view.y + view.height / 2);
            prin = scaledPageToPrincipal(oldScale).transform(viewportPoint);
        }

        scaleEditPane();
        setViewportRelation(prin, viewportPoint);
    }

    double getLineWidth() {
        CuspFigure path = getSelectedCuspFigure();
        if (path != null) {
            return path.getLineWidth();
        }
        return lineWidth;
    }

    void customLineWidth() {
        boolean firstTime = false;
        if (lineWidthDialog == null) {
            lineWidthDialog = new LineWidthDialog(editFrame);
            firstTime = true;
        }
        LineWidthDialog dog = lineWidthDialog;
        if (!isFixedAspect()) {
            dog.setUnitsVisible(false);
            if (dog.isUserUnits()) {
                dog.getValueField().setText(null); // Clear it.
            }
            dog.setUserUnits(false);
        }
        if (firstTime) {
            dog.setValue(lineWidth);
        }

        if (dog.showModal()) {
            try {
                double w = dog.getValue();
                if (dog.isUserUnits()) {
                    setGridLineWidth(w);
                } else {
                    setLineWidth(w);
                }
            } catch (NumberFormatException e) {
                showError("Invalid number format.");
                return;
            }
        }
    }

    /** Set the default line width to equal the screen length of the
        principal coordinates vector (x=1, y=0). */
    void setGridLineWidth(double mult) {
        if (principalToStandardPage == null) {
            return;
        }
        Point2D.Double iv = new Point2D.Double(1.0, 0.0);
        principalToStandardPage.deltaTransform(iv, iv);
        setLineWidth(mult * Geom.length(iv));
        editFrame.customLineWidth.setSelected(true);
    }

    EditPane getEditPane() { return editFrame.getEditPane(); }

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
        if (filename == null) {
            run(new String[] {});
        } else {
            run(new String[] { filename });
        }
    }

    public void nextFile() {
        if (!verifyCloseDiagram()) {
            return;
        }
        File file = filesList[++fileNo];
        setCurrentDirectory(file.getParent());
        if (fileNo+1 >= filesList.length) {
            editFrame.mnNextFile.setVisible(false);
        }
        String filename = file.toString();
        if (isPED(file)) {
            try {
                openDiagram(file);
            } catch (IOException e) {
                JOptionPane.showMessageDialog
                    (null, filename + ": " + e,
                     "File load error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            openImage(filename);
        }
        bestFit();
    }

    /** Remove -open arguments, because if the PED Editor is opened
        because of a file association, its arguments have the form
        -open <file>. */
    static String[] stripOpenArguments(String[] args) {
        ArrayList<String> strs = new ArrayList<>();
        for (String s: args) {
            if (!("-open".equals(s))) {
                strs.add(s);
            }
        }
        return strs.toArray(new String[0]);
    }

    public void run(String[] args) {
        setFileAssociations(args.length == 0);
        args = stripOpenArguments(args);
        if (args.length > 0) {
            filesList = new File[args.length];
            for (int i = 0; i < args.length; ++i) {
                filesList[i] = new File(args[i]);
            }
            editFrame.mnNextFile.setVisible(true);
            nextFile();
        } else {
            initializeGUI();
            run();
        }
    }

    /** run() when no arguments are present */
    public void run() {
        Object[] options = {
            "Load image file", "Load .PED file", "Create new diagram"
        };
        switch (JOptionPane.showOptionDialog
                (editFrame,
                 "Please choose how to begin.",
                 "Start",
                 JOptionPane.YES_NO_CANCEL_OPTION,
                 JOptionPane.QUESTION_MESSAGE,
                 null,
                 options,
                 options[0])) {
        case JOptionPane.YES_OPTION:
            showOpenDialog(editFrame, openImageFilesDialog(editFrame));
            break;
        case JOptionPane.NO_OPTION:
            showOpenDialog(editFrame, openPEDFilesDialog(editFrame));
            break;
        case JOptionPane.CANCEL_OPTION:
            newDiagram(false);
            break;
        }
    }

    /** Initializes the crosshairs in the zoom (not edit) frame. */
    public static void initializeCrosshairs() {
        if (crosshairs == null) {
            URL url =
                BasicEditor.class.getResource("images/crosshairs.png");
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
        System.err.println("Usage: java -jar PEDEditor.jar [<filename> ...]");
    }

    @Override public void mouseClicked(MouseEvent e) {}

    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (isZoomMode()) {
                Point p1 = mousePress.e.getPoint();
                Point p2 = e.getPoint();
                if (!p1.equals(p2)) {
                    zoom(mousePress.e.getPoint(), e.getPoint());
                    setMouseStuck(false);
                    mouseTravel = null;
                }
            } else if (isEditable() && principalToStandardPage != null
                       && mprin != null
                       && mousePress != null) {
                add(mprin = mousePress.prin);
                setMouseStuck(true);
            }
            mousePress = null;
        }
    }
    @Override public void mouseEntered(MouseEvent e) {
    }

    @Override public void mouseExited(MouseEvent e) {
        if (rightClick == null) {
            mprin = null;
            redraw();
        }
    }

    ScaledCroppedImage getScaledOriginalImage() {
        Rectangle viewBounds = getViewRect();
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

        ScaledCroppedImage im = new ScaledCroppedImage();
        im.scale = scale;
        im.imageBounds = imageBounds;
        im.cropBounds = cropBounds;
        ImageTransform.DithererType dither
            = (cropBounds.getWidth() * cropBounds.getHeight() > 3000000)
            ? ImageTransform.DithererType.FAST
            : ImageTransform.DithererType.GOOD;

        im.croppedImage = transformOriginalImage
            (cropBounds, scale, dither, DEFAULT_BACKGROUND_IMAGE_ALPHA);
        scaledOriginalImages.add(im);
        return im;
    }

    BufferedImage transformOriginalImage(Dimension size) {
        return transformOriginalImage
            (new Rectangle(size), bestFitScale(size), 
             ImageTransform.DithererType.FAST, getBackgroundImageAlpha());
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
        RGBA allows bitmaps to be layered in nontrivial ways. Allowing
        diagrams to be saved as semitransparent bitmaps would also be
        neat, though I'm not aware of a need for that right now.
    */

    synchronized BufferedImage transformOriginalImage
        (Rectangle cropBounds, double scale, ImageTransform.DithererType dither,
         double alpha) {
        BufferedImage input = getOriginalImage();

        if (input == null || alpha == 0.0) {
            BufferedImage im = new BufferedImage
                (cropBounds.width, cropBounds.height,
                 BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = im.createGraphics();
            g2d.setPaint(Color.WHITE);
            g2d.fill(cropBounds);
            return im;
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
        
        Cursor oldCursor = editFrame.getCursor();
        ++paintSuppressionRequestCnt;
        BufferedImage im = null;
        try {
            editFrame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            System.out.println("Resizing original image (" + dither + ")...");
            im = ImageTransform.run
                (originalToCrop, input, Color.WHITE, cropBounds.getSize(), dither);
            fade(im, im, alpha);
        } finally {
            --paintSuppressionRequestCnt;
            editFrame.setCursor(oldCursor);
        }
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

    /** Recursively hunt for menu items whose action is act, and call
        setVisible(b) on them. */
    static void setVisible(AbstractAction act, MenuElement menu, boolean b) {
        for (MenuElement me: menu.getSubElements()) {
            if (me instanceof JMenuItem) {
                JMenuItem mi = (JMenuItem) me;
                if (mi.getAction() == act) {
                    mi.setVisible(b);
                }
            }
            setVisible(act, me, b);
        }
    }

    void setVisible(AbstractAction act, boolean b) {
        setVisible(act, getEditFrame().getJMenuBar(), b);
        setVisible(act, getRightClickMenu(), b);
    }

    String formatCoordinates(Point2D prin) {
        if (prin == null) {
            return "";
        }
        return "(" + getXAxis().valueAsString(prin) + ", "
            + getYAxis().valueAsString(prin) + ")";
    }

    void showPopupMenu(MousePress mp) {
        if (rightClick == null) {
            rightClick = mp;
            mnRightClick.setCoordinates(formatCoordinates(mp.prin));
        }
        mnRightClick.show(mp.e.getComponent(), mp.e.getX(), mp.e.getY());
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
