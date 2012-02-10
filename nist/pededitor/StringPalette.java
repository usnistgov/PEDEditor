package gov.nist.pededitor;


abstract public class StringPalette {
    Object[] labels = null;

    /** Return the number of strings in this palette. */
    public abstract int size();

    /** Return the literal string contained at position #index. */
    public abstract String get(int index);

    /** Return either a String or an Icon that somehow represents the
       string get(index). */
    public Object getLabel(int index) {
        if (labels == null) {
            int cnt = size();
            labels = new Object[cnt];
            for (int i = 0; i < cnt; ++i) {
                labels[i] = createLabel(i);
            }
        }
        return labels[index];
    }

    /** Create label #index. */
    Object createLabel(int index) {
        return get(index);
    }
}