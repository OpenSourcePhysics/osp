/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <https://www.compadre.org/osp/>
 */

package demo;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.ejs.control.value.Value;
import org.opensourcephysics.frames.PlotFrame;

/**
 * A simple program to test the OSP Library installation.
 *
 * @author anonymous
 * @version 1.0
 */
public class FirstPlotApp {
	
	static Value v = Value.VALUE_NULL;
	
  public static void main(String[] args) {
    PlotFrame frame = new PlotFrame("position", "amplitude", "First Plot"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    frame.setSize(400, 400);
    for(double x = -10, dx = 0.1; x<10; x += dx) {
      frame.append(0, x, Math.sin(x));
    }
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    OSPLog.info("FirstPlotApp running.");
    
    //WC: embedding test
    if(OSPRuntime.isJS) {
    	frame.setLocation(0, 0);       // sets position to top left corner
      OSPRuntime.setAppClass(frame); // undecorated frame can now be embedded into html page
    }
    //WC: end testing
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
 * Copyright (c) 2019  The Open Source Physics project
 *                     https://www.compadre.org/osp
 */
