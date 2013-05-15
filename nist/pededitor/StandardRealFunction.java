package gov.nist.pededitor;

/** IDs and icons for different types of PEDs */
enum StandardRealFunction {
    IDENTITY (new RealFunction() {
            @Override public double value(double x) {
                return x;
            }
        }, "None"),
    LOG (new RealFunction() {
            @Override public double value(double x) {
                return Math.log(x);
            }
        }, "f(<var>v</var>) = log<sub><var>e</var></sub> <var>v</var>"),
    EXP (new RealFunction() {
            @Override public double value(double x) {
                return Math.exp(x);
            }
        }, "f(<var>v</var>) = e<sup><var>v</var></sup>"),
    LOG_10 (new RealFunction() {
            @Override public double value(double x) {
                return Math.log(x);
            }
        }, "f(<var>v</var>) = log<sub>10</sub> <var>v</var>"),
    EXP_10 (new RealFunction() {
            @Override public double value(double x) {
                return Math.pow(10, x);
            }
        }, "f(<var>v</var>) = 10<sup><var>v</var></sup>");

    private final RealFunction f;
    private final String text;

    StandardRealFunction(RealFunction f, String text) {
        this.f = f;
        this.text = text;
    }

    public String getText() { return text; }
    public double value(double x) {
        return f.value(x);
    }

    public RealFunction getFunction() {
        return f;
    }
}
