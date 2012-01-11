package gov.nist.pededitor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

class TestPanelZ extends JPanel {

    LinearRuler r = new LinearRuler();

   public TestPanelZ() {
       r.startPoint = new Point2D.Double(10.0, 510.0);
       r.endPoint = new Point2D.Double(610.0, 110.0);
       r.xWeight = 1.0;
       r.yWeight = 0.5;
       r.fontSize = 12.0;
       r.textAngle = Math.PI/2;
       r.tickRight = true;
       r.lineWidth = 1.5;
       r.labelAnchor = LinearRuler.LabelAnchor.LEFT;
       r.tickType = LinearRuler.TickType.V;
       r.axis = new LinearAxisInfo(-0.001, 0.0, 17);
       setPreferredSize(new Dimension(700, 600));
       r.startArrow = true;
       r.endArrow = true;
   }

   public void paintComponent(Graphics g) {
      Rectangle drawHere = g.getClipBounds();
      g.setColor(Color.BLACK);
      ((Graphics2D) g).fill(drawHere);
      g.setColor(Color.WHITE);

      ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON);
      r.draw((Graphics2D) g, new AffineTransform(), 1.0);
   }
}

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
