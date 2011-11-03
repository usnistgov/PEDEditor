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
    BINARY ("images/binaryicon.png", "Binary or Cartesian", 4),
    TERNARY ("images/triangleicon.png", "Ternary", 3),
    TERNARY_LEFT ("images/leftangleicon.png",
                  "Partial Ternary -- bottom left corner", 3),
    TERNARY_RIGHT ("images/rightangleicon.png",
                   "Partial ternary -- bottom right corner", 3),
    TERNARY_TOP ("images/upangleicon.png",
                 "Partial ternary -- top corner", 3),
    TERNARY_BOTTOM ("images/trianglebottomicon.png",
                    "Partial ternary -- top corner missing", 4),
    OTHER ("images/schematicicon.png", "Schematic/Other", 4);

    private final int vertexCnt;
    private final URL iconUrl;
    private Icon icon = null;
    private String description;

    DiagramType(String imagePath, String description, int vertexCnt) {
        this.description = description;
        this.vertexCnt = vertexCnt;
        iconUrl = DiagramType.class.getResource(imagePath);
        if (iconUrl == null) {
            throw new IllegalStateException("Could not load " + imagePath);
        }
    }

    public Icon getIcon() {
        if (icon == null) {
            icon = new ImageIcon(iconUrl);
            if (icon == null) {
                throw new IllegalStateException("Could not load " + iconUrl);
            }
        }
        return icon;
    }

    public String getDescription() {
        return description;
    }

    /** @return the number of vertices in the outline of this diagram. */
    public int getVertexCnt() {
        return vertexCnt;
    }
}
