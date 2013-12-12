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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.FontRenderContext;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.controls.XMLProperty;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.TeXParser;

/**
 * A JPanel that manages a table of objects with editable names and expressions.
 *
 * @author Douglas Brown
 */
public class FunctionEditor extends JPanel implements PropertyChangeListener {
  // static constants
	@SuppressWarnings("javadoc")
	public final static String THETA = TeXParser.parseTeX("$\\theta$"); //$NON-NLS-1$
	public final static String OMEGA = TeXParser.parseTeX("$\\omega$"); //$NON-NLS-1$
	public final static String DEGREES = "º"; //$NON-NLS-1$
  public final static int ADD_EDIT = 0;
  public final static int REMOVE_EDIT = 1;
  public final static int NAME_EDIT = 2;
  public final static int EXPRESSION_EDIT = 3;  
  final static Color LIGHT_BLUE = new Color(204, 204, 255);
  final static Color MEDIUM_RED = new Color(255, 160, 180);
  final static Color LIGHT_RED = new Color(255, 180, 200);
  final static Color LIGHT_GRAY = javax.swing.UIManager.getColor("Panel.background");   //$NON-NLS-1$
  final static Color DARK_RED = new Color(220, 0, 0);
  
  // static fields
  static NumberFormat decimalFormat;
  static DecimalFormat sciFormat;
  protected static String[] editTypes = {"add row", //$NON-NLS-1$
    	"delete row", "edit name", "edit expression"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  static FontRenderContext frc
		  = new FontRenderContext(null,   // no AffineTransform
		                          false,  // no antialiasing
		                          false); // no fractional metrics
  
  // instance fields
  protected ParamEditor paramEditor;
  protected ArrayList<Object> objects = new ArrayList<Object>();
  protected String[] names = new String[0];
  protected ArrayList<Object> sortedObjects = new ArrayList<Object>();
  protected HashSet<String> forbiddenNames = new HashSet<String>();
  protected boolean removablesAtTop = false;
  protected Collection<Object> circularErrors = new HashSet<Object>();
  protected Collection<Object> errors = new HashSet<Object>();
  protected List<Object> evaluate = new ArrayList<Object>();
  protected Table table;
  protected TableModel tableModel = new TableModel();
  protected CellEditor tableCellEditor = new CellEditor();
  protected CellRenderer tableCellRenderer = new CellRenderer();
  protected JScrollPane tableScroller;
  protected JButton newButton;
  protected JButton cutButton;
  protected JButton copyButton;
  protected JButton pasteButton;
  protected JPanel buttonPanel;
  protected FunctionPanel functionPanel;
  protected AbstractButton[] customButtons;
  protected boolean anglesInDegrees;
  protected boolean usePopupEditor = true;

  static {
    decimalFormat = NumberFormat.getInstance();
    decimalFormat.setMaximumFractionDigits(6);
    decimalFormat.setMinimumFractionDigits(1);
    decimalFormat.setMaximumIntegerDigits(3);
    decimalFormat.setMinimumIntegerDigits(1);
    sciFormat = new DecimalFormat("0.000000E0"); //$NON-NLS-1$
  }

  /**
   * No-arg constructor
   */
  public FunctionEditor() {
    super(new BorderLayout());
    createGUI();
    refreshGUI();
  }

  /**
   * Gets the table.
   *
   * @return the table
   */
  public Table getTable() {
    return table;
  }

  /**
   * Override getPreferredSize().
   *
   * @return the table size plus button and instruction heights
   */
  public Dimension getPreferredSize() {
    Dimension dim = table.getPreferredSize();
    dim.height += table.getTableHeader().getHeight();
    dim.height += buttonPanel.getPreferredSize().height;
    dim.height += 1.25*table.getRowHeight()+14;
    return dim;
  }

  /**
   * Replaces the current objects with new ones.
   *
   * @param newObjects a list of objects
   */
  public void setObjects(java.util.List<Object> newObjects) {
    // determine row and column selected
    int row = table.getSelectedRow();
    int col = table.getSelectedColumn();
    objects.clear();
    objects.addAll(newObjects);
    evaluateAll();
    tableModel.fireTableStructureChanged();
    // select same cell
    if(row<table.getRowCount()) {
      table.rowToSelect = row;
      table.columnToSelect = col;
    }
    table.requestFocusInWindow();
    refreshGUI();
  }

  /**
   * Gets a shallow clone of the objects list.
   *
   * @return a list of objects
   */
  public List<Object> getObjects() {
    return new ArrayList<Object>(objects);
  }

  /**
   * Gets an array containing the names of the objects.
   *
   * @return an array of names
   */
  public String[] getNames() {
    return names;
  }

  /**
   * Returns the name of the object.
   *
   * @param obj the object
   * @return the name
   */
  public String getName(Object obj) {
    return null;
  }

  /**
   * Returns the expression of the object.
   *
   * @param obj the object
   * @return the expression
   */
  public String getExpression(Object obj) {
    return null;
  }

  /**
   * Returns a tooltip for the object.
   *
   * @param obj the object
   * @return the tooltip
   */
  public String getTooltip(Object obj) {
    return null;
  }

  /**
   * Gets an existing object with specified name. May return null.
   *
   * @param name the name
   * @return the object
   */
  public Object getObject(String name) {
    if((name==null)||name.equals("")) { //$NON-NLS-1$   
      return null;
    }
    Iterator<Object> it = objects.iterator();
    while(it.hasNext()) {
      Object next = it.next();
      if(name.equals(getName(next))) {
        return next;
      }
    }
    return null;
  }

  /**
   * Sets the expression of an existing named object, if any.
   *
   * @param name the name
   * @param expression the expression
   * @param postEdit true to post an undoable edit
   */
  public void setExpression(String name, String expression, boolean postEdit) {
    if((name==null)||name.equals("")) { //$NON-NLS-1$   
      return;
    }
    for(int row = 0; row<objects.size(); row++) {
      Object obj = objects.get(row);
      if(name.equals(getName(obj))&&!getExpression(obj).equals(expression)) {
        String prev = getExpression(obj);
        obj = createObject(name, expression, obj);
        objects.remove(row);
        objects.add(row, obj);
        evaluateAll();
        tableModel.fireTableStructureChanged();
        // select row
        if(row>=0) {
          table.changeSelection(row, 1, false, false);
        }
        // inform and pass undoable edit to listeners
        UndoableEdit edit = null;
        if(postEdit) {
          edit = getUndoableEdit(EXPRESSION_EDIT, expression, row, 1, prev, row, 1, getName(obj));
        }
        firePropertyChange("edit", getName(obj), edit); //$NON-NLS-1$
      }
    }
  }

  /**
   * Adds an object.
   *
   * @param obj the object
   * @param postEdit true to post an undoable edit
   * @return the added object
   */
  public Object addObject(Object obj, boolean postEdit) {
    if(obj==null) {
      return null;
    }
    // determine row number
    int row = objects.size(); // end of table
    if(isRemovable(obj)) {
      if(removablesAtTop) {
        row = getRemovableRowCount();   // after removable rows
      }
    } else if(!removablesAtTop) {
      row = row-getRemovableRowCount(); // before removable rows
    }
    return addObject(obj, row, postEdit, true);
  }

  /**
   * Adds an object at a specified row.
   *
   * @param obj the object
   * @param row the row
   * @param postEdit true to post an undoable edit
   * @param firePropertyChange true to fire a property change event
   * @return the added object
   */
  public Object addObject(Object obj, int row, boolean postEdit, boolean firePropertyChange) {
    obj = createUniqueObject(obj, getName(obj), true);
    if(obj==null) {
      return null;
    }
    int undoRow = table.getSelectedRow();
    int undoCol = table.getSelectedColumn();
    java.util.List<Object> newObjects = new ArrayList<Object>(objects);
    newObjects.add(row, obj);
    setObjects(newObjects);
    // select new object
    table.columnToSelect = 0;
    table.rowToSelect = row;
    table.selectOnFocus = true;
    table.requestFocusInWindow();
    // inform and pass undoable edit to listeners
    UndoableEdit edit = null;
    if(postEdit) {
      edit = getUndoableEdit(ADD_EDIT, obj, row, 0, obj, undoRow, undoCol, getName(obj));
    }
    if(firePropertyChange) {
      firePropertyChange("edit", getName(obj), edit); //$NON-NLS-1$
    }
    refreshGUI();
    return obj;
  }

  /**
   * Removes an object.
   *
   * @param obj the object to remove
   * @param postEdit true to post an undoable edit
   * @return the removed object
   */
  public Object removeObject(Object obj, boolean postEdit) {
    if((obj==null)||!isRemovable(obj)) {
      return null;
    }
    int undoCol = table.getSelectedColumn();
    for(int undoRow = 0; undoRow<objects.size(); undoRow++) {
      Object next = objects.get(undoRow);
      if(next.equals(obj)) {
        objects.remove(obj);
        tableModel.fireTableStructureChanged();
        // select new row
        int row = (undoRow==objects.size()) ? undoRow-1 : undoRow;
        if(row>=0) {
          table.changeSelection(row, 0, false, false);
        }
        // inform and pass undoable edit to listeners
        UndoableEdit edit = null;
        if(postEdit) {
          edit = getUndoableEdit(REMOVE_EDIT, obj, row, 0, obj, undoRow, undoCol, getName(obj));
        }
        evaluateAll();
        firePropertyChange("edit", getName(obj), edit); //$NON-NLS-1$
        refreshGUI();
      }
    }
    return obj;
  }

  /**
   * Refreshes button strings based on current locale.
   */
  public void refreshStrings() {
    refreshGUI();
  }

  /**
   * Responds to property change events.
   *
   * @param e the event
   */
  public void propertyChange(PropertyChangeEvent e) {
    String name = e.getPropertyName();
    if(name.equals("focus")||name.equals("edit")) {      //$NON-NLS-1$ //$NON-NLS-2$
      // another table gained focus or changed
      table.clearSelection();
      table.rowToSelect = 0;
      table.columnToSelect = 0;
      table.selectOnFocus = false;
      refreshButtons();
    } else if(e.getPropertyName().equals("clipboard")) { //$NON-NLS-1$
      // clipboard contents have changed
      refreshButtons();
    }
  }

  /**
   * Sets custom buttons on the button panel. Setting buttons to null
   * removes all buttons from this editor.
   */
  public void setCustomButtons(AbstractButton[] buttons) {
    customButtons = buttons;
    if((buttons==null)||(buttons.length==0)) {
      remove(buttonPanel);
      return;
    }
    buttonPanel.removeAll();
    for(int i = 0; i<buttons.length; i++) {
      buttonPanel.add(buttons[i]);
    }
    add(buttonPanel, BorderLayout.NORTH);
  }
  
  /**
   * sets the usePopupEditor flag.
   * 
   * @param popup true to use the popup editor.
   */
  public void setUsePopupEditor(boolean popup) {
  	usePopupEditor = popup;
  }

  /**
   * Gets an undoable edit.
   *
   * @param type may be ADD_EDIT, REMOVE_EDIT, NAME_EDIT, or EXPRESSION_EDIT 
   * @param redo the new state
   * @param redoRow the newly selected row
   * @param redoCol the newly selected column
   * @param undo the previous state
   * @param undoRow the previously selected row
   * @param undoCol the previously selected column
   * @param name the name of the edited object
   */
  protected UndoableEdit getUndoableEdit(int type, Object redo, int redoRow, int redoCol, Object undo, int undoRow, int undoCol, String name) {
    if(type==EXPRESSION_EDIT) {
      ArrayList<AbstractButton> selectedButtons = new ArrayList<AbstractButton>();
      undo = new Object[] {undo, selectedButtons};
      redo = new Object[] {redo, selectedButtons};
      if(customButtons!=null) {
        for(AbstractButton b : customButtons) {
          if(b.isSelected()) {
            selectedButtons.add(b);
          }
        }
      }
    }
    return new DefaultEdit(type, redo, redoRow, redoCol, undo, undoRow, undoCol, name);
  }

  /**
   * Determines if an object's name is editable.
   *
   * @param obj the object
   * @return true if the name is editable
   */
  public boolean isNameEditable(Object obj) {
    return true;
  }

  /**
   * Determines if an object's expression is editable.
   *
   * @param obj the object
   * @return true if the expression is editable
   */
  public boolean isExpressionEditable(Object obj) {
    return true;
  }

  /**
   * Determines if an object is removable.
   *
   * @param obj the object
   * @return true if removable
   */
  protected boolean isRemovable(Object obj) {
    return !isImportant(obj)&&isNameEditable(obj)&&isExpressionEditable(obj);
  }

  /**
   * Determines if an object is important.
   *
   * @param obj the object
   * @return true if important
   */
  protected boolean isImportant(Object obj) {
    return false;
  }

  /**
   * Sets the anglesInDegrees flag. Angles are displayed in degrees
   * when true, radians when false.
   *
   * @param degrees true to display angles in degrees
   */
  public void setAnglesInDegrees(boolean degrees) {
    anglesInDegrees = degrees;
    table.repaint();
  }

  /**
   * Evaluates all current objects.
   */
  public void evaluateAll() {
    // refresh names array
    if(names.length!=objects.size()) {
      names = new String[objects.size()];
    }
    for(int i = 0; i<names.length; i++) {
      names[i] = getName(objects.get(i));
    }
    // sort the objects by name length
    sortedObjects.clear();
    if(objects.size()>0) {
      sortedObjects.add(objects.get(0));
      for(int i = 1; i<objects.size(); i++) {
        int size = sortedObjects.size();
        for(int j = 0; j<size; j++) {
          Object obj = objects.get(i);
          String name = getName(obj);
          if(name.length()>getName(sortedObjects.get(j)).length()) {
            sortedObjects.add(j, obj);
            break;
          } else if(j==size-1) {
            sortedObjects.add(obj);
          }
        }
      }
    }
    // check for circular references
    circularErrors.clear();
    for(Iterator<Object> it = objects.iterator(); it.hasNext(); ) {
      Object next = it.next();
      String name = getName(next);
      if(getReferences(name, null).contains(name)) {
        circularErrors.add(next);
      }
    }
    // find all functions that reference circular errors
    errors.clear();
    for(Iterator<Object> it = objects.iterator(); it.hasNext(); ) {
      Object next = it.next();
      String name = getName(next);
      for(Iterator<Object> it2 = circularErrors.iterator(); it2.hasNext(); ) {
        String badName = getName(it2.next());
        if(getReferences(name, null).contains(badName)) {
          errors.add(next);
          break;
        }
      }
    }
    // establish evaluation order
    evaluate.clear();
    ArrayList<Object> temp = new ArrayList<Object>(objects);
    temp.removeAll(errors);
    ArrayList<String> names = new ArrayList<String>();
    while(!temp.isEmpty()) {
      for(int i = 0; i<temp.size(); i++) {
        Object next = temp.get(i);
        String name = getName(next);
        Set<String> references = getReferences(name, null);
        if(names.containsAll(references)) {
          evaluate.add(next);
          names.add(name);
        }
      }
      temp.removeAll(evaluate);
    }
  }

  /**
   * Gets the names of functions referenced in a named function expression
   * either directly or indirectly.
   *
   * @param name the name of the function
   * @param references a Set to add references to (may be null)
   * @return the set filled with names of referenced functions
   */
  protected Set<String> getReferences(String name, Set<String> references) {
    if(references==null) {
      references = new HashSet<String>();
    }
    Object obj = getObject(name);
    if(obj!=null) {
      String eqn = getExpression(obj);
      List<Object> directReferences = new ArrayList<Object>();
      for(Iterator<Object> it = sortedObjects.iterator(); it.hasNext(); ) {
        Object next = it.next();
        if(next==obj) {
          continue;
        }
        name = getName(next);
        if(eqn.indexOf(name)>-1) {
          eqn = eqn.replaceAll(name, "#"); //$NON-NLS-1$
          directReferences.add(next);
          if(!references.contains(name)) {
            references.add(name);
            references.addAll(getReferences(name, references));
          }
        }
      }
      setReferences(obj, directReferences);
    }
    return references;
  }

  /**
   * Subclasses implement to set objects referenced in an object's expression.
   */
  protected void setReferences(Object obj, List<Object> referencedObjects) {
    /** empty block */
  }

  /**
   * Creates the GUI.
   */
  protected void createGUI() {
    // create table and scroller
    table = new Table(tableModel);
    tableScroller = new JScrollPane(table);
    tableScroller.createHorizontalScrollBar();
    add(tableScroller, BorderLayout.CENTER);
    buttonPanel = new JPanel(new FlowLayout());
    newButton = new JButton();
    newButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String name = getDefaultName();
        Object obj = createUniqueObject(null, name, false);
        addObject(obj, true);
      }

    });
    cutButton = new JButton();
    cutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Object[] array = getSelectedObjects();
        copy(array);
        for(int i = array.length; i>0; i--) {
          removeObject(array[i-1], true);
        }
        evaluateAll();
      }

    });
    copyButton = new JButton();
    copyButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        copy(getSelectedObjects());
      }

    });
    pasteButton = new JButton();
    pasteButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        paste();
      }

    });
    buttonPanel.add(newButton);
    buttonPanel.add(copyButton);
    buttonPanel.add(cutButton);
    buttonPanel.add(pasteButton);
    add(buttonPanel, BorderLayout.NORTH);
    MouseListener tableFocuser = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        table.requestFocusInWindow();
        functionPanel.clearSelection();
      }

    };
    table.getTableHeader().addMouseListener(tableFocuser);
    tableScroller.addMouseListener(tableFocuser);
    buttonPanel.addMouseListener(tableFocuser);
    buttonPanel.addMouseListener(tableFocuser);
    addMouseListener(tableFocuser);
  }

  /**
   * Refreshes the GUI.
   */
  protected void refreshGUI() {
    int[] rows = table.getSelectedRows();
    int col = table.getSelectedColumn();
    tableModel.fireTableStructureChanged(); // refreshes table header strings
    revalidate();
    for(int i = 0; i<rows.length; i++) {
      table.addRowSelectionInterval(rows[i], rows[i]);
    }
    if(rows.length>0) {
      table.setColumnSelectionInterval(col, col);
      table.requestFocusInWindow();
    }
    newButton.setText(ToolsRes.getString("FunctionEditor.Button.New"));                    //$NON-NLS-1$
    newButton.setToolTipText(ToolsRes.getString("FunctionEditor.Button.New.Tooltip"));     //$NON-NLS-1$
    cutButton.setText(ToolsRes.getString("FunctionEditor.Button.Cut"));                    //$NON-NLS-1$
    cutButton.setToolTipText(ToolsRes.getString("FunctionEditor.Button.Cut.Tooltip"));     //$NON-NLS-1$
    copyButton.setText(ToolsRes.getString("FunctionEditor.Button.Copy"));                  //$NON-NLS-1$
    copyButton.setToolTipText(ToolsRes.getString("FunctionEditor.Button.Copy.Tooltip"));   //$NON-NLS-1$
    pasteButton.setText(ToolsRes.getString("FunctionEditor.Button.Paste"));                //$NON-NLS-1$
    pasteButton.setToolTipText(ToolsRes.getString("FunctionEditor.Button.Paste.Tooltip")); //$NON-NLS-1$
    setBorder(BorderFactory.createTitledBorder(ToolsRes.getString("FunctionEditor.Border.Title"))); //$NON-NLS-1$     
    refreshButtons();
  }

  /**
   * Refreshes button states.
   */
  protected void refreshButtons() {
    boolean b = getSelectedObject()!=null;
    copyButton.setEnabled(b);
    cutButton.setEnabled(b&&isRemovable(getSelectedObject()));
    pasteButton.setEnabled(getClipboardContents()!=null);
  }

  /**
   * Gets the param editor that defines parameters for functions.
   */
  protected ParamEditor getParamEditor() {
    return paramEditor;
  }

  /**
   * Sets the param editor that defines parameters for functions.
   * By default, the editor pased in is ignored unless not yet set.
   */
  protected void setParamEditor(ParamEditor editor) {
    if((paramEditor==null)&&(editor!=null)) {
      paramEditor = editor;
      evaluateAll();
      refreshGUI();
    }
  }

  /**
   * Returns the default name for newly created objects.
   */
  protected String getDefaultName() {
    return ToolsRes.getString("FunctionEditor.New.Name.Default"); //$NON-NLS-1$
  }

  /**
   * Returns a String with the names of variables available for expressions.
   * This default returns the names of all objects in this panel
   * except the selected object.
   */
  protected String getVariablesString(String separator) {
    StringBuffer vars = new StringBuffer(""); //$NON-NLS-1$
    int init = vars.length();
    boolean firstItem = true;
    String nameToSkip = getName(getSelectedObject());
    for(int i = 0; i<names.length; i++) {
      if(names[i].equals(nameToSkip)) {
        continue;
      }
      if(!firstItem) {
        vars.append(" "); //$NON-NLS-1$
      }
      vars.append(names[i]);
      firstItem = false;
    }
    if(vars.length()==init) {
      return ToolsRes.getString("FunctionPanel.Instructions.Help"); //$NON-NLS-1$
    }
    return ToolsRes.getString("FunctionPanel.Instructions.ValueCell") //$NON-NLS-1$
           +separator+vars.toString(); 
  }

  /**
   * Returns the number of removable rows.
   */
  private int getRemovableRowCount() {
    int i = 0;
    for(Iterator<Object> it = objects.iterator(); it.hasNext(); ) {
      Object obj = it.next();
      if(isRemovable(obj)) {
        i++;
      }
    }
    return i;
  }

  /**
   * Returns the number of editable rows.
   */
  protected int getPartlyEditableRowCount() {
    int i = 0;
    for(Iterator<Object> it = objects.iterator(); it.hasNext(); ) {
      Object obj = it.next();
      if(isNameEditable(obj)||isExpressionEditable(obj)) {
        i++;
      }
    }
    return i;
  }

  /**
   * Returns true if the object expression is invalid.
   */
  protected boolean isInvalidExpression(Object obj) {
    return false;
  }

  /**
   * Returns true if any objects have invalid expressions.
   */
  protected boolean containsInvalidExpressions() {
    for(Iterator<Object> it = objects.iterator(); it.hasNext(); ) {
      if(isInvalidExpression(it.next())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Copies an array of objects to the clipboard.
   *
   * @param array the array
   */
  private void copy(Object[] array) {
    if((array!=null)&&(array.length>0)) {
      XMLControl control = new XMLControlElement(this);
      control.setValue("selected", array);         //$NON-NLS-1$
      StringSelection ss = new StringSelection(control.toXML());
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(ss, ss);
      pasteButton.setEnabled(true);
      firePropertyChange("clipboard", null, null); //$NON-NLS-1$
    }
  }

  /**
   * Pastes the clipboard contents.
   */
  protected void paste() {
    XMLControl[] controls = getClipboardContents();
    if(controls==null) {
      return;
    }
    for(int i = 0; i<controls.length; i++) {
      // create a new object
      Object obj = controls[i].loadObject(null);
      addObject(obj, true);
    }
    evaluateAll();
  }

  /**
   * Gets the clipboard contents.
   */
  protected XMLControl[] getClipboardContents() {
    try {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable tran = clipboard.getContents(null);
      String dataString = (String) tran.getTransferData(DataFlavor.stringFlavor);
      if(dataString!=null) {
        XMLControlElement control = new XMLControlElement();
        control.readXML(dataString);
        if(control.getObjectClass()==this.getClass()) {
          java.util.List<Object> list = control.getPropertyContent();
          for(int i = 0; i<list.size(); i++) {
            Object next = list.get(i);
            if(next instanceof XMLProperty) {
              XMLProperty prop = (XMLProperty) next;
              if(prop.getPropertyName().equals("selected")) { //$NON-NLS-1$
                return prop.getChildControls();
              }
            }
          }
          return null;
        }
      }
    } catch(Exception ex) {
      /** empty block */
    }
    return null;
  }

  /**
   * Returns the currently selected object, if any.
   */
  protected Object getSelectedObject() {
    int row = table.getSelectedRow();
    if(row==-1) {
      return null;
    }
    return objects.get(row);
  }

  /**
   * Returns the currently selected objects, if any.
   */
  protected Object[] getSelectedObjects() {
    int[] rows = table.getSelectedRows();
    Object[] selected = new Object[rows.length];
    for(int i = 0; i<rows.length; i++) {
      selected[i] = objects.get(rows[i]);
    }
    return selected;
  }

  /**
   * Creates an object with specified name and expression. An existing object
   * may be passed in for modification or cloning, but there is no guarantee
   * the same object will be returned.
   *
   * @param name the name
   * @param expression the expression
   * @param obj an object to assign values (may be null)
   * @return the object
   */
  protected Object createObject(String name, String expression, Object obj) {
    return null;
  }

  /**
   * Returns true if a name is forbidden or in use.
   *
   * @param obj the object (may be null)
   * @param name the proposed name for the object
   * @return true if disallowed
   */
  protected boolean isDisallowedName(Object obj, String name) {
    if(forbiddenNames.contains(name)) {
      return true;
    }
    Iterator<Object> it = objects.iterator();
    while(it.hasNext()) {
      Object next = it.next();
      if(next==obj) {
        continue;
      }
      if(name.equals(getName(next))) {
        return true;
      }
    }
    if((paramEditor!=null)&&(paramEditor!=this)) { // check for
      Parameter[] params = paramEditor.getParameters();
      for(int i = 0; i<params.length; i++) {
        if((params[i]!=obj)&&name.equals(params[i].getName())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns a valid function name by removing spaces and symbols.
   *
   * @param proposed the proposed name
   * @return a valid name
   */
  private String getValidName(String proposedName) {
    if(proposedName==null || proposedName.trim().equals("")) { //$NON-NLS-1$   
      return "";                         //$NON-NLS-1$
    }
    String name = proposedName;
    ArrayList<String> invalid = getInvalidTokens(name);
    while(!invalid.isEmpty()) {
      for(Iterator<String> it = invalid.iterator(); it.hasNext(); ) {
        String next = it.next();
        int n = name.indexOf(next);
        while(n>-1) {
          name = (n==0) ? name.substring(next.length()) : name.substring(0, n)+name.substring(n+next.length());
          n = name.indexOf(next);
        }
      }
      Object input = JOptionPane.showInputDialog(FunctionEditor.this, 
      		ToolsRes.getString("FunctionEditor.Dialog.InvalidName.Message"), //$NON-NLS-1$
      		ToolsRes.getString("FunctionEditor.Dialog.InvalidName.Title"), //$NON-NLS-1$
          JOptionPane.WARNING_MESSAGE, null, null, name);
      if(input==null) {
        return null;
      }
      if(input.equals(name)) {
        break;
      }
      name = input.toString();
      invalid = getInvalidTokens(name);
    }
    return name;
  }

  /**
   * Returns a list of invalid tokens found in the name.
   * Invalid tokens include spaces and mathematical symbols.
   *
   * @param name the name
   * @return a list of invalid tokens
   */
  private ArrayList<String> getInvalidTokens(String name) {
    ArrayList<String> invalid = new ArrayList<String>();
    if(name.indexOf(" ")>-1) { //$NON-NLS-1$
      invalid.add(" "); //$NON-NLS-1$
    }
    String[] suspects = FunctionTool.parserOperators;
    for(int i = 0; i<suspects.length; i++) {
      if(name.indexOf(suspects[i])>-1) {
        invalid.add(suspects[i]);
      }
    }
    return invalid;
  }

  /**
   * Creates an object with a unique name.
   *
   * @param obj the object (may be null)
   * @param proposedName the proposed name
   * @param confirmChanges true to require user to confirm changes
   * @return the object
   */
  protected Object createUniqueObject(Object obj, String proposedName, boolean confirmChanges) {
    // construct a unique name from that proposed if nec
    proposedName = getValidName(proposedName);
    if(proposedName.trim().equals("")) { //$NON-NLS-1$   
      return null;
    }
    String name = proposedName;
    while(isDisallowedName(obj, proposedName)) {
      int i = 0;
      while(isDisallowedName(obj, name)) {
        i++;
        name = proposedName+i;
      }
      if(!confirmChanges) {
        break;
      }
      Object input = JOptionPane.showInputDialog(this, "\""+proposedName+"\" "+ //$NON-NLS-1$ //$NON-NLS-2$
        ToolsRes.getString("FunctionEditor.Dialog.DuplicateName.Message"),      //$NON-NLS-1$
          ToolsRes.getString("FunctionEditor.Dialog.DuplicateName.Title"),      //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE, null, null, name);
      if(input==null) {
        return null;
      }
      if(input.equals("")||input.equals(name)) {                                //$NON-NLS-1$   
        break;
      }
      name = proposedName = input.toString();
    }
    String expression = (obj==null) ? "0" : getExpression(obj); //$NON-NLS-1$
    return createObject(name, expression, obj);
  }

  /**
   * Class description
   *
  */
  public class Table extends JTable {
    public boolean selectOnFocus = true;
    int rowToSelect, columnToSelect;

    // constructor
    Table(TableModel model) {
      setModel(model);
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      setColumnSelectionAllowed(false);
      getTableHeader().setReorderingAllowed(false);
      setGridColor(Color.BLACK);
      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          int row = rowAtPoint(e.getPoint());
          int col = columnAtPoint(e.getPoint());
          table.rowToSelect = row;
          table.columnToSelect = col;
          if(!tableModel.isCellEditable(row, col)) {
            functionPanel.clearSelection();
            selectOnFocus = false;
          } else if(e.getClickCount()==1) {
            functionPanel.refreshInstructions(FunctionEditor.this, false, col);
            selectOnFocus = table.hasFocus();
          }
        }

      });
      addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
          firePropertyChange("focus", null, null); //$NON-NLS-1$
          if(getRowCount()==0) {
            functionPanel.tabToNext(FunctionEditor.this);
            return;
          }
          if(selectOnFocus&&(getRowCount()>0)) {
            selectCell(rowToSelect, columnToSelect);
            int col = table.getSelectedColumn();
            functionPanel.refreshInstructions(FunctionEditor.this, false, col);
          }
          selectOnFocus = true;
        }
        public void focusLost(FocusEvent e) {
          rowToSelect = Math.max(0, getSelectedRow());
          columnToSelect = Math.max(0, getSelectedColumn());
        }

      });
      // enter key action starts editing
      InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      Action enterAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          // start editing
          JTable table = (JTable) e.getSource();
          int row = table.getSelectedRow();
          int column = table.getSelectedColumn();
          table.editCellAt(row, column, e);
          FunctionEditor.this.tableCellEditor.field.requestFocus();
          FunctionEditor.this.tableCellEditor.field.selectAll();
        }

      };
      KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
      getActionMap().put(im.get(enter), enterAction);
      // tab key tabs to next editable cell or component
      KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
      Action tabAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          int rowCount = table.getRowCount();
          int row = table.rowToSelect;
          int col = table.columnToSelect;
          boolean atEnd = ((col==1)&&(row==rowCount-1));
          // determine next cell row and column
          col = (col==0) ? 1 : 0;
          row = (col==0) ? ((row==getRowCount()-1) ? 0 : row+1) : row;
          if(table.isEditing()) {
            table.rowToSelect = row;
            table.columnToSelect = col;
            tableCellEditor.stopCellEditing();
          }
          if(atEnd) {
            functionPanel.tabToNext(FunctionEditor.this);
            table.clearSelection();
          } else {
            table.requestFocusInWindow();
            selectCell(row, col);
            functionPanel.refreshInstructions(FunctionEditor.this, false, col);
          }
        }

      };
      getActionMap().put(im.get(tab), tabAction);
    }

    public void selectCell(int row, int col) {
      // trap for high row numbers
      if(row==getRowCount()) {
        row = getRowCount()-1;
        col = 0;
      }
      if(row==-1) {
        return;
      }
      while(!isCellEditable(row, col)) {
        if(col==0) {
          col = 1;
        } else {
          col = 0;
          row += 1;
        }
        if(row==getRowCount()) {
          row = 0;
        }
        if((row==getSelectedRow())&&(col==getSelectedColumn())) {
          break;
        }
      }
      table.rowToSelect = row;
      table.columnToSelect = col;
      table.changeSelection(row, col, false, false);
    }

    // gets the cell editor
    public TableCellEditor getCellEditor(int row, int column) {
      return tableCellEditor;
    }

    // gets the cell renderer
    public TableCellRenderer getCellRenderer(int row, int column) {
      return tableCellRenderer;
    }

    public void setFont(Font font) {
      super.setFont(font);
      getTableHeader().setFont(font);
      tableCellRenderer.font = font;
      tableCellEditor.field.setFont(font);
      int size = Math.max(font.getSize(), 24);
      tableCellEditor.popupField.setFont(font.deriveFont((float)size));
      setRowHeight(font.getSize()+4);
    }

  }

  /**
   * The table model.
   */
  protected class TableModel extends AbstractTableModel {
    boolean settingValue = false;

    // returns the number of columns in the table
    public int getColumnCount() {
      return 2;
    }

    // returns the number of rows in the table
    public int getRowCount() {
      return objects.size();
    }

    // gets the name of the column
    public String getColumnName(int col) {
      return(col==0) ? ToolsRes.getString("FunctionEditor.Table.Column.Name") : //$NON-NLS-1$
        ToolsRes.getString("FunctionEditor.Table.Column.Value");                //$NON-NLS-1$
    }

    // gets the value in a cell
    public Object getValueAt(int row, int col) {
      Object obj = objects.get(row);
      String name = getName(obj);
      if (col==0) return name;
      String expression = getExpression(obj);
      try {
				double value = Double.parseDouble(expression);
	      if (anglesInDegrees && 
	      		(name.indexOf(THETA)>-1 || name.indexOf(OMEGA)>-1)) {
	      	String s = format(value*180/Math.PI, 0.0001);
					if (name.indexOf(THETA)>-1)
						s += DEGREES;
					return s;		      	
	      }
	      return format(value, 0);
			} catch (NumberFormatException e) {
			}
      return expression;
    }

    // changes the value of a cell
    public void setValueAt(Object value, int row, int col) {
      if(settingValue) {
        return;
      }
      if(value instanceof String) {
        String val = (String) value;
        int n = val.indexOf(DEGREES);
        if (n>-1)
        	val = val.substring(0, n);
        // get previous state for undoable edit
        String prev = null;
        int type = 0;
        Object obj = objects.get(row);
        if(col==0) { // name
          prev = getName(obj);
          type = NAME_EDIT;
          settingValue = true;
          if(!val.equals(prev)) {
            obj = createUniqueObject(obj, val, true);
          }
          settingValue = false;
          if((obj==null)||val.equals(prev)) {
            functionPanel.refreshInstructions(FunctionEditor.this, false, 0);
            return;
          }
          objects.remove(row);
          objects.add(row, obj);
        } 
        else {  // expression
//          prev = getExpression(obj);
          prev = getValueAt(row, col).toString();
          type = EXPRESSION_EDIT;
          if(val.equals(prev)) {
            functionPanel.refreshInstructions(FunctionEditor.this, false, 1);
            return;
          }
          if(val.equals("")) {                          //$NON-NLS-1$   
            val = "0";                                  //$NON-NLS-1$  
          }
          String name = getName(obj);          
          if (anglesInDegrees && 
          		(name.indexOf(THETA)>-1 || name.indexOf(OMEGA)>-1)) {
            try {
      				double d = Double.parseDouble(val);
      				val = String.valueOf(d*Math.PI/180);
      			} catch (NumberFormatException e) {  				
      			}
          }
          obj = createObject(getName(obj), val, obj);
          objects.remove(row);
          objects.add(row, obj);
        }
        evaluateAll();
        table.repaint();
     // inform and pass undoable edit to listeners
        UndoableEdit edit = getUndoableEdit(type, val, row, col, prev, row, col, getName(obj));
        firePropertyChange("edit", getName(obj), edit); //$NON-NLS-1$
        functionPanel.refreshInstructions(FunctionEditor.this, false, col);
      }
    }

    // determines if a cell is editable
    public boolean isCellEditable(int row, int col) {
      Object obj = objects.get(row);
      return((col==0)&&isNameEditable(obj))||((col==1)&&isExpressionEditable(obj));
    }

  }

  private class CellEditor extends AbstractCellEditor implements TableCellEditor {
    JPanel panel = new JPanel(new BorderLayout());
    JTextField field = new JTextField();
    boolean keyPressed = false;
    boolean mouseClicked = false;
    JDialog popupEditor;
    JPanel editorPane;
    JTextField popupField = new JTextField();
    JTextPane variablesPane;
    int minPopupWidth, varBegin, varEnd;
    String prevExpression;

    // Constructor.
    CellEditor() {
      panel.add(field, BorderLayout.CENTER);
      panel.setOpaque(false);
      panel.setBorder(BorderFactory.createEmptyBorder(0, 1, 1, 2));
      field.setBorder(null);
      field.setEditable(true);
      field.setFont(field.getFont().deriveFont(18f));
      field.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if(e.getKeyCode()==KeyEvent.VK_ENTER) {
            keyPressed = true;
            stopCellEditing();
          } else {
            field.setBackground(Color.yellow);
          }
        }

      });
      field.addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
        	if (usePopupEditor) {
            stopCellEditing();
        	}
          mouseClicked = false;
          table.clearSelection();
        }
        public void focusLost(FocusEvent e) {
          if(!mouseClicked) {
            stopCellEditing();
          }
          if(keyPressed) {
            keyPressed = false;
            table.requestFocusInWindow();
          }
        }

      });
    }
    
    // Gets the component to be displayed while editing.
    public Component getTableCellEditorComponent(JTable atable, Object value, boolean isSelected, int row, int column) {
      table.rowToSelect = row;
      table.columnToSelect = column;
      field.setText(value.toString());
      if (usePopupEditor) {
      	JDialog popup = getPopupEditor();
	      popupField.setText(value.toString());
	      prevExpression = value.toString();
	      popupField.selectAll();
	      if (column==1) {
		      variablesPane.setText(getVariablesString(":\n")); //$NON-NLS-1$
	        StyledDocument doc = variablesPane.getStyledDocument();
	        Style blue = doc.getStyle("blue"); //$NON-NLS-1$
	        doc.setCharacterAttributes(0, variablesPane.getText().length(), blue, false);
	        popup.getContentPane().add(variablesPane, BorderLayout.SOUTH);
	      }
	      else {
	        popup.getContentPane().remove(variablesPane);
	      }
	      Rectangle cell = table.getCellRect(row, column, true);
	      minPopupWidth = cell.width+2;
	      Dimension dim = resizePopupEditor();
	      Point p = table.getLocationOnScreen();
	      popup.setLocation(p.x+cell.x+cell.width/2-dim.width/2, 
	      		p.y+cell.y+cell.height/2-dim.height/2);
	      popup.setVisible(true);
      }
      else {
        functionPanel.refreshInstructions(FunctionEditor.this, true, column);
        functionPanel.tableEditorField = field;
      }
      return panel;
    }

    // Determines when editing starts.
    public boolean isCellEditable(EventObject e) {
      if(e instanceof MouseEvent) {
        firePropertyChange("focus", null, null); //$NON-NLS-1$
        MouseEvent me = (MouseEvent) e;
        if(me.getClickCount()==2) {
          mouseClicked = true;
          Runnable runner = new Runnable() {
            public synchronized void run() {
              field.selectAll();
            }
          };
          SwingUtilities.invokeLater(runner);
          return true;
        }
      } else if((e==null)||(e instanceof ActionEvent)) {
        return true;
      }
      return false;
    }
    
    // Called when editing is completed.
    public Object getCellEditorValue() {
    	if (usePopupEditor) {
        popupField.setBackground(Color.WHITE);
    	}
      field.setBackground(Color.WHITE);
      return field.getText();
    }

    // Resizes the popup editor and returns the new size
    private Dimension resizePopupEditor() {
    	String s = popupField.getText();
    	Font font = popupField.getFont();
    	Rectangle rect = font.getStringBounds(s, frc).getBounds();
    	int h = rect.height;
    	int w = Math.max(minPopupWidth, rect.width+32);
    	if (table.columnToSelect==1) {
	    	s = variablesPane.getText();
	    	int n = s.indexOf("\n"); //$NON-NLS-1$
	    	s = s.substring(n+1);
	    	font = variablesPane.getFont().deriveFont(Font.BOLD);
	    	rect = font.getStringBounds(s, frc).getBounds();
	    	w = Math.max(w, rect.width);
    	}
    	Dimension dim = new Dimension(w, h);    	
    	editorPane.setPreferredSize(dim);
      popupEditor.pack();
      dim.width = popupEditor.getWidth();
      return dim;
    }

    // Gets the popup editor
    private JDialog getPopupEditor() {
    	if (popupEditor==null) {
        popupField.setEditable(true);
        popupField.setFont(popupField.getFont().deriveFont(24f));
        popupField.addKeyListener(new KeyAdapter() {
          public void keyPressed(KeyEvent e) {
            if(e.getKeyCode()==KeyEvent.VK_ENTER) {
              keyPressed = true;
            	popupEditor.setVisible(false);
              field.setText(popupField.getText());
              field.requestFocusInWindow();
              field.selectAll();
              stopCellEditing();
            } else {
            	popupField.setBackground(Color.yellow);
            	resizePopupEditor();
            }
          }
        });
        
        // create variables pane
        variablesPane = new JTextPane() {
          public void paintComponent(Graphics g) {
            if(OSPRuntime.antiAliasText) {
              Graphics2D g2 = (Graphics2D) g;
              RenderingHints rh = g2.getRenderingHints();
              rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
              rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            super.paintComponent(g);
          }

        };
        variablesPane.setEditable(false);
//        variablesPane.setOpaque(false);
        variablesPane.setFocusable(false);
        variablesPane.setBorder(popupField.getBorder());
        variablesPane.setFont(popupField.getFont().deriveFont(14f));
        StyledDocument doc = variablesPane.getStyledDocument();
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(def, "SansSerif"); //$NON-NLS-1$
        Style blue = doc.addStyle("blue", def); //$NON-NLS-1$
        StyleConstants.setBold(blue, false);
        StyleConstants.setForeground(blue, Color.blue);
        Style red = doc.addStyle("red", blue); //$NON-NLS-1$
        StyleConstants.setBold(red, true);
        StyleConstants.setForeground(red, Color.red);
        varBegin = varEnd = 0;
        variablesPane.addMouseListener(new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            if(varEnd==0) {
              return;
            }
            variablesPane.setCaretPosition(varBegin);
            variablesPane.moveCaretPosition(varEnd);
            popupField.replaceSelection(variablesPane.getSelectedText());
            popupField.setBackground(Color.yellow);
          }
          public void mouseExited(MouseEvent e) {
            StyledDocument doc = variablesPane.getStyledDocument();
            Style blue = doc.getStyle("blue"); //$NON-NLS-1$
            doc.setCharacterAttributes(0, variablesPane.getText().length(), blue, false);
            varBegin = varEnd = 0;
          }

        });
        variablesPane.addMouseMotionListener(new MouseMotionAdapter() {
          public void mouseMoved(MouseEvent e) {
            varBegin = varEnd = 0;
            // select and highlight the variable under mouse
            String text = variablesPane.getText();
            // first separate the instructions from the variables
            int startVars = text.indexOf(":\n");     //$NON-NLS-1$
            if(startVars==-1) {
              return;
            }
            startVars += 2;
            String vars = text.substring(startVars);
            StyledDocument doc = variablesPane.getStyledDocument();
            Style blue = doc.getStyle("blue"); //$NON-NLS-1$
            Style red = doc.getStyle("red");   //$NON-NLS-1$
            int beginVar = variablesPane.viewToModel(e.getPoint())-startVars;
            if(beginVar<0) {  // mouse is over instructions
              doc.setCharacterAttributes(0, text.length(), blue, false);
              return;
            }
            while (beginVar>0) {
	            // back up to preceding space
	            String s = vars.substring(0, beginVar);
	            if (s.endsWith(" ")) //$NON-NLS-1$
	            	break;
	            beginVar--;
            }
            varBegin = beginVar+startVars;
            // find following comma, space or end
            String s = vars.substring(beginVar);
            int len = s.indexOf(",");          //$NON-NLS-1$
            if(len==-1) len = s.indexOf(" ");  //$NON-NLS-1$
            if(len==-1) len = s.length();
           // set variable bounds and character style
            varEnd = varBegin+len;
            doc.setCharacterAttributes(0, varBegin, blue, false);
            doc.setCharacterAttributes(varBegin, len, red, false);
            doc.setCharacterAttributes(varEnd, text.length()-varEnd, blue, false);
          }
        });
        // make close button
        String imageFile = "/org/opensourcephysics/resources/tools/images/close.gif"; //$NON-NLS-1$
        Icon icon = ResourceLoader.getIcon(imageFile);
        JButton closeButton = new JButton(icon);
        Border line = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
        Border space = BorderFactory.createEmptyBorder(0,2,0,2);
        closeButton.setBorder(BorderFactory.createCompoundBorder(line, space));
        closeButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            keyPressed = true;
          	popupField.setBackground(Color.WHITE);
          	popupField.setText(prevExpression);
          	popupEditor.setVisible(false);
            stopCellEditing();
          }
        });
        // assemble components
	      Frame frame = JOptionPane.getFrameForComponent(FunctionEditor.this);
	      popupEditor = new JDialog(frame, true);
	      popupEditor.setUndecorated(true);
	      popupEditor.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
	      JPanel contentPane = new JPanel(new BorderLayout());
	      popupEditor.setContentPane(contentPane);
	      editorPane = new JPanel(new BorderLayout());
	      editorPane.setBackground(Color.WHITE);
	      editorPane.add(popupField, BorderLayout.CENTER);
	      editorPane.add(closeButton, BorderLayout.EAST);
	      contentPane.add(editorPane, BorderLayout.NORTH);
	      contentPane.add(variablesPane, BorderLayout.SOUTH);
    	}
    	return popupEditor;
    }

  }

  private class CellRenderer extends DefaultTableCellRenderer {
    Font font = new JTextField().getFont();

    // Constructor

    /**
     * Constructor CellRenderer
     */
    public CellRenderer() {
      super();
      setOpaque(true); // make background visible
      setFont(font);
      setHorizontalAlignment(SwingConstants.LEFT);
      setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 2));
    }

    // Returns a label for the specified cell.
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
      setText(value.toString());
      Object obj = objects.get(row);
      String tooltip = getTooltip(obj);
      setToolTipText(((col==0)&&(tooltip!=null)) ? tooltip : (col==0) ? ToolsRes.getString("FunctionEditor.Table.Cell.Name.Tooltip") : //$NON-NLS-1$
        ToolsRes.getString("FunctionEditor.Table.Cell.Value.Tooltip")); //$NON-NLS-1$
      if((col==1)&&circularErrors.contains(obj)) {
        setToolTipText(ToolsRes.getString("FunctionEditor.Table.Cell.CircularErrors.Tooltip")); //$NON-NLS-1$
        setForeground(DARK_RED);
        if(isSelected) {
          setBackground(MEDIUM_RED);
        } else {
          setBackground(LIGHT_RED);
        }
      } 
      else if((col==1)&&isInvalidExpression(obj)) {                                           // error condition
        setToolTipText(ToolsRes.getString("FunctionEditor.Table.Cell.Invalid.Tooltip"));        //$NON-NLS-1$
        setForeground(DARK_RED);
        if(isSelected) {
          setBackground(MEDIUM_RED);
        } else {
          setBackground(LIGHT_RED);
        }
      } 
      else if(((col==0)&&!isNameEditable(obj))||((col==1)&&!isExpressionEditable(obj))) {     // uneditable cell
        setForeground(Color.BLACK);
        setBackground(LIGHT_GRAY);
      } 
      else {
        if(isSelected) {                                                                        // selected editable cell
          setForeground(hasFocus ? Color.BLUE : Color.BLACK);
          setBackground(LIGHT_BLUE);
        } else {                                                                                // unselected editable cell
          setForeground(Color.BLACK);
          setBackground(Color.WHITE);
        }
      }
      setFont(((col==0)&&isImportant(obj)) ? font.deriveFont(Font.BOLD) : font);
      refreshButtons();
      return this;
    }

  }

  /**
   * A class to undo/redo edits.
   */
  protected class DefaultEdit extends AbstractUndoableEdit {
    Object redoObj, undoObj;
    int redoRow, redoCol, undoRow, undoCol, editType;
    String name;

    /**
     * A class to undo/redo edits.
     *
     * @param type may be ADD_EDIT, REMOVE_EDIT, NAME_EDIT, or EXPRESSION_EDIT 
     * @param newVal the new object, name or expression
     * @param newRow the row selected
     * @param newCol the col selected
     * @param prevVal the previous object, name or expression
     * @param prevRow the previous row selected
     * @param prevCol the previous col selected
     * @param name the name of the edited object
     */
    public DefaultEdit(int type, Object newVal, int newRow, int newCol, Object prevVal, int prevRow, int prevCol, String name) {
      editType = type;
      redoObj = newVal;
      undoObj = prevVal;
      redoRow = newRow;
      redoCol = newCol;
      undoRow = prevRow;
      undoCol = prevCol;
      this.name = name;
      OSPLog.finer(editTypes[type]+": \""+name+"\"" //$NON-NLS-1$ //$NON-NLS-2$
                   +"\nold value: "+prevVal         //$NON-NLS-1$
                   +"\nnew value: "+newVal);        //$NON-NLS-1$
    }

    // undoes the change
    public void undo() throws CannotUndoException {
      super.undo();
      switch(editType) {
         case ADD_EDIT : {
           removeObject(undoObj, false);
           break;
         }
         case REMOVE_EDIT : {
           addObject(undoObj, undoRow, false, true);
           break;
         }
         case NAME_EDIT : {
           Object obj = objects.get(undoRow);
           String expression = getExpression(obj);
           name = undoObj.toString();
           String prevName = redoObj.toString();
           obj = createObject(name, expression, obj);
           objects.remove(undoRow);
           objects.add(undoRow, obj);
           evaluateAll();
           firePropertyChange("edit", name, prevName); //$NON-NLS-1$
           break;
         }
         case EXPRESSION_EDIT : {
           Object obj = objects.get(undoRow);
           Object[] undoArray = (Object[]) undoObj;    // array is {expression, buttons}
//           ArrayList<?> buttons = (ArrayList<?>) undoArray[1];
//           for(Object next : buttons) {
//             AbstractButton b = (AbstractButton) next;
//             if(!b.isSelected()) {
//               b.doClick(0);
//             }
//           }
           obj = createObject(name, undoArray[0].toString(), obj);
           objects.remove(undoRow);
           objects.add(undoRow, obj);
           evaluateAll();
           firePropertyChange("edit", name, null);     //$NON-NLS-1$
         }
      }
      // select cell and request keyboard focus
      table.rowToSelect = undoRow;
      table.columnToSelect = undoCol;
      getTable().selectOnFocus = true;
      getTable().requestFocusInWindow();
      refreshGUI();
    }

    // redoes the change
    public void redo() throws CannotUndoException {
      super.redo();
      switch(editType) {
         case ADD_EDIT : {
           addObject(redoObj, redoRow, false, true);
           break;
         }
         case REMOVE_EDIT : {
           removeObject(redoObj, false);
           break;
         }
         case NAME_EDIT : {
           Object obj = objects.get(redoRow);
           String expression = getExpression(obj);
           name = redoObj.toString();
           String prevName = undoObj.toString();
           obj = createObject(name, expression, obj);
           objects.remove(redoRow);
           objects.add(redoRow, obj);
           evaluateAll();
           firePropertyChange("edit", name, prevName); //$NON-NLS-1$
           break;
         }
         case EXPRESSION_EDIT : {
           Object obj = objects.get(redoRow);
           Object[] redoArray = (Object[]) redoObj;    // array is {expression, buttons}
           ArrayList<?> buttons = (ArrayList<?>) redoArray[1];
           for(Object next : buttons) {
             AbstractButton b = (AbstractButton) next;
             if(!b.isSelected()) {
               b.doClick(0);
             }
           }
           obj = createObject(name, redoArray[0].toString(), obj);
           objects.remove(redoRow);
           objects.add(redoRow, obj);
           evaluateAll();
           firePropertyChange("edit", name, null);     //$NON-NLS-1$
         }
      }
      // select cell and request keyboard focus
      table.rowToSelect = redoRow;
      table.columnToSelect = redoCol;
      getTable().selectOnFocus = true;
      getTable().requestFocusInWindow();
      refreshGUI();
    }

    // returns the presentation name
    public String getPresentationName() {
      if(editType==REMOVE_EDIT) {
        return "Deletion"; //$NON-NLS-1$
      }
      return "Edit"; //$NON-NLS-1$
    }

  }

  /**
   * Formats a number.
   *
   * @param value the number
   * @param zeroLevel the level below which the value is considered zero
   * @return the formatted string
   */
  public static String format(double value, double zeroLevel) {
    if(Math.abs(value)<zeroLevel) {
      value = 0;
    }
    int rounded = (int) Math.round(value);
    if(Math.abs(value-rounded)<zeroLevel) {
      value = rounded;
    }
    double absVal = Math.abs(value);
    boolean scientific = ((absVal<0.1)&&(value!=0))||(absVal>=1000);
    String s = scientific ? sciFormat.format(value) : decimalFormat.format(value);
    // eliminate trailing "0000x" and "9999x"
    int n = s.indexOf("E");                     // exponential symbol //$NON-NLS-1$
    String tail = (n>-1) ? s.substring(n) : ""; //$NON-NLS-1$
    s = (n>-1) ? s.substring(0, n) : s;
    n = s.indexOf("0000"); //$NON-NLS-1$
    if(n>1) {
      s = s.substring(0, n+1); // leave one trailing zero
    }
    n = s.indexOf("9999"); //$NON-NLS-1$
    if(n>1) {
      s = s.substring(0, n);
      DecimalFormatSymbols symbols = sciFormat.getDecimalFormatSymbols();
      char separator = symbols.getDecimalSeparator();
      int m = s.indexOf(separator);
      if(m==s.length()-1) {
        int i = Integer.parseInt(s.substring(0, m))+1;
        s = Integer.toString(i)+separator+"0"; //$NON-NLS-1$
      } else {
        int i = Integer.parseInt(s.substring(n-1))+1;
        s = s.substring(0, n-1)+i;
      }
    }
    return s+tail;
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
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
