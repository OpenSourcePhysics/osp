/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2004  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;
import java.awt.geom.AffineTransform;

/**
 * This manages an AffineTransform array.
 * Every array element is guaranteed to be non-null.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class TransformArray {
  // instance fields
  private AffineTransform[] array;
  private int length;

  /**
   * Constructs a TransformArray object.
   *
   * @param initialLength the initial length of the array
   */
  public TransformArray(int initialLength) {
    length = initialLength;
    array = new AffineTransform[length];
    array[0] = new AffineTransform(); // seed
    fill(array[0]);
  }

  /**
   * Gets the specified transform array element.
   *
   * @param n the array index
   * @return the transform at the specified index
   */
  public AffineTransform get(int n) {
    if(n>=length) {
      setLength(n+1);
    }
    return array[n];
  }

  /**
   * Sets the length of the transform array.
   *
   * @param newLength the new length of the array
   */
  public void setLength(int newLength) {
    if((newLength==length)||(newLength<1)) {
      return;
    }
    AffineTransform[] newArray = new AffineTransform[newLength];
    System.arraycopy(array, 0, newArray, 0, Math.min(newLength, length));
    array = newArray;
    if(newLength>length) {
      AffineTransform at = array[length-1];
      length = newLength;
      fill(at);
    } else {
      length = newLength;
    }
  }

  //__________________________ private methods ___________________________

  /**
   * Replaces null elements of the the array with copies of the specified
   * transform.
   *
   * @param at the transform to copy
   */
  private void fill(AffineTransform at) {
    for(int n = 0; n<length; n++) {
      if(array[n]==null) {
        array[n] = new AffineTransform(at);
      }
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
