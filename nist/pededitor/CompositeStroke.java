/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.*;
import java.util.*;

/** Compositions of multiple BasicStrokes. It would be theoretically
    nice to make CompositeStroke implement Stroke, but the amount of
    use that CompositeStroke gets would not justify writing the amount
    of code that would be required to do that. */
public class CompositeStroke {
    protected ArrayList<BasicStroke> strokes
        = new ArrayList<BasicStroke>();

    public CompositeStroke() {}

    public CompositeStroke(BasicStroke stroke) {
        strokes.add(stroke);
    }

    /** Add a new BasicStroke element to this CompositeStroke.

       @param s The BasicStroke to use for drawing this.
    */
    public void add(BasicStroke s) {
        strokes.add(s);
    }

    public BasicStroke[] getStrokes() {
        return strokes.toArray(new BasicStroke[0]);
    }

    public void setStrokes(Collection<BasicStroke> strokes) {
        this.strokes = new ArrayList<BasicStroke>(strokes);
    }

    /** Draw the given path with this stroke while multiplying the
        line width and dash length by "scale". */
    public void draw(Graphics2D g, Shape shape, double scale) {
        Stroke oldStroke = g.getStroke();

        for (BasicStroke s: strokes) {
            g.setStroke(BasicStrokes.scaledStroke(s, scale));
            g.draw(shape);
        }

        g.setStroke(oldStroke);
    }

    public int strokeCount() {
        return strokes.size();
    }

    public BasicStroke getStroke(int strokeNum) {
        return strokes.get(strokeNum);
    }

    public static CompositeStroke getSolidLine() {
        return new CompositeStroke(BasicStrokes.getSolidLine());
    }

    public static CompositeStroke getDottedLine(double dashPeriod) {
        return new CompositeStroke(BasicStrokes.getDottedLine(dashPeriod));
    }

    public static CompositeStroke getDashedLine(double dashPeriod) {
        return new CompositeStroke(BasicStrokes.getDashedLine(dashPeriod));
    }

    public static CompositeStroke getBlankFirstDashedLine() {
        return new CompositeStroke(BasicStrokes.getBlankFirstDashedLine());
    }

    public static CompositeStroke getInvisibleLine() {
        return new CompositeStroke(BasicStrokes.getInvisibleLine());
    }

    public static CompositeStroke getStartingDot() {
        return new CompositeStroke(BasicStrokes.getStartingDot());
    }

    public static CompositeStroke getDotDashLine() {
        CompositeStroke output = new CompositeStroke();

        // Dash structure:

        // Dot diameter 1.8" (offset = 0), pause 5.1", dash 5.1"
        // (offset = 0.9 + 5.1 = 6"), pause 5.1", total = 17.1"

        output.add(new BasicStroke
                   (1.8f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    3.0f,
                    new float[] { 0, 17.1f },
                    0.0f));

        // Add the dashes at 1.0x scale.
        output.add(new BasicStroke
                   (1.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    3.0f,
                    new float[] { 5.1f, 12.0f },
                    11.1f));
        return output;
    }

    public static CompositeStroke getRailroadLine(float length, float distance) {
        CompositeStroke output = new CompositeStroke();

        // Dash structure:

        // Continuous solid line plus cross-hatches 1" wide, length" long,
        // and distance" apart.

        // Add the solid line.
        output.add(new BasicStroke
                   (1.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

        // Add the cross-hatches.
        output.add(new BasicStroke
                   (length,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,
                    3.0f,
                    new float[] { 1, distance },
                    0f));
        return output;
    }

    /** A superposition of a dotted and a solid line. */
    public static CompositeStroke getSolidDotLine() {
        CompositeStroke output = new CompositeStroke();

        // Solid (thinner than normal so the dots stand out)
        output.add(new BasicStroke
                   (0.6f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
        // Dots
        output.add(new BasicStroke
                   (1.8f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    3.0f,
                    new float[] { 0, 5.4f },
                    0.0f));
        return output;
    }
}
