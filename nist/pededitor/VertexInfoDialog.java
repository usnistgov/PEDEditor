package gov.nist.pededitor;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Point2D;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;

import org.codehaus.jackson.annotate.JsonIgnore;

/** GUI to display information about a tangency point in the
    diagram. */
public class VertexInfoDialog extends JDialog {
	private static final long serialVersionUID = 1686051698640332170L;
	// TODO Use diagram components to determine slope.


    public JLabel angle = new JLabel("180.00");
    public JLabel slope = new JLabel("9999.99");
    JLabel slopeLabel;
    public JLabel lineWidth = new JLabel("0.00000");

    protected void add(JComponent c, GridBagLayout gb,
                       GridBagConstraints gbc) {
        gb.setConstraints(c, gbc);
        getContentPane().add(c);
    }

    public VertexInfoDialog(Frame owner) {
        super(owner, "Tangent", false);
        setFocusableWindowState(false);

        GridBagLayout gb = new GridBagLayout();
        getContentPane().setLayout(gb);

        Insets insets = new Insets(0, 3, 0, 3);
        GridBagConstraints east = new GridBagConstraints();
        east.anchor = GridBagConstraints.EAST;
        east.insets = insets;
        GridBagConstraints west = new GridBagConstraints();
        west.anchor = GridBagConstraints.WEST;
        west.insets = insets;

        GridBagConstraints endRow = new GridBagConstraints();
        endRow.anchor = GridBagConstraints.WEST;
        endRow.gridwidth = GridBagConstraints.REMAINDER;

        add(new JLabel("Angle:"), gb, east);
        add(angle, gb, west);
        add(new JLabel("\u00B0"), gb, endRow);

        add(new JLabel("Slope:"), gb, east);
        add(slope, gb, endRow);

        add(new JLabel("Line width:"), gb, east);
        add(lineWidth, gb, endRow);
        pack();
        setGradient(null);
        setLineWidth(0.0);
    }

    public void setGradient(Point2D point) {
        double x = (point == null) ? 0 : point.getX();
        double y = (point == null) ? 0 : point.getY();
        if (x == 0 && y == 0) {
            angle.setText("");
            slope.setText("");
            return;
        }

        if (x == 0) {
            slope.setText("");
        } else {
            setSlope(y/x);
        }

        setAngle(Math.atan2(y, x));
    }

    public void setAngle(double theta) {
        double deg = (-theta / Math.PI) * 180;
        if (deg < -90) {
            deg += 180;
        } else if (deg > 90) {
            deg -= 180;
        }

        angle.setText(String.format("%.2f", deg));
        repaint();
    }

    @JsonIgnore
    public double getAngleDegrees() {
        try {
            return Double.valueOf(angle.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public double getAngle() {
        return Compass.degreesToTheta(getAngleDegrees());
    }

    public void setSlope(double m) {
        slope.setText(String.format("%.4f", -m));
        repaint();
    }

    public void setSlopeLabel(String label) {
        slopeLabel.setText(label + ":");
        repaint();
    }

    // TODO Kind of lame -- use full precision slope instead of using
    // the displayed approximation.
    public double getSlope() {
        try {
            return -Double.valueOf(slope.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setLineWidth(double w) {
        lineWidth.setText(String.format("%.5f", w));
        repaint();
    }
    
}
