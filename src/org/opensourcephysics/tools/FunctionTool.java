/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.DataTable;
import org.opensourcephysics.display.TextFrame;
import org.opensourcephysics.media.core.Trackable;
import org.opensourcephysics.numerics.SuryonoParser;

/**
 * This tool allows users to create and manage editable Functions.
 * 
 * OSP DataBuilder, DataToolTab.dataBuilder, FitBuilder
 * 
 * Tracker ModelBuilder and TrackDataBuilder
 *
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
public class FunctionTool extends JDialog implements PropertyChangeListener {
	// static fields
	
	public static final String PROPERTY_FUNCTIONTOOL_PANEL = "panel";
	public static final String PROPERTY_FUNCTIONTOOL_VISIBLE = "ft_visible";
	public static final String PROPERTY_FUNCTIONTOOL_FUNCTION  = "function";

	
	public static class FTObject {

		public Icon icon; //[0]
		public String name; //[1]
		public String displayName; //[2]
		public Trackable track;
		
		public FTObject(Icon icon, String name, String displayName) {
			this.icon = icon;
			this.name = name;
			this.displayName = displayName;
		}

		public FTObject(Icon icon, Trackable track, String displayName) {
			this.icon = icon;
			this.track = track;
			this.displayName = displayName;
		}

		@Override
		public boolean equals(Object o) {
			FTObject fto = (FTObject) o;
			return fto != null 
					&& (name == null ? fto.name == null : name.equals(fto.name))
					&& (track == null ? fto.track == null : track.equals(fto.track))			
					&& (displayName == null ? fto.displayName == null : displayName.equals(fto.displayName))
					&& (icon == null ? fto.icon == null : icon.equals(fto.icon));
		}
	}

	public static String[] parserNames = new String[] { "e", "pi", "min", "mod", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"sin", "cos", "abs", "log", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"acos", "acosh", "ceil", "cosh", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"asin", "asinh", "atan", "atanh", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"exp", "frac", "floor", "int", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"random", "round", "sign", "sinh", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"step", "tanh", "atan2", "max", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"sqrt", "sqr", "if", "tan" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ ;
	public static String[] parserOperators = new String[] { "!", ",", ".", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"+", "-", "*", "/", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"^", "=", ">", "<", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"&", "|", "(", ")" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ ;
	// instance fields

	protected HashSet<String> forbiddenNames = new HashSet<String>();

	protected Set<DatasetCurveFitter> curveFitters = new HashSet<DatasetCurveFitter>();
	protected Map<String, FunctionPanel> trackFunctionPanels = new TreeMap<String, FunctionPanel>(); // maps name to FunctionPanel
	protected FunctionPanel selectedPanel;

	protected String helpPath = ""; //$NON-NLS-1$
	protected String helpBase = "https://www.compadre.org/online_help/tools/"; //$NON-NLS-1$
	protected ActionListener helpAction;
	protected int fontLevel = 0;
	protected boolean refreshing;

	// GUI

	// toolbars 
	
	private JPanel myContentPane = new JPanel(new BorderLayout());
	private JPanel noData;
	
	private JToolBar toolbar;
	private Component[] toolbarComponents; // may be null
	
	protected JToolBar dropdownbar;
	private JLabel dropdownLabel;
	private JComboBox<FTObject> dropdown;
	
	private JPanel buttonbar = new JPanel(new FlowLayout());
//	private JButton helpButton, closeButton, fontButton, undoButton, redoButton;
	private JButton helpButton, closeButton, undoButton, redoButton;

	private JPanel north = new JPanel(new BorderLayout());
	private JScrollPane selectedPanelScroller;

	private TextFrame helpFrame;
	private JDialog helpDialog;

	protected String dropdownTipText;
	protected String titleText;
	protected String dropdownLabelText;	
	
	private boolean haveGUI;
	private boolean isFitBuilder;

	// popup

//	private JPopupMenu popup;
//	private JPopupMenu getPopup() {
//		return (popup == null ? (popup = createPopup()) : popup);
//	}
//	private JMenuItem defaultFontSizeItem;

	
	/**
	 * Constructs a tool for the specified component (may be null)
	 *
	 * @param comp Component used to get Frame owner of this Dialog
	 */
	public FunctionTool(Component comp) {
		this(comp, false, false);
	}
	
	/**
	 * Constructor allowing for a fitBuilder flag and lazyGUI option
	 * @param comp
	 * @param isFitBuilder
	 * @param lazyGUI
	 */
	public FunctionTool(Component comp, boolean isFitBuilder, boolean lazyGUI) {

		
		
		// modal if no owner (ie if comp is null)
		super(JOptionPane.getFrameForComponent(comp), comp == null);
		this.isFitBuilder = isFitBuilder;
		addForbiddenNames(parserNames);
		addForbiddenNames(UserFunction.dummyVars);
		setName("FunctionTool"); //$NON-NLS-1$
		init();
		if (!lazyGUI)
			createGUI();
	}

	public void checkGUI() {
		if (haveGUI)
			return;
		createGUI();
		refreshDropdown(null);
		refreshGUI();
	}

	protected void init() {

	}

	protected boolean haveGUI() {
		return haveGUI;
	}

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		haveGUI = true;
		// listen to ToolsRes for locale changes
		ToolsRes.addPropertyChangeListener(ToolsRes.OSP_PROPERTY_LOCALE, this); //$NON-NLS-1$
		// configure the dialog
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		// create the noData panel
		noData = new JPanel(new BorderLayout());
		// create the toolbars
		dropdownbar = new JToolBar();
		dropdownbar.setFloatable(false);
		dropdownLabel = new JLabel();
		dropdownLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 2));
		dropdownLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				String name = getSelectedName();
				if (name != null) {
					FunctionPanel panel = trackFunctionPanels.get(name);
					panel.clearSelection();
				}
			}

		});
		dropdownbar.add(dropdownLabel);
		// create dropdown
		dropdown = new JComboBox<FTObject>() {
			// override getMaximumSize method so has same height as buttons
			@Override
			public Dimension getMaximumSize() {
				Dimension dim = super.getMaximumSize();
			if (toolbarComponents != null
						&& toolbarComponents.length > 0 & toolbarComponents[0] instanceof JButton) {
					JButton button = (JButton) toolbarComponents[0];
					dim.height = button.getHeight();
				}
				return dim;
			}

			// override addItem method to alphabetize added items
			@Override
			public void addItem(FTObject obj) {
				if (obj == null)
					return;
				int count = getItemCount();
				for (int i = 0; i < count; i++) {
					if (obj.equals(getItemAt(i)))
						return;
				}
				// add in alphabetical order, ignoring case
				String displayName = obj.displayName;
				//array.length > 2 ? (String) array[2] : (String) array[1];
				// substitute localized name if this tool is a FitBuilder
				if (isFitBuilder) {
					displayName = FitBuilder.localize(displayName);
				}

				for (int i = 0; i < count; i++) {
					FTObject fto = getItemAt(i);
					String dname = fto.displayName;
					// substitute localized name if this tool is a FitBuilder
					if (isFitBuilder) {
						dname = FitBuilder.localize(dname);
					}
					if (displayName.compareToIgnoreCase(dname) < 0) {
						// next comes after name, so insert object here
						insertItemAt(obj, i);
						return;
					}
				}
				super.addItem(obj);
			}

		};
		dropdown.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
		dropdownbar.add(dropdown);
		// custom cell renderer for dropdown items
		DropdownRenderer renderer = new DropdownRenderer();
		dropdown.setRenderer(renderer);
		dropdown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FTObject item = (FTObject) dropdown.getSelectedItem();
				if (item != null) {
					String name = item.name;
					select(name);
					FunctionPanel panel = trackFunctionPanels.get(name);
					if (panel != null && panel.haveGUI()) {
						panel.getFunctionTable().clearSelection();
						panel.getFunctionTable().selectOnFocus = false;
						panel.getParamTable().clearSelection();
						panel.getParamTable().selectOnFocus = false;
						panel.refreshGUI();
					}
				}
				helpButton.requestFocusInWindow();
			}
		});
		// create toolbar for custom components
		toolbar = new JToolBar();
		toolbar.setFloatable(false);

		north.add(dropdownbar, BorderLayout.SOUTH);
		north.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		// create buttons
		closeButton = new JButton(ToolsRes.getString("Tool.Button.Close")); //$NON-NLS-1$
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		helpAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (helpFrame == null) {
					// show web help if available
					String help = XML.getResolvedPath(helpPath, helpBase);
					if (ResourceLoader.getResource(help) != null) {
						helpFrame = new TextFrame(help);
					} else {
						String classBase = "/org/opensourcephysics/resources/tools/html/"; //$NON-NLS-1$
						help = XML.getResolvedPath(helpPath, classBase);
						helpFrame = new TextFrame(help);
					}
					// create help dialog
					helpDialog = new JDialog(FunctionTool.this, false);
					helpDialog.setContentPane(helpFrame.getContentPane());
					helpDialog.setSize(700, 550);
					// center on the screen
					Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
					int x = (dim.width - helpDialog.getBounds().width) / 2;
					int y = (dim.height - helpDialog.getBounds().height) / 2;
					helpDialog.setLocation(x, y);
				}
				helpDialog.setVisible(true);
			}

		};
		helpButton = new JButton(ToolsRes.getString("Tool.Button.Help")); //$NON-NLS-1$
		helpButton.addActionListener(helpAction);
		undoButton = new JButton(ToolsRes.getString("DataFunctionPanel.Button.Undo")); //$NON-NLS-1$
		redoButton = new JButton(ToolsRes.getString("DataFunctionPanel.Button.Redo")); //$NON-NLS-1$
//		// create font sizer button and popup
//		fontButton = new JButton(ToolsRes.getString("Tool.Menu.FontSize")); //$NON-NLS-1$
//
//		fontButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				getPopup().show(fontButton, 0, fontButton.getHeight());
//			}
//
//		});
		// prepare button bar
		buttonbar.setBorder(BorderFactory.createEtchedBorder());
		buttonbar.add(helpButton);
		buttonbar.add(undoButton);
		buttonbar.add(redoButton);
//		buttonbar.add(fontButton);
		buttonbar.add(closeButton);
		myContentPane.add(north, BorderLayout.NORTH);
		myContentPane.add(noData, BorderLayout.CENTER);
		myContentPane.add(buttonbar, BorderLayout.SOUTH);
		setContentPane(myContentPane);
		Dimension dim = getPreferredSize();
		setPreferredSize(new Dimension(dim.width, Math.max(400, dim.height))); // BH was 360, but that was too short
		pack();
		buttonbar.remove(undoButton);
		buttonbar.remove(redoButton);
//		buttonbar.remove(fontButton);
		dropdown.setEnabled(false);
		dropdownLabel.setEnabled(false);
		// center this on the screen
		dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (dim.width - getBounds().width) / 2;
		int y = (dim.height - getBounds().height) / 2;
		setLocation(x, y);
	}

//	private JPopupMenu createPopup() {
//		popup = new JPopupMenu();
//		FontSizer.setFonts(popup); // BH needs testing
//		ButtonGroup group = new ButtonGroup();
//		Action fontSizeAction = new AbstractAction() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				int i = Integer.parseInt(e.getActionCommand());
//				setFontLevel(i);
//			}
//
//		};
//		for (int i = 0; i < 4; i++) {
//			JMenuItem item = new JRadioButtonMenuItem("+" + i); //$NON-NLS-1$
//			if (i == 0) {
//				defaultFontSizeItem = item;
//			}
//			item.addActionListener(fontSizeAction);
//			item.setActionCommand("" + i); //$NON-NLS-1$
//			popup.add(item);
//			group.add(item);
//			if (i == fontLevel) {
//				item.setSelected(true);
//			}
//		}
//		defaultFontSizeItem.setText(ToolsRes.getString("Tool.MenuItem.DefaultFontSize")); //$NON-NLS-1$
//		return popup;
//	}

	protected void setTitles() {
		// subclasses only will do this
	}

	/**
	 * Refreshes the GUI.
	 */
	protected void refreshGUI() {
		// refresh toolbar
		if (!haveGUI)
			return;
		if (toolbarComponents == null) {
			north.remove(toolbar);
		} else {
			north.add(toolbar, BorderLayout.NORTH);
			toolbar.removeAll();
			for (int i = 0; i < toolbarComponents.length; i++) {
				toolbar.add(toolbarComponents[i]);
			}
		}
		if (dropdownLabelText != null) {
			dropdownLabel.setText(dropdownLabelText);
		} else if (selectedPanel != null) {
			String label = selectedPanel.getLabel();
			dropdownLabel.setText(label + ":"); //$NON-NLS-1$
		}
		if (dropdownTipText != null)
			dropdown.setToolTipText(dropdownTipText);
		if (titleText != null)
			setTitle(titleText);
		closeButton.setText(ToolsRes.getString("Tool.Button.Close")); //$NON-NLS-1$
		closeButton.setToolTipText(ToolsRes.getString("Tool.Button.Close.ToolTip")); //$NON-NLS-1$
		helpButton.setText(ToolsRes.getString("Tool.Button.Help")); //$NON-NLS-1$
		helpButton.setToolTipText(ToolsRes.getString("Tool.Button.Help.ToolTip")); //$NON-NLS-1$
//		fontButton.setText(ToolsRes.getString("Tool.Menu.FontSize")); //$NON-NLS-1$
//		fontButton.setToolTipText(ToolsRes.getString("FunctionTool.Button.Display.Tooltip")); //$NON-NLS-1$
		Iterator<FunctionPanel> it = trackFunctionPanels.values().iterator();
		while (it.hasNext()) {
			FunctionPanel panel = it.next();
			panel.refreshGUI();
		}
		Dimension dim = getSize();
		dim.width = Math.max(dim.width, getMinimumSize().width);
		setSize(dim);
		helpButton.requestFocusInWindow();
	}

	/**
	 * Sets the custom buttons or other components.
	 *
	 * @param toolbarItems an array of components (may be null)
	 */
	public void setToolbarComponents(Component[] toolbarItems) {
		toolbarComponents = toolbarItems;
		refreshGUI();
	}

	/**
	 * Gets the custom buttons or other components.
	 *
	 * @return an array of components (may be null)
	 */
	public Component[] getToolbarComponents() {
		return toolbarComponents;
	}

	/**
	 * Adds a FunctionPanel.
	 *
	 * @param name  a descriptive name
	 * @param panel the FunctionPanel
	 * @return the added panel
	 */
	public void addPanel(String name, FunctionPanel panel) {
		//System.out.println("FunctionTool.addPanel " + name); //$NON-NLS-1$
		panel.setFontLevel(fontLevel);
		panel.setName(name);
		panel.setFunctionTool(this);
		trackFunctionPanels.put(name, panel);
		panel.addForbiddenNames(forbiddenNames.toArray(new String[0]));
		panel.clearSelection();
		if (this.isVisible())
			refreshDropdown(name);
	}

	/**
	 * Removes a named FunctionPanel.
	 *
	 * @param name the name
	 * @return the removed panel, if any
	 */
	public FunctionPanel removePanel(String name) {
		FunctionPanel panel = trackFunctionPanels.get(name);
		if (panel != null) {
			//OSPLog.finest("removing panel " + name); //$NON-NLS-1$
			trackFunctionPanels.remove(name);
			refreshDropdown(null);
			firePropertyChange(PROPERTY_FUNCTIONTOOL_PANEL, panel, null); // $NON-NLS-1$
			// dispose of panel AFTER firing property change
			panel.dispose();
		}
		return panel;
	}

	/**
	 * Renames a FunctionPanel.
	 *
	 * @param prevName the previous name
	 * @param newName  the new name
	 * @return the renamed panel
	 */
	public FunctionPanel renamePanel(String prevName, String newName) {
		FunctionPanel panel = getPanel(prevName);
		if ((panel == null) || prevName.equals(newName)) {
			return panel;
		}
		//OSPLog.finest("renaming panel " + prevName + " to " + newName); //$NON-NLS-1$ //$NON-NLS-2$
		trackFunctionPanels.remove(prevName);
		trackFunctionPanels.put(newName, panel);
		panel.prevName = prevName;
		panel.setName(newName);
		refreshDropdown(newName);
		return panel;
	}

	/**
	 * Selects a FunctionPanel by name.
	 *
	 * @param name the name
	 */
	public void setSelectedPanel(String name) {
		if (!haveGUI())
			return;
		//System.out.println("FunctionTool.setSelectedPanel " + name + " " + dropdown.getSelectedItem());
		FTObject item = getDropdownItem(name);
		if (item != null)
			dropdown.setSelectedItem(item);
	}

	/**
	 * Returns the name of the selected FunctionPanel.
	 *
	 * @return the name
	 */
	public String getSelectedName() {
		if (selectedPanel == null) {
			return null;
		}
		Iterator<String> it = trackFunctionPanels.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (trackFunctionPanels.get(name) == selectedPanel) {
				return name;
			}
		}
		return null;
	}

	/**
	 * Returns the selected FunctionPanel.
	 *
	 * @return the FunctionPanel
	 */
	public FunctionPanel getSelectedPanel() {
		String name = getSelectedName();
		FunctionPanel panel = getPanel(name);
		if (panel == null && name != null) {
			refreshDropdown(name);
			panel = getPanel(name);
		}
		return panel;
	}

	/**
	 * Returns the named FunctionPanel.
	 *
	 * @param name the name
	 * @return the FunctionPanel
	 */
	public FunctionPanel getPanel(String name) {
		return (name == null) ? null : trackFunctionPanels.get(name);
	}

	/**
	 * Returns the set of all panel names.
	 *
	 * @return a set of names
	 */
	public Set<String> getPanelNames() {
		return trackFunctionPanels.keySet();
	}

	/**
	 * Clears all FunctionPanels.
	 */
	public void clearPanels() {
		//OSPLog.finest("clearing panels"); //$NON-NLS-1$
		trackFunctionPanels.clear();
		refreshDropdown(null);
	}

	/**
	 * Responds to property change events from TrackerPanel.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (!haveGUI())
			return;
		refreshGUI();
		if (isFitBuilder) {
			// refresh dropdown since localized names change
			refreshDropdown(null);
		}
	}

	/**
	 * Adds names to the forbidden set.
	 * 
	 * @param names the names to add
	 */
	public void addForbiddenNames(String[] names) {
		for (int i = 0; i < names.length; i++) {
			forbiddenNames.add(names[i]);
		}
	}

	/**
	 * Overrides JDialog setVisible method.
	 *
	 * @param vis true to show this tool
	 */
	@Override
	public void setVisible(boolean vis) {
		if (vis == isVisible())
			return;
		if (vis) {
			checkGUI();
			setFontLevel(FontSizer.getLevel());
			refreshDropdown(null);
		} else if (!haveGUI()) {
			return;
		}
		Container top = myContentPane.getTopLevelAncestor();
		if (top == null || top == this)
			super.setVisible(vis);
		else
			top.setVisible(vis);
		firePropertyChange(PROPERTY_FUNCTIONTOOL_VISIBLE, null, Boolean.valueOf(vis)); // $NON-NLS-1$
	}

	/**
	 * Overrides JDialog isVisible method.
	 *
	 * @return true if visible
	 */
	@Override
	public boolean isVisible() {
		Container top = myContentPane.getTopLevelAncestor();
		return (top == null ? false : top == this ? super.isVisible() : top.isVisible());
	}

	/**
	 * Sets the path of the help file.
	 *
	 * @param path a filename or url
	 */
	public void setHelpPath(String path) {
		helpPath = path;
	}

	/**
	 * Sets the help action. this will replace the current help action
	 *
	 * @param action a custom help action
	 */
	public void setHelpAction(ActionListener action) {
		helpButton.removeActionListener(helpAction);
		helpAction = action;
		helpButton.addActionListener(helpAction);
	}

	/**
	 * Reports if this is empty.
	 *
	 * @return true if empty
	 */
	public boolean isEmpty() {
		return trackFunctionPanels.isEmpty();
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the level
	 */
	public void setFontLevel(int level) {
		level = Math.max(0, level);
		if (level == fontLevel) {
			return;
		}
		fontLevel = level;
		boolean vis = isVisible();
		setVisible(false);
		FontSizer.setFonts(this, level);
		FontSizer.setFonts(myContentPane, level);
//		FontSizer.setFonts(fontButton, level);
		for (Iterator<FunctionPanel> it = trackFunctionPanels.values().iterator(); it.hasNext();) {
			FunctionPanel next = it.next();
			if (next == getSelectedPanel()) {
				continue;
			}
			next.setFontLevel(level);
		}
//		if (popup != null && level < popup.getSubElements().length) {
//			MenuElement[] e = popup.getSubElements();
//			JRadioButtonMenuItem item = (JRadioButtonMenuItem) e[level];
//			item.setSelected(true);
//		}

		int n = dropdown.getSelectedIndex();
		FTObject[] items = new FTObject[dropdown.getItemCount()];
		for (int i = 0; i < items.length; i++) {
			items[i] = dropdown.getItemAt(i);
		}
		DefaultComboBoxModel<FTObject> model = new DefaultComboBoxModel<>(items);
		dropdown.setModel(model);
		dropdown.setSelectedIndex(n);

		java.awt.Container c = myContentPane.getTopLevelAncestor();
		Dimension dim = c.getSize();
		dim.width = c.getMinimumSize().width;
		int h = (int) (280 * FontSizer.getFactor(level));
		h = Math.max(h, dim.height);
		h = Math.min(h, (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		dim.height = h;
		setSize(dim);
		c.setSize(dim);
		setVisible(vis);
		refreshDropdown(null);
	}

	/**
	 * Gets the font level.
	 *
	 * @return the level
	 */
	public int getFontLevel() {
		return fontLevel;
	}

	/**
	 * Sets the independent variables of all function panels.
	 *
	 * @param vars the independent variable names
	 */
	public void setDefaultVariables(String[] vars) {
		for (String name : getPanelNames()) {
			FunctionPanel panel = getPanel(name);
			UserFunctionEditor editor = (UserFunctionEditor) panel.getFunctionEditor();
			editor.setDefaultVariables(vars);
			editor.repaint();
		}
	}
	
	/**
	 * Fires a property change. This makes this method visible to the tools package.
	 */
	@Override
	protected void firePropertyChange(String name, Object oldObj, Object newObj) {
		//System.out.println("FunctionTool firing " + name);
		super.firePropertyChange(name, oldObj, newObj);
	}

	/**
	 * Gets the dropdown item, if any, with the specified name.
	 * 
	 * @param name the name
	 * @return the dropdown item
	 */
	private FTObject getDropdownItem(String name) {
		for (int i = 0; i < dropdown.getItemCount(); i++) {
			FTObject item = dropdown.getItemAt(i);
			String itemName = item.name;
			if (itemName.equals(name))
				return item;
		}
		return null;
	}

	/**
	 * Gets the selected dropdown item name.
	 * 
	 * @return the selected name
	 */
	protected String getSelectedDropdownName() {
		FTObject item = (FTObject) dropdown.getSelectedItem();
		if (item != null)
			return item.name;
		return null;
	}

	/**
	 * Refreshes the dropdown and selects a specified panel. If name is null, the
	 * current selection is retained if possible.
	 * 
	 * @param name the name of the panel to select
	 */
	public void refreshDropdown(String name) {
		if (!haveGUI())
			return;
		refreshing = true; // prevents selecting items while adding to dropdown
		if (name == null) {
			FTObject item = (FTObject) dropdown.getSelectedItem();
			if (item != null)
				name = item.name;
		}
		FTObject toSelect = null;
		dropdown.removeAllItems();
		for (String next : trackFunctionPanels.keySet()) {
			FunctionPanel panel = trackFunctionPanels.get(next);
			String displayName = panel.getDisplayName();
			Icon icon = panel.getIcon();
			FTObject item = new FTObject(icon, next, displayName);
			dropdown.addItem(item);
			if (toSelect == null || next.equals(name)) {
				toSelect = item;
			}
		}
		refreshing = false;
		// select desired item
		if (toSelect != null) {
			dropdown.setSelectedItem(toSelect);
		} else {
			select(null);
		}
		if (dropdownLabelText != null)
			dropdownLabel.setText(dropdownLabelText);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				dropdown.revalidate();
				helpButton.requestFocusInWindow();
			}
		});
	}

	/**
	 * Selects the named function panel.
	 * 
	 * @param name the name
	 */
	private void select(String name) {
		if (refreshing)
			return;
		FunctionPanel panel = (name == null) ? null : trackFunctionPanels.get(name);
		FunctionPanel prev = selectedPanel;
		if (selectedPanel != null) {
			myContentPane.remove(selectedPanelScroller);
		} else {
			myContentPane.remove(noData);
		}
		selectedPanel = panel;
		dropdown.setEnabled(panel != null);
		dropdownLabel.setEnabled(panel != null);
		if (panel != null) {
			selectedPanelScroller = new JScrollPane(panel);
			myContentPane.add(selectedPanelScroller, BorderLayout.CENTER);
			panel.refreshGUI();
		} else {
			myContentPane.add(noData, BorderLayout.CENTER);
			buttonbar.removeAll();
			buttonbar.add(helpButton);
			buttonbar.add(closeButton);
		}
		java.awt.Container c = myContentPane.getTopLevelAncestor();
		c.validate();
		refreshGUI();
		c.repaint();
		firePropertyChange(PROPERTY_FUNCTIONTOOL_PANEL, prev, panel); // $NON-NLS-1$
	}
	
	/**
	 * Gets a unique name.
	 *
	 * @param proposedName the proposed name
	 * @return the unique name
	 */
	protected String getUniqueName(String proposedName) {
		// construct a unique name from that proposed by adding trailing digit
		int i = 0;
		String name = proposedName;
		// special case for new fits defined by DatasetCurveFitter.fitBuilder
		if (ToolsRes.getString("DatasetCurveFitter.NewFit.Name").equals(proposedName)) { //$NON-NLS-1$
			i++;
			name = name + i;
		}
		while (trackFunctionPanels.keySet().contains(name) || forbiddenNames.contains(name)) {
			i++;
			name = proposedName + i;
		}
		return name;
	}

	/**
	 * Custom renderer to show name and icon in dropdown list items
	 */
	public class DropdownRenderer extends JLabel implements ListCellRenderer<Object> {

		public DropdownRenderer() {
			setOpaque(true);
			setHorizontalAlignment(LEFT);
			setVerticalAlignment(CENTER);
			setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 0));
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			if (value != null) {
				FTObject obj = (FTObject) value;
				setIcon(obj.icon);
				String displayName = obj.displayName;
				// substitute localized name if this tool is a FitBuilder
				if (isFitBuilder) {
					displayName = FitBuilder.localize(displayName);
				}
				setText(displayName);
			} else {
				setIcon(null);
				setText(null);
			}
			return this;
		}
	}

	// __________________________ static methods ___________________________

	/**
	 * Returns an ObjectLoader to save and load data for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load data for this class.
	 */
	static class Loader implements XML.ObjectLoader {
		@Override
		public void saveObject(XMLControl control, Object obj) {
			FunctionTool tool = (FunctionTool) obj;
			ArrayList<FunctionPanel> functions = new ArrayList<FunctionPanel>(tool.trackFunctionPanels.values());
			control.setValue("functions", functions); //$NON-NLS-1$
		}

		@Override
		public Object createObject(XMLControl control) {
			return null;
		}

		@Override
		public Object loadObject(XMLControl control, Object obj) {
			FunctionTool tool = (FunctionTool) obj;
			ArrayList<?> functions = (ArrayList<?>) control.getObject("functions"); //$NON-NLS-1$
			if (functions != null) {
				for (Iterator<?> it = functions.iterator(); it.hasNext();) {
					FunctionPanel panel = (FunctionPanel) it.next();
					tool.addPanel(panel.getName(), panel);
				}
			}
			return obj;
		}

	}

	public void setButtonBar(Object[] btns) {
		if (!haveGUI)
			return;
		buttonbar.removeAll();
		for (Object btn : btns) {
			if (btn instanceof String) {
				switch ((String) btn) {
				case "help":
					btn = helpButton;
					break;
				case "close":
					btn = closeButton;
					break;
//				case "font":
//					btn = fontButton;
//					break;
				}
			}
			buttonbar.add((Component) btn);
		}
	}

	public boolean hasButton(JButton btn) {
		return (btn != null && btn.getParent() == buttonbar);
	}

	public void focusHelp() {
		if (haveGUI)
			helpButton.requestFocusInWindow();
	}

	@Override
	public void dispose() {
		if (helpDialog != null) {
			helpDialog.dispose();
		}
		super.dispose();
	}

	public static boolean arrayContains(String[] s, String name) {
		for (int i = s.length; --i >= 0;) {
			if (s[i].equals(name))
				return true;
		}
		return false;
	}

	/**
	 * Returns true if name is reserved by the OSP parser.
	 *
	 * @param name the proposed name
	 * @return true if reserved
	 */
	public static boolean isReservedName(String name) {
		// check for localized "row" name
		// check for parser terms
		return (DataTable.rowName.equals(name) || FunctionTool.arrayContains(parserNames, name)
				|| FunctionTool.arrayContains(UserFunction.dummyVars, name) || !Double.isNaN(SuryonoParser.getNumber(name)));
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
