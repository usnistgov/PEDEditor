package gov.nist.pededitor;

public class HTMLPalette extends StringPalette {
    final static String yesPrefix = "<html>"; // no check mark
    // final static String yesPrefix = "<html>\u2713 "; // with check mark
    final static String noPrefix = "<html><font color=\"red\">&nbsp;\u20e0 ";
    final static String noSuffix = "</font></html>";
    public HTMLPalette() {
        super(new Object[][] {
                // Turn-on row
                {"<sub>", yesPrefix + "T<sub>sub</sub></html>"},
                {"<sup>", yesPrefix + "T<sup>sup</sup></html>"},
                {"<i>", yesPrefix + "<i>italic</i></html>"},
                {"<b>", yesPrefix + "<b>bold</b></html>"},
                {"<u>", yesPrefix + "<u>under</u></html>"},

                // Turn-off row
                {"</sub>", noPrefix + "T<sub>sub</sub>" + noSuffix},
                {"</sup>", noPrefix + "T<sup>sup</sup>" + noSuffix},
                {"</i>", noPrefix + "<i>italic</i>" + noSuffix},
                {"</b>", noPrefix + "<b>bold</b>" + noSuffix},
                {"</u>", noPrefix + "<u>under</u>" + noSuffix},
                null,

                {"<br>", "<html>line <br> break</html>"}
            });
    }
}