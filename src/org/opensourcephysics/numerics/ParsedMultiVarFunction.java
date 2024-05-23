/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.numerics;

/**
 * ParsedMultiVarFunction defines a function of multiple variables using a String.
 *
 * This function is immutable.  That is, once an instance is created with a particular
 * function string, the function cannot be changed.  Because immutable
 * objects cannot change, they are thread safe and can be freely shared in a Java
 * program.
 *
 * @author Wolfgang Christian
 */
public final class ParsedMultiVarFunction implements MultiVarFunction {
  private final String fStr;
  private final MultiVarFunction myFunction;
  private final String[] myFunctionNames;
  public boolean isNull;

  /**
   * Constructs a ParsedFunction from the given string and independent variable.
   *
   * @param _fStr the function
   * @param var the independent variable
   * @param allowUnknownIdentifiers   always false
   * @throws ParserException
   */
  public ParsedMultiVarFunction(String _fStr, String[] var, boolean allowUnkownIdentifiers) throws ParserException {
    fStr = _fStr;
    isNull = (fStr.equals(SuryonoParser.NULL) || fStr.equals(SuryonoParser.NULL_D));
    SuryonoParser parser = new SuryonoParser(fStr, var, allowUnkownIdentifiers);
    myFunction = parser;
    myFunctionNames = parser.getFunctionNames();
  }

  /**
   * Evaluates the function, f.
   *
   * @param x the value of the independent variable
   *
   * @return the value of the function
   */
  @Override
  public double evaluate(double[] x) {
    return (isNull ? 0 : 
    	myFunction.evaluate(x));
  }

  /**
   * Represents the function as a string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "f(x) = "+fStr; //$NON-NLS-1$
  }

  /**
   * Returns function names.
   * Added by D. Brown 06 Jul 2008
   *
   * @return array of function names
   */
  public String[] getFunctionNames() {
    return myFunctionNames;
  }

  /**
   * Determines if last evaluation resulted in NaN. Added by D Brown 15 Sep 2010.
   *
   * @return true if result was converted from NaN to zero
   */
  public boolean evaluatedToNaN() {
  	return !isNull && myFunction instanceof SuryonoParser && ((SuryonoParser)myFunction).evaluatedToNaN();
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
 * Copyright (c) 2024  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
