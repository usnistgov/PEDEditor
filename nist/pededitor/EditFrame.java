package gov.nist.pededitor;

import java.awt.event.*;
import javax.swing.*;

public class EditFrame extends ImageScrollFrame {

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

   /**
    * Create the frame.
    */
   public EditFrame() {
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
      mnEdit.setEnabled(false);
		
      JMenu mnView = new JMenu("View");
      menuBar.add(mnView);

      mnView.add(new EditFrameAction("Zoom In", KeyEvent.VK_I,
                                     "typed +") {
              public void actionPerformed(ActionEvent e) {
                    // TODO setScale(getScale() * 1.25);
                }
            });
      mnView.add(new EditFrameAction("Zoom Out", KeyEvent.VK_O,
                                     "typed -") {
              public void actionPerformed(ActionEvent e) {
                    // TODO setScale(getScale() / 1.25);
                }
            });
		
      JMenu mnHelp = new JMenu("Help");
      menuBar.add(mnHelp);
		
      JMenuItem mnAbout = new JMenuItem("About");
      mnAbout.setEnabled(false);
      mnHelp.add(mnAbout);
      // getContentPane().setBorder(new EmptyBorder(5, 5, 5, 5));
   }

    protected ImagePane newImagePane() {
        return new EditPane(this);
    }

    protected EditPane getEditPane() {
        return (EditPane) getImagePane();
    }
}
