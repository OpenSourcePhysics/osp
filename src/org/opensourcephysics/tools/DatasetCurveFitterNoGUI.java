/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import org.opensourcephysics.display.*;
import org.opensourcephysics.numerics.Function;
import org.opensourcephysics.numerics.HessianMinimize;
import org.opensourcephysics.numerics.LevenbergMarquardt;
import org.opensourcephysics.numerics.MultiVarFunction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

/**
 * A panel that displays and controls functional curve fits to a Dataset.
 *
 * @author Douglas Brown, Nikolai Avdeev
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DatasetCurveFitterNoGUI extends JPanel {

	Dataset dataset;               // the data to be fit
	double correlation = Double.NaN;
	double[] uncertainties = new double[2];
	FitBuilder fitBuilder;
	HessianMinimize hessian = new HessianMinimize();
	LevenbergMarquardt levmar = new LevenbergMarquardt();
	boolean fitEvaluatedToNaN = false;


	/**
	 * Constructs a DatasetCurveFitter for the specified Dataset.
	 *
	 * @param data    the dataset
	 * @param builder the FitBuilder used for constructing custom fits
	 */
	public DatasetCurveFitterNoGUI(Dataset data, FitBuilder builder) {
		dataset = data;
		fitBuilder = builder;
	}

	/**
	 * Fits a fit function to the current data.
	 *
	 * @param fit the function to fit
	 * @return the rms deviation
	 */
	public double fit(KnownFunction fit) {
		if (fit == null) return Double.NaN;
		if (dataset == null) {
			return Double.NaN;
		}

		double[] x = dataset.getValidXPoints();
		double[] y = dataset.getValidYPoints();
		double devSq = 0;
		double[] prevParams = null;
		// get deviation before fitting
		double prevDevSq = getDevSquared(fit, x, y);
		boolean isLinearFit = false;
		// autofit if checkbox is selected
		if (!Double.isNaN(prevDevSq)) {
			if (fit instanceof KnownPolynomial) {
				KnownPolynomial poly = (KnownPolynomial) fit;
				poly.fitData(x, y);
				isLinearFit = poly.degree() == 1;
			} else if (fit instanceof UserFunction) {
				// use HessianMinimize to autofit user function
				UserFunction f = (UserFunction) fit;
				double[] params = new double[f.getParameterCount()];
				// can't autofit if no parameters or data length < parameter count
				if (params.length > 0 && params.length <= x.length && params.length <= y.length) {
					MinimizeUserFunction minFunc = new MinimizeUserFunction(f, x, y);
					prevParams = new double[params.length];
					for (int i = 0; i < params.length; i++) {
						params[i] = prevParams[i] = f.getParameterValue(i);
					}
					double tol = 1.0E-6;
					int iterations = 20;
					hessian.minimize(minFunc, params, iterations, tol);
					// get deviation after minimizing
					devSq = getDevSquared(fit, x, y);
					// restore parameters and try Levenberg-Marquardt if Hessian fit is worse
					if (devSq > prevDevSq) {
						for (int i = 0; i < prevParams.length; i++) {
							f.setParameterValue(i, prevParams[i]);
						}
						levmar.minimize(minFunc, params, iterations, tol);
						// get deviation after minimizing
						devSq = getDevSquared(fit, x, y);
					}
					// restore parameters and deviation if new fit is worse
					if (devSq > prevDevSq) {
						for (int i = 0; i < prevParams.length; i++) {
							f.setParameterValue(i, prevParams[i]);
						}
						devSq = prevDevSq;
					}
				}
			}
		}
		doLinearRegression(x, y, isLinearFit);
		if (devSq == 0) {
			devSq = getDevSquared(fit, x, y);
		}
		double rmsDev = Math.sqrt(devSq / x.length);
		return rmsDev;
	}

	/**
	 * Gets the total deviation squared between function and data
	 */
	protected double getDevSquared(Function f, double[] x, double[] y) {
		fitEvaluatedToNaN = false;
		double total = 0;
		for(int i = 0; i<x.length; i++) {
			double next = f.evaluate(x[i]);
			double dev = (next-y[i]);
			total += dev*dev;
		}
		return fitEvaluatedToNaN? Double.NaN: total;
	}

	/**
	 * Determines the Pearson correlation and linear fit parameter SEs.
	 *
	 * @param xd double[]
	 * @param yd double[]
	 * @param isLinearFit true if linear fit (sets uncertainties to slope and intercept SE)
	 */
	public void doLinearRegression(double[] xd, double[] yd, boolean isLinearFit) {
		int n = xd.length;

		// set Double.NaN defaults
		correlation = Double.NaN;
		for (int i=0; i< uncertainties.length; i++)
			uncertainties[i] = Double.NaN;

		// return if less than 3 data points
		if (n<3)  return;

		double mean_x = xd[0];
		double mean_y = yd[0];
		for(int i=1; i<n; i++){
			mean_x += xd[i];
			mean_y += yd[i];
		}
		mean_x /= n;
		mean_y /= n;

		double sum_sq_x = 0;
		double sum_sq_y = 0;
		double sum_coproduct = 0;
		for(int i=0; i<n; i++){
			double delta_x = xd[i]-mean_x;
			double delta_y = yd[i]-mean_y;
			sum_sq_x += delta_x*delta_x;
			sum_sq_y += delta_y*delta_y;
			sum_coproduct += delta_x*delta_y;
		}
		if (sum_sq_x==0 || sum_sq_y==0) {
			correlation = Double.NaN;
			for (int i=0; i< uncertainties.length; i++)
				uncertainties[i] = Double.NaN;
			return;
		}

		double pop_sd_x = sum_sq_x/n;
		double pop_sd_y = sum_sq_y/n;
		double cov_x_y = sum_coproduct/n;
		correlation = cov_x_y*cov_x_y/(pop_sd_x*pop_sd_y);

		if (isLinearFit) {
			double sumSqErr =  Math.max(0.0, sum_sq_y - sum_coproduct * sum_coproduct / sum_sq_x);
			double meanSqErr = sumSqErr/(n-2);
			uncertainties[0] = Math.sqrt(meanSqErr / sum_sq_x); // slope SE
			uncertainties[1] = Math.sqrt(meanSqErr * ((1.0/n) + (mean_x*mean_x) / sum_sq_x)); // intercept SE
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
 * Copyright (c) 2017-2018  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
