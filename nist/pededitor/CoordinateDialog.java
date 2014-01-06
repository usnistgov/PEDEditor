package gov.nist.pededitor;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** GUI for entering a coordinate pair using a combination of any two variables. */
public class CoordinateDialog extends JDialog {
    private static final long serialVersionUID = -3619180189523033215L;
    protected JLabel descr = new JLabel
        (BasicEditor.htmlify("Enter a pair of coordinates. Fractions and "
                        + "percentages are allowed."));
    ArrowListenVariableSelector[] vars;
    AutofocusTextField vals[];
    protected transient boolean pressedOK = false;
    @SuppressWarnings("serial")
    protected JButton okButton =  new JButton
        (new AbstractAction("Go") {
                @Override public void actionPerformed(ActionEvent e) {
                    normalExit();
                }
            });
    { okButton.setFocusable(false); }

    @SuppressWarnings("serial") static class AutofocusTextField
        extends JTextField {
        Component arrowTarget = null;
        public AutofocusTextField(int size) {
            super(size);
            addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                                @Override public void run () {
                                    selectAll();
                                }
                            });
                    }
                });
            addKeyListener(new KeyAdapter() {
                    @Override public void keyPressed(KeyEvent e) {
                        switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP:
                        case KeyEvent.VK_DOWN:
                            if (arrowTarget != null) {
                                arrowTarget.requestFocusInWindow();
                            }
                            break;
                        default:
                            break;
                        }
                    }
                });
        }

        public void setArrowTarget(Component c) {
            arrowTarget = c;
        }
    }

    @SuppressWarnings("serial") static class ArrowListenVariableSelector
        extends VariableSelector {
        Component arrowTarget = null;
        public ArrowListenVariableSelector() {
            addKeyListener(new KeyAdapter() {
                    @Override public void keyPressed(KeyEvent e) {
                        switch (e.getKeyCode()) {
                        case KeyEvent.VK_RIGHT:
                        case KeyEvent.VK_LEFT:
                            if (arrowTarget != null) {
                                arrowTarget.requestFocusInWindow();
                            }
                            break;
                        default:
                            break;
                        }
                    }
                });
        }

        public void setArrowTarget(Component c) {
            arrowTarget = c;
        }
    }

    public CoordinateDialog(JFrame parent) {
        super(parent, "Enter Coordinate Pair", false);
        
        int cnt = rowCnt();
        vars = new ArrowListenVariableSelector[cnt];
        vals = new AutofocusTextField[cnt];

        GridBagUtil gb = new GridBagUtil(getContentPane());
        gb.centerAndEndRow(descr);
        gb.addWest(new JLabel("Variable"));
        gb.endRowWith(new JLabel("Value"));
        for (int row = 0; row < rowCnt(); ++row) {
            vars[row] = new ArrowListenVariableSelector();
            gb.addEast(vars[row]);
            vals[row] = new AutofocusTextField(20);
            gb.endRowWith(vals[row]);
            vars[row].setArrowTarget(vals[row]);
        }
        vals[0].setArrowTarget(vals[1]);
        vals[1].setArrowTarget(vals[0]);
        gb.centerAndEndRow(okButton);
        getRootPane().setDefaultButton(okButton);
        setResizable(false);
        vals[0].requestFocusInWindow();
    }

    final public int rowCnt() {
        return 2;
    }

    public void normalExit() {
        pressedOK = true;
        setVisible(false);
    }

    public void setValue(int varNum, String text) {
        vals[varNum].setText(text);
    }

    public void setAxes(List<LinearAxis> axes) {
        for (int row = 0; row < rowCnt(); ++row) {
            vars[row].setAxes(axes);
        }
    }

    public void setAxis(int varNum, LinearAxis axis) {
        vars[varNum].setSelected(axis);
    }

    public LinearAxis getAxis(int varNum, List<LinearAxis> axes) {
        return vars[varNum].getSelected(axes);
    }

    /** Return the text that the user entered into the formula box. */
    public String getValue(int varNum) {
        return vals[varNum].getText();
    }

    /** Show the dialog as document-modal. Return true if the user
        pressed OK. */
    public boolean showModal() {
        pack();
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        pressedOK = false;
        setVisible(true);
        return pressedOK;
    }
}
