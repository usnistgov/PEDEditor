package gov.nist.pededitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

@SuppressWarnings("serial")
public class EditFrame extends JFrame
    implements Observer {

    protected JPanel contentPane;
    protected JScrollPane scrollPane;
    protected EditPane imagePane;
    protected int preferredWidth = 800;
    protected int preferredHeight = 600;

    protected JPanel statusBar;
    protected JLabel statusLabel;
    protected Editor parentEditor;
    protected ButtonGroup fillStyleGroup = new ButtonGroup();
    protected ButtonGroup lineStyleGroup = new ButtonGroup();
    protected ButtonGroup lineWidthGroup = new ButtonGroup();
    protected ButtonGroup backgroundImageGroup = new ButtonGroup();
    protected ButtonGroup fontGroup = new ButtonGroup();
    protected JMenu mnBackgroundImage = new JMenu("Background Image");
    protected JRadioButtonMenuItem lightGrayBackgroundImage;
    protected JRadioButtonMenuItem darkGrayBackgroundImage;
    protected JRadioButtonMenuItem blackBackgroundImage;
    protected JRadioButtonMenuItem blinkBackgroundImage;
    protected JRadioButtonMenuItem noBackgroundImage;
    protected Action setAspectRatio;

    JMenu mnSelection = new JMenu("Edit Selection");
    JMenu mnCurve = new JMenu("Curve");
    JMenu mnDecorations = new JMenu("Decorations");
    JMenu mnProperties = new JMenu("Properties");
    JMenu mnFont = new JMenu("Font");
    JMenu mnMargins = new JMenu("Margins");

    protected JMenuItem mnSave = new JMenuItem
        (new Action("Save", KeyEvent.VK_S) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().save();
                }
            });
    protected JMenuItem mnSaveAsPED = new JMenuItem
        (new Action("PED", KeyEvent.VK_P) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().saveAsPED();
                }
            });
    protected JMenuItem mnSelectNearestPoint = new JMenuItem
        (new Action("Select nearest key point",
                    KeyEvent.VK_S,
                    KeyStroke.getKeyStroke('Q')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestPoint(true);
                }
            });
    protected JMenuItem mnSelectNearestCurve = new JMenuItem
        (new Action("Select nearest line/curve",
                    KeyEvent.VK_I,
                    KeyStroke.getKeyStroke('W')) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestCurve(true);
                }
            });
    protected JMenuItem mnUnstickMouse = new JMenuItem
        (new Action("Unstick mouse",
                    KeyEvent.VK_U,
                    KeyStroke.getKeyStroke('u')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().unstickMouse();
                    repaint();
                }
            });
    protected JMenuItem mnAddKey = new JMenuItem
        (new Action("Add", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().put();
                }
            });


    protected JSeparator mnTagsSeparator = new JSeparator();
    protected JMenuItem mnRemoveTag = new JMenuItem("Delete");
    protected JMenu mnTags = new JMenu("Tags");
    protected JSeparator mnVariablesSeparator = new JSeparator();
    protected JMenuItem mnRemoveVariable = new JMenuItem("Delete");
    protected JMenu mnVariables = new JMenu("Variables");
    protected JMenu mnScale = new JMenu("Scale");
    protected JMenuItem mnAddTag = new JMenuItem
        (new Action("Add", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().addTag();
                }
            });
    protected JMenuItem mnSetTitle = new JMenuItem
        (new Action("Title", KeyEvent.VK_T) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().setTitle();
                }
            });
    protected JMenuItem mnCopyCoordinatesFromClipboard = new JMenuItem
        (new Action("Copy label or curve coordinates from clipboard",
                    KeyEvent.VK_F) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().copyCoordinatesFromClipboard();
                }
            });
    protected JMenu mnComponents = new JMenu("Components");


    protected transient BackgroundImageType backgroundType = null;
    protected transient BackgroundImageType oldBackgroundType = null;

    // How to show the original scanned image in the background of
    // the new diagram:
    enum BackgroundImageType
    { LIGHT_GRAY, // White parts look white, black parts appear light gray
      DARK_GRAY, // Halfway between light gray and black
      BLACK, // Original appearance
      BLINK, // Blinks on and off
      NONE // Not shown
      };

    public BackgroundImageType getBackgroundImage() {
        return !mnBackgroundImage.isEnabled() ? BackgroundImageType.NONE
            : lightGrayBackgroundImage.isSelected() ? BackgroundImageType.LIGHT_GRAY
            : blinkBackgroundImage.isSelected() ? BackgroundImageType.BLINK
            : darkGrayBackgroundImage.isSelected() ? BackgroundImageType.DARK_GRAY
            : blackBackgroundImage.isSelected() ? BackgroundImageType.BLACK
            : BackgroundImageType.NONE;
    }

    public void setBackgroundType(BackgroundImageType value) {
        switch (value) {
        case NONE:
            noBackgroundImage.setSelected(true);
            break;
        case LIGHT_GRAY:
            lightGrayBackgroundImage.setSelected(true);
            break;
        case DARK_GRAY:
            darkGrayBackgroundImage.setSelected(true);
            break;
        case BLACK:
            blackBackgroundImage.setSelected(true);
            break;
        case BLINK:
            blinkBackgroundImage.setSelected(true);
            break;
        }
    }

    /** setBackgroundTypeEnabled(false) disables all of the background
        type menu items; (true) re-enables them. */
    public void setBackgroundTypeEnabled(boolean enabled) {
        noBackgroundImage.getAction().setEnabled(enabled);
        lightGrayBackgroundImage.getAction().setEnabled(enabled);
        darkGrayBackgroundImage.getAction().setEnabled(enabled);
        blackBackgroundImage.getAction().setEnabled(enabled);
        blinkBackgroundImage.getAction().setEnabled(enabled);
    }

    protected Action setLeftComponent = new Action
        ("Set left component", KeyEvent.VK_L) {
            @Override public void actionPerformed(ActionEvent e) {
                getParentEditor().setDiagramComponent(Side.LEFT);
            }
        };

    protected Action setRightComponent = new Action
        ("Set right component", KeyEvent.VK_R) {
            @Override public void actionPerformed(ActionEvent e) {
                getParentEditor().setDiagramComponent(Side.RIGHT);
            }
        };

    protected Action setTopComponent = new Action
        ("Set top component", KeyEvent.VK_T) {
            @Override public void actionPerformed(ActionEvent e) {
                getParentEditor().setDiagramComponent(Side.TOP);
            }
        };

    protected Action scaleXUnits = new Action
        ("X axis/right component", KeyEvent.VK_X) {
            @Override public void actionPerformed(ActionEvent e) {
                getParentEditor().scaleXUnits();
            }
        };

    protected Action scaleYUnits = new Action
        ("Y axis/top component", KeyEvent.VK_Y) {
            @Override public void actionPerformed(ActionEvent e) {
                getParentEditor().scaleYUnits();
            }
        };

    /** Internal use; called from Editor.java. Make the GUI changes
        necessary to reflect whether we are or aren't currently using weight
        fraction values. */
    void setUsingWeightFraction(boolean b) {
        convertToMole.setEnabled(b);
        convertToWeight.setEnabled(!b);
        usingWeightFraction.setSelected(b);
    }

    /** Internal use; called from Editor.java. */
    void setSmoothed(boolean b) {
        smoothed.setSelected(b);
    }

    void conversionError() {
        JOptionPane.showMessageDialog
            (this,
             "<html><p>The conversion could not be performed.</p>"
             + "<p>Conversions can only be performed on "
             + "diagrams for which the left, right, and (for ternary diagrams) "
             + "top components are defined (using the "
             + "<code>Che<u>m</u>istry/<u>C</u>omponents</code> "
             + "menu) as simple chemical formulas such as \"Ca\" or "
             + "\"Pb3(PO4)2\".</p>"
             + "</html>");
    }

    protected Action convertToMole = new Action
        ("Convert to mole fraction", KeyEvent.VK_C) {
            {
                setEnabled(false);
            }
            @Override public void actionPerformed(ActionEvent e) {
                if (!getParentEditor().weightToMoleFraction()) {
                    conversionError();
                }
            }
        };

    protected Action convertToWeight = new Action
        ("Convert to weight fraction", KeyEvent.VK_C) {
            @Override public void actionPerformed(ActionEvent e) {
                if (!getParentEditor().moleToWeightFraction()) {
                    conversionError();
                }
            }
        };

    protected JCheckBoxMenuItem usingWeightFraction
        = new JCheckBoxMenuItem
        (new Action
         ("Already displaying weight fraction", KeyEvent.VK_W) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().setUsingWeightFraction
                        (usingWeightFraction.isSelected());
                }
            });

    protected JCheckBoxMenuItem editingEnabled
        = new JCheckBoxMenuItem
        (new Action("Show editing options", KeyEvent.VK_W) {
                @Override public void actionPerformed(ActionEvent e) {
                    setEditable(editingEnabled.isSelected());
                }
            });

    protected JCheckBoxMenuItem smoothed
        = new JCheckBoxMenuItem
        (new Action
         ("Smooth through new points", KeyEvent.VK_S,
          KeyStroke.getKeyStroke('s')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().setSmoothed(smoothed.isSelected());
                }
            });

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

    class SaveImageAction extends Action {
        String ext;
        SaveImageAction(String ext, int mnemonic) {
            super(ext);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
            this.ext = ext;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().saveAsImage(ext);
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
            repaint();
        }
    }

    class LayerAction extends Action {
        int layerDelta;

        public LayerAction(String name, int mnemonic, KeyStroke accelerator,
                           int layerDelta) {
            super(name, mnemonic, accelerator);
            this.layerDelta = layerDelta;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().changeLayer(layerDelta);
            repaint();
        }
    }

    class FontMenuItem extends JRadioButtonMenuItem {
        FontMenuItem(String label, String fontName) {
            super(new FontAction(label, fontName));
            fontGroup.add(this);
        }
    }

    static Icon getLineWidthIcon(double lineWidth) {
        int w = (int) Math.round(lineWidth / 0.0008);
        return icon(StandardStroke.SOLID, 25, w, w);
    }

    class LineWidthAction extends AbstractAction {
        double lineWidth;

        LineWidthAction(double lineWidth) {
            super(String.format("%.4f", lineWidth), getLineWidthIcon(lineWidth));
            this.lineWidth = lineWidth;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().setLineWidth(lineWidth);
        }
    }

    class LineWidthMenuItem extends JRadioButtonMenuItem {
        LineWidthMenuItem(double lineWidth) {
            super(new LineWidthAction(lineWidth));
            lineWidthGroup.add(this);
        }
    }

    class LineStyleAction extends AbstractAction {
        StandardStroke lineStyle;

        LineStyleAction(Icon icon, StandardStroke lineStyle) {
            super(null, icon);
            this.lineStyle = lineStyle;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().setLineStyle(lineStyle);
        }
    }

    class FillStyleAction extends AbstractAction {
        StandardFill fill;

        FillStyleAction(StandardFill fill) {
            super(null, icon(fill));
            this.fill = fill;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().setFill(fill);
        }
    }

    class LineStyleMenuItem extends JRadioButtonMenuItem {
        LineStyleMenuItem(StandardStroke lineStyle, int width, int height,
                          double lineWidth) {
            this (icon(lineStyle, width, height, lineWidth), lineStyle);
        }

        LineStyleMenuItem(Icon icon, StandardStroke lineStyle) {
            super(new LineStyleAction(icon, lineStyle));
            lineStyleGroup.add(this);
        }
    }

    class FillStyleMenuItem extends JRadioButtonMenuItem {
        FillStyleMenuItem(StandardFill fill) {
            super(new FillStyleAction(fill));
            fillStyleGroup.add(this);
        }
    }

    class BackgroundImageAction extends AbstractAction {
        BackgroundImageType value;

        BackgroundImageAction(String name, BackgroundImageType value,
                              int mnemonic) {
            super(name);
            putValue(MNEMONIC_KEY, new Integer(mnemonic));
            this.value = value;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().toggleBackgroundType(value);
        }
    }

    

    class BackgroundImageMenuItem extends JRadioButtonMenuItem {
        BackgroundImageMenuItem(String name, BackgroundImageType back,
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

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().move(dx, dy);
        }
    }

    class RemoveTagAction extends Action {
        RemoveTagAction(String tag) {
            super(tag);
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().removeTag(e.getActionCommand());
        }
    }

    class RemoveVariableAction extends Action {
        RemoveVariableAction(String variable) {
            super(variable);
        }

        @Override public void actionPerformed(ActionEvent e) {
            getParentEditor().removeVariable(e.getActionCommand());
        }
    }

    /**
     * Create the frame.
     */
    public EditFrame(Editor parentEditor) {
        this.parentEditor = parentEditor;
        parentEditor.addObserver(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(0, 0);
        contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);
        imagePane = new EditPane(this);
        scrollPane = new JScrollPane(imagePane);
        scrollPane.setPreferredSize
            (new Dimension(preferredWidth, preferredHeight));
        contentPane.add(scrollPane, BorderLayout.CENTER);

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
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().newDiagram();
                }
            });

        mnFile.add(new Action("Open", KeyEvent.VK_O) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().showOpenDialog(EditFrame.this);
                }
            });
        mnFile.add(mnSave);

        // "Save As" submenu
        JMenu mnSaveAs = new JMenu("Save As");
        mnSaveAs.setMnemonic(KeyEvent.VK_A);
        mnFile.add(mnSaveAs);
        mnSaveAs.add(mnSaveAsPED);
        mnSaveAs.add(new Action("PDF", KeyEvent.VK_F) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().saveAsPDF();
                }
            });

        mnSaveAs.add(new SaveImageAction("PNG", KeyEvent.VK_G));

        mnFile.add(mnSaveAs);

        mnFile.add(new Action("Reload", KeyEvent.VK_R) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().reloadDiagram();
                }
            });

        // "Print" menu item
        mnFile.add(new Action("Print", KeyEvent.VK_P) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().print();
                }
            });

        mnFile.addSeparator();

        // "Exit" menu item
        mnFile.add(new Action("Exit", KeyEvent.VK_X) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().close();
                }
            });

        menuBar.add(mnSelection);
        mnSelection.setMnemonic(KeyEvent.VK_E);
        mnSelection.add(new Action
                        ("Color...",
                         KeyEvent.VK_R,
                         KeyStroke.getKeyStroke('r')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().colorSelection();
                }
            });

        mnSelection.add(new Action("Copy",
                                   KeyEvent.VK_C,
                                   KeyStroke.getKeyStroke('c')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().copySelection();
                }
            });

        mnSelection.add(new Action("Copy everything in selected region",
                                   KeyEvent.VK_O,
                                   KeyStroke.getKeyStroke('C')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().copyRegion();
                }
            });

        mnSelection.add(new Action("Delete", KeyEvent.VK_D, "DELETE") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().removeSelection();
                }
            });

        mnSelection.add(new Action("Deselect", KeyEvent.VK_S, "pressed ESCAPE") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().deselectCurve();
                }
            });

        JMenu mnLayer = new JMenu("Layer");
        mnLayer.setMnemonic(KeyEvent.VK_L);
        mnLayer.add
            (new LayerAction
             ("Lower", KeyEvent.VK_L, null, -1));
        mnLayer.add
            (new LayerAction
             ("Raise", KeyEvent.VK_R, null, +1));
        mnLayer.add
            (new LayerAction
             ("To bottom", KeyEvent.VK_B, null, -1000000));
        mnLayer.add
            (new LayerAction
             ("To top", KeyEvent.VK_T, null, +1000000));
        mnSelection.add(mnLayer);

        mnSelection.add(new Action("Move selection only",
                                   KeyEvent.VK_V,
                                   KeyStroke.getKeyStroke('V')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().moveSelection(false);
                }
            });
 
        mnSelection.add(new Action("Move everything at selected point",
                                   KeyEvent.VK_M,
                                   KeyStroke.getKeyStroke('v')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().moveSelection(true);
                }
            });

        mnSelection.add(new Action("Move everything in selected region",
                                   KeyEvent.VK_R,
                                   KeyStroke.getKeyStroke('R')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().moveRegion();
                }
            });

        mnSelection.add(new Action
                        ("Properties...",
                         KeyEvent.VK_P,
                         KeyStroke.getKeyStroke('e')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().editSelection();
                }
            });

        mnSelection.add(new Action("Revert properties to default",
                                   KeyEvent.VK_T,
                                   KeyStroke.getKeyStroke('d')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().resetSelectionToDefaultSettings();
                }
            });

        mnSelection.add(new Action("Make selection's properties the new default",
                                   KeyEvent.VK_F,
                                   KeyStroke.getKeyStroke('D')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().setDefaultSettingsFromSelection();
                }
            });

        // "Position" top-level menu
        JMenu mnPosition = new JMenu("Position");
        mnPosition.setMnemonic(KeyEvent.VK_P);
        menuBar.add(mnPosition);

        mnPosition.add(new Action
                       ("Auto-position",
                        KeyEvent.VK_A,
                        KeyStroke.getKeyStroke('A')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().autoPosition();
                }
            });

        mnPosition.add(new Action
                       ("Enter coordinates",
                        KeyEvent.VK_E,
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().enterPosition();
                }
            });

        mnPosition.add(new Action
                       ("Jump to selection",
                        KeyEvent.VK_J,
                        KeyStroke.getKeyStroke('j')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().centerSelection();
                }
            });

        mnPosition.add(new Action
                       ("Nearest key point",
                        KeyEvent.VK_N,
                        KeyStroke.getKeyStroke('q')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestPoint(false);
                }
            });

        mnPosition.add(new Action
                       ("Nearest line/curve",
                        KeyEvent.VK_L,
                        KeyStroke.getKeyStroke('w')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().seekNearestCurve(false);
                }
            });

        mnPosition.add(mnSelectNearestPoint);
        mnPosition.add(mnSelectNearestCurve);
        mnPosition.add(mnUnstickMouse);

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
        mnCurve.setMnemonic(KeyEvent.VK_C);

        mnCurve.add(createFillMenu());

        JMenu mnLineStyle = new JMenu("Line style");
        mnLineStyle.setMnemonic(KeyEvent.VK_L);
        
        LineStyleMenuItem solidLineItem = 
            new LineStyleMenuItem(StandardStroke.SOLID, 59, 2, 2.0);
        solidLineItem.setSelected(true);
        mnLineStyle.add(solidLineItem);
        mnLineStyle.add(new LineStyleMenuItem
                        (StandardStroke.DOT_DASH, 59, 3, 2.0));
        mnLineStyle.add(new LineStyleMenuItem
                        (StandardStroke.SOLID_DOT, 55, 5, 3.0));

        {
            JMenu mnDensity = new JMenu();
            mnDensity.setIcon(icon(StandardStroke.DASH3, 60, 2, 2.0));

            for (StandardStroke stroke:
                     EnumSet.range(StandardStroke.DASH1, 
                                   StandardStroke.DASH5)) {
                mnDensity.add(new LineStyleMenuItem(stroke, 104, 4, 2.0));
            }
            mnLineStyle.add(mnDensity);
        }

        {
            JMenu mnDensity = new JMenu();
            mnDensity.setIcon(icon(StandardStroke.DOT3, 56, 4, 2.0));

            for (StandardStroke stroke:
                     EnumSet.range(StandardStroke.DOT1, 
                                   StandardStroke.DOT5)) {
                mnDensity.add(new LineStyleMenuItem(stroke, 104, 4, 2.0));
            }
            mnLineStyle.add(mnDensity);
        }

        {
            JMenu mnDensity = new JMenu();
            mnDensity.setIcon(icon(StandardStroke.RAILROAD12, 54, 7, 1.0));

            for (StandardStroke stroke:
                     EnumSet.range(StandardStroke.RAILROAD2,
                                   StandardStroke.RAILROAD24)) {
                mnDensity.add(new LineStyleMenuItem(stroke, 104, 24, 2.0));
            }
            mnLineStyle.add(mnDensity);
        }
        mnCurve.add(mnLineStyle);

        JMenu mnLineWidth = new JMenu("Line width");
        mnLineWidth.setMnemonic(KeyEvent.VK_W);
        double[] lineWidths = {0.0006, 0.0012, 0.0017, 0.0024, 0.0034,
                               0.0048};

        for (int i = 0; i < lineWidths.length; ++i) {
            LineWidthMenuItem item = new LineWidthMenuItem(lineWidths[i]);
            if (i == 3) {
                item.setSelected(true);
            }
            mnLineWidth.add(item);
        }
        mnLineWidth.add
            (new JRadioButtonMenuItem
             (new AbstractAction("Custom...") {
                     @Override public void actionPerformed(ActionEvent e) {
                         getParentEditor().customLineWidth();
                     }
                 }));
        mnCurve.add(mnLineWidth);
        mnCurve.add(smoothed);

        mnCurve.add(new Action("Toggle closure",
                               KeyEvent.VK_O,
                               KeyStroke.getKeyStroke('o')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().toggleCurveClosure();
                }
            });

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
                    ("Toggle point smoothing", KeyEvent.VK_P, "typed ,") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().toggleCusp();
                }
            });

        mnCurve.add(new Action("Select left vertex", KeyEvent.VK_L, "typed [") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().shiftActiveVertex(false);
                }
            });

        mnCurve.add(new Action("Select right vertex", KeyEvent.VK_R, "typed ]") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().shiftActiveVertex(true);
                }
            });

        menuBar.add(mnCurve);

        // "Decorations" top-level menu
        mnDecorations.setMnemonic(KeyEvent.VK_D);
        mnDecorations.add(new Action
                          ("Text...",
                           KeyEvent.VK_T,
                           KeyStroke.getKeyStroke('t')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().addLabel();
                }
            });

        mnDecorations.add(new Action("Left arrowhead",
                                     KeyEvent.VK_L,
                                     KeyStroke.getKeyStroke('<')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().addArrow(false);
                }
            });

        mnDecorations.add(new Action("Right arrowhead",
                                     KeyEvent.VK_R,
                                     KeyStroke.getKeyStroke('>')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().addArrow(true);
                }
            });

        mnDecorations.add(new Action
                          ("Ruler", KeyEvent.VK_U) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().addRuler();
                }
            });

        mnDecorations.add(new Action
                          ("Tie lines", KeyEvent.VK_I) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().addTieLine();
                }
            });
        menuBar.add(mnDecorations);
    
        // "Properties" top-level menu
        mnProperties.setMnemonic(KeyEvent.VK_R);

        setAspectRatio = new Action
            ("Aspect ratio", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().setAspectRatio();
                }
            };
        setAspectRatio.setEnabled(false);
        mnProperties.add(setAspectRatio);

        mnFont.setMnemonic(KeyEvent.VK_F);
        mnFont.add(new FontMenuItem("Sans", "DejaVu LGC Sans PED"));
        mnFont.add(new FontMenuItem("Serif", "DejaVu LGC Serif PED"));
        mnFont.add(new FontMenuItem("Sans (Widely-spaced lines)",
                                    "DejaVu LGC Sans GRUMP"));

        mnProperties.add(mnFont);

        JMenu mnKeys = new JMenu("Key/value pairs");
        mnKeys.setMnemonic(KeyEvent.VK_K);
        mnKeys.add(mnAddKey);

        mnKeys.add(new Action("List", KeyEvent.VK_L) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().listKeyValues();
                }
            });
        mnProperties.add(mnKeys);

        mnMargins.setMnemonic(KeyEvent.VK_M);
        for (Side side: Side.values()) {
            mnMargins.add(new MarginAction(side));
        }
        mnMargins.add(new Action("Auto-fit", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().computeMargins();
                }
            });
        mnMargins.add(new Action("Crop to selection", KeyEvent.VK_P) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().cropToSelection();
                }
            });

        mnProperties.add(mnMargins);

        mnScale.setMnemonic(KeyEvent.VK_S);
        mnScale.add(scaleXUnits);
        mnScale.add(scaleYUnits);
        mnProperties.add(mnScale);

        mnTags.setMnemonic(KeyEvent.VK_T);
        mnProperties.add(mnTags);

        mnTags.add(mnAddTag);
        mnTags.add(mnTagsSeparator);
        mnRemoveTag.setEnabled(false);
        mnTags.add(mnRemoveTag);

        mnTagsSeparator.setVisible(false);
        mnRemoveTag.setVisible(false);

        mnProperties.add(mnSetTitle);

        mnVariables.setMnemonic(KeyEvent.VK_V);
        mnProperties.add(mnVariables);
        mnVariables.add(new Action("Add", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().addVariable();
                }
            });
        mnVariables.add(mnVariablesSeparator);
        mnRemoveVariable.setEnabled(false);
        mnVariables.add(mnRemoveVariable);

        mnVariablesSeparator.setVisible(false);
        mnRemoveVariable.setVisible(false);
        mnProperties.add(editingEnabled);
        menuBar.add(mnProperties);

        // "Digitize" top-level menu
        JMenu mnDigit = new JMenu("Digitize");
        mnDigit.setMnemonic(KeyEvent.VK_I);
        menuBar.add(mnDigit);

        mnDigit.add(new Action("Copy all text to clipboard",
                              KeyEvent.VK_T) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().copyAllTextToClipboard();
                }
            });

        mnDigit.add(new Action("Copy label or curve coordinates to clipboard",
                              KeyEvent.VK_P,
                              KeyStroke.getKeyStroke("control C")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().copyCoordinatesToClipboard();
                }
            });

        mnDigit.add(mnCopyCoordinatesFromClipboard);

        mnDigit.add(new Action
                       ("Copy status bar to clipboard",
                        KeyEvent.VK_S,
                        KeyStroke.getKeyStroke("control shift S")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().copyPositionToClipboard();
                }
            });

        // "Chemistry" top-level menu
        JMenu mnChem = new JMenu("Chemistry");
        mnChem.setMnemonic(KeyEvent.VK_M);
        menuBar.add(mnChem);

        mnComponents.setMnemonic(KeyEvent.VK_C);
        setTopComponent.setEnabled(false);
        mnComponents.add(setLeftComponent);
        mnComponents.add(setRightComponent);
        mnComponents.add(setTopComponent);
        mnChem.add(mnComponents);

        {
            JMenu mnProp = new JMenu("Proportions");
            mnProp.setMnemonic(KeyEvent.VK_P);
            mnProp.add(convertToMole);
            mnProp.add(convertToWeight);
            mnProp.add(usingWeightFraction);
            mnChem.add(mnProp);
        }

        mnChem.add(new Action
                   ("Copy all formulas to clipboard", KeyEvent.VK_O) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().copyAllFormulasToClipboard();
                }
            });

        mnChem.add(new Action
                   ("Formula to mole/weight fraction", KeyEvent.VK_M,
                    KeyStroke.getKeyStroke('%')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().computeFraction();
                }
            });

        // "View" top-level menu
        JMenu mnView = new JMenu("View");
        mnView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(mnView);

        mnBackgroundImage.setMnemonic(KeyEvent.VK_B);
        mnBackgroundImage.setEnabled(false);
        lightGrayBackgroundImage = new BackgroundImageMenuItem
            ("Light", BackgroundImageType.LIGHT_GRAY, KeyEvent.VK_L);
        mnBackgroundImage.add(lightGrayBackgroundImage);
        darkGrayBackgroundImage = new BackgroundImageMenuItem
            ("Medium", BackgroundImageType.DARK_GRAY, KeyEvent.VK_M);
        mnBackgroundImage.add(darkGrayBackgroundImage);
        blackBackgroundImage = new BackgroundImageMenuItem
            ("Dark", BackgroundImageType.BLACK, KeyEvent.VK_D);
        mnBackgroundImage.add(blackBackgroundImage);
        blinkBackgroundImage = new BackgroundImageMenuItem
            ("Blink", BackgroundImageType.BLINK, KeyEvent.VK_B);
        mnBackgroundImage.add(blinkBackgroundImage);
        noBackgroundImage = new BackgroundImageMenuItem
            ("Hide", BackgroundImageType.NONE, KeyEvent.VK_N);
        noBackgroundImage.getAction().putValue
            (AbstractAction.ACCELERATOR_KEY,
             KeyStroke.getKeyStroke("control H"));
        mnBackgroundImage.add(noBackgroundImage);
        mnBackgroundImage.add
            (new Action("Detach", KeyEvent.VK_E) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getParentEditor().detachOriginalImage();
                    }
                });
        lightGrayBackgroundImage.setSelected(true);
        mnView.add(mnBackgroundImage);

        mnView.add(new Action("Best fit", KeyEvent.VK_B,
                              KeyStroke.getKeyStroke("control B")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().bestFit();
                }
            });
        mnView.add(new Action("Center mouse", KeyEvent.VK_C,
                              KeyStroke.getKeyStroke("control L")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().centerMouse();
                }
            });

        mnView.add(new Action("Zoom in", KeyEvent.VK_I,
                              "typed +") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().zoomBy(1.5);
                }
            });
        mnView.add(new Action("Zoom out", KeyEvent.VK_O,
                              "typed -") {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().zoomBy(1 / 1.5);
                }
            });

        mnView.add(new Action("Zoom to selection", KeyEvent.VK_S,
                              KeyStroke.getKeyStroke("control Z")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getParentEditor().zoomToSelection();
                }
            });

        JMenu mnHelp = new JMenu("Help");
        mnHelp.setMnemonic(KeyEvent.VK_H);
        menuBar.add(mnHelp);
        mnHelp.add(new Action("Help", KeyEvent.VK_H, "F1") {
                @Override public void actionPerformed(ActionEvent e) {
                    help();
                }
            });

        mnHelp.add(new Action("About", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    about();
                }
            });

        setEditable(true);
    }

    void setEditable(boolean b) {
        mnSave.setVisible(b);
        mnSaveAsPED.setVisible(b);
        mnSelection.setVisible(b);
        mnUnstickMouse.setVisible(b);
        mnCurve.setVisible(b);
        mnDecorations.setVisible(b);
        mnFont.setVisible(b);
        mnAddKey.setVisible(b);
        mnMargins.setVisible(b);
        mnScale.setVisible(b);
        mnAddTag.setVisible(b);
        // TODO Make the tag menu invisible if not editable and no
        // tags; also make it so the deletion part doesn't delete. Yuck!
        mnSetTitle.setVisible(b);
        mnVariables.setVisible(b);

        mnProperties.setVisible(getVisibleItemCount(mnProperties) > 0);
        mnCopyCoordinatesFromClipboard.setVisible(b);
        mnComponents.setVisible(b);

        // TODO Verify that usingWeightFraction works right.
        usingWeightFraction.setVisible(b);

        // TODO "Enable editing features" item under Properties
        mnBackgroundImage.setVisible(b);

        if (editingEnabled.isSelected() != b) {
            editingEnabled.setSelected(b);
        }
    }

    /** Return the number of visible items in the menu. */
    public static int getVisibleItemCount(JMenu m) {
        int cnt = 0;
        int ic = m.getItemCount();
        for (int i = 0; i < ic; ++i) {
            JMenuItem it = m.getItem(i);
            if (it.isVisible()) {
                cnt++;
            }
        }
        return cnt;
    }

    @Override public void update(Observable o, Object arg) {
        setTitle(getParentEditor().getProvisionalTitle());
        repaint();
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
        ShowHTML.show("edithelp.html", this);
    }

    protected void about() {
        ShowHTML.show("about.html", this);
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

        mnTagsSeparator.setVisible(true);
        mnRemoveTag.setVisible(true);
        for (int i = firstTagIndex(); i <= itemCount; ++i) {
            if (i == itemCount
                || mnTags.getItem(i).getText().compareToIgnoreCase(tag) > 0) {
                mnTags.insert(new RemoveTagAction(tag), i);
                break;
            }
        }
    }

    /** Return the index into mnTags of the first tag. If there are no
        tags, then the returned value will equal getItemCount(). */
    int firstTagIndex() {
        int itemCount = mnTags.getItemCount();
        for (int i = 0; i < itemCount; ++i) {
            if (mnTags.getItem(i) == mnRemoveTag) {
                return i + 1;
            }
        }
        return -1;
    }

    void removeTag(int i) {
        mnTags.remove(i);

        if (firstTagIndex() == mnTags.getItemCount()) {
            // Hide the separator and the list of tags.
            mnTagsSeparator.setVisible(false);
            mnRemoveTag.setVisible(false);
        }
    }

    /** This is not a public interface. It is an interface that Editor
        uses to perform UI operations in support of an addTag()
        request. */
    void removeTag(String tag) {
        int itemCount = mnTags.getItemCount();

        for (int i = firstTagIndex(); i <= itemCount; ++i) {
            if (mnTags.getItem(i).getText().equals(tag)) {
                removeTag(i);
                return;
            }
        }
    }

    void removeAllTags() {
        int index = firstTagIndex();
        while (mnTags.getItemCount() > index) {
            removeTag(index);
        }
    }

    /** This is not a public interface. It is an interface that Editor
        uses to perform UI operations in support of an addVariable()
        request. */
    void addVariable(String variable) {
        mnVariablesSeparator.setVisible(true);
        int itemCount = mnVariables.getItemCount();

        mnVariablesSeparator.setVisible(true);
        mnRemoveVariable.setVisible(true);
        for (int i = firstVariableIndex(); i <= itemCount; ++i) {
            if (i == itemCount
                || mnVariables.getItem(i).getText().compareTo(variable) > 0) {
                mnVariables.insert(new RemoveVariableAction(variable), i);
                break;
            }
        }
    }

    /** Return the index into mnVariables of the first variable. If there are no
        variables, then the returned value will equal getItemCount(). */
    int firstVariableIndex() {
        int itemCount = mnVariables.getItemCount();
        for (int i = 0; i < itemCount; ++i) {
            if (mnVariables.getItem(i) == mnRemoveVariable) {
                return i + 1;
            }
        }
        return -1;
    }

    void removeVariable(int i) {
        mnVariables.remove(i);

        if (firstVariableIndex() == mnVariables.getItemCount()) {
            // Hide the separator and the list of variables.
            mnVariablesSeparator.setVisible(false);
            mnRemoveVariable.setVisible(false);
        }
    }

    /** This is not a public interface. It is an interface that Editor
        uses to perform UI operations in support of an addVariable()
        request. */
    void removeVariable(String variable) {
        int itemCount = mnVariables.getItemCount();

        for (int i = firstVariableIndex(); i <= itemCount; ++i) {
            if (mnVariables.getItem(i).getText().equals(variable)) {
                removeVariable(i);
                return;
            }
        }
    }

    void removeAllVariables() {
        int index = firstVariableIndex();
        while (mnVariables.getItemCount() > index) {
            removeVariable(index);
        }
    }

    static class FillCategory {
        StandardFill example;
        EnumSet<StandardFill> choices;

        FillCategory(StandardFill example, EnumSet<StandardFill> choices) {
            this.example = example;
            this.choices = choices;
        }
    }

    JMenu createFillMenu() {
        JMenu mnFillStyle = new JMenu("Fill style");
        mnFillStyle.setMnemonic(KeyEvent.VK_F);

        for (FillCategory cat: new FillCategory[]
            { new FillCategory
              (StandardFill.ALPHA50,
               EnumSet.range(StandardFill.SOLID,
                             StandardFill.ALPHA10)),
              new FillCategory
              (StandardFill.V2_25,
               EnumSet.range(StandardFill.V1_25,
                             StandardFill.V4_25)),
              new FillCategory
              (StandardFill.H2_25,
               EnumSet.range(StandardFill.H1_25,
                             StandardFill.H4_25)),
              new FillCategory
              (StandardFill.DU2_25,
               EnumSet.range(StandardFill.DU1_25,
                             StandardFill.DU4_25)),
              new FillCategory
              (StandardFill.DD2_25,
               EnumSet.range(StandardFill.DD1_25,
                             StandardFill.DD4_25)),
              new FillCategory
              (StandardFill.X1_10,
               EnumSet.range(StandardFill.X1_10,
                             StandardFill.X4_10)),
              new FillCategory
              (StandardFill.PD2_25,
               EnumSet.range(StandardFill.PD2_25,
                             StandardFill.PD8_25))
            }) {
            JMenu mnCat = new JMenu();
            mnCat.setIcon(icon(cat.example));
            for (StandardFill fill: cat.choices) {
                mnCat.add(new FillStyleMenuItem(fill));
            }
            mnFillStyle.add(mnCat);
        }

        return mnFillStyle;
    }

    ImageIcon icon(StandardFill fill) {
        int width = 70;
        int height = 40;

        BufferedImage im = new BufferedImage
            (width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setBackground(new Color(255, 255, 255, 0));
        g.clearRect(0, 0, im.getWidth(), im.getHeight());
        g.setColor(Color.BLACK);
        g.setFont(g.getFont().deriveFont(9.0f));
        LabelDialog.drawString(g, "Underlayer", width/2, height/2, 0.5, 0.5);
        g.drawRect(0, 0, im.getWidth() - 1, im.getHeight() - 1);
        g.setPaint(fill.getPaint(new Color(100, 100, 100), 1));
        g.fill(new Rectangle(0, 0, im.getWidth(), im.getHeight()));
        return new ImageIcon(im);
    }

    static ImageIcon icon(StandardStroke stroke, int width, int height, double lineWidth) {
        float middle = height/2.0f;
        Shape line = new Line2D.Float(0f, middle, (float) width, middle);
        BufferedImage im = new BufferedImage
            (width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setBackground(new Color(255, 255, 255, 0));
        g.clearRect(0, 0, im.getWidth(), im.getHeight());
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        stroke.getStroke().draw(g, line, lineWidth);
        return new ImageIcon(im);
    }
}
