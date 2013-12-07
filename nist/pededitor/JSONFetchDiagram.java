package gov.nist.pededitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;

/** Class to start the PED Editor as a PED Viewer. The differences are
    that the editable flag is off by default, and the PED file is
    fetched using an HTTP connection to the url args[0]. */

public class JSONFetchDiagram {

    /** Task that gets called regularly to see if any Editor objects
        are open. A slightly better solution would be to check after a
        given delay since the last non Editor closed, but that takes a
        little more code. Instead, just check every 30 minutes whether
        any Editors are open. */
    static class ShutdownChecker extends TimerTask {
        @Override public void run() {
            int cnt = Editor.getOpenEditorCnt();
            if (cnt == 0) {
                System.exit(0);
            }
        }
    }

    static class Listener implements SingleInstanceListener {
        Timer closer = new Timer("ShutdownChecker", false);
        TimerTask closeTask;
        long delay = 60*1000*30; // 30 minutes in msecs

        @Override public void newActivation(String[] args) {
            run(args);
            if (closeTask != null) {
                closeTask.cancel();
            }
            closeTask = new ShutdownChecker();
            closer.scheduleAtFixedRate(closeTask, delay, delay);
        }
    }

    public static void run(String[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException
                ("run() via JWS takes 1 or 2 arguments");
        }
        run(args[0], (args.length > 1) ? args[1] : null);
    }

    public static Editor run(String urlStr, String title) {
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
            URLConnection conn = url.openConnection();
            Diagram d = Diagram.loadFrom(conn.getInputStream());
            if (title != null) {
                d.setTitle(title);
            }
            Viewer e = new Viewer();
            e.setExitOnClose(false);
            e.copyFrom(d);
            e.init();
            return e;
        } catch (IOException x) {
            if (url != null) {
                System.err.println("While connecting to " + url + ":");
            }
            System.err.println("Error " + x);
            return null;
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
        try {
            SingleInstanceService sis = (SingleInstanceService)
                ServiceManager.lookup("javax.jnlp.SingleInstanceService");
            Listener listen = new Listener();
            sis.addSingleInstanceListener(listen);
            listen.newActivation(args);
        } catch(UnavailableServiceException x) {
            // I guess we're not running via JWS...
            run(args);
        }
    }
}
