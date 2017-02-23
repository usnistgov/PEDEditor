/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Class to start the PED BasicEditor as a PED Viewer. The differences are
    that the editable flag is off by default, and the PED file is
    fetched using an HTTP connection to the url args[0]. */

public class JsonPostDiagram {

    static byte[] encodeForHttp(Map<String,String> args) {
        StringJoiner sj = new StringJoiner("&");
        for(Map.Entry<String,String> entry : args.entrySet())
            try {
                sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                        + URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException x) {
                throw new IllegalStateException(x);
            }
        return sj.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] encodeForHttp(String key, String value) {
        return encodeForHttp(
                Collections.singletonMap(key, value));
    }

    /** @return the HTTP response code. 200 is success. */
    public static void sendBytes(String urlStr, byte[] bytes)
            throws IOException {
        URL url = null;
        try {
            // Sanitize the urlStr argument.
            if (!Pattern.matches
                ("^https?://[A-Za-z0-9_.]+(?::\\d+)?/[-/A-Za-z0-9_.?&;%]+$",
                 urlStr)) {
                throw new IllegalArgumentException
                    ("URL " + urlStr + ": illegal URL syntax");
            }
            url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) conn;
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setFixedLengthStreamingMode(bytes.length);
            http.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            http.connect();
            try(OutputStream os = http.getOutputStream()) {
                os.write(bytes);
            }
            int code = http.getResponseCode();
            if (code != 200) {
                throw new IOException("Connection to " + urlStr + " returned " + code);
            }

        } catch (PatternSyntaxException x) {
            throw new IllegalStateException("Pattern could not compile: " + x);
        }
    }

    public static void main(String[] args) throws IOException {
        String url = "http://crystaldbdev.nist.gov:8080/page/Test";
        String value = "{\"cow\":5}";
        sendBytes(url, encodeForHttp("diagram", value));
    }
}
