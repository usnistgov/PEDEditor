package gov.nist.pededitor;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** Enum for standard stroke styles. */
public enum StandardStroke {
    SOLID (CompositeStroke.getSolidLine()),
    DOT (CompositeStroke.getDottedLine()),
    DASH (CompositeStroke.getDashedLine()),
    DOT_DASH (CompositeStroke.getDotDashLine()),
    RAILROAD (CompositeStroke.getRailroadLine());

    private final CompositeStroke stroke;

    StandardStroke(CompositeStroke stroke) {
        this.stroke = stroke;
    }

    public CompositeStroke getStroke() { return stroke; }
}
