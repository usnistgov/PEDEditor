package gov.nist.pededitor;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;

/** This module provides a method to transform an input image to an
    output image. The xform object should support #inverse (whose
    output should support the #call(x,y) method), #max_output_x, and
    #max_output_y. */
public class ImageTransform {
   static final class AverageColor {

      BufferedImage image;

      int weight = 0;
      int r = 0;
      int g = 0;
      int b = 0;
      Dimension inSize;

      /** Background colors */
      Color background;
      int rb;
      int gb;
      int bb;

      AverageColor(BufferedImage image, Color background) {
         this.image = image;
         inSize = new Dimension(image.getWidth(), image.getHeight());
         this.background = background;
         rb = background.getRed();
         gb = background.getGreen();
         bb = background.getBlue();
      }

      void add(Point2D.Double p) {
         int x = (int) Math.floor(p.x);
         int y = (int) Math.floor(p.y);
         ++weight;
         if (x < 0 || x >= inSize.width || y < 0 || y >= inSize.height) {
            r += rb;
            g += gb;
            b += bb;
         } else {
            // System.out.println("weight " + weight + " (" + x + ", " + y + ")");
            Color c = new Color(image.getRGB(x,y));
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
         }
      }

      boolean empty() { return weight == 0; }

      Color getColor() {
         if (weight == 0) {
            return background;
         } else {
            double w = (double) weight;
            return new Color((int) Math.round(r/w), 
                             (int) Math.round(g/w),
                             (int) Math.round(b/w));
         }
      }
   }

   /** run() with default size and black background. */
   public static BufferedImage run(PolygonTransform xform,
                                   BufferedImage imageIn) {
      return run(xform, imageIn, Color.BLACK);
   }

   /** run() with output image size just large enough to hold
       xform.outputBounds(). */
   public static BufferedImage run(PolygonTransform xform,
                                   BufferedImage imageIn,
                                   Color background) {
      Rectangle2D.Double b = xform.outputBounds();
      int width = (int) Math.ceil(b.x + b.width);
      int height = (int) Math.ceil(b.y + b.height);
      System.out.println("Xform: " + xform);
      System.out.println("Xform output bounds: " + b);
      System.out.println("Xform input bounds: " + xform.inputBounds());
      return run(xform, imageIn, background, new Dimension(width, height));
   }

   /**  The scale of the output is 1 pixel = 1 unit. The minimum x and
        y values are 0 and 0. If those values are not suitable, then
        concatenate xform with an affine transformation as needed. */
   public static BufferedImage run(PolygonTransform xform,
                                   BufferedImage input,
                                   Color background,
                                   Dimension size) {
      // Forget about alpha channel for now.

      int width = size.width;
      int height = size.height;

      BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Transform2D xformi;

      try {
         // We actually want the inverse transformation, to measure the
         // color of the input image at each pixel of the output image.
         xformi = xform.createInverse();
      } catch (NoninvertibleTransformException e) {
         throw new RuntimeException(e);
      }

      Rectangle2D.Double ib = xform.inputBounds();
      double ipixels = (ib.width+1) * (ib.height+1);
      int sampleCnt = (int) Math.round(Math.max(3, Math.min(11, 3 * Math.sqrt(ipixels / (width * height)))));

      // The sampling resolution is sampleCnt times smaller than an
      // output pixel. (The sampleCnt setting is pretty arbitrary; the
      // resolution is up to 11 times smaller than an output pixel if
      // the output image is much smaller than the input one, and (for
      // better speed) as little as 3 times smaller than the output
      // pixel if the output image is as big or bigger than the input
      // image.

      // The sample is taken not from the middle of each sub-pixel,
      // but from a random location within the sub-pixel; hopefully,
      // this randomization should prevent visible artifacts.

      for (int x = 0; x < width; ++x) {
         for (int y = 0; y < height; ++y) {
            try {
               AverageColor c = new AverageColor(input, background);
               for (int xsub = 0; xsub < sampleCnt; ++xsub) {
                  for (int ysub = 0; ysub < sampleCnt; ++ysub) {
                     double xd = x + (xsub + Math.random()) / sampleCnt;
                     double yd = y + (ysub + Math.random()) / sampleCnt;
                     c.add(xformi.transform(xd, yd));
                  }
               }
               Color col = c.getColor();
               output.setRGB(x,y,col.getRGB());
            } catch (UnsolvableException e) {
               output.setRGB(x,y,background.getRGB());
            }
         }
      }

      return output;
   }

   /** Just a test harness. */
   public static void main(String[] args) {
      BufferedImage input;
      if (args.length != 1) {
         System.err.println("Usage: java ImageTransform <filename>");
         return;
      }
      try {
         input = ImageIO.read(new File(args[0]));
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      if (input == null) {
         System.err.println("Read of " + args[0] + " failed.");
         System.exit(2);
      }

      double[][] coords = {{0,4}, {4,4}, {2,5}, {2, 0}}; // kite_reverse
      Point2D.Double[] points = Duh.toPoint2DDoubles(coords);
      RectToQuad.sort(points);
      // QuadToRect xform = new QuadToRect();
      RectToQuad xform = new RectToQuad();
      xform.setVertices(points);

      System.out.println(xform);

      {
         Affine af = new Affine();
         af.setToScale(1.0/input.getWidth(), 1.0/input.getHeight());
         xform.preConcatenate(af);
      }

      int outWidth = 800;
      int outHeight = 600;

      System.out.println(xform);

      {
         Rectangle2D.Double outb = xform.outputBounds();
         double outScale = Math.min(outWidth/(outb.x + outb.width), outHeight/(outb.y + outb.height));
         Affine af = new Affine();
         af.setToScale(outScale, outScale);
         xform.concatenate(af);
      }

      System.out.println(xform);
      xform.check();

      BufferedImage output = run(xform, input);
      try {
         ImageIO.write(output, "jpg", new File("test-out.jpg"));
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
