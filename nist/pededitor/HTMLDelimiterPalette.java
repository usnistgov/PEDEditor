package gov.nist.pededitor;

public class HTMLDelimiterPalette extends DelimiterPalette {
    final static String prefix = "<html>";
    final static String suffix = "</html>";
    public HTMLDelimiterPalette() {
        super(new String[][] {
                {"<sub>", "</sub>", prefix + "T<sub>sub</sub>" + suffix},
                {"<sup>", "<s/up>", prefix + "T<sup>sup</sup>" + suffix},
                {"<i>", "</i>", prefix + "<i>italic</i>" + suffix},
                {"<b>", "</b>", prefix + "<b>bold</b>" + suffix},
                {"<u>", "</u>", prefix + "<u>under</u>" + suffix},
                {"<span style=\"font-size: 75%;\">", "</span>",
                 prefix
                 + "<span style=\"font-size: 75%;\">Small</span>" + suffix} ,
                {"<span style=\"font-size: 133%;\">", "</span>",
                 prefix
                 + "<span style=\"font-size: 133%;\">Big</span>" + suffix}
            });
    }
}
