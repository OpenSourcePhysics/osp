/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package csm.ch03;
import java.awt.Color;

//import java.awt.*;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.Display3DFrame;
import org.opensourcephysics.display3d.simple3d.*;
import org.opensourcephysics.display3d.core.Resolution;

/**
 * Ball3DApp demonstrates the 3D drawing framework by creating a bouncing ball simulation.
 * and implementing the doStep method.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0  05/16/05
 */
public class Ball3DApp extends AbstractSimulation {
  Display3DFrame frame = new Display3DFrame("3D Ball");
  Element ball = new ElementEllipsoid();
  double time = 0, dt = 0.1;
  double vz = 0;

  /**
   *
   * Constructs Ball3DApp and initializes the drawing.
   *
   */
  public Ball3DApp() {
    frame.setPreferredMinMax(-5.0, 5.0, -5.0, 5.0, 0.0, 10.0);
    ball.setXYZ(0, 0, 9);
    ball.setSizeXYZ(1, 1, 1); // ball displayed in 3D as a planar ellipse of size (dx,dy,dz)
    frame.addElement(ball);
    Element box = new ElementBox();
    box.setXYZ(0, 0, 0);
    box.setSizeXYZ(4, 4, 1);
    box.getStyle().setFillColor(Color.RED);
    // divide sides of box into smaller rectangles
    box.getStyle().setResolution(new Resolution(5, 5, 2));
    frame.addElement(box);
    frame.setMessage("time = "+ControlUtils.f2(time));
  }

  /**
   *
   * Does an animation step by moving the ball.
   *
   */
  @Override
protected void doStep() {
    time += 0.1;
    double z = ball.getZ()+vz*dt-4.9*dt*dt;
    vz -= 9.8*dt;
    if((vz<0)&&(z<1)) {
      vz = -vz;
    }
    ball.setZ(z);
    frame.setMessage("time = "+ControlUtils.f2(time));
  }

  /**
   *
   * Starts the Java application.
   *
   * @param args  command line parameters
   *
   */
  public static void main(String[] args) {
    SimulationControl.createApp(new Ball3DApp());
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
