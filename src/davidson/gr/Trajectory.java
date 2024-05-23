package davidson.gr;

import org.opensourcephysics.numerics.ODE;
import org.opensourcephysics.display.TeXParser;

public interface Trajectory extends ODE {

  public final static double SQRTTWO = Math.sqrt(2);
  public static String PHIRATE = TeXParser.parseTeX("d$\\phi$/dt");
  public static String PHI = TeXParser.parseTeX("$\\phi$");

    /**
     * Initializes the state using the given initial state.
     * @param state double[]
     */
    public void initialize(double[] initialState);

    /**
     * Resets the trajectory to its initial state.
     */
    void resetInitial();

    /**
     * Steps the time using an ode solver.
     */
    public void stepTime();

    /**
     * Sets the solution tolerance.
     * @param tol double
     */
    public void setTolerance(double tol);

}
