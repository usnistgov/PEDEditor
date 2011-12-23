package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import org.codehaus.jackson.annotate.JsonIgnore;

/** GUI for selecting a label string and an anchoring position for
    that label. */
public class LabelDialog extends JDialog {
    double xWeight = 0;
    double yWeight = 0;
    JTextField textField;
    JTextField sizeNumerator;
    JTextField sizeDenominator;
    JTextField angleField;
    boolean pressedOK = false;
    ImagePane compassPane;

    JButton[][] anchorButtons = new JButton[3][3];

    class AnchorAction extends AbstractAction {
        final double xWeight;
        final double yWeight;
        AnchorAction(Image image, double xWeight, double yWeight) {
            super(null, new ImageIcon(image));
            this.xWeight = xWeight;
            this.yWeight = yWeight;
        }

        @Override
            public void actionPerformed(ActionEvent e) {
            LabelDialog.this.xWeight = xWeight;
            LabelDialog.this.yWeight = yWeight;
            pressedOK = true;
            setVisible(false);
        }
    }

    double getXWeight() { return xWeight; }
    double getYWeight() { return yWeight; }

    public void setXWeight(double xWeight) {
        setWeightIsHighlighted(false);
        this.xWeight = xWeight;
        setWeightIsHighlighted(true);
    }

    public void setYWeight(double yWeight) {
        setWeightIsHighlighted(false);
        this.yWeight = yWeight;
        setWeightIsHighlighted(true);
    }

    protected void setWeightIsHighlighted(boolean b) {
        int x = (int) Math.round(xWeight * 2.0);
        int y = (int) Math.round(yWeight * 2.0);
        anchorButtons[y][x].setAction
            (createAnchorAction(xWeight, yWeight, b));
    }

    public void setText(String s) {
        textField.setText(s);
    }

    public void setFontSize(double scale) {
        ContinuedFraction f = ContinuedFraction.create(scale, 0.00001, 9.0);
        if (f == null || f.looksLikeDecimal()) {
            sizeNumerator.setText(String.format("%6f", scale));
            sizeDenominator.setText("1");
        } else {
            sizeNumerator.setText(Long.toString(f.numerator));
            sizeDenominator.setText(Long.toString(f.denominator));
        }
    }

    public double getFontSize() {
        double n, d;
        try {
            n = Double.valueOf(sizeNumerator.getText());
            d = Double.valueOf(sizeDenominator.getText());
            if (d == 0) {
                throw new NumberFormatException("Zero denominator");
            }
            return n/d;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    LabelDialog(Frame owner) {
        super(owner, "Select Label", false);

        ButtonGroup group = new ButtonGroup();

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new GridLayout(0,1));

        Box box = new Box(BoxLayout.PAGE_AXIS);

        textField = new JTextField();

        box.add(new JLabel("Label:"));
        box.add(textField);
        box.add(new JLabel("Font size:"));
        Box sizeBox = new Box(BoxLayout.LINE_AXIS);
        sizeBox.add(new JLabel("("));
        sizeNumerator = new JTextField("1");
        sizeBox.add(sizeNumerator);
        sizeBox.add(new JLabel("\u00F7"));
        sizeDenominator = new JTextField("1");
        sizeBox.add(sizeDenominator);
        sizeBox.add(new JLabel(") \u00D7 normal size"));
        box.add(sizeBox);

        Box orientationBox = new Box(BoxLayout.LINE_AXIS);
        angleField = new JTextField("0");
        compassPane = new ImagePane();
        compassPane.setImage(createCompassImage());
        orientationBox.add(compassPane);
        Box ob2 = new Box(BoxLayout.PAGE_AXIS);
        angleField.setPreferredSize(new Dimension(90, 30));
        angleField.setMaximumSize(new Dimension(90, 30));
        ob2.add(new JLabel("Orientation:"));
        Box ob3 = new Box(BoxLayout.LINE_AXIS);
        ob3.add(angleField);
        ob3.add(new JLabel("degrees"));
        ob2.add(ob3);
        orientationBox.add(ob2);


        box.add(orientationBox);
        
        box.add(new JLabel("Label position relative to anchor:"));
        JPanel anchorPane = new JPanel();
        anchorPane.setLayout(new GridLayout(3, 3));
        for (int y = 2; y >= 0; --y) {
            for (int x = 2; x >= 0; --x) {
                AnchorAction action = createAnchorAction(x/2.0, y/2.0, false);
                JButton button = anchorButtons[y][x] = new JButton(action);
                anchorPane.add(button);
            }
        }

        box.add(anchorPane);
        contentPane.add(box);
    }

    BufferedImage createCompassImage() {
        int cr = 60;
        int cmargin = 30;
        Compass c = new Compass(cr + cmargin, cr + cmargin, cr);
        int width = cr * 2 + cmargin * 2;
        int height = width;

        BufferedImage cim = new BufferedImage
            (width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) cim.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font(null, Font.BOLD, 12));
        c.drawTickedCircle(g);
        g.setColor(Color.GREEN);
        c.drawHand(g, getAngleDegrees());

        // TODO Change as you change the value...

        return cim;
    }

    public AnchorAction createAnchorAction(double xWeight,
                                           double yWeight,
                                           boolean highlight) {
        int width = 100;
        int height = 50;
        int margin = 10;
        BufferedImage image = new BufferedImage
            (width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        double cx = margin + (width - 2 * margin) * xWeight;
        double cy = margin + (height - 2 * margin) * yWeight;
        double r = 3;
        g.setColor(new Color(0, 200, 0));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r*2, r*2));
        g.setColor(Color.BLACK);
        String str = highlight ? "(Label)" : "Label";
        drawString(g, str, cx, cy, xWeight, yWeight);
        return new AnchorAction(image, xWeight, yWeight);
    }

    /** @param weightX 0.0 = anchor on left ... 1.0 = anchor on right

        @param weightY 0.0 = anchor on top ... 1.0 = anchor on bottom
    */
    public static void drawString(Graphics g, String str,
                                  double x, double y,
                                  double weightX, double weightY) {
        Graphics2D g2d = (Graphics2D) g;
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(str, g);

        x += -bounds.getX() - bounds.getWidth() * weightX;
        y += -bounds.getY() - bounds.getHeight() * weightY;
        g2d.drawString(str, (float) x, (float) y);
    }

    /** Show the dialog as document-modal, and return the
        AnchoredLabel selected. Return null if the dialog was closed
        abnormally. */
    public AnchoredLabel showModal() {
        pack();
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        setVisible(true);
        if (!pressedOK) {
            return null;
        }

        AnchoredLabel al =
            new AnchoredLabel(textField.getText(), xWeight, yWeight);
        al.setFontSize(getFontSize());
        al.setAngle(getAngle());
        return al;
    }

    @JsonIgnore
    public double getAngleDegrees() {
        try {
            return Double.valueOf(angleField.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public double getAngle() {
        return Compass.degreesToTheta(getAngleDegrees());
    }

    public void setAngle(double theta) {
        double deg = Compass.thetaToDegrees(theta);
        angleField.setText(String.format("%.1f", deg));
        compassPane.setImage(createCompassImage());
    }

    public static void main(String[] args) {
        LabelDialog dialog = new LabelDialog(null);
        dialog.setFontSize(1.333333333333);
        dialog.setAngle(Math.PI / 2);
        AnchoredLabel t = dialog.showModal();
        System.out.println("You selected " + t);
    }
   
}

class LabelAnchorButton extends JButton {
    double xWeight;
    double yWeight;

    LabelAnchorButton(double xWeight, double yWeight) {
        this.xWeight = xWeight;
        this.yWeight = yWeight;
    }
}