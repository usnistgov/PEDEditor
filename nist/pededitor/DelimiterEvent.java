package gov.nist.pededitor;

import java.util.EventObject;

public class DelimiterEvent extends EventObject {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3327008561770899183L;
	protected Delimiter delimiter;

    public DelimiterEvent(Object source, Delimiter delimiter) {
        super(source);
        this.delimiter = delimiter;
    }

    public Delimiter getDelimiter() {
        return delimiter;
    }

    @Override public String toString() {
        return getClass().getSimpleName() + "[" + getSource() + ", "
            + delimiter + "]";
    }
}
