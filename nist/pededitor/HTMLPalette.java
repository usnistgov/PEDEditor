package gov.nist.pededitor;

public class HTMLPalette extends StringPalette {
    
    String[][] members =
    {
        {"<sub>", "<html>\u2713 T<sub>sub</sub></html>"},
        {"<sup>", "<html>\u2713 T<sup>sup</sup></html>"},
        {"<i>", "<html>\u2713 <i>italic</i></html>"},
        {"<b>", "<html>\u2713 <b>bold</b></html>"},
        {"<u>", "<html>\u2713 <u>under</u></html>"},
        {null, null},
        {"</sub>", "<html>\u2715 T<sub>sub</sub></html>"},
        {"</sup>", "<html>\u2715 T<sup>sup</sup></html>"},
        {"</i>", "<html>\u2715 <i>italic</i></html>"},
        {"</b>", "<html>\u2715 <b>bold</b></html>"},
        {"</u>", "<html>\u2715 <u>under</u></html>"},
        {null, null},
        {"<br>", "<html>line <br> break</html>"}
    };

    // TODO This is a bit lame, in that it exposes the null separators
    // in the array. That's a presentation feature that API users
    // shouldn't have to worry about. (On the other hand, API users
    // shouldn't really care much about size() and get() anyhow; all
    // they care about is the StringEvent that gets emitted.)
    public int size() {
        return members.length;
    }

    public String get(int index) {
        return members[index][0];
    }

    public Object createLabel(int index) {
        return members[index][1];
    }
}