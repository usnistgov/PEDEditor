package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.swing.*;

public class EditFrame extends JFrame {
    static JDialog helpDialog = null;
    static JDialog aboutDialog = null;

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
    protected ButtonGroup backgroundImageGroup = new ButtonGroup();
    protected JMenu mnBackgroundImage = new JMenu("Background Image");
    protected JRadioButtonMenuItem grayBackgroundImage;
    protected JRadioButtonMenuItem blinkBackgroundImage;
    protected JRadioButtonMenuItem noBackgroundImage;

    // How to show the original scanned image in the background of
    // the new diagram:
    enum BackgroundImage
    { GRAY, // White parts look white, black parts appear gray
      BLINK, // Blinks on and off
      NONE // Not shown
    };

    public BackgroundImage getBackgroundImage() {
        return !mnBackgroundImage.isEnabled() ? BackgroundImage.NONE
            : blinkBackgroundImage.isSelected() ? BackgroundImage.BLINK
            : grayBackgroundImage.isSelected() ? BackgroundImage.GRAY
            : BackgroundImage.NONE;
    }

    public void setBackgroundImage(BackgroundImage value) {
        switch (value) {
        case NONE:
            noBackgroundImage.setSelected(true);
            break;
        case GRAY:
            grayBackgroundImage.setSelected(true);
            break;
        case BLINK:
            blinkBackgroundImage.setSelected(true);
            break;
        }
    }

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

    class LineWidthMenuItem extends JRadioButtonMenuItem {
        LineWidthMenuItem(String imagePath, double lineWidth) {
            super(new LineWidthAction(imagePath, lineWidth));
            lineWidthGroup.add(this);
        }
    }

    class LineStyleAction extends AbstractAction {
        StandardStroke lineStyle;

        LineStyleAction(String imagePath, StandardStroke lineStyle) {
            super(null, loadIcon(imagePath));
            this.lineStyle = lineStyle;
        }

        @Override
            public void actionPerformed(ActionEvent e) {
            getParentEditor().setLineStyle(lineStyle);
        }
    }

    class LineStyleMenuItem extends JRadioButtonMenuItem {
        LineStyleMenuItem(String imagePath, StandardStroke lineStyle) {
            super(new LineStyleAction(imagePath, lineStyle));
            lineStyleGroup.add(this);
        }
    }

    class BackgroundImageAction extends AbstractAction {
        BackgroundImage value;

        BackgroundImageAction(String name, BackgroundImage value,
                              int mnemonic) {
            super(name);
            putValue(MNEMONIC_KEY, new Integer(mnemonic));
            this.value = value;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().setBackground(value);
        }
    }

    class BackgroundImageMenuItem extends JRadioButtonMenuItem {
        BackgroundImageMenuItem(String name, BackgroundImage back,
                                int mnemonic) {
            super(new BackgroundImageAction(name, back, mnemonic));
            backgroundImageGroup.add(this);
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
            getParentEditor().move(dx, dy);
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
        JMenu mnEdit = new JMenu("Edit");
        mnEdit.setMnemonic(KeyEvent.VK_E);
        menuBar.add(mnEdit);

        if (editable) {

            mnEdit.add(new EditFrameAction("Move",
                                             KeyEvent.VK_M,
                                             KeyStroke.getKeyStroke('m')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().moveSelection();
                    }
                });

            mnEdit.add(new EditFrameAction("Copy",
                                             KeyEvent.VK_C,
                                             KeyStroke.getKeyStroke('c')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().copySelection();
                    }
                });

            mnEdit.add(new EditFrameAction("Delete",
                                             KeyEvent.VK_D,
                                             "DELETE") {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().removeSelection();
                    }
                });

            mnEdit.add(new EditFrameAction("Deselect",
                                           KeyEvent.VK_E, "pressed END") {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().deselectCurve();
                    }
                });
        }

        // "Vertex" top-level menu
        JMenu mnPosition = new JMenu("Position");
        mnPosition.setMnemonic(KeyEvent.VK_P);
        menuBar.add(mnPosition);

        if (editable) {
            mnPosition.add(new EditFrameAction
                         ("Enter coordinates",
                          KeyEvent.VK_L,
                          KeyStroke.getKeyStroke('=')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addVertexLocation();
                    }
                });
        }

        mnPosition.add(new EditFrameAction
                   ("Nearest key point",
                    KeyEvent.VK_N,
                    KeyStroke.getKeyStroke('p')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestPoint(false);
                }
            });

        if (editable) {
            mnPosition.add(new EditFrameAction
                         ("Select nearest key point",
                          KeyEvent.VK_S,
                          KeyStroke.getKeyStroke('P')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().seekNearestPoint(true);
                    }
                });
        }

        mnPosition.add(new EditFrameAction
                   ("Nearest line/curve",
                    KeyEvent.VK_L,
                    KeyStroke.getKeyStroke('l')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestSegment(false);
                }
            });

        if (editable) {
            mnPosition.add(new EditFrameAction
                        ("Select nearest line/curve",
                         KeyEvent.VK_I,
                         KeyStroke.getKeyStroke('L')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().seekNearestSegment(true);
                    }
                });
        }

        mnPosition.addSeparator();

        AdjustAction[] arrows =
            { new AdjustAction("Up", KeyEvent.VK_U, "UP", 0, -1),
              new AdjustAction("Down", KeyEvent.VK_D, "DOWN", 0, 1),
              new AdjustAction("Left", KeyEvent.VK_L, "LEFT", -1, 0),
              new AdjustAction("Right", KeyEvent.VK_R, "RIGHT", 1, 0) };
        for (AdjustAction a : arrows) {
                mnPosition.add(a);
        }


        // "Curve" top-level menu
        JMenu mnCurve = new JMenu("Curve");
        mnCurve.setMnemonic(KeyEvent.VK_C);

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
                                            KeyEvent.VK_O,
                                            KeyStroke.getKeyStroke('o')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().toggleCurveClosure();
                    }
                });

            JMenu mnLineStyle = new JMenu("Line style");
            mnLineStyle.setMnemonic(KeyEvent.VK_T);
        
            LineStyleMenuItem solidLineItem = 
                new LineStyleMenuItem("images/line.png",
                                           StandardStroke.SOLID);
            solidLineItem.setSelected(true);
            mnLineStyle.add(solidLineItem);
            mnLineStyle.add
                (new LineStyleMenuItem("images/dashedline.png",
                                       StandardStroke.DASH));
            mnLineStyle.add
                (new LineStyleMenuItem("images/dottedline.png",
                                            StandardStroke.DOT));
            mnLineStyle.add
                (new LineStyleMenuItem("images/dashdotline.png",
                                            StandardStroke.DOT_DASH));
            mnLineStyle.add
                (new LineStyleMenuItem("images/railroadline.png",
                                            StandardStroke.RAILROAD));
            mnLineStyle.add
                (new LineStyleMenuItem("images/soliddotline.png",
                                            StandardStroke.SOLID_DOT));
            mnCurve.add(mnLineStyle);

            JMenu mnLineWidth = new JMenu("Line width");
            mnLineWidth.setMnemonic(KeyEvent.VK_W);
            mnLineWidth.add(new LineWidthMenuItem("images/line1.png", 0.0006));
            LineWidthMenuItem normalWidthItem = 
                new LineWidthMenuItem("images/line2.png", 0.0012);
            normalWidthItem.setSelected(true);
            mnLineWidth.add(normalWidthItem);
            mnLineWidth.add(new LineWidthMenuItem("images/line4.png", 0.0024));
            mnLineWidth.add(new LineWidthMenuItem("images/line8.png", 0.0048));
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
            JMenuItem mnGradient = new JMenuItem("Apply gradient");
            mnGradient.setEnabled(false);
            mnCurve.add(mnGradient);
        }
        menuBar.add(mnCurve);


        // "Decorations" top-level menu
        JMenu mnDecorations = new JMenu("Decorations");
        mnDecorations.setMnemonic(KeyEvent.VK_D);

        if (editable) {
            mnDecorations.add(new EditFrameAction
                        ("New label",
                         KeyEvent.VK_N,
                         KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addLabel();
                    }
                });

            mnDecorations.add(new EditFrameAction
                        ("Edit label",
                         KeyEvent.VK_E,
                         KeyStroke.getKeyStroke('e')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().editLabel();
                    }
                });

            mnDecorations.add(new EditFrameAction
                        ("Add dot", KeyEvent.VK_D, "typed d") {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addDot();
                    }
                });

            mnDecorations.add(new EditFrameAction("Add left arrowhead",
                                            KeyEvent.VK_L,
                                            KeyStroke.getKeyStroke('<')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addArrow(false);
                    }
                });

            mnDecorations.add(new EditFrameAction("Add right arrowhead",
                                            KeyEvent.VK_R,
                                            KeyStroke.getKeyStroke('>')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addArrow(true);
                    }
                });

            mnDecorations.add(new EditFrameAction
                        ("Compute Chemical Label Coordinates", KeyEvent.VK_C) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        // TODO not defined yet...
                        // getParentEditor().computeLabelCoordinates();
                    }
                });

            mnDecorations.add(new EditFrameAction
                        ("Add tie lines", KeyEvent.VK_T) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addTieLine();
                    }
                });
        }

        if (mnDecorations.getItemCount() > 0) {
            menuBar.add(mnDecorations);
        }

        // "Layout" top-level menu
        JMenu mnLayout = new JMenu("Layout");
        mnLayout.setMnemonic(KeyEvent.VK_L);

        if (editable) {
            JMenu mnAxes = new JMenu("Axes");
            mnAxes.setMnemonic(KeyEvent.VK_X);
            mnLayout.add(mnAxes);
            mnAxes.add(changeXUnits);
            mnAxes.add(changeYUnits);

            mnLayout.add(new EditFrameAction
                          ("Margins...", KeyEvent.VK_M) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().editMargins();
                    }
                });

            JMenu mnComponents = new JMenu("Components");
            setLeftComponent.setEnabled(false);
            setRightComponent.setEnabled(false);
            setTopComponent.setEnabled(false);
            mnComponents.add(setLeftComponent);
            mnComponents.add(setRightComponent);
            mnComponents.add(setTopComponent);
            mnLayout.add(mnComponents);
        }

        if (mnLayout.getItemCount() > 0) {
            menuBar.add(mnLayout);
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

        if (editable) {
            mnBackgroundImage.setMnemonic(KeyEvent.VK_B);
            mnBackgroundImage.setEnabled(false);
            grayBackgroundImage = new BackgroundImageMenuItem
                ("Gray", BackgroundImage.GRAY, KeyEvent.VK_G);
            grayBackgroundImage.setSelected(true);
            blinkBackgroundImage = new BackgroundImageMenuItem
                ("Blink", BackgroundImage.BLINK, KeyEvent.VK_B);
            noBackgroundImage = new BackgroundImageMenuItem
                ("Hide", BackgroundImage.NONE, KeyEvent.VK_N);
            mnBackgroundImage.add(grayBackgroundImage);
            mnBackgroundImage.add(blinkBackgroundImage);
            mnBackgroundImage.add(noBackgroundImage);
            mnView.add(mnBackgroundImage);
        }

        JMenu mnHelp = new JMenu("Help");
        mnHelp.setMnemonic(KeyEvent.VK_H);
        menuBar.add(mnHelp);
        mnHelp.add(new EditFrameAction("Help", KeyEvent.VK_H, "F1") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    help();
                }
            });

        mnHelp.add(new EditFrameAction("About", KeyEvent.VK_A) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    about();
                }
            });
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
            helpDialog = new HTMLDialog
                (this, "edithelp.html", "PED Edit Window Help");
        }
        helpDialog.setVisible(true);
        helpDialog.toFront();
    }

    protected void about() {
        if (aboutDialog == null) {
            aboutDialog = new HTMLDialog
                (this, "about.html", "About PED Editor");
        }
        aboutDialog.setVisible(true);
        aboutDialog.toFront();
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
