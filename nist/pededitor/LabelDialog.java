package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.codehaus.jackson.annotate.JsonIgnore;

/** GUI for selecting a label string and an anchoring position for
    that label. */
public class LabelDialog extends JDialog {
    private static final long serialVersionUID = 7836636038916887920L;

    /** See AnchoredLabel.xWeight for the definition of this field. */
    double xWeight = 0;
    /** See AnchoredLabel.yWeight for the definition of this field. */
    double yWeight = 0;
    JTextField textField = new JTextField(55);

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
        private static final long serialVersionUID = -4526429983591205919L;

        final double xWeight;
        final double yWeight;
        AnchorAction(Image image, double xWeight, double yWeight) {
            super(null, new ImageIcon(image));
            this.xWeight = xWeight;
            this.yWeight = yWeight;
        }

        @Override public void actionPerformed(ActionEvent e) {
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

    LabelDialog(Frame owner, String title, Font font) {
        super(owner, "Edit Text", true);

        GridBagUtil cpgb = new GridBagUtil(this);

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);

            JLabel textLabel = new JLabel("Text:");
            textLabel.setLabelFor(textField);
            textField.setFont(font.deriveFont(16f));
            gb.addWest(textLabel);
            gb.endRowWith(textField);
            cpgb.endRowWith(panel);
        }

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);

            StringPalettePanel pal;

            StringEventListener listen = new StringEventListener() {
                    @Override public void actionPerformed(StringEvent e) {
                        String str = textField.getText();
                        textField.setText
                            (str.substring(0, textField.getSelectionStart()) +
                             e.getString()
                             + str.substring(textField.getSelectionEnd()));
                    }
                };

            // Kind of a hack to bring in the no-symbol. TODO fix.
            pal = new StringPalettePanel(new HTMLPalette(), 5, font);
            pal.addListener(listen);
            gb.endRowWith(pal);

            pal = new StringPalettePanel(new PedPalette(), 8, font);
            pal.addListener(listen);
            gb.endRowWith(pal);
            cpgb.addNorthwest(panel);
        }

        JPanel miscPane = new JPanel();
        GridBagUtil mgb = new GridBagUtil(miscPane);

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);

            gb.endRowWith
                (new JButton(new AbstractAction("H2SO4 \u2192 H\u2082SO\u2084") {
                        private static final long serialVersionUID = 197868896745807236L;

                        @Override public void actionPerformed(ActionEvent e) {
                            textField.setText(ChemicalString.autoSubscript
                                              (textField.getText()));
                        }
                    }));

            /* TODO

            gb.endRowWith
                (new JButton(new AbstractAction("Compute location") {
                        private static final long serialVersionUID = 197868896745807236L;

                        @Override public void actionPerformed(ActionEvent e) {
                            textField.setText(ChemicalString.autoSubscript
                                              (textField.getText()));
                        }
                    }));
            */

            JLabel fontSizeLabel = new JLabel("Font size:");
            fontSizeLabel.setLabelFor(fontSize);

            gb.addWest(fontSizeLabel);
            gb.addWest(fontSize);
            gb.endRowWith(new JLabel("of standard"));
            mgb.endRowWith(panel);
        }

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);

            JLabel textAngleLabel = new JLabel("Text angle:");
            textAngleLabel.setLabelFor(angleField);
            gb.addWest(textAngleLabel);
            gb.addWest(angleField);
            gb.addWest(new JLabel("degrees"));

            compassPane = new ImagePane(createCompassImage());
            gb.endRowWith(compassPane);
            mgb.endRowWith(panel);
        }

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);

            gb.addWest(mIsOpaque);

            JPanel boxMe = new JPanel();
            boxMe.add(mIsBoxed);
            boxMe.setBorder(BorderFactory.createLineBorder(Color.black));

            gb.endRowWith(boxMe);
            mgb.endRowWith(panel);
        }

        mgb.endRowWith(new JLabel("Label position relative to anchor:"));

        JPanel anchorPane = new JPanel();
        anchorPane.setLayout(new GridLayout(3, 3));
        for (int y = 2; y >= 0; --y) {
            for (int x = 2; x >= 0; --x) {
                AnchorAction action = createAnchorAction(x/2.0, y/2.0, false);
                JButton button = anchorButtons[y][x] = new JButton(action);
                anchorPane.add(button);
            }
        }

        mgb.endRowWith(anchorPane);
        cpgb.endRowWith(miscPane);

        setXWeight(0.5);
        setYWeight(0.5);
    }

    LabelDialog(Frame owner, String title, AnchoredLabel label, Font font) {
        this(owner, title, font);
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
        drawString(g, str, x, y, xWeight, yWeight, false);
    }

    /** @param xWeight 0.0 = anchor on left ... 1.0 = anchor on right

        @param yWeight 0.0 = anchor on top ... 1.0 = anchor on bottom

        @param showBounds For debugging purposes, show the 0 line and
        the string bounding box as returned by getStringBounds().
    */
    public static void drawString(Graphics g, String str,
                                  double x, double y,
                                  double xWeight, double yWeight,
                                  boolean showBounds) {
        Graphics2D g2d = (Graphics2D) g;
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(str, g);

        x += -bounds.getX() - bounds.getWidth() * xWeight;
        y += -bounds.getY() - bounds.getHeight() * yWeight;
        g2d.drawString(str, (float) x, (float) y);
        if (showBounds) {
            System.out.println("Bounds are " + bounds);
            g2d.draw(new Rectangle2D.Double(x + bounds.getX(), y + bounds.getY(),
                                            bounds.getWidth(), bounds.getHeight()));
            g2d.draw(new Line2D.Double(x , y + bounds.getY(),
                                       x + bounds.getWidth(), y + bounds.getY()));
        }
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
        // String fontName = "Free Serif";
        // String fontName = "Arial Unicode MS";
        String fontName = "Lucida Sans Unicode";
        // Font font = new Font(fontName, 0, 14);
        // Font font = new Font(null, 0, 14);
        Font font = (new Editor()).getFont();
        boolean foundFont = false;
        for (Font f: GraphicsEnvironment.getLocalGraphicsEnvironment()
                 .getAllFonts()) {
            if (f.getFontName().equals(fontName)) {
                foundFont = true;
                break;
            }
        }

        if (!foundFont) {
            JOptionPane.showMessageDialog
                (null, "Warning: font '" + fontName + "' not found.");
        }
        LabelDialog dialog = new LabelDialog(null, "Labels test", font);
        dialog.setFontSize(4.0/3);
        dialog.setAngle(Math.PI / 2);
        AnchoredLabel t = dialog.showModal();
        System.out.println("You selected " + t);
    }
   
}

class LabelAnchorButton extends JButton {
    private static final long serialVersionUID = 7002196720099380748L;

    double xWeight;
    double yWeight;

    LabelAnchorButton(double xWeight, double yWeight) {
        this.xWeight = xWeight;
        this.yWeight = yWeight;
    }
}