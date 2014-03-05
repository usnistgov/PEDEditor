/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Frame;

/** Specialization of NumberTableDialog that only supports a single
    column of data. */
public class NumberColumnDialog extends NumberTableDialog {

    private static final long serialVersionUID = -8031120132173798426L;

    NumberColumnDialog(Frame owner, int rowCnt, String[] labels,
                       String intro) {
        super(owner, rowCnt, 1, labels, null, intro);
    }

    NumberColumnDialog(Frame owner, double[] data, String[] labels,
                       String intro) {
        super(owner, columnToTable(data), labels, null, intro);
    }

    NumberColumnDialog(Frame owner, String[] data, String[] labels,
                       String intro) {
        super(owner, columnToTable(data), labels, null, intro);
    }

    public static double[][] columnToTable(double[] data) {
        double[][] res = new double[data.length][1];
        for (int i = 0; i < data.length; ++i) {
            res[i][0] = data[i];
        }
        return res;
    }

    public static String[][] columnToTable(String[] data) {
        String[][] res = new String[data.length][1];
        for (int i = 0; i < data.length; ++i) {
            res[i][0] = data[i];
        }
        return res;
    }

    public static double[] tableToColumn(double[][] table) {
        if (table == null) {
            return null;
        }
        if (table.length == 0) {
            return new double[0];
        }

        if (table[0].length != 1) {
            throw new IllegalArgumentException("Expected 1-column table");
        }

        double[] res = new double[table.length];
        for (int i = 0; i < table.length; ++i) {
            res[i] = table[i][0];
        }
        return res;
    }

    public double getValueAt(int i) {
        return getValueAt(i,0);
    }

    public String getTextAt(int i) {
        return getTextAt(i,0);
    }

    /** Set whether to show the given field as a percentage. */
    public void setPercentage(int row, boolean b) {
        setPercentage(row, 0, b);
    }

    /** Show the dialog as document-modal, and return the selected
        values. Return null if the dialog was closed
        abnormally. */
    public double[] showModalColumn() {
        double[][] res = showModal();
        return (res == null) ? null : tableToColumn(res);
    }
}
