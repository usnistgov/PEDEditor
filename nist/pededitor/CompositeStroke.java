package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** It would be theoretically nice to make CompositeStroke implement
    Stroke, but the amount of use that CompositeStroke gets would not
    justify writing the amount of code that would be required to do
    that. */
public class CompositeStroke {
    protected ArrayList<BasicStroke> strokes
        = new ArrayList<BasicStroke>();

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
    public void draw(Graphics2D g, Path2D path, double scale) {
        Stroke oldStroke = g.getStroke();

        for (BasicStroke s: strokes) {
            g.setStroke(scaledStroke(s, scale));
            g.draw(path);
        }

        g.setStroke(oldStroke);
    }

    public int strokeCount() {
        return strokes.size();
    }

    public BasicStroke getStroke(int strokeNum) {
        return strokes.get(strokeNum);
    }

    /** @return a copy of "stroke" with its line width and dash
        pattern lengths scaled by a factor of "scaled". */
    public static BasicStroke scaledStroke(BasicStroke stroke, double scaled) {
        if (scaled == 1.0) {
            return stroke;
        }

        float scale = (float) scaled;
        float[] dashes = stroke.getDashArray();

        if (dashes != null) {
            dashes = (float[]) dashes.clone();
            for (int i = 0; i < dashes.length; ++i) {
                dashes[i] *= scale;
            }
        }
        return new BasicStroke(stroke.getLineWidth() * scale,
                               stroke.getEndCap(), stroke.getLineJoin(),
                               stroke.getMiterLimit(), dashes,
                               stroke.getDashPhase() * scale);
    }

    public static CompositeStroke getSolidLine() {
        CompositeStroke output = new CompositeStroke();
        output.add(new BasicStroke
                   (1.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
        return output;
    }

    public static CompositeStroke getDottedLine() {
        CompositeStroke output = new CompositeStroke();
        output.add(new BasicStroke
                   (1.8f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    3.0f,
                    new float[] { 0, 5.4f },
                    0.0f));
        return output;
    }

    public static CompositeStroke getDashedLine() {
        CompositeStroke output = new CompositeStroke();
        output.add(new BasicStroke
                   (1.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    3.0f,
                    new float[] { 5, 4 },
                    0.0f));
        return output;
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

    public static CompositeStroke getRailroadLine() {
        CompositeStroke output = new CompositeStroke();

        // Dash structure:

        // Continuous solid line plus cross-hatches 1" wide, 8" long, and 8" apart.

        // Add the solid line.
        output.add(new BasicStroke
                   (1.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

        // Add the cross-hatches.
        output.add(new BasicStroke
                   (8.0f,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,
                    3.0f,
                    new float[] { 1, 8 },
                    5.0f));
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

    /**  Put a dot at the starting point and that's all. */
    public static CompositeStroke getStartingDot() {
        CompositeStroke output = new CompositeStroke();
        output.add(new BasicStroke
                   (2.0f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    3.0f,
                    new float[] { 0, 1e6f },
                    0.0f));
        return output;
    }
}
