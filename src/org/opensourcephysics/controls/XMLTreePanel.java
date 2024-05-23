/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.ArrayInspector;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This is a split pane view of an XML tree and its contents.
 *
 * @author Douglas Brown
 */
public class XMLTreePanel extends JPanel {
  // instance fields
  protected JLabel label;
  protected JTextField input;
  protected JTextPane xmlPane;
  protected JTree tree;
  protected JScrollPane treeScroller = new JScrollPane();
  protected Icon valueIcon;
  protected Icon inspectIcon;
  protected Icon inspectFolderIcon;
  protected Icon folderIcon;
  protected XMLControl control;
  protected XMLProperty property;
  protected boolean editable;
  protected JPopupMenu popup;
  int maxStringLength = 24;

  /**
   * Constructs a tree panel with an XMLControl
   *
   * @param control the XMLControl
   */
  public XMLTreePanel(XMLControl control) {
    this(control, true);
  }

  /**
   * Constructs a tree panel with an XMLControl
   *
   * @param control the XMLControl
   * @param editable true to enable xml edits via the input field
   */
  public XMLTreePanel(XMLControl control, boolean editable) {
    super(new BorderLayout());
    this.control = control;
    this.editable = editable;
    createGUI();
  }

  /**
   * Refreshes the tree. Called after changing the control externally.
   */
  public void refresh() {
    XMLTreeNode root = createTree(control);
    displayProperty(root, editable);
  }

  /**
   * Gets the control displayed in the tree.
   */
  public XMLControl getControl() {
    return control;
  }

  /**
   * Selects and returns the first node with the specified property name.
   *
   * @param propertyName the property name
   * @return the selected node, or null if none found
   */
  public XMLTreeNode setSelectedNode(String propertyName) {
    XMLTreeNode root = (XMLTreeNode) tree.getModel().getRoot();
    Enumeration<?> e = root.breadthFirstEnumeration();
    while(e.hasMoreElements()) {
      XMLTreeNode node = (XMLTreeNode) e.nextElement();
      XMLProperty prop = node.getProperty();
      if(prop.getPropertyName().equals(propertyName)) {
        TreePath path = new TreePath(node.getPath());
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        showInspector(node);
        return node;
      }
    }
    return null;
  }

  /**
   * Selects and returns the node with the specified TreePath.
   * This uses only the names of the nodes in the path so
   * it can be used after creating a new similar Tree.
   *
   * @param treePath the TreePath
   * @return the selected node, or null if none found
   */
  public XMLTreeNode setSelectedNode(TreePath treePath) {
  	Object[] elements = treePath.getPath();
    XMLTreeNode root = (XMLTreeNode) tree.getModel().getRoot();
    XMLTreeNode node = null;
    for (int i = 0; i < elements.length; i++) {
    	node = getChildNode(root, elements[i].toString());
    	if (node == null)
    		break;
    	root = node;
    }
    if (node != null) {
      TreePath path = new TreePath(node.getPath());
      tree.setSelectionPath(path);
      tree.scrollPathToVisible(path);
      showInspector(node);
      return node;
    }
    return null;
  }

  /**
   * Returns the first child of a specified root node
   * that has a given name.
   *
   * @param root the node to search
   * @param name the name
   * @return the node, or null if none found
   */
  public XMLTreeNode getChildNode(XMLTreeNode root, String name) {
    Enumeration<?> e = root.breadthFirstEnumeration();
    while(e.hasMoreElements()) {
      XMLTreeNode node = (XMLTreeNode) e.nextElement();
      if (node.toString().equals(name)) {
        return node;
      }
    }
    return null;
  }

  /**
   * Displays the property data for the specified node.
   *
   * @param node the XMLTreeNode
   * @param editable <code>true</code> if the input field is editable
   */
  protected void displayProperty(XMLTreeNode node, boolean editable) {
    // input field hidden by default
    input.setVisible(false);
    XMLProperty prop = node.getProperty();
    // display property type and name on label
    label.setText(prop.getPropertyType()+" "+prop.getPropertyName()); //$NON-NLS-1$
    if(!prop.getPropertyContent().isEmpty()) {
      // get first content item
      Object value = prop.getPropertyContent().get(0);
      // display primitive properties in input field
      if(value instanceof String) {
        property = prop;
        String content = XML.removeCDATA((String) value);
        input.setText(content);
        input.setEditable(editable);
        input.setVisible(true);
      }
    }
    // display xml in xmlPane
    String xml = prop.toString();
    xmlPane.setText(getDisplay(xml));
    xmlPane.setCaretPosition(0);
  }

  /**
   * Gets the xml to be displayed.
   *
   * @param xml the raw xml
   * @return the displayed xml
   */
  protected String getDisplay(String xml) {
    // find and truncate every array string in the xml
    String newXML = "";                                  // newly assembled xml //$NON-NLS-1$
    String preArray = "name=\"array\" type=\"string\">"; //$NON-NLS-1$
    String postArray = "</property>";                    //$NON-NLS-1$
    String array;
    int i = xml.indexOf(preArray);
    while(i>0) {
      i += preArray.length();
      newXML += xml.substring(0, i);
      xml = xml.substring(i);
      i = xml.indexOf(postArray);
      array = xml.substring(0, i);
      xml = xml.substring(i, xml.length());
      if(array.length()>maxStringLength) {
        array = array.substring(0, maxStringLength-3)+"..."; //$NON-NLS-1$
      }
      newXML += array;
      i = xml.indexOf(preArray);
    }
    newXML += xml;
    return newXML;
  }

  /**
   * Creates the GUI and listeners.
   */
  protected void createGUI() {
    // create popup and icons
    String imageFile = "/org/opensourcephysics/resources/controls/images/inspect.gif"; //$NON-NLS-1$
    // Don't use resource loader to improve performance.  Changed by W. Christian
    //inspectIcon = ResourceLoader.getIcon(imageFile);
    inspectIcon = ResourceLoader.getImageIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/controls/images/value.gif";         //$NON-NLS-1$
    //valueIcon = ResourceLoader.getIcon(imageFile);
    valueIcon = ResourceLoader.getImageIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/controls/images/folder.gif";        //$NON-NLS-1$
    //folderIcon = ResourceLoader.getIcon(imageFile);
    folderIcon = ResourceLoader.getImageIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/controls/images/inspectfolder.gif"; //$NON-NLS-1$
    //inspectFolderIcon = ResourceLoader.getIcon(imageFile);
    inspectFolderIcon = ResourceLoader.getImageIcon(imageFile);
    popup = new JPopupMenu();
    JMenuItem item = new JMenuItem(ControlsRes.getString("XMLTreePanel.Popup.MenuItem.Inspect")); //$NON-NLS-1$
    popup.add(item);
    item.addActionListener(new ActionListener() {
      @Override
	public void actionPerformed(ActionEvent e) {
        XMLTreeNode node = (XMLTreeNode) tree.getLastSelectedPathComponent();
        if(node!=null) {
          showInspector(node);
        }
      }

    });
    // create tree
    XMLTreeNode root = createTree(control);
    // create toolbar for label and input text field
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    // create label
    label = new JLabel();
    toolbar.add(label);
    // create input text field
    input = new JTextField(20);
    input.setVisible(false);
    input.addActionListener(new ActionListener() {
      @Override
	public void actionPerformed(ActionEvent e) {
        property.setValue(input.getText());
        Object obj = control.loadObject(null);
        if(obj instanceof Component) {
          ((Component) obj).repaint();
        }
        input.setText((String) property.getPropertyContent().get(0));
        input.selectAll();
        XMLTreeNode node = (XMLTreeNode) tree.getLastSelectedPathComponent();
        if(node!=null) {
          displayProperty(node, editable);
        }
      }

    });
    input.addKeyListener(new KeyAdapter() {
      @Override
	public void keyPressed(KeyEvent e) {
        if(!editable) {
          return;
        }
        JComponent comp = (JComponent) e.getSource();
        if(e.getKeyCode()==KeyEvent.VK_ENTER) {
          comp.setBackground(Color.white);
        } else {
          comp.setBackground(Color.yellow);
        }
      }

    });
    input.addFocusListener(new FocusAdapter() {
      @Override
	public void focusLost(FocusEvent e) {
        JComponent comp = (JComponent) e.getSource();
        comp.setBackground(Color.white);
      }

    });
    toolbar.add(input);
    // create xml pane and scroller
    xmlPane = GUIUtils.newJTextPane();
    xmlPane.setPreferredSize(new Dimension(360, 200));
    xmlPane.setEditable(false);
    JScrollPane xmlScroller = new JScrollPane(xmlPane);
    // create data panel for right side of split pane
    JPanel dataPanel = new JPanel(new BorderLayout());
    dataPanel.add(toolbar, BorderLayout.NORTH);
    dataPanel.add(xmlScroller, BorderLayout.CENTER);
    // create split pane
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroller, dataPanel);
    add(splitPane, BorderLayout.CENTER);
    treeScroller.setPreferredSize(new Dimension(140, 200));
    // with the default splitPane.setResizeWeight(0), any overage
    // results in a closed pane. HTML5 may have ever so slightly different character widths.
    splitPane.setDividerLocation(140); // BH 2020.03.25 setting this guarantees the preferred size initially
    displayProperty(root, editable);
  }

  private XMLTreeNode createTree(XMLControl control) {
    XMLTreeNode root = new XMLTreeNode(control);
    tree = new JTree(root);
    tree.setCellRenderer(new XMLRenderer());
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    // listen for tree selections and display the property data
    tree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
	public void valueChanged(TreeSelectionEvent e) {
        XMLTreeNode node = (XMLTreeNode) tree.getLastSelectedPathComponent();
        if(node!=null) {
          displayProperty(node, editable);
        }
      }

    });
    // listen for mouse events to display array tables
    tree.addMouseListener(getMouseListener());
    // put tree in scroller
    treeScroller.setViewportView(tree);
    return root;
  }
  
  protected MouseListener getMouseListener() {
  	return new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(OSPRuntime.isPopupTrigger(e)) {
          // select node and show popup menu
          TreePath path = tree.getPathForLocation(e.getX(), e.getY());
          if(path==null) {
            return;
          }
          tree.setSelectionPath(path);
          XMLTreeNode node = (XMLTreeNode) tree.getLastSelectedPathComponent();
          if(node.isInspectable()) {
            popup.show(tree, e.getX(), e.getY()+8);
          }
        }
      }
    };
  }

  private void showInspector(XMLTreeNode node) {
    if(node==null) {
      return;
    }
    // show array inspector if available
    if(node.getProperty().getPropertyType() == XMLProperty.TYPE_ARRAY) { //$NON-NLS-1$
      XMLProperty arrayProp = node.getProperty();
      ArrayInspector inspector = ArrayInspector.getInspector(arrayProp);
      if(inspector!=null) {
        String name = arrayProp.getPropertyName();
        XMLProperty parent = arrayProp.getParentProperty();
        while(!(parent instanceof XMLControl)) {
          name = parent.getPropertyName();
          arrayProp = parent;
          parent = parent.getParentProperty();
        }
        final XMLControl arrayControl = (XMLControl) parent;
        final String arrayName = name;
        final Object arrayObj = inspector.getArray();
        final XMLTreeNode parentNode = (XMLTreeNode) node.getParent();
        inspector.setEditable(editable);
        // listen for changes in the table array
        inspector.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
          @Override
		public void propertyChange(PropertyChangeEvent e) {
            if(e.getPropertyName().equals("cell")) {           //$NON-NLS-1$
              // set new array value in array control (creates new XMLProperty)
              arrayControl.setValue(arrayName, arrayObj);
              control.loadObject(null);
              // find the new XMLProperty and make a new tree node for it
              Iterator<?> it = arrayControl.getPropertyContent().iterator();
              while(it.hasNext()) {
                XMLProperty next = (XMLProperty) it.next();
                if(next.getPropertyName().equals(arrayName)) {
                  // replace current tree node with new one
                  for(int i = 0; i<parentNode.getChildCount(); i++) {
                    XMLTreeNode node = (XMLTreeNode) parentNode.getChildAt(i);
                    if(node.getProperty().getPropertyName().equals(arrayName)) {
                      XMLTreeNode child = new XMLTreeNode(next);
                      TreeModel model = tree.getModel();
                      if(model instanceof DefaultTreeModel) {
                        DefaultTreeModel treeModel = (DefaultTreeModel) model;
                        treeModel.removeNodeFromParent(node);
                        treeModel.insertNodeInto(child, parentNode, i);
                        TreePath path = new TreePath(child.getPath());
                        tree.setSelectionPath(path);
                      }
                      break;
                    }
                  }
                  break;
                }
              }
            }
          }

        });
        // offset new inspector relative to parent container
        Container cont = getTopLevelAncestor();
        Point p = cont.getLocationOnScreen();
        inspector.setLocation(p.x+30, p.y+30);
        inspector.setVisible(true);
      }
    }
  }

  /**
   * A cell renderer to show xml nodes.
   */
  private class XMLRenderer extends DefaultTreeCellRenderer {
    @Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      XMLTreeNode node = (XMLTreeNode) value;
      if(node.isLeaf()) {
        if(node.isInspectable()) {
          setIcon(inspectIcon);
        } else {
          setIcon(valueIcon);
        }
      } else if(node.isInspectable()) {
        setIcon(inspectFolderIcon);
      } else {
        setIcon(folderIcon);
      }
      return this;
    }

  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2024  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
