package gov.nist.pededitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;

/** Enum for standard fill styles. Having so many standard types is a
    bit ugly. */
public enum StandardFill {
     // ALPHAXX -> XX% transparent gray
    SOLID (fill(0, 1, 1, false)),
    ALPHA50 (fill(0, 0.5, 0.5, false)),
    ALPHA25 (fill(0, 0.25, 0.25, false)),
    ALPHA10 (fill(0, 0.10, 0.10, false)),

    // VX_YY -> Vertical line X wide, YY% dense
    V1_25 (fill(Math.PI / 2, 1, 0.25, false)),
    V1_10 (fill(Math.PI / 2, 1, 0.1, false)),
    V2_25 (fill(Math.PI / 2, 2, 0.25, false)),
    V2_10 (fill(Math.PI / 2, 2, 0.1, false)),
    V4_25 (fill(Math.PI / 2, 4, 0.25, false)),

    // HX_YY -> Horizontal line X wide, YY% dense
    H1_25 (fill(0, 1, 0.25, false)),
    H1_10 (fill(0, 1, 0.1, false)),
    H2_25 (fill(0, 2, 0.25, false)),
    H2_10 (fill(0, 2, 0.1, false)),
    H4_25 (fill(0, 4, 0.25, false)),

    // DUX_YY -> Diagonal-up line X wide, YY% dense
    DU1_25 (fill(-Math.PI / 4, 1, 0.25, false)),
    DU1_10 (fill(-Math.PI / 4, 1, 0.1, false)),
    DU2_25 (fill(-Math.PI / 4, 2, 0.25, false)),
    DU2_10 (fill(-Math.PI / 4, 2, 0.1, false)),
    DU4_25 (fill(-Math.PI / 4, 4, 0.25, false)),

    // DDX_YY -> Diagonal-down line X wide, YY% dense
    DD1_25 (fill(Math.PI / 4, 1, 0.25, false)),
    DD1_10 (fill(Math.PI / 4, 1, 0.1, false)),
    DD2_25 (fill(Math.PI / 4, 2, 0.25, false)),
    DD2_10 (fill(Math.PI / 4, 2, 0.1, false)),
    DD4_25 (fill(Math.PI / 4, 4, 0.25, false)),

    // DDX_YY -> Crosshatch X wide, YY% dense
    X1_10 (fill(Math.PI / 4, 0.5, 0.1, true)),
    X2_10 (fill(Math.PI / 4, 1, 0.1, true)),
    X4_10 (fill(Math.PI / 4, 2, 0.1, true)),

    // PDX_YY -> Polka dot size ~X, ~YY% dense
    PD2_25 (fill(0, 0.25, false, BasicStrokes.getDottedLine())),
    PD4_25 (fill(0, 0.25, false, BasicStrokes.scaledStroke(BasicStrokes.getDottedLine(), 2))),
    PD8_25 (fill(0, 0.25, false, BasicStrokes.scaledStroke(BasicStrokes.getDottedLine(), 4)));

    private static interface FillArgs {
        public Paint getPaint(Color c, double scale);
    }

    private static class LineFillArgs implements FillArgs {
        double theta;
        double lineWidth;
        double density;
        boolean crosshatch;
        @Override
		public Paint getPaint(Color c, double scale) {
            return Fill.createHatch(theta, lineWidth * scale, density,
                                    crosshatch, c);
        }
    }

    private static FillArgs fill(double theta, double lineWidth,  double density,
                          boolean crosshatch) {
        LineFillArgs res = new LineFillArgs();
        res.theta = theta;
        res.lineWidth = lineWidth;
        res.density = density;
        res.crosshatch = crosshatch;
        return res;
    }

    private static class StrokeFillArgs implements FillArgs {
        double theta;
        double lineWidth;
        boolean crosshatch;
        BasicStroke stroke;
        
        @Override
		public Paint getPaint(Color c, double scale) {
            return Fill.createHatch(theta, lineWidth * scale, crosshatch,
                                    stroke, c);
        }
    }

    private static FillArgs fill(double theta, double lineWidth, boolean crosshatch,
    		BasicStroke stroke) {
        StrokeFillArgs res = new StrokeFillArgs();
        res.theta = theta;
        res.lineWidth = lineWidth;
        res.stroke = stroke;
        res.crosshatch = crosshatch;
        return res;
    }

    private final FillArgs fillArgs;

    StandardFill(FillArgs fillArgs) {
        this.fillArgs = fillArgs;
    }

    public Paint getPaint(Color c, double scale) {
        return fillArgs.getPaint(c, scale);
    }
}
