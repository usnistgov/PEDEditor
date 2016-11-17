/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public class HTMLPalette extends StringPalette {
    public HTMLPalette() {
        super(new Object[] {
                "\u00b0" /* degree */,
                new Object[][] {
                    {"&lt;", "<"},
                    {"&gt;", ">"},
                    {"&amp;", "&"},
                    {"<br>\n", "<html><span style=\"font-size: 65%;\">line<br>break</span>"},
                    {"\n<p>", "\u00b6" /* pilcrow paragraph symbol */ },
                    {"&nbsp;", "Hard space"},
                },
                "\ud835\udf0b" /* mathematical pi -- u+1d70b */,
                "\ud835\udc52" /* mathematical e -- u+1d452 */
            });
    }
}
