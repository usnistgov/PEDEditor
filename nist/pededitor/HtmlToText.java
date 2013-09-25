package gov.nist.pededitor;

import org.jsoup.Jsoup;

public class HtmlToText {
    public static String htmlToText(String html) {
        if (html == null) {
            return null;
        }
        String text = Jsoup.parse(html).text();
        // Convert #0a (non-breaking-space) into ordinary spaces.
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            res.append((ch == '\u00a0') ? ' ' : ch);
        }
        return res.toString().trim();
    }
}
