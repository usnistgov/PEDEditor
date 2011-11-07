package gov.nist.pededitor;

import java.awt.*;
import javax.imageio.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class Editor implements CropEventListener {
    static protected Image crosshairs = null;

    protected CropFrame cropFrame = new CropFrame();
    protected EditFrame editFrame = new EditFrame(this);
    protected ImageZoomFrame zoomFrame = new ImageZoomFrame();

    protected PolygonTransform originalToPrincipal = null;
    protected PolygonTransform principalToOriginal = null;
    protected PolygonTransform originalToScreen = null;
    protected Transform2D principalToStandardPage = null;
    protected Transform2D principalToScreen = null;
    protected Transform2D screenToPrincipal = null;
    protected Rectangle2D.Double pageBounds = null;
    protected DiagramType diagramType = null;
    protected double scale = 800.0;

    public String getFilename() {
        return cropFrame.getFilename();
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        // sun.java2d.d3d = false;
    	try {
            // The system file chooser looks much nicer, but
            // unfortunately the Windows L&F somehow introduces a
            // bug in the image drawing in ImagePane (or the
            // Windows L&F has a bug of its own).
    		
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    	} catch (Exception e){
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
        editPolygon(vertices);
    }

    protected void editPolygon(Point2D.Double[] vertices,
                               int outWidth, int outHeight, int margin) {

        int cnt = vertices.length;
        int innerWidth = outWidth - margin*2;
        int innerHeight = outHeight - margin*2;
        PolygonTransform xform;

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

        Rescale r;

        switch (diagramType) {
        case TERNARY_BOTTOM:
            {
                double height;

                double defaultHeight = 1.0
                    - (vertices[1].distance(vertices[2]) / 
                       vertices[0].distance(vertices[3]));
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
                originalToPrincipal = new QuadToQuad(vertices, outputVertices);

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
                // Transform the input quadrilateral into a rectangle
                QuadToRect q = new QuadToRect();
                q.setVertices(vertices);
                originalToPrincipal = q;

                r = new Rescale(1.0, leftMargin + rightMargin, maxPageWidth,
                                1.0, topMargin + bottomMargin, maxPageHeight);

                principalToStandardPage = new Affine
                    (r.t, 0.0,
                     0.0, -r.t,
                     leftMargin, 1.0 - bottomMargin);
                break;
            }
        default:
            {
                // TODO There's more to do for the partial ternary diagrams.
                originalToPrincipal = new TriangleTransform
                    (vertices, principalTrianglePoints);

                r = new Rescale(1.0, leftMargin + rightMargin, maxPageWidth,
                                TriangleTransform.UNIT_TRIANGLE_HEIGHT, topMargin + bottomMargin,
                                maxPageHeight);
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
        }
        pageBounds = new Rectangle2D.Double(0.0, 0.0, r.width, r.height);
        try {
            principalToOriginal = (PolygonTransform)
                originalToPrincipal.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }

        // Force the editor frame image to be initialized.
        zoomBy(1.0);

        editFrame.setTitle("Edit " + diagramType + " " + cropFrame.getFilename());
        zoomFrame.setImage(cropFrame.getImage());
        initializeCrosshairs();
        zoomFrame.getImageZoomPane().crosshairs = crosshairs;
        editFrame.getImagePane().addMouseMotionListener
            (new MouseAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        double x = e.getX() + 0.5;
                        double y = e.getY() + 0.5;
                        try {
                            Point2D.Double prin = screenToPrincipal.transform(x,y);
                            Editor.this.showCoordinates(prin);
                            Point2D.Double orig = principalToOriginal.transform(prin);
                            zoomFrame.setImageFocus((int) Math.floor(orig.x),
                                                    (int) Math.floor(orig.y));
                        } catch (UnsolvableException ex) {
                            // Ignore the exception
                        }
                    }
                    public void mouseDragged(MouseEvent e) {
                        mouseMoved(e);
                    }
                });
        editFrame.pack();
        Rectangle rect = editFrame.getBounds();
        zoomFrame.setLocation(rect.x + rect.width, rect.y);
        zoomFrame.setTitle("Zoom " + cropFrame.getFilename());
        zoomFrame.pack();
        editFrame.setVisible(true);
        zoomFrame.setVisible(true);
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
        Affine standardPageToScreen = new Affine(scale, 0.0,
                                                 0.0, scale,
                                                 0.0, 0.0);
        principalToScreen = principalToStandardPage.clone();
        principalToScreen.preConcatenate(standardPageToScreen);
        originalToScreen = originalToPrincipal.clone();
        originalToScreen.preConcatenate(principalToScreen);
        BufferedImage output = ImageTransform.run
            (originalToScreen, cropFrame.getImage(), Color.WHITE,
             new Dimension((int) Math.ceil(pageBounds.width * scale),
                           (int) Math.ceil(pageBounds.height * scale)));
        lighten(output);
        editFrame.setImage(output);

        try {
            screenToPrincipal = (Transform2D) principalToScreen.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }

        Polygon poly = new Polygon();
        for (Point2D.Double pt : originalToScreen.outputVertices()) {
            poly.addPoint((int) Math.round(pt.x), (int) Math.round(pt.y));
        }
        editFrame.getEditPane().setDiagramOutline(poly);
    }
        

    protected void showCoordinates(Point2D.Double point) {
        if (diagramType.isTernary()) {
            double z = 100 - point.x - point.y;
            if (point.x >= 0 && point.y >= 0 && z >= 0) {
                editFrame.showCoordinates(z, point.y, point.x);
            } else {
                editFrame.showCoordinates();
            }
        } else {
            editFrame.showCoordinates(point.x, point.y);
        }
    }

    protected void editPolygon(Point[] verticesIn) {
        editPolygon(Duh.toPoint2DDoubles(verticesIn),
                    800 /* width */, 800 /* height */, 100 /* margin */);
    }

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
        String title = (filename == null) ? "PED Editor" : filename;
        editFrame.setTitle(title);

        if (filename == null) {
            cropFrame.showOpenDialog();
        } else {
            cropFrame.setFilename(filename);
        }

        cropFrame.addCropEventListener(this);
        cropFrame.pack();
        cropFrame.setVisible(true);
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
}
