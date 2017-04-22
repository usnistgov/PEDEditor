/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

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
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.ServiceConfigurationError;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
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
import javax.swing.ScrollPaneConstants;

@SuppressWarnings("serial")
public class EditFrame extends JFrame
    implements Observer {

    protected JScrollPane scrollPane;
    protected EditPane imagePane;
    protected int preferredWidth = 800;
    protected int preferredHeight = 600;

    protected JPanel statusBar;
    protected JLabel colorLabel;
    protected JLabel statusLabel;
    protected BasicEditor parentEditor;
    protected ButtonGroup fillStyleGroup = new ButtonGroup();
    protected ButtonGroup lineStyleGroup = new ButtonGroup();
    protected ButtonGroup lineWidthGroup = new ButtonGroup();
    protected ButtonGroup backgroundImageGroup = new ButtonGroup();
    protected ButtonGroup fontGroup = new ButtonGroup();
    protected JMenu mnBackgroundImage = new JMenu("Background image");
    protected JRadioButtonMenuItem lightGrayBackgroundImage;
    protected JRadioButtonMenuItem darkGrayBackgroundImage;
    protected JRadioButtonMenuItem blackBackgroundImage;
    protected JRadioButtonMenuItem noBackgroundImage;
    protected JRadioButtonMenuItem customLineWidth =
        new JRadioButtonMenuItem(new AbstractAction("Custom...") {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().customLineWidth();
                    finishEvent();
                }
            });
    protected Action setAspectRatio;
    protected String longHelpFile = "edithelp.html";
    protected String shortHelpFile = "viewhelp.html";
    protected String helpAboutFile = "about.html";

    JMenu mnFile = new JMenu("File");
    JMenu mnCurve = new JMenu("Curve");
    JMenu mnProperties = new JMenu("Properties");
    JMenu mnSwap = new JMenu("Swap Components");
    {
        mnSwap.setMnemonic(KeyEvent.VK_W);
    }
    JMenu mnFont = new JMenu("Font");
    JMenu mnMargins = new JMenu("Margins");
    JMenu mnView = new JMenu("View");

    protected JMenuItem mnNewDiagram = new JMenuItem
        (new Action("New diagram", KeyEvent.VK_N) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().newDiagram(false);
                }
            });
    protected JMenuItem mnOpen = new JMenuItem
        (new Action("Open", KeyEvent.VK_O) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().open();
                }
            });

    protected Action actMonitor = new Action("Monitor Directory",
            KeyEvent.VK_M, "control shift D") {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().monitor();
            }
        };

    protected JMenuItem mnMonitor = new JMenuItem(actMonitor);
    {
        mnMonitor.setVisible(false);
    }

    protected Action actSave = new Action("Save", KeyEvent.VK_S,
                                          KeyStroke.getKeyStroke("control S")) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().save();
            }
        };
    protected Action actSubmit = new Action("Submit", KeyEvent.VK_S,
                                          KeyStroke.getKeyStroke("control S")) {
            { setEnabled(false); }
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().submit();
            }
        };
    protected JMenu mnSaveAs = new JMenu("Save as");
    protected JMenuItem mnSave = toMenuItem(actSave);
    protected JMenuItem mnSubmit = toMenuItem(actSubmit);
    {
        mnSubmit.setVisible(false);
    }
    protected Action actSaveAsPED = new Action("PED", KeyEvent.VK_P) {
            {
                putValue(SHORT_DESCRIPTION,
                         "Save diagram in PED Editor's native format");
            }
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().saveAsPED();
            }
        };
    protected JMenuItem mnSaveAsPED = toMenuItem(actSaveAsPED);
    protected JMenuItem mnReload = toMenuItem
        (new Action("Refresh", KeyEvent.VK_R) {
                {
                    putValue(SHORT_DESCRIPTION,
                             "Reload the current diagram");
                }
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().reloadDiagram();
                }
            });
    JMenuItem mnNextFile = toMenuItem
        (new Action("Next", KeyEvent.VK_R,
                    KeyStroke.getKeyStroke("control RIGHT")) {
                {
                    putValue(SHORT_DESCRIPTION,
                             "Show next diagram");
                }
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().nextFile();
                }
            });
    {
        mnNextFile.setVisible(false); // Hidden by default
    }

    JMenuItem mnHints = new JMenuItem
        (new Action("Hints..") {
                @Override public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog
                    (EditFrame.this,
                     Stuff.htmlify
                     ("<ol>"
                      + "<li>You can see more functions and their short-cut keys "
                      + "by right-clicking (pressing the right mouse button) "
                      + " while the mouse is inside the diagram."
                      + "<li>To zoom in, drag the mouse (move the mouse while holding down the left mouse button)."
                      + "<li>Hold down the <code>Shift</code> key while moving the "
                      + "mouse to find special points and curves, which will be "
                      + "marked with a second pair of crosshairs."
                      + "</ol>"));
                }
            });

    Action actShiftPressed = new Action("Shift Pressed", 0, "pressed SHIFT") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().setShiftDown(true);
            finishEvent();
        }
    };

    Action actShiftReleased = new Action("Shift Released", 0, "released SHIFT") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().setShiftDown(false);
            finishEvent();
        }
    };

    Action actCopy = new Action("Copy", KeyEvent.VK_C, KeyStroke.getKeyStroke("control C")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().copySelection();
            finishEvent();
        }
    };

    Action actCutRegion = new Action("Cut everything in selected region", KeyEvent.VK_E,
            KeyStroke.getKeyStroke("control shift X")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().cutRegion();
            finishEvent();
        }
    };

    Action actColor = new Action("Color...", KeyEvent.VK_R, KeyStroke.getKeyStroke('r')) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().colorSelection();
            finishEvent();
        }
    };

    Action actPaste = new Action("Paste", KeyEvent.VK_P, KeyStroke.getKeyStroke("control V")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().paste();
            finishEvent();
        }
    };

    Action actCutAll = new Action("Cut all", KeyEvent.VK_A,
            KeyStroke.getKeyStroke("control A")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().cutAll();
            finishEvent();
        }
    };

    Action actRemoveSelection = new Action("Delete", KeyEvent.VK_D, Stuff.isOSX() ? "BACK_SPACE" : "DELETE") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().removeSelection();
            finishEvent();
        }
    };

    Action actCut = new Action("Cut", KeyEvent.VK_U, KeyStroke.getKeyStroke("control X")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().cutSelection();
            finishEvent();
        }
    };

    Action actCopyAndPaste = new Action("Copy and paste", KeyEvent.VK_C, KeyStroke.getKeyStroke("typed c")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().copyAndPaste();
            finishEvent();
        }
    };

    Action actRedo = new Action("Redo", KeyEvent.VK_R, KeyStroke.getKeyStroke("control Y")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().redo();
            finishEvent();
        }
    };

    Action actUndo = new Action("Undo", KeyEvent.VK_U, KeyStroke.getKeyStroke("control Z")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().undo();
            finishEvent();
        }
    };

    Action actDeselect = new Action("Deselect", KeyEvent.VK_S, "pressed ESCAPE") {
        {
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().clearSelection();
            finishEvent();
        }
    };

    Action actEditSelection = new Action("Properties...", KeyEvent.VK_P, KeyStroke.getKeyStroke('e')) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().editSelection();
            finishEvent();
        }
    };

    Action actResetToDefault = new Action("Revert properties to default", KeyEvent.VK_T, KeyStroke.getKeyStroke('d')) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().resetSelectionToDefaultSettings();
            finishEvent();
        }
    };

    Action actMakeDefault = new Action("Make selection's properties the new default", KeyEvent.VK_F,
            KeyStroke.getKeyStroke("typed D")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().setDefaultSettingsFromSelection();
            finishEvent();
        }
    };

    Action actMoveSelection = new Action("Move selection only", KeyEvent.VK_V, KeyStroke.getKeyStroke("typed V")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().moveSelection(false);
            finishEvent();
        }
    };

    Action actMovePoint = new Action("Move everything at selected point", KeyEvent.VK_M, KeyStroke.getKeyStroke('v')) {
        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().moveSelection(true);
            finishEvent();
        }
    };

    Action actAutoPosition = new Action("Auto-position", KeyEvent.VK_A, KeyStroke.getKeyStroke("shift A")) {
        {
            putValue(SHORT_DESCRIPTION, "Move the mouse to the closest key point or curve");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getEditor().autoPosition();
            finishEvent();
        }
    };

    JMenuItem mnEnterCoordinates = toMenuItem
        (new Action("Enter coordinates",
                    KeyEvent.VK_E,
                    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().enterPosition();
                    finishEvent();
                }
            });

    JMenuItem mnJumpToSelection = toMenuItem
        (new Action("Jump to selection",
                    KeyEvent.VK_J,
                    KeyStroke.getKeyStroke('j')) {
                {
                    putValue(SHORT_DESCRIPTION,
                             "Move the mouse to the selected item");
                }
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().centerSelection();
                    finishEvent();
                }
            });

    Action actNearestGridPoint = new Action
        ("Nearest grid point",
         KeyEvent.VK_G, KeyStroke.getKeyStroke('g')) {
            {
                putValue(SHORT_DESCRIPTION,
                         "Move the mouse to the closest integer coordinates");
            }
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().seekNearestGridPoint("g");
                finishEvent();
                }
            };

    Action actNearestPoint = new Action
        ("Nearest key point",
         KeyEvent.VK_N, KeyStroke.getKeyStroke('q')) {
            {
                putValue(SHORT_DESCRIPTION,
                         "Move the mouse to the nearest data point, label, "
                         + "arrow, curve endpoint, curve intersection, etc.");
            }
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().seekNearestPoint(false, "q");
                finishEvent();
            }
        };
    Action actNearestCurve = new Action
        ("Nearest line/curve",
         KeyEvent.VK_L, KeyStroke.getKeyStroke('w')) {
            {
                putValue(SHORT_DESCRIPTION,
                         "Move the mouse to the nearest point on any curve "
                         + "or ruler.");
            }
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().seekNearestCurve(false, "w");
                finishEvent();
            }
        };
    Action actSelectNearestPoint = new Action
        ("Select nearest key point",
         KeyEvent.VK_S, KeyStroke.getKeyStroke("typed Q")) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().seekNearestPoint(true, "Shift+Q");
                finishEvent();
            }
        };
    Action actSelectNearestCurve = new Action
        ("Select nearest line/curve",
         KeyEvent.VK_I, KeyStroke.getKeyStroke("typed W")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                getEditor().seekNearestCurve(true, "Shift+W");
                finishEvent();
            }
        };
    protected JMenuItem mnUnstickMouse = toMenuItem
        (new Action("Unstick mouse",
                    KeyEvent.VK_U,
                    KeyStroke.getKeyStroke('u')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setMouseStuck(false);
                    repaint();
                    finishEvent();
                }
            });

    JMenu mnPosition = new JMenu("Position");
    { mnPosition.setMnemonic(KeyEvent.VK_P); }

    JMenu mnStep = new JMenu("Step");
    { mnStep.setMnemonic(KeyEvent.VK_S); }
    JMenu mnJump = new JMenu("Jump");
    { mnJump.setMnemonic(KeyEvent.VK_J); }

    Action actAddVertex = new Action
        ("Add vertex", KeyEvent.VK_X,
         KeyStroke.getKeyStroke('x')) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addVertex();
                finishEvent();
            }
        };
    Action actAddAutoPositionedVertex = new Action
        ("Add auto-positioned vertex", KeyEvent.VK_A,
         KeyStroke.getKeyStroke("typed X")) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().autoPosition();
                getEditor().addVertex();
                finishEvent();
            }
        };

    Action actText = new Action
        ("Label...",
         KeyEvent.VK_L, KeyStroke.getKeyStroke('t')) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addLabel();
                finishEvent();
            }
        };
    Action actIsotherm = new Action
        ("Isotherm/Line label...",
         KeyEvent.VK_I, KeyStroke.getKeyStroke('i')) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addIsotherm();
                finishEvent();
            }
        };
    Action actLeftArrow = new Action
        ("Left arrowhead",
         KeyEvent.VK_L, KeyStroke.getKeyStroke('<')) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addArrow(false);
                finishEvent();
            }
        };
    Action actRightArrow = new Action
        ("Right arrowhead",
         KeyEvent.VK_R, KeyStroke.getKeyStroke('>')) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addArrow(true);
                finishEvent();
            }
        };
    Action actRuler = new Action
        ("Ruler", KeyEvent.VK_U) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addRuler();
                finishEvent();
            }
        };
    Action actTieLine = new Action
        ("Tie lines", KeyEvent.VK_I, KeyStroke.getKeyStroke("typed T")) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addTieLine();
                finishEvent();
            }
        };
    Action actCircle = new Action
        ("Circle/Arc/Ellipse", KeyEvent.VK_C) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addCircle();
                finishEvent();
            }
        };

    JMenu mnKeys = new JMenu("Key/value pairs");
    Action actAddKey = new Action("Add", KeyEvent.VK_A) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().put();
                finishEvent();
            }
        };
    protected JMenuItem mnAddKey = toMenuItem(actAddKey);
    protected JSeparator mnTagsSeparator = new JSeparator();
    protected JMenuItem mnRemoveTag = new JMenuItem("Delete");
    JMenu mnTags = new JMenu("Tags");
    protected JSeparator mnVariablesSeparator = new JSeparator();
    protected JMenuItem mnEditVariable = new JMenuItem("Edit");
    protected JMenu mnVariables = new JMenu("Variables");

    protected Action actSwapXY = new Action("Swap X and Y axes",
            KeyEvent.VK_S) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().swapXY();
                finishEvent();
            }
        };
    protected JMenu mnScale = new JMenu("Scale");
    Action actAddTag = new Action("Add", KeyEvent.VK_A) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().addTag();
                finishEvent();
            }
        };
    protected JMenuItem mnAddTag = new JMenuItem(actAddTag);
    Action actSetTitle = new Action("Title", KeyEvent.VK_T) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().setTitle();
                finishEvent();
            }
        };
    protected JMenuItem mnSetTitle = new JMenuItem(actSetTitle);
    protected JMenuItem mnExportText = new JMenuItem
        (new Action("Export text to clipboard", KeyEvent.VK_T) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().copyTextToClipboard();
                    finishEvent();
                }
            });
    protected JMenuItem mnImportCoordinates = new JMenuItem
        (new Action("Import curve or label coordinates",
                    KeyEvent.VK_I) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().importCoordinates();
                    finishEvent();
                }
            });
    protected JMenu mnSetComponents = new JMenu("Set components");

    JMenuItem mnCopyFormulas = new JMenuItem
        (new Action
         ("Copy all formulas to clipboard", KeyEvent.VK_O) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().copyAllFormulasToClipboard();
                }
            });

    Action actCopyStatusBar = new Action
        ("Copy coordinates to clipboard", KeyEvent.VK_C) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().copyPositionToClipboard();
                finishEvent();
            }
        };

    Action actZoomIn = new Action
        ("Zoom in", KeyEvent.VK_I, "control typed +") {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().zoomBy(1.5);
                    finishEvent();
                }
            };

    Action actZoomOut = new Action
        ("Zoom out", KeyEvent.VK_O, "control typed -") {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().zoomBy(1/1.5);
                    finishEvent();
                }
            };

    Action actCenterMouse = new Action
        ("Center mouse",
         KeyEvent.VK_C, KeyStroke.getKeyStroke("control L")) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().centerMouse();
                finishEvent();
            }
        };

    LayerAction[] layers =
    { new LayerAction("Lower", KeyEvent.VK_L, null, -1),
      new LayerAction("Raise", KeyEvent.VK_R, null, +1),
      new LayerAction("To bottom", KeyEvent.VK_B, null, -1000000),
      new LayerAction("To top", KeyEvent.VK_T, null, +1000000) };

    public void setBackgroundType(StandardAlpha value) {
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
        }
    }

    public void setAlpha(double value) {
        for (StandardAlpha alpha: StandardAlpha.values()) {
            if (alpha.getAlpha() == value) {
                setBackgroundType(alpha);
                return;
            }
        }
        System.err.println("Nonstandard alpha value " + value);
    }

    /** setBackgroundTypeEnabled(false) disables all of the background
        type menu items; (true) re-enables them. */
    public void setBackgroundTypeEnabled(boolean enabled) {
        noBackgroundImage.getAction().setEnabled(enabled);
        lightGrayBackgroundImage.getAction().setEnabled(enabled);
        darkGrayBackgroundImage.getAction().setEnabled(enabled);
        blackBackgroundImage.getAction().setEnabled(enabled);
    }

    protected Action setLeftComponent = new Action
        ("Left", KeyEvent.VK_L) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().setDiagramComponent(Side.LEFT);
                finishEvent();
            }
        };

    protected Action setRightComponent = new Action
        ("Right", KeyEvent.VK_R) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().setDiagramComponent(Side.RIGHT);
                finishEvent();
            }
        };

    protected Action setTopComponent = new Action
        ("Top", KeyEvent.VK_T) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().setDiagramComponent(Side.TOP);
                finishEvent();
            }
        };

    protected Action swapBinary = new Action
        ("Swap components", KeyEvent.VK_S) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().swapDiagramComponents(Side.LEFT, Side.RIGHT);
                finishEvent();
            }
        };

    protected Action guessComponents = new Action
        ("Guess components", KeyEvent.VK_G) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().guessComponents(false);
                finishEvent();
            }
        };

    protected Action scaleXUnits = new Action
        ("X axis/right component", KeyEvent.VK_X) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().scaleXUnits();
                finishEvent();
            }
        };

    protected Action scaleYUnits = new Action
        ("Y axis/top component", KeyEvent.VK_Y) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().scaleYUnits();
                finishEvent();
            }
        };

    protected Action scaleBoth = new Action
        ("Both axes uniformly", KeyEvent.VK_B) {
            @Override public void actionPerformed(ActionEvent e) {
                getEditor().scaleBoth();
                finishEvent();
            }
        };

    /** Internal use; called from BasicEditor.java. Make the GUI changes
        necessary to reflect whether we are or aren't currently using weight
        fraction values. */
    void setUsingWeightFraction(boolean b) {
        convertToMole.setEnabled(b);
        convertToWeight.setEnabled(!b);
        usingWeightFraction.setSelected(b);
    }

    /** Internal use; called from BasicEditor.java. */
    void setSmoothed(boolean b) {
        smoothed.setSelected(b);
    }

    /** Internal use; called from BasicEditor.java. */
    void setHideImages(boolean b) {
        hideImages.setSelected(b);
    }

    public boolean isHideImages() {
        return hideImages.isSelected();
    }

    public void setHideImagesVisible(boolean b) {
        hideImages.setVisible(b);
    }

    /** Internal use; called from BasicEditor.java. */
    void setShowGrid(boolean b) {
        showGrid.setSelected(b);
    }

    /** Internal use; called from BasicEditor.java. */
    void setPixelMode(boolean b) {
        pixelMode.setSelected(b);
    }

    /** Internal use; called from BasicEditor.java. */
    void setPixelModeVisible(boolean b) {
        pixelMode.setVisible(b);
        mnProperties.setVisible(getVisibleItemCount(mnProperties) > 0);
    }

    void conversionError() {
        String msg = getEditor().isEditable()
            ? ("<p>The conversion was canceled or could not be performed. "
               + "<p>Conversions can only be performed on "
               + "diagrams for which the left, right, and (for ternary diagrams) "
               + "top components are defined (using the "
               + "<code>Chemistry/Components</code> "
               + "menu) in elemental formulas such as \"Ca + Mg\" or "
               + "\"Pb3(PO4)2\".")
            : "<p>This diagram does not support mole \u2194 weight conversions.";
        getEditor().showError(msg);
    }

    protected Action convertToMole = new Action
        ("Convert to mole fraction", KeyEvent.VK_C) {
            {
                setEnabled(false);
            }
            @Override public void actionPerformed(ActionEvent e) {
                if (!getEditor().weightToMoleFraction()) {
                    conversionError();
                }
                finishEvent();
            }
        };

    protected Action convertToWeight = new Action
        ("Convert to weight fraction", KeyEvent.VK_C) {
            @Override public void actionPerformed(ActionEvent e) {
                if (!getEditor().moleToWeightFraction()) {
                    conversionError();
                }
                finishEvent();
            }
        };

    protected JCheckBoxMenuItem usingWeightFraction
        = new JCheckBoxMenuItem
        (new Action
         ("Already displaying weight fraction", KeyEvent.VK_W) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setUsingWeightFraction
                        (usingWeightFraction.isSelected());
                    finishEvent();
                }
            });

    protected JCheckBoxMenuItem editingEnabled
        = new JCheckBoxMenuItem
        (new Action("Show editing options", KeyEvent.VK_W) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setEditable(editingEnabled.isSelected());
                    finishEvent();
                }
            });

    protected JCheckBoxMenuItem hideImages
        = new JCheckBoxMenuItem
        (new Action("Hide original image when saving or printing", KeyEvent.VK_W) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setPrintImages(!hideImages.isSelected());
                    finishEvent();
                }
            });

    protected JCheckBoxMenuItem smoothed
        = new JCheckBoxMenuItem
        (new Action
         ("Smooth through new points", KeyEvent.VK_S,
          KeyStroke.getKeyStroke('s')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setSmoothed(smoothed.isSelected());
                    finishEvent();
                }
            });

    protected JCheckBoxMenuItem pixelMode
        = new JCheckBoxMenuItem
        (new Action("Pixel mode", KeyEvent.VK_P) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setPixelModeComplex(pixelMode.isSelected());
                    finishEvent();
                }
            });

    protected JCheckBoxMenuItem showGrid
        = new JCheckBoxMenuItem
        (new Action("Show grid", KeyEvent.VK_G,
                    KeyStroke.getKeyStroke("control G")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setShowGrid(showGrid.isSelected());
                    finishEvent();
                }
            });

    protected JCheckBoxMenuItem showMathWindow
        = new JCheckBoxMenuItem
        (new Action("Show math window", KeyEvent.VK_S) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().mathWindow.setVisible
                        (showMathWindow.isSelected());
                    finishEvent();
                }
            });

    public BasicEditor getEditor() { return parentEditor; }

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

    class SaveImageAction extends Action {
        String ext;
        SaveImageAction(String ext, int mnemonic) {
            super(ext);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
            this.ext = ext;

            putValue(SHORT_DESCRIPTION,
                     "Save diagram in the " + ext + " image format");
        }

        @Override public void actionPerformed(ActionEvent e) {
            getEditor().saveAsImage(ext);
            finishEvent();
        }
    }

    class FontAction extends Action {
        String fontName;

        FontAction(String label, String fontName) {
            super(label);
            this.fontName = fontName;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getEditor().setFontName(fontName);
            repaint();
            finishEvent();
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
            getEditor().changeLayer(layerDelta);
            repaint();
            finishEvent();
        }
    }

    class FontMenuItem extends JRadioButtonMenuItem {
        FontMenuItem(String label, String fontName) {
            super(new FontAction(label, fontName));
            fontGroup.add(this);
        }
    }

    static protected BufferedImage pedIcon = null;

    public static BufferedImage getIcon() {
        try {
            if (pedIcon == null) {
                URL url =
                    BasicEditor.class.getResource("images/PEDicon.png");
                try {
                    pedIcon = ImageIO.read(url);
                } catch (IOException e) {
                    return null;
                }
            }
            return pedIcon;
        } catch (ServiceConfigurationError x) {
            Stuff.showError(null,
                            "When the pop-up window asks you whether to enable or block mixed code, you should "
                            + "select the 'block' option. Alternatively, you can make that the default behavior "
                            + "from the Java Control Panel. Select the 'Advanced' tab, then in the 'Mixed code' "
                            + "section, select \"Enable - hide warning and don't run untrusted code\". "
                            + "If you need additional guidance, contact phase3@ceramics.org.",
                            "Please block execution of mixed signed/unsigned code");
            System.exit(3);
            return null;
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
            getEditor().setLineWidth(lineWidth);
            finishEvent();
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
            getEditor().setSelectedLineStyle(lineStyle);
            finishEvent();
        }
    }

    class FillStyleAction extends AbstractAction {
        StandardFill fill;

        FillStyleAction(StandardFill fill) {
            super(null, icon(fill));
            this.fill = fill;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getEditor().setSelectedFill(fill);
            finishEvent();
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
        StandardAlpha value;

        BackgroundImageAction(String name, StandardAlpha value,
                              int mnemonic) {
            super(name);
            putValue(MNEMONIC_KEY, new Integer(mnemonic));
            this.value = value;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getEditor().toggleImageAlpha(value.getAlpha());
            finishEvent();
        }
    }



    class BackgroundImageMenuItem extends JRadioButtonMenuItem {
        BackgroundImageMenuItem(String name, StandardAlpha back,
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
            getEditor().move(dx, dy);
            finishEvent();
        }
    }

    /** Class for fine-grain adjustments of the last vertex added. */
    class JumpAction extends Action {
        int dx;
        int dy;

        JumpAction(String name, int mnemonic, String accelerator,
                     int dx, int dy) {
            super(name, mnemonic, accelerator);
            this.dx = dx;
            this.dy = dy;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getEditor().jump(dx, dy);
            finishEvent();
        }
    }

    class RemoveTagAction extends Action {
        RemoveTagAction(String tag) {
            super(tag);
        }

        @Override public void actionPerformed(ActionEvent e) {
            getEditor().removeTag(e.getActionCommand());
            finishEvent();
        }
    }

    class RemoveVariableAction extends Action {
        String variable;

        RemoveVariableAction(String variable) {
            super("Delete");
            this.variable = variable;
        }

        @Override public void actionPerformed(ActionEvent e) {
            try {
                getEditor().removeVariable(variable);
            } catch (CannotDeletePrincipalVariableException x) {
                getEditor().showError
                    ("Cannot delete principal coordinate variable "
                     + x.getVariable().name);
            } catch (NoSuchVariableException x) {
                throw new IllegalStateException(x);
            }
            finishEvent();
        }
    }

    class RenameVariableAction extends Action {
        String variable;

        RenameVariableAction(String variable) {
            super("Rename");
            this.variable = variable;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getEditor().renameVariable(variable);
            finishEvent();
        }
    }

    class TogglePercentageDisplayAction extends Action {
        String variable;

        TogglePercentageDisplayAction(String variable) {
            super("Toggle Percentage Display");
            this.variable = variable;
        }

        @Override public void actionPerformed(ActionEvent e) {
            getEditor().togglePercentageDisplay(variable);
            finishEvent();
        }
    }

    JMenu createEditVariableMenu(String variable) {
        JMenu menu = new JMenu(variable);
        menu.add(new RenameVariableAction(variable));
        menu.add(new RemoveVariableAction(variable));
        menu.add(new TogglePercentageDisplayAction(variable));
        return menu;
    }

    /**
     * Create the frame.
     */
    public EditFrame(BasicEditor parentEditor) {
        this.parentEditor = parentEditor;
        parentEditor.addObserver(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(0, 0);
        getContentPane().setLayout(new BorderLayout());

        imagePane = new EditPane(this);
        // This is a workaround for an apparent Swing bug. When there
        // isn't room for a horizontal scrollbar and a horizontal
        // expansion forces a horizontal scrollbar to be added, so a
        // vertical scrollbar becomes necessary too, it seems to screw
        // up the process so that setViewPosition() doesn't work right
        // the first time out.
        scrollPane = new JScrollPane
            (imagePane,
             ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize
            (new Dimension(preferredWidth, preferredHeight));
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        statusBar = new JPanel();
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        statusBar.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        colorLabel = new JLabel();
        statusBar.add(colorLabel);
        statusBar.add(Box.createRigidArea(new Dimension(5, 0)));

        statusLabel = new JLabel("<html><font size=\"-2\">"
                                 + "No diagram loaded</font></html>");
        statusBar.add(statusLabel);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // "File" top-level menu
        mnFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(mnFile);

        mnFile.add(mnNewDiagram);
        mnFile.add(mnOpen);
        mnFile.add(mnMonitor);
        mnFile.add(mnSave);
        mnFile.add(mnSubmit);

        // "Save As" submenu
        mnSaveAs.setMnemonic(KeyEvent.VK_A);
        mnFile.add(mnSaveAs);
        mnSaveAs.add(mnSaveAsPED);
        for (SaveImageAction act: new SaveImageAction[] {
                new SaveImageAction("PNG", KeyEvent.VK_N),
                new SaveImageAction("GIF", KeyEvent.VK_G),
                new SaveImageAction("JPEG", KeyEvent.VK_J)}) {
            mnSaveAs.add(toMenuItem(act));
        }

        mnFile.add(mnSaveAs);

        // "Print" menu item
        mnFile.add(new Action("Print", KeyEvent.VK_P) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().print();
                    finishEvent();
                }
            });

        mnFile.add(mnNextFile);
        mnFile.add(mnReload);

        mnFile.addSeparator();

        // Close one window
        mnFile.add(new Action("Close", KeyEvent.VK_C) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().verifyThenCloseOrClear();
                    finishEvent();
                }
            });

        // Close all windows
        mnFile.add(new Action("Exit", KeyEvent.VK_X) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getEditor().verifyExit();
                    }
                });

        // "Position" top-level menu

        mnPosition.add(mnEnterCoordinates);
        mnPosition.add(mnJumpToSelection);
        mnPosition.add(mnUnstickMouse);

        menuBar.add(mnPosition);

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
        double[] lineWidths = {0.0006, 0.0012, 0.0017, 0.0020, 0.0024, 0.0029,
                               0.0034, 0.0048};

        mnLineWidth.add(customLineWidth);
        lineWidthGroup.add(customLineWidth);
        for (int i = 0; i < lineWidths.length; ++i) {
            LineWidthMenuItem item = new LineWidthMenuItem(lineWidths[i]);
            if (lineWidths[i] == Diagram.STANDARD_LINE_WIDTH) {
                item.setSelected(true);
            }
            mnLineWidth.add(item);
        }
        mnCurve.add(mnLineWidth);
        mnCurve.add(smoothed);

        mnCurve.add(new Action("Toggle closure",
                               KeyEvent.VK_O,
                               KeyStroke.getKeyStroke('o')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().toggleCurveClosure();
                    finishEvent();
                }
            });

        mnCurve.add(new Action
                    ("Toggle smoothing of selected point", KeyEvent.VK_P, "typed ,") {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().toggleCusp();
                    finishEvent();
                }
            });

        mnCurve.add(new Action("Select left vertex", KeyEvent.VK_L, "typed [") {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().shiftActiveVertex(false);
                    finishEvent();
                }
            });

        mnCurve.add(new Action("Select right vertex", KeyEvent.VK_R, "typed ]") {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().shiftActiveVertex(true);
                    finishEvent();
                }
            });

        menuBar.add(mnCurve);

        // "Properties" top-level menu
        mnProperties.setMnemonic(KeyEvent.VK_R);

        mnFont.setMnemonic(KeyEvent.VK_F);
        FontMenuItem sans = new FontMenuItem
            ("Sans", "DejaVu LGC Sans PED");
        mnFont.add(sans);
        sans.setSelected(true);
        mnFont.add(new FontMenuItem("Serif", "DejaVu LGC Serif PED"));
        mnFont.add(new FontMenuItem("Sans (Widely-spaced lines)",
                                    "DejaVu LGC Sans GRUMP"));

        mnProperties.add(mnFont);

        mnKeys.setMnemonic(KeyEvent.VK_K);
        mnKeys.add(mnAddKey);

        mnKeys.add(new Action("List", KeyEvent.VK_L) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().listKeyValues();
                    finishEvent();
                }
            });
        mnProperties.add(mnKeys);

        mnMargins.setMnemonic(KeyEvent.VK_M);
        mnMargins.add(toMenuItem(new Action("Auto-fit", KeyEvent.VK_A) {
                {
                    putValue(SHORT_DESCRIPTION,
                             "Eliminate all excees white space");
                }
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().computeMargins();
                    finishEvent();
                }
            }));
        mnMargins.add(toMenuItem(new Action("Expand all", KeyEvent.VK_X) {
                {
                    putValue(SHORT_DESCRIPTION,
                             "Expand the diagram on all sides");
                }
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().expandMargins(0.2);
                    finishEvent();
                }
            }));
        mnMargins.add(new Action("Set", KeyEvent.VK_S) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setMargins();
                    finishEvent();
                }
            });

        mnProperties.add(mnMargins);

        mnScale.setMnemonic(KeyEvent.VK_S);
        mnScale.add(scaleXUnits);
        mnScale.add(scaleYUnits);
        mnScale.add(scaleBoth);
        mnProperties.add(mnScale);

        mnTags.setMnemonic(KeyEvent.VK_T);
        mnProperties.add(mnTags);

        mnTags.add(mnAddTag);
        mnTags.add(mnTagsSeparator);
        mnRemoveTag.setEnabled(false);
        mnTags.add(mnRemoveTag);

        mnTagsSeparator.setVisible(false);
        mnRemoveTag.setVisible(false);

        mnVariables.setMnemonic(KeyEvent.VK_V);
        mnProperties.add(mnVariables);
        mnVariables.add(new Action("Add", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().addVariable();
                    finishEvent();
                }
            });
        mnVariables.add(mnVariablesSeparator);
        mnEditVariable.setEnabled(false);
        mnVariables.add(mnEditVariable);

        mnVariablesSeparator.setVisible(false);
        mnEditVariable.setVisible(false);

        mnProperties.add(actSwapXY);

        setAspectRatio = new Action
            ("Aspect ratio", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().setAspectRatio();
                    finishEvent();
                }
            };
        setAspectRatio.setEnabled(false);
        mnProperties.add(setAspectRatio);
        mnProperties.add(mnSetTitle);

        mnProperties.add(pixelMode);
        mnProperties.add(editingEnabled);
        mnProperties.add(hideImages);
        menuBar.add(mnProperties);

        // "Digitize" top-level menu
        JMenu mnDigit = new JMenu("Digitize");
        mnDigit.setMnemonic(KeyEvent.VK_I);
        menuBar.add(mnDigit);
        mnDigit.add(mnExportText);

        mnDigit.add(new Action("Export selected curve or label's coordinates",
                        KeyEvent.VK_C,
                        KeyStroke.getKeyStroke("typed C")) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().exportSelectedCoordinates();
                    finishEvent();
                }
            });

        mnDigit.add(new Action("Export all curve and label coordinates",
                              KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().exportAllCoordinates();
                    finishEvent();
                }
            });

        mnDigit.add(mnImportCoordinates);

        // "Chemistry" top-level menu
        JMenu mnChem = new JMenu("Chemistry");
        mnChem.setMnemonic(KeyEvent.VK_M);
        menuBar.add(mnChem);

        mnSetComponents.setMnemonic(KeyEvent.VK_C);
        setTopComponent.setEnabled(false);
        mnSetComponents.add(setLeftComponent);
        mnSetComponents.add(setRightComponent);
        mnSetComponents.add(setTopComponent);
        mnSetComponents.add(guessComponents);
        mnChem.add(mnSetComponents);

        mnChem.add(swapBinary);

        mnSwap.add(new Action
                ("Left \u2194 Right", KeyEvent.VK_L) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().swapDiagramComponents(Side.LEFT, Side.RIGHT);
                    finishEvent();
                }
            });
        mnSwap.add(new Action
                ("Left \u2194 Top", KeyEvent.VK_T) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().swapDiagramComponents(Side.LEFT, Side.TOP);
                    finishEvent();
                }
            });
        mnSwap.add(new Action
                ("Top \u2194 Right", KeyEvent.VK_R) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().swapDiagramComponents(Side.TOP, Side.RIGHT);
                    finishEvent();
                }
            });
        mnChem.add(mnSwap);

        {
            JMenu mnProp = new JMenu("Proportions");
            mnProp.setMnemonic(KeyEvent.VK_P);
            mnProp.add(convertToMole);
            mnProp.add(convertToWeight);
            mnProp.add(usingWeightFraction);
            mnChem.add(mnProp);
        }
        mnChem.add(mnCopyFormulas);
        mnChem.add(new Action
                   ("Formula to mole/weight fraction", KeyEvent.VK_M,
                    KeyStroke.getKeyStroke('%')) {
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().computeFraction();
                    finishEvent();
                }
            });

        // "View" top-level menu
        mnView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(mnView);

        mnBackgroundImage.setMnemonic(KeyEvent.VK_B);
        mnBackgroundImage.setEnabled(false);
        lightGrayBackgroundImage = new BackgroundImageMenuItem
            ("Light", StandardAlpha.LIGHT_GRAY, KeyEvent.VK_L);
        mnBackgroundImage.add(lightGrayBackgroundImage);
        darkGrayBackgroundImage = new BackgroundImageMenuItem
            ("Medium", StandardAlpha.DARK_GRAY, KeyEvent.VK_M);
        mnBackgroundImage.add(darkGrayBackgroundImage);
        blackBackgroundImage = new BackgroundImageMenuItem
            ("Dark", StandardAlpha.BLACK, KeyEvent.VK_D);
        mnBackgroundImage.add(blackBackgroundImage);
        noBackgroundImage = new BackgroundImageMenuItem
            ("Hide", StandardAlpha.NONE, KeyEvent.VK_H);
        noBackgroundImage.getAction().putValue
            (AbstractAction.ACCELERATOR_KEY,
             KeyStroke.getKeyStroke("control H"));
        mnBackgroundImage.add(noBackgroundImage);
        mnBackgroundImage.add
            (new Action("Detach", KeyEvent.VK_E) {
                    @Override public void actionPerformed(ActionEvent e) {
                        getEditor().removeImage();
                        finishEvent();
                    }
                });
        lightGrayBackgroundImage.setSelected(true);
        mnView.add(mnBackgroundImage);

        mnView.add(new Action("Best fit (100% zoom)", KeyEvent.VK_B,
                              KeyStroke.getKeyStroke("control B")) {
                {
                    putValue(SHORT_DESCRIPTION,
                             "Adjust the zoom ratio so the diagram is fully visible");
                }
                @Override public void actionPerformed(ActionEvent e) {
                    getEditor().bestFit();
                    finishEvent();
                }
            });
        mnView.add(showGrid);
        mnView.add(showMathWindow);
        mnView.add(mnHints);
        showMathWindow.setSelected(true);

        JMenu mnHelp = new JMenu("Help");
        mnHelp.setMnemonic(KeyEvent.VK_H);
        menuBar.add(mnHelp);
        mnHelp.add(new Action("Help", KeyEvent.VK_H, "F1") {
                @Override public void actionPerformed(ActionEvent e) {
                    help();
                    finishEvent();
                }
            });

        mnHelp.add(new Action("About", KeyEvent.VK_A) {
                @Override public void actionPerformed(ActionEvent e) {
                    about();
                    finishEvent();
                }
            });

        JumpAction[] arrows1 =
            { new JumpAction("Up", KeyEvent.VK_U, "shift UP", 0, -1),
              new JumpAction("Down", KeyEvent.VK_D, "shift DOWN", 0, 1),
              new JumpAction("Left", KeyEvent.VK_L, "shift LEFT", -1, 0),
              new JumpAction("Right", KeyEvent.VK_R, "shift RIGHT", 1, 0) };
        for (JumpAction a : arrows1) {
            mnJump.add(a);
            enable(a);
        }

        AdjustAction[] arrows =
            { new AdjustAction("Up", KeyEvent.VK_U, "UP", 0, -1),
              new AdjustAction("Down", KeyEvent.VK_D, "DOWN", 0, 1),
              new AdjustAction("Left", KeyEvent.VK_L, "LEFT", -1, 0),
              new AdjustAction("Right", KeyEvent.VK_R, "RIGHT", 1, 0) };
        for (AdjustAction a : arrows) {
            mnStep.add(a);
            enable(a);
        }

        // Enable shortcuts for actions that do not appear in the top
        // menu because they are position-sensitive, or in the case of
        // actMonitor, semi-secret.
        for (Action act: new Action[]
            {
              actAddAutoPositionedVertex,
              actAddVertex,
              actAutoPosition,
              actCenterMouse,
              actCircle,
              actColor,
              actCopy,
              actCopyAndPaste,
              actCopyStatusBar,
              actCut,
              actCutAll,
              actCutRegion,
              actDeselect,
              actEditSelection,
              actIsotherm,
              actLeftArrow,
              actMakeDefault,
              actMonitor,
              actMovePoint,
              actMoveSelection,
              actNearestCurve,
              actNearestGridPoint,
              actNearestPoint,
              actPaste,
              actRemoveSelection,
              actRedo,
              actResetToDefault,
              actRightArrow,
              actRuler,
              actSelectNearestCurve,
              actSelectNearestPoint,
              actShiftPressed,
              actShiftReleased,
              actText,
              actTieLine,
              actUndo,
            }) {
            enable(act);
        }

        enableZoom();
        setIconImage(getIcon());
    }

    public void setReloadVisible(boolean b) {
        mnReload.setVisible(b);
    }

    public void setNewDiagramVisible(boolean b) {
        mnNewDiagram.setVisible(b);
    }

    public void setOpenVisible(boolean b) {
        mnOpen.setVisible(b);
    }

    public JMenu createLayerMenu() {
        JMenu mn = new JMenu("Layer");
        for (LayerAction a : layers) {
            mn.add(a);
        }
        mn.setMnemonic(KeyEvent.VK_L);
        return mn;
    }

    public JMenu createDecorationsMenu() {
        JMenu mn = new JMenu("Add decoration");
        mn.setMnemonic(KeyEvent.VK_D);
        mn.add(actText);
        mn.add(actIsotherm);
        mn.add(actLeftArrow);
        mn.add(actRightArrow);
        mn.add(actRuler);
        mn.add(actCircle);
        mn.add(actTieLine);
        return mn;
    }

    /* Set whether the Math window is visible or not. */
    public void setMathWindowVisible(boolean b) {
        getEditor().mathWindow.setVisible(b);
        showMathWindow.setSelected(b);
    }

    public void setEditable(boolean b) {
        BasicEditor e = getEditor();
        e.setVisible(actSave, b);
        e.setVisible(actSaveAsPED, b);
        mnUnstickMouse.setVisible(b);
        mnCurve.setVisible(b);
        mnFont.setVisible(b);
        e.setVisible(setAspectRatio, b);
        mnKeys.setVisible(b);
        e.setVisible(actAddKey, b);
        mnMargins.setVisible(b);
        mnScale.setVisible(b);
        e.setVisible(actAddTag, b);
        // TODO Make the tag menu invisible if not editable and no
        // tags; also make it so the deletion part doesn't delete. Yuck!
        e.setVisible(actSetTitle, b);
        mnVariables.setVisible(b);

        mnProperties.setVisible(getVisibleItemCount(mnProperties) > 0);
        mnImportCoordinates.setVisible(b);
        mnSetComponents.setVisible(b);

        usingWeightFraction.setVisible(b);
        mnBackgroundImage.setVisible(b);

        for (Action act: new Action[]
            { actMoveSelection,
              actMovePoint,
              actAddVertex,
              actAddAutoPositionedVertex,
              actText,
              actIsotherm,
              actLeftArrow,
              actRightArrow}) {
            act.setEnabled(b);
        }

        if (isEditingEnabled() != b) {
            setEditingEnabled(b);
        }

        mnTags.setVisible(b || firstTagIndex() < mnTags.getItemCount());
    }


    public boolean isEditingEnabled() {
        return editingEnabled.isSelected();
    }

    public void setEditingEnabled(boolean b) {
        editingEnabled.setSelected(b);
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

    /** The event indicating that a JPopupMenu will close may precede
        the action that the menu selection triggered, which makes it
        hard to create a self-contained solution of how to determine
        when to go back to letting the logical diagram position track
        the mouse instead of having the logical diagram position be
        the position where the popup menu was last triggered. My
        solution is to have all events that may be triggered by
        right-clicks end by calling finishEvent(). */
    void finishEvent() {
        BasicEditor e = getEditor();
        if (e != null) {
            e.rightClick = null;
        }
    }

    @Override public void update(Observable o, Object arg) {
        Diagram e = getEditor();
        if (e != null) {
            setTitle(e.getProvisionalTitle());
            repaint();
        }
    }

    /** This method is assumed to be a passive receiver of information
        that the font name has changed, to reflect the change in the
        menu selection. To actively change the font name, use
        BasicEditor#setFontName(s) instead. */
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

    protected void help() {
        if (isEditingEnabled()) {
            ShowHTML.show(longHelpFile, this);
        } else {
            ShowHTML.show(shortHelpFile, this);
        }
    }

    protected void about() {
        ShowHTML.show(helpAboutFile, this);
    }

    protected void setStatus(String s) {
        statusLabel.setText("<html><font size=\"-2\">" + ((s != null) ? s : "")
                            + "</font></html>");
    }

    protected EditPane getEditPane() {
        return (EditPane) imagePane;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    /** This is not a public interface. It is an interface that BasicEditor
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

    /** This is not a public interface. It is an interface that BasicEditor
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

    /** This is not a public interface. It is an interface that BasicEditor
        uses to perform UI operations in support of an addVariable()
        request. */
    void addVariable(String variable) {
        mnVariablesSeparator.setVisible(true);
        int itemCount = mnVariables.getItemCount();

        mnVariablesSeparator.setVisible(true);
        mnEditVariable.setVisible(true);
        for (int i = firstVariableIndex(); i <= itemCount; ++i) {
            if (i == itemCount
                || mnVariables.getItem(i).getText().compareTo(variable) > 0) {
                mnVariables.insert(createEditVariableMenu(variable), i);
                break;
            }
        }
    }

    /** Return the index into mnVariables of the first variable. If there are no
        variables, then the returned value will equal getItemCount(). */
    int firstVariableIndex() {
        int itemCount = mnVariables.getItemCount();
        for (int i = 0; i < itemCount; ++i) {
            if (mnVariables.getItem(i) == mnEditVariable) {
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
            mnEditVariable.setVisible(false);
        }
    }

    /** This is not a public interface. It is an interface that BasicEditor
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

    void setColor(Color c) {
        int width = 10;
        int height = 10;

        BufferedImage im = new BufferedImage
            (width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setBackground(c);
        g.clearRect(0, 0, im.getWidth(), im.getHeight());
        if (c.equals(Color.WHITE)) {
            g.setColor(Color.RED);
            g.drawRect(0, 0, im.getWidth()-1, im.getHeight()-1);
        }
        colorLabel.setIcon(new ImageIcon(im));
    }

    static JMenuItem toMenuItem(Action act) {
        JMenuItem it = new JMenuItem(act);
        Object o = act.getValue(Action.SHORT_DESCRIPTION);
        if (o != null) {
            it.setToolTipText((String) o);
        }
        return it;
    }

    /** Enable the keyboard accelerator for the given action. This
        should enable the accelerator much like when you add the
        action to the frame's menu. */
    void enable(AbstractAction act) {
        // Avoid name duplication.
        String name = "_KEY137" + (++actionId);
        KeyStroke accel = (KeyStroke) act.getValue(Action.ACCELERATOR_KEY);
        if (name != null && accel != null) {
            JPanel cp = (JPanel) getContentPane();
            cp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accel, name);
            cp.getActionMap().put(name, act);
        }
    }

    /** Enabling control-plus and control-minus shortcuts is special in the
        developmentally disabled sense. From instructions at
        https://forums.oracle.com/thread/1356291... */
    void enableZoom() {
        JPanel cp = (JPanel) getContentPane();
        InputMap imap = cp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap amap = cp.getActionMap();
        Object amapPlus  = "ctrl+";
        Object amapMinus = "ctrl-";
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK), amapPlus);  // + key in English keyboards
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK), amapPlus);  // + key in non-English keyboards
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK), amapPlus);  // + key on the numpad
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK), amapMinus); // - key
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_DOWN_MASK), amapMinus); // - key on the numpad
        amap.put(amapPlus, actZoomIn);
        amap.put(amapMinus, actZoomOut);
    }

    private static int actionId = 0;

    public void addAlphabetized(JMenu menu, AbstractAction act,
                                int startFrom) {
        String str = (String) act.getValue(Action.NAME);
        int itemCount = menu.getItemCount();
        for (int i = startFrom; i <= itemCount; ++i) {
            if (i == itemCount
                || menu.getItem(i).getText().compareTo(str) > 0) {
                menu.insert(new JMenuItem(act), i);
                break;
            }
        }
    }
}
