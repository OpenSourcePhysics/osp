/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package csm.ch03;
import org.opensourcephysics.display3d.simple3d.*; // Change this line to get a new implementation
import org.opensourcephysics.frames.*;

/**
 * A demonstration of 3D Elements
 * @author Francisco Esquembre
 * @version 1.0  05/16/05
 */
public class Demo3D_1App {

  /**
   * Starts the Java application.
   * @param args  command line parameters
   */
  static public void main(String args[]) {
    Display3DFrame frame = new Display3DFrame("3D Demo 1");
    // panel = new DrawingPanel3D(DrawingPanel3D.DISPLAY_PLANAR_XY);
    frame.setPreferredMinMax(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);
    frame.setDecorationType(org.opensourcephysics.display3d.core.VisualizationHints.DECORATION_AXES);
    frame.setAltitude(0.6);
    frame.setAzimuth(0.2);
    Element cylinder1 = new ElementCylinder();
    cylinder1.setXYZ(0, 0, -0.4);
    cylinder1.setSizeXYZ(0.4, 0.4, 0.8);
    cylinder1.getStyle().setResolution(new Resolution(5, 5, 2));
    frame.addElement(cylinder1);
    Element cylinder2 = new ElementCylinder();
    cylinder2.setXYZ(0.8, -0.8, -0.4);
    cylinder2.setSizeXYZ(0.0, 0.0, 0.8);
    cylinder2.setSizeZ(0.2);
    cylinder2.getStyle().setResolution(new Resolution(5, 5, 2));
    frame.addElement(cylinder2);
    Element sphere1 = new ElementEllipsoid();
    sphere1.setXYZ(-0.8, 0.8, 0);
    sphere1.setSizeXYZ(0.4, 0.4, 0.4);
    frame.addElement(sphere1);
    Element cone1 = new ElementCone();
    cone1.setXYZ(-0.8, -0.8, 0.0);
    cone1.setSizeXYZ(0.4, 0.4, 0.8);
    cone1.getStyle().setResolution(new Resolution(5, 5, 5));
    cone1.getStyle().setFillColor(java.awt.Color.PINK);
    frame.addElement(cone1);
    ElementSurface surface1 = new ElementSurface();
    surface1.setXYZ(-1, -1, -1);
    surface1.getStyle().setFillColor(java.awt.Color.RED);
    frame.addElement(surface1);
    int nu = 16, nv = 32;
    double[][][] data = new double[nu][nv][3];
    for(int i = 0;i<nu;i++) {
      for(int j = 0;j<nv;j++) {
        data[i][j][1] = 0.0+i*2.0/(nu-1);
        data[i][j][0] = 0.0+j*2.0/(nv-1);
        data[i][j][2] = Math.cos(3.0*(data[i][j][1]-1))*(data[i][j][0]-1)*(1.5-data[i][j][0])/2.0;
      }
    }
    surface1.setData(data);
    frame.setSize(600, 600);
    frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
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
