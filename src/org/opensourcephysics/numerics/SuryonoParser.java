/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.numerics;

import java.util.ArrayList;
import java.util.Arrays;
/*----------------------------------------------------------------------------------------*
 * Parser.java version 1.0                                                    Jun 16 1996 *
 * Parser.java version 2.0                                                    Aug 25 1996 *
 * Parser.java version 2.1                                                    Oct 14 1996 *
 * Parser.java version 2.11                                                   Oct 25 1996 *
 * Parser.java version 2.2                                                    Nov  8 1996 *
 * Parser.java version 3.0                                                    May 17 1997 *
 * Parser.java version 3.01                                                   Oct 18 2001 *
 *                                                                                        *
 * Parser.java version 4.0                                                    Oct 25 2001 *
 *                                                                                        *
 * Copyright (c) 1996 Yanto Suryono. All Rights Reserved.                                 *
 * Version 4 Modifications by Wolfgang Christian                                          *
 *                                                                                        *
 * This program is free software; you can redistribute it and/or modify it                *
 * under the terms of the GNU General Public License as published by the                  *
 * Free Software Foundation; either version 2 of the License, or (at your option)         *
 * any later version.                                                                     *
 *                                                                                        *
 * This program is distributed in the hope that it will be useful, but                    *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or          *
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for               *
 * more details.                                                                          *
 *                                                                                        *
 * You should have received a copy of the GNU General Public License along                *
 * with this program; if not, write to the Free Software Foundation, Inc.,                *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA                                  *
 *                                                                                        *
 *----------------------------------------------------------------------------------------*/
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * The class <code>Parser</code> is a mathematical expression parser.
 * <p>
 * Example of code that uses this class:
 * <p>
 *
 * <pre>
 * Parser parser = new Parser(1); // creates parser with one variable
 * parser.defineVariable(1, "x"); // lets the variable be 'x'
 * parser.define("sin(x)/x"); // defines function: sin(x)/x
 * parser.parse(); // parses the function
 *
 * // calculates: sin(x)/x with x = -5.0 .. +5.0 in 20 steps
 * // and prints the result to standard output.
 *
 * float result;
 * for (i = -10; i <= 10; i++) {
 * 	parser.setVariable(1, (float) i / 2.0f);
 * 	result = parser.evaluate();
 * 	System.out.println(result);
 * }
 * </pre>
 */
public final class SuryonoParser extends MathExpParser {

	
	/**
	 * The compiled function, for evaluation.
	 * 
	 */
	private class Func {
		
		@Override
		public String toString() {
			if (var_count < 0) {
				return super.toString();
			}
			String s = "" + function + ":\n";
			for (int i = 0; i < var_count; i++) {
				s += var_name[i] + "=" + var_value[i] + ";";
			}
			return s;
		}
		
		protected int var_count = -1; // number of variables
		protected String var_name[]; // variables' name
		protected double var_value[]; // value of variables
		protected double number[] = new double[MAX_NUM]; // numeric constants in defined function
		protected Map<String, int[]> references = new Hashtable<>();
		protected List<String> refnames = new ArrayList<String>();
		protected int[] postfix_code = new int[100]; // the postfix code //$NON-NLS-1$
		protected boolean radian = true; // radian unit flag

		protected int err = NO_ERROR;
		/**
		 * set when evaluate() method converts NaN to zero
		 * 
		 * --added by D Brown 15 Sep 2010
		 * 
		 */
		protected boolean isNaN;
		

		// temporary variables
		private double[] refvalue = null; // temporary values of references
		private double[] stack = new double[STACK_SIZE]; 
		private int numberindex; // pointer to numbers/constants bank

		protected Func(int nVar) {
			reset(nVar);
		}

		protected void reset(int nVar) {
			if (nVar != var_count) {
				var_count = nVar;
				var_name = new String[nVar];
				var_value = new double[nVar];
				references.clear();
				refnames.clear();
			}
		}

		public void defineVariable(int index, String name) {
			if (index > var_count) {
				return;
			}
			var_name[index - 1] = name;
		}

		public void setVariable(int index, double value) {
			if (index > var_count) {
				return;
			}
			var_value[index - 1] = value;
		}

		public void setVariable(String name, double value) {
			for (int i = 0; i < var_count; i++) {
				if (var_name[i].equals(name)) {
					var_value[i] = value;
					break;
				}
			}
		}

		protected double evaluate(double x, double y, double z, int n) {
			if (var_count != n) {
				return 0;
			}
			switch (n) {
			case 3:
				var_value[2] = z;
			case 2:
				var_value[1] = y;
			case 1:
				var_value[0] = x;
			}
			return evaluate();
		}

		protected double evaluate(double[] v) {
			// added by Wolfgang Christian to make it easier to call parser with an array.
			if (var_value.length != v.length) {
				System.out.println("SuryonoParser.Func Error: incorrect number of variables."); //$NON-NLS-1$
				return 0;
			}
			System.arraycopy(v, 0, var_value, 0, v.length);
			return evaluate();
		}

		protected double evaluate() {
			double result = 0;
			err = NO_ERROR;
			numberindex = 0;
			int size = refnames.size();
			if (size == 0) {
				if (refvalue == null || refvalue.length < size)
					refvalue = new double[size];
				for (int i = 0; i < size; i++) {
					result = refvalue[i] = evaluateSubFunction(references.get(refnames.get(i)), stack);
					if (Double.isNaN(result)) {
						break;
					}
				}
			}
			if (!Double.isNaN(result))
				result = evaluateSubFunction(postfix_code, stack);
			// added by D Brown to flag NaN results
			// BH note! isNaN was not being set if there was an issue in reference functions
			isNaN = Double.isNaN(result);
			// added by W. Christian to trap for NaN
			if (isNaN) {
				result = 0.0;
			}			
			setError(err);
			return result;
		}

		/**
		 * Evaluates subfunction.
		 *
		 * @return the result of the subfunction
		 */
		private double evaluateSubFunction(int[] codes, double[] stack) {
			int spt = -1;
			int cpt = 0;
			int destination;
			int code;
			int codeLength = codes[0]; // added bt W. Christian to check the length.
			//OSPLog.debug("Suryono " + toString());
			while (true) {
				try {
					if (cpt == codeLength) {
						//OSPLog.debug("Suryono result " + stack[0] + " for " + function);
						return stack[0]; // added by W. Christian. Do not use doing an Exception!
					}
					code = codes[++cpt];
				} catch (ArrayIndexOutOfBoundsException e) {
					return stack[0];
				}
				//OSPLog.debug("Suryono cpt=" + cpt + " code=" + code + " " + Arrays.toString(codes));
				try {
					switch (code) {
					case ADD:
						stack[--spt] += stack[spt + 1];
						break;
					case SUB:
						stack[--spt] -= stack[spt + 1];
						break;
					case MUL:
						stack[--spt] *= stack[spt + 1];
						break;
					case DIV:
						if (stack[spt] == 0) {
							stack[--spt] /= 1.0e-128; // added by W.Christian to trap for divide by zero.
						} else {
							stack[--spt] /= stack[spt + 1];
						}
						break;
					case POWER:
						stack[--spt] = Math.pow(stack[spt], stack[spt + 1]);
						break;
					case NEGATE:
						stack[spt] = -stack[spt];
						break;
					case LESS_THAN:
						stack[--spt] = (stack[spt] < stack[spt + 1]) ? 1.0 : 0.0;
						break;
					case GREATER_THAN:
						stack[--spt] = (stack[spt] > stack[spt + 1]) ? 1.0 : 0.0;
						break;
					case LESS_EQUAL:
						stack[--spt] = (stack[spt] <= stack[spt + 1]) ? 1.0 : 0.0;
						break;
					case GREATER_EQUAL:
						stack[--spt] = (stack[spt] >= stack[spt + 1]) ? 1.0 : 0.0;
						break;
					case EQUAL:
						stack[--spt] = (stack[spt] == stack[spt + 1]) ? 1.0 : 0.0;
						break;
					case NOT_EQUAL:
						stack[--spt] = (stack[spt] != stack[spt + 1]) ? 1.0 : 0.0;
						break;
					case IF_CODE:
						if (stack[spt--] != 0) {
							cpt++;
							break;
						}
						// fall through
					case JUMP_CODE:
						// BH note that "destination" here is 
						// the code point PRIOR to the 
						// code to jump to. The destination will
						// be the end of the IF clause, for example. 
						destination = cpt + codes[++cpt];
						while (cpt < destination) {
							if (codes[++cpt] == NUMERIC) {
								numberindex++;
							}
						}
						// ready for next code = codes[++cpt];
						break;
					case ENDIF:
						break; // same as NOP
					case AND_CODE:
						stack[--spt] = (stack[spt] != 0 && stack[spt + 1] != 0 ? 1 : 0);
						break;
					case OR_CODE:
						stack[--spt] = (stack[spt] != 0 || stack[spt + 1] != 0 ? 1 : 0);
						break;
					case NOT_CODE:
						stack[spt] = (stack[spt] == 0 ? 1 : 0);
						break;
					case NUMERIC:
						stack[++spt] = number[numberindex++];
						break;
					case PI_CODE:
						stack[++spt] = Math.PI;
						break;
					case E_CODE:
						stack[++spt] = Math.E;
						break;
					default:
						int val = code & ~OFFSET_MASK;
						switch (code & OFFSET_MASK) {
						case REF_OFFSET:
							stack[++spt] = refvalue[val];
							break;
						case VAR_OFFSET:
							stack[++spt] = var_value[val];
							break;
						case FUNC_OFFSET:
							stack[spt] = builtInFunction(val, stack[spt]);
							break;
						case EXT_FUNC_OFFSET:
							stack[--spt] = builtInExtFunction(val, stack[spt],
									stack[spt + 1]);
							break;
						default:
							err = CODE_DAMAGED;
							return Double.NaN;
						}
					}
				} catch (ArrayIndexOutOfBoundsException oe) {
					err = STACK_OVERFLOW;
					return Double.NaN;
				} catch (NullPointerException ne) {
					err = CODE_DAMAGED;
					return Double.NaN;
				}
				
//				OSPLog.debug("Suryono code=" + code + " spt=" + spt + " " + Arrays.toString(stack));

			}
		}

		/**
		 * Built-in one parameter function call.
		 *
		 * @param index  the function index
		 * @param p the parameter to the function
		 * @return the function result
		 */
		private double builtInFunction(int index, double p) {
			switch (index) {
			case 0:
				return Math.sin(radian ? p : p * DEGTORAD);
			case 1:
				return Math.cos(radian ? p : p * DEGTORAD);
			case 2:
				return Math.tan(radian ? p : p * DEGTORAD);
			case 3:
				return Math.log(p);
			case 4:
				return Math.log(p) / LOG10;
			case 5:
				return Math.abs(p);
			case 6:
				return Math.rint(p);
			case 7:
				return p - Math.rint(p);
			case 8:
				return Math.asin(p) / (radian ? 1 : DEGTORAD);
			case 9:
				return Math.acos(p) / (radian ? 1 : DEGTORAD);
			case 10:
				return Math.atan(p) / (radian ? 1 : DEGTORAD);
			case 11:
				return Math.sinh(p);// (Math.exp(parameter) - Math.exp(-parameter)) / 2;
			case 12:
				return Math.cosh(p);//(Math.exp(parameter) + Math.exp(-parameter)) / 2;
			case 13:
				return Math.tanh(p); //double a = Math.exp(parameter); double b = Math.exp(-parameter);	return (a - b) / (a + b);
			case 14: // asinh
				return Math.log(p + Math.sqrt(p * p + 1));
			case 15: // acosh
				return Math.log(p + Math.sqrt(p * p - 1));
			case 16: // atanh
				return Math.log((1 + p) / (1 - p)) / 2;
			case 17:
				return Math.ceil(p);
			case 18:
				return Math.floor(p);
			case 19:
				return Math.round(p);
			case 20:
				return Math.exp(p);
			case 21:
				return p * p;
			case 22:
				return Math.sqrt(p);
			case 23:
				return Math.signum(p); // {-1, 0, 1}
			case 24:
				return (p < 0 ? 0 : 1); // {0, 0, 1} added by W. Christian for step function
			case 25:
				return p * Math.random(); // added by W. Christian for random function
			default:
				err = CODE_DAMAGED;
				return Double.NaN;
			}
		}

		/**
		 * Built-in two parameters extended function call.
		 *
		 * @param index the function index
		 * @param p1   the first parameter to the function
		 * @param p2   the second parameter to the function
		 * @return the function result
		 */
		private double builtInExtFunction(int index, double p1, double p2) {
			switch (index) {
			case 0:
				return Math.min(p1, p2);
			case 1:
				return Math.max(p1, p2);
			case 2:
				return Math.IEEEremainder(p1, p2);
			case 3:
				return Math.atan2(p1, p2);
			default:
				err = CODE_DAMAGED;
				return Double.NaN;
			}
		}

	}

	private Func f;
	
	/////// rest of this is just the parser itself
	
	private String function = ""; // function definition //$NON-NLS-1$
	private boolean valid = false; // postfix code status
	private int error; // error code of last process

	// variables used during parsing
	private boolean isBoolean = false; // boolean flag
	private boolean inRelation = false; // relation flag
	private int position; // parsing pointer
	private int start; // starting position of identifier
	private int num; // number of numeric constants
	private char ch; // current character

	// private static final int MAX_NUM = 100; // max numeric constants // changed
	// by W. Christian
	private static final int MAX_NUM = 200; // max numeric constants
	// private static final int NO_FUNCS = 24; // no. of built-in functions
	// changed from 24 function by W. Christian to add step and random function
	private static final int NO_FUNCS = 26; // no. of built-in functions
	private static final int NO_EXT_FUNCS = 4; // no. of extended functions
	private static final int STACK_SIZE = 50; // evaluation stack size
	
	// constants
	private static final double DEGTORAD = Math.PI / 180;
	private static final double LOG10 = Math.log(10);
	/**
	 * Parentheses expected.
	 */
	public static final int PAREN_EXPECTED = 2;

	/**
	 * Attempt to evaluate an uncompiled function.
	 */
	public static final int UNCOMPILED_FUNCTION = 3;

	/**
	 * Expression expected.
	 */
	public static final int EXPRESSION_EXPECTED = 4;

	/**
	 * Unknown identifier.
	 */
	public static final int UNKNOWN_IDENTIFIER = 5;

	/**
	 * Operator expected.
	 */
	public static final int OPERATOR_EXPECTED = 6;

	/**
	 * Parenthesis mismatch.
	 */
	public static final int PAREN_NOT_MATCH = 7;

	/**
	 * Code damaged.
	 */
	public static final int CODE_DAMAGED = 8;

	/**
	 * Stack overflow.
	 */
	public static final int STACK_OVERFLOW = 9;

	/**
	 * Too many constants.
	 */
	public static final int TOO_MANY_CONSTS = 10;

	/**
	 * Comma expected.
	 */
	public static final int COMMA_EXPECTED = 11;

	/**
	 * Invalid operand.
	 */
	public static final int INVALID_OPERAND = 12;

	/**
	 * Invalid operator.
	 */
	public static final int INVALID_OPERATOR = 13;

	/**
	 * No function definition to parse.
	 */
	public static final int NO_FUNC_DEFINITION = 14;

	/**
	 * Referenced name could not be found.
	 */
	public static final int REF_NAME_EXPECTED = 15;
	// postfix codes

	private static final int OFFSET_MASK     = 0xF000;
	private static final int FUNC_OFFSET     = 0x1000;
	private static final int EXT_FUNC_OFFSET = 0x2000;// FUNC_OFFSET + NO_FUNCS;
	private static final int VAR_OFFSET      = 0x4000;
	private static final int REF_OFFSET		 = 0x8000;
	private static final int PI_CODE = 253;
	private static final int E_CODE = 254;
	private static final int NUMERIC = 255;
	// Jump, followed by n : Displacement
	private static final int JUMP_CODE = 1;
	// Relation less than (<)
	private static final int LESS_THAN = 2;
	// Relation greater than (>)
	private static final int GREATER_THAN = 3;
	// Relation less than or equal (<=)
	private static final int LESS_EQUAL = 4;
	// Relation greater than or equal (>=)
	private static final int GREATER_EQUAL = 5;
	// Relation not equal (<>)
	private static final int NOT_EQUAL = 6;
	// Relation equal (=)
	private static final int EQUAL = 7;
	/**
	 * Conditional statement IF, followed by a conditional block : <code>
	 Displacement (Used to jump to condition FALSE code)
	 Condition TRUE code
	 Jump to next code outside conditional block
	 Condition FALSE code
	 ENDIF
	 * </code>
	 */
	private static final int IF_CODE = 8;
	private static final int ENDIF = 9;
	private static final int AND_CODE = 10;
	private static final int OR_CODE = 11;
	private static final int NOT_CODE = 12;
	private static final int ADD = '+';
	private static final int SUB = '-';
	private static final int MUL = '*';
	private static final int DIV = '/';
	private static final int NEGATE = '_';
	private static final int POWER = '^';
	// built in functions
	private final static String funcname[] = { 
			"sin", "cos", "tan", "ln", "log",          //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 
			"abs", "int", "frac", "asin", "acos",      //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 
			"atan",  "sinh", "cosh", "tanh", "asinh",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 
			"acosh", "atanh", "ceil", "floor", "round",//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$  
			"exp", "sqr", "sqrt", "sign", "step",      //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 
			"random"                                   //$NON-NLS-1$
	};
	// extended functions
	private final static String extfunc[] = { "min", "max", "mod", "atan2" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private boolean allowUnknown; // always false

	private static String[] allFunctions;
	
	/**
	 * The constructor of <code>Parser</code>.
	 *
	 * Added by W. Christian to make it easy to construct a parser for with one
	 * variable.
	 *
	 * @param f function
	 * @param v variable
	 *
	 * @throws ParserException
	 */
	public SuryonoParser(String f, String v) throws ParserException {
		this(1);
		defineVariable(1, v); // lets the variable be v
		define(f); // defines function: f
		parse(); // parses the function
		if (getErrorCode() != NO_ERROR) {
			String msg = "Error in function string: " + f; //$NON-NLS-1$
			msg = msg + '\n' + "Error: " + getErrorString(); //$NON-NLS-1$
			msg = msg + '\n' + "Position: " + getErrorPosition(); //$NON-NLS-1$
			throw new ParserException(msg);
		}
	}

	public void setError(int err) {
		error = err;
	}

	/**
	 * The constructor of <code>Parser</code>.
	 *
	 * Added by W. Christian to make it easy to construct a parser for with two
	 * variables.
	 *
	 * @param f  the function
	 * @param v1 variable 1
	 * @param v2 variable 2
	 * @throws ParserException
	 */
	public SuryonoParser(String f, String v1, String v2) throws ParserException {
		this(2);
		defineVariable(1, v1);
		defineVariable(2, v2);
		define(f); // defines function: f
		parse(); // parses the function
		if (getErrorCode() != NO_ERROR) {
			String msg = "Error in function string: " + f; //$NON-NLS-1$
			msg = msg + '\n' + "Error: " + getErrorString(); //$NON-NLS-1$
			msg = msg + '\n' + "Position: " + getErrorPosition(); //$NON-NLS-1$
			throw new ParserException(msg);
		}
	}

	/**
	 * The constructor of <code>Parser</code>.
	 *
	 * Added by W. Christian to make it easy to construct a parser for with multiple
	 * variables.
	 *
	 * @param f the function
	 * @param v variables
	 * @throws ParserException
	 */public SuryonoParser(String f, String[] v) throws ParserException {
			this(f, v, false);
		}
		
	 /**
	  * 
	  * @param funcStr
	  * @param vars
	  * @param allowUnkownIdentifiers always false
	  * @throws ParserException
	  */
	 public SuryonoParser(String funcStr, String[] vars, boolean allowUnkownIdentifiers) throws ParserException {
		this(vars.length);
		for (int i = 0; i < vars.length; i++) {
			defineVariable(i + 1, vars[i]);
		}
		allowUnknown = allowUnkownIdentifiers; // always false
		define(funcStr);
		parse();
		if (getErrorCode() != NO_ERROR) {
			String msg = "Error in function string: " + funcStr; //$NON-NLS-1$
			msg = msg + '\n' + "Error: " + getErrorString(); //$NON-NLS-1$
			msg = msg + '\n' + "Position: " + getErrorPosition(); //$NON-NLS-1$
			throw new ParserException(msg);
		}
	}

	/**
	 * The constructor of <code>Parser</code>.
	 *
	 * @param nVar the number of variables
	 */
	public SuryonoParser(int nVar) {
		f = new Func(nVar);
	}

	/**
	 * Sets the funtion to zero.
	 */
	public void setToZero() {
		try {
			setFunction("0"); //$NON-NLS-1$
		} catch (ParserException ex) {
		}
	}

	boolean appendVariables = false;

	/**
	 * Sets the angle unit to radian. Default upon construction.
	 */
	public void useRadian() {
		f.radian = true;
	}

	/**
	 * Sets the angle unit to degree.
	 */
	public void useDegree() {
		f.radian = false;
	}

	/**
	 * Remove any escape sequences such as \, and replace with the character. by W.
	 * Christian.
	 *
	 * @param myFunction the function
	 *
	 * @return the new function
	 */
	private String removeEscapeCharacter(String str) {
		if ((str == null) || (str.length() < 1)) {
			return str;
		}
		StringBuffer sb = new StringBuffer(str.length());
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) != '\\') {
				sb.append(str.charAt(i));
			}
		}
		return sb.toString();
	}

	/**
	 * Sets the variable names. Nothing happens if variable index > number of
	 * variables.
	 *
	 * @param index the variable index (one based)
	 * @param name  the variable name
	 */
	public void defineVariable(int index, String name) {
		f.defineVariable(index, name);
	}

	/**
	 * Sets the variable value. The variable is accessed by index. Nothing happens
	 * if variable index > number of variables.
	 *
	 * @param index the variable index (one based)
	 * @param value the variable value
	 */
	public void setVariable(int index, double value) {
		f.setVariable(index, value);
	}

	/**
	 * Sets the variable value. The variable is accessed by name. Nothing happens if
	 * variable could not be found.
	 *
	 * @param name  the variable name
	 * @param value the variable value
	 */
	public void setVariable(String name, double value) {
		f.setVariable(name, value);
	}

	/**
	 * Defines a function. Current postfix code becomes invalid.
	 *
	 * @param definition the function definition
	 */
	public void define(String definition) {
		function = definition;
		function.toLowerCase();
		function = removeEscapeCharacter(function); // added by W. Christian
		valid = false;
	}

	/**
	 * Parses defined function.
	 */
	public void parse(String function) throws ParserException {
		define(function);
		parse();
		if (getErrorCode() != NO_ERROR) {
			String msg = "Error in function string: " + function; //$NON-NLS-1$
			msg = msg + '\n' + "Error: " + getErrorString(); //$NON-NLS-1$
			msg = msg + '\n' + "Position: " + getErrorPosition(); //$NON-NLS-1$
			throw new ParserException(msg);
		}
	}

	/**
	 * Parses a function looking for unknown variables. Unknown tokens are used to
	 * create the variable list in the order that they are found.
	 */
	public String[] parseUnknown(String function) throws ParserException {
		f.var_name = new String[0];
		f.var_value = new double[0];
		f.var_count = 0;
		appendVariables = true;
		define(function);
		parse();
		if (getErrorCode() != NO_ERROR) {
			String msg = "Error in function string: " + function; //$NON-NLS-1$
			msg = msg + '\n' + "Error: " + getErrorString(); //$NON-NLS-1$
			msg = msg + '\n' + "Position: " + getErrorPosition(); //$NON-NLS-1$
			appendVariables = false;
			throw new ParserException(msg);
		}
		appendVariables = false;
		return f.var_name;
	}

	public String[] getVariableNames() {
		return f.var_name;
	}

	/**
	 * Returns all built-in and extended function names. Added by D. Brown 06 Jul
	 * 2008
	 *
	 * @return array of function names
	 */
	@Override
	public String[] getFunctionNames() {
		if (allFunctions != null)
			return allFunctions;
		int len = funcname.length;
		String[] names = new String[len + extfunc.length];
		System.arraycopy(funcname, 0, names, 0, len);
		System.arraycopy(extfunc, 0, names, len, extfunc.length);
		return allFunctions = names;
	}

	/**
	 * Parses defined function.
	 */
	public void parse() {
		if (valid) {
			return;
		}
		num = 0;
		error = NO_ERROR;
		f.references.clear();
		f.refnames.clear();
		switch (function) {
		case "":
			error = EXPRESSION_EXPECTED;
			valid = false;
			return;
		case "0":
		case "0.0":
			addNum(0);
			valid = true;
			return;
		case "1":
		case "1.0":
			addNum(1);
			valid = true;
			return;
		}
		String allFunction = function;
		String orgFunction = function;
		int index;
		while ((index = allFunction.lastIndexOf(";")) >= 0) { //$NON-NLS-1$
			function = allFunction.substring(++index) + ')';
			allFunction = allFunction.substring(0, index);
			// references are of form: refname1:reffunc1;refname2:reffunc2;...
			String refname = null;
			int separator = function.indexOf(":"); //$NON-NLS-1$
			if (separator == -1) {
				error = NO_FUNC_DEFINITION;
				for (position = 0; position < function.length(); position++) {
					if (function.charAt(position) != ' ') {
						break;
					}
				}
				position++;
			} else {
				refname = function.substring(0, separator);
				function = function.substring(separator + 1);
				refname = refname.trim();
				if (refname.equals("")) { //$NON-NLS-1$
					error = REF_NAME_EXPECTED;
					position = 1;
				} else {
					index += ++separator;
					parseSubFunction();
				}
			}
			if (error != NO_ERROR) {
				position += index;
				break;
			}
			f.references.put(refname, f.postfix_code);
			f.refnames.add(refname);
		}
		if (error == NO_ERROR) {
			function = allFunction + ')';
			parseSubFunction();
		}
		function = orgFunction;
		valid = (error == NO_ERROR);
	}

	@Override
	public double evaluate(double x)
	// added by Wolfgang Christian to make it easier to call parser.
	{
		return evaluate(x, 0, 0, 1);
	}

	public double evaluate(double x, double y)
	// added by Wolfgang Christian to make it easier to call parser.
	{
		return evaluate(x, y, 0, 2);
	}

	public double evaluate(double x, double y, double z)
	// added by Wolfgang Christian to make it easier to call parser.
	{
		return evaluate(x, y, z, 3);
	}

	private double evaluate(double x, double y, double z, int n) {
		return (checkEval() ? f.evaluate(x, y, z, n) : 0);
	}

	@Override
	public double evaluate(double[] v) {
		return (checkEval() ? f.evaluate(v) : 0);
	}

	/**
	 * Evaluates compiled function.
	 *
	 * @return the result of the functio
	 */
	public double evaluate() {
		return (checkEval() ? f.evaluate() : 0);
	}

	private boolean checkEval() {
		if (valid)
			return true;
		error = UNCOMPILED_FUNCTION;
		return false;
	}

	/**
	 * Determines if last evaluation resulted in NaN. Added by D Brown 15 Sep 2010.
	 *
	 * @return true if result was converted from NaN to zero
	 */
	public boolean evaluatedToNaN() {
		return f.isNaN;
	}

	/**
	 * Gets error code of last operation.
	 *
	 * @return the error code
	 */
	public int getErrorCode() {
		return error;
	}

	/**
	 * Gets error string/message of last operation.
	 *
	 * @return the error string
	 */
	public String getErrorString() {
		return toErrorString(error);
	}

	/**
	 * Gets error position. Valid only if error code != NO_ERROR
	 *
	 * @return error position (one based)
	 */
	public int getErrorPosition() {
		return position;
	}

	/**
	 * Converts error code to error string.
	 *
	 * @return the error string
	 */
	public static String toErrorString(int errorcode) {
		String s = ""; //$NON-NLS-1$
		switch (errorcode) {
		case NO_ERROR:
			s = "no error"; //$NON-NLS-1$
			break;
		case SYNTAX_ERROR:
			s = "syntax error"; //$NON-NLS-1$
			break;
		case PAREN_EXPECTED:
			s = "parenthesis expected"; //$NON-NLS-1$
			break;
		case UNCOMPILED_FUNCTION:
			s = "uncompiled function"; //$NON-NLS-1$
			break;
		case EXPRESSION_EXPECTED:
			s = "expression expected"; //$NON-NLS-1$
			break;
		case UNKNOWN_IDENTIFIER:
			s = "unknown identifier"; //$NON-NLS-1$
			break;
		case OPERATOR_EXPECTED:
			s = "operator expected"; //$NON-NLS-1$
			break;
		case PAREN_NOT_MATCH:
			s = "parentheses not match"; //$NON-NLS-1$
			break;
		case CODE_DAMAGED:
			s = "internal code damaged"; //$NON-NLS-1$
			break;
		case STACK_OVERFLOW:
			s = "execution stack overflow"; //$NON-NLS-1$
			break;
		case TOO_MANY_CONSTS:
			s = "too many constants"; //$NON-NLS-1$
			break;
		case COMMA_EXPECTED:
			s = "comma expected"; //$NON-NLS-1$
			break;
		case INVALID_OPERAND:
			s = "invalid operand type"; //$NON-NLS-1$
			break;
		case INVALID_OPERATOR:
			s = "invalid operator"; //$NON-NLS-1$
			break;
		case NO_FUNC_DEFINITION:
			s = "bad reference definition (: expected)"; //$NON-NLS-1$
			break;
		case REF_NAME_EXPECTED:
			s = "reference name expected"; //$NON-NLS-1$
			break;
		}
		return s;
	}

	/**
	 * Gets function string of last parsing operation.
	 *
	 * Added by W. Christian to implement the MathExpParser interface.
	 *
	 * @return the function string
	 */
	@Override
	public String getFunction() {
		return function;
	}

	/**
	 * Parse the function string using the existing variables.
	 *
	 * Added by W. Christian to implement the MathExpParser interface.
	 */
	@Override
	public void setFunction(String funcStr) throws ParserException {
		setFunction(funcStr, null);
	}

	/**
	 * Parse the function string using new variable names.
	 *
	 * Added by W. Christian to implement the MathExpParser interface.
	 */
	@Override
	public void setFunction(String funcStr, String[] vars) throws ParserException {
		if (vars != null) {
			int n = vars.length;
			f.reset(n);
			for (int i = 0; i < n; i++) {
				defineVariable(i + 1, vars[i]);
			}
		}
		define(function = funcStr);
		parse();
		if (error != NO_ERROR) {
			String msg = "Error in function string: " + funcStr; //$NON-NLS-1$
			msg = msg + '\n' + "Error: " + toErrorString(error); //$NON-NLS-1$
			msg = msg + '\n' + "Position: " + getErrorPosition(); //$NON-NLS-1$
			throw new ParserException(msg);
		}
	}

	/*----------------------------------------------------------------------------------------*
	 *                            Private methods begin here                                  *
	 *----------------------------------------------------------------------------------------*/

	/**
	 * Advances parsing pointer, skips pass all white spaces.
	 *
	 * @exception ParserException
	 */
	private void skipSpaces() throws ParserException {
		try {
			while (function.charAt(position - 1) == ' ') {
				position++;
			}
			ch = function.charAt(position - 1);
		} catch (StringIndexOutOfBoundsException e) {
			throw new ParserException(PAREN_NOT_MATCH);
		}
	}

	/**
	 * Advances parsing pointer, gets next character.
	 *
	 * @exception ParserException
	 */
	private void getNextch() throws ParserException {
		try {
			ch = function.charAt(position++);
		} catch (StringIndexOutOfBoundsException e) {
			throw new ParserException(PAREN_NOT_MATCH);
		}
	}

	/**
	 * Appends postfix code to compiled code.
	 *
	 * @param code the postfix code to append
	 */
	private void addCode(int code) {
		f.postfix_code[++f.postfix_code[0]] = code;
	}

	private static void addCode(int[] a, int code) {
		a[++a[0]] = code;
	}

	private static void addCodes(int[] a, int[] b) {
		int n = b[0];
		int pt = a[0];
		a[0] += n;
		System.arraycopy(b, 1, a, pt + 1, n);
	}

	/**
	 * Scans a number. Valid format: xxx[.xxx[e[+|-]xxx]]
	 *
	 * @exception ParserException
	 */
	private void scanNumber() throws ParserException {
		// changed by W. Christian to parse numbers with leading zeros.
		String numstr = ""; //$NON-NLS-1$
		double value;
		if (num == MAX_NUM) {
			throw new ParserException(TOO_MANY_CONSTS);
		}
		if (ch != '.') { // added by W. Christian
			do {
				numstr += ch;
				getNextch();
			} while ((ch >= '0') && (ch <= '9'));
		} else {
			numstr += '0';
		} // added by W. Christian
		if (ch == '.') {
			do {
				numstr += ch;
				getNextch();
			} while ((ch >= '0') && (ch <= '9'));
		}
		// if(ch=='e') {
		if ((ch == 'e') || (ch == 'E')) { // changed by Doug Brown May 2007
			numstr += ch;
			getNextch();
			if ((ch == '+') || (ch == '-')) {
				numstr += ch;
				getNextch();
			}
			while ((ch >= '0') && (ch <= '9')) {
				numstr += ch;
				getNextch();
			}
		}
		value = getNumber(numstr);
		if (Double.isNaN(value)) {
			position = start;
			throw new ParserException(SYNTAX_ERROR);
		}
		addNum(value);
	}

	private void addNum(double value) {
		f.number[num++] = value;
		addCode(NUMERIC);
	}

	public static double getNumber(String name) {
		if (couldBeNumber(name)) {
			try {
				return Double.parseDouble(name);
			} catch (NumberFormatException e) {
			}
		}
		return Double.NaN;
	}

	/**
	 * before we test for a NFE, at least check that it COULD be a number. I is for
	 * "Infinity"
	 * 
	 * @param n
	 * @return
	 */
	public static boolean couldBeNumber(String n) {
		return (n.length() > 0 && "+-.I0123456789".indexOf(n.charAt(0)) >= 0);
	}

	/**
	 * Scans a non-numerical identifier. Can be function call, variable, reference,
	 * etc.
	 *
	 * @exception ParserException
	 */
	private void scanNonNumeric() throws ParserException {
		String stream = ""; //$NON-NLS-1$
		if ((ch == '*') || (ch == '/') || (ch == '^') || (ch == ')') || (ch == ',')
				|| (ch == '<') || (ch == '>') || (ch == '=') || (ch == '&')
				|| (ch == '|')) {
			throw new ParserException(SYNTAX_ERROR);
		}
		do {
			stream += ch;
			getNextch();
		} while (!((ch == ' ') || (ch == '+') || (ch == '-') || (ch == '*')
				|| (ch == '/') || (ch == '^') || (ch == '(') || (ch == ')')
				|| (ch == ',') || (ch == '<') || (ch == '>') || (ch == '=')
				|| (ch == '&') || (ch == '|')));
		if (stream.equals("pi")) { //$NON-NLS-1$
			addCode(PI_CODE);
			return;
		} else if (stream.equals("e")) { //$NON-NLS-1$
			addCode(E_CODE);
			return;
		}
		// if
		if (stream.equals("if")) { //$NON-NLS-1$
			skipSpaces();
			if (ch != '(') {
				throw new ParserException(PAREN_EXPECTED);
			}
			scanAndParse();
			if (ch != ',') {
				throw new ParserException(COMMA_EXPECTED);
			}
			addCode(IF_CODE);
			int[] savecode = Arrays.copyOf(f.postfix_code, f.postfix_code.length);
			f.postfix_code[0] = 0; //$NON-NLS-1$
			scanAndParse();
			if (ch != ',') {
				throw new ParserException(COMMA_EXPECTED);
			}
			addCode(JUMP_CODE);
			addCode(savecode, f.postfix_code[0] + 2);
			addCodes(savecode, f.postfix_code);
			f.postfix_code[0] = 0; //$NON-NLS-1$
			scanAndParse();
			if (ch != ')') {
				throw new ParserException(PAREN_EXPECTED);
			}
			addCode(savecode, f.postfix_code[0] + 1);
			addCodes(savecode, f.postfix_code);
			f.postfix_code = Arrays.copyOf(savecode, savecode.length);
			getNextch();
			return;
		}
		// built-in function
		for (int i = 0; i < NO_FUNCS; i++) {
			if (stream.equals(funcname[i])) {
				skipSpaces();
				if (ch != '(') {
					throw new ParserException(PAREN_EXPECTED);
				}
				scanAndParse();
				if (ch != ')') {
					throw new ParserException(PAREN_EXPECTED);
				}
				getNextch();
				addCode(i | FUNC_OFFSET);
				return;
			}
		}
		// extended functions
		for (int i = 0; i < NO_EXT_FUNCS; i++) {
			if (stream.equals(extfunc[i])) {
				skipSpaces();
				if (ch != '(') {
					throw new ParserException(PAREN_EXPECTED);
				}
				scanAndParse();
				if (ch != ',') {
					throw new ParserException(COMMA_EXPECTED);
				}
				int[] savecode = Arrays.copyOf(f.postfix_code, f.postfix_code.length);
				f.postfix_code[0] = 0; //$NON-NLS-1$
				scanAndParse();
				if (ch != ')') {
					throw new ParserException(PAREN_EXPECTED);
				}
				getNextch();
				addCodes(savecode, f.postfix_code);
				f.postfix_code = Arrays.copyOf(savecode, savecode.length);
				addCode(i | EXT_FUNC_OFFSET);
				return;
			}
		}
		// registered variables
		for (int i = 0; i < f.var_count; i++) {
			if (stream.equals(f.var_name[i])) {
				addCode(i | VAR_OFFSET);
				return;
			}
		}
		// references
		int index = f.refnames.indexOf(stream);
		if (index != -1) {
			addCode(index | REF_OFFSET);
			return;
		}
		// appendVariables option added by W. Christian
		if ((allowUnknown || 
				appendVariables) && append(stream)) {
			return;
		}
		position = start;
		throw new ParserException(UNKNOWN_IDENTIFIER);
	}

	// W. Christian addition to automatically add variables
	private boolean append(String stream) {
		String[] var_name2 = new String[f.var_count + 1];
		double[] var_value2 = new double[f.var_count + 1];
		System.arraycopy(f.var_name, 0, var_name2, 0, f.var_count);
		System.arraycopy(f.var_value, 0, var_value2, 0, f.var_count);
		var_name2[f.var_count] = stream;
		f.var_name = var_name2;
		f.var_value = var_value2;
		f.var_count++;
		// System.out.println("appended=" + stream);
		for (int i = 0; i < f.var_count; i++) {
			if (stream.equals(f.var_name[i])) {
				addCode(i | VAR_OFFSET);
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets an identifier starting from current parsing pointer.
	 *
	 * @return whether the identifier should be negated
	 * @exception ParserException
	 */
	private boolean getIdentifier() throws ParserException {
		boolean negate = false;
		getNextch();
		skipSpaces();
		if (ch == '!') {
			getNextch();
			skipSpaces();
			if (ch != '(') {
				throw new ParserException(PAREN_EXPECTED);
			}
			scanAndParse();
			if (ch != ')') {
				throw new ParserException(PAREN_EXPECTED);
			}
			if (!isBoolean) {
				throw new ParserException(INVALID_OPERAND);
			}
			addCode(NOT_CODE);
			getNextch();
			return false;
		}
		isBoolean = false;
		while ((ch == '+') || (ch == '-')) {
			if (ch == '-') {
				negate = !negate;
			}
			getNextch();
			skipSpaces();
		}
		start = position;
		// if ((ch >= '0') && (ch <= '9')) changed be W. Christian to
		// handle leanding zeros.
		if (((ch >= '0') && (ch <= '9')) || (ch == '.')) {
			scanNumber();
		} else if (ch == '(') {
			scanAndParse();
			getNextch();
		} else {
			scanNonNumeric();
		}
		skipSpaces();
		return (negate);
	}

	/**
	 * Scans arithmetic level 3 (highest). Power arithmetics.
	 *
	 * @exception ParserException
	 */
	private void arithmeticLevel3() throws ParserException {
		boolean negate;
		
		if (isBoolean) {
			throw new ParserException(INVALID_OPERAND);
		}
		negate = getIdentifier();
		if (isBoolean) {
			throw new ParserException(INVALID_OPERAND);
		}
		if (ch == '^') {
			arithmeticLevel3();
		}
		addCode(POWER);
		if (negate) {
			addCode(NEGATE);
		}
	}

	/**
	 * Scans arithmetic level 2. Multiplications and divisions.
	 *
	 * @exception ParserException
	 */
	private void arithmeticLevel2() throws ParserException {
		boolean negate;
		if (isBoolean) {
			throw new ParserException(INVALID_OPERAND);
		}
		do {
			int operator = ch;
			negate = getIdentifier();
			if (isBoolean) {
				throw new ParserException(INVALID_OPERAND);
			}
			if (ch == '^') {
				arithmeticLevel3();
			}
			if (negate) {
				addCode(NEGATE);
			}
			addCode(operator);
		} while ((ch == '*') || (ch == '/'));
	}

	/**
	 * Scans arithmetic level 1 (lowest). Additions and substractions.
	 *
	 * @exception ParserException
	 */
	private void arithmeticLevel1() throws ParserException {
		boolean negate;
		if (isBoolean) {
			throw new ParserException(INVALID_OPERAND);
		}
		do {
			int operator = ch;
			negate = getIdentifier();
			if (isBoolean) {
				throw new ParserException(INVALID_OPERAND);
			}
			switch (ch) {
			case '^':
				arithmeticLevel3();
				if (negate) {
					addCode(NEGATE);
				}
				break;
			case '*':
			case '/':
				if (negate) {
					addCode(NEGATE);
				}
				arithmeticLevel2();
				break;
			}
			addCode(operator);
		} while ((ch == '+') || (ch == '-'));
	}

	/**
	 * Scans relation level.
	 *
	 * @exception ParserException
	 */
	private void relationLevel() throws ParserException {
		int code =  0;
		if (inRelation) {
			throw new ParserException(INVALID_OPERATOR);
		}
		inRelation = true;
		if (isBoolean) {
			throw new ParserException(INVALID_OPERAND);
		}
		switch (ch) {
		case '=':
			code = EQUAL;
			break;
		case '<':
			code = LESS_THAN;
			getNextch();
			if (ch == '>') {
				code = NOT_EQUAL;
			} else if (ch == '=') {
				code = LESS_EQUAL;
			} else {
				position--;
			}
			break;
		case '>':
			code = GREATER_THAN;
			getNextch();
			if (ch == '=') {
				code = GREATER_EQUAL;
			} else {
				position--;
			}
			break;
		}
		scanAndParse();
		inRelation = false;
		if (isBoolean) {
			throw new ParserException(INVALID_OPERAND);
		}
		addCode(code);
		isBoolean = true;
	}

	/**
	 * Scans boolean level.
	 *
	 * @exception ParserException
	 */
	private void booleanLevel() throws ParserException {
		if (!isBoolean) {
			throw new ParserException(INVALID_OPERAND);
		}
		char c = ch;
		scanAndParse();
		if (!isBoolean) {
			throw new ParserException(INVALID_OPERAND);
		}
		switch (c) {
		case '&':
			addCode(AND_CODE);
			break;
		case '|':
			addCode(OR_CODE);
			break;
		}
	}

	/**
	 * Main method of scanning and parsing process.
	 *
	 * @exception ParserException
	 */
	private void scanAndParse() throws ParserException {
		boolean negate;
		negate = getIdentifier();
		if ((ch != '^') && (negate)) {
			addCode(NEGATE);
		}
		do {
			switch (ch) {
			case '+':
			case '-':
				arithmeticLevel1();
				break;
			case '*':
			case '/':
				arithmeticLevel2();
				break;
			case '^':
				arithmeticLevel3();
				if (negate) {
					addCode(NEGATE);
				}
				break;
			case ',':
			case ')':
				return;
			case '=':
			case '<':
			case '>':
				relationLevel();
				break;
			case '&':
			case '|':
				booleanLevel();
				break;
			default:
				throw new ParserException(OPERATOR_EXPECTED);
			}
		} while (true);
	}

	/**
	 * Parses subfunction.
	 */
	private void parseSubFunction() {
		position = 0;
		f.postfix_code[0] = 0; //$NON-NLS-1$
		inRelation = false;
		isBoolean = false;
		try {
			scanAndParse();
		} catch (ParserException e) {
			error = e.getErrorCode();
			if ((error == SYNTAX_ERROR) && (f.postfix_code[0] == 0)) { //$NON-NLS-1$
				error = EXPRESSION_EXPECTED;
			}
		}
		if ((error == NO_ERROR) && (position != function.length())) {
			error = PAREN_NOT_MATCH;
		}
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
