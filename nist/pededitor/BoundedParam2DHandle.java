package gov.nist.pededitor;

/** Interface for a DecorationHandle that is associated with a point
    on a Parameterization2D. */
public interface ParameterizableHandle extends DecorationHandle, Parameterizable2D {
    @Override Parameterization2D getParameterization();
    double getT();
}
