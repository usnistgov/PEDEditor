package gov.nist.pededitor;

public class HTMLPalette extends StringPalette {
    public HTMLPalette() {
        super(new Object[][] {
                // Turn-on row
                {"<sub>", "<html>\u2713 T<sub>sub</sub></html>"},
                {"<sup>", "<html>\u2713 T<sup>sup</sup></html>"},
                {"<i>", "<html>\u2713 <i>italic</i></html>"},
                {"<b>", "<html>\u2713 <b>bold</b></html>"},
                {"<u>", "<html>\u2713 <u>under</u></html>"},
                null,

                // Turn-off row
                {"</sub>", "<html>\u2715 T<sub>sub</sub></html>"},
                {"</sup>", "<html>\u2715 T<sup>sup</sup></html>"},
                {"</i>", "<html>\u2715 <i>italic</i></html>"},
                {"</b>", "<html>\u2715 <b>bold</b></html>"},
                {"</u>", "<html>\u2715 <u>under</u></html>"},
                null,

                {"<br>", "<html>line <br> break</html>"}
            });
    }
}