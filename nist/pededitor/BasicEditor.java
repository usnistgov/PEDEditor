/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import static gov.nist.pededitor.Stuff.getExtension;
import static gov.nist.pededitor.Stuff.htmlify;
import static gov.nist.pededitor.Stuff.isFileAssociationBroken;
import static gov.nist.pededitor.Stuff.removeExtension;
import static gov.nist.pededitor.Stuff.setClipboardString;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.DoubleUnaryOperator;
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

import com.fasterxml.jackson.annotation.JsonIgnore;

import Jama.Matrix;

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class BasicEditor extends Diagram
    implements CropEventListener, MouseListener, MouseMotionListener {
    static ArrayList<BasicEditor> openEditors = new ArrayList<>();

    abstract class Action extends AbstractAction {
        private static final long serialVersionUID = 1834208008403586162L;

        Action(String name) {
            super(name);
        }
    }

    /**
     * AutoCloseable that, if nothing is selected, hunts for a nearby
     * target for an operation that applies to a Decoration or
     * DecorationHandle and selects that, then finally unselects
     * it. */
    class SelectSomething implements AutoCloseable {
        boolean hadSelection;

        public SelectSomething() {
            hadSelection = (selection != null);
            if (!hadSelection) {
                selectSomething();
            }
        }

        public SelectSomething(String errorTitle) {
            this();
            if (selection == null) {
                showError("Could not guess target. Please select something and try again.",
                        errorTitle);
            }
        }

        @Override public void close() {
            if (!hadSelection) {
                clearSelection();
            }
        }
    }

    class AddDecoration implements Undoable {
        Decoration d;
        int layer;

        AddDecoration(Decoration d, int layer) {
            this.d = d;
            this.layer = layer;
        }

        AddDecoration(Decoration d) {
            this(d, decorations.size());
        }

        @Override public void execute() {
            addDecoration(layer, d);
        }

        @Override public void undo() {
            removeDecoration(d);
        }
    }

    class SetInsertBeforeSelection implements Undoable {
        boolean value;
        boolean oldValue;

        SetInsertBeforeSelection(boolean value) {
            this.value = value;
            this.oldValue = insertBeforeSelection;
        }

        @Override public void execute() {
            insertBeforeSelection = value;
        }

        @Override public void undo() {
            insertBeforeSelection = oldValue;
        }
    }

    class SetSelection implements Undoable {
        DecorationHandle sel;
        DecorationHandle oldSel;

        SetSelection(DecorationHandle sel) {
            oldSel = selection;
            this.sel = sel;
        }

        @Override public void execute() {
            setSelection(sel);
        }

        @Override public void undo() {
            setSelection(oldSel);
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
            int hash = diagramHashCode();
            if (hash != lastSaveHashCode && hash != autoSaveHashCode) {
                Path file = getAutosave();
                try {
                    saveAsPED(file, false);
                    System.out.println("Saved '" + file + "'");
                    autosaveFile = file;
                    autoSaveHashCode = hash;
                } catch (IOException x) {
                    System.err.println("Could not save '" + file + "': " + x);
                }
            }
        }
    }

    void markAsSaved(int hash) {
        lastSaveHashCode = hash;
    }

    void markAsSaved() {
        markAsSaved(diagramHashCode());
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

    public static boolean editable(Decoration d) {
        return d instanceof Label
            || d instanceof TieLine
            || d instanceof LinearRuler;
    }

    /** Set selection to whatever is close, if that's unambiguous. Return true for
        success, false if that was not possible. */
    boolean selectSomething() {
        DecorationHandle h = getAutoPositionHandle(null, false, mprin);
        if (h != null && h.getDecoration() != null) {
            setSelection(h);
            return true;
        }

        return false;
    }

    public void editSelection() {
       boolean hadSelection = (selection != null);

        String errorTitle = "Cannot edit selection";
        if (selection == null) {
            // Find the nearest editable item, and edit it.

            for (DecorationHandle handle: nearestHandles(DecorationHandle.Type.SELECTION)) {
                if (editable(handle.getDecoration())) {
                    setSelection(handle);
                    break;
                }
            }

            if (selection == null) {
                showError("There are no editable items.", errorTitle);
                return;
            }
        }

        Decoration d = selection.getDecoration();
        if (d instanceof Label) {
            edit((Label) d);
        } else if (d instanceof TieLine) {
            edit((TieLine) d);
        } else if (d instanceof LinearRuler) {
            edit((LinearRuler) d);
        } else {
            showError("This item does not have a special edit function.",
                      errorTitle);
        }
        if (!hadSelection) {
            clearSelection();
        }
    }

    public void edit(TieLine item) {
        int lineCnt = askNumberOfTieLines(item.lineCnt);
        if (lineCnt >= 0) {
            item.lineCnt = lineCnt;
            propagateChange();
        }
    }

    public void edit(LinearRuler item) {
        if (getRulerDialog().showModal(item, axes, principalToStandardPage)) {
            propagateChange();
        }
    }

    private static final String PREF_DIR = "dir";
    private static final String PREF_FILE = "file";
    private static final String SHOW_IMAGES = "show_images";
    private static final long AUTO_SAVE_DELAY = 5 * 60 * 1000; // 5 minutes

    static final StandardStroke DEFAULT_LINE_STYLE = StandardStroke.SOLID;
    static final StandardFill DEFAULT_FILL = StandardFill.SOLID;
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
    protected MarginsDialog marginsDialog = null;
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
    protected transient boolean editorIsPacked = false;
    protected transient boolean smoothed = false;
    protected transient boolean showGrid = false;
    protected transient int lastSaveHashCode = 0;
    protected transient int autoSaveHashCode = 0;
    protected transient Dimension oldFrameSize = null;
    protected transient boolean autoRescale = false;
    protected transient boolean allowRobotToMoveMouse = true;
    // This warning is more annoying than helpful.
    protected transient boolean alwaysConvertLabels = true;
    // Number of times paintEditPane() has been called.
    protected transient int paintCnt = 0;
    protected transient boolean removeDegenerateDecorations = true;
    protected transient boolean updateMathWindow = true;
    protected transient WatchNewFiles watchNewFiles = null;
    protected boolean mEditable = true;
    protected boolean exitOnClose = true;

    /** The item (vertex, label, etc.) that is selected, or null if nothing is. */
    protected transient DecorationHandle selection;
    transient Timer fileSaver = null;

    protected transient ArrayList<EditorState.StringAndTransientState>
        undoStack = new ArrayList<>();
    // If undoStackOffset < undoStack.size() then the extras are operations that
    // one can Redo.
    protected transient int undoStackOffset = 0;
    private transient int changesSinceStateSaved = 0;

    protected transient boolean preserveMprin = false;
    protected transient boolean isShiftDown = false;
    protected transient Point2D.Double statusPt = null;

    /** autosaveFile is null unless an autosave event happened and the
        resulting autosavefile has not been deleted by this program. */
    protected transient Path autosaveFile = null;

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

    protected transient double lineWidth = STANDARD_LINE_WIDTH;
    protected transient StandardStroke lineStyle = DEFAULT_LINE_STYLE;
    protected transient StandardFill fill = null;
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
    protected transient ArrayList<DecorationAndT> tieLineCorners;

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
        /** Principal coordinates of the point. */
        Point2D.Double prin;

        /** Don't do autopositioning if the Shift key is depressed
         * when you start dragging. */
        Point2D.Double prinIfDragging;

        public MousePress(MouseEvent e, Point2D.Double prin) {
            this.e = e;
            this.prin = new Point2D.Double(prin.getX(), prin.getY());
            this.prinIfDragging = prin;
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

        /** The mouse is going to (x,y), but don't count that as travel. */
        public void jumpTo(int x, int y) {
            lastX = x;
            lastY = y;
        }
    }

    /** Until the mouse is released, we can't tell whether the user is
        dragging a box to zoom or clicking to add a point. Use
        mousePress to store the mousePressed event and the position of
        the point that will be added if that's what we end up
        doing. */
    protected transient MousePress mousePress = null;
    protected transient MousePress rightClick = null;
    protected transient Rectangle2D dragRectangle = null;

    /**
     * mouseStickTravel logs the distance in pixels that the mouse has
     * moved since the mouse was explicitly positioned, to determine
     * whether to unstick the mouse from that position. */
    protected transient MouseTravel mouseStickTravel = null;
    /** mouseDragTravel logs the distance in pixels that the mouse has
     * moved since the button was depressed, to discriminate between
     * clicks and drags. */
    protected transient MouseTravel mouseDragTravel = null;

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
        mathWindow.refresh();
        mathWindow.setLineWidth(lineWidth);
        mouseIsStuck = false;
        mouseStickTravel = mouseDragTravel = null;
        mousePress = null;
        rightClick = null;
        principalFocus = null;
        paintSuppressionRequestCnt = 0;
        tieLineDialog.setVisible(false);
        tieLineCorners = new ArrayList<>();
        lastSaveHashCode = autoSaveHashCode = 0;

        if (Stuff.isFileAssociationBroken()) {
            // Enable directory monitoring.
            editFrame.mnMonitor.setVisible(Stuff.isFileAssociationBroken());
        }
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
        undoStack.clear();
        undoStackOffset = 0;
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
            if (marginsDialog != null) {
                marginsDialog.dispose();
                marginsDialog = null;
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

    @JsonIgnore Interp2DHandle getInterp2DHandle() {
        return (selection instanceof Interp2DHandle) ?
            ((Interp2DHandle) selection) : null;
    }

    @JsonIgnore public final Decoration getSelectedDecoration() {
        return (selection == null) ? null
            : selection.getDecoration();
    }

    @JsonIgnore public BoundedParam2D getSelectedParameterization() {
        Decoration d = getSelectedDecoration();
        return (d instanceof BoundedParameterizable2D)
            ? ((BoundedParameterizable2D) d).getParameterization()
            : null;
    }

    @JsonIgnore CuspDecoration getSelectedCuspDecoration() {
        Decoration d = getSelectedDecoration();
        return (d instanceof CuspDecoration) ? ((CuspDecoration) d) : null;
    }

    @JsonIgnore LabelHandle getLabelHandle() {
        return (selection instanceof LabelHandle)
            ? ((LabelHandle) selection)
            : null;
    }

    @JsonIgnore Label getSelectedLabel() {
        Decoration d = getSelectedDecoration();
        return (d instanceof Label) ? ((Label) d) : null;
    }

    @JsonIgnore LinearRuler getSelectedRuler() {
        Decoration d = getSelectedDecoration();
        return (d instanceof LinearRuler) ? ((LinearRuler) d) : null;
    }

    @JsonIgnore Arrow getSelectedArrow() {
        return (selection instanceof Arrow)
            ? ((Arrow) selection)
            : null;
    }

    @JsonIgnore Interp2D getSelectedInterp2D() {
        Decoration d = getSelectedDecoration();
        return (d instanceof Interp2DDecoration) ? ((Interp2DDecoration) d).getCurve() : null;
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

    public void removeImage() {
        SourceImage image = firstImage();
        if (image != null)
            removeDecoration(image);
        revalidateZoomFrame();
    }

    public void setDefaultSettingsFromSelection() {
        try (SelectSomething ss = new SelectSomething()) {
            if (selection == null) return;

            Decoration dec = selection.getDecoration();
            StandardStroke ls = dec.getLineStyle();
            if (ls != null) {
                setLineStyle(ls);
            }

            if (dec instanceof Fillable) {
                setFill(((Fillable) dec).getFill());
            }

            setColor(thisOrBlack(dec.getColor()));
            double lw = dec.getLineWidth();
            if (lw != 0) {
                lineWidth = lw;
            }
            Label label = getSelectedLabel();
            if (label != null) {
                getLabelDialog().setFontSize(label.getScale());
            }
        }
    }

    public void setColor(Color c) {
        color = c;
        editFrame.setColor(c);
        redraw();
    }

    public void resetSelectionToDefaultSettings() {
      try (SelectSomething ss = new SelectSomething()) {
          if (selection == null) return;

          Decoration dec = selection.getDecoration();
          resetToDefaultSettings(dec);
          propagateChange();
      }
    }

    void resetToDefaultSettings(Decoration dec) {
        dec.setColor(color);
        dec.setLineWidth(lineWidth);
        if (dec instanceof Label) {
            Label label = (Label) dec;
            if (label != null) {
                label.setScale(getLabelDialog().getFontSize());
            }
        }

        if (fill != null && dec instanceof Fillable) {
            Fillable fillable = (Fillable) dec;
            if (fillable.getFill() != null) {
                fillable.setFill(fill);
            }
        }

        if (lineStyle != null && dec.getLineStyle() != null) {
            dec.setLineStyle(lineStyle);
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

    protected void setImageAlpha(double value) {
        SourceImage image = selectedOrFirstImage();
        if (image != null) {
            image.setAlpha(value);
        }
        editFrame.setAlpha(value);
        propagateChange();
    }

    protected SourceImage selectedOrFirstImage() {
        if (selection instanceof ImageHandle)
            return (SourceImage) selection.getDecoration();
        return firstImage();
    }

    /* Like setImageAlpha(), but attempting to set the background
       to its current value causes it to revert to its previous value.
       This exists just to allow control-H to hide the background
       image and then uh-hide it. */
    protected void toggleImageAlpha(double value) {
        SourceImage image = selectedOrFirstImage();
        if (image != null) {
            image.toggleAlpha(value);
        }
        editFrame.setAlpha(value);
        propagateChange();
    }

    /** Return a list of every decoration that is completely inside
        the closed border of the selection. */
    List<Decoration> decorationsInsideSelection() {
        Decoration d = getSelectedDecoration();
        if (d == null) {
            return Collections.emptyList();
        } else if (d instanceof Interp2DDecoration) {
            return decorationsInside(
                    ((Interp2DDecoration) d).getCurve().createTransformed(
                            principalToStandardPage));
        } else {
            // The decoration itself always counts.
            return Collections.singletonList(d);
        }
    }

    void setClipboard(List<Decoration> ds) {
        ArrayList<Decoration> copies = new ArrayList<>();
        Point2D.Double delta = (mprin != null) ? mprin : principalLocation(selection);
        AffineTransform toOrigin = AffineTransform.getTranslateInstance(-delta.x, -delta.y);
        DecorationsAndHandle wrap = new DecorationsAndHandle();
        wrap.decorations = new ArrayList<>(ds);
        wrap.saveHandle(selection);

        for (Decoration d: ds) {
            // Don't transform tie lines -- let the transformation of
            // the underlying curve do the work.
            if (!(d instanceof TieLine)) {
                Decoration od = d;
                d = d.createTransformed(toOrigin);
                if (d instanceof HasJSONId) {
                    ((HasJSONId) d).setJsonId(((HasJSONId) od).getJsonId());
                }
            }
            copies.add(d);
        }
        wrap.decorations = copies;

        try {
            Stuff.setClipboardString(toJsonString(wrap), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyAndPaste() {
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

        if (selection instanceof Interp2DHandle2) {
            // Move the selection to the nearest vertex.
            selection = ((Interp2DHandle2) selection).indexHandle();
        }

        if (mouseIsStuckAtSelection()) {
            setMouseStuck(false);
        }

        Point2D.Double p = principalLocation(selection);
        setSelection(selection.copy(mprin.x - p.x, mprin.y - p.y));
        addDecoration(selection.getDecoration());
    }

    /**
     * Assuming the clipboard contains the JSON for an array of
     * Decorations, paste those decorations to the diagram. */
    public void paste() {
        if (mprin == null) {
            showError(
                    "Move the mouse to the destination first.",
                    "Cannot paste");
            return;
        }
        saveState();

        // Clear the IDs of existing decorations to prevent overlap with IDs of the decorations being added.
        for (Decoration d: decorations) {
            if (d instanceof HasJSONId) {
                ((HasJSONId) d).clearJsonId();
            }
        }

        String json = Stuff.getClipboardString();
        if (json == null || json.equals("")) {
            showError("The clipboard is currently empty.");
            return;
        }
        AffineTransform toPrin = AffineTransform.getTranslateInstance(mprin.x, mprin.y);
        try {
            DecorationsAndHandle wrap = jsonStringToDecorations(Stuff.getClipboardString());
            finishDeserialization(wrap.decorations);
            List<Decoration> ds = wrap.decorations;
            DecorationHandle selNew = wrap.createHandle();
            // TODO Pasting source images isn't reliable right now, so get rid of them.
            // ds = ds.stream().filter(p -> !(p instanceof SourceImage)).collect(Collectors.toList());

            for (Decoration d : ds) {
                if (!(d instanceof TieLine)) {
                    d.transform(toPrin);
                }
                if (d instanceof LinearRuler) {
                    // We don't want to transform the axis, we just
                    // want to move the ruler endpoints. Reassociate
                    // the translated ruler with the existing axis.
                    linkRulerWithAxis((LinearRuler) d, axes);
                }
                addDecoration(d);
            }
            if (selNew != null) {
                setSelection(selNew);
            }
        } catch (IOException e) {
            if (json.length() < 5000) {
                // Maybe this is ordinary text, and the user wanted to create a label.
                addLabel(json);
            }
        }
    }

    /** Reset the location of all vertices and labels that have the
        same location as the selection to mprin.

        @param moveAll If true, all items located at the selected
        point will move moved. If false, only the selected item itself
        will be moved.
    */
    public void moveSelection(boolean moveAll) {
        saveState();
        String errorTitle = "Cannot move selection";
        if (selection == null) {
            showError("You must select an item before you can move it.",
                    errorTitle);
            return;
        }

        if (mprin == null) {
            showError("You must move the mouse to the target destination " +
                    "before you can move items.",
                 errorTitle);
            return;
        }

        if (selection instanceof Interp2DHandle2) {
            // Move the selection to the nearest vertex.
            selection = ((Interp2DHandle2) selection).indexHandle();
        }

        if (mouseIsStuckAtSelection()) {
            setMouseStuck(false);
        }

        setSelection(moveSelection(mprin, moveAll));

        setMouseStuck(true);
    }

    /** Copy the selection to the clipboard. */
    public void copySelection() {
        String errorTitle = "Cannot copy selection";

        try (SelectSomething ss = new SelectSomething(errorTitle)) {
            if (selection == null) return;

            setClipboard(Collections.singletonList(selection.getDecoration()));
        }
    }

    public void cut(List<Decoration> decorations) {
        setClipboard(decorations);
        saveState();
        for (Decoration d: decorations) {
            int layer = getLayer(d);
            if (layer == -1) {
                continue; // OK, removing a different decoration may have deleted this one too.
            } else {
                removeDecoration(layer);
            }
        }
    }

    /** Cut the selection and copy it to the clipboard. */
    public void cutSelection() {
        String errorTitle = "Cannot cut selection";
        try (SelectSomething ss = new SelectSomething(errorTitle)) {
            if (selection == null) return;
            cut(Collections.singletonList(selection.getDecoration()));
        }
    }

    /**
     * Cut everything in the selected region. All control points of a
     * decoration must be inside the region for it to be cut. */
    public void cutRegion() {
        String errorTitle = "Cannot cut region";
        try (SelectSomething ss = new SelectSomething(errorTitle)) {
            if (selection == null) return;

            if (selection.getDecoration() instanceof Label) {
                Label label = (Label) selection.getDecoration();
                ArrayList<Decoration> likeThis = new ArrayList<>();
                for (Label l2: labels()) {
                    if (l2.getText().equals(label.getText())) {
                        likeThis.add(l2);
                    }
                }
                cut(likeThis);
                return;
            }

            if (!(selection.getDecoration() instanceof Interp2DDecoration)) {
                showError("Select a curve that will become the boundary of the cut region. "
                        + "Hint: you may press Shift and hold down the left mouse button "
                        + "while dragging the mouse to select a new rectangle.",
                        errorTitle);
                return;
            }
            cut(decorationsInsideSelection());
        }
    }

    /**
     * Cut everything and copy it to the clipboard. */
    public void cutAll() {
        cut(new ArrayList<Decoration>(decorations));
    }

    public void changeLayer(int delta) {
        try (SelectSomething ss = new SelectSomething("Cannot change layer")) {
            if (selection == null) return;

            saveState();
            changeLayer0(delta);
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
                Rectangle2D.Double bounds2 = bounds(jumpedPast);
                if (bounds2 == null || Geom.distanceSq(selectionBounds, bounds2)
                    < 1e-12) {
                    break;
                }
            }
        }
        setLayer(d, layer);
    }

    /** Change the selection's color. */
    public void colorSelection() {
        try (SelectSomething ss = new SelectSomething("Cannot change color")) {
            if (selection == null) return;

            if (colorChooser == null) {
                colorChooser = new JColorChooser();
                colorDialog = JColorChooser.createDialog
                    (editFrame, "Choose color", true, colorChooser,
                            new ActionListener() {
                                @Override public void actionPerformed(ActionEvent e) {
                                    setColor(colorChooser.getColor());
                                    BasicEditor.this.selection.getDecoration().setColor(color);
                                    propagateChange();
                                }
                            },
                            null);
                colorDialog.pack();
            }

            Color c = selection.getDecoration().getColor();
            if (c != null) {
                saveState();
                setColor(c);
            }
            colorChooser.setColor(color);
            colorDialog.setVisible(true);
        }
    }

    boolean mouseIsStuckAtSelection() {
        return mouseIsStuck && selection != null && mprin != null
            && principalCoordinatesMatch(principalLocation(selection),
                    mprin);
    }

    /** Move the selection to prin.

        @param moveAll If true, move all selectable items that have
        the same location as the selection to prin. If false, move
        only the selection itself.
     * @return */
    public DecorationHandle moveSelection(Point2D.Double prin,
            boolean moveAll) {
        DecorationHandle res = null;
        Point2D.Double page = principalToStandardPage.transform(prin);
        Point2D.Double oldPage = pageLocation(selection);
        Point2D.Double delta = Geom.aMinusB(page, oldPage);
        standardPageToPrincipal.deltaTransform(delta, delta);
        try {
            CuspDecoration.removeDuplicates = true;
            Point2D.Double selPage = pageLocation(selection);
            res = selection.moveHandle(delta.x, delta.y);
            if (moveAll) {
                for (DecorationHandle h: getDecorationHandles(DecorationHandle.Type.CONTROL_POINT)) {
                    if (h.getDecoration() == selection.getDecoration())
                        continue;
                    if (pageCoordinatesMatch(selPage, pageLocation(h))) {
                        h.moveHandle(delta.x, delta.y);
                    }
                }
            }
        } finally {
            CuspDecoration.removeDuplicates = false;
        }
        propagateChange();
        return res;
    }

    @Override public void swapDiagramComponents(Side side1, Side side2) {
        saveState();
        try {
            super.swapDiagramComponents(side1, side2);
        } catch (IllegalArgumentException x) {
            showError(x.toString());
        }
    }

    @Override public void swapXY() {
        saveState();
        try {
            super.swapXY();
            bestFit();
        } catch (IllegalArgumentException x) {
            showError(x.toString());
        }
    }

    public void removeSelection() {
        try (SelectSomething ss = new SelectSomething()) {
            if (selection == null) return;
            saveState();
            setSelection(removeHandle(selection, insertBeforeSelection));
        }
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
        Interp2DHandle sel = getInterp2DHandle();
        if (sel == null) {
            return; // Nothing to do.
        }

        Interp2DDecoration d = sel.getDecoration();
        int size = d.getCurve().size();
        Interp2D path = d.getCurve().createTransformed(principalToStandardPage);
        BoundedParam2D param = path.getParameterization();
        Point2D.Double g = (param == null) ? null
            : param.getDerivative(sel.getT());

        if (g == null) {
            return; // Nothing to do.
        }

        // nextIsRight is true if the gradient points rightward or if
        // the gradient points straight up (to within numeric
        // error).
        boolean nextIsRight = g.getX() > 0 ||
            Math.abs(g.getX() * 1e12) < g.getY();
        int delta = (nextIsRight == rightward) ? 1 : -1;

        int vertexNo = sel.getIndex();

        if (path.isClosed()) {
            vertexNo = (vertexNo + size + delta) % size;
        } else {
            vertexNo += delta;
            if (vertexNo > size - 1) {
                vertexNo = size - 1;
                insertBeforeSelection = false;
            } else if (vertexNo < 0) {
                vertexNo = 0;
                insertBeforeSelection = true;
            }
        }

        setSelection(sel.getDecoration().createHandle(vertexNo));
        moveMouse(principalLocation(selection));
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

        Point2D.Double mousePage = mousePage();
        mousePage.x += dx / scale;
        mousePage.y += dy / scale;
        moveMouseAndMaybeSelection(
                standardPageToPrincipal.transform(mousePage));
    }

    /** Like move(), but move in the given direction until you run
        into a line. If there are no lines past this one, then do
        nothing.
    */
    public void jump(int dx, int dy) {
        if (mprin == null) {
            return;
        }

        Point2D.Double mousePage = mousePage();

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

    void closeWaitDialog() {
        if (waitDialog != null) {
            waitDialog.dispose();
            waitDialog = null;
        }
    }

    /** Most of the information required to paint the EditPane is part
        of this object, so it's simpler to do the painting from
        here. */
    public void paintEditPane(Graphics g) {
        if (++paintCnt == 1) {
            closeWaitDialog();
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

    static Color toColor(AutoPositionType ap) {
        return ap == AutoPositionType.NONE ? Color.RED
            : ap == AutoPositionType.CURVE
            ? new Color(0xd87000) // Orange
            : new Color(0xb0c000); // Yellow
    }

    /** Return true if the user is currently dragging the mouse to select a region. */
    boolean isDragging() {
        return mousePress != null && mouseDragTravel != null
            && mouseDragTravel.getTravel() >= mouseDragDistance;
    }

    Point2D.Double clickPosition(boolean unstick, AutoPositionHolder ap) {
        if (isDragging()) {
            return null;
        } else if (isShiftDown) {
            return statusPt = getAutoPosition(ap);
        } else if (unstick && mouseIsStuckAtSelection()) {
            // Show the point that would be added if the mouse became
            // unstuck.
            return getMousePrincipal();
        } else {
            return mprin;
        }
    }

    void highlightNoncurve(Graphics2D g, double scale, DecorationHandle sel) {
        Decoration d = sel.getDecoration();
        try (UpdateSuppressor us = new UpdateSuppressor()) {
            Color oldColor = d.getColor();
            Color highlight = getHighlightColor(oldColor);

            g.setColor(highlight);
            d.setColor(highlight);
            if (selection instanceof LabelHandle) {
                paintSelectedLabel(g, scale);
            } else {
                draw(g, sel.getDecoration(), scale);
                circleVertices(g, sel, scale);
            }
            d.setColor(oldColor);
        }
    }

    /**
     * Show the outline of hand in green, and show in red the result
     * of clicking at the mouse point if hand is selected. */
    void highlightCurve(Graphics2D g, double scale,
                            Interp2DHandle hand) {

        // Color in red the curve that would exist if the current
        // mouse position were added, assuming we're not at maxSize
        // already. Color in green the curve that already exists.

        hand = hand.copy(0,0);
        Interp2DDecoration curve = hand.getDecoration();

        try (UpdateSuppressor us = new UpdateSuppressor()) {
            boolean isFilled = (curve instanceof Fillable)
                && ((Fillable) curve).getFill() != null;
            if (isFilled)
                curve.setLineStyle(StandardStroke.SOLID);

            Color highlightColor = getHighlightColor(curve.getColor());
            if (!(curve instanceof LinearRuler)) {
                double MAX_LINE_WIDTH_PIXELS = 2;
                curve.setLineWidth(MAX_LINE_WIDTH_PIXELS / scale);
            }

            // Disable anti-aliasing for this phase because it
            // prevents the green line from precisely overwriting
            // the red line.

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_OFF);

            highlightChange(g, scale, hand);

            curve.setColor(highlightColor);
            draw(g, curve, scale);
            g.setColor(highlightColor);
            circleVertices(g, hand, scale);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    /** Special label highlighting rules: show the label box, and if
        the anchor is not at the center, show the anchor as either a
        hollow circle (if not selected) or as a solid circle (if
        selected). */
    void paintSelectedLabel(Graphics2D g, double scale) {
        LabelHandle hand = getLabelHandle();
        Label label = hand.getDecoration();
        draw(g, label, scale);

        if (label.getXWeight() != 0.5 || label.getYWeight() != 0.5) {
            // Mark the anchor with a solid circle if it is the
            // selection handle or a hollow circle otherwise.
            double r = Math.max(scale * 2.0 / BASE_SCALE, 4.0);
            boolean anchorIsSelected = (hand.getType() != LabelHandle.Type.CENTER);
            circleVertex(g, principalLocation(label), scale, anchorIsSelected, r);
        }
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
            (c.getGreen() + 2.0 * c.getRed() + c.getBlue() + 1);
        return (brightness > 0.2 && greenness > 0.5) ? Color.MAGENTA
            : Color.GREEN;
    }

    public double gridStep(LinearAxis ax, double scale) {
        if (isTernary()) {
            return 0.1;
        } else if (isPixelMode()) {
            return 0.5;
        } else {
            double maxGridLineDistance = 50; // pixels
            double axisChangePerPixel = Geom.length(pageGradient(ax));
            return RulerTick.roundFloor(maxGridLineDistance/scale * axisChangePerPixel);
        }
    }

    public double gridStepX(double scale) {
        return gridStep(getXAxis(), scale);
    }

    public double gridStepY(double scale) {
        return gridStep(getYAxis(), scale);
    }

    public Point2D.Double nearestGridPoint(Point2D prin) {
        double gx = gridStep(getXAxis(), scale);
        double x = prin.getX();
        x = Math.rint(x / gx) * gx;
        double gy = gridStep(getYAxis(), scale);
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
        Rectangle2D.Double vr = getStandardPageViewRect();
        double[] lineRange = getRange(lineAx, vr);
        double[] stepRange = getRange(stepAx, vr);

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
        for (double step = stepMin; step < stepMax; step += dstep) {
            Point2D.Double p1 = toPage.transform(lineMin, step);
            Point2D.Double p2 = toPage.transform(lineMax, step);
            g.drawLine((int) Math.round(p1.x), (int) Math.round(p1.y),
                       (int) Math.round(p2.x), (int) Math.round(p2.y));
        }
    }

    public void paintGridLines(Graphics2D g, double scale) {
        double xstep = isPixelMode() ? 1.0 : gridStepX(scale);
        double ystep = isPixelMode() ? 1.0 : gridStepY(scale);
        g.setColor(new Color(180, 180, 180));
        paintGridLines(g, scale, getXAxis(), getYAxis(), ystep);
        paintGridLines(g, scale, getYAxis(), getXAxis(), xstep);
        if (isTernary()) {
            paintGridLines(g, scale, getYAxis(), getLeftAxis(), xstep);
        } else {
            g.setColor(Color.BLACK);
            paintGridLines(g, scale, getXAxis(), getYAxis(), ystep*5);
            paintGridLines(g, scale, getYAxis(), getXAxis(), xstep*5);
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

        applyRenderingHints(g);
        paintBackground(g, scale, Color.WHITE);
        if (showGrid) {
            paintGridLines(g, scale);
        }

        statusPt = mprin;

        Interp2DHandle curveHandle = (selection instanceof Interp2DHandle) ? getInterp2DHandle()
            : null;
        for (int dn = 0; dn < decorations.size(); ++dn) {
            Decoration decoration = decorations.get(dn);
            g.setColor(thisOrBlack(decoration.getColor()));
            draw(g, decoration, scale);
        }

        if (curveHandle != null) {
            highlightCurve(g, scale, curveHandle);
        } else {
            highlightChange(g, scale, selection);
            if (selection != null) {
                highlightNoncurve(g, scale, selection);
            }
        }

        if (isDragging() && mprin != null) {
            g.setColor(mousePress.e.isShiftDown() ? Color.GREEN : Color.RED);
            Stroke oldStroke = g.getStroke();
            g.setStroke(new BasicStroke(2.0f));
            Affine xform = principalToScaledPage(scale);
            Point2D[] points = { xform.transform(mousePress.prinIfDragging), xform.transform(mprin) };
            g.draw(Geom.bounds(points));
            g.setStroke(oldStroke);
        }

        if (selection instanceof Interp2DHandle2) {
            Point2D.Double p1 = principalLocation(selection);
            Point2D.Double p2 = principalLocation(((Interp2DHandle2) selection).indexHandle());
            if (!principalCoordinatesMatch(p1, p2)) {
                g.setColor(toColor(AutoPositionType.CURVE));
                circleVertex(g, p1, scale, true, 4);
            }
        }

        if (statusPt != null) {
            editFrame.setStatus(principalToPrettyString(statusPt));
        }
    }

    /** Paint a crosshairs at principal coordinate p. */
    void paintCross(Graphics2D g, Point2D.Double p, double scale) {
        Point2D.Double vPage = principalToScaledPage(scale).transform(p);
        int r = 11;
        int ix = (int) vPage.x;
        int iy = (int) vPage.y;
        int r2 = 1;
        g.setStroke(new BasicStroke(2));
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

        DecorationHandle oldSel = selection;
        selection = hand;

       if (oldSel != null) {
            Decoration oldd = oldSel.getDecoration();
             if (removeDegenerateDecorations && oldd.isDegenerate() &&
                    (hand == null || oldd != hand.getDecoration())) {
                removeDecorationIfFound(oldd);
            }
        }

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

    public void setSelectedFill(StandardFill fill) {
        setFill(fill);
        Decoration d = getSelectedDecoration();
        if (d != null && (d instanceof Fillable)) {
            if (d instanceof CurveCloseable) {
                ((CurveCloseable) d).setClosed(true);
            }

            ((Fillable) d).setFill(fill);
            propagateChange();
        }
    }

    public void setFill(StandardFill fill) {
        this.fill = fill;
        if (fill != null) {
            this.lineStyle = null;
        }
    }

    /** Toggle the highlighted vertex between the smoothed and
        un-smoothed states. Return false if it was not possible to do
        this, true otherwise. */
    public boolean toggleCusp() {
        try (SelectSomething ss = new SelectSomething()) {
            if (!(selection instanceof Interp2DHandle)) {
                return false;
            }

            Interp2DHandle hand = (Interp2DHandle) selection;
            Interp2D curve = hand.getDecoration().getCurve();
            if (!(curve instanceof Smoothable))
                return false;

            ((Smoothable) curve).toggleSmoothed(hand.getIndex());
            propagateChange();
            return true;
        }
    }

    /** Update the math window to show whatever information is
        available for the given handle. */
    public void showTangent(DecorationHandle hand) {
        if (hand instanceof BoundedParam2DHandle) {
            BoundedParam2DHandle bp = (BoundedParam2DHandle) hand;
            showTangent(bp.getDecoration(), bp.getT());
        } else if (hand != null) {
            showTangent(hand.getDecoration());
        }
    }

    /** Return the slope at the given t value in terms of
        dStandardPageY/dStandardPageX, or null if the value cannot be
        computed. */
    public Point2D.Double derivative(Decoration dec, double t) {
        BoundedParam2D param = getStandardPageParameterization(dec);
        if (param == null) {
            return null;
        }
        if (t == Math.floor(t)) {
            // If this is a pointy vertex, then it makes a big
            // difference whether I compute the derivative on the left
            // or right side. If this isn't point, it won't make much
            // difference. So just assume it's pointy.
            t = BoundedParam2Ds.constrainToDomain
                (param, t + 1e-10 * (insertBeforeSelection ? -1 : 1));
        }

        return param.getDerivative(t);
    }

    boolean hasArea(Decoration dec) {
        return (dec instanceof Interp2DDecoration)
            && ((Interp2DDecoration) dec).getCurve().isClosed();
    }

    public void showTangent(Decoration dec, double t) {
        if (!updateMathWindow) {
            return;
        }
        Point2D.Double g = derivative(dec, t);
        if (g != null && (g.x != 0 || g.y != 0)) {
            mathWindow.setScreenDerivative(g);
        }
        BoundedParam2D b = null;
        mathWindow.setAreaVisible(showArea());
        mathWindow.setLengthVisible(showLength());
        if (showArea() && ((b = getStandardPageParameterization(dec)) != null)) {
            double t0, t1;
            double areaMul;
            if (b.getStart().x > b.getEnd().x) {
                areaMul = -1;
                t0 = t;
                t1 = b.getMaxT();
            } else {
                areaMul = 1;
                t0 = b.getMinT();
                t1 = t;
            }

            // When translating page coordinates back to principal
            // coordinates, divide by the "multipliers".
            areaMul /= areaMultiplier();
            double area = b.area(t0, t1) * areaMul;
            double totArea = b.area() * areaMul;
            double lengthMul  = lengthMultiplier();
            double length = b.length(0, 1e-6, 800, t0, t1).value / lengthMul;
            double totLength = b.length(0, 1e-6, 800).value / lengthMul;

            if (hasArea(dec)) {
                mathWindow.setTotLengthLabel("Perimeter");
                mathWindow.setTotAreaLabel("Area");
                totArea = Math.abs(totArea);
            } else {
                mathWindow.setTotLengthLabel("Total length");
                mathWindow.setTotAreaLabel("Total \u222B");
            }

            mathWindow.setAreas(area, totArea);
            if (showLength()) {
                mathWindow.setLengths(length, totLength);
            }
        }

        showTangentCommon(dec);
    }

    boolean showArea() {
        if (isTernary()) { return false; }
        for (String s: diagramComponents) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }

    boolean showLength() {
        return showArea() && isFixedAspect();
    }

    public void showTangent(Decoration dec) {
        if (dec == null || !updateMathWindow) {
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

    /**
     * Return true if, were point p inserted into path at index, it would be the
     * same as the point preceding or following it. CubicSpline2D barfs on
     * smoothing between a series of points where the same point appears twice
     * in a row, so inserting duplicate points is bad.
     */
    boolean isDuplicate(Point2D prin, Interp2D path, int index) {
        int s = path.size();
        double pmd = pageMatchDistance();
        return (index > 0 && principalCoordinatesMatch(prin, path.get(index-1), pmd))
            || (index < s && principalCoordinatesMatch(prin, path.get(index), pmd))
            || (path.isClosed() && s > 1 &&
                ((index == 0 && principalCoordinatesMatch(prin, path.get(s-1), pmd))
                 || (index == s && principalCoordinatesMatch(prin, path.get(0), pmd))));
    }

    int vertexInsertionIndex() {
        return getInterp2DHandle().getIndex() + (insertBeforeSelection ? 0 : 1);
    }

    private CuspDecoration emptyCuspDecoration() {
        StandardStroke ls = (lineStyle == null) ? DEFAULT_LINE_STYLE
            : lineStyle;
        CuspDecoration d = (fill != null)
            ? new CuspDecoration(new CuspInterp2D(true), fill)
            : new CuspDecoration(new CuspInterp2D(false), ls);
        resetToDefaultSettings(d);
        return d;
    }

    /** Start a new CuspDecoration consisting of a single point, and
        make it the new selection. */
    Undoable newCurveCommand(Point2D.Double point) {
        StandardFill fill1 = (fill == null) ? DEFAULT_FILL : fill;
        StandardStroke ls = (lineStyle == null) ? DEFAULT_LINE_STYLE
            : lineStyle;

        CuspDecoration curve = null;

        if (isPixelMode()) {
            // If point is integer coordinates, assume this is the
            // corner of a fill region. That could go wrong if the
            // user wants to create 2x2 pixel dots, but that's less
            // likely.

            if (Geom.integerish(point.x) && Geom.integerish(point.y)) {
                curve = new CuspDecoration(new CuspInterp2D(true), fill1);
            }

            // If point has integer-and-a-half coordinates, assume
            // this is a line, not a fill region.
            if (Geom.integerish(point.x + 0.5) && Geom.integerish(point.y + 0.5)) {
                curve = new CuspDecoration(new CuspInterp2D(false), ls);
            }
        }

        if (curve == null) {
            curve = emptyCuspDecoration();
        }
        curve.setLineWidth(lineWidth);
        curve.setColor(color);
        Interp2DHandle h = new Interp2DHandle(curve, 0);
        addVertexCommand(h, point).execute();
        return new UndoableList(
                new AddDecoration(curve),
                new SetSelection(h),
                new SetInsertBeforeSelection(false));
    }

    Undoable addVertexCommand(Interp2DHandle handle, Point2D point) {
        return new AddVertex(handle.getDecoration(), handle.getIndex(), point, smoothed);
    }

    Undoable clickCommand(Point2D.Double point) {
        if (principalToStandardPage == null) {
            return new NoOp();
        }

        Decoration d = getSelectedDecoration();

        if (d != null && (d instanceof Label || d instanceof Interp2DDecoration)) {
            return clickCommand(selection, point);
        } else {
            return newCurveCommand(point);
        }
    }

    Undoable clickCommand(DecorationHandle hand0, Point2D.Double point) {
        Decoration d0 = hand0.getDecoration();
        if (d0 instanceof Label || hand0 instanceof ArcCenterHandle) {
            Point2D.Double selPos = principalLocation(hand0);
            DecorationHandle lhand = hand0.copy(point.x - selPos.x, point.y - selPos.y);
            return new UndoableList(
                    new AddDecoration(lhand.getDecoration()),
                    new SetSelection(lhand));
        }
        Interp2D curve = ((Interp2DDecoration) hand0.getDecoration()).getCurve();
        Interp2DHandle hand = (Interp2DHandle) hand0;
        Point2D.Double oldPoint = principalLocation(hand);

        if (curve.size() == curve.maxSize()) {
            if (isDuplicate(point, curve, hand.index)) {
                return new NoOp(); // Adding the same point twice causes problems.
            }
            return new MoveVertex(hand, point.x - oldPoint.x, point.y - oldPoint.y);
        } else {
            int i2 = vertexInsertionIndex();
            if (isDuplicate(point, curve, i2)) {
                return new NoOp();
            }
            return addVertexCommand(hand.getDecoration().createHandle(i2), point);
        }
    }

    /**
     * Show what would happen if the user clicked. */
    void highlightChange(Graphics2D g, double scale,
            DecorationHandle hand) {
        Decoration d = (hand != null) ? hand.getDecoration() : null;
        boolean unstick = (d instanceof Interp2DDecoration);
        AutoPositionHolder ap = new AutoPositionHolder();
        if (mouseIsStuck && !unstick) {
            ap.position = AutoPositionType.POINT;
        }
        Point2D.Double extraVertex = clickPosition(unstick, ap);
        if (extraVertex == null) {
            return;
        }
        Color color = toColor(ap.position);

        if (isEditable()) {
            if (d instanceof Interp2DDecoration || d instanceof Label) {
                boolean oldR = removeDegenerateDecorations;
                boolean oldU = updateMathWindow;
                try {
                    removeDegenerateDecorations = false;
                    updateMathWindow = false;
                    Color oldColor = null;
                    Decoration d2 = null;
                    try (DoThenUndo thing = new DoThenUndo(
                                    clickCommand(hand, extraVertex))) {
                        d2 = (hand instanceof Interp2DHandle) ? d : selection.getDecoration();
                        oldColor = d2.getColor();
                        d2.setColor(color);
                        draw(g, d2, scale);
                    } finally {
                        if (oldColor != null) {
                            d2.setColor(oldColor);
                        }
                    }
                } finally {
                    removeDegenerateDecorations = oldR;
                    updateMathWindow = oldU;
                }
            }
        }

        if (mouseIsStuck || ap.position != AutoPositionType.NONE) {
            g.setColor(color);
            paintCross(g, extraVertex, scale);
        }
    }

    /** Add a point to getActiveCurve(), or move a point if the curve
        is at maxSize already, or create and select a new curve if no
        curve is currently selected. */
    public void click(Point2D.Double point) {
        Undoable command = clickCommand(point);
        if (++changesSinceStateSaved >= 3) {
            saveState();
        }
        if (command instanceof AddVertex) {
            AddVertex add = (AddVertex) command;
            addVertex(add);
            setSelection(add.d.createHandle(add.index));
        } else {
            command.execute();
            propagateChange();
        }
        showTangent(selection);
    }

    @Override public Decoration removeDecoration(int layer) {
        Decoration res = super.removeDecoration(layer);

        if (selection != null && getLayer(selection.getDecoration()) == -1) {
            clearSelection();
        }
        if (res instanceof SourceImage) {
            revalidateZoomFrame();
        }
        return res;
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

    private void tieLineStep() {
        int stepNo = tieLineCorners.size();
        tieLineDialog.getButton().setText("Select corner " + (stepNo+1) + " / 4"
                    + " (Shift+T)");
    }

    void tieLineCornerSelected() {
        String errorTitle = "Invalid tie line selection";

        Interp2DHandle vhand = getInterp2DHandle();
        if (vhand == null) {
            showError("You must select a vertex.", errorTitle);
            return;
        }
        if (vhand instanceof Interp2DHandle2) {
            // Move the selection to the nearest vertex, because we
            // decided tie lines ending on non-control-points was more
            // of a nuisance than a help.
            vhand = ((Interp2DHandle2) selection).indexHandle();
        }

        DecorationAndT pat = new DecorationAndT(vhand.getDecoration(),
                pageT(vhand));

        int oldCnt = tieLineCorners.size();

        if ((oldCnt == 1 || oldCnt == 3)
            && pat.d != tieLineCorners.get(oldCnt-1).d) {
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
            tieLineStep();
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

        tie.innerEdge = tieLineCorners.get(2).d;
        tie.it1 = tieLineCorners.get(2).t;
        tie.it2 = tieLineCorners.get(3).t;

        tie.outerEdge = tieLineCorners.get(0).d;
        tie.ot1 = tieLineCorners.get(0).t;
        tie.ot2 = tieLineCorners.get(1).t;

        setSelection(new TieLineHandle(tie, TieLineHandle.Type.OUTER2));
        addDecoration(tie);
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

    static class DecorationAndPoints {
        CuspDecoration d; // The old decoration, not the new one.
        List<Point2D.Double> points;
        Interp2DHandle handle;
    }

    DecorationAndPoints convertSelection(Interp2DDecoration d,
            boolean useRectangleMidpoints) {
        String type = d.typeName();
        String errorTitle = "Cannot create " + type;
        CuspDecoration cdec = getSelectedCuspDecoration();
        // One less than the min is OK, because then you can see what
        // the decoration will look like if you add one more vertex.
        int size = cdec.getCurve().size();
        int minSize = Math.max(0, d.getCurve().minSize());
        int maxSize = d.getCurve().maxSize();
        if (maxSize < 0)
            maxSize = 100;
        if (cdec == null || size < minSize || size > maxSize) {
            String tween = (minSize == maxSize) ?
                ("" + minSize) :
                ("between " + minSize + " and " + maxSize);
            showError(
                    "Before you can create a new " + type
                    + ", you must create and select a curve "
                    + "consisting of " + tween + " vertices.",
                    errorTitle);
            return null;
        }
        DecorationAndPoints res = new DecorationAndPoints();
        res.points = new ArrayList<>(Arrays.asList(cdec.getCurve().getPoints()));
        res.d = cdec;
        res.handle = d.createHandle(getInterp2DHandle().getIndex());
        d.setLineWidth(cdec.getLineWidth());
        d.setColor(cdec.getColor());
        d.setLineStyle(cdec.getLineStyle());
        if (useRectangleMidpoints && cdec.isClosed() && size == 4) {
            ArrayList<Point2D.Double> ms = new ArrayList<>();
            for (int i = 0; i < 4; ++i) {
                ms.add(Geom.midpoint(res.points.get(i), res.points.get((i + 1) % 4)));
            }
            res.points = ms;
        }
        if (d instanceof Fillable)
            d.setFill(cdec.getFill());
        return res;
    }

    public void addRuler() {
        LinearRuler r = new LinearRuler();
        DecorationAndPoints dap = convertSelection(r, false);
        if (dap == null)
            return;

        r.setPoints(dap.points);
        r.fontSize = rulerFontSize();
        r.tickPadding = 0.0;
        r.labelAnchor = LinearRuler.LabelAnchor.NONE;
        r.drawSpine = true;

        if (isTernary()) {
            r.tickType = LinearRuler.TickType.V;
        }

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

        removeDecoration(dap.d);
        addDecoration(r);
        setSelection(dap.handle);
    }

    public void addCircle() {
        ArcDecoration r = new ArcDecoration(new ArcInterp2D());
        DecorationAndPoints dap = convertSelection(r, true);
        if (dap == null)
            return;

        r.setCurve(new ArcInterp2D(dap.points, true));
        removeDecoration(dap.d);
        addDecoration(r);
        setSelection(dap.handle);
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
        super.rename(axis, name);
    }

    @Override public void add(LinearAxis axis) {
        super.add(axis);
        editFrame.addVariable((String) axis.name);
    }

    @Override public void remove(LinearAxis axis) {
        LinearRuler ruler = getSelectedRuler();
        if (ruler != null && axis == ruler.axis) {
            clearSelection();
        }
        super.remove(axis);
        editFrame.removeVariable((String) axis.name);
    }

    public void addVariable() {
        String errorTitle = "Cannot add variable";
        CuspDecoration cdec = getSelectedCuspDecoration();
        int index = -1;
        if (cdec == null || cdec.getCurve().size() != 3
                || (index = ((Interp2DHandle) selection).index) == 1) {
            showError(
"To add a user variable, first select an endpoint of a curve consisting of three points "
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

            Interp2D path = cdec.getCurve();
            Point2D.Double p0 = path.get(0);
            Point2D.Double p1 = path.get(1);
            Point2D.Double p2 = path.get(2);
            if (index == 0) { // Swap the first and last points.
                Point2D.Double tmp = p0;
                p0 = p2;
                p2 = tmp;
            }

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
        redraw();
    }

    /** Toggle whether to show grid lines at round (x,y) values. */
    public void setShowGrid(boolean b) {
        showGrid = b;
        editFrame.setShowGrid(b);
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
        saveState();
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
        saveState();
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
                    ("The following diagram component(s) were not successfully "
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
            componentsSum += axis.applyAsDouble(fractions);
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
        (LinearAxis v1, DoubleUnaryOperator f1,
         LinearAxis v2, DoubleUnaryOperator f2,
         boolean addComments, int sigFigs) {
        Interp2D curve = getSelectedInterp2D();
        if (curve != null) {
            return toString(Arrays.asList(curve.getPoints()), v1, f1, v2, f2, sigFigs);
        }

        Label label = getSelectedLabel();
        if (label != null) {
            return toString(labelCoordinates(label.getText()),
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
                dog.setFunction(i, StandardDoubleUnaryOperator.TO_PERCENT);
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
            setClipboardString(s, false);
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
            ? importStringFromFile() : Stuff.getClipboardString();
        if (str == null) {
            return;
        }

        copyCoordinatesFromString(str,
                dig.getVariable(0, axes), dig.getFunction(0),
                dig.getVariable(1, axes), dig.getFunction(1));
    }

    /** Convert a String that is a list of lists of x,y coordinate
        pairs into an array of arrays of Point2D.Doubles. Each
        coordinate pair is separated by newlines, and each list of
        coordinate pairs is separated by blank lines. Lines may be
        terminated by comments that start with the character #. For
        example:

        "1,2.3\n2,5.7\n\n9.4,9.4 # comment"

        would be converted to [[(1,2.3), (2,5.7)], [(9.4, 9.4)]]
        (where the coordinate pairs represent Point2D.Double objects).
    */
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

    public void copyCoordinatesFromString(String lines,
            LinearAxis v1, DoubleUnaryOperator f1,
            LinearAxis v2, DoubleUnaryOperator f2) {

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
        boolean haveCurve = (selection instanceof CuspDecoration);

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
                    clearSelection();
                }
                for (Point2D.Double point: curve) {
                    click(point);
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
                dog.setFunction(i, StandardDoubleUnaryOperator.FROM_PERCENT);
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
        saveState();
        super.expandMargins(factor);
        bestFit();
    }

    @Override public void computeMargins(boolean onlyExpand) {
        saveState();
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
        setClipboardString(HtmlToText.htmlToText(principalToPrettyString(mprin)), false);
    }

    /** If a label is selected, then copy it to the clipboard. If
        nothing is selected, copy all text to the clipboard. */
    public void copyTextToClipboard() {
        Label label = getSelectedLabel();
        StringBuilder res = new StringBuilder();
        if (label != null) {
            res.append(HtmlToText.htmlToText(label.getText()));
        } else {
            for (String s: getAllText()) {
                res.append(s);
                res.append("\n");
            }
        }
        setClipboardString(res.toString(), false);
    }

    public void copyAllFormulasToClipboard() {
        StringBuilder res = new StringBuilder();
        for (String s: getAllFormulas()) {
            res.append(s);
            res.append("\n");
        }
        setClipboardString(res.toString(), false);
    }

    public void addTieLine() {
        if (tieLineDialog.isVisible()) {
            tieLineCornerSelected();
        } else {
            tieLineCorners.clear();
            tieLineDialog.getLabel().setText(tieLineStepStrings[0]);
            tieLineStep();
            tieLineDialog.pack();
            tieLineDialog.setVisible(true);
            tieLineDialog.toFront();
        }
    }

    @Override List<DecorationHandle> getHandles(Decoration d,
            DecorationHandle.Type type) {
        DecorationHandle.Type type2 =
            (selection != null && d == selection.getDecoration())
            ? DecorationHandle.Type.CONTROL_POINT : type;
        return Arrays.asList(d.getHandles(type2));
    }

    final private Point2D.Double mousePage() {
        return (mprin == null) ? null : principalToStandardPage.transform(mprin);
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

        Point2D.Double pagePoint;

        if (!select) {
            pagePoint = pageLocation(getAutoPositionHandle(null, true));
            if (pagePoint == null) {
                return;
            }
        } else {
            boolean haveFocus = (principalFocus != null);
            ArrayList<DecorationHandle> hands = nearestHandles(DecorationHandle.Type.CONTROL_POINT);
            if (hands.isEmpty()) {
                return;
            }

            DecorationHandle sel = hands.get(0);

            if (selection != null && haveFocus) {
                // Check if the old selection is one of the nearest
                // hands. If so, then choose the next one after it. This
                // is to allow users to cycle through a set of overlapping
                // key hands using the selection key. Select once for the
                // first item; select it again and get the second one,
                // then the third, and so on.

                int matchCount = 0;
                for (DecorationHandle handle: hands) {
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
                    for (DecorationHandle handle: hands) {
                        if (matchCount == 0 || handle.equals(selection)) {
                            System.err.println(handle);
                        }
                    }
                    throw new IllegalStateException(error);
                } else {
                    for (int i = 0; i < hands.size() - 1; ++i) {
                        if (selection.equals(hands.get(i))) {
                            sel = hands.get(i+1);
                            break;
                        }
                    }
                }
            }

            if (sel == null)
                return;

            setSelection(sel);

            if (sel instanceof Interp2DHandle) {
                Interp2DHandle hand = (Interp2DHandle) sel;
                // You're more likely to want to add a point before
                // vertex #0 than to insert a point between vertex #0
                // and vertex #1.
                insertBeforeSelection = (hand.getIndex() == 0)
                    && (hand.getCurve().size() >= 2);
            }

            pagePoint = sel.getLocation(principalToStandardPage);
        }

        moveMouse(standardPageToPrincipal.transform(pagePoint));
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

        Rectangle2D principalBounds = getPrincipalBounds();

        Point2D.Double p1 = null;
        Point2D.Double p2 = null;

        BoundedParam2D param = getSelectedParameterization();
        if (param != null && param.isLineSegment()) {
            p1 = param.getStart();
            p2 = param.getEnd();
        }

        double v1 = isX ? principalBounds.getX() : principalBounds.getY();
        double v2 = isX ? principalBounds.getX() + principalBounds.getWidth()
            : principalBounds.getY() + principalBounds.getHeight();
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

        saveState();
        invisiblyTransformPrincipalCoordinates(xform);
        resetPixelModeVisible();
        propagateChange();
    }

    /** Scale both axes by the same amount. */
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

        saveState();
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

        setDiagramComponentGUI(side, str);
    }

    void setSwapXYVisible() {
        setSwapXYVisible(swapXYShouldBeVisible());
    }

    boolean swapXYShouldBeVisible() {
        if (isTernary()) {
            return false;
        }
        for (String s: diagramComponents) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }

    @JsonIgnore void setSwapXYVisible(boolean b) {
        setVisible(editFrame.actSwapXY, b);
    }


    @Override public void setDiagramComponent(Side side, String str)
        throws DuplicateComponentException {
        super.setDiagramComponent(side, str);
        setSwapXYVisible();
    }

    /** Like setDiagramComponent(Side, String), but show errors to the
        user instead of throwing exceptions. */
    void setDiagramComponentGUI(Side side, String str) {
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
        addLabel(null);
    }

    public void addLabel(String str) {
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
        if (str != null) {
            dog.setText(str);
        }
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
        Label label = dog.showModal();
        editFrame.toFront();
        if (label == null || !check(label)) {
            return;
        }

        saveState();
        label.setAngle(pageToPrincipalAngle(label.getAngle()));
        label.setX(x);
        label.setY(y);
        label.setColor(color);
        decorations.add(label);
        setSelection(new LabelHandle(label, LabelHandle.Type.ANCHOR));
        moveMouse(new Point2D.Double(x,y));
        setMouseStuck(true);
        propagateChange();
    }

    public boolean check(Label label) {
        if (label.getScale() <= 0) {
            showError("Font size is not a positive number");
            return false;
        }
        return true;
    }

    public void edit(Label label) {
        Label newLabel = null;
        LabelDialog dog = getLabelDialog();
        dog.setTitle("Edit Label");
        Label tmp = (Label) label.clone();
        tmp.setAngle(principalToPageAngle(label.getAngle()));
        dog.set(tmp);
        newLabel = dog.showModal();
        editFrame.toFront();
        if (newLabel == null || !check(newLabel)) {
            return;
        }
        if (newLabel.getText().isEmpty()) {
            removeDecoration(label);
            return;
        }

        newLabel.setX(label.getX());
        newLabel.setY(label.getY());
        newLabel.setColor(label.getColor());
        label.copyFrom(newLabel);
        label.setAngle(pageToPrincipalAngle(newLabel.getAngle()));
        moveMouse(label.getLocation());
        setMouseStuck(true);
        propagateChange();
    }

    /** Invoked from the EditFrame menu */
    public void setSelectedLineStyle(StandardStroke lineStyle) {
        setLineStyle(lineStyle);
        if (selection != null) {
            selection.getDecoration().setLineStyle(lineStyle);
            propagateChange();
        }
    }

    /** Invoked from the EditFrame menu */
    public void setLineStyle(StandardStroke lineStyle) {
        this.lineStyle = lineStyle;
        if (lineStyle != null) {
            this.fill = null;
        }
    }

    /** Return a value that's small compared to the page size but
        still big enough to avoid getting swallowed by loss of
        precision. */
    double pagePrecision() {
        return 1e-10 * (Math.abs(pageBounds.width) + Math.abs(pageBounds.height)
                + Math.abs(pageBounds.x) + Math.abs(pageBounds.y));
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
        if (principalToStandardPage == null) {
            return false;
        }

        Point spoint = viewPosition(prin);

        if (spoint != null) {
            if (mouseDragTravel != null) {
                mouseDragTravel.jumpTo(spoint.x, spoint.y);
            }
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
        if (selection instanceof Interp2DHandle2) {
            setSelection(((Interp2DHandle2) selection).indexHandle());
        }
        mprin = principalLocation(selection);
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

    /** @return a list of all DecorationHandles in order of their
        distance from principalFocus (if not null) or mprin (otherwise). */
    ArrayList<DecorationHandle> nearestHandles(DecorationHandle.Type type) {
        if (mprin == null) {
            return new ArrayList<>();
        }
        if (principalFocus == null) {
            principalFocus = mprin;
        }

        return nearestHandles(principalFocus, type);
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

        Point2D.Double mousePage = mousePage();
        DecorationDistance dist = nearestCurve(mousePage);

        if (dist == null) {
            return;
        }

        Decoration dec = dist.decoration;
        CurveDistance minDist = dist.distance;
        double t = minDist.t;

        if (select) {
            Interp2DHandle2 hand = toHandle(dist);
            setSelection(hand);
            insertBeforeSelection = hand.beforeThis;
        }
        moveMouse(standardPageToPrincipal.transform(minDist.point));
        setMouseStuck(true);
        showTangent(dec, t);
        redraw();
    }

    Interp2DHandle2 toHandle(DecorationDistance dist) {
        double t = dist.distance.t;
        return new Interp2DHandle2(
                (Interp2DDecoration) dist.decoration,
                dist.pageCurve.info(t),
                standardPageToPrincipal.transform(
                        dist.pageCurve.getLocation(t)));
}

    /** Toggle the closed/open status of the currently selected
        curve. */
    public void toggleCurveClosure() {
        try (SelectSomething ss = new SelectSomething()) {
            if (selection == null) return;

            Decoration d = selection.getDecoration();
            if (d instanceof CurveCloseable) {
                try {
                    toggleCurveClosure((CurveCloseable) d);
                } catch (IllegalArgumentException x) {
                    showError(x.toString());
                }
            }
        }
    }

    public String mimeType() {
        return "application/x-pededitor";
    }

    boolean setFileAssociations() throws UnavailableServiceException {
        return setFileAssociations
            (mimeType(), launchPEDFileExtensions());
    }

    /** @return true if the file associations were set (apparently)
        successfully. */
    boolean setFileAssociations(String mime, String[] exts) throws UnavailableServiceException {
        try {
            IntegrationService is
                = (IntegrationService) ServiceManager.lookup("javax.jnlp.IntegrationService");
            // It appears that the return value of
            // requestAssociation() is unreliable for Macs. It appears
            // that a theoretically redundant subsequent call to
            // hasAssocation() actually does improve trustworthiness.
            return is.requestAssociation(mime, exts)
                && is.hasAssociation(mime, exts);
        } catch (NullPointerException x) {
            Stuff.showError(null,
                            "JNLP startup failure: null pointer exception in requestAssociation(). "
                            + "This may be caused by an existing " + fallbackTitle()
                            + "process that is no longer current or erased from the cache. "
                            + "Restart your computer and try again.",
                            "JNLP Startup failure");
            System.exit(2);
            return false;
        }
    }

    /** Standard successful installation message. */
    String successfulAssociationMessage() {
        return fallbackTitle() + " has been installed. At any time, you can " +
            "uninstall, run, or create a shortcut for it by opening the Java Control " +
            "Panel's General tab and and pressing the \"View...\" " +
            "button.";
    }

    /** Installation message if setting file associations fails on an
        OS where it might be expected to succeed (e.g. Windows). */
    String failedAssociationMessage(boolean haveOptions) {
        return fallbackTitle() + " could not register as the handler for "
            + "PED diagrams (.PED files).";
    }

    public static String PROGRAM_TITLE = "PED Editor";

    public static void main(BasicEditorCreator ec, String[] args) {
        String programTitle = ec.getProgramTitle();
        if (args.length == 1 && "-help".equals(args[0])) {
            printHelp();
            System.exit(2);
        }

        BasicEditorArgsRunnable bear = new BasicEditorArgsRunnable(ec, args);
        if (waitDialog == null) {
            waitDialog = new WaitDialog(bear, "Loading " + programTitle + "...");
            waitDialog.setTitle(programTitle);
            waitDialog.pack();
            waitDialog.setVisible(true);
        } else {
            bear.run();
        }
    }

    /** Launch the application. */
    public static void main(String[] args) {
        main(new BasicEditorCreator(), args);
    }

    @Override public void cropPerformed(CropEvent e) {
        try (UpdateSuppressor us = new UpdateSuppressor()) {
                diagramType = e.getDiagramType();
                newDiagram(e.filename,
                        Geom.toPoint2DDoubles(e.getVertices()));
                initializeGUI();
                saveState();
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
            lastSaveHashCode = autoSaveHashCode = diagramHashCode();
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
        saveState();
        propagateChange1();
    }
    
    void startFileSaver() {
        if (isEditable() && fileSaver == null) {
            fileSaver = new Timer("FileSaver", true);
            fileSaver.schedule(new FileSaver(), AUTO_SAVE_DELAY, AUTO_SAVE_DELAY);
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
                SourceImage image = null;
                if (tracing) {
                    image = new SourceImage();
                    image.setFilename(originalFilename);
                    addDecoration(image);
                    setImageAlpha(StandardAlpha.LIGHT_GRAY.getAlpha());
                }
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
                            image.setTransform(new QuadToQuad(vertices,
                                            outputVertices));
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
                        setPrincipalToStandardPage(new TriangleTransform
                            (new Point2D.Double[]
                                { new Point2D.Double(minRight, minTop),
                                  new Point2D.Double(minRight,
                                                     maxRight - minRight + minTop),
                                  new Point2D.Double(maxRight, minTop) },
                             trianglePagePositions));

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
                            image.setTransform(q);
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
                        setPrincipalToStandardPage(new RectangleTransform
                            (domain,
                             new Rectangle2D.Double
                             (0, r.height, r.width, -r.height)));

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
                            image.setTransform(new TriangleTransform(
                                            vertices, trianglePoints));
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

                        setPrincipalToStandardPage(new TriangleTransform
                            (trianglePoints, xformed));

                        for (Axis axis: getAxes()) {
                            setPercentageDisplay(axis, isPercent);
                        }

                        break;
                    }
                case TERNARY:
                    {
                        if (tracing) {
                            image.setTransform(new TriangleTransform(vertices,
                                            principalTrianglePoints));
                        }

                        r = new Rescale(1.0, 0.0, maxDiagramWidth,
                                        TriangleTransform.UNIT_TRIANGLE_HEIGHT,
                                        0.0, maxDiagramHeight);
                        Point2D.Double[] trianglePagePositions =
                            { new Point2D.Double(0.0, r.height),
                              new Point2D.Double(r.width/2, 0.0),
                              new Point2D.Double(r.width, r.height) };
                        setPrincipalToStandardPage(new TriangleTransform
                                (principalTrianglePoints, trianglePagePositions));

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
        showError("The maximum amount of " + component + ", "
                  + String.format("%.2f%%", max * 100) + ", "
                  + "does not exceed the minimum amount "
                  + String.format("%.2f%%", min * 100) + ".");
    }

    protected double rulerFontSize() {
        return normalRulerFontSize() * lineWidth / STANDARD_LINE_WIDTH;
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
        editFrame.setPixelModeVisible(maybePixelMode());
    }

    @Override protected void initializeDiagram() {
        super.initializeDiagram();
        editFrame.setAspectRatio.setEnabled(!isTernary());
        editFrame.setTopComponent.setEnabled(isTernary());
        editFrame.mnSwap.setVisible(isTernary());
        setSwapXYVisible();
        setVisible(editFrame.swapBinary, diagramType == DiagramType.BINARY);
        editFrame.scaleBoth.setEnabled(!isTernary());
        resetPixelModeVisible();
        bestFit();
    }

    protected void resizeEditFrame(int otherEditorCnt) {
        Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();
        Dimension size = rect.getSize();
        size.width = editFrame.getPreferredSize().width;
        size.height = Math.max(100, size.height - 15 * otherEditorCnt);
        editFrame.setPreferredSize(size);
    }

    protected void initializeGUI() {
        // Force the editor frame image to be initialized.

        if (!editorIsPacked) {
            int otherEditorCnt = getOpenEditorCnt();
            resizeEditFrame(otherEditorCnt);
            editFrame.pack();
            editorIsPacked = true;
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
        editFrame.setHideImages(!printImagesPreference());
        SourceImage image = firstImage();
        if (image != null) {
            editFrame.setAlpha(image.getAlpha());
        }
        editFrame.setHideImagesVisible(image != null);
        setColor(color);
        bestFit();
    }

    @JsonIgnore public boolean isSaveNeeded() {
        if (!isEditable() || principalToStandardPage == null) {
            return false;
        }
        int stateHash = diagramHashCode();
        return stateHash != lastSaveHashCode;
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
                     htmlify("Enter the width-to-height ratio for the diagram."),
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

        setAspectRatio(aspectRatio);
        bestFit();
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
        setPageBounds(addMargins(sbounds, defaultRelativeMargin()));
    }

    private Rectangle2D selectedPageRectangle() {
        CuspDecoration d = getSelectedCuspDecoration();
        if (d == null) {
            return null;
        }
        CuspInterp2D curve = d.getCurve();
        if (curve.size() != 4 || curve.smoothedPointCnt() > 0) {
            return null;
        }
        Point2D.Double[] points = curve.getPoints();
        principalToStandardPage.transform(points, 0, points, 0, points.length);
        return Geom.bounds(points);
    }

    Rectangle2D pageOrRectangleBounds() {
        Rectangle2D r = selectedPageRectangle();
        return (r == null) ? pageBounds : r;
    }

    public void setMargins() {
        if (marginsDialog == null) {
            marginsDialog = new MarginsDialog(editFrame, 20);
        }
        MarginsDialog dog = marginsDialog;
        dog.fromPage = standardPageToPrincipal;
        dog.axes = axes;
        Rectangle2D bounds = pageOrRectangleBounds();

        Point2D.Double[] directions =
            { new Point2D.Double(1, 0),
              new Point2D.Double(0, 1) };
        LinearAxis[][] axes2 = new LinearAxis[2][];
        for (int varNo = 0; varNo < directions.length; ++varNo) {
            LinearAxis[] axes = getPageAxes(directions[varNo].x, directions[varNo].y);
            axes2[varNo] = axes;
            if (axes.length == 0) {
                String[] names = { "X", "Y" };
                showError("No variable corrsponds to the " + names[varNo]
                          + " axis, so it cannot be set.");
                return;
            }
            VariableSelector vs = dog.getVariableSelector(varNo);
            vs.setAxes(Arrays.asList(axes));
            LinearAxis ax = vs.getSelected(Arrays.asList(axes));
            double[] range = getRange(ax, bounds);
            dog.getNumberField(varNo, 0).setValue(range[0]);
            dog.getNumberField(varNo, 1).setValue(range[1]);
        }

        if (!dog.showModal()) {
            return;
        }

        try {
            double[] rangeX = dog.pageXRange();
            double[] rangeY = dog.pageYRange();
            setPageBounds(new Rectangle2D.Double(rangeX[0], rangeY[0],
                            rangeX[1] - rangeX[0], rangeY[1] - rangeY[0]));
            bestFit();
        } catch (NumberFormatException x) {
            showError(x.toString());
            return;
        }
    }

    @Override public void openDiagram(File file) throws IOException {
        super.openDiagram(file);
        markAsSaved();
        startFileSaver();
        initializeGUI();
    }

    @Override public void openDiagram(String jsonString) throws IOException {
        super.openDiagram(jsonString);
        initializeGUI();
    }

    /** Invoked from the EditFrame menu.

        @throws IOException */
    public void submit() {
        String url = get("saveURL");
        if (url == null)
            return;
        try {
            JsonPostDiagram.sendBytes(url,
                    JsonPostDiagram.encodeForHttp("diagram", toJsonString()));
            JOptionPane.showMessageDialog(editFrame,
                    "Diagram submitted successfully.");
        } catch (IOException x) {
            showError("File submit error: " + x);
        }

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
        boolean isSubmit = isSubmit();
        editFrame.mnSubmit.setVisible(isSubmit);
        editFrame.actSubmit.setEnabled(isSubmit);
        clearSelection();
    }

    boolean isSubmit() {
        return get("saveURL") != null;
    }

    @Override public BufferedImage getOriginalImage() {
        try {
            return super.getOriginalImage();
        } catch (IOException x) {
            return null;
        }
    }

    /** If the zoom frame is not needed, then then make sure it's null
        or invisible. Otherwise, make sure the zoom frame is non-null,
        initialized, visible, and shows the correct image. */
    void revalidateZoomFrame() {
        SourceImage image = firstImage();
        if (image != null && image.getImage() != null) {
            BufferedImage bi = image.getImage();
            editFrame.setBackgroundTypeEnabled(true);
            initializeZoomFrame();
            zoomFrame.setImage(bi);
            initializeCrosshairs();
            zoomFrame.getImageZoomPane().crosshairs = crosshairs;
            editFrame.mnBackgroundImage.setEnabled(true);
            zoomFrame.setTitle
                ("Zoom " + image.getFilename());
            zoomFrame.pack();
            zoomFrame.setVisible(true);
        } else {
            if (zoomFrame != null) {
                zoomFrame.setVisible(false);
            }
            if (!tracingImage()) {
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

    /** Return all PED type file extensions this program can open. */
    public String[] pedFileExtensions() {
        return new String[] {"ped"};
    }

    /** Return all PED type file extensions this program should take ownership of. */
    public String[] launchPEDFileExtensions() {
        return pedFileExtensions();
    }

    public File[] openPEDFilesDialog(Component parent) {
        String what = String.join("/", Arrays.asList(pedFileExtensions())).toUpperCase();
        return openFilesDialog(parent,
                "Open " + what + "  File", what + " files",
                pedFileExtensions());
    }

    static Preferences getPreferences() {
        return Preferences.userNodeForPackage(BasicEditor.class);
    }

    /** Return the default directory to save to and load from. */
    public static String getCurrentDirectory() {
        return getPreferences().get(PREF_DIR,  null);
    }

    /** Set the default directory to save to and load from. */
    public static void setCurrentDirectory(String dir) {
        getPreferences().put(PREF_DIR,  dir);
    }

    /** Return the default directory to save to and load from. */
    public static String getCurrentFile() {
        return getPreferences().get(PREF_FILE,  null);
    }

    /** Set the default directory to save to and load from. */
    public static void setCurrentFile(String dir) {
        getPreferences().put(PREF_FILE,  dir);
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
        String file = getCurrentFile();
        if (file != null) {
            chooser.setSelectedFile(new File(file));
        }
       chooser.setFileFilter
            (new FileNameExtensionFilter("PED and image files", allExts));
       chooser.addChoosableFileFilter
           (new FileNameExtensionFilter("PED files only", pedExts));
       chooser.addChoosableFileFilter
           (new FileNameExtensionFilter("Image files only", imageExts));
       if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
           File[] files = chooser.getSelectedFiles();
           if (files != null) {
               setCurrentDirectory(files[0].getParent());
               setCurrentFile(files[0].toString());
           }
           return files;
       } else {
           return null;
       }
    }

    public File[] openFilesDialog(Component parent, String title,
            String filterName, String[] suffixes) {
        Preferences prefs = Preferences.userNodeForPackage(CropFrame.class);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setMultiSelectionEnabled(true);
        String dir = prefs.get(PREF_DIR,  null);
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
        String file = getCurrentFile();
        if (file != null) {
            chooser.setSelectedFile(new File(file));
        }
        chooser.setFileFilter
            (new FileNameExtensionFilter(filterName, suffixes));
        while (true) {
            if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                File[] files = chooser.getSelectedFiles();
                if (files != null) {
                    setCurrentDirectory(files[0].getParent());
                    setCurrentFile(files[0].toString());
                }
                for (int i = 0; i < files.length; ++i) {
                    files[i] = addExtensionIfMissing(files[i]);
                }
                if (fileNotFound(files, parent)) {
                    continue;
                }
                return files;
            } else {
                return null;
            }
        }
    }


    public File[] openImageFilesDialog(Component parent) {
        return openFilesDialog(parent, "Open Image File", "Image files",
                ImageIO.getReaderFileSuffixes());
    }

    public void open() {
        showOpenDialog(editFrame);
    }

    /**
     * If set to false, SourceImage decorations are not displayed when
     * printing, nor are they included in image files. */
    @JsonIgnore public void setPrintImages(boolean v) {
        getPreferences().putBoolean(SHOW_IMAGES, v);
    }

    @JsonIgnore @Override public boolean isPrintImages() {
        return !editFrame.isHideImages();
    }

    private boolean printImagesPreference() {
        return getPreferences().getBoolean(SHOW_IMAGES, true);
    }

    public void monitor() {
        if (watchNewFiles == null || !watchNewFiles.isEnabled()) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select directory to monitor for new diagram files");
            String dir = getCurrentDirectory();
            File dirFile;
            if (dir == null) {
                String home = System.getProperty("user.home");
                if (home != null) {
                    dir = Paths.get(home, "Downloads").toString();
                }
            }
            if (dir != null) {
                dirFile = new File(dir);
                // This shouldn't be necessary, and it doesn't even
                // work correctly, but there's a bug that causes
                // directory selection to initially select the wrong directory:
                // https://bugs.openjdk.java.net/browse/JDK-7110156 .
                chooser.setCurrentDirectory(dirFile.getParentFile());
                chooser.setSelectedFile(dirFile);
            }
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(getEditFrame())
                == JFileChooser.APPROVE_OPTION) {
                dirFile = chooser.getSelectedFile();
                setCurrentDirectory(dirFile.toString());
                String[] exts = pedFileExtensions();
                watchNewFiles = new WatchNewFiles
                    (dirFile.toPath(), exts,
                     path -> showOpenDialog(getEditFrame(), path.toFile()));
                try {
                    watchNewFiles.start();
                    JOptionPane.showMessageDialog
                        (editFrame,
                         htmlify(dirFile + " will be monitored for newly "
                                 + "created files with extension '"
                                 + String.join("' or '", exts) + "'"));
                } catch (IOException x) {
                    showError("Could not watch directory: " + x);
                }
            }
        } else {
            watchNewFiles.stop();
            watchNewFiles = null;
            JOptionPane.showMessageDialog
                (editFrame, htmlify("The existing directory monitor has been stopped."));
        }
    }

    private static boolean fileNotFound(File[] files, Component parent) {
        if (files != null && files.length == 1 && Files.notExists(files[0].toPath())) {
            Stuff.showError(parent,
                    "File not found: " + files[0],
                    "File not found");
            return true;
        }
        return false;
    }

    public void showOpenDialog(Component parent) {
        while (true) {
            File[] filesList = isEditable() ? openPEDOrImageFilesDialog(parent)
                : openPEDFilesDialog(parent);
            for (int i = 0; i < filesList.length; ++i) {
                filesList[i] = addExtensionIfMissing(filesList[i]);
            }
            if (fileNotFound(filesList, parent)) {
                continue;
            }
            showOpenDialog(parent, filesList);
            break;
        }
    }

    File addExtensionIfMissing(File file) {
        String ext = getExtension(file.getName());
        if (ext != null) {
            return file;
        }
        if (Files.notExists(file.toPath())) {
            String[] exts = pedFileExtensions();
            String ext2 = exts[0];
            return new File(file.getAbsolutePath() + "." + ext2);
        }
        return file;
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
        String[] exts = pedFileExtensions();
        file = addExtensionIfMissing(file);
        boolean ped = isPED(file);

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
                    Stuff.showError(parent, buf.toString(), "File load error");
                    closeIfNotUsed();
                }
            }
        } catch (IOException x) {
            Stuff.showError(parent, "Could not load file: " + x,
                      "File load error");
            closeIfNotUsed();
        }
        saveState();
    }

    void showError(String mess, String title) {
        Stuff.showError(editFrame, mess, title);
    }

    void showError(String mess) {
        Stuff.showError(editFrame, mess, "Cannot perform operation");
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
        int flags = drawFlags();
        if (maybeTransparent && dog.isTransparent()) {
            flags |= FLAG_TRANSPARENT;
        }

        try {
            saveAsImage(file, ext, size.width, size.height, flags);
        } catch (IOException x) {
            showError("File error: " + x);
        } catch (OutOfMemoryError x) {
            showError("Out of memory. You may either re-run Java with a "
                      + "larger heap size or try saving as a smaller image.");
        }
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
            markAsSaved();
            return true;
        } else {
            return false;
        }
    }

    private int diagramHashCode() {
        if (!isEditable()) {
            return 0;
        }
        try {
            int res = EditorState.toStringAndTransientState(this).str.hashCode();
            return res;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
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
        Interp2DHandle vh = getInterp2DHandle();
        if (vh != null) {
            Interp2DDecoration dec = vh.getDecoration();
            int size = dec.getCurve().size();
            if (size < 2) {
                return null;
            }
            int vertexNo = vh.getIndex() + (insertBeforeSelection ? -1 : 1);
            if (dec.getCurve().isClosed()) {
                vertexNo = (vertexNo + size) % size;
            } else if (vertexNo < 0) {
                vertexNo = 1;
            } else if (vertexNo >= size) {
                vertexNo = size - 2;
            }
            return dec.createHandle(vertexNo);
        }
        return null;
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
        DecorationHandle h = getAutoPositionHandle(ap, false);
        return (h == null) ? null : principalLocation(h);
    }

    /** Return a DecorationHandle for the point in principal
        coordinates that auto-positioning would move the mouse to -- a
        nearby key point if possible, or a point on a nearby curve
        otherwise, or where the mouse already is otherwise, or null if
        the mouse is outside the panel containing the diagram. The
        DecorationHandle will be a normal DecorationHandle if it is a
        key point with exactly one decoration handle at that location;
        it will be an Interp2DHandle2 if it is a random location on a
        nearby curve; and it will be a NullDecorationHandle if it is a
        key point associated with zero or two or more decoration
        handles, or if nothing special was found nearby.

        @param ap If not null, ap.position will be set to
        AutoPositionType.NONE, AutoPositionType.CURVE, or
        AutoPositionType.POINT to reflect whether the autoposition is
        the regular mouse position, the nearest curve, or the nearest
        key point.

        @param keypointsOnly If true, return the closest key point,
        never mind how far away it may be.
    */
    DecorationHandle getAutoPositionHandle(AutoPositionHolder ap,
            boolean keypointsOnly) {
        return getAutoPositionHandle(ap, keypointsOnly, getMousePrincipal());
    }

    DecorationHandle getAutoPositionHandle(AutoPositionHolder ap,
            boolean keypointsOnly, Point2D mprin2) {
        double maxMovePixels = 50; // Maximum number of pixels to
        if (ap == null) {
            ap = new AutoPositionHolder();
        }
        ap.position = AutoPositionType.NONE;

        if (mprin2 == null) {
            return (mprin == null) ? null : new NullDecorationHandle(mprin);
        }
        Point2D.Double mousePage = principalToStandardPage.transform(mprin2);

        DecorationHandle res = null;
        Point2D.Double newPage = null;
        double pageDist = 1e100;

        int oldSize = decorations.size();

        try (UpdateSuppressor us = new UpdateSuppressor()) {
                ArrayList<Point2D> selections = new ArrayList<>();

                Point2D.Double selPoint = null;
                if (selection != null) {
                    // For autopositioning purposes, ignore the
                    // precise spot we picked and focus on the
                    // highlighted vertex.
                    DecorationHandle d = (selection instanceof Interp2DHandle2)
                        ? ((Interp2DHandle2) selection).indexHandle()
                        : selection;
                    selPoint = principalLocation(d);
                    if (selPoint != null) {
                        selections.add(selPoint);
                    }
                }
                Point2D.Double point2 = principalLocation(secondarySelection());
                if (point2 != null) {
                    selections.add(point2);
                    double dx = selPoint.x - point2.x;
                    double dy = selPoint.y - point2.y;

                    if (!(selection.getDecoration() instanceof ArcDecoration)) {
                        Point2D[] diameter = {
                            point2,
                            new Point2D.Double(selPoint.x + dx, selPoint.y + dy) };
                        addDecoration(new ArcDecoration(
                                        new ArcInterp2D(Arrays.asList(diameter))));
                    }

                    double distPixels = point2.distance(selPoint) * scale;
                    if (distPixels > maxMovePixels * 2) {
                        // Add the line through the midpoint of (selPoint, point2).
                        Point2D.Double midpoint = Geom.midpoint(selPoint, point2);
                        Point2D[] midpointLine = { midpoint,
                                                   new Point2D.Double(midpoint.x - dy, midpoint.y + dx) };
                        principalToStandardPage.transform(midpointLine, 0, midpointLine, 0,
                                midpointLine.length);
                        Line2D.Double pageSeg = pageSegmentToLine(new Line2D.Double(
                                        midpointLine[0], midpointLine[1]));
                        midpointLine[0] = pageSeg.getP1();
                        midpointLine[1] = pageSeg.getP2();
                        standardPageToPrincipal.transform(midpointLine, 0, midpointLine, 0,
                                midpointLine.length);
                        addDecoration(new CuspDecoration(
                                        new CuspInterp2D(Arrays.asList(midpointLine), false, false)));
                    }
                }

                for (Point2D p: selections) {
                    principalToStandardPage.transform(p, p);
                    Line2D.Double gridLine = nearestGridLine(
                            new Line2D.Double(p, mousePage));
                    if (gridLine == null) {
                        continue;
                    }
                    gridLine = Geom.transform(standardPageToPrincipal, gridLine);
                    addDecoration(new CuspDecoration(new CuspInterp2D(gridLine),
                                         StandardStroke.INVISIBLE, 0));
                }

                ArrayList<DecorationHandle> hands = keyPointHandles(
                        DecorationHandle.Type.SELECTION);
                res = nearest(hands, mousePage);
                if (res != null) {
                    newPage = pageLocation(res);
                    pageDist = mousePage.distance(newPage);
                }

                final double OVERLAP_DISTANCE = 1e-10;
                int parameterizableCnt = 0;
                for (DecorationHandle h: hands) {
                    Point2D.Double pagePt = pageLocation(h);
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
                            res = new NullDecorationHandle(principalLocation(h));
                            break;
                        } else {
                            // If there's only one parameterizable
                            // handle, use it.
                            res = h;
                        }
                    }
                }

                if (!keypointsOnly) {
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
                        res = toHandle(nc);
                        newPage = nc.distance.point;
                        pageDist = nc.distance.distance;
                    }

                    // move the mouse
                    if (newPage == null
                            || pageDist * scale > maxMovePixels) {
                        ap.position = AutoPositionType.NONE;
                        newPage = mousePage; // Leave the mouse where it is.
                        res = new NullDecorationHandle(mprin2);
                    }
                }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        } finally {
            while (decorations.size() > oldSize) {
                removeDecoration(decorations.get(decorations.size()-1));
            }
        }

        if (res != null) {
            int layer = getLayer(res.getDecoration());
            if (layer == -1) {
                // Don't return handles to temporary decorations!
                return new NullDecorationHandle(principalLocation(res));
            }
        }

        return res;
    }

    /** Invoked from the EditFrame menu */
    public void autoPosition() {
        if (mouseIsStuck) {
            setMouseStuck(false);
        }

        DecorationHandle h = getAutoPositionHandle(null, false);
        if (h != null) {
            moveMouse(principalLocation(h));
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
                dog.setValue(i, axes[i].applyAsDouble(mprin));
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
            mouseStickTravel = null;
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
        redraw();
    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() != MouseEvent.BUTTON1) {
           if (e.isShiftDown()) {
              mprin = getAutoPosition();
           }
           showPopupMenu(new MousePress(e,mprin));
        } else {
            Point2D.Double p = getVertexAddMousePosition(e.getPoint());
            Point2D.Double pIfDragging = p;
            if (e.isShiftDown()) {
                DecorationHandle h = getAutoPositionHandle(null, false);
                if (h == null)
                    return;
                p = principalLocation(h);
            } else {
                if (isPixelMode()) {
                    p = nearestGridPoint(p);
                }
            }
            if (p == null)
                return;
            mousePress = new MousePress(e,p);
            mousePress.prinIfDragging = pIfDragging;
            mouseDragTravel = mouseStickTravel = null;
        }
    }

    Point2D.Double getVertexAddMousePosition(Point panep) {
        if (mprin == null ||
            (getInterp2DHandle() != null &&
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
        click(mprin = getVertexAddMousePosition(getEditPane().getMousePosition()));
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

    /** The mouse was moved in the edit window. Update the coordinates
        in the edit window status bar, repaint the diagram, and update
        the position in the zoom window,. */
    @Override public void mouseMoved(MouseEvent e) {
        if (rightClick != null) {
            return;
        }
        isShiftDown = e.isShiftDown();
        if (mouseDragTravel == null) {
            mouseDragTravel = new MouseTravel(e.getX(), e.getY());
        } else {
            mouseDragTravel.travel(e);
        }
        if (mouseStickTravel == null) {
            mouseStickTravel = new MouseTravel(e.getX(), e.getY());
        } else {
            mouseStickTravel.travel(e);
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
        if (rightClick != null || (mousePress != null && !isDragging())) {
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
                || (mouseStickTravel != null
                    && mouseStickTravel.getTravel() >= MOUSE_UNSTICK_DISTANCE)) {
                mouseIsStuck = false;
                principalFocus = null;
                mprin = paneToPrincipal(mpos);
            }
        }

        if (mprin != null) {
            SourceImage image = selectedOrFirstImage();
            if (image != null && image.getImage() != null) {
                try {
                    // Update image zoom frame.

                    Point2D.Double orig = image.inverseTransform(mprin);
                    zoomFrame.setImageFocus((int) Math.floor(orig.x),
                                            (int) Math.floor(orig.y));
                } catch (UnsolvableException ex) {
                    // Ignore the exception
                }
            }
        }
    }

    /**
     * Return the current mouse position, or the position of the mouse
     * at the moment the button was pressed if the left or right mouse
     * buttons is still depressed or was just released and we're not
     * in the middle of a drag operation.
     */
    Point2D.Double getMousePrincipal() {
        if (mousePress != null && !isDragging()) {
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
            propagateChange();
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

    /** Return the view rectangle transformed to standard page space. */
    Rectangle2D.Double getStandardPageViewRect() {
        Rectangle r = getViewRect();
        Point2D.Double p = paneToStandardPage(new Point(r.x, r.y));
        Point2D.Double p2 = paneToStandardPage(new Point(r.x + r.width, r.y + r.height));
        return new Rectangle2D.Double(p.x, p.y, p2.x - p.x, p2.y - p.y);
    }

    Rectangle2D toPageRectangle(Point2D prin1, Point2D prin2) {
        Point2D.Double page1 = principalToStandardPage.transform(prin1);
        Point2D.Double page2 = principalToStandardPage.transform(prin2);
        return Geom.bounds(new Point2D.Double[] { page1, page2 });
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
                || !roughlyInside(mousePage(), pageBounds)) {
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

    void customLineWidth() {
        boolean firstTime = false;
        if (lineWidthDialog == null) {
            lineWidthDialog = new LineWidthDialog(editFrame);
            lineWidthDialog.setUserUnits(isPixelMode());
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
            double lw = dog.isUserUnits() ? getGridLineWidth() : lineWidth;
            dog.setValue(lw);
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

    /** Return the line width in user coordinate terms, or 0 if undefined. */
    double getGridLineWidth() {
        if (principalToStandardPage == null) {
            return 0;
        }
        Point2D.Double iv = new Point2D.Double(1.0, 0.0);
        principalToStandardPage.deltaTransform(iv, iv);
        return lineWidth / Geom.length(iv);
    }

    EditPane getEditPane() { return editFrame.getEditPane(); }

    public void nextFile() {
        if (!verifyCloseDiagram()) {
            return;
        }
        File file = filesList[++fileNo];
        String parent = file.getParent();
        if (parent != null) {
            setCurrentDirectory(parent);
        }
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

    // NO: Don't launch; ASK: Ask whether to launch; YES: launch no matter what.
    static enum LaunchType { NO, ASK, YES };

    public void run(String[] args) {
        // By default, ask the user whether to visibly launch the program or not.
        LaunchType launch = LaunchType.ASK;

        { // Crude command line processing
            ArrayList<String> res = new ArrayList<>();
            for (String s: args) {
                if ("-open".equals(s)) {
                    // Remove -open arguments, because if the PED
                    // Editor is opened because of a file association,
                    // its arguments have the form -open <file>.

                    // Do nothing.
                } else if ("-launch".equals(s)) {
                    // Launch the program -- don't give the user a
                    // chance to exit first.
                    launch = LaunchType.YES;
                } else if ("-exit".equals(s)) {
                    // Exit after setting file associations -- don't
                    // even ask the user whether to launch or not.
                    launch = LaunchType.NO;
                } else {
                    res.add(s);
                }
            }
            args = res.toArray(new String[0]);
        }

        if (args.length == 0) {
            if (isFileAssociationBroken()) {
                doFileAssociationsBrokenMessage();
                initializeGUI();
            } else {
                try {
                    boolean ok = setFileAssociations();
                    if (launch != LaunchType.YES) {
                        launch = doFileAssociationsMessage(ok, launch == LaunchType.ASK)
                            ? LaunchType.YES : LaunchType.NO;
                    }
                } catch (UnavailableServiceException x) {
                    launch = LaunchType.YES;
                }

                if (launch == LaunchType.YES) {
                    initializeGUI();
                    run();
                }
            }

            if (getOpenEditorCnt() == 0) {
                lastWindowClosed();
                closeWaitDialog();
            }
        } else {
            filesList = new File[args.length];
            for (int i = 0; i < args.length; ++i) {
                filesList[i] = new File(args[i]);
            }
            editFrame.mnNextFile.setVisible(true);
            nextFile();
        }
    }

    /**
       @param ok If true, file associations were set successfully.

       @param exitOnly If true, quit afterwards. If false, give users
       a choice whether to quit afterwards or not.

       @return true if the user asks to launch the application afterwards.
    */
    boolean doFileAssociationsMessage(boolean ok, boolean haveOptions) {
        String title;
        String mess;
        int messType = JOptionPane.PLAIN_MESSAGE;
        if (ok) {
            title = "Installation successful";
            mess = successfulAssociationMessage();
        } else {
            title = "Installation partly successful";
            Stuff.setFileAssociationBroken(true);
            mess = failedAssociationMessage(haveOptions);
        }
        if (haveOptions) {
            Object[] options = {"Run Now", "Finished"};
            int defaultIndex = ok ? 1 : 0;
            return JOptionPane.showOptionDialog
                (editFrame, Stuff.htmlify(mess), title,
                 JOptionPane.YES_NO_OPTION,
                 messType,
                 null, options, options[defaultIndex]) == JOptionPane.YES_OPTION;
        } else {
            JOptionPane.showMessageDialog
                (editFrame, Stuff.htmlify(mess), title, messType);
            return false;
        }
    }

    void doFileAssociationsBrokenMessage() {
        doFileAssociationsBrokenMessage
            ("At any time, you can " +
             "uninstall, run, or create a shortcut for this program by opening the Java Control " +
             "Panel's General tab and and pressing the \"View...\" button. ",
             fallbackTitle());
    }

    /** Standard installation message for OSes such Macintosh for
        which file associations don't work. For the PED Editor, that's
        not a big deal. */
    void doFileAssociationsBrokenMessage(String mess, String title) {
        JOptionPane.showMessageDialog(editFrame, Stuff.htmlify(mess), title,
                                      JOptionPane.PLAIN_MESSAGE);
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
        String fn;
        try {
            fn = new File(BasicEditor.class.getProtectionDomain().getCodeSource()
                          .getLocation().toURI().getPath()).toString();
        } catch (URISyntaxException e) {
            fn = "???.jar";
        }
        System.err.println("Usage: java -jar " + fn + " <options...>");
        System.err.println("    -launch: open file dialog");
        System.err.println("    -exit: attempt to set file associations, then quit.");
        System.err.println("    <filename>...: open these files.");
        System.err.println();
        System.err.println("If no command line arguments are given, attempt to set");
        System.err.println("file associations, then ask whether to exit or continue.");
    }

    @Override public void mouseClicked(MouseEvent e) {}

    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (isDragging()) {
                Point2D p1 = mousePress.prinIfDragging;
                Point2D p2 = mprin;
                if (!p1.equals(p2)) {
                    Rectangle2D pageRegion = toPageRectangle(p1, p2);
                    if (mousePress.e.isShiftDown()) {
                        if (isEditable()) {
                            selectPageRegion(pageRegion);
                        } else {
                            setPageBounds(pageRegion);
                            bestFit();
                        }
                    } else {
                        mprin = null; // Force the mouse to center itself.
                        zoom(pageRegion);
                        setMouseStuck(false);
                    }
                    mouseDragTravel = null;
                }
            } else if (isEditable() && principalToStandardPage != null
                       && mprin != null
                       && mousePress != null) {
                click(mprin = mousePress.prin);
                setMouseStuck(true);
            }
            mousePress = null;
        }
    }
    
    void selectPageRegion(Rectangle2D r) {
        Point2D.Double[] points = Geom.toPoint2DDoubles(r);
        standardPageToPrincipal.transform(points, 0, points, 0, points.length);
        CuspInterp2D curve = new CuspInterp2D(Arrays.asList(points), false, true);
        CuspDecoration d = emptyCuspDecoration();
        d.setCurve(curve);
        addDecoration(d);
        setSelection(d.createHandle(0));
    }

    @Override public void mouseEntered(MouseEvent e) {
    }

    @Override public void mouseExited(MouseEvent e) {
        if (rightClick == null) {
            mprin = null;
            redraw();
        }
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
        return "(" + getXAxis().applyAsString(prin) + ", "
            + getYAxis().applyAsString(prin) + ")";
    }

    void showPopupMenu(MousePress mp) {
        if (rightClick == null) {
            rightClick = mp;
            mnRightClick.setCoordinates(formatCoordinates(mp.prin));
        }
        mnRightClick.show(mp.e.getComponent(), mp.e.getX(), mp.e.getY());
    }

    public void redo() {
        if (undoStackOffset == undoStack.size()) {
            showError("No more operations to redo.");
            return;
        }
        try {
            EditorState.copyToEditor(this, undoStack.get(undoStackOffset));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            undoStackOffset++;
        }
    }

    void saveState() {
        if (!isEditable()) {
            return;
        }

        try {
            EditorState.StringAndTransientState state = EditorState.toStringAndTransientState(this);

            if (undoStackOffset > 0 &&
                    state.str.equals(undoStack.get(undoStackOffset - 1).str)) {
                // The state is already saved.
                return;
            }
            if (undoStackOffset < undoStack.size() &&
                    state.str.equals(undoStack.get(undoStackOffset).str)) {
                // Move the stack offset past the current state, which
                // is already saved.
                undoStackOffset++;
                return;
            }

            // Clear any items left to redo.
            while (undoStackOffset < undoStack.size()) {
                undoStack.remove(undoStack.size() - 1);
            }

            trimUndoStack();
            changesSinceStateSaved = 0;
            undoStack.add(state);
            ++ undoStackOffset;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ensure the undo stack doesn't get too big or too long.
     */
    private void trimUndoStack() {
        if (undoStack.size() >= 25) {
            undoStack.remove(1);
            if (undoStackOffset >= 1) {
                --undoStackOffset;
            }
        }
        int totalBytes = 0;
        for (int i = undoStack.size() - 1; i >= 1; --i) {
            if (totalBytes > 20_000_000) {
                undoStack.remove(i);
                if (undoStackOffset >= i) {
                    -- undoStackOffset;
                }
            }
            totalBytes += undoStack.get(i).str.length();
        }
    }

    public void undo() {
        try {
            saveState();
            if (undoStackOffset < 2) {
                showError("Cannot undo any more operations.");
                return;
            }
            EditorState.copyToEditor(this, undoStack.get(undoStackOffset - 2));
            --undoStackOffset;
        } catch (IOException e) {
            showError("This operation cannot be undone.");
            while (undoStackOffset > 0) {
                --undoStackOffset;
                undoStack.remove(0);
            }
        }
    }

    @Override public void addDecoration(int index, Decoration d) {
        super.addDecoration(index, d);
        if (d instanceof SourceImage) {
            revalidateZoomFrame();
        }
    }
}
