/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.net.*;
import javax.swing.*;

/** IDs and icons for different types of PEDs */
public enum DiagramType {
    BINARY ("images/binaryicon.png", "Binary or Cartesian", 4, false),
    OTHER ("images/schematicicon.png", "Free-form diagram or map", 4, false),
    TERNARY ("images/triangleicon.png", "Ternary", 3, true),
    TERNARY_LEFT ("images/leftangleicon.png",
                  "Partial ternary -- bottom left corner", 3, true, 0),
    TERNARY_RIGHT ("images/rightangleicon.png",
                   "Partial ternary -- bottom right corner", 3, true, 2),
    TERNARY_TOP ("images/upangleicon.png",
                 "Partial ternary -- top corner", 3, true, 1),
    TERNARY_BOTTOM ("images/trianglebottomicon.png",
                    "Partial ternary -- trapezoid", 4, true);

    private final int mVertexCnt;
    private final URL mIconUrl;
    private final String mDescription;
    private final int mTriangleVertexNo;
    private final boolean mIsTernary;
    private Icon mIcon = null;

    DiagramType(String imagePath, String description, int vertexCnt,
                boolean isTernary, int triangleVertexNo) {
        this.mDescription = description;
        this.mVertexCnt = vertexCnt;
        this.mIsTernary = isTernary;
        this.mTriangleVertexNo = triangleVertexNo;
        mIconUrl = DiagramType.class.getResource(imagePath);
        if (mIconUrl == null) {
            throw new IllegalStateException("Could not load " + imagePath);
        }
    }

    DiagramType(String imagePath, String description, int vertexCnt,
                boolean isTernary) {
        this(imagePath, description, vertexCnt, isTernary, -1);
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

    /** For left, right, and top partial ternaries only -- return the
        number of the vertex the diagram is centered on. As usual,
        start with 0 = lower left, and successive numbers proceed
        clockwise. */
    public int getTriangleVertexNo() {
        return mTriangleVertexNo;
    }
}
