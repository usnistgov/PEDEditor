package gov.nist.pededitor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

@SuppressWarnings("serial")
class TestPanel extends JPanel {

   /** The transformation used for this panel */
   QuadrilateralTransform xform;
   /** Transformation from unit square to the domain */
   Transform2D s2d;

   double inputXOffset, inputYOffset;
   double outputXOffset, outputYOffset;
   int width;
   int height;
   double scale;

   public TestPanel(QuadrilateralTransform xform) {
      this.xform = xform;
      s2d = xform.squareToDomain();

      Rectangle2D.Double inb = xform.inputBounds();
      Rectangle2D.Double outb = xform.outputBounds();

      double distance = (inb.width + outb.width) / 6.0;

      inputXOffset = -inb.x;
      inputYOffset = -inb.y;
      outputXOffset = distance + inb.width - outb.x;
      outputYOffset =  -outb.y;

      double widthd = inb.width + outb.width + distance;
      double heightd = Math.max(inb.height, outb.height);

      scale = Math.min(800/widthd, 600/heightd);
      width = (int) Math.ceil(widthd * scale);
      height = (int) Math.ceil(heightd * scale);
      setPreferredSize(new Dimension(width, height));
   }

   Point inputWindowPoint(Point2D.Double point)
   {
      return new Point
         ((int) Math.round((point.x + inputXOffset) * scale),
          (int) Math.round((point.y + inputYOffset) * scale));
   }

   Point outputWindowPoint(Point2D.Double point)
   {
      return new Point
         ((int) Math.round((point.x + outputXOffset) * scale),
          (int) Math.round((point.y + outputYOffset) * scale));
   }

  // Remember how the transformation converts everything to a square in
  // the middle? For the points in square_points, connect the dots
  // between the points' preimages in the input quadrilateral, then
  // apply the transformation to those preimage points and conntect the
  // dots between the resulting points in the output quadrilateral.
   void drawSquareLineImages(Graphics g,
                             ArrayList<Point2D.Double> squareCoordinates) {
      try {
         ArrayList<Point> prelines = new ArrayList<Point>();
         ArrayList<Point> postlines = new ArrayList<Point>();
         for (Point2D.Double point : squareCoordinates) {
            Point2D.Double inPoint = s2d.transform(point);
            prelines.add(inputWindowPoint(inPoint));
            postlines.add(outputWindowPoint(xform.transform(inPoint)));
         }

         g.setColor(Color.RED);
         for (int i = 1; i < prelines.size(); ++i) {
            g.drawLine(prelines.get(i-1).x, prelines.get(i-1).y,
                       prelines.get(i).x, prelines.get(i).y);
         }

         g.setColor(Color.CYAN);
         for (int i = 1; i < postlines.size(); ++i) {
            g.drawLine(postlines.get(i-1).x, postlines.get(i-1).y,
                       postlines.get(i).x, postlines.get(i).y);
         }
      } catch (UnsolvableException e) {
         throw new RuntimeException(e);
      }
   }

   public void paintComponent(Graphics g) {
      Rectangle drawHere = g.getClipBounds();
      g.setColor(Color.BLACK);
      ((Graphics2D) g).fill(drawHere);

      int steps = 16;

         // Draw the horizontal and vertical grids.
      for (int xi = 0; xi <= steps; ++xi) {
         ArrayList<Point2D.Double> squareCoordinates =
            new ArrayList<Point2D.Double>();
         ArrayList<Point2D.Double> squareCoordinatesTranspose =
            new ArrayList<Point2D.Double>();

         double x = xi / ((double) steps);
         for (int yi = 0; yi <= steps; ++yi) {
            double y = yi/((double) steps);
            squareCoordinates.add(new Point2D.Double(x,y));
            squareCoordinatesTranspose.add(new Point2D.Double(y,x));
         }
         drawSquareLineImages(g,squareCoordinates);
         if (xi % 2 == 0) {
            drawSquareLineImages(g,squareCoordinatesTranspose);
         }
      }

      {
         // Draw diagonal grid
         ArrayList<Point2D.Double> squareCoordinates =
            new ArrayList<Point2D.Double>();

         for (int xi = 0; xi <= steps; ++xi) {
            double x = xi / ((double) steps);
            squareCoordinates.add(new Point2D.Double(x,x));
         }
         drawSquareLineImages(g,squareCoordinates);
      }

      {
         // Draw the other diagonal
         ArrayList<Point2D.Double> squareCoordinates =
            new ArrayList<Point2D.Double>();

         for (int xi = 0; xi <= steps; ++xi) {
            double x = xi / ((double) steps);
            squareCoordinates.add(new Point2D.Double(x,1-x));
         }
         drawSquareLineImages(g,squareCoordinates);
      }
   }
}

@SuppressWarnings("serial")
class TestFrame extends JFrame {
   public TestFrame(QuadrilateralTransform xform) {
      setContentPane(new TestPanel(xform));
      pack();
   }
}

public class RectToQuadTest {
   public static void main(String[] args) {
      double[][][] vertexSets = 
         {{{3,1}, {0,2}, {2,3}, {1,0}}, // rotated square
          {{8,3}, {4,5}, {7,5}, {3,3}}, // trapezoid
          {{0,4}, {4,4}, {2,5}, {2, 0}}, // kite_reverse
          // {{3,0.5},{4,5},{2,3},{0,2}}, // Transforming a concave
          // quadrilateral causes an exception (as intended)
          {{4,0}, {0,2}, {2,8}, {9,7}}};
      
      for (int i = 0; i < vertexSets.length; ++i) {
         Point2D.Double[] points = Duh.toPoint2DDoubles(vertexSets[i]);
         Duh.sort(points, true);
         
         RectToQuad xform = new RectToQuad();
         xform.setVertices(points);
         xform.setX(3); xform.setY(2); xform.setW(4); xform.setH(4);
         (new TestFrame(xform)).setVisible(true);
         (new TestFrame(xform.createInverse())).setVisible(true);
      }
   }
}
