package gov.nist.pededitor;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Class to start the PED Editor as a PED Viewer. The differences are
    that the editable flag is off by default, and the PED file is
    fetched using an HTTP connection to the url args[0]. */

public class JSONFetchDiagram2 {

    public static void run(String[] args) {
        run(args[0]);
    }

    public static void run(String urlStr) {
        URL url = null;

        try {
            // Sanitize the urlStr argument.
            if (!Pattern.matches
                ("^https?://[A-Za-z0-9_.]+(?::\\d+)?/[/A-Za-z0-9_.?&;%]+$",
                 urlStr)) {
                throw new IllegalArgumentException
                    ("URL " + urlStr + ": illegal URL syntax");
            }
            url = new URL(urlStr);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            Diagram d = Diagram.loadFrom(connection.getInputStream());
            Editor e = new Editor();
            e.copyFrom(d);
            e.initializeGUI();
            e.editFrame.setReloadVisible(false);
            e.detachOriginalImage();
            e.bestFit();
            e.editFrame.setEditable(false);
        } catch (IOException x) {
            if (url != null) {
                System.err.println("While connecting to " + url + ":");
            }
            System.err.println("Error " + x);
            return;
        } catch (PatternSyntaxException x) {
            throw new IllegalStateException("Pattern could not compile: " + x);
        }
    }

    static String resourceToString(String path) throws IOException {
        URL url = JSONFetchDiagram.class.getResource(path);

        try (BufferedReader in = new BufferedReader
             (new InputStreamReader(url.openStream()))) {
                return in.readLine();
            }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new ArgsRunnable(args) {
                @Override public void run() {
                    try {
                        if (args.length >= 1) {
                            JSONFetchDiagram2.run(args);
                        } else {
                            Editor app = new Editor();
                            app.run(null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }
}
