package gov.nist.pededitor;

import java.awt.*;

import javax.imageio.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.io.*;

// TODO Keep center of scroll pane fixed relative to the image when
// zooming into a new diagram.

// TODO Allow the equals key to be attracted to the intersection of
// curves, even when those intersections were not explicitly added to
// the diagram.

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class Editor implements CropEventListener, MouseListener,
                               MouseMotionListener {
    static protected Image crosshairs = null;

    protected CropFrame cropFrame = new CropFrame();
    protected EditFrame editFrame = new EditFrame(this);
    protected ImageZoomFrame zoomFrame = null;

    protected PolygonTransform originalToPrincipal;
    protected PolygonTransform principalToOriginal;
    protected Affine principalToStandardPage;
    protected Affine standardPageToPrincipal;
    protected Affine principalToScreen;
    protected Affine screenToPrincipal;
    protected Affine screenToStandardPage;
    protected Affine standardPageToScreen;
    protected Rectangle2D.Double pageBounds;
    protected Rectangle screenBounds;
    protected DiagramType diagramType = null;
    protected double scale;
    protected ArrayList<GeneralPolyline> paths;
    protected int activeCurveNo;
    protected int activeVertexNo;
    protected ArrayList<AxisInfo> axes;
    protected AxisInfo xAxis = null;
    protected AxisInfo yAxis = null;
    protected AxisInfo zAxis = null;
    protected AxisInfo pageXAxis = null;
    protected AxisInfo pageYAxis = null;

    /** Ignore the next mouseMoved() event. Useful when robot() is
        used to move the mouse to a preferred location, to avoid the
        precisely chosen coordinates being overwritten by the mouse
        pointer's crude approximation to the nearest pixel. */
    protected boolean leaveMprinAlone = false;

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
        principalToScreen = null;
        screenToPrincipal = null;
        screenToStandardPage = null;
        standardPageToScreen = null;
        pageBounds = null;
        screenBounds = null;
        scale = 800.0;
        paths = new ArrayList<GeneralPolyline>();
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

        getEditPane().repaint();
    }

    /** Cycle the currently active curve. */
    public void cycleActiveCurve() {
        if (activeCurveNo == -1) {
            // Nothing to do.
            return;
        }

        --activeCurveNo;
        if (activeCurveNo == -1) {
            activeCurveNo = paths.size() - 1;
        }
        if (activeCurveNo >= 0) {
            activeVertexNo = paths.get(activeCurveNo).size() - 1;
        } else {
            activeVertexNo = -1;
        }
            
        getEditPane().repaint();
    }

    /** Move the location of the last curve vertex added so that the
        screen location changes by the given amount. */
    public void moveLastVertex(int dx, int dy) {
        Point2D.Double p = getActiveVertex();
        if (p != null) {
            principalToScreen.transform(p, p);
            p.x += dx;
            p.y += dy;
            screenToPrincipal.transform(p, p);
            getActiveCurve().set(activeVertexNo, p);
            getEditPane().repaint();
        }
    }

    public void paintEditPane(Graphics g0) {

        Graphics2D g = (Graphics2D) g0;

        if (!tracingImage() && standardPageToScreen != null) {
            // Print the white rectangular background of the page.
            g.setColor(Color.WHITE);
            g.fill(screenBounds);
        }

        // Disable anti-aliasing for this phase because the
        // anti-aliasing prevents the green line from precisely
        // overwriting the red line.

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.BLACK);

        // TODO fix...
        // if (diagramOutline != null) {
        // g.draw(diagramOutline);
        // }

        int pathCnt = paths.size();
        if (pathCnt == 0) {
            return;
        }

        for (int i = 0; i < pathCnt; ++i) {
            if (i != activeCurveNo) {
                GeneralPolyline path = paths.get(i);
                if (path.size() == 1) {
                    circleVertices(g, path);
                } else {
                    draw(g, path);
                }
            }
        }

        GeneralPolyline lastPath = paths.get(activeCurveNo);

        if (mprin != null && activeVertexNo != -1) {

            // Disable anti-aliasing for this phase because it
            // prevents the green line from precisely overwriting the
            // red line.

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_OFF);

            g.setColor(Color.RED);
            lastPath.add(activeVertexNo + 1, mprin);
            draw(g, lastPath);
            lastPath.remove(activeVertexNo + 1);
        }

        g.setColor(Color.GREEN);
        draw(g, lastPath);
        circleVertices(g, lastPath);

        // TODO Undo (testing only)...
        for (Point2D.Double point: keyPoints()) {
            Point p = Duh.toPoint(principalToScreen.transform(point, null));
            g.drawOval(p.x - 8, p.y - 8, 16, 16);
        }

    }

    /** @return The GeneralPolyline that is currently being edited. If
        there is none yet, then create it. */
    public GeneralPolyline getActiveCurve() {
        if (paths.size() == 0) {
            endCurve();
        }
        return paths.get(activeCurveNo);
    }

    /** Start a new curve. */
    void endCurve() {
        if (screenToPrincipal == null) {
            return;
        }
        if (paths.size() > 0) {
            if (paths.get(paths.size() - 1).size() == 0) {
                // Remove the old empty path.
                paths.remove(paths.size() - 1);
            }
        }
        paths.add(GeneralPolyline.create
                  (smoothingType,
                   new Point2D.Double[0],
                   new BasicStroke((float) 0.0012,
                                   BasicStroke.CAP_ROUND,
                                   BasicStroke.JOIN_ROUND)));
        activeCurveNo = paths.size() - 1;
        activeVertexNo = -1;
        getEditPane().repaint();
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
        // visitation order matters. Start with the currently active
        // curve and work backwards.

        int pathCnt = paths.size();

        for (int i = 0; i < pathCnt; ++i) {
            int pathNo = (activeCurveNo - i + pathCnt) % pathCnt;
            GeneralPolyline path = paths.get(pathNo);
            int vertexCnt = path.size();
            int startVertex = (i == 0) ? activeVertexNo : vertexCnt - 1;
            for (int j = 0; j < vertexCnt; ++j) {
                int vertexNo = (startVertex - j + vertexCnt) % vertexCnt;
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

    /** Move the mouse pointer so its position corresponds to the
        given location in principal coordinates. */
    void moveMouse(Point2D.Double point) {
        mprin = point;
        if (principalToScreen == null) {
            return;
        }
        Point spos = Duh.toPoint(principalToScreen.transform(mprin));
        JScrollPane spane = editFrame.getScrollPane();
        Rectangle view = spane.getViewport().getViewRect();
        Point topCorner = spane.getLocationOnScreen();
        spos.x += topCorner.x - view.x;
        spos.y += topCorner.y - view.y;

        // TODO fix jumping out of bounds...

        try {
            Robot robot = new Robot();
            robot.mouseMove(spos.x, spos.y);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }

        leaveMprinAlone = true;
        updateStatusBar();
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

                if (nearPoint == null || di.distance < minDist) {
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
        switch (smoothingType) {
        case GeneralPolyline.LINEAR:
            smoothingType = GeneralPolyline.CUBIC_SPLINE;
            // TODO Check a check mark to indicate smoothing is on.
            break;
        case GeneralPolyline.CUBIC_SPLINE:
            smoothingType = GeneralPolyline.LINEAR;
            // TODO Uncheck check mark.
            break;
        default:
            throw new IllegalArgumentException
                ("Unknown smoothingType value " + smoothingType);
        }

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

    /** Connect the dots in the various paths that have been added to
        this diagram. */
    public void drawPaths(Graphics2D g) {
        for (GeneralPolyline path : paths) {
            path.draw(g, path.getPath(principalToScreen), (float) scale);
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

    void draw(Graphics2D g, GeneralPolyline path) {
        path.draw(g, path.getPath(principalToScreen), (float) scale);
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
        // TODO Check about saving the old diagram...
        diagramType = (new DiagramDialog(null)).showModal();
        newDiagram((Point2D.Double[]) null);
    }

    protected void newDiagram(Point2D.Double[] vertices) {
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
                    pageMaxes = new double[] { 100.0 * angleSideLengths[0] / maxSideLength,
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
        editFrame.setVisible(true);
        if (tracing) {
            zoomFrame.setLocation(rect.x + rect.width, rect.y);
            zoomFrame.setTitle("Zoom " + cropFrame.getFilename());
            zoomFrame.pack();
            zoomFrame.setVisible(true);
        }
    }

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

    @Override
        public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
        public void mousePressed(MouseEvent e) {
        if (screenToPrincipal == null) {
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
        if (screenToPrincipal == null) {
            return;
        }

        if (leaveMprinAlone) {
            leaveMprinAlone = false;
            return;
        }

        double sx = e.getX() + 0.5;
        double sy = e.getY() + 0.5;
        mprin = screenToPrincipal.transform(sx,sy);

        updateStatusBar();
    }

    public void updateStatusBar() {
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
        getEditPane().repaint();
            
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

    /** Equivalent to setScale(getScale() * factor). */
    void zoomBy(double factor) {
        setScale(getScale() * factor);
    }

    /** @return the screen scale of this image relative to a standard
        page (in which the longer of the two axes has length 1). */
    double getScale() { return scale; }

    /** Set the screen scale of this image relative to a standard
        page. */
    void setScale(double value) {
        scale = value;
        if (principalToStandardPage == null) {
            return;
        }
        standardPageToScreen = new Affine(scale, 0.0,
                                          0.0, scale,
                                          0.0, 0.0);

        {
            Point2D.Double p1
                = standardPageToScreen.transform(pageBounds.x, pageBounds.y);
            Point2D.Double p2 = standardPageToScreen.transform
                (pageBounds.x + pageBounds.width,
                 pageBounds.y + pageBounds.height);
            int x = (int) Math.floor(p1.x);
            int y = (int) Math.floor(p1.y);
            screenBounds = new Rectangle(x, y,
                                         (int) Math.ceil(p2.x) - x,
                                         (int) Math.ceil(p2.y) - y);
            getEditPane().setPreferredSize
                (new Dimension(screenBounds.x + screenBounds.width,
                               screenBounds.y + screenBounds.height));
            getEditPane().revalidate();
        }

        try {
            screenToStandardPage = standardPageToScreen.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException();
        }
        principalToScreen = principalToStandardPage.clone();
        principalToScreen.preConcatenate((Transform2D) standardPageToScreen);
        if (originalToPrincipal != null) {
            PolygonTransform originalToScreen = originalToPrincipal.clone();
            originalToScreen.preConcatenate(principalToScreen);
            BufferedImage output = ImageTransform.run
                (originalToScreen, cropFrame.getImage(), Color.WHITE,
                 new Dimension((int) Math.ceil(pageBounds.width * scale),
                               (int) Math.ceil(pageBounds.height * scale)));
            lighten(output);
            editFrame.setImage(output);
        }
        editFrame.repaint();

        try {
            screenToPrincipal = principalToScreen.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }
    }

    EditPane getEditPane() { return editFrame.getEditPane(); }

    static String format(double d, int decimalPoints) {
        Formatter f = new Formatter();
        f.format("%." + decimalPoints + "f", d);
        return f.toString();
    }

    protected void newDiagram(Point[] verticesIn) {
        newDiagram((verticesIn == null) ? null
                   : Duh.toPoint2DDoubles(verticesIn));
    }

    /** Compress the brightness into the upper third of the range
        0..255. */
    static int lighten1(int i) {
        return 255 - (255 - i)/3;
    }

    static void lighten(BufferedImage image) { 
        int width = image.getWidth();
        int height = image.getHeight();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                Color c = new Color(image.getRGB(x,y));
                c = new Color(lighten1(c.getRed()),
                              lighten1(c.getGreen()),
                              lighten1(c.getBlue()));
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
        // TODO Auto-generated method stub
    }

    @Override
        public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
        public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    /** @return an array of all straight line segments defined for
        this diagram. */
    public LineSegment[] getAllLineSegments() {
        ArrayList<LineSegment> output
            = new ArrayList<LineSegment>();
         
        for (GeneralPolyline path : paths) {
            if (path.getSmoothingType() != GeneralPolyline.LINEAR) {
                // TODO handle the general smoothing case.

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
    void circleVertices(Graphics2D g, GeneralPolyline path) {
        BasicStroke s = path.getStroke();
        if (s == null) {
            s = (BasicStroke) g.getStroke();
        }
        double r = s.getLineWidth() * 2 * scale;
        Point2D.Double xpoint = new Point2D.Double();
        for (Point2D.Double point: path.getPoints()) {
            principalToScreen.transform(point, xpoint);
            g.fill(new Ellipse2D.Double(xpoint.x - r, xpoint.y - r, r * 2, r * 2));
        }
    }

    @Override
        public void mouseExited(MouseEvent e) {
        mprin = null;
        getEditPane().repaint();
    }
}
