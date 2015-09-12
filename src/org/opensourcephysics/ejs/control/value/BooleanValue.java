/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.ejs.control.value;

/**
 * A <code>BooleanValue</code> is a <code>Value</code> object that
 * holds a boolean value.
 * <p>
 * @see     Value
 */
public class BooleanValue extends Value {
  public boolean value;

  /**
   * Constructor BooleanValue
   * @param _val
   */
  public BooleanValue(boolean _val) {
    value = _val;
  }

  public boolean getBoolean() {
    return value;
  }

  public int getInteger() {
    if(value) {
      return 1;
    }
    return 0;
  }

  public double getDouble() {
    if(value) {
      return 1.0;
    }
    return 0.0;
  }

  public String getString() {
    if(value) {
      return "true"; //$NON-NLS-1$
    }
    return "false"; //$NON-NLS-1$
  }

  public Object getObject() {
    return null;
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
