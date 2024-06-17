/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.opensourcephysics.controls.ControlsRes;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.controls.XMLProperty;
import org.opensourcephysics.controls.XMLTree;
import org.opensourcephysics.controls.XMLTreeChooser;
import org.opensourcephysics.desktop.OSPDesktop;
import org.opensourcephysics.display.Data;
import org.opensourcephysics.display.DataFunction;
import org.opensourcephysics.display.DataTable;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.display.DisplayColors;
import org.opensourcephysics.display.DisplayRes;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPFrame;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.TeXParser;
import org.opensourcephysics.display.TextFrame;
import org.opensourcephysics.media.core.VideoIO;

import javajs.async.AsyncDialog;

/**
 * This provides a GUI for analyzing OSP Data objects.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DataTool extends OSPFrame implements Tool, PropertyChangeListener {

	boolean standAlone = false; // true when running as application

	// static fields
	@SuppressWarnings("javadoc")
	public static boolean loadClass = false;
	protected static Dimension dim = new Dimension(800, 540);
	protected static final int defaultButtonHeight = 28;
	protected static int buttonHeight = defaultButtonHeight;
	protected static final String SPACE = " ";
	protected static String[] delimiters = new String[] { "\t", ",", ";", SPACE }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	protected static TextFrame helpFrame;
//  protected static String helpName = "data_tool_help.html";                                 //$NON-NLS-1$
	protected static String helpName = "datatool/datatool_help.html"; //$NON-NLS-1$
	protected static String helpBase = "https://www.compadre.org/osp/online_help/tools/"; //$NON-NLS-1$
	private static ArrayList<Data> processedData = new ArrayList<Data>();
	protected static boolean loadMultipleTracksInSingleTab = false;
	protected static boolean askToLoadMultipleTracksInSingleTab = true;

	// instance fields
	protected JTabbedPane tabbedPane;
	protected boolean useChooser = true;
	protected JPanel contentPane = new JPanel(new BorderLayout());
	protected PropertyChangeSupport support;
	protected XMLControlElement control = new XMLControlElement();
	protected Data addableData = null;
	protected boolean controlContainsData;
	protected JMenuBar emptyMenubar;
	protected JMenu emptyFileMenu;
	protected JMenuItem emptyNewTabItem;
	protected JMenuItem emptyOpenItem;
	protected JMenuItem emptyExitItem;
	protected JMenu emptyEditMenu;
	protected JMenuItem emptyPasteMenu;
	protected JMenuItem emptyPasteTabItem;
	protected JMenuBar menubar;
	protected JMenu fileMenu;
	protected JMenuItem newTabItem;
	protected JMenuItem openItem;
	protected JMenuItem importItem;
	protected JMenuItem exportItem;
	protected JMenuItem saveItem;
	protected JMenuItem saveAsItem;
	protected JMenuItem closeItem;
	protected JMenuItem closeAllItem;
	protected JMenuItem printItem;
	protected JMenuItem exitItem;
	protected JMenu editMenu;
	protected JMenuItem undoItem;
	protected JMenuItem redoItem;
	protected JMenu copyMenu;
	protected JMenuItem copyImageItem;
	protected JMenuItem copyTabItem;
	protected JMenu copyDataMenu, setDelimiterMenu;
	protected JMenuItem copyDataAsFormattedItem, copyDataRawItem;
	protected JMenu pasteMenu;
	protected JMenuItem pasteTabItem;
	protected JMenuItem pasteColumnsItem;
	protected JMenu displayMenu;
	protected JMenu languageMenu;
	protected JMenuItem[] languageItems;
	protected JMenu fontSizeMenu;
	protected JMenuItem defaultFontSizeItem;
	protected ButtonGroup fontSizeGroup;
	protected JMenu helpMenu;
	protected JMenuItem helpItem;
	protected JMenuItem logItem;
	protected JMenuItem aboutItem;
	protected DataBuilder dataBuilder;
	protected boolean exitOnClose = false;
	protected boolean saveChangesOnClose = false;
	protected FitBuilder fitBuilder;
	protected boolean isLoading = false;
	protected JButton loadDataFunctionsButton, saveDataFunctionsButton;
	protected boolean slopeExtended = false;
	protected int myPopupFontLevel;
	protected int myCopyMenuFontLevel;

	private MenuListener editMenuChecker = new MenuListener() {

		@Override
		public void menuSelected(MenuEvent e) {
			DataToolTab tab = getSelectedTab();
			// undo and redo items
			if (tab != null) {
				undoItem.setEnabled(tab.undoManager.canUndo());
				redoItem.setEnabled(tab.undoManager.canRedo());
			}
			// enable paste menu if clipboard contains pastable data
			boolean enabled = hasPastableData();
			emptyPasteMenu.setEnabled(enabled);
			pasteMenu.setEnabled(enabled);
			// prepare copy menu
			copyMenu.removeAll();
			if (tab != null) {
				boolean isEmpty = tab.dataManager.getDatasetsRaw().isEmpty();
				copyDataMenu.setEnabled(!isEmpty);
				if (!isEmpty) {
					copyTabItem.setText(ToolsRes.getString("DataTool.MenuItem.CopyTab")); //$NON-NLS-1$
					copyMenu.add(copyTabItem);
					copyMenu.addSeparator();
					String s = ToolsRes.getString("DataTool.MenuItem.CopyData"); //$NON-NLS-1$
					int[] selectedRows = getSelectedTab().dataTable.getSelectedRows();
					int endRow = getSelectedTab().dataTable.getRowCount() - 1;
					boolean emptySelection = (selectedRows.length == 1) && (selectedRows[0] == endRow)
							&& getSelectedTab().dataTable.isEmptyRow(endRow);
					if ((selectedRows.length > 0) && !emptySelection) {
						s = ToolsRes.getString("DataTool.MenuItem.CopySelectedData"); //$NON-NLS-1$
					}
					copyDataMenu.setText(s);
					copyDataMenu.removeAll();
					copyDataRawItem.setText(ToolsRes.getString("DataTool.MenuItem.Unformatted"));
					copyDataAsFormattedItem.setText(ToolsRes.getString("DataTool.MenuItem.Formatted"));
					setDelimiterMenu.setText(ToolsRes.getString("DataTool.Menu.SetDelimiter")); //$NON-NLS-1$
					copyDataMenu.add(copyDataAsFormattedItem);
					copyDataMenu.add(copyDataRawItem);
					copyDataMenu.addSeparator();
					copyDataMenu.add(setDelimiterMenu);

					copyMenu.add(copyDataMenu);
					copyMenu.addSeparator();
				}
			}
			copyMenu.add(copyImageItem);
			FontSizer.setFonts(copyMenu);
		}

		@Override
		public void menuDeselected(MenuEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void menuCanceled(MenuEvent e) {
			// TODO Auto-generated method stub

		}
	};

	private JMenuItem editDataItem;

	/**
	 * A shared data tool.
	 */
	private static DataTool tool;

	public static DataTool getTool() {
		return getTool(true);
	}

	/**
	 * Gets the shared DataTool.
	 *
	 * @return the shared DataTool
	 */
	public static DataTool getTool(boolean forceNew) {
		return (tool == null && forceNew ? (tool = new DataTool()) : tool);
	}

	/*
	 * @Override public void setName(String name) { // set custom name
	 * super.setName("DataToolTest"); }
	 */

	/**
	 * Main entry point when used as application.
	 *
	 * @param args args[0] may be a data or xml file name
	 */
	public static void main(String[] args) {
		getTool(true);
		if (OSPRuntime.isJS)
			tool.standAlone = true;
		if (tool.standAlone)
			tool.setLocation(0, 0);
		OSPRuntime.setAppClass(tool);
		tool.exitOnClose = true;
		tool.saveChangesOnClose = true;
		if ((args != null) && (args.length > 0) && (args[0] != null)) {
			tool.setVisible(true);
			tool.open(new File(args[0]));
		} else {
			tool.addWindowListener(new WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent e) {
					if (tool.getTabCount() == 0) {
						DataToolTab tab = tool.createTab(null);
						tab.setUserEditable(true);
						tool.addTab(tab);
					}
				}
			});
			tool.setVisible(true);
		}

	}

	/**
	 * Constructs a blank DataTool.
	 */
	public DataTool() {
		this(ToolsRes.getString("DataTool.Frame.Title"), "DataTool"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Constructs a DataTool and opens the specified xml file.
	 *
	 * @param fileName the name of the xml file
	 */
	public DataTool(String fileName) {
		this();
		open(new File(fileName));
	}

	/**
	 * Constructs a DataTool and loads data from an xml control.
	 *
	 * @param control the xml control
	 */
	public DataTool(XMLControl control) {
		this();
		addTabs(control, null);
	}

	/**
	 * Constructs a DataTool and loads the specified data object.
	 *
	 * @param data the data
	 */
	public DataTool(Data data) {
		this();
		ArrayList<DataToolTab> tabs = createTabs(data);
		for (DataToolTab tab : tabs) {
			addTab(tab);
		}
	}
	
	/**
	 * Replace any open tabs with a single tab loaded with the given path.
	 * JavaScript only?
	 * 
	 * @j2sAlias loadDatasetURL
	 * 
	 * @param path
	 */
	public void loadDatasetURL(String path) {
		if (getTabCount() > 0)
			removeAllTabs();
		File f= new File(path);
		open(f);
		
	}
	
	public static String NEW_LINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Replace any open tabs with a single tab loaded with the given path.
	 * JavaScript only?
	 * 
	 * @j2sAlias loadDatasetURI
	 * 
	 * @param relpath
	 */
	@SuppressWarnings("null")
	public void loadDatasetURI(String relpath) {
		if (getTabCount() > 0)
			removeAllTabs();

		String baseURI = (/** @j2sNative document.body.baseURI || */
		null); // html page that has script
		String rootpath = (baseURI == null) ? "" : baseURI.substring(0, baseURI.lastIndexOf('/') + 1);
		String path = rootpath + relpath;

		try {
			System.err.println("Debugging: reading path=" + path);
			URL url = new URL(path);
			Object content = url.getContent();
			if (content instanceof InputStream) {
				InputStream is = (InputStream) content;
				BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) content));
				StringBuffer buffer = new StringBuffer(0);
				String line;
				while ((line = reader.readLine()) != null) {
					buffer.append(line + NEW_LINE);
				}
				String dataset = buffer.toString();
				System.out.print(dataset); // for testing to see what was read
				System.err.println("Debugging: Close stream \n");
				loadDataset(dataset, path);
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Gets the main OSPFrame when DataTool is a stand alone application.
	 * 
	 * @j2sAlias getMainFrame
	 * 
	 * @return OSPFrame
	 */
	public OSPFrame getMainFrame() {
		return this;
	}

	/**
	 * Gets the DataTool frame size.
	 * 
	 * @j2sAlias getMainFrameSize
	 * 
	 */
	public int[] getMainFrameSize() {
		Dimension d = this.getSize();
		return new int[] { d.width, d.height };
	}

	/**
	 * Sets the saveChangesOnClose flag.
	 *
	 * @param save true to save changes when exiting
	 */
	public void setSaveChangesOnClose(boolean save) {
		saveChangesOnClose = save && !OSPRuntime.appletMode;
	}

	/**
	 * Adds tabs loaded with data from an xml control.
	 *
	 * @param control  the xml control
	 * @param whenDone
	 */
	public void addTabs(XMLControl control, Consumer<ArrayList<DataToolTab>> whenDone) {
		// * @return a list of newly added tabs, or null if failed
		// BH 2020.02.13 changed from returning tabs to void
		// if control is for DataToolTab class, load tab from control
		if (DataToolTab.class == control.getObjectClass()) {
			// store this DataTool in the control so DataToolTab loader can use it for
			// instantiation
			control.setValue("datatool", this); //$NON-NLS-1$

			isLoading = true;
			DataToolTab tab = (DataToolTab) control.loadObject(null);
			addTab(tab);
			tab.refreshGUI();
			isLoading = false;
			ArrayList<DataToolTab> tabs = new ArrayList<DataToolTab>();
			tabs.add(tab);
			if (whenDone != null)
				whenDone.accept(tabs);
			return;
		}
		// if control is for FourierToolTab, load the source data into a tab
		if (control.getObjectClassName().endsWith("FourierToolTab")) { //$NON-NLS-1$
			XMLControl child = control.getChildControl("source_data"); //$NON-NLS-1$
			Data source = (Data) child.loadObject(null);
			DataToolTab tab = createTab(source);
			addTab(tab);
			tab.refreshGUI();
			ArrayList<DataToolTab> tabs = new ArrayList<DataToolTab>();
			tabs.add(tab);
			if (whenDone != null)
				whenDone.accept(tabs);
			return;
		}
		// otherwise load data from control into tabs
		loadTabsFromXMLAsync(control, new Consumer<ArrayList<DataToolTab>>() {

			@Override
			public void accept(ArrayList<DataToolTab> tabs) {
				for (DataToolTab tab : tabs) {
					addTab(tab);
					tab.refreshGUI();
				}
				if (whenDone != null)
					whenDone.accept(tabs);
			}

		});
	}

	/**
	 * Creates a tab for each Data object returned by DataTool.getDataList(source).
	 * The tab names will be those of the Data objects in the list if they define a
	 * getName() method.
	 *
	 * @param source the source Data
	 * @return a list of new tabs
	 */
	public ArrayList<DataToolTab> createTabs(Data source) {
		ArrayList<Data> dataList = getSelfContainedData(source);
		ArrayList<DataToolTab> tabList = new ArrayList<DataToolTab>();
		for (Data next : dataList) {
			DataToolTab tab = createTab(next);
			if (tab != null) {
				tabList.add(tab);
			}
		}
		return tabList;
	}

	/**
	 * Creates a tab for the specified Data object. The tab name will be that of the
	 * Data object if it defines a getName() method.
	 *
	 * @param data the Data
	 * @return the new tab
	 */
	protected DataToolTab createTab(Data data) {
		// be sure fitBuilder is instantiated
		fitBuilder = getFitBuilder();
		DataToolTab tab = new DataToolTab(data, this);
		if (data != null) {
			String name = data.getName();
			if ((name != null) && !name.equals("")) { //$NON-NLS-1$ )
				tab.setName(name);
			}
		}
		return tab;
	}

	/**
	 * Removes the tab at the specified index.
	 *
	 * @param index       the tab number
	 * @param saveChanges
	 * @return the removed tab, or null if none removed
	 */
	public DataToolTab removeTab(int index, boolean saveChanges) {
		if ((index >= 0) && (index < tabbedPane.getTabCount())) {
			if (saveChanges && !saveChangesAt(index)) {
				return null;
			}
			DataToolTab tab = getTab(index);
			getFitBuilder().curveFitters.remove(tab.getCurveFitter());
			tab.getCurveFitter().notifyTabRemoved();
//			String title = tabbedPane.getTitleAt(index);
			//OSPLog.finer("removing tab " + title); //$NON-NLS-1$
			tabbedPane.removeTabAt(index);
			refreshTabTitles();
			refreshMenubar();
			refreshDataBuilder();
			return tab;
		}
		return null;
	}

	/**
	 * Removes a specified tab.
	 *
	 * @param tab the tab
	 * @return the removed tab, or null if none removed
	 */
	public DataToolTab removeTab(DataToolTab tab) {
		return removeTab(getTabIndex(tab), true);
	}

	/**
	 * Loads a Data object into existing tabs and/or newly created tabs as needed.
	 *
	 * @param data the Data
	 * @return a list of the loaded tabs
	 */
	public ArrayList<DataToolTab> loadData(Data data) {
		DataToolTab tab = null;
		ArrayList<DataToolTab> loadedTabs = new ArrayList<DataToolTab>();
		for (Data next : getSelfContainedData(data)) {
			tab = getTab(next); // tab may be null
			if (tab != null) {
				tab.loadData(next, tab.replaceColumnsWithMatchingNames);
			} else {
				tab = createTab(next);
				addTab(tab);
			}
			loadedTabs.add(tab);
		}
		if (tab != null) {
			setSelectedTab(tab);
		}
		return loadedTabs;
	}

	/**
	 * Loads multiple Data objects into a single existing or newly created tab.
	 *
	 * @param data one or more Data objects
	 * @return the loaded tab
	 */
	public DataToolTab loadData(Data... data) {
		if (data == null) {
			return null;
		}
		ArrayList<Data> selfContained = new ArrayList<Data>();
		for (Data next : data) {
			selfContained.addAll(DataTool.getSelfContainedData(next));
		}
		DataToolTab tab = null;
		for (Data next : selfContained) {
			// retrieve or create tab for first Data object
			if (tab == null) {
				tab = getTab(next); // may still be null
				if (tab != null) {
					tab.loadData(next, tab.replaceColumnsWithMatchingNames);
				} else {
					tab = createTab(next);
					addTab(tab);
				}
			}
			// add additional columns to tab
			else {
				ArrayList<DataColumn> columns = getDataColumns(next);
				columns.remove(0);
				tab.addColumns(columns, false, false, false);
			}
		}
		if (tab != null) {
			setSelectedTab(tab);
		}
		return tab;
	}

	/**
	 * Returns the tab associated with the specified Data object. May return null.
	 *
	 * @param data the Data
	 * @return the tab
	 */
	public DataToolTab getTab(Data data) {
		int i = getTabIndex(data);
		return (i > -1) ? getTab(i) : null;
	}

	/**
	 * Returns the tab at the specified index. May return null.
	 *
	 * @param index the tab index
	 * @return the tab
	 */
	public DataToolTab getTab(int index) {
		return ((index > -1) && (index < tabbedPane.getTabCount())) ? (DataToolTab) tabbedPane.getComponentAt(index)
				: null;
	}

	/**
	 * Returns the tab count.
	 *
	 * @return the number of tabs
	 */
	public int getTabCount() {
		return tabbedPane.getTabCount();
	}

	/**
	 * Returns a list of all open tabs.
	 *
	 * @return a list of DataToolTabs
	 */
	public List<DataToolTab> getTabs() {
		List<DataToolTab> tabs = new ArrayList<DataToolTab>();
		for (int i = 0; i < getTabCount(); i++) {
			tabs.add(getTab(i));
		}
		return tabs;
	}

	/**
	 * Opens an xml or data file specified by name.
	 *
	 * @param fileName the file name
	 */
	public void open(File file) {
		// @return the file name, if successfully opened (datasets loaded)
		// BH 2020.02.13 return was string;
		OSPLog.fine("opening " + file); //$NON-NLS-1$
		Resource res = new Resource(file);
		Reader in = res.openReader();
		String firstLine = readFirstLine(in);
		// if xml, read the file into an XML control and add tab
		if (firstLine.startsWith("<?xml")) { //$NON-NLS-1$
			XMLControlElement control = new XMLControlElement(file);
			addTabs(control, new Consumer<ArrayList<DataToolTab>>() {

				@Override
				public void accept(ArrayList<DataToolTab> tabs) {
					if (tabs.isEmpty()) {
						OSPLog.finest("no data found"); //$NON-NLS-1$
					} else {
						for (DataToolTab tab : tabs) {
							refreshDataBuilder();
							if (tabs.size() == 1) {
								tab.fileName = file.toString();
							}
							tab.tabChanged(false);
						}
						try {
							in.close();
						} catch (IOException e) {
							// e.printStackTrace();
						}
					}
				}

			});
			return;
		}
		if (res.getString() != null) {
			// if not xml, attempt to import data and add tab(s)
			setMultipleTabPolicy(res.getString());
			Data[] data = parseData(res.getString(), file.toString());
			if (data != null) {
				DataToolTab first = null;
				for (int i = 0; i < data.length; i++) {
					DataToolTab tab = createTab(data[i]);
					first = first == null? tab: first;
					addTab(tab);
					if (data.length == 1)
						tab.fileName = file.getAbsolutePath();
					tab.tabChanged(false);
				}
				setSelectedTab(first);
				refreshDataBuilder();
				return;
			}
		}
		OSPLog.finest("no data found"); //$NON-NLS-1$
	}
	
	/**
	 * Loads content into DataDataTool.
	 *
	 * @param content the xml or CVS dataset
	 */
	public void loadDataset(String content, String title) {
		// if xml, read the file into an XML control and add tab
		if (content.startsWith("<?xml")) { //$NON-NLS-1$
			XMLControlElement control = new XMLControlElement(content);
			addTabs(control, new Consumer<ArrayList<DataToolTab>>() {

				@Override
				public void accept(ArrayList<DataToolTab> tabs) {
					if (tabs.isEmpty()) {
						OSPLog.finest("no data found"); //$NON-NLS-1$
					} else {
						for (DataToolTab tab : tabs) {
							refreshDataBuilder();
							if (tabs.size() == 1) {
								tab.fileName = title;
							}
							tab.tabChanged(false);
						}
					}
				}

			});
			return;
		}
		// if not xml, attempt to import data and add tab
		setMultipleTabPolicy(content);
		Data[] data = parseData(content, title);
		if (data != null) {
			DataToolTab first = null;
			for (int i = 0; i < data.length; i++) {
				DataToolTab tab = createTab(data[i]);
				first = first == null? tab: first;
				addTab(tab);
				if (data.length == 1)
					tab.fileName = title;
				tab.tabChanged(false);
			}
			setSelectedTab(first);
			return;
		}
//		if (data != null) {
//			DataToolTab tab = createTab(data[0]);
//			addTab(tab);
//			refreshDataBuilder();
//			tab.fileName = title;
//			tab.tabChanged(false);
//			return;
//		}
		OSPLog.finest("no data found"); //$NON-NLS-1$
	}

	/**
	 * Imports an xml or data file into an existing tab.
	 *
	 * @param tab      the tab
	 * @param fileName the file name
	 * @return the file name, if successfully imported (datasets loaded)
	 */
	void importFileIntoTab(DataToolTab tab, File file) {
		OSPLog.fine("importing " + file); //$NON-NLS-1$
		Resource res = new Resource(file);
		Reader in = res.openReader();
		String firstLine = readFirstLine(in);
		// if xml, read the file into an XML control and add tab
		if (firstLine.startsWith("<?xml")) { //$NON-NLS-1$
			XMLControlElement control = new XMLControlElement(file);
			getSelfContainedDataAsync(control, false, new Consumer<ArrayList<Data>>() {

				@Override
				public void accept(ArrayList<Data> dataList) {
					if (dataList.isEmpty()) {
						OSPLog.finest("no data found"); //$NON-NLS-1$
					} else {
						DatasetManager manager = new DatasetManager();
						for (Data next : dataList) {
							for (DataColumn column : getDataColumns(next)) {
								manager.addDataset(column);
							}
						}
						tab.addColumns(manager, true, true, true);
					}
				}
			});
			return;
		}
		if (res.getString() != null) {
			// if not xml, attempt to import data and add tab
			Data[] data = parseData(res.getString(), file.toString());
			if (data != null) {
				tab.addColumns(data[0], true, true, true);
				return;
			}
		}
		OSPLog.finest("no data found"); //$NON-NLS-1$
	}

	/**
	 * Sends a job to this tool and specifies a tool to reply to.
	 *
	 * @param job     the Job
	 * @param replyTo the tool to notify when the job is complete (may be null)
	 */
	@Override
	public void send(Job job, Tool replyTo) {
		XMLControlElement control = new XMLControlElement(job.getXML());
		if (control.failedToRead() || (control.getObjectClass() == Object.class)) {
			return;
		}
		// if control defines Data, construct the Data
		if (Data.class.isAssignableFrom(control.getObjectClass())) {
			Data data = (Data) control.loadObject(null, true, true);
			// if self-contained, then send job to an existing tab or create a new tab
			if (isSelfContained(data)) {
				DataToolTab tab = getTab(data); // may be null
				if (tab == null) {
					tab = createTab(null);
					String name = data.getName();
					if ((name != null) && !name.equals("")) { //$NON-NLS-1$ )
						tab.setName(name);
					}
					addTab(tab);
				} else {
					setSelectedTab(tab);
				}
				tab.receiveJobControl(job, replyTo, control);
			} else {
				// if data is a container, then send a new job to an existing tab
				// or create a new tab
				for (Data next : getSelfContainedData(data)) {
					DataToolTab tab = getTab(next); // may be null
					if (tab == null) {
						tab = createTab(null);
						String name = next.getName();
						if ((name != null) && !name.equals("")) { //$NON-NLS-1$ )
							tab.setName(name);
						}
						addTab(tab);
					}
					tab.send(new LocalJob(next), replyTo);
				}
			}
		} else {
			// else add tabs based on child Data objects, if any, within the control
			addTabs(control, null); // adds Data objects found in XMLControl
		}
		toFront();
	}

	/**
	 * Sets the useChooser flag.
	 *
	 * @param useChooser true to load datasets with a chooser
	 */
	public void setUseChooser(boolean useChooser) {
		this.useChooser = useChooser;
	}

	/**
	 * Gets the useChooser flag.
	 *
	 * @return true if loading datasets with a chooser
	 */
	public boolean isUseChooser() {
		return useChooser;
	}

	/**
	 * Listens for property changes "function"
	 *
	 * @param e the event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		switch (e.getPropertyName()) {
		case DataFunctionEditor.PROPERTY_DATAFUNCTIONEDITOR_FUNCTION:
			DataToolTab tab = getSelectedTab();
			if (tab == null)
				return;
			tab.tabChanged(true);
			tab.dataTable.refreshTable(DataTable.MODE_FUNCTION);
			tab.statsTable.refreshStatistics();
			if (e.getNewValue() instanceof DataFunction) { // new function has been created
				String funcName = e.getNewValue().toString();
				tab.dataTable.getWorkingData(funcName);
			}
			if (e.getOldValue() instanceof DataFunction) { // function has been deleted
				String funcName = e.getOldValue().toString();
				tab.dataTable.removeWorkingData(funcName);
			}
			if (e.getNewValue() instanceof String) {
				String funcName = e.getNewValue().toString();
				if (e.getOldValue() instanceof String) { // function name has changed
					String prevName = e.getOldValue().toString();
					tab.columnNameChanged(prevName, funcName);
				} else {
					tab.dataTable.getWorkingData(funcName);
				}
			}
			tab.refreshPlot();
			tab.varPopup = null;
			break;
		}
	}

	/**
	 * Determines if an array contains any duplicate or Double.NaN values.
	 *
	 * @param values the array
	 * @return true if at least one duplicate is found
	 */
	protected static boolean containsDuplicateValues(double[] values) {
		for (int i = 0; i < values.length; i++) {
			if (Double.isNaN(values[i])) {
				return true;
			}
			int n = getIndex(values[i], values, i);
			if (n > -1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the first array index at which the specified value is found.
	 *
	 * @param value       the value to find
	 * @param array       the array to search
	 * @param ignoreIndex an array index to ignore
	 * @return the index, or -1 if not found
	 */
	protected static int getIndex(double value, double[] array, int ignoreIndex) {
		for (int i = 0; i < array.length; i++) {
			if (i == ignoreIndex) {
				continue;
			}
			if (array[i] == value) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns an array of row numbers.
	 *
	 * @param rowCount length of the array
	 * @return the array
	 */
	protected static double[] getRowArray(int rowCount) {
		double[] rows = new double[rowCount];
		for (int i = 0; i < rowCount; i++) {
			rows[i] = i;
		}
		return rows;
	}

	/**
	 * Parses a String into tokens separated by a specified delimiter. A token may
	 * be "".
	 *
	 * @param text      the text to parse
	 * @param delimiter the delimiter
	 * @return an array of String tokens
	 */
	protected static String[] parseStrings(String text, String delimiter) {
		Collection<String> tokens = new ArrayList<String>();
		if (text != null) {
			// get the first token
			String next = text;
			int i = text.indexOf(delimiter);
			if (i == -1) { // no delimiter
				tokens.add(stripQuotes(next));
				text = null;
			} else {
				next = text.substring(0, i);
				text = text.substring(i + 1);
				while (" ".equals(delimiter) //$NON-NLS-1$
						&& (text.startsWith(" ") || text.startsWith("\t"))) { //$NON-NLS-1$ //$NON-NLS-2$
					// treat multiple spaces/tabs as a single delimiter
					text = text.substring(1);
				}
			}
			// iterate thru the tokens and add to token list
			while (text != null) {
				tokens.add(stripQuotes(next));
				i = text.indexOf(delimiter);
				if (i == -1) { // no delimiter
					next = text;
					tokens.add(stripQuotes(next));
					text = null;
				} else {
					next = text.substring(0, i).trim();
					text = text.substring(i + 1);
					while (" ".equals(delimiter) //$NON-NLS-1$
							&& (text.startsWith(" ") || text.startsWith("\t"))) { //$NON-NLS-1$ //$NON-NLS-2$
						// treat multiple spaces/tabs as a single delimiter
						text = text.substring(1);
					}
				}
			}
		}
		return tokens.toArray(new String[0]);
	}

	/**
	 * Strips quotation marks around a string.
	 *
	 * @param text the text to strip
	 * @return the stripped string
	 */
	private static String stripQuotes(String text) {
		if (text.startsWith("\"")) { //$NON-NLS-1$
			String stripped = text.substring(1);
			int n = stripped.indexOf("\""); //$NON-NLS-1$
			if (n > -1 && n == stripped.length() - 1) {
				return stripped.substring(0, n);
			}
		}
		return text;
	}

	/**
	 * Parses a String into doubles separated by a specified delimiter. Unparsable
	 * strings are set to Double.NaN.
	 *
	 * @param text      the text to parse
	 * @param delimiter the delimiter
	 * @return an array of doubles
	 */
	protected static double[] parseDoubles(String text, String delimiter) {
		return parseDoubles(parseStrings(text, delimiter), delimiter);
	}

	/**
	 * Parses a String array into doubles. Unparsable strings are set to Double.NaN.
	 *
	 * @param strings   the String array to parse
	 * @param delimiter the delimiter that was used to parse the strings
	 * @return an array of doubles
	 */
	protected static double[] parseDoubles(String[] strings, String delimiter) {
		double[] doubles = new double[strings.length];
		boolean checkComma = (delimiter != null && !delimiter.equals(","));
		for (int i = 0; i < strings.length; i++) {
			String s = strings[i];
			doubles[i] = Double.NaN;
			if (s != "" && s.indexOf("\t") < 0) { //$NON-NLS-1$
				try {
					doubles[i] = Double.parseDouble(s);
				} catch (NumberFormatException e) {
					// convert decimal separator commas with periods
					if (checkComma && s.indexOf(",") >= 0) { //$NON-NLS-1$ //$NON-NLS-2$
						try {
							doubles[i] = Double.parseDouble(s.replace(",", "."));
						} catch (NumberFormatException e1) {
						}
					} 
				}
			}
		}
		return doubles;
	}

	/**
	 * Parses a String into tokens separated by specified row and column delimiters.
	 * 
	 * NOTE: This method WILL check for European comma decimal point
	 *
	 * @param text         the text to parse
	 * @param rowDelimiter the column delimiter
	 * @param colDelimiter the column delimiter
	 * @return a 2D array of String tokens
	 */
	protected static String[][] parseStrings(String text, String rowDelimiter, String colDelimiter) {
		String[] rows = parseStrings(text, rowDelimiter);
		String[][] tokens = new String[rows.length][0];
		for (int i = 0; i < rows.length; i++) {
			tokens[i] = parseStrings(rows[i], colDelimiter);
		}
		return tokens;
	}

	/**
	 * Parses a String into doubles separated by specified row and column
	 * delimiters.
	 * 
	 * NOTE: This method is not set to parse European comma decimal points (but could be)
	 *
	 * @param text         the text to parse
	 * @param rowDelimiter the column delimiter
	 * @param colDelimiter the column delimiter
	 * @return a 2D array of doubles
	 */
	protected static double[][] parseDoubles(String text, String rowDelimiter, String colDelimiter) {
		String[][] strings = parseStrings(text, rowDelimiter, colDelimiter);
		double[][] doubles = new double[strings.length][0];
		for (int i = 0; i < strings.length; i++) {
			doubles[i] = parseDoubles(strings[i], null);
		}
		return doubles;
	}
	
	private static boolean containsDelimeter(String s) {
		for (int i = 0; i < delimiters.length; i++) {
			if (delimiters[i] != SPACE && s.contains(delimiters[i]))
				return true;
		}
		return false;
	}

	/**
	 * Parses character-delimited data from a string. This attempts to extract the
	 * following information from the string:
	 *
	 * 1. A title to be used for the tab name 2. One or more columns of double data
	 * values 3. Column names for the data columns
	 *
	 * @param dataString the data string
	 * @param fileName   name of file containing the data string (may be null)
	 * @return DatasetManager[] with parsed data, or null if none found
	 */
	public static DatasetManager[] parseData(String dataString, String fileName) {
		final String gnuPlotComment = "#"; //$NON-NLS-1$
		BufferedReader input = null;
		// if dataString is a file path then return null
		if (new File(dataString).exists())
			return null;
		// if datastring is XML, return null
		if (dataString.trim().startsWith("<?xml"))
			return null;
		// if datastring is XMLControl code, return null
		if (dataString.trim().startsWith("<object class="))
			return null;
		try {
			for (int i = 0; i < delimiters.length; i++) {
				input = new BufferedReader(new StringReader(dataString));
				String textLine = input.readLine();
				ArrayList<double[]> rows = new ArrayList<double[]>();
				int columns = Integer.MAX_VALUE;
				String[] columnNames = null;
				String title = null;
				ArrayList<String> titles = new ArrayList<String>();
				boolean isMulti = false;
				int lineCount = 0;
				int columnStride = -1;
				while (textLine != null) {
					// process each line of text
					if (textLine.startsWith("//")) { //$NON-NLS-1$
						// ignore comments (lines starting with "//")
						textLine = input.readLine();
						continue;
					}
					if (textLine.contains(gnuPlotComment)) {
						// trim gnuPlot comments
						textLine = textLine.trim(); // added by W. Christian
					}
					// look for gnuPlot-commented name and/or columnNames
					// or multitrack hint
					if (textLine.startsWith(gnuPlotComment)) {
						int k = textLine.indexOf("name:"); //$NON-NLS-1$
						if (k > -1) {
							title = textLine.substring(k + 5).trim();
						}
						k = textLine.indexOf("columnNames:"); //$NON-NLS-1$
						if (k > -1) {
							textLine = textLine.substring(k + 12).trim();
						}
						k = textLine.indexOf("multi:"); //$NON-NLS-1$
						if (k > -1) {
							textLine = textLine.substring(k + 6).trim();
							isMulti = true;
							if (textLine.length() == 0) {
								textLine = input.readLine();
								continue;
							}
						} else {
							textLine = input.readLine();
							continue;
						}
					}
					// skip Vernier Format 2 header lines
					if ((textLine.indexOf("Vernier Format") > -1 //$NON-NLS-1$
					) || (textLine.indexOf(".cmbl") > -1)) { //$NON-NLS-1$
						textLine = input.readLine();
						continue;
					}
					String[] strings = parseStrings(textLine, delimiters[i]);
					double[] rowData = parseDoubles(strings, delimiters[i]);
					// set title if:
					// --not yet set (null)
					// --String[] length > 0
					// --all values are NaN
					// --if isMulti, use all non-"" String entries
					// --else must find only one non-"" String entry
					// --none can contains another delimiter
					if (rows.isEmpty() && (strings.length > 0) && (title == null)) {
						for (int k = 0; k < strings.length; k++) {
							if (Double.isNaN(rowData[k]) && !strings[k].equals("")) { //$NON-NLS-1$
								titles.add(strings[k]);
								if (columnStride == -1)
									columnStride = k;
								else
									columnStride = k - columnStride;
							}
						}
						String s = "";
						switch (titles.size()) {
						case 0:
							break;
						case 1:
							s = titles.get(0);
							break;
						default: // 2 or more
							if (!isMulti)
								break;
							s = titles.get(0) + "+" + (titles.size() - 1);
						}

						if (!s.equals("") && !containsDelimeter(s)) { //$NON-NLS-1$
							title = s;
							textLine = input.readLine();
							continue;
						}
					}

					// set column names if:
					// --not yet set (null)
					// --String[] length > 0
					// --all entries are NaN
					// --no data yet loaded
					if (columnNames == null && rows.isEmpty() && strings.length > 0) {
						boolean valid = true;
						for (int k = 0; k < strings.length; k++) {
							if (containsDelimeter(strings[k]) || !Double.isNaN(rowData[k])) {
								valid = false;
								break;
							}
						}
						if (valid) {
							// replace "" with "?"
							for (int k = 0; k < strings.length; k++) {
								if ("".equals(strings[k])) { //$NON-NLS-1$
									strings[k] = "?"; //$NON-NLS-1$
								}
							}
							columnNames = strings;
							columns = strings.length;
							textLine = input.readLine();
							continue;
						}
					}
					// add double[] of length 1 or longer to rows
					boolean singleColumn = (columnNames != null && columnNames.length == 1);
					if (strings.length > 0) {
						lineCount++;
						boolean validData = true;
						boolean emptyData = true;
						for (int k = 0; k < strings.length; k++) {
							// invalid if any NaN entries other than ""
							if (Double.isNaN(rowData[k]) && !strings[k].equals("")) { //$NON-NLS-1$
								validData = false;
							}
							// look for empty row--every entry is ""
							if (!strings[k].equals("")) { //$NON-NLS-1$
								emptyData = false;
							}
						}
						// ignore blank lines (NaN data) that precede real data
						// unless both title and column names are non-null
						// and number of columns is 1
						if (!singleColumn && emptyData && title == null
								&& rows.isEmpty()) {
							validData = false;
						}
						// ignore if column count is 1 and string contains comma
						// which may be a delimiter interpreted as decimal separator
						// unless a single column name is non-null
						if (rowData.length == 1 && strings[0].contains(",")
								&& !singleColumn) {
							validData = false;
						}
						// add valid data
						if (validData) {
							rows.add(rowData);
							if (columns == Integer.MAX_VALUE) {
								columns = rowData.length;
							} else {
								columns = Math.max(rowData.length, columns);
							}
						}
					}
					// abort processing if no data found in first several lines
					if (rows.isEmpty() && lineCount > 10) {
						break;
					}
					textLine = input.readLine();
				} // end while loop

				// BH: Note that at this point, columns can be > 0 and columnNames can be null
				
				// create datasets if data found
				if (columns > 0 && columns < Integer.MAX_VALUE) {
					input.close();
					// first reassemble data from rows into columns
					double[][] dataArray = new double[columns][rows.size()];
					for (int row = 0; row < rows.size(); row++) {
						double[] rowData = rows.get(row);
						for (int j = 0; j < columns; j++) {
							dataArray[j][row] = rowData.length > j ? rowData[j] : Double.NaN;
						}
					}
					// then append data to datasets
					DatasetManager[] data = null;
					double[] rowColumn = getRowArray(rows.size());
					if (!isMulti || loadMultipleTracksInSingleTab) {
						data = new DatasetManager[1];
						data[0] = new DatasetManager();
						data[0].setName((title == null) ? XML.getName(fileName) : title);
						int droppedCols = 0;
						outer: for (int j = 0; j < columns; j++) {
							Dataset dataset = data[0].getDataset(j - droppedCols);
							String yColName = (columnNames != null && columnNames.length > j ? columnNames[j]
									: columns == 1 && title != null ? title : "?"); //$NON-NLS-1$
							dataset.setXYColumnNames("row", yColName); //$NON-NLS-1$
							dataset.setXColumnVisible(false);
							if (yColName.equals("?")) {
								// if data array is all NaN, ignore it
								boolean allNaN = true;
								for (int k = 0; k < dataArray[j].length; k++) {
									allNaN = allNaN && Double.isNaN(dataArray[j][k]);
									if (!allNaN)
										break;
								}
								if (allNaN) {
									data[0].removeDataset(j - droppedCols);
									droppedCols++;
									continue outer;
								}
							}
							dataset.append(rowColumn, dataArray[j]);
						}
					} else {
						// must be multi && !loadMultipleTracksInSingleTab 
						// BH: Eclipse is complaining that columnNames might be null here
						data = new DatasetManager[titles.size()];
						for (int tab = 0; tab < titles.size(); tab++) {
							data[tab] = new DatasetManager();
							data[tab].setName(titles.get(tab));
							// add t column first to every datasetManager
							Dataset dataset = data[tab].getDataset(0);
							String yColName = columnNames != null && columnNames.length > 0 ? 
									columnNames[0]: "?"; //$NON-NLS-1$							
							dataset.setXYColumnNames("row", yColName); //$NON-NLS-1$
							dataset.setXColumnVisible(false);

							// trim NaN values from start and end of data array
							double[] values = dataArray[1 + (tab * columnStride)];
							double[] rowNums = rowColumn;
							int startIndex = 0, endIndex = values.length - 1;
							for (int k = 0; k < values.length; k++) {
								if (!Double.isNaN(values[k]))
									break;
								startIndex++;
							}
							for (int k = values.length - 1; k >= 0; k--) {
								if (!Double.isNaN(values[k]))
									break;
								endIndex--;
							}
							if (startIndex > 0 || endIndex < values.length - 1) {
								values = Arrays.copyOfRange(dataArray[0], startIndex, endIndex + 1);
								rowNums = getRowArray(endIndex - startIndex + 1);
							}

							dataset.append(rowNums, values);
							// now add other columns
							for (int j = 0; j < columnStride; j++) {
								int index = 1 + (tab * columnStride) + j;
								dataset = data[tab].getDataset(1 + j);
								yColName = columnNames != null && columnNames.length > index ? 
										columnNames[index]: "?"; //$NON-NLS-1$	
								dataset.setXYColumnNames("row", yColName); //$NON-NLS-1$
								dataset.setXColumnVisible(false);
								values = Arrays.copyOfRange(dataArray[index], startIndex, endIndex + 1);
								dataset.append(rowNums, values);
							}
						}
					}
					OSPLog.finest("data found using delimiter \"" //$NON-NLS-1$
							+ delimiters[i] + "\""); //$NON-NLS-1$
					return data;
				}
				// close the reader and open a new one
				input.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (input != null)
				input.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	// ______________________________ protected methods ________________________
	protected String readFirstLine(Reader in) {
		BufferedReader input = null;
		if (in instanceof BufferedReader) {
			input = (BufferedReader) in;
		} else {
			input = new BufferedReader(in);
		}
		String openingLine;
		try {
			openingLine = input.readLine();
			while ((openingLine == null) || openingLine.equals("")) { //$NON-NLS-1$
				openingLine = input.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		try {
			input.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return openingLine;
	}

	protected void setupDelimiterMenu(JMenu menu) {
		Action setDelimiterAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				VideoIO.setDelimiter(e.getActionCommand());
				refreshGUI();
			}
		};
		ButtonGroup delimiterButtonGroup = new ButtonGroup();
		menu.removeAll();
		String delimiter = VideoIO.getDelimiter();

		for (String key : VideoIO.getDelimiters().keySet()) {
			String nextDelimiter = VideoIO.getDelimiters().get(key);
			JMenuItem item = new JRadioButtonMenuItem(key);
			item.setActionCommand(nextDelimiter);
			item.addActionListener(setDelimiterAction);
			delimiterButtonGroup.add(item);
			menu.add(item);
			if (delimiter.equals(nextDelimiter))
				item.setSelected(true);
		}
	}


	/**
	 * Gets a unique name.
	 *
	 * @param proposed the proposed name
	 * @return the unique name
	 */
	protected String getUniqueTabName(String proposed) {
		if ((proposed == null) || proposed.equals("")) { //$NON-NLS-1$
			proposed = ToolsRes.getString("DataToolTab.DefaultName"); //$NON-NLS-1$
		}
		// collect existing tab names
		ArrayList<String> taken = new ArrayList<String>();
		for (int i = 0; i < getTabCount(); i++) {
			DataToolTab tab = getTab(i);
			taken.add(tab.getName());
		}
		if (!taken.contains(proposed)) {
			return proposed;
		}
		// strip existing numbered subscript if any
		String subscript = TeXParser.getSubscript(proposed);
		try {
			Integer.parseInt(subscript);
			proposed = TeXParser.removeSubscript(proposed);
		} catch (Exception ex) {
		}
		// construct a unique name from proposed by adding digit
		proposed += "_"; //$NON-NLS-1$
		int i = 1;
		String name = proposed + i;
		while (taken.contains(name)) {
			i++;
			name = proposed + i;
		}
		return name;
	}

	/**
	 * Loads self-contained Data objects from an XMLControl.
	 *
	 * @param control    the XMLControl
	 * @param useChooser true to present data choices to user
	 * @param whenDone   Runnable to run after choice is made
	 * @return a list of self-contained Data objects
	 */
	private void getSelfContainedDataAsync(XMLControl control, boolean useChooser, Consumer<ArrayList<Data>> whenDone) {
		HashSet<Integer> IDs = new HashSet<Integer>();
		ArrayList<Data> dataList = new ArrayList<Data>();
		// first get the Data XMLControls

		Consumer<List<XMLProperty>> whenChosen = new Consumer<List<XMLProperty>>() {

			@Override
			public void accept(List<XMLProperty> xmlControls) {

				// load the Data XMLControls and collect Data objects
				for (XMLProperty prop : xmlControls) {
					XMLControl next = (XMLControl) prop;
					Data data = null;
					if (next instanceof XMLControlElement) {
						XMLControlElement element = (XMLControlElement) next;
						data = (Data) element.loadObject(null, true, true);
					} else {
						data = (Data) next.loadObject(null);
					}
					if (data != null) {
						for (Data nextData : getSelfContainedData(data)) {
							// check IDs to prevent duplicates
							Integer id = Integer.valueOf(nextData.getID());
							if (!IDs.contains(id)) {
								IDs.add(id);
								dataList.add(nextData);
								// remove any previously added Datasets within DatasetManagers
								if (nextData instanceof DatasetManager) {
									for (Dataset dataset : ((DatasetManager)nextData).getDatasetsRaw()) {
										dataList.remove(dataset);
										id = Integer.valueOf(dataset.getID());
										IDs.add(id);
									}
								}
							}
						}
					}
				}
				if (whenDone != null)
					whenDone.accept(dataList);
			}

		};
		if (useChooser) {
			// get user-selected Data XMLControls from an xml tree chooser
			XMLTreeChooser chooser = new XMLTreeChooser(ToolsRes.getString("Chooser.Title"), //$NON-NLS-1$
					ToolsRes.getString("Chooser.Label"), null); //$NON-NLS-1$
			chooser.chooseAsync(control, Data.class, new Runnable() {

				@Override
				public void run() {
					whenChosen.accept(chooser.getList());
				}

			});
		} else {
			// get all Data XMLControls
			XMLTree tree = new XMLTree(control);
			tree.setHighlightedClass(Data.class);
			tree.selectHighlightedProperties();
			List<XMLProperty> xmlControls = tree.getSelectedProperties();
			if (xmlControls.isEmpty()) {
				JOptionPane.showMessageDialog(null, ToolsRes.getString("Dialog.NoDatasets.Message")); //$NON-NLS-1$
			}
			whenChosen.accept(xmlControls);
		}
	}

	/**
	 * Loads data from an XMLControl into one or more tabs.
	 * 
	 * @param control
	 *
	 * @param control    the XMLControl describing the data
	 * @param useChooser true to present data choices to user
	 *
	 * @return a list of loaded tabs
	 */
	private void loadTabsFromXMLAsync(XMLControl control, Consumer<ArrayList<DataToolTab>> whenLoaded) {
		getSelfContainedDataAsync(control, useChooser, new Consumer<ArrayList<Data>>() {

			@Override
			public void accept(ArrayList<Data> dataList) {
				ArrayList<DataToolTab> loadedTabs = new ArrayList<DataToolTab>();
				for (Data next : dataList) {
					loadedTabs.add(createTab(next));
				}
				whenLoaded.accept(loadedTabs);
			}
		});
	}

	/**
	 * Constructs a dataset from independent xColumn and yColumn datasets.
	 *
	 * @param xColumn the dataset containing data for the x column
	 * @param yColumn the dataset containing data for the y column
	 * @return the x-y dataset
	 */
	public static Dataset createDatasetFromYPoints(Dataset xColumn, Dataset yColumn) {
		Dataset dataset = new Dataset();
		dataset.setXYColumnNames(xColumn.getYColumnName(), yColumn.getYColumnName());
//    dataset.setConnected(true);
		dataset.setLineColor(yColumn.getLineColor());
		dataset.setMarkerShape(yColumn.getMarkerShape());
		dataset.setMarkerColor(yColumn.getFillColor(), yColumn.getEdgeColor());
		double[] xPoints = xColumn.getYPoints();
		double[] yPoints = yColumn.getYPoints();
		if (xPoints.length != yPoints.length) {
			int len = Math.min(xPoints.length, yPoints.length);
			double[] newPoints = new double[len];
			for (int i = 0; i < len; i++) {
				newPoints[i] = len < xPoints.length ? xPoints[i] : yPoints[i];
			}
			if (len < xPoints.length)
				xPoints = newPoints;
			else
				yPoints = newPoints;
		}
		dataset.append(xPoints, yPoints);
		return dataset;
	}

	/**
	 * Gets a list of Datasets from a self-contained source Data object.
	 *
	 * @param source the self-contained Data
	 * @return a list of Datasets
	 */
	public static ArrayList<Dataset> getDatasets(Data source) {
		// if the source supplies datasets, return them
		ArrayList<Dataset> datasets = (source instanceof DatasetManager ? 
				((DatasetManager)source).getDatasetsRaw() : source.getDatasets());
		if (datasets != null) {
			return datasets;
		}
		// else create an empty dataset list and populate it from data2D
		datasets = new ArrayList<Dataset>();
		double[][] data2D = source.getData2D();
		if ((data2D == null) || (data2D.length == 0) || (data2D[0] == null)) {
			return datasets; // return empty list
		}
		// get column names--minimum 2
		// colNames[0] will be x-column name
		// colNames[1] and up will be y-column names
		String[] colNames = source.getColumnNames();
		// if colNames is null, create colNames
		if (colNames == null) {
			colNames = new String[2];
			// if only one point set in data2D, then first column is named "n"
			if (data2D.length == 1) {
				colNames[0] = "n"; //$NON-NLS-1$
			}
		}
		int n = Math.max(2, data2D.length); // number of columns
		if (colNames.length > n) {
			n++;
		}
		colNames = getColumnNames(colNames, n);
		// create datasets for double[] columns
		// if more names than double[] columns, then first double[] is row numbers
		boolean xPointsAreRowNumbers = colNames.length > data2D.length;
		double[] xPoints = xPointsAreRowNumbers ? getRowArray(data2D[0].length) : data2D[0];
		for (int i = 1; i < colNames.length; i++) {
			double[] yPoints = xPointsAreRowNumbers ? data2D[i - 1] : data2D[i];
			Dataset dataset = createDataset(xPoints, yPoints, colNames[0], colNames[i], i, source);
			if (dataset != null) {
				datasets.add(dataset);
			}
		}
		return datasets;
	}

	/**
	 * Gets a list of all Datasets from any Data object.
	 *
	 * @param source a self-contained or container Data object
	 * @return a list of all Datasets
	 */
	public static ArrayList<Dataset> getAllDatasets(Data source) {
		ArrayList<Dataset> datasets = new ArrayList<Dataset>();
		for (Data next : getSelfContainedData(source)) {
			datasets.addAll(getDatasets(next));
		}
		return datasets;
	}

	/**
	 * Gets a list of self-contained Data objects.
	 *
	 * @param container the container Data
	 *
	 * @return a list of self-contained Data objects
	 */
	protected static ArrayList<Data> getSelfContainedData(Data container) {
		processedData.clear();
		ArrayList<Data> list = getSelfContainedDataWithTrap(container);
		return list;
	}

	/**
	 * Gets a list of DataColumns from a self-contained Data object.
	 *
	 * @param source a self-contained Data object
	 * @return a list of DataColumns
	 */
	protected static ArrayList<DataColumn> getDataColumns(Data source) {
		if (!isSelfContained(source)) {
			return null;
		}
		ArrayList<DataColumn> columns = new ArrayList<DataColumn>();
		// look for datasets in Data
		ArrayList<Dataset> datasetList = (source instanceof DatasetManager ? 
				((DatasetManager)source).getDatasetsRaw() : source.getDatasets());
		if (datasetList != null) {
			for (Dataset next : datasetList) {
				// get new columns from next dataset
				ArrayList<DataColumn> newColumns = createDataColumns(next);
				for (DataColumn newCol : newColumns) {
					// add new columns that are not exact duplicates of existing columns
					boolean isDup = false;
					for (DataColumn existing : columns) {
						if (existing.getYColumnName().equals(newCol.getYColumnName())) {
							double[] exPts = existing.getYPoints();
							double[] nextPts = newCol.getYPoints();
							if (exPts.length == nextPts.length) {
								isDup = true;
								for (int i = 0; i < exPts.length; i++) {
									isDup = exPts[i] == nextPts[i] && isDup;
								}
							}
						}
					}
					if (!isDup)
						columns.add(newCol);
				}
			}
		} else {
			double[][] data2D = source.getData2D();
			if ((data2D == null) || (data2D.length == 0) || (data2D[0] == null)) {
				return null;
			}
			// get column names
			String[] colNames = source.getColumnNames();
			// if colNames is null, create colNames
			if (colNames == null) {
				colNames = new String[2];
				// if only one point set in data2D, then first column is named "n"
				if (data2D.length == 1) {
					colNames[0] = "n"; //$NON-NLS-1$
				}
			}
			int n = Math.max(2, data2D.length); // number of columns: minimum 2
			if (colNames.length > n) {
				n++;
			}
			colNames = getColumnNames(colNames, n);
			// create a data column for each name
			// if more names than double[] columns, then first double[] is row numbers
			boolean includeRows = colNames.length > data2D.length;
			int index = data2D[0].length;
			for (int i = 0; i < data2D.length; i++) {
				if (data2D[i] != null) {
					index = Math.max(index, data2D[i].length);
				}
			}
			for (int i = 0; i < colNames.length; i++) {
				double[] colData = includeRows ? (i == 0) ? getRowArray(index) : data2D[i - 1] : data2D[i];
				DataColumn dataset = createDataColumn(colData, colNames[i], i, source);
				if (dataset != null) {
					columns.add(dataset);
				}
			}
		}
		return columns;
	}

	/**
	 * Gets a list of all DataColumns from any Data object.
	 *
	 * @param source a self-contained or container Data object
	 * @return a list of all DataColumns
	 */
	protected static ArrayList<DataColumn> getAllDataColumns(Data source) {
		ArrayList<DataColumn> columns = new ArrayList<DataColumn>();
		for (Data next : getSelfContainedData(source)) {
			columns.addAll(getDataColumns(next));
		}
		return columns;
	}

	/**
	 * Gets an array of column names.
	 *
	 * @param proposed  the proposed names
	 * @param nameCount the required number of names
	 * @return the column names
	 */
	private static String[] getColumnNames(String[] proposed, int nameCount) {
		String[] colNames = proposed;
		if (colNames.length != nameCount) {
			colNames = new String[nameCount];
			int len = Math.min(proposed.length, colNames.length);
			System.arraycopy(proposed, 0, colNames, 0, len);
		}
		// deal with null names
		ArrayList<String> taken = new ArrayList<String>();
		char c = 'A';
		for (int i = 0; i < nameCount; i++) {
			String next = colNames[i];
			if ((next != null) && !taken.contains(next)) {
				taken.add(next);
				continue;
			}
			if (next == null) {
				// replace null names with capital letters
				next = String.valueOf(c++);
				while (taken.contains(next)) {
					next = String.valueOf(c++);
				}
				colNames[i] = next;
				taken.add(next);
			}
		}
		return colNames;
	}

	/**
	 * Gets a list of Data objects which provide actual data. Each Data object in
	 * the list is suitable for adding to a tab. This method traps for duplicate
	 * Data objects and prevents infinite loops.
	 *
	 * @param source the source Data object
	 * @return a list of data-producing Data objects
	 */
	private static ArrayList<Data> getSelfContainedDataWithTrap(Data source) {
		ArrayList<Data> list = new ArrayList<Data>();
		if ((source == null) || processedData.contains(source)) {
			return list;
		}
		processedData.add(source);
		if (isSelfContained(source)) {
			list.add(source);
		} else {
			for (Data next : source.getDataList()) {
				ArrayList<Data> subList = getSelfContainedDataWithTrap(next);
				list.addAll(subList);
			}
		}
		return list;
	}

	/**
	 * Determines if a Data object is self-contained.
	 *
	 * @param data the Data object
	 * @return true if self-contained
	 */
	private static boolean isSelfContained(Data data) {
		return data.getDataList() == null;
	}

	/**
	 * Creates data columns from the visible columns of a dataset.
	 *
	 * @param source the source dataset
	 * @return a list of data columns
	 */
	private static ArrayList<DataColumn> createDataColumns(Dataset source) {
		ArrayList<DataColumn> columns = new ArrayList<DataColumn>();
		if (source instanceof DataColumn) {
			columns.add((DataColumn) source);
			return columns;
		}
		String[] colNames = source.getColumnNames();
		String rowName = "row"; //$NON-NLS-1$
		for (int i = 0; i < 2; i++) {
			if ((i == 0) && !source.isXColumnVisible()) {
				continue;
			}
			if ((i == 1) && !source.isYColumnVisible()) {
				continue;
			}
			DataColumn column = new DataColumn();
			column.setName(source.getName());
			column.setXYColumnNames(rowName, colNames[i]);
			column.setConnected(source.isConnected());
			column.setLineColor(source.getLineColor());
			column.setMarkerSize(source.getMarkerSize());
			column.setMarkerShape(source.getMarkerShape());
			column.setMarkerColor(source.getFillColor(), source.getLineColor());
			column.setID(source.getID());
			column.setColumnID(i);
			column.setPoints((i == 0) ? source.getXPointsRaw()
					: source.isShifted() ? source.getYPoints() : source.getYPointsRaw(), source.getIndex());
			column.setXColumnVisible(false);
			columns.add(column);
		}
		return columns;
	}

	/**
	 * Creates a data column from a double[] of data.
	 *
	 * @param data       the data points
	 * @param columnName the name of the column
	 * @param columnID   the column ID to assign the column
	 * @param source     the Data source providing the ID and colors
	 * @return the data column dataset
	 */
	private static DataColumn createDataColumn(double[] data, String columnName, int columnID, Data source) {
		if (data == null) {
			return null;
		}
		DataColumn column = new DataColumn();
		column.setXYColumnNames("row", columnName); //$NON-NLS-1$
		column.setConnected(true);
		Color[] lineColors = source.getLineColors();
		if ((lineColors != null) && (lineColors[columnID] != null)) {
			column.setLineColor(lineColors[columnID]);
		} else {
			column.setLineColor(DisplayColors.getLineColor(columnID));
		}
		column.setMarkerShape(Dataset.SQUARE);
		Color[] fillColors = source.getFillColors();
		if ((lineColors != null) && (lineColors[columnID] != null) && (fillColors != null)
				&& (fillColors[columnID] != null)) {
			column.setMarkerColor(fillColors[columnID], lineColors[columnID]);
		} else {
			column.setMarkerColor(DisplayColors.getMarkerColor(columnID), DisplayColors.getLineColor(columnID));
		}
		column.setID(source.getID());
		column.setColumnID(columnID);
		column.setPoints(data, data.length);
		return column;
	}

	/**
	 * Creates a dataset from double[] of data.
	 *
	 * @param xPoints  the x points
	 * @param yPoints  the y points
	 * @param xName    the name of the x column
	 * @param yName    the name of the y column
	 * @param columnID the column ID
	 * @param source   the Data source providing the ID and colors
	 * @return the dataset
	 */
	private static Dataset createDataset(double[] xPoints, double[] yPoints, String xName, String yName, int columnID,
			Data source) {
		if (yPoints == null) {
			return null;
		}
		Dataset dataset = new Dataset();
		dataset.setXYColumnNames(xName, yName);
		dataset.setConnected(true);
		Color[] lineColors = source.getLineColors();
		if ((lineColors != null) && (lineColors[columnID] != null)) {
			dataset.setLineColor(lineColors[columnID]);
		} else {
			dataset.setLineColor(DisplayColors.getLineColor(columnID));
		}
		dataset.setMarkerShape(Dataset.SQUARE);
		Color[] fillColors = source.getFillColors();
		if ((lineColors != null) && (lineColors[columnID] != null) && (fillColors != null)
				&& (fillColors[columnID] != null)) {
			dataset.setMarkerColor(fillColors[columnID], lineColors[columnID]);
		} else {
			dataset.setMarkerColor(DisplayColors.getMarkerColor(columnID), DisplayColors.getLineColor(columnID));
		}
		dataset.setID(source.getID());
		dataset.append(xPoints, yPoints);
		return dataset;
	}

	/**
	 * Copies a dataset. If includeDataAndID is false, only the name and display
	 * properties are copied.
	 *
	 * @param source           the source dataset
	 * @param target           the target dataset (may be null)
	 * @param includeDataAndID true to copy data and ID
	 * @return the copy
	 */
	public static Dataset copyDataset(Dataset source, Dataset target, boolean includeDataAndID) {
		if (target == null) {
			target = new Dataset();
		}
		if (includeDataAndID) {
			target.clear();
			double[] x = source.getXPointsRaw();
			double[] y = (source.isShifted() ? source.getYPoints() : source.getYPointsRaw());
			target.append(x, y, source.getIndex());
			target.setID(source.getID());
		}
		target.setName(source.getName());
		target.setXYColumnNames(source.getXColumnName(), source.getYColumnName());
		target.setMarkerShape(source.getMarkerShape());
		target.setMarkerSize(source.getMarkerSize());
		Color fill = source.getFillColor();
		Color edge = source.getEdgeColor();
		target.setMarkerColor(fill, edge);
		target.setLineColor(source.getLineColor());
		target.setConnected(source.isConnected());
		target.setXColumnVisible(source.isXColumnVisible());
		target.setYColumnVisible(source.isYColumnVisible());
		return target;
	}

	/**
	 * Inserts a specified value into an array.
	 *
	 * @param input the value to insert
	 * @param array the array into which the value is inserted
	 * @param trend positive if array is ascending, negative if descending, 0 if
	 *              neither
	 * @return an array containing the inserted value
	 */
	protected static double[] insert(double input, double[] array, int trend) {
		int n = array.length;
		double[] newArray = new double[n + 1];
		if (trend == 0) { // append at end
			System.arraycopy(array, 0, newArray, 0, n);
			newArray[n] = input;
		} else if (trend > 0) { // append before first array value larger than input
			for (int i = 0; i < n; i++) {
				if (input < array[i]) {
					System.arraycopy(array, 0, newArray, 0, i);
					System.arraycopy(array, i, newArray, i + 1, n - i);
					newArray[i] = input;
					return newArray;
				}
			}
			// if newArray not yet returned, then append at end
			System.arraycopy(array, 0, newArray, 0, n);
			newArray[n] = input;
		} else { // append before first array value smaller than input
			for (int i = 0; i < n; i++) {
				if (input > array[i]) {
					System.arraycopy(array, 0, newArray, 0, i);
					System.arraycopy(array, i, newArray, i + 1, n - i);
					newArray[i] = input;
					return newArray;
				}
			}
			// if newArray not yet returned, then append at end
			System.arraycopy(array, 0, newArray, 0, n);
			newArray[n] = input;
		}
		return newArray;
	}

	/**
	 * Adds a tab. The tab should be named before calling this method.
	 *
	 * @param tab a DataToolTab
	 */
	public void addTab(final DataToolTab tab) {
		// remove single empty tab, if any
		if (getTabCount() == 1) {
			DataToolTab prev = getTab(0);
			if (prev.originatorID == 0) {
				prev.tabChanged(false);
				removeTab(0, false);
			}
		}
		tab.dataTool = this;
		// assign a unique name (also traps for null name)
		tab.setName(getUniqueTabName(tab.getName()));
		//OSPLog.finer("adding tab " + tab.getName()); //$NON-NLS-1$
		tabbedPane.addTab("", tab); //$NON-NLS-1$
		tab.setFontLevel(FontSizer.getLevel());
		tabbedPane.setSelectedComponent(tab);
		// validate();
		refreshTabTitles();
		refreshMenubar();
	}

	/**
	 * Offers to save changes to the tab at the specified index.
	 *
	 * @param i the tab index
	 * @return true unless canceled by the user
	 */
	protected boolean saveChangesAt(int i) {
		if (OSPRuntime.appletMode) {
			return true;
		}
		DataToolTab tab = getTab(i);
		if (!tab.tabChanged) {
			return true;
		}
		String name = tab.getName();
		if (ToolsRes.getString("DataToolTab.DefaultName").equals(name) //$NON-NLS-1$
				&& (tab.originatorID == 0)) {
			return true;
		}
		int selected = JOptionPane.showConfirmDialog(this, ToolsRes.getString("DataTool.Dialog.SaveChanges.Message1") + //$NON-NLS-1$
				" \"" + name + "\" " + //$NON-NLS-1$ //$NON-NLS-2$
				ToolsRes.getString("DataTool.Dialog.SaveChanges.Message2"), //$NON-NLS-1$
				ToolsRes.getString("DataTool.Dialog.SaveChanges.Title"), //$NON-NLS-1$
				JOptionPane.YES_NO_CANCEL_OPTION);
		if (selected == JOptionPane.CANCEL_OPTION) {
			return false;
		}
		if (selected == JOptionPane.YES_OPTION) {
			// save root and all owned nodes
			if (save(tab, tab.fileName) == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets the currently selected DataToolTab, if any.
	 *
	 * @return the selected tab
	 */
	public DataToolTab getSelectedTab() {
		return (DataToolTab) tabbedPane.getSelectedComponent();
	}

	/**
	 * Selects a DataToolTab.
	 *
	 * @param tab the tab to select
	 */
	public void setSelectedTab(DataToolTab tab) {
		tabbedPane.setSelectedComponent(tab);
	}

	/**
	 * Clears data by removing all tabs.
	 */
	@Override
	public void clearData() {
		removeAllTabs();
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the level
	 */
	@Override
	public void setFontLevel(int level) {
		if (getJMenuBar() == null)
			return;
		super.setFontLevel(level);
		FontSizer.setFonts(emptyMenubar, level);
		FontSizer.setFonts(fileMenu, level);
		FontSizer.setFonts(editMenu, level);
		double factor = FontSizer.getFactor(level);
		buttonHeight = (int) (factor * defaultButtonHeight);
		if (tabbedPane != null) {
			for (int i = 0; i < getTabCount(); i++) {
				getTab(i).setFontLevel(level);
			}
		}
		if (dataBuilder != null) {
			dataBuilder.setFontLevel(level);
		}
		if (fontSizeGroup != null) {
			Enumeration<AbstractButton> e = fontSizeGroup.getElements();
			for (; e.hasMoreElements();) {
				AbstractButton button = e.nextElement();
				int i = Integer.parseInt(button.getActionCommand());
				if (i == FontSizer.getLevel()) {
					button.setSelected(true);
				}
			}
		}
		OSPLog.setFonts(level);
	}

	@Override
	public void setVisible(boolean vis) {
		// set preferred size the first time shown
		if (contentPane.getPreferredSize().equals(dim)) {
			double f = 1 + (0.2 * FontSizer.getLevel());
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			int w = Math.min(screen.width - 40, (int) (dim.width * f));
			int h = Math.min(screen.height - 100, (int) (dim.height * f));
			// add one pixel to width so no longer equals dim
			contentPane.setPreferredSize(new Dimension(w, h + 1));
			pack();
			// center this on the screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - getBounds().width) / 2;
			int y = (dim.height - getBounds().height) / 2;
			if (!standAlone)
				setLocation(x, y);
		}

		super.setVisible(vis);
	}

	/**
	 * Gets the fit builder.
	 * 
	 * @return the fit builder
	 */
	public FitBuilder getFitBuilder() {
		if (fitBuilder == null) {
			// BH for now we set lazyGUI false; room for improvement!
			fitBuilder = new FitBuilder(this, false);
			fitBuilder.setFontLevel(FontSizer.getLevel());
			fitBuilder.setHelpPath("fit_builder_help.html"); //$NON-NLS-1$
			fitBuilder.autoloadFits();
		}
		return fitBuilder;
	}

	/**
	 * Writes text to a file with the specified name.
	 *
	 * @param text     the text
	 * @param fileName the file name
	 * @return the path of the saved document or null if failed
	 */
	protected static String write(String text, String fileName) {
		int n = fileName.lastIndexOf("/"); //$NON-NLS-1$
		if (n < 0) {
			n = fileName.lastIndexOf("\\"); //$NON-NLS-1$
		}
		if (n > 0) {
			String dir = fileName.substring(0, n + 1);
			File file = new File(dir);
			if (!file.exists() && !file.mkdir()) {
				return null;
			}
		}
		try {
			File file = new File(fileName);
			// check to see if file already exists
			if (file.exists()) {
				if (!file.canWrite()) {
					JOptionPane.showMessageDialog(null, ControlsRes.getString("Dialog.ReadOnly.Message"), //$NON-NLS-1$
							ControlsRes.getString("Dialog.ReadOnly.Title"), //$NON-NLS-1$
							JOptionPane.PLAIN_MESSAGE);
					return null;
				}
				int selected = JOptionPane.showConfirmDialog(null,
						ToolsRes.getString("Tool.Dialog.ReplaceFile.Message") + " " + file.getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						ToolsRes.getString("Tool.Dialog.ReplaceFile.Title"), //$NON-NLS-1$
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (selected != JOptionPane.YES_OPTION) {
					return null;
				}
			}
			FileOutputStream stream = new FileOutputStream(file);
			java.nio.charset.Charset charset = java.nio.charset.Charset.forName("UTF-8"); //$NON-NLS-1$
			write(text, new OutputStreamWriter(stream, charset));
			if (file.exists()) {
				return file.getAbsolutePath();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Writes text to a Writer.
	 *
	 * @param text the text
	 * @param out  the Writer
	 */
	protected static void write(String text, Writer out) {
		try {
			Writer output = new BufferedWriter(out);
			output.write(text);
			output.flush();
			output.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Opens an xml or data file selected with a chooser.
	 *
	 * @return the name of the opened file
	 */
	protected void open() {
		// BH changed return String to void
		OSPRuntime.getChooser().showOpenDialog(null, new Runnable() {

			@Override
			public void run() {
				OSPRuntime.chooserDir = OSPRuntime.getChooser().getCurrentDirectory().toString();
				open(OSPRuntime.getChooser().getSelectedFile());
			}
		}, null);
	}

	/**
	 * Imports an xml or data file selected with a chooser into a specified tab.
	 *
	 * @param tab the tab to import into
	 */
	void importFileIntoTab(DataToolTab tab) {
		OSPRuntime.getChooser().showOpenDialog(tab, new Runnable() {

			@Override
			public void run() {
				OSPRuntime.chooserDir = OSPRuntime.getChooser().getCurrentDirectory().toString();
				importFileIntoTab(tab, OSPRuntime.getChooser().getSelectedFile());
			}

		}, null);
	}

	/**
	 * Saves the current tab to the specified file.
	 *
	 * @param fileName the file name
	 * @return the name of the saved file, or null if not saved
	 */
	protected String save(String fileName) {
		return save(getSelectedTab(), fileName);
	}

	/**
	 * Saves a tab to the specified file.
	 *
	 * @param tab      the tab
	 * @param fileName the file name
	 * @return the name of the saved file, or null if not saved
	 */
	protected String save(DataToolTab tab, String fileName) {
		if ((fileName == null) || fileName.equals("")) { //$NON-NLS-1$
			return saveAs();
		}
		XMLControl control = new XMLControlElement(tab);
		if (control.write(fileName) == null) {
			return null;
		}
		tab.fileName = fileName;
		tab.tabChanged(false);
		return fileName;
	}

	/**
	 * Saves the current tab to a file selected with a chooser.
	 *
	 * @return the name of the saved file, or null if not saved
	 */
	protected String saveAs() {
		int result = OSPRuntime.getChooser().showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			OSPRuntime.chooserDir = OSPRuntime.getChooser().getCurrentDirectory().toString();
			File file = OSPRuntime.getChooser().getSelectedFile();
			// check to see if file already exists
			if (file.exists()) {
				int selected = JOptionPane.showConfirmDialog(null,
						ToolsRes.getString("Tool.Dialog.ReplaceFile.Message") + " " + file.getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						ToolsRes.getString("Tool.Dialog.ReplaceFile.Title"), //$NON-NLS-1$
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (selected != JOptionPane.YES_OPTION) {
					return null;
				}
			}
			String fileName = file.getAbsolutePath();
			if ((fileName == null) || fileName.trim().equals("")) { //$NON-NLS-1$
				return null;
			}
			// add .xml extension if none but don't require it
			if (XML.getExtension(fileName) == null) {
				fileName += ".xml"; //$NON-NLS-1$
			}
			return save(XML.getRelativePath(fileName));
		}
		return null;
	}

	/**
	 * Returns the index of the tab containing the specified Data object.
	 *
	 * @param data the Data
	 * @return the index, or -1 if not found
	 */
	protected int getTabIndex(Data data) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			DataToolTab tab = (DataToolTab) tabbedPane.getComponentAt(i);
			if (tab.isOwnedBy(data)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of a specified tab.
	 *
	 * @param tab the tab
	 * @return the index, or -1 if not found
	 */
	protected int getTabIndex(DataToolTab tab) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			if (tab == tabbedPane.getComponentAt(i)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Constructs a DataTool with title and name.
	 * 
	 * @param title
	 * @param name
	 */
	protected DataTool(String title, String name) {
		super(title);
		setName(name);
		createGUI();
		Toolbox.addTool(name, this);
		ToolsRes.addPropertyChangeListener(ToolsRes.OSP_PROPERTY_LOCALE, new PropertyChangeListener() { //$NON-NLS-1$
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				refreshGUI();
			}

		});
		FileDropHandler fileDropHandler = new FileDropHandler();
		tabbedPane.setTransferHandler(fileDropHandler);
		setTransferHandler(fileDropHandler);
	}

	/**
	 * Removes all tabs except the specified index.
	 *
	 * @param index the tab number
	 * @return true if tabs removed
	 */
	protected boolean removeAllButTab(int index) {
		for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
			if (i == index) {
				continue;
			}
			if (!saveChangesAt(i)) {
				return false;
			}
//			String title = tabbedPane.getTitleAt(i);
			//OSPLog.finer("removing tab " + title); //$NON-NLS-1$
			DataToolTab tab = getTab(i);
			getFitBuilder().curveFitters.remove(tab.getCurveFitter());
			tab.getCurveFitter().notifyTabRemoved();
			tabbedPane.removeTabAt(i);
		}
		refreshTabTitles();
		refreshDataBuilder();
		return true;
	}

	/**
	 * Removes all tabs.
	 *
	 * @return true if all tabs removed
	 */
	protected boolean removeAllTabs() {
		for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
			if (!saveChangesAt(i)) {
				return false;
			}
//			String title = tabbedPane.getTitleAt(i);
			//OSPLog.finer("removing tab " + title); //$NON-NLS-1$
			DataToolTab tab = getTab(i);
			getFitBuilder().curveFitters.remove(tab.getCurveFitter());
			tab.getCurveFitter().notifyTabRemoved();
			tabbedPane.removeTabAt(i);
		}
		refreshMenubar();
		refreshDataBuilder();
		return true;
	}

	protected void refreshTabTitles() {
		// show variables being plotted as tab titles
		for (int i = 0, n = tabbedPane.getTabCount(); i < n; i++) {
			tabbedPane.setTitleAt(i, ((DataToolTab) tabbedPane.getComponentAt(i)).getName());
		}
	}

	protected void refreshMenubar() {
		if (getTabCount() == 0) {
			emptyMenubar.add(displayMenu);
			emptyMenubar.add(helpMenu);
			setJMenuBar(emptyMenubar);
		} else {
			menubar.add(displayMenu);
			menubar.add(helpMenu);
			setJMenuBar(menubar);
		}
	}

	/**
	 * Gets the data builder for defining custom data functions.
	 * 
	 * @return the data builder
	 */
	protected FunctionTool getDataBuilder() {
		if (dataBuilder == null) { // create new tool if none exists
			dataBuilder = new DataBuilder(this);
			dataBuilder.setFontLevel(FontSizer.getLevel());
			dataBuilder.addPropertyChangeListener(FunctionTool.PROPERTY_FUNCTIONTOOL_FUNCTION, this); // $NON-NLS-1$
		}
		refreshDataBuilder();
		return dataBuilder;
	}

	/**
	 * Refreshes the data builder.
	 */
	protected void refreshDataBuilder() {
		if (dataBuilder != null) {
			dataBuilder.refreshPanels();
		}
	}

	/**
	 * Shows the DataTool help.
	 */
	protected static void showHelp() {
		String fileName = helpName;
		String helpPath = XML.getResolvedPath(fileName, helpBase);
		if (ResourceLoader.getResource(helpPath) != null) {
			// show help in desktop browser
			OSPDesktop.displayURL(helpPath);
		} else {
			fileName = "data_tool_help.html"; //$NON-NLS-1$
			String classBase = "/org/opensourcephysics/resources/tools/html/"; //$NON-NLS-1$
			helpPath = XML.getResolvedPath(fileName, classBase);
			if ((helpFrame == null) || !helpPath.equals(helpFrame.getTitle())) {
				helpFrame = new TextFrame(helpPath);
				helpFrame.enableHyperlinks();
				helpFrame.setSize(800, 600);
				// center on the screen
				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				int x = (dim.width - helpFrame.getBounds().width) / 2;
				int y = (dim.height - helpFrame.getBounds().height) / 2;
				helpFrame.setLocation(x, y);
			}
			helpFrame.setVisible(true);
		}
	}

	/**
	 * Overrides OSPFrame method. This converts EXIT_ON_CLOSE to DO_NOTHING_ON_CLOSE
	 * and sets the exitOnClose flag.
	 *
	 * @param operation the operation
	 */
	@Override
	public void setDefaultCloseOperation(int operation) {
		if ((operation == JFrame.EXIT_ON_CLOSE)) {
			exitOnClose = true;
			operation = WindowConstants.DO_NOTHING_ON_CLOSE;
		}
		if ((operation != WindowConstants.DO_NOTHING_ON_CLOSE)) {
			saveChangesOnClose = false;
		}
		super.setDefaultCloseOperation(operation);
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
	}

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		// configure the frame

		// set preferred size
		double f = 1 + 0.25 * FontSizer.getLevel();
		Dimension used = new Dimension((int) (dim.width * f), (int) (dim.height * f));
		contentPane.setPreferredSize(used);
		setContentPane(contentPane);

		JPanel centerPanel = new JPanel(new BorderLayout());
		contentPane.add(centerPanel, BorderLayout.CENTER);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// add window listener to exit
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exitItem.doClick(0);
			}

		});
//    this.addComponentListener(new ComponentAdapter() {
//      public void componentResized(ComponentEvent e) {
//        DataToolTab tab = getSelectedTab();
//        if(tab==null) {
//          return;
//        }
//        if(!tab.propsCheckbox.isSelected()&&!tab.statsCheckbox.isSelected()) {
//          tab.splitPanes[2].setDividerLocation(0);
//        }
//      }
//
//    });
		// create tabbed pane
		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		centerPanel.add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				final DataToolTab tab = getSelectedTab();
				if (tab != null) {
					tab.refreshData();
					tab.dataTable.refreshTable(DataTable.MODE_TAB);
					tab.statsTable.refreshStatistics();
					tab.propsTable.refreshTable();
					tab.refreshPlot(true);
					refreshGUI();
					tab.dataTable.requestFocusInWindow();
					if (tab.dataTable.workingData != null) {
						String var = tab.dataTable.workingData.getXColumnName();
						var = TeXParser.removeSubscripting(var);
						getFitBuilder().setDefaultVariables(new String[] { var });
					}
				}
			}

		});
		tabbedPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (OSPRuntime.isPopupTrigger(e)) {
					final int index = tabbedPane.getSelectedIndex();
					// make popup with name change, clone and close items
					JPopupMenu popup = new JPopupMenu();
					JMenuItem item = new JMenuItem(ToolsRes.getString("DataTool.MenuItem.Name")); //$NON-NLS-1$
					popup.add(item);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							DataToolTab tab = getTab(index);
							String name = tab.getName();
							String input = GUIUtils.showInputDialog(DataTool.this, 
									ToolsRes.getString("DataTool.Dialog.Name.Message"), //$NON-NLS-1$
									ToolsRes.getString("DataTool.Dialog.Name.Title"), //$NON-NLS-1$
									JOptionPane.QUESTION_MESSAGE, name);
							if (input == null) {
								return;
							}
							// hide tab name so getUniqueTabName() not confused
							tab.setName(""); //$NON-NLS-1$
							tab.setName(getUniqueTabName(input.toString()));
							tab.tabChanged(true);
							refreshTabTitles();
							refreshDataBuilder();
						}

					});
					if (!getTab(index).dataManager.getDatasetsRaw().isEmpty()) {
						popup.addSeparator();
						item = new JMenuItem(ToolsRes.getString("DataTool.MenuItem.NewTab")); //$NON-NLS-1$
						item.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								newTabItem.doClick(0);
							}

						});
						popup.add(item);
						JMenu cloneMenu = new JMenu(ToolsRes.getString("DataTool.Menu.Clone")); //$NON-NLS-1$
						popup.add(cloneMenu);
						final JMenuItem cloneTabItem = new JMenuItem(ToolsRes.getString("DataTool.MenuItem.Editable")); //$NON-NLS-1$
						cloneMenu.add(cloneTabItem);
						cloneTabItem.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								cloneTab(index, true);
							}
						});
						item = new JMenuItem(ToolsRes.getString("DataTool.MenuItem.Noneditable")); //$NON-NLS-1$
						cloneMenu.add(item);
						item.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								cloneTab(index, false);
							}

						});
					}
					popup.addSeparator();
					item = new JMenuItem(ToolsRes.getString("MenuItem.Close")); //$NON-NLS-1$
					popup.add(item);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							removeTab(index, true);
						}

					});
					item = new JMenuItem(ToolsRes.getString("MenuItem.CloseOthers")); //$NON-NLS-1$
					popup.add(item);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							removeAllButTab(index);
						}

					});
					item = new JMenuItem(ToolsRes.getString("MenuItem.CloseAll")); //$NON-NLS-1$
					popup.add(item);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							removeAllTabs();
						}

					});
					FontSizer.setFonts(popup);
					popup.show(tabbedPane, e.getX(), e.getY() + 8);
				}
			}

		});
		int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		// create regular menubar
		menubar = new JMenuBar();
		fileMenu = new JMenu();
		menubar.add(fileMenu);
		MouseAdapter fileMenuChecker = new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				mousePressed(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				boolean empty = getSelectedTab().originatorID == 0;
				if (!OSPRuntime.appletMode) {
					exportItem.setEnabled(!empty);
					saveItem.setEnabled(!empty);
					saveAsItem.setEnabled(!empty);
					int[] selectedRows = getSelectedTab().dataTable.getSelectedRows();
					int endRow = getSelectedTab().dataTable.getRowCount() - 1;
					if ((selectedRows.length == 0) || ((selectedRows.length == 1) && (selectedRows[0] == endRow)
							&& getSelectedTab().dataTable.isEmptyRow(endRow))) {
						exportItem.setText(ToolsRes.getString("DataTool.MenuItem.Export")); //$NON-NLS-1$
					} else {
						exportItem.setText(ToolsRes.getString("DataTool.MenuItem.ExportSelection")); //$NON-NLS-1$
					}
				}
			}

		};
		fileMenu.addMouseListener(fileMenuChecker);
		newTabItem = new JMenuItem();
		newTabItem.setAccelerator(KeyStroke.getKeyStroke('N', keyMask));
		newTabItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DataToolTab tab = createTab(null);
				tab.userEditable = true;
				addTab(tab);
				tab.refreshGUI();
			}

		});
		fileMenu.add(newTabItem);
		if (!OSPRuntime.appletMode) {
			openItem = new JMenuItem();
			openItem.setAccelerator(KeyStroke.getKeyStroke('O', keyMask));
			openItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					open();
				}
			});
			fileMenu.add(openItem);
		}
		fileMenu.addSeparator();
		closeItem = new JMenuItem();
		closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = tabbedPane.getSelectedIndex();
				removeTab(index, true);
			}

		});
		fileMenu.add(closeItem);
		closeAllItem = new JMenuItem();
		closeAllItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeAllTabs();
			}

		});
		fileMenu.add(closeAllItem);
		fileMenu.addSeparator();
		if (!OSPRuntime.appletMode) {
			importItem = new JMenuItem();
			importItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					DataToolTab tab = getSelectedTab();
					importFileIntoTab(tab);
				}

			});
			fileMenu.add(importItem);
			exportItem = new JMenuItem();
			exportItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getSelectedTab().saveTableDataToFile(false); // raw data
				}

			});
			fileMenu.add(exportItem);
			fileMenu.addSeparator();
			// save item
			saveItem = new JMenuItem();
			saveItem.setAccelerator(KeyStroke.getKeyStroke('S', keyMask));
			saveItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					DataToolTab tab = getSelectedTab();
					save(tab.fileName);
				}

			});
			fileMenu.add(saveItem);
			// save as item
			saveAsItem = new JMenuItem();
			saveAsItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					saveAs();
				}

			});
			fileMenu.add(saveAsItem);
			fileMenu.addSeparator();
		}
		printItem = new JMenuItem();
		printItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SnapshotTool.getTool().printImage(DataTool.this);
			}

		});
		if (!OSPRuntime.isJS)
			printItem.setAccelerator(KeyStroke.getKeyStroke('P', keyMask));
		if (!OSPRuntime.isJS)
			fileMenu.add(printItem);
		fileMenu.addSeparator();
		exitItem = new JMenuItem();
		exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', keyMask));
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!saveChangesOnClose || removeAllTabs()) {
					if (exitOnClose) {
						System.exit(0);
					} else {
						setVisible(false);
					}
				}
			}

		});
		fileMenu.add(exitItem);
		editMenu = new JMenu();
		editMenu.addMenuListener(editMenuChecker);
		menubar.add(editMenu);
		// undo and redo items
		undoItem = new JMenuItem();
		undoItem.setEnabled(false);
		undoItem.setAccelerator(KeyStroke.getKeyStroke('Z', keyMask));
		undoItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (getSelectedTab().undoManager.canUndo()) {
					getSelectedTab().undoManager.undo();
				}
			}

		});
		editMenu.add(undoItem);
		redoItem = new JMenuItem();
		redoItem.setEnabled(false);
		redoItem.setAccelerator(KeyStroke.getKeyStroke('Y', keyMask));
		redoItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (getSelectedTab().undoManager.canRedo()) {
					getSelectedTab().undoManager.redo();
				}
			}

		});
		editMenu.add(redoItem);
		editMenu.addSeparator();
		// copy menu
		copyMenu = new JMenu();
		editMenu.add(copyMenu);
		copyTabItem = new JMenuItem();
		copyTabItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copyTab();
			}

		});
		copyDataMenu = new JMenu();
		copyDataAsFormattedItem = new JMenuItem();
		copyDataAsFormattedItem.setAccelerator(KeyStroke.getKeyStroke('C', keyMask));
		copyDataAsFormattedItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getSelectedTab().copyTableDataToClipboard(true);
			}

		});
		copyDataRawItem = new JMenuItem();
		copyDataRawItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getSelectedTab().copyTableDataToClipboard(false);
			}

		});
		setDelimiterMenu = new JMenu();
		setDelimiterMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				setupDelimiterMenu(setDelimiterMenu);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}

		});
		copyImageItem = new JMenuItem();
		copyImageItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String tabName = getSelectedTab().getName();
				OSPLog.finest("copying image of " + tabName); //$NON-NLS-1$
				SnapshotTool.getTool().copyImage(DataTool.this);
			}

		});
		MouseAdapter pasteMenuChecker = new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				// ignore if menu is disabled or already displayed
				if (!pasteMenu.isEnabled() || pasteMenu.isPopupMenuVisible()) {
					return;
				}
				// enable pasteColumnsItem if clipboard contains pastable columns
				if (hasPastableColumns(getSelectedTab())) {
					pasteMenu.add(pasteColumnsItem);
				} else {
					addableData = null;
					pasteMenu.remove(pasteColumnsItem);
				}
				FontSizer.setFonts(pasteMenu, FontSizer.getLevel());
			}

		};
		pasteMenu = new JMenu();
		pasteMenu.addMouseListener(pasteMenuChecker);
		editMenu.add(pasteMenu);
		pasteTabItem = new JMenuItem();
		pasteTabItem.setAction(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pasteTab(pasteTabItem);
			}

		});
		pasteMenu.add(pasteTabItem);
		pasteColumnsItem = new JMenuItem();
		pasteColumnsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (controlContainsData) {
					getSelfContainedDataAsync(control, useChooser, new Consumer<ArrayList<Data>>() {

						@Override
						public void accept(ArrayList<Data> dataList) {
							if (!dataList.isEmpty()) {
								DatasetManager manager = new DatasetManager();
								for (Data next : dataList) {
									for (DataColumn column : getDataColumns(next)) {
										manager.addDataset(column);
									}
								}
								addableData = manager;
								addColumnsFromPaste();
							}
						}

					});
				} else {
					addColumnsFromPaste();
				}
			}

		});
		pasteMenu.add(pasteColumnsItem);
		editMenu.addSeparator();
		editDataItem = new JMenuItem(ToolsRes.getString("DataToolTab.Button.EditData.Text")+"..."); //$NON-NLS-1$
		editDataItem.addActionListener((e) -> {
			editDataAction();
		});
		editMenu.add(editDataItem);
		displayMenu = new JMenu();
		menubar.add(displayMenu);
		languageMenu = new JMenu();
		// get jar resource before installed locales so that launch jar is not null
		ResourceLoader.getImageIcon("resources/tools/images/open.gif");
		final Locale[] locales = OSPRuntime.getInstalledLocales();
		Action languageAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String language = e.getActionCommand();
				//OSPLog.finest("setting language to " + language); //$NON-NLS-1$
				for (int i = 0; i < locales.length; i++) {
					if (language.equals(locales[i].toString())) {
						ToolsRes.setLocale(locales[i]);
						return;
					}
				}
			}

		};
		ButtonGroup languageGroup = new ButtonGroup();
		languageItems = new JMenuItem[locales.length];
		for (int i = 0; i < locales.length; i++) {
			languageItems[i] = new JRadioButtonMenuItem(OSPRuntime.getDisplayLanguage(locales[i]));
			languageItems[i].setActionCommand(locales[i].toString());
			languageItems[i].addActionListener(languageAction);
			languageMenu.add(languageItems[i]);
			languageGroup.add(languageItems[i]);
		}
		displayMenu.add(languageMenu);
		fontSizeMenu = new JMenu();
		displayMenu.add(fontSizeMenu);
		fontSizeGroup = new ButtonGroup();
		Action fontSizeAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = Integer.parseInt(e.getActionCommand());
				FontSizer.setLevel(i);
//        setFontLevel(i);
			}

		};
		for (int i = 0; i < 4; i++) {
			JMenuItem item = new JRadioButtonMenuItem("+" + i); //$NON-NLS-1$
			if (i == 0) {
				defaultFontSizeItem = item;
				item.setText(ToolsRes.getString("Tool.MenuItem.DefaultFontSize")); //$NON-NLS-1$
			}
			item.addActionListener(fontSizeAction);
			item.setActionCommand("" + i); //$NON-NLS-1$
			fontSizeMenu.add(item);
			fontSizeGroup.add(item);
			if (i == FontSizer.getLevel()) {
				item.setSelected(true);
			}
		}
		helpMenu = new JMenu();
		menubar.add(helpMenu);
		helpItem = new JMenuItem();
		helpItem.setAccelerator(KeyStroke.getKeyStroke('H', keyMask));
		helpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showHelp();
			}

		});
		helpMenu.add(helpItem);
		helpMenu.addSeparator();
		logItem = new JMenuItem();
		logItem.setAccelerator(KeyStroke.getKeyStroke('L', keyMask));
		logItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Point p0 = new Frame().getLocation();
				JFrame frame = OSPLog.getFrame();
				if ((frame.getLocation().x == p0.x) && (frame.getLocation().y == p0.y)) {
					Point p = getLocation();
					frame.setLocation(p.x + 28, p.y + 28);
				}
				frame.setVisible(true);
			}

		});
		helpMenu.add(logItem);
		helpMenu.addSeparator();
		aboutItem = new JMenuItem();
		aboutItem.setAccelerator(KeyStroke.getKeyStroke('A', keyMask));
		aboutItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showAboutDialog();
			}

		});
		helpMenu.add(aboutItem);
		setJMenuBar(menubar);
		// create the empty menu bar for use when no tabs are open
		emptyMenubar = new JMenuBar();
		emptyFileMenu = new JMenu();
		emptyMenubar.add(emptyFileMenu);
		emptyNewTabItem = new JMenuItem();
		emptyNewTabItem.setAccelerator(KeyStroke.getKeyStroke('N', keyMask));
		emptyNewTabItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DataToolTab tab = createTab(null);
				tab.userEditable = true;
				addTab(tab);
				tab.refreshGUI();
			}

		});
		emptyFileMenu.add(emptyNewTabItem);
		emptyOpenItem = new JMenuItem();
		emptyOpenItem.setAccelerator(KeyStroke.getKeyStroke('O', keyMask));
		emptyOpenItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				open();
			}

		});
		emptyFileMenu.add(emptyOpenItem);
		emptyFileMenu.addSeparator();
		emptyExitItem = new JMenuItem();
		emptyExitItem.setAccelerator(KeyStroke.getKeyStroke('Q', keyMask));
		emptyExitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (exitOnClose) {
					System.exit(0);
				} else {
					setVisible(false);
				}
			}

		});
		emptyFileMenu.add(emptyExitItem);
		emptyEditMenu = new JMenu();
		emptyEditMenu.addMenuListener(editMenuChecker);
		emptyMenubar.add(emptyEditMenu);
		emptyPasteMenu = new JMenu();
		emptyEditMenu.add(emptyPasteMenu);
		emptyPasteTabItem = new JMenuItem();
		emptyPasteTabItem.setAction(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pasteTab(emptyPasteTabItem);
			}
		});
		emptyPasteMenu.add(emptyPasteTabItem);
		refreshGUI();
		refreshMenubar();
		int level = FontSizer.getLevel();
		if (level != 0)
			setFontLevel(level);
		pack();
		// center this on the screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (dim.width - getBounds().width) / 2;
		int y = (dim.height - getBounds().height) / 2;
		if (!standAlone)
			setLocation(x, y); // don't set location if running stand alone
	}

	protected void pasteTab(Object source) {
		OSPRuntime.paste((s) -> {
			pasteAction(s, source);
		});
	}

	protected void copyTab() {
		int i = tabbedPane.getSelectedIndex();
		String title = tabbedPane.getTitleAt(i);
		OSPLog.finest("copying tab " + title); //$NON-NLS-1$
		OSPRuntime.copy(getTabXML(), null);
	}

	private String getTabXML() {
		return new XMLControlElement(getSelectedTab()).toXML();
	}

	protected void cloneTab(int index, boolean editable) {
		// determine name of cloned tab
		pasteAction(getTabXML(), pasteTabItem);
		String name = getTab(index).getName();
		String postfix = "_" + ToolsRes.getString("DataTool.Clone.Subscript"); //$NON-NLS-1$ //$NON-NLS-2$
		int pt = name.indexOf(postfix);
		if (pt >= 0) {
			name = name.substring(0, pt);
		}
		DataToolTab tab = getTab(getTabCount() - 1);
		tab.setName(getUniqueTabName(name + postfix));
	    if (!editable) {
			tab.setUserEditable(false);
			for (Dataset next : tab.dataManager.getDatasetsRaw()) {
				if (next instanceof DataColumn) {
					((DataColumn) next).deletable = false;
				}
			}	    	
		}		
		refreshTabTitles();
	}

	protected void pasteAction(String dataString, Object source) {
		boolean failed = false;
		try { // we will check for failure finally, before returning
			if (dataString == null) {
				return;
			}
			if (dataString.startsWith("<?xml")) { //$NON-NLS-1$
				// pasted string is xml, so load into XMLControl
				control = new XMLControlElement();
				control.readXML(dataString);
				if (control.failedToRead()) {
					failed = true;
					return;
				}
				// we now have a valid XMLControl
				// OSPLog.finest("pasting clipboard XML into new tabs"); //$NON-NLS-1$
				if (Data.class.isAssignableFrom(control.getObjectClass())) {
					Data data = (Data) control.loadObject(null, true, true);
					if (data == null) {
						failed = true;
						return;
					}
					for (Data next : getSelfContainedData(data)) {
						DataToolTab tab = createTab(next);
						addTab(tab);
					}
					int i = getTabCount() - 1;
					tabbedPane.setSelectedIndex(i);
					return;
				}
				addTabs(control, new Consumer<ArrayList<DataToolTab>>() {

					@Override
					public void accept(ArrayList<DataToolTab> tabs) {
						for (DataToolTab tab : tabs) {
							tab.setUserEditable(true);
						}
						tabbedPane.setSelectedIndex(getTabCount() - 1);
						refreshDataBuilder();
					}

				});
			} else {
				// pasted string is not xml, so parse to import Data //$NON-NLS-1$
				setMultipleTabPolicy(dataString);
				Data[] importedData = parseData(dataString, null);
				if (importedData == null) {
					failed = true;
					return;
				}
				if (source != null) {
					DataToolTab first = null;
					for (int i = 0; i < importedData.length; i++) {
						DataToolTab tab = createTab(importedData[i]);
						if (first == null)
							first = tab;
						addTab(tab);
						tab.userEditable = true;
						tab.refreshGUI();
						tab.tabChanged(false);
					}
					setSelectedTab(first);
				}
			}
		} finally {
			if (failed) {
				JOptionPane.showMessageDialog(DataTool.this, ToolsRes.getString("Tool.Dialog.NoData.Message"), //$NON-NLS-1$
						ToolsRes.getString("Tool.Dialog.NoData.Title"), //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE);
			} else {
				refreshDataBuilder();
			}
		}
	}

	/**
	 * add columns, possibly asynchronously
	 */
	protected void addColumnsFromPaste() {
		if (addableData != null) {
			DataToolTab tab = getSelectedTab();
			//OSPLog.finest("pasting columns into " + tab.getName()); //$NON-NLS-1$
			tab.addColumns(addableData, true, true, true);
		}
	}

	void editDataAction() {
		new EditDataDialog();
	}


	/**
	 * Refreshes the GUI.
	 */
	@Override
	protected void refreshGUI() {
		setTitle(ToolsRes.getString("DataTool.Frame.Title")); //$NON-NLS-1$
		emptyFileMenu.setText(ToolsRes.getString("Menu.File")); //$NON-NLS-1$
		emptyNewTabItem.setText(ToolsRes.getString("DataTool.MenuItem.NewTab")); //$NON-NLS-1$
		emptyOpenItem.setText(ToolsRes.getString("MenuItem.Open")); //$NON-NLS-1$
		emptyExitItem.setText(ToolsRes.getString("MenuItem.Exit")); //$NON-NLS-1$
		emptyEditMenu.setText(ToolsRes.getString("Menu.Edit")); //$NON-NLS-1$
		emptyPasteMenu.setText(ToolsRes.getString("MenuItem.Paste")); //$NON-NLS-1$
		emptyPasteTabItem.setText(ToolsRes.getString("DataTool.MenuItem.PasteNewTab")); //$NON-NLS-1$
		fileMenu.setText(ToolsRes.getString("Menu.File")); //$NON-NLS-1$
		newTabItem.setText(ToolsRes.getString("DataTool.MenuItem.NewTab")); //$NON-NLS-1$
		if (!OSPRuntime.appletMode) {
			openItem.setText(ToolsRes.getString("MenuItem.Open")); //$NON-NLS-1$
			importItem.setText(ToolsRes.getString("DataTool.MenuItem.Import")); //$NON-NLS-1$
			saveItem.setText(ToolsRes.getString("DataTool.MenuItem.Save")); //$NON-NLS-1$
			saveAsItem.setText(ToolsRes.getString("DataTool.MenuItem.SaveAs")); //$NON-NLS-1$
		}
		closeItem.setText(ToolsRes.getString("MenuItem.Close")); //$NON-NLS-1$
		closeAllItem.setText(ToolsRes.getString("MenuItem.CloseAll")); //$NON-NLS-1$
		printItem.setText(ToolsRes.getString("DataTool.MenuItem.Print")); //$NON-NLS-1$
		exitItem.setText(ToolsRes.getString("MenuItem.Exit")); //$NON-NLS-1$
		editMenu.setText(ToolsRes.getString("Menu.Edit")); //$NON-NLS-1$
		undoItem.setText(ToolsRes.getString("DataTool.MenuItem.Undo")); //$NON-NLS-1$
		redoItem.setText(ToolsRes.getString("DataTool.MenuItem.Redo")); //$NON-NLS-1$
		copyMenu.setText(ToolsRes.getString("DataTool.Menu.Copy")); //$NON-NLS-1$
		copyImageItem.setText(ToolsRes.getString("DataTool.MenuItem.CopyImage")); //$NON-NLS-1$
		pasteMenu.setText(ToolsRes.getString("MenuItem.Paste")); //$NON-NLS-1$
		pasteTabItem.setText(ToolsRes.getString("DataTool.MenuItem.PasteNewTab")); //$NON-NLS-1$
		pasteColumnsItem.setText(ToolsRes.getString("DataTool.MenuItem.PasteNewColumns")); //$NON-NLS-1$
		displayMenu.setText(ToolsRes.getString("Tool.Menu.Display")); //$NON-NLS-1$
		languageMenu.setText(ToolsRes.getString("Tool.Menu.Language")); //$NON-NLS-1$
		fontSizeMenu.setText(ToolsRes.getString("Tool.Menu.FontSize")); //$NON-NLS-1$
		defaultFontSizeItem.setText(ToolsRes.getString("Tool.MenuItem.DefaultFontSize")); //$NON-NLS-1$
		editDataItem.setText(ToolsRes.getString("DataToolTab.Button.EditData.Text")+"..."); //$NON-NLS-1$
		editDataItem.setEnabled(getSelectedTab() != null && getSelectedTab().isUserEditable());
		helpMenu.setText(ToolsRes.getString("Menu.Help")); //$NON-NLS-1$
		helpItem.setText(ToolsRes.getString("DataTool.MenuItem.Help")); //$NON-NLS-1$
		logItem.setText(ToolsRes.getString("MenuItem.Log")); //$NON-NLS-1$
		aboutItem.setText(ToolsRes.getString("MenuItem.About")); //$NON-NLS-1$
		Locale[] locales = OSPRuntime.getInstalledLocales();
		for (int i = 0; i < locales.length; i++) {
			if (locales[i].getLanguage().equals(ToolsRes.getLanguage())) {
				languageItems[i].setSelected(true);
			}
		}
	}

	/**
	 * Refreshes decimal separators in all tabs.
	 */
	public void refreshDecimalSeparators() {
		for (int i = 0; i < getTabCount(); i++) {
			getTab(i).refreshDecimalSeparators();
		}
	}

	/**
	 * Determines if the clipboard has pastable data.
	 *
	 * @return true if data is pastable
	 */
	protected boolean hasPastableData() {
		if (OSPRuntime.isJS)
			return true; // just guessing
		return isPastableData(OSPRuntime.paste(null));
	}

	private boolean isPastableData(String dataString) {
		controlContainsData = false;
		if (dataString == null)
			return false;
		if (!dataString.startsWith("<?xml")) { //$NON-NLS-1$
			Data[] temp = parseData(dataString, null);
			addableData = temp == null? null: temp[0];
			return (addableData != null);
		}
		control = new XMLControlElement();
		control.readXML(dataString);
		Class<?> type = control.getObjectClass();
		if (Data.class.isAssignableFrom(type)) {
			addableData = (Data) control.loadObject(null);
		} else if (!DataToolTab.class.isAssignableFrom(type)) {
			// find all Data objects in the control
			XMLTree tree = new XMLTree(control);
			tree.setHighlightedClass(Data.class);
			tree.selectHighlightedProperties();
			if (!tree.getSelectedProperties().isEmpty()) {
				controlContainsData = true;
			}
		}
		return (addableData != null) || DataToolTab.class.isAssignableFrom(type) || controlContainsData;
	}

	/**
	 * Determines if the clipboard has columns that are pastable into a specified
	 * tab.
	 *
	 * @param tab the tab
	 * @return true if clipboard has pastable columns
	 */
	protected boolean hasPastableColumns(DataToolTab tab) {
		boolean pastable = false;
		if (addableData != null) {
			// columns are pastable if tab name is different or tab is empty
			String dataName = addableData.getName();
			if (tab.dataManager.getDatasetsRaw().isEmpty() || ((dataName != null) && !dataName.equals(tab.getName()))) {
				pastable = true;
			}
		}
		return pastable || controlContainsData;
	}

	/**
	 * Shows the about dialog.
	 */
	protected void showAboutDialog() {
		String date = OSPRuntime.getLaunchJarBuildDate();
		if ("".equals(date))
			date = OSPRuntime.RELEASE_DATE;
		String toolname = ToolsRes.getString("DataTool.Frame.Title");
		String aboutString = ToolsRes.getString("LibraryBrowser.Version")+ " " + OSPRuntime.VERSION + "\nRelease date " + date + "\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "Open Source Physics\n" //$NON-NLS-1$
				+ "www.opensourcephysics.org"; //$NON-NLS-1$
		JOptionPane.showMessageDialog(this, aboutString, ToolsRes.getString("Dialog.About.Title") + " " + toolname, //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	protected void setMultipleTabPolicy(String dataString) {
		if (askToLoadMultipleTracksInSingleTab &&
				dataString.startsWith("#multi:")) {
			JPanel panel = new JPanel(new BorderLayout());
			JLabel label1 = new JLabel(ToolsRes.getString("DataTool.Dialog.Multitab.Message1"));
			JLabel label2 = new JLabel(ToolsRes.getString("DataTool.Dialog.Multitab.Message2"));
			JCheckBox checkbox = new JCheckBox(ToolsRes.getString("DataTool.Dialog.Multitab.Checkbox.Text"));
			checkbox.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
			Box box = Box.createVerticalBox();
			box.add(label1);
			box.add(label2);
			panel.add(box, BorderLayout.NORTH);
			panel.add(checkbox, BorderLayout.SOUTH);
			boolean[] multipleTabs = new boolean[] {true};
			new AsyncDialog().showConfirmDialog(null, 
					panel,
					ToolsRes.getString("DataTool.Dialog.Multitab.Title"), 
					JOptionPane.YES_NO_OPTION, 
					(ev) -> {
						if (ev.getID() == JOptionPane.NO_OPTION)
							multipleTabs[0] = false;
					});
			loadMultipleTracksInSingleTab = !multipleTabs[0];
			askToLoadMultipleTracksInSingleTab = !checkbox.isSelected();
		}
	}

	/**
	 * Creates a button with a specified text.
	 *
	 * @param text the button text
	 * @return the button
	 */
	protected static JButton createButton(String text) {
		return createButton(text, true);
	}
	
	protected static JButton createButton(String text, boolean allowBorder) {

		JButton button = new JButton(text) {
			@Override
			public Dimension getMaximumSize() {
				Dimension dim = super.getMaximumSize();
				dim.height = buttonHeight;
				return dim;
			}

			@Override
			public void setBorder(Border b) {
				if (allowBorder || b instanceof EmptyBorder)
				super.setBorder(b);
				// don't do this - color button will not be painted
			}
		};
		return button;
	}

	public class FileDropHandler extends TransferHandler {

		public FileDropHandler() {
		}

		Boolean isDropOK = null;

		/**
		 * Check to see that we can import this file. It if is NOT a video-type file
		 * (mp4, jpg, etc) then set the drop action to COPY rather than MOVE.
		 * 
		 */
		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			return true;
		}

		@Override
		public boolean importData(TransferHandler.TransferSupport support) {
			for (File f : getFileList(support.getTransferable())) {
				open(f);
			}
			return true;
		}

		@SuppressWarnings("unchecked")
		private List<File> getFileList(Transferable t) {
			try {
				return (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
			} catch (Exception e) {
				return null;
			}
		}

	}
	
	/**
	 * A class to find an exponential moving average (weighting for
	 * previous values fall off by factor exp(-alpha)).
	 */
	public static class ExpMovingAvg {
    private double alpha;
    private Double oldValue;
    
  	/**
  	 * Constructor. 
  	 * @param alpha exponential constant 
  	 */
    public ExpMovingAvg(double alpha) {
      this.alpha = alpha;
    }

  	/**
  	 * Returns the moving average 
  	 * @param value the raw value 
  	 */
    public double average(double value) {
      if (oldValue == null) {
          oldValue = value;
          return value;
      }
      double newValue = oldValue + alpha * (value - oldValue);
      oldValue = newValue;
      return newValue;
    }
    
 }

	/**
	 * A dialog to edit table data using delimited text.
	 */
	public class EditDataDialog extends JDialog {
		JButton okButton, cancelButton, applyButton;
		JTextArea dataArea;
		JTextArea helpArea;
		boolean canceled;
		private String currentData;
		private JButton undoButton, redoButton;
		private String defaultColumnNameText = "x, y\n";
		private int undoCount = 0;

		EditDataDialog() {
			super(JOptionPane.getFrameForComponent(DataTool.this), true);
			setLayout(new BorderLayout());
			setTitle(ToolsRes.getString("DataToolTab.Button.EditData.Text")); //$NON-NLS-1$
			// create buttons
			okButton = new JButton(DisplayRes.getString("Dialog.Button.Close.Text")); //$NON-NLS-1$
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
			applyButton = new JButton(DisplayRes.getString("Dialog.Button.Apply.Text")); //$NON-NLS-1$
			applyButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setData(null);
					refreshUndoButtons();
 				}
			});
			undoButton = new JButton(ToolsRes.getString("DataTool.MenuItem.Undo")); //$NON-NLS-1$
			undoButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					undoItem.doClick(0);
					refreshUndoButtons();
					undoCount--;
					refreshDataTextFromTable(false);
				}
			});
			redoButton = new JButton(ToolsRes.getString("DataTool.MenuItem.Redo")); //$NON-NLS-1$
			redoButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					redoItem.doClick(0);
					refreshUndoButtons();
					undoCount++;
					refreshDataTextFromTable(false);
 				}
			});
			cancelButton = new JButton(DisplayRes.getString("GUIUtils.Cancel")); //$NON-NLS-1$
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					canceled = true;
					setVisible(false);
				}
			});
			dataArea = new JTextArea(20,30);
			dataArea.setCaretColor(Color.red);
			dataArea.setMargin(new Insets(1,1,1,1));
			add(new JScrollPane(dataArea), BorderLayout.CENTER);
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(applyButton);
			buttonPanel.add(undoButton);
			buttonPanel.add(redoButton);
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel, BorderLayout.SOUTH);
			helpArea = new JTextArea(2,30);
			helpArea.setEditable(false);
			helpArea.setLineWrap(true);
			helpArea.setForeground(Color.green.darker());
			helpArea.setText(ToolsRes.getString("DataTool.Dialog.EditData.Help1") + "\n" + //$NON-NLS-1$
					ToolsRes.getString("DataTool.Dialog.EditData.Help2")); //$NON-NLS-1$
			Border line = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
			Border space = BorderFactory.createEmptyBorder(4, 6, 4, 6);
			helpArea.setBorder(BorderFactory.createCompoundBorder(line, space));
			add(helpArea, BorderLayout.NORTH);
			refreshUndoButtons();
			FontSizer.setFont(dataArea);
			FontSizer.setFont(helpArea);
			this.setLocation(15, 30);
			pack();
			setVisible(true);
		}
		
		private void refreshUndoButtons() {
			undoButton.setEnabled(getSelectedTab().undoManager.canUndo());
			redoButton.setEnabled(getSelectedTab().undoManager.canRedo());			
		}

		@Override
		public void setVisible(boolean vis) {
			if (vis) {
				canceled = false;
				refreshDataTextFromTable(true); // initializes data0 when opened
			} else {
				if (canceled) {
					resetData();
				} else {
					if (!dataArea.getText().equals(defaultColumnNameText))
						setData(null); // sets data to dataArea text
				}
				getSelectedTab().dataTable.refreshTable();
			}
			super.setVisible(vis);
		}

		/**
		 * Sets dataArea text and currentData to clean text version of the data table.
		 * 
		 * @param init true to initialize data0 for the cancel action
		 */
		void refreshDataTextFromTable(boolean init) {
			getSelectedTab().dataTable.selectAll();
			String data = getSelectedTab().getSelectedTableData(false, VideoIO.COMMA); // always show raw data
			getSelectedTab().dataTable.clearSelection();
			data = data.substring(data.indexOf("\n") + 1); // eliminates tab name			
			if (data.length() == 0) { // no data
				currentData = "";
				dataArea.setText(init? defaultColumnNameText: "");
			} else {
				data = data.replace('\t', ',');
				currentData = cleanData(data); // adds space after each comma for readability 
				dataArea.setText(currentData);
			}
		}
		
		/**
		 * Cleans delimited text data by trimming individual entries and separating by ", ".
		 * 
		 * @param data the delimited text.
		 */
		private String cleanData(String data) {
			String newData="";
			if (data==null || data.trim().equals("")) return("");
			String[] lines=data.trim().split("\\r?\\n");
			String[] colNames = lines[0].split(",");
			int j=0;
			for (j=0; j<colNames.length; j++) { 
				newData+=colNames[j].trim()+", ";
			}
			newData=newData.substring(0, newData.lastIndexOf(','))+'\n';
			 
			for (int i=1; i<lines.length; i++)  { 
				String[] line = lines[i].split(",");
				for (j=0; j<line.length; j++) {
					newData+=line[j].trim()+", ";
				}
				// pad missing data with space
				for (int k=j; k<colNames.length; k++) { 
					newData+= ", ";
				}
				newData=newData.substring(0, newData.lastIndexOf(','))+'\n';
			}
			return newData;
		}

		/**
		 * Sets the table data to the specified delimited text. If dataString is null
		 * then it is taken from the dataArea and an undoable edit is posted.
		 * 
		 * @param dataString the delimited text. May be null.
		 */
		void setData(String dataString) {
			if (dataString == null) {
				dataString = dataArea.getText().trim() + "\n";
			}
			dataString = cleanData(dataString);
			if (dataString.equals(currentData))
				return;
			DataToolTab tab = getSelectedTab();
			try {
				// set new currentData to dataString using existing currentData if posting edit
				if (tab.setDelimitedData(dataString, currentData))
					undoCount++;
				currentData = dataString;
				refreshDataTextFromTable(false); // don't reset data0 for cancel action
			} catch (Exception e) {
				resetData();
			}
		}

		void resetData() {
			for (int i = 0; i < undoCount; i++) {
				undoItem.doClick(0);
			}
			for (int i = 0; i < -undoCount; i++) {
				redoItem.doClick(0);
			}
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
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
