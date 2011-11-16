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
        EditFrameAction(String name, int mnemonic, String accelerator) {
            super(name);
            if (mnemonic != 0) {
                putValue(MNEMONIC_KEY, new Integer(mnemonic));
            }
            putValue(ACCELERATOR_KEY,
                     KeyStroke.getKeyStroke(accelerator));
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
        statusLabel = new JLabel("<html><font size=\"-2\">(?,?)</font></html>");
        statusBar.add(statusLabel);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenu mnOpen = new JMenu("Open");
        mnFile.add(mnOpen);

        JMenuItem mnDiagram = new JMenuItem("Diagram");
        mnOpen.add(mnDiagram);
        mnDiagram.setEnabled(false);

        JMenuItem mnCropImage = new JMenuItem("Crop image");
        mnOpen.add(mnCropImage);
        mnCropImage.setEnabled(false);

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
