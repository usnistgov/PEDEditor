/* Eric Boesch, NIST Materials Measurement Laboratory, 2014.
 *
 * This file uses the iText library (http://itextpdf.com) and is
 * subject to the GNU Affero General Public License
 * (http://www.gnu.org/licenses/agpl-3.0.html). */

package gov.nist.pededitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;

/** Class to start the PED BasicEditor as a PED Viewer. The differences are
    that the editable flag is off by default, and the PED file is
    fetched using an HTTP connection to the url args[0]. */

public class JSONPostDiagram {

    public static void run(String urlStr, String data) {
        try {
            // Sanitize the urlStr argument.
            if (!Pattern.matches
                ("^https?://[A-Za-z0-9_.]+(?::\\d+)?/[/A-Za-z0-9_.?&;%]+$",
                 urlStr)) {
                throw new IllegalArgumentException
                    ("URL " + urlStr + ": illegal URL syntax");
            }
                URLConnection conn = url.openConnection();
                Diagram d = Diagram.loadFrom(conn.getInputStream());
                Viewer e = new Viewer();
                e.copyFrom(d);

                EditFrame ef = e.editFrame;
                ef.toFront();
                ef.repaint();
        } catch (IOException x) {
            if (url != null) {
                System.err.println("While connecting to " + url + ":");
            }
            System.err.println("Error " + x);
        } catch (PatternSyntaxException x) {
            throw new IllegalStateException("Pattern could not compile: " + x);
        }
    }


    static String resourceToString(String path) throws IOException {
        URL url = JSONPostDiagram.class.getResource(path);

        try (BufferedReader in = new BufferedReader
             (new InputStreamReader(url.openStream()))) {
                return in.readLine();
            }
    }

    public static void main(String[] args) {
        try {
            SingleInstanceService sis = (SingleInstanceService)
                ServiceManager.lookup("javax.jnlp.SingleInstanceService");
            BasicEditorSingleInstanceListener listen
                = new BasicEditorSingleInstanceListener
                (new BasicEditorCreator() {
                        @Override public BasicEditor run() {
                            return new Viewer();
                        }});
            sis.addSingleInstanceListener(listen);
            listen.newActivation(args);
        } catch(UnavailableServiceException x) {
            // I guess we're not running via JWS...
            run(args);
        }
    }
}
