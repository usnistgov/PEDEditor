package gov.nist.pededitor;

import java.io.File;
import java.io.IOException;

/** Wrapper class for conversion of PED files to JPEG files. */
public class PEDToImage {
    public static void main(String format, String[] args) {
        if (args.length != 4) {
            System.err.println
                ("Usage: java -jar PEDTo" + format + ".jar [PED filename] ["
                 + format + " filename]\n"
                 + "          [max image width] [max image height]\n\n"
                 + "Convert the given PED file to " + format + " format.");
            System.exit(1);
        }
        String ifn = args[0];
        String ofn = args[1];
        int width;
        try {
            width = Integer.parseInt(args[2]);
        } catch (NumberFormatException x) {
            throw new IllegalArgumentException
                ("Invalid width value '" + args[2] + "'");
        }
        int height;
        try {
            height = Integer.parseInt(args[3]);
        } catch (NumberFormatException x) {
            throw new IllegalArgumentException
                ("Invalid height value '" + args[3] + "'");
        }
        Diagram d;
        try {
            d = Diagram.loadFrom(new File(ifn));
        } catch (IOException x) {
            throw new IllegalArgumentException
                ("Invalid input file '" + args[0] + "': " + x);
        }
        try {
            d.saveAsImage(new File(ofn), format, width, height);
        } catch (IOException x) {
            throw new IllegalArgumentException
                ("Invalid output file '" + args[1] + "': " + x);
        }
    }
}
