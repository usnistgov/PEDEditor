package gov.nist.pededitor;

/** Enum for standard stroke styles. Having so many standard railroad
    types is a kludge, but it permits all line styles to be stored as
    type StandardStroke instead of as CompositeStroke. CompositeStroke
    works, and it's what I originally did, but it's really verbose to
    use a half-dozen lines just to say "solid line", for example.
    Combining the best of both -- concise standard style when
    possible, custom when necessary -- would be nice, but it would
    require custom serializers and de-serializers.
*/
public enum StandardStroke {
    SOLID (CompositeStroke.getSolidLine()),
    DOT (CompositeStroke.getDottedLine()),
    DASH (CompositeStroke.getDashedLine()),
    DOT_DASH (CompositeStroke.getDotDashLine()),
    RAILROAD (CompositeStroke.getRailroadLine(12f, 5f)),
    SOLID_DOT (CompositeStroke.getSolidDotLine()),
    INVISIBLE (CompositeStroke.getInvisibleLine()),
    RAILROAD3 (CompositeStroke.getRailroadLine(12f, 3f)),
    RAILROAD4 (CompositeStroke.getRailroadLine(12f, 4f)),
    RAILROAD5 (CompositeStroke.getRailroadLine(12f, 5f)),
    RAILROAD6 (CompositeStroke.getRailroadLine(12f, 6f)),
    RAILROAD7 (CompositeStroke.getRailroadLine(12f, 7f)),
    RAILROAD8 (CompositeStroke.getRailroadLine(12f, 8f)),
    RAILROAD10 (CompositeStroke.getRailroadLine(12f, 10f)),
    RAILROAD12 (CompositeStroke.getRailroadLine(12f, 12f)),
    RAILROAD15 (CompositeStroke.getRailroadLine(12f, 15f)),
    RAILROAD19 (CompositeStroke.getRailroadLine(12f, 20f)),
    RAILROAD24 (CompositeStroke.getRailroadLine(12f, 24f));

    private final CompositeStroke stroke;

    StandardStroke(CompositeStroke stroke) {
        this.stroke = stroke;
    }

    public CompositeStroke getStroke() { return stroke; }
}
