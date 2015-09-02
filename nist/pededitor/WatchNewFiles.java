/* Eric Boesch, NIST Materials Measurement Laboratory, 2015. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Watch a directory for newly created files whose extensions match a
 * given list.
 */

public class WatchNewFiles {

    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;

    /**
     * Register the given directory with the WatchService and return
     * this.
     */
    public WatchNewFiles addDir(Path dir) throws IOException {
        WatchKey key = dir.register
            (watcher, StandardWatchEventKinds.ENTRY_CREATE);
        keys.put(key, dir);
        return this;
    }

    /**
     * Creates a WatchService and registers the given directory
     * @throws IOException 
     */
    public WatchNewFiles() throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();
    }

    /**
     * Whenever a file whose extension is in exts[] (do not include
     * the period) is created in a registered directory, call
     * fileCreated.accept(path) where path is the resolved path to the
     * new file.
     */
    void processEvents(Consumer<Path> fileCreated, String[] exts) {
        HashSet<String> set = new HashSet<>();
        for (String s: exts) {
            set.add(s.toLowerCase());
        }

        for (;;) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind != StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("Rejecting " + kind);
                    continue;
                }

                @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path path = dir.resolve(ev.context());
                if (set.contains(Stuff.getExtension(path.toString()))) {
                    fileCreated.accept(dir.resolve(ev.context()));
                } else {
                    System.out.println("Rejecting '" + path.toString() + "'");
                }
            }
            key.reset();
        }
    }

    public static void main(String[] args) throws IOException {
        // register directory and process its events
        Path dir = Paths.get(".");
        new WatchNewFiles().addDir(dir).processEvents
            ((Path p) -> { System.out.println("Created " + p); },
             new String[] { "clam", "clem" });
    }
}
