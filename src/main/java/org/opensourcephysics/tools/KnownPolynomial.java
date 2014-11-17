/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import org.opensourcephysics.numerics.PolynomialLeastSquareFit;

/**
 * A polynomial that implements KnownFunction.
 */
public class KnownPolynomial extends PolynomialLeastSquareFit implements KnownFunction {
  String[] paramNames = {"A", "B", "C",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                         "D", "E", "F"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

  KnownPolynomial(double[] xdata, double[] ydata, int degree) {
    super(xdata, ydata, degree);
  }

  KnownPolynomial(double[] coeffs) {
    super(coeffs);
  }

  /**
   * Gets the parameter count.
   * @return the number of parameters
   */
  public int getParameterCount() {
    return coefficients.length;
  }

  /**
   * Gets a parameter name.
   *
   * @param i the parameter index
   * @return the name of the parameter
   */
  public String getParameterName(int i) {
    return paramNames[i];
  }

  /**
   * Gets a parameter value.
   *
   * @param i the parameter index
   * @return the value of the parameter
   */
  public double getParameterValue(int i) {
    return coefficients[coefficients.length-i-1];
  }

  /**
   * Sets a parameter value.
   *
   * @param i the parameter index
   * @param value the value
   */
  public void setParameterValue(int i, double value) {
    coefficients[coefficients.length-i-1] = value;
  }

  /**
   * Gets the equation.
   *
   * @param indepVarName the name of the independent variable
   * @return the equation
   */
  public String getExpression(String indepVarName) {
    StringBuffer eqn = new StringBuffer();
    int end = coefficients.length-1;
    for(int i = 0; i<=end; i++) {
      eqn.append(getParameterName(i));
      if(end-i>0) {
        eqn.append("*");   //$NON-NLS-1$
        eqn.append(indepVarName);
        if(end-i>1) {
          eqn.append("^"); //$NON-NLS-1$
          eqn.append(end-i);
        }
        eqn.append(" + "); //$NON-NLS-1$
      }
    }
    return eqn.toString();
  }

  /**
   * Gets the name of the function.
   *
   * @return the name
   */
  public String getName() {
    return "Poly"+(getParameterCount()-1); //$NON-NLS-1$
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
