package gov.nist.pededitor;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.filechooser.*;
import java.util.prefs.*;

/** A frame that displays a scanned image with scrollbars and permits
    selection of a rectangular, triangular, or quadrilateral cropping
    region. Clicking three points defines a rectangular region if the
    three points are laid out roughly in an L-shape, or a triangular
    region if they are laid out in a roughly equilateral-triangle
    shape. Clicking a fourth point defines a quadrilateral cropping
    region. */
public class CropFrame extends ImageScrollFrame {
    private static final String PREF_DIR = "dir";

    String filename = null;
    protected List<CropEventListener> cropListeners = new
        ArrayList<CropEventListener>();
    protected CropFrameAction cropAction;
    protected CropFrameAction openAction;
    JDialog helpDialog = null;

    public synchronized void addCropEventListener(CropEventListener listener) {
        cropListeners.add(listener);
    }
    public synchronized void removeCropEventListener(CropEventListener listener) {
        cropListeners.remove(listener);
    }

    protected void cropDone() {
        CropEvent ce = new CropEvent(this);
        for (CropEventListener l : cropListeners) {
            l.cropPerformed(ce);
        }
        dispose();
    }

    abstract class CropFrameAction extends AbstractAction {
        CropFrameAction(String name, int mnemonic, String accelerator) {
            super(name);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
            putValue(ACCELERATOR_KEY,
                     KeyStroke.getKeyStroke(accelerator));
        }

        CropFrameAction(String name, int mnemonic) {
            super(name);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
        }
    }

    /** Class for fine-grain adjustments of the last vertex added. */
    class AdjustAction extends CropFrameAction {
        int dx;
        int dy;

        AdjustAction(String name, int mnemonic, String accelerator,
                     int dx, int dy) {
            super(name, mnemonic, accelerator);
            this.dx = dx;
            this.dy = dy;
        }

        @Override
            public void actionPerformed(ActionEvent e) {
            ArrayList<Point> vs = getCropPane().vertices;
            int cnt = vs.size();
            if (cnt > 0) {
                Point p = vs.get(cnt - 1);
                p.x += dx;
                p.y += dy;
                getCropPane().repaint();
            }
        }
    }

    /**
     * Create the frame.
     */
    public CropFrame() {
        // contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
        JMenuBar menuBar = new JMenuBar();
		
        JMenu mnFile = new JMenu("File");
        mnFile.setMnemonic('F');
        menuBar.add(mnFile);

        openAction = new CropFrameAction("Open", KeyEvent.VK_O) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    Preferences prefs = Preferences.userNodeForPackage
                        (CropFrame.this.getClass());
                    String dir = prefs.get(PREF_DIR,  null);
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Open PED Image");
                    if (dir != null) {
                        chooser.setCurrentDirectory(new File(dir));
                    }
                    String[] suffixes = ImageIO.getReaderFileSuffixes();
                    chooser.setFileFilter
                        (new FileNameExtensionFilter("Image files", suffixes));
                    if (chooser.showOpenDialog(CropFrame.this) ==
                        JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();
                        prefs.put(PREF_DIR, file.getParent());
                        setFilename(file.getAbsolutePath());
                    }
                }
            };
		
        mnFile.add(openAction);

        JMenu mnEdit = new JMenu("Edit");
        mnEdit.setMnemonic('E');
        menuBar.add(mnEdit);

        cropAction = new CropFrameAction("Crop", KeyEvent.VK_C, "ENTER") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    CropEvent ce = new CropEvent(CropFrame.this);
                    cropDone();
                }
            };
        cropAction.setEnabled(false);
        mnEdit.add(cropAction);

        mnEdit.add(new CropFrameAction("Delete last vertex", KeyEvent.VK_D,
                                       "DELETE") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    ArrayList<Point> vs = getCropPane().vertices;
                    int cnt = vs.size();
                    if (cnt > 0) {
                        vs.remove(cnt - 1);
                    }
                    getCropPane().repaint();
                }
            });

        mnEdit.addSeparator();

        JMenuItem mnAdjust = new JMenuItem("Adjust");
        mnAdjust.setEnabled(false);
        mnEdit.add(mnAdjust);

        AdjustAction[] arrows =
            { new AdjustAction("Up", KeyEvent.VK_U, "UP", 0, -1),
              new AdjustAction("Down", KeyEvent.VK_D, "DOWN", 0, 1),
              new AdjustAction("Left", KeyEvent.VK_L, "LEFT", -1, 0),
              new AdjustAction("Right", KeyEvent.VK_R, "RIGHT", 1, 0) };
        for (AdjustAction a : arrows) {
            mnEdit.add(a);
        }

        CropFrameAction help = new
            CropFrameAction("Help", KeyEvent.VK_H, "F1") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    help();
                }
            };

        JMenu mnHelp = new JMenu("Help");
        mnHelp.setMnemonic('H');
        menuBar.add(mnHelp);
        mnHelp.add(help);

        setJMenuBar(menuBar);
    }

    protected void help() {
        if (helpDialog == null) {
            String filename = "crophelp.html";
            URL helpURL = CropFrame.class.getResource(filename);
            if (helpURL == null) {
                throw new Error("File " + filename + " not found");
            }
            JEditorPane editorPane = new JEditorPane();
            editorPane.setEditable(false);
            try {
                editorPane.setPage(helpURL);
            } catch (IOException e) {
                throw new Error(e);
            }
            JScrollPane editorScrollPane = new JScrollPane(editorPane);
            editorScrollPane.setPreferredSize(new Dimension(500, 500));
            
            helpDialog = new JDialog(this, "Crop Window Help");
            helpDialog.getContentPane().add(editorScrollPane);
            helpDialog.pack();
        }
        Rectangle r = getBounds();
        helpDialog.setLocation(r.x + r.width, r.y);
        helpDialog.setVisible(true);
        helpDialog.toFront();
    }

    public CropPane getCropPane() {
        return (CropPane) getImagePane();
    }

    void verticesChanged() {
        int cnt = getCropPane().getVertices().size();
        cropAction.setEnabled(cnt >= 3);
    }

    @Override
        public void setFilename(String filename) {
        super.setFilename(filename);
        this.filename = filename;
        setTitle("Crop " + filename);
    }

    public String getFilename() {
        return filename;
    }

    public BufferedImage getImage() {
        return getImagePane().getImage();
    }

    @Override
        protected ImagePane newImagePane() {
        return new CropPane(this);
    }

    /** Test harness */
    public static void printHelp() {
        System.err.println("Usage: java CropFrame [<filename>]");
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new ArgsRunnable(args) {
                public void run() {
                    if (args.length != 1) {
                        printHelp();
                        System.exit(2);
                    }
                    
                    try {
                        ImageScrollFrame frame = new CropFrame();
                        frame.setFilename(args[0]);
                        frame.pack();
                        frame.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }
}
