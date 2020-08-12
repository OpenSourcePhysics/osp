/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.Color;
import java.util.ArrayList;
import java.util.BitSet;

import org.opensourcephysics.display.DataFunction;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.display.TeXParser;

/**
 * A FunctionEditor for DataFunctions. The source data must contain datasets
 * that are linked.
 *
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
public class DataFunctionEditor extends FunctionEditor {

	public static final String PROPERTY_DATAFUNCTIONEDITOR_FUNCTION = "function";

	static Color[] markerColors = { Color.green.darker(), Color.red, Color.cyan.darker(), Color.yellow.darker(),
			Color.blue };
	
	private DatasetManager data;

	/**
	 * Constructor.
	 *
	 * @param data the data source for DataFunctions
	 */
	public DataFunctionEditor(DatasetManager data) {
		this.data = data;
		init();
	}

	private void init() {
		// load existing DataFunctions, if any
		ArrayList<Dataset> sets = data.getDatasets();
		for (int i = 0, n = sets.size(); i < n; i++) {
			Dataset next = sets.get(i);
			if (next instanceof DataFunction) {
				objects.add((DataFunction) next);
			}
		}
	}


	@Override
	protected void setTitles() {
		titledBorderText = ToolsRes.getString("DataFunctionEditor.Border.Title"); //$NON-NLS-1$
	}
	/**
	 * Returns the DatasetManager.
	 *
	 * @return the DatasetManager
	 */
	public DatasetManager getData() {
		return data;
	}

	/**
	 * Returns the name of the object.
	 *
	 * @param obj the object
	 * @return the name
	 */
	@Override
	public String getName(FObject obj) {
		return (obj == null) ? null : ((DataFunction) obj).getYColumnName();
	}

	/**
	 * Returns the expression of the object.
	 *
	 * @param obj the object
	 * @return the expression
	 */
	@Override
	public String getExpression(FObject obj) {
		return (obj == null) ? null : ((DataFunction) obj).getInputString();
	}

	/**
	 * Returns the description of the object.
	 *
	 * @param obj the object
	 * @return the description
	 */
	@Override
	public String getDescription(FObject obj) {
		return (obj == null) ? null : ((DataFunction) obj).getYColumnDescription();
	}

	/**
	 * Sets the description of the object.
	 *
	 * @param obj  the object
	 * @param desc the description
	 */
	@Override
	public void setDescription(FObject obj, String desc) {
		if (obj != null) {
			if (desc != null && desc.trim().equals("")) { //$NON-NLS-1$
				desc = null;
			}
			((DataFunction) obj).setYColumnDescription(desc);
			super.setDescription(obj, desc);
		}
	}

	/**
	 * Returns a tooltip for the object.
	 *
	 * @param obj the object
	 * @return the tooltip
	 */
	@Override
	public String getTooltip(FObject obj) {
		return (obj == null) ? null : ((DataFunction) obj).getYColumnDescription();
	}

	/**
	 * Determines if an object's name is editable.
	 *
	 * @param obj the object
	 * @return true if the name is editable
	 */
	@Override
	public boolean isNameEditable(FObject obj) {
		return true;
	}

	/**
	 * Determines if an object's expression is editable.
	 *
	 * @param obj the object
	 * @return true if the expression is editable
	 */
	@Override
	public boolean isExpressionEditable(FObject obj) {
		return true;
	}

	/**
	 * Evaluates all current objects.
	 */
	@Override
	public void evaluateAll() {
		setArrays();
		for (int i = 0; i < evaluate.size(); i++) {
			DataFunction f = (DataFunction) evaluate.get(i);
			f.setExpression(f.getInputString()); // refreshes data
		}
	}

	/**
	 * Adds an object.
	 *
	 * @param obj      the object
	 * @param postEdit true to post an undoable edit
	 */
	@Override
	public FObject addObject(FObject obj, int row, boolean postEdit, boolean firePropertyChange) {
		obj = super.addObject(obj, row, postEdit, firePropertyChange);
		if (obj != null) {
			firePropertyChange(DataFunctionEditor.PROPERTY_DATAFUNCTIONEDITOR_FUNCTION, null, obj); //$NON-NLS-1$
		}
		return obj;
	}

	/**
	 * Removes an object.
	 *
	 * @param obj      the object to remove
	 * @param postEdit true to post an undoable edit
	 * @return the removed object
	 */
	@Override
	public FObject removeObject(FObject obj, boolean postEdit) {
		obj = super.removeObject(obj, postEdit);
		if (obj != null) {
			firePropertyChange(DataFunctionEditor.PROPERTY_DATAFUNCTIONEDITOR_FUNCTION, obj, null); //$NON-NLS-1$
		}
		return obj;
	}

	/**
	 * Returns true if a name is already in use.
	 *
	 * @param obj  the object (may be null)
	 * @param name the proposed name for the object
	 * @return true if duplicate
	 */
	@Override
	protected boolean isDisallowedName(FObject obj, String name) {
		ArrayList<Dataset> datasets = data.getDatasets();
		for (int i = 0; i < datasets.size(); i++) {
			Dataset next = datasets.get(i);
			// added following line so leaving object name unchanged is not disallowed
			if (obj != null && next == obj && getName(obj).equals(name))
				return false;
			if ((i == 0) && TeXParser.removeSubscripting(next.getXColumnName()).equals(name)) {
				return true;
			}
			if (TeXParser.removeSubscripting(next.getYColumnName()).equals(name)) {
				return true;
			}
		}
		return super.isDisallowedName(obj, name);
	}

	/**
	 * Returns a String with the names of variables available for expressions.
	 */
	@Override
	protected String getVariablesString(String separator) {
		StringBuffer vars = new StringBuffer(""); //$NON-NLS-1$
		// add parameters, if any
		if (paramEditor != null) {
			Parameter[] parameters = paramEditor.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				vars.append(" "); //$NON-NLS-1$
				vars.append(parameters[i].getName());
			}
		}
		String nameToSkip = getName(getSelectedObject());
		ArrayList<Dataset> datasets = data.getDatasets();
		for (int i = 0; i < datasets.size(); i++) {
			Dataset next = datasets.get(i);
			if (i == 0) {
				String name = next.getXColumnName();
				vars.append(" "); //$NON-NLS-1$
				vars.append(TeXParser.removeSubscripting(name));
			}
			String name = next.getYColumnName();
			if (name.equals(nameToSkip)) {
				continue;
			}
			vars.append(TeXParser.removeSubscripting(name));
		}
		return getVariablesString(vars, separator);
	}

	/**
	 * Returns true if the object expression is invalid.
	 */
	@Override
	protected boolean isInvalidExpression(FObject obj) {
		DataFunction f = (DataFunction) obj;
		return !f.getInputString().equals(f.getExpression());
	}

	/**
	 * Creates an object with specified name and expression. This modifies and
	 * returns the input DataFunction (unless null).
	 *
	 * @param name       the name
	 * @param expression the expression
	 * @param obj        ignored
	 * @return the object
	 */
	@Override
	protected FObject createObject(String name, String expression, FObject obj) {
		DataFunction f = (DataFunction) obj;
		if ((f != null) && f.getYColumnName().equals(name) && f.getInputString().equals(expression)) {
			return f;
		}
		if (f == null) {
			f = new DataFunction(data);
			int i = objects.size();
			if (i < markerColors.length) {
				f.setMarkerColor(markerColors[i], markerColors[i].darker());
				f.setLineColor(markerColors[i]);
			}
			f.setYColumnName(name);
			f.setExpression(expression);
		} else if (!f.getYColumnName().equals(name)) {
			f.setYColumnName(name);
		} else {
			f.setExpression(expression);
		}
		return f;
	}

	/**
	 * Pastes the clipboard contents.
	 */
	@Override
	protected void pasteAction() {
		getClipboardContentsAsync((controls) -> {
			if (controls == null) {
				return;
			}
			for (int i = 0; i < controls.length; i++) {
				// create a new DataFunction
				DataFunction f = new DataFunction(data);
				FObject obj = (FObject) controls[i].loadObject(f);
				addObject(obj, true);
			}
			evaluateAll();
			
		});
	}

	@Override
	protected boolean isImportant(FObject obj) {
		return false;
	}

	@Override
	protected void setReferences(FObject obj, BitSet directRefrences) {
		// TODO Auto-generated method stub

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
