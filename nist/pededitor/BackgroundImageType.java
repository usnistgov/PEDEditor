/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Enum of how to shade a scanned image shown in a diagram. */
enum BackgroundImageType
{ NONE, // Not shown
  LIGHT_GRAY, // White parts look white, black parts appear light gray
  DARK_GRAY, // Halfway between light gray and black
  BLACK, // Original appearance
  BLINK, // Blinks on and off
};
