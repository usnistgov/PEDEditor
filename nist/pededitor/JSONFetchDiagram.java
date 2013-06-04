package gov.nist.pededitor;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/** Class to start the PED Editor as a PED Viewer. The differences are
    that the editable flag is off by default, and the PED file is
    fetched using an HTTP connection to the url
    prefix+args[0]+suffix. */

public class JSONFetchDiagram {

    public static void run(String[] args) {
        run(args[0]);
    }

    public static void run(String id) {
        URL url = null;

        try {
            String prefix = resourceToString("ped-fetch-url-prefix.txt");
            String suffix = resourceToString("ped-fetch-url-suffix.txt");

            // Sanitize the id argument.
            url = new URL(prefix + Integer.parseInt(id) + suffix);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            Diagram d = Diagram.loadFrom(connection.getInputStream());
            Editor e = new Editor();
            e.copyFrom(d);
            e.initializeGUI();
            e.editFrame.setReloadVisible(false);
            e.bestFit();
            e.editFrame.setEditable(false);
        } catch (IOException x) {
            if (url != null) {
                System.err.println("While connecting to " + url + ":");
            }
            System.err.println("Error " + x);
            return;
        } catch (NumberFormatException x) {
            System.err.println("Cannot parse ID '" + id + "'");
            return;
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
                        JSONFetchDiagram.run(args);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }
}
