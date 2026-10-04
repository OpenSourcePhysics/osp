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
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.Library.ListMap;

/**
 * A GUI for managing My Library, search targets, and the OSP cache.
 *
 * package-private
 *
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
class LibraryManager extends JDialog {

	protected final static int SELECT_COLLECTIONS = 1;
	protected final static int SELECT_SEARCH      = 2;
	protected final static int SELECT_CACHE       = 3;
	
	private LibraryBrowser browser;
	private Library library;
	private JTabbedPane tabbedPane;
	private JPanel collectionsPanel, importsPanel, searchPanel, cachePanel;
	//, recentPanel;
	private JList<String> collectionList;
	private JList<String> guestList;
	private JTextField nameField, pathField;
	private JButton okButton, setCacheButton;
	private JButton moveUpButton, moveDownButton, addButton, removeButton; // for collections and imports tabs
	private JButton allButton, noneButton, clearCacheButton; // for search and cache tabs
	private JToolBar libraryButtonbar;
	private Box nameBox, pathBox, libraryEditBox, searchBox, cacheBox;
	private JLabel nameLabel, pathLabel;
	private Font sharedFont;
	private TitledBorder collectionsTitleBorder, importsTitleBorder, searchTitleBorder, cacheTitleBorder;
	private ArrayList<SearchCheckBox> checkboxes = new ArrayList<SearchCheckBox>();
	private Dimension defaultSize = new Dimension(400, 300);
	private Border listButtonBorder;

	/**
	 * Constructor for a frame
	 * 
	 * @param browser a LibraryBrowser
	 * @param frame   the frame
	 */
	protected LibraryManager(LibraryBrowser browser, JFrame frame) {
		super(frame, true);
		init(browser);
	}

	private void init(LibraryBrowser browser) {
		this.browser = browser;
		library = browser.getLibrary();
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		createGUI();
		Dimension dim = new Dimension(defaultSize);
		double factor = 1 + FontSizer.getLevel() * 0.25;
		dim.width = (int) (dim.width * factor);
		dim.height = (int) (dim.height * factor);
		setSize(dim);
		refreshGUI();
		// center on screen
		dim = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle bounds = getBounds();
		int x = (dim.width - bounds.width) / 2;
		int y = (dim.height - bounds.height) / 2;
		setLocation(x, y);
	}

	/**
	 * Constructor for a dialog
	 * 
	 * @param browser a LibraryBrowser
	 * @param dialog  the dialog
	 */
	protected LibraryManager(LibraryBrowser browser, JDialog dialog) {
		super(dialog, true);
		init(browser);
	}

	@Override
	public void setVisible(boolean vis) {
		if (vis) {
			refreshSearchTab();
			refreshCacheTab();
		} else {
			Set<String>set = library.getNoSearchSet();
			set.clear();
			for (SearchCheckBox next : checkboxes) {
				if (!next.isSelected())
					set.add(next.urlPath);
			}
		}
		super.setVisible(vis);
	}

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		JButton throwaway = newJButton("by", null); //$NON-NLS-1$
		int h = throwaway.getPreferredSize().height;
		sharedFont = throwaway.getFont();

		// create collections list
		ListModel<String> collectionListModel = new AbstractListModel<String>() {
			@Override
			public int getSize() {
				return library.getPathToNameMap().size();
			}

			@Override
			public String getElementAt(int i) {
				return library.getPathToNameMap().get(i);
			}
		};
		collectionList = new JList<String>(collectionListModel);
		collectionList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				refreshGUI();
			}
		});
		collectionList.setFixedCellHeight(h);
		collectionList.setFont(sharedFont);
		collectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// create import list
		ListModel<String> importListModel = new AbstractListModel<String>() {
			@Override
			public int getSize() {
				return library.getImportedPathToLibraryMap().size();
			}

			@Override
			public String getElementAt(int i) {
				return library.getImportedPathToLibraryMap().get(i);
			}
		};
		guestList = new JList<String>(importListModel);
		guestList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				refreshGUI();
			}
		});
		guestList.setFont(sharedFont);
		guestList.setFixedCellHeight(h);
		guestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// create name action, field and label
		ActionListener nameAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = pathField.getText();
				String prev = library.getPathToNameMap().getMappedName(path);
				String input = nameField.getText().trim();
				if (input == null || input.equals("") || input.equals(prev)) { //$NON-NLS-1$
					return;
				}
				library.renameCollection(path, input);
				browser.refreshCollectionsMenu();
				collectionList.repaint();
				refreshGUI();
			}
		};
		nameField = new LibraryTreePanel.EntryField();
		nameField.addActionListener(nameAction);
		nameField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				nameField.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				nameAction.actionPerformed(null);
			}
		});
		nameField.setBackground(Color.white);

		nameLabel = new JLabel();
		nameLabel.setFont(sharedFont);
		nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
		nameLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		pathField = new LibraryTreePanel.EntryField();
		pathField.setEditable(false);
		pathField.setBackground(Color.white);

		pathLabel = new JLabel();
		pathLabel.setFont(sharedFont);
		pathLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
		pathLabel.setHorizontalAlignment(SwingConstants.TRAILING);

		// create buttons
		okButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		moveUpButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doMove(true);
			}
		});
		moveDownButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doMove(false);
			}
		});
		addButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doAdd();
			}
		});
		removeButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRemove();
			}
		});
		// create all and none buttons
		allButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSelect(true);
			}
		});
		noneButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSelect(false);
			}
		});
		clearCacheButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doClearCache();
			}
		});
		setCacheButton = newJButton(null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSetCache();
			}
		});
		Border emptyInside = BorderFactory.createEmptyBorder(1, 2, 1, 2);
		Border etched = BorderFactory.createEtchedBorder();
		Border buttonbarBorder = BorderFactory.createCompoundBorder(etched, emptyInside);

		libraryButtonbar = new JToolBar();
		libraryButtonbar.setFloatable(false);
		libraryButtonbar.setBorder(buttonbarBorder);
		libraryButtonbar.add(moveUpButton);
		libraryButtonbar.add(moveDownButton);
		libraryButtonbar.add(addButton);
		libraryButtonbar.add(removeButton);

		nameBox = Box.createHorizontalBox();
		nameBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 4));
		nameBox.add(nameLabel);
		nameBox.add(nameField);
		pathBox = Box.createHorizontalBox();
		pathBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 4));
		pathBox.add(pathLabel);
		pathBox.add(pathField);
		libraryEditBox = Box.createVerticalBox();
		libraryEditBox.add(nameBox);
		libraryEditBox.add(pathBox);

		// create and assemble tabs
		// collections tab
		collectionsPanel = new JPanel(new BorderLayout());
		JScrollPane scroller = new JScrollPane(collectionList);
		scroller.setViewportBorder(etched);
		scroller.getVerticalScrollBar().setUnitIncrement(8);
		collectionsTitleBorder = BorderFactory.createTitledBorder(""); //$NON-NLS-1$
		scroller.setBorder(collectionsTitleBorder);
		collectionsPanel.add(scroller, BorderLayout.CENTER);
		collectionsPanel.add(libraryEditBox, BorderLayout.SOUTH);
		collectionsPanel.add(libraryButtonbar, BorderLayout.NORTH);

		// imports tab
		importsPanel = new JPanel(new BorderLayout());
		scroller = new JScrollPane(guestList);
		scroller.setViewportBorder(etched);
		scroller.getVerticalScrollBar().setUnitIncrement(8);
		importsTitleBorder = BorderFactory.createTitledBorder(""); //$NON-NLS-1$
		scroller.setBorder(importsTitleBorder);
		importsPanel.add(scroller, BorderLayout.CENTER);

		// search tab
		searchPanel = new JPanel(new BorderLayout());
		searchBox = Box.createVerticalBox();
		searchBox.setBackground(Color.white);
		searchBox.setOpaque(true);
		refreshSearchTab();

		scroller = new JScrollPane(searchBox);
		scroller.setViewportBorder(etched);
		scroller.getVerticalScrollBar().setUnitIncrement(8);
		searchTitleBorder = BorderFactory.createTitledBorder(""); //$NON-NLS-1$
		scroller.setBorder(searchTitleBorder);
		searchPanel.add(scroller, BorderLayout.CENTER);
		JToolBar searchButtonbar = new JToolBar();
		searchButtonbar.setFloatable(false);
		searchButtonbar.setBorder(buttonbarBorder);
		searchButtonbar.add(allButton);
		searchButtonbar.add(noneButton);
		searchPanel.add(searchButtonbar, BorderLayout.NORTH);

		// cache tab
		cachePanel = new JPanel(new BorderLayout());
		cacheBox = Box.createVerticalBox();
		cacheBox.setBackground(Color.white);
		cacheBox.setOpaque(true);
		refreshCacheTab();

		scroller = new JScrollPane(cacheBox);
		scroller.setViewportBorder(etched);
		scroller.getVerticalScrollBar().setUnitIncrement(8);
		cacheTitleBorder = BorderFactory.createTitledBorder(""); //$NON-NLS-1$
		scroller.setBorder(cacheTitleBorder);
		cachePanel.add(scroller, BorderLayout.CENTER);
		JToolBar cacheButtonbar = new JToolBar();
		cacheButtonbar.setFloatable(false);
		cacheButtonbar.setBorder(buttonbarBorder);
		cacheButtonbar.add(clearCacheButton);
		cacheButtonbar.add(setCacheButton);
		cachePanel.add(cacheButtonbar, BorderLayout.NORTH);

		// create tabbedPane
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("", collectionsPanel); //$NON-NLS-1$
//		tabbedPane.addTab("", importsPanel); //$NON-NLS-1$
		tabbedPane.addTab("", cachePanel); //$NON-NLS-1$
		if (!OSPRuntime.isJS)
			tabbedPane.addTab("", searchPanel); //$NON-NLS-1$

		// add change listener last
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (tabbedPane.getSelectedComponent() == collectionsPanel) {
					collectionsPanel.add(libraryButtonbar, BorderLayout.NORTH);
					collectionsPanel.add(libraryEditBox, BorderLayout.SOUTH);
					refreshGUI();
				} else if (tabbedPane.getSelectedComponent() == importsPanel) {
					importsPanel.add(libraryButtonbar, BorderLayout.NORTH);
					importsPanel.add(libraryEditBox, BorderLayout.SOUTH);
					refreshGUI();
				}
			}
		});

		Border space = BorderFactory.createEmptyBorder(0, 2, 0, 2);
		listButtonBorder = BorderFactory.createCompoundBorder(etched, space);

		// assemble content pane
		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		JPanel south = new JPanel();
		south.add(okButton);
		contentPane.add(south, BorderLayout.SOUTH);
	}

	protected void doMove(boolean isUp) {
		boolean isImports = tabbedPane.getSelectedComponent() == importsPanel;
		JList<String> list = (isImports ? guestList : collectionList);
		ListMap paths = (isImports ? library.getImportedPathToLibraryMap() : library.getPathToNameMap());
		int i = list.getSelectedIndex();
		String path = paths.get(i);
		i += (isUp ? -1 : 1);
		paths.remove(path);
		paths.add(i, path);
		list.setSelectedIndex(i);
		browser.refreshCollectionsMenu();
		browser.refreshGUI();
	}

	protected void doAdd() {
		boolean imported = tabbedPane.getSelectedComponent() == importsPanel;
		String message = imported ? ToolsRes.getString("LibraryBrowser.Dialog.AddLibrary.Message") : //$NON-NLS-1$
		ToolsRes.getString("LibraryBrowser.Dialog.AddCollection.Message"); //$NON-NLS-1$
		String title = imported ? ToolsRes.getString("LibraryBrowser.Dialog.AddLibrary.Title") : //$NON-NLS-1$
		ToolsRes.getString("LibraryBrowser.Dialog.AddCollection.Title"); //$NON-NLS-1$
		String input = GUIUtils.showInputDialog(browser, message, title, JOptionPane.QUESTION_MESSAGE, null);
		if (input == null || input.equals("")) { //$NON-NLS-1$
			return;
		}
		String path = input;
		path = XML.forwardSlash(path);
		// BH 2020.11.12 presumes file not https here
		path = ResourceLoader.getNonURIPath(path);

		if (tabbedPane.getSelectedComponent() == collectionsPanel) {
			boolean isResource = false;
			if (!ResourceLoader.isHTTP(path) && new File(path).isDirectory()) {
				isResource = true;
			} else {
				XMLControl control = new XMLControlElement(path);
				if (!control.failedToRead() && control.getObjectClass() == LibraryCollection.class) {
					isResource = true;
				}
			}
			if (isResource) {
				System.out.println("LM OK " + path);
				browser.addToCollections(path);
				ListModel<String> model = collectionList.getModel();
				collectionList.setModel(model);
				refreshGUI();
				collectionList.repaint();
				collectionList.setSelectedIndex(library.getPathToNameMap().size() - 1);
				browser.refreshCollectionsMenu();
				return;
			}
		}
		if (tabbedPane.getSelectedComponent() == importsPanel) {
			boolean isLibrary = false;
			XMLControl control = new XMLControlElement(path);
			if (!control.failedToRead() && control.getObjectClass() == Library.class) {
				isLibrary = true;
			}
			if (isLibrary) {
				Library newLibrary = new Library();
				control.loadObject(newLibrary);
				if (library.importLibrary(path, newLibrary)) {
					ListModel<String> model = guestList.getModel();
					guestList.setModel(model);
					refreshGUI();
					guestList.repaint();
					guestList.setSelectedIndex(library.getImportedPathToLibraryMap().size() - 1);
					browser.refreshCollectionsMenu();
				}
				return;
			}
		}
		warnNotFound(path);
	}

	protected void doRemove() {
		boolean isImports = tabbedPane.getSelectedComponent() == importsPanel;
		JList<String> list = isImports ? guestList : collectionList;
		ArrayList<String> paths = isImports ? library.getImportedPathToLibraryMap() : library.getPathToNameMap();
		int i = list.getSelectedIndex();
		String path = paths.get(i);
		paths.remove(path);
		if (isImports)
			library.getImportedPathToLibraryMap().remove(path);
		else
			library.getPathToNameMap().remove(path);
		list.repaint();
		if (i >= paths.size()) {
			list.setSelectedIndex(paths.size() - 1);
		}
		browser.refreshCollectionsMenu();
		refreshGUI();
		browser.refreshGUI();
	}

	protected void doSelect(boolean b) {
		for (SearchCheckBox next : checkboxes) {
			next.setSelected(b);
		}
	}

	protected void doSetCache() {
		ResourceLoader.setOSPCache(ResourceLoader.chooseOSPCache(browser));
		refreshCacheTab();
	}

	private JButton newJButton(String text, ActionListener listener) {
		JButton b = new JButton(text);
		b.setOpaque(false);
		b.setBorder(browser.getButtonBorder());
		if (listener != null)
			b.addActionListener(listener);
		return b;
	}

	protected void doClearCache() {
		LibraryBrowser.clearCache();
		refreshCacheTab();
		tabbedPane.repaint();
	}

	protected void warnNotFound(String path) {
		String s = ToolsRes.getString("LibraryBrowser.Dialog.CollectionNotFound.Message"); //$NON-NLS-1$
		
		System.out.println("WARN - LibraryManager " + s + " "+ path);
//		
//		JOptionPane.showMessageDialog(this, s + ":\n" + path, //$NON-NLS-1$
//				ToolsRes.getString("LibraryBrowser.Dialog.CollectionNotFound.Title"), //$NON-NLS-1$
//				JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Refreshes the GUI including locale-based resource strings.
	 */
	protected void refreshGUI() {
		setTitle(ToolsRes.getString("LibraryManager.Title")); //$NON-NLS-1$

		okButton.setText(ToolsRes.getString("Tool.Button.Close")); //$NON-NLS-1$
		addButton.setText(ToolsRes.getString("LibraryManager.Button.Add")); //$NON-NLS-1$
		removeButton.setText(ToolsRes.getString("LibraryManager.Button.Remove")); //$NON-NLS-1$
		moveUpButton.setText(ToolsRes.getString("LibraryTreePanel.Button.Up")); //$NON-NLS-1$
		moveDownButton.setText(ToolsRes.getString("LibraryTreePanel.Button.Down")); //$NON-NLS-1$
		allButton.setText(ToolsRes.getString("LibraryManager.Button.All")); //$NON-NLS-1$
		noneButton.setText(ToolsRes.getString("LibraryManager.Button.None")); //$NON-NLS-1$
		clearCacheButton.setText(ToolsRes.getString("LibraryManager.Button.ClearCache")); //$NON-NLS-1$
		setCacheButton.setText(ToolsRes.getString("LibraryManager.Button.SetCache")); //$NON-NLS-1$

		addButton.setToolTipText(ToolsRes.getString("LibraryManager.Button.Add.Tooltip")); //$NON-NLS-1$
		removeButton.setToolTipText(ToolsRes.getString("LibraryManager.Button.Remove.Tooltip")); //$NON-NLS-1$
		moveUpButton.setToolTipText(ToolsRes.getString("LibraryTreePanel.Button.Up.Tooltip")); //$NON-NLS-1$
		moveDownButton.setToolTipText(ToolsRes.getString("LibraryTreePanel.Button.Down.Tooltip")); //$NON-NLS-1$
		allButton.setToolTipText(ToolsRes.getString("LibraryManager.Button.All.Tooltip")); //$NON-NLS-1$
		noneButton.setToolTipText(ToolsRes.getString("LibraryManager.Button.None.Tooltip")); //$NON-NLS-1$
		clearCacheButton.setToolTipText(ToolsRes.getString("LibraryManager.Button.ClearCache.Tooltip")); //$NON-NLS-1$
		setCacheButton.setToolTipText(ToolsRes.getString("LibraryManager.Button.SetCache.Tooltip")); //$NON-NLS-1$

		nameLabel.setText(ToolsRes.getString("LibraryManager.Label.Name") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
		pathLabel.setText(ToolsRes.getString("LibraryManager.Label.Path") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
		collectionsTitleBorder.setTitle(ToolsRes.getString("LibraryManager.Title.MenuItems") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
		importsTitleBorder.setTitle(ToolsRes.getString("LibraryManager.Title.Import") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
		searchTitleBorder.setTitle(ToolsRes.getString("LibraryManager.Title.Search") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
		cacheTitleBorder.setTitle(ToolsRes.getString("LibraryManager.Title.Cache") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
		int k = tabbedPane.indexOfComponent(collectionsPanel);
		if (k > -1) {
			tabbedPane.setTitleAt(k, ToolsRes.getString("LibraryManager.Tab.MyLibrary")); //$NON-NLS-1$
			tabbedPane.setToolTipTextAt(k, ToolsRes.getString("LibraryManager.Tab.MyLibrary.Tooltip")); //$NON-NLS-1$
		}
		k = tabbedPane.indexOfComponent(importsPanel);
		if (k > -1) {
			tabbedPane.setTitleAt(k, ToolsRes.getString("LibraryManager.Tab.Import")); //$NON-NLS-1$
			tabbedPane.setToolTipTextAt(k, ToolsRes.getString("LibraryManager.Tab.Import.Tooltip")); //$NON-NLS-1$
		}
		k = tabbedPane.indexOfComponent(searchPanel);
		if (k > -1) {
			tabbedPane.setTitleAt(k, ToolsRes.getString("LibraryManager.Tab.Search")); //$NON-NLS-1$
			tabbedPane.setToolTipTextAt(k, ToolsRes.getString("LibraryManager.Tab.Search.Tooltip")); //$NON-NLS-1$
		}
		k = tabbedPane.indexOfComponent(cachePanel);
		if (k > -1) {
			tabbedPane.setTitleAt(k, ToolsRes.getString("LibraryManager.Tab.Cache")); //$NON-NLS-1$
			tabbedPane.setToolTipTextAt(k, ToolsRes.getString("LibraryManager.Tab.Cache.Tooltip")); //$NON-NLS-1$
		}

		resizeLabels();

		pathField.setForeground(LibraryTreePanel.getDefaultForeground());
		if (tabbedPane.getSelectedComponent() == collectionsPanel) {
			nameField.setEditable(true);
			int i = collectionList.getSelectedIndex();
			ListMap paths = library.getPathToNameMap();
			moveDownButton.setEnabled(i < library.getPathToNameMap().size() - 1);
			moveUpButton.setEnabled(i > 0);
			ListMap map = library.getPathToNameMap();
			if (i > -1 && i < paths.size()) {
				removeButton.setEnabled(true);
				String path = paths.get(i);
				pathField.setText(path);
				pathField.setCaretPosition(0);
				String name = map.getMappedName(path);
				nameField.setText(name);
				boolean unavailable = ResourceLoader.isHTTP(path) && !browser.isWebConnected(null);
				Resource res = unavailable ? null : ResourceLoader.getResourceZipURLsOK(path);
				if (res == null) {
					pathField.setForeground(LibraryTreePanel.darkRed);
				}
			} else {
				removeButton.setEnabled(false);
				nameField.setEditable(false);
				nameField.setText(null);
				nameField.setBackground(Color.white);
				pathField.setText(null);
				pathField.setBackground(Color.white);
			}
		} else if (tabbedPane.getSelectedComponent() == importsPanel) {
			nameField.setEditable(false);
			int i = guestList.getSelectedIndex();
			int n = library.getImportedPathToLibraryMap().size();
			moveDownButton.setEnabled(i < n - 1);
			moveUpButton.setEnabled(i > 0);
			if (i > -1 && i < n) {
				removeButton.setEnabled(true);
				String path = library.getImportedPathToLibraryMap().getMappedName(i);
				pathField.setText(path);
				pathField.setCaretPosition(0);
				String name = library.getImportedPathToLibraryMap().getLibraryName(path);
				nameField.setText(name);
				boolean unavailable = ResourceLoader.isHTTP(path) && !browser.isWebConnected(null);
				Resource res = unavailable ? null : ResourceLoader.getResourceZipURLsOK(path);
				if (res == null) {
					pathField.setForeground(LibraryTreePanel.darkRed);
				}
			} else {
				removeButton.setEnabled(false);
				nameField.setText(null);
				nameField.setBackground(Color.white);
				pathField.setText(null);
				pathField.setBackground(Color.white);
			}
		}
		nameField.setBackground(Color.white);
		pathField.setBackground(Color.white);
	}

	protected void refreshSearchTab() {
		// refresh list of cached search targets
		searchBox.removeAll();
		checkboxes.clear();
		ArrayList<JLabel> labels = new ArrayList<JLabel>();
		TreeMap<String, String> map = browser.getSearchPathMap();
		Set<String> names = map.keySet();
		for (String name: names) {
			String path = map.get(name);
			JLabel label = new JLabel(name);
			label.setToolTipText(path);
			labels.add(label);

			SearchCheckBox checkbox = new SearchCheckBox(path);
			checkboxes.add(checkbox);
			Box bar = Box.createHorizontalBox();
			bar.add(label);
			bar.add(checkbox);
			if (!ResourceLoader.isHTTP(path))
				bar.add(new DeleteButton(path));
			bar.add(Box.createHorizontalGlue());
			searchBox.add(bar);			
		}
		FontSizer.setFonts(searchBox, FontSizer.getLevel());
		
		if (labels.isEmpty())
			return;

		// set label sizes
		Font font = labels.get(0).getFont();
		int w = 0;
		for (JLabel next : labels) {
			Rectangle2D rect = font.getStringBounds(next.getText(), OSPRuntime.frc);
			w = Math.max(w, (int) rect.getWidth());
		}
		Dimension labelSize = new Dimension(w + 48, 20);
		for (JLabel next : labels) {
			next.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
			next.setPreferredSize(labelSize);
		}

	}

	protected void refreshCacheTab() {
		// refresh list of cache hosts
		cacheBox.removeAll();
		ArrayList<JLabel> labels = new ArrayList<JLabel>();
		File cache = ResourceLoader.getOSPCache();
		File[] hosts = (cache == null ? new File[0] : cache.listFiles(ResourceLoader.OSP_CACHE_FILTER));
		clearCacheButton.setEnabled(hosts.length > 0);

		if (hosts.length == 0) {
			JLabel label = new JLabel(ToolsRes.getString("LibraryManager.Cache.IsEmpty")); //$NON-NLS-1$
			label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
			Box box = Box.createHorizontalBox();
			box.add(label);
			box.add(Box.createHorizontalGlue());
			cacheBox.add(box);
			return;
		}

		for (File hostFile : hosts) {
			// eliminate the "osp-" that starts all cache host filenames
			String hostText = hostFile.getName().substring(4).replace('_', '.');
			long bytes = getFileSize(hostFile);
			long size = bytes / (1024 * 1024);
			if (bytes > 0) {
				if (size > 0)
					hostText += " (" + size + " MB)"; //$NON-NLS-1$ //$NON-NLS-2$
				else
					hostText += " (" + bytes / 1024 + " kB)"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			JLabel label = new JLabel(hostText);
			label.setToolTipText(hostFile.getAbsolutePath());
			labels.add(label);

			ClearHostButton button = new ClearHostButton(hostFile);

			Box bar = Box.createHorizontalBox();
			bar.add(label);
			bar.add(button);
			bar.add(Box.createHorizontalGlue());

			cacheBox.add(bar);
			FontSizer.setFonts(cacheBox, FontSizer.getLevel());
		}

		// set label sizes
		Font font = labels.get(0).getFont();
		int w = 0;
		for (JLabel next : labels) {
			Rectangle2D rect = font.getStringBounds(next.getText(), OSPRuntime.frc);
			w = Math.max(w, (int) rect.getWidth());
		}
		Dimension labelSize = new Dimension(w + 48, 20);
		for (JLabel next : labels) {
			next.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
			next.setPreferredSize(labelSize);
		}
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the desired font level
	 */
	protected void setFontLevel(int level) {
		FontSizer.setFonts(this, level);
		// set cell height of collectionList
		Font font = collectionList.getFont();
		font = FontSizer.getResizedFont(font, level);
		int space = 8 + level;
		collectionList.setFixedCellHeight(font.getSize() + space);
		resizeLabels();
	}

	private void resizeLabels() {
		// adjust size of labels so they right-align
		int w = 0;
		Font font = nameLabel.getFont();
		Rectangle2D rect = font.getStringBounds(nameLabel.getText() + " ", OSPRuntime.frc); //$NON-NLS-1$
		w = Math.max(w, (int) rect.getWidth() + 4);
		rect = font.getStringBounds(pathLabel.getText() + " ", OSPRuntime.frc); //$NON-NLS-1$
		w = Math.max(w, (int) rect.getWidth() + 4);

		Dimension labelSize = new Dimension(w, 20);
		nameLabel.setPreferredSize(labelSize);
		nameLabel.setMinimumSize(labelSize);
		pathLabel.setPreferredSize(labelSize);
		pathLabel.setMinimumSize(labelSize);
	}

	/**
	 * Gets the total size of a folder.
	 * 
	 * @param folder the folder
	 * @return the size in bytes
	 */
	private long getFileSize(File folder) {
		if (folder == null)
			return 0;
		long foldersize = 0;
		File[] files = folder.equals(ResourceLoader.getOSPCache()) ? folder.listFiles(ResourceLoader.OSP_CACHE_FILTER)
				: folder.listFiles();
		if (files == null)
			return 0;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				foldersize += getFileSize(files[i]);
			} else {
				foldersize += files[i].length();
			}
		}
		return foldersize;
	}

	/**
	 * A checkbox to add and remove a collection from the no-search list
	 */
	protected class SearchCheckBox extends JCheckBoxMenuItem {

		String urlPath;

		/**
		 * Constructs a RemoveButton.
		 * 
		 * @param path
		 */
		public SearchCheckBox(String path) {
			urlPath = path;
			setText(ToolsRes.getString("LibraryManager.Checkbox.Search")); //$NON-NLS-1$
			setFont(sharedFont);
			setSelected(!library.getNoSearchSet().contains(path));
			setOpaque(false);
			int space = 20 + FontSizer.getLevel() * 5;
			setBorder(BorderFactory.createEmptyBorder(0, 0, 0, space));
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

	}

	/**
	 * A button to delete xml files from the search cache
	 */
	protected class DeleteButton extends JButton {

		String urlPath;

		/**
		 * Constructs a DeleteButton.
		 * 
		 * @param path
		 */
		public DeleteButton(String path) {
			urlPath = path;
			setText(ToolsRes.getString("LibraryManager.Button.Delete")); //$NON-NLS-1$
			setToolTipText(ToolsRes.getString("LibraryManager.Button.Delete.Tooltip")); //$NON-NLS-1$
			setOpaque(false);
			setBorder(listButtonBorder);
			setBorderPainted(false);
			setContentAreaFilled(false);
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					setBorderPainted(true);
					setContentAreaFilled(true);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					setBorderPainted(false);
					setContentAreaFilled(false);
				}
			});
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					File file = ResourceLoader.getSearchCacheFile(urlPath);
					if (file.delete()) {
						LibraryBrowser.resetSearchMap();
						refreshSearchTab();
					}
				}
			});
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

	}

	/**
	 * A button to delete xml files from the search cache
	 */
	protected class ClearHostButton extends JButton {

		File hostCacheDir;

		/**
		 * Constructs a RemoveButton.
		 * 
		 * @param host
		 */
		public ClearHostButton(File host) {
			hostCacheDir = host;
			setText(ToolsRes.getString("LibraryManager.Button.Clear")); //$NON-NLS-1$
			setToolTipText(ToolsRes.getString("LibraryManager.Button.Clear.Tooltip")); //$NON-NLS-1$
			setOpaque(false);
			setBorder(listButtonBorder);
			setBorderPainted(false);
			setContentAreaFilled(false);
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					setBorderPainted(true);
					setContentAreaFilled(true);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					setBorderPainted(false);
					setContentAreaFilled(false);
				}
			});
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ResourceLoader.clearOSPCacheHost(hostCacheDir);
					refreshCacheTab();
					tabbedPane.repaint();
				}
			});
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

	}

	public void selectPanel(int panel) {
		JPanel p;
		switch (panel) {
		case SELECT_COLLECTIONS:
			p = collectionsPanel;
			break;
		case SELECT_SEARCH:
			p = searchPanel;
			break;
		case SELECT_CACHE:
			p = cachePanel;
			break;
		default:
			return;
		}
		tabbedPane.setSelectedComponent(p);
		setVisible(true);
	}

	protected LibraryManager update() {
		if (library.getPathToNameMap().size() > 0 && collectionList.getSelectedIndex() == -1) {
			collectionList.setSelectedIndex(0);
		}
		if (library.getImportedPathToLibraryMap().size() > 0 && guestList.getSelectedIndex() == -1) {
			guestList.setSelectedIndex(0);
		}
		setFontLevel(FontSizer.getLevel());
		return this;
	}

}
