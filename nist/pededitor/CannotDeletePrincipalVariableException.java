/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

class CannotDeletePrincipalVariableException extends Exception {
    private static final long serialVersionUID = 4202654365454151656L;
    LinearAxis v;

    CannotDeletePrincipalVariableException(LinearAxis v) {
        super((String) v.name);
        this.v = v;
    }

    LinearAxis getVariable() {
        return v;
    }
}
