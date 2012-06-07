package gov.nist.pededitor;

import java.awt.*;

/** Utility class that provides some standard BasicStroke styles. */
public class BasicStrokes {
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

        if (scale == 0) {
            throw new IllegalStateException("Zero scale");
        }

        return new BasicStroke(stroke.getLineWidth() * scale,
                               stroke.getEndCap(), stroke.getLineJoin(),
                               stroke.getMiterLimit(), dashes,
                               stroke.getDashPhase() * scale);
    }

    public static BasicStroke getSolidLine() {
        return new BasicStroke
            (1.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND);
    }

    public static BasicStroke getDottedLine() {
        return new BasicStroke
            (1.8f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { 0, 5.4f },
             0.0f);
    }

    public static BasicStroke getDashedLine() {
        return new BasicStroke
            (1.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { 5, 4 },
             0.0f);
    }

    public static BasicStroke getBlankFirstDashedLine() {
        return new BasicStroke
            (1.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { 5, 4 },
             7.0f);
    }

    /** Put a dot at the starting point and that's all. */
    public static BasicStroke getStartingDot() {
        return new BasicStroke
            (2.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { 0, 1e6f },
             0.0f);
    }

    /** Plot nothing. */
    public static BasicStroke getInvisibleLine() {
        return new BasicStroke
            (1.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { 0, 1e6f },
             2.0f);
    }
}
