/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.Dimension;
import java.util.List;

/**
 * A FunctionEditor for initial values.
 *
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
public class InitialValueEditor extends ParamEditor {
	/**
	 * Default constructor
	 * 
	 * @param editor
	 */
	public InitialValueEditor(ParamEditor editor) {
		super();
		skipAllName = "t";
		paramEditor = editor;
		setFunctionPanel(editor.getFunctionPanel());
	}

	/**
	 * Determines if an object's name is editable.
	 *
	 * @param obj the object
	 * @return always false
	 */
	@Override
	public boolean isNameEditable(FObject obj) {
		return false;
	}

	@Override
	public Dimension getMaximumSize() {
		Dimension dim = super.getMaximumSize();
		dim.height = getPreferredSize().height;
		return dim;
	}

	/**
	 * Evaluates all current objects.
	 */
	@Override
	public void evaluateAll() {
		setArrays();
		int nObj = objects.size();
		if (paramValues.length != nObj) {
			paramValues = new double[nObj];
		}
		List<FObject> params = paramEditor.getObjects();
		for (int i = 0; i < evaluate.size(); i++) {
			Parameter p = (Parameter) evaluate.get(i);
			p.evaluate(params);
		}
		for (int i = 0; i < nObj; i++) {
			Parameter p = (Parameter) objects.get(i);
			paramValues[i] = p.getValue();
		}
	}

	@Override
	protected boolean isValidExpression(String expression) {
		Parameter p = new Parameter("xxzz", expression); //$NON-NLS-1$
		return !Double.isNaN(p.evaluate(paramEditor.getObjects()));
	}

	/**
	 * Creates the GUI.
	 */
	@Override
	protected void createGUI() {
		addButtonPanel = false;
		super.createGUI();
	}

	@Override
	protected void setTitles() {
		newButtonTipText = ToolsRes.getString("ParamEditor.Button.New.Tooltip"); //$NON-NLS-1$
		titledBorderText = ToolsRes.getString("InitialValueEditor.Border.Title");
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
