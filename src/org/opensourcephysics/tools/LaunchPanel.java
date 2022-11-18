/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.LaunchNode.DisplayTab;
import org.opensourcephysics.tools.Launcher.HTMLPane;

/**
 * This is a panel that displays a tree with a LaunchNode root.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class LaunchPanel extends JPanel {
	// static fields
	protected static final String TEXT_TYPE = "text"; // text editor type //$NON-NLS-1$
	// instance fields
	protected JTree tree;
	protected DefaultTreeModel treeModel;
	protected JSplitPane splitPane;
	protected JPanel dataPanel; // on splitPane right
	protected JTabbedPane tabbedPane; // each tab is an html page or Java model
	protected JEditorPane descriptionPane;
	protected JScrollPane descriptionScroller;
	protected boolean showAllNodes;
	protected Map<LaunchNode, VisibleNode> visibleNodeMap = new HashMap<LaunchNode, VisibleNode>();
	protected Launcher launcher;
	protected boolean rebuildingTabs;
	protected Map<String, String> htmlSubstitutions = new TreeMap<String, String>();
	private String noTitle;
	public boolean isSelectingNode;

	/**
	 * Constructor.
	 *
	 * @param rootNode the root node
	 * @param launcher the Launcher that is creating this panel
	 */
	public LaunchPanel(LaunchNode rootNode, Launcher launcher) {
		showAllNodes = launcher instanceof LaunchBuilder;
		this.launcher = launcher;
		createGUI();
		createTree(rootNode);
		setTreeSelectionPath(rootNode);
	}

	/**
	 * Sets the selected node.
	 *
	 * @param node the node to select
	 */
	public void setTreeSelectionPath(LaunchNode node) {
		if (node == null) {
			return;
		}
		if (node == getSelectedNode()) {
			rebuildAndDisplayTabs(node);
		} else {
			tree.setSelectionPath(new TreePath(node.getPath()));
		}
	}

	/**
	 * Sets the selected nodes.
	 *
	 * @param nodes the nodes to select
	 */
	public void setTreeSelectionPaths(ArrayList<LaunchNode> nodes) {
		if (nodes == null || nodes.size() == 0) {
			return;
		}
		TreePath[] paths = new TreePath[nodes.size()];
		int i = 0;
		for (LaunchNode node : nodes) {
			paths[i++] = new TreePath(node.getPath());
		}
		tree.setSelectionPaths(paths);
	}

	/**
	 * Sets the selected node.
	 *
	 * @param node      the node to select
	 * @param tabNumber the tab to display
	 */
	public void setSelectedNode(LaunchNode node, int tabNumber) {
		setSelectedNode(node, tabNumber, null);
	}

	/**
	 * Sets the selected node and displays a URL. Detects if the url is a reference
	 * change only, as from a hyperlink on a page and, if so, just scrolls to that
	 * position on the page. Otherwise rebuilds the full tab system.
	 *
	 * @param node      the node to select
	 * @param tabNumber the tab to display
	 * @param url       the URL to display in the tab
	 */
	public void setSelectedNode(LaunchNode node, int tabNumber, java.net.URL url) {
		node.tabNumber = (url == null && node.getDisplayTabCount() == 0 ? -1 : tabNumber);
		LaunchNode.DisplayTab htmlData = null;
		URL prevURL = null;
		LaunchNode node0 = getSelectedNode();
		if (node.tabNumber >= 0) {
			htmlData = node.tabData.get(node.tabNumber);
		}
		if (htmlData != null) {
			prevURL = htmlData.url;
		}
		if (url != null) {
			// set url to include anchor, if any
			node.setURL(url);
			if (htmlData != null) {
				htmlData.url = url;
			}
		}
		String scrollRef = (node0 == node && url != null && prevURL != null && url.getPath().equals(prevURL.getPath())
				? "#"
				: null);
		if (scrollRef == null) {
			setTreeSelectionPath(node);
		} else if (url != null) {
			setTreeSelectionPath(node); // added by DB Dec 2021
			// DB code below leaves html.editorPane blank!
//			scrollRef = url.getRef();
//			// a null scrollRef means TOP -- scroll to [0,0], not to #xxx
//			Launcher.HTMLPane html = launcher.getHTMLTab(node.tabNumber);
//			if (scrollRef == null)
//				html.editorPane.scrollRectToVisible(new Rectangle());
//			else
//				html.editorPane.scrollToReference(scrollRef);
		}
		// restore previous URL
		if (htmlData != null) {
			htmlData.url = prevURL;
		}
	}

	/**
	 * Gets the selected node.
	 *
	 * @return the selected node
	 */
	public LaunchNode getSelectedNode() {
		TreePath path = tree.getSelectionPath();
		if (path == null) {
			return null;
		}
		return (LaunchNode) path.getLastPathComponent();
	}

	/**
	 * Gets the selected nodes.
	 *
	 * @return the selected nodes
	 */
	public ArrayList<LaunchNode> getSelectedNodes() {
		TreePath[] paths = tree.getSelectionPaths();
		if (paths == null) {
			return null;
		}
		// assemble list of selected nodes
		ArrayList<LaunchNode> temp = new ArrayList<LaunchNode>();
		for (int i = 0; i < paths.length; i++) {
			temp.add((LaunchNode) paths[i].getLastPathComponent());
		}
		// use preorder enumeration to put list in top-bottom order
		ArrayList<LaunchNode> nodes = new ArrayList<LaunchNode>();
		Enumeration<?> e = getRootNode().preorderEnumeration();
		while (e.hasMoreElements()) {
			LaunchNode next = (LaunchNode) e.nextElement();
			if (temp.contains(next))
				nodes.add(next);
		}
		return nodes;
	}

	/**
	 * Gets the selected display tab.
	 *
	 * @return the selected display tab
	 */
	public int getSelectedDisplayTab() {
		return tabbedPane.getSelectedIndex();
	}

	/**
	 * Gets the root node.
	 *
	 * @return the root node
	 */
	public LaunchNode getRootNode() {
		return (LaunchNode) treeModel.getRoot();
	}

	/**
	 * Gets the HTML substitution map. This maps target to replacement Strings to be
	 * substituted in HTML documents.
	 *
	 * @return the HTML substitution map
	 */
	public Map<String, String> getHTMLSubstitutionMap() {
		return htmlSubstitutions;
	}

	// ______________________________ protected methods
	// _____________________________

	/**
	 * Returns the node with the same file name as the specified node. May return
	 * null.
	 *
	 * @param node the node to match
	 * @return the first node with the same file name
	 */
	protected LaunchNode getClone(LaunchNode node) {
		if (node.getFileName() == null) {
			return null;
		}
		Enumeration<?> e = getRootNode().breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			LaunchNode next = (LaunchNode) e.nextElement();
			if (node.getFileName().equals(next.getFileName())) {
				return next;
			}
		}
		return null;
	}

	/**
	 * Rebuilds the tabs from scratch in order to displays all tabs for the
	 * specified node.
	 *
	 * @param node the LaunchNode
	 */
	protected void rebuildAndDisplayTabs(LaunchNode node) {
		if (node == null)
			return;
		OSPLog.finer(LaunchRes.getString("Log.Message.NodeSelected") //$NON-NLS-1$
				+ " " + node); //$NON-NLS-1$
		boolean isBuilder = launcher instanceof LaunchBuilder;
		URL url = (node.isDisplayable ? node.getURL() : null); // what URL to display - was checking only for PDF
		// don't display PDF files in Launcher
		int tabNumber = node.tabNumber; // which tab to display it in
		boolean hasModel = false;
		// find the first displayable tab if the url is null
		if (url == null && node.getDisplayTabCount() > 0) {
			int k = 0;
			for (int i = tabNumber; i < node.getDisplayTabCount(); i++, k++) {
				DisplayTab tab = node.getDisplayTab(i);
				// next!=null condition added by W. Christian
				// skip display tabs with PDFs
				if (tab == null || tab.isDisplayable) // - was checking only for PDF
					break;
			}
			// node has multiple URLs so pick the tab-associated one
			tabNumber = Math.max(0, tabNumber);
			DisplayTab displayTab = node.getDisplayTab(tabNumber + k);
			if (displayTab == null) { // null check added by W.Chrisitan
				// do nothing
			} else if (displayTab.url == null) {
				hasModel = displayTab.getModelClass() != null;
			} else if (displayTab.isDisplayable) { // - was checking only for PDF
				// display only non-PDF files in Launcher
				url = displayTab.url;
			}
		}
		// rebuild tabs
		rebuildingTabs = true;
		// BH 2021.03.23 prevents repaint of previous tab contents
		//tabbedPane.setVisible(false);
		int tabCount = 0;
		if (!isBuilder) {
			tabbedPane.removeAll();
		}
		noTitle = LaunchRes.getString("HTMLTab.Title.Untitled"); //$NON-NLS-1$
		Iterator<?> it = node.tabData.iterator();
		while (it.hasNext()) {
			LaunchNode.DisplayTab displayTab = (LaunchNode.DisplayTab) it.next();
			if (displayTab.isDisplayable) { // - was checking only for PDF
				if (displayTab.urlExists()) {
					Launcher.HTMLPane html = launcher.getHTMLTab(tabCount);
					URL theURL = (tabNumber == tabCount && url != null ? url : displayTab.url);
					SwingUtilities.invokeLater(() -> {
						launchHtml(html, theURL, displayTab.hyperlinksEnabled && node.enabled);
					});
					if (!isBuilder) {
						addTab(displayTab, html.scroller);
						tabCount++;
					}
				} else {
					OSPLog.fine(LaunchRes.getString("Log.Message.BadURL") //$NON-NLS-1$
							+ " " + displayTab.url); //$NON-NLS-1$
				}
			} else if (displayTab.getModelScroller() != null) {
				if (!isBuilder) {
					addTab(displayTab, displayTab.getModelScroller());
					tabCount++;
				}
			}
		}
		// display appropriate tabs
		if (!isBuilder) {
			if (url != null || hasModel) {
				if (tabbedPane.getTabCount() == 1 && tabbedPane.getTitleAt(0).equals(noTitle)) {
					splitPane.setRightComponent(tabbedPane.getComponentAt(0));
				} else if (tabbedPane.getTabCount() > 0) {
					splitPane.setRightComponent(tabbedPane);
					if (tabbedPane.getTabCount() > tabNumber) {
						tabbedPane.setSelectedIndex(tabNumber);
					}
				}
			} else {
				JScrollPane launchPane = node.getLaunchModelScroller();
				if (launchPane != null) {
					splitPane.setRightComponent(launchPane);
				} else {
					descriptionPane.setText(node.description);
					splitPane.setRightComponent(descriptionScroller);
				}
			}
		}
		// BH 2021.03.23 prevents repaint of previous tab contents
		tabbedPane.setVisible(tabbedPane.getTabCount() > 1);
		rebuildingTabs = false;
		launcher.refreshGUI();
	}

	private void addTab(DisplayTab displayTab, JScrollPane scroller) {
		String title = (displayTab.getTitle() == null) ? noTitle : displayTab.getTitle();
		tabbedPane.addTab(title, scroller);
	}

	protected void launchHtml(HTMLPane html, URL theURL, boolean nodeEnabled) {
		try {
			if (htmlSubstitutions.isEmpty()) {
				// the traditional method
				html.editorPane.setPage(theURL);
			} else {
				// read url into a string
				Scanner scanner = new Scanner(ResourceLoader.openStream(theURL), "UTF-8"); //$NON-NLS-1$
				String text = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
				scanner.close();
				// make html substitutions
				for (String target : htmlSubstitutions.keySet()) {
					String newValue = htmlSubstitutions.get(target);
					text = text.replaceAll(target, newValue);
				}
				// set document base for relative paths
				HTMLDocument doc = (HTMLDocument) html.editorPane.getDocument();
				doc.setBase(theURL);
				// ignore charset to prevent charset exception when fontLevel > 0
				doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
				// set editorPane text
				html.editorPane.setText(text);
				// scroll to top
				html.editorPane.setCaretPosition(0);
			}
		} catch (IOException e) {
		}

		if (theURL.getRef() != null) {
			html.editorPane.scrollToReference(theURL.getRef());
			if (FontSizer.getLevel() > 0) {
				// invoke scrollToReference again later at high font levels
				OSPRuntime.trigger(100, (e) -> {
						html.editorPane.scrollToReference(theURL.getRef());
				});
			}
		}
		launcher.setLinksEnabled(html.editorPane, nodeEnabled);
	}

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		setPreferredSize(new Dimension(400, 200));
		setLayout(new BorderLayout());
		splitPane = new JSplitPane();
		add(splitPane, BorderLayout.CENTER);
		dataPanel = new JPanel(new BorderLayout());
		descriptionPane = GUIUtils.newJTextPane();
		descriptionPane.setEditable(false);
		descriptionPane.setContentType(LaunchPanel.TEXT_TYPE);
		descriptionScroller = new JScrollPane(descriptionPane);
		// create the tabbed pane
		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				LaunchNode node = getSelectedNode();
				if (node != null && node == launcher.selectedNode) {
					URL htmlURL = node.getURL();
					if (rebuildingTabs && htmlURL != null)
						return;
					// save prev html properties
					node.prevTabNumber = node.tabNumber;
					node.prevURL = htmlURL;
					// set new html properties
					int n = Math.max(0, tabbedPane.getSelectedIndex());
					node.tabNumber = node.tabData.isEmpty() ? -1 : n;
					if (node.tabNumber > -1) {
						DisplayTab displayTab = node.getDisplayTab(node.tabNumber);
						if (displayTab.url != null) {
							node.setURL(displayTab.url);
						} else {
							Launcher.HTMLPane tab = launcher.getHTMLTab(node.tabNumber);
							node.setURL(tab.editorPane.getPage());
						}
//            OSPLog.info("set node "+node+" to "+node.htmlURL //$NON-NLS-1$
//                +"and tabnumber "+node.tabNumber);  //$NON-NLS-1$
					}
				}
			}

		});
		tabbedPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				LaunchNode node = getSelectedNode();
				if ((node != null) && launcher.postEdits) {
					String nodePath = node.getPathString();
					Integer undoPage = Integer.valueOf(node.prevTabNumber);
					Integer redoPage = Integer.valueOf(node.tabNumber);
					Object[] undoData = new Object[] { null, nodePath, undoPage, node.prevURL };
					Object[] redoData = new Object[] { null, nodePath, redoPage, node.getURL() };
					LauncherUndo.NavEdit edit = launcher.undoManager.new NavEdit(undoData, redoData);
					launcher.undoSupport.postEdit(edit);
				}
			}

		});
		splitPane.setRightComponent(dataPanel);
		splitPane.setDividerLocation(160);
	}

	/**
	 * Creates the tree.
	 *
	 * @param rootNode the root node
	 */
	protected void createTree(LaunchNode rootNode) {
		// if not showing all nodes, create the VisibleNode structure
		if (!showAllNodes) {
			VisibleNode visibleRoot = new VisibleNode(rootNode);
			visibleNodeMap.put(rootNode, visibleRoot);
			addVisibleNodes(visibleRoot);
		}
		treeModel = new LaunchTreeModel(rootNode);
		tree = new JTree(treeModel);
		tree.setToolTipText(""); // enables tool tips for nodes //$NON-NLS-1$
		tree.setRootVisible(!rootNode.hiddenWhenRoot);
		if (launcher instanceof LaunchBuilder)
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		else
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				LaunchNode node = launcher.getSelectedNode();
				if (launcher.postEdits) {
					// post undoable NavEdit if prev treePath is not null
					TreePath treePath = e.getOldLeadSelectionPath();
					if ((treePath != null) && (node != null)) {
						LaunchNode prevNode = (LaunchNode) treePath.getLastPathComponent();
						// set html properties of newly selected node
						if (!node.tabData.isEmpty()) {
							int page = Math.max(0, node.tabNumber);
							LaunchNode.DisplayTab htmlData = node.tabData.get(page);
							node.setURL(htmlData.url);
							node.tabNumber = page;
						}
						LauncherUndo.NavEdit edit = launcher.undoManager.new NavEdit(prevNode, node);
						launcher.undoSupport.postEdit(edit);
					}
				}
				rebuildAndDisplayTabs(node);
			}

		});
		// put tree in a scroller and add to the split pane
		JScrollPane treeScroller = new JScrollPane(tree);
		splitPane.setLeftComponent(treeScroller);
	}

	/**
	 * Returns a collection of nodes that are currently expanded.
	 *
	 * @return the expanded nodes
	 */
	protected Collection<String> getExpandedNodes() {
		Collection<String> list = new ArrayList<String>();
		TreePath path = new TreePath(getRootNode());
		Enumeration<?> en = tree.getExpandedDescendants(path);
		while ((en != null) && en.hasMoreElements()) {
			TreePath next = (TreePath) en.nextElement();
			LaunchNode node = (LaunchNode) next.getLastPathComponent();
			list.add(node.getPathString());
		}
		return list;
	}

	/**
	 * Expands the specified nodes.
	 *
	 * @param expanded the nodes to expand
	 */
	protected void setExpandedNodes(Collection<?> expanded) {
		Iterator<?> it = expanded.iterator();
		while (it.hasNext()) {
			// get node path
			String path = it.next().toString();
			Enumeration<?> e = getRootNode().breadthFirstEnumeration();
			while (e.hasMoreElements()) {
				LaunchNode node = (LaunchNode) e.nextElement();
				if (path.equals(node.getPathString())) { // found node
					// get treePath
					TreePath treePath = new TreePath(node.getPath());
					tree.expandPath(treePath);
				}
			}
		}
	}

	/**
	 * A tree model class for launch nodes.
	 */
	class LaunchTreeModel extends DefaultTreeModel {
		LaunchTreeModel(LaunchNode root) {
			super(root);
		}

		@Override
		public Object getChild(Object parent, int index) {
			if (showAllNodes) {
				return super.getChild(parent, index);
			}
			VisibleNode visibleParent = visibleNodeMap.get(parent);
			if (visibleParent != null) {
				VisibleNode visibleChild = (VisibleNode) visibleParent.getChildAt(index);
				if (visibleChild != null) {
					return visibleChild.node;
				}
			}
			return null;
		}

		@Override
		public int getChildCount(Object parent) {
			if (showAllNodes) {
				return super.getChildCount(parent);
			}
			VisibleNode visibleParent = visibleNodeMap.get(parent);
			if (visibleParent != null) {
				return visibleParent.getChildCount();
			}
			return 0;
		}

		@Override
		public int getIndexOfChild(Object parent, Object child) {
			if (showAllNodes) {
				return super.getIndexOfChild(parent, child);
			}
			VisibleNode visibleParent = visibleNodeMap.get(parent);
			VisibleNode visibleChild = visibleNodeMap.get(child);
			if ((visibleParent != null) && (visibleChild != null)) {
				return visibleParent.getIndex(visibleChild);
			}
			return -1;
		}

	}

	/**
	 * A tree node that references a launch node.
	 */
	private class VisibleNode extends DefaultMutableTreeNode {
		LaunchNode node;

		VisibleNode(LaunchNode node) {
			this.node = node;
		}

	}

	private void addVisibleNodes(VisibleNode visibleParent) {
		int n = visibleParent.node.getChildCount();
		for (int i = 0; i < n; i++) {
			LaunchNode child = (LaunchNode) visibleParent.node.getChildAt(i);
			if (child.isHiddenInLauncher()) {
				continue;
			}
			VisibleNode visibleChild = new VisibleNode(child);
			visibleNodeMap.put(child, visibleChild);
			visibleParent.add(visibleChild);
			addVisibleNodes(visibleChild);
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
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
