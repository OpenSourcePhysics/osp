/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package csm.ch02;
import org.opensourcephysics.controls.AbstractSimulation;
import org.opensourcephysics.controls.SimulationControl;

/**
 * SimulationApp demonstrates a SimulationControl.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0  05/07/05
 */
public class SimulationApp extends AbstractSimulation {
  int counter = 0;

  /**
   * Does a simulation step by decrementing the counter.
   */
  public void doStep() { // does a simulation step
    control.println("Counter = "+(counter--));
  }

  /**
   * Initializes the simulation by setting the counter variable
   */
  public void initialize() {
    counter = control.getInt("counter");
  }

  /**
   * Resets the simulation parameters to their intial state.
   */
  public void reset() { // invoked when reset button is pressed
    control.setAdjustableValue("counter", 100); // allows dt to be changed after initializaton
  }

  /**
   * Starts the Java application.
   * @param args  command line parameters
   */
  public static void main(String[] args) {
    // creates a simulation structure using this class
    SimulationControl.createApp(new SimulationApp());
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
