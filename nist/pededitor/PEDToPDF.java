package gov.nist.pededitor;

import java.io.File;

/** Wrapper class for conversion of PED files to PDF files. */
public class PEDToPDF {
    public static void main(String[] args) {
        // TODO Implement convert-all feature.
        Editor ed = new Editor();
        String ifn = args[0];
        String ofn = args[1];
        ed.openDiagram(new File(ifn));
        ed.saveAsPDF(new File(ofn));
    }
}
