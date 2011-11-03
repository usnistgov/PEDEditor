package gov.nist.pededitor;

import java.awt.*;
import java.util.*;

    /** A CropEvent is triggered when the user indicates that their
        crop selection is final. */
public class CropEvent extends EventObject {

    /** Coordinate spaces

        The transforms between spaces are all affine, except that
        transforms from the original space to other spaces will be
        QuadToRect transforms in the case of transforms drawn from
        four sample points, and the inverse transforms are RectToQuad
        transforms. All transforms are PolygonTransforms.

        Original coordinates: offsets into the original scanned image.

        Principal logical coordinates:

        These coordinates are whichever are most natural to work with
        for the application, but an affine transform should suffice to
        transform these coordinates into screen or printed page
        coordinates.

        If logical coordinates do not exist, then anything can be used
        here.

        The specific choice of logical coordinates is as follows:

        Schematic diagrams are treated like binary diagrams whose axes
        cover [0,1] and that have no axis labels.

        For binaries and schematics, the logical coordinates are
        ([0,1], [0,1]).

        For rectangular diagrams (schematics and binaries), either x
        or y or both should range from 0 to 1, and if either one does
        not, then it should range from 0 to some value less than 1,
        indicating that that size should be presented as smaller than
        the other size. The Y value of 0 represents the bottom, not
        the top, of the diagram.

        For ternary diagrams, either x or y or both should range from
        0 to 1, but in this case, a y value of 1 represents the top of
        an equilateral triangle, and the height of an equilateral
        triangle is only sqrt(3)/2 times its width. The top vertex (if
        present) is located at (x=0.5, y=1). Also, if the y dimension
        does not have range [0,1], then it may either have range [0, x
        | x in (0,1)], representing a topless partial ternary, or
        range [x | x in (0,1), 1], representing a ^-shaped partial
        ternary. Similarly, for partial ternary diagrams that show
        only the lower right corner, the minimum x value will be in
        (0,1) and the maximum x value will equal 1.

        All 3 (for ternary) or 4 (for binary) axes may have logical
        scales associated with them as well, but that can wait.

        For ternary diagrams and partial ternary diagrams, the top of
        the diagram space is at (0.5, 1.0) in the
        Diagram space, the left vertex is at (0,0), and the right
        vertex is at (1,0). The output rectangle may or may not be
        smaller than (1,1) for partial ternary diagrams: for ternary
        diagrams missing their tops, the max y value will be less than
        1; for ternary diagrams missing a single vertex, the
        presentation triangle will be equilateral or not depending on
        whether the two existing legs have equal <i>presentation</i> length
        (regardless of whether they have equal logical length).

        Device-independent page space:

        This is a correct-aspect-ratio space with x or y range being
        [0,1], and the other dimension being [0,n | n in (0,1]]. y=0
        represents the top of the diagram. It is a rescaling and
        translation of the diagram space, but with a different (and
        generally larger) bounding rectangle.

        Device space:

        This represents pixels in the printed or displayed image. It
        is a simple rescaling of the page space.

        Logical space:

        TBD. There are lots of variations, everything from no logical
        dimensions to several logical dimensions per axis (e.g. mole%
        versus mass%) to something in between (e.g. ternary diagrams'
        3 logical axes expressed in 2 dimensions).
    */

    Point[] vertices;
    String filename;
    DiagramType diagramType;

    public CropEvent(CropFrame source) {
        super(source);
        vertices = source.getCropPane().getVertices().toArray(new Point[0]);
        filename = source.getFilename();
        diagramType = source.getDiagramType();
    }

    public Point[] getVertices() { return vertices; }
    public String getFilename() { return filename; }
    public DiagramType getDiagramType() { return diagramType; }
}
