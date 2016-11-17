/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class TestPanelZ extends JPanel {

    LinearRuler r = new LinearRuler();

   public TestPanelZ() {
       r.startPoint = new Point2D.Double(10.0, 510.0);
       r.endPoint = new Point2D.Double(610.0, 110.0);
       r.fontSize = 12.0;
       r.textAngle = (Math.PI / 2) * 3.5;
       r.tickRight = true;
       r.lineWidth = 1.5;
       r.labelAnchor = LinearRuler.LabelAnchor.LEFT;
       r.tickType = LinearRuler.TickType.V;
       r.axis = new LinearAxis(-0.001, 0.0, 17);
       setPreferredSize(new Dimension(700, 600));
       r.startArrow = true;
       r.endArrow = true;
   }

   @Override public void paintComponent(Graphics g) {
      Rectangle drawHere = g.getClipBounds();
      g.setColor(Color.BLACK);
      ((Graphics2D) g).fill(drawHere);
      g.setColor(Color.WHITE);

      ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON);
      r.draw((Graphics2D) g, new AffineTransform(), 1.0);
   }
}

@SuppressWarnings("serial")
class TestFrameZ extends JFrame {
   public TestFrameZ() {
      setContentPane(new TestPanelZ());
      pack();
   }
}

public class LinearRulerTest {
   public static void main(String[] args) {
       (new TestFrameZ()).setVisible(true);
   }
}
