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
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Watch a directory for newly created files whose extensions match a
 * given list.
 */

public class WatchNewFiles {

    volatile private Thread thread;
    HashSet<String> exts;
    Path dir;
    Consumer<Path> fileCreated;
    WatchService watcher;

    /**
     * Whenever a file whose extension is in exts[] (do not include
     * the period) is created in directory dir, call
     * fileCreated.accept(path) where path is the resolved path to the
     * new file.
     *
     * You have to call start() before anything happens.
     */
    public WatchNewFiles(Path dir, String[] exts, Consumer<Path> fileCreated) {
        this.dir = dir;
        this.exts = new HashSet<>();
        for (String s: exts) {
            this.exts.add(s.toLowerCase());
        }
        this.fileCreated = fileCreated;
    }

    /** Start the directory-watching thread. Return true if the thread
        wasn't already running. The call returns promptly, but the
        thread it starts runs until you call watchNewFiles.stop() or
        you end the program (as with exit() or abort()).

        Throws an IOException if for whatever reason monitoring cannot
        be enabled.
    */
    synchronized public boolean start() throws IOException {
        if (thread != null && thread.isAlive()) {
            return false;
        }
        watcher = FileSystems.getDefault().newWatchService();
        dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
        thread = new Thread(() -> { watch(); });
        thread.start();
        return true;
    }

    /** Stop the directory-watching thread. Return true if a directory
        was actually being watched.

        You may call stop() and then later call start() to restart the thread.
    */
    synchronized public boolean stop() {
        if (thread != null) {
            if (thread.isAlive()) {
                thread.interrupt();
                thread = null;
                return true;
            } else {
                thread = null;
            }
        }
        return false;
    }
        
    private void watch() {
        try {
            while (thread != null) {
                WatchKey key;
                key = watcher.take();

                for (WatchEvent<?> event: key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind != StandardWatchEventKinds.ENTRY_CREATE) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path path = dir.resolve(ev.context());
                    if (exts.contains(Stuff.getExtension(path.toString()))) {
                        fileCreated.accept(dir.resolve(ev.context()));
                    }
                    key.reset();
                }
            }
        } catch (InterruptedException x) {
        }
        thread = null;
    }

    /** Test harness detects the creation of files with extension .clam or .clem. */
    public static void main(String[] args) throws IOException {
        try {
            Path dir = Paths.get(".");
            String[] exts = { "clam", "clem" };
            WatchNewFiles watcher = new WatchNewFiles
                (dir, exts, (Path p) -> { System.out.println("Created " + p); });
            watcher.start();
            Thread.sleep(3000);
            System.out.println("Stopped monitoring output.");
            watcher.stop();
            Thread.sleep(3000);
            System.out.println("And we're back!");
            watcher.start();
            System.out.println(watcher.start());
            Thread.sleep(3000);
            watcher.stop();
            System.out.println("Finished.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
