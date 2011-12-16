package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** GUI for selecting a label string and an anchoring position for
    that label. */
public class LabelDialog extends JDialog {
    int xWeight = 0;
    int yWeight = 0;
    JTextField textField;
    boolean pressedOK = false;

    class AnchorAction extends AbstractAction {
        final int xWeight;
        final int yWeight;
        AnchorAction(Image image, int xWeight, int yWeight) {
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

    LabelDialog(Frame owner) {
        super(owner, "Select Label", false);

        ButtonGroup group = new ButtonGroup();

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new GridLayout(0,1));

        Box box = new Box(BoxLayout.PAGE_AXIS);

        textField = new JTextField();

        box.add(new JLabel("Label:"));
        box.add(textField);
        box.add(new JLabel("Label position relative to anchor:"));
        JPanel anchorPane = new JPanel();
        anchorPane.setLayout(new GridLayout(3, 3));
        for (int y = 2; y >= 0; --y) {
            for (int x = 2; x >= 0; --x) {
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
                double cx = margin + (width - 2 * margin) * x / 2.0;
                double cy = margin + (height - 2 * margin) * y / 2.0;
                double r = 3;
                g.setColor(new Color(0, 200, 0));
                g.fill(new Ellipse2D.Double(cx - r, cy - r, r*2, r*2));
                g.setColor(Color.BLACK);
                drawString(g, "Label", cx, cy, x / 2.0, y / 2.0);
                anchorPane.add(new JButton(new AnchorAction(image, x, y)));
            }
        }

        box.add(anchorPane);
        contentPane.add(box);
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
        return pressedOK
            ? new AnchoredLabel(textField.getText(), xWeight / 2.0, yWeight / 2.0)
            : null;
    }

    public static void main(String[] args) {
        AnchoredLabel t = (new LabelDialog(null)).showModal();
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