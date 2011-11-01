package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

/** IDs and icons for different types of PEDs */
public enum DiagramType {
    BINARY ("images/binaryicon.png", "Binary or Cartesian", 3, 4), /* Binary phase diagrams and other Cartesian graphs */
        TERNARY ("images/triangleicon.png", "Full Ternary", 3), /* Full ternary diagram */
        TERNARY_LEFT ("images/leftangleicon.png",
                      "Partial Ternary -- bottom left corner", 3), /* Partial ternary including the lower left corner */
        TERNARY_RIGHT ("images/rightangleicon.png",
                       "Partial ternary -- bottom right corner", 3), /* Partial ternary including the lower right corner */
        TERNARY_TOP ("images/upangleicon.png",
                     "Partial ternary -- top corner", 3), /* Partial ternary including the top */
        TERNARY_BOTTOM ("images/toplesstriangleicon.png",
                        "Partial ternary -- top corner omitted", 3, 4), /* Partial ternary with the top chopped off */
        OTHER ("images/schematicicon.png", "Schematic/Other", 3, 4) /* Schematic diagram or free-form digitization */
        ;

    private final int minVertexCnt;
    private final int maxVertexCnt;
    private final URL iconUrl;
    private Icon icon = null;
    private String description;

    DiagramType(String imagePath, String description,
                int minVertexCnt, int maxVertexCnt) {
        this.description = description;
        this.minVertexCnt = minVertexCnt;
        this.maxVertexCnt = maxVertexCnt;
        iconUrl = DiagramType.class.getResource(imagePath);
        if (iconUrl == null) {
            throw new IllegalStateException("Could not load " + imagePath);
        }
    }

    DiagramType(String imagePath, String description, int vertexCnt) {
        this(imagePath, description, vertexCnt, vertexCnt);
    }

    Icon getIcon() {
        if (icon == null) {
            icon = new ImageIcon(iconUrl);
            if (icon == null) {
                throw new IllegalStateException("Could not load " + iconUrl);
            }
        }
        return icon;
    }

    String getDescription() {
        return description;
    }

    /** @return only those values that are valid for diagrams with
        vertexCnt vertices. */
    static DiagramType[] values(int vertexCnt) {
        ArrayList<DiagramType> output = new ArrayList<>();
        for (DiagramType d : values()) {
            if (vertexCnt >= d.minVertexCnt && vertexCnt <= d.maxVertexCnt) {
                output.add(d);
            }
        }
        return output.toArray(new DiagramType[0]);
    }
}
