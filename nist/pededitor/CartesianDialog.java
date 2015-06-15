package gov.nist.pededitor;

import java.awt.Frame;
import java.awt.geom.Rectangle2D;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

public class CartesianDialog extends NumberColumnDialog {

    private static final long serialVersionUID = 1731583998807758331L;
    protected ButtonGroup diagramShape = new ButtonGroup();
    protected JRadioButton fixedAspect = new JRadioButton();
    protected JRadioButton uniformScale = new JRadioButton
        ("Uniform scale");
    protected NumberField aspectRatio = new NumberField(6);
    protected JCheckBox pixelMode = new JCheckBox
        ("Pixel mode (for drawing tiny pictures)");
    {
        fixedAspect.setSelected(true);
        diagramShape.add(fixedAspect);
        diagramShape.add(uniformScale);
        uniformScale.setToolTipText
            ("Width proportional to X range, height proportional to Y range");
        aspectRatio.setPercentage(true);
        aspectRatio.setValue(1.0);
    }

    public boolean isUniformScale() {
        return uniformScale.isSelected();
    }

    public void setUniformScale(boolean v) {
        (v ? uniformScale : fixedAspect).setSelected(true);
    }

    public void setPixelModeVisible(boolean b) {
        pixelMode.setVisible(b);
    }

    public boolean isPixelMode() {
        return pixelMode.isSelected();
    }

    public void setPixelMode(boolean v) {
        (v ? pixelMode : fixedAspect).setSelected(true);
    }

    public void setAspectRatio(double d) {
        aspectRatio.setValue(d);
    }

    public double getAspectRatio() throws NumberFormatException {
        return aspectRatio.getValue();
    }

    public CartesianDialog(Frame owner) {
        super(owner, 4,
              new String[] { "Left X value", "Right X value", 
                             "Bottom Y value", "Top Y value" },
              BasicEditor.htmlify
              ("<p>Fractions are allowed. "
               + "If you enter percentages, you must include the percent sign."
               + "<p>For logarithmic axes, enter the range of logarithms, "
               + "such as -2&nbsp;to&nbsp;2 for "
               + "10<sup>-2</sup>&nbsp;to&nbsp;10<sup>2</sup>."
               + "<p>You can modify these settings later using the "
               + "<code>Properties/Scale</code> and "
               + "<code>Properties/Aspect Ratio</code> menu items."));
        setTitle("Set Diagram Domain and Proportions");
        GridBagUtil gb = new GridBagUtil(panelBeforeOK);
        gb.endRowWith(Box.createVerticalStrut(6 /* pixels */));
        gb.addEast(new JLabel("Diagram width:height ratio:"));
        gb.addWest(fixedAspect);
        gb.addWest(aspectRatio);
        gb.endRowWith(uniformScale);
        gb.centerAndEndRow(pixelMode);
        gb.endRowWith(Box.createVerticalStrut(6 /* pixels */));
    }

    public CartesianDialog(Frame owner, Rectangle2D.Double rect) {
        this(owner);
        setRectangle(rect);
    }

    protected static double[] rectToValues(Rectangle2D rect) {
        return new double[]
            { rect.getX(), rect.getMaxX(), rect.getY(), rect.getMaxY() };
    }

    protected static Rectangle2D.Double valuesToRect(double[] values) {
        double left = values[0];
        double right = values[1];
        double bottom = values[2];
        double top = values[3];
        double width = right - left;
        double height = top - bottom;
        return new Rectangle2D.Double(left, bottom, width, height);
    }

    public Rectangle2D.Double getRectangle() {
        return valuesToRect(getColumnValues());
    }

    public void setRectangle(Rectangle2D rect) {
        setValues(rectToValues(rect));
    }


    public boolean xIsPercentage() {
        return getTextAt(0).contains("%") ||
            getTextAt(1).contains("%");
    }

    public boolean yIsPercentage() {
        return getTextAt(2).contains("%") ||
            getTextAt(3).contains("%");
    }

    void showError(String mess) {
        JOptionPane.showMessageDialog
            (getParent(), BasicEditor.htmlify(mess), "Numeric entry error",
             JOptionPane.ERROR_MESSAGE);
    }

    Rectangle2D.Double showModalRectangle() {
        Rectangle2D.Double old = getRectangle();
        while (true) {
            double[] values;
            try {
                values = showModalColumn();
                if (values == null) {
                    return old;
                }
            } catch (NumberFormatException x) {
                showError(x.getMessage());
                continue;
            }

            Rectangle2D.Double rect = valuesToRect(values);

            if (rect.width == 0) {
                showError("The left and right X values cannot be equal.");
                continue;
            }
            if (rect.height == 0) {
                showError("The top and bottom Y values cannot be equal.");
                continue;
            }

            return rect;
        }
    }

    public static void main(String[] args) {
        CartesianDialog dog = new CartesianDialog
            (null, new Rectangle2D.Double(0, 0, 1, 2));
        Rectangle2D.Double rect = dog.showModalRectangle();
        System.out.println(rect);
    }
}
