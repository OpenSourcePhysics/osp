/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Stack;

import org.opensourcephysics.tools.KnownFunction;

/**
 * When uncertain and unfilled, UncertainFunctionDrawer draws a KnownFunction 
 * with upper and lower limits based on specified parameter uncertainties. 
 * When not uncertain, behaves exactly like FunctionDrawer. 
 * The function is evaluated at every screen pixel.
 *
 * @author Doug Brown
 * @version 1.0
 */
public class UncertainFunctionDrawer extends FunctionDrawer {
	
  protected GeneralPath upperPath = new GeneralPath();
  protected GeneralPath lowerPath = new GeneralPath();
  protected KnownFunction knownFunction;
  protected boolean uncertain = true;
  protected double[] uncertainties, paramValues;
  protected double[][] uncertainParams;
  protected double[] multiplier = {-1, 1, 0}; // 0 is last to reset parameters to original values
  protected AlphaComposite composite = AlphaComposite.getInstance(
  		AlphaComposite.SRC_OVER, (float) 0.1);

	
  /**
   * Constructs an UncertainFunctionDrawer.
   *
   * @param f the KnownFunction to draw.
   */
  public UncertainFunctionDrawer(KnownFunction f) {
    super(f);
    knownFunction = f;
    uncertainties = new double[f.getParameterCount()];
    paramValues = new double[f.getParameterCount()];
  }

  /**
   * Evaluates the function and determines min/max values 
   * by evaluating the function with uncertainParameter sets.
   * 
   * @param x
   * @param results double[3] {min, value, max}, may be null
   * @return results
   */
  public double[] evaluateMinMax(double x, double[] results) {
    int n = paramValues.length;
    if (results == null)
    	results = new double[3];
    results[0] = results[1] = results[2] = knownFunction.evaluate(x);
    if (uncertainParams == null)
    	return results;
    for (int i = 0; i < uncertainParams.length; i++) {
    	for (int j = 0; j < n; j++) {
    		knownFunction.setParameterValue(j, uncertainParams[i][j]);
    	}
  		double y = knownFunction.evaluate(x);
  		results[0] = Math.min(results[0], y);
  		results[2] = Math.max(results[2], y);
    }
    // restore original parameters
  	for (int j = 0; j < n; j++) {
  		knownFunction.setParameterValue(j, paramValues[j]);
  	}
    return results;
  }
 
	@Override
	protected void checkRange(DrawingPanel panel) {
		// check to see if the range or function has changed
		if ((xrange[0] == panel.getXMin()) && (xrange[1] == panel.getXMax()) && (numpts == panel.getWidth())
				&& !functionChanged) {
			return;
		}
		if (!isUncertain()) {
			super.checkRange(panel);
			return;
		}
		functionChanged = false;
		xrange[0] = panel.getXMin();
		xrange[1] = panel.getXMax();
		numpts = panel.getWidth();
    generalPath.reset();
    upperPath.reset();
    lowerPath.reset();
		if (numpts < 1) {
			return;
		}
    double[] vals = new double[3];
		for (int i = 0; i < paramValues.length; i++) {
			paramValues[i] = knownFunction.getParameterValue(i);		
		}
		evaluateMinMax(xrange[0], vals);
    yrange[0] = vals[0];
    yrange[1] = vals[2]; // starting values for ymin and ymax
    lowerPath.moveTo((float) xrange[0], (float) vals[0]);
    generalPath.moveTo((float) xrange[0], (float) vals[1]);
    upperPath.moveTo((float) xrange[0], (float) vals[2]);
		double x = xrange[0];
		double dx = (xrange[1] - xrange[0]) / (numpts);
		for (int i = 0; i < numpts; i++) {
			x = x + dx;
      evaluateMinMax(x, vals);
      double y = vals[1];
			if (!Double.isNaN(x) && !Double.isNaN(y)) {
				y = Math.min(y, 1.0e+12);
				y = Math.max(y, -1.0e+12);
	      generalPath.lineTo((float) x, (float) y);
	      y = vals[0];
	      if (!Double.isNaN(y)) {
					y = Math.min(y, 1.0e+12);
					y = Math.max(y, -1.0e+12);
	      	lowerPath.lineTo((float) x, (float) y);
		      yrange[0] = Math.min(yrange[0], y); // the minimum value
	      }
	      y = vals[2];
	      if (!Double.isNaN(y)) {
					y = Math.min(y, 1.0e+12);
					y = Math.max(y, -1.0e+12);
	      	upperPath.lineTo((float) x, (float) y);
		      yrange[1] = Math.max(yrange[1], y); // the maximum value
	      }
			}
		}
		
		if (uncertain) {
			lowerPath.append(reverse(upperPath), true);
			lowerPath.closePath();
		}
	}
	
  /**
   * Reverses a GeneralPath. This is based on a snippet from org.geotools/gt2-geometry.
   * @param path the GeneralPath to reverse
   * @return the reversed path
   */
	private GeneralPath reverse(GeneralPath path) {
		PathIterator iterator = path.getPathIterator(new AffineTransform());
		double[] coords = new double[6];
		Stack<Point2D> pts = new Stack<Point2D>();
		while(!iterator.isDone()) {
			int type = iterator.currentSegment(coords);
			if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
				pts.push(new Point2D.Double(coords[0], coords[1]));
			}
			iterator.next();
		}
		GeneralPath reverse = new GeneralPath();
		Point2D.Double pt = (Point2D.Double)pts.pop();
		reverse.moveTo((float)pt.x, (float)pt.y);
		while (!pts.empty()) {
			pt = (Point2D.Double)pts.pop();
			reverse.lineTo((float)pt.x, (float)pt.y);
		}
		return reverse;
	}
	
	private boolean isUncertain() {
		if (filled || !uncertain)
			return false;
		boolean hasUncertainty = false;
		for (int i = 0; i < uncertainties.length; i++) {
			hasUncertainty = hasUncertainty || uncertainties[i] != 0;
		}
		return hasUncertainty;
	}
	
  /**
   * Sets the uncertain property.
   * @param uncertain true to show non-zero uncertainties
   */
	public void setUncertain(boolean uncertain) {
		if (uncertain == this.uncertain)
			return;
		this.uncertain = uncertain;
		functionChanged = true;	
	}
	
  /**
   * Sets the uncertainties of all parameters.
   * @param sigmasAndParams array of uncertainties and uncertain limit parameters
   */
	public void setUncertainties(double[][] sigmasAndParams) {
		int n = uncertainties.length;
		if (sigmasAndParams == null || 
				sigmasAndParams.length < 1 + 2 * n ||
				sigmasAndParams[0].length != n) {
			uncertainParams = null;
		}
		else {
			uncertainties = sigmasAndParams[0];
			uncertainParams = new double[sigmasAndParams.length - 1][];
			System.arraycopy(sigmasAndParams, 1, uncertainParams, 0, uncertainParams.length);
		}
		functionChanged = true;
	}
  
  /**
   * Sets all uncertainties to zero.
   */
	public void clearUncertainties() {
		for (int i = 0; i < uncertainties.length; i++) {
			uncertainties[i] = 0;
			functionChanged = true;
		}
	}
  
  /**
   * Draws the function on a drawing panel.
   * @param panel the drawing panel
   * @param g the graphics context
   */
  @Override
  public void draw(DrawingPanel panel, Graphics g) {
    if (!isUncertain()) {
    	super.draw(panel, g);
    	return;
    }
	  if (!enabled )
		  return;
    if(!measured) {
      checkRange(panel);
    }
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(color);
    // transform from world to pixel coordinates
    Shape s = generalPath.createTransformedShape(panel.getPixelTransform());
    g2.draw(s);
    s = lowerPath.createTransformedShape(panel.getPixelTransform());
    g2.setComposite(composite);
    g2.fill(s);
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
 * Copyright (c) 2017  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
