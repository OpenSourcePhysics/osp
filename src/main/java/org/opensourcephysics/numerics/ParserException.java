/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.numerics;

/**
 * Indicates that an error occurred in parser operation, and the operation
 * could not be completed. Used internally in <code>Parser</code> class.
 */
public final class ParserException extends Exception {
  private int errorcode;

  /**
   * The constructor of <code>ParserException</code>.
   *
   * @param code the error code
   */
  public ParserException(int code) {
    super();
    errorcode = code;
  }

  /**
   * Constructor ParserException
   * @param msg
   */
  public ParserException(String msg) {
    super(msg);
    errorcode = MathExpParser.SYNTAX_ERROR; // a generic  syntax error
  }

  /**
   * Gets the error code.
   *
   * @return the error code
   */
  public int getErrorCode() {
    return errorcode;
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
