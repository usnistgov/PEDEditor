package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** GUI for entering a chemical formula. */
public class FormulaDialog extends JDialog {
    private static final long serialVersionUID = 3114984690946697109L;
    protected JLabel descr = new JLabel
        ("The string you enter will be placed in the clipboard.\n");
    protected JTextField formula = new JTextField(30);
    protected JLabel hillFormula = new JLabel();
    protected JLabel weight = new JLabel();
    protected transient boolean pressedOK = false;
    @SuppressWarnings("serial")
	protected JButton okButton =  new JButton
        (new AbstractAction("Locate compound in diagram") {
                @Override public void actionPerformed(ActionEvent e) {
                    normalExit();
                }
            });

    public FormulaDialog(JFrame parent) {
        super(parent, "Enter Chemical Formula", false);
        
        formula.getDocument().addDocumentListener
            (new DocumentListener() {
                    @Override public void changedUpdate(DocumentEvent e) {
                        updateFormula();
                    }

                    @Override public void insertUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }

                    @Override public void removeUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }
                });
        
        GridBagUtil gb = new GridBagUtil(getContentPane());

        gb.centerAndEndRow(descr);
        gb.addEast(new JLabel("Formula:"));
        gb.endRowWith(formula);
        gb.addEast(new JLabel("Hill system:"));
        gb.endRowWith(hillFormula);
        gb.addEast(new JLabel("Weight:"));
        gb.endRowWith(weight);
        gb.centerAndEndRow(okButton);
        getRootPane().setDefaultButton(okButton);
    }

    public void normalExit() {
        pressedOK = true;
        setVisible(false);
    }

    public void setFormula(String text) {
        formula.setText(text);
    }

    /** Process a change to formula.getText() */
    void updateFormula() {
        String res = "<html><div style=\"width:350px;\"><p><font color=\"red\">Invalid formula</font>";
        weight.setText("");
        do {
            String formula = getFormula();
            if (formula == null) {
                break;
            }
            
            // Attempt to convert the input from HTML to regular text.
            // This should be mostly harmless even if it was regular
            // text to begin with, since normal chemical formulas
            // don't include <, >, or & anyhow.
            formula = HtmlToText.htmlToText(formula).trim();
            if (formula.isEmpty()) {
                break;
            }
        
            ChemicalString.Match m = ChemicalString.maybeQuotedComposition(formula);
            if (m != null && m.isWholeStringMatch()) {
                res = "<html>" + ChemicalString.autoSubscript(m.toString());
                double wt = m.getWeight();
                if (!Double.isNaN(wt)) {
                    weight.setText(String.format("%.3f", wt));
                }
            } else {
                int endIndex = (m == null) ? 0 : m.endIndex;
                res += "<p>Parse error after <font color=\"red\">&lt;&lt;HERE</font> in '"
                    + formula.substring(0, endIndex)
                    + "<font color=\"red\">&lt;&lt;HERE</font>" + formula.substring(endIndex) + "'";
            }
        } while (false);
        hillFormula.setText(res);
        pack();
    }

    /** Return the text that the user entered into the formula box. */
    public String getFormula() {
        return formula.getText();
    }

    /** Return the text that the user entered into the formula box,
        trimmed and with HTML stripped out. */
    public String getPlainFormula() {
        return HtmlToText.htmlToText(getFormula()).trim();
    }

    public String getHillFormula() {
        ChemicalString.Match m = ChemicalString.maybeQuotedComposition(getFormula());
        if (m != null && m.isWholeStringMatch()) {
            return m.toString();
        }
        return null;
    }

    public Map<String,Double> getComposition() {
        ChemicalString.Match m = ChemicalString.maybeQuotedComposition(getFormula());
        if (m != null && m.isWholeStringMatch()) {
            return m.composition;
        }
        return null;
    }

    /** Show the dialog as document-modal, and return the value of
        getPlainFormula() if a valid formula was entered. Return null
        if the user pressed the exit button or the formula could not
        be parsed. */
    public String showModal() {
        pack();
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        setVisible(true);
        if (pressedOK && getHillFormula() != null) {
            return getPlainFormula();
        }
        return null;
    }

    public static void main(String[] args) {
        String formula = (new FormulaDialog(null)).showModal();
        System.out.println("You selected " + formula);
    }
}
