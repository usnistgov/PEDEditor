package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class EditFrame extends ImageScrollFrame {
    protected JPanel statusBar;
    protected JLabel statusLabel;
    protected Editor parentEditor;

    public Editor getParentEditor() { return parentEditor; }

    abstract class EditFrameAction extends AbstractAction {
        EditFrameAction(String name, int mnemonic, KeyStroke accelerator) {
            super(name);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
            putValue(ACCELERATOR_KEY, accelerator);
        }

        EditFrameAction(String name, int mnemonic, String accelerator) {
            this(name, mnemonic, KeyStroke.getKeyStroke(accelerator));
        }

        EditFrameAction(String name, int mnemonic) {
            super(name);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
        }
    }

    /** Class for fine-grain adjustments of the last vertex added. */
    class AdjustAction extends EditFrameAction {
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
            getParentEditor().moveLastVertex(dx, dy);
        }
    }

    /**
     * Create the frame.
     */
    public EditFrame(Editor parentEditor) {
        this.parentEditor = parentEditor;
        statusBar = new JPanel();
        statusLabel = new JLabel("<html><font size=\"-2\">"
                                 + "No diagram loaded</font></html>");
        statusBar.add(statusLabel);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        mnFile.add(new EditFrameAction("New Diagram", KeyEvent.VK_N) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().newDiagram();
                }
            });

        JMenu mnOpen = new JMenu("Open");
        mnFile.add(mnOpen);

        /**
        mnOpen.add(new EditFrameAction("Diagram", KeyEvent.VK_D) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor.openDiagram();
                }
            });
        */

        JMenuItem mnDiagram = new JMenuItem("Diagram");
        mnOpen.add(mnDiagram);
        mnDiagram.setEnabled(false);

        mnOpen.add(new EditFrameAction("Image for Digitization", KeyEvent.VK_I) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().openImage(null);
                }
            });

        JMenuItem mnPrint = new JMenuItem("Print");
        mnFile.add(mnPrint);
        mnPrint.setEnabled(false);

        JMenuItem mnSave = new JMenuItem("Save");
        mnFile.add(mnSave);
        mnSave.setEnabled(false);

        JSeparator separator = new JSeparator();
        mnFile.add(separator);

        JMenuItem mnExit = new JMenuItem("Exit");
        mnFile.add(mnExit);
        mnExit.setEnabled(false);

        JMenu mnEdit = new JMenu("Edit");
        menuBar.add(mnEdit);

        mnEdit.add(new EditFrameAction("End curve", KeyEvent.VK_E, "typed .") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().endCurve();
                }
            });

        mnEdit.add(new EditFrameAction
                   ("Start a new curve connected to active point",
                    KeyEvent.VK_S, "typed ,") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().startConnectedCurve();
                }
            });

        mnEdit.add(new EditFrameAction
                   ("Reinsert closest previously added point",
                    KeyEvent.VK_R,
                    KeyStroke.getKeyStroke('=')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().addNearestPoint();
                }
            });

        mnEdit.add(new EditFrameAction
                   ("Add nearest point on a line segment",
                    KeyEvent.VK_L,
                    KeyStroke.getKeyStroke('_')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().addNearestSegment();
                }
            });

        mnEdit.add(new EditFrameAction("Toggle smoothing",
                                       KeyEvent.VK_T,
                                       KeyStroke.getKeyStroke('o')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().toggleSmoothing();
                }
            });

        mnEdit.add(new EditFrameAction("Cycle active curve",
                                       KeyEvent.VK_C,
                                       KeyStroke.getKeyStroke('/')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().cycleActiveCurve();
                }
            });

        mnEdit.add(new EditFrameAction("Delete last vertex", KeyEvent.VK_D,
                                       "DELETE") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().removeCurrentVertex();
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

        JMenu mnView = new JMenu("View");
        menuBar.add(mnView);

        mnView.add(new EditFrameAction("Zoom In", KeyEvent.VK_I,
                                       "typed +") {
                public void actionPerformed(ActionEvent e) {
                    getParentEditor().zoomBy(1.5);
                }
            });
        mnView.add(new EditFrameAction("Zoom Out", KeyEvent.VK_O,
                                       "typed -") {
                public void actionPerformed(ActionEvent e) {
                    getParentEditor().zoomBy(1 / 1.5);
                }
            });

        JMenu mnHelp = new JMenu("Help");
        menuBar.add(mnHelp);

        JMenuItem mnAbout = new JMenuItem("About");
        mnAbout.setEnabled(false);
        mnHelp.add(mnAbout);
        // getContentPane().setBorder(new EmptyBorder(5, 5, 5, 5));
    }

    protected void setStatus(String s) {
        statusLabel.setText("<html><font size=\"-2\">" + s 
                            + "</font></html>");
    }

    protected ImagePane newImagePane() {
        return new EditPane(this);
    }

    protected EditPane getEditPane() {
        return (EditPane) getImagePane();
    }
}
