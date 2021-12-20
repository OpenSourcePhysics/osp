package org.opensourcephysics.tools;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;

import org.opensourcephysics.controls.ListChooser;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.controls.XMLProperty;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.TeXParser;
import org.opensourcephysics.numerics.SuryonoParser;
import javajs.async.AsyncFileChooser;

/**
 * This is a FunctionTool used by DatasetCurveFitter to build, save and load
 * custom fit functions. Some methods are tailored for use with DataTool since
 * that is its main application.
 * 
 * @author Doug Brown
 *
 */
@SuppressWarnings("serial")
public class FitBuilder extends FunctionTool {

	private final static java.io.FileFilter xmlFilter;
	private final static Collection<String> initialAutoloadSearchPaths = new TreeSet<String>();
	private final static Map<String, String[]> autoloadExclusionsMap = new TreeMap<String, String[]>();

	private static String[] preferredAutoloadSearchPaths;
	private static AsyncFileChooser chooser;

	static {
		xmlFilter = new java.io.FileFilter() {
			// accept only *.xml files.
			@Override
			public boolean accept(File f) {
				if (f == null || f.isDirectory())
					return false;
				String ext = XML.getExtension(f.getName());
				if (ext != null && "xml".equals(ext.toLowerCase())) //$NON-NLS-1$
					return true;
				return false;
			}
		};
		// load preferred autoload search paths and excluded functions from
		// OSPRuntime.preferences
		preferredAutoloadSearchPaths = (String[]) OSPRuntime.getPreference("autoload_search_paths"); //$NON-NLS-1$
		String[][] autoloadData = (String[][]) OSPRuntime.getPreference("autoload_exclusions"); //$NON-NLS-1$
		if (autoloadData != null) {
			for (String[] next : autoloadData) {
				String filePath = XML.forwardSlash(next[0]);
				String[] functions = new String[next.length - 1];
				System.arraycopy(next, 1, functions, 0, functions.length);
				autoloadExclusionsMap.put(filePath, functions);
			}
		}
	}

	protected JButton newFitButton, deleteFitButton, cloneFitButton, loadButton, saveButton, autoloadButton;
	protected Component myParent;
	protected TreeSet<String> addedFits = new TreeSet<String>();
	protected String defaultFitName;
	protected AutoloadManager autoloadManager;

	/**
	 * Constructor
	 * 
	 * @param c a component to determine the dialog owner
	 */
	public FitBuilder(Component c) {
		this(c, false);
	}

	public FitBuilder(Component c, boolean lazyGUI) {
		super(c, true, lazyGUI);
		myParent = c;
		if (!lazyGUI)
			createGUI();
	}

	@Override
	protected void createGUI() {
		if (haveGUI())
			return;
		super.createGUI();
		newFitButton = new JButton(ToolsRes.getString("DatasetCurveFitter.Button.NewFit.Text")); //$NON-NLS-1$
		newFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.NewFit.Tooltip")); //$NON-NLS-1$
		newFitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// create new user function and function panel
				String name = getUniqueName(ToolsRes.getString("DatasetCurveFitter.NewFit.Name")); //$NON-NLS-1$
				UserFunction f = new UserFunction(name);
				Dataset dataset = null;
				DatasetCurveFitter fitter = getSelectedCurveFitter();
				if (fitter != null) {
					dataset = fitter.getData();
				}
				String var = (dataset == null) ? "x" : //$NON-NLS-1$
				TeXParser.removeSubscripting(dataset.getColumnName(0));

				f.setExpression(SuryonoParser.NULL, new String[] { var }); //$NON-NLS-1$
				addFitFunctionPanel(f);
			}
		});
		deleteFitButton = new JButton(ToolsRes.getString("DatasetCurveFitter.Button.DeleteFit.Text")); //$NON-NLS-1$
		deleteFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.DeleteFit.Tooltip")); //$NON-NLS-1$
		deleteFitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// remove selected fit and panel
				String name = getSelectedName();
				removePanel(name);
			}

		});
		cloneFitButton = new JButton(ToolsRes.getString("DatasetCurveFitter.Button.Clone.Text")); //$NON-NLS-1$
		cloneFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.Clone.Tooltip")); //$NON-NLS-1$
		cloneFitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final Map<String, KnownFunction> fits = new HashMap<String, KnownFunction>();
				final ArrayList<String> fitnames = new ArrayList<String>();
				for (DatasetCurveFitter fitter : curveFitters) {
					fitter.getFits(fits, fitnames);
				}
				// inner popup menu listener class
				ActionListener listener = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						for (String name : fitnames) {
							if (name.equals(e.getActionCommand())) {
								DatasetCurveFitter fitter = getSelectedCurveFitter();
								if (fitter != null) {
									KnownFunction f = fits.get(name);
									UserFunction uf = fitter.createClone(f, name);
									UserFunctionEditor editor = new UserFunctionEditor();
									editor.setMainFunctions(new UserFunction[] { uf });
									FitFunctionPanel panel = new FitFunctionPanel(editor);
									addPanel(uf.getName(), panel);
								}
							}
						}
					}
				};
				// create popup menu and add existing fit function items
				JPopupMenu popup = new JPopupMenu();
				for (String name : fitnames) {
					JMenuItem item = new JMenuItem(name);
					item.setActionCommand(name);
					item.addActionListener(listener);
					popup.add(item);
				}
				// show the popup below the button
				popup.show(cloneFitButton, 0, cloneFitButton.getHeight());
			}
		});
		String imageFile = "/org/opensourcephysics/resources/tools/images/open.gif"; //$NON-NLS-1$
		Icon openIcon = ResourceLoader.getImageIcon(imageFile);
		loadButton = new JButton(openIcon);
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadFits();
			}

		});
		imageFile = "/org/opensourcephysics/resources/tools/images/save.gif"; //$NON-NLS-1$
		Icon saveIcon = ResourceLoader.getImageIcon(imageFile);
		saveButton = new JButton(saveIcon);
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveFits();
			}
		});
		// create autoloadButton
		autoloadButton = new JButton();
		autoloadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AutoloadManager manager = getAutoloadManager();
				manager.refreshAutoloadData();
				manager.setVisible(true);
			}

		});
		setToolbarComponents(new Component[] { loadButton, saveButton, new JToolBar.Separator(), newFitButton,
				cloneFitButton, deleteFitButton, Box.createHorizontalGlue(), autoloadButton });
	}

	/**
	 * Gets the DatasetCurveFitter currently selected in the DataTool.
	 * 
	 * @return the selected DataSetCurveFitter
	 */
	public DatasetCurveFitter getSelectedCurveFitter() {
		Window win = this.getOwner();
		if (win != null && win instanceof DataTool) {
			DataTool dataTool = (DataTool) win;
			DataToolTab tab = dataTool.getSelectedTab();
			if (tab != null) {
				return tab.getCurveFitter();
			}
		}
		return null;
	}

	/**
	 * Refreshes the dropdown with names of the available fits.
	 * 
	 * @param name the selected fit name
	 */
	@Override
	public void refreshDropdown(String name) {
		if (name == null) {
			name = defaultFitName;
		}
		deleteFitButton.setEnabled(!getPanelNames().isEmpty());
		if (getPanelNames().isEmpty()) {
			String label = ToolsRes.getString("FitFunctionPanel.Label"); //$NON-NLS-1$
			dropdownLabelText = (label + ":"); //$NON-NLS-1$
		} else {
			dropdownLabelText = null;
		}
		super.refreshDropdown(name);
	}

	/**
	 * Adds a fit function unless already added or loaded.
	 * 
	 * @param f the fit function to add
	 * @return true if added now or previously
	 */
	public boolean addFitFunction(KnownFunction f) {
		if (f instanceof UserFunction) {
			String name = f.getName();
			if (addedFits.contains(name))
				return true;
			// if a panel exists with the same original name, don't add this fit
			for (String next : getPanelNames()) {
				FitFunctionPanel panel = (FitFunctionPanel) getPanel(next);
				if (name.equals(panel.originalName)) {
					return false;
				}
			}
			FitFunctionPanel panel = addFitFunctionPanel((UserFunction) f);
			panel.originalName = name;
			addedFits.add(name);
		} else if (f instanceof KnownPolynomial) {
			UserFunction uf = new UserFunction((KnownPolynomial) f);
			return addFitFunction(uf);
		}
		return true;
	}

	/**
	 * Loads fit functions from an XML file chosen by the user.
	 * 
	 * @return the path to the file opened, or null if none
	 */
	private File loadFits() {
		if (chooser == null) {
			chooser = OSPRuntime.getChooser();
			for (FileFilter filter : chooser.getChoosableFileFilters()) {
				if (filter.getDescription().toLowerCase().indexOf("xml") > -1) { //$NON-NLS-1$
					chooser.setFileFilter(filter);
					break;
				}
			}
		}
		chooser.showOpenDialog(FitBuilder.this, new Runnable() {

			@Override
			public void run() {
				OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
				loadFits(chooser.getSelectedFile(), false);
			}

		}, null);
		return null;
	}

	/**
	 * Loads fit functions from an XML file.
	 * 
	 * @param path    the path to the XML file
	 * @param loadAll true to load all fit functions defined in the file, false to
	 *                let the user choose
	 */
	private void loadFits(File path, boolean loadAll) {
		if (path == null) {
			loadFits();
			return;
		}

		XMLControl control = new XMLControlElement(path);
		if (control.failedToRead()) {
			JOptionPane.showMessageDialog(FitBuilder.this, ToolsRes.getString("Dialog.Invalid.Message"), //$NON-NLS-1$
					ToolsRes.getString("Dialog.Invalid.Title"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		Class<?> type = control.getObjectClass();
		if (type != null && FitBuilder.class.isAssignableFrom(type)) {
			// choose fits to load
			if (loadAll)
				control.loadObject(FitBuilder.this);
			else 
				chooseFitFunctions(control, "Load", new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					control.loadObject(FitBuilder.this);
				}
			});
			
		} else {
			JOptionPane.showMessageDialog(FitBuilder.this,
					ToolsRes.getString("DatasetCurveFitter.FitBuilder.Dialog.WrongType.Message"), //$NON-NLS-1$
					ToolsRes.getString("DatasetCurveFitter.FitBuilder.Dialog.WrongType.Title"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Saves fit functions to an XML file chosen by the user.
	 * 
	 * @return the path to the file saved, or null if cancelled or failed
	 */
	private void saveFits() {
		XMLControl control = new XMLControlElement(this);
		// choose fits to save
		chooseFitFunctions(control, "Save", new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (chooser == null) {
					chooser = OSPRuntime.getChooser();
					for (FileFilter filter : chooser.getChoosableFileFilters()) {
						if (filter.getDescription().toLowerCase().indexOf("xml") > -1) { //$NON-NLS-1$
							chooser.setFileFilter(filter);
							break;
						}
					}
				}
				FontSizer.setFonts(chooser, FontSizer.getLevel());
				int result = chooser.showSaveDialog(FitBuilder.this);
				if (result == JFileChooser.APPROVE_OPTION) {
					OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
					File file = chooser.getSelectedFile();
					// check to see if file already exists
					if (file.exists()) {
						int isSelected = JOptionPane.showConfirmDialog(FitBuilder.this,
								ToolsRes.getString("Tool.Dialog.ReplaceFile.Message") + " " + file.getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								ToolsRes.getString("Tool.Dialog.ReplaceFile.Title"), //$NON-NLS-1$
								JOptionPane.YES_NO_CANCEL_OPTION);
						if (isSelected != JOptionPane.YES_OPTION) {
							return;
						}
					}
					saveFits(file.getAbsolutePath(), control);
				}
			}
		});

	}

	/**
	 * Saves fit functions to an XML file.
	 * 
	 * @param path    the file path
	 * @param control a FitBuilder XMLControl
	 */
	private void saveFits(String path, XMLControl control) {
		if (path == null) {
			saveFits();
			return;
		}
		// add .xml extension if none but don't replace other extensions
		if (XML.getExtension(path) == null) {
			path += ".xml"; //$NON-NLS-1$
		}
		if (control == null || control.getObjectClass() != this.getClass()) {
			control = new XMLControlElement(this);
		}
		control.write(path);
	}
	
	/**
	 * Loads fit functions from all initial search paths.
	 */
	protected void autoloadFits() {
		// autoload fits if parent is data tool
		if (myParent != null && myParent instanceof DataTool) {
			for (String dir : getInitialSearchPaths()) {
				autoloadFits(dir);
			}
		}
	}

	/**
	 * Loads fit functions from all FitBuilder XMLControl files found in a specified
	 * directory.
	 * 
	 * @param dirPath the directory path
	 */
	private void autoloadFits(String dirPath) {
		if (dirPath == null)
			return;

		File dir = new File(dirPath);
		if (!dir.exists())
			return;

		File[] files = dir.listFiles(xmlFilter);
		if (files != null) {
			for (File file : files) {
				XMLControl control = new XMLControlElement(file);
				if (control.failedToRead()) {
					continue;
				}

				Class<?> type = control.getObjectClass();
				if (type != null && FitBuilder.class.isAssignableFrom(type)) {
					// copy the control for modification if any functions are autoload_off
					XMLControl copyControl = new XMLControlElement(control);
					String filePath = XML.forwardSlash(file.getAbsolutePath());
					eliminateExcludedFunctions(copyControl, filePath);
					copyControl.loadObject(this);
				}

			}
		}
	}

	/**
	 * Finds fit functions in all FitBuilder XMLControl files in a specified
	 * directory. Each fit function is String[] {String name, String expression,
	 * String description}
	 * 
	 * @param dirPath the directory path
	 * @return a map of filePath to list of fit functions
	 */
	private Map<String, ArrayList<String[]>> findFitFunctions(String dirPath) {
		Map<String, ArrayList<String[]>> results = new TreeMap<String, ArrayList<String[]>>();
		if (dirPath == null)
			return results;

		File dir = new File(dirPath);
		if (!dir.exists())
			return results;

		File[] files = dir.listFiles(xmlFilter);
		if (files != null) {
			for (File file : files) {
				XMLControl control = new XMLControlElement(file);
				if (control.failedToRead()) {
					continue;
				}

				Class<?> type = control.getObjectClass();
				if (type != null && FitBuilder.class.isAssignableFrom(type)) {
					ArrayList<String[]> functions = new ArrayList<String[]>();

					// look through XMLControl for fit functions
					for (XMLProperty next : control.getPropsRaw()) {
						if (next.getPropertyName().equals("functions")) { //$NON-NLS-1$
							// found FitFunctionPanels
							XMLControl[] panels = next.getChildControls();
							for (XMLControl panelControl : panels) {
								String name = panelControl.getString("name"); //$NON-NLS-1$
								String expression = panelControl.getString("description"); //$NON-NLS-1$
								String[] data = new String[] { name, expression };
								functions.add(data);
							}
							break;
						}
					}

					// add entry to the results map
					results.put(file.getName(), functions);
				}

			}
		}
		return results;
	}

	/**
	 * Eliminates unwanted function entries from a FitBuilder XMLControl. Typical
	 * control:
	 * 
	 * <object class="org.opensourcephysics.tools.FitBuilder">
	 * <property name="functions" type="collection" class="java.util.ArrayList">
	 * <property name="item" type="object">
	 * <object class="org.opensourcephysics.tools.FitFunctionPanel">
	 * <property name="name" type="string">NaturalLog</property>
	 * <property name="description" type="string">y = ln(x)</property>
	 * <property name="user_parameters" type="array" class=
	 * "[Lorg.opensourcephysics.tools.Parameter;"/>
	 * <property name="function_editor" type="object">
	 * <object class="org.opensourcephysics.tools.UserFunctionEditor">
	 * <property name="main_functions" type="array" class=
	 * "[Lorg.opensourcephysics.tools.UserFunction;">
	 * <property name="[0]" type="object">
	 * <object class="org.opensourcephysics.tools.UserFunction">
	 * <property name="name" type="string">NaturalLog</property>
	 * <property name="name_editable" type="boolean">true</property>
	 * <property name="parameter_names" type="array" class="[Ljava.lang.String;"/>
	 * <property name="parameter_values" type="array" class="[D"/>
	 * <property name="parameter_descriptions" type="array" class=
	 * "[Ljava.lang.String;"/>
	 * <property name="variables" type="array" class="[Ljava.lang.String;">
	 * <property name="[0]" type="string">x</property> </property>
	 * <property name="expression" type="string">ln(x)</property> </object>
	 * </property> </property> </object> </property>
	 * <property name="autoload_off_NaturalLog" type="boolean">true</property>
	 * </object> </property> </property> </object>
	 *
	 * @param fitBuilderControl the XMLControl to modify
	 * @param filePath          the path to the XML file read by the XMLControl
	 */
	protected void eliminateExcludedFunctions(XMLControl fitBuilderControl, String filePath) {
		for (XMLProperty obj : fitBuilderControl.getPropsRaw()) {
			if (obj.getPropertyName().equals("functions")) { //$NON-NLS-1$
				// found fitFunctionPanels
				XMLProperty prop = (XMLProperty) obj;
				List<Object> items = prop.getPropertyContent();
				ArrayList<Object> toRemove = new ArrayList<Object>();
				XMLControl[] panels = prop.getChildControls();
				for (int i = 0; i < panels.length; i++) {
					XMLControl panelControl = panels[i];
					String name = panelControl.getString("name"); //$NON-NLS-1$
					if (isFunctionExcluded(filePath, name)) {
						toRemove.add(items.get(i));
					}
				}
				for (Object next : toRemove) {
					items.remove(next);
				}
			}
		}
	}

	/**
	 * Determines if a named function is excluded from autoloading.
	 *
	 * @param filePath     the path to the file defining the function
	 * @param functionName the function name
	 * @return true if the function is excluded
	 */
	private boolean isFunctionExcluded(String filePath, String functionName) {
		String[] functions = autoloadExclusionsMap.get(filePath);
		if (functions == null)
			return false;
		for (String name : functions) {
			if (name.equals("*")) //$NON-NLS-1$
				return true;
			if (name.equals(functionName))
				return true;
		}
		return false;
	}


	@Override 
	protected void setTitles() {
		if (getPanelNames().isEmpty()) {
			String label = ToolsRes.getString("FitFunctionPanel.Label"); //$NON-NLS-1$
			dropdownLabelText = (label + ":"); //$NON-NLS-1$
		} else {
			dropdownLabelText = null;
		}
	    titleText = ToolsRes.getString("DatasetCurveFitter.FitBuilder.Title");
	}
	
	/**
	 * Refreshes the GUI.
	 */
	@Override
	protected void refreshGUI() {
		if (!haveGUI())
			return;
		super.refreshGUI();
		if (deleteFitButton != null) {
			saveButton.setEnabled(!getPanelNames().isEmpty());
			loadButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.FitBuilder.Button.Load.Tooltip")); //$NON-NLS-1$
			saveButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.FitBuilder.Button.Save.Tooltip")); //$NON-NLS-1$
			FitFunctionPanel panel = (FitFunctionPanel) getSelectedPanel();
			deleteFitButton.setEnabled(panel != null && !getPanelNames().isEmpty() && panel.originalName == null);
			newFitButton.setText(ToolsRes.getString("DatasetCurveFitter.Button.NewFit.Text")); //$NON-NLS-1$
			newFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.NewFit.Tooltip")); //$NON-NLS-1$
			deleteFitButton.setText(ToolsRes.getString("DatasetCurveFitter.Button.DeleteFit.Text")); //$NON-NLS-1$
			deleteFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.DeleteFit.Tooltip")); //$NON-NLS-1$
			DatasetCurveFitter fitter = getSelectedCurveFitter();
			cloneFitButton.setEnabled(fitter != null);
			autoloadButton.setText(ToolsRes.getString("FitBuilder.Button.Autoload") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
			autoloadButton.setToolTipText(ToolsRes.getString("FitBuilder.Button.Autoload.Tooltip")); //$NON-NLS-1$
		}
	}

	/**
	 * Displays a dialog with a list of fit functions to load or save.
	 * 
	 * @param control     a FitBuilder XMLControl
	 * @param description a description of the purpose (ie load or save)
	 * @return true if not cancelled by the user
	 */
	protected void chooseFitFunctions(XMLControl control, String description, ActionListener listener) {
		// choose the elements and load the function tool
		ArrayList<XMLControl> originals = new ArrayList<XMLControl>();
		ArrayList<XMLControl> choices = new ArrayList<XMLControl>();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> expressions = new ArrayList<String>();
		for (XMLProperty prop : control.getPropsRaw()) {
			for (Object obj : prop.getPropertyContent()) {
				if (obj instanceof XMLProperty) {
					XMLProperty f = (XMLProperty) obj;
					XMLControl function = f.getChildControls()[0];
					originals.add(function);
					choices.add(function);
					names.add(function.getString("name")); //$NON-NLS-1$
					String desc = function.getString("description"); //$NON-NLS-1$
					expressions.add(desc);
				}
			}
		}
		// select all by default
		boolean[] selected = new boolean[choices.size()];
		for (int i = 0; i < selected.length; i++) {
			selected[i] = true;
		}
		ListChooser listChooser = new ListChooser(
				ToolsRes.getString("DatasetCurveFitter.FitBuilder." + description + ".Title"), //$NON-NLS-1$ //$NON-NLS-2$
				ToolsRes.getString("DatasetCurveFitter.FitBuilder." + description + ".Message"), //$NON-NLS-1$ //$NON-NLS-2$
				this, new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						if (e.getID() == ActionEvent.ACTION_PERFORMED) {
							// compare choices with originals and remove unwanted object content
							for (XMLControl next : originals) {
								if (!choices.contains(next)) {
									XMLProperty prop = next.getParentProperty();
									XMLProperty parent = prop.getParentProperty();
									parent.getPropertyContent().remove(prop);
								}
							}
						}
						listener.actionPerformed(e);
					}

				});
		listChooser.choose(choices, names, expressions, null, selected, null);
	}

	/**
	 * Adds a fit function panel.
	 * 
	 * @param f the fit function to add
	 * @return the fit function panel
	 */
	protected FitFunctionPanel addFitFunctionPanel(UserFunction f) {
		UserFunctionEditor editor = new UserFunctionEditor();
		editor.setMainFunctions(new UserFunction[] { f });
		FitFunctionPanel panel = new FitFunctionPanel(editor);
		addPanel(f.getName(), panel);
		return panel; 
	}

	/**
	 * Gets the autoload manager, creating it the first time called.
	 * 
	 * @return the autoload manageer
	 */
	protected AutoloadManager getAutoloadManager() {
		if (autoloadManager == null) {
			autoloadManager = new AutoloadManager(this);
			// center on screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - autoloadManager.getBounds().width) / 2;
			int y = (dim.height - autoloadManager.getBounds().height) / 2;
			autoloadManager.setLocation(x, y);
		}
		autoloadManager.setFontLevel(FontSizer.getLevel());
		return autoloadManager;
	}

	// -------------------------------- static methods ---------------------------

	public static String localize(String functionName) {
		String s = ToolsRes.getString("Function." + functionName + ".Name"); //$NON-NLS-1$ //$NON-NLS-2$
		return (s.startsWith("!") ? functionName : s); //$NON-NLS-1$
	}

	/**
	 * Gets the autoload search paths.
	 * 
	 * @return the search paths
	 */
	protected static Collection<String> getInitialSearchPaths() {
		if (initialAutoloadSearchPaths.isEmpty()) {
			if (preferredAutoloadSearchPaths != null) {
				for (String next : preferredAutoloadSearchPaths) {
					initialAutoloadSearchPaths.add(next);
				}
			} else {
				for (String next : OSPRuntime.getDefaultSearchPaths()) {
					initialAutoloadSearchPaths.add(next);
				}
			}
		}
		return initialAutoloadSearchPaths;
	}

	// -------------------------------- inner classes ---------------------------

	/**
	 * An AutoloadManager for DataFunctions.
	 */
	class AutoloadManager extends AbstractAutoloadManager {

		/**
		 * Constructor for a dialog.
		 * 
		 * @param dialog the dialog (DataBuilder)
		 */
		public AutoloadManager(JDialog dialog) {
			super(dialog);
		}

		@Override
		public void setVisible(boolean vis) {
			super.setVisible(vis);
			if (!vis) {
				// get name of currently selected fit
				String name = getSelectedDropdownName();
				// reload fits--will leave last loaded fit selected
				for (String dir : getSearchPaths()) {
					autoloadFits(dir);
				}
				// restore previously selected fit
				refreshDropdown(name);
				// save non-default search paths in preferredAutoloadSearchPaths
				Collection<String> searchPaths = getSearchPaths();
				Collection<String> defaultPaths = OSPRuntime.getDefaultSearchPaths();
				boolean isDefault = searchPaths.size() == defaultPaths.size();
				for (String next : searchPaths) {
					isDefault = isDefault && defaultPaths.contains(next);
				}
				if (isDefault) {
					preferredAutoloadSearchPaths = null;
				} else {
					preferredAutoloadSearchPaths = searchPaths.toArray(new String[searchPaths.size()]);
				}
				OSPRuntime.setPreference("autoload_search_paths", preferredAutoloadSearchPaths); //$NON-NLS-1$

				// clean up autoloadMap of excluded autoloadable functions
				for (Iterator<String> it = getExclusionsMap().keySet().iterator(); it.hasNext();) {
					String filePath = it.next();
					String parentPath = XML.getDirectoryPath(filePath);
					boolean keep = false;
					for (String dir : searchPaths) {
						keep = keep || parentPath.equals(dir);
					}
					if (!keep || !new File(filePath).exists()) {
						it.remove();
					}
				}

				// save excluded autoloadable functions
				if (getExclusionsMap().isEmpty()) {
					OSPRuntime.setPreference("autoload_exclusions", null); //$NON-NLS-1$
				} else {
					String[][] autoloadData = new String[getExclusionsMap().size()][];
					int i = 0;
					for (String filePath : getExclusionsMap().keySet()) {
						String[] functions = getExclusionsMap().get(filePath);
						String[] fileAndFunctions = new String[functions.length + 1];
						fileAndFunctions[0] = filePath;
						System.arraycopy(functions, 0, fileAndFunctions, 1, functions.length);
						autoloadData[i] = fileAndFunctions;
						i++;
					}
					OSPRuntime.setPreference("autoload_exclusions", autoloadData); //$NON-NLS-1$
				}

				OSPRuntime.savePreferences();
			}
		}

//		@Override
//		protected void setFunctionSelected(String filePath, String[] function, boolean select) {
//			super.setFunctionSelected(filePath, function, select);
//			// reload fits
//			for (String dir : getSearchPaths()) {
//				autoloadFits(dir);
//			}
//		}

//		@Override
//		protected void setFileSelected(String filePath, boolean select) {
//			super.setFileSelected(filePath, select);			
//			// reload fits
//			for (String dir : getSearchPaths()) {
//				autoloadFits(dir);
//			}
//		}
//
		/**
		 * Gets the collection of search paths.
		 * 
		 * @return the search paths
		 */
		@Override
		public Collection<String> getSearchPaths() {
			Collection<String> paths = super.getSearchPaths();
			if (paths.isEmpty() && !initialized) {
				initialized = true;
				for (String next : getInitialSearchPaths()) {
					paths.add(next);
					addSearchPath(next);
				}
			}
			return paths;
		}

		/**
		 * Refreshes the autoload data by reading all xml files it can find
		 */
		@Override
		protected void refreshAutoloadData() {
			final Map<String, Map<String, ArrayList<String[]>>> data = new TreeMap<String, Map<String, ArrayList<String[]>>>();
			for (String path : getSearchPaths()) {
				Map<String, ArrayList<String[]>> functionMap = findFitFunctions(path);
				data.put(path, functionMap);
			}
			setAutoloadData(data);
		}

		/**
		 * Refreshes the GUI.
		 */
		@Override
		protected void refreshGUI() {
			refreshAutoloadData();
			super.refreshGUI();
			String title = FitBuilder.this.getTitle() + " " + getTitle(); //$NON-NLS-1$
			setTitle(title);
			setInstructions(ToolsRes.getString("FitBuilder.Instructions.SelectToAutoload") //$NON-NLS-1$
					+ "\n\n" + ToolsRes.getString("FitBuilder.Instructions.WhereDefined") //$NON-NLS-1$ //$NON-NLS-2$
					+ " " + ToolsRes.getString("FitBuilder.Instructions.HowToAddFunction") //$NON-NLS-1$ //$NON-NLS-2$
					+ " " + ToolsRes.getString("FitBuilder.Instructions.HowToAddDirectory")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		@Override
		protected Map<String, String[]> getExclusionsMap() {
			return autoloadExclusionsMap;
		}

	}
}
