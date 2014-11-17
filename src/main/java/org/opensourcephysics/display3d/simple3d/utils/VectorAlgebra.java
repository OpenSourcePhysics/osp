/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display3d.simple3d.utils;

/**
 * Some utility functions
 */
public class VectorAlgebra {
  static public double norm(double[] u) {
    return Math.sqrt(u[0]*u[0]+u[1]*u[1]+u[2]*u[2]);
  }

  static public double[] crossProduct(double[] u, double[] v) {
    return new double[] {u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0]};
  }

  static public double[] normalize(double[] u) {
    double r = norm(u);
    return new double[] {u[0]/r, u[1]/r, u[2]/r};
  }

  static public double[] normalTo(double[] vector) {
    if(vector[0]==0.0) {
      return new double[] {1.0, 0.0, 0.0};
    } else if(vector[1]==0.0) {
      return new double[] {0.0, 1.0, 0.0};
    } else if(vector[2]==0.0) {
      return new double[] {0.0, 0.0, 1.0};
    } else {
      double norm = Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]);
      return new double[] {-vector[1]/norm, vector[0]/norm, 0.0};
    }
  }

} // end of class

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
