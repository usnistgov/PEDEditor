package gov.nist.pededitor;

/** Wrapper class for conversion of PED files to PDF files. */
public class LoadAndSave {
    public static void main(String[] args) {
        if (args.length == 2) {
            String inDir = args[0];
            String outDir = args[1];
            PEDToPDF.fixAll(inDir, outDir);
        } else {
            System.err.println("Expected 2 arguments");
        }
    }
}
