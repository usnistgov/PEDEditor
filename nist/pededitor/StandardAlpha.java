/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Enum of how to shade a scanned image shown in a diagram. */
enum StandardAlpha
{ NONE(0, "Hide"), // Not shown
  LIGHT_GRAY(1.0/3, "Light"), // White parts look white, black parts appear light gray
  DARK_GRAY(0.577, "Medium"), // Halfway between light gray and black
  BLACK(1.0, "Dark"); // Original appearance

  private final double alpha;
  private final String label;

  StandardAlpha(double alpha, String label) {
      this.alpha = alpha;
      this.label = label;
  }

  double getAlpha() { return alpha; }
  String getLabel() { return label; }
};
