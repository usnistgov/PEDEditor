/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Functions useful for transforming graphs and chemical diagrams. */
enum StandardRealFunction implements RealFunction {
    IDENTITY ((double x) -> x, "None"),
    LOG_10 ((double x) -> Math.log10(x),
            "f(<var>v</var>) = log<sub>10</sub> <var>v</var>"),
    EXP_10 ((double x) -> Math.pow(10, x),
            "f(<var>v</var>) = 10<sup><var>v</var></sup>"),
    LOG ((double x) -> Math.log(x),
         "f(<var>v</var>) = log<sub><var>e</var></sub> <var>v</var>"),
    EXP ((double x) -> Math.exp(x),
         "f(<var>v</var>) = e<sup><var>v</var></sup>"),
    C_TO_K ((double x) -> x + 273.15, "\u00b0C to K"),
    K_TO_C ((double x) -> x - 273.15, "K to \u00b0C"),
    TO_PERCENT ((double x) -> x * 100, "f(v) = 100 v"),
    FROM_PERCENT ((double x) -> x / 100, "f(v) = v / 100");

    private final RealFunction f;
    private final String text;

    StandardRealFunction(RealFunction f, String text) {
        this.f = f;
        this.text = text;
    }

    public String getText() { return text; }
    @Override public double value(double x) {
        return f.value(x);
    }

    public RealFunction getFunction() {
        return f;
    }
}
