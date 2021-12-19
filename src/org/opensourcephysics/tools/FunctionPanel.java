/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.GUIUtils;

/**
 * This is a JPanel for managing Functions and supporting Parameters.
 *
 * subclassed as DataFunctionPanel, FitFunctionPanel, and ModelFunctionPanel (as
 * AnalyticFunctionPanel, DynamicFunctionPanel, and
 * ParticleDataTrackFunctionPanel
 * 
 * 
 * <code>
  FunctionPanel
     DataFunctionPanel
     FitFunctionPanel
     ModelFunctionPanel
        AnalyticFunctionPanel
        DynamicFunctionPanel
        ParticleDataTrackFunctionPanel
 </code>
 * 
 * 
 * 
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
public class FunctionPanel extends JPanel implements PropertyChangeListener {
	// instance fields
	protected FunctionTool functionTool;
	protected ParamEditor paramEditor;
	protected FunctionEditor functionEditor;

	private UndoableEditSupport undoSupport;
	protected UndoManager undoManager;
	protected String prevName, description;

	// GUI

	protected Container box;
	protected JTextPane instructions;
	protected JTextField tableEditorField;

	private JButton undoButton;
	private JButton redoButton;
	private int varBegin, varEnd;
	private Icon icon;
	private boolean haveGUI;

	protected boolean haveGUI() {
		return haveGUI;
	}

	/**
	 * Constructor FunctionPanel
	 * 
	 * @param editor
	 */
	public FunctionPanel(FunctionEditor editor) {
		super(new BorderLayout());
		functionEditor = editor;
		editor.functionPanel = this;
		init();
	}

	protected void init() {
		if (functionEditor instanceof DataFunctionEditor) {
			paramEditor = new ParamEditor(((DataFunctionEditor) functionEditor).getData());
		} else {
			paramEditor = new ParamEditor();
		}
		paramEditor.functionPanel = this;
		functionEditor.setParamEditor(paramEditor);
		paramEditor.setFunctionEditors(new FunctionEditor[] { functionEditor });
		paramEditor.addPropertyChangeListener(this);
		paramEditor.addPropertyChangeListener(functionEditor);
		functionEditor.addPropertyChangeListener(this);
		functionEditor.addPropertyChangeListener(paramEditor);
	}

	public void checkGUI() {
		if (!haveGUI) {
			createGUI();
			refreshGUI();
		}
	}

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		haveGUI = true;
		// create textpane and color styles for instructions
		instructions = GUIUtils.newJTextPane();
		instructions.setMinimumSize(new Dimension(300, 60));
		instructions.setEditable(false);
		instructions.setOpaque(false);
		instructions.setFocusable(false);
		instructions.setBackground(Color.yellow);
		instructions.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		StyledDocument doc = instructions.getStyledDocument();
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setFontFamily(def, "SansSerif"); //$NON-NLS-1$
		Style blue = doc.addStyle("blue", def); //$NON-NLS-1$
		StyleConstants.setBold(blue, false);
		StyleConstants.setForeground(blue, Color.blue);
		Style red = doc.addStyle("red", blue); //$NON-NLS-1$
		StyleConstants.setBold(red, true);
		StyleConstants.setForeground(red, Color.red);
		// create box and function editors
		box = Box.createVerticalBox();
		box.add(paramEditor);
		box.add(functionEditor);
		box.add(new JScrollPane(instructions) {
			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				Font font = instructions.getFont();
				dim.height = Math.max(dim.height, font.getSize() * 4);
				return dim;
			}

		});
		// set up the undo system
		undoManager = new UndoManager();
		undoSupport = new UndoableEditSupport();
		undoSupport.addUndoableEditListener(undoManager);
		undoButton = new JButton();
		undoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				undoManager.undo();
			}

		});
		redoButton = new JButton();
		redoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				undoManager.redo();
			}

		});
		clearSelection();
		add(box, BorderLayout.CENTER);
	}

	private String lang = null;
	private Object lastInstruction;

	/**
	 * Refreshes the GUI.
	 */
	protected void refreshGUI() {
		if (!haveGUI)
			return;
		if (functionTool != null && functionTool.getSelectedPanel() == this) {
			if (!functionTool.hasButton(undoButton)) {
				functionTool.setButtonBar(new Object[] { "help", undoButton, redoButton, "close" });
			}
		}
		undoButton.setEnabled(undoManager.canUndo());
		redoButton.setEnabled(undoManager.canRedo());
		lastInstruction = null;
		refreshInstructions(null, -1);

		if (lang == ToolsRes.getLanguage())
			return;
		lang = ToolsRes.getLanguage();
		undoButton.setText(ToolsRes.getString("DataFunctionPanel.Button.Undo")); //$NON-NLS-1$
		undoButton.setToolTipText(ToolsRes.getString("DataFunctionPanel.Button.Undo.Tooltip")); //$NON-NLS-1$
		redoButton.setText(ToolsRes.getString("DataFunctionPanel.Button.Redo")); //$NON-NLS-1$
		redoButton.setToolTipText(ToolsRes.getString("DataFunctionPanel.Button.Redo.Tooltip")); //$NON-NLS-1$
		paramEditor.refreshGUI();
		functionEditor.refreshGUI();
	}

	/**
	 * Gets the ParamEditor.
	 *
	 * @return the param editor
	 */
	public ParamEditor getParamEditor() {
		return paramEditor;
	}

	/**
	 * Gets the function editor.
	 *
	 * @return the function editor
	 */
	public FunctionEditor getFunctionEditor() {
		return functionEditor;
	}

	/**
	 * Gets the function table.
	 *
	 * @return the table
	 */
	public FunctionEditor.Table getFunctionTable() {
		return functionEditor.getTable();
	}

	/**
	 * Gets the parameter table.
	 *
	 * @return the table
	 */
	public FunctionEditor.Table getParamTable() {
		return paramEditor.getTable();
	}

	/**
	 * Gets an appropriate label for the FunctionTool dropdown.
	 *
	 * @return a label string
	 */
	public String getLabel() {
		return ToolsRes.getString("FunctionPanel.Label"); //$NON-NLS-1$
	}

	/**
	 * Gets the display name for the FunctionTool dropdown. By default, this returns
	 * the name of this panel.
	 *
	 * @return the display name
	 */
	public String getDisplayName() {
		return getName();
	}

	/**
	 * Override getPreferredSize().
	 *
	 * @return the preferred size
	 */
	@Override
	public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		JPanel bp = paramEditor.getButtonPanel();
		dim.width = bp.getPreferredSize().width;
		return dim;
	}

	/**
	 * Adds names to the forbidden set.
	 * 
	 * @param names the names
	 */
	protected void addForbiddenNames(String[] names) {
		for (int i = 0; i < names.length; i++) {
			functionEditor.forbiddenNames.add(names[i]);
			if (paramEditor != null) {
				paramEditor.forbiddenNames.add(names[i]);
			}
		}
	}

	/**
	 * Listens for property changes "edit" and "function"
	 *
	 * @param e the event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		switch (e.getPropertyName()) {
		case FunctionEditor.PROPERTY_FUNCTIONEDITOR_EDIT:
			// Parameter or Function has been edited
			if ((e.getNewValue() instanceof UndoableEdit)) {
				// post undo edit
				undoSupport.postEdit((UndoableEdit) e.getNewValue());
			}
			refreshFunctions();
			refreshGUI();
			if (functionEditor.getObjects().size() > 0) {
				String functionName = (String) e.getOldValue();
				String prevName = null;
				if (e.getNewValue() instanceof FunctionEditor.DefaultEdit) {
					FunctionEditor.DefaultEdit edit = (FunctionEditor.DefaultEdit) e.getNewValue();
					if (edit.editType == FunctionEditor.NAME_EDIT) {
						prevName = edit.undoObj.toString();
					}
				} else if (e.getNewValue() instanceof String) {
					prevName = e.getNewValue().toString();
				}
				if (functionTool != null)
					functionTool.firePropertyChange(FunctionTool.PROPERTY_FUNCTIONTOOL_FUNCTION, prevName,
							functionName); // $NON-NLS-1$
			}
			break;
		case DataFunctionEditor.PROPERTY_DATAFUNCTIONEDITOR_FUNCTION:
			// function has been added or removed
			refreshFunctions();
			refreshGUI();
			if (functionTool != null) {
				functionTool.refreshGUI();
				functionTool.firePropertyChange(FunctionTool.PROPERTY_FUNCTIONTOOL_FUNCTION, null, null); // $NON-NLS-1$
			}
			break;
		case FunctionEditor.PROPERTY_FUNCTIONEDITOR_DESCRIPTION:
			if (functionTool != null) {
				functionTool.firePropertyChange(FunctionEditor.PROPERTY_FUNCTIONEDITOR_DESCRIPTION, null, null); // $NON-NLS-1$
			}
		}
	}

	/**
	 * Clears the selection.
	 */
	protected void clearSelection() {
		if (!haveGUI)
			return;
		getFunctionTable().clearSelection();
		getParamTable().clearSelection();
		refreshInstructions(null, -1);
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the level
	 */
	protected void setFontLevel(int level) {
		lastInstruction = null;
		FontSizer.setFonts(this);
//    FontSizer.setFonts(undoButton, level);
//    FontSizer.setFonts(redoButton, level);
	}

	/**
	 * Gets the description for this panel.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description == null ? "" : description; //$NON-NLS-1$
	}

	/**
	 * Sets the description for this panel.
	 * 
	 * @param desc the description
	 */
	public void setDescription(String desc) {
		description = desc;
	}

	/**
	 * Gets the Icon for this panel, if any.
	 * 
	 * @return the icon
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * Sets the Icon for this panel.
	 * 
	 * @param icon the icon
	 */
	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	/**
	 * Refreshes the functions.
	 */
	protected void refreshFunctions() {
		functionEditor.evaluateAll();
	}

	/**
	 * Sets the FunctionTool. This method is called by the tool to which this panel
	 * is added.
	 *
	 * @param tool the FunctionTool
	 */
	public void setFunctionTool(FunctionTool tool) {
		functionTool = tool;
	}

	/**
	 * Tabs to the next editor.
	 *
	 * @param editor the current editor
	 */
	protected void tabToNext(FunctionEditor editor) {
		if (!haveGUI)
			return;
		if (editor == functionEditor) {
			functionTool.focusHelp();
		} else {
			functionEditor.tabToNext();
		}
	}

	/**
	 * Refreshes the instructions based on selected cell.
	 *
	 * @param source         the function editor (may be null)
	 * @param selectedColumn the selected table column, or -1 if none
	 */
	final protected void refreshInstructions(FunctionEditor source, int selectedColumn) {
		// BH 2021.12.19 removed "editing" parameter; always false. This was for the JavaScript hack
		if (instructions == null)
			return;
		boolean isError = false;
		String s;
		if (hasCircularErrors()) { // error condition
			s = ToolsRes.getString("FunctionPanel.Instructions.CircularErrors"); //$NON-NLS-1$
			isError = true;
		} else if (hasInvalidExpressions()) { // error condition
			s = ToolsRes.getString("FunctionPanel.Instructions.BadCell"); //$NON-NLS-1$
			isError = true;
		} else {
			s = getCustomInstructions(source, selectedColumn);
		}
		if (s.equals(lastInstruction))
			return;
		lastInstruction = s;
		instructions.setText(s);
		StyledDocument doc = instructions.getStyledDocument();
		doc.setCharacterAttributes(0, s.length(), doc.getStyle(isError ? "red" : "blue"), false);
		// BH: this next is not necessary and causes unnecessary full layout of the FunctionPanel
		// revalidate();
	}

	/**
	 * Overwritten in ParticleDataTrack
	 * 
	 * @param source
	 * @param selectedColumn
	 * @return
	 */
	protected String getCustomInstructions(FunctionEditor source, int selectedColumn) {
		String s;
		if (source != null && selectedColumn >= 0) {
			s = ToolsRes.getString("FunctionPanel.Instructions.EditCell"); //$NON-NLS-1$
			if (selectedColumn == 0) {
				s += "  " + ToolsRes.getString("FunctionPanel.Instructions.NameCell"); //$NON-NLS-1$//$NON-NLS-2$
				s += "\n" + ToolsRes.getString("FunctionPanel.Instructions.EditDescription"); //$NON-NLS-1$//$NON-NLS-2$
			} else {
				s += " " + ToolsRes.getString("FunctionPanel.Instructions.Help"); //$NON-NLS-1$//$NON-NLS-2$
			}
		} else {
			s = (isEmpty() ? ToolsRes.getString("FunctionPanel.Instructions.GetStarted") //$NON-NLS-1$
					: ToolsRes.getString("FunctionPanel.Instructions.General") //$NON-NLS-1$
							+ "  " + ToolsRes.getString("FunctionPanel.Instructions.EditDescription")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return s;
	}

	protected boolean isEmpty() {
		return (functionEditor.getObjects().size() == 0) && (paramEditor.getObjects().size() == 0);
	}

	protected boolean hasInvalidExpressions() {
		return functionEditor.containsInvalidExpressions() || paramEditor.containsInvalidExpressions();
	}

	protected boolean hasCircularErrors() {
		return !functionEditor.circularErrors.isEmpty() || !paramEditor.circularErrors.isEmpty();
	}

	/**
	 * Disposes of this panel.
	 */
	protected void dispose() {
		paramEditor.removePropertyChangeListener(this);
		paramEditor.removePropertyChangeListener(functionEditor);
		functionEditor.removePropertyChangeListener(this);
		functionEditor.removePropertyChangeListener(paramEditor);
		functionEditor.setParamEditor(null);
		paramEditor.setFunctionEditors(new FunctionEditor[0]);
		functionEditor.setFunctionPanel(null);
		functionEditor = null;
		paramEditor.setFunctionPanel(null);
		paramEditor = null;
		setFunctionTool(null);
	}

	@Override
	public void finalize() {
		OSPLog.finalized(this);
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
