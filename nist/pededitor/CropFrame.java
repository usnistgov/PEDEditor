package gov.nist.pededitor;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.filechooser.*;
import java.util.prefs.*;

/** A frame that displays a scanned image with scrollbars and permits
    selection of a cropping region. */
public class CropFrame extends ImageScrollFrame {
    private static final long serialVersionUID = -4522482981189328347L;

    /** Name of the preferences key value that identifies the
        directory from which the last image was pulled, so the file
        chooser can automatically start in that directory next
        time. */
    private static final String PREF_DIR = "dir";

    protected String filename = null;
    protected DiagramType diagramType = null;
    protected DiagramDialog diagramDialog = null;

    protected List<CropEventListener> cropListeners = new
        ArrayList<CropEventListener>();
    protected CropFrameAction cropAction;
    protected CropFrameAction openAction;

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
        private static final long serialVersionUID = 7152450019959819145L;

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
        private static final long serialVersionUID = -3339878862920887087L;

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
        setTitle("Select Diagram");

        JMenuBar menuBar = new JMenuBar();

        JMenu mnFile = new JMenu("File");
        mnFile.setMnemonic('F');
        menuBar.add(mnFile);

        openAction = new CropFrameAction("Open", KeyEvent.VK_O) {
                private static final long serialVersionUID = -8441353884343354948L;

                @Override public void actionPerformed(ActionEvent e) {
                    showOpenDialog();
                }
            };

        mnFile.add(openAction);

        JMenu mnEdit = new JMenu("Edit");
        mnEdit.setMnemonic('E');
        menuBar.add(mnEdit);

        cropAction = new CropFrameAction("Selection Complete",
                                         KeyEvent.VK_S, "ENTER") {
                private static final long serialVersionUID = 6287954432789148634L;

                @Override public void actionPerformed(ActionEvent e) {
                    cropDone();
                }
            };
        cropAction.setEnabled(true);
        mnEdit.add(cropAction);

        mnEdit.add(new CropFrameAction("Delete last vertex", KeyEvent.VK_D,
                                       "DELETE") {
                private static final long serialVersionUID = 2227992506850526182L;

                @Override public void actionPerformed(ActionEvent e) {
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
                private static final long serialVersionUID = -5668311761560787337L;

                @Override public void actionPerformed(ActionEvent e) {
                    help();
                }
            };

        JMenu mnHelp = new JMenu("Help");
        mnHelp.setMnemonic('H');
        menuBar.add(mnHelp);
        mnHelp.add(help);

        setJMenuBar(menuBar);
    }

    public static File openFileDialog(Component parent, String filterName,
                                      String[] suffixes) {
        Preferences prefs = Preferences.userNodeForPackage(CropFrame.class);
        String dir = prefs.get(PREF_DIR,  null);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open PED Image");
        if (dir != null) {
            chooser.setCurrentDirectory(new File(dir));
        }
        chooser.setFileFilter
            (new FileNameExtensionFilter(filterName, suffixes));
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            prefs.put(PREF_DIR, file.getParent());
            return file;
        } else {
            return null;
        }
    }

    public void showOpenDialog() {
        File file = openFileDialog(CropFrame.this, "Image files",
                                   ImageIO.getReaderFileSuffixes());
        if (file != null) {
            try {
                setFilename(file.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog
                    (this, "Could not load file: " + e);
            }
        }
    }

    protected void help() {
        ShowHTML.show("crophelp.html", this);
    }

    public CropPane getCropPane() {
        return (CropPane) getImagePane();
    }

    void setSelectionReady(boolean ready) {
        cropAction.setEnabled(ready);
    }

    @Override public void setFilename(String filename)
        throws IOException {
        super.setFilename(filename);
        this.filename = filename;
        setTitle("Select Diagram in " + filename);
        if (diagramDialog == null) {
            diagramDialog = new DiagramDialog(this, getImage());
            diagramDialog.pack();
        }
        diagramType = (new DiagramDialog(this, getImage())).showModal();
        repaint();
    }

    public DiagramType getDiagramType() {
        return diagramType;
    }

    public String getFilename() {
        return filename;
    }

    @Override
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
                @Override
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
