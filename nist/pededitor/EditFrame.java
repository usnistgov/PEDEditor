package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;

import javax.swing.*;

public class EditFrame extends ImageScrollFrame {
    protected JPanel statusBar;
    protected JLabel statusLabel;
    protected Editor parentEditor;
    protected JCheckBoxMenuItem smoothingMenuItem;

    public Editor getParentEditor() { return parentEditor; }

    public JCheckBoxMenuItem getSmoothingMenuItem() {
        return smoothingMenuItem;
    }

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

    static Icon loadIcon(String imagePath) {
        URL url = EditFrame.class.getResource(imagePath);
        if (url == null) {
            throw new IllegalStateException("Could not load " + imagePath);
        }
        Icon icon = new ImageIcon(url);
        if (icon == null) {
            throw new IllegalStateException("Could not load image " + imagePath);
        }
        return icon;
    }

    class LineWidthAction extends AbstractAction {
        double lineWidth;

        LineWidthAction(String imagePath, double lineWidth) {
            super(null, loadIcon(imagePath));
            this.lineWidth = lineWidth;
        }

        @Override
            public void actionPerformed(ActionEvent e) {
            getParentEditor().setLineWidth(lineWidth);
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

        JMenuItem mnSave = new JMenuItem("Save");
        mnFile.add(mnSave);
        mnSave.setEnabled(false);

        JMenu mnSaveAs = new JMenu("Save As");
        mnFile.add(mnSaveAs);

        mnSaveAs.add(new EditFrameAction("PDF", KeyEvent.VK_P) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().saveAsPDF();
                }
            });

        mnSaveAs.add(new EditFrameAction("SVG", KeyEvent.VK_P) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().saveAsSVG();
                }
            });

        mnFile.add(new EditFrameAction("Print", KeyEvent.VK_P) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().print();
                }
            });

        JSeparator separator = new JSeparator();
        mnFile.add(separator);

        JMenuItem mnExit = new JMenuItem("Exit");
        mnFile.add(mnExit);
        mnExit.setEnabled(false);


        JMenu mnVertex = new JMenu("Vertex");
        menuBar.add(mnVertex);

        mnVertex.add(new EditFrameAction
                   ("Enter location",
                    KeyEvent.VK_L,
                    KeyStroke.getKeyStroke('=')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().addVertexLocation();
                }
            });

        mnVertex.add(new EditFrameAction
                   ("Duplicate nearest",
                    KeyEvent.VK_D,
                    KeyStroke.getKeyStroke('\"')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().addNearestPoint();
                }
            });

        mnVertex.add(new EditFrameAction
                   ("Select nearest",
                    KeyEvent.VK_S,
                    KeyStroke.getKeyStroke('?')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().selectNearestPoint();
                }
            });

        mnVertex.add(new EditFrameAction
                   ("Add on curve",
                    KeyEvent.VK_C,
                    KeyStroke.getKeyStroke('_')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().addNearestSegment();
                }
            });

        mnVertex.add(new EditFrameAction("Delete",
                                       KeyEvent.VK_D,
                                       "DELETE") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().removeCurrentVertex();
                }
            });

        mnVertex.addSeparator();

        JMenuItem mnAdjust = new JMenuItem("Adjust");
        mnAdjust.setEnabled(false);
        mnVertex.add(mnAdjust);

        AdjustAction[] arrows =
            { new AdjustAction("Up", KeyEvent.VK_U, "UP", 0, -1),
              new AdjustAction("Down", KeyEvent.VK_D, "DOWN", 0, 1),
              new AdjustAction("Left", KeyEvent.VK_L, "LEFT", -1, 0),
              new AdjustAction("Right", KeyEvent.VK_R, "RIGHT", 1, 0) };
        for (AdjustAction a : arrows) {
            mnVertex.add(a);
        }


        JMenu mnCurve = new JMenu("Curve");
        menuBar.add(mnCurve);

        mnCurve.add(new EditFrameAction("New",
                                        KeyEvent.VK_N, "typed .") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().endCurve();
                }
            });

        mnCurve.add(new EditFrameAction
                   ("Add cusp", KeyEvent.VK_C, "typed ,") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().startConnectedCurve();
                }
            });


        mnCurve.add(new EditFrameAction("Select last",
                                       KeyEvent.VK_L,
                                       KeyStroke.getKeyStroke('<')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().cycleActiveCurve();
                }
            });

        smoothingMenuItem = new JCheckBoxMenuItem
            (new EditFrameAction("Toggle smoothing",
                                 KeyEvent.VK_T,
                                 KeyStroke.getKeyStroke('o')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().toggleSmoothing();
                    }
                });
        mnCurve.add(smoothingMenuItem);

        mnCurve.add(new EditFrameAction
                   ("Line style", KeyEvent.VK_S) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setLineStyle();
                }
            });

        JMenu mnLineWidth = new JMenu("Line width");
        mnLineWidth.add(new LineWidthAction("images/line1.png", 0.0006));
        mnLineWidth.add(new LineWidthAction("images/line2.png", 0.0012));
        mnLineWidth.add(new LineWidthAction("images/line4.png", 0.0024));
        mnCurve.add(mnLineWidth);

        mnCurve.add(new EditFrameAction
                    ("Line width", KeyEvent.VK_W) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setLineWidth();
                }
            });

        mnCurve.add(new EditFrameAction
                   ("Reverse vertex order", KeyEvent.VK_R) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().reverseInsertionOrder();
                }
            });

        JMenuItem mnCopyCurve = new JMenuItem("Copy");
        mnCopyCurve.setEnabled(false);
        mnCurve.add(mnCopyCurve);

        JMenuItem mnGradient = new JMenuItem("Apply gradient");
        mnGradient.setEnabled(false);
        mnCurve.add(mnGradient);

        JMenu mnLabel = new JMenu("Label");
        menuBar.add(mnLabel);
        mnLabel.add(new EditFrameAction
                   ("Text",
                    KeyEvent.VK_T,
                    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setLabelText();
                }
            });

        mnLabel.add(new EditFrameAction
                   ("Anchor", KeyEvent.VK_A) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setLabelAnchor();
                }
            });

        mnLabel.add(new EditFrameAction
                   ("Angle", KeyEvent.VK_N) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setLabelAngle();
                }
            });

        mnLabel.add(new EditFrameAction
                   ("Font", KeyEvent.VK_N) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setLabelFont();
                }
            });

        mnLabel.add(new EditFrameAction
                   ("Compute Chemical Label Coordinates", KeyEvent.VK_C) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    // TODO not defined yet...
                    // getParentEditor().computeLabelCoordinates();
                }
            });
        

        // TODO Haven't figured out what goes here yet...
        // JMenu mnAxis = new JMenu("Axis");
        // menuBar.add(mnAxis);

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
