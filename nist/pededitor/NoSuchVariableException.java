/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public class NoSuchVariableException extends Exception {
    private static final long serialVersionUID = 5151827219327809828L;
    String name;

    public NoSuchVariableException(String name) {
        super("No such variable '" + name + "'");
	this.name = name;
    }

    String getVariableName() {
        return name;
    }
}
