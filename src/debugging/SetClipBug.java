/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package debugging;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;
import org.opensourcephysics.frames.*;

/**
 * DrawingApp demonstrates how to create and use Drawable objects.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0  05/16/05
 */
public class SetClipBug extends AbstractSimulation {
	PlotFrame frame = new PlotFrame("x", "y", "Graphics");
  double time=0;

  /**
   * Constructs the DrawingApp and sets the world coordinate scale.
   */
  public SetClipBug() {
    frame.setPreferredMinMax(0, 10, 0, 10);
  }

  /**
   * Creates a rectangle and adds it to the DisplayFrame.
   */
  public void calculate() {
    // gets rectangle location
    int left = control.getInt("xleft");
    int top = control.getInt("ytop");
    // gets rectangle dimensions
    int width = control.getInt("width");
    int height = control.getInt("height");
    Drawable rectangle = new PixelRectangle(left, top, width, height);
    frame.addDrawable(rectangle);
    // frame is automatically rendered after Calculate button is pressed
  }

  /**
   * Resets the program to its initial state.
   */
  public void reset() {
    frame.clearDrawables();        // removes drawables added by the user
    control.setValue("xleft", 0); // sets default input values
    control.setValue("ytop", 70);
    control.setValue("width", 100);
    control.setValue("height", 150);
    frame.setMessage("t="+ControlUtils.f2(time));
    calculate() ;
    frame.append(0, time, time); // trajectory data added
  }

  /**
   * Starts the Java application.
   * @param args  command line parameters
   */
  public static void main(String[] args) { // creates a calculation control structure using this class
  	SimulationControl.createApp(new SetClipBug());
  }

	protected void doStep() {
		time +=0.01;
	  frame.setMessage("t="+ControlUtils.f2(time));
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
