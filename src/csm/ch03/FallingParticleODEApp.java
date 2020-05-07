/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */
package csm.ch03;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.numerics.*;

/**
 * FallingParticleODEApp demonstrates how to use an ODE solver to model a falling particle.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0  05/16/05
 */
public class FallingParticleODEApp extends AbstractCalculation {

  /**
   * Calculates the time required for an falling particle to reach the ground and prints the result.
   */
  @Override
public void calculate() {
    // gets initial conditions
    double y0 = control.getDouble("Initial y");
    double v0 = control.getDouble("Initial v");
    // creates ball with initial conditions
    FallingParticleODE ball = new FallingParticleODE(y0, v0);
    // creates ODE solver
    ODESolver solver = new Euler(ball); // note how particular algorithm is chosen
    // sets time step dt in the solver
    solver.setStepSize(control.getDouble("dt"));
    while(ball.state[0]>0) {
      solver.step();
    }
    control.println("final time = "+ball.state[2]);
    control.println("y = "+ball.state[0]+" v = "+ball.state[1]);
  }

  /**
   * Resets the program to its initial state.
   */
  @Override
public void reset() {
    control.setValue("Initial y", 10); // sets default input values
    control.setValue("Initial v", 0);
    control.setValue("dt", 0.01);
  }

  /**
   *
   * Starts the Java application.
   *
   * @param args  command line parameters
   *
   */
  public static void main(String[] args) { // creates a calculation control structure for this class
    CalculationControl.createApp(new FallingParticleODEApp());
  }
}

/* 
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
