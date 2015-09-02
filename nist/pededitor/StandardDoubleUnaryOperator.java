/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.function.DoubleUnaryOperator;

/** Functions useful for transforming graphs and chemical diagrams. */
enum StandardDoubleUnaryOperator implements DoubleUnaryOperator {
    IDENTITY (x -> x, "None"),
    LOG_10 (x -> Math.log10(x),
            "f(<var>v</var>) = log<sub>10</sub> <var>v</var>"),
    EXP_10 (x -> Math.pow(10, x),
            "f(<var>v</var>) = 10<sup><var>v</var></sup>"),
    LOG (x -> Math.log(x),
         "f(<var>v</var>) = log<sub><var>e</var></sub> <var>v</var>"),
    EXP (x -> Math.exp(x),
         "f(<var>v</var>) = e<sup><var>v</var></sup>"),
    C_TO_K (x -> x + 273.15, "\u00b0C to K"),
    K_TO_C (x -> x - 273.15, "K to \u00b0C"),
    TO_PERCENT (x -> x * 100, "f(v) = 100 v"),
    FROM_PERCENT (x -> x / 100, "f(v) = v / 100");

    private final DoubleUnaryOperator f;
    private final String text;

    StandardDoubleUnaryOperator(DoubleUnaryOperator f, String text) {
        this.f = f;
        this.text = text;
    }

    public String getText() { return text; }
    @Override public double applyAsDouble(double x) {
        return f.applyAsDouble(x);
    }

    public DoubleUnaryOperator getFunction() {
        return f;
    }
}
