package gov.nist.pededitor;

import java.awt.geom.Point2D;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.codehaus.jackson.annotate.JsonIgnore;

/** GUI to display information about a tangency point in the
    diagram. */
public class VertexInfoDialog extends JDialog {
    private static final long serialVersionUID = 1686051698640332170L;

    protected JTextField angle = new JTextField(9);
    protected JTextField slope = new JTextField(12);

    /** These values provide greater precision than angle.getText()
     * and slope.getText() do. */

    protected double angled = 0;
    protected double sloped = 0;
    protected double lineWidthd = 0;
    protected Editor parentEditor = null;

    public boolean selfModifying = false;
    JLabel slopeLabel = new JLabel("d####.../d####....");
    public JLabel lineWidth = new JLabel("0.00000");

    public VertexInfoDialog(Editor parentEditor) {
        super(parentEditor.editFrame, "Slope", false);
        this.parentEditor = parentEditor;
        setAngleDegrees(0);

        slope.getDocument().addDocumentListener
            (new DocumentListener() {
                    @Override public void changedUpdate(DocumentEvent e) {
                        if (selfModifying) {
                            // This event was presumably generated by
                            // another event.
                            return;
                        }
                        try {
                            setSlope
                                (ContinuedFraction.parseDouble(slope.getText()),
                                 false);
                        } catch (NumberFormatException ex) {
                            return;
                        }
                    }

                    @Override public void insertUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }

                    @Override public void removeUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }
                });

        angle.getDocument().addDocumentListener
            (new DocumentListener() {
                    @Override public void changedUpdate(DocumentEvent e) {
                        if (selfModifying) {
                            // This event was presumably generated by
                            // another event.
                            return;
                        }
                        try {
                            setAngleDegrees
                                (ContinuedFraction.parseDouble(angle.getText()),
                                 false);
                        } catch (NumberFormatException ex) {
                            return;
                        }
                    }

                    @Override public void insertUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }

                    @Override public void removeUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }
                });

        GridBagUtil gb = new GridBagUtil(getContentPane());

        gb.addEast(new JLabel("Angle:"));
        gb.addWest(angle);
        gb.endRowWith(new JLabel("\u00B0") /* degree symbol */);

        gb.addEast(slopeLabel);
        gb.endRowWith(slope);

        gb.addEast(new JLabel("Line width:"));
        gb.endRowWith(lineWidth);
        pack();
        setDerivative(null);
        setLineWidth(0.0);
        setResizable(false);
    }

    public Editor getParentEditor() { return parentEditor; }

    /** Like setDerivative(), but the derivative is expressed in
     standard page coordinates, not principal coordinates. */
    public void setScreenDerivative(Point2D p) {
        Affine af = getParentEditor().standardPageToPrincipal;
        if (af == null) {
            setDerivative(p);
        } else {
            setDerivative(af.deltaTransform(p, new Point2D.Double()));
        }
    }

    public void setDerivative(Point2D point) {
        double x = (point == null) ? 0 : point.getX();
        double y = (point == null) ? 0 : point.getY();
        if (x == 0 && y == 0) {
            angle.setText("");
            slope.setText("");
            angled = Double.NaN;
            sloped = Double.NaN;
            return;
        }

        if (x == 0) {
            sloped = (y > 0) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            slope.setText("");
        } else {
            setSlope(y/x);
        }

        Point2D.Double p = new Point2D.Double();
        getParentEditor().principalToStandardPage.deltaTransform(point, p);
        setAngle(Math.atan2(p.y, p.x));
    }

    static double thetaToDegrees(double theta) {
        // Semi-lame hack to turn values almost exactly equal to -90
        // degrees into 90 degrees so it's not a coin flip whether a
        // nearly-vertical line ends up being displayed as pointing
        // upwards (90 degrees) or downwards (-90).
        double deg = -theta * 180 / Math.PI;
        if (deg < -90 - 1e-10) {
            deg += 180;
        } else if (deg > 90 - 1e-10) {
            deg -= 180;
        }
        return deg;
    }

    static public double degreesToTheta(double deg) {
        return -deg * Math.PI / 180;
    }

    /** Return true if v1 and v2 are so close to parallel that they approach the limits of double precision floating point numbers. */
    static boolean nearlyParallel(Point2D.Double v1, Point2D.Double v2) {
        return 1e13 * Math.abs(v1.x * v2.y - v1.y * v2.x) < Math.abs(v1.x * v2.x + v1.y * v2.y);
    }

    public double thetaToSlope(double theta) {
        Point2D.Double p = new Point2D.Double
            (Math.cos(theta), Math.sin(theta));
        Affine af = getParentEditor().standardPageToPrincipal;
        if (af == null) {
            return Double.NaN;
        }

        // Don't show values like -5.7e16 when the slope is +/-
        // infinity to the limits of precision.
        Affine afInverse = getParentEditor().principalToStandardPage;
        Point2D.Double vert = new Point2D.Double(0, 1);
        afInverse.deltaTransform(vert, vert);
        if (nearlyParallel(p, vert)) { // Nearly vertical
            return Double.NaN;
        }
        // Don't show values like -5.7e-16 when the slope is zero to
        // the limits of precision.
        Point2D.Double horiz = new Point2D.Double(1, 0);
        afInverse.deltaTransform(horiz, horiz);
        if (nearlyParallel(p, vert)) { // Nearly horizontal
            return 0;
        }

        af.deltaTransform(p, p);
        return p.y/p.x;
    }

    public double slopeToTheta(double s) {
        Point2D.Double p = new Point2D.Double(1.0, s);
        Affine af = getParentEditor().principalToStandardPage;
        if (af != null) {
            af.deltaTransform(p, p);
        }
        return Math.atan2(p.y, p.x);
    }

    public void setAngle(double theta) {
        setAngleDegrees(thetaToDegrees(theta));
    }

    public void setAngleDegrees(double deg) {
        setAngleDegrees(deg, true);
    }

    protected void setAngleDegrees(double deg, boolean changeDegreeText) {
        try {
            selfModifying = true;
            basicSetAngleDegrees(deg, changeDegreeText);
            basicSetSlope(thetaToSlope(degreesToTheta(deg)), true);
            repaint();
        } finally {
            selfModifying = false;
        }
    }

    protected void basicSetAngleDegrees(double deg, boolean changeText) {
        angled = deg;
        if (changeText) {
            angle.setText(LinearRuler.fixMinusZero(String.format("%.2f", deg)));
        }
    }

    @JsonIgnore public double getAngleDegrees() {
        return angled;
    }

    /** Return the angle in radians, where 0 is straight right and
        values increase clockwise. */
    public double getAngle() {
        return Compass.degreesToTheta(angled);
    }

    public void setSlope(double m) {
        setSlope(m, true);
    }

    protected void setSlope(double m, boolean changeSlopeText) {
        try {
            selfModifying = true;
            basicSetSlope(m, changeSlopeText);
            basicSetAngleDegrees(thetaToDegrees(slopeToTheta(m)), true);
            repaint();
        } finally {
            selfModifying = false;
        }
    }

    protected void basicSetSlope(double m, boolean changeText) {
        sloped = m;
        if (changeText) {
            if (Double.isNaN(m)) {
                slope.setText("");
            } else {
                double mabs = Math.abs(sloped);
                String format =
                    (mabs < 1e-12) ? "%.0f"
                    : (mabs < 1e-4) ? "%.4e"
                    : (mabs < 1) ? "%.6f"
                    : (mabs < 1e5) ? "%.4f"
                    : (mabs < 1e9) ? "%.0f"
                    : "%.6e";
                slope.setText(LinearRuler.fixMinusZero(String.format(format, sloped)));
            }
        }
    }

    public void setSlopeLabel(String label) {
        slopeLabel.setText(label + ":");
        slopeLabel.repaint();
    }

    static String truncatedName(LinearAxis ax, String defaultName) {
        String s = defaultName;
        if (ax != null && ax.name != null) {
            s = (String) ax.name;
        }
        if (s.length() >= 6) {
            return s.substring(0,4) + "...";
        } else {
            return s;
        }
    }

    /** Set the slope label automatically from the parent Editor's
        settings. */
    void setSlopeLabel() {
        Editor e = getParentEditor();
        setSlopeLabel("d" + truncatedName(e.getYAxis(), "y")
                      + "/d" + truncatedName(e.getXAxis(), "x"));
    }

    /** Set the slope label automatically from the parent Editor's
        settings. */
    public void refresh() {
        setSlopeLabel();
        setAngle(0);
    }

    public double getSlope() {
        return sloped;
    }

    public void setLineWidth(double w) {
        lineWidthd = w;
        lineWidth.setText(String.format("%.5f", w));
        repaint();
    }

    public double getLineWidth() {
        return lineWidthd;
    }
}
