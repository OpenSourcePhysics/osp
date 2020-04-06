/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.ejs.control.value;

/**
 * @see Value
 */
public class ObjectValue extends Value {
	public Object value;

	/**
	 * Constructor ObjectValue
	 * 
	 * @param _val
	 */
	public ObjectValue(Object _val) {
		super(TYPE_OBJECT);
		value = _val;
	}

	public boolean getBoolean() {
		if (value == null || value == Boolean.FALSE) {
			return false;
		}
		if (value == Boolean.TRUE)
			return true;
		if (value instanceof Number)
			return ((Number) value).doubleValue() != 0;
		return value.toString().equals("true"); //$NON-NLS-1$
	}

	public int getInteger() {
		if (value instanceof Number)
			return ((Number) value).intValue();
		return (int) Math.round(getDouble());
	}

	public double getDouble() {
		try {
			if (value instanceof Number)
				return ((Number) value).doubleValue();
			return Double.valueOf(value.toString()).doubleValue();
		} catch (NumberFormatException exc) {
			return 0.0;
		}
	}

	public String getString() {
		if (value == null)
			return null;
		// System.out.println ("Value is "+value);
		if (value instanceof double[]) {
			double[] data = (double[]) value;
			String txt = "new double[]{";
			for (int i = 0; i < data.length; i++) {
				if (i > 0)
					txt += ",";
				txt += data[i];
			}
			return txt + "}";
		}
		if (value instanceof int[]) {
			int[] data = (int[]) value;
			String txt = "new int[]{";
			for (int i = 0; i < data.length; i++) {
				if (i > 0)
					txt += ",";
				txt += data[i];
			}
			return txt + "}";
		}
		if (value instanceof double[][]) {
			double[][] data = (double[][]) value;
			// System.out.println ("dim = "+data.length+","+data[0].length);
			String txt = "new double[][]{";
			for (int i = 0; i < data.length; i++) {
				if (i > 0)
					txt += ",{";
				else
					txt += "{";
				for (int j = 0; j < data[i].length; j++) {
					if (j > 0)
						txt += ",";
					txt += data[i][j];
				}
				txt += "}";
			}
			// System.out.println ("Returning "+txt);
			return txt + "}";
		}
		if (value instanceof int[][]) {
			int[][] data = (int[][]) value;
			String txt = "new int[][]{";
			for (int i = 0; i < data.length; i++) {
				if (i > 0)
					txt += ",{";
				else
					txt += "{";
				for (int j = 0; j < data[i].length; j++) {
					if (j > 0)
						txt += ",";
					txt += data[i][j];
				}
				txt += "}";
			}
			return txt + "}";
		}
		if (value instanceof double[][][]) {
			double[][][] data = (double[][][]) value;
			// System.out.println ("dim = "+data.length+","+data[0].length);
			String txt = "new double[][][]{";
			for (int i = 0; i < data.length; i++) {
				if (i > 0)
					txt += ",{";
				else
					txt += "{";
				for (int j = 0; j < data[i].length; j++) {
					if (j > 0)
						txt += ",{";
					else
						txt += "{";
					for (int k = 0; k < data[i][j].length; k++) {
						if (k > 0)
							txt += ",";
						txt += data[i][j][k];
					}
					txt += "}";
				}
				txt += "}";
			}
			// System.out.println ("Returning "+txt);
			return txt + "}";
		}
		if (value instanceof int[][][]) {
			int[][][] data = (int[][][]) value;
			// System.out.println ("dim = "+data.length+","+data[0].length);
			String txt = "new int[][][]{";
			for (int i = 0; i < data.length; i++) {
				if (i > 0)
					txt += ",{";
				else
					txt += "{";
				for (int j = 0; j < data[i].length; j++) {
					if (j > 0)
						txt += ",{";
					else
						txt += "{";
					for (int k = 0; k < data[i][j].length; k++) {
						if (k > 0)
							txt += ",";
						txt += data[i][j][k];
					}
					txt += "}";
				}
				txt += "}";
			}
			// System.out.println ("Returning "+txt);
			return txt + "}";
		}
		return value.toString();
	}

	public Object getObject() {
		return value;
	}
	// public void copyInto (double[] array) {
	// double[] data = (double[]) value;
	// int n = data.length;
	// if (array.length<n) n = array.length;
	// System.arraycopy(data,0,array,0,n);
	// }
	//
	// public void copyInto (double[][] array) {
	// double[][] data = (double[][]) value;
	// int n = data.length;
	// if (array.length<n) n = array.length;
	// for (int i=0; i<n; i++) {
	// int ni = data[i].length;
	// if (array[i].length<ni) ni = array[i].length;
	// System.arraycopy(data[i],0,array[i],0,ni);
	// }
	// }

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
