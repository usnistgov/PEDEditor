package gov.nist.pededitor;

import java.awt.*;

import javax.imageio.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.io.*;
import java.awt.print.*;

// TODO Allow the vertex duplication operation to detect all curve
// intersections, not just line segment intersections. (Line segment +
// 2D cubic spline intersections only require solving a cubic
// equation, which CubicCurve2D can help with; cubic spline + cubic
// spline intersections probably must be solved numerically.)

// TODO Fix the bug (in Java itself?) where scrolling the scroll pane
// doesn't cause the image to be redrawn.

// TODO Checkbox in the menu to indicate whether curve smoothing is
// enabled.

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class Editor implements CropEventListener, MouseListener,
                               MouseMotionListener, Printable {
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
        // System.out.println("Painting...");
        updateMousePosition();
        Graphics2D g = (Graphics2D) g0;
        paintDiagram(g, scale, true);
    }


    /** Compute the scaling factor to apply to pageBounds (and
        standardPage coordinates) in order for xform.transform(scale *
        pageBounds) to fill deviceBounds as much as possible without
        going over. */
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
            // xxx might need more work, but not a priority now

            if (editing) {
                g.setColor(Color.WHITE);
                g.fill(scaledPageBounds(scale));
            }
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
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

        if (editing) {
            GeneralPolyline lastPath = paths.get(activeCurveNo);

            // Disable anti-aliasing for this phase because it
            // prevents the green line from precisely overwriting the
            // red line.

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_OFF);

            if (mprin != null && activeVertexNo != -1) {

                g.setColor(Color.RED);
                // System.out.println("Added " + mprin);
                lastPath.add(activeVertexNo + 1, mprin);
                draw(g, lastPath, scale);
                lastPath.remove(activeVertexNo + 1);
            }

            g.setColor(Color.GREEN);
            draw(g, lastPath, scale);
            circleVertices(g, lastPath, scale);

            double r = 8.0; // Radius ~= r/72nds of an inch.

            // TODO Undo (testing only)...
            for (Point2D.Double point: keyPoints()) {
                Point2D.Double pagept = new Point2D.Double();
                principalToStandardPage.transform(point, pagept);
                g.draw(new Ellipse2D.Double
                       (scale * pagept.x - r, scale * pagept.y - r, r*2, r*2));
            }
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
                  (smoothingType,
                   new Point2D.Double[0],
                   new BasicStroke((float) lineWidth,
                                   BasicStroke.CAP_ROUND,
                                   BasicStroke.JOIN_ROUND)));
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
             oldCurve.getStroke());
        paths.set(activeCurveNo, curve);
        activeVertexNo = curve.size() - 1 - activeVertexNo;
        repaintEditFrame();
    }

    /** Invoked from the EditFrame menu */
    public void setLabelText() {
        // TODO Just a stub.
    }

    /** Invoked from the EditFrame menu */
    public void setLabelAnchor() {
        // TODO Just a stub.
    }

    /** Invoked from the EditFrame menu */
    public void setLabelAngle() {
        // TODO Just a stub.
    }

    /** Invoked from the EditFrame menu */
    public void setLabelFont() {
        // TODO Just a stub.
    }

    /** Invoked from the EditFrame menu */
    public void setLineStyle() {
        // TODO Just a stub.
    }

    /** Invoked from the EditFrame menu */
    public void setLineWidth() {
        // TODO Just a stub.
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

        // TODO For whatever reason, I need to add 1 to the x and y
        // coordinates if I want the output to satisfy

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
        // TODO Check about saving the old diagram...
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
                System.out.println("bl = " + sideLengths[BOTTOM_SIDE]);

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
        System.out.println("Smoothing type " + smoothingType);
        int oldSmoothingType = smoothingType;
        smoothingType = GeneralPolyline.LINEAR;
        for (Point2D.Double point: diagramPolyline) {
            add(point);
        }
        smoothingType = oldSmoothingType;
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

    /** Invoked from the EditFrame menu */
    public void saveAsPDF() {
        // TODO just a stub
    }

    /** Invoked from the EditFrame menu */
    public void saveAsSVG() {
        // TODO just a stub
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
            } catch (PrinterException e) {
                System.err.println(e);
            }
        } else {
            System.out.println("Print job canceled.");
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

        System.out.println("PageFormat bounds = " + bounds);
        g.translate(bounds.getX(), bounds.getY());
        double scale = Math.min(bounds.height / pageBounds.height,
                                bounds.width / pageBounds.width);
        System.out.println("Scale = " + scale);
        paintDiagram(g, scale, false);
        g.setTransform(oldTransform);

        return Printable.PAGE_EXISTS;
    }

    /** Invoked from the EditFrame menu */
    public void addVertexLocation() {
        // TODO just a stub
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
            path.setStroke
                (GeneralPolyline.scaledStroke(path.getStroke(),
                              lineWidth / path.getStroke().getLineWidth()));
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
    void circleVertices(Graphics2D g, GeneralPolyline path, double scale) {
        Point2D.Double xpoint = new Point2D.Double();
        Affine p2d = principalToScaledPage(scale);

        BasicStroke s = path.getStroke();
        if (s == null) {
            s = (BasicStroke) g.getStroke();
        }
        double r = s.getLineWidth() * 2 * scale;

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
}
