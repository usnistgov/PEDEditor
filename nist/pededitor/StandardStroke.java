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
    DOT (CompositeStroke.getDottedLine(5.4)),
    DOT1 (CompositeStroke.getDottedLine(2.4)),
    DOT2 (CompositeStroke.getDottedLine(3.6)),
    DOT3 (CompositeStroke.getDottedLine(5.4)),
    DOT4 (CompositeStroke.getDottedLine(8.1)),
    DOT5 (CompositeStroke.getDottedLine(12.15)),
    DASH (CompositeStroke.getDashedLine(9)),
    DASH1 (CompositeStroke.getDashedLine(4)),
    DASH2 (CompositeStroke.getDashedLine(6)),
    DASH3 (CompositeStroke.getDashedLine(9)),
    DASH4 (CompositeStroke.getDashedLine(13.5f)),
    DASH5 (CompositeStroke.getDashedLine(20)),
    BLANK_FIRST_DASH (CompositeStroke.getBlankFirstDashedLine()),
    DOT_DASH (CompositeStroke.getDotDashLine()),
    RAILROAD (CompositeStroke.getRailroadLine(12f, 5f)),
    SOLID_DOT (CompositeStroke.getSolidDotLine()),
    INVISIBLE (CompositeStroke.getInvisibleLine()),
    RAILROAD1 (CompositeStroke.getRailroadLine(12f, 1f)),
    RAILROAD2 (CompositeStroke.getRailroadLine(12f, 2f)),
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
