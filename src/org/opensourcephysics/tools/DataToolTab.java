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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractSpinnerModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
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
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.HighlightableDataset;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.InteractivePanel;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display.Selectable;
import org.opensourcephysics.display.TeXParser;
import org.opensourcephysics.display.axes.CartesianCoordinateStringBuilder;
import org.opensourcephysics.display.axes.CartesianInteractive;
import org.opensourcephysics.media.core.TPoint;
import org.opensourcephysics.tools.DataToolTable.TableEdit;
import org.opensourcephysics.tools.DataToolTable.WorkingDataset;
import org.opensourcephysics.tools.DatasetCurveFitter.NumberField;

/**
 * This tab displays and analyzes a single Data object in a DataTool.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DataToolTab extends JPanel implements Tool, PropertyChangeListener {

	// static fields

	public final static String SHIFTED = "'"; //$NON-NLS-1$
	protected static DecimalFormat correlationFormat = (DecimalFormat) NumberFormat.getInstance();
	private static final Cursor SELECT_CURSOR, SELECT_ZOOM_CURSOR;
	private static final Cursor SELECT_REMOVE_CURSOR, SELECT_ADD_CURSOR;
	// mouse states	
	private final static int STATE_INACTIVE = 0;
	private final static int STATE_SELECT = 1;
	private final static int STATE_SELECT_ADD = 2;
	private final static int STATE_SELECT_REMOVE = 3;
	private final static int STATE_MOVE = 4;
	private final static int STATE_ZOOM = 5;

	static {
		if (correlationFormat instanceof DecimalFormat) {
			DecimalFormat format = correlationFormat;
			format.applyPattern("0.000"); //$NON-NLS-1$
		}
		// create cursors
		String imageFile = "/org/opensourcephysics/resources/tools/images/selectcursor.gif"; //$NON-NLS-1$
		Image im = ResourceLoader.getImage(imageFile);
		SELECT_CURSOR = GUIUtils.createCustomCursor(im, new Point(0, 0), "Select points", Cursor.CROSSHAIR_CURSOR); //$NON-NLS-1$
		imageFile = "/org/opensourcephysics/resources/tools/images/selectremovecursor.gif"; //$NON-NLS-1$
		im = ResourceLoader.getImage(imageFile);
		SELECT_REMOVE_CURSOR = GUIUtils.createCustomCursor(im, new Point(0, 0), "Remove points", //$NON-NLS-1$
				Cursor.CROSSHAIR_CURSOR);
		imageFile = "/org/opensourcephysics/resources/tools/images/selectaddcursor.gif"; //$NON-NLS-1$
		im = ResourceLoader.getImage(imageFile);
		SELECT_ADD_CURSOR = GUIUtils.createCustomCursor(im, new Point(0, 0), "Add points", //$NON-NLS-1$
				Cursor.CROSSHAIR_CURSOR);
		imageFile = "/org/opensourcephysics/resources/tools/images/selectzoomcursor.gif"; //$NON-NLS-1$
		im = ResourceLoader.getImage(imageFile);
		SELECT_ZOOM_CURSOR = GUIUtils.createCustomCursor(im, new Point(8, 8), "Zoom", Cursor.CROSSHAIR_CURSOR); //$NON-NLS-1$
	}

	// instance fields

	private DatasetCurveFitter curveFitter;

	DatasetCurveFitter getCurveFitter() {
		checkGUI();
		return curveFitter;
	}
	
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
	protected JCheckBoxMenuItem statsCheckbox, propsCheckbox, fourierCheckbox;
	protected FourierPanel fourierPanel;
	protected JDialog fourierDialog;
	protected JButton measureButton, analyzeButton, dataBuilderButton, newColumnButton, refreshDataButton;
	protected JCheckBoxMenuItem valueCheckbox, slopeCheckbox, areaCheckbox;
	protected Action showFitterAction, hideFitterAction, propsAndStatsAction;
	protected JMenu fitMenu;
	protected String fileName, ownerName;
	protected Map<String, String[]> ownedColumns = new TreeMap<String, String[]>();
	protected JButton helpButton;
	protected JButton editDataButton;
	protected int colorIndex = 0;
	protected boolean tabChanged;
	protected boolean userEditable = false;
	protected UndoableEditSupport undoSupport;
	protected UndoManager undoManager;
	protected FunctionTool dataBuilder;
	protected JobManager jobManager = new JobManager(this);
	protected JLabel statusLabel, editableLabel;
	protected DataToolAxes plotAxes;
	protected boolean positionVisible = false;
	protected boolean slopeVisible = false;
	protected boolean areaVisible = false;
	protected boolean originShiftEnabled = false;
	protected boolean measureFit = false;
	protected JPopupMenu varPopup;
	protected boolean isHorzVarPopup;
	protected Action setVarAction;
	protected boolean isInitialized = false;
	protected Object[][] constantsLoadedFromXML;
	protected boolean replaceColumnsWithMatchingNames = true;
	protected JCheckBoxMenuItem measureFitCheckbox, originShiftCheckbox;
	protected double prevShiftX, prevShiftY;
	protected NumberField shiftXField, shiftYField, selectedXField, selectedYField;
	protected JSpinner shiftXSpinner, shiftYSpinner;
	protected ShiftEditListener shiftEditListener;
	protected JLabel shiftXLabel, shiftYLabel, selectedXLabel, selectedYLabel;
	protected int selectedDataIndex = -1;
	protected boolean toggleMeasurement, freezeMeasurement;	
	private int mouseState = STATE_INACTIVE;
	private BitSet rowsInside = new BitSet(); // points inside selectionBox
	private BitSet recent = new BitSet(); // points recently added or removed
	private boolean selectionChanged, readyToFindHits, selectionBoxChanged;
	private Interactive mouseDrawable;
	private Timer timerToFindHits;

	/**
	 * Constructs a DataToolTab for the specified Data.
	 *
	 * @param data the Data object
	 * @param tool the DataTool
	 */
	public DataToolTab(Data data, DataTool tool) {
		dataTool = tool;
		dataTable = new DataToolTable(this);
		String name = ToolsRes.getString("DataToolTab.DefaultName"); //$NON-NLS-1$
		if (data != null) {
			String s = data.getName();
			if ((s != null) && !s.equals("")) { //$NON-NLS-1$
				name = s;
			}
		}
		setName(name);
		loadData(data, false);
		tabChanged(false);
	}

	@Override
	public void addNotify() {
		checkGUI();
		super.addNotify();
	}

	boolean haveGUI = false;

	public void checkGUI() {
		if (haveGUI)
			return;
		createGUI();
	}

	/**
	 * Loads data into this tab.
	 *
	 * @param data              the data to load
	 * @param replaceIfSameName true to replace existing data, if any
	 * @return true if loaded
	 */
	public ArrayList<DataColumn> loadData(final Data data, boolean replaceIfSameName) {
		ArrayList<DataColumn> loadedColumns = new ArrayList<DataColumn>();
		if (data == null) {
			return loadedColumns;
		}
		ArrayList<DataColumn> inputColumns = DataTool.getAllDataColumns(data);
		if (inputColumns == null) {
			return loadedColumns;
		}
		boolean updatedColumns = false;
		// case 1: tab contains no data
		if (dataManager.getDatasetsRaw().isEmpty()) {
			originatorID = data.getID();
			for (DataColumn next : inputColumns) {
				addColumn(next);
				loadedColumns.add(next);
			}
		}
		// case 2: tab already contains data
		else {
			// for each local column, find matching input column
			for (Dataset local : dataManager.getDatasetsRaw()) {
				DataColumn match = getIDMatch(local, inputColumns);
				if (match != null) {
					// if match is found, compare with local column and remove match from input
					// get y-column names
					String localName = local.getYColumnName();
					String name = match.getYColumnName();
					// temporarily set local y-column name to "" to get unique name for match
					local.setXYColumnNames("row", ""); //$NON-NLS-1$ //$NON-NLS-2$
					name = dataManager.uniquifyColumnName(match, name);
					local.setXYColumnNames("row", localName); //$NON-NLS-1$
					// update local if incoming points or name is different
					if (!Arrays.equals(local.getYPoints(), match.getYPoints()) || !name.equals(localName)) {
						local.clear();
						double[] rows = DataTool.getRowArray(match.getIndex());
						local.append(rows, match.getYPoints());
						local.setXYColumnNames("row", name); //$NON-NLS-1$
						updatedColumns = true;
					}
					inputColumns.remove(match);
				} else if (replaceIfSameName) { // no match found
					// see if name matches an existing column
					match = getNameMatch(local, inputColumns);
					if (match != null) {
						// if match is found, compare with local column and remove match from input
						// update local if incoming points are different
						if (!Arrays.equals(local.getYPoints(), match.getYPoints())) {
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
			for (DataColumn next : inputColumns) {
				addColumn(next);
				loadedColumns.add(next);
			}
		}
		if (haveGUI && (updatedColumns || !loadedColumns.isEmpty())) {
			dataTable.refreshTable(DataTable.MODE_COLUMN);
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
	 * @param source        the Data source of the columns
	 * @param deletable     true to allow added columns to be deleted
	 * @param addDuplicates true to add duplicate IDs
	 * @param postEdit      true to post an undoable edit
	 */
	public void addColumns(Data source, boolean deletable, boolean addDuplicates, boolean postEdit) {
		// look for independent variable column and duplicate input column
		ArrayList<Dataset> datasets = dataManager.getDatasetsRaw();
		// independent variable column
		Dataset indepVar = datasets.isEmpty() ? null : datasets.get(0);
		double[] indepVarPts = (indepVar == null) ? null : indepVar.getYPoints();
		// remove Double.NaN from end of indepVarPts
		if (indepVarPts != null) {
			while ((indepVarPts.length > 0) && Double.isNaN(indepVarPts[indepVarPts.length - 1])) {
				double[] newVals = new double[indepVarPts.length - 1];
				System.arraycopy(indepVarPts, 0, newVals, 0, newVals.length);
				indepVarPts = newVals;
			}
		}
		// indepVarPts cannot contain duplicates
		indepVar = ((indepVarPts == null) || DataTool.containsDuplicateValues(indepVarPts)) ? null : indepVar;
		ArrayList<DataColumn> inputColumns = DataTool.getDataColumns(source);
		Dataset duplicate = null; // duplicate input column
		if (indepVar != null) {
			String indepVarName = indepVar.getYColumnName();
			for (DataColumn next : inputColumns) {
				if ((duplicate == null) && next.getYColumnName().equals(indepVarName)) {
					// found matching column name, now compare their points
					double[] inputPts = next.getYPoints();
					// remove Double.NaN from end of inputPts
					while ((inputPts.length > 0) && Double.isNaN(inputPts[inputPts.length - 1])) {
						double[] newVals = new double[inputPts.length - 1];
						System.arraycopy(inputPts, 0, newVals, 0, newVals.length);
						inputPts = newVals;
					}
					// inputPts also can't contain duplicate points
					if (DataTool.containsDuplicateValues(inputPts)) {
						continue;
					}
					// does at least one point in inputPts match a point in indepVarPts?
					boolean foundMatchingPoint = false;
					for (double value : inputPts) {
						if (DataTool.getIndex(value, indepVarPts, -1) > -1) {
							foundMatchingPoint = true;
							break;
						}
					}
					if (!foundMatchingPoint) {
						continue;
					}
					// found a duplicate
					duplicate = next;
					// are indepVarPts in ascending or descending order?
					int trend = 1; // positive trend = ascending order
					double prev = -Double.MAX_VALUE;
					for (double d : indepVarPts) {
						if (d > prev) {
							prev = d;
						} else {
							trend = -1; // negative trend = descending order
							break;
						}
					}
					if (trend == -1) {
						prev = Double.MAX_VALUE;
						for (double d : indepVarPts) {
							if (d < prev) {
								prev = d;
							} else {
								trend = 0; // neither ascending nor descending
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
					int len = 0; // number of inserted values
					for (int i = 0; i < inputPts.length; i++) {
						int index = DataTool.getIndex(inputPts[i], indepVarPts, -1);
						if (index == -1) { // need to insert inputPts[i]
							valuesInserted[len] = inputPts[i];
							len++;
							newIndepVarPts = DataTool.insert(inputPts[i], newIndepVarPts, trend);
						}
					}
					if (len > 0) {
						// determine where insertions were made
						double[] rowsInserted = new double[len]; // double[] needed for getIndex()
						int[] rowsToInsert = new int[len];
						for (int i = 0; i < len; i++) {
							double val = valuesInserted[i];
							int index = DataTool.getIndex(val, newIndepVarPts, -1);
							rowsInserted[i] = index;
							rowsToInsert[i] = index;
						}
						// arrange rowsToInsert in ascending order
						Arrays.sort(rowsToInsert);
						// assemble valuesToInsert array for executing insertRows()
						double[] valuesToInsert = new double[len];
						for (int i = 0; i < len; i++) {
							int row = rowsToInsert[i];
							int index = DataTool.getIndex(row, rowsInserted, -1);
							valuesToInsert[i] = valuesInserted[index];
						}
						// prepare map of column names to double[] values to insert
						dataTable.pasteValues.clear();
						dataTable.pasteValues.put(indepVarName, valuesToInsert);
						HashMap<String, double[]> prevState = dataTable.insertRows(rowsToInsert, dataTable.pasteValues);
						// post edit: target is rows, value is map
						TableEdit edit = dataTable.new TableEdit(DataToolTable.INSERT_ROWS_EDIT, null, rowsToInsert,
								prevState);
						undoSupport.postEdit(edit);
						// rearrange non-duplicate columns
						for (DataColumn d : inputColumns) {
							if (d == duplicate) {
								continue;
							}
							double[] prevY = d.getYPoints();
							double[] rows = DataTool.getRowArray(newIndepVarPts.length);
							double[] newY = new double[rows.length];
							Arrays.fill(newY, Double.NaN);
							int k = Math.min(inputPts.length, prevY.length);
							for (int i = 0; i < k; i++) {
								int index = DataTool.getIndex(inputPts[i], newIndepVarPts, -1);
								newY[index] = prevY[i];
							}
							d.set(rows, newY);
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
	 * @param columns       the columns to add
	 * @param deletable     true to allow added columns to be deleted
	 * @param addDuplicates true to add duplicate IDs
	 * @param postEdit      true to post an undoable edit
	 */
	protected void addColumns(ArrayList<DataColumn> columns, boolean deletable, boolean addDuplicates,
			boolean postEdit) {
		for (DataColumn next : columns) {
			int id = next.getID();
			if (addDuplicates) {
				// change ID so column always added
				next.setID(-id);
			}
			ArrayList<DataColumn> loadedColumns = loadData(next, false);
			// restore original ID
			next.setID(id);
			if (!loadedColumns.isEmpty()) {
				for (DataColumn dc : loadedColumns) {
					dc.deletable = deletable;
				}
				if (postEdit) {
					int col = dataTable.getColumnCount() - 1;
					// post edit: target is column, value is data column
					TableEdit edit = dataTable.new TableEdit(DataToolTable.INSERT_COLUMN_EDIT, next.getYColumnName(),
							Integer.valueOf(col), next);
					undoSupport.postEdit(edit);
				}
				refreshDataBuilder();
			}
		}
		dataTable.refreshUndoItems();
		refreshGUI();
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

	@Override
	public void setName(String name) {
		name = replaceSpacesWithUnderscores(name);
		super.setName(name);
		if (dataTool != null) {
			dataTool.refreshTabTitles();
		}
	}

	/**
	 * Sets the userEditable flag.
	 *
	 * @param editable true to enable user editing
	 */
	public void setUserEditable(boolean editable) {
		if (userEditable == editable) {
			return;
		}
		userEditable = editable;
		refreshGUI();
	}

	/**
	 * Returns true if this tab is user editable.
	 *
	 * @return true if user editable
	 */
	public boolean isUserEditable() {
		return userEditable && !originShiftEnabled;
	}

	/**
	 * Gets the data builder for defining custom data functions.
	 * 
	 * @return the data builder
	 */
	public FunctionTool getDataBuilder() {
		if (dataTool != null) {
			return dataTool.getDataBuilder();
		}
		if (dataBuilder == null) { // create new tool if none exists
			dataBuilder = new FunctionTool(this) {

				@Override
				protected void setTitles() {
					dropdownTipText = (ToolsRes.getString("DataTool.DataBuilder.Dropdown.Tooltip")); //$NON-NLS-1$
					titleText = (ToolsRes.getString("DataTool.DataBuilder.Title")); //$NON-NLS-1$
				}

			};
			dataBuilder.setFontLevel(FontSizer.getLevel());
			dataBuilder.setHelpPath("data_builder_help.html"); //$NON-NLS-1$
			dataBuilder.addPropertyChangeListener(FunctionTool.PROPERTY_FUNCTIONTOOL_FUNCTION, this); // $NON-NLS-1$
		}
		refreshDataBuilder();
		return dataBuilder;
	}

	/**
	 * Listens for property change "function".
	 *
	 * @param e the event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		stopFitTimer();
		switch (e.getPropertyName()) {
		case DataFunctionEditor.PROPERTY_DATAFUNCTIONEDITOR_FUNCTION:
			tabChanged(true);
			dataTable.refreshTable(DataTable.MODE_FUNCTION);
			statsTable.refreshStatistics();
			if (e.getNewValue() instanceof DataFunction) { // new function has been created
				String funcName = e.getNewValue().toString();
				dataTable.getWorkingData(funcName);
			}
			if (e.getOldValue() instanceof DataFunction) { // function has been deleted
				String funcName = e.getOldValue().toString();
				dataTable.removeWorkingData(funcName);
			}
			if (e.getNewValue() instanceof String) {
				String funcName = e.getNewValue().toString();
				if (e.getOldValue() instanceof String) { // function name has changed
					String prevName = e.getOldValue().toString();
					columnNameChanged(prevName, funcName);
				} else {
					dataTable.getWorkingData(funcName);
				}
			}
			refreshPlot();
			varPopup = null;
			break;
		}
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
		// log the job in
		jobManager.log(job, replyTo);
		// if control is for a Data object, load it into this tab
		if (Data.class.isAssignableFrom(control.getObjectClass())) {
			Data data = (Data) control.loadObject(null, true, true);
			loadData(data, replaceColumnsWithMatchingNames);
			jobManager.associate(job, dataManager);
			refreshGUI();
		}
		// if control is for DataToolTab class, load this tab from control
		else if (DataToolTab.class.isAssignableFrom(control.getObjectClass())) {
			control.loadObject(this);
			refreshGUI();
		}
	}

	/**
	 * Adds a fit function. UserFunctions can optionally be added to the fit
	 * builder.
	 *
	 * @param f               the fit function to add
	 * @param addToFitBuilder true to add a UserFunction to the fit builder
	 */
	public void addFitFunction(KnownFunction f, boolean addToFitBuilder) {
		getCurveFitter().addFitFunction(f, addToFitBuilder);
	}

	/**
	 * Clears all data.
	 * 
	 * @param postEdit true to post an undoable edit
	 */
	public void clearData(boolean postEdit) {
		ArrayList<String> colNames = new ArrayList<String>();
		for (Dataset next : dataManager.getDatasetsRaw()) {
			colNames.add(next.getYColumnName());
		}
		dataTable.setSelectedColumnNames(colNames);
		dataTable.deleteSelectedColumns(postEdit); // posts undoable edits
	}

	/**
	 * Sets the replaceColumnsWithMatchingNames flag.
	 *
	 * @param replace true to replace columns with same name but different ID
	 */
	public void setReplaceColumnsWithMatchingNames(boolean replace) {
		replaceColumnsWithMatchingNames = replace;
	}

	/**
	 * Returns true if this tab is interested in a Data object.
	 *
	 * @param data the Data object
	 * @return true if data is of interest
	 */
	public boolean isInterestedIn(Data data) {
		if (data == null)
			return false;
		if (isOwnedBy(data))
			return true;
		Collection<Tool> tools = jobManager.getTools(dataManager);
		for (Tool tool : tools) {
			if (tool instanceof DataRefreshTool) {
				DataRefreshTool refresher = (DataRefreshTool) tool;
				if (refresher.moreData.contains(data))
					return true;
			}
		}
		return false;
	}

	/**
	 * Sets DataColumn IDs to corresponding column owner IDs based on saved names.
	 * Call this after loading this tab from XML to set column IDs to column owner
	 * IDs.
	 * 
	 * @param columnOwnerName the guest name
	 * @param data            the guest Data
	 * @return true if any column IDs were changed
	 */
	public boolean setOwnedColumnIDs(String columnOwnerName, Data data) {
		// only match column names associated with this column owner name
		Set<String> namesToMatch = new HashSet<String>();
		for (String colName : ownedColumns.keySet()) {
			String[] dataNames = ownedColumns.get(colName);
			if (dataNames != null && dataNames[0].equals(columnOwnerName)) {
				namesToMatch.add(colName);
			}
		}
		Map<DataColumn, Dataset> matches = getColumnMatchesByName(namesToMatch, data);
		for (DataColumn column : matches.keySet()) {
			Dataset match = matches.get(column);
			column.setID(match.getID());
		}
		return !matches.isEmpty();
	}

	/**
	 * Saves DataColumn names with associated column owner and Data object. Call
	 * this before saving this tab so owned columns will be saved in XML.
	 * 
	 * @param columnOwnerName the guest name
	 * @param data            the guest Data
	 */
	public void saveOwnedColumnNames(String columnOwnerName, Data data) {
		Map<DataColumn, Dataset> matches = getColumnMatchesByID(data);
		for (DataColumn column : matches.keySet()) {
			Dataset match = matches.get(column);
			ownedColumns.put(column.getYColumnName(), new String[] { columnOwnerName, match.getYColumnName() });
		}
	}

	/**
	 * Gets the column name for the first DataColumn with a given ID.
	 * 
	 * @param ID the ID number of the desired column
	 * @return the tab column name, or null if not found
	 */
	public String getColumnName(int ID) {
		for (Dataset column : dataManager.getDatasetsRaw()) {
			if (column.getID() == ID)
				return column.getYColumnName();
		}
		return null;
	}

	/**
	 * Returns true if (a) the Data ID is this tab owner's ID or (b) the Data name
	 * is this tab's name.
	 *
	 * @param data the Data object
	 * @return true if data owns this tab
	 */
	public boolean isOwnedBy(Data data) {
		if (data == null)
			return false;
		// return true if data name is the name of this tab
		String name = data.getName();
		if ((name != null) && replaceSpacesWithUnderscores(name).equals(getName())) {
			return true;
		}
		// return true if data ID is the originator of this tab
		return data.getID() == originatorID;
	}

	/**
	 * Sets the owner of this tab. This method is used before saving and after
	 * loading this tab so the tab can refresh its data from a new owner.
	 * 
	 * @param name the owner name
	 * @param data the owner Data
	 */
	public void setOwner(String name, Data data) {
		ownerName = name;
		originatorID = data.getID();
	}

	/**
	 * Gets the name of the owner of this tab. May return null, even if an owner
	 * exists.
	 * 
	 * @return the name of the owner
	 */
	public String getOwnerName() {
		return ownerName;
	}

	/**
	 * Refreshes the data by sending a request to the source. Note that this only
	 * works if the data was received from a DataRefreshTool.
	 */
	public void refreshData() {
		// set dataManager name to tab name so reply will be recognized
		dataManager.setName(getName());
		jobManager.sendReplies(dataManager);
	}

	// _______________________ protected & private methods
	// __________________________

	/**
	 * Adds a DataColumn to this tab.
	 *
	 * @param column the column to add
	 */
	protected void addColumn(DataColumn column) {
		String name = column.getYColumnName();
		String yName = dataManager.uniquifyColumnName(column, name);
		if (!name.equals(yName)) {
			String xName = column.getXColumnName();
			column.setXYColumnNames(xName, yName);
		}
		if (dataManager.getDatasetsRaw().isEmpty()) {
			column.setMarkerColor(Color.BLACK);
			column.setLineColor(Color.BLACK);
		}
		dataManager.addDataset(column); // was true here - for vis?
		dataTable.getWorkingData(yName);
	}

	/**
	 * Determines if a dataset is deletable.
	 *
	 * @param data the dataset
	 * @return true if deletable
	 */
	protected boolean isDeletable(Dataset data) {
		if (data == null) {
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
	 * Replaces spaces with underscores in a name.
	 *
	 * @param name the name with spaces
	 * @return the name with underscores
	 */
	protected String replaceSpacesWithUnderscores(String name) {
		name.trim();
		int n = name.indexOf(" "); //$NON-NLS-1$
		while (n > -1) {
			name = name.substring(0, n) + "_" + name.substring(n + 1); //$NON-NLS-1$
			n = name.indexOf(" "); //$NON-NLS-1$
		}
		return name;
	}

	/**
	 * Refreshes the data builder.
	 */
	protected void refreshDataBuilder() {
		if (dataTool != null) {
			dataTool.refreshDataBuilder();
			return;
		}
		if (dataBuilder == null) {
			return;
		}
		if (dataBuilder.getPanel(getName()) == null) {
			FunctionPanel panel = new DataFunctionPanel(dataManager);
			dataBuilder.addPanel(getName(), panel);
		}
		for (String name : dataBuilder.trackFunctionPanels.keySet()) {
			if (!name.equals(getName())) {
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
		FontSizer.setFonts(this, level);

		// FontSizer.setFonts(statsTable, level);
		// FontSizer.setFonts(propsTable, level);

		getCurveFitter().setFontLevel(level);

		double factor = FontSizer.getFactor(level);
		plot.getAxes().resizeFonts(factor, plot);
		FontSizer.setFonts(plot.getPopupMenu(), level);
		if (propsTable.styleDialog != null) {
			FontSizer.setFonts(propsTable.styleDialog, level);
			propsTable.styleDialog.pack();
		}
		if (dataBuilder != null) {
			dataBuilder.setFontLevel(level);
		}
		Action a = isFitterVisible()? showFitterAction: hideFitterAction;
		a.actionPerformed(null);
		propsTable.refreshTable();

		// set shift field and label fonts in case they are not currently displayed
		FontSizer.setFonts(shiftXLabel, level);
		FontSizer.setFonts(shiftYLabel, level);
		FontSizer.setFonts(selectedXLabel, level);
		FontSizer.setFonts(selectedYLabel, level);
		FontSizer.setFonts(shiftXField, level);
		FontSizer.setFonts(shiftYField, level);
		FontSizer.setFonts(selectedXField, level);
		FontSizer.setFonts(selectedYField, level);
		shiftXField.refreshPreferredWidth();
		shiftYField.refreshPreferredWidth();
		selectedXField.refreshPreferredWidth();
		selectedYField.refreshPreferredWidth();
		toolbar.revalidate();

		refreshStatusBar(null);
		// BH ????
		// kludge to display tables correctly: do propsAndStatsAction now, again after a
		// millisecond!
		propsAndStatsAction.actionPerformed(null);
		Timer timer = new Timer(1, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				propsAndStatsAction.actionPerformed(null);
			}
		});
		timer.setRepeats(false);
		timer.start();
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
//    dataTable.getSelectedData();
		return dataTable.workingData;
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
		if ((propsTable.styleDialog != null) && propsTable.styleDialog.isVisible()
				&& propsTable.styleDialog.getName().equals(oldName)) {
			propsTable.styleDialog.setName(newName);
			String title = ToolsRes.getString("DataToolPropsTable.Dialog.Title"); //$NON-NLS-1$
			String var = TeXParser.removeSubscripting(newName);
			propsTable.styleDialog.setTitle(title + " \"" + var + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		statsTable.refreshStatistics();
		Dataset working = getWorkingData();
		if (working == null) {
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
		if (!dataManager.getDatasetsRaw().isEmpty()) {
			colorIndex++;
		}
		DataColumn column = new DataColumn();
		column.setMarkerColor(markerColor);
		column.setLineColor(lineColor);
		column.setConnected(false);
		int rowCount = Math.max(1, dataTable.getRowCount());
		double[] y = new double[rowCount];
		Arrays.fill(y, Double.NaN);
		column.setPoints(y, rowCount);
		column.setXColumnVisible(false);
		return column;
	}

	/**
	 * Saves the selected table data to a file selected with a fileChooser.
	 *
	 * @return the path of the saved file or null if failed
	 */
	protected String saveTableDataToFile() {
		String tabName = getName();
		OSPLog.finest("saving table data from " + tabName); //$NON-NLS-1$
		JFileChooser chooser = OSPRuntime.getChooser();
		chooser.setSelectedFile(new File(tabName + ".txt")); //$NON-NLS-1$
		FontSizer.setFonts(chooser, FontSizer.getLevel());
		int result = chooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
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
		OSPRuntime.copy(getSelectedTableData(), null);
	}

	/**
	 * Gets the table cells selected by the user. The tab name and column names
	 * precede the data. Data rows are delimited by new lines ("\n"), columns by
	 * tabs.
	 *
	 * @return a String containing the data.
	 */
	protected String getSelectedTableData() {
		StringBuffer buf = new StringBuffer();
		if (getName() != null) {
			buf.append(getName() + "\n"); //$NON-NLS-1$
		}
		int[] columns = null;
		if (dataTable.getRowCount() == 0) {
			columns = new int[dataTable.getColumnCount() - 1];
			for (int i = 0; i < columns.length; i++) 
				columns[i] = i + 1;
//			return buf.toString();
		}
//		if ((dataTable.getColumnCount() == 1) || (dataTable.getRowCount() == 0)) {
//			return buf.toString();
//		}
		dataTable.clearSelectionIfEmptyEndRow();
		// get selected rows and columns
		int[] rows = dataTable.getSelectedRows();
		// if no rows selected, select all
		if (rows.length == 0) {
			dataTable.selectAllCells();
			rows = dataTable.getSelectedRows();
		}
		if (columns == null)
			columns = dataTable.getSelectedColumns();
		// copy column headings
		for (int j = 0; j < columns.length; j++) {
			int col = columns[j];
			// ignore row heading
			int modelCol = dataTable.convertColumnIndexToModel(col);
			if (dataTable.isRowNumberVisible() && (modelCol == 0)) {
				continue;
			}
			buf.append(dataTable.getColumnName(col));
			buf.append("\t"); // tab after each column //$NON-NLS-1$
		}
		buf.setLength(buf.length() - 1); // remove last tab
		buf.append("\n"); //$NON-NLS-1$
		java.text.DateFormat df = java.text.DateFormat.getInstance();
		for (int i = 0; i < rows.length; i++) {
			for (int j = 0; j < columns.length; j++) {
				int col = columns[j];
				int modelCol = dataTable.convertColumnIndexToModel(col);
				// don't copy row numbers
				if (dataTable.isRowNumberVisible() && (modelCol == 0)) {
					continue;
				}
				Object value = dataTable.getValueAt(rows[i], col);
				if (value != null) {
					if (value instanceof java.util.Date) {
						value = df.format(value);
					}
					buf.append(value);
				}
				buf.append("\t"); // tab after each column //$NON-NLS-1$
			}
			buf.setLength(buf.length() - 1); // remove last tab
			buf.append("\n"); // new line after each row //$NON-NLS-1$
		}
		return buf.toString();
	}

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		if (haveGUI)
			return;
		haveGUI = true;
		ToolsRes.addPropertyChangeListener("locale", new PropertyChangeListener() { //$NON-NLS-1$
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				refreshGUI();
			}

		});

		setLayout(new BorderLayout());
		splitPanes = new JSplitPane[3];
		// splitPanes[0] is plot/fitter on left, tables on right
		splitPanes[0] = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPanes[0].setResizeWeight(0.7);
		splitPanes[0].setOneTouchExpandable(true);
		// splitPanes[1] is plot on top, fitter on bottom
		splitPanes[1] = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPanes[1].setResizeWeight(1);
		splitPanes[1].setDividerSize(0);
		// splitPanes[2] is stats/props tables on top, data table on bottom
		splitPanes[2] = new JSplitPane(JSplitPane.VERTICAL_SPLIT) {
			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				dim.width = dataTable.getMinimumTableWidth() + 6;
				JScrollBar scrollbar = dataScroller.getVerticalScrollBar();
				if (scrollbar.isVisible()) {
					dim.width += scrollbar.getWidth();
				}
				dim.height = 1;
				return dim;
			}
		};
		splitPanes[2].setDividerSize(0);
		splitPanes[2].setEnabled(false);

		// add ancestor listener to initialize
		this.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent e) {
//				OSPLog.getOSPLog(); // workaround needed for consistent initialization!
				if (getSize().width > 0) {
					init();
				}
			}

			@Override
			public void ancestorRemoved(AncestorEvent event) {
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {
			}

		});
		// add component listener for resizing
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				Action a = isFitterVisible()? showFitterAction: hideFitterAction;
				a.actionPerformed(null);
			}

		});
		// add window listener to dataTool to display curvefitter properly
		dataTool.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowOpened(java.awt.event.WindowEvent e) {
				Action a = isFitterVisible()? showFitterAction: hideFitterAction;
				a.actionPerformed(null);
			}
		});
		// configure data table
		dataTable.setRowNumberVisible(true);
		dataScroller = new JScrollPane(dataTable);
		dataTable.refreshTable(DataTable.MODE_CREATE);
		dataTable.addPropertyChangeListener(DataTable.PROPERTY_DATATABLE_FORMAT, (e) -> {
				refreshShiftFields();
		});

		dataScroller.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				dataTable.clearSelection();
			}
		});
		dataScroller.setToolTipText(ToolsRes.getString("DataToolTab.Scroller.Tooltip")); //$NON-NLS-1$
		dataTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnAdded(TableColumnModelEvent e) {
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {
			}

			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {
			}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
			}

			@Override
			public void columnMoved(TableColumnModelEvent e) {
				if (e.getFromIndex() - e.getToIndex() != 0)
					columnOrderChanged();
			}

		});
		// create show/hide fitter actions and fourier checkbox
		showFitterAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				splitPanes[1].setEnabled(true);
				getCurveFitter().setFontLevel(FontSizer.getLevel());
				splitPanes[1].setBottomComponent(curveFitter);
				splitPanes[1].setDividerSize(splitPanes[0].getDividerSize());
				splitPanes[1].setDividerLocation(-1);
				plot.addDrawable(curveFitter.getDrawer());
//				curveFitter.setActiveAndFit(true);
				if (e != null) {
					curveFitter.setSelectedItem(e.getActionCommand());
					refreshPlot();
				}
			}

		};
		
		hideFitterAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// remove curveFitter
				splitPanes[1].remove(curveFitter);
				splitPanes[1].setDividerSize(splitPanes[2].getDividerSize());
				splitPanes[1].setDividerLocation(1.0);
				plot.removeDrawables(FunctionDrawer.class);
				if (e != null) {
					refreshPlot();
				}
			}

		};
		
		fitMenu = new JMenu();
		
		fourierCheckbox = new JCheckBoxMenuItem();
		fourierCheckbox.setSelected(false);
		fourierCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (fourierPanel == null && dataTool != null) {
					// create fourier panel
					fourierPanel = new FourierPanel();
					fourierDialog = new JDialog(dataTool, false) {
						@Override
						public void setVisible(boolean vis) {
							super.setVisible(vis);
							fourierCheckbox.setSelected(vis);
						}
					};
					fourierDialog.setContentPane(fourierPanel);
					Dimension dim = new Dimension(640, 400);
					fourierDialog.setSize(dim);
					fourierPanel.splitPane.setDividerLocation(dim.width / 2);
					fourierPanel.refreshFourierData(dataTable.getSelectedData(), DataToolTab.this.getName());
					fourierDialog.setLocationRelativeTo(dataTool);
				}
				fourierDialog.setVisible(fourierCheckbox.isSelected());
			}

		});
		originShiftCheckbox = new JCheckBoxMenuItem();
		originShiftCheckbox.setSelected(originShiftEnabled);
		originShiftCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean previouslyEnabled = originShiftEnabled;
				originShiftEnabled = originShiftCheckbox.isSelected();
				double shiftX = 0;
				// set all columns except row column to shifted
				for (int i = 1; i < dataTable.getColumnCount(); i++) {
					String colName = dataTable.getColumnName(i);
					Dataset data = dataTable.getDataset(colName);
					if (data != null && data instanceof DataColumn) {
						DataColumn dataCol = (DataColumn) data;
						dataCol.setShifted(originShiftEnabled);
						if (i == 1) {
							shiftX = dataCol.getShift();
						}
					}
				}
				if (originShiftEnabled) {
					toolbar.add(shiftXLabel, 2);
					toolbar.add(shiftXSpinner, 3);
					toolbar.add(shiftYLabel, 4);
					toolbar.add(shiftYSpinner, 5);
					if (!previouslyEnabled) {
						// shift area limits
						if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
							plot.areaLimits[0].refreshX();
							plot.areaLimits[1].refreshX();
						} else {
							plot.areaLimits[0].setX(plot.areaLimits[0].getX() + shiftX);
							plot.areaLimits[1].setX(plot.areaLimits[1].getX() + shiftX);
						}
					}
					((CrawlerSpinnerModel) shiftXSpinner.getModel()).refreshDelta();
					((CrawlerSpinnerModel) shiftYSpinner.getModel()).refreshDelta();
				} else {
					toolbar.remove(shiftXLabel);
					toolbar.remove(shiftXSpinner);
					toolbar.remove(shiftYLabel);
					toolbar.remove(shiftYSpinner);
					toolbar.remove(selectedXLabel);
					toolbar.remove(selectedXField);
					toolbar.remove(selectedYLabel);
					toolbar.remove(selectedYField);
					if (previouslyEnabled) {
						// shift area limits
						if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
							plot.areaLimits[0].refreshX();
							plot.areaLimits[1].refreshX();
						} else {
							plot.areaLimits[0].setX(plot.areaLimits[0].getX() - shiftX);
							plot.areaLimits[1].setX(plot.areaLimits[1].getX() - shiftX);
						}

					}
				}
				toolbar.validate();
				refreshAll(DataTable.MODE_VALUES);
				prevShiftX = -shiftXField.getValue();
				prevShiftY = -shiftYField.getValue();
			}
		});
		measureFitCheckbox = new JCheckBoxMenuItem();
		measureFitCheckbox.setSelected(false);
		measureFitCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				measureFit = measureFitCheckbox.isSelected();
				if (areaVisible) {
					plot.refreshArea();
				}
				plot.refreshMeasurements();
				plot.repaint();
			}
		});
		// create newColumnButton button
		newColumnButton = DataTool.createButton(""); //$NON-NLS-1$
		newColumnButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				DataColumn column = createDataColumn();
				String newName = dataManager.getUniqueYColumnName(DataToolTab.this, column, ToolsRes.getString("DataToolTab.NewColumn.Name"));
				if (newName == null) {
					return;
				}
				if (newName.equals("")) { //$NON-NLS-1$
					String colName = ToolsRes.getString("DataToolTab.NewColumn.Name"); //$NON-NLS-1$
					newName = dataManager.uniquifyColumnName(column, colName);
				}
				column.setXYColumnNames("row", newName); //$NON-NLS-1$
				ArrayList<DataColumn> loadedColumns = loadData(column, false);
				if (!loadedColumns.isEmpty()) {
					for (DataColumn next : loadedColumns) {
						next.deletable = true;
					}
				}
				// post edit: target is column, value is dataset
				TableEdit edit = dataTable.new TableEdit(DataToolTable.INSERT_COLUMN_EDIT, newName,
						Integer.valueOf(dataTable.getColumnCount() - 1), column);
				undoSupport.postEdit(edit);
				dataTable.refreshUndoItems();
				SwingUtilities.invokeLater(() -> {
					int col = dataTable.getColumnCount() - 1;
					dataTable.changeSelection(0, col, false, false);
					dataTable.editCellAt(0, col, e);
					dataTable.editor.field.requestFocus();
				});
			}

		});
		// add mouse listeners to make sure new column name input field is editable
		newColumnButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (dataTable.getColumnCount() == 2) {
					newColumnButton.requestFocusInWindow();
				}
			}
		});
		newColumnButton.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (dataTable.getColumnCount() == 2) {
					newColumnButton.requestFocusInWindow();
				}
			}
		});
		
		
		// create edit data button
		editDataButton = DataTool.createButton("Edit Table as Text..."); 
		editDataButton.setToolTipText("Edit data as comma-delimited text"); 
		editDataButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataTool.editDataAction();
			}

		});
		
		// create dataBuilderButton
		dataBuilderButton = DataTool.createButton(ToolsRes.getString("DataToolTab.Button.DataBuilder.Text")); //$NON-NLS-1$
		dataBuilderButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Tooltip")); //$NON-NLS-1$
		dataBuilderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getDataBuilder().setVisible(true);
				getDataBuilder().setSelectedPanel(getName());
			}

		});
		// create refreshDataButton
		refreshDataButton = DataTool.createButton(ToolsRes.getString("DataToolTab.Button.Refresh.Text")); //$NON-NLS-1$
		refreshDataButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.Refresh.Tooltip")); //$NON-NLS-1$
		refreshDataButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshData();
			}

		});
		// create help button
		helpButton = DataTool.createButton(ToolsRes.getString("Tool.Button.Help")); //$NON-NLS-1$
		helpButton.setToolTipText(ToolsRes.getString("Tool.Button.Help.ToolTip")); //$NON-NLS-1$
		helpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DataTool.showHelp();
			}

		});
		
		// create valueCheckbox
		valueCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("DataToolTab.Checkbox.Position")); //$NON-NLS-1$
		valueCheckbox.setSelected(false);
		valueCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Position.Tooltip")); //$NON-NLS-1$
		valueCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				freezeMeasurement = false;
				positionVisible = valueCheckbox.isSelected();
				plot.setMessage(plot.createMessage());
				refreshStatusBar(null);
			}

		});
		// create slopeCheckbox
		slopeCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("DataToolTab.Checkbox.Slope")); //$NON-NLS-1$
		slopeCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Slope.Tooltip")); //$NON-NLS-1$
		slopeCheckbox.setSelected(false);
		slopeCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				freezeMeasurement = false;
				slopeVisible = slopeCheckbox.isSelected();
				plot.setMessage(plot.createMessage());
				refreshStatusBar(null);
			}
		});
		// create areaCheckbox
		areaCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("DataToolTab.Checkbox.Area")); //$NON-NLS-1$
		areaCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Area.Tooltip")); //$NON-NLS-1$
		areaCheckbox.setSelected(false);
		areaCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				plot.setAreaVisible(areaCheckbox.isSelected());
			}

		});
		// create measureButton
		measureButton = DataTool.createButton(ToolsRes.getString("DataToolTab.Button.Measure.Label")); //$NON-NLS-1$
		measureButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// build a popup menu with measure items
				JPopupMenu popup = new JPopupMenu();
				popup.add(valueCheckbox);
				popup.add(slopeCheckbox);
				popup.add(areaCheckbox);
				popup.addSeparator();
				popup.add(originShiftCheckbox);
				if (isFitterVisible()) {
					popup.addSeparator();
					popup.add(measureFitCheckbox);
				}
				FontSizer.setFonts(popup, FontSizer.getLevel());
				popup.show(measureButton, 0, measureButton.getHeight());
			}
		});
		// create analyzeButton
		analyzeButton = DataTool.createButton(ToolsRes.getString("DataToolTab.Button.Analyze.Label")); //$NON-NLS-1$
		analyzeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// build a popup menu with analyze items
				JPopupMenu popup = new JPopupMenu();
				fitMenu.removeAll();
				String[] fitNames = getCurveFitter().getFitNames();
				for (int i = 0; i < fitNames.length; i++) {
					JMenuItem item = new JMenuItem(FitBuilder.localize(fitNames[i]));
					item.setActionCommand(fitNames[i]);
					item.addActionListener(showFitterAction);
					fitMenu.add(item);
				}
				popup.add(fitMenu);
				if (isFitterVisible()) {
					JMenuItem item = new JMenuItem(ToolsRes.getString("DataToolTab.MenuItem.CloseFitter.Text"));
					item.addActionListener(hideFitterAction);
					popup.add(item);
				}
				popup.addSeparator();
				popup.add(statsCheckbox);
				popup.add(fourierCheckbox);
				FontSizer.setFonts(popup, FontSizer.getLevel());
				popup.show(analyzeButton, 0, analyzeButton.getHeight());
			}
		});

		// create propsAndStatsAction
		propsAndStatsAction = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// lay out the table bar split panes
				boolean statsVis = statsCheckbox.isSelected();
				boolean propsVis = propsCheckbox.isSelected();
				if (statsVis) {
					statsTable.refreshStatistics();
				}
				refreshStatusBar(null);
				int statsHeight = statsTable.getPreferredSize().height;
				int propsHeight = propsTable.getPreferredSize().height;
				LookAndFeel currentLF = UIManager.getLookAndFeel();
				int h = (currentLF.getClass().getName().indexOf("Nimbus") > -1) //$NON-NLS-1$
						? 8
						: 4;
				if (statsVis && propsVis) {
					Box box = Box.createVerticalBox();
					box.add(statsScroller);
					box.add(propsScroller);
					splitPanes[2].setTopComponent(box);
					splitPanes[2].setDividerLocation(statsHeight + propsHeight + 2 * h);
				} else if (statsVis) {
					splitPanes[2].setTopComponent(statsScroller);
					splitPanes[2].setDividerLocation(statsHeight + h);
				} else if (propsVis) {
					splitPanes[2].setTopComponent(propsScroller);
					splitPanes[2].setDividerLocation(propsHeight + h);
				} else {
					splitPanes[2].setDividerLocation(0);
				}

			}

		};
		// create stats checkbox
		statsCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("Checkbox.Statistics.Label"), false); //$NON-NLS-1$
		statsCheckbox.setToolTipText(ToolsRes.getString("Checkbox.Statistics.ToolTip")); //$NON-NLS-1$
		statsCheckbox.addActionListener(propsAndStatsAction);
		// create style properties checkbox
		propsCheckbox = new JCheckBoxMenuItem(ToolsRes.getString("DataToolTab.Checkbox.Properties.Text"), true); //$NON-NLS-1$
		propsCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Properties.Tooltip")); //$NON-NLS-1$
		propsCheckbox.addActionListener(propsAndStatsAction);

		// create curve fitter
		FitBuilder fitBuilder = dataTool.getFitBuilder();
		curveFitter = new DatasetCurveFitter(getWorkingData(), fitBuilder);
		curveFitter.setDataToolTab(this);
		fitBuilder.curveFitters.add(curveFitter);
		curveFitter.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				switch (e.getPropertyName()) {
				case DatasetCurveFitter.PROPERTY_DATASETCURVEFITTER_CHANGED:
					tabChanged(true);
					if (isFitterVisible())
						plot.repaint();
					break;
				case DatasetCurveFitter.PROPERTY_DATASETCURVEFITTER_DRAWER:
					if (isFitterVisible()) {
						plot.removeDrawables(FunctionDrawer.class);
						// add fit drawer to plot drawable
						plot.addDrawable((FunctionDrawer) e.getNewValue());
						plot.repaint();
					}
					break;
				}
			}
		});

		// create plotting panel and axes
		plot = new DataToolPlotter(getWorkingData());
		plotAxes = new DataToolAxes(plot);
		plot.setAxes(plotAxes);
		if (getWorkingData() != null) {
			plot.addDrawable(getWorkingData());
			plot.setTitle(getWorkingData().getName());
		}
		// set new CoordinateStringBuilder
		plot.stringBuilder = plot.new PlotCoordinateStringBuilder();
		plot.setCoordinateStringBuilder(plot.stringBuilder);

		// create mouse listener to manage states/cursors and select data points in plot
		MouseInputListener mouseSelector = new MouseInputAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				mouseState = getMouseState(e);
				switch (mouseState) {
				case STATE_ZOOM:
					plot.setMouseCursor(SELECT_ZOOM_CURSOR);
				case STATE_MOVE: // cursor set by axes
					return;					
				case STATE_SELECT:
					if (plot.interactive == null) {
						plot.setMouseCursor(SELECT_CURSOR);
					}
					break;
				case STATE_SELECT_ADD:
					if (plot.interactive == null) {
						plot.setMouseCursor(SELECT_ADD_CURSOR);
					}
					break;
				case STATE_SELECT_REMOVE:
					if (plot.interactive == null) {
						plot.setMouseCursor(SELECT_REMOVE_CURSOR);
					}
				}
				mousePressedAction(e, e.isControlDown(), e.isShiftDown());
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				boolean add = e.isShiftDown();
				boolean remove = e.isControlDown();
				switch (mouseState) {
				case STATE_ZOOM:
					plot.setMouseCursor(SELECT_ZOOM_CURSOR);
				case STATE_MOVE:
					return;					
				case STATE_SELECT:
				case STATE_SELECT_ADD:
				case STATE_SELECT_REMOVE:
					mouseState = remove ? STATE_SELECT_REMOVE: 
						add? STATE_SELECT_ADD: STATE_SELECT;
					plot.setMouseCursor(remove ? SELECT_REMOVE_CURSOR : 
						add? SELECT_ADD_CURSOR: SELECT_CURSOR);
				}
				mouseDraggedAction(e, e.isControlDown(), add);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mouseState = STATE_INACTIVE;
				mouseReleasedAction(e, e.isControlDown(), e.isShiftDown());
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				if (!freezeMeasurement) {
					plot.measurementX = e.getX();
					plot.measurementIndex = -1;
				}
				plot.refreshMeasurements();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				dataTable.dataToolTab.refreshStatusBar(null);
			}


		};
		plot.addMouseListener(mouseSelector);
		plot.addMouseMotionListener(mouseSelector);

		// create toolbar
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBorder(BorderFactory.createEtchedBorder());
		toolbar.add(measureButton);
		toolbar.add(analyzeButton);
		toolbar.add(Box.createGlue());
		toolbar.add(newColumnButton);
		toolbar.add(dataBuilderButton);
		toolbar.add(refreshDataButton);
		if (isUserEditable())
			toolbar.add(editDataButton);
		toolbar.add(helpButton);

		// create statistics table
		statsTable = new DataToolStatsTable(dataTable);
		statsScroller = new JScrollPane(statsTable) {
			@Override
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
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals(DataToolPropsTable.PROPERTY_PROPTABLE_DISPLAY)) { //$NON-NLS-1$
					refreshPlot();
				}
			}
		});
		propsScroller = new JScrollPane(propsTable) {
			@Override
			public Dimension getPreferredSize() {
				Dimension dim = propsTable.getPreferredSize();
				return dim;
			}
		};
		propsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		propsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		// create laels
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
//		splitPanes[1].setBottomComponent(curveFitter);
		splitPanes[2].setBottomComponent(dataScroller);

		// set up the undo system
		undoManager = new UndoManager();
		undoSupport = new UndoableEditSupport();
		undoSupport.addUndoableEditListener(undoManager);

		// create origin shift labels and fields
		shiftXLabel = new JLabel();
		shiftXLabel.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 2));
		shiftYLabel = new JLabel();
		shiftYLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 2));
		selectedXLabel = new JLabel();
		selectedXLabel.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 2));
		selectedYLabel = new JLabel();
		selectedYLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 2));

		// create shift spinner and field listeners
		// BH TODO note that this timer runs whether or not the shifters are shown
		shiftEditListener = new ShiftEditListener();
		KeyAdapter numberFieldKeyListener = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				JComponent comp = (JComponent) e.getSource();
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					comp.setBackground(Color.white);
				} else {
					comp.setBackground(Color.yellow);
				}
			}
		};
		FocusAdapter numberFieldFocusListener = new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				NumberField field = (NumberField) e.getSource();
				if (field.getBackground() != Color.white) {
					field.setBackground(Color.white);
					field.postActionEvent();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				NumberField field = (NumberField) e.getSource();
				field.selectAll();
			}
		};

		shiftXField = new NumberField(4) {
			@Override
			public Dimension getMaximumSize() {
				Dimension dim = getPreferredSize();
				dim.height = super.getMaximumSize().height;
				return dim;
			}
		};
		shiftXField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Dataset data = dataTable.getDataset(plot.xVar);
				if (data != null && data instanceof DataColumn) {
					DataColumn col = (DataColumn) data;
					double prevX = col.getShift();
					double shiftX = -shiftXField.getValue();
					if (col.setShift(shiftX)) {
						tabChanged(true);
						// shift area limits
						if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
							plot.areaLimits[0].refreshX();
							plot.areaLimits[1].refreshX();
						} else {
							plot.areaLimits[0].setX(plot.areaLimits[0].getX() + shiftX - prevX);
							plot.areaLimits[1].setX(plot.areaLimits[1].getX() + shiftX - prevX);
						}

						refreshAll(DataTable.MODE_VALUES);
						((CrawlerSpinnerModel) shiftXSpinner.getModel()).refreshDelta();
						dataTable.dorepaint(5);
					}
				}
				shiftXField.selectAll();
			}

		});
		shiftXField.addKeyListener(numberFieldKeyListener);
		shiftXField.addFocusListener(numberFieldFocusListener);

		SpinnerModel spinModel = new CrawlerSpinnerModel();
		shiftXSpinner = new JSpinner(spinModel) {

			@Override
			public Dimension getMaximumSize() {
				Dimension dim = super.getMaximumSize();
				dim.width = getPreferredSize().width;
				return dim;
			}

			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				for (Component c : this.getComponents()) {
					if (c instanceof JButton) {
						dim.width = shiftXField.getPreferredSize().width + c.getWidth() - 2;
						return dim;
					}
				}
				return dim;
			}
		};
		shiftXSpinner.setEditor(shiftXField);
		ChangeListener xChangeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				Dataset data = dataTable.getDataset(plot.xVar);
				if (data != null && data instanceof DataColumn) {
					DataColumn col = (DataColumn) data;
					double prevX = col.getShift();
					double shiftX = -(Double) shiftXSpinner.getValue();
					if (col.setShift(shiftX)) {
						tabChanged(true);
						// shift area limits
						if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
							plot.areaLimits[0].refreshX();
							plot.areaLimits[1].refreshX();
						} else {
							plot.areaLimits[0].setX(plot.areaLimits[0].getX() + shiftX - prevX);
							plot.areaLimits[1].setX(plot.areaLimits[1].getX() + shiftX - prevX);
						}

						refreshAll(DataTable.MODE_VALUES);
						dataTable.dorepaint(5);
					}
				}
			}
		};
		shiftXSpinner.addChangeListener(xChangeListener);
		shiftXSpinner.addChangeListener(shiftEditListener);

		shiftYField = new NumberField(4) {
			@Override
			public Dimension getMaximumSize() {
				Dimension dim = getPreferredSize();
				dim.height = super.getMaximumSize().height;
				return dim;
			}
		};
		shiftYField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Dataset data = dataTable.getDataset(plot.yVar);
				if (data != null && data instanceof DataColumn) {
					DataColumn col = (DataColumn) data;
					if (col.setShift(-shiftYField.getValue())) {
						tabChanged(true);
						refreshAll(DataTable.MODE_VALUES);
						((CrawlerSpinnerModel) shiftYSpinner.getModel()).refreshDelta();
						dataTable.dorepaint(5);
					}
				}
				shiftYField.selectAll();
			}
		});
		shiftYField.addKeyListener(numberFieldKeyListener);
		shiftYField.addFocusListener(numberFieldFocusListener);

		spinModel = new CrawlerSpinnerModel();
		shiftYSpinner = new JSpinner(spinModel) {

			@Override
			public Dimension getMaximumSize() {
				Dimension dim = super.getMaximumSize();
				dim.width = getPreferredSize().width;
				return dim;
			}

			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				for (Component c : this.getComponents()) {
					if (c instanceof JButton) {
						dim.width = shiftYField.getPreferredSize().width + c.getWidth() - 2;
						return dim;
					}
				}
				return dim;
			}
		};
		shiftYSpinner.setEditor(shiftYField);

		ChangeListener yChangeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				Dataset data = dataTable.getDataset(plot.yVar);
				if (data != null && data instanceof DataColumn) {
					DataColumn col = (DataColumn) data;
					double shiftY = -(Double) shiftYSpinner.getValue();
					if (col.setShift(shiftY)) {
						tabChanged(true);
						refreshAll(DataTable.MODE_VALUES);
						dataTable.dorepaint(5);
					}
				}
			}
		};
		shiftYSpinner.addChangeListener(yChangeListener);
		shiftYSpinner.addChangeListener(shiftEditListener);

		selectedXField = new NumberField(4) {
			@Override
			public Dimension getMaximumSize() {
				Dimension dim = getPreferredSize();
				dim.height = super.getMaximumSize().height;
				return dim;
			}
		};
		selectedXField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Dataset data = dataTable.getDataset(plot.xVar);
				if (data != null && data instanceof DataColumn) {
					DataColumn col = (DataColumn) data;
					double prev = col.getShift();
					double val = selectedXField.getValue();
					if (col.setShiftedValue(selectedDataIndex, val)) {
						tabChanged(true);
						// shift area limits
						if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
							plot.areaLimits[0].refreshX();
							plot.areaLimits[1].refreshX();
						} else {
							double shift = col.getShift();
							plot.areaLimits[0].setX(plot.areaLimits[0].getX() + shift - prev);
							plot.areaLimits[1].setX(plot.areaLimits[1].getX() + shift - prev);
						}
						refreshAll(DataTable.MODE_SELECT);
						((CrawlerSpinnerModel) shiftXSpinner.getModel()).refreshDelta();
						dataTable.dorepaint(5);
					}
				}
				selectedXField.requestFocusInWindow();
				selectedXField.selectAll();
				shiftEditListener.stateChanged(null);
			}

		});
		selectedXField.addKeyListener(numberFieldKeyListener);
		selectedXField.addFocusListener(numberFieldFocusListener);

		selectedYField = new NumberField(4) {
			@Override
			public Dimension getMaximumSize() {
				Dimension dim = getPreferredSize();
				dim.height = super.getMaximumSize().height;
				return dim;
			}
		};
		selectedYField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Dataset data = dataTable.getDataset(plot.yVar);
				if (data != null && data instanceof DataColumn) {
					DataColumn col = (DataColumn) data;
					double val = selectedYField.getValue();
					if (col.setShiftedValue(selectedDataIndex, val)) {
						tabChanged(true);
						refreshAll(DataTable.MODE_SELECT);
						((CrawlerSpinnerModel) shiftYSpinner.getModel()).refreshDelta();
						dataTable.dorepaint(5);
					}
				}
				selectedYField.requestFocusInWindow();
				selectedYField.selectAll();
				shiftEditListener.stateChanged(null);
			}

		});
		selectedYField.addKeyListener(numberFieldKeyListener);
		selectedYField.addFocusListener(numberFieldFocusListener);

	}

	protected void columnOrderChanged() {
		tabChanged(true);
		plot.selectionBox.setSize(0, 0);
		refreshPlot(true);
		plot.repaint();
		prevShiftY = refreshShiftFields();		 
	}
	
	private int getMouseState(MouseEvent e) {
		boolean button3 = (e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK;
		boolean shift = e.isShiftDown();
		boolean ctrl = e.isControlDown();
		boolean zoom = button3 || OSPRuntime.isPopupTrigger(e);
		boolean remove = OSPRuntime.isMac()? shift && ctrl: ctrl;
		return zoom? STATE_ZOOM: 
			e.isAltDown()? STATE_MOVE: 
			remove? STATE_SELECT_REMOVE:
			shift? 	STATE_SELECT_ADD: 
							STATE_SELECT;
	}

	protected void mouseDraggedAction(MouseEvent e, boolean controlDown, boolean shiftDown) {
		Point point = e.getPoint();
		selectionChanged = true;
		if (mouseDrawable == plot.origin) {
			plot.selectionBox.visible = false;
			double deltaX = 0, deltaY = 0;
			int dx = plot.origin.mouseDownPt.x - point.x;
			int dy = plot.origin.mouseDownPt.y - point.y;
			if (shiftDown) {
				if (Math.abs(dx) >= Math.abs(dy))
					dy = 0;
				else
					dx = 0;
			}
			Dataset data = dataTable.getDataset(plot.xVar);
			if (data != null && data instanceof DataColumn && plot.origin.isVertHit) {
				deltaX = dx / plot.getXPixPerUnit();
				double shift = prevShiftX + deltaX;
				DataColumn col = (DataColumn) data;
				double prev = col.getShift();
				col.setShift(shift);
				tabChanged(true);
				// shift area limits
				if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
					plot.areaLimits[0].refreshX();
					plot.areaLimits[1].refreshX();
				} else {
					plot.areaLimits[0].setX(plot.areaLimits[0].getX() + shift - prev);
					plot.areaLimits[1].setX(plot.areaLimits[1].getX() + shift - prev);
				}
			}
			data = dataTable.getDataset(plot.yVar);
			if (data != null && data instanceof DataColumn && plot.origin.isHorzHit) {
				deltaY = -dy / plot.getYPixPerUnit();
				double shiftY = prevShiftY + deltaY;
				DataColumn col = (DataColumn) data;
				col.setShift(shiftY);
				tabChanged(true);
			}

			refreshAll(DataTable.MODE_VALUES);
			plot.lockedXMin = plot.mouseDownXMin + deltaX;
			plot.lockedXMax = plot.mouseDownXMax + deltaX;
			plot.lockedYMin = plot.mouseDownYMin + deltaY;
			plot.lockedYMax = plot.mouseDownYMax + deltaY;
			plot.repaint();
			
			return;
		}

		if (mouseState == STATE_INACTIVE) {
			return;
		}
		Dataset data = getWorkingData();
		if (data == null) {
			return;
		}
		
		// don't show selection box if adjusting an area limit line or zooming
		boolean isSelectionBoxActive = mouseState != STATE_ZOOM &&
				mouseDrawable != plot.areaLimits[0] && 
				mouseDrawable != plot.areaLimits[1];
		plot.selectionBox.visible = isSelectionBoxActive;
		
		int dx = point.x - plot.selectionBox.xstart;
		int dy = point.y - plot.selectionBox.ystart;
		plot.selectionBox.setSize(dx, dy);
		selectionBoxChanged = true;
		refreshFit();
		plot.repaint();
	}

	protected void mouseReleasedAction(MouseEvent e, boolean controlDown, boolean shiftDown) {
		Point point = e.getPoint();
		if (!selectionChanged && freezeMeasurement) {
			freezeMeasurement = false;
			plot.measurementX = point.x;
			plot.measurementIndex = -1;
			plot.refreshMeasurements();
		}
		selectionChanged = false;
		plot.lockScale(false);
		plot.selectionBox.visible = false;
		if (mouseDrawable != null) {
			if (mouseDrawable == plot.origin) {
				postShiftEdit();
				((CrawlerSpinnerModel) shiftXSpinner.getModel()).refreshDelta();
				((CrawlerSpinnerModel) shiftYSpinner.getModel()).refreshDelta();
				dataTable.dorepaint(5);
			}
			if (mouseDrawable == plot.areaLimits[0])
				plot.areaLimits[0].isAdjusting = false;
			else if (mouseDrawable == plot.areaLimits[1])
				plot.areaLimits[1].isAdjusting = false;
			
			if (mouseDrawable instanceof Selectable) {
				plot.setMouseCursor(((Selectable) mouseDrawable).getPreferredCursor());
			} else {
				plot.setMouseCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			if (mouseDrawable instanceof HighlightableDataset) {
				HighlightableDataset data = (HighlightableDataset) mouseDrawable;
				TableModel tableModel = dataTable.getModel();
				int yCol = dataTable.getYColumn();
				ListSelectionModel model = dataTable.getColumnModel().getSelectionModel();
				for (int i = tableModel.getColumnCount(); --i >= 1;) {
					if (data.getYColumnName().equals(dataTable.getColumnName(i)) && yCol != i) {
						// data.clearHighlights();
						data.setHighlightColor(Color.YELLOW);
						model.removeSelectionInterval(i, i);
						break;
					}
				}
			}
		}
		plot.repaint();
		if (timerToFindHits != null) {
			timerToFindHits.stop();
			timerToFindHits = null; // BH 2020.02.14
		}
		if (selectionBoxChanged) {
			findHits(true);
			selectionBoxChanged = false;
		}

	}

	protected void mousePressedAction(MouseEvent e, boolean controlDown, boolean shiftDown) {
		Point point = e.getPoint();
		mouseDrawable = plot.getInteractive();
		
		if (mouseDrawable == plot.origin) {
			plot.selectionBox.visible = false;
			plot.origin.mouseDownPt = point;
			plot.lockScale(true);
			return;
		}
		
		if (mouseDrawable == plot.areaLimits[0]) {
			plot.areaLimits[0].setX(plot.areaLimits[0].trueLimit);
			plot.areaLimits[0].isAdjusting = true;
		}
		else if (mouseDrawable == plot.areaLimits[1]) {
			plot.areaLimits[1].setX(plot.areaLimits[1].trueLimit);
			plot.areaLimits[1].isAdjusting = true;
		}
		
		// add or remove point if Interactive is dataset
		if (mouseDrawable instanceof HighlightableDataset) {

			HighlightableDataset data = (HighlightableDataset) mouseDrawable;
			int index = data.getHitIndex();
			ListSelectionModel model = dataTable.getColumnModel().getSelectionModel();
			int col = dataTable.getXColumn();
			model.setSelectionInterval(col, col);
			col = dataTable.getYColumn();
			model.addSelectionInterval(col, col);
			TableModel tableModel = dataTable.getModel();
			for (int i = 1; i < tableModel.getColumnCount(); i++) {
				if (data.getYColumnName().equals(dataTable.getColumnName(i))) {
					model.addSelectionInterval(i, i);
					if (col != i)
						data.setHighlightColor(data.getFillColor());
					data.setHighlighted(index, true);
					break;
				}
			}
			if (controlDown || shiftDown) {
				BitSet rows = dataTable.getSelectedModelRowsBS();
				if (shiftDown) {
					rows.set(index);					
				}
				else if (rows.get(index)) {
					rows.clear(index);
				} else {
					rows.set(index);
				}
				if (!OSPRuntime.isJS) {
					dataTable.setSelectedModelRowsBS(rows);
					dataTable.selectModelRowsBS(rows); // DB needed so table displays multiple selected rows
				}
			} else {
				dataTable.selectModelRows(new int[] { index });
			}
			dataTable.getSelectedData();
			mouseState = STATE_INACTIVE;
			selectionChanged = true;
			if (curveFitter.isAutoFit())
				refreshFit();
			plot.repaint();
			return;
		}
		if (mouseDrawable != null) {
			mouseState = STATE_INACTIVE;
			return;
		}
		if (timerToFindHits == null && !OSPRuntime.isJS) { // BH 2020.02.14
			timerToFindHits = new Timer(200, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					findHits(!OSPRuntime.isJS);
					timerToFindHits.restart();
				}
			});
			timerToFindHits.setRepeats(false); // BH 2020.02.14
		}
		// prepare to drag
		if (!controlDown && !shiftDown) {
			dataTable.clearSelection();
		}
		// prefill rowsInside with currently selected rows
		rowsInside.clear();
		rowsInside.or(dataTable.getSelectedModelRowsBS());
		recent.clear();
		plot.selectionBox.xstart = point.x;
		plot.selectionBox.ystart = point.y;
		readyToFindHits = true;
		if (timerToFindHits != null)
			timerToFindHits.start();
	}
	
	private boolean isFitterVisible() {
//		return splitPanes[1].getDividerLocation() <= splitPanes[1].getMaximumDividerLocation();
		return curveFitter != null && splitPanes[1].getBottomComponent() == curveFitter;
	}

	private void findHits(boolean showInTable) {
		if (!readyToFindHits || showInTable && !selectionBoxChanged)
			return;
		selectionBoxChanged = false;
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				findHitsRun(showInTable);
			}
		};
		// should this be in separate thread?
		runner.run();
//	new Thread(runner).start();
	}

	private void findHitsRun(boolean showInTable) {
		if (dataTable.workingData == null || dataTable.workingRowToModelRow == null)
			return;
		double[][] screenPoints = dataTable.workingData.getScreenCoordinates();
		ListSelectionModel columnSelectionModel = dataTable.getColumnModel().getSelectionModel();
		if (mouseState == STATE_SELECT) {
			rowsInside.clear();
			recent.clear();
		}
		for (int i = 0; i < screenPoints[0].length; i++) {
			Integer row = dataTable.workingRowToModelRow.get(i);
			if (row == null) {
				readyToFindHits = true;
				return;
			}
			int irow = row.intValue();
			// if a data point is inside the box, add/remove it
			if (!Double.isNaN(screenPoints[1][i])
					&& plot.selectionBox.contains(screenPoints[0][i], screenPoints[1][i])) {
				if (rowsInside.isEmpty()) {
					columnSelectionModel.setSelectionInterval(1, 2);
				}
				if (mouseState == STATE_SELECT_REMOVE) {
					if (rowsInside.get(irow))
						recent.set(irow);
					rowsInside.clear(irow);
				} else {
					if (!rowsInside.get(irow))
						recent.set(irow);
					rowsInside.set(irow);
				}
			} else if (recent.get(irow)) {
				// if a recently added/removed data point is outside the box, remove/add it back
				if (mouseState == STATE_SELECT_REMOVE) {
					rowsInside.set(irow);
				} else {
					rowsInside.clear(irow);
				}
				recent.clear(irow);
			}
		}
		if (showInTable) {
			if (rowsInside.isEmpty()) {
				if (dataTable.getSelectedRowCount() > 0) {
					columnSelectionModel.removeSelectionInterval(0, dataTable.getColumnCount() - 1);
					dataTable.getSelectionModel().clearSelection();
					dataTable.getSelectedData(); // updates highlights
				}
			} else {
				dataTable.selectModelRowsBS(rowsInside);
				int r0 = rowsInside.nextSetBit(0);
				Rectangle r = dataTable.getCellRect(r0, 0, false);
				if (r != null) {
					r.height += (rowsInside.length() - 1 - r0) * dataTable.getRowHeight();
					dataTable.scrollRectToVisible(r);
				}
			}
		}
		readyToFindHits = true;
		plot.repaint();
	}

	/**
	 * Refreshes the GUI.
	 */
	protected void refreshGUI() {
		SwingUtilities.invokeLater(() -> {
			refreshGUIAsync();
		});
	}

	protected void refreshGUIAsync() {
		if (!haveGUI)
			return;
		boolean changed = tabChanged;
		
		editDataButton.setText(ToolsRes.getString("DataToolTab.Button.EditData.Text")+"..."); //$NON-NLS-1$
		editDataButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.EditData.Tooltip")); //$NON-NLS-1$ 		
		newColumnButton.setText(ToolsRes.getString("DataToolTab.Button.NewColumn.Text")); //$NON-NLS-1$
		newColumnButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.NewColumn.Tooltip")); //$NON-NLS-1$
		dataBuilderButton.setText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Text")); //$NON-NLS-1$
		dataBuilderButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Tooltip")); //$NON-NLS-1$
		dataBuilderButton.setEnabled(originatorID != 0);
		refreshDataButton.setText(ToolsRes.getString("DataToolTab.Button.Refresh.Text")); //$NON-NLS-1$
		refreshDataButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.Refresh.Tooltip")); //$NON-NLS-1$
		measureButton.setText(ToolsRes.getString("DataToolTab.Button.Measure.Label")); //$NON-NLS-1$
		measureButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.Measure.Tooltip")); //$NON-NLS-1$
		analyzeButton.setText(ToolsRes.getString("DataToolTab.Button.Analyze.Label")); //$NON-NLS-1$
		analyzeButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.Analyze.Tooltip")); //$NON-NLS-1$
		statsCheckbox.setText(ToolsRes.getString("Checkbox.Statistics.Label")); //$NON-NLS-1$
		statsCheckbox.setToolTipText(ToolsRes.getString("Checkbox.Statistics.ToolTip")); //$NON-NLS-1$
		fitMenu.setText(ToolsRes.getString("Checkbox.Fits.Label")); //$NON-NLS-1$
		fourierCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Fourier.Label")); //$NON-NLS-1$
		fourierCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Fourier.ToolTip")); //$NON-NLS-1$
		originShiftCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.DataShift.Label")); //$NON-NLS-1$
		originShiftCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.DataShift.ToolTip")); //$NON-NLS-1$
		originShiftCheckbox.setEnabled(!plot.getDrawablesExcept(WorkingDataset.class, null).isEmpty());
		measureFitCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.MeasureFit.Label")); //$NON-NLS-1$
		measureFitCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.MeasureFit.ToolTip")); //$NON-NLS-1$
		propsCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Properties.Text")); //$NON-NLS-1$
		propsCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Properties.Tooltip")); //$NON-NLS-1$
		valueCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Position")); //$NON-NLS-1$
		valueCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Position.Tooltip")); //$NON-NLS-1$
		slopeCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Slope")); //$NON-NLS-1$
		slopeCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Slope.Tooltip")); //$NON-NLS-1$
		areaCheckbox.setText(ToolsRes.getString("DataToolTab.Checkbox.Area")); //$NON-NLS-1$
		areaCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Area.Tooltip")); //$NON-NLS-1$
		helpButton.setText(ToolsRes.getString("Tool.Button.Help")); //$NON-NLS-1$
		helpButton.setToolTipText(ToolsRes.getString("Tool.Button.Help.ToolTip")); //$NON-NLS-1$
		// set origin and selected point shift labels
		String label = ToolsRes.getString("DataToolTab.Origin.Label") + ":  "; //$NON-NLS-1$ //$NON-NLS-2$
		shiftXLabel.setText(label + plot.xVar);
		shiftYLabel.setText(plot.yVar);
		shiftXLabel.setToolTipText(ToolsRes.getString("DataToolTab.Origin.Tooltip")); //$NON-NLS-1$
		label = ToolsRes.getString("DataToolTab.Selection.Label") + ":  "; //$NON-NLS-1$ //$NON-NLS-2$
		selectedXLabel.setText(label + plot.xVar);
		selectedXLabel.setToolTipText(ToolsRes.getString("DataToolTab.Selection.Tooltip")); //$NON-NLS-1$
		selectedYLabel.setText(plot.yVar);

		toolbar.remove(newColumnButton);
		if (userEditable) {
			int n = toolbar.getComponentIndex(helpButton);
			toolbar.add(newColumnButton, n);
			toolbar.validate();
		}
		toolbar.remove(refreshDataButton);
		Collection<Tool> tools = jobManager.getTools(dataManager);
		for (Tool tool : tools) {
			if (tool instanceof DataRefreshTool) {
				int n = toolbar.getComponentIndex(helpButton);
				toolbar.add(refreshDataButton, n);
				toolbar.validate();
				break;
			}
		}
		curveFitter.refreshGUI();
		statsTable.refreshGUI();
//		propsTable.refreshGUI();
		refreshPlot();
		refreshStatusBar(null);
		tabChanged = changed;
	}

	/**
	 * Refreshes the decimal separators.
	 */
	protected void refreshDecimalSeparators() {
		plot.sciFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
		plot.fixedFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
		plot.stringBuilder.refreshFormats();
		correlationFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
		dataTable.refreshTable(DataTable.MODE_FORMAT);
	}

	/**
	 * Initializes this panel.
	 */
	private void init() {
		if (isInitialized) {
			return;
		}

		splitPanes[1].setDividerLocation(1.0);
		propsAndStatsAction.actionPerformed(null);
		for (int i = 0; i < dataTable.getColumnCount(); i++) {
			String colName = dataTable.getColumnName(i);
			dataTable.getWorkingData(colName);
		}
		refreshPlot();
		refreshGUI();
		isInitialized = true;
	}

	/**
	 * Builds the axis variables popup menu.
	 */
	protected void buildVarPopup() {
		// from action listener
		if (setVarAction == null) {
			// create action to set axis variable
			setVarAction = new AbstractAction() {
				@Override
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
					dataTable.moveColumn(var,  col);
					// restore other variable if needed
					if (!var.equals(otherVar)) {
						col = isHorzVarPopup ? dataTable.getYColumn() : dataTable.getXColumn();
						dataTable.moveColumn(otherVar, col);
					}
					// restore labels
					col = dataTable.convertColumnIndexToView(0);
					dataTable.getColumnModel().moveColumn(col, labelCol);
					refreshPlot();
				}

			};
		}
		varPopup = new JPopupMenu();
		Font font = new JTextField().getFont();
		for (Dataset next : dataManager.getDatasetsRaw()) {
			String s = TeXParser.removeSubscripting(next.getYColumnName());
			JMenuItem item = new JMenuItem(s);
			item.setActionCommand(next.getYColumnName());
			item.addActionListener(setVarAction);
			item.setFont(font);
			varPopup.add(item);
		}
	}

	/**
	 * Returns the column with matching ID and columnID in the specified list. May
	 * return null.
	 *
	 * @param local           the Dataset to match
	 * @param columnsToSearch the Datasets to search
	 * @return the matching Dataset, if any
	 */
	private DataColumn getIDMatch(Dataset local, ArrayList<DataColumn> columnsToSearch) {
		if ((columnsToSearch == null) || (local == null)) {
			return null;
		}
		for (Iterator<DataColumn> it = columnsToSearch.iterator(); it.hasNext();) {
			DataColumn next = it.next();
			// next is match if has same ID and columnID
			if ((local.getID() == next.getID()) && (local.getColumnID() == next.getColumnID())) {
				return next;
			}
		}
		return null;
	}

	/**
	 * Returns the column with matching name in the specified list. May return null.
	 *
	 * @param local           the Dataset to match
	 * @param columnsToSearch the Datasets to search
	 * @return the matching DataColumn, if any
	 */
	private DataColumn getNameMatch(Dataset local, ArrayList<DataColumn> columnsToSearch) {
		if ((columnsToSearch == null) || (local == null)) {
			return null;
		}
		for (Iterator<DataColumn> it = columnsToSearch.iterator(); it.hasNext();) {
			DataColumn next = it.next();
			// next is match if has same y-column name
			if (local.getYColumnName().equals(next.getYColumnName())) {
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
		ArrayList<Dataset> datasets = dataManager.getDatasetsRaw();
		for (int i = 0, n = datasets.size(); i < n; i++) {
			Dataset next = datasets.get(i);
			double[] y = next.getYPoints();
			if (name.equals(next.getYColumnName()) && isDuplicate(data, next.getYPoints())) {
				// next is duplicate column: add new points if any
				if (data.length > y.length) {
					next.set(data, data);
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
		for (int i = 0; i < len; i++) {
			if (Double.isNaN(data0[i]) && Double.isNaN(data1[i])) {
				continue;
			}
			if (data0[i] != data1[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets datasets matching columns by ID in this tab.
	 * 
	 * @param data Data object with datasets to match
	 * @return map of column to dataset
	 */
	protected Map<DataColumn, Dataset> getColumnMatchesByID(Data data) {
		Map<DataColumn, Dataset> matches = new HashMap<DataColumn, Dataset>();
		ArrayList<Dataset> datasets = DataTool.getDatasets(data);
		for (Dataset next : dataManager.getDatasetsRaw()) {
			if (next instanceof DataColumn) {
				DataColumn column = (DataColumn) next;
				Dataset match = getMatchByID(column, datasets);
				if (match != null) {
					matches.put(column, match);
				}
			}
		}
		return matches;
	}

	/**
	 * Gets datasets matching columns by name in this tab.
	 * 
	 * @param columnNames set of column names
	 * @param data        Data object with datasets to match
	 * @return map of column to dataset
	 */
	protected Map<DataColumn, Dataset> getColumnMatchesByName(Set<String> columnNames, Data data) {
		Map<DataColumn, Dataset> matches = new HashMap<DataColumn, Dataset>();
		ArrayList<Dataset> datasets = DataTool.getDatasets(data);
		for (Dataset next : dataManager.getDatasetsRaw()) {
			if (next instanceof DataColumn) {
				DataColumn column = (DataColumn) next;
				if (columnNames != null && !columnNames.contains(column.getYColumnName()))
					continue;
				Dataset match = getMatchByName(column, datasets);
				if (match != null) {
					matches.put(column, match);
				}
			}
		}
		return matches;
	}

	/**
	 * Gets a matching Dataset by name.
	 * 
	 * @param column   the DataColumn to match
	 * @param datasets the Datasets to search
	 * @return the matching Dataset
	 */
	protected Dataset getMatchByName(DataColumn column, ArrayList<Dataset> datasets) {
		// convert DataColumn name to dataset column name
		String[] dataNames = ownedColumns.get(column.getYColumnName());
		if (dataNames == null)
			return null;
		String dataName = dataNames[1];
		for (int i = 0; i < datasets.size(); i++) {
			Dataset next = datasets.get(i);
			if (next == null)
				continue;
			if (i == 0 && dataName.equals(next.getXColumnName()))
				return next;
			if (dataName.equals(next.getYColumnName()))
				return next;
		}
		return null;
	}

	/**
	 * Gets a matching Dataset by ID.
	 * 
	 * @param column   the DataColumn to match
	 * @param datasets the Datasets to search
	 * @return the matching Dataset
	 */
	protected Dataset getMatchByID(DataColumn column, ArrayList<Dataset> datasets) {
		for (Dataset next : datasets) {
			if (next == null)
				continue;
			if (column.getID() == next.getID())
				return next;
		}
		return null;
	}
	
	/**
	 * Sets the table data to the specified delimited text. Posts an undoable
	 * edit if prevString is given.
	 * 
	 * @param dataString the delimited text
	 * @param prevString optional previous delimited text for undoable edit
	 * @return true if an undoable edit was posted
	 */
	protected boolean setDelimitedData(String dataString, String prevString) throws Exception {
		if (dataString == null) {
			return false;
		}
		if (dataString.length() == 0) {
			clearData(false); // do not post edits (one for each column!)
//			if (prevString != null) {
//				// post edit: target is prev data, value is ""
//				TableEdit edit = dataTable.new TableEdit(DataToolTable.DELIMITED_TEXT_EDIT, null,
//						prevString, dataString);
//				undoSupport.postEdit(edit);
//				refreshUndoItems();
//			}
			return false;
		}
		String parsableData = dataString.replace(" ", "").replace(",", "\t");
		Data data = DataTool.parseData(parsableData, "edited");
		clearData(false); // do not post edits
		if (data != null) {
			loadData(data, false);
			tabChanged(true);
			if (prevString != null) {
				// post edit: target is prev data, value is new data
				TableEdit edit = dataTable.new TableEdit(DataToolTable.DELIMITED_TEXT_EDIT, null,
						prevString, dataString);
				undoSupport.postEdit(edit);
				refreshUndoItems();
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the selected data in the curve fitter and fourier panel.
	 * 
	 * @param selectedData the Dataset to pass to the fitter and fourier panel
	 */
	protected void setSelectedData(Dataset selectedData, boolean dofit) {
		getCurveFitter().setData(selectedData, dofit);
		if (fourierPanel != null) {
			try {
			fourierPanel.refreshFourierData(selectedData, getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (originShiftEnabled && selectedData != null) {
			if (selectedData.getIndex() == 1) {
				selectedDataIndex = dataTable.getSelectedRow();
				toolbar.add(selectedXLabel, 6);
				toolbar.add(selectedXField, 7);
				toolbar.add(selectedYLabel, 8);
				toolbar.add(selectedYField, 9);
				selectedXField.setValue(selectedData.getXPointsRaw()[0]);
				selectedXField.refreshPreferredWidth();
				selectedYField.setValue(selectedData.getY(0));
				selectedYField.refreshPreferredWidth();
				toolbar.revalidate();
			} else {
				toolbar.remove(selectedXLabel);
				toolbar.remove(selectedXField);
				toolbar.remove(selectedYLabel);
				toolbar.remove(selectedYField);
				toolbar.revalidate();
				selectedDataIndex = -1;
			}
		}
		if (positionVisible || slopeVisible) {
			plot.refreshMeasurements();
		}
		if (areaVisible) {
			plot.refreshArea();
		}
	}

	protected void refreshPlot() {
		refreshPlot(false);
	}

	/**
	 * Refreshes the plot.
	 */
	protected void refreshPlot(boolean andFit) {
		if (!haveGUI)
			return;
		// refresh data for curve fitting and plotting
		HighlightableDataset d = dataTable.getSelectedData();
		setSelectedData(d, andFit);
		plot.removeDrawables(Dataset.class);
		WorkingDataset workingData = getWorkingData();

		valueCheckbox.setEnabled((workingData != null) && (workingData.getIndex() > 0));
		if (!valueCheckbox.isEnabled()) {
			valueCheckbox.setSelected(false);
			positionVisible = false;
		}
		slopeCheckbox.setEnabled((workingData != null) && (workingData.getIndex() > 2));
		if (!slopeCheckbox.isEnabled()) {
			slopeCheckbox.setSelected(false);
			slopeVisible = false;
		}
		areaCheckbox.setEnabled((workingData != null) && (workingData.getIndex() > 1));
		if (!areaCheckbox.isEnabled()) {
			areaCheckbox.setSelected(false);
			areaVisible = false;
		}
		plot.dataPresent = false;
		if (workingData != null) {
			plot.dataPresent = workingData.getIndex() > 0;
			int labelCol = dataTable.convertColumnIndexToView(0);
			String xName = dataTable.getColumnName((labelCol == 0) ? 1 : 0);
			Map<String, WorkingDataset> datasets = dataTable.workingMap;
			for (Iterator<WorkingDataset> it = datasets.values().iterator(); it.hasNext();) {
				DataToolTable.WorkingDataset next = it.next();
				next.setXSource(workingData.getXSource());
				String colName = next.getYColumnName();
				if (next == workingData || colName.equals(xName)
						|| (originShiftEnabled && (colName + SHIFTED).equals(xName))) {
					continue;
				}
				if (next.isMarkersVisible() || next.isConnected()) {
					// next.clearHighlights();
					if (!next.isMarkersVisible()) {
						next.setMarkerShape(Dataset.NO_MARKER);
					}
					plot.addDrawable(next);
					plot.dataPresent = plot.dataPresent || next.getIndex() > 0;
				}
			}
			plot.addDrawable(workingData);

//      // keep area limits within dataset limits
//      if (areaVisible) {
//        plot.areaLimits[0].x = Math.max(plot.areaLimits[0].x, workingData.getXMin());
//        plot.areaLimits[0].x = Math.min(plot.areaLimits[0].x, workingData.getXMax());
//        plot.areaLimits[1].x = Math.max(plot.areaLimits[1].x, workingData.getXMin());
//        plot.areaLimits[1].x = Math.min(plot.areaLimits[1].x, workingData.getXMax());
//      }
			workingData.restoreHighlights();
			// draw curve fit on top of dataset if curve fitter is visible
			if (isFitterVisible()) {
				plot.removeDrawable(curveFitter.getDrawer());
				plot.addDrawable(curveFitter.getDrawer());
			}
			// set axis labels
			String xLabel = workingData.getColumnName(0);
			String yLabel = workingData.getColumnName(1);
			plot.setAxisLabels(xLabel, yLabel);

			// construct equation string
			if (curveFitter.fit != null) {
				String depVar = TeXParser.removeSubscripting(workingData.getColumnName(1));
				String indepVar = TeXParser.removeSubscripting(workingData.getColumnName(0));
				if (originShiftEnabled) {
					depVar += DataToolTab.SHIFTED;
					indepVar += DataToolTab.SHIFTED;
				}
				curveFitter.setText(depVar + " = " + //$NON-NLS-1$
						(curveFitter.fit instanceof UserFunction
								? ((UserFunction) curveFitter.fit).getFullExpression(new String[] { indepVar })
								: curveFitter.fit.getExpression(indepVar)));
			}
		} else { // working data is null
			plot.setXLabel(""); //$NON-NLS-1$
			plot.setYLabel(""); //$NON-NLS-1$
		}
		if (dataTool != null) {
			dataTool.refreshTabTitles();
		}

		// refresh crossbars, slope line and area if visible
		if (positionVisible || slopeVisible) {
			plot.refreshMeasurements();
		}
		if (areaVisible) {
			plot.refreshArea();
		}
		// BH was this.repaint?
		if (fitTimer == null || !fitTimer.isRunning() || ! curveFitter.isAutoFit())
			plot.repaint();
		refreshFit();
	}

	/**
	 * Refreshes the status bar.
	 * 
	 * @param hint an optional hint to display (may be null)
	 */
	protected void refreshStatusBar(String hint) {
		if (hint != null) {
			statusLabel.setText(hint);
		} else if (slopeCheckbox.isSelected()) {
			String s = ToolsRes.getString("DataToolTab.Status.Slope"); //$NON-NLS-1$
			if (isFitterVisible()) {
				s += " " + ToolsRes.getString("DataToolTab.Status.MeasureFit"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			statusLabel.setText(s);
		} else if (areaCheckbox.isSelected()) {
			String s = ToolsRes.getString("DataToolTab.Status.Area"); //$NON-NLS-1$
			if (isFitterVisible()) {
				s += " " + ToolsRes.getString("DataToolTab.Status.MeasureFit"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			statusLabel.setText(s);
		} else if (valueCheckbox.isSelected()) {
			String s = ToolsRes.getString("DataToolTab.Status.Value"); //$NON-NLS-1$
			if (isFitterVisible()) {
				s += " " + ToolsRes.getString("DataToolTab.Status.MeasureFit"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			statusLabel.setText(s);
		} else if (originShiftCheckbox.isSelected()) {
			statusLabel.setText(ToolsRes.getString("DataToolTab.Status.ShiftOrigin")); //$NON-NLS-1$
		} else if (statsCheckbox.isSelected()) {
			statusLabel.setText(getCorrelationString());
		} else {
			if (dataManager.getDatasetsRaw().size() < 2) {
				statusLabel.setText(userEditable ? ToolsRes.getString("DataToolTab.StatusBar.Text.CreateColumns") //$NON-NLS-1$
						: ToolsRes.getString("DataToolTab.StatusBar.Text.PasteColumns")); //$NON-NLS-1$
			} else {
				statusLabel.setText(ToolsRes.getString("DataToolTab.StatusBar.Text.DragColumns")); //$NON-NLS-1$
			}
		}
		editableLabel.setText(isUserEditable() ? ToolsRes.getString("DataTool.MenuItem.Editable").toLowerCase() //$NON-NLS-1$
				: ToolsRes.getString("DataTool.MenuItem.Noneditable").toLowerCase()); //$NON-NLS-1$
		editableLabel.setForeground(isUserEditable() ? Color.GREEN.darker() : Color.RED.darker());
	}

	/**
	 * Gets a correlation string to display in the status bar.
	 */
	protected String getCorrelationString() {
		String s = ToolsRes.getString("DataToolTab.Status.Correlation"); //$NON-NLS-1$
		if (Double.isNaN(curveFitter.correlation)) {
			s += " " + ToolsRes.getString("DataToolTab.Status.Correlation.Undefined"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			s += " = " + correlationFormat.format(curveFitter.correlation); //$NON-NLS-1$
		}
		return s;
	}

	/**
	 * Refreshes the origin shift fields.
	 * 
	 * @return the current y-field shift
	 */
	public double refreshShiftFields() {
		boolean doRevalidate = false;
		Dataset data = dataTable.getDataset(plot.xVar);
		if (data != null && data instanceof DataColumn) {
			// check format pattern
			String pattern = dataTable.getFormatPattern(plot.xVar);
			if (pattern == null || "".equals(pattern)) { //$NON-NLS-1$
				pattern = "#0.0#"; //$NON-NLS-1$
			}
			String existing = shiftXField.getPattern();
			if (!pattern.equals(existing)) {
				shiftXField.applyPattern(pattern);
				selectedXField.applyPattern(pattern);
			}
			// set values
			double shift = ((DataColumn) data).getShift();
			shiftXField.setValue(shift == 0 ? 0 : -shift);
			shiftXSpinner.setValue(shift == 0 ? 0 : -shift);
			if (selectedDataIndex > -1) {
				selectedXField.setValue(data.getYPoints()[selectedDataIndex]);
			}
			if (shift != prevShiftX || !pattern.equals(existing)) {
				shiftXField.refreshPreferredWidth();
				selectedXField.refreshPreferredWidth();
				doRevalidate = true;
			}
		}
		double currentShift = prevShiftY;
		data = dataTable.getDataset(plot.yVar);
		if (data != null && data instanceof DataColumn) {
			// check format pattern
			String pattern = dataTable.getFormatPattern(plot.yVar);
			if (pattern == null || "".equals(pattern)) { //$NON-NLS-1$
				pattern = "#0.0#"; //$NON-NLS-1$
			}
			String existing = shiftYField.getPattern();
			if (!pattern.equals(existing)) {
				shiftYField.applyPattern(pattern);
				selectedYField.applyPattern(pattern);
			}
			// set values
			currentShift = ((DataColumn) data).getShift();
			shiftYField.setValue(currentShift == 0 ? 0 : -currentShift);
			shiftYSpinner.setValue(currentShift == 0 ? 0 : -currentShift);
			if (selectedDataIndex > -1) {
				selectedYField.setValue(data.getYPoints()[selectedDataIndex]);
			}
			if (currentShift != prevShiftY || !pattern.equals(existing)) {
				shiftYField.refreshPreferredWidth();
				selectedYField.refreshPreferredWidth();
				doRevalidate = true;
			}
		}
		if (doRevalidate)
			toolbar.revalidate();
		return currentShift;
	}

	/**
	 * Refreshes all.
	 */
	public void refreshAll(int mode) {
		refreshPlot(false);
		refreshShiftFields();
		dataTable.refreshTable(mode);
	}

	Timer fitTimer;

	private ActionListener fitTimerAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (fitTimer != null) {
				curveFitter.fit(curveFitter.fit);
				plot.refreshArea();
				plot.refreshMeasurements();
				plot.repaint();
			}
		}

	};

	private static final int fitDelayMS = (OSPRuntime.isJS ? 250 : 25);

	void refreshFit() {
		if (fitTimer == null) {
			fitTimer = new Timer(fitDelayMS, fitTimerAction);
			fitTimer.setRepeats(false);
			fitTimer.start();
		} else {
			fitTimer.restart();
		}
	}

	private void stopFitTimer() {
		if (fitTimer != null) {
			fitTimer.stop();
			fitTimer = null;
		}
	}

	/**
	 * Refreshes the undo and redo menu items.
	 */
	protected void refreshUndoItems() {
		if (dataTool != null) {
			dataTool.undoItem.setEnabled(undoManager.canUndo());
			dataTool.redoItem.setEnabled(undoManager.canRedo());
		}
	}

	protected void postShiftEdit() {
		// check for change
		double shiftX = -shiftXField.getValue();
		double shiftY = -shiftYField.getValue();
		if (prevShiftX == shiftX && prevShiftY == shiftY)
			return;

		// post undoable edit
		double[] newShift = new double[] { shiftX, shiftY };
		double[] prevShift = new double[] { prevShiftX, prevShiftY };
		String[] colNames = new String[] { plot.xVar, plot.yVar };
		ShiftEdit edit = new ShiftEdit(colNames, newShift, prevShift);
		undoSupport.postEdit(edit);
		refreshUndoItems();
		prevShiftX = shiftX;
		prevShiftY = shiftY;
	}

//_____________________________  inner classes ____________________________  

	/**
	 * An interactive axes class that returns popup menus for x and y-variables.
	 */
	protected class DataToolAxes extends CartesianInteractive {

		/**
		 * Constructor.
		 *
		 * @param panel a PlottingPanel
		 */
		protected DataToolAxes(PlottingPanel panel) {
			super(panel);
		}

		@Override
		protected boolean hasHorzVariablesPopup() {
			return dataTable.workingData != null;
		}

		@Override
		protected javax.swing.JPopupMenu getHorzVariablesPopup() {
			if (varPopup == null) {
				buildVarPopup();
			}
			isHorzVarPopup = true;
			FontSizer.setFonts(varPopup, FontSizer.getLevel());
			for (Component c : varPopup.getComponents()) {
				JMenuItem item = (JMenuItem) c;
				if (xLine.getText().equals(item.getActionCommand())) {
					item.setFont(item.getFont().deriveFont(Font.BOLD));
				} else {
					item.setFont(item.getFont().deriveFont(Font.PLAIN));
				}
			}
			return varPopup;
		}

		@Override
		protected boolean hasVertVariablesPopup() {
			return dataTable.workingData != null;
		}

		@Override
		protected javax.swing.JPopupMenu getVertVariablesPopup() {
			if (varPopup == null) {
				buildVarPopup();
			}
			isHorzVarPopup = false;
			FontSizer.setFonts(varPopup, FontSizer.getLevel());
			for (Component c : varPopup.getComponents()) {
				JMenuItem item = (JMenuItem) c;
				if (yLine.getText().equals(item.getActionCommand())) {
					item.setFont(item.getFont().deriveFont(Font.BOLD));
				} else {
					item.setFont(item.getFont().deriveFont(Font.PLAIN));
				}
			}
			return varPopup;
		}
		
	} // end DataToolAxes class

	/**
	 * A class to plot datasets, value crossbars, slope lines, areas, and axes.
	 */
	protected class DataToolPlotter extends PlottingPanel {
		SelectionBox selectionBox;
		Crossbars valueCrossbars;
		SlopeLine slopeLine;
		XYAxes origin;
		LimitLine[] areaLimits = new LimitLine[2];
		Dataset areaDataset;
		double value = Double.NaN, slope = Double.NaN, area;
		DecimalFormat sciFormat = org.opensourcephysics.numerics.Util.newDecimalFormat("0.00E0"); //$NON-NLS-1$
		DecimalFormat fixedFormat = org.opensourcephysics.numerics.Util.newDecimalFormat("0.00"); //$NON-NLS-1$
		PlotCoordinateStringBuilder stringBuilder;
		String xVar, yVar, message;
		boolean scaleLocked, dataPresent;
		double lockedXMin, lockedXMax, lockedYMin, lockedYMax;
		double mouseDownXMin, mouseDownXMax, mouseDownYMin, mouseDownYMax;
		int measurementIndex = -1, measurementX = -1;
		Interactive interactive;

		/**
		 * Constructor
		 * 
		 * @param dataset the initial dataset to plot
		 */
		protected DataToolPlotter(Dataset dataset) {
			super((dataset == null) ? "x" //$NON-NLS-1$
					: dataset.getColumnName(0),
					(dataset == null) ? "y" //$NON-NLS-1$
							: dataset.getColumnName(1),
					""); //$NON-NLS-1$
			setAntialiasShapeOn(true);
			selectionBox = new SelectionBox();
			valueCrossbars = new Crossbars();
			slopeLine = new SlopeLine();
			origin = new XYAxes();
			areaLimits[0] = new LimitLine();
			areaLimits[1] = new LimitLine();
			addDrawable(areaLimits[0]);
			addDrawable(areaLimits[1]);
			addDrawable(selectionBox);
			addDrawable(origin);

			// create key listener to move origin, fix measurements and extend slope line
			addKeyListener(new KeyAdapter() {
				
				private BitSet keyBits = new BitSet(256);

				@Override
				public void keyPressed(KeyEvent e) {
					// only handle the first event, ignore repeated keys
	        int keyCode = e.getKeyCode();
	        if (keyBits.get(keyCode))
	        	return;
					
					keyBits.set(keyCode);	        
					if (e.getKeyCode() == KeyEvent.VK_ALT) {
						mouseState = STATE_MOVE;
						if (plot.selectionBox.visible) {
							plot.selectionBox.visible = false;
							plot.repaint();
						}
						return;
					}
					if (keyCode == KeyEvent.VK_SHIFT) {
						if (interactive != null)
							return;
						boolean remove = e.isControlDown(); // we know that shift is down
						switch (mouseState) {
						case STATE_ZOOM:
						case STATE_MOVE:
							return;					
						case STATE_SELECT:
						case STATE_SELECT_ADD:
						case STATE_SELECT_REMOVE:
							mouseState = remove ? STATE_SELECT_REMOVE: STATE_SELECT_ADD;
							plot.setMouseCursor(remove ? SELECT_REMOVE_CURSOR : SELECT_ADD_CURSOR);
						case STATE_INACTIVE:
						}
						return;
					}
					if (keyCode == KeyEvent.VK_CONTROL) {
						if (interactive != null)
							return;
						switch (mouseState) {
						case STATE_ZOOM:
						case STATE_MOVE:
							return;					
						case STATE_SELECT:
						case STATE_SELECT_ADD:
						case STATE_SELECT_REMOVE:
							mouseState = STATE_SELECT_REMOVE;
							plot.setMouseCursor(SELECT_REMOVE_CURSOR);
						case STATE_INACTIVE:
							if (!OSPRuntime.isMac())
								plot.setMouseCursor(SELECT_REMOVE_CURSOR);
						}
						if (toggleMeasurement)
							return;
						toggleMeasurement = true;
						refreshMeasurements();
						refreshArea();
						return;
					}
					if (keyCode == KeyEvent.VK_SPACE) {
						if (!freezeMeasurement && e.isShiftDown()) {
							freezeMeasurement = true;
						} else {
							freezeMeasurement = false;
						}
						;
						if (!freezeMeasurement && mouseEvent != null) {
							plot.measurementX = mouseEvent.getX();
							plot.measurementIndex = -1;
							plot.refreshMeasurements();
						}
						return;
					}
					if (keyCode == KeyEvent.VK_S && e.isShiftDown()) {
						dataTool.slopeExtended = !dataTool.slopeExtended;
						plot.refreshMeasurements();
						return;
					}
					if (!originShiftEnabled)
						return;
					double dy = Double.NaN, dx = Double.NaN;
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						if (e.isShiftDown()) {
							dy = -10 / getYPixPerUnit();
						} else {
							dy = -1 / getYPixPerUnit();
						}
					} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						if (e.isShiftDown()) {
							dy = 10 / getYPixPerUnit();
						} else {
							dy = 1 / getYPixPerUnit();
						}
					} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						if (e.isShiftDown()) {
							dx = -10 / getXPixPerUnit();
						} else {
							dx = -1 / getXPixPerUnit();
						}
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						if (e.isShiftDown()) {
							dx = 10 / getXPixPerUnit();
						} else {
							dx = 1 / getXPixPerUnit();
						}
					}
					if (!Double.isNaN(dx) || !Double.isNaN(dy)) {
						if (!Double.isNaN(dx)) {
							Dataset data = dataTable.getDataset(plot.xVar);
							if (data != null && data instanceof DataColumn) {
								DataColumn col = (DataColumn) data;
								col.setShift(col.getShift() - dx);
								tabChanged(true);
								// shift area limits
								if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
									plot.areaLimits[0].refreshX();
									plot.areaLimits[1].refreshX();
								} else {
									plot.areaLimits[0].setX(plot.areaLimits[0].getX() - dx);
									plot.areaLimits[1].setX(plot.areaLimits[1].getX() - dx);
								}
							}
						} else if (!Double.isNaN(dy)) {
							Dataset data = dataTable.getDataset(plot.yVar);
							if (data != null && data instanceof DataColumn) {
								DataColumn col = (DataColumn) data;
								col.setShift(col.getShift() + dy);
								tabChanged(true);
							}
						}
						refreshAll(DataTable.MODE_VALUES);
						((CrawlerSpinnerModel) shiftXSpinner.getModel()).refreshDelta();
						((CrawlerSpinnerModel) shiftYSpinner.getModel()).refreshDelta();
						dataTable.dorepaint(5);
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
	        keyBits.clear(e.getKeyCode());
					if (e.getKeyCode() == KeyEvent.VK_ALT) {
						plot.setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
						return;
					}
					if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
						if (interactive != null)
							return;
						switch (mouseState) {
						case STATE_INACTIVE:
							plot.setMouseCursor(e.isControlDown() && !OSPRuntime.isMac()? 
									SELECT_REMOVE_CURSOR:
									Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
						case STATE_ZOOM:
						case STATE_MOVE:
							return;
						case STATE_SELECT:
						case STATE_SELECT_ADD:
						case STATE_SELECT_REMOVE:
							boolean remove = e.isControlDown(); // we know that shift is up
							mouseState = remove ? STATE_SELECT_REMOVE: STATE_SELECT;
							plot.setMouseCursor(remove ? SELECT_REMOVE_CURSOR : SELECT_CURSOR);
						}
						return;
					}
					if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
						if (interactive != null)
							return;
						switch (mouseState) {
						case STATE_INACTIVE:
							plot.setMouseCursor(e.isShiftDown()? 
									SELECT_ADD_CURSOR:
									Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
						case STATE_ZOOM:
						case STATE_MOVE:
							return;
						case STATE_SELECT:
						case STATE_SELECT_ADD:
						case STATE_SELECT_REMOVE:
							mouseState = e.isShiftDown()? STATE_SELECT_ADD: STATE_SELECT;
							plot.setMouseCursor(e.isShiftDown()? SELECT_ADD_CURSOR: SELECT_CURSOR);
						}
						if (!toggleMeasurement)
							return;
						toggleMeasurement = false;
						refreshMeasurements();
						refreshArea();
						return;
					}
					if (!originShiftEnabled)
						return;
					if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN
							|| e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {

						postShiftEdit();
					}
				}

			});
		}
		
		@Override
		public boolean isZoomEvent(MouseEvent e) {
			return OSPRuntime.isPopupTrigger(e) && 
					!(mouseState == STATE_SELECT || 
					mouseState == STATE_SELECT_ADD || 
					mouseState == STATE_SELECT_REMOVE);
		}

	  @Override
	  public Interactive getInteractive() {
	  	interactive = origin.findInteractive(this, mouseEvent.getX(), mouseEvent.getY());
      if (interactive == null)
      	interactive = super.getInteractive();
	    return interactive;
	  }

		@Override
		protected void refreshDecimalSeparators() {
			super.refreshDecimalSeparators();
			if (dataTool.getSelectedTab() == DataToolTab.this) {
				dataTool.refreshDecimalSeparators();
			}
		}

		/**
		 * Locks the scale so it can be manipulated when shifting the origin.
		 * 
		 * @param lock true to lock, false to unlock
		 */
		protected void lockScale(boolean lock) {
			scaleLocked = lock;
			if (lock) {
				lockedXMax = mouseDownXMax = xmax;
				lockedXMin = mouseDownXMin = xmin;
				lockedYMax = mouseDownYMax = ymax;
				lockedYMin = mouseDownYMin = ymin;
			}
		}

		@Override
		protected void scale(ArrayList<Drawable> tempList) {
			if (scaleLocked) {
				xminPreferred = lockedXMin;
				xmaxPreferred = lockedXMax;
				yminPreferred = lockedYMin;
				ymaxPreferred = lockedYMax;
			} else {
				super.scale(tempList);
			}
		}
		
		@Override
		protected void paintDrawableList(Graphics g, ArrayList<Drawable> tempList) {
			String s = message;
			if (tempList.contains(curveFitter.getDrawer())) {
				boolean auto = curveFitter.isAutoFit();
				boolean inactive = (mouseState == STATE_INACTIVE);
				curveFitter.setFitVisible(!auto || dataTable.isFitFittable(curveFitter.fit, inactive));
				double[] ylimits = curveFitter.getDrawer().getYRange();
				if (ylimits[0] != ylimits[1] && (ylimits[0] >= this.getYMax() || ylimits[1] <= this.getYMin())) {
					s = ToolsRes.getString("DataToolTab.Plot.Message.FitNotVisible")
							+ (message == null || s == "" ? "" : "  " + s);
				}
			}
			super.paintDrawableList(g, tempList);
			setMessage(s);
			slopeLine.draw(g);
			valueCrossbars.draw(g);
		}

		/**
		 * Sets the visibility of the area limits and dataset.
		 * 
		 * @param visible true to show the area
		 */
		protected void setAreaVisible(boolean visible) {
			areaVisible = visible;
			if (areaDataset == null) { // first time shown
				areaDataset = new Dataset();
				areaDataset.setMarkerShape(Dataset.AREA);
				areaDataset.setConnected(false);
				areaDataset.setMarkerColor(new Color(102, 102, 102, 51));
				Dataset data = dataTable.workingData;
				if ((data != null) && (data.getIndex() > 1)) {
					areaLimits[0].x = data.getXMin();
					areaLimits[1].x = data.getXMax();
					// set initial point indices
					double[] pts = data.getXPointsRaw();
					int n = data.getIndex();
					for (int i = 0; i < n; i++) {
						if (pts[i] == areaLimits[0].x) {
							areaLimits[0].pointIndex = i;
						}
						if (pts[i] == areaLimits[1].x) {
							areaLimits[1].pointIndex = i;
						}
						if (areaLimits[0].pointIndex > -1 && areaLimits[1].pointIndex > -1) {
							break;
						}
					}
				}
			}
			refreshPlot();
			setMessage(createMessage());
		}

		/**
		 * Refreshes the coordinate, slope and area measurements.
		 */
		protected void refreshMeasurements() {
			
			HighlightableDataset data = dataTable.workingData;
			if (mouseEvent != null) {
				Interactive ia = plot.getInteractive();
				if (ia instanceof HighlightableDataset) {
					data = (HighlightableDataset) ia;
				}
			}
			
			if (data != null && (positionVisible || slopeVisible || areaVisible)) {
				
				int j = measurementIndex;
				double x = plot.pixToX(measurementX);
					if (data.getIndex() > 0 && j < 0) {
					measurementIndex = j = plot.findIndexNearestX(x, data);
				}

				double[] xpoints = data.getXPointsRaw();
				double[] ypoints = data.getYPointsRaw();

				boolean measureData = !isFitterVisible() || (measureFit && toggleMeasurement)
						|| (!measureFit && !toggleMeasurement);
				FunctionDrawer drawer = getCurveFitter().getDrawer();

				boolean positionOrSlopeChanged = false;
				if (positionVisible) {
					
					double xtemp = Double.NaN, ytemp = Double.NaN;
					if (measureData && j > -1 && !Double.isNaN(ypoints[j])) {
						xtemp = xpoints[j];
						ytemp = ypoints[j];
					} else if (!measureData) {
						xtemp = x;
						ytemp = drawer.evaluate(x);
					}
					
					positionOrSlopeChanged = (!Double.isNaN(xtemp) && !Double.isNaN(ytemp) &&
							(xtemp != plot.valueCrossbars.x || ytemp != plot.valueCrossbars.y));
					
					if (positionOrSlopeChanged) {
						plot.value = ytemp;
						plot.valueCrossbars.x = xtemp;
						plot.valueCrossbars.y = ytemp;
						plot.xVar = data.getXColumnName();
						plot.yVar = data.getYColumnName();						
					}
				}
				if (slopeVisible) {
					
					double xtemp = Double.NaN, ytemp = Double.NaN, slopetemp = Double.NaN;
					if (measureData && j > 0 && j < data.getIndex() - 1 && !Double.isNaN(ypoints[j])) {
						xtemp = xpoints[j];
						ytemp = ypoints[j];
						slopetemp = (ypoints[j + 1] - ypoints[j - 1]) / (xpoints[j + 1] - xpoints[j - 1]);
					} else if (!measureData) {
						xtemp = x;
						ytemp = drawer.evaluate(x);
						double dx = 1 / plot.getXPixPerUnit();
						slopetemp = (drawer.evaluate(x + dx) - drawer.evaluate(x - dx)) / (2 * dx);
					}
					
					positionOrSlopeChanged = positionOrSlopeChanged ||
						(!Double.isNaN(xtemp) && !Double.isNaN(ytemp) && !Double.isNaN(slopetemp) &&
						(xtemp != plot.slopeLine.x || 
						ytemp != plot.slopeLine.y ||  
						slopetemp != plot.slope));
					
					if (positionOrSlopeChanged) {
						plot.slopeLine.x = xtemp;
						plot.slopeLine.y = ytemp;
						plot.slope = slopetemp;
					}
				}
				plot.setMessage(plot.createMessage());
				
				if (positionOrSlopeChanged || !DrawingPanel.messagesAsJLabels)
					plot.repaint();
			}
			else {
				plot.slope = plot.value = Double.NaN;				
			}
		}

		/**
		 * Fills the areaDataset with points whose x values are between the limit lines.
		 */
		protected void refreshArea() {
			if (!areaVisible) {
				return;
			}
			area = 0;
			Dataset data = dataTable.workingData;
			if (data == null) {
				areaVisible = false;
				setMessage(createMessage());
				return;
			}

			boolean measureData = !isFitterVisible() || (measureFit && toggleMeasurement)
					|| (!measureFit && !toggleMeasurement);
			FunctionDrawer drawer = getCurveFitter().getDrawer();
			areaLimits[0].refreshX();
			areaLimits[1].refreshX();
			double lower = Math.min(areaLimits[0].x, areaLimits[1].x);
			double upper = Math.max(areaLimits[0].x, areaLimits[1].x);
			double del = (upper - lower) / 200000;
			if (del > 0) {
				lower -= del;
				upper += del;
			}

			double[] xpoints, ypoints, xp, yp;
			int numpts;
			if (measureData) {
				numpts = data.getIndex();
				xp = data.getXPointsRaw();
				yp = data.isShifted() ? data.getYPoints() : data.getYPointsRaw();
			} else {
				numpts = plot.xToPix(upper) - plot.xToPix(lower);
				double delta = (upper - lower) / numpts;
				xp = new double[numpts];
				yp = new double[numpts];
				for (int i = 0; i < numpts; i++) {
					xp[i] = lower + i * delta;
					yp[i] = drawer.evaluate(xp[i]);
				}
			}

			areaDataset.clear();

			BitSet bsPoints = new BitSet(numpts);
			int n = 0;
			for (int i = 0; i < numpts; i++) {
				if (xp[i] >= lower && xp[i] <= upper && !Double.isNaN(yp[i])) {
					bsPoints.set(i);
					n++;
				}
			}

//			
//			// find data points within range
//			ArrayList<Double> x = new ArrayList<Double>();
//			ArrayList<Double> y = new ArrayList<Double>();
//			for (int i = 0; i < numpts; i++) {
//				if (xpoints[i] >= lower && xpoints[i] <= upper && !Double.isNaN(ypoints[i])) {
//					x.add(xpoints[i]);
//					y.add(ypoints[i]);
//				}
//			}
			if (!bsPoints.isEmpty()) {

				xpoints = new double[n];
				ypoints = new double[n];
				for (int p = 0, i = bsPoints.nextSetBit(0); i >= 0; i = bsPoints.nextSetBit(i + 1), p++) {
					xpoints[p] = xp[i];
					ypoints[p] = yp[i];
				}
//				
//				
//				xpoints = new double[x.size()];
//				ypoints = new double[x.size()];
//				for (int i = 0; i < numpts; i++) {
//					xpoints[i] = x.get(i);
//					ypoints[i] = y.get(i);
//				}
				areaDataset.append(xpoints[0], 0);
				areaDataset.append(xpoints, ypoints);
				areaDataset.append(xpoints[n - 1], 0);
				if (n > 1) {
					plot.addDrawable(areaDataset);
					// determine area under the curve
					area = ypoints[0] * (xpoints[1] - xpoints[0]);
					area += ypoints[n - 1] * (xpoints[n - 1] - xpoints[n - 2]);
					for (int i = 1; i < n - 1; i++) {
						area += ypoints[i] * (xpoints[i + 1] - xpoints[i - 1]);
					}
					area /= 2;
				}
			}
			// set true limits
			areaLimits[0].trueLimit = areaDataset.getXMin();
			areaLimits[1].trueLimit = areaDataset.getXMax();
			setMessage(createMessage());
		}

		/**
		 * Returns the index of the data point nearest the specified x on the plot.
		 *
		 * @param x    the x-value on the plot
		 * @param data the dataset to search
		 * @return the index, or -1 if none found
		 */
		protected int findIndexNearestX(double x, Dataset data) {
			if (data == null) {
				return -1; // no dataset
			}
			int len = data.getIndex();
			if (len == 0) {
				return -1; // dataset has no points
			}
			double[] xpoints = data.getXPointsRaw();
			double[] ypoints = data.getYPointsRaw();
			// limit x to plot area
			double min = plot.getXMin();
			double max = plot.getXMax();

			return findNearestXIndex(x, xpoints, ypoints, len, min, max);
		}

		/**
		 * Creates a message showing the current coordinates, slope and/or area.
		 */
		protected String createMessage() {
			String xAxis = xVar, yAxis = yVar;
			if (originShiftEnabled) {
				xAxis += DataToolTab.SHIFTED;
				yAxis += DataToolTab.SHIFTED;
			}

			StringBuffer buf = new StringBuffer();
			if (positionVisible && !Double.isNaN(value)) {
				buf.append(TeXParser.removeSubscripting(xAxis) + "="); //$NON-NLS-1$
				buf.append(format(plot.valueCrossbars.x, getXMax() - getXMin()));
				buf.append("  "); //$NON-NLS-1$
				buf.append(TeXParser.removeSubscripting(yAxis) + "="); //$NON-NLS-1$
				buf.append(format(plot.valueCrossbars.y, getYMax() - getYMin()));
			}
			if (slopeVisible && !Double.isNaN(slope)) {
				if (buf.length() > 0) {
					buf.append("  "); //$NON-NLS-1$
				}
				buf.append(ToolsRes.getString("DataToolPlotter.Message.Slope")); //$NON-NLS-1$
				buf.append(format(plot.slope, 0));
			}
			if (areaVisible) {
				if (buf.length() > 0) {
					buf.append("  "); //$NON-NLS-1$
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
		protected String format(double value, double range) {
			double zero = Math.min(1, range) / 1000;
			if (Math.abs(value) < zero) {
				value = 0;
			}
			if ((range < 1) && (value != 0)) {
				return sciFormat.format(value);
			}
			return (Math.abs(value) <= 10) ? fixedFormat.format(value) : sciFormat.format(value);
		}

		/**
		 * Sets the plot axis and related labels.
		 *
		 * @param xAxis the x-axis label
		 * @param yAxis the y-axis label
		 */
		protected void setAxisLabels(String xAxis, String yAxis) {
			if (xAxis == null || yAxis == null)
				return;

			xVar = xAxis;
			yVar = yAxis;

			xAxis = TeXParser.removeSubscripting(xAxis);
			yAxis = TeXParser.removeSubscripting(yAxis);

			if (originShiftEnabled) {
				xAxis = xAxis + DataToolTab.SHIFTED;
				yAxis = yAxis + DataToolTab.SHIFTED;
			}

			// set axis labels
			setXLabel(xAxis);
			setYLabel(yAxis);

			// set coordinate string builder variables
			coordinateStrBuilder.setCoordinateLabels(xAxis + "=", "  " + yAxis + "="); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

			// set origin and selected point shift labels
			String label = ToolsRes.getString("DataToolTab.Origin.Label") + ":  "; //$NON-NLS-1$ //$NON-NLS-2$
			shiftXLabel.setText(label + xVar);
			shiftYLabel.setText(yVar);
			label = ToolsRes.getString("DataToolTab.Selection.Label") + ":  "; //$NON-NLS-1$ //$NON-NLS-2$
			selectedXLabel.setText(label + xVar);
			selectedYLabel.setText(yVar);
		}

		/**
		 * Gets the mouse controller.
		 * 
		 * @return the current mouse controller
		 */
		protected MouseInputAdapter getMouseController() {
			return mouseController;
		}

		/**
		 * An inner class for selecting points on this plot.
		 */
		protected class SelectionBox extends Rectangle implements Drawable {
			boolean visible = true;
			int xstart, ystart;
			Color color = new Color(0, 255, 0, 127);

			@Override
			public void setSize(int w, int h) {
				int xoffset = Math.min(0, w);
				int yoffset = Math.min(0, h);
				w = Math.abs(w);
				h = Math.abs(h);
				super.setLocation(xstart + xoffset, ystart + yoffset);
				super.setSize(w, h);
			}

			@Override
			public void draw(DrawingPanel drawingPanel, Graphics g) {
				if (visible) {
					Graphics2D g2 = (Graphics2D) g;
					g2.setColor(color);
					g2.draw(this);
				}
			}

		} // end SelectionBox class

		/**
		 * An inner class to draw crossbars on the measured point.
		 */
		protected class Crossbars {
			double x, y;
			Color color = new Color(0, 0, 0);

			/**
			 * Draws the crossbars on the specified Graphics.
			 * 
			 * @param g the Graphics object
			 */
			public void draw(Graphics g) {
				if (!positionVisible || java.lang.Double.isNaN(value)) {
					return;
				}
				Color c = g.getColor();
				g.setColor(color);
				g.drawLine(getLeftGutter(), yToPix(y), getWidth() - getRightGutter() - 1, yToPix(y));
				g.drawLine(xToPix(x), getTopGutter(), xToPix(x), getHeight() - getBottomGutter() - 1);
				g.setColor(c);
			}

		} // end Crossbars class

		/**
		 * An inner class to draw a slope line on the measured point.
		 */
		protected class SlopeLine extends Line2D.Double {
			double x, y;
			Stroke stroke = new BasicStroke(2.5f);
			int length = 30;
			Color color = new Color(102, 102, 102);

			/**
			 * Draws this slope line on the specified Graphics.
			 * 
			 * @param g the Graphics object
			 */
			public void draw(Graphics g) {
				if (!slopeVisible || java.lang.Double.isNaN(slope)) {
					return;
				}
				Graphics2D g2 = (Graphics2D) g.create();
				double dxPix = 1 * getXPixPerUnit();
				double dyPix = slope * getYPixPerUnit();
				double hyp = Math.sqrt(dxPix * dxPix + dyPix * dyPix);
				double sin = dyPix / hyp;
				double cos = dxPix / hyp;
				int xCenter = xToPix(x);
				int yCenter = yToPix(y);
				int len = length;
				if (dataTool.slopeExtended) {
					len *= 40;
					int w = plot.getWidth() - plot.getRightGutter() - plot.getLeftGutter();
					int h = plot.getHeight() - plot.getTopGutter() - plot.getBottomGutter();
					Rectangle rect = new Rectangle(plot.getLeftGutter(), plot.getTopGutter(), w, h);
					g2.setClip(rect);
				}
				setLine(xCenter - len * cos + 1, yCenter + len * sin + 1, xCenter + len * cos + 1,
						yCenter - len * sin + 1);
				g2.setColor(color);
				g2.setStroke(stroke);
				g2.draw(this);
				g2.dispose(); // BH 2020.02.26
			}
		} // end SlopeLine class

		/**
		 * An inner class that draws a vertical limit line for areas.
		 */
		protected class LimitLine extends Line2D.Double implements Selectable {
			int pointIndex = -1;
			double x, trueLimit;
			Stroke stroke = new BasicStroke(1.5f);
			Rectangle hitRect = new Rectangle();
			Color color = new Color(51, 51, 51);
			Color trueLimitColor;
			Cursor move;
			boolean isAdjusting;

			@Override
			public void draw(DrawingPanel panel, Graphics g) {
				if (!areaVisible) {
					return;
				}
				if (trueLimitColor == null) {
					trueLimitColor = color.brighter().brighter().brighter();
				}
				Graphics2D g2 = (Graphics2D) g.create();
				
				// set this to current limit line position
				int y0 = plot.getTopGutter();
				int y1 = plot.getBounds().height - plot.getBottomGutter();
				int x1 = plot.xToPix(x);
				setLine(x1 + 1, y0, x1 + 1, y1);
				
				// draw limit line if adjusting
				if (isAdjusting) {
					g2.setColor(color);
					g2.setStroke(stroke);
					g2.draw(this);
				}
				
				// draw truelimit line and set hitRect
				x1 = plot.xToPix(trueLimit);
				g2.setColor(trueLimitColor);
				g2.drawLine(x1 + 1, y0, x1 + 1, y1);
				g2.dispose();
				hitRect.setBounds(x1 - 2, y0, 6, y1 - y0 - 20);
			}

			@Override
			public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
				if (areaVisible && hitRect.contains(xpix, ypix)) {
					return this;
				}
				return null;
			}

			@Override
			public Cursor getPreferredCursor() {
				if (move == null) {
					// create cursor
					String imageFile = "/org/opensourcephysics/resources/tools/images/limitcursor.gif"; //$NON-NLS-1$
					Image im = ResourceLoader.getImage(imageFile);
					move = GUIUtils.createCustomCursor(im, new Point(16, 16), "Move Integration Limit", //$NON-NLS-1$
							Cursor.MOVE_CURSOR);
				}
				return move;
			}

			@Override
			public void setXY(double x, double y) {
				setX(x);
			}

			@Override
			public void setX(double x) {
				Dataset data = dataTable.workingData;
				pointIndex = -1;
				if (mouseEvent != null && mouseEvent.isShiftDown()) {
					pointIndex = findIndexNearestX(x, data);
				}
				this.x = (pointIndex == -1) ? x : data.getXPointsRaw()[pointIndex];
				refreshArea();
				createMessage();
				plot.setMessage(message);
			}

			@Override
			public boolean isMeasured() {
				return areaVisible;
			}

			@Override
			public double getXMin() {
				Dataset data = dataTable.workingData;
				double dx = 0, min = 0;
				if ((data != null) && (data.getIndex() > 1)) {
					dx = Math.abs(data.getXMax() - data.getXMin());
					min = Math.min(data.getXMax(), data.getXMin());
				} else {
					dx = Math.abs(areaLimits[0].x - areaLimits[1].x);
					min = Math.min(areaLimits[0].x, areaLimits[1].x);
				}
				return min - 0.02 * dx;
			}

			@Override
			public double getXMax() {
				Dataset data = dataTable.workingData;
				double dx = 0, max = 0;
				if ((data != null) && (data.getIndex() > 1)) {
					dx = Math.abs(data.getXMax() - data.getXMin());
					max = Math.max(data.getXMax(), data.getXMin());
				} else {
					dx = Math.abs(areaLimits[0].x - areaLimits[1].x);
					max = Math.max(areaLimits[0].x, areaLimits[1].x);
				}
				return max + 0.02 * dx;
			}

			@Override
			public double getYMin() {
				return (plot.getYMin() + plot.getYMax()) / 2;
			}

			@Override
			public double getYMax() {
				return (plot.getYMin() + plot.getYMax()) / 2;
			}

			/**
			 * refreshes the value of x based on current pointIndex.
			 */
			public void refreshX() {
				double[] data = dataTable.workingData.getXPointsRaw();
				int n = dataTable.workingData.getIndex();
				if (pointIndex > -1 && pointIndex < n && !java.lang.Double.isNaN(data[pointIndex])) {
					x = data[pointIndex];
				}
			}

			// the following methods are required by Selectable but not used

			@Override
			public void setY(double y) {
			}

			@Override
			public double getX() {
				return x;
			}

			@Override
			public double getY() {
				return 0;
			}

			@Override
			public void setSelected(boolean selectable) {
			}

			@Override
			public boolean isSelected() {
				return false;
			}

			@Override
			public void toggleSelected() {
			}

			@Override
			public boolean isEnabled() {
				return true;
			}

			@Override
			public void setEnabled(boolean enable) {
			}

		} // end LimitLine class

		/**
		 * An inner class that draws coordinate axes on the plot.
		 */
		protected class XYAxes extends TPoint {
			Line2D axisLine = new Line2D.Double();
			Stroke stroke = new BasicStroke(1.0f);
			Color color = Color.green.darker();
			Rectangle hitRectVert = new Rectangle(), hitRectHorz = new Rectangle();
			Ellipse2D hitOrigin = new Ellipse2D.Double();
			Point mouseDownPt;
			double mouseDownShiftX, mouseDownShiftY;
			boolean isHorzHit, isVertHit;

			@Override
			public void draw(DrawingPanel panel, Graphics g) {
				Color gcolor = g.getColor();
				g.setColor(color);
				Graphics2D g2 = (Graphics2D) g;
				int top = plot.getTopGutter();
				int h = plot.getBounds().height;
				int bottom = h - plot.getBottomGutter();
				int xx = plot.xToPix(0);
				int left = plot.getLeftGutter();
				int w = plot.getBounds().width;
				int right = w - plot.getRightGutter();
				int yy = plot.yToPix(0);
				g2.drawLine(xx, top, xx, bottom);
				g2.drawLine(left, yy, right, yy);
				if (originShiftEnabled) {
					g2.drawOval(xx - 6, yy - 6, 12, 12);
				}
				g.setColor(gcolor);

				// set up hit shapes
				hitRectHorz.setBounds(left, yy - 4, w, 8);
				hitRectVert.setBounds(xx - 4, top, 8, h);
				hitOrigin.setFrameFromCenter(xx, yy, xx + 6, yy + 6);
			}

			@Override
			public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
				isHorzHit = false;
				isVertHit = false;
				if (originShiftEnabled) {
					isHorzHit = hitRectHorz.contains(xpix, ypix);
					isVertHit = hitRectVert.contains(xpix, ypix);
					if (hitOrigin.contains(xpix, ypix)) {
						isHorzHit = isVertHit = true;
					}
					if (isHorzHit || isVertHit) {
						return this;
					}
				}
				return null;
			}

			@Override
			public boolean isMeasured() {
				return originShiftEnabled;
			}

			@Override
			public void setXY(double x, double y) {
			}

			@Override
			public double getXMin() {
				return getX() - getXSetback();
			}

			@Override
			public double getXMax() {
				return getX() + getXSetback();
			}

			@Override
			public double getYMin() {
				return getY() - getYSetback();
			}

			@Override
			public double getYMax() {
				return getY() + getYSetback();
			}

			/**
			 * Gets the minimum setback of the x-axis from the plot edges.
			 * 
			 * @return the minimum setback
			 */
			private double getXSetback() {
				if (originShiftEnabled && plot.dataPresent) {
					Dataset data = dataTable.workingData;
					double w = Math.abs(data.getXMax() - data.getXMin());
					w = Math.max(w, Math.abs(getX() - data.getXMax()));
					w = Math.max(w, Math.abs(getX() - data.getXMin()));
					return w / 20;
				}
				return 0;
			}

			/**
			 * Gets the minimum setback of the y-axis from the plot edges.
			 * 
			 * @return the minimum setback
			 */
			private double getYSetback() {
				if (originShiftEnabled && plot.dataPresent) {
					Dataset data = dataTable.workingData;
					double h = Math.abs(data.getYMax() - data.getYMin());
					h = Math.max(h, Math.abs(getY() - data.getYMax()));
					h = Math.max(h, Math.abs(getY() - data.getYMin()));
					return h / 20;
				}
				return 0;
			}

		} // end XYAxes class

		/**
		 * An inner CoordinateStringBuilder class that uses datatable formats.
		 */
		protected class PlotCoordinateStringBuilder extends CartesianCoordinateStringBuilder {

			String defaultXLabel = "x=", defaultYLabel = "  y="; //$NON-NLS-1$ //$NON-NLS-2$

			PlotCoordinateStringBuilder() {
				decimalFormat = org.opensourcephysics.numerics.Util.newDecimalFormat("0.00#"); //$NON-NLS-1$
				scientificFormat = org.opensourcephysics.numerics.Util.newDecimalFormat("0.00#E0"); //$NON-NLS-1$
			}

			@Override
			public String getCoordinateString(DrawingPanel panel, MouseEvent e) {
				// determine if x and y columns are actually displayed in the table
				boolean xColDisplayed = false, yColDisplayed = false;
				for (int i = 0; i < dataTable.getColumnCount(); i++) {
					if (xVar != null && xVar.equals(dataTable.getColumnName(i))) {
						xColDisplayed = true;
					}
					if (yVar != null && yVar.equals(dataTable.getColumnName(i))) {
						yColDisplayed = true;
					}
				}
				String labelX = xColDisplayed ? xLabel : defaultXLabel;
				String labelY = yColDisplayed ? yLabel : defaultYLabel;

				// get values to display
				double x = panel.pixToX(e.getPoint().x);
				double y = panel.pixToY(e.getPoint().y);
				if ((panel.isInteractive) && ((InteractivePanel) panel).getCurrentDraggable() != null) {
					x = ((InteractivePanel) panel).getCurrentDraggable().getX();
					y = ((InteractivePanel) panel).getCurrentDraggable().getY();
				}

				// get formatted values
				Object xValue = "", yValue = ""; //$NON-NLS-1$ //$NON-NLS-2$
				if (xColDisplayed) {
					xValue = getFormattedValue(x, xVar);
				} else {
					if (Math.abs(x) > 100 || Math.abs(x) < 1.0) {
						xValue = scientificFormat.format((float) x);
					} else {
						xValue = decimalFormat.format((float) x);
					}
				}
				if (yColDisplayed) {
					yValue = getFormattedValue(y, yVar);
				} else {
					if (Math.abs(y) > 100 || Math.abs(y) < 1.0) {
						yValue = scientificFormat.format((float) y);
					} else {
						yValue = decimalFormat.format((float) y);
					}
				}

				String msg = ""; //$NON-NLS-1$
				if (labelX != null) {
					msg = labelX + xValue;
				}
				if (labelY != null) {
					msg += labelY + yValue;
				}
				return msg;
			}

			void refreshFormats() {
				scientificFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
				decimalFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
			}

			/**
			 * Gets a value formatted for a given table column.
			 *
			 * @param value    the value
			 * @param colNamae the column name
			 * @return the value formatted as it would be displayed in the table
			 */
			public Object getFormattedValue(Object value, String colName) {
				if (value == null)
					return null;

				int col = -1;
				for (int i = 0; i < dataTable.getColumnCount(); i++) {
					if (colName != null && colName.equals(dataTable.getColumnName(i))) {
						col = i;
						break;
					}
				}
				if (col > -1) {
					TableCellRenderer renderer = dataTable.getCellRenderer(0, col);
					Component c = renderer.getTableCellRendererComponent(dataTable, value, false, false, 0, col);
					if (c instanceof JLabel) {
						String s = ((JLabel) c).getText().trim(); // formatting includes units, if any
						return s;
					}
				}
				return value;
			}

		} // end PlotCoordinateStringBuilder class

	} // end DataToolPlotter class

	/**
	 * A class to undo/redo origin shift edits.
	 */
	protected class ShiftEdit extends AbstractUndoableEdit {
		double[] redoShift, undoShift;
		String[] columnName;

		/**
		 * Contructor.
		 *
		 * @param colName   the column names {xCol, yCol}
		 * @param newShift  the new shift values {xShift, yShift}
		 * @param prevShift the previous shift values {xShift, yShift}
		 */
		public ShiftEdit(String[] colNames, double[] newShifts, double[] prevShifts) {
			columnName = colNames;
			redoShift = newShifts;
			undoShift = prevShifts;
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			for (int i = 0; i < columnName.length; i++) {
				Dataset data = dataTable.getDataset(columnName[i]);
				if (data != null && data instanceof DataColumn) {
					DataColumn dataCol = (DataColumn) data;
					dataCol.setShift(undoShift[i]);
					// shift area limits
					if (i == 0) {
						if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
							plot.areaLimits[0].refreshX();
							plot.areaLimits[1].refreshX();
						} else {
							plot.areaLimits[0].setX(plot.areaLimits[0].getX() + undoShift[0] - redoShift[0]);
							plot.areaLimits[1].setX(plot.areaLimits[1].getX() + undoShift[0] - redoShift[0]);
						}
					}
				}
			}
			refreshAll(DataTable.MODE_VALUES);
			shiftEditListener.valueChanged = false;
		}

		@Override
		public void redo() throws CannotUndoException {
			super.redo();
			for (int i = 0; i < columnName.length; i++) {
				Dataset data = dataTable.getDataset(columnName[i]);
				if (data != null && data instanceof DataColumn) {
					DataColumn dataCol = (DataColumn) data;
					dataCol.setShift(redoShift[i]);
					// shift area limits
					if (i == 0) {
						if (plot.areaLimits[0].pointIndex > -1 && plot.areaLimits[1].pointIndex > -1) {
							plot.areaLimits[0].refreshX();
							plot.areaLimits[1].refreshX();
						} else {
							plot.areaLimits[0].setX(plot.areaLimits[0].getX() + redoShift[0] - undoShift[0]);
							plot.areaLimits[1].setX(plot.areaLimits[1].getX() + redoShift[0] - undoShift[0]);
						}
					}
				}
			}
			refreshAll(DataTable.MODE_VALUES);
			shiftEditListener.valueChanged = false;
		}
	}

	/**
	 * A number spinner model with a settable delta.
	 */
	class CrawlerSpinnerModel extends AbstractSpinnerModel {
		double val = 0;
		double delta = 1;
		double percentDelta = 1;

		@Override
		public Object getValue() {
			return new Double(val);
		}

		@Override
		public Object getNextValue() {
			return new Double(val + delta);
		}

		@Override
		public Object getPreviousValue() {
			return new Double(val - delta);
		}

		@Override
		public void setValue(Object value) {
			if (value != null) {
				val = ((Double) value).doubleValue();
				fireStateChanged();
			}
		}

//    public void setPercentDelta(double percent) {
//      percentDelta = percent;
//    }
//
//    public double getPercentDelta() {
//      return percentDelta;
//    }
//
		// refresh delta based on current value and percent
		public void refreshDelta() {
			if (val != 0) {
				delta = Math.abs(val * percentDelta / 100);
			} else {
				if (shiftXSpinner.getModel() == this) {
					Dataset dataset = dataTable.getDataset(plot.xVar);
					if (dataset != null) {
						double range = dataset.getYMax() - dataset.getYMin();
						delta = range * percentDelta / 100;
					}
				} else if (shiftYSpinner.getModel() == this) {
					Dataset dataset = dataTable.getDataset(plot.yVar);
					if (dataset != null) {
						double range = dataset.getYMax() - dataset.getYMin();
						delta = range * percentDelta / 100;
					}
				}
			}
		}

	}

	class ShiftEditListener implements ChangeListener, ActionListener {

		// minimum time in ms between update requests
		static final int MIN_TIME = 400;
		long lastChange = System.currentTimeMillis();
		boolean valueChanged = false;
		Timer repeatTimer;

		public ShiftEditListener() {
			// timer polling every 200ms
			repeatTimer = new Timer(200, this);
			repeatTimer.start();
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			valueChanged = true;
			lastChange = System.currentTimeMillis();
		}

		// action called by timer
		@Override
		public void actionPerformed(ActionEvent e) {
			if (valueChanged && (System.currentTimeMillis() - lastChange) > MIN_TIME) {
				valueChanged = false;
				if (shiftXField.hasFocus() || shiftYField.hasFocus()) {
					// post undoable edit
					postShiftEdit();
				} else if (selectedXField.hasFocus() || selectedYField.hasFocus()) {
					// post undoable edit
					postShiftEdit();
				}
			}
		}
	}

//__________________________ static methods and classes ___________________________

	/**
	 * Returns an ObjectLoader to save and load data for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

//	public static int findNearestXIndex(double x, double[] xpoints, double[] ypoints, int len, double min, double max) {
//
//		x = Math.min(max, Math.max(min, x));
//
//
//		// sort x data, keeping only points for which the y-value is not NaN
//		ArrayList<Double> valid = new ArrayList<Double>();
//		for (int i = 0; i < len; i++) {
//			if (Double.isNaN(ypoints[i]))
//				continue;
//			valid.add(xpoints[i]);
//		}
//		Double[] sorted = valid.toArray(new Double[valid.size()]);
//		java.util.Arrays.sort(sorted);
//		int last = sorted.length - 1;
//
//		// check if pixel outside data range
//		if (x < sorted[0]) {
//			return 0;
//		}
//		if (x >= sorted[last]) {
//			return last;
//		}
//
//		// look thru sorted data to find point nearest x
//		for (int i = 1; i < sorted.length; i++) {
//			if (x >= sorted[i - 1] && x < sorted[i]) {
//				// found it
//				if (sorted[i - 1] < min) {
//					x = sorted[i];
//				} else if (sorted[i] > max) {
//					x = sorted[i - 1];
//				} else {
//					x = (Math.abs(x - sorted[i - 1]) < Math.abs(x - sorted[i])) ? sorted[i - 1] : sorted[i];
//				}
//
//				// find index of first data point with this value of x
//				for (int j = 0; j < xpoints.length; j++) {
//					if (xpoints[j] == x && !Double.isNaN(ypoints[j])) {
//						return j;
//					}
//				}
//				return -1;
//			}
//		}
//		return -1; // none found (should never get here)
//}

/**
	 * Far more efficient method of searching a sparse array. Just track the closest
	 * point, running linearly through the array.
	 * 
	 * See test.PerformanceTests.findNearestXIndex.
	 * 
	 * @param x
	 * @param xpoints
	 * @param ypoints
	 * @param len
	 * @param min
	 * @param max
	 * @return
	 */
	public static int findNearestXIndex(double x, double[] xpoints, double[] ypoints, int len, double min, double max) {

		x = Math.min(max, Math.max(min, x));

		int imin = -1;
		double dxmin = Double.MAX_VALUE;
		for (int i = 0; i < len; i++) {
			if (Double.isNaN(ypoints[i]))
				continue;
			double dx = Math.abs(x - xpoints[i]);
			if (dx < dxmin) {
				dxmin = dx;
				imin = i;
			}
		}
		if (xpoints[imin] < min)
			imin++;
		if (imin == len || xpoints[imin] > max)
			imin = len - 1;
		return imin;
	}
//
//	static {
//		double[] xp = new double[] { 1, 2, 3, 4, 5, 6, 0, 0, 0 };
//		double[] yp = new double[] { 0, 0, Double.NaN, 0, 0, 0 };
//		System.out.println(findNearestXIndex(3.1, xp, yp, 6, 0, 10) == 3);
//		System.out.println(findNearestXIndex(3.1, xp, yp, 3, 0, 10) == 1);
//		System.out.println(findNearestXIndex(3.1, xp, yp, 6, 0, 3) == 1);
//		System.out.println(findNearestXIndex(3.1, xp, yp, 6, 5, 10) == 4);
//		System.out.println(findNearestXIndex(3.1, xp, yp, 6, 0, 10) == 3);
//		System.out.println(findNearestXIndex(3.1, xp, yp, 3, 0, 10) == 1);
//		System.out.println(findNearestXIndex(3.1, xp, yp, 6, 0, 3) == 1);
//		System.out.println(findNearestXIndex(3.1, xp, yp, 6, 5, 10) == 4);
//		System.out.println("");
//	}

	/**
	 * A class to save and load data for this class.
	 */
	static class Loader implements XML.ObjectLoader {

		@Override
		public void saveObject(XMLControl control, Object obj) {
			DataToolTab tab = (DataToolTab) obj;
			// save name and owner name
			control.setValue("name", tab.getName()); //$NON-NLS-1$
			control.setValue("owner_name", tab.getOwnerName()); //$NON-NLS-1$
			// save owned columns
			if (!tab.ownedColumns.isEmpty()) {
				String[][] columns = new String[tab.ownedColumns.size()][3];
				// each element is {tab column name, owner/guest name, owner/guest dataset
				// y-column name}
				int i = 0;
				for (String key : tab.ownedColumns.keySet()) {
					String[] data = tab.ownedColumns.get(key);
					columns[i] = new String[] { key, data[0], data[1] };
					i++;
				}
				control.setValue("owned_columns", columns); //$NON-NLS-1$
			}

			// save userEditable
			control.setValue("editable", tab.userEditable); //$NON-NLS-1$
			// save data columns but leave out data functions
			DatasetManager data = new DatasetManager();
			ArrayList<Dataset> functions = new ArrayList<Dataset>();
			ArrayList<Dataset> datasets = tab.dataManager.getDatasetsRaw();
			for (int i = 0, n = datasets.size(); i < n; i++) {
				Dataset next = datasets.get(i);
				if (next instanceof DataFunction) {
					functions.add(next);
				} else {
					data.addDataset(next);
				}
			}
			control.setValue("data", data); //$NON-NLS-1$
			// save function parameters
			ArrayList<String> paramNames = tab.dataManager.getConstantNames();
			int n = paramNames.size();
			if (n > 0) {
				Object[][] paramArray = new Object[n][4];
				int i = 0;
				for (String name : paramNames) {
					paramArray[i][0] = name;
					paramArray[i][1] = tab.dataManager.getConstantValue(name);
					paramArray[i][2] = tab.dataManager.getConstantExpression(name);
					paramArray[i][3] = tab.dataManager.getConstantDescription(name);
					i++;
				}
				control.setValue("constants", paramArray); //$NON-NLS-1$
			}
			// save data functions
			if (!functions.isEmpty()) {
				DataFunction[] f = functions.toArray(new DataFunction[0]);
				control.setValue("data_functions", f); //$NON-NLS-1$
			}
			// save origin shifted status
			if (tab.originShiftEnabled) {
				control.setValue("origin_shifted", tab.originShiftEnabled); //$NON-NLS-1$
			}
			// save selected fit function panel
			// note: as of Dec 2014, no longer save ALL fitBuilder panels since most
			// are for default or autoloaded functions
			tab.getCurveFitter();
			if (tab.dataTool.fitBuilder != null) {
				String fitName = tab.curveFitter.fit.getName();
				FitFunctionPanel panel = (FitFunctionPanel) tab.dataTool.fitBuilder.getPanel(fitName);
				if (panel != null) {
					ArrayList<FunctionPanel> fits = new ArrayList<FunctionPanel>();
					fits.add(panel);
					control.setValue("fits", fits); //$NON-NLS-1$
				}
			}
			// save selected fit name
			control.setValue("selected_fit", tab.curveFitter.fit.getName()); //$NON-NLS-1$
			// save autofit status
			control.setValue("autofit", tab.curveFitter.isAutoFit()); //$NON-NLS-1$
			// save fit parameters
//      if (!tab.curveFitter.autofitCheckBox.isSelected()) {
			double[] params = new double[tab.curveFitter.paramModel.getRowCount()];
			for (int i = 0; i < params.length; i++) {
				Double val = (Double) tab.curveFitter.paramModel.getValueAt(i, 1);
				params[i] = val.doubleValue();
			}
			control.setValue("fit_parameters", params); //$NON-NLS-1$
//      }
			// save fit color
			control.setValue("fit_color", tab.curveFitter.color); //$NON-NLS-1$
			// save fit visibility
			control.setValue("fit_visible", tab.isFitterVisible()); //$NON-NLS-1$
			// save props visibility
			control.setValue("props_visible", tab.propsCheckbox.isSelected()); //$NON-NLS-1$
			// save statistics visibility
			control.setValue("stats_visible", tab.statsCheckbox.isSelected()); //$NON-NLS-1$
			// save splitPane locations
			int loc = tab.splitPanes[0].getDividerLocation();
			control.setValue("split_pane", loc); //$NON-NLS-1$
			loc = tab.curveFitter.getSplitPane().getDividerLocation();
			control.setValue("fit_split_pane", loc); //$NON-NLS-1$
			// save model column order
			int[] cols = tab.dataTable.getModelColumnOrder();
			control.setValue("column_order", cols); //$NON-NLS-1$
			// save hidden markers
			String[] hidden = tab.dataTable.getHiddenMarkers();
			control.setValue("hidden_markers", hidden); //$NON-NLS-1$
			// save column format patterns, if any
			String[] patternColumns = tab.dataTable.getFormattedColumnNames();
			if (patternColumns.length > 0) {
				ArrayList<String[]> patterns = new ArrayList<String[]>();
				for (int i = 0; i < patternColumns.length; i++) {
					String colName = patternColumns[i];
					String pattern = tab.dataTable.getFormatPattern(colName);
					patterns.add(new String[] { colName, pattern });
				}
				control.setValue("format_patterns", patterns); //$NON-NLS-1$
			}
		}

		@Override
		public Object createObject(XMLControl control) {
			// get DataTool from control
			DataTool dataTool = (DataTool) control.getObject("datatool"); //$NON-NLS-1$
			// load data
			DatasetManager data = (DatasetManager) control.getObject("data"); //$NON-NLS-1$
			if (data == null) {
				return new DataToolTab(null, dataTool);
			}
			for (Dataset next : data.getDatasetsRaw()) {
				next.setXColumnVisible(false);
			}
			return new DataToolTab(data, dataTool);
		}

		@Override
		public Object loadObject(XMLControl control, Object obj) {
			final DataToolTab tab = (DataToolTab) obj;
			// load tab name and owner name, if any
			tab.setName(control.getString("name")); //$NON-NLS-1$
			tab.ownerName = control.getString("owner_name"); //$NON-NLS-1$
			// load owned columns
			String[][] columns = (String[][]) control.getObject("owned_columns"); //$NON-NLS-1$
			if (columns != null) {
				tab.ownedColumns.clear();
				for (String[] next : columns) {
					// next is {tab column name, owner/guest name, owner/guest dataset y-column
					// name}
					// column name becomes key in map to owner/guest data
					String[] data = new String[] { next[1], next[2] };
					tab.ownedColumns.put(next[0], data);
				}
			}
			// load data functions and constants
			Object[][] constants = (Object[][]) control.getObject("constants"); //$NON-NLS-1$
			if (constants != null) {
				for (int i = 0; i < constants.length; i++) {
					String name = (String) constants[i][0];
					double val = (Double) constants[i][1];
					String expression = (String) constants[i][2];
					if (constants[i].length >= 4) {
						String desc = (String) constants[i][3];
						tab.dataManager.setConstant(name, val, expression, desc);
					} else
						tab.dataManager.setConstant(name, val, expression);
				}
			}
			Iterator<XMLProperty> it = control.getPropsRaw().iterator();
			while (it.hasNext()) {
				XMLProperty prop = it.next();
				if (prop.getPropertyName().equals("data_functions")) { //$NON-NLS-1$
					XMLControl[] children = prop.getChildControls();
					for (int i = 0; i < children.length; i++) {
						DataFunction f = new DataFunction(tab.dataManager);
						children[i].loadObject(f);
						f.setXColumnVisible(false);
						tab.dataManager.addDataset(f);
					}
					// refresh dataFunctions
					ArrayList<Dataset> datasets = tab.dataManager.getDatasetsRaw();
					for (int i = 0; i < datasets.size(); i++) {
						Dataset d = datasets.get(i);
						if (d instanceof DataFunction) {
							((DataFunction) d).refreshFunctionData();
						}
					}
					tab.dataTable.refreshTable(DataTable.MODE_CREATE);
					break;
				}
			}
			// load userEditable
			tab.userEditable = control.getBoolean("editable"); //$NON-NLS-1$
			// load user fit function panels
			ArrayList<?> fits = (ArrayList<?>) control.getObject("fits"); //$NON-NLS-1$
			if (fits != null) {
				for (Iterator<?> iter = fits.iterator(); iter.hasNext();) {
					FitFunctionPanel panel = (FitFunctionPanel) iter.next();
					tab.dataTool.getFitBuilder().addPanel(panel.getName(), panel);
				}
			}

			// select fit
			tab.getCurveFitter();
			String fitName = control.getString("selected_fit"); //$NON-NLS-1$
			tab.curveFitter.setSelectedItem(fitName);
			tab.curveFitter.selectFit(fitName);
			// load fit parameters
			final double[] params = (double[]) control.getObject("fit_parameters"); //$NON-NLS-1$
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					tab.curveFitter.setParameterValue(i, params[i]);
				}
			}
			// load autofit
			boolean autofit = control.getBoolean("autofit"); //$NON-NLS-1$
			tab.curveFitter.setAutoFit(autofit);
			// load fit color
			Color color = (Color) control.getObject("fit_color"); //$NON-NLS-1$
			tab.curveFitter.setColor(color);
			// load fit visibility
			if (control.getBoolean("fit_visible")) //$NON-NLS-1$
			 tab.showFitterAction.actionPerformed(null);

//      // don't load load props visibility: always visible!
//      vis = control.getBoolean("props_visible"); //$NON-NLS-1$
//      tab.propsCheckbox.setSelected(vis);

			// load stats visibility
			boolean vis = control.getBoolean("stats_visible"); //$NON-NLS-1$
			tab.statsCheckbox.setSelected(vis);
			// load splitPane locations
			final int loc = control.getInt("split_pane"); //$NON-NLS-1$
			final int fitLoc = control.getInt("fit_split_pane"); //$NON-NLS-1$
			// load model column order
			int[] cols = (int[]) control.getObject("column_order"); //$NON-NLS-1$
			tab.dataTable.setModelColumnOrder(cols);
			if (cols == null) { // for legacy files: load working columns
				String[] names = (String[]) control.getObject("working_columns"); //$NON-NLS-1$
				if (names != null) {
					tab.dataTable.setWorkingColumns(names[0], names[1]);
				}
			}
			// load hidden markers
			String[] hidden = (String[]) control.getObject("hidden_markers"); //$NON-NLS-1$
			tab.dataTable.hideMarkers(hidden);
			// load format patterns
			ArrayList<?> patterns = (ArrayList<?>) control.getObject("format_patterns"); //$NON-NLS-1$
			if (patterns != null) {
				for (Iterator<?> iter = patterns.iterator(); iter.hasNext();) {
					String[] next = (String[]) iter.next();
					tab.dataTable.setFormatPattern(next[0], next[1]);
				}
			}
			// load origin_shifted
			final boolean origin_shifted = control.getBoolean("origin_shifted"); //$NON-NLS-1$
			Runnable runner = new Runnable() {
				@Override
				public synchronized void run() {
					Action a = tab.isFitterVisible()? tab.showFitterAction: tab.hideFitterAction;
					a.actionPerformed(null);
					tab.propsAndStatsAction.actionPerformed(null);
					tab.splitPanes[0].setDividerLocation(loc);
					tab.curveFitter.getSplitPane().setDividerLocation(fitLoc);
					if (origin_shifted) {
						tab.originShiftCheckbox.doClick(0);
						if (params != null) {
							for (int i = 0; i < params.length; i++) {
								tab.curveFitter.setParameterValue(i, params[i]);
							}
						}
					}
					tab.dataTable.refreshTable(DataTable.MODE_VALUES);
					tab.propsTable.refreshTable();
					tab.tabChanged(false);
				}

			};
			SwingUtilities.invokeLater(runner);
			return obj;
		}

	} // end Loader class

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
