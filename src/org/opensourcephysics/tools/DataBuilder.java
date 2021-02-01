/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.opensourcephysics.controls.ListChooser;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.MediaRes;

/**
 * A FunctionTool for building data functions for DataTool tabs.
 */
@SuppressWarnings("serial")
public class DataBuilder extends FunctionTool {

	private DataTool dataTool;
	private JButton loadButton, saveButton;

	public DataBuilder(DataTool tool) {
		this(tool, false);
	}

	public DataBuilder(DataTool tool, boolean lazyGUI) {
		super(tool, false, lazyGUI);
		dataTool = tool;
		setHelpPath("data_builder_help.html"); //$NON-NLS-1$
		if (!lazyGUI)
			createGUI();
	}

	@Override
	protected void createGUI() {
		if (haveGUI())
			return;
		super.createGUI();
		String imageFile = "/org/opensourcephysics/resources/tools/images/open.gif"; //$NON-NLS-1$
		Icon openIcon = ResourceLoader.getImageIcon(imageFile);
		loadButton = new JButton(openIcon);
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = OSPRuntime.createChooser(ToolsRes.getString("DataBuilder.Load.Title"), //$NON-NLS-1$
						ToolsRes.getString("FileChooser.Filter.XMLFiles"), //$NON-NLS-1$
						new String[] { "xml" }); //$NON-NLS-1$
				int result = chooser.showOpenDialog(dataTool);
				if (result == JFileChooser.APPROVE_OPTION) {
					OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
					XMLControl control = new XMLControlElement(chooser.getSelectedFile());
					if (control.failedToRead()) {
						JOptionPane.showMessageDialog(dataTool, ToolsRes.getString("Dialog.Invalid.Message"), //$NON-NLS-1$
								ToolsRes.getString("Dialog.Invalid.Title"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					Class<?> type = control.getObjectClass();
					if (DataFunctionPanel.class.isAssignableFrom(type)) {
						chooseDataFunctions(control, "Load", null, new ActionListener() {

							@Override
							public void actionPerformed(ActionEvent e) {
								if (e.getID() == ActionEvent.ACTION_PERFORMED) {
									// load data function panel
									control.loadObject(getSelectedPanel());
								}

							}

						});
					} else {
						JOptionPane.showMessageDialog(dataTool,
								ToolsRes.getString("DataBuilder.Dialog.WrongType.Message"), //$NON-NLS-1$
								ToolsRes.getString("DataBuilder.Dialog.WrongType.Title"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
					}

				}
			}
		});

		imageFile = "/org/opensourcephysics/resources/tools/images/save.gif"; //$NON-NLS-1$
		Icon saveIcon = ResourceLoader.getImageIcon(imageFile);
		saveButton = new JButton(saveIcon);
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				XMLControl control = new XMLControlElement(getSelectedPanel());
				chooseDataFunctions(control, "Save", null, new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						if (e.getID() == ActionEvent.ACTION_PERFORMED) {
							JFileChooser chooser = OSPRuntime.createChooser(
									ToolsRes.getString("DataBuilder.Save.Title"), //$NON-NLS-1$
									ToolsRes.getString("FileChooser.Filter.XMLFiles"), //$NON-NLS-1$
									new String[] { "xml" }); //$NON-NLS-1$
							int result = chooser.showSaveDialog(dataTool);
							if (result == JFileChooser.APPROVE_OPTION) {
								OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
								File file = chooser.getSelectedFile();
								String fileName = file.getAbsolutePath();
								if (!"xml".equals(XML.getExtension(fileName))) { //$NON-NLS-1$
									fileName = XML.stripExtension(fileName) + ".xml"; //$NON-NLS-1$
									file = new File(fileName);
								}
								// check for duplicate file
								if (file.exists()) {
									int selected = JOptionPane.showConfirmDialog(dataTool,
											" \"" + file.getName() + "\" " //$NON-NLS-1$ //$NON-NLS-2$
													+ MediaRes.getString("VideoIO.Dialog.FileExists.Message"), //$NON-NLS-1$
											MediaRes.getString("VideoIO.Dialog.FileExists.Title"), //$NON-NLS-1$
											JOptionPane.OK_CANCEL_OPTION);
									if (selected != JOptionPane.OK_OPTION) {
										return;
									}
								}
								control.write(fileName);
							}

						}
					}
				});

			}

		});

		setToolbarComponents(new Component[] { loadButton, saveButton });
	}

	@Override
	protected void setTitles() {
		dropdownTipText = ToolsRes.getString("DataTool.DataBuilder.Dropdown.Tooltip"); //$NON-NLS-1$
		titleText = ToolsRes.getString("DataTool.DataBuilder.Title");
	}
	@Override
	protected void refreshGUI() {
		if (!haveGUI())
			return;
		super.refreshGUI();
		if (loadButton != null) {
			FunctionPanel panel = getSelectedPanel();
			loadButton.setEnabled(panel != null);
			saveButton.setEnabled(panel != null);
			loadButton.setToolTipText(ToolsRes.getString("DataBuilder.Button.Load.Tooltip")); //$NON-NLS-1$
			saveButton.setToolTipText(ToolsRes.getString("DataBuilder.Button.Save.Tooltip")); //$NON-NLS-1$
		}

	}

	protected void refreshPanels() {
		// add and remove DataFunctionPanels based on current tabs
		ArrayList<String> tabNames = new ArrayList<String>();
		for (int i = 0; i < dataTool.tabbedPane.getTabCount(); i++) {
			DataToolTab tab = dataTool.getTab(i);
			tabNames.add(tab.getName());
			if (getPanel(tab.getName()) == null) {
				addPanel(tab.getName(), new DataFunctionPanel(tab.dataManager));
			}
		}
		ArrayList<String> remove = new ArrayList<String>();
		for (Iterator<String> it = trackFunctionPanels.keySet().iterator(); it.hasNext();) {
			String name = it.next().toString();
			if (!tabNames.contains(name)) {
				remove.add(name);
			}
		}
		for (Iterator<String> it = remove.iterator(); it.hasNext();) {
			String name = it.next().toString();
			removePanel(name);
		}
		FunctionPanel p = getSelectedPanel();
		if (p != null) {
			p.getFunctionEditor().enableMenuButtons();
			p.getParamEditor().enableMenuButtons();
		}
		
	}

	/**
	 * Chooses data functions from a DataFunctionPanel XMLControl.
	 *
	 * @param control           the XMLControl
	 * @param description       "Save" or "Load"
	 * @param selectedFunctions collection of DataFunction choices
	 * @return true if user clicked OK
	 */
	protected void chooseDataFunctions(XMLControl control, String description, Collection<String[]> selectedFunctions,
			ActionListener listener) {
		// choose the elements and save
		ArrayList<Object> originals = new ArrayList<Object>();
		ArrayList<Object> choices = new ArrayList<Object>();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> expressions = new ArrayList<String>();
		ArrayList<?> functions = (ArrayList<?>) control.getObject("functions"); //$NON-NLS-1$

		for (Object next : functions) {
			String[] function = (String[]) next;
			originals.add(function);
			choices.add(function);
			names.add(function[0]);
			expressions.add(function[1]);
		}
		// select all by default
		boolean[] selected = new boolean[choices.size()];
		for (int i = 0; i < selected.length; i++) {
			selected[i] = true;
		}
		ListChooser listChooser = new ListChooser(ToolsRes.getString("DataBuilder." + description + ".Title"), //$NON-NLS-1$ //$NON-NLS-2$
				ToolsRes.getString("DataBuilder." + description + ".Message"), //$NON-NLS-1$ //$NON-NLS-2$
				this, new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						if (e.getID() == ActionEvent.ACTION_PERFORMED) {
							// compare choices with originals and remove unwanted object content
							for (Object next : originals) {
								if (!choices.contains(next)) {
									functions.remove(next);
								}
							}
							// rewrite the control with only selected functions
							control.setValue("functions", functions); //$NON-NLS-1$
						}
						listener.actionPerformed(e);
					}

				});
		listChooser.setSeparator(" = "); //$NON-NLS-1$
		listChooser.choose(choices, names, expressions, null, selected, null);
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
