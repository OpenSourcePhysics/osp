/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLLoader;
import org.opensourcephysics.numerics.MultiVarFunction;
import org.opensourcephysics.numerics.ParsedMultiVarFunction;
import org.opensourcephysics.numerics.ParserException;
import org.opensourcephysics.tools.FunctionEditor.FObject;

/**
 * A known function for which the expression and parameters are user-editable.
 *
 * @author Douglas Brown
 */
public class UserFunction implements FObject, KnownFunction, MultiVarFunction, Cloneable {
	// static constants
	protected final static String[] dummyVars = { "'", "@", //$NON-NLS-1$ //$NON-NLS-2$
			"`", "~", "#" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	// instance fields
	protected String name;
	protected String[] paramNames = new String[0];
	protected double[] paramValues = new double[0];
	protected String[] paramDescriptions = new String[0];
	protected String[] functionNames = new String[0];
	protected ParsedMultiVarFunction myFunction = null;
	protected String[] vars = { "x" }; //$NON-NLS-1$
	protected UserFunction[] references = new UserFunction[0];
	protected boolean nameEditable = true;
	protected String description;
	protected KnownPolynomial polynomial;
	
	private double myval = Double.NaN;
	private String dummyInputString = "0"; //$NON-NLS-1$
	private String clearExpr;
	private String clearInput;
	private String paddedExpr;
	private String paddedInput;

	/**
	 * Constructor.
	 *
	 * @param name the function name
	 */
	public UserFunction(String name) {
		setName(name);
		try {
			myFunction = new ParsedMultiVarFunction("0", new String[0], false); //$NON-NLS-1$
			functionNames = myFunction.getFunctionNames();
		} catch (ParserException ex) {
			/** empty block */
		}
	}

	/**
	 * Constructor that copies a KnownPolynomial.
	 *
	 * @param poly the KnownPolynomial
	 */
	public UserFunction(KnownPolynomial poly) {
		this(poly.getName());
		polynomial = poly;
		// set up name and description
		setName(poly.getName());
		setDescription(poly.getDescription());

		// set up parameters
		String[] params = new String[poly.getParameterCount()];
		double[] paramValues = new double[poly.getParameterCount()];
		String[] desc = new String[poly.getParameterCount()];
		for (int i = 0; i < params.length; i++) {
			params[i] = poly.getParameterName(i);
			paramValues[i] = poly.getParameterValue(i);
			desc[i] = poly.getParameterDescription(i);
		}
		setParameters(params, paramValues, desc);

		setExpression(poly.getExpression("x"), new String[] { "x" }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the name
	 */
	@Override
	public void setName(String name) {
		if (!isNameEditable()) {
			return;
		}
		this.name = name;
	}

	/**
	 * Returns true if the name is user-editable.
	 *
	 * @return true if editable
	 */
	public boolean isNameEditable() {
		return nameEditable;
	}

	/**
	 * Sets the name editable property.
	 *
	 * @param editable true if editable
	 */
	public void setNameEditable(boolean editable) {
		nameEditable = editable;
	}

	/**
	 * Gets the current independent variable.
	 *
	 * @return the variable name
	 */
	public String getIndependentVariable() {
		return vars[0];
	}

	/**
	 * Gets the current independent variables.
	 *
	 * @return the variable names
	 */
	public String[] getIndependentVariables() {
		return vars;
	}

	/**
	 * Gets the expression, removing all dummy variables
	 *
	 * @return the expression
	 */
	public String getInputString() {
		return clearInput;
	}


	/**
	 * Gets the expression using the current variables, without dummy variables
	 *
	 * @return the expression
	 */
	public String getExpression() {
		return (clearExpr == null ? generateExpressionForVars() : clearExpr);
	}
	
	/**
	 * Gets the expression and sets the independent variable.
	 *
	 * @param indepVarName the name of the independent variable
	 * @return the expression
	 */
	@Override
	public String getExpression(String indepVarName) {
		return getExpression(new String[] { indepVarName });
	}

	/**
	 * Gets the expression and sets the independent variables.
	 *
	 * @param varNames the name of the independent variables
	 * @return the expression
	 */
	public String getExpression(String[] varNames) {
		vars = varNames;
		return generateExpressionForVars();
	}

	/**
	 * Gets the full expression using the current variables, replacing all
	 * references with their full expressions in parentheses
	 *
	 * @param varNames the name of the independent variables
	 * @return the expression
	 */
	public String getFullExpression(String[] varNames) {
		getExpression(varNames);
		String s = paddedExpr;
		for (int i = 0, n = references.length; i < n; i++) {
			UserFunction f = references[i];
			s = replaceAllWords(s, f.getName(), "(" + f.getFullExpression(varNames) + ")"); //$NON-NLS-1$//$NON-NLS-2$
		}
		return s.replaceAll(" ", "");
	}

	/**
	 * Sets the expression.
	 *
	 * @param exp  a parsable expression of the parameters and variables
	 * @param vars the names of the independent variables
	 * @return true if successfully parsed
	 */
	public boolean setExpression(String exp, String[] vars) {

		paddedInput = exp;
		clearInput = exp = exp.replaceAll(" ", "");
		
		String[] names = setVariables(vars);

		// add padding around all names -- disallowing <number or .>E as in "5E0"
		exp = padNames(exp);
		// replace dependents
		boolean hasDummy = false;
		for (int i = 0; i < vars.length; i++) {
			hasDummy = (hasDummy || exp.indexOf(dummyVars[i]) >= 0);
			exp = exp.replaceAll(" " + vars[i] + " ", " " + dummyVars[i] + " ");
		}
		dummyInputString = exp; // for cloning only
		// try to parse expression
		try {
			myFunction = new ParsedMultiVarFunction(exp, names, false);
			// successful, so save expression unless it contains "="
			if (exp.indexOf("=") < 0) { //$NON-NLS-1$
				if (hasDummy) {
					generateExpressionForVars();
				} else {
					// being equal is the test for isValid()
					clearExpr = clearInput;
				}
				return true;
			}
		} catch (ParserException ex) {
			try {
				// Note that any constants or unidentified variables will cause this condition.
				myFunction = new ParsedMultiVarFunction("0", names, false); //$NON-NLS-1$
			} catch (ParserException ex2) {
				/** empty block */
			}
			clearExpr = "0"; //$NON-NLS-1$
		}
		return false;
	}

	private String[] setVariables(String[] vars) {
		this.vars = vars;
		String[] names = new String[vars.length + paramNames.length + references.length];
		for (int i = 0; i < vars.length; i++) {
			names[i] = dummyVars[i];
		}
		for (int i = 0; i < paramNames.length; i++) {
			names[i + vars.length] = paramNames[i];
		}
		for (int i = 0; i < references.length; i++) {
			names[i + vars.length + paramNames.length] = references[i].getName();
		}
		return names;
	}

	private String generateExpressionForVars() {
		String exp = dummyInputString;
		// clarify expression
		// maybe just from a clone?
		// user can use ` for y if they desire, I guess
		for (int i = 0; i < vars.length; i++) {
			exp = exp.replaceAll(dummyVars[i], vars[i]);
		}
		paddedExpr = exp;
		// being equal is the test for isValid()
		return clearInput = clearExpr = exp.replaceAll(" ", "");
	}

	/**
	 * Gets the parameter count.
	 * 
	 * @return the number of parameters
	 */
	@Override
	public int getParameterCount() {
		return paramNames.length;
	}

	/**
	 * Gets a parameter name.
	 *
	 * @param i the parameter index
	 * @return the name of the parameter
	 */
	@Override
	public String getParameterName(int i) {
		return paramNames[i];
	}

	/**
	 * Gets a parameter value.
	 *
	 * @param i the parameter index
	 * @return the value of the parameter
	 */
	@Override
	public double getParameterValue(int i) {
		return paramValues[i];
	}

	/**
	 * Sets a parameter value.
	 *
	 * @param i     the parameter index
	 * @param value the value
	 */
	@Override
	public void setParameterValue(int i, double value) {
		paramValues[i] = value;
	}

	/**
	 * Sets the parameters.
	 *
	 * @param names  the parameter names
	 * @param values the parameter values
	 */
	public void setParameters(String[] names, double[] values) {
		paramNames = names;
		paramValues = values;
	}

	/**
	 * Sets the parameters.
	 *
	 * @param names        the parameter names
	 * @param values       the parameter values
	 * @param descriptions the parameter descriptions
	 */
	@Override
	public void setParameters(String[] names, double[] values, String[] descriptions) {
		paramNames = names;
		paramValues = values;
		if (descriptions != null) {
			paramDescriptions = descriptions;
		}
	}

	/**
	 * Sets the parameters of reference functions to those of this function.
	 */
	public void updateReferenceParameters() {
		for (int i = 0, n = references.length; i < n; i++) {
			UserFunction next = references[i];
			next.setParameters(paramNames, paramValues, paramDescriptions);
			next.updateReferenceParameters();
		}
	}

	/**
	 * Sets the reference functions.
	 *
	 * @param functions the functions referenced by this one
	 */
	public void setReferences(UserFunction[] functions) {
		references = functions;
	}

	/**
	 * Gets the description of this function. May return null.
	 *
	 * @return the description
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of this function.
	 *
	 * @param desc the description
	 */
	@Override
	public void setDescription(String desc) {
		description = desc;
	}

	/**
	 * Gets a parameter description. May be null.
	 *
	 * @param i the parameter index
	 * @return the description of the parameter (may be null)
	 */
	@Override
	public String getParameterDescription(int i) {
		if (i >= paramDescriptions.length)
			return null;
		return paramDescriptions[i];
	}

	/**
	 * Returns function names. Added by D. Brown 10 Dec 2015
	 *
	 * @return array of parser function names
	 */
	public String[] getFunctionNames() {
		return functionNames;
	}

	/**
	 * Evaluates the function for a single variable x.
	 *
	 * @param x
	 * @return f(x)
	 */
	@Override
	public double evaluate(double x) {
		if (myFunction == null) {
			return Double.NaN;
		}
		ensureBufferLength(1);
		temp[0] = x;
		System.arraycopy(paramValues, 0, temp, 1, paramValues.length);
		for (int pt = 1 + paramValues.length, n = references.length, i = 0; i < n;) {
			temp[pt++] = references[i++].evaluate(x);
		}
		return myFunction.evaluate(temp);
	}

	/**
	 * results [---x---,---params---,---references---]
	 * 
	 */

	/**
	 * Evaluates the function for a variables array x.
	 *
	 * @param x
	 * @return f(x)
	 */
	@Override
	public double evaluate(double[] x) {
		// no longer called?
		// only called from DynamicParticle.getXYForces?
		if (myFunction == null) {
			return Double.NaN;
		}
		ensureBufferLength(x.length);
		System.arraycopy(x, 0, temp, 0, x.length);
		System.arraycopy(paramValues, 0, temp, x.length, paramValues.length);
		for (int pt = x.length + paramValues.length, n = references.length, i = 0; i < n;) {
			temp[pt++] = references[i++].evaluate(x);
		}
		return myFunction.evaluate(temp);
	}

	public double evaluateMyVal(double[] x) {
		// only called from DynamicParticle.getXYForces?
		if (myFunction == null) {
			return Double.NaN;
		}
		// BH 2020.06.24 allow for precalculated value. 
		if (Double.isNaN(myval)) {
//			OSPLog.debug("UserFunction.evaluate " + name + " = " + myFunction);
			ensureBufferLength(x.length);
			System.arraycopy(x, 0, temp, 0, x.length);
			System.arraycopy(paramValues, 0, temp, x.length, paramValues.length);
			for (int pt = x.length + paramValues.length, n = references.length, i = 0; i < n;) {
				temp[pt++] = references[i++].evaluateMyVal(x);
			}
			myval = myFunction.evaluate(temp);
		}
//		OSPLog.debug("UserFunction.evaluate " + name + " = " + myval);
		return myval;
	}

	public void clear() {
		myval  = Double.NaN;
		for (int i = references.length; --i >= 0;) {
			 references[i].clear();
		}

	}
	private double[] temp;

	private void ensureBufferLength(int xLen) {
		int n = xLen + paramValues.length + references.length;
		if (temp == null || temp.length < n)
			temp = new double[n];
	}

	/**
	 * Determines if last evaluation resulted in NaN.
	 *
	 * @return true if result was converted from NaN to zero
	 */
	public boolean evaluatedToNaN() {
		return myFunction == null ? false : myFunction.evaluatedToNaN();
	}

	/**
	 * Returns a clone of this UserFunction.
	 *
	 * @return the clone
	 */
	@Override
	public UserFunction clone() {
		UserFunction f = new UserFunction(name);
		f.setDescription(description);
		f.setNameEditable(nameEditable);
		f.setParameters(paramNames, paramValues, paramDescriptions);
		UserFunction[] refs = new UserFunction[references.length];
		for (int i = 0; i < refs.length; i++) {
			refs[i] = references[i].clone();
		}
		f.setReferences(refs);
		f.setExpression(dummyInputString, vars);
		f.polynomial = (polynomial == null ? null : polynomial.clone());
		return f;
	}

	/**
	 * Determines if another KnownFunction is the same as this one.
	 *
	 * @param f the KnownFunction to test
	 * @return true if equal
	 */
	@Override
	public boolean equals(Object f) {
		if (!(f instanceof UserFunction))
			return false;
		UserFunction uf = (UserFunction) f;
		if (!getName().equals(uf.getName()))
			return false;
		if (!getInputString().equals(uf.getInputString()))
			return false;
		int n = getParameterCount();
		if (n != uf.getParameterCount())
			return false;
		for (int i = 0; i < n; i++) {
			if (!getParameterName(i).equals(uf.getParameterName(i)))
				return false;
		}
		// Q: references?

		// ignore descriptions and parameter values
		return true;
	}

	/**
	 * Updates the associated polynomial, if any, with this functions current
	 * properties.
	 * 
	 * @return true if updated
	 */
	public boolean updatePolynomial() {
		if (polynomial == null)
			return false;
		// update name and description
		// see if function name is different than default polynomial name
		polynomial.setName(this.getName());
		polynomial.setDescription(this.getDescription());

		// update parameters
		polynomial.setParameters(paramNames, paramValues, paramDescriptions);
		return true;
	}

	/**
	 * Replaces a parameter name with a new one in the function expression.
	 *
	 * @param oldName the existing parameter name
	 * @param newName the new parameter name
	 * @return the modified expression, or null if failed
	 */
	protected String replaceParameterNameInExpression(String oldName, String newName) {
		String exp = replaceAllWords(paddedInput, oldName, newName);
		return (exp != null && setExpression(exp, getIndependentVariables()) ? exp : null);
	}

	static String padNames(String exp) {
		// but not 5.0E3
		return exp.replaceAll("([A-Za-z_]\\w*)", " $1 ").replaceAll("([0123456789\\.]) ([eE])", "$1$2");
	}

	/**
	 * Safe replaceAll for expressions.
	 * 
	 * Pad full words (starting with a letter or _ and continuing with letter,
	 * number, or _) with spaces, replace full words, remove all spaces.
	 * 
	 * @param paddedExp
	 * @param key
	 * @param rep
	 * @return
	 */
	private static String replaceAllWords(String paddedExp, String key, String rep) {
		// replace full words
		return paddedExp.replaceAll(" " + key + " ", " " + rep + " ");
	}

	public static boolean containsWord(String paddedExp, String key) {
		return (paddedExp.indexOf(" " + key + " ") >= 0);
	}

	/**
	 * Returns the XML.ObjectLoader for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load UserFunction data in an XMLControl.
	 */
	protected static class Loader extends XMLLoader {
		@Override
		public void saveObject(XMLControl control, Object obj) {
			UserFunction f = (UserFunction) obj;
			control.setValue("name", f.getName()); //$NON-NLS-1$
			control.setValue("description", f.getDescription()); //$NON-NLS-1$
			control.setValue("name_editable", f.isNameEditable()); //$NON-NLS-1$
			control.setValue("parameter_names", f.paramNames); //$NON-NLS-1$
			control.setValue("parameter_values", f.paramValues); //$NON-NLS-1$
			control.setValue("parameter_descriptions", f.paramDescriptions); //$NON-NLS-1$
			control.setValue("variables", f.getIndependentVariables()); //$NON-NLS-1$
			control.setValue("expression", f.getInputString()); //$NON-NLS-1$
			if (f.polynomial != null) {
				control.setValue("polynomial", f.polynomial.getCoefficients()); //$NON-NLS-1$
			}
		}

		@Override
		public Object createObject(XMLControl control) {
			String name = control.getString("name"); //$NON-NLS-1$
			return new UserFunction(name);
		}

		@Override
		public Object loadObject(XMLControl control, Object obj) {
			UserFunction f = (UserFunction) obj;
			f.setName(control.getString("name")); //$NON-NLS-1$
			f.setDescription(control.getString("description")); //$NON-NLS-1$
			if (control.getPropertyNamesRaw().contains("name_editable")) { //$NON-NLS-1$
				f.setNameEditable(control.getBoolean("name_editable")); //$NON-NLS-1$
			}
			String[] names = (String[]) control.getObject("parameter_names"); //$NON-NLS-1$
			if (names != null) {
				double[] values = (double[]) control.getObject("parameter_values"); //$NON-NLS-1$
				String[] desc = (String[]) control.getObject("parameter_descriptions"); //$NON-NLS-1$
				f.setParameters(names, values, desc);
			}
			String[] vars = (String[]) control.getObject("variables"); //$NON-NLS-1$
			if (vars == null) { // for legacy code
				String var = control.getString("variable"); //$NON-NLS-1$
				vars = new String[] { var };
			}
			f.setExpression(control.getString("expression"), vars); //$NON-NLS-1$
			double[] coeff = (double[]) control.getObject("polynomial"); //$NON-NLS-1$
			if (coeff != null) {
				f.polynomial = new KnownPolynomial(coeff);
			}
			return obj;
		}

	}

	@Override
	public String toString() {
		return "[UserFunction " + name + " = " + myFunction.toString() + "]";
	}

	public boolean isValid() {
		// BH Q: should isValid be any expression that has no dummy variables?
		return clearExpr == clearInput;
	}

	@Override
	public UserFunction newUserFunction(String var) {
		return clone();
	}
}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 * 
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
