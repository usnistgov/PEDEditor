package gov.nist.pededitor;

import java.util.EventObject;

public class StringEvent extends EventObject {
    private static final long serialVersionUID = -4509193865663354233L;

    protected String string;

    public StringEvent(Object source, String string) {
        super(source);
        this.string = string;
    }

    public String getString() {
        return string;
    }

    @Override
	public String toString() {
        return "StringEvent[" + getSource() + ", " + string + "]";
    }
}
