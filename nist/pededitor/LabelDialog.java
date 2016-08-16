/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

/** GUI for selecting a label string and an anchoring position for
    that label. */
public class LabelDialog extends JDialog {
    private static final long serialVersionUID = 7836636038916887920L;

    /** See AnchoredLabel.xWeight for the definition of this field. */
    double xWeight;
    /** See AnchoredLabel.yWeight for the definition of this field. */
    double yWeight;
    JTextArea textField = new JTextArea(6,45);

    JCheckBox mIsOpaque = new JCheckBox("White label background");
    JCheckBox mIsBoxed = new JCheckBox("Label border");
    JCheckBox mIsCutout = new JCheckBox("White character holes (no HTML allowed)");
    JCheckBox mIsAutoWidth = new JCheckBox("Compact label");
    JTextField fontSize = new JTextField(10);
    JTextField codePoint = new JTextField("0000", 10);

    {
        mIsAutoWidth.setToolTipText
            ("Wrap text as needed to reduce label size.");
        String layerUp = "You may need to raise this object to the top layer "
            + "to see the effect.";
        mIsCutout.setToolTipText
            ("Helps data markers with holes stand out against dark backgrounds. "
             + layerUp);
        mIsBoxed.setToolTipText
            ("Place a box around this label.");
        mIsOpaque.setToolTipText
            ("Helps labels stand out against dark backgrounds. " + layerUp);
    }

    /** Text angle in degrees, where 0 = left-to-right and 90 =
        bottom-to-top. */
    JTextField angleField = new JTextField(7);
    transient boolean pressedOK = false;
    transient boolean packed = false;

    JButton[][] anchorButtons = new JButton[3][3];
    JButton okButton =  new JButton(new AbstractAction("OK") {
            private static final long serialVersionUID = 7893219422446551863L;
            @Override public void actionPerformed(ActionEvent e) {
                normalExit();
            }
        });

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
            setXWeight(xWeight);
            setYWeight(yWeight);
        }
    }

    public void normalExit() {
        pressedOK = true;
        setVisible(false);
    }

    /** The funny method name is because isOpaque() is already taken by JDialog. */
    public boolean isAutoWidth() { return mIsAutoWidth.isSelected(); }
    public boolean isBoxed() { return mIsBoxed.isSelected(); }
    public boolean isCutout() { return mIsCutout.isSelected(); }
    public boolean isOpaqueLabel() { return mIsOpaque.isSelected(); }

    public void setAutoWidth(boolean v) {
        mIsAutoWidth.setSelected(v);
    }
    public void setBoxed(boolean v) {
        mIsBoxed.setSelected(v);
    }
    public void setCutout(boolean v) {
        mIsCutout.setSelected(v);
    }
    public void setOpaqueLabel(boolean v) {
        mIsOpaque.setSelected(v);
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
        fontSize.setText(ContinuedFraction.toString(scale, false));
    }

    public double getFontSize() {
        try {
            return ContinuedFraction.parseDouble(fontSize.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void insertText(String s) {
        String str = textField.getText();
        int ss = textField.getSelectionStart();
        textField.setText
            (str.substring(0, ss) + s
             + str.substring(textField.getSelectionEnd()));
        textField.setSelectionStart(ss + s.length());
        textField.setSelectionEnd(ss + s.length());
    }

    Function<String, String> csvToHTML = str -> {
        try {
            ICsvListReader r = null;
            try {
                r = new CsvListReader(new StringReader(str),
                        CsvPreference.STANDARD_PREFERENCE);
                StringBuilder outStr = new StringBuilder("<table>\n");
                for (List<String> cellList; (cellList = r.read()) != null;) {
                    outStr.append("  <tr>");
                    int cellNum = 0;
                    for (String cell: cellList) {
                        if (cellNum == 0) {
                            outStr.append("<td align=right>");
                        } else {
                            outStr.append("&nbsp;<td>");
                        }
                        ++cellNum;
                        String br = "<br>";
                        if (cell.endsWith(br)) {
                            cell = cell.substring(0, cell.length() - br.length());
                        }
                        outStr.append(cell);
                    }
                    outStr.append("\n");
                }
                outStr.append("</table>\n");
                return outStr.toString();
            } finally {
                if (r != null) {
                    r.close();
                }
            }
        } catch (IOException x) {
            throw new IllegalStateException(x);
        }
    };
            

    /** Replace the selected text with the same plus a starting
        delimiter before it and an ending delimiter afterwards. */
    public void delimit(Delimiter d) {
        String str = textField.getText();
        int ss = textField.getSelectionStart();
        int se = textField.getSelectionEnd();
        int dsl = d.getStart().length();
        textField.setText
            (str.substring(0, ss) + d.getStart()
             + str.substring(ss, se) + d.getEnd()
             + str.substring(se));

        textField.setSelectionStart(ss + dsl);
        textField.setSelectionEnd(se + dsl);
    }

    /** Replace the selected text with transform.apply(selection).
        Afterwards, all of the new text will be selected. */
    public void transformSelection(Function<String, String> transform) {
        String str = textField.getText();
        int ss0 = textField.getSelectionStart();
        int se0 = textField.getSelectionEnd();
        int ss;
        int se;
        if (ss0 < se0) { // If a region is selected, transform that region.
            ss = ss0;
            se = se0;
        } else { // Otherwise transform entire label.
            ss = 0;
            se = str.length();
        }
        String prefix = str.substring(0, ss);
        String suffix = str.substring(se);
        String sel = transform.apply(str.substring(ss, se));
        if (sel.isEmpty())
            return;
        str = prefix + sel + suffix;
        textField.setText(str);
        textField.setSelectionStart(newSelectionPos(ss0, ss, se, sel.length()));
        textField.setSelectionEnd(newSelectionPos(se0, ss, se, sel.length()));
    }

    /** Return the new selection position. Anything before the
        selection is unchanged, anything after the selection has its
        position unchanged with respect to the end of the label, and
        anything inside the selection is transformed into the
        selection start. */
    int newSelectionPos(int pos, int ss, int se, int len) {
        if (pos <= ss)
            return pos;
        if (pos < se)
            return ss;
        return ss + len + pos - se;
    }

    LabelDialog(Frame owner, String title, Font font) {
        this (owner, title, font, true);
    }

    LabelDialog(Frame owner, String title, Font font, boolean modifiableText) {
        super(owner, "Edit Text", true);

        GridBagUtil cpgb = new GridBagUtil(this);

        if (modifiableText) {
            StringPalettePanel pal;

            StringEventListener listen = new StringEventListener() {
                    @Override public void actionPerformed(StringEvent e) {
                        insertText(e.getString());
                    }
                };

            GridBagConstraints greedy = (GridBagConstraints) GridBagUtil.endRow.clone();
            greedy.weightx = 1.0;
            greedy.weighty = 1.0;
            greedy.fill = GridBagConstraints.BOTH;

            {
                JPanel panel = new JPanel();

                TransferFocus.patch(textField);
                JScrollPane sp = new JScrollPane(textField);
                textField.setLineWrap(true);
                textField.setWrapStyleWord(true);
                textField.setFont(font.deriveFont(16f));
                InputMap im = textField.getInputMap();
                im.put(KeyStroke.getKeyStroke("ENTER"),
                       okButton.getAction());

                GridBagUtil gb = new GridBagUtil(panel);
                gb.add(sp, greedy);

                DelimiterEventListener dlisten = new DelimiterEventListener() {
                        @Override public void actionPerformed(DelimiterEvent e) {
                            delimit(e.getDelimiter());
                        }
                    };

                DelimiterPalettePanel dpal = new DelimiterPalettePanel
                    (new HTMLDelimiterPalette(), 8, font);
                @SuppressWarnings("serial")
                    JButton butTable = new JButton(new AbstractAction("Table") {
                            @Override public void actionPerformed(ActionEvent e) {
                                transformSelection(csvToHTML);
                            }
                        });
                butTable.setToolTipText("Convert comma separated values into a table.");
                dpal.addButton(butTable);
                dpal.addListener(dlisten);
                gb.endRowWith(dpal);

                pal = new StringPalettePanel(new HTMLPalette(), 10, font);
                pal.addListener(listen);
                gb.endRowWith(pal);

                cpgb.add(panel, greedy);
            }

            {
                JPanel panel = new JPanel();
                GridBagUtil gb = new GridBagUtil(panel);

                pal = new StringPalettePanel(new PedPalette(), 8, font);
                pal.addListener(listen);
                gb.endRowWith(pal);

                JLabel codePointLabel = new JLabel("Unicode code point (hex):");
                codePointLabel.setLabelFor(codePoint);
                gb.addWest(codePointLabel);
                gb.addWest(codePoint);
                gb.endRowWith
                    (new JButton(new AbstractAction("Insert") {
                            private static final long serialVersionUID = 197868896745807236L;

                            @Override public void actionPerformed(ActionEvent e) {
                                String hex = codePoint.getText();
                                int codePoint;

                                try {
                                    codePoint = Integer.parseInt(hex, 16);
                                    String utfChar = new String(Character.toChars(codePoint));
                                    insertText(utfChar);
                                } catch (NumberFormatException nfe) {
                                    JOptionPane.showMessageDialog
                                        (null, "'" + hex + "' is not a valid input.\n" +
                                         "Enter a hexadecimal number such as '25bc'.");
                                    return;
                                } catch (IllegalArgumentException iae) {
                                    JOptionPane.showMessageDialog
                                        (null, "'" + hex + "' is not a valid Unicode code point.\n");
                                    return;
                                }
                            }
                        }));

                cpgb.addNorthwest(panel);
            }
        }

        JPanel miscPane = new JPanel();
        GridBagUtil mgb = new GridBagUtil(miscPane);

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);

            if (modifiableText) {
                JButton autoSubscriptButton = new JButton
                    (new AbstractAction
                     ("H2SO4 \u2192 H\u2082SO\u2084") {
                            private static final long serialVersionUID
                                = 197868896745807236L;

                            @Override public void actionPerformed(ActionEvent e) {
                                textField.setText(ChemicalString.autoSubscript
                                                  (textField.getText()));
                            }
                        });
                autoSubscriptButton.setToolTipText
                    ("Automatically add subscripts to chemical formulas.");
                gb.endRowWith(autoSubscriptButton);
            }

            JLabel fontSizeLabel = new JLabel("Font size:");
            fontSizeLabel.setLabelFor(fontSize);

            gb.addWest(fontSizeLabel);
            gb.addWest(fontSize);
            gb.endRowWith(new JLabel("times standard"));
            mgb.endRowWith(panel);
        }

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);

            JLabel textAngleLabel = new JLabel("Label angle:");
            textAngleLabel.setLabelFor(angleField);
            gb.addWest(textAngleLabel);
            gb.endRowWith(angleField);
            angleField.setToolTipText("(degrees counterclockwise from east)");
            mgb.endRowWith(panel);
        }

        if (modifiableText) {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);

            gb.addWest(mIsOpaque);

            JPanel boxMe = new JPanel();
            boxMe.add(mIsBoxed);
            boxMe.setBorder(BorderFactory.createLineBorder(Color.black));

            gb.endRowWith(boxMe);
            gb.endRowWith(mIsCutout);
            gb.endRowWith(mIsAutoWidth);
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
        getRootPane().setDefaultButton(okButton);
        mgb.endRowWith(okButton);
        cpgb.endRowWith(miscPane);

        reset();
    }

    LabelDialog(Frame owner, String title, AnchoredLabel label, Font font) {
        this(owner, title, font, true);
        set(label);
    }

    /** Make this dialog look newly constructed, except that the font won't change. */
    public void reset() {
        setText("");
        setXWeight(0.5);
        setYWeight(0.5);
        setFontSize(1.2);
        setAngle(0);
        setOpaqueLabel(false);
        setBoxed(false);
        setCutout(false);
    }

    /** Make the settings of this dialog reflect those of the given label. */
    public void set(AnchoredLabel label) {
        setText(label.getText());
        setXWeight(label.getXWeight());
        setYWeight(label.getYWeight());
        setFontSize(label.getScale());
        setAngle(label.getAngle());
        setAutoWidth(label.isAutoWidth());
        setBoxed(label.isBoxed());
        setCutout(label.isCutout());
        setOpaqueLabel(label.isOpaque());
    }

    public AnchorAction createAnchorAction(double xWeight,
                                           double yWeight,
                                           boolean highlight) {
        int width = 55;
        int height = 35;
        int margin = 10;
        BufferedImage image = new BufferedImage
            (width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setBackground(new Color(255, 255, 255, 0));
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        double cx = margin + (width - 2 * margin) * xWeight;
        double cy = margin + (height - 2 * margin) * yWeight;
        double r = 3;
        g.setColor(new Color(0, 200, 0));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r*2, r*2));
        g.setColor(highlight ? Color.RED : Color.BLACK);
        String str = "Label";
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
        if (!packed) {
            pack();
            setMinimumSize(getSize());
            packed = true;
        }
        pressedOK = false;
        textField.requestFocusInWindow();
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        setVisible(true);
        if (!pressedOK) {
            return null;
        }

        AnchoredLabel al =
            new AnchoredLabel(textField.getText(), xWeight, yWeight);
        al.setFontSize(getFontSize());
        al.setAngle(getAngle());
        al.setAutoWidth(mIsAutoWidth.isSelected());
        al.setOpaque(isOpaqueLabel());
        al.setBoxed(mIsBoxed.isSelected());
        al.setCutout(mIsCutout.isSelected());
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
        // Avoid the stupid behavior where negative zero is displayed
        // as -0.0
        angleField.setText(String.format("%.1f", (deg == 0 ? 0 : deg)));
    }

    public static void main(String[] args) {
        // String fontName = "Free Serif";
        // String fontName = "Arial Unicode MS";
        String fontName = "Lucida Sans Unicode";
        Font font = (new BasicEditor()).getFont();
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
