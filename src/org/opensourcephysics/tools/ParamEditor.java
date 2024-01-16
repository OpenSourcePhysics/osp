/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.swing.JCheckBox;

import org.opensourcephysics.display.DatasetManager;

/**
 * A FunctionEditor for Parameters.
 *
 * @author Douglas Brown
 */
public class ParamEditor extends FunctionEditor {
	
	protected double[] paramValues = new double[0];
	private DatasetManager data;
	private FunctionEditor[] functionEditors;
	protected String[] paramDescriptions = new String[0];
	protected JCheckBox syncedCheckbox;
	protected boolean syncing = false;

	/**
	 * Default constructor
	 */
	public ParamEditor() {
		super();
		paramEditor = this;
	}

	/**
	 * Constructor using a DatasetManager to define initial parameters
	 *
	 * @param input the DatasetManager
	 */
	public ParamEditor(DatasetManager input) {
		this();
		data = input;
		loadParametersFromData();
	}
	
	@Override
	protected void createGUI() {
		super.createGUI();
		if (syncedCheckbox == null) {
			syncedCheckbox = new JCheckBox();
			syncedCheckbox.addActionListener((e) -> {
				doSyncAction();
			});
		}
	}
	
	@Override
	public void refreshGUI() {
		super.refreshGUI();
		if (syncedCheckbox != null) {
			syncedCheckbox.setText(ToolsRes.getString("ParamEditor.Checkbox.Synced.Text"));
		}
	}
	
	@Override
	protected void enableMenuButtons() {
		super.enableMenuButtons();
		if (syncedCheckbox != null) {
			Parameter param = (Parameter)getSelectedObject();
			syncedCheckbox.setEnabled(param != null && param.isNameEditable());
			syncedCheckbox.setSelected(param != null && param.isSynced());
			String tooltip = ToolsRes.getString("ParamEditor.Checkbox.Synced.Tooltip");
			if (param != null && param.isNameEditable()) {
				tooltip += " \"" +param.getName() + "\"";
			}
			syncedCheckbox.setToolTipText(tooltip);
		}
	}
	
	/**
	 * Sets the synced property of a Parameter.
	 * @param synced boolean
	 * @param param the Parameter
	 */
	protected void doSyncAction() {
		boolean sync = syncedCheckbox.isSelected();
		Parameter param = (Parameter)getSelectedObject();
		if (param == null || !param.isNameEditable()
				|| sync == param.isSynced())
			return;
		param.setSynced(sync);
		firePropertyChange(Parameter.PROPERTY_PARAMETER_SYNCED, null, param); // $NON-NLS-1$
	}

	/**
	 * Gets an array containing copies of the current parameters.
	 *
	 * @return an array of Parameters
	 */
	public Parameter[] getParameters() {
		Parameter[] params = new Parameter[objects.size()];
		for (int i = 0; i < objects.size(); i++) {
			Parameter next = (Parameter) objects.get(i);
			params[i] = new Parameter(next.paramName, next.expression);
			params[i].setExpressionEditable(next.isExpressionEditable());
			params[i].setNameEditable(next.isNameEditable());
			params[i].setDescription(next.getDescription());
			params[i].value = next.value;
			params[i].synced = next.synced;
		}
		return params;
	}

	/**
	 * Replaces the current parameters with new ones.
	 *
	 * @param params an array of Parameters
	 */
	public void setParameters(Parameter[] params) {
		List<FObject> list = new ArrayList<FObject>();
		for (int i = 0; i < params.length; i++) {
			list.add(params[i]);
		}
		setObjects(list);
		if (haveGUI())
			updateTable();

	}

	/**
	 * Sets the function editors that use these parameters.
	 *
	 * @param editors an array of FunctionEditors
	 */
	public void setFunctionEditors(FunctionEditor[] editors) {
		functionEditors = editors;
		if (functionEditors == null) {
			paramEditor = null;
		}
	}

	/**
	 * Gets the current parameter values.
	 *
	 * @return an array of values
	 */
	public double[] getValues() {
		return paramValues;
	}

	/**
	 * Gets the current parameter descriptions.
	 *
	 * @return an array of descriptions
	 */
	public String[] getDescriptions() {
		return paramDescriptions;
	}

	/**
	 * Returns the name of the object.
	 *
	 * @param obj the object
	 * @return the name
	 */
	@Override
	public String getName(FObject obj) {
		return (obj == null) ? null : ((Parameter) obj).paramName;
	}

	/**
	 * Returns the expression of the object.
	 *
	 * @param obj the object
	 * @return the expression
	 */
	@Override
	public String getExpression(FObject obj) {
		return (obj == null) ? null : ((Parameter) obj).expression;
	}

	/**
	 * Returns the description of the object.
	 *
	 * @param obj the object
	 * @return the description
	 */
	@Override
	public String getDescription(FObject obj) {
		return (obj == null) ? null : ((Parameter) obj).getDescription();
	}

	/**
	 * Sets the description of an object.
	 *
	 * @param obj  the object
	 * @param desc the description
	 */
	@Override
	public void setDescription(FObject obj, String desc) {
		if (obj != null) {
			Parameter p = (Parameter) obj;
			if (desc != null && desc.trim().equals("")) { //$NON-NLS-1$
				desc = null;
			}
			p.setDescription(desc);
//      for(int i = 0; i<objects.size(); i++) {
//        p = (Parameter) objects.get(i);
//        paramValues[i] = p.getValue();
//        paramDescriptions[i] = p.getDescription();
//      }
			super.setDescription(obj, desc);
		}
	}

	/**
	 * Sets the description of the named parameter, if any.
	 *
	 * @param name        the name
	 * @param description the description
	 */
	public void setDescription(String name, String description) {
		for (FObject obj : objects) {
			Parameter param = (Parameter) obj;
			if (param.getName().equals(name)) {
				setDescription(obj, description);
				break;
			}
		}
	}
	
	/**
	 * Turns on syncing, used by ModelBuilder to sync Parameters
	 *
	 * @param obj the object
	 * @return the tooltip
	 */
	public void setSyncing(boolean sync) {
		syncing = sync;
		if (getButtonPanel() == null)
			return;
		if (syncing )
			getButtonPanel().add(syncedCheckbox);
		else
			getButtonPanel().remove(syncedCheckbox);
	}

	/**
	 * Returns a tooltip for the object.
	 *
	 * @param obj the object
	 * @return the tooltip
	 */
	@Override
	public String getTooltip(FObject obj) {
		String s = ((Parameter) obj).getDescription();
		if (s == null) {
			s = ToolsRes.getString("ParamEditor.Table.Cell.Name.Tooltip"); //$NON-NLS-1$
			s += " (" + ToolsRes.getString("FunctionEditor.Tooltip.HowToEdit") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return s;
	}

	/**
	 * Determines if an object's name is editable.
	 *
	 * @param obj the object
	 * @return true if the name is editable
	 */
	@Override
	public boolean isNameEditable(FObject obj) {
		return ((Parameter) obj).isNameEditable();
	}

	/**
	 * Determines if an object's expression is editable.
	 *
	 * @param obj the object
	 * @return true if the expression is editable
	 */
	@Override
	public boolean isExpressionEditable(FObject obj) {
		return ((Parameter) obj).isExpressionEditable();
	}

	/**
	 * Evaluates an object.
	 */
	protected void evaluateObject(FObject obj) {
		Parameter p = (Parameter) obj;
		p.evaluate(objects);
	}

	/**
	 * Evaluates parameters that depend on the named parameter.
	 *
	 * @param seed the independent parameter
	 * @return a list of evaluated dependent parameters
	 */
	public ArrayList<Parameter> evaluateDependents(Parameter seed) {
		ArrayList<Parameter> temp = new ArrayList<Parameter>();
		for (int i = evaluate.size(); --i >= 0;) {
			Parameter param = (Parameter) evaluate.get(i);
			if (param.paramName.equals(seed.paramName)) {
				temp.add(seed);
				for (int j = i + 1; j < evaluate.size(); j++) {
					Parameter p = (Parameter) evaluate.get(j);
					temp.add(new Parameter(p.paramName, p.expression));
				}
				// evaluate temp list
				for (int j = temp.size(); --j >= 0;) {
					// for each parameter, evaluate and set paramValues element
					Parameter p = temp.get(j);
					p.evaluate(temp);
					referencesChecked.clear();
					if (!references(p.getName(), referencesChecked)) {
						temp.remove(j);
					}
				}
				temp.remove(seed);
				return temp;
			}
		}
		return temp;
	}

	/**
	 * Evaluates all current objects.
	 */
	@Override
	public void evaluateAll() {
		setArrays();
		if (this.getClass() != ParamEditor.class) {
			return;
		}
		if (paramValues.length != objects.size()) {
			paramValues = new double[objects.size()];
		}
		for (int i = 0; i < evaluate.size(); i++) {
			Parameter p = (Parameter) evaluate.get(i);
			p.evaluate(objects);
		}
		if (paramDescriptions.length != objects.size()) {
			paramDescriptions = new String[objects.size()];
		}
		for (int i = 0; i < objects.size(); i++) {
			Parameter p = (Parameter) objects.get(i);
			paramValues[i] = p.getValue();
			paramDescriptions[i] = p.getDescription();
		}
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
		boolean disallowed = super.isDisallowedName(obj, name);
		// added following line so leaving object name unchanged is not disallowed
		if (!disallowed && obj != null && getName(obj).equals(name))
			return false;
		if (functionEditors != null) {
			for (int i = 0; i < functionEditors.length; i++) {
				disallowed = disallowed || functionEditors[i].isDisallowedName(null, name);
			}
		}
		return disallowed;
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
				// create a new object
				Parameter param = (Parameter) controls[i].loadObject(null);
				param.setNameEditable(true);
				param.setExpressionEditable(true);
				addObject(param, true);
			}
			evaluateAll();
		});
	}

	/**
	 * Returns true if the object expression is invalid.
	 */
	@Override
	protected boolean isInvalidExpression(FObject obj) {
		return Double.isNaN(((Parameter) obj).getValue());
	}

	/**
	 * Creates an object with specified name and expression. This always returns a
	 * new Parameter but copies the editable properties.
	 *
	 * @param name       the name
	 * @param expression the expression
	 * @param obj        ignored
	 * @return the object
	 */
	@Override
	protected FObject createObject(String name, String expression, FObject obj) {
		Parameter original = (Parameter) obj;
		if ((original != null) && original.paramName.equals(name) && original.expression.equals(expression)) {
			return original;
		}
		Parameter p = new Parameter(name, expression);
		if (original != null) {
			p.setExpressionEditable(original.isExpressionEditable());
			p.setNameEditable(original.isNameEditable());
			p.setDescription(original.getDescription());
			p.setSynced(original.isSynced());
		}
		return p;
	}


	@Override
	protected void setTitles() {
		newButtonTipText = ToolsRes.getString("ParamEditor.Button.New.Tooltip"); //$NON-NLS-1$
		titledBorderText = ToolsRes.getString("ParamEditor.Border.Title"); //$NON-NLS-1$
	}

	/**
	 * Loads parameters from the current datasetManager.
	 */
	public void loadParametersFromData() {
		if (data == null)
			return;
		for (String name : data.getConstantNames()) {
			String expression = data.getConstantExpression(name);
			Parameter p = (Parameter) getObject(name);
			if (p == null) {
				p = new Parameter(name, expression);
				p.setDescription(data.getConstantDescription(name));
				addObject(p, false);
			} else {
				setExpression(name, expression, false);
			}
		}
	}

	/**
	 * Refreshes the parameters associated with a user function.
	 */
	protected void refreshParametersFromFunction(UserFunction f) {
		// identify values that have changed
		for (int i = 0; i < f.getParameterCount(); i++) {
			String name = f.getParameterName(i);
			String val = String.valueOf(f.getParameterValue(i));
			Parameter p = (Parameter) getObject(name);
			if (p == null) {
				p = new Parameter(name, val);
				p.setNameEditable(false);
				p.setExpressionEditable(false);
				addObject(p, false);
			}
			// change parameter value
			else {
				setExpression(name, val, false);
			}
		}
	}

	/**
	 * Returns the default name for newly created objects.
	 */
	@Override
	protected String getDefaultName() {
		return ToolsRes.getString("ParamEditor.New.Name.Default"); //$NON-NLS-1$
	}

	@Override
	protected boolean isImportant(FObject obj) {
		return false;
	}

	@Override
	protected void setReferences(FObject obj, BitSet directReferences) {
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
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
