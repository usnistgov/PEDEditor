package gov.nist.pededitor;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/** Wrapper class for conversion of PED files to JPEG files. */
public class PEDToImage {

    static void help() {
        System.err.println
            ("Usage:\n\n"
             + "    java -jar PEDToImage.jar <PED file> <PDF file>\n\n"
             + "         or\n\n"
             + "    java -jar PEDToImage.jar <PED file> <image file> <width> <height>\n\n"
             + "Supported image formats include GIF, JPEG, and PNG.");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            help();
        }
        String ifn = args[0];
        String ofn = args[1];
        String ext = BasicEditor.getExtension(ofn);
        File ofh = new File(ofn);

        if (ext == null) {
            throw new IllegalArgumentException
                ("Output file '" + ofn + "' missing extension (such as .jpg, .gif, .png, or .pdf)");
        }

        boolean isPDF = ext.equalsIgnoreCase("pdf");

        if (isPDF) {
            if (args.length != 2) {
                help();
            }

            Diagram d;
            try {
                d = Diagram.loadFrom(new File(ifn));
            } catch (IOException x) {
                throw new IllegalArgumentException
                    ("Invalid input file '" + args[0] + "': " + x);
            }
            d.saveAsPDF(ofh);
            return;
        }

        if (args.length != 4) {
            help();
        }

        if (!isPDF) {
            String[] imageExts = ImageIO.getReaderFileSuffixes();

            boolean foundMatch = false;
            for (String x: imageExts) {
                if (ext.equalsIgnoreCase(x)) {
                    ext = x;
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                throw new IllegalArgumentException
                    ("Unsupported image format '" + ext + "'");
            }
        }
        
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
            d.saveAsImage(ofh, ext, width, height);
        } catch (IOException x) {
            throw new IllegalArgumentException
                ("Invalid output file '" + args[1] + "': " + x);
        }
    }
}
