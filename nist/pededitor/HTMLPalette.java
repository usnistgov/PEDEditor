package gov.nist.pededitor;

public class HTMLPalette extends StringPalette {
    public HTMLPalette() {
        super(new Object[][] {
                {"&lt;", "<"},
                {"&gt;", ">"},
                {"&amp;", "&"},
                {"&nbsp;", "Hard space"},
            });
    }
}
