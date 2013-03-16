package gov.nist.pededitor;

public class HTMLPalette extends StringPalette {
    public HTMLPalette() {
        super(new Object[][] {
                {"<br>\n", "<html>line <br> break</html>"},
                {"&lt;", "<"},
                {"&gt;", ">"},
                {"&amp;", "&"},
                {"&nbsp;", "Hard space"},
            });
    }
}
