package gov.nist.pededitor;

import java.awt.*;
import java.util.*;

    /** A CropEvent is triggered when the user indicates that their
        crop selection is final. */
public class CropEvent extends EventObject {
    Point[] vertices;
    String filename;

    public CropEvent(CropFrame source) {
        super(source);
        vertices = source.getCropPane().getVertices().toArray(new Point[0]);
        filename = source.getFilename();
    }

    public Point[] getVertices() { return vertices; }
    public String getFilename() { return filename; }
}
