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
    /** See AnchoredLabel.xWeight for the definition of this field. */
    double xWeight = 0;
    /** See AnchoredLabel.yWeight for the definition of this field. */
    double yWeight = 0;
    JTextField textField = new JTextField(30);

    JCheckBox mIsOpaque = new JCheckBox("Opaque background");
    JCheckBox mIsBoxed = new JCheckBox("Box label");
    JTextField fontSize = new JTextField("100%", 10);

    /** Text angle in degrees, where 0 = left-to-right and 90 =
        bottom-to-top. */
    JTextField angleField = new JTextField("0", 7);
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

    public boolean isOpaque() { return mIsOpaque.isSelected(); }
    public boolean isBoxed() { return mIsBoxed.isSelected(); }
    public void setOpaque(boolean v) {
        mIsOpaque.setSelected(v);
    }
    public void setBoxed(boolean v) {
        mIsBoxed.setSelected(v);
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
        if (b) {
            getRootPane().setDefaultButton(anchorButtons[y][x]);
        }
    }

    public void setText(String s) {
        textField.setText(s);
    }

    public void setFontSize(double scale) {
        fontSize.setText(ContinuedFraction.toString(scale, true));
    }

    public double getFontSize() {
        try {
            return ContinuedFraction.parseDouble(fontSize.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static void add(JComponent parent, JComponent child,
                       GridBagLayout gb, GridBagConstraints gbc) {
        gb.setConstraints(child, gbc);
        parent.add(child);
    }

    LabelDialog(Frame owner, String title) {
        super(owner, "Edit Text", true);

        ButtonGroup group = new ButtonGroup();

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new GridLayout(0,1));

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

        GridBagLayout gbc = new GridBagLayout();
        contentPane.setLayout(gbc);

        {
            GridBagLayout gb = new GridBagLayout();
            JPanel panel = new JPanel();
            panel.setLayout(gb);

            {
                JLabel label = new JLabel("Text:");
                label.setLabelFor(textField);

                add(panel, label, gb, west);
                add(panel, textField, gb, endRow);
            }

            {
                JLabel label = new JLabel("Font size:");
                label.setLabelFor(fontSize);

                add(panel, label, gb, west);
                add(panel, fontSize, gb, west);
                JLabel label2 = new JLabel("of standard");
                add(panel, label2, gb, endRow);
            }

            add(contentPane, panel, gbc, endRow);
        }

        {
            GridBagLayout gb = new GridBagLayout();
            JPanel panel = new JPanel();
            panel.setLayout(gb);

            {
                JLabel label = new JLabel("Text angle:");
                label.setLabelFor(angleField);
                add(panel, label, gb, west);
                add(panel, angleField, gb, west);

                JLabel label2 = new JLabel("degrees");
                add(panel, label2, gb, west);
            }

            compassPane = new ImagePane();
            compassPane.setImage(createCompassImage());
            add(panel, compassPane, gb, endRow);

            add(contentPane, panel, gbc, endRow);
        }

        {
            GridBagLayout gb = new GridBagLayout();
            JPanel panel = new JPanel();
            panel.setLayout(gb);

            add(panel, mIsOpaque, gb, west);

            JPanel boxMe = new JPanel();
            boxMe.add(mIsBoxed);
            boxMe.setBorder(BorderFactory.createLineBorder(Color.black));

            add(panel, boxMe, gb, endRow);
            add(contentPane, panel, gbc, endRow);
        }

        
        add(contentPane, new JLabel("Label position relative to anchor:"),
            gbc, endRow);
        JPanel anchorPane = new JPanel();
        anchorPane.setLayout(new GridLayout(3, 3));
        for (int y = 2; y >= 0; --y) {
            for (int x = 2; x >= 0; --x) {
                AnchorAction action = createAnchorAction(x/2.0, y/2.0, false);
                JButton button = anchorButtons[y][x] = new JButton(action);
                anchorPane.add(button);
            }
        }

        add(contentPane, anchorPane, gbc, endRow);
    }

    LabelDialog(Frame owner, String title, AnchoredLabel label) {
        this(owner, title);
        setText(label.getText());
        setXWeight(label.getXWeight());
        setYWeight(label.getYWeight());
        setFontSize(label.getFontSize());
        setAngle(label.getAngle());
        setOpaque(label.isOpaque());
        setBoxed(label.isBoxed());
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

    /** @param xWeight 0.0 = anchor on left ... 1.0 = anchor on right

        @param yWeight 0.0 = anchor on top ... 1.0 = anchor on bottom
    */
    public static void drawString(Graphics g, String str,
                                  double x, double y,
                                  double xWeight, double yWeight) {
        Graphics2D g2d = (Graphics2D) g;
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(str, g);

        x += -bounds.getX() - bounds.getWidth() * xWeight;
        y += -bounds.getY() - bounds.getHeight() * yWeight;
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
        al.setOpaque(mIsOpaque.isSelected());
        al.setBoxed(mIsBoxed.isSelected());
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
        LabelDialog dialog = new LabelDialog(null, "Labels test");
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