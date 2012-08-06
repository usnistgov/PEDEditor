package gov.nist.pededitor;

public class HTMLPalette extends StringPalette {
    public HTMLPalette() {
        super(new Object[][] {
                {"<br>", "<html>line <br> break</html>"},
                {"&lt;", "<"},
                {"&gt;", ">"},
                {"&amp;", "&"},
            });
    }
}
