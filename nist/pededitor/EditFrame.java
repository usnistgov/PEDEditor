package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.swing.*;

public class EditFrame extends JFrame {
    static JDialog helpDialog = null;

    protected JPanel contentPane;
    protected JScrollPane scrollPane;
    protected EditPane imagePane;
    protected int preferredWidth = 800;
    protected int preferredHeight = 600;

    protected JPanel statusBar;
    protected JLabel statusLabel;
    protected Editor parentEditor;
    protected ButtonGroup lineStyleGroup = new ButtonGroup();
    protected ButtonGroup lineWidthGroup = new ButtonGroup();

    protected EditFrameAction setLeftComponent = new EditFrameAction
        ("Set left component", KeyEvent.VK_L) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setComponent(0);
                }
            };

    protected EditFrameAction setRightComponent = new EditFrameAction
        ("Set right component", KeyEvent.VK_R) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setComponent(1);
                }
            };

    protected EditFrameAction setTopComponent = new EditFrameAction
        ("Set top component", KeyEvent.VK_T) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setComponent(2);
                }
            };

    protected EditFrameAction changeXUnits = new EditFrameAction
        ("Change X Units", KeyEvent.VK_X) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().changeXUnits();
                }
            };

    protected EditFrameAction changeYUnits = new EditFrameAction
        ("Change Y Units", KeyEvent.VK_Y) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().changeYUnits();
                }
            };

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

    class LineWidthRadioMenuItem extends JRadioButtonMenuItem {
        LineWidthRadioMenuItem(String imagePath, double lineWidth) {
            super(new LineWidthAction(imagePath, lineWidth));
            lineWidthGroup.add(this);
        }
    }

    class LineStyleAction extends AbstractAction {
        CompositeStroke lineStyle;

        LineStyleAction(String imagePath, CompositeStroke lineStyle) {
            super(null, loadIcon(imagePath));
            this.lineStyle = lineStyle;
        }

        @Override
            public void actionPerformed(ActionEvent e) {
            getParentEditor().setLineStyle(lineStyle);
        }
    }

    class LineStyleRadioMenuItem extends JRadioButtonMenuItem {
        LineStyleRadioMenuItem(String imagePath, CompositeStroke lineStyle) {
            super(new LineStyleAction(imagePath, lineStyle));
            lineStyleGroup.add(this);
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
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(0, 0);
        contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);
        imagePane = new EditPane(this);
        scrollPane = new JScrollPane(imagePane);
        scrollPane.setPreferredSize
            (new Dimension(preferredWidth, preferredHeight));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        boolean editable = parentEditor.isEditable();
        statusBar = new JPanel();
        statusLabel = new JLabel("<html><font size=\"-2\">"
                                 + "No diagram loaded</font></html>");
        statusBar.add(statusLabel);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // "File" top-level menu
        JMenu mnFile = new JMenu("File");
        mnFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(mnFile);

        mnFile.add(new EditFrameAction("New Diagram", KeyEvent.VK_N) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().newDiagram();
                }
            });

        // "Open" submenu
        JMenu mnOpen = new JMenu("Open");
        mnOpen.setMnemonic(KeyEvent.VK_O);
        mnFile.add(mnOpen);

        mnOpen.add(new EditFrameAction("Diagram", KeyEvent.VK_D) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().openDiagram();
                }
            });

        if (editable) {
            mnOpen.add(new EditFrameAction("Image for Digitization", KeyEvent.VK_I) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().openImage(null);
                    }
                });

            // "Save" menu item
            mnFile.add(new EditFrameAction("Save", KeyEvent.VK_S) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().save();
                    }
                });
        }

        // "Save As" submenu
        JMenu mnSaveAs = new JMenu("Save As");
        mnSaveAs.setMnemonic(KeyEvent.VK_A);
        mnFile.add(mnSaveAs);

        if (editable) {
            mnSaveAs.add(new EditFrameAction("PED", KeyEvent.VK_P) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().saveAsPED(null);
                    }
                });
        }

        mnSaveAs.add(new EditFrameAction("PDF", KeyEvent.VK_F) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().saveAsPDF();
                }
            });

        mnSaveAs.add(new EditFrameAction("SVG", KeyEvent.VK_S) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().saveAsSVG();
                }
            });

        mnFile.add(mnSaveAs);

        // "Print" menu item
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


        // "Vertex" top-level menu
        JMenu mnVertex = new JMenu("Vertex");
        mnVertex.setMnemonic(KeyEvent.VK_V);
        menuBar.add(mnVertex);

        if (editable) {
            mnVertex.add(new EditFrameAction
                         ("Enter location",
                          KeyEvent.VK_L,
                          KeyStroke.getKeyStroke('=')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addVertexLocation();
                    }
                });
        }

        mnVertex.add(new EditFrameAction
                   ("Nearest",
                    KeyEvent.VK_N,
                    KeyStroke.getKeyStroke('.')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestPoint(false);
                }
            });

        if (editable) {
            mnVertex.add(new EditFrameAction
                         ("Select nearest",
                          KeyEvent.VK_S,
                          KeyStroke.getKeyStroke('?')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().seekNearestPoint(true);
                    }
                });
        }

        mnVertex.add(new EditFrameAction
                   ("Nearest on curve",
                    KeyEvent.VK_C,
                    KeyStroke.getKeyStroke('_')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestSegment();
                }
            });

        if (editable) {

            mnVertex.add(new EditFrameAction("Move",
                                             KeyEvent.VK_M,
                                             KeyStroke.getKeyStroke('m')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().moveVertex();
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
        }


        // "Curve" top-level menu
        JMenu mnCurve = new JMenu("Curve");
        mnCurve.setMnemonic(KeyEvent.VK_C);

        mnCurve.add(new EditFrameAction("Deselect",
                                        KeyEvent.VK_D, "pressed END") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().deselectCurve();
                }
            });

        if (editable) {
            mnCurve.add(new EditFrameAction
                        ("Add cusp", KeyEvent.VK_C, "typed ,") {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addCusp();
                    }
                });
        }

        mnCurve.add(new EditFrameAction("Select last", KeyEvent.VK_L) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().cycleActiveCurve(-1);
                }
            });

        mnCurve.add(new EditFrameAction("Select next", KeyEvent.VK_N) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().cycleActiveCurve(+1);
                }
            });

        if (editable) {
            mnCurve.add(new EditFrameAction("Toggle smoothing",
                                            KeyEvent.VK_S,
                                            KeyStroke.getKeyStroke('s')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().toggleSmoothing();
                    }
                });

            mnCurve.add(new EditFrameAction("Toggle curve closure",
                                            KeyEvent.VK_C,
                                            KeyStroke.getKeyStroke('c')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().toggleCurveClosure();
                    }
                });

            JMenu mnLineStyle = new JMenu("Line style");
            mnLineStyle.setMnemonic(KeyEvent.VK_T);
        
            LineStyleRadioMenuItem solidLineItem = 
                new LineStyleRadioMenuItem("images/line.png",
                                           CompositeStroke.getSolidLine());
            solidLineItem.setSelected(true);
            mnLineStyle.add(solidLineItem);
            mnLineStyle.add
                (new LineStyleRadioMenuItem("images/dashedline.png",
                                            CompositeStroke.getDashedLine()));
            mnLineStyle.add
                (new LineStyleRadioMenuItem("images/dottedline.png",
                                            CompositeStroke.getDottedLine()));
            mnLineStyle.add
                (new LineStyleRadioMenuItem("images/dashdotline.png",
                                            CompositeStroke.getDotDashLine()));
            mnLineStyle.add
                (new LineStyleRadioMenuItem("images/railroadline.png",
                                            CompositeStroke.getRailroadLine()));
            mnCurve.add(mnLineStyle);

            JMenu mnLineWidth = new JMenu("Line width");
            mnLineWidth.setMnemonic(KeyEvent.VK_W);
            mnLineWidth.add(new LineWidthRadioMenuItem("images/line1.png", 0.0006));
            LineWidthRadioMenuItem normalWidthItem = 
                new LineWidthRadioMenuItem("images/line2.png", 0.0012);
            normalWidthItem.setSelected(true);
            mnLineWidth.add(normalWidthItem);
            mnLineWidth.add(new LineWidthRadioMenuItem("images/line4.png", 0.0024));
            mnLineWidth.add(new LineWidthRadioMenuItem("images/line8.png", 0.0048));
            mnLineWidth.add(new JRadioButtonMenuItem
                            (new AbstractAction("Custom...") {
                                    @Override
                                        public void actionPerformed(ActionEvent e) {
                                        getParentEditor().customLineWidth();
                                    }
                                }));
            mnCurve.add(mnLineWidth);

            mnCurve.add(new EditFrameAction
                        ("Reverse vertex order", KeyEvent.VK_R) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().reverseInsertionOrder();
                    }
                });
        }

        JMenuItem mnCopyCurve = new JMenuItem("Copy");
        mnCopyCurve.setEnabled(false);
        mnCurve.add(mnCopyCurve);

        if (editable) {
            JMenuItem mnGradient = new JMenuItem("Apply gradient");
            mnGradient.setEnabled(false);
            mnCurve.add(mnGradient);
        }
        menuBar.add(mnCurve);


        // "Label" top-level menu
        JMenu mnLabel = new JMenu("Decorations");

        if (editable) {
            mnLabel.add(new EditFrameAction
                        ("New label",
                         KeyEvent.VK_N,
                         KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addLabel();
                    }
                });

            mnLabel.add(new EditFrameAction
                        ("Edit label",
                         KeyEvent.VK_E,
                         KeyStroke.getKeyStroke('e')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().editLabel();
                    }
                });

            mnLabel.add(new EditFrameAction
                        ("Change label font", KeyEvent.VK_F) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().setLabelFont();
                    }
                });

            mnLabel.add(new EditFrameAction
                        ("Add dot", KeyEvent.VK_D, "typed d") {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addDot();
                    }
                });

            mnLabel.add(new EditFrameAction("Add left arrowhead",
                                            KeyEvent.VK_L,
                                            KeyStroke.getKeyStroke('<')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addArrow(false);
                    }
                });

            mnLabel.add(new EditFrameAction("Add right arrowhead",
                                            KeyEvent.VK_R,
                                            KeyStroke.getKeyStroke('>')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addArrow(true);
                    }
                });

            mnLabel.add(new EditFrameAction("Delete nearest symbol",
                                            KeyEvent.VK_D,
                                            KeyStroke.getKeyStroke('z')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().deleteSymbol();
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
        }

        if (mnLabel.getItemCount() > 0) {
            menuBar.add(mnLabel);
        }

        // "Diagram" top-level menu
        JMenu mnDiagram = new JMenu("Diagram");
        if (editable) {
            mnDiagram.add(new EditFrameAction
                          ("Margins...", KeyEvent.VK_M) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().editMargins();
                    }
                });

            JMenu mnComponents = new JMenu("Components");
            mnComponents.add(setLeftComponent);
            mnComponents.add(setRightComponent);
            mnComponents.add(setTopComponent);
            mnDiagram.add(mnComponents);

            mnDiagram.add(new EditFrameAction
                          ("C-to-F...", KeyEvent.VK_T) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().cToF();
                    }
                });
        }

        if (mnDiagram.getItemCount() > 0) {
            menuBar.add(mnDiagram);
        }

        if (editable) {
            JMenu mnAxes = new JMenu("Axes");
            menuBar.add(mnAxes);
            mnAxes.add(changeXUnits);
            mnAxes.add(changeYUnits);
        }


        // "View" top-level menu
        JMenu mnView = new JMenu("View");
        mnView.setMnemonic(KeyEvent.VK_V);
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
        mnView.add(new EditFrameAction("Best Fit", KeyEvent.VK_B,
                                       KeyStroke.getKeyStroke("control B")) {
                public void actionPerformed(ActionEvent e) {
                    getParentEditor().bestFit();
                }
            });

        JMenu mnHelp = new JMenu("Help");
        mnHelp.setMnemonic(KeyEvent.VK_H);
        menuBar.add(mnHelp);
        mnHelp.add(new EditFrameAction("Help", KeyEvent.VK_H, "F1") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    help();
                }
            });

        JMenuItem mnAbout = new JMenuItem("About");
        mnAbout.setEnabled(false);
        mnHelp.add(mnAbout);
    }

    /** Set the maximum number of components in the diagram. A
        schematic/"other" has 0; a binary diagram may have 2 (this
        program has no specific understanding of binary diagrams with
        4 components, so such diagrams should be treated as
        0-component diagrams); and a ternary diagram has 3.

        TODO Maybe users should be allowed to make schematics with as
        many diagram components as they want?
    */
    void setComponentCount(int n) {
        setLeftComponent.setEnabled(n >= 2);
        setRightComponent.setEnabled(n >= 2);
        setTopComponent.setEnabled(n >= 3);
    }

    /** Set the number of meaningful axes that the diagram has. */
    void setAxisCount(int n) {
        changeXUnits.setEnabled(n >= 1);
        changeYUnits.setEnabled(n >= 2);
    }

    protected void help() {
        if (helpDialog == null) {
            String filename = "edithelp.html";
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
            
            helpDialog = new JDialog(this, "PED Edit Window Help");
            helpDialog.getContentPane().add(editorScrollPane);
            helpDialog.pack();
        }
        Rectangle r = getBounds();
        helpDialog.setLocation(r.x + r.width, r.y);
        helpDialog.setVisible(true);
        helpDialog.toFront();
    }

    protected void setStatus(String s) {
        statusLabel.setText("<html><font size=\"-2\">" + s 
                            + "</font></html>");
    }

    protected EditPane getEditPane() {
        return (EditPane) imagePane;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
