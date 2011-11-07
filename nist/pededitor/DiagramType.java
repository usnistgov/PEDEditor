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
    BINARY ("images/binaryicon.png", "Binary or Cartesian", 4, false),
    TERNARY ("images/triangleicon.png", "Ternary", 3, true),
    TERNARY_LEFT ("images/leftangleicon.png",
                  "Partial Ternary -- bottom left corner", 3, true),
    TERNARY_RIGHT ("images/rightangleicon.png",
                   "Partial ternary -- bottom right corner", 3, true),
    TERNARY_TOP ("images/upangleicon.png",
                 "Partial ternary -- top corner", 3, true),
    TERNARY_BOTTOM ("images/trianglebottomicon.png",
                    "Partial ternary -- top corner missing", 4, true),
        OTHER ("images/schematicicon.png", "Schematic/Other", 4, false);

    private final int mVertexCnt;
    private final URL mIconUrl;
    private boolean mIsTernary;
    private Icon mIcon = null;
    private String mDescription;

    DiagramType(String imagePath, String description, int vertexCnt,
                boolean isTernary) {
        this.mDescription = description;
        this.mVertexCnt = vertexCnt;
        this.mIsTernary = isTernary;
        mIconUrl = DiagramType.class.getResource(imagePath);
        if (mIconUrl == null) {
            throw new IllegalStateException("Could not load " + imagePath);
        }
    }

    public Icon getIcon() {
        if (mIcon == null) {
            mIcon = new ImageIcon(mIconUrl);
            if (mIcon == null) {
                throw new IllegalStateException("Could not load " + mIconUrl);
            }
        }
        return mIcon;
    }

    public String getDescription() {
        return mDescription;
    }

    /** @return the number of vertices in the outline of this diagram. */
    public int getVertexCnt() {
        return mVertexCnt;
    }

    public boolean isTernary() {
        return mIsTernary;
    }
}
