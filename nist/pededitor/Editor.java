package gov.nist.pededitor;

import java.awt.*;
import javax.imageio.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.net.*;
import java.io.*;

public class Editor implements CropEventListener {
    static protected Image crosshairs = null;

    protected CropFrame cropFrame = new CropFrame();
    protected EditFrame editFrame = new EditFrame();
    protected ImageZoomFrame zoomFrame = new ImageZoomFrame();
    protected PolygonTransform originalToUnscaled = null;
    protected PolygonTransform unscaledToOriginal = null;

    public String getFilename() {
        return cropFrame.getFilename();
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
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
        editPolygon(vertices, e.getDiagramType());
    }

    protected void editPolygon(Point2D.Double[] vertices,
                               DiagramType diagramType, 
                               int outWidth, int outHeight, int margin) {
        int cnt = vertices.length;
        int innerWidth = outWidth - margin*2;
        int innerHeight = outHeight - margin*2;
        PolygonTransform xform;

        if (cnt == 4) {
            // Transform the input quadrilateral into a rectangle
            QuadToRect q = new QuadToRect();

            // Vertices are returned in order (LL, UL, UR, LR), but to
            // make the first vertex correspond to position (xpos,
            // ypos), you need to swap LL and UL and UR and LR.

            Point2D.Double[] swappedVertices =
                {vertices[1], vertices[0], vertices[3], vertices[2]};
            q.setVertices(swappedVertices);
            q.setRectangle
                (new Rectangle2D.Double
                 ((double) margin,
                  (double) margin,
                  (double) innerWidth,
                  (double) innerHeight));
            q.check();
            xform = q;
        } else {
            // Transform the input triangle into an equilateral triangle
            final double uth = TriangleTransform.UNIT_TRIANGLE_HEIGHT;
            double heightRatio = innerHeight / (uth * innerWidth);
            double width = (heightRatio > 1) ? innerWidth : (innerHeight / uth);
            double height = width * uth;
            double xMargin = (outWidth - width)/2;
            double yMargin = (outHeight - height)/2;
            Point2D.Double[] outputVertices =
                { new Point2D.Double(xMargin, outHeight - yMargin),
                  new Point2D.Double(outWidth / 2.0, yMargin),
                  new Point2D.Double(outWidth - xMargin, outHeight - yMargin) };
            TriangleTransform t = new TriangleTransform(vertices, outputVertices);
            t.check();
            xform = t;
        }

        System.out.println("Transformation is " + xform);
        BufferedImage output = ImageTransform.run
            (xform, cropFrame.getImage(), Color.WHITE,
             new Dimension(outWidth, outHeight));
        try {
            unscaledToOriginal = (PolygonTransform) xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            System.err.println("This transform is not invertible");
            System.exit(2);
        }
        lighten(output);
        editFrame.setImage(output);
        editFrame.setTitle("Edit " + diagramType + " " + cropFrame.getFilename());
        zoomFrame.setImage(cropFrame.getImage());
        initializeCrosshairs();
        zoomFrame.getImageZoomPane().crosshairs = crosshairs;
        Polygon poly = new Polygon();
        for (Point2D.Double pt : xform.outputVertices()) {
            poly.addPoint((int) Math.round(pt.x), (int) Math.round(pt.y));
        }
        editFrame.getEditPane().setDiagramOutline(poly);
        editFrame.getImagePane().addMouseMotionListener
            (new MouseAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        double x = e.getX() + 0.5;
                        double y = e.getY() + 0.5;
                        try {
                            Point2D.Double p = unscaledToOriginal.transform(x,y);
                            zoomFrame.setImageFocus((int) Math.floor(p.x),
                                                    (int) Math.floor(p.y));
                        } catch (UnsolvableException ex) {
                            // Ignore the exception
                        }
                    }
                    public void mouseDragged(MouseEvent e) {
                        mouseMoved(e);
                    }
                });
        editFrame.pack();
        Rectangle r = editFrame.getBounds();
        zoomFrame.setLocation(r.x + r.width, r.y);
        zoomFrame.setTitle("Zoom " + cropFrame.getFilename());
        zoomFrame.pack();
        editFrame.setVisible(true);
        zoomFrame.setVisible(true);
    }

    protected void editPolygon(Point[] verticesIn, DiagramType diagramType) {
        editPolygon(Duh.toPoint2DDoubles(verticesIn), diagramType,
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

        if (filename != null) {
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
