package davidson.gr;

import java.awt.Graphics;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.numerics.RK45MultiStep;
import org.opensourcephysics.controls.*;
import javax.swing.JFrame;

/**
 * TrajectoryClassical models the classical trajectory of a particle in a spherically symmetric gravitational field
 * using polar coordinates.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class ClassicalTrajectory extends AbstractTrajectory {
  public static final double GM = 1.0;

  public ClassicalTrajectory() {
    // initial state values = {r, dr/dt, phi, dphi/dt, t}
    state = new double[] {1.0, 0.0, 0.0, 1.0, 0.0};
    stateInitial = new double[] {1.0, 0.0, 0.0, 1.0, 0.0};
    ode_solver = new RK45MultiStep(this);
    pixRadius = 4;
    setXY(1, 0); // sets the initial position
  }

  /**
   * Gets the state array.
   *
   * @return an array containing {x, v, t}
   */
  public double[] getState() {
    return state;
  }

  /**
   * Calculates the rate array using the given state.
   *
   * Values in the rate array are overwritten.
   *
   * @param state  the state
   * @param rate   the rate
   */
  public void getRate(double[] state, double[] rate) {
    // state = {r, dr/dt, phi, dphi/dt, t}
    double r = state[0]; // define local variable for readability
    rate[0] = state[1];                   // dr/dt
    rate[1] = r*state[3]*state[3]-GM/r/r; // dr_v/dt
    rate[2] = state[3];                   // dphi/dt
    rate[3] = -2*state[3]*state[1]/r;     // dphi_v/dt
    rate[4] = 1;                          // dt/dt = 1
  }

  /**
   * Sets position of the particle in response to mouse actions.
   *
   * The state array is also updated.
   *
   * @param x
   * @param y
   */
  public void setXY(double x, double y) {
    super.setXY(x, y);
    state[0]=Math.sqrt(x*x+y*y);   // set r
    //state[1]=0;                    // set dr/dt=0
    state[2] = Math.atan2(y, x);   // set phi
    //state[3] = vt/state[0];        // sets dphi/dt= vt/r
    initialize(state);
  }

  /**
   * Initializes the particle's state.
   * The state array = {r, dr/dt, phi, dphi/dt, t}
   *
   * @param newState state array
   *
   */
public void initialize(double[] newState) {
  if(newState!=state) System.arraycopy(newState,0,state, 0, newState.length);
  System.arraycopy(newState,0,stateInitial, 0, newState.length);
  x = state[0]*Math.cos(state[2]);
  y = state[0]*Math.sin(state[2]);
  trail.clear();        // clears the old trail
  trail.moveToPoint(x, y); // add first point to trail
}

public void resetInitial(){
  initialize(stateInitial);
}

/**
 * Displays the Schwarzschild particle's initial conditions in an editor.
 * @return OSPControl
 */
public OSPTableInspector edit(){
  OSPTableInspector inspector = new OSPTableInspector(true, true);
  Control control=inspector.getControl();
  inspector.setTitle("Classical Particle");
  inspector.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  double r=1, phi=0;
  control.setValue("r", r);
  control.setValue("dr/dt", phi);
  control.setValue(PHI, 0);
  control.setValue(PHIRATE, 1);
  control.setValue("draggable state", true);
  control.setValue("track points", 0);
  control.setValue("label", "");
  x=r*Math.cos(phi);
  y=r*Math.sin(phi);
  inspector.setSize(160,170);
  return inspector;
}



  /**
   * Steps the time using an ode solver.
   */
  public void stepTime() {
    ode_solver.step();
    x = state[0]*Math.cos(state[2]);
    y = state[0]*Math.sin(state[2]);
    trail.addPoint(x, y);
  }

  /**
   * Draws the circle and the trail.
   *
   * @param panel
   * @param g
   */
  public void draw(DrawingPanel panel, Graphics g) {
    trail.draw(panel, g);
    super.draw(panel, g);
  }

  public static XML.ObjectLoader getLoader() {
    return new ClassicalTrajectoryLoader();
  }

  /**
   * A class to save and load InteractiveCircle data in an XMLControl.
   */
  static class ClassicalTrajectoryLoader extends AbstractTrajectoryLoader {

      /**
       * Saves the TrajectoryClassical's data in the xml control.
       * @param control XMLControl
       * @param obj Object
       */
      public void saveObject(XMLControl control, Object obj) {
        super.saveObject(control, obj);
        //ClassicalTrajectory trajectory = (ClassicalTrajectory) obj;
    }

      /**
       * Creates a default InteractiveCircle.
       * @param control XMLControl
       * @return Object
       */
      public Object createObject(XMLControl control) {
        return new ClassicalTrajectory();
      }

      /**
       * Loads data from the xml control into the TrajectoryClassical object.
       * @param control XMLControl
       * @param obj Object
       * @return Object
       */
      public Object loadObject(XMLControl control, Object obj) {
        super.loadObject(control, obj);
        return obj;
    }
  }

}
