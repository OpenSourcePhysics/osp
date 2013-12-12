/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableModel;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.controls.XMLProperty;
import org.opensourcephysics.display.Data;
import org.opensourcephysics.display.DataFunction;
import org.opensourcephysics.display.DataTable;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.display.DisplayColors;
import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.FunctionDrawer;
import org.opensourcephysics.display.HighlightableDataset;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display.Selectable;
import org.opensourcephysics.display.TeXParser;
import org.opensourcephysics.display.axes.CartesianInteractive;
import org.opensourcephysics.tools.DataToolTable.TableEdit;
import org.opensourcephysics.tools.DataToolTable.WorkingDataset;

/**
 * This tab displays and analyzes a single Data object in a DataTool.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class DataToolTab extends JPanel implements Tool, PropertyChangeListener {
  // static fields
  protected static String helpName = "data_tool_help.html";    //$NON-NLS-1$
  protected static NumberFormat correlationFormat = NumberFormat.getInstance();
  
  static {
    if(correlationFormat instanceof DecimalFormat) {
    	DecimalFormat format = (DecimalFormat) correlationFormat;
    	format.applyPattern("0.000"); //$NON-NLS-1$
    }

  }
  
  // instance fields
  protected DataTool dataTool; // the DataTool that displays this tab
  protected int originatorID = 0; // the ID of the Data object that owns this tab
  protected DatasetManager dataManager = new DatasetManager(); // datasets in this tab
  protected JSplitPane[] splitPanes;
  protected DataToolPlotter plot;
  protected DataToolTable dataTable;
  protected DataToolStatsTable statsTable;
  protected DataToolPropsTable propsTable;
  protected JScrollPane dataScroller, statsScroller, propsScroller, tableScroller;
  protected JToolBar toolbar;
  protected JCheckBoxMenuItem statsCheckbox, fitterCheckbox, propsCheckbox, fourierCheckbox;
  protected DatasetCurveFitter curveFitter;
  protected FourierPanel fourierPanel;
  protected JDialog fourierDialog;
  protected JButton measureButton, analyzeButton, dataBuilderButton, newColumnButton, refreshDataButton;
  protected JCheckBoxMenuItem valueCheckbox, slopeCheckbox, areaCheckbox;
  protected Action fitterAction, propsAndStatsAction;
  protected String fileName, ownerName;
  protected Map<String, String[]> ownedColumns = new TreeMap<String, String[]>();
  protected JButton helpButton;
  protected int colorIndex = 0;
  protected boolean tabChanged;
  protected boolean userEditable = false;
  protected UndoableEditSupport undoSupport;
  protected UndoManager undoManager;
  protected FunctionTool dataBuilder;
  protected int fontLevel = FontSizer.getLevel();
  protected JobManager jobManager = new JobManager(this);
  protected JLabel statusLabel, editableLabel;
  protected CartesianInteractive plotAxes;
  protected boolean positionVisible = false;
  protected boolean slopeVisible = false;
  protected boolean areaVisible = false;
  protected JPopupMenu varPopup;
  protected boolean isHorzVarPopup;
  protected Action setVarAction;
  protected boolean isInitialized = false;
  protected Object[][] constantsLoadedFromXML;
  protected boolean replaceColumnsWithMatchingNames = true;

  /**
   * Constructs a DataToolTab for the specified Data.
   *
   * @param data the Data object
   * @param tool the DataTool
   */
  public DataToolTab(Data data, DataTool tool) {
  	dataTool = tool;
    dataTable = new DataToolTable(this);
    createGUI();
    String name = ToolsRes.getString("DataToolTab.DefaultName"); //$NON-NLS-1$
    if(data!=null) {
      String s = data.getName();
      if((s!=null)&&!s.equals("")) { //$NON-NLS-1$
        name = s;
      }
    }
    setName(name);
    loadData(data, false);
    tabChanged(false);
  }

  /**
   * Loads data into this tab.
   *
   * @param data the data to load
   * @param replaceIfSameName true to replace existing data, if any
   * @return true if loaded
   */
  public ArrayList<DataColumn> loadData(final Data data, boolean replaceIfSameName) {
    ArrayList<DataColumn> loadedColumns = new ArrayList<DataColumn>();
    if(data==null) {
      return loadedColumns;
    }
    ArrayList<DataColumn> inputColumns = DataTool.getAllDataColumns(data);
    if(inputColumns==null) {
      return loadedColumns;
    }
    boolean updatedColumns = false;
    // case 1: tab contains no data
    if(dataManager.getDatasets().isEmpty()) {
      originatorID = data.getID();
      for(DataColumn next : inputColumns) {
        addColumn(next);
        loadedColumns.add(next);
      }
    }
    // case 2: tab already contains data 
    else {
      // for each local column, find matching input column
      for(Dataset local : dataManager.getDatasets()) {
        DataColumn match = getIDMatch(local, inputColumns);
        if (match!=null) {
          // if match is found, compare with local column and remove match from input
          // get y-column names
          String localName = local.getYColumnName();
          String name = match.getYColumnName();
          // temporarily set local y-column name to "" to get unique name for match
          local.setXYColumnNames("row", "");        //$NON-NLS-1$ //$NON-NLS-2$
          name = getUniqueYColumnName(match, name, false);
          local.setXYColumnNames("row", localName); //$NON-NLS-1$
          // update local if incoming points or name is different
          if(!Arrays.equals(local.getYPoints(), match.getYPoints())||!name.equals(localName)) {
            local.clear();
            double[] rows = DataTool.getRowArray(match.getIndex());
            local.append(rows, match.getYPoints());
            local.setXYColumnNames("row", name);    //$NON-NLS-1$
            updatedColumns = true;
          }
          inputColumns.remove(match);
        }
        else if (replaceIfSameName) { // no match found
        	// see if name matches an existing column
          match = getNameMatch(local, inputColumns);
          if(match!=null) {
            // if match is found, compare with local column and remove match from input
            // update local if incoming points are different
            if(!Arrays.equals(local.getYPoints(), match.getYPoints())) {
              local.clear();
              double[] rows = DataTool.getRowArray(match.getIndex());
              local.append(rows, match.getYPoints());
              updatedColumns = true;
            }
            inputColumns.remove(match);
          }
        }
      }
      // add non-matching columns
      for(DataColumn next : inputColumns) { 
        addColumn(next);
        loadedColumns.add(next);
      }
    }
    if(updatedColumns||!loadedColumns.isEmpty()) {
      dataTable.refreshTable();
      statsTable.refreshStatistics();
      refreshPlot();
      refreshGUI();
      tabChanged(true);
      varPopup = null;
    }
    return loadedColumns;
  }

  /**
   * Adds new dataColumns to this tab.
   *
   * @param source the Data source of the columns
   * @param deletable true to allow added columns to be deleted
   * @param addDuplicates true to add duplicate IDs
   * @param postEdit true to post an undoable edit
   */
  public void addColumns(Data source, boolean deletable, boolean addDuplicates, boolean postEdit) {
    // look for independent variable column and duplicate input column
    ArrayList<Dataset> datasets = dataManager.getDatasets();
    // independent variable column
    Dataset indepVar = datasets.isEmpty() ? null : datasets.get(0);
    double[] indepVarPts = (indepVar==null) ? null : indepVar.getYPoints();
    // remove Double.NaN from end of indepVarPts
    if(indepVarPts!=null) {
      while((indepVarPts.length>0)&&Double.isNaN(indepVarPts[indepVarPts.length-1])) {
        double[] newVals = new double[indepVarPts.length-1];
        System.arraycopy(indepVarPts, 0, newVals, 0, newVals.length);
        indepVarPts = newVals;
      }
    }
    // indepVarPts cannot contain duplicates
    indepVar = ((indepVarPts==null)||DataTool.containsDuplicateValues(indepVarPts)) ? null : indepVar;
    ArrayList<DataColumn> inputColumns = DataTool.getDataColumns(source);
    Dataset duplicate = null; // duplicate input column
    if(indepVar!=null) {
      String indepVarName = indepVar.getYColumnName();
      for(DataColumn next : inputColumns) {
        if((duplicate==null)&&next.getYColumnName().equals(indepVarName)) {
          // found matching column name, now compare their points
          double[] inputPts = next.getYPoints();
          // remove Double.NaN from end of inputPts
          while((inputPts.length>0)&&Double.isNaN(inputPts[inputPts.length-1])) {
            double[] newVals = new double[inputPts.length-1];
            System.arraycopy(inputPts, 0, newVals, 0, newVals.length);
            inputPts = newVals;
          }
          // inputPts also can't contain duplicate points
          if(DataTool.containsDuplicateValues(inputPts)) {
            continue;
          }
          // does at least one point in inputPts match a point in indepVarPts?
          boolean foundMatchingPoint = false;
          for(double value : inputPts) {
            if(DataTool.getIndex(value, indepVarPts, -1)>-1) {
              foundMatchingPoint = true;
              break;
            }
          }
          if(!foundMatchingPoint) {
            continue;
          }
          // found a duplicate
          duplicate = next;
          // are indepVarPts in ascending or descending order?
          int trend = 1;                             // positive trend = ascending order
          double prev = -Double.MAX_VALUE;
          for(double d : indepVarPts) {
            if(d>prev) {
              prev = d;
            } else {
              trend = -1;                            // negative trend = descending order
              break;
            }
          }
          if(trend==-1) {
            prev = Double.MAX_VALUE;
            for(double d : indepVarPts) {
              if(d<prev) {
                prev = d;
              } else {
                trend = 0;                           // neither ascending nor descending
                break;
              }
            }
          }
          // add new indepVar rows, if any, to table
          // first combine inputPts with indepVarPts into newIndepVarPts
          double[] newIndepVarPts = new double[indepVarPts.length];
          System.arraycopy(indepVarPts, 0, newIndepVarPts, 0, indepVarPts.length);
          // keep track of which values are inserted
          double[] valuesInserted = new double[inputPts.length];
          int len = 0;                               // number of inserted values
          for(int i = 0; i<inputPts.length; i++) {
            int index = DataTool.getIndex(inputPts[i], indepVarPts, -1);
            if(index==-1) {                          // need to insert inputPts[i]
              valuesInserted[len] = inputPts[i];
              len++;
              newIndepVarPts = DataTool.insert(inputPts[i], newIndepVarPts, trend);
            }
          }
          if(len>0) {
            // determine where insertions were made
            double[] rowsInserted = new double[len]; // double[] needed for getIndex()
            int[] rowsToInsert = new int[len];
            for(int i = 0; i<len; i++) {
              double val = valuesInserted[i];
              int index = DataTool.getIndex(val, newIndepVarPts, -1);
              rowsInserted[i] = index;
              rowsToInsert[i] = index;
            }
            // arrange rowsToInsert in ascending order
            Arrays.sort(rowsToInsert);
            // assemble valuesToInsert array for executing insertRows()
            double[] valuesToInsert = new double[len];
            for(int i = 0; i<len; i++) {
              int row = rowsToInsert[i];
              int index = DataTool.getIndex(row, rowsInserted, -1);
              valuesToInsert[i] = valuesInserted[index];
            }
            // prepare map of column names to double[] values to insert
            dataTable.pasteValues.clear();
            dataTable.pasteValues.put(indepVarName, valuesToInsert);
            HashMap<String, double[]> prevState = dataTable.insertRows(rowsToInsert, dataTable.pasteValues);
            // post edit: target is rows, value is map
            TableEdit edit = dataTable.new TableEdit(DataToolTable.INSERT_ROWS_EDIT, null, rowsToInsert, prevState);
            undoSupport.postEdit(edit);
            // rearrange non-duplicate columns
            for(DataColumn d : inputColumns) {
              if(d==duplicate) {
                continue;
              }
              double[] prevY = d.getYPoints();
              double[] rows = DataTool.getRowArray(newIndepVarPts.length);
              double[] newY = new double[rows.length];
              Arrays.fill(newY, Double.NaN);
              int k = Math.min(inputPts.length, prevY.length);
              for(int i = 0; i<k; i++) {
                int index = DataTool.getIndex(inputPts[i], newIndepVarPts, -1);
                newY[index] = prevY[i];
              }
              d.clear();
              d.append(rows, newY);
            }
          }
        }
      }
    }
    // finished processing input--now add input columns to tab
    inputColumns.remove(duplicate);
    addColumns(inputColumns, deletable, addDuplicates, postEdit);
  }

  /**
   * Adds DataColumns to this tab.
   *
   * @param columns the columns to add
   * @param deletable true to allow added columns to be deleted
   * @param addDuplicates true to add duplicate IDs
   * @param postEdit true to post an undoable edit
   */
  protected void addColumns(ArrayList<DataColumn> columns, boolean deletable, boolean addDuplicates, boolean postEdit) {
    for(DataColumn next : columns) {
      int id = next.getID();
      if(addDuplicates) {
        // change ID so column always added
        next.setID(-id);
      }
      ArrayList<DataColumn> loadedColumns = loadData(next, false);
      // restore original ID
      next.setID(id);
      if(!loadedColumns.isEmpty()) {
        for(DataColumn dc : loadedColumns) {
          dc.deletable = deletable;
        }
        if(postEdit) {
          int col = dataTable.getColumnCount()-1;
          // post edit: target is column, value is data column
          TableEdit edit = dataTable.new TableEdit(DataToolTable.INSERT_COLUMN_EDIT, next.getYColumnName(), new Integer(col), next);
          undoSupport.postEdit(edit);
        }
        refreshDataBuilder();
      }
    }
    dataTable.refreshUndoItems();
    refreshGUI();
  }

  /**
   * Adds a DataColumn to this tab.
   *
   * @param column the column to add
   */
  protected void addColumn(DataColumn column) {
    String name = column.getYColumnName();
    String yName = getUniqueYColumnName(column, name, false);
    if(!name.equals(yName)) {
      String xName = column.getXColumnName();
      column.setXYColumnNames(xName, yName);
    }
    if(dataManager.getDatasets().isEmpty()) {
      column.setMarkerColor(Color.BLACK);
      column.setLineColor(Color.BLACK);
    }
    dataManager.addDataset(column);
    dataTable.getWorkingData(yName);
  }

  /**
   * Sets the x and y columns by name.
   *
   * @param xColName the name of the horizontal axis variable
   * @param yColName the name of the vertical axis variable
   */
  public void setWorkingColumns(String xColName, String yColName) {
    dataTable.setWorkingColumns(xColName, yColName);
  }

  /**
   * Overrides Component.setName();
   *
   * @param name the name
   */
  public void setName(String name) {
    name = replaceSpacesWithUnderscores(name);
    super.setName(name);
    if(dataTool!=null) {
      dataTool.refreshTabTitles();
    }
  }

  /**
   * Sets the userEditable flag.
   *
   * @param editable true to enable user editing
   */
  public void setUserEditable(boolean editable) {
    if(userEditable==editable) {
      return;
    }
    userEditable = editable;
    refreshGUI();
  }

  /**
   * Determines if a dataset is deletable.
   *
   * @param data the dataset
   * @return true if deletable
   */
  protected boolean isDeletable(Dataset data) {
    if(data==null) {
      return false;
    }
    // commented out by D Brown Mar 2011 so all columns are deletable
//    if(!userEditable&&(data instanceof DataColumn)) {
//      DataColumn column = (DataColumn) data;
//      if(!column.deletable) {
//        return false;
//      }
//    }
    return true;
  }

  /**
   * Gets the data builder for defining custom data functions.
   * 
   * @return the data builder
   */
  public FunctionTool getDataBuilder() {
    if(dataTool!=null) {
      return dataTool.getDataBuilder();
    }
    if(dataBuilder==null) {                                                   // create new tool if none exists
      dataBuilder = new FunctionTool(this) {
  		  protected void refreshGUI() {
  		  	super.refreshGUI();
  		  	dropdown.setToolTipText(ToolsRes.getString
		  				("DataTool.DataBuilder.Dropdown.Tooltip")); //$NON-NLS-1$
  	  		setTitle(ToolsRes.getString("DataTool.DataBuilder.Title")); //$NON-NLS-1$
  		  }  			
      };
      dataBuilder.setFontLevel(fontLevel);
      dataBuilder.setHelpPath("data_builder_help.html");                      //$NON-NLS-1$
      dataBuilder.addPropertyChangeListener("function", this);                //$NON-NLS-1$
    }
    refreshDataBuilder();
    return dataBuilder;
  }

  /**
   * Listens for property change "function".
   *
   * @param e the event
   */
  public void propertyChange(PropertyChangeEvent e) {
    String name = e.getPropertyName();
    if(name.equals("function")) {                   //$NON-NLS-1$
      tabChanged(true);
      dataTable.refreshTable();
      statsTable.refreshStatistics();
      if(e.getNewValue() instanceof DataFunction) { // new function has been created
        String funcName = e.getNewValue().toString();
        dataTable.getWorkingData(funcName);
      }
      if(e.getOldValue() instanceof DataFunction) { // function has been deleted
        String funcName = e.getOldValue().toString();
        dataTable.removeWorkingData(funcName);
      }
      if(e.getNewValue() instanceof String) {
        String funcName = e.getNewValue().toString();
        if(e.getOldValue() instanceof String) {     // function name has changed
          String prevName = e.getOldValue().toString();
          columnNameChanged(prevName, funcName);
        } else {
          dataTable.getWorkingData(funcName);
        }
      }
      refreshPlot();
      varPopup = null;
    }
  }

  /**
   * Sends a job to this tool and specifies a tool to reply to.
   *
   * @param job the Job
   * @param replyTo the tool to notify when the job is complete (may be null)
   * @throws RemoteException
   */
  public void send(Job job, Tool replyTo) throws RemoteException {
    XMLControlElement control = new XMLControlElement(job.getXML());
    if(control.failedToRead()||(control.getObjectClass()==Object.class)) {
      return;
    }
    // log the job in
    jobManager.log(job, replyTo);
    // if control is for a Data object, load it into this tab
    if(Data.class.isAssignableFrom(control.getObjectClass())) {
      Data data = (Data) control.loadObject(null, true, true);
      loadData(data, replaceColumnsWithMatchingNames);
      jobManager.associate(job, dataManager);
      refreshGUI();
    }
    // if control is for DataToolTab class, load this tab from control
    else if(DataToolTab.class.isAssignableFrom(control.getObjectClass())) {
      control.loadObject(this);
      refreshGUI();
    }
  }

  /**
   * Adds a fit function. UserFunctions can optionally be added to the fit builder.
   *
   * @param f the fit function to add
   * @param addToFitBuilder true to add a UserFunction to the fit builder
   */
  public void addFitFunction(KnownFunction f, boolean addToFitBuilder) {
    curveFitter.addFitFunction(f, addToFitBuilder);
  }

  /**
   * Clears all data.
   */
  public void clearData() {
    ArrayList<String> colNames = new ArrayList<String>();
    for(Dataset next : dataManager.getDatasets()) {
      colNames.add(next.getYColumnName());
    }
    dataTable.setSelectedColumnNames(colNames);
    dataTable.deleteSelectedColumns(); // also posts undoable edits
  }

  /**
   * Sets the replaceColumnsWithMatchingNames flag.
   *
   * @param replace true to replace columns with same name but different ID
   */
  public void setReplaceColumnsWithMatchingNames(boolean replace) {
  	replaceColumnsWithMatchingNames = replace;
  }

  // _______________________ protected & private methods __________________________

  /**
   * Replaces spaces with underscores in a name.
   *
   * @param name the name with spaces
   * @return the name with underscores
   */
  protected String replaceSpacesWithUnderscores(String name) {
    name.trim();
    int n = name.indexOf(" "); //$NON-NLS-1$
    while(n>-1) {
      name = name.substring(0, n)+"_"+name.substring(n+1); //$NON-NLS-1$
      n = name.indexOf(" ");                               //$NON-NLS-1$
    }
    return name;
  }

  /**
   * Refreshes the data builder.
   */
  protected void refreshDataBuilder() {
    if(dataTool!=null) {
      dataTool.refreshDataBuilder();
      return;
    }
    if(dataBuilder==null) {
      return;
    }
    if(dataBuilder.getPanel(getName())==null) {
      FunctionPanel panel = new DataFunctionPanel(dataManager);
      dataBuilder.addPanel(getName(), panel);
    }
    for(String name : dataBuilder.panels.keySet()) {
      if(!name.equals(getName())) {
        dataBuilder.removePanel(name);
      }
    }
  }

  /**
   * Sets the font level.
   *
   * @param level the level
   */
  protected void setFontLevel(int level) {
    if(fontLevel==level) {
      return;
    }
    fontLevel = level;
    plot.setFontLevel(level);
    FontSizer.setFonts(statsTable, level);
    FontSizer.setFonts(propsTable, level);
    curveFitter.setFontLevel(level);
    double factor = FontSizer.getFactor(level);
    plot.getAxes().resizeFonts(factor, plot);
    FontSizer.setFonts(plot.getPopupMenu(), level);
    if(propsTable.styleDialog!=null) {
      FontSizer.setFonts(propsTable.styleDialog, level);
      propsTable.styleDialog.pack();
    }
    if(dataBuilder!=null) {
      dataBuilder.setFontLevel(fontLevel);
    }
    Runnable runner = new Runnable() {
      public void run() {
        fitterAction.actionPerformed(null);
        propsAndStatsAction.actionPerformed(null);
        propsTable.refreshTable();
		    refreshStatusBar();
      }

    };
    SwingUtilities.invokeLater(runner);
  }

  /**
   * Sets the tabChanged flag.
   *
   * @param changed true if tab is changed
   */
  protected void tabChanged(boolean changed) {
    tabChanged = changed;
  }

  /**
   * Gets the working dataset.
   *
   * @return the first two data columns in the datatable (x-y order)
   */
  protected WorkingDataset getWorkingData() {
    dataTable.getSelectedData();
    return dataTable.workingData;
  }

  /**
   * Returns a column name that is unique to this tab, contains
   * no spaces, and is not reserved by the OSP parser.
   *
   * @param d the dataset
   * @param proposed the proposed name for the column
   * @param askUser true to ask user to approve changes
   * @return unique name
   */
  protected String getUniqueYColumnName(Dataset d, String proposed, boolean askUser) {
    if(proposed==null) {
      return null;
    }
    // remove all spaces
    proposed = proposed.replaceAll(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
    boolean containsOperators = containsOperators(proposed);
    // check for duplicate or reserved names
    if(askUser || containsOperators) {
      int tries = 0, maxTries = 3;
      while(tries<maxTries) {
        tries++;
        if(isDuplicateName(d, proposed)) {       	
          Object response = JOptionPane.showInputDialog(this, "\""+proposed+"\" "+       //$NON-NLS-1$ //$NON-NLS-2$
            ToolsRes.getString("DataFunctionPanel.Dialog.DuplicateName.Message"), //$NON-NLS-1$
            ToolsRes.getString("DataFunctionPanel.Dialog.DuplicateName.Title"),   //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE, null, null,
            proposed);
          proposed = (response==null)? null: response.toString();
        }
        if((proposed==null)||proposed.equals("")) {                               //$NON-NLS-1$
          return null;
        }
        if(isReservedName(proposed)) {
          Object response = JOptionPane.showInputDialog(this, "\""+proposed+"\" "+       //$NON-NLS-1$ //$NON-NLS-2$
            ToolsRes.getString("DataToolTab.Dialog.ReservedName.Message"),        //$NON-NLS-1$
            ToolsRes.getString("DataToolTab.Dialog.ReservedName.Title"),          //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE, null, null,
            proposed);
          proposed = (response==null)? null: response.toString();
        }
        if((proposed==null)||proposed.equals("")) {                               //$NON-NLS-1$
          return null;
        }
        containsOperators = containsOperators(proposed);
        if(containsOperators) {
        	Object response = JOptionPane.showInputDialog(this, 
            ToolsRes.getString("DataToolTab.Dialog.OperatorInName.Message"),      //$NON-NLS-1$
            ToolsRes.getString("DataToolTab.Dialog.OperatorInName.Title"),        //$NON-NLS-1$
            JOptionPane.WARNING_MESSAGE, null, null,
            proposed);
          proposed = (response==null)? null: response.toString();
        }
        if((proposed==null)||proposed.equals("")) {                               //$NON-NLS-1$
          return null;
        }
      }
    }
    if (containsOperators) return null;
    int i = 0;
    // trap for names that are numbers
    try {
      Double.parseDouble(proposed);
      proposed = ToolsRes.getString("DataToolTab.NewColumn.Name"); //$NON-NLS-1$
    } catch(NumberFormatException ex) {}
    // remove existing number subscripts, if any, from duplicate names
    boolean subscriptRemoved = false;
    if(isDuplicateName(d, proposed)) {
      String subscript = TeXParser.getSubscript(proposed);
      try {
        i = Integer.parseInt(subscript);
        proposed = TeXParser.removeSubscript(proposed);
        subscriptRemoved = true;
      } catch(Exception ex) {}
    }
    String name = proposed;
    while(subscriptRemoved||isDuplicateName(d, name)||isReservedName(name)) {
      i++;
      name = TeXParser.addSubscript(proposed, String.valueOf(i));
      subscriptRemoved = false;
    }
    return name;
  }

  /**
   * Returns true if name is a duplicate of an existing dataset.
   *
   * @param d the dataset
   * @param name the proposed name for the dataset
   * @return true if duplicate
   */
  protected boolean isDuplicateName(Dataset d, String name) {
    if(dataManager.getDatasets().isEmpty()) {
      return false;
    }
    if(dataManager.getDataset(0).getXColumnName().equals(name)) {
      return true;
    }
    name = TeXParser.removeSubscripting(name);
    Iterator<Dataset> it = dataManager.getDatasets().iterator();
    while(it.hasNext()) {
      Dataset next = it.next();
      if(next==d) {
        continue;
      }
      String s = TeXParser.removeSubscripting(next.getYColumnName());
      if(s.equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if name is reserved by the OSP parser.
   *
   * @param name the proposed name
   * @return true if reserved
   */
  protected boolean isReservedName(String name) {
    // check for parser terms
    String[] s = FunctionTool.parserNames;
    for(int i = 0; i<s.length; i++) {
      if(s[i].equals(name)) {
        return true;
      }
    }
    // check for localized "row" name
    if(DataTable.rowName.equals(name)) {
      return true;
    }
    // check for dummy variables
    s = UserFunction.dummyVars;
    for(int i = 0; i<s.length; i++) {
      if(s[i].equals(name)) {
        return true;
      }
    }
    // check for numbers
    try {
      Double.parseDouble(name);
      return true;
    } catch(NumberFormatException ex) {}
    return false;
  }

  /**
   * Determines if the name contains any FunctionTool.parserOperators.
   *
   * @param name the name
   * @return true if the name contains one or more operators
   */
  protected boolean containsOperators(String name) {
    for (String next: FunctionTool.parserOperators) {
      if (name.indexOf(next)>-1) return true;
    }
    return false;
  }

  /**
   * Responds to a changed column name.
   *
   * @param oldName the previous name
   * @param newName the new name
   */
  protected void columnNameChanged(String oldName, String newName) {
    tabChanged(true);
    varPopup = null;
    String pattern = dataTable.getFormatPattern(oldName);
    dataTable.removeWorkingData(oldName);
    dataTable.getWorkingData(newName);
    dataTable.setFormatPattern(newName, pattern);
    if((propsTable.styleDialog!=null)&&propsTable.styleDialog.isVisible()&&propsTable.styleDialog.getName().equals(oldName)) {
      propsTable.styleDialog.setName(newName);
      String title = ToolsRes.getString("DataToolPropsTable.Dialog.Title"); //$NON-NLS-1$
      String var = TeXParser.removeSubscripting(newName);
      propsTable.styleDialog.setTitle(title+" \""+var+"\"");                //$NON-NLS-1$ //$NON-NLS-2$
    }
    statsTable.refreshStatistics();
    Dataset working = getWorkingData();
    if(working==null) {
      return;
    }
    refreshPlot();
  }

  /**
   * Creates a new empty DataColumn.
   *
   * @return the column
   */
  protected DataColumn createDataColumn() {
    Color markerColor = DisplayColors.getMarkerColor(colorIndex);
    Color lineColor = DisplayColors.getLineColor(colorIndex);
    if(!dataManager.getDatasets().isEmpty()) {
      colorIndex++;
    }
    DataColumn column = new DataColumn();
    column.setMarkerColor(markerColor);
    column.setLineColor(lineColor);
    column.setConnected(false);
    int rowCount = Math.max(1, dataTable.getRowCount());
    double[] y = new double[rowCount];
    Arrays.fill(y, Double.NaN);
    column.setPoints(y);
    column.setXColumnVisible(false);
    return column;
  }

  /**
   * Saves the selected table data to a file.
   *
   * @return the path of the saved file or null if failed
   */
  protected String saveTableDataToFile() {
    String tabName = getName();
    OSPLog.finest("saving tabe data from "+tabName); //$NON-NLS-1$
    JFileChooser chooser = OSPRuntime.getChooser();
    chooser.setSelectedFile(new File(tabName+".txt")); //$NON-NLS-1$
    int result = chooser.showSaveDialog(this);
    if(result==JFileChooser.APPROVE_OPTION) {
      OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
      String fileName = chooser.getSelectedFile().getAbsolutePath();
      fileName = XML.getRelativePath(fileName);
      String data = getSelectedTableData();
      return DataTool.write(data, fileName);
    }
    return null;
  }

  /**
   * Copies the selected table data to the clipboard.
   */
  protected void copyTableDataToClipboard() {
    OSPLog.finest("copying table data from "+getName()); //$NON-NLS-1$
    DataTool.copy(getSelectedTableData());
  }

  /**
   * Gets the table cells selected by the user.
   * The tab name and column names precede the data.
   * Data rows are delimited by new lines ("\n"), columns by tabs.
   *
   * @return a String containing the data.
   */
  protected String getSelectedTableData() {
    StringBuffer buf = new StringBuffer();
    if(getName()!=null) {
      buf.append(getName()+"\n"); //$NON-NLS-1$
    }
    if((dataTable.getColumnCount()==1)||(dataTable.getRowCount()==0)) {
      return buf.toString();
    }
    dataTable.clearSelectionIfEmptyEndRow();
    // get selected rows and columns
    int[] rows = dataTable.getSelectedRows();
    // if no rows selected, select all
    if(rows.length==0) {
      dataTable.selectAll();
      rows = dataTable.getSelectedRows();
    }
    int[] columns = dataTable.getSelectedColumns();
    // copy column headings
    for(int j = 0; j<columns.length; j++) {
      int col = columns[j];
      // ignore row heading
      int modelCol = dataTable.convertColumnIndexToModel(col);
      if(dataTable.isRowNumberVisible()&&(modelCol==0)) {
        continue;
      }
      buf.append(dataTable.getColumnName(col));
      buf.append("\t"); // tab after each column //$NON-NLS-1$
    }
    buf.setLength(buf.length()-1); // remove last tab
    buf.append("\n");              //$NON-NLS-1$
    java.text.DateFormat df = java.text.DateFormat.getInstance();
    for(int i = 0; i<rows.length; i++) {
      for(int j = 0; j<columns.length; j++) {
        int col = columns[j];
        int modelCol = dataTable.convertColumnIndexToModel(col);
        // don't copy row numbers
        if(dataTable.isRowNumberVisible()&&(modelCol==0)) {
          continue;
        }
        Object value = dataTable.getValueAt(rows[i], col);
        if(value!=null) {
          if(value instanceof java.util.Date) {
            value = df.format(value);
          }
          buf.append(value);
        }
        buf.append("\t");            // tab after each column //$NON-NLS-1$
      }
      buf.setLength(buf.length()-1); // remove last tab
      buf.append("\n");              // new line after each row //$NON-NLS-1$
    }
    return buf.toString();
  }

  /**
   * Creates the GUI.
   */
  protected void createGUI() {
    ToolsRes.addPropertyChangeListener("locale", new PropertyChangeListener() { //$NON-NLS-1$
      public void propertyChange(PropertyChangeEvent e) {
        refreshGUI();
      }

    });
    setLayout(new BorderLayout());
    splitPanes = new JSplitPane[3];
    // splitPanes[0] is plot/fitter on left, tables on right
    splitPanes[0] = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPanes[0].setResizeWeight(1);
    splitPanes[0].setOneTouchExpandable(true);
    // splitPanes[1] is plot on top, fitter on bottom
    splitPanes[1] = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPanes[1].setResizeWeight(1);
    splitPanes[1].setDividerSize(0);
    // splitPanes[2] is stats/props tables on top, data table on bottom
    splitPanes[2] = new JSplitPane(JSplitPane.VERTICAL_SPLIT) {
    	public Dimension getPreferredSize() {
    		Dimension dim = super.getPreferredSize();
    		dim.width = dataTable.getMinimumTableWidth()+6;
    		JScrollBar scrollbar = dataScroller.getVerticalScrollBar();
    		if (scrollbar.isVisible()) {
    			dim.width += scrollbar.getWidth();
    		}
    		dim.height = 10;
    		return dim;
    	}
    };
    splitPanes[2].setDividerSize(0);
    splitPanes[2].setEnabled(false);
    // add ancestor listener to initialize
    this.addAncestorListener(new AncestorListener() {
      public void ancestorAdded(AncestorEvent e) {
        OSPLog.getOSPLog(); // workaround needed for consistent initialization!
        if(getSize().width>0) {
          init();
        }
      }
      public void ancestorRemoved(AncestorEvent event) {}
      public void ancestorMoved(AncestorEvent event) {}

    });
    // add component listener for resizing
    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        fitterAction.actionPerformed(null);
      }

    });
    // add window listener to dataTool to display curvefitter properly
		dataTool.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowOpened(java.awt.event.WindowEvent e) {
        fitterAction.actionPerformed(null);
      }
    });
    // configure data table
    dataTable.setRowNumberVisible(true);
    dataScroller = new JScrollPane(dataTable);
    dataTable.refreshTable();
    dataScroller.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
      	dataTable.clearSelection();
      }
    });
    dataScroller.setToolTipText(ToolsRes.getString("DataToolTab.Scroller.Tooltip")); //$NON-NLS-1$
    dataTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
      public void columnAdded(TableColumnModelEvent e) {}
      public void columnRemoved(TableColumnModelEvent e) {}
      public void columnSelectionChanged(ListSelectionEvent e) {}
      public void columnMarginChanged(ChangeEvent e) {}
      public void columnMoved(TableColumnModelEvent e) {
        Dataset prev = dataTable.workingData;
        Dataset working = getWorkingData();
        if(working!=prev && dataTool.fitBuilder!=null) {
          tabChanged(true);
        }
        if((working==null)||(working==prev)) {
          return;
        }
        plot.selectionBox.setSize(0, 0);
        refreshPlot();
      }

    });
    // create bottom pane action, fit and fourier checkboxes
    fitterAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if(fitterCheckbox==null) {
          return;
        }
        // remove curveFitter
        splitPanes[1].remove(curveFitter);
        splitPanes[1].setDividerSize(splitPanes[2].getDividerSize());
        splitPanes[1].setDividerLocation(1.0);
        plot.removeDrawables(FunctionDrawer.class);
        // restore if fit checkbox is checked
        boolean fitterVis = fitterCheckbox.isSelected();
        splitPanes[1].setEnabled(fitterVis);
        curveFitter.setActive(fitterVis);
        if(fitterVis) {
          splitPanes[1].setBottomComponent(curveFitter);
          splitPanes[1].setDividerSize(splitPanes[0].getDividerSize());
          splitPanes[1].setDividerLocation(-1);
          plot.addDrawable(curveFitter.getDrawer());
        }
        if(e!=null) {
          refreshPlot();
        }
      }

    };    
    fitterCheckbox = new JCheckBoxMenuItem();
    fitterCheckbox.setSelected(false);
//    fitterCheckbox.setOpaque(false);
    fitterCheckbox.addActionListener(fitterAction);
    fourierCheckbox = new JCheckBoxMenuItem();
    fourierCheckbox.setSelected(false);
//    fourierCheckbox.setOpaque(false);
    fourierCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	if (fourierPanel==null && dataTool!=null) {
          // create fourier panel
          fourierPanel = new FourierPanel();
          fourierDialog = new JDialog(dataTool, false) {
          	public void setVisible(boolean vis) {
          		super.setVisible(vis);
          		fourierCheckbox.setSelected(vis);
          	}
          };
          fourierDialog.setContentPane(fourierPanel);
          Dimension dim = new Dimension(640, 400);
          fourierDialog.setSize(dim);
          fourierPanel.splitPane.setDividerLocation(dim.width/2);
          fourierPanel.refreshFourierData(dataTable.getSelectedData(), DataToolTab.this.getName());
          fourierDialog.setLocationRelativeTo(dataTool);
      	}
      	fourierDialog.setVisible(fourierCheckbox.isSelected());
      }

    });
    // create newColumnButton button
    newColumnButton = DataTool.createButton(""); //$NON-NLS-1$
    newColumnButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        DataColumn column = createDataColumn();
        String proposed = ToolsRes.getString("DataToolTab.NewColumn.Name");                                                       //$NON-NLS-1$
        proposed = getUniqueYColumnName(column, proposed, false);
        Object input = JOptionPane.showInputDialog(DataToolTab.this, ToolsRes.getString("DataToolTab.Dialog.NameColumn.Message"), //$NON-NLS-1$
          ToolsRes.getString("DataToolTab.Dialog.NameColumn.Title"),         //$NON-NLS-1$
            JOptionPane.QUESTION_MESSAGE, null, null, proposed);
        if(input==null) {
          return;
        }
        String newName = getUniqueYColumnName(column, input.toString(), true);
        if(newName==null) {
          return;
        }
        if(newName.equals("")) {                                             //$NON-NLS-1$
          String colName = ToolsRes.getString("DataToolTab.NewColumn.Name"); //$NON-NLS-1$
          newName = getUniqueYColumnName(column, colName, false);
        }
        OSPLog.finer("adding new column \""+newName+"\"");                   //$NON-NLS-1$ //$NON-NLS-2$
        column.setXYColumnNames("row", newName);                             //$NON-NLS-1$
        ArrayList<DataColumn> loadedColumns = loadData(column, false);
        if(!loadedColumns.isEmpty()) {
          for(DataColumn next : loadedColumns) {
            next.deletable = true;
          }
        }
        int col = dataTable.getColumnCount()-1;
        // post edit: target is column, value is dataset
        TableEdit edit = dataTable.new TableEdit(DataToolTable.INSERT_COLUMN_EDIT, newName, new Integer(col), column);
        undoSupport.postEdit(edit);
        dataTable.refreshUndoItems();
        Runnable runner = new Runnable() {
          public synchronized void run() {
            int col = dataTable.getColumnCount()-1;
            dataTable.changeSelection(0, col, false, false);
            dataTable.editCellAt(0, col, e);
            dataTable.editor.field.requestFocus();
          }

        };
        SwingUtilities.invokeLater(runner);
      }

    });
    // create dataBuilderButton
    dataBuilderButton = DataTool.createButton(ToolsRes.getString("DataToolTab.Button.DataBuilder.Text")); //$NON-NLS-1$
    dataBuilderButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Tooltip")); //$NON-NLS-1$
    dataBuilderButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        getDataBuilder().setSelectedPanel(getName());
        getDataBuilder().setVisible(true);
      }

    });
    // create refreshDataButton
    refreshDataButton = DataTool.createButton(ToolsRes.getString("DataToolTab.Button.Refresh.Text")); //$NON-NLS-1$
    refreshDataButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.Refresh.Tooltip")); //$NON-NLS-1$
    refreshDataButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	refreshData();
      }

    });
    // create help button
    helpButton = DataTool.createButton(ToolsRes.getString("Tool.Button.Help")); //$NON-NLS-1$
    helpButton.setToolTipText(ToolsRes.getString("Tool.Button.Help.ToolTip")); //$NON-NLS-1$
    helpButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if(dataTool!=null) {
          DataTool.showHelp(DataTool.helpName);
        } else {
          DataTool.showHelp(DataToolTab.helpName);
        }
      }

    });
    // create valueCheckbox
    valueCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("DataToolTab.Checkbox.Position")); //$NON-NLS-1$
    valueCheckbox.setSelected(false);
//    valueCheckbox.setOpaque(false);
    valueCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Position.Tooltip")); //$NON-NLS-1$
    valueCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        positionVisible = valueCheckbox.isSelected();
        plot.setMessage(plot.createMessage());
        plot.repaint();
      }

    });
    // create slopeCheckbox
    slopeCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("DataToolTab.Checkbox.Slope")); //$NON-NLS-1$
    slopeCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Slope.Tooltip")); //$NON-NLS-1$
    slopeCheckbox.setSelected(false);
//    slopeCheckbox.setOpaque(false);
    slopeCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        slopeVisible = slopeCheckbox.isSelected();
        plot.setMessage(plot.createMessage());
        plot.repaint();
      }

    });
    // create areaCheckbox
    areaCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("DataToolTab.Checkbox.Area")); //$NON-NLS-1$
    areaCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Area.Tooltip")); //$NON-NLS-1$
    areaCheckbox.setSelected(false);
//    areaCheckbox.setOpaque(false);
    areaCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        plot.setAreaVisible(areaCheckbox.isSelected());
      }

    });
    // create measureButton
    measureButton = DataTool.createButton(ToolsRes.getString("DataToolTab.Button.Measure.Label")); //$NON-NLS-1$
    measureButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
		  	// build a popup menu with measure items
		    JPopupMenu popup = new JPopupMenu();
		    popup.add(valueCheckbox);
		    popup.add(slopeCheckbox);
		    popup.add(areaCheckbox);
		    popup.show(measureButton, 0, measureButton.getHeight());       		
      }
    });
    // create analyzeButton
    analyzeButton = DataTool.createButton(ToolsRes.getString("DataToolTab.Button.Analyze.Label")); //$NON-NLS-1$
    analyzeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
		  	// build a popup menu with analyze items
		    JPopupMenu popup = new JPopupMenu();
		    popup.add(statsCheckbox);
		    popup.add(fitterCheckbox);
		    popup.add(fourierCheckbox);
		    popup.show(analyzeButton, 0, analyzeButton.getHeight());       		
      }
    });

    // create propsAndStatsAction
    propsAndStatsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        boolean statsVis = statsCheckbox.isSelected();
        boolean propsVis = propsCheckbox.isSelected();
        if(statsVis) {
          statsTable.refreshStatistics();
        }
        refreshStatusBar();
        int statsHeight = statsTable.getPreferredSize().height;
        int propsHeight = propsTable.getPreferredSize().height;
        LookAndFeel currentLF = UIManager.getLookAndFeel();
        int h = (currentLF.getClass().getName().indexOf("Nimbus")>-1) //$NON-NLS-1$
                ? 8 : 4;
        if(statsVis&&propsVis) {
          Box box = Box.createVerticalBox();
          box.add(statsScroller);
          box.add(propsScroller);
          splitPanes[2].setTopComponent(box);
          splitPanes[2].setDividerLocation(statsHeight+propsHeight+2*h);
        } else if(statsVis) {
          splitPanes[2].setTopComponent(statsScroller);
          splitPanes[2].setDividerLocation(statsHeight+h);
        } else if(propsVis) {
          splitPanes[2].setTopComponent(propsScroller);
          splitPanes[2].setDividerLocation(propsHeight+h);
        } else {
          splitPanes[2].setDividerLocation(0);
        }
      }

    };
    // create stats checkbox
    statsCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("Checkbox.Statistics.Label"), false); //$NON-NLS-1$
//    statsCheckbox.setOpaque(false);
    statsCheckbox.setToolTipText(ToolsRes.getString("Checkbox.Statistics.ToolTip")); //$NON-NLS-1$
    statsCheckbox.addActionListener(propsAndStatsAction);
    // create style properties checkbox
    propsCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("DataToolTab.Checkbox.Properties.Text"), true); //$NON-NLS-1$
    propsCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Properties.Tooltip")); //$NON-NLS-1$
//    propsCheckbox.setOpaque(false);
    propsCheckbox.addActionListener(propsAndStatsAction);
    // create plotting panel and axes
    plot = new DataToolPlotter(getWorkingData());
    plotAxes = new DataToolAxes(plot);
    plot.setAxes(plotAxes);
    if(getWorkingData()!=null) {
      plot.addDrawable(getWorkingData());
      plot.setTitle(getWorkingData().getName());
    }
    // create mouse listener for selecting data points in plot
    MouseInputListener mouseSelector = new MouseInputAdapter() {
      Set<Integer> rowsInside = new HashSet<Integer>(); // points inside selectionBox
      Set<Integer> recent = new HashSet<Integer>();     // points recently added or removed
      boolean boxActive;
      Interactive ia;
      public void mousePressed(MouseEvent e) {
        ia = plot.getInteractive();
        // add or remove point if Interactive is dataset
        if(ia instanceof HighlightableDataset) {
          HighlightableDataset data = (HighlightableDataset) ia;
          int index = data.getHitIndex();
          ListSelectionModel model = dataTable.getColumnModel().getSelectionModel();
          int col = dataTable.getXColumn();
          model.setSelectionInterval(col, col);
          col = dataTable.getYColumn();
          model.addSelectionInterval(col, col);
          TableModel tableModel = dataTable.getModel();
          for(int i = 1; i<tableModel.getColumnCount(); i++) {
            if(data.getYColumnName().equals(dataTable.getColumnName(i))) {
              model.addSelectionInterval(i, i);
              if (col!=i)
              	data.setHighlightColor(data.getFillColor());
              data.setHighlighted(index, true);
              break;
            }
          }
          if(!e.isControlDown()) {
            dataTable.setSelectedModelRows(new int[] {index});
          } else {
            int[] rows = dataTable.getSelectedModelRows();
            boolean needsAdding = true;
            for(int row : rows) {
              if(row==index) {
                needsAdding = false;
              }
            }
            int[] newRows = new int[needsAdding ? rows.length+1 : rows.length-1];
            if(needsAdding) {
              System.arraycopy(rows, 0, newRows, 0, rows.length);
              newRows[rows.length] = index;
            } else {
              int j = 0;
              for(int row : rows) {
                if(row==index) {
                  continue;
                }
                newRows[j] = row;
                j++;
              }
            }
            dataTable.setSelectedModelRows(newRows);
          }
          dataTable.getSelectedData();
          plot.repaint();
          boxActive = false;
          return;
        } else if(ia!=null) {
          boxActive = false;
          return;
        }
        boxActive = !OSPRuntime.isPopupTrigger(e);
        if(boxActive) {
          // prepare to drag
          if(!(e.isControlDown()||e.isShiftDown())) {
            dataTable.clearSelection();
          }
          // prefill rowsInside with currently selected rows
          rowsInside.clear();
          for(int row : dataTable.getSelectedModelRows()) {
            rowsInside.add(new Integer(row));
          }
          recent.clear();
          Point p = e.getPoint();
          plot.selectionBox.xstart = p.x;
          plot.selectionBox.ystart = p.y;
        }
      }
      public void mouseDragged(MouseEvent e) {
        if(!boxActive) {
          return;
        }
        Dataset data = getWorkingData();
        if(data==null) {
          return;
        }
        Point mouse = e.getPoint();
        plot.selectionBox.visible = true;
        plot.selectionBox.setSize(mouse.x-plot.selectionBox.xstart, mouse.y-plot.selectionBox.ystart);
        // look for data points inside box
        double[] xpoints = data.getXPoints();
        double[] ypoints = data.getYPoints();
        for(int i = 0; i<xpoints.length; i++) {
          double xp = plot.xToPix(xpoints[i]);
          double yp = plot.yToPix(ypoints[i]);
          Integer index = dataTable.workingRows.get(new Integer(i));
          int row = index.intValue();
          // if a data point is inside the box, add it
          if(plot.selectionBox.contains(xp, yp)) {
            if(!rowsInside.contains(index)&&!recent.contains(index)) {
              if(rowsInside.isEmpty()) {
                ListSelectionModel model = dataTable.getColumnModel().getSelectionModel();
                int col = dataTable.getXColumn();
                model.setSelectionInterval(col, col);
                col = dataTable.getYColumn();
                model.addSelectionInterval(col, col);
              }
              rowsInside.add(index);
              recent.add(index);
              dataTable.getSelectionModel().addSelectionInterval(row, row);
            }
          }
          // if a previously added data point is outside the box, remove it
          else if(rowsInside.contains(index)&&recent.contains(index)) { // point should be removed
            dataTable.getSelectionModel().removeSelectionInterval(row, row);
            rowsInside.remove(index);
            recent.remove(index);
            dataTable.getSelectionModel().removeSelectionInterval(row, row);
            if(rowsInside.isEmpty()) {
              dataTable.getColumnModel().getSelectionModel().removeSelectionInterval(0, dataTable.getColumnCount()-1);
            }
          }
        }
        data = dataTable.getSelectedData();
        plot.repaint();
      }
      public void mouseReleased(MouseEvent e) {
        plot.selectionBox.visible = false;
        if(ia!=null) {
          if(ia instanceof Selectable) {
            plot.setMouseCursor(((Selectable) ia).getPreferredCursor());
          } else {
            plot.setMouseCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }
          if(ia instanceof HighlightableDataset) {
            HighlightableDataset data = (HighlightableDataset) ia;
	          TableModel tableModel = dataTable.getModel();
            int yCol = dataTable.getYColumn();
	          for(int i = 1; i<tableModel.getColumnCount(); i++) {
	            if(data.getYColumnName().equals(dataTable.getColumnName(i))
	            		&& yCol!=i) {
	              data.clearHighlights();
	              data.setHighlightColor(Color.YELLOW);
	              ListSelectionModel model = dataTable.getColumnModel().getSelectionModel();
	              model.removeSelectionInterval(i, i);
	              break;
	            }
	          }
          }
        }
        plot.repaint();
      }
      public void mouseMoved(MouseEvent e) {
      	HighlightableDataset data = dataTable.workingData;
        ia = plot.getInteractive();
        if(ia instanceof HighlightableDataset) {
          data = (HighlightableDataset) ia;
        }
        if (data!=null)
        	plot.setCoordinateLabels(data.getXColumnName(), data.getYColumnName());
        plot.slope = plot.value = Double.NaN;
        double[] xpoints = null;
        double[] ypoints = null;
        int j = -1;
        if(positionVisible||slopeVisible||areaVisible) {
          if(data==null) {
            return;
          }
          if(data.getIndex()>0) {
            double x = plot.pixToX(e.getX());
            j = plot.findIndexNearestX(x, data);
          }
          xpoints = data.getXPoints();
          ypoints = data.getYPoints();
        }
        if(positionVisible&&(j>-1)) {
          plot.value = ypoints[j];
          plot.crossbars.x = xpoints[j];
          plot.crossbars.y = ypoints[j];
          plot.xVar = data.getXColumnName();
          plot.yVar = data.getYColumnName();
        }
        if(slopeVisible&&(j>0)&&(j<data.getIndex()-1)) {
          plot.slopeLine.x = xpoints[j];
          plot.slopeLine.y = ypoints[j];
          plot.slope = (ypoints[j+1]-ypoints[j-1])/(xpoints[j+1]-xpoints[j-1]);
        }
        if(positionVisible||slopeVisible||areaVisible) {
          plot.setMessage(plot.createMessage());
        }
        plot.repaint();
      }
    };
    plot.addMouseListener(mouseSelector);
    plot.addMouseMotionListener(mouseSelector);
    // create toolbar
    toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.setBorder(BorderFactory.createEtchedBorder());
//    toolbar.add(propsCheckbox);
    toolbar.add(measureButton);
    toolbar.add(analyzeButton);
//    toolbar.add(statsCheckbox);
//    toolbar.add(valueCheckbox);
//    toolbar.add(slopeCheckbox);
//    toolbar.add(areaCheckbox);
//    toolbar.addSeparator();
//    toolbar.add(fitterCheckbox);
//    toolbar.add(fourierCheckbox);
    toolbar.add(Box.createGlue());
    toolbar.add(newColumnButton);
    toolbar.add(dataBuilderButton);
    toolbar.add(refreshDataButton);
    toolbar.add(helpButton);
    // create curve fitter
    FitBuilder fitBuilder = dataTool.getFitBuilder();
    curveFitter = new DatasetCurveFitter(getWorkingData(), fitBuilder);
    curveFitter.setDataToolTab(this);
    fitBuilder.curveFitters.add(curveFitter);
    fitBuilder.removePropertyChangeListener(curveFitter.fitListener);
    fitBuilder.addPropertyChangeListener(curveFitter.fitListener);
    curveFitter.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if(e.getPropertyName().equals("changed")) { //$NON-NLS-1$
          tabChanged(true);
          return;
        }
        if(e.getPropertyName().equals("drawer")     //$NON-NLS-1$
          &&(fitterCheckbox!=null)&&fitterCheckbox.isSelected()) {
          plot.removeDrawables(FunctionDrawer.class);
          // add fit drawer to plot drawable
          plot.addDrawable((FunctionDrawer) e.getNewValue());
        }
        plot.repaint();
      }

    });
    // create statistics table
    statsTable = new DataToolStatsTable(dataTable);
    statsScroller = new JScrollPane(statsTable) {
      public Dimension getPreferredSize() {
        Dimension dim = statsTable.getPreferredSize();
        return dim;
      }

    };
    statsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    statsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    // create properties table
    propsTable = new DataToolPropsTable(dataTable);
    propsTable.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if(e.getPropertyName().equals("display")) { //$NON-NLS-1$
          refreshPlot();
        }
      }

    });
    propsScroller = new JScrollPane(propsTable) {
      public Dimension getPreferredSize() {
        Dimension dim = propsTable.getPreferredSize();
        return dim;
      }
    };
    propsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    propsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    statusLabel = new JLabel(" ", SwingConstants.LEADING); //$NON-NLS-1$
    statusLabel.setFont(new JTextField().getFont());
    statusLabel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
    editableLabel = new JLabel(" ", SwingConstants.TRAILING); //$NON-NLS-1$
    editableLabel.setFont(statusLabel.getFont());
    editableLabel.setBorder(BorderFactory.createEmptyBorder(1, 12, 1, 2));
    // assemble components
    add(toolbar, BorderLayout.NORTH);
    add(splitPanes[0], BorderLayout.CENTER);
    JPanel south = new JPanel(new BorderLayout());
    south.add(statusLabel, BorderLayout.WEST);
    south.add(editableLabel, BorderLayout.EAST);
    add(south, BorderLayout.SOUTH);
    tableScroller = new JScrollPane(splitPanes[2]);
    tableScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    splitPanes[0].setLeftComponent(splitPanes[1]);
    splitPanes[0].setRightComponent(tableScroller);
    splitPanes[1].setTopComponent(plot);
    splitPanes[1].setBottomComponent(curveFitter);
    splitPanes[2].setBottomComponent(dataScroller);
    splitPanes[0].setDividerLocation(0);
    curveFitter.splitPane.setDividerLocation(0);
    // set up the undo system
    undoManager = new UndoManager();
    undoSupport = new UndoableEditSupport();
    undoSupport.addUndoableEditListener(undoManager);
  }

  /**
   * Refreshes the GUI.
   */
  protected void refreshGUI() {
    Runnable runner = new Runnable() {
      public void run() {
        boolean changed = tabChanged;
        newColumnButton.setText(ToolsRes.getString("DataToolTab.Button.NewColumn.Text"));               //$NON-NLS-1$
        newColumnButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.NewColumn.Tooltip"));     //$NON-NLS-1$
        dataBuilderButton.setText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Text"));           //$NON-NLS-1$
        dataBuilderButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Tooltip")); //$NON-NLS-1$
        dataBuilderButton.setEnabled(originatorID!=0);
        refreshDataButton.setText(ToolsRes.getString("DataToolTab.Button.Refresh.Text"));               //$NON-NLS-1$
        refreshDataButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.Refresh.Tooltip"));     //$NON-NLS-1$
        measureButton.setText(ToolsRes.getString("DataToolTab.Button.Measure.Label"));               //$NON-NLS-1$
        measureButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.Measure.Tooltip"));     //$NON-NLS-1$
        analyzeButton.setText(ToolsRes.getString("DataToolTab.Button.Analyze.Label"));               //$NON-NLS-1$
        analyzeButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.Analyze.Tooltip"));     //$NON-NLS-1$
        statsCheckbox.setText(ToolsRes.getString("Checkbox.Statistics.Label"));                         //$NON-NLS-1$
        statsCheckbox.setToolTipText(ToolsRes.getString("Checkbox.Statistics.ToolTip"));                //$NON-NLS-1$
        fitterCheckbox.setText(ToolsRes.getString("Checkbox.Fits.Label"));                          //$NON-NLS-1$
        fitterCheckbox.setToolTipText(ToolsRes.getString("Checkbox.Fits.ToolTip"));                 //$NON-NLS-1$
        fourierCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Fourier.Label"));                          //$NON-NLS-1$
        fourierCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Fourier.ToolTip"));                 //$NON-NLS-1$
        propsCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Properties.Text"));              //$NON-NLS-1$
        propsCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Properties.Tooltip"));    //$NON-NLS-1$
        valueCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Position"));                     //$NON-NLS-1$
        valueCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Position.Tooltip"));      //$NON-NLS-1$
        slopeCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Slope"));                        //$NON-NLS-1$
        slopeCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Slope.Tooltip"));         //$NON-NLS-1$
        areaCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Area"));                          //$NON-NLS-1$
        areaCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Area.Tooltip"));           //$NON-NLS-1$
        helpButton.setText(ToolsRes.getString("Tool.Button.Help"));                                     //$NON-NLS-1$
        helpButton.setToolTipText(ToolsRes.getString("Tool.Button.Help.ToolTip"));                      //$NON-NLS-1$
        toolbar.remove(newColumnButton);
        if(userEditable) {
          int n = toolbar.getComponentIndex(helpButton);
          toolbar.add(newColumnButton, n);
          toolbar.validate();
        }
        toolbar.remove(refreshDataButton);
        Collection<Tool> tools = jobManager.getTools(dataManager);
        for(Tool tool : tools) {
          if(tool instanceof DataRefreshTool) {
            int n = toolbar.getComponentIndex(helpButton);
            toolbar.add(refreshDataButton, n);
            toolbar.validate();
            break;
          }
        }
        curveFitter.refreshGUI();
        statsTable.refreshGUI();
        propsTable.refreshGUI();
        refreshPlot();
        refreshStatusBar();
        tabChanged = changed;
      }

    };
    if(SwingUtilities.isEventDispatchThread()) {
      runner.run();
    } else {
      SwingUtilities.invokeLater(runner);
    }
  }

  /**
   * Initializes this panel.
   */
  private void init() {
    if(isInitialized) {
      return;
    }
    if(splitPanes[0].getDividerLocation()<10) {
      splitPanes[0].setDividerLocation(0.7);
      curveFitter.splitPane.setDividerLocation(1.0);
    }
    splitPanes[1].setDividerLocation(1.0);
    propsAndStatsAction.actionPerformed(null);
    for(int i = 0; i<dataTable.getColumnCount(); i++) {
      String colName = dataTable.getColumnName(i);
      dataTable.getWorkingData(colName);
    }
    refreshPlot();
    refreshGUI();
    isInitialized = true;
  }

  private void buildVarPopup() {
    if(setVarAction==null) {
      // create action to set axis variable
      setVarAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          JMenuItem item = (JMenuItem) e.getSource();
          // get desired variable for targeted axis
          String var = item.getActionCommand();
          // get current variable on other axis
          String otherVar = isHorzVarPopup ? plot.yVar : plot.xVar;
          // get current label column
          int labelCol = dataTable.convertColumnIndexToView(0);
          // find specified variable and move to x or y column
          int col = isHorzVarPopup ? dataTable.getXColumn() : dataTable.getYColumn();
          TableModel model = dataTable.getModel();
          for(int i = 0; i<model.getColumnCount(); i++) {
            if(var.equals(dataTable.getColumnName(i))) {
              if(i==col) {
                return; // no change
              }
              dataTable.getColumnModel().moveColumn(i, col);
              break;
            }
          }
          // restore other variable if needed
          if(!var.equals(otherVar)) {
            col = isHorzVarPopup ? dataTable.getYColumn() : dataTable.getXColumn();
            for(int i = 0; i<model.getColumnCount(); i++) {
              if(otherVar.equals(dataTable.getColumnName(i))) {
                dataTable.getColumnModel().moveColumn(i, col);
                break;
              }
            }
          }
          // restore labels
          col = dataTable.convertColumnIndexToView(0);
          dataTable.getColumnModel().moveColumn(col, labelCol);
        }

      };
    }
    varPopup = new JPopupMenu();
    Font font = new JTextField().getFont();
    for(Dataset next : dataManager.getDatasets()) {
      String s = TeXParser.removeSubscripting(next.getYColumnName());
      JMenuItem item = new JMenuItem(s);
      item.setActionCommand(next.getYColumnName());
      item.addActionListener(setVarAction);
      item.setFont(font);
      varPopup.add(item);
    }
  }

  /**
   * Returns the column with matching ID and columnID in the specified list.
   * May return null.
   *
   * @param local the Dataset to match
   * @param columnsToSearch the Datasets to search
   * @return the matching Dataset, if any
   */
  private DataColumn getIDMatch(Dataset local, ArrayList<DataColumn> columnsToSearch) {
    if((columnsToSearch==null)||(local==null)) {
      return null;
    }
    for(Iterator<DataColumn> it = columnsToSearch.iterator(); it.hasNext(); ) {
      DataColumn next = it.next();
      // next is match if has same ID and columnID
      if((local.getID()==next.getID())&&(local.getColumnID()==next.getColumnID())) {
        return next;
      }
    }
    return null;
  }

  /**
   * Returns the column with matching name in the specified list.
   * May return null.
   *
   * @param local the Dataset to match
   * @param columnsToSearch the Datasets to search
   * @return the matching Dataset, if any
   */
  private DataColumn getNameMatch(Dataset local, ArrayList<DataColumn> columnsToSearch) {
    if((columnsToSearch==null)||(local==null)) {
      return null;
    }
    for(Iterator<DataColumn> it = columnsToSearch.iterator(); it.hasNext(); ) {
      DataColumn next = it.next();
      // next is match if has same y-column name
      if(local.getYColumnName().equals(next.getYColumnName())) {
        return next;
      }
    }
    return null;
  }

  /**
   * Returns true if the name and data duplicate an existing column.
   *
   * @param name the name
   * @param data the data array
   * @return true if a duplicate is found
   */
  protected boolean isDuplicateColumn(String name, double[] data) {
    Iterator<Dataset> it = dataManager.getDatasets().iterator();
    while(it.hasNext()) {
      Dataset next = it.next();
      double[] y = next.getYPoints();
      if(name.equals(next.getYColumnName())&&isDuplicate(data, next.getYPoints())) {
        // next is duplicate column: add new points if any
        if(data.length>y.length) {
          next.clear();
          next.append(data, data);
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if two data arrays have identical values.
   *
   * @param data0 data array 0
   * @param data1 data array 1
   * @return true if identical
   */
  private boolean isDuplicate(double[] data0, double[] data1) {
    int len = Math.min(data0.length, data1.length);
    for(int i = 0; i<len; i++) {
      if(Double.isNaN(data0[i])&&Double.isNaN(data1[i])) {
        continue;
      }
      if(data0[i]!=data1[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if this tab is interested in a Data object.
   *
   * @param data the Data object
   * @return true if data is of interest
   */
  public boolean isInterestedIn(Data data) {
    if (data==null) return false;
    if (isOwnedBy(data)) return true;
    Collection<Tool> tools = jobManager.getTools(dataManager);
    for(Tool tool : tools) {
      if(tool instanceof DataRefreshTool) {
      	DataRefreshTool refresher = (DataRefreshTool)tool;
      	if (refresher.moreData.contains(data)) return true;
      }
    }
    return false;
  }
  
  /**
   * Sets DataColumn IDs to corresponding column owner IDs based on saved names.
   * Call this after loading this tab from XML to set column IDs to column owner IDs.
   * @param columnOwnerName the guest name
   * @param data the guest Data
   * @return true if any column IDs were changed
   */
  public boolean setOwnedColumnIDs(String columnOwnerName, Data data) {
		// only match column names associated with this column owner name
		Set<String> namesToMatch = new HashSet<String>();
		for (String colName: ownedColumns.keySet()) {
			String[] dataNames = ownedColumns.get(colName);
			if (dataNames!=null && dataNames[0].equals(columnOwnerName)) { 				
  			namesToMatch.add(colName);
			}
		}
  	Map<DataColumn, Dataset> matches = getColumnMatchesByName(namesToMatch, data);
		for (DataColumn column: matches.keySet()) {
			Dataset match = matches.get(column);
			column.setID(match.getID());
		}
		return !matches.isEmpty();
  }
  
  /**
   * Saves DataColumn names with associated column owner and Data object.
   * Call this before saving this tab so owned columns will be saved in XML.
   * @param columnOwnerName the guest name
   * @param data the guest Data
   */
  public void saveOwnedColumnNames(String columnOwnerName, Data data) {
  	Map<DataColumn, Dataset> matches = getColumnMatchesByID(data);
		for (DataColumn column: matches.keySet()) {
			Dataset match = matches.get(column);
			ownedColumns.put(column.getYColumnName(), new String[] {columnOwnerName, match.getYColumnName()});
		}
  }
  
  /**
   * Gets the column name for the first DataColumn with a given ID.
   * @param ID the ID number of the desired column
   * @return the tab column name, or null if not found
   */
  public String getColumnName(int ID) {
  	for (Dataset column: dataManager.getDatasets()) {
  		if (column.getID()==ID) return column.getYColumnName();
  	}
  	return null;
  }
  
  /**
   * Returns true if (a) the Data ID is this tab owner's ID
   * or (b) the Data name is this tab's name.
   *
   * @param data the Data object
   * @return true if data owns this tab
   */
  public boolean isOwnedBy(Data data) {
    if(data==null) return false;
    // return true if data name is the name of this tab
    String name = data.getName();
    if((name!=null)&&replaceSpacesWithUnderscores(name).equals(getName())) {
      return true;
    }
    // return true if data ID is the originator of this tab
    return data.getID()==originatorID;
  }
  
  /**
   * Sets the owner of this tab. This method is used before saving and after loading this tab
   * so the tab can refresh its data from a new owner.
   * @param name the owner name
   * @param data the owner Data
   */
  public void setOwner(String name, Data data) {
  	ownerName = name;
  	originatorID = data.getID();
  }
  
  /**
   * Gets the name of the owner of this tab. May return null, even if an owner exists.
   * @return the name of the owner
   */
  public String getOwnerName() {
  	return ownerName;
  }
  
  /**
   * Gets datasets matching columns by ID in this tab.
   * @param data Data object with datasets to match
   * @param columnNames optional set of column names
   * @return map of column to dataset
   */
  protected Map<DataColumn, Dataset> getColumnMatchesByID(Data data) {
  	Map<DataColumn, Dataset> matches = new HashMap<DataColumn, Dataset>();
    ArrayList<Dataset> datasets = DataTool.getDatasets(data);
  	for (Dataset next: dataManager.getDatasets()) {
  		if (next instanceof DataColumn) {
				DataColumn column = (DataColumn)next;
  			Dataset match = getMatchByID(column, datasets);
  			if (match!=null) {
  				matches.put(column, match);
  			}
  		}
  	}
  	return matches;
  }
  
  /**
   * Gets datasets matching columns by name in this tab.
   * @param columnNames set of column names
   * @param data Data object with datasets to match
   * @return map of column to dataset
   */
  protected Map<DataColumn, Dataset> getColumnMatchesByName(Set<String> columnNames, Data data) {
  	Map<DataColumn, Dataset> matches = new HashMap<DataColumn, Dataset>();
    ArrayList<Dataset> datasets = DataTool.getDatasets(data);
  	for (Dataset next: dataManager.getDatasets()) {
  		if (next instanceof DataColumn) {
				DataColumn column = (DataColumn)next;
				if (columnNames!=null && !columnNames.contains(column.getYColumnName()))
					continue;
  			Dataset match = getMatchByName(column, datasets);
  			if (match!=null) {
  				matches.put(column, match);
  			}
  		}
  	}
  	return matches;
  }
  
  /**
   * Gets a matching Dataset by name.
   * @param column the DataColumn to match
   * @param datasets the Datasets to search
   * @return the matching DAtaset
   */
  protected Dataset getMatchByName(DataColumn column, ArrayList<Dataset> datasets) {
  	// convert DataColumn name to dataset column name
  	String[] dataNames = ownedColumns.get(column.getYColumnName());
  	if (dataNames==null) return null;
  	String dataName = dataNames[1];
    for (int i=0; i< datasets.size(); i++) {
    	Dataset next = datasets.get(i);
      if (next==null) continue;
      if (i==0 && dataName.equals(next.getXColumnName())) return next;
      if (dataName.equals(next.getYColumnName())) return next;
    }
    return null;
  }

  /**
   * Gets a matching Dataset by ID.
   * @param column the DataColumn to match
   * @param datasets the Datasets to search
   * @return the matching Dataset
   */
  protected Dataset getMatchByID(DataColumn column, ArrayList<Dataset> datasets) {
    for (Dataset next: datasets) {
      if (next==null) continue;
      if (column.getID()==next.getID()) return next;
    }
    return null;
  }

  /**
   * Sets the selected data in the curve fitter and fourier panel.
   * @param selectedData the Dataset to pass to the fitter and fourier panel
   */
  protected void setSelectedData(Dataset selectedData) {
    curveFitter.setData(selectedData);
  	if (fourierPanel!=null)
  		fourierPanel.refreshFourierData(selectedData, getName());
  }

  /**
   * Refreshes the plot.
   */
  protected void refreshPlot() {
    // refresh data for curve fitting and plotting
    setSelectedData(dataTable.getSelectedData());
    plot.removeDrawables(Dataset.class);
    WorkingDataset workingData = getWorkingData();
    valueCheckbox.setEnabled((workingData!=null)&&(workingData.getIndex()>0));
    if(!valueCheckbox.isEnabled()) {
      valueCheckbox.setSelected(false);
      positionVisible = false;
    }
    slopeCheckbox.setEnabled((workingData!=null)&&(workingData.getIndex()>2));
    if(!slopeCheckbox.isEnabled()) {
      slopeCheckbox.setSelected(false);
      slopeVisible = false;
    }
    areaCheckbox.setEnabled((workingData!=null)&&(workingData.getIndex()>1));
    if(!areaCheckbox.isEnabled()) {
      areaCheckbox.setSelected(false);
      areaVisible = false;
    }
    if(workingData!=null) {
      int labelCol = dataTable.convertColumnIndexToView(0);
      String xName = dataTable.getColumnName((labelCol==0) ? 1 : 0);
      Map<String, WorkingDataset> datasets = dataTable.workingMap;
      for(Iterator<WorkingDataset> it = datasets.values().iterator(); it.hasNext(); ) {
        DataToolTable.WorkingDataset next = it.next();
        next.setXSource(workingData.getXSource());
        String colName = next.getYColumnName();
        if((next==workingData)||colName.equals(xName)) {
          continue;
        }
        if(next.isMarkersVisible()||next.isConnected()) {
          next.clearHighlights();
          if(!next.isMarkersVisible()) {
            next.setMarkerShape(Dataset.NO_MARKER);
          }
          plot.addDrawable(next);
        }
      }
      plot.addDrawable(workingData);
      // keep area limits within dataset limits
      if(areaVisible) {
        plot.limits[0].x = Math.max(plot.limits[0].x, workingData.getXMin());
        plot.limits[0].x = Math.min(plot.limits[0].x, workingData.getXMax());
        plot.limits[1].x = Math.max(plot.limits[1].x, workingData.getXMin());
        plot.limits[1].x = Math.min(plot.limits[1].x, workingData.getXMax());
      }
      workingData.restoreHighlights();
      // draw curve fit on top of dataset if curve fitter is visible
      if((fitterCheckbox!=null)&&fitterCheckbox.isSelected()) {
        plot.removeDrawable(curveFitter.getDrawer());
        plot.addDrawable(curveFitter.getDrawer());
      }
      // set axis labels
      String xLabel = workingData.getColumnName(0);
      String yLabel = workingData.getColumnName(1);
//      for(int i = 0; i<dataTable.getColumnCount(); i++) {
//        String colName = dataTable.getColumnName(i);
//        if(colName.equals(xLabel)||colName.equals(workingData.getColumnName(1))||colName.equals(DataTable.rowName)) {
//          continue;
//        }
//        WorkingDataset next = dataTable.getWorkingData(colName);
//        if((next!=null)&&(next.markersVisible||next.isConnected())) {
//          yLabel += ", "+colName;                  //$NON-NLS-1$
//        }
//      }
      plot.setAxisLabels(xLabel, yLabel);
      // construct equation string
      String depVar = TeXParser.removeSubscripting(workingData.getColumnName(1));
      String indepVar = TeXParser.removeSubscripting(workingData.getColumnName(0));
      if(curveFitter.fit instanceof UserFunction) {
        curveFitter.eqnField.setText(depVar+" = "+ //$NON-NLS-1$
          ((UserFunction) curveFitter.fit).getFullExpression(new String[] {indepVar}));
      } else {
        curveFitter.eqnField.setText(depVar+" = "+ //$NON-NLS-1$
          curveFitter.fit.getExpression(indepVar));
      }
    } else {                                       // working data is null
      plot.setXLabel("");                          //$NON-NLS-1$
      plot.setYLabel("");                          //$NON-NLS-1$
    }
    if(dataTool!=null) {
      dataTool.refreshTabTitles();
    }
    // refresh area
    if(areaVisible) {
      plot.refreshArea(workingData);
    }
    repaint();
  }

  /**
   * Refreshes the data by sending a request to the source. Note that this only works
   * if the data was received from a DataRefreshTool.
   */
  public void refreshData() {
    // set dataManager name to tab name so reply will be recognized 
    dataManager.setName(getName());
    jobManager.sendReplies(dataManager);  	
  }
  
  /**
   * Refreshes the status bar.
   */
  protected void refreshStatusBar() {
  	if (statsCheckbox.isSelected()) {
  		String s = ToolsRes.getString("DataToolTab.Status.Correlation"); //$NON-NLS-1$
  		if (Double.isNaN(curveFitter.correlation)) {
  			s += " "+ ToolsRes.getString("DataToolTab.Status.Correlation.Undefined"); //$NON-NLS-1$ //$NON-NLS-2$
  		}
  		else {
  			s += " = "+ correlationFormat.format(curveFitter.correlation); //$NON-NLS-1$ 			
  		}
  		statusLabel.setText(s); 
      statusLabel.setFont(editableLabel.getFont().deriveFont(Font.BOLD));
  	}
  	else {
      statusLabel.setFont(editableLabel.getFont().deriveFont(Font.PLAIN));
  		if(dataManager.getDatasets().size()<2) {
	      statusLabel.setText(userEditable ? ToolsRes.getString("DataToolTab.StatusBar.Text.CreateColumns")  //$NON-NLS-1$
	                                       : ToolsRes.getString("DataToolTab.StatusBar.Text.PasteColumns")); //$NON-NLS-1$
	    } else {
	      statusLabel.setText(ToolsRes.getString("DataToolTab.StatusBar.Text.DragColumns"));                 //$NON-NLS-1$
	    }
  	}
    editableLabel.setText(userEditable? ToolsRes.getString("DataTool.MenuItem.Editable").toLowerCase()  //$NON-NLS-1$
        : ToolsRes.getString("DataTool.MenuItem.Noneditable").toLowerCase()); //$NON-NLS-1$
    editableLabel.setForeground(userEditable? Color.GREEN.darker(): Color.RED.darker());
  }

  /**
   * An interactive axes class that returns popup menus for x and y-variables.
   */
  class DataToolAxes extends CartesianInteractive {
    DataToolAxes(PlottingPanel panel) {
      super(panel);
    }

    /**
     * Overrides CartesianInteractive method.
     *
     * @return true
     */
    protected boolean hasHorzVariablesPopup() {
      return dataTable.workingData!=null;
    }

    /**
     * Gets a popup menu with horizontal axis variables.
     *
     * @return the popup menu
     */
    protected javax.swing.JPopupMenu getHorzVariablesPopup() {
      if(varPopup==null) {
        buildVarPopup();
      }
      isHorzVarPopup = true;
      FontSizer.setFonts(varPopup, fontLevel);
      for(Component c : varPopup.getComponents()) {
        JMenuItem item = (JMenuItem) c;
        if(xLine.getText().equals(item.getActionCommand())) {
          item.setFont(item.getFont().deriveFont(Font.BOLD));
        } else {
          item.setFont(item.getFont().deriveFont(Font.PLAIN));
        }
      }
      return varPopup;
    }

    /**
     * Overrides CartesianInteractive method.
     *
     * @return true
     */
    protected boolean hasVertVariablesPopup() {
      return dataTable.workingData!=null;
    }

    /**
     * Gets a popup menu with vertical axis variables.
     *
     * @return the popup menu
     */
    protected javax.swing.JPopupMenu getVertVariablesPopup() {
      if(varPopup==null) {
        buildVarPopup();
      }
      isHorzVarPopup = false;
      FontSizer.setFonts(varPopup, fontLevel);
      for(Component c : varPopup.getComponents()) {
        JMenuItem item = (JMenuItem) c;
        if(yLine.getText().equals(item.getActionCommand())) {
          item.setFont(item.getFont().deriveFont(Font.BOLD));
        } else {
          item.setFont(item.getFont().deriveFont(Font.PLAIN));
        }
      }
      return varPopup;
    }

  }

  /**
   * A class to plot datasets, slope lines, areas and limits.
   */
  class DataToolPlotter extends PlottingPanel {
    SelectionBox selectionBox;
    Crossbars crossbars;
    SlopeLine slopeLine;
    Dataset areaDataset;
    LimitLine[] limits = new LimitLine[2];
    double value = Double.NaN, slope = Double.NaN, area;
    DecimalFormat sciFormat = new DecimalFormat("0.00E0"); //$NON-NLS-1$
    DecimalFormat fixedFormat = new DecimalFormat("0.00"); //$NON-NLS-1$
    String xVar, yVar, message;

    // constructor
    DataToolPlotter(Dataset dataset) {
      super((dataset==null) ? "x"                                             //$NON-NLS-1$
                            : dataset.getColumnName(0), (dataset==null) ? "y" //$NON-NLS-1$
                            : dataset.getColumnName(1), "");                  //$NON-NLS-1$
      setAntialiasShapeOn(true);
      selectionBox = new SelectionBox();
      crossbars = new Crossbars();
      slopeLine = new SlopeLine();
      limits[0] = new LimitLine();
      limits[1] = new LimitLine();
      addDrawable(limits[0]);
      addDrawable(limits[1]);
      addDrawable(selectionBox);
    }

    /**
     * Paints all the drawables. Overrides DrawingPanel method.
     * @param g the graphics
     * @param tempList the list of drawables
     */
    protected void paintDrawableList(Graphics g, ArrayList<Drawable> tempList) {
      super.paintDrawableList(g, tempList);
      if(tempList.contains(curveFitter.getDrawer())) {
        double[] ylimits = curveFitter.getDrawer().getYRange();
        if((ylimits[0]>=this.getYMax())||(ylimits[1]<=this.getYMin())) {
        	String s = ToolsRes.getString("DataToolTab.Plot.Message.FitNotVisible"); //$NON-NLS-1$
          if (message!=null && !"".equals(message)) { //$NON-NLS-1$
          	s += "  "+message; //$NON-NLS-1$
          }
        	setMessage(s);
        } else {
          setMessage(message);
        }
      } else {
        setMessage(message);
      }
      slopeLine.draw(g);
      crossbars.draw(g);
    }

    void setAreaVisible(boolean visible) {
      areaVisible = visible;
      if(areaDataset==null) { // first time shown
        areaDataset = new Dataset();
        areaDataset.setMarkerShape(Dataset.AREA);
        areaDataset.setConnected(false);
        areaDataset.setMarkerColor(new Color(102, 102, 102, 51));
        Dataset data = dataTable.workingData;
        if((data!=null)&&(data.getIndex()>1)) {
          limits[0].x = data.getXMin();
          limits[1].x = data.getXMax();
        }
      }
      refreshPlot();
      setMessage(createMessage());
    }

    /**
     * Fills the areaDataset with points whose x values are between the limit lines.
     *
     * @param data the source dataset
     */
    void refreshArea(Dataset data) {
      if(!areaVisible) {
        return;
      }
      area = 0;
      if(data==null) {
        areaVisible = false;
        setMessage(createMessage());
        return;
      }
      double[] xpoints = data.getXPoints();
      double[] ypoints = data.getYPoints();
      areaDataset.clear();
      // find data points within range
      ArrayList<Double> x = new ArrayList<Double>();
      ArrayList<Double> y = new ArrayList<Double>();
      for(int i = 0; i<xpoints.length; i++) {
        double lower = Math.min(limits[0].x, limits[1].x);
        double upper = Math.max(limits[0].x, limits[1].x);
        if((xpoints[i]>=lower)&&(xpoints[i]<=upper)) {
          x.add(new Double(xpoints[i]));
          y.add(new Double(ypoints[i]));
        }
      }
      if(!x.isEmpty()) {
        xpoints = new double[x.size()];
        ypoints = new double[x.size()];
        for(int i = 0; i<xpoints.length; i++) {
          xpoints[i] = x.get(i).doubleValue();
          ypoints[i] = y.get(i).doubleValue();
        }
        areaDataset.append(xpoints[0], 0);
        areaDataset.append(xpoints, ypoints);
        areaDataset.append(xpoints[xpoints.length-1], 0);
        int n = xpoints.length;
        if(n>1) {
          plot.addDrawable(areaDataset);
          // determine area under the curve
          area = ypoints[0]*(xpoints[1]-xpoints[0]);
          area += ypoints[n-1]*(xpoints[n-1]-xpoints[n-2]);
          for(int i = 1; i<n-1; i++) {
            area += ypoints[i]*(xpoints[i+1]-xpoints[i-1]);
          }
          area /= 2;
        }
      }
      setMessage(createMessage());
    }

    /**
     * Returns the index of the data point nearest the specified x on the plot.
     *
     * @param x the x-value on the plot
     * @param data the dataset to search
     * @return the index, or -1 if none found
     */
    protected int findIndexNearestX(double x, Dataset data) {
      if(data==null) {
        return -1; // no dataset
      }
      int last = data.getIndex()-1;
      if(last==-1) {
        return -1; // dataset has no points
      }
      // limit x to plot area
      x = Math.max(plot.getXMin(), x);
      x = Math.min(plot.getXMax(), x);
      // look thru sorted data to find point nearest x
      double[] sorted = data.getXPoints();
      java.util.Arrays.sort(sorted);
      // check if pixel outside data range
      if(x<sorted[0]) {
        return 0;
      }
      if(x>=sorted[last]) {
        return last;
      }
      for(int i = 1; i<sorted.length; i++) {
        if((x>=sorted[i-1])&&(x<sorted[i])) {
          if(sorted[i-1]<plot.getXMin()) {
            x = sorted[i];
          } else if(sorted[i]>plot.getXMax()) {
            x = sorted[i-1];
          } else {
            x = (Math.abs(x-sorted[i-1])<Math.abs(x-sorted[i])) ? sorted[i-1] : sorted[i];
          }
          // find index of data point with this value of x
          double[] xpoints = data.getXPoints();
          for(int j = 0; j<xpoints.length; j++) {
            if(xpoints[j]==x) {
              return j;
            }
          }
          return -1;
        }
      }
      return -1; // none found (should never get here)
    }

    String createMessage() {
      StringBuffer buf = new StringBuffer();
      if(positionVisible&&!Double.isNaN(value)) {
        buf.append(TeXParser.removeSubscripting(xVar)+"="); //$NON-NLS-1$
        buf.append(format(plot.crossbars.x, getXMax()-getXMin()));
        buf.append("  ");                                   //$NON-NLS-1$
        buf.append(TeXParser.removeSubscripting(yVar)+"="); //$NON-NLS-1$
        buf.append(format(plot.crossbars.y, getYMax()-getYMin()));
      }
      if(slopeVisible&&!Double.isNaN(slope)) {
        if(buf.length()>0) {
          buf.append("  ");                                              //$NON-NLS-1$
        }
        buf.append(ToolsRes.getString("DataToolPlotter.Message.Slope")); //$NON-NLS-1$
        buf.append(format(plot.slope, 0));
      }
      if(areaVisible) {
        if(buf.length()>0) {
          buf.append("  ");                                             //$NON-NLS-1$
        }
        buf.append(ToolsRes.getString("DataToolPlotter.Message.Area")); //$NON-NLS-1$
        buf.append(format(plot.area, 0));
      }
      message = buf.toString();
      return message;
    }

    /**
     * Formats a number.
     *
     * @param value the number
     * @param range a min-max range of values
     * @return the formatted string
     */
    String format(double value, double range) {
      double zero = Math.min(1, range)/1000;
      if(Math.abs(value)<zero) {
        value = 0;
      }
      if((range<1)&&(value!=0)) {
        return sciFormat.format(value);
      }
      return(Math.abs(value)<=10) ? fixedFormat.format(value) : sciFormat.format(value);
    }

    void setAxisLabels(String xAxis, String yAxis) {
      setXLabel(xAxis);
      setYLabel(yAxis);
      xVar = TeXParser.removeSubscripting(xAxis);
      yVar = TeXParser.removeSubscripting(yAxis);
      String xLabel = xVar+"=";      //$NON-NLS-1$
      String yLabel = "  "+yVar+"="; //$NON-NLS-1$ //$NON-NLS-2$
      coordinateStrBuilder.setCoordinateLabels(xLabel, yLabel);
    }

    void setCoordinateLabels(String xCoord, String yCoord) {
      xCoord = TeXParser.removeSubscripting(xCoord);
      yCoord = TeXParser.removeSubscripting(yCoord);
      String xLabel = xCoord+"=";      //$NON-NLS-1$
      String yLabel = "  "+yCoord+"="; //$NON-NLS-1$ //$NON-NLS-2$
      coordinateStrBuilder.setCoordinateLabels(xLabel, yLabel);
    }

    protected void setFontLevel(int level) {
      super.setFontLevel(level);
    }

    /**
     * A class for selecting points on this plot.
     */
    class SelectionBox extends Rectangle implements Drawable {
      boolean visible = true;
      int xstart, ystart;
      Color color = new Color(0, 255, 0, 127);

      public void setSize(int w, int h) {
        int xoffset = Math.min(0, w);
        int yoffset = Math.min(0, h);
        w = Math.abs(w);
        h = Math.abs(h);
        super.setLocation(xstart+xoffset, ystart+yoffset);
        super.setSize(w, h);
      }

      public void draw(DrawingPanel drawingPanel, Graphics g) {
        if(visible) {
          Graphics2D g2 = (Graphics2D) g;
          g2.setColor(color);
          g2.draw(this);
        }
      }

    }

    /**
     * A class to draw crossbars on this plot.
     */
    class Crossbars {
      double x, y;
      Color color = new Color(0, 0, 0);

      public void draw(Graphics g) {
        if(!positionVisible||java.lang.Double.isNaN(value)) {
          return;
        }
        Color c = g.getColor();
        g.setColor(color);
        g.drawLine(getLeftGutter(), yToPix(y), getWidth()-getRightGutter()-1, yToPix(y));
        g.drawLine(xToPix(x), getTopGutter(), xToPix(x), getHeight()-getBottomGutter()-1);
        g.setColor(c);
      }

    }

    /**
     * A class to draw a slope line on this plot.
     */
    class SlopeLine extends Line2D.Double {
      double x, y;
      Stroke stroke = new BasicStroke(1.5f);
      int length = 30;
      Color color = new Color(0, 0, 0);

      public void draw(Graphics g) {
        if(!slopeVisible||java.lang.Double.isNaN(slope)) {
          return;
        }
        double dxPix = 1*getXPixPerUnit();
        double dyPix = slope*getYPixPerUnit();
        double hyp = Math.sqrt(dxPix*dxPix+dyPix*dyPix);
        double sin = dyPix/hyp;
        double cos = dxPix/hyp;
        int xCenter = xToPix(x);
        int yCenter = yToPix(y);
        setLine(xCenter-length*cos+1, yCenter+length*sin+1, xCenter+length*cos+1, yCenter-length*sin+1);
        Color gcolor = g.getColor();
        g.setColor(color);
        ((Graphics2D) g).fill(stroke.createStrokedShape(this));
        g.setColor(gcolor);
      }

    }

    /**
     * A class that draws a vertical limit line on the plot.
     */
    class LimitLine extends Line2D.Double implements Selectable {
      double x;
      Stroke stroke = new BasicStroke(1.0f);
      Rectangle hitRect = new Rectangle();
      Color color = new Color(51, 51, 51);
      Cursor move;

      public void draw(DrawingPanel panel, Graphics g) {
        if(!areaVisible) {
          return;
        }
        Color gcolor = g.getColor();
        g.setColor(color);
        int y0 = plot.getTopGutter();
        int y1 = plot.getBounds().height-plot.getBottomGutter();
        int x1 = plot.xToPix(x);
        setLine(x1+1, y0, x1+1, y1);
        ((Graphics2D) g).fill(stroke.createStrokedShape(this));
        g.setColor(gcolor);
        hitRect.setBounds(x1-2, y0, 6, y1-y0-20);
      }

      public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
        if(areaVisible&&hitRect.contains(xpix, ypix)) {
          return this;
        }
        return null;
      }

      public Cursor getPreferredCursor() {
        if(move==null) {
          // create cursor
          String imageFile = "/org/opensourcephysics/resources/tools/images/limitcursor.gif";                     //$NON-NLS-1$
          Image im = ResourceLoader.getImage(imageFile);
          move = Toolkit.getDefaultToolkit().createCustomCursor(im, new Point(16, 16), "Move Integration Limit"); //$NON-NLS-1$
        }
        return move;
      }

      public void setXY(double x, double y) {
        setX(x);
      }

      public void setX(double x) {
        Dataset data = dataTable.workingData;
        int j = findIndexNearestX(x, data);
        this.x = (j==-1) ? x : data.getXPoints()[j];
        refreshArea(data);
        createMessage();
        plot.setMessage(message);
      }

      public boolean isMeasured() {
        return areaVisible;
      }

      public double getXMin() {
        Dataset data = dataTable.workingData;
        double dx = 0, min = 0;
        if((data!=null)&&(data.getIndex()>1)) {
          dx = Math.abs(data.getXMax()-data.getXMin());
          min = Math.min(data.getXMax(), data.getXMin());
        } else {
          dx = Math.abs(limits[0].x-limits[1].x);
          min = Math.min(limits[0].x, limits[1].x);
        }
        return min-0.02*dx;
      }

      public double getXMax() {
        Dataset data = dataTable.workingData;
        double dx = 0, max = 0;
        if((data!=null)&&(data.getIndex()>1)) {
          dx = Math.abs(data.getXMax()-data.getXMin());
          max = Math.max(data.getXMax(), data.getXMin());
        } else {
          dx = Math.abs(limits[0].x-limits[1].x);
          max = Math.max(limits[0].x, limits[1].x);
        }
        return max+0.02*dx;
      }

      public double getYMin() {
        return(plot.getYMin()+plot.getYMax())/2;
      }

      public double getYMax() {
        return(plot.getYMin()+plot.getYMax())/2;
      }

      // the following methods are required by Selectable but not used
      public void setY(double y) {}

      public double getX() {
        return x;
      }

      public double getY() {
        return 0;
      }

      public void setSelected(boolean selectable) {}

      public boolean isSelected() {
        return false;
      }

      public void toggleSelected() {}

      public boolean isEnabled() {
        return true;
      }

      public void setEnabled(boolean enable) {}

    }

  }

  //__________________________ static methods ___________________________

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
    public void saveObject(XMLControl control, Object obj) {
      DataToolTab tab = (DataToolTab) obj;
      // save name and owner name
      control.setValue("name", tab.getName()); //$NON-NLS-1$
      control.setValue("owner_name", tab.getOwnerName()); //$NON-NLS-1$
      // save owned columns
      if (!tab.ownedColumns.isEmpty()) {     	
	      String[][] columns = new String[tab.ownedColumns.size()][3];
	      // each element is {tab column name, owner/guest name, owner/guest dataset y-column name}
	      int i = 0;
	      for (String key: tab.ownedColumns.keySet()) {
	      	String[] data = tab.ownedColumns.get(key);	      	
	      	columns[i] = new String[] {key, data[0], data[1]};
	      	i++;
	      }
	      control.setValue("owned_columns", columns); //$NON-NLS-1$      	
      }

      // save userEditable
      control.setValue("editable", tab.userEditable); //$NON-NLS-1$
      // save data columns but leave out data functions
      DatasetManager data = new DatasetManager();
      ArrayList<Dataset> functions = new ArrayList<Dataset>();
      for(Iterator<Dataset> it = tab.dataManager.getDatasets().iterator(); it.hasNext(); ) {
        Dataset next = it.next();
        if(next instanceof DataFunction) {
          functions.add(next);
        } else {
          data.addDataset(next);
        }
      }
      control.setValue("data", data); //$NON-NLS-1$
      // save function parameters
      String[] paramNames = tab.dataManager.getConstantNames();
      if (paramNames.length>0) {
    		Object[][] paramArray = new Object[paramNames.length][3];
    		int i = 0;
    		for (String name: paramNames) {
    			paramArray[i][0] = name;
    			paramArray[i][1] = tab.dataManager.getConstantValue(name);
    			paramArray[i][2] = tab.dataManager.getConstantExpression(name);
    			i++;
    		}
    		control.setValue("constants", paramArray); //$NON-NLS-1$
      }
      // save data functions
      if(!functions.isEmpty()) {
        DataFunction[] f = functions.toArray(new DataFunction[0]);
        control.setValue("data_functions", f); //$NON-NLS-1$
      }
      // save fit function panels
      if(tab.dataTool.fitBuilder!=null) {
        ArrayList<FunctionPanel> fits = new ArrayList<FunctionPanel>(tab.dataTool.fitBuilder.panels.values());
        control.setValue("fits", fits); //$NON-NLS-1$
      }
      // save selected fit name
      control.setValue("selected_fit", tab.curveFitter.getSelectedFitName());    //$NON-NLS-1$
      // save autofit status
      control.setValue("autofit", tab.curveFitter.autofitCheckBox.isSelected()); //$NON-NLS-1$
      // save fit parameters
      if(!tab.curveFitter.autofitCheckBox.isSelected()) {
        double[] params = new double[tab.curveFitter.paramModel.getRowCount()];
        for(int i = 0; i<params.length; i++) {
          Double val = (Double) tab.curveFitter.paramModel.getValueAt(i, 1);
          params[i] = val.doubleValue();
        }
        control.setValue("fit_parameters", params); //$NON-NLS-1$
      }
      // save fit color
      control.setValue("fit_color", tab.curveFitter.color);                 //$NON-NLS-1$
      // save fit visibility
      control.setValue("fit_visible", tab.fitterCheckbox.isSelected()); //$NON-NLS-1$
      // save props visibility
      control.setValue("props_visible", tab.propsCheckbox.isSelected());    //$NON-NLS-1$
      // save statistics visibility
      control.setValue("stats_visible", tab.statsCheckbox.isSelected());    //$NON-NLS-1$
      // save splitPane locations
      int loc = tab.splitPanes[0].getDividerLocation();
      control.setValue("split_pane", loc); //$NON-NLS-1$
      loc = tab.curveFitter.splitPane.getDividerLocation();
      control.setValue("fit_split_pane", loc); //$NON-NLS-1$
      // save model column order
      int[] cols = tab.dataTable.getModelColumnOrder();
      control.setValue("column_order", cols); //$NON-NLS-1$
      // save hidden markers
      String[] hidden = tab.dataTable.getHiddenMarkers();
      control.setValue("hidden_markers", hidden); //$NON-NLS-1$
      // save column format patterns, if any
      String[] patternColumns = tab.dataTable.getFormattedColumnNames();
      if(patternColumns.length>0) {
        ArrayList<String[]> patterns = new ArrayList<String[]>();
        for(int i=0; i<patternColumns.length; i++) {
          String colName = patternColumns[i];
          String pattern = tab.dataTable.getFormatPattern(colName);
          patterns.add(new String[] {colName, pattern});
        }
        control.setValue("format_patterns", patterns); //$NON-NLS-1$
      }
    }

    public Object createObject(XMLControl control) {
    	// get DataTool from control
    	DataTool dataTool = (DataTool)control.getObject("datatool"); //$NON-NLS-1$
      // load data
      DatasetManager data = (DatasetManager) control.getObject("data"); //$NON-NLS-1$
      if(data==null) {
        return new DataToolTab(null, dataTool);
      }
      for(Dataset next : data.getDatasets()) {
        next.setXColumnVisible(false);
      }
      return new DataToolTab(data, dataTool);
    }

    public Object loadObject(XMLControl control, Object obj) {
      final DataToolTab tab = (DataToolTab) obj;
      // load tab name and owner name, if any
      tab.setName(control.getString("name")); //$NON-NLS-1$
      tab.ownerName = control.getString("owner_name"); //$NON-NLS-1$
      // load owned columns
      String[][] columns = (String[][])control.getObject("owned_columns"); //$NON-NLS-1$
      if (columns!=null) {
      	tab.ownedColumns.clear();
      	for (String[] next: columns) { 
      		// next is {tab column name, owner/guest name, owner/guest dataset y-column name}
      		// column name becomes key in map to owner/guest data
      		String[] data = new String[] {next[1], next[2]};
      		tab.ownedColumns.put(next[0], data);
      	}
      }
      // load data functions and constants
      Object[][] constants = (Object[][])control.getObject("constants"); //$NON-NLS-1$
    	if (constants!=null) {
    		for (int i=0; i<constants.length; i++) {
	    		String name = (String)constants[i][0];
	    		double val = (Double)constants[i][1];
	    		String expression = (String)constants[i][2];
	    		tab.dataManager.setConstant(name, val, expression);
    		}
    	}      
      Iterator<?> it = control.getPropertyContent().iterator();
      while(it.hasNext()) {
        XMLProperty prop = (XMLProperty) it.next();
        if(prop.getPropertyName().equals("data_functions")) { //$NON-NLS-1$
          XMLControl[] children = prop.getChildControls();
          for(int i = 0; i<children.length; i++) {
            DataFunction f = new DataFunction(tab.dataManager);
            children[i].loadObject(f);
            f.setXColumnVisible(false);
            tab.dataManager.addDataset(f);
          }
          // refresh dataFunctions
          ArrayList<Dataset> datasets = tab.dataManager.getDatasets();
          for(int i = 0; i<datasets.size(); i++) {
            if(datasets.get(i) instanceof DataFunction) {
              ((DataFunction) datasets.get(i)).refreshFunctionData();
            }
          }
          tab.dataTable.refreshTable();
          break;
        }
      }
      // load userEditable
      tab.userEditable = control.getBoolean("editable"); //$NON-NLS-1$
      // load user fit function panels
      ArrayList<?> fits = (ArrayList<?>) control.getObject("fits"); //$NON-NLS-1$
      if(fits!=null) {
        for(it = fits.iterator(); it.hasNext(); ) {
          FitFunctionPanel panel = (FitFunctionPanel) it.next();
          tab.dataTool.fitBuilder.addPanel(panel.getName(), panel);
        }
      }
      // select fit
      String fitName = control.getString("selected_fit"); //$NON-NLS-1$
      tab.curveFitter.fitDropDown.setSelectedItem(fitName);
      // load autofit
      boolean autofit = control.getBoolean("autofit"); //$NON-NLS-1$
      tab.curveFitter.autofitCheckBox.setSelected(autofit);
      // load fit parameters
      double[] params = (double[]) control.getObject("fit_parameters"); //$NON-NLS-1$
      if(params!=null) {
        for(int i = 0; i<params.length; i++) {
          tab.curveFitter.setParameterValue(i, params[i]);
        }
      }
      // load fit color
      Color color = (Color) control.getObject("fit_color"); //$NON-NLS-1$
      tab.curveFitter.setColor(color);
      // load fit visibility
      boolean vis = control.getBoolean("fit_visible"); //$NON-NLS-1$
      tab.fitterCheckbox.setSelected(vis);
      // load props visibility
      vis = control.getBoolean("props_visible"); //$NON-NLS-1$
      tab.propsCheckbox.setSelected(vis);
      // load stats visibility
      vis = control.getBoolean("stats_visible"); //$NON-NLS-1$
      tab.statsCheckbox.setSelected(vis);
      // load splitPane locations
      final int loc = control.getInt("split_pane");           //$NON-NLS-1$
      final int fitLoc = control.getInt("fit_split_pane");    //$NON-NLS-1$
      // load model column order
      int[] cols = (int[]) control.getObject("column_order"); //$NON-NLS-1$
      tab.dataTable.setModelColumnOrder(cols);
      if(cols==null) {                                                    // for legacy files: load working columns
        String[] names = (String[]) control.getObject("working_columns"); //$NON-NLS-1$
        if(names!=null) {
          tab.dataTable.setWorkingColumns(names[0], names[1]);
        }
      }
      // load hidden markers
      String[] hidden = (String[]) control.getObject("hidden_markers"); //$NON-NLS-1$
      tab.dataTable.hideMarkers(hidden);
      // load format patterns
      ArrayList<?> patterns = (ArrayList<?>) control.getObject("format_patterns"); //$NON-NLS-1$
      if(patterns!=null) {
        for(it = patterns.iterator(); it.hasNext(); ) {
          String[] next = (String[]) it.next();
          tab.dataTable.setFormatPattern(next[0], next[1]);
        }
      }
      Runnable runner = new Runnable() {
        public synchronized void run() {
          tab.fitterAction.actionPerformed(null);
          tab.propsAndStatsAction.actionPerformed(null);
          tab.splitPanes[0].setDividerLocation(loc);
          tab.curveFitter.splitPane.setDividerLocation(fitLoc);
          tab.dataTable.refreshTable();
          tab.propsTable.refreshTable();
          tab.tabChanged(false);
        }

      };
      SwingUtilities.invokeLater(runner);
      return obj;
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
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
