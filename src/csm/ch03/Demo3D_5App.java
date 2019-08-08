/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package csm.ch03;
import org.opensourcephysics.display3d.simple3d.*;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.Display3DFrame;

/**
 * Demo3D_5App creates a basic simulation of a ball in a Box by extending AbstractSimulation
 * and implementing the doStep method.
 *
 * @author F. Esquembre and W. Christian
 * @version 1.0  05/16/05
 */
public class Demo3D_5App extends AbstractSimulation {
  // Graphical elements
  Display3DFrame frame = new Display3DFrame("Ball in box.");
  ElementCircle ball;
  ElementTrail trail;
  double ballRadius = 0.05;
  double x, y, z, vx, vy, vz;
  double dt = 0.1;
  double min = -1.0, max = 1.0;

  /**
   * Constructs a Demo3D_5App.
   */
  public Demo3D_5App() {
    frame.setPreferredMinMax(min, max, min, max, min, max);
    ball = new ElementCircle();
    ball.setSizeXYZ(2*ballRadius, 2*ballRadius, 2*ballRadius);
    frame.addElement(ball);
    trail = new ElementTrail();
    trail.setMaximumPoints(30);
    trail.getStyle().setLineColor(java.awt.Color.GRAY);
    frame.addElement(trail);
  }

  public void initialize() {
    x = (max-min)*(Math.random()-0.5);
    y = (max-min)*(Math.random()-0.5);
    z = (max-min)*(Math.random()-0.5);
    vx = (max-min)*(Math.random()-0.5);
    vy = (max-min)*(Math.random()-0.5);
    vz = (max-min)*(Math.random()-0.5);
    ball.setXYZ(x, y, z);
    trail.clear();
  }

  /**
   * Does an animation step.
   */
  protected void doStep() {
    x += vx*dt;
    y += vy*dt;
    z += vz*dt;
    if(x<min||x>max) {
      vx = -vx;
    }
    if(y<min||y>max) {
      vy = -vy;
    }
    if(z<min||z>max) {
      vz = -vz;
    }
    ball.setXYZ(x, y, z);
    trail.addPoint(x, y, z);
  }

  public static void main(String[] args) {
    SimulationControl.createApp(new Demo3D_5App());
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
