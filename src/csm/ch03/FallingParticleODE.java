/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package csm.ch03;
import org.opensourcephysics.numerics.*;

/**
 * FallingParticleODE models a falling particle by implementing the ODE interface.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0  05/16/05
 */
public class FallingParticleODE implements ODE {
  final static double g = 9.8;
  double[] state = new double[3];

  /**
   * Constructs the FallingParticleODE model with the given intial postion and velocity.
   *
   * @param y double
   * @param v double
   */
  public FallingParticleODE(double y, double v) {
    state[0] = y;
    state[1] = v;
    state[2] = 0; // initial time
  }

  /**
   * Gets the state array.  Required to implement ODE interface
   *
   * @return double[]
   */
  @Override
public double[] getState() { // required to implement ODE interface
    return state;
  }

  /**
   * Gets the rate array.  Required to implement ODE interface
   * The rate is computed using the given state, not the object's state.
   *
   * @param state double[]
   * @param rate double[]
   */
  @Override
public void getRate(double[] state, double[] rate) {
    rate[0] = state[1]; // rate of change of y is v
    rate[1] = -g;
    rate[2] = 1;        // rate of change of time is 1
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
