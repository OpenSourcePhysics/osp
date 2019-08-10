/*These examples are proof-of-concept only.
 *
 * Copyright (c) 2005 W. Christian.
 */

package davidson.gravitation;

import java.awt.*;
import org.opensourcephysics.display.*;
import org.opensourcephysics.numerics.*;

/**
 * Helium models a classical helium atom.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class Helium implements Drawable, ODE {
  int pixRadius=4;
  double[] state = new double[9];  //  state= {x1, vx1, y1, vy1, x2, vx2, y2, vy2, t}
  ODESolver ode_solver = new RK45MultiStep(this);
  Trail trail_1= new Trail(), trail_2=new Trail();

  /**
   * Gets the state array: x1, vx1, y1, vy1, x2, vx2, y2, vy2, t.
   *
   * @return the state
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
    // state[]: x1, vx1, y1, vy1, x2, vx2, y2, vy2, t
    double deltaX=(state[4]-state[0]);                  // x12 separation
    double deltaY=(state[6]-state[2]);                  // y12 separation
    double dr_2=(deltaX*deltaX+deltaY*deltaY);          // r12 squared
    double dr_3=Math.sqrt(dr_2)*dr_2;                   // r12 cubed

    rate[0]=state[1];                                  // x1 rate
    rate[2]=state[3];                                  // y1 rate

    double r_2=state[0]*state[0]+state[2]*state[2];     // r1 squared
    double r_3=r_2*Math.sqrt(r_2);                      // r1 cubed
    rate[1]=-2*state[0]/r_3-deltaX/dr_3;               //vx1 rate
    rate[3]=-2*state[2]/r_3-deltaY/dr_3;               //vy1 rate

    rate[4]=state[5];                                  // x2 rate
    rate[6]=state[7];                                  // y2 rate
    r_2=state[4]*state[4]+state[6]*state[6];            // r2 squared
    r_3=r_2*Math.sqrt(r_2);                             // r2 cubed
    rate[5]=-2*state[4]/r_3+deltaX/dr_3;               //vx2 rate
    rate[7]=-2*state[6]/r_3+deltaY/dr_3;               //vy2 rate
    rate[8]=1;                                         // time rate

  }

  /**
   * Initialize the atom with the given parameters.
   * @param x1
   * @param vy1
   * @param x2
   * @param vy2
   */
  void initialize(double x1, double vy1, double x2, double vy2) {
    state[0] = x1; // x1
    state[1] = 0; // vx1
    state[2] = 0; // y1
    state[3] = vy1; // vy1
    state[4] = x2; // x2
    state[5] = 0; // vx2
    state[6] = 0; // y2
    state[7] = vy2; // vy2
    state[8] = 0; // time
    trail_1.clear(); // clears the old trail
    trail_2.clear(); // clears the old trail
    trail_1.addPoint(state[0], state[2]);
    trail_2.addPoint(state[4], state[6]);
  }

  /**
   * Steps the time using an ode solver.
   */
  public void stepTime() {
    ode_solver.step();
    trail_1.addPoint(state[0],state[2]);
    trail_2.addPoint(state[4],state[6]);
  }

  /**
   * Draws the objects making up the helium atom.
   *
   * @param panel
   * @param g
   */
  public void draw(DrawingPanel panel, Graphics g) {
    trail_1.draw(panel,g);
    trail_2.draw(panel,g);
    int xpix = panel.xToPix(state[0]) - pixRadius;
    int ypix = panel.yToPix(state[2]) - pixRadius;
    g.setColor(Color.red);
    g.fillOval(xpix, ypix, 2*pixRadius, 2*pixRadius);       // draw the particle onto the screen
    xpix = panel.xToPix(state[4]) - pixRadius;
    ypix = panel.yToPix(state[6]) - pixRadius;
    g.setColor(Color.green);
    g.fillOval(xpix, ypix, 2*pixRadius, 2*pixRadius);       // draw the particle onto the screen

  }

}
