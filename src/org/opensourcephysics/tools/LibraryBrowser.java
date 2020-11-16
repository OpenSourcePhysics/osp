/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.desktop.OSPDesktop;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.ResizableIcon;
import org.opensourcephysics.display.TextFrame;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.tools.LibraryResource.Metadata;

import javajs.async.AsyncFileChooser;

/**
 * A GUI for browsing OSP digital library collections.
 *
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
public class LibraryBrowser extends JPanel {

	// static constants
	@SuppressWarnings("javadoc")
	public static final String TRACKER_LIBRARY = "https://physlets.org/tracker/library/tracker_library.xml"; //$NON-NLS-1$
	@SuppressWarnings("javadoc")
	public static final String SHARED_LIBRARY = "https://physlets.org/tracker/library/shared_library.xml"; //$NON-NLS-1$
	protected static final String AND = " AND "; //$NON-NLS-1$
	protected static final String OR = " OR "; //$NON-NLS-1$
	protected static final String OPENING = "("; //$NON-NLS-1$
	protected static final String CLOSING = ")"; //$NON-NLS-1$
	protected static final String MY_LIBRARY_NAME = "my_library.xml"; //$NON-NLS-1$
	protected static final String MY_COLLECTION_NAME = "my_collection.xml"; //$NON-NLS-1$
	protected static final String RECENT_COLLECTION_NAME = "recent_collection.xml"; //$NON-NLS-1$
	protected static final String LIBRARY_HELP_NAME = "library_browser_help.html"; //$NON-NLS-1$
	protected static final String LIBRARY_HELP_BASE = "http://www.opensourcephysics.org/online_help/tools/"; //$NON-NLS-1$
	protected static final String WINDOWS_OSP_DIRECTORY = "/My Documents/OSP/"; //$NON-NLS-1$
	protected static final String OSP_DIRECTORY = "/Documents/OSP/"; //$NON-NLS-1$
	public static final String HINT_LOAD_RESOURCE = "LOAD";
	public static final String HINT_DOWNLOAD_RESOURCE = "DOWNLOAD";

	// static fields
	private static String ospPath;
	private static LibraryBrowser browser;
	protected static Border buttonBorder;
	private static boolean checkedWebConnection = false;
	protected static JFrame frame;
	protected static JDialog externalDialog;
	protected static JMenuBar menubar;
	protected static ResizableIcon expandIcon, contractIcon, heavyExpandIcon, heavyContractIcon;
	protected static ResizableIcon refreshIcon, downloadIcon, downloadDisabledIcon;
	protected static final FileFilter TRACKER_FILTER = new TrackerDLFilter();
	protected static javax.swing.filechooser.FileFilter filesAndFoldersFilter = new FilesAndFoldersFilter();
	protected static Timer searchTimer;
	protected static String searchTerm;
	public static boolean fireHelpEvent = false;
	public static int maxRecentCollectionSize = 18;

	static {
		buttonBorder = BorderFactory.createEtchedBorder();
		Border space = BorderFactory.createEmptyBorder(1, 2, 2, 2);
		buttonBorder = BorderFactory.createCompoundBorder(buttonBorder, space);
		space = BorderFactory.createEmptyBorder(0, 1, 0, 1);
		buttonBorder = BorderFactory.createCompoundBorder(space, buttonBorder);
		menubar = new JMenuBar();
		String imageFile = "/org/opensourcephysics/resources/tools/images/expand.png"; //$NON-NLS-1$
		expandIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/contract.png"; //$NON-NLS-1$
		contractIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/expand_bold.png"; //$NON-NLS-1$
		heavyExpandIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/contract_bold.png"; //$NON-NLS-1$
		heavyContractIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/refresh.gif"; //$NON-NLS-1$
		refreshIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/download.gif"; //$NON-NLS-1$
		downloadIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/downloaddisabled.gif"; //$NON-NLS-1$
		downloadDisabledIcon = ResourceLoader.getResizableIcon(imageFile);
	}

	// instance fields
	protected boolean webConnected = OSPRuntime.isJS;
	protected boolean localLibraryLoaded = false;
	protected Library library = new Library();
	protected String libraryPath;
	protected JToolBar toolbar;
	protected JButton messageButton;
	protected Action commandAction, searchAction, openRecentAction, downloadAction;
	protected JLabel commandLabel, searchLabel;
	protected JTextField commandField, searchField;
	protected JMenu fileMenu, recentMenu, collectionsMenu, manageMenu, helpMenu;
	protected JMenuItem newItem, openItem, saveItem, saveAsItem, closeItem, closeAllItem, exitItem, deleteItem,
			collectionsItem, searchItem, cacheItem, aboutItem, logItem, helpItem;
	protected JButton commandButton, editButton, refreshButton, downloadButton;
	protected ActionListener loadCollectionAction;
	protected boolean exitOnClose, isCancelled;
	protected JTabbedPane tabbedPane;
	protected JScrollPane htmlScroller;
	protected PropertyChangeListener treePanelListener;
	protected boolean keyPressed, textChanged;
	protected TextFrame helpFrame;
	protected JEditorPane htmlAboutPane;
	protected FileFilter dlFileFilter = TRACKER_FILTER;
	protected boolean isResourcePathXML;
	protected LibraryManager libraryManager;
	private int myFontLevel;
	public static final String PROPERTY_LIBRARY_TARGET = "target";
	public static final String PROPERTY_LIBRARY_EDITED = "collection_edit";

	/**
	 * Gets the shared singleton browser.
	 * 
	 * @return the shared LibraryBrowser
	 */
	public static LibraryBrowser getBrowser() {
		if (browser == null) {
			browser = getBrowser(null);
		}
		return browser;
	}

	/**
	 * Gets the shared singleton browser in a JDialog or, if none, in a shared
	 * JFrame.
	 * 
	 * @param dialog a JDialog (if null, browser is returned in a JFrame)
	 * @return the shared LibraryBrowser
	 */
	public static LibraryBrowser getBrowser(JDialog dialog) {
		boolean newFrame = false;
		if (frame == null && dialog == null) {
			newFrame = true;
			frame = new JFrame();
		}
		externalDialog = dialog;
		if (externalDialog != null)
			externalDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		if (browser == null) {
			String libraryPath = null;
			if (!OSPRuntime.isJS) {

				String ospPath = getOSPPath();
				libraryPath = ospPath + MY_LIBRARY_NAME;
				File libraryFile = new File(libraryPath);
				// create new library if none exists
				boolean libraryExists = libraryFile.exists();
				if (!libraryExists) {
					String collectionPath = ospPath + MY_COLLECTION_NAME;
					File collectionFile = new File(collectionPath);
					// create new collection if none exists
					if (!collectionFile.exists()) {
						String name = ToolsRes.getString("LibraryCollection.Name.Local"); //$NON-NLS-1$
						LibraryCollection collection = new LibraryCollection(name);
						String base = XML.getDirectoryPath(collectionPath);
						collection.setBasePath(XML.forwardSlash(base));
						// save new collection
						XMLControl control = new XMLControlElement(collection);
						control.write(collectionPath);
					}
					
					Library library = new Library();
					String name = ToolsRes.getString("LibraryCollection.Name.Local"); //$NON-NLS-1$
					library.addCollection(collectionPath, name);
					library.save(libraryPath);
				}

			}
			browser = new LibraryBrowser(libraryPath);

			LibraryTreePanel treePanel = browser.getSelectedTreePanel();
			if (treePanel != null) {
				treePanel.setSelectedNode(treePanel.rootNode);
				treePanel.showInfo(treePanel.rootNode, "LibraryBrowser.getBrowser");
			}
			OSPLog.getOSPLog(); // instantiate log in case of exceptions, etc
		}

		browser.setTitle(ToolsRes.getString("LibraryBrowser.Title")); //$NON-NLS-1$
		if (externalDialog != null) {
			externalDialog.setContentPane(browser);
			externalDialog.setJMenuBar(menubar);
			externalDialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					browser.exit();
				}
			});
			externalDialog.pack();
		} else {
			frame.setContentPane(browser);
			frame.setJMenuBar(menubar);
			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			// add window listener to exit
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					browser.exit();
				}
			});
			try {
				java.net.URL url = ResourceLoader.getImageZipResource(OSPRuntime.OSP_ICON_FILE);
				ImageIcon icon = new ImageIcon(url);
				frame.setIconImage(icon.getImage());
			} catch (Exception ex) {
			}
			frame.pack();
			if (newFrame) {
				// center on screen
				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				int x = (dim.width - frame.getBounds().width) / 2;
				int y = (dim.height - frame.getBounds().height) / 2;
				frame.setLocation(x, y);
			}
		}

		return browser;
	}
	
	public void setAlwaysOnTop(boolean alwaysOnTop) {
		frame.setAlwaysOnTop(alwaysOnTop);
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the desired font level
	 */
	public void setFontLevel(int level) {
		if (myFontLevel == level)
			return;
		myFontLevel = FontSizer.setFonts(frame);
		Font font = tabbedPane.getFont();
		tabbedPane.setFont(FontSizer.getResizedFont(font, level));
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			LibraryTreePanel treePanel = getTreePanel(i);
			treePanel.setFontLevel(level);
		}
		if (libraryManager != null) {
			libraryManager.setFontLevel(level);
		}
		FontSizer.setFonts(OSPLog.getOSPLog());
	}

	/**
	 * Imports a library with a specified path.
	 * 
	 * @param path the path to the Library xml file
	 */
	public void importLibrary(final String path) {
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				library.importLibrary(path);
				refreshCollectionsMenu();
			}
		};
		new Thread(runner).start();
	}

	/**
	 * Adds an OSP-sponsored library with a specified path.
	 * 
	 * @param path the path to the Library xml file
	 */
	public void addOSPLibrary(final String path) {
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				library.addOSPLibrary(path);
				refreshCollectionsMenu();
			}
		};
		new Thread(runner).start();
	}

	/**
	 * Adds a ComPADRE collection with a specified path.
	 * 
	 * @param path the ComPADRE query
	 */
	public void addComPADRECollection(String path) {
		library.addComPADRECollection(path, LibraryComPADRE.getCollectionName(path));
	}

	/**
	 * Refreshes the Collections menu.
	 */
	synchronized public void refreshCollectionsMenu() {
		JMenu menu = collectionsMenu;
		menu.removeAll();
		if (!OSPRuntime.isJS) {
			JMenu myLibraryMenu = new JMenu(ToolsRes.getString("Library.Name.Local")); //$NON-NLS-1$
			menu.add(myLibraryMenu);
			if (!library.pathList.isEmpty()) {
				for (String path : library.pathList) {
					String name = library.pathToNameMap.get(path);
					JMenuItem item = new JMenuItem(name);
					myLibraryMenu.add(item);
					item.addActionListener(loadCollectionAction);
					item.setToolTipText(path);
					item.setActionCommand(path);
				}
			}
		}
		if (!library.comPADREPathList.isEmpty()) {
			JMenu submenu = new JMenu(ToolsRes.getString("Library.Name.ComPADRE")); //$NON-NLS-1$
			menu.add(submenu);
			for (String path : library.comPADREPathList) {
				String name = library.comPADREPathToNameMap.get(path);
				JMenuItem item = new JMenuItem(name);
				submenu.add(item);
				item.addActionListener(loadCollectionAction);
//	  		if (LibraryComPADRE.primary_only)
//	  			path += LibraryComPADRE.PRIMARY_ONLY;
				item.setToolTipText(path);
				item.setActionCommand(path);
			}
		}
		if (!library.ospPathList.isEmpty()) {
			for (String path : library.ospPathList) {
				Library lib = library.ospPathToLibraryMap.get(path);
				JMenu submenu = new JMenu(lib.getName());
				menu.add(submenu);
				populateSubMenu(submenu, lib);
			}
		}
		if (!library.importedPathList.isEmpty()) {
			menu.addSeparator();
			for (String path : library.importedPathList) {
				Library lib = library.importedPathToLibraryMap.get(path);
				JMenu submenu = new JMenu(lib.getName());
				menu.add(submenu);
				for (String next : lib.pathList) {
					String name = lib.pathToNameMap.get(next);
					JMenuItem item = new JMenuItem(name);
					submenu.add(item);
					item.addActionListener(loadCollectionAction);
					item.setToolTipText(next);
					item.setActionCommand(next);
				}
			}
		}
		FontSizer.setMenuFonts(collectionsMenu);
	}

	/**
	 * Populates a submenu.
	 * 
	 * @param menu the menu to populate
	 * @param lib  the library with collections for the submenu
	 */
	private void populateSubMenu(JMenu menu, Library lib) {
		for (String next : lib.pathList) {
			String name = lib.pathToNameMap.get(next);
			JMenuItem item = new JMenuItem(name);
			menu.add(item);
			item.addActionListener(loadCollectionAction);
			item.setToolTipText(next);
			item.setActionCommand(next);
		}
		if (!lib.subPathList.isEmpty()) {
			for (String path : lib.subPathList) {
				if (library.ospPathList.contains(path))
					continue;
				Library sublib = lib.subPathToLibraryMap.get(path);
				JMenu submenu = new JMenu(sublib.getName());
				menu.add(submenu);
				populateSubMenu(submenu, sublib);
			}
		}

	}

	/**
	 * Sets the title of this DL browser.
	 * 
	 * @param title the title
	 */
	public void setTitle(String title) {
		if (frame != null) {
			frame.setTitle(title);
		} else if (externalDialog != null) {
			externalDialog.setTitle(title);
		}
	}

	/**
	 * Gets the fileFilter used to determine which files are DL resources.
	 * 
	 * @return the file filter
	 */
	public FileFilter getDLFileFilter() {
		return dlFileFilter;
	}

	/**
	 * Sets the fileFilter used to determine which files are DL resources.
	 * 
	 * @param filter the file filter (may be null)
	 */
	public void setDLFileFilter(FileFilter filter) {
		dlFileFilter = filter;
	}

	/**
	 * Sets the visibility of this browser
	 * 
	 * @param vis true to show, false to hide
	 */
	@Override
	public void setVisible(boolean vis) {
		super.setVisible(vis);
		Window c = (Window) getTopLevelAncestor();
		if (c != null) {
			c.setVisible(vis);
			if (vis) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						c.toFront();
					}

				});
			}
		}
//		if (externalDialog != null) {
//			externalDialog.setVisible(vis);
//			if (vis)
//				externalDialog.toFront();
//		} else {
//			frame.setVisible(vis);
//			if (vis)
//				frame.toFront();
//		}
	}

	/**
	 * Exits this browser.
	 * 
	 * @return true if exited, false if cancelled by user
	 */
	public boolean exit() {
		// request focus?
		LibraryTreePanel selected = getSelectedTreePanel();
		if (selected != null)
			selected.refreshEntryFields();
		String recentCollectionPath = OSPRuntime.isJS? null:
			LibraryBrowser.getOSPPath()+LibraryBrowser.RECENT_COLLECTION_NAME;
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			LibraryTreePanel treePanel = getTreePanel(i);
			if (!treePanel.saveChanges(getTabTitle(i)))
				return false; // true unless the user cancels
			if (!OSPRuntime.isJS && recentCollectionPath.equals(treePanel.rootResource.collectionPath)) {
	  		treePanel.save();
			}
		}
		// determine which open tabs to save
		ArrayList<String> tabsToSave = new ArrayList<String>();
		int n = tabbedPane.getTabCount();
		for (int i = 0; i < n; i++) {
			String path = getTreePanel(i).pathToRoot;
			if (path.equals("")) //$NON-NLS-1$
				continue;
			tabsToSave.add(path);
		}
		library.openTabPaths = tabsToSave.isEmpty() ? null : tabsToSave.toArray(new String[tabsToSave.size()]);
		// save library if previously loaded
		if (localLibraryLoaded)
			library.save(libraryPath);

		if (exitOnClose) {
			System.exit(0);
		} else {
			refreshGUI();
			setVisible(false);
		}
		return true;
	}
	
	public boolean isCancelled() {
		return isCancelled;
	}
	
	public void setCanceled(boolean b) {
		isCancelled = b;
		if (isCancelled) {
			setMessage("Loading cancelled", Color.WHITE);
			Timer timer = new Timer(2000, (ev) -> {
				setCanceled(false);
			});
			timer.setRepeats(false);
			timer.start();
		} else {
			setComandButtonEnabled(true);
			isCancelled = false;
			LibraryTreePanel treePanel = getSelectedTreePanel();
			LibraryTreeNode node = treePanel.getSelectedNode();
			setMessage(node == null ? "" : node.getToolTip(), null);
		}
	}

//____________________ private and protected methods ____________________________

	/**
	 * Private constructor to prevent instantiation except for singleton.
	 * 
	 * @param libraryPath the path to a Library xml file
	 */
	private LibraryBrowser(String libraryPath) {
		super(new BorderLayout());
		this.libraryPath = libraryPath;
		library.browser = this;
		createGUI();
		refreshGUI(true);
		refreshCollectionsMenu();
		editButton.requestFocusInWindow();
		ToolsRes.addPropertyChangeListener("locale", new PropertyChangeListener() { //$NON-NLS-1$
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				refreshGUI(true);
				refreshCollectionsMenu();
				if (libraryManager != null)
					libraryManager.refreshGUI();
				LibraryTreePanel.clearMaps();
				LibraryTreePanel treePanel = getSelectedTreePanel();
				if (treePanel != null)
					treePanel.showInfo(treePanel.getSelectedNode(), "LibraryBrowser locale change");
			}
		});
	}

	/**
	 * Gets the library manager for this browser.
	 * 
	 * @return the collections manager
	 */
	protected LibraryManager getManager() {
		if (libraryManager == null) {
			if (externalDialog != null)
				libraryManager = new LibraryManager(this, LibraryBrowser.externalDialog);
			else
				libraryManager = new LibraryManager(this, LibraryBrowser.frame);
			libraryManager.refreshGUI();
			// center on screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - libraryManager.getBounds().width) / 2;
			int y = (dim.height - libraryManager.getBounds().height) / 2;
			libraryManager.setLocation(x, y);
		}
		if (library.pathList.size() > 0 && libraryManager.collectionList.getSelectedIndex() == -1) {
			libraryManager.collectionList.setSelectedIndex(0);
		}
		if (library.importedPathList.size() > 0 && libraryManager.guestList.getSelectedIndex() == -1) {
			libraryManager.guestList.setSelectedIndex(0);
		}
		libraryManager.setFontLevel(FontSizer.getLevel());
		return libraryManager;
	}

	/**
	 * Gets the selected LibraryTreePanel, if any.
	 * 
	 * @return the selected treePanel, or null if none
	 */
	public LibraryTreePanel getSelectedTreePanel() {
		return (LibraryTreePanel) tabbedPane.getSelectedComponent();
	}

	/**
	 * Gets the LibraryTreePanel at a specified tab index.
	 * 
	 * @param index the tab index
	 * @return the treePanel
	 */
	protected LibraryTreePanel getTreePanel(int index) {
		return (LibraryTreePanel) tabbedPane.getComponentAt(index);
	}

	/**
	 * Gets the title of the tab associated with a given path.
	 * 
	 * @param path the collection path
	 * @return the tab title
	 */
	protected String getTabTitle(String path) {
		int i = getTabIndexFromPath(path);
		return i > -1 ? getTabTitle(i) : null;
	}

	/**
	 * Gets the title of the tab at a given index.
	 * 
	 * @param index the tab index
	 * @return the tab title
	 */
	protected String getTabTitle(int index) {
		String title = tabbedPane.getTitleAt(index);
		if (title.endsWith("*")) //$NON-NLS-1$
			title = title.substring(0, title.length() - 1);
		return title;
	}

	/**
	 * Gets the index of the tab associated with a given path.
	 * 
	 * @param path the collection path
	 * @return the tab index
	 */
	protected int getTabIndexFromPath(String path) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			LibraryTreePanel next = getTreePanel(i);
			if (next.pathToRoot.equals(path))
				return i;
		}
		return -1;
	}

	/**
	 * Gets the index of the tab associated with a given title.
	 * 
	 * @param title the tab title
	 * @return the tab index
	 */
	protected int getTabIndexFromTitle(String title) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			String next = tabbedPane.getTitleAt(i);
			if (next.equals(title))
				return i;
		}
		return -1;
	}

	/**
	 * Loads a tab from a given path. If the tab is already loaded, this selects it.
	 * if not yet loaded, this adds a new tab and selects it. If a treePath is
	 * specified, the node it points to will be selected
	 * 
	 * @param path     the path
	 * @param treePath tree path to select in root-first order (may be null)
	 */
	protected void loadTab(String path, List<String> treePath) {
		if (path == null)
			return;
		path = XML.forwardSlash(path);
		library.addRecent(path, false);
		refreshRecentMenu();
		// select tab and treePath if path is already loaded
		int i = getTabIndexFromPath(path);
		if (i > -1) {
			tabbedPane.setSelectedIndex(i);
			LibraryTreePanel treePanel = getTreePanel(i);
			treePanel.setSelectionPath(treePath);
			return;
		}
		// otherwise add new tab
		loadTabAndListen(path, treePath, "LoadTab");
	}

	protected boolean loadTabAndListen(String path, List<String> treePath, String mode) {
		return addTabAndExecute(path, treePath, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if ("progress".equals(e.getPropertyName())) { //$NON-NLS-1$
					Integer n = (Integer) e.getNewValue();
					if (n > -1) {
						OSPRuntime.showStatus("LibaryBrowser: " + n);//$NON-NLS-1$
						tabbedPane.setSelectedIndex(n);
						switch (mode) {
						case "LoadTab"://$NON-NLS-1$
							break;
						case "OpenRecent"://$NON-NLS-1$
							refreshGUI();
							break;
						case "CreateNew"://$NON-NLS-1$
							LibraryTreePanel treePanel = getSelectedTreePanel();
							treePanel.setEditing(true);
							refreshGUI();
							break;
						}
					}
				}
			}
		});
	}
	
  /**
   * Gets the recent collection.
   * 
   * @return the collection, or null if failed
   */
  public LibraryCollection getRecentCollection() {
  	if (OSPRuntime.isJS)
  		return null;

  	String path = getOSPPath()+RECENT_COLLECTION_NAME;
  	// check open tabs
  	int i = getTabIndexFromPath(path);
  	if (i>-1) {
      LibraryTreePanel treePanel = getTreePanel(i);
      LibraryResource record = treePanel.rootResource;
      if (record instanceof LibraryCollection) {
      	return (LibraryCollection)record;
      }
  	}
  	
		File recentCollectionFile = new File(path);
		// create recent collection if none exists
    if (!recentCollectionFile.exists()) {
      String name = ToolsRes.getString("LibraryCollection.Name.Recent"); //$NON-NLS-1$
			LibraryCollection recentCollection = new LibraryCollection(name);
			String base = XML.getDirectoryPath(path);
			recentCollection.setBasePath(XML.forwardSlash(base));
			// save new collection
			XMLControl control = new XMLControlElement(recentCollection);
			control.write(path);
    }
    
   	XMLControlElement control = new XMLControlElement(path);
  	if (!control.failedToRead() 
  			&& control.getObjectClass()!=null
  			&& LibraryCollection.class.isAssignableFrom(control.getObjectClass())) {
  		LibraryCollection collection = (LibraryCollection)control.loadObject(null);
  		collection.collectionPath = path;
  		return collection;
  	}
  	return null;
  }
  


	protected void loadResourceAsync(String path, Function<LibraryResource, Void> whenDone) {
		isResourcePathXML = false;

		// BH moving this forward; Java was looking at an "https://..." file
		// path to check if it was a directory?? SwingJS was allowing it to be a
		// directory
		if (LibraryComPADRE.isComPADREPath(path)) {
			whenDone.apply(LibraryComPADRE.getCollection(path));
			return;
		}

		// was first:
		File targetFile = new File(path);
		if (targetFile.isDirectory()) {
			whenDone.apply(createCollectionFromDirectory(targetFile, targetFile, dlFileFilter));
			return;
		}

		XMLControlElement control = new XMLControlElement();
		control.readAsync(path, (fullPath) -> {
				if (control.failedToRead() || control.getObjectClass() == null
						|| !LibraryResource.class.isAssignableFrom(control.getObjectClass())) {
					whenDone.apply(createResource(targetFile, targetFile.getParentFile(), dlFileFilter));
				} else {
					isResourcePathXML = true;
					whenDone.apply((LibraryResource) control.loadObject(null));
				}
				return null;
		});

	}

	/**
	 * Loads a library resource from a given path.
	 * 
	 * @param path the path
	 * @return the resource, or null if failed
	 */
	protected LibraryResource loadResource(String path) {
		isResourcePathXML = false;

		// BH moving this forward; Java was looking at an "https://..." file
		// path to check if it was a directory?? SwingJS was allowing it to be a
		// directory
		if (LibraryComPADRE.isComPADREPath(path)) {
			return LibraryComPADRE.getCollection(path);
		}

		boolean isHTTP = ResourceLoader.isHTTP(path);

		// BH 2020.11.12 presumes file not https here
		if (!isHTTP)
		path = ResourceLoader.getNonURIPath(path);
		// was first:
		File targetFile = null;

			targetFile = new File(path);
		if (targetFile.isDirectory()) {
			return createCollectionFromDirectory(targetFile, targetFile, dlFileFilter);
		}
		
		if (!dlFileFilter.accept(targetFile)) {
			XMLControlElement control = (isHTTP ? new XMLControlElement(path) : new XMLControlElement(targetFile));
			if (!control.failedToRead() && control.getObjectClass() != null
					&& LibraryResource.class.isAssignableFrom(control.getObjectClass())) {
				isResourcePathXML = true;
				return (LibraryResource) control.loadObject(null);
			}
		}
		
		return createResource(targetFile, targetFile.getParentFile(), dlFileFilter);
	}

	/**
	 * Creates a LibraryResource that describes and targets a file.
	 * 
	 * @param targetFile the target file
	 * @param baseDir    the base directory for relative paths
	 * @param filter     a FileFilter to determine if the file is a DL library
	 *                   resource
	 * @return a LibraryResource that describes and targets the file
	 */
	protected LibraryResource createResource(File targetFile, File baseDir, FileFilter filter) {
		if (targetFile == null || !targetFile.exists())
			return null;
		if (!filter.accept(targetFile))
			return null;

		String fileName = targetFile.getName();
		String path = XML.forwardSlash(targetFile.getAbsolutePath());
		String base = XML.forwardSlash(baseDir.getAbsolutePath());
		String relPath = XML.getPathRelativeTo(path, base);
		LibraryResource record = new LibraryResource(fileName);
		record.setBasePath(base);
		record.setTarget(relPath);

		fileName = fileName.toLowerCase();
		if (fileName.indexOf(".htm") > -1) { //$NON-NLS-1$
			record.setHTMLPath(relPath);
			record.setType(LibraryResource.HTML_TYPE);
		}
		if (fileName.endsWith(".zip")) { //$NON-NLS-1$
			if (filter == TRACKER_FILTER) {
				record.setType(LibraryResource.TRACKER_TYPE);
			}
		}
		if (fileName.endsWith(".trz")) { //$NON-NLS-1$
			record.setType(LibraryResource.TRACKER_TYPE);
		}
		return record;
	}

	/**
	 * Creates a LibraryCollection containing all DL resources in a target
	 * directory.
	 * 
	 * @param targetDir the target directory
	 * @param base      the base directory for relative paths
	 * @param filter    a FileFilter to determine which files are DL resources
	 * @return the collection
	 */
	private LibraryCollection createCollectionFromDirectory(File targetDir, File base, FileFilter filter) {
		// find HTML files in this folder
		FileFilter htmlFilter = new HTMLFilter();
		File[] htmlFiles = targetDir.listFiles(htmlFilter);
		HashSet<File> matchedNames = new HashSet<File>();

		String name = targetDir.getName();
		LibraryCollection collection = new LibraryCollection(name);
		if (base == targetDir) { // set base path ONLY for the root directory
			collection.setBasePath(XML.forwardSlash(base.getAbsolutePath()));
		}
		// look for HTML with name = folder name + "_info"
		for (File htmlFile : htmlFiles) {
			if (XML.stripExtension(htmlFile.getName()).equals(name + "_info")) { //$NON-NLS-1$
				String relPath = XML.getPathRelativeTo(htmlFile.getAbsolutePath(), base.getAbsolutePath());
				collection.setHTMLPath(relPath);
				String htmlCode = ResourceLoader.getHTMLCode(htmlFile.getAbsolutePath());
				String title = ResourceLoader.getTitleFromHTMLCode(htmlCode);
				if (title != null) {
					collection.setName(title);
				}
				matchedNames.add(htmlFile);
			}
		}

		// find subfolders
		File[] subdirs = targetDir.listFiles(new DirectoryFilter());
		for (File dir : subdirs) {
			LibraryCollection subCollection = createCollectionFromDirectory(dir, base, filter);
			if (subCollection.getResources().length > 0)
				collection.addResource(subCollection);
		}

		// find filtered DL resources
		File[] resourceFiles = filter == null ? targetDir.listFiles() : targetDir.listFiles(filter);
		for (File next : resourceFiles) {
			if (htmlFilter.accept(next))
				continue;
			String relPath = XML.getPathRelativeTo(next.getAbsolutePath(), base.getAbsolutePath());
			String fileName = next.getName();
			String baseName = XML.stripExtension(fileName);
			LibraryResource record = new LibraryResource(fileName);
			collection.addResource(record);
			record.setTarget(relPath);
			// assign resource type to zip files
			if (fileName.toLowerCase().endsWith(".zip")) { //$NON-NLS-1$
				if (filter == TRACKER_FILTER) {
					record.setType(LibraryResource.TRACKER_TYPE);
				}
			}
			if (fileName.toLowerCase().endsWith(".trz")) { //$NON-NLS-1$
				record.setType(LibraryResource.TRACKER_TYPE);
			}
			// look for HTML with base name + "_info"
			for (File htmlFile : htmlFiles) {
				String htmlName = XML.stripExtension(htmlFile.getName());
				if (htmlName.equals(baseName + "_info")) { //$NON-NLS-1$
					if ("".equals(record.getHTMLPath())) { //$NON-NLS-1$
						relPath = XML.getPathRelativeTo(htmlFile.getAbsolutePath(), base.getAbsolutePath());
						record.setHTMLPath(relPath);
						String htmlCode = ResourceLoader.getHTMLCode(htmlFile.getAbsolutePath());
						String title = ResourceLoader.getTitleFromHTMLCode(htmlCode);
						if (title != null) {
							record.setName(title);
						}
					}
					matchedNames.add(htmlFile);
					break;
				}
			}

		}
		// insert unmatched HTML files at top of the collection
		int i = 0;
		for (File html : htmlFiles) {
			if (matchedNames.contains(html) || !filter.accept(html))
				continue;
			String fileName = html.getName();
			LibraryResource record = new LibraryResource(fileName);
			String relPath = XML.getPathRelativeTo(html.getAbsolutePath(), base.getAbsolutePath());
			record.setHTMLPath(relPath);
			record.setType(LibraryResource.HTML_TYPE);
			collection.insertResource(record, i++);
		}
		return collection;
	}

	/**
	 * Adds a tab displaying a library resource with a given path. If a treePath is
	 * specified, the node it points to will be selected
	 * 
	 * @param path     the path to the resource
	 * @param treePath tree path to select in root-first order (may be null)
	 * @param listener 
	 * @return the TabLoader that adds the tab
	 */
	protected boolean addTabAndExecute(String path, List<String> treePath, PropertyChangeListener listener) {
		if (path == null)
			return false;
		File cachedFile = ResourceLoader.getSearchCacheFile(path);
		boolean isCachePath = cachedFile.exists();
		if (!isCachePath && !isWebConnected() && ResourceLoader.isHTTP(path)) {
			JOptionPane.showMessageDialog(this, ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Message"), //$NON-NLS-1$
					ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Title"), //$NON-NLS-1$
					JOptionPane.WARNING_MESSAGE);
			return false;
		}
		loadTabAsync(path, -1, treePath, listener);
		return true;
	}

	/**
	 * Refreshes the title of a tab based on the properties of a LibraryCollection
	 * and the path associated with that collection.
	 * 
	 * @param path       the collection path
	 * @param collection the LibraryCollection itself
	 */
	protected void refreshTabTitle(String path, LibraryResource collection) {
		int n = getTabIndexFromPath(path);
		if (n == -1)
			return;

		String title = collection.getTitle(path);

		// add a TabTitle with expand and contract icons to ComPADRE tab
		if (path.contains(LibraryComPADRE.TRACKER_SERVER_TREE) && tabbedPane.getTabComponentAt(n) == null) {
			boolean primary = path.contains("OSPPrimary"); //$NON-NLS-1$
			Icon icon = primary ? expandIcon : contractIcon;
			Icon heavyIcon = primary ? heavyExpandIcon : heavyContractIcon;
			TabTitle tabTitle = new TabTitle(icon, heavyIcon);
			FontSizer.setFont(tabTitle);
			tabTitle.iconLabel.setToolTipText(primary ? ToolsRes.getString("LibraryBrowser.Tooltip.Expand") : //$NON-NLS-1$
					ToolsRes.getString("LibraryBrowser.Tooltip.Contract")); //$NON-NLS-1$
			tabbedPane.setTabComponentAt(n, tabTitle);
		}
		boolean changed = getTreePanel(n).isChanged();
		tabbedPane.setTitleAt(n, changed ? title + "*" : title); //$NON-NLS-1$
		library.getNameMap().put(path, title);
		if (n == tabbedPane.getSelectedIndex()) {
			String tabname = " '" + title + "'"; //$NON-NLS-1$ //$NON-NLS-2$
			closeItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.CloseTab") + tabname); //$NON-NLS-1$
		}
	}

	protected void loadTabAsync(String path, int index, List<String> treePath, PropertyChangeListener listener) {
		TabLoader loader = new TabLoader(path, index, treePath);
		if (listener != null)
			loader.addPropertyChangeListener(listener);
		loader.execute();
	}

	/**
	 * Creates the visible components of this panel.
	 */
	protected void createGUI() {
		double factor = 1 + FontSizer.getLevel() * 0.25;
		int w = (int) (factor * 1000);
		int h = (int) (factor * 600);
		setPreferredSize(new Dimension(w, h));

		loadCollectionAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadTab(e.getActionCommand(), null);
			}
		};

		// create command action, label, field and button
		commandAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCommand();
			}
		};
		
		// create download action for button listener
		downloadAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String urlPath = commandField.getText().trim();
				if (urlPath == null)
					return;
				//String urlPath="https://physlets.org/tracker/library/videos/BallTossOut.mp4";  // for testing
				System.err.println("Performing download action with urlPath="+urlPath);
				String name = XML.getName(urlPath);
				// choose file and save resource
				VideoIO.getChooserFilesAsync("save resource "+name, //$NON-NLS-1$
						(files) -> {
							if (VideoIO.getChooser().getSelectedOption() != AsyncFileChooser.APPROVE_OPTION
									|| files == null) {
								return null;
							}
							String filePath = files[0].getAbsolutePath();
							try {
								File file = ResourceLoader.copyURLtoFile(urlPath, filePath);
								LibraryBrowser.this.firePropertyChange(PROPERTY_LIBRARY_TARGET, HINT_DOWNLOAD_RESOURCE, file); // $NON-NLS-1$
							} catch (IOException e1) {
								System.err.println("Failed to download urlPath="+urlPath);
								e1.printStackTrace();
							}
							return null;
						});

//				String filePath=getChooserSavePath(urlPath);
//				System.err.println("filePath="+filePath);
//				try {
//					File file = ResourceLoader.copyURLtoFile(urlPath, filePath);
//					LibraryBrowser.this.firePropertyChange(PROPERTY_LIBRARY_TARGET, HINT_DOWNLOAD_RESOURCE, file); // $NON-NLS-1$
//				} catch (IOException e1) {
//					System.err.println("Failed to load urlPath="+urlPath);
//					e1.printStackTrace();
//				}
			}
		};
		
		commandLabel = new JLabel();
		commandLabel.setAlignmentX(CENTER_ALIGNMENT);
		commandLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 2));
		commandField = new JTextField() {
			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				dim.width = Math.max(dim.width, 400);
				return dim;
			}
		};
		LibraryTreePanel.defaultForeground = commandField.getForeground();
		commandField.addActionListener(commandAction);
		commandField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				String text = commandField.getText();
				boolean enable = !"".equals(text);
				commandButton.setEnabled(enable); //$NON-NLS-1$
				downloadButton.setEnabled(enable); //$NON-NLS-1$
				textChanged = keyPressed;
				LibraryTreePanel treePanel = getSelectedTreePanel();
				if (treePanel != null) {
					treePanel.command = text;
					LibraryTreeNode node = treePanel.getSelectedNode();
					if (node != null && node.isRoot() && node.record instanceof LibraryCollection
							&& treePanel.pathToRoot.equals(text)) {
						commandButton.setEnabled(false);
					  downloadButton.setEnabled(false);
					}
				} else {
					commandField.setBackground(Color.yellow);
					commandField.setForeground(LibraryTreePanel.defaultForeground);
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				boolean enable = !"".equals(commandField.getText());
				commandButton.setEnabled(enable); //$NON-NLS-1$
				downloadButton.setEnabled(enable); //$NON-NLS-1$
				textChanged = keyPressed;
				LibraryTreePanel treePanel = getSelectedTreePanel();
				if (treePanel != null) {
					treePanel.command = commandField.getText();
				} else {
					commandField.setBackground(Color.yellow);
					commandField.setForeground(LibraryTreePanel.defaultForeground);
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});
		commandField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				keyPressed = true;
			}

			@Override
			public void keyReleased(KeyEvent e) {
				LibraryTreePanel treePanel = getSelectedTreePanel();
				if (treePanel != null && textChanged && e.getKeyCode() != KeyEvent.VK_ENTER) {
					commandField.setBackground(Color.yellow);
					commandField.setForeground(LibraryTreePanel.defaultForeground);
					treePanel.setSelectedNode(null);
				}
				textChanged = keyPressed = false;
			}
		});
		commandField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				commandField.selectAll();
			}
		});

		commandButton = new JButton(commandAction);
		commandButton.setOpaque(false);
		commandButton.setBorder(buttonBorder);
		
		downloadButton = new JButton(downloadAction);
		downloadButton.setIcon(downloadIcon);
		downloadButton.setDisabledIcon(downloadDisabledIcon);
		downloadButton.setOpaque(true);
		downloadButton.setBorder(buttonBorder);
//		downloadButton.setEnabled(true);

		// create search action, label, field and button
		searchAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSearch();
			}
		};
		searchLabel = new JLabel();
		searchLabel.setAlignmentX(CENTER_ALIGNMENT);
		searchLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 2));
		searchField = new LibraryTreePanel.EntryField() {
			@Override
			public Dimension getMaximumSize() {
				Dimension dim = super.getMaximumSize();
				dim.width = (int) (120 * (1 + FontSizer.getLevel() * 0.25));
				return dim;
			}

			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				dim.width = (int) (120 * (1 + FontSizer.getLevel() * 0.25));
				return dim;
			}

		};
		searchField.addActionListener(searchAction);

		refreshButton = new JButton(refreshIcon);
		refreshButton.setBorder(buttonBorder);
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LibraryTreePanel treePanel = getSelectedTreePanel();
				if (treePanel == null)
					return;
				LibraryTreeNode node = treePanel.getSelectedNode();
				// if node is root, delete the cache file, if any, and reload the entire
				// collection
				if (node == treePanel.rootNode) {
					File cachedFile = ResourceLoader.getSearchCacheFile(treePanel.pathToRoot);
					if (cachedFile.exists()) {
						cachedFile.delete();
					}
					// reload the root resource or directory
					LibraryResource resource = loadResource(treePanel.pathToRoot);
					if (resource != null) {
						treePanel.setRootResource(resource, treePanel.pathToRoot, treePanel.rootNode.isEditable(),
								false);
						refreshTabTitle(treePanel.pathToRoot, treePanel.rootResource);
						// start background SwingWorker to load metadata and set up search database
						if (treePanel.metadataLoader != null) {
							treePanel.metadataLoader.cancel();
						}
						treePanel.metadataLoader = treePanel.new MetadataLoader(null);
						treePanel.metadataLoader.execute();
						return;
					}
				} else if (node != null) {
					// for other nodes delete cached files and reload the node
					LibraryTreePanel.HTMLPane pane = new LibraryTreePanel.HTMLPane();
					pane.setText(
							"<h2>" + ToolsRes.getString("LibraryBrowser.Info.Refreshing") + " '" + node + "'</h2>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					treePanel.htmlScroller.setViewportView(pane);
					URL url = node.getHTMLURL(); // returns cached file URL, if any
					if (url != null) {
						File cachedFile = ResourceLoader.getOSPCacheFile(url.toExternalForm());
						if (cachedFile.exists()) {
							cachedFile.delete();
						}
						LibraryTreePanel.htmlPanesByURL.remove(url);
					}

					// delete thumbnail image, if any
					String target = node.getAbsoluteTarget();
					if (OSPRuntime.doCacheThumbnail && target != null) {
						File thumb = node.getThumbnailFile();
						if (thumb.exists()) {
							thumb.delete();
							node.record.setThumbnail(null);
						}
					}
					// clear metadata and description
					node.record.setMetadata(null);
					node.record.setDescription(null);
					node.tooltip = null;
					node.metadataSource = null;

					treePanel.new NodeLoader(node).execute();
				}

			}
		});

		tabbedPane = new JTabbedPane(SwingConstants.TOP) {
			@Override
			public void setTitleAt(int i, String title) {
				super.setTitleAt(i, title);
				TabTitle tab = (TabTitle) tabbedPane.getTabComponentAt(i);
				if (tab != null) {
					tab.setTitle(title);
				}
			}
		};
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				refreshGUI();
				LibraryTreePanel treePanel = getSelectedTreePanel();
				if (treePanel != null) {
					LibraryTreeNode node = treePanel.getSelectedNode();
					if (node != null) {
						String path = node.isRoot() ? treePanel.pathToRoot : node.getAbsoluteTarget();
						commandField.setText(path);
						setMessage(node.getToolTip(), null);
						treePanel.showInfo(node, "tabbedPaneChange");
					} else {
						commandField.setText(treePanel.command);
						commandField.setCaretPosition(0);
					}
				}
				commandField.setBackground(Color.white);
				commandField.setForeground(LibraryTreePanel.defaultForeground);
				if (libraryManager != null && libraryManager.isVisible())
					libraryManager.refreshGUI();
			}
		});
		tabbedPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!OSPRuntime.isPopupTrigger(e))
					return;
					// make popup and add items
					JPopupMenu popup = new JPopupMenu();
					// close this tab
					JMenuItem item = new JMenuItem(ToolsRes.getString("MenuItem.Close")); //$NON-NLS-1$
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							int i = tabbedPane.getSelectedIndex();
							closeTab(i);
						}
					});
					popup.add(item);
					// add tab to Collections menu
					final LibraryTreePanel treePanel = getSelectedTreePanel();
					if (!"".equals(treePanel.pathToRoot) && !library.containsPath(treePanel.pathToRoot, false)) { //$NON-NLS-1$
						item = new JMenuItem(ToolsRes.getString("LibraryBrowser.MenuItem.AddToLibrary")); //$NON-NLS-1$
						item.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								addToCollections(treePanel.pathToRoot);
							}
						});
						popup.addSeparator();
						popup.add(item);
					}
					FontSizer.setFonts(popup, FontSizer.getLevel());
					popup.show(tabbedPane, e.getX(), e.getY() + 8);
				}
		});

		// create property change listener for treePanels
		treePanelListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				System.out.println("LibraryBrower.propertyChange " + e);
				switch (e.getPropertyName()) {
				default:
					return;
				case PROPERTY_LIBRARY_EDITED:
					refreshGUI();
					return;
				case PROPERTY_LIBRARY_TARGET:
					break;
				}

				Object hint = e.getOldValue();
				Object newValue = e.getNewValue();

				LibraryTreeNode node = null;

				// Check for a LibraryResource that is not a LibraryColletion

				LibraryResource record = null;
				if (newValue instanceof LibraryTreeNode) {
					node = (LibraryTreeNode) newValue;
					if (node.record instanceof LibraryCollection) {
						processTargetCollection(node);
						return;
					}
					record = node.record.getClone();
					record.setBasePath(node.getBasePath());
				} else if (newValue instanceof LibraryResource) {
					record = (LibraryResource) newValue;
				}
				processTargetSelection(record, hint);
			}

		};

		// create edit button
		editButton = new JButton();
		editButton.setOpaque(false);
		editButton.setBorder(buttonBorder);
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final LibraryTreePanel treePanel = getSelectedTreePanel();
				if (!treePanel.isEditing()) {
					treePanel.setEditing(true);
					refreshGUI();
				} else if (!treePanel.isChanged()) {
					treePanel.setEditing(false);
					refreshGUI();
				} else {
					JPopupMenu popup = new JPopupMenu();
					JMenuItem item = new JMenuItem(ToolsRes.getString("LibraryBrowser.MenuItem.SaveEdits")); //$NON-NLS-1$
					popup.add(item);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							String path = save();
							if (path == null)
								return;
							treePanel.setEditing(false);
							refreshGUI();
						}
					});
					item = new JMenuItem(ToolsRes.getString("LibraryBrowser.MenuItem.Discard")); //$NON-NLS-1$
					popup.add(item);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							treePanel.setEditing(false);
							treePanel.revert();
							refreshGUI();
						}
					});
					FontSizer.setFonts(popup, FontSizer.getLevel());
					popup.show(editButton, 0, editButton.getHeight());
				}
			}
		});

		// assemble toolbar
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		Border empty = BorderFactory.createEmptyBorder(1, 2, 1, 2);
		Border etched = BorderFactory.createEtchedBorder();
		toolbar.setBorder(BorderFactory.createCompoundBorder(etched, empty));
		toolbar.add(commandLabel);
		toolbar.add(commandField);
		toolbar.add(commandButton);
		toolbar.add(downloadButton);
		toolbar.addSeparator();
		toolbar.add(searchLabel);
		toolbar.add(searchField);
		toolbar.addSeparator();
		toolbar.add(editButton);
		toolbar.addSeparator();
		toolbar.add(refreshButton);

		add(toolbar, BorderLayout.NORTH);

		// menu items
		fileMenu = new JMenu();
		menubar.add(fileMenu);
		newItem = new JMenuItem();
		int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		newItem.setAccelerator(KeyStroke.getKeyStroke('N', mask));
		newItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = createNewCollection();
				library.addRecent(path, false);
				refreshRecentMenu();
			}
		});
		openItem = new JMenuItem();
		openItem.setAccelerator(KeyStroke.getKeyStroke('O', mask));
		openItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		closeItem = new JMenuItem();
		closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = tabbedPane.getSelectedIndex();
				if (closeTab(i)) {
					refreshGUI();
				}
			}
		});
		closeAllItem = new JMenuItem();
		closeAllItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
					if (!closeTab(i))
						break;
				}
				refreshGUI();
			}
		});
		recentMenu = new JMenu();
		fileMenu.add(recentMenu);
		saveItem = new JMenuItem();
		saveItem.setAccelerator(KeyStroke.getKeyStroke('S', mask));
		saveItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		saveAsItem = new JMenuItem();
		saveAsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = saveAs();
				library.addRecent(path, false);
				refreshRecentMenu();
			}
		});

		exitItem = new JMenuItem();
		exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', mask));
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});

		collectionsMenu = new JMenu();
		menubar.add(collectionsMenu);

		manageMenu = new JMenu();
		if (!OSPRuntime.isJS)
			menubar.add(manageMenu);
		collectionsItem = new JMenuItem();
		manageMenu.add(collectionsItem);
		collectionsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LibraryManager manager = browser.getManager();
				manager.tabbedPane.setSelectedComponent(manager.collectionsPanel);
				manager.setVisible(true);
			}
		});
		searchItem = new JMenuItem();
		manageMenu.add(searchItem);
		searchItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LibraryManager manager = browser.getManager();
				manager.tabbedPane.setSelectedComponent(manager.searchPanel);
				manager.setVisible(true);
			}
		});
		cacheItem = new JMenuItem();
		manageMenu.add(cacheItem);
		cacheItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LibraryManager manager = browser.getManager();
				manager.tabbedPane.setSelectedComponent(manager.cachePanel);
				manager.setVisible(true);
			}
		});

		helpMenu = new JMenu();
		menubar.add(helpMenu);
		helpItem = new JMenuItem();
		helpItem.setAccelerator(KeyStroke.getKeyStroke('H', mask));
		helpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showHelp();
			}
		});
		helpMenu.add(helpItem);
		helpMenu.addSeparator();
		logItem = new JMenuItem();
		logItem.setAccelerator(KeyStroke.getKeyStroke('L', mask));
		logItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Point p0 = new Frame().getLocation();
				JFrame frame = OSPLog.getOSPLog();
				if ((frame.getLocation().x == p0.x) && (frame.getLocation().y == p0.y)) {
					frame.setLocationRelativeTo(LibraryBrowser.this);
				}
				frame.setVisible(true);
			}
		});
		helpMenu.add(logItem);
		helpMenu.addSeparator();
		aboutItem = new JMenuItem();
		aboutItem.setAccelerator(KeyStroke.getKeyStroke('A', mask));
		aboutItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showAboutDialog();
			}
		});
		helpMenu.add(aboutItem);

		// create html about-browser pane
		htmlAboutPane = new LibraryTreePanel.HTMLPane();
		htmlScroller = new JScrollPane(htmlAboutPane);
		htmlAboutPane.setText(getAboutLibraryBrowserText());
		htmlAboutPane.setCaretPosition(0);

		if (externalDialog != null) {
			externalDialog.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent e) {
					new LibraryLoader().execute();
				}
			});
		} else {
			frame.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent e) {
					new LibraryLoader().execute();
				}
			});
		}
		
		messageButton = new JButton();
		messageButton.addActionListener((e) -> {
			if (messageButton.getBackground() == Color.YELLOW) {
				setCanceled(true);
			}
		});
		messageButton.setHorizontalAlignment(SwingConstants.LEFT);
		messageButton.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
		add(messageButton, BorderLayout.SOUTH);
		
		setMessage(null, null);
	}
	
	/**
	 * Sets a message in the message label.
	 * 
	 * @param message the message. Will be truncated if too long.
	 * @param color the background color
	 */
	public void setMessage(String message, Color color) {
		boolean isEmpty = (message == null || "".equals(message.trim()));
		messageButton.setText(isEmpty? " ": message);
		messageButton.setBackground(color != null? color: Color.WHITE);
		messageButton.setFont(color != null? commandButton.getFont(): commandField.getFont());
	}

	protected void processTargetCollection(LibraryTreeNode node) {

		Runnable onSuccess = new Runnable() {

			@Override
			public void run() {
				node.createChildNodes();
				LibraryTreePanel.htmlPanesByNode.remove(node);
				getSelectedTreePanel().scrollToPath(((LibraryTreeNode) node.getLastChild()).getTreePath(), false);
				getSelectedTreePanel().showInfo(node, "LibraryBrowser.processTargetCollection");
				setCursor(Cursor.getDefaultCursor());
			}

		};

		Runnable onFailure = new Runnable() {

			@Override
			public void run() {
				setCursor(Cursor.getDefaultCursor());
				JOptionPane.showMessageDialog(LibraryBrowser.this,
						ToolsRes.getString("LibraryBrowser.Dialog.NoResources.Message"), //$NON-NLS-1$
						ToolsRes.getString("LibraryBrowser.Dialog.NoResources.Title"), //$NON-NLS-1$
						JOptionPane.PLAIN_MESSAGE);
			}

		};
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		LibraryComPADRE.loadResources(node, onSuccess, onFailure);
	}

	protected void processTargetSelection(LibraryResource record, Object hint) {
		if (record == null)
			return;
		String target = record.getTarget();
		if (target != null && (target.toLowerCase().endsWith(".pdf") //$NON-NLS-1$
				|| target.toLowerCase().endsWith(".html") //$NON-NLS-1$
				|| target.toLowerCase().endsWith(".htm"))) { //$NON-NLS-1$
			target = XML.getResolvedPath(target, record.getBasePath());
			target = ResourceLoader.getURIPath(target);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			OSPDesktop.displayURL(target);
			setCursor(Cursor.getDefaultCursor());
			return;
		}
		// fire the event to TFrame and other listeners
		firePropertyChange(PROPERTY_LIBRARY_TARGET, hint, record); // $NON-NLS-1$
	}

	protected void doSearch() {
		searchTerm = searchField.getText();
		if (searchTerm.trim().length() == 0)
			return;
		searchField.selectAll();
		searchField.setBackground(Color.white);
		new Searcher().execute();
	}

	// do actual search in separate swingworker thread
	class Searcher extends SwingWorker<LibraryTreePanel, Object> {
		@Override
		public LibraryTreePanel doInBackground() {
			// search all cache targets except those in the library no_search set
			Set<LibraryResource> searchTargets = getSearchCacheTargets();
			for (Iterator<LibraryResource> it = searchTargets.iterator(); it.hasNext();) {
				LibraryResource next = it.next();
				if (library.noSearchSet.contains(next.collectionPath))
					it.remove();
			}
			return searchFor(searchTerm.trim(), searchTargets);
		}

		@Override
		protected void done() {
			try {
				LibraryTreePanel results = get();
				if (results == null) {
					Toolkit.getDefaultToolkit().beep();
					// give visual cue, too
					final Color color = searchField.getForeground();
					searchField.setText(ToolsRes.getString("LibraryBrowser.Search.NotFound")); //$NON-NLS-1$
					searchField.setForeground(Color.RED);
					searchField.setBackground(Color.white);
					if (searchTimer == null) {
						searchTimer = new Timer(1000, new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								searchField.setText(searchTerm);
								searchField.setForeground(color);
								searchField.selectAll();
								searchField.setBackground(Color.white);
							}
						});
						searchTimer.setRepeats(false);
						searchTimer.start();
					} else {
						searchTimer.restart();
					}

					return;
				}
				String title = "'" + searchTerm.trim() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
				int i = getTabIndexFromTitle(title);
				synchronized (tabbedPane) {
					if (i > -1) {
						// replace existing tab
						tabbedPane.setComponentAt(i, results);
					} else {
						tabbedPane.addTab(title, results);
					}
					tabbedPane.setSelectedComponent(results);
				}
				LibraryTreePanel.htmlPanesByNode.remove(results.rootNode);
				results.showInfo(results.rootNode, "LibraryBrowser.Searcher.done");

				refreshGUI();
			} catch (Exception e) {
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

	protected void doCommand() {
		if (!commandButton.isEnabled())
			return;
		commandField.setBackground(Color.white);
		commandField.setForeground(LibraryTreePanel.defaultForeground);

		// get or create LibraryResource to send to TFrame and other listeners
		LibraryResource record = null;
//		LibraryTreePanel treePanel = getSelectedTreePanel();
		// if tree node is selected, use its record
//		if (treePanel != null && treePanel.getSelectedNode() != null) {
//			record = treePanel.getSelectedNode().record.getClone();
//			record.setBasePath(treePanel.getSelectedNode().getBasePath());
//			// send LibraryResource via property change event to TFrame and other listeners
//			firePropertyChange(PROPERTY_LIBRARY_TARGET, HINT_LOAD_RESOURCE, record); // $NON-NLS-1$
//			return;
//		}

		// see if the command field describes a resource that can be found
		String path = commandField.getText().trim();
		LibraryTreeNode node = getSelectedTreePanel().getSelectedNode();
		if (node != null && path.equals(node.record.getAbsoluteTarget())) {
			processTargetSelection(node.record, HINT_LOAD_RESOURCE);
			return;
		}
		if (path.equals("")) //$NON-NLS-1$
			return;
		path = XML.forwardSlash(path);
		Resource res = null;
		// BH 2020.11.12 presumes file not https here
		String xmlPath = ResourceLoader.getNonURIPath(path);
		if (ResourceLoader.isHTTP(path)) {
			path = path.replace("/OSP/", "/osp/");
			if (OSPRuntime.isJS) {
				path = path.replace("http:", "https:");
			}
		}
		// if path has no extension, look for xml file with same name
		if (!path.startsWith("https://www.compadre.org/osp/") //$NON-NLS-1$
				&& XML.getExtension(path) == null) {
			while (xmlPath.endsWith("/")) //$NON-NLS-1$
				xmlPath = xmlPath.substring(0, xmlPath.length() - 1);
			if (!xmlPath.equals("")) { //$NON-NLS-1$
				String name = XML.getName(xmlPath);
				xmlPath += "/" + name + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
				res = ResourceLoader.getResource(xmlPath);
			}
		}

		if (res != null)
			path = xmlPath;
		else
			res = ResourceLoader.getResourceZipURLsOK(path);

		if (res == null) {
			commandField.setForeground(LibraryTreePanel.darkRed);
			return;
		}

//		boolean isCollection = (res.getFile() != null && res.getFile().isDirectory()
//				|| res.getURL() != null && ResourceLoader.isJarZipTrz(res.getURL().toString(), false));
		boolean isCollection = res.getFile() != null && res.getFile().isDirectory();
		if (!isCollection) {
			XMLControl control = new XMLControlElement(path);
			isCollection = !control.failedToRead() && control.getObjectClass() == LibraryCollection.class;
		}

		// BH 2020.11.15 OK?
		// DB 2020.11.16 we need this to load collections from paths entered into the command field
		if (isCollection) {
			loadTab(path, null);
			refreshGUI();
			LibraryTreePanel treePanel = getSelectedTreePanel();
			if (treePanel != null && treePanel.pathToRoot.equals(path)) {
				treePanel.setSelectedNode(treePanel.rootNode);
				commandField.setBackground(Color.white);
				commandField.repaint();
			}
			return;
		}

		record = new LibraryResource(""); //$NON-NLS-1$
		record.setTarget(path);
		isCancelled = false;
		// send LibraryResource via property change event to TFrame and other listeners
		// DB 2020.11.15 don't hide LibraryBrowser since it has cancel button
//		setVisible(false);
		firePropertyChange(PROPERTY_LIBRARY_TARGET, HINT_LOAD_RESOURCE, record); // $NON-NLS-1$
	}

	/**
	 * Enables/disables the command button.
	 * @param enabled true if enabled
	 */
	public void setComandButtonEnabled(boolean enabled) {
		String text = commandField.getText();
		commandButton.setEnabled(enabled && !"".equals(text)); //$NON-NLS-1$
	}

	/**
	 * Refreshes the GUI, including locale-dependent resources strings.
	 */
	protected void refreshGUI() {
		refreshGUI(false);
	}

	/**
	 * Refreshes the GUI, including locale-dependent resources strings.
	 * @param andRebuild true to refresh strings and rebuild file menu
	 */
	protected void refreshGUI(boolean andRebuild) {
		if (tabbedPane.getTabCount() == 0) {
			// BH unnecessary refresh causes flashing in JavaScript
			if (htmlScroller.getParent() != this) {
				remove(tabbedPane);
				add(htmlScroller, BorderLayout.CENTER);
				validate();
			}
		} else {
			// BH unnecessary refresh causes flashing in JavaScript
			if (tabbedPane.getParent() != this) {
				remove(htmlScroller);
				add(tabbedPane, BorderLayout.CENTER);
			}
		}
		if (andRebuild) {
			// set text strings
			setTitle(ToolsRes.getString("LibraryBrowser.Title")); //$NON-NLS-1$
			fileMenu.setText(ToolsRes.getString("Menu.File")); //$NON-NLS-1$
			newItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.New")); //$NON-NLS-1$
			openItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.Open")); //$NON-NLS-1$
			closeAllItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.CloseAll")); //$NON-NLS-1$
			saveItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.Save")); //$NON-NLS-1$
			saveAsItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.SaveAs")); //$NON-NLS-1$
			exitItem.setText(ToolsRes.getString("MenuItem.Exit")); //$NON-NLS-1$
			collectionsMenu.setText(ToolsRes.getString("LibraryBrowser.Menu.Collections")); //$NON-NLS-1$
			manageMenu.setText(ToolsRes.getString("LibraryBrowser.Menu.Manage")); //$NON-NLS-1$
			collectionsItem.setText(ToolsRes.getString("LibraryManager.Tab.MyLibrary") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
			searchItem.setText(ToolsRes.getString("LibraryManager.Tab.Search") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
			cacheItem.setText(ToolsRes.getString("LibraryManager.Tab.Cache") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
			helpMenu.setText(ToolsRes.getString("Menu.Help")); //$NON-NLS-1$
			helpItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.Help")); //$NON-NLS-1$
			logItem.setText(ToolsRes.getString("MenuItem.Log")); //$NON-NLS-1$
			aboutItem.setText(ToolsRes.getString("MenuItem.About")); //$NON-NLS-1$
			commandLabel.setText(ToolsRes.getString("LibraryTreePanel.Label.Target")); //$NON-NLS-1$
			commandButton.setText(ToolsRes.getString("LibraryTreePanel.Button.Load")); //$NON-NLS-1$
			commandField.setToolTipText(ToolsRes.getString("LibraryBrowser.Field.Command.Tooltip")); //$NON-NLS-1$
			searchLabel.setText(ToolsRes.getString("LibraryBrowser.Label.Search")); //$NON-NLS-1$
			searchField.setToolTipText(ToolsRes.getString("LibraryBrowser.Field.Search.Tooltip")); //$NON-NLS-1$
			saveAsItem.setEnabled(true);
			refreshRecentMenu();
			// rebuild file menu
			fileMenu.removeAll();
			fileMenu.add(newItem);
			fileMenu.add(openItem);
			fileMenu.add(recentMenu);
			fileMenu.addSeparator();
			fileMenu.add(closeItem);
			fileMenu.add(closeAllItem);
			fileMenu.addSeparator();
			fileMenu.add(saveItem);
			fileMenu.add(saveAsItem);
			fileMenu.addSeparator();
			fileMenu.add(exitItem);
		}
		LibraryTreePanel treePanel = getSelectedTreePanel();
		if (treePanel != null) {
			editButton.setText(!treePanel.isEditing() ? ToolsRes.getString("LibraryBrowser.Button.OpenEditor") : //$NON-NLS-1$
					ToolsRes.getString("LibraryBrowser.Button.CloseEditor")); //$NON-NLS-1$
			editButton.setEnabled(treePanel.isEditable());
			String tabname = " '" + getTabTitle(treePanel.pathToRoot) + "'"; //$NON-NLS-1$ //$NON-NLS-2$
			closeItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.CloseTab") + tabname); //$NON-NLS-1$
			closeItem.setEnabled(true);
			closeAllItem.setEnabled(true);
			saveItem.setEnabled(treePanel.isChanged());
			int i = tabbedPane.getSelectedIndex();
			String title = tabbedPane.getTitleAt(i);
			if (treePanel.isChanged() && !title.endsWith("*")) { //$NON-NLS-1$
				tabbedPane.setTitleAt(i, title + "*"); //$NON-NLS-1$
			} else if (!treePanel.isChanged() && title.endsWith("*")) { //$NON-NLS-1$
				tabbedPane.setTitleAt(i, title.substring(0, title.length() - 1));
			}
			treePanel.refreshGUI(andRebuild);
		} else {
			refreshButton.setToolTipText(ToolsRes.getString("LibraryBrowser.Tooltip.Refresh")); //$NON-NLS-1$
			downloadButton.setToolTipText("Download selected item to desktop"); 
			editButton.setText(ToolsRes.getString("LibraryBrowser.Button.OpenEditor")); //$NON-NLS-1$
			saveItem.setEnabled(false);
			closeItem.setText(ToolsRes.getString("LibraryBrowser.MenuItem.CloseTab")); //$NON-NLS-1$
			closeItem.setEnabled(false);
			closeAllItem.setEnabled(false);
			editButton.setEnabled(false);
			refreshButton.setEnabled(false);
			commandField.setText(null);
			commandButton.setEnabled(false);
			downloadButton.setEnabled(false);
			saveAsItem.setEnabled(false);
		}
		repaint();
	}

	/**
	 * Refreshes the open recent files menu.
	 *
	 * @param menu the menu to refresh
	 */
	public void refreshRecentMenu() {
		synchronized (library.recentTabs) {
			recentMenu.setText(ToolsRes.getString("LibraryBrowser.Menu.OpenRecent")); //$NON-NLS-1$
			recentMenu.setEnabled(!library.recentTabs.isEmpty());
			if (openRecentAction == null) {
				openRecentAction = new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						String path = e.getActionCommand();
						library.addRecent(path, false);
						// select tab if path is already loaded
						int i = getTabIndexFromPath(path);
						if (i > -1) {
							tabbedPane.setSelectedIndex(i);
							return;
						}
						if (!loadTabAndListen(path, null, "OpenRecent")) {
							library.recentTabs.remove(path);
							refreshRecentMenu();
							JOptionPane.showMessageDialog(LibraryBrowser.this,
									ToolsRes.getString("LibraryBrowser.Dialog.FileNotFound.Message") //$NON-NLS-1$
											+ ": " + path, //$NON-NLS-1$
									ToolsRes.getString("LibraryBrowser.Dialog.FileNotFound.Title"), //$NON-NLS-1$
									JOptionPane.WARNING_MESSAGE);
						}
					}
				};
			}
			recentMenu.removeAll();
			recentMenu.setEnabled(!library.recentTabs.isEmpty());
			for (String next : library.recentTabs) {
				String text = library.getNameMap().get(next);
				if (text == null)
					text = XML.getName(next);
				JMenuItem item = new JMenuItem(text);
				item.setActionCommand(next);
				item.setToolTipText(next);
				item.addActionListener(openRecentAction);
				recentMenu.add(item);
			}
		}
		FontSizer.setMenuFonts(recentMenu);
	}

	/**
	 * Opens a file using an AsyncFileChooser.
	 */
	protected void open() {
		AsyncFileChooser fileChooser = OSPRuntime.getChooser();
		if (fileChooser == null)
			return;
		for (javax.swing.filechooser.FileFilter filter : fileChooser.getChoosableFileFilters()) {
			fileChooser.removeChoosableFileFilter(filter);
		}
		fileChooser.addChoosableFileFilter(filesAndFoldersFilter);
		fileChooser.addChoosableFileFilter(Launcher.getXMLFilter());
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setFileFilter(filesAndFoldersFilter);
		String oldTitle = fileChooser.getDialogTitle();
		fileChooser.showOpenDialog(this, new Runnable() {

			@Override
			public void run() {
				File file = fileChooser.getSelectedFile();
				if (file != null)
					open(file.getAbsolutePath());
			}

		}, null);
		// reset chooser to original state
		fileChooser.setDialogTitle(oldTitle);
		fileChooser.removeChoosableFileFilter(filesAndFoldersFilter);
		fileChooser.removeChoosableFileFilter(Launcher.getXMLFilter());
		fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	}

	/**
	 * Opens a file with a specified path.
	 * 
	 * @param path the path to the file
	 */
	public void open(String path) {
		
		loadTab(path, null);
	}

	/**
	 * Closes a tab.
	 * 
	 * @param index the tab number
	 * @return true unless cancelled by user
	 */
	protected boolean closeTab(int index) {
		if (index < 0 || index >= tabbedPane.getTabCount())
			return true;
		LibraryTreePanel treePanel = getTreePanel(index);
		if (!treePanel.saveChanges(getTabTitle(index)))
			return false;
		if (treePanel.rootResource == getRecentCollection()) {
			treePanel.save();
		}
		tabbedPane.removeTabAt(index);
		if (treePanel.metadataLoader != null) {
			treePanel.metadataLoader.cancel();
		}
		return true;
	}

	/**
	 * Saves the selected LibraryTreePanel collection.
	 * 
	 * @return the path to the saved file, or null if not saved
	 */
	protected String save() {
		LibraryTreePanel treePanel = getSelectedTreePanel();
		String path = treePanel.save();
		refreshGUI();
		return path;
	}

	/**
	 * Saves the current root resource as a new xml file.
	 * 
	 * @return the path to the saved file, or null if not saved
	 */
	protected String saveAs() {
		String title = ToolsRes.getString("LibraryBrowser.FileChooser.Title.SaveAs"); //$NON-NLS-1$
		String path = getChooserSavePath(title);
		if (path != null) {
			path = XML.forwardSlash(path);
			LibraryTreePanel treePanel = getSelectedTreePanel();
			treePanel.setRootResource(treePanel.rootResource, path, true, true);
			path = save();
			treePanel.setEditing(true);
			refreshTabTitle(path, treePanel.rootResource);
			refreshGUI();
			commandField.setForeground(LibraryTreePanel.defaultForeground);
		}
		return path;
	}

	/**
	 * Uses a file chooser to define a path to which a library or resource file
	 * (xml) can be saved. This adds the extension ".xml", if none, and checks for
	 * duplicates.
	 * 
	 * @param chooserTitle the title of the file chooser
	 * @return the path, or null if canceled by the user
	 */
	protected String getChooserSavePath(String chooserTitle) {
		File file = GUIUtils.showSaveDialog(this, chooserTitle);
		if (file == null)
			return null;
		String path = file.getAbsolutePath();
		String extension = XML.getExtension(path);
		if (extension == null) {
			path = XML.stripExtension(path) + ".xml"; //$NON-NLS-1$
			file = new File(path);
			if (file.exists()) {
				int response = JOptionPane.showConfirmDialog(this, ToolsRes.getString("Tool.Dialog.ReplaceFile.Message") //$NON-NLS-1$
						+ " " + file.getName() + "?", //$NON-NLS-1$ //$NON-NLS-2$
						ToolsRes.getString("Tool.Dialog.ReplaceFile.Title"), //$NON-NLS-1$
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (response != JOptionPane.YES_OPTION) {
					return null;
				}
			}
		}
		return path;
	}

	/**
	 * Returns the set of all searchable cache resources.
	 * 
	 * @return a set of searchable resources
	 */
	protected Set<LibraryResource> getSearchCacheTargets() {
		// set up search targets
		Set<LibraryResource> searchTargets = new TreeSet<LibraryResource>();
		File cache = ResourceLoader.getSearchCache();
		FileFilter xmlFilter = new XMLFilter();
		List<File> xmlFiles = ResourceLoader.getFiles(cache, xmlFilter);
		for (File file : xmlFiles) {
			XMLControl control = new XMLControlElement(file);
			if (!control.failedToRead() && LibraryResource.class.isAssignableFrom(control.getObjectClass())) {
				LibraryResource resource = (LibraryResource) control.loadObject(null);
				resource.collectionPath = control.getString("real_path"); //$NON-NLS-1$
				searchTargets.add(resource);
			}
		}
		return searchTargets;
	}

	/**
	 * Searches a set of LibraryResources for resources matching a search phrase.
	 * 
	 * @param searchPhrase  the phrase to match
	 * @param searchTargets a set of LibraryResources to search
	 * @return a LibraryTreePanel containing the search results, or null if no nodes
	 *         found
	 */
	protected LibraryTreePanel searchFor(String searchPhrase, Set<LibraryResource> searchTargets) {
		if (searchPhrase == null || searchPhrase.trim().equals("")) //$NON-NLS-1$
			return null;

		Map<LibraryResource, List<String[]>> found = new TreeMap<LibraryResource, List<String[]>>();

		for (LibraryResource target : searchTargets) {
			if (target == null)
				continue;
			if (target instanceof LibraryCollection) {
				Map<LibraryResource, List<String[]>> map = searchCollectionFor(searchPhrase,
						(LibraryCollection) target);
				for (LibraryResource next : map.keySet()) {
					next.collectionPath = target.collectionPath;
					found.put(next, map.get(next));
				}
			} else {
				List<String[]> results = searchResourceFor(searchPhrase, target);
				if (results != null) {
					found.put(target, results);
				}
			}
		}

		if (found.isEmpty())
			return null;

		// create a LibraryCollection for the search results
		String title = "'" + searchPhrase + "'"; //$NON-NLS-1$ //$NON-NLS-2$
		LibraryTreePanel treePanel = createLibraryTreePanel();
		String name = ToolsRes.getString("LibraryBrowser.SearchResults") + ": " + title; //$NON-NLS-1$ //$NON-NLS-2$
		LibraryCollection results = new LibraryCollection(name);
		treePanel.setRootResource(results, "", false, false); //$NON-NLS-1$
		LibraryTreeNode root = treePanel.rootNode;

		for (LibraryResource next : found.keySet()) {
			LibraryResource clone = next.getClone();
			results.addResource(clone);
			LibraryTreeNode newNode = new LibraryTreeNode(clone, treePanel);
			newNode.setBasePath(next.getInheritedBasePath());
			treePanel.insertChildAt(newNode, root, root.getChildCount());
		}

		treePanel.scrollToPath(((LibraryTreeNode) root.getLastChild()).getTreePath(), false);
		treePanel.isChanged = false;
		return treePanel;
	}

	/**
	 * Searches a LibraryCollection for matches to a search phrase.
	 * 
	 * @param searchPhrase the phrase
	 * @param collection   the LibraryResource
	 * @return a List of String[] {where match was found, value in which match was
	 *         found}, or null if no match found
	 */
	protected Map<LibraryResource, List<String[]>> searchCollectionFor(String searchPhrase,
			LibraryCollection collection) {
		// deal with AND and OR requests
		String[] toAND = searchPhrase.split(AND);
		String[] toOR = searchPhrase.split(OR);
		if (toAND.length > 1 && toOR.length == 1) {
			Map<LibraryResource, List<String[]>> results = searchCollectionFor(toAND[0], collection);
			OSPLog.finer("AND '" + toAND[0] + "' (found: " + results.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			for (int i = 1; i < toAND.length; i++) {
				Map<LibraryResource, List<String[]>> next = searchCollectionFor(toAND[i], collection);
				OSPLog.finer("AND '" + toAND[i] + "' (found: " + results.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				results = applyAND(results, next);
			}
			OSPLog.finer("AND found: " + results.size()); //$NON-NLS-1$
			return results;
		}
		if (toOR.length > 1 && toAND.length == 1) {
			Map<LibraryResource, List<String[]>> results = searchCollectionFor(toOR[0], collection);
			OSPLog.finer("OR '" + toOR[0] + "' (found: " + results.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			for (int i = 1; i < toOR.length; i++) {
				Map<LibraryResource, List<String[]>> next = searchCollectionFor(toOR[i], collection);
				OSPLog.finer("OR '" + toOR[i] + "' (found: " + results.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				results = applyOR(results, next);
			}
			OSPLog.finer("OR found: " + results.size()); //$NON-NLS-1$
			return results;
		}
		if (toOR.length > 1 && toAND.length > 1) {
			// apply operations in left-to-right order but give precedence to parentheses
			String[] split = getNextSplit(searchPhrase);
			Map<LibraryResource, List<String[]>> results = searchCollectionFor(split[0], collection);
			while (split.length > 2) {
				String operator = split[1];
				String remainder = split[2];
				split = getNextSplit(remainder);
				Map<LibraryResource, List<String[]>> next = searchCollectionFor(split[0], collection);
				if (operator.equals(AND)) {
					results = applyAND(results, next);
				} else if (operator.equals(OR)) {
					results = applyOR(results, next);
				}
			}
			return results;
		}

		// do actual searching
		Map<LibraryResource, List<String[]>> found = new TreeMap<LibraryResource, List<String[]>>();
		List<String[]> results = searchResourceFor(searchPhrase, collection);
		if (results != null) {
			found.put(collection, results);
		}
		for (LibraryResource record : collection.getResources()) {
			if (record == null)
				continue;
			if (record instanceof LibraryCollection) {
				Map<LibraryResource, List<String[]>> map = searchCollectionFor(searchPhrase,
						(LibraryCollection) record);
				for (LibraryResource next : map.keySet()) {
					found.put(next, map.get(next));
				}
			} else {
				results = searchResourceFor(searchPhrase, record);
				if (results != null) {
					found.put(record, results);
				}
			}
		}
		return found;
	}

	/**
	 * Searches a LibraryResource for matches to a search phrase.
	 * 
	 * @param searchPhrase the phrase
	 * @param record       the LibraryResource
	 * @return a List of String[] {category where match found, value where match
	 *         found}, or null if no match found
	 */
	protected List<String[]> searchResourceFor(String searchPhrase, LibraryResource record) {
		String toMatch = searchPhrase.toLowerCase();
		ArrayList<String[]> foundData = new ArrayList<String[]>();
		// search node name
		String name = record.getName();
		if (name.toLowerCase().contains(toMatch)) {
			foundData.add(new String[] { "name", name }); //$NON-NLS-1$
		}
		// search node type
		String type = record.getType();
		if (type.toLowerCase().contains(toMatch)) {
			foundData.add(new String[] { "type", type }); //$NON-NLS-1$
		}
		// search metadata
		Set<Metadata> metadata = record.getMetadata();
		if (metadata != null) {
			for (Metadata next : metadata) {
				String key = next.getData()[0];
				String value = next.getData()[1];
				if (value.toLowerCase().indexOf(toMatch) > -1) {
					foundData.add(new String[] { key, value });
				}
			}
		}
		return foundData.isEmpty() ? null : foundData;
	}

	/**
	 * Returns the phrase before the next AND or OR operator, the operator itself,
	 * and the remainder of the phrase.
	 *
	 * @param phrase a search phrase
	 * @return String[]
	 */
	protected String[] getNextSplit(String phrase) {
		String[] and = phrase.split(AND, 2);
		String[] or = phrase.split(OR, 2);
		String[] open = phrase.split(Pattern.quote(OPENING), 2);
		int which = and[0].length() <= or[0].length() ? and[0].length() <= open[0].length() ? 0 : 2
				: or[0].length() <= open[0].length() ? 1 : 2;
		if (which == 2 && open.length > 1) { // found opening parentheses

			// split remainder into parenthesis contents and remainder
			String[] split = getParenthesisSplit(open[1]);
			if (split.length == 1) {
				return new String[] { split[0] };
			}
			int n = split[1].indexOf(AND);
			int m = split[1].indexOf(OR);
			if (n == -1 && m == -1) {
				return new String[] { open[1] };
			}
			if (n > -1 && (m == -1 || n < m)) { // AND
				return new String[] { split[0], AND, split[1].substring(n + AND.length()) };
			}
			if (m > -1 && (n == -1 || m < n)) { // OR
				return new String[] { split[0], OR, split[1].substring(m + OR.length()) };
			}
		}
		switch (which) {
		case 0:
			if (and.length == 1)
				return new String[] { and[0] };
			return new String[] { and[0], AND, and[1] };
		case 1:
			if (or.length == 1)
				return new String[] { or[0] };
			return new String[] { or[0], OR, or[1] };
		}
		return new String[] { phrase };
	}

	/**
	 * Returns the phrase enclosed in parentheses along with the remainder of a
	 * phrase.
	 *
	 * @param phrase a phrase that starts immediately AFTER an opening parenthesis
	 * @return String[] {the enclosed phrase, the remainder}
	 */
	protected String[] getParenthesisSplit(String phrase) {

		int index = 1; // index of closing parenthesis
		int n = 1; // number of unpaired opening parentheses
		int opening = phrase.indexOf(OPENING, index);
		int closing = phrase.indexOf(CLOSING, index);
		while (n > 0) {
			if (opening > -1 && opening < closing) {
				n++;
				index = opening + 1;
				opening = phrase.indexOf(OPENING, index);
			} else if (closing > -1) {
				n--;
				index = closing + 1;
				closing = phrase.indexOf(CLOSING, index);
			} else
				return new String[] { phrase };
		}
		String token = phrase.substring(0, index - 1);
		String remainder = phrase.substring(index);
		return remainder.trim().equals("") ? new String[] { token } : new String[] { token, remainder }; //$NON-NLS-1$
	}

	/**
	 * Returns the resources that are contained in the keysets of both of two input
	 * maps.
	 * 
	 * @param results1
	 * @param results2
	 * @return map of resources found in both keysets
	 */
	protected Map<LibraryResource, List<String[]>> applyAND(Map<LibraryResource, List<String[]>> results1,
			Map<LibraryResource, List<String[]>> results2) {
		Map<LibraryResource, List<String[]>> resultsAND = new TreeMap<LibraryResource, List<String[]>>();
		Set<LibraryResource> keys1 = results1.keySet();
		for (LibraryResource node : results2.keySet()) {
			if (keys1.contains(node)) { // node is in both keysets
				List<String[]> matchedTerms = new ArrayList<String[]>();
				matchedTerms.addAll(results1.get(node));
				matchedTerms.addAll(results2.get(node));
				resultsAND.put(node, matchedTerms);
			}
		}
		return resultsAND;
	}

	/**
	 * Returns the resources that are contained in the keysets of either of two
	 * input maps.
	 * 
	 * @param results1
	 * @param results2
	 * @return map of resources found in either keyset
	 */
	protected Map<LibraryResource, List<String[]>> applyOR(Map<LibraryResource, List<String[]>> results1,
			Map<LibraryResource, List<String[]>> results2) {
		Map<LibraryResource, List<String[]>> resultsOR = new TreeMap<LibraryResource, List<String[]>>();
		// add nodes in results1
		for (LibraryResource node : results1.keySet()) {
			List<String[]> matchedTerms = new ArrayList<String[]>();
			matchedTerms.addAll(results1.get(node));
			resultsOR.put(node, matchedTerms);
		}
		// add nodes in results2
		for (LibraryResource node : results2.keySet()) {
			if (resultsOR.keySet().contains(node)) {
				resultsOR.get(node).addAll(results2.get(node));
				continue;
			}
			List<String[]> matchedTerms = new ArrayList<String[]>();
			matchedTerms.addAll(results2.get(node));
			resultsOR.put(node, matchedTerms);
		}
		return resultsOR;
	}

	/**
	 * Adds a collection to this browser's library after prompting the user to
	 * assign it a name.
	 * 
	 * @param path the path to the collection
	 */
	protected void addToCollections(String path) {
		if (library.containsPath(path, true)) {
			return;
		}
		String proposed = getTabTitle(path);
		if (proposed == null) {
			LibraryResource collection = loadResource(path);
			if (collection != null)
				proposed = collection.getName();
		}
		if (proposed.equals("")) { //$NON-NLS-1$
			proposed = XML.getName(path); // filename
		}

		library.addCollection(path, proposed);
		refreshCollectionsMenu();
		refreshGUI();
	}

	/**
	 * Creates a new LibraryCollection file.
	 * 
	 * @return the path to the new collection
	 */
	protected String createNewCollection() {
		String title = ToolsRes.getString("LibraryBrowser.FileChooser.Title.SaveCollectionAs"); //$NON-NLS-1$
		String path = getChooserSavePath(title);
		if (path != null) {
			LibraryCollection collection = new LibraryCollection(null);
			// save new collection
			XMLControl control = new XMLControlElement(collection);
			control.write(path);
			path = XML.forwardSlash(path);
			loadTabAndListen(path, null, "CreateNew");
		}
		return path;
	}

	/**
	 * Returns a name that is not a duplicate of an existing name.
	 * 
	 * @param proposed     a proposed name
	 * @param nameToIgnore a name that is ignored when comparing
	 * @return a unique name that is the proposed name plus a possible suffix
	 */
	protected String getUniqueName(String proposed, String nameToIgnore) {
		proposed = proposed.trim();
		if (isDuplicateName(proposed, nameToIgnore)) {
			int i = 2;
			String s = proposed + " (" + i + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			while (isDuplicateName(s, nameToIgnore)) {
				i++;
				s = proposed + " (" + i + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return s;
		}
		return proposed;
	}

	/**
	 * Determines if a name duplicates an existing name.
	 * 
	 * @param name         the proposed name
	 * @param nameToIgnore a name that is ignored when comparing
	 * @return true if name is a duplicate
	 */
	protected boolean isDuplicateName(String name, String nameToIgnore) {
		// compare with existing names in library and tabbedPane
		for (String next : library.getNames()) {
			if (next.equals(nameToIgnore))
				continue;
			if (name.equals(next))
				return true;
		}
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			String title = tabbedPane.getTitleAt(i);
			if (title.endsWith("*")) //$NON-NLS-1$
				title = title.substring(0, title.length() - 1);
			if (title.equals(nameToIgnore))
				continue;
			if (name.equals(title))
				return true;
		}
		return false;
	}

	/**
	 * Creates a new empty LibraryTreePanel.
	 * 
	 * @return the library tree panel
	 */
	protected LibraryTreePanel createLibraryTreePanel() {
		LibraryTreePanel treePanel = new LibraryTreePanel(this);
		treePanel.addPropertyChangeListener(treePanelListener);
		return treePanel;
	}

	/**
	 * Shows the about dialog.
	 */
	protected void showAboutDialog() {
		String aboutString = ToolsRes.getString("LibraryBrowser.Title") + " 2.0,  Dec 2012\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "Open Source Physics Project\n" //$NON-NLS-1$
				+ "www.opensourcephysics.org"; //$NON-NLS-1$
		JOptionPane.showMessageDialog(this, aboutString, ToolsRes.getString("Dialog.About.Title") //$NON-NLS-1$
				+ " " + ToolsRes.getString("LibraryBrowser.Title"), //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Shows the help frame and displays a help HTML page.
	 */
	protected void showHelp() {
		if (fireHelpEvent) {
			firePropertyChange("help", null, null); //$NON-NLS-1$
			return;
		}
		String helpPath = XML.getResolvedPath(LIBRARY_HELP_NAME, LIBRARY_HELP_BASE);
		if (ResourceLoader.getResource(helpPath) == null) {
			String classBase = "/org/opensourcephysics/resources/tools/html/"; //$NON-NLS-1$
			helpPath = XML.getResolvedPath(LIBRARY_HELP_NAME, classBase);
		}
		if ((helpFrame == null) || !helpPath.equals(helpFrame.getTitle())) {
			helpFrame = new TextFrame(helpPath);
			helpFrame.enableHyperlinks();
			helpFrame.setSize(760, 560);
			// center on the screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - helpFrame.getBounds().width) / 2;
			int y = (dim.height - helpFrame.getBounds().height) / 2;
			helpFrame.setLocation(x, y);
		}
		helpFrame.setVisible(true);
	}

	/**
	 * Returns html code that describes this browser. This is displayed when no
	 * LibraryTreePanel is loaded.
	 * 
	 * @return the html code
	 */
	protected String getAboutLibraryBrowserText() {

		String path = "/org/opensourcephysics/resources/tools/images/compadre_banner.jpg"; //$NON-NLS-1$
		Resource res = ResourceLoader.getResource(path);
		String imageCode = "<p align=\"center\"><img src=\"" + res.getURL() + "\"></p>"; //$NON-NLS-1$ //$NON-NLS-2$
		String code = imageCode + "<h1>Open Source Physics Digital Library Browser</h1>" + //$NON-NLS-1$
				"<p>The OSP Digital Library Browser enables you to browse, organize and access collections of digital library resources " //$NON-NLS-1$
				+ "such as EJS models and Tracker experiments. Collections and resources may be on a local drive or remote server.</p>"
				+ "<ul>"
				+ "  <li>Open a collection by choosing from the <strong>Collections</strong> menu or entering a URL directly in the toolbar "
				+ "as with a web browser.</li>"
				+ "	 <li>Collections are organized and displayed in a tree. Each tree node is a resource or sub-collection. "
				+ "Click a node to learn about the resource or double-click to download and/or open it in EJS or Tracker.</li>"
				+ "	 <li>Build and organize your own local collection by clicking the <strong>Open Editor</strong> button. "
				+ "Collections are stored as xml documents that contain references to the actual resource files. "
				+ "For more information, see the Help menu.</li>"
				+ "	 <li>Share your collections by uploading all files to the web or a local network. For more information, see the Help menu.</li>"
				+ "</ul>" + "<h2>ComPADRE Digital Library</h2>"
				+ "<p>The ComPADRE Pathway, a part of the National Science Digital Library, is a growing network of educational resource "
				+ "collections supporting teachers and students in Physics and Astronomy. As a user you may explore collections designed to meet "
				+ "your specific needs and help build the network by recommending resources, commenting on resources, and starting or joining "
				+ "discussions. For more information, see &lt;<b><a href=\"https://www.compadre.org/osp/\">http://www.compadre.org/osp/</a></b>&gt;. "
				+ "To recommend an OSP resource for ComPADRE, visit the Suggest a Resource page at &lt;<b><a href="
				+ "\"https://www.compadre.org/osp/items/suggest.cfm\">http://www.compadre.org/osp/items/suggest.cfm</a></b>&gt;.&nbsp; "
				+ "Contact the OSP Collection editor, Wolfgang Christian, for additional information.</p>";
		return code;
	}

//______________________________ inner classes _________________________________

	/**
	 * A class to display and handle actions for a ComPADRE tab title.
	 */
	class TabTitle extends JPanel {
		private JLabel titleLabel, iconLabel;
		private Icon normalIcon, boldIcon;

		TabTitle(Icon lightIcon, Icon heavyIcon) {
			super(new BorderLayout());
			this.setOpaque(false);
			titleLabel = new JLabel();
			iconLabel = new JLabel();
			iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
			iconLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					int i = getTabIndexFromTitle(titleLabel.getText());
					if (i >= 0 && tabbedPane.getSelectedIndex() != i)
						tabbedPane.setSelectedIndex(i);
					boolean primaryOnly = (normalIcon == contractIcon);
					int index = getTabIndexFromTitle(titleLabel.getText());
					if (index >= 0) {
						LibraryTreePanel treePanel = getTreePanel(index);
						String path = LibraryComPADRE.getCollectionPath(treePanel.pathToRoot, primaryOnly);
						loadTabAsync(path, index, null, null);

						setIcons(primaryOnly ? expandIcon : contractIcon,
								primaryOnly ? heavyExpandIcon : heavyContractIcon);
						iconLabel.setToolTipText(primaryOnly ? ToolsRes.getString("LibraryBrowser.Tooltip.Expand") : //$NON-NLS-1$
						ToolsRes.getString("LibraryBrowser.Tooltip.Contract")); //$NON-NLS-1$
					}
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					iconLabel.setIcon(boldIcon);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					iconLabel.setIcon(normalIcon);
				}
			});
			add(titleLabel, BorderLayout.WEST);
			add(iconLabel, BorderLayout.EAST);
			setIcons(lightIcon, heavyIcon);
		}

		void setTitle(String title) {
			titleLabel.setText(title);
		}

		void setIcons(Icon lightIcon, Icon heavyIcon) {
			normalIcon = lightIcon;
			boldIcon = heavyIcon;
			iconLabel.setIcon(normalIcon);
		}
	}

	/**
	 * A SwingWorker class to load the Library at startup.
	 */
	class LibraryLoader extends SwingWorker<Library, Object> {

		@Override
		public Library doInBackground() {
			if (libraryPath == null)
				return library;

			Runnable webChecker = new Runnable() {
				@Override
				public void run() {
					if (!isWebConnected()) {
						JOptionPane.showMessageDialog(LibraryBrowser.this,
								ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Message"), //$NON-NLS-1$
								ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Title"), //$NON-NLS-1$
								JOptionPane.WARNING_MESSAGE);
					}
				}
			};
			if (!ResourceLoader.isHTTP(libraryPath)) {
				// load library
				library.load(libraryPath);
				localLibraryLoaded = true;
				// add previously open tabs that are available
				if (library.openTabPaths != null) {
					ArrayList<String> unopenedTabs = new ArrayList<String>();
					String[] paths = library.openTabPaths;
					for (String path : paths) {
						// first check cache
						File cachedFile = ResourceLoader.getSearchCacheFile(path); // 
						if (cachedFile.exists()) {
							addTabAndExecute(path, null, null);
						} else {
							unopenedTabs.add(path);
						}
					}
					if (!unopenedTabs.isEmpty()) {
						paths = unopenedTabs.toArray(new String[unopenedTabs.size()]);
						unopenedTabs.clear();
						for (String path : paths) {
							// check for local resource
							Resource res = ResourceLoader.getResource(path);
							if (res != null && !ResourceLoader.isHTTP(path)) {
								 addTabAndExecute(path, null, null);
							} else {
								unopenedTabs.add(path);
							}
						}
					}
					boolean done = unopenedTabs.isEmpty();
					// save web-based tabs for done() method
					library.openTabPaths = done ? null : unopenedTabs.toArray(new String[unopenedTabs.size()]);
				}
				webChecker.run();
			} else {
				webChecker.run(); // check web connection first
				if (isWebConnected()) {
					library.load(libraryPath);
				}
			}
			return library;
		}

		@Override
		protected void done() {
			try {
				Library library = get();
				// add previously open tabs not available for loading in doInBackground method
				if (library.openTabPaths != null) {
					for (final String path : library.openTabPaths) {
						boolean available = isWebConnected() && ResourceLoader.isHTTP(path);
						if (available) {
							addTabAndExecute(path, null, null);
						}
					}
				}
			} catch (Exception ignore) {
			}
			refreshCollectionsMenu();
			refreshRecentMenu();
		}
	}

	/**
	 * A SwingWorker class to load tabs.
	 */
	class TabLoader extends SwingWorker<LibraryTreePanel, Object> {

		String path;
		int index;
		List<String> treePath;

		TabLoader(String pathToAdd, int tabIndex, List<String> treePath) {
			path = pathToAdd;
			index = tabIndex;
			this.treePath = treePath;
		}

		@Override
		public LibraryTreePanel doInBackground() {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			String realPath = path;
			File cachedFile = ResourceLoader.getSearchCacheFile(path);
			if (cachedFile.exists() && ResourceLoader.isHTTP(path)) { // $NON-NLS-1$
				realPath = cachedFile.getAbsolutePath();
//				saveToCache = false;
			}

			// BH 2020.04.14 added to speed up zip file checking
			boolean doCache = OSPRuntime.doCacheZipContents;
			OSPRuntime.doCacheZipContents = true;
			LibraryResource resource = loadResource(realPath);
			OSPRuntime.doCacheZipContents = doCache;
			if (!doCache)
				ResourceLoader.clearZipCache();
			
			boolean isLocalTRZ =  (!ResourceLoader.isHTTP(realPath) && ResourceLoader.isJarZipTrz(path.toLowerCase(), false));
			
			if (resource != null) {
				LibraryTreePanel treePanel = null;
	  		// open local files in the recentCollection instead of in their own tab
				if (isLocalTRZ) {
					String recentCollectionPath = getOSPPath()+RECENT_COLLECTION_NAME;
	  			path = recentCollectionPath;
	  			LibraryCollection collection = getRecentCollection();
		    	LibraryResource child = loadResource(realPath);
		    	if (child!=null) {
		  			if (collection.insertResource(child, 0)) {
		    			// remove excess nodes from recent collection
		  				LibraryResource[] resources = collection.getResources();
		  				int n = resources.length - maxRecentCollectionSize;
		  				for (int i = 0; i<n; i++) {
			  				collection.removeResource(resources[resources.length-1-i]);  					
		  				}
		  				child.collectionPath = recentCollectionPath;
//			  			XMLControl control = new XMLControlElement(collection);
//			    		control.setValue("real_path", recentCollectionPath); //$NON-NLS-1$
//			    		control.write(recentCollectionPath);
		  			}
		  			// name child before getting tree path
		  			String prevName = child.getName();
//		  			child.setName(getNodeName(child)); // DB! in ver 5.1.5 this looked for html title in trz
		  			treePath = child.getTreePath(null);
		  			child.setName(prevName);
	  		  	index = getTabIndexFromPath(recentCollectionPath);
		    	}
	  			resource = collection;					
				}
				treePanel = index < 0 ? createLibraryTreePanel() : getTreePanel(index);
    		if (isLocalTRZ) {
	    		// tab is non-editable XML
	    		treePanel.setRootResource(resource, path, false, true);
    		}
    		else {
	    		// tab is editable only if it is a local XML file
					// and if running in Java, not the recent collection and not in the search cache
					boolean editable = !ResourceLoader.isHTTP(path) && path.toLowerCase().endsWith(".xml"); //$NON-NLS-1$
					if (!OSPRuntime.isJS) {
		    		if (path.equals(getOSPPath() + RECENT_COLLECTION_NAME)) {
		    			editable = false;
		    			resource.collectionPath = path;
		    		}
		    		File searchCache = ResourceLoader.getSearchCache();
		    		if (XML.forwardSlash(path).startsWith(XML.forwardSlash(searchCache.getPath()))) 
		    			editable = false;
					}
					treePanel.setRootResource(resource, path, editable, isResourcePathXML);
				}
				return treePanel;
			}
			return null;
		}

		@Override
		protected void done() {
			try {
				LibraryTreePanel treePanel = get();
				if (treePanel != null) {
					treePanel.setFontLevel(FontSizer.getLevel());

					if (index < 0) {
						tabbedPane.addTab("", treePanel); //$NON-NLS-1$
						index = tabbedPane.getTabCount() - 1;
					}
					refreshTabTitle(path, treePanel.rootResource);
					tabbedPane.setToolTipTextAt(index, path);

					treePanel.setSelectionPath(treePath);

					// start background SwingWorker to load metadata and set up search database
					if (treePanel.metadataLoader != null) {
						treePanel.metadataLoader.cancel();
					}
					treePanel.metadataLoader = treePanel.new MetadataLoader(treePath);
					treePanel.metadataLoader.execute();
					setProgress(index);
				} else {
					String s = ToolsRes.getString("LibraryBrowser.Dialog.CollectionNotFound.Message"); //$NON-NLS-1$
					JOptionPane.showMessageDialog(LibraryBrowser.this, s + ":\n" + path, //$NON-NLS-1$
							ToolsRes.getString("LibraryBrowser.Dialog.CollectionNotFound.Title"), //$NON-NLS-1$
							JOptionPane.WARNING_MESSAGE);
					library.removeRecent(path);
					refreshRecentMenu();
					setProgress(-1);
				}
			} catch (Exception ignore) {
			}
			setCursor(Cursor.getDefaultCursor());
		}
	}

//______________________________ static methods and classes ____________________________

	/**
	 * Entry point when run as an independent application.
	 * 
	 * @param args String[] ignored
	 */
	public static void main(String[] args) {
		final LibraryBrowser browser = LibraryBrowser.getBrowser();
		browser.addOSPLibrary(TRACKER_LIBRARY);
		browser.addOSPLibrary(SHARED_LIBRARY);
		browser.addComPADRECollection(LibraryComPADRE.EJS_SERVER_TREE + LibraryComPADRE.PRIMARY_ONLY);
		browser.addComPADRECollection(LibraryComPADRE.TRACKER_SERVER_TREE + LibraryComPADRE.PRIMARY_ONLY);
		browser.refreshCollectionsMenu();

		// code below opens Tracker when LibraryBrowser is launched as an independent
		// application

//  	browser.addPropertyChangeListener("target", new PropertyChangeListener() { //$NON-NLS-1$
//  		public void propertyChange(PropertyChangeEvent e) {
//  			LibraryResource record = (LibraryResource)e.getNewValue();
//				String target = XML.getResolvedPath(record.getTarget(), record.getBasePath());
//					  				
//  			ArrayList<String> extensions = new ArrayList<String>();
//  			for (String ext: VideoIO.getVideoExtensions()) {
//  				extensions.add(ext);
//  			}
//  			extensions.add("trk"); //$NON-NLS-1$
//  			extensions.add("zip"); //$NON-NLS-1$
//  			extensions.add("trz"); //$NON-NLS-1$
//  			for (String ext: extensions) {
//  				if (target.endsWith("."+ext)) { //$NON-NLS-1$
//  			    Tracker tracker = Tracker.getTracker();
//  			    final TFrame frame = tracker.getFrame();
//  			    frame.setVisible(true);
//            try {
//        			target = ResourceLoader.getURIPath(target);
//							URL url = new URL(target);
//							TrackerIO.open(url, new TrackerPanel(), frame);
//						} catch (Exception ex) {ex.printStackTrace();}
//     			}
//  			}
//  		}
//  	});

		browser.exitOnClose = true;
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (dim.width - browser.getBounds().width) / 2;
		int y = (dim.height - browser.getBounds().height) / 2;
		browser.setLocation(x, y);
		browser.setVisible(true);
	}

	/**
	 * Returns true if connected to the web.
	 * 
	 * @return true if web connected
	 */
	protected boolean isWebConnected() {
		if (!checkedWebConnection) {
			checkedWebConnection = true;
			webConnected = ResourceLoader.isWebConnected();
			if (!webConnected) {
				JOptionPane.showMessageDialog(this,
						ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Message"), //$NON-NLS-1$
						ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Title"), //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE);
			}
		}
		return webConnected;
	}

	/**
	 * Returns the redirect URL path, if any, of an HTML page.
	 * 
	 * @param code the HTML code
	 * @return the redirect path
	 */
	protected static String getRedirectFromHTMLCode(String code) {
		if (code == null)
			return null;
		String[] parts = code.split("<!--"); //$NON-NLS-1$
		if (parts.length > 1) {
			for (int i = 1; i < parts.length; i++) {
				if (parts[i].trim().startsWith("redirect:")) { //$NON-NLS-1$
					String[] subparts = parts[i].split("-->"); //$NON-NLS-1$
					return subparts.length > 1 ? subparts[0].substring(9).trim() : null;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the metadata, if any, defined in HTML code
	 * 
	 * @param htmlCode the HTML code
	 * @return a list of String[] {name, value}
	 */
	public static ArrayList<String[]> getMetadataFromHTML(String htmlCode) {
		ArrayList<String[]> results = new ArrayList<String[]>();
		if (htmlCode == null)
			return results;
		String[] parts = htmlCode.split("<meta name=\""); //$NON-NLS-1$
		for (int i = 1; i < parts.length; i++) { // ignore parts[0]
			// parse metadata and add to array
			int n = parts[i].indexOf("\">"); //$NON-NLS-1$
			if (n > -1) {
				parts[i] = parts[i].substring(0, n);
				String divider = "\" content=\""; //$NON-NLS-1$
				String[] subparts = parts[i].split(divider);
				if (subparts.length > 1) {
					String name = subparts[0];
					String value = subparts[1];
					results.add(new String[] { name, value });
				}
			}
		}
		return results;
	}

  /**
   * Gets the path to the local OSP folder.
   * 
   * @return the path
   */
  protected static String getOSPPath() {
  	if (OSPRuntime.isJS)
  		return null;
  	if (ospPath == null) {
			String userHome = OSPRuntime.getUserHome().replace('\\', '/');
			String ospFolder = OSPRuntime.isWindows()? WINDOWS_OSP_DIRECTORY: OSP_DIRECTORY;
			ospPath = userHome+ospFolder;
			// if OSP folder doesn't exist in user home, then use 
			// default OSPRuntime search directory
			if (!new File(ospPath).exists()) {
	  		ArrayList<String> dirs = OSPRuntime.getDefaultSearchPaths();
	  		ospPath = XML.forwardSlash(dirs.get(0));
			}
			if (!ospPath.endsWith("/")) { //$NON-NLS-1$
				ospPath += "/"; //$NON-NLS-1$
			}		
  	}
  	return ospPath;
  }  
	


	/**
	 * A FileFilter that accepts trk, pdf, html, trz and zip (if trk found inside) files
	 * with names that do NOT start with underscore. Also accepts all supported video files.
	 */
	static class TrackerDLFilter implements FileFilter {

		@Override
		public boolean accept(File file) {
			String name;
			if (file == null || file.isDirectory() || (name = file.getName()).startsWith("_")) //$NON-NLS-1$
				return false;
			if (name.indexOf("TrackerSet=") >= 0)
				return true;
			String ext = ("" + XML.getExtension(name)).toLowerCase();
			switch (ext) {
			case "xml":
				return false;
			case "zip": //$NON-NLS-1$
				// This is a massive test just to find out if we might have an appropriate file
				// It is used, for example, to see if a control could be NOT an XML file.
				if (ResourceLoader.isHTTP(file.toString())) {
					return true;
				}
				Map<String, ZipEntry> files = ResourceLoader.getZipContents(file.getAbsolutePath());
				for (String next : files.keySet()) {
					if (next.toLowerCase().endsWith(".trk")) //$NON-NLS-1$
						return true;
				}
				return false;
			case "htm": //$NON-NLS-1$
			case "html": //$NON-NLS-1$
			case "trk": //$NON-NLS-1$
			case "pdf": //$NON-NLS-1$
			case "trz": //$NON-NLS-1$
				return true;
			default:
				for (String next : VideoIO.getVideoExtensions()) {
					if (ext.equals(next.toLowerCase()))
						return true;
				}
				return false;
			}
		}
	}

	/**
	 * A FileFilter that accepts only directories with names that do NOT start with
	 * underscore.
	 */
	static class DirectoryFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			if (file.getName().startsWith("_")) //$NON-NLS-1$
				return false;
			return file.isDirectory();
		}
	}

	/**
	 * A FileFilter that accepts html files with names that do NOT start with
	 * underscore.
	 */
	static class HTMLFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			if (file == null || file.isDirectory())
				return false;
			String name = file.getName();
			if (name.startsWith("_")) //$NON-NLS-1$
				return false;
			String ext = XML.getExtension(name);
			if (ext == null)
				return false;
			if (ext.toLowerCase().startsWith("htm")) //$NON-NLS-1$
				return true;
			return false;
		}
	}

	/**
	 * A FileFilter that accepts xml files and directories.
	 */
	static class XMLFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			if (file == null)
				return false;
			if (file.isDirectory())
				return true;
			String ext = XML.getExtension(file.getName());
			if (ext == null)
				return false;
			if (ext.toLowerCase().equals("xml")) //$NON-NLS-1$
				return true;
			return false;
		}
	}

	/**
	 * A filechooser FileFilter that accepts files and folders.
	 */
	static class FilesAndFoldersFilter extends javax.swing.filechooser.FileFilter {
		// accept all directories and files.
		@Override
		public boolean accept(File f) {
			return f != null;
		}

		// the description of this filter
		@Override
		public String getDescription() {
			return ToolsRes.getString("LibraryBrowser.FilesAndFoldersFilter.Description"); //$NON-NLS-1$
		}

	}

	public static void clearCache() {
		ResourceLoader.clearOSPCache(ResourceLoader.getOSPCache(), false);
		LibraryTreePanel.clearMaps();
	}

}
