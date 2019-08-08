/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package csm.ch03;
import java.awt.*;
import org.opensourcephysics.display3d.simple3d.*; // Change this line to get a new implementation
import org.opensourcephysics.frames.*;
import org.opensourcephysics.numerics.*;

/**
 * A demonstration of an OSP 3D group.
 * A group can later be positioned as a single object.
 *
 * @author Francisco Esquembre
 * @version 1.0  05/16/05
 */
public class Demo3D_2App {

  /**
   * Starts the Java application.
   * @param args  command line parameters
   */
  static public void main(String args[]) {
    Display3DFrame frame = new Display3DFrame("3D Demo 1");
    frame.setPreferredMinMax(-10.0, 10.0, -10.0, 10.0, -10.0, 10.0);
    Group group = new Group();
    Element planet = new ElementEllipsoid();
    planet.setXYZ(0.0, 0.0, 0.0);
    planet.setSizeXYZ(10.0, 10.0, 10.0);
    group.addElement(planet);
    ElementText caption = new ElementText();
    caption.setText("Test Program");
    caption.setXYZ(0.0, 0.0, -10.0);
    frame.addElement(caption);
    for(int i = 0;i<10;i++) {
      double alpha = Math.random()*Math.PI*2.0;
      double beta = -Math.PI*0.5+Math.random()*Math.PI;
      double x = 7*Math.cos(alpha)*Math.cos(beta);
      double y = 7*Math.sin(alpha)*Math.cos(beta);
      double z = 7*Math.sin(beta);
      Element satellite = new ElementCircle();
      satellite.setSizeXYZ(2, 2, 2);
      satellite.getStyle().setFillColor(Color.RED);
      satellite.setXYZ(x, y, z);
      group.addElement(satellite);
    }
    frame.addElement(group);
    frame.setSize(300, 300);
    frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    double theta = 0;
    // Affine3DTransformation rotation;
    Quaternion rotation = new Quaternion(1, 0, 0, 0);
    double[] n = new double[] {1, 1, 0}; // rotation axis
    while(true) { // animate until the program exits
      try {
        Thread.sleep(100);
      } catch(InterruptedException ex) {}
      theta += Math.PI/20;
      double cos = Math.cos(theta), sin = Math.sin(theta);
      rotation.setCoordinates(cos, sin*n[0], sin*n[1], sin*n[2]);
      // rotation = Affine3DTransformation.Quaternion(cos, sin*n[0], sin*n[1], sin*n[2]);
      group.setTransformation(rotation);
      frame.render();
    }
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
