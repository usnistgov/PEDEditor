package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Paint;
import java.awt.TexturePaint;

/** Enum for standard fill styles. Having so many standard types is a
    bit ugly. */
public enum StandardFill {
     // ALPHAXX -> XX% transparent gray
    SOLID (Color.BLACK),
    ALPHA50 (Fill.createHatch(0, 0.5, 0.5, false)),
    ALPHA25 (Fill.createHatch(0, 0.25, 0.25, false)),
    ALPHA10 (Fill.createHatch(0, 0.10, 0.10, false)),

    // VX_YY -> Vertical line X wide, YY% dense
    V1_25 (Fill.createHatch(Math.PI / 2, 1, 0.25, false)),
    V1_10 (Fill.createHatch(Math.PI / 2, 1, 0.1, false)),
    V2_25 (Fill.createHatch(Math.PI / 2, 2, 0.25, false)),
    V2_10 (Fill.createHatch(Math.PI / 2, 2, 0.1, false)),
    V4_25 (Fill.createHatch(Math.PI / 2, 4, 0.25, false)),

    // HX_YY -> Horizontal line X wide, YY% dense
    H1_25 (Fill.createHatch(0, 1, 0.25, false)),
    H1_10 (Fill.createHatch(0, 1, 0.1, false)),
    H2_25 (Fill.createHatch(0, 2, 0.25, false)),
    H2_10 (Fill.createHatch(0, 2, 0.1, false)),
    H4_25 (Fill.createHatch(0, 4, 0.25, false)),

    // DUX_YY -> Diagonal-up line X wide, YY% dense
    DU1_25 (Fill.createHatch(-Math.PI / 4, 1, 0.25, false)),
    DU1_10 (Fill.createHatch(-Math.PI / 4, 1, 0.1, false)),
    DU2_25 (Fill.createHatch(-Math.PI / 4, 2, 0.25, false)),
    DU2_10 (Fill.createHatch(-Math.PI / 4, 2, 0.1, false)),
    DU4_25 (Fill.createHatch(-Math.PI / 4, 4, 0.25, false)),

    // DDX_YY -> Diagonal-down line X wide, YY% dense
    DD1_25 (Fill.createHatch(Math.PI / 4, 1, 0.25, false)),
    DD1_10 (Fill.createHatch(Math.PI / 4, 1, 0.1, false)),
    DD2_25 (Fill.createHatch(Math.PI / 4, 2, 0.25, false)),
    DD2_10 (Fill.createHatch(Math.PI / 4, 2, 0.1, false)),
    DD4_25 (Fill.createHatch(Math.PI / 4, 4, 0.25, false)),

    // DDX_YY -> Crosshatch X wide, YY% dense
    X1_10 (Fill.createHatch(Math.PI / 4, 0.5, 0.1, true)),
    X2_10 (Fill.createHatch(Math.PI / 4, 1, 0.1, true)),
    X4_10 (Fill.createHatch(Math.PI / 4, 2, 0.1, true)),

    // PDX_YY -> Polka dot size ~X, ~YY% dense
    PD2_25 (Fill.createHatch(0, 0.25, false, BasicStrokes.getDottedLine())),
    PD4_25 (Fill.createHatch(0, 0.25, false, BasicStrokes.scaledStroke(BasicStrokes.getDottedLine(), 2))),
    PD8_25 (Fill.createHatch(0, 0.25, false, BasicStrokes.scaledStroke(BasicStrokes.getDottedLine(), 4)));

    private final Paint fill;

    StandardFill(Paint fill) {
        this.fill = fill;
    }

    public Paint getFill() { return fill; }
}
