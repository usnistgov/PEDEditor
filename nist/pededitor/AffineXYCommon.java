package gov.nist.pededitor;

import java.awt.geom.*;

/** Common elements between AffineXY and AffineXYInverse. */
abstract public class AffineXYCommon implements Transform2D {

   protected double xk = 0;
   protected double xkx = 0;
   protected double xky = 0;
   protected double xkxy = 0;
   protected double yk = 0;
   protected double ykx = 0;
   protected double yky = 0;
   protected double ykxy = 0;

   public void setxk(double xk) { this.xk = xk; }
   public double getxk() { return xk; }
   public void setxkx(double xkx) { this.xkx = xkx; }
   public double getxkx() { return xkx; }
   public void setxky(double xky) { this.xky = xky; }
   public double getxky() { return xky; }
   public void setxkxy(double xkxy) { this.xkxy = xkxy; }
   public double getxkxy() { return xkxy; }
   public void setyk(double yk) { this.yk = yk; }
   public double getyk() { return yk; }
   public void setykx(double ykx) { this.ykx = ykx; }
   public double getykx() { return ykx; }
   public void setyky(double yky) { this.yky = yky; }
   public double getyky() { return yky; }
   public void setykxy(double ykxy) { this.ykxy = ykxy; }
   public double getykxy() { return ykxy; }

   public void set(double xk, double xkx, double xky, double xkxy,
                   double yk, double ykx, double yky, double ykxy) {
      this.xk = xk;
      this.xkx = xkx;
      this.xky = xky;
      this.xkxy = xkxy;
      this.yk = yk;
      this.ykx = ykx;
      this.yky = yky;
      this.ykxy = ykxy;
   }

   abstract public Point2D.Double transform(double x, double y)
      throws UnsolvableException;
   abstract public Point2D.Double transform(Point2D.Double p)
      throws UnsolvableException;
   abstract public AffineXYCommon createInverse();

   protected void copyFieldsFrom(AffineXYCommon src) {
      setxk(src.xk);
      setxkx(src.xkx);
      setxky(src.xky);
      setxkxy(src.xkxy);
      setyk(src.yk);
      setykx(src.ykx);
      setyky(src.yky);
      setykxy(src.ykxy);
   }

   /** @ Return true if this is an affine transformation. */
   public boolean isAffine() {
      return xkxy == 0 && ykxy == 0;
   }

   private String plus(double x) {
      if (x > 0) {
         return "+ " + x;
      } else {
         return "- " + Math.abs(x);
      }
   }

   public String toString(String className) {
      return className + "(" +
         xk + " " + plus(xkx) + " x " + plus(xky) + " y " + plus(xkxy) + " xy, " +
         yk + " " + plus(ykx) + " x " + plus(yky) + " y " + plus(ykxy) + " xy" +
         ")";
   }
};
