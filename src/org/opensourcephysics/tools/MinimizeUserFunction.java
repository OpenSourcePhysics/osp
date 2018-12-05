package org.opensourcephysics.tools;

import org.opensourcephysics.numerics.MultiVarFunction;

/**
 * A function whose value is the total deviation squared
 * between a user function and a set of data points.
 * This function is minimized by the HessianMinimize class.
 */
public class MinimizeUserFunction implements MultiVarFunction {
	UserFunction f;
	double[] x, y; // the data

	// Constructor
	MinimizeUserFunction(UserFunction f, double[] x, double[] y) {
		this.f = f;
		this.x = x;
		this.y = y;
	}

	// Evaluates this function
	public double evaluate(double[] params) {
		// set the parameter values of the user function
		for(int i = 0; i<params.length; i++) {
			f.setParameterValue(i, params[i]);
		}
		double sum = 0.0;
		for(int i = 0; i<x.length; i++) {
			// evaluate the user function and find deviation
			double dev = y[i]-f.evaluate(x[i]);
			// sum the squares of the deviations
			sum += dev*dev;
		}
		return sum;
	}
}
