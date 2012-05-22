package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.EnumSet;
import java.util.Enumeration;

import javax.swing.*;

@SuppressWarnings("serial")
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
    protected ButtonGroup fontGroup = new ButtonGroup();
    protected JMenu mnBackgroundImage = new JMenu("Background Image");
    protected JRadioButtonMenuItem grayBackgroundImage;
    protected JRadioButtonMenuItem blinkBackgroundImage;
    protected JRadioButtonMenuItem noBackgroundImage;
    protected Action setAspectRatio;
    protected JSeparator mnTagsSeparator = new JSeparator();
    protected JMenuItem mnRemoveTag = new JMenuItem("Delete");
    protected JMenu mnTags = new JMenu("Tags");

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

    protected Action setLeftComponent = new Action
        ("Set left component", KeyEvent.VK_L) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setDiagramComponent(Side.LEFT);
                }
            };

    protected Action setRightComponent = new Action
        ("Set right component", KeyEvent.VK_R) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setDiagramComponent(Side.RIGHT);
                }
            };

    protected Action setTopComponent = new Action
        ("Set top component", KeyEvent.VK_T) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().setDiagramComponent(Side.TOP);
                }
            };

    protected Action scaleXUnits = new Action
        ("X Axis", KeyEvent.VK_X) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().scaleXUnits();
                }
            };

    protected Action scaleYUnits = new Action
        ("Y Axis", KeyEvent.VK_Y) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().scaleYUnits();
                }
            };

    public Editor getParentEditor() { return parentEditor; }

    abstract class Action extends AbstractAction {
        Action(String name, int mnemonic, KeyStroke accelerator) {
            super(name);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
            putValue(ACCELERATOR_KEY, accelerator);
        }

        Action(String name, int mnemonic, String accelerator) {
            this(name, mnemonic, KeyStroke.getKeyStroke(accelerator));
        }

        Action(String name, int mnemonic) {
            super(name);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
        }

        Action(String name) {
            super(name);
        }
    }

    class MarginAction extends Action {
        Side side;
        MarginAction(Side side) {
            super(side.toString());
            this.side = side;
            char ch = side.toString().charAt(0);
            int code = (ch == 'L') ? KeyEvent.VK_L
                : (ch == 'R') ? KeyEvent.VK_R
                : (ch == 'T') ? KeyEvent.VK_T
                : (ch == 'B') ? KeyEvent.VK_B
                : 0;
            putValue(MNEMONIC_KEY, code);
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().setMargin(side);
        }
    }

    class FontAction extends Action {
        String fontName;

        FontAction(String label, String fontName) {
            super(label);
            this.fontName = fontName;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().setFontName(fontName);
            getParentEditor().initializeLabelViews();
            repaint();
        }
    }

    class FontMenuItem extends JRadioButtonMenuItem {
        FontMenuItem(String label, String fontName) {
            super(new FontAction(label, fontName));
            fontGroup.add(this);
        }
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
            this (loadIcon(imagePath), lineStyle);
        }

        LineStyleAction(Icon icon, StandardStroke lineStyle) {
            super(null, icon);
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

        LineStyleMenuItem(Icon icon, StandardStroke lineStyle) {
            super(new LineStyleAction(icon, lineStyle));
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
    class AdjustAction extends Action {
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

    class RemoveTagAction extends Action {
        RemoveTagAction(String tag) {
            super(tag);
        }

        @Override
            public void actionPerformed(ActionEvent e) {
            getParentEditor().removeTag(e.getActionCommand());
        }
    }

    static Icon loadIcon(String imagePath) {
        URL url = EditFrame.class.getResource(imagePath);
        if (url == null) {
            throw new IllegalStateException("Could not load " + imagePath);
        }
        Icon icon = new ImageIcon(url);
        return icon;
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

        mnFile.add(new Action("New Diagram", KeyEvent.VK_N) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().newDiagram();
                }
            });

        // "Open" submenu
        JMenu mnOpen = new JMenu("Open");
        mnOpen.setMnemonic(KeyEvent.VK_O);
        mnFile.add(mnOpen);

        mnOpen.add(new Action("Diagram", KeyEvent.VK_D) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().openDiagram();
                }
            });

        if (editable) {
            mnOpen.add(new Action("Image for Digitization", KeyEvent.VK_I) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().openImage(null);
                    }
                });

            // "Save" menu item
            mnFile.add(new Action("Save", KeyEvent.VK_S) {
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
            mnSaveAs.add(new Action("PED", KeyEvent.VK_P) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().saveAsPED(null);
                    }
                });
        }

        mnSaveAs.add(new Action("PDF", KeyEvent.VK_F) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().saveAsPDF();
                }
            });

        mnSaveAs.add(new Action("SVG", KeyEvent.VK_S) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().saveAsSVG();
                }
            });

        mnFile.add(mnSaveAs);

        mnFile.add(new Action("Reload", KeyEvent.VK_R) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().reloadDiagram();
                }
            });

        // "Print" menu item
        mnFile.add(new Action("Print", KeyEvent.VK_P) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().print();
                }
            });

        mnFile.addSeparator();

        JMenuItem mnExit = new JMenuItem("Exit");
        mnFile.add(mnExit);
        mnExit.setEnabled(false);


        // "Edit" top-level menu
        JMenu mnEdit = new JMenu("Edit");
        mnEdit.setMnemonic(KeyEvent.VK_E);
        menuBar.add(mnEdit);

        if (editable) {

            mnEdit.add(new Action
                        ("Color...",
                         KeyEvent.VK_R,
                         KeyStroke.getKeyStroke('r')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().colorSelection();
                    }
                });

            mnEdit.add(new Action("Copy",
                                             KeyEvent.VK_C,
                                             KeyStroke.getKeyStroke('c')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().copySelection();
                    }
                });

            mnEdit.add(new Action("Delete",
                                             KeyEvent.VK_D,
                                             "DELETE") {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().removeSelection();
                    }
                });

            mnEdit.add(new Action("Deselect",
                                           KeyEvent.VK_S, "pressed END") {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().deselectCurve();
                    }
                });

            mnEdit.add(new Action
                        ("Edit selection...",
                         KeyEvent.VK_E,
                         KeyStroke.getKeyStroke('e')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().editSelection();
                    }
                });

            mnEdit.add(new Action("Move",
                                             KeyEvent.VK_M,
                                             KeyStroke.getKeyStroke('m')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().moveSelection(true);
                    }
                });

            mnEdit.add(new Action("Move green item only",
                                             KeyEvent.VK_V,
                                             KeyStroke.getKeyStroke('M')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().moveSelection(false);
                    }
                });
        }

        // "Vertex" top-level menu
        JMenu mnPosition = new JMenu("Position");
        mnPosition.setMnemonic(KeyEvent.VK_P);
        menuBar.add(mnPosition);

        mnPosition.add(new Action
                       ("Jump to selection",
                        KeyEvent.VK_J,
                        KeyStroke.getKeyStroke('j')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().centerSelection();
                }
            });

        mnPosition.add(new Action
                       ("Auto-position",
                        KeyEvent.VK_A,
                        KeyStroke.getKeyStroke('a')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().autoPosition();
                }
            });

        mnPosition.add(new Action
                       ("Enter coordinates",
                        KeyEvent.VK_T,
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().enterPosition();
                }
            });

        mnPosition.add(new Action
                   ("Nearest key point",
                    KeyEvent.VK_N,
                    KeyStroke.getKeyStroke('p')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestPoint(false);
                }
            });

        if (editable) {
            mnPosition.add(new Action
                         ("Select nearest key point",
                          KeyEvent.VK_S,
                          KeyStroke.getKeyStroke('P')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().seekNearestPoint(true);
                    }
                });
        }

        mnPosition.add(new Action
                   ("Nearest line/curve",
                    KeyEvent.VK_L,
                    KeyStroke.getKeyStroke('l')) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestCurve(false);
                }
            });

        if (editable) {
            mnPosition.add(new Action
                        ("Select nearest line/curve",
                         KeyEvent.VK_I,
                         KeyStroke.getKeyStroke('L')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().seekNearestCurve(true);
                    }
                });
        }

        if (editable) {
            mnPosition.add(new Action
                        ("Unstick mouse",
                         KeyEvent.VK_U,
                         KeyStroke.getKeyStroke('u')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().unstickMouse();
                        repaint();
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
            mnCurve.add(new Action("Toggle smoothing",
                                            KeyEvent.VK_S,
                                            KeyStroke.getKeyStroke('s')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().toggleSmoothing();
                    }
                });

            mnCurve.add(new Action("Toggle curve closure",
                                            KeyEvent.VK_O,
                                            KeyStroke.getKeyStroke('o')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().toggleCurveClosure();
                    }
                });

            mnCurve.add(new Action
                        ("Toggle fill", KeyEvent.VK_F, KeyStroke.getKeyStroke('f')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().fill();
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
                (new LineStyleMenuItem("images/soliddotline.png",
                                            StandardStroke.SOLID_DOT));
            mnCurve.add(mnLineStyle);

            JMenu mnRailroad = new JMenu();
            mnRailroad.setIcon(loadIcon("images/railroadline.png"));

            int width = 104;
            int height = 24;

            Shape line = new Line2D.Float(0f, 12f, (float) width, 12f);

            for (StandardStroke railroad:
                     EnumSet.range(StandardStroke.RAILROAD1,
                                   StandardStroke.RAILROAD24)) {
                // Draw a BufferedImage of this line style and create
                // an Icon from it. To consider: do that for all line
                // styles, ditching the pre-drawn images such as
                // "dottedline.png".)
                BufferedImage im = new BufferedImage
                    (width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = im.createGraphics();
                // Set the background color to transparent.
                g.setBackground(new Color(255, 255, 255, 0));
                g.clearRect(0, 0, im.getWidth(), im.getHeight());
                g.setColor(Color.BLACK);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                railroad.getStroke().draw(g, line, 2.0);
                
                mnRailroad.add
                    (new LineStyleMenuItem(new ImageIcon(im), railroad));
            }
            mnLineStyle.add(mnRailroad);

            JMenu mnLineWidth = new JMenu("Line width");
            mnLineWidth.setMnemonic(KeyEvent.VK_W);
            mnLineWidth.add(new LineWidthMenuItem("images/line1.png", 0.0008));
            LineWidthMenuItem normalWidthItem = 
                new LineWidthMenuItem("images/line2.png", 0.0016);
            normalWidthItem.setSelected(true);
            mnLineWidth.add(normalWidthItem);
            mnLineWidth.add(new LineWidthMenuItem("images/line4.png", 0.0032));
            mnLineWidth.add(new LineWidthMenuItem("images/line8.png", 0.0064));
            mnLineWidth.add(new JRadioButtonMenuItem
                            (new AbstractAction("Custom...") {
                                    @Override
                                        public void actionPerformed(ActionEvent e) {
                                        getParentEditor().customLineWidth();
                                    }
                                }));
            mnCurve.add(mnLineWidth);

            mnCurve.add(new Action
                        ("Add vertex", KeyEvent.VK_X,
                         KeyStroke.getKeyStroke('x')) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getParentEditor().addVertex();
                    }
                });

            mnCurve.add(new Action
                        ("Add auto-positioned vertex", KeyEvent.VK_A,
                         KeyStroke.getKeyStroke('X')) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getParentEditor().autoPosition();
                        getParentEditor().addVertex();
                    }
                });

            mnCurve.add(new Action
                        ("Add cusp", KeyEvent.VK_C, "typed ,") {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addCusp();
                    }
                });
        }

        mnCurve.add(new Action("Select previous vertex", KeyEvent.VK_P) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().cycleActiveVertex(-1);
                }
            });

        mnCurve.add(new Action("Select next vertex", KeyEvent.VK_N) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().cycleActiveVertex(+1);
                }
            });

        mnCurve.add(new Action("Select previous curve", KeyEvent.VK_R) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    getParentEditor().cycleActiveCurve(-1);
                }
            });

        mnCurve.add(new Action("Select next curve", KeyEvent.VK_X) {
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
            mnDecorations.add(new Action
                        ("Add text...",
                         KeyEvent.VK_T,
                         KeyStroke.getKeyStroke('t')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addLabel();
                    }
                });

            mnDecorations.add(new Action("Add left arrowhead",
                                            KeyEvent.VK_L,
                                            KeyStroke.getKeyStroke('<')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addArrow(false);
                    }
                });

            mnDecorations.add(new Action("Add right arrowhead",
                                            KeyEvent.VK_R,
                                            KeyStroke.getKeyStroke('>')) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addArrow(true);
                    }
                });

            mnDecorations.add(new Action
                        ("Add ruler", KeyEvent.VK_U) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addRuler();
                    }
                });

            mnDecorations.add(new Action
                        ("Add tie lines", KeyEvent.VK_I) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        getParentEditor().addTieLine();
                    }
                });

            mnDecorations.add(new Action
                        ("Compute Chemical Label Coordinates", KeyEvent.VK_C) {
                    @Override
                        public void actionPerformed(ActionEvent e) {
                        JOptionPane.showMessageDialog
                            (EditFrame.this, "Sorry, this feature doesn't work yet.");
                        // TODO not defined yet...
                        // getParentEditor().computeLabelCoordinates();
                    }
                });
        }

        if (mnDecorations.getItemCount() > 0) {
            menuBar.add(mnDecorations);
        }

        // "Properties" top-level menu
        JMenu mnLayout = new JMenu("Properties");
        mnLayout.setMnemonic(KeyEvent.VK_R);

        if (editable) {
            setAspectRatio = new Action
                ("Aspect ratio", KeyEvent.VK_A) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getParentEditor().setAspectRatio();
                    }
                };
            setAspectRatio.setEnabled(false);
            mnLayout.add(setAspectRatio);

            JMenu mnComponents = new JMenu("Components");
            mnComponents.setMnemonic(KeyEvent.VK_C);
            setTopComponent.setEnabled(false);
            mnComponents.add(setLeftComponent);
            mnComponents.add(setRightComponent);
            mnComponents.add(setTopComponent);
            mnLayout.add(mnComponents);

            JMenu mnFont = new JMenu("Font");
            mnFont.setMnemonic(KeyEvent.VK_F);
            mnFont.add(new FontMenuItem("Sans", "DejaVu LGC Sans PED"));
            mnFont.add(new FontMenuItem("Serif", "DejaVu LGC Serif PED"));
            mnFont.add(new FontMenuItem("Serif (Widely-spaced lines)",
                                        "DejaVu LGC Serif GRUMP"));

            mnLayout.add(mnFont);
        }

        JMenu mnKeys = new JMenu("Key/value pairs");
        mnKeys.setMnemonic(KeyEvent.VK_K);

        if (editable) {
            mnKeys.add(new Action("Add", KeyEvent.VK_A) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getParentEditor().put();
                    }
                });
        }

        mnKeys.add(new Action("List", KeyEvent.VK_L) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().listKeyValues();
                }
            });
        mnLayout.add(mnKeys);

        if (editable) {
            JMenu mnMargins = new JMenu("Margins");
            mnMargins.setMnemonic(KeyEvent.VK_M);
            for (Side side: Side.values()) {
                mnMargins.add(new MarginAction(side));
            }
            mnMargins.add(new Action("Compute", KeyEvent.VK_C) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getParentEditor().computeMargins();
                    }
                });

            mnLayout.add(mnMargins);

            JMenu mnScale = new JMenu("Scale");
            mnScale.setMnemonic(KeyEvent.VK_S);
            mnLayout.add(mnScale);
            mnScale.add(scaleXUnits);
            mnScale.add(scaleYUnits);
        }

        mnTags.setMnemonic(KeyEvent.VK_T);
        mnLayout.add(mnTags);

        if (editable) {
            mnTags.add(new Action("Add", KeyEvent.VK_A) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getParentEditor().addTag();
                    }
                });
            mnTags.add(mnTagsSeparator);
            mnRemoveTag.setEnabled(false);
            mnTags.add(mnRemoveTag);

            mnTagsSeparator.setVisible(false);
            mnRemoveTag.setVisible(false);
        } else {
            mnTags.setEnabled(false);
        }

        if (mnLayout.getItemCount() > 0) {
            menuBar.add(mnLayout);
        }

        // "View" top-level menu
        JMenu mnView = new JMenu("View");
        mnView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(mnView);

        mnView.add(new Action("Zoom In", KeyEvent.VK_I,
                                       "typed +") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().zoomBy(1.5);
                }
            });
        mnView.add(new Action("Zoom Out", KeyEvent.VK_O,
                                       "typed -") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().zoomBy(1 / 1.5);
                }
            });
        mnView.add(new Action("Best Fit", KeyEvent.VK_B,
                                       KeyStroke.getKeyStroke("control B")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().bestFit();
                }
            });
        mnView.add(new Action("Center Mouse", KeyEvent.VK_C,
                                       KeyStroke.getKeyStroke("control L")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().centerMouse();
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
        mnHelp.add(new Action("Help", KeyEvent.VK_H, "F1") {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    help();
                }
            });

        mnHelp.add(new Action("About", KeyEvent.VK_A) {
                @Override
                    public void actionPerformed(ActionEvent e) {
                    about();
                }
            });
    }

    /** This method is assumed to be a passive receiver of information
        that the font name has changed, to reflect the change in the
        menu selection. To actively change the font name, use
        Editor#setFontName(s) instead. */
    void setFontName(String s) {
        for (Enumeration<AbstractButton> bs = fontGroup.getElements();
             bs.hasMoreElements();) {
            AbstractButton b = (FontMenuItem) bs.nextElement();
            FontAction fact = (FontAction) b.getAction();
            if (s.equals(fact.fontName)) {
                b.setSelected(true);
                break;
            }
        }
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
        scaleXUnits.setEnabled(n >= 1);
        scaleYUnits.setEnabled(n >= 2);
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

    /** This is not a public interface. It is an interface that Editor
        uses to perform UI operations in support of an addTag()
        request. */
    void addTag(String tag) {
        mnTagsSeparator.setVisible(true);
        int itemCount = mnTags.getItemCount();
        int itemsStart = itemCount;
        for (int i = 0; i < itemCount; ++i) {
            if (mnTags.getItem(i) == mnRemoveTag) {
                itemsStart = i + 1;
                break;
            }
        }

        mnTagsSeparator.setVisible(true);
        mnRemoveTag.setVisible(true);
        for (int i = itemsStart; i <= itemCount; ++i) {
            if (i == itemCount
                || mnTags.getItem(i).getText().compareToIgnoreCase(tag) > 0) {
                mnTags.insert(new RemoveTagAction(tag), i);
                break;
            }
        }
    }

    /** This is not a public interface. It is an interface that Editor
        uses to perform UI operations in support of an addTag()
        request. */
    void removeTag(String tag) {
        int itemCount = mnTags.getItemCount();
        int itemsStart = itemCount;
        for (int i = 0; i < itemCount; ++i) {
            if (mnTags.getItem(i) == mnRemoveTag) {
                itemsStart = i + 1;
                break;
            }
        }

        for (int i = itemsStart; i <= itemCount; ++i) {
            if (mnTags.getItem(i).getText().equals(tag)) {
                mnTags.remove(i);

                if (itemCount <= itemsStart + 1) {
                    // Hide the separator and the list of tags.
                    mnTagsSeparator.setVisible(false);
                    mnRemoveTag.setVisible(false);
                }
                break;
            }
        }
    }
}
