/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.event.TableModelEvent;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.controls.XMLLoader;

/**
 * Dataset stores and plots (x,y) points. Dataset is Drawable and can be
 * rendered on a DrawingPanel. Dataset extends AbstractTableModel and can be
 * rendered in a JTable.
 *
 * @author Joshua Gould
 * @author Wolfgang Christian
 * @created February 13, 2002
 * @version 1.0
 */
public class Dataset extends DataTable.DataModel implements Measurable, LogMeasurable, Data {

	public class Model extends DataTable.OSPTableModel {

		/**
			stride for table view
		*/
		protected int stride = 1;

		@Override
		public boolean isFoundOrdered() {
			double[] data = (foundColumn == 0 ? xpoints : ypoints);
			double d = Double.MAX_VALUE;
			for (int i = index; --i >= 0;) {
				if (data[i] > d)
					return false;
				d = data[i];
			}
			return true;
		}

		@Override
		public int getStride() {
			return stride;
		}

		@Override
		public int getRowCount() {
			return Dataset.this.getRowCount();
		}

		@Override
		public int getColumnCount() {
			return Dataset.this.getColumnCount();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return Double.valueOf(Dataset.this.getValueAt(rowIndex, columnIndex));
		}
		

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return Double.class;
		}

		protected void setStride(int stride) {
			this.stride = stride;
			// for DataTable
			fireTableChanged(new TableModelEvent(this, 0, Integer.MAX_VALUE, stride, TableModelEvent.HEADER_ROW));		
		}


	}

	final public Model model;
	
	protected static int id = 0;
	
	/**
	 * No two Datasets or updates of datasets have the same update number.
	 * 
	 */
	private void updateID() {
		update = ++id;
	}

	/** Field datasetID: an integer ID that identifies this object */
	protected int datasetID = hashCode();

	/** Field columnID: an integer used by DataTool */
	protected int columnID = 0;

	/** Field NO_MARKER */
	public final static int NO_MARKER = 0; // no marker

	/** Field CIRCLE */
	public final static int CIRCLE = 1; // marker type

	/** Field SQUARE */
	public final static int SQUARE = 2; // marker type

	/** Field AREA */
	public final static int AREA = 5; // marker type

	/** Field PIXEL */
	public final static int PIXEL = 6; // marker type

	/** Field BAR */
	public final static int BAR = 7; // marker type

	/** Field POST */
	public final static int POST = 8; // marker type

	/** Field POST */
	public final static int CUSTOM = -1; // marker type

	/** maxPointsMultiplier: a multiplier for maxPoints */
	public static double maxPointsMultiplier = 1.0;

	/** defaultMaxPoints: the default maxPoints */
	private static int defaultMaxPoints = 16 * 1024;
	
	// See DatasetManager.find
	
	int foundColumn = 0;
	
	protected double[] xpoints = new double[initialSize];
	// array of x points

	protected double[] ypoints = new double[initialSize];
	// array of y points\

	protected double shift;

	protected GeneralPath generalPath;
	// path used to draw line plots

	protected double xmax;
	// the maximum x value in the dataset

	protected double ymax;
	// the maximum y value in the dataset

	protected double xmin;
	// the minimum x value in the dataset

	protected double ymin;
	// the minimum y value in the dataset

	protected double xmaxLogscale;
	// the maximum x value in the dataset when using a log scale

	protected double ymaxLogscale;
	// the maximum y value in the dataset when using a log scale

	protected double xminLogscale;
	// the minimum x value in the dataset when using a log scale

	protected double yminLogscale;
	// the minimum y value in the dataset when using a log scale

	protected int index;
	// the current index of the array

	protected boolean sorted = false;
	// sort the data by increasing x
	

	private final static Rectangle2D.Double tmpRect = new Rectangle2D.Double();
	
	public int update = ++id;
	
	/**
	 * the initial size of the points array
	 */
	private final static int initialSize = 10;
	
	private int markerSize = 2;
	// the size in pixels of the marker

	private int markerShape = SQUARE;
	// the type of marker

	private Color lineColor;
	// the color of the line

	private Color fillColor;
	// the fill color of the marker

	private Color edgeColor;
	// the edge color of the marker

	private Color errorBarColor;
	// the error bar color of the marker

	private boolean connected;
	// whether the points are connected

	private String name = null;
	// an optional name that can used to identify this dataset

	private String xColumnName;
	// the name of the x data

	private String yColumnName;
	// the name of the y data

	protected String xColumnDescription;
	// a description of the y data

	protected String yColumnDescription;
	// a description of the y data

	private BitSet bsColVis = new BitSet();
	// column visibilities for table view

	protected boolean visible = true;
	// visible in drawing panel; only visible dataset affect autoscale

	protected int maxPoints = defaultMaxPoints;
	// the maximum number of points that will be saved in a dataset

	protected ArrayList<ErrorBar> errorBars = new ArrayList<ErrorBar>();
	protected Shape customMarker = new Rectangle2D.Double(-markerSize / 2, -markerSize / 2, markerSize, markerSize);

	private Shape myShape;

	private AffineTransform pixelTransform;

	/**
	 * Dataset constructor.
	 */
	public Dataset() {
		this(Color.black, Color.black, false);
	}

	/**
	 * Dataset constructor specifying the marker color.
	 *
	 * @param _markerColor
	 */
	public Dataset(Color _markerColor) {
		this(_markerColor, Color.black, false);
	}

	/**
	 * Dataset constructor specifying the marker color, line color, and whether
	 * points are connected.
	 *
	 * @param markerColor
	 * @param _lineColor
	 * @param _connected
	 */
	public Dataset(Color markerColor, Color _lineColor, boolean _connected) {
		fillColor = markerColor;
		edgeColor = markerColor;
		errorBarColor = markerColor;
		lineColor = _lineColor;
		connected = _connected;
		markerSize = 2;
		xColumnName = "x"; //$NON-NLS-1$
		yColumnName = "y"; //$NON-NLS-1$
		generalPath = new GeneralPath();
		index = 0;
		bsColVis.set(0, 2);
		clear();
		model = new Model();
	}

	public Dataset set(double[] x, double[] y) {
		clear();
		append(x, y, y.length);
		return this;
	}

	/**
	 * Sets the ID number of this Data.
	 *
	 * @param id the ID number
	 */
	@Override
	public void setID(int id) {
		datasetID = id;
	}

	/**
	 * Returns a unique identifier for this Data.
	 *
	 * @return the ID number
	 */
	@Override
	public int getID() {
		return datasetID;
	}

	/**
	 * Sets the column ID.
	 *
	 * @param id the column ID
	 */
	public void setColumnID(int id) {
		columnID = id;
	}

	/**
	 * Returns the column ID.
	 *
	 * @return the column ID
	 */
	public int getColumnID() {
		return columnID;
	}

	/**
	 * Sets the sorted flag. Data is sorted by increasing x.
	 *
	 * @param _sorted <code>true<\code> to sort
	 */
	public void setSorted(boolean _sorted) {
		sorted = _sorted;
		if (sorted) {
			insertionSort();
		}
	}

	/**
	 * Sets the data connected flag. Points are connected by straight lines.
	 *
	 * @param _connected <code>true<\code> if points are connected
	 *
	 */
	public void setConnected(boolean _connected) {
		connected = _connected;
		if (connected) {
			recalculatePath();
		}
	}

	/**
	 * Sets the data point fill, edge, and error bar colors to the same color.
	 *
	 * @param markerColor
	 */
	public void setMarkerColor(Color markerColor) {
		fillColor = markerColor;
		edgeColor = markerColor;
		errorBarColor = markerColor;
	}

	/**
	 * Sets the data point marker colors.
	 *
	 * The error bar color is set equal to the edge color.
	 *
	 * @param _fillColor
	 * @param _edgeColor
	 */
	public void setMarkerColor(Color _fillColor, Color _edgeColor) {
		fillColor = _fillColor;
		edgeColor = _edgeColor;
		errorBarColor = _edgeColor;
	}

	/**
	 * Sets the data point marker colors.
	 *
	 * @param _fillColor
	 * @param _edgeColor
	 * @param _errorBarColor
	 */
	public void setMarkerColor(Color _fillColor, Color _edgeColor, Color _errorBarColor) {
		fillColor = _fillColor;
		edgeColor = _edgeColor;
		errorBarColor = _errorBarColor;
	}

	/**
	 * Gets the data point fill color.
	 *
	 * @return the fill color
	 */
	public Color getFillColor() {
		return fillColor;
	}

	/**
	 * Fill colors for Data interface.
	 * 
	 * @return color array
	 */
	@Override
	public java.awt.Color[] getFillColors() {
		return new Color[] { Color.BLACK, fillColor };
	}

	/**
	 * Gets the data point edge color.
	 *
	 * @return the edge color
	 */
	public Color getEdgeColor() {
		return edgeColor;
	}

	/**
	 * Gets the line color.
	 *
	 * @return the line color
	 */
	public Color getLineColor() {
		return lineColor;
	}

	/**
	 * Line colors for Data interface.
	 * 
	 * @return color array
	 */
	@Override
	public java.awt.Color[] getLineColors() {
		return new Color[] { Color.BLACK, lineColor };
	}

	/**
	 * Sets a custom marker shape.
	 *
	 * @param marker Shape
	 */
	public void setCustomMarker(Shape marker) {
		customMarker = marker;
		if (customMarker == null) {
			markerShape = SQUARE;
			customMarker = new Rectangle2D.Double(-markerSize / 2, -markerSize / 2, markerSize, markerSize);
		} else {
			markerShape = CUSTOM;
		}
	}

	/**
	 * Sets the data point marker shape. Shapes are: NO_MARKER, CIRCLE, SQUARE,
	 * AREA, PIXEL, BAR, POST
	 *
	 * @param _markerShape
	 */
	public void setMarkerShape(int _markerShape) {
		markerShape = _markerShape;
	}

	/**
	 * Gets the data point marker shape.
	 *
	 * @return the marker shape
	 */
	public int getMarkerShape() {
		return markerShape;
	}

	/**
	 * Sets the half-width of the data point marker.
	 *
	 * @param _markerSize in pixels
	 */
	public void setMarkerSize(int _markerSize) {
		markerSize = _markerSize;
	}

	/**
	 * Sets the maximum number of allowed datapoints.
	 *
	 * @param maxPoints int
	 */
	public void setMaximumPoints(int maxPoints) {
		this.maxPoints = maxPoints;
	}

	/**
	 * Gets the half-width of the data point marker.
	 *
	 * @return the marker size in pixels
	 */
	public int getMarkerSize() {
		return markerSize;
	}

	/**
	 * Sets the color of the lines connecting data points.
	 *
	 * @param _lineColor
	 */
	public void setLineColor(Color _lineColor) {
		lineColor = _lineColor;
	}

	/**
	 * Sets the column names when rendering this dataset in a JTable.
	 *
	 * @param _xColumnName
	 * @param _yColumnName
	 */
	public void setXYColumnNames(String _xColumnName, String _yColumnName) {
		xColumnName = TeXParser.parseTeX(_xColumnName);
		yColumnName = TeXParser.parseTeX(_yColumnName);
	}

	/**
	 * Sets the column names and the dataset name.
	 *
	 * @param xColumnName
	 * @param yColumnName
	 * @param name
	 */
	public void setXYColumnNames(String xColumnName, String yColumnName, String name) {
		setXYColumnNames(xColumnName, yColumnName);
		this.name = TeXParser.parseTeX(name);
	}

	/**
	 * Gets the x column name.
	 *
	 * @return String
	 */
	public String getXColumnName() {
		return xColumnName;
	}

	/**
	 * Gets the y column name.
	 *
	 * @return String
	 */
	public String getYColumnName() {
		return yColumnName;
	}

	/**
	 * Gets the x column description.
	 *
	 * @return the description (may be null)
	 */
	public String getXColumnDescription() {
		return xColumnDescription;
	}

	/**
	 * Sets the x column description.
	 *
	 * @param desc the description (may be null)
	 */
	public void setXColumnDescription(String desc) {
		xColumnDescription = desc;
	}

	/**
	 * Gets the y column description.
	 *
	 * @return the description (may be null)
	 */
	public String getYColumnDescription() {
		return yColumnDescription;
	}

	/**
	 * Sets the y column description.
	 *
	 * @param desc the description (may be null)
	 */
	public void setYColumnDescription(String desc) {
		yColumnDescription = desc;
	}

	/**
	 * Sets a name that can be used to identify the dataset.
	 *
	 * @param name String
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the dataset name.
	 *
	 * @return String
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Some Data objects (e.g., a Group) do not contain data, but a list of Data
	 * objects which do. This method is used by Data displaying tools to create as
	 * many pages as needed.
	 * 
	 * @return a list of Data objects, or null if this object contains data
	 */
	@Override
	public java.util.List<Data> getDataList() {
		return null;
	}

	/**
	 * The column names to be used in the data tool
	 * 
	 * @return array of names
	 */
	@Override
	public String[] getColumnNames() {
		return new String[] { xColumnName, yColumnName };
	}

	/**
	 * Gets the valid measure flag. The measure is valid if the min and max values
	 * have been set.
	 *
	 * @return <code>true<\code> if measure is valid
	 *
	 */
	@Override
	public boolean isMeasured() {
		if (visible) {
			return ymin < Double.MAX_VALUE; // changed by D.Brown
			// return index >= 1;
		}
		return false;
	}

	/**
	 * Gets the x world coordinate for the left hand side of the panel.
	 *
	 * @return xmin
	 */
	@Override
	public double getXMin() {
		return xmin;
	}

	/**
	 * Gets the x world coordinate for the right hand side of the panel.
	 *
	 * @return xmax
	 */
	@Override
	public double getXMax() {
		return xmax;
	}

	/**
	 * Gets y world coordinate for the bottom of the panel.
	 *
	 * @return ymin
	 */
	@Override
	public double getYMin() {
		return ymin;
	}

	/**
	 * Gets y world coordinate for the top of the panel.
	 *
	 * @return ymax
	 */
	@Override
	public double getYMax() {
		return ymax;
	}

	/**
	 * Gets the minimum x needed to draw this object on a log scale.
	 * 
	 * @return minimum
	 */
	@Override
	public double getXMinLogscale() {
		return xminLogscale;
	}

	/**
	 * Gets the maximum x needed to draw this object on a log scale.
	 * 
	 * @return maximum
	 */
	@Override
	public double getXMaxLogscale() {
		return xmaxLogscale;
	}

	/**
	 * Gets the minimum y needed to draw this object on a log scale.
	 * 
	 * @return minimum
	 */
	@Override
	public double getYMinLogscale() {
		return yminLogscale;
	}

	/**
	 * Gets the maximum y needed to draw this object on a log scale on a log scale.
	 * 
	 * @return maximum
	 */
	@Override
	public double getYMaxLogscale() {
		return ymaxLogscale;
	}

	/**
	 * Gets a data array containing both x and y values. The data is arranged as an
	 * array of (x,y) pairs.
	 *
	 * @return a double[index][2] array of data
	 */
	public double[][] getPoints() {
		double[][] temp = new double[index][2];
		double[] xValues = xpoints;
		double[] yValues = ypoints;
		double shift = (isShifted() ? this.shift : 0); 
		for (int i = 0; i < index; i++) {
			temp[i] = new double[] { xValues[i], yValues[i] + shift };
		}
		return temp;
	}

	/**
	 * Gets the shifted property. DataColumn only.
	 *
	 * @return true if values are shifted
	 */
	public boolean isShifted() {
		return false;
	}

	/**
	 * Sets the shifted property to shift the values of all elements.
	 * DataColumn only.
	 * 
	 * @param shift true to shift the values
	 */
	public void setShifted(boolean shifted) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the shift used to shift the values of all elements.
	 * DataColumn only.
	 * 
	 * @param shift the shift
	 * @return true if shift was changed
	 */
	public boolean setShift(double shift) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets the shift used to shift the values of all elements.
	 * DataColumn only.
	 * 
	 * @return the shift
	 */
	public double getShift() {
		return 0;
	}

	public boolean setShiftedValue(int i, double value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets a data array containing both x and y values. Implements Data. The data
	 * is arranged as a pair of separate x and y arrays.
	 *
	 * @return a double[2][index] array of data
	 */
	@Override
	public double[][] getData2D() {
		double[][] data = new double[2][index];
		data[0] = getXPoints();
		data[1] = getYPoints();
		return data;
	}

	/**
	 * Returns a null 3D array of data. Implements Data.
	 *
	 * @return null
	 */
	@Override
	public double[][][] getData3D() {
		return null;
	}

	/**
	 * Returns a list containing this Dataset. Implements Data.
	 *
	 * @return ArrayList
	 */
	@Override
	public ArrayList<Dataset> getDatasets() {
		ArrayList<Dataset> list = new ArrayList<Dataset>();
		list.add(this);
		return list;
	}

	/**
	 * Gets a copy of the xpoints array.
	 *
	 * @return xpoints clone
	 */
	public final double[] getXPoints() {
		double[] temp = new double[index];
		System.arraycopy(xpoints, 0, temp, 0, index);
		return temp;
	}

	//static int nRaw = 0;
	
	/**
	 * Get xpoints; CAUTION: You must use getIndex() to get the meaningful length of the array data.
	 * 
	 * 
	 * @return uncloned xpoints
	 */
	public final double[] getXPointsRaw() {
		return xpoints;
	}

	/**
	 * Gets a copy of the ypoints array, with shift added if shifted.
	 *
	 * @return a clone of the y points (may be shifted)
	 */
	public double[] getYPoints() {
		double[] temp = new double[index];
		if (isShifted()) {
			for (int i = 0; i < index; i++) {
				temp[i] = ypoints[i] + shift;
			}
		} else {
			System.arraycopy(ypoints, 0, temp, 0, index);
		}

		return temp;
	}

	/**
	 * CAUTION -- could be a shift in DataColumn set
	 * 
	 * @return uncloned ypoints
	 */
	public final double[] getYPointsRaw() {
			return ypoints;
	}

	public double getY(int i) {
		return ypoints[i];
	}

	/**
	 * Gets an array of valid xpoints. A point is valid if the ypoint for that index
	 * is not Double.NaN.
	 *
	 * @return valid xpoints, trimmed to (index - nNAN) length
	 */
	public double[] getValidXPoints() {
		return getValidPoints(xpoints);
	}

	/**
	 * Gets an array of valid ypoints. A point is valid if the ypoint for that index
	 * is not Double.NaN.
	 *
	 * @return valid ypoints[], trimmed to index length
	 */
	public double[] getValidYPoints() {
		return getValidPoints(ypoints);
	}

	/**
	 * Gets the sorted flag.
	 *
	 * @return <code>true<\code> if the data is sorted
	 *
	 */
	public boolean isSorted() {
		return sorted;
	}

	/**
	 * Gets the data connected flag.
	 *
	 * @return <code>true<\code> if points are connected
	 *
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Gets the number of columns for rendering in a JTable.
	 *
	 * @return the count
	 */
	@Override
	public int getColumnCount() {
		return bsColVis.cardinality();
	}

	/**
	 * Gets the current index of the array.
	 *
	 * The index is equal to the number of data points that are currently stored.
	 * When data is appended, it will fill the xpoints and ypoints arrays starting
	 * at the current index.
	 *
	 * @return the count
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the number of rows for rendering in a JTable.
	 *
	 * @return the count
	 */
	@Override
	public int getRowCount() {
		return (index + model.stride - 1) / model.stride;
	}

	/**
	 * Gets the name of the colummn for rendering in a JTable
	 *
	 * @param columnIndex
	 * @return the name
	 */
	@Override
	public String getColumnName(int columnIndex) {
		return (convertTableColumnIndex(bsColVis, columnIndex) == 0 ? xColumnName : yColumnName);
	}

//	/**
//	 * Gets an x or y value for rendering in a JTable.
//	 *
//	 * @param rowIndex
//	 * @param columnIndex
//	 * @return the datum
//	 */
//	@Override
//	public Object getValueAt(int rowIndex, int columnIndex) {
//		foundColumn = columnIndex = convertTableColumnIndex(bsColVis, columnIndex);
//		rowIndex = rowIndex * stride;
//		// conversionFactor added by D Brown Dec 2010
//		if (columnIndex == 0) {
//			return Double.valueOf(xpoints[rowIndex]);
//		}
//		double y = ypoints[rowIndex];
//		return (Double.isNaN(y) ? null : Double.valueOf(y + shift));
//	}

	@Override
	public double getValueAt(int rowIndex, int columnIndex) {
		foundColumn = columnIndex = convertTableColumnIndex(bsColVis, columnIndex);
		rowIndex = rowIndex * model.stride;
		// conversionFactor added by D Brown Dec 2010
		if (columnIndex == 0) {
			return xpoints[rowIndex];
		}
		return ypoints[rowIndex] + shift;
	}

	/**
	 * Appends a data point and its uncertainty to the Dataset.
	 *
	 * @param x
	 * @param y
	 * @param delx
	 * @param dely
	 */
	public void append(double x, double y, double delx, double dely) {
		errorBars.add(new ErrorBar(x, y, delx, dely));
		append(x, y);
	}

	/**
	 * Appends an (x,y) datum to the Dataset. A y value of Double.NaN is treated as
	 * null in plots and tables.
	 *
	 * @param x
	 * @param y
	 */
	public void append(double x, double y) {
		if (Double.isNaN(x) || Double.isInfinite(x) || Double.isInfinite(y)) {
			return;
		}
		updateID();
		myShape = null;
		if (addSorted(x, y))
			recalculatePath();
	}

	/**
	 * Add the point and shift the array if necessary
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean addSorted(double x, double y) {
		if (!Double.isNaN(y)) {
			Point2D curPt = generalPath.getCurrentPoint();
			if (curPt == null) {
				generalPath.moveTo((float) x, (float) y);
			} else {
				generalPath.lineTo((float) x, (float) y);
			}
			ymax = Math.max(y, ymax);
			ymin = Math.min(y, ymin);
			if (y > 0) {
				ymaxLogscale = Math.max(y, ymaxLogscale);
				yminLogscale = Math.min(y, yminLogscale);
			}
		}
		xmax = Math.max(x, xmax);
		xmin = Math.min(x, xmin);
		if (x > 0) {
			xmaxLogscale = Math.max(x, xmaxLogscale);
			xminLogscale = Math.min(x, xminLogscale);
		}
		if (index >= xpoints.length) {
			increaseCapacity(xpoints.length * 2);
		}
		xpoints[index] = x;
		ypoints[index] = y;
		// move the new datum if x is less than the last value.
		if (++index > 1 && sorted && xpoints[index - 2] > x) {
			moveDatum(xpoints, ypoints, index - 1);
			return true;
		}
		return false;
	}

	/**
	 * Appends arrays of data points and uncertainties to the Dataset.
	 *
	 * @param xpoints
	 * @param ypoints
	 * @param delx
	 * @param dely
	 */
	public void append(double[] xpoints, double[] ypoints, double[] delx, double[] dely) {
		for (int i = 0, n = xpoints.length; i < n; i++) {
			errorBars.add(new ErrorBar(xpoints[i], ypoints[i], delx[i], dely[i]));
		}
		append(xpoints, ypoints, xpoints.length);
	}

	/**
	 * Appends (x,y) arrays to the Dataset. Any y value of Double.NaN is treated as
	 * null in plots and tables.
	 *
	 * @param _xpoints
	 * @param _ypoints
	 */
	public void append(double[] _xpoints, double[] _ypoints) { 
		append(_xpoints, _ypoints, _xpoints.length);	
	}

	/**
	 * Allow for raw xpoints, with an index length
	 * 
	 * @param _xpoints
	 * @param _ypoints
	 * @param len
	 */
	public void append(double[] _xpoints, double[] _ypoints, int len) {
		updateID();
		boolean badData = false;
		myShape = null;
		for (int i = 0; i < len; i++) {
			double xp = _xpoints[i];
			double yp = _ypoints[i];
			if (Double.isNaN(xp) || Double.isInfinite(xp) || Double.isInfinite(yp)) {
				badData = true;
				continue;
			}
			xmax = Math.max(xp, xmax);
			xmin = Math.min(xp, xmin);
			if (xp > 0) {
				xmaxLogscale = Math.max(xp, xmaxLogscale);
				xminLogscale = Math.min(xp, xminLogscale);
			}
			if (!Double.isNaN(yp)) {
				ymax = Math.max(yp, ymax);
				ymin = Math.min(yp, ymin);
				if (yp > 0) {
					ymaxLogscale = Math.max(yp, ymaxLogscale);
					yminLogscale = Math.min(yp, yminLogscale);
				}
				Point2D curPt = generalPath.getCurrentPoint();
				if (curPt == null) {
					generalPath.moveTo((float) xp, (float) yp);
				} else {
					generalPath.lineTo((float) xp, (float) yp);
				}
			}
		}
		int pointsAdded = len;
		int availableSpots = xpoints.length - index;
		boolean increasedCapacity = false;
		if (pointsAdded > availableSpots) {
			increaseCapacity(xpoints.length + pointsAdded);
			increasedCapacity = true;
		}
		int maxPts = maxPoints == defaultMaxPoints ? (int) (maxPoints * maxPointsMultiplier) : maxPoints;
		pointsAdded = Math.min(pointsAdded, maxPts); // cannot add more than the maximum capacity
		System.arraycopy(_xpoints, Math.max(0, len - pointsAdded), xpoints, index, pointsAdded);
		System.arraycopy(_ypoints, Math.max(0, len - pointsAdded), ypoints, index, pointsAdded);
		index += pointsAdded;
		if (badData) {
			removeBadData(xpoints, ypoints, index);
		}
		if (sorted) {
			insertionSort();
		}
		if (increasedCapacity) {
			resetXYMinMax(false);
		}
	}

	/**
	 * Reads a file and appends the data contained in the file to this Dataset. The
	 * format of the file is x and y coordinates separated by tabs. Lines beginning
	 * with # are ignored.
	 * 
	 * @param inputFile
	 */
	public void read(String inputFile) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			List<String> lines = getLines(reader);
			int nlines = lines.size();
			double[][] xy = new double[2][nlines];
			int n = 0;
			for (int i = 0; i < nlines; i++) {
				String s = lines.get(i).trim();
				if ((s.length() == 0) || (s.charAt(0) == '#')) { // ignore lines beginning with #
					continue;
				}
				StringTokenizer st = new StringTokenizer(s, "\t"); //$NON-NLS-1$
				switch (st.countTokens()) {
				case 0:
					continue;
				case 2:
					xy[0][n] = Double.parseDouble(st.nextToken());
					xy[1][n] = Double.parseDouble(st.nextToken());
					n++;
					break;
				default:
					throw new IOException();
				}
			}
			append(xy[0], xy[1], n);
			updateID();
			reader.close();
		} catch (java.io.FileNotFoundException fnfe) {
			System.err.println("File " + inputFile + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (java.io.IOException ioe) {
			System.err.println("Error reading file " + inputFile); //$NON-NLS-1$
		} catch (NumberFormatException nfe) {
			System.err.println("Error reading file " + inputFile); //$NON-NLS-1$
		}
	}

	private static List<String> getLines(BufferedReader reader) throws IOException {
		String s;
		List<String> list = new ArrayList<>();
		while ((s = reader.readLine()) != null) {
			list.add(s);			
		}
		return list;
	}

	/**
	 * Writes data from this Dataset to a file. The format of the file is x and y
	 * coordinates separated by tabs.
	 * 
	 * @param outputFile
	 */
	public void write(String outputFile) {
		try {
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
			for (int i = 0; i < index; i++) {
				writer.println(xpoints[i] + "\t" + ypoints[i]); //$NON-NLS-1$
			}
			writer.close();
		} catch (java.io.FileNotFoundException fnfe) {
			System.err.println("File " + outputFile + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (java.io.IOException ioe) {
			System.err.println("Error writing file " + outputFile); //$NON-NLS-1$
		}
	}
//
//	private static int testCount = 0;

	/**
	 * Draw this Dataset in the drawing panel.
	 *
	 * @param drawingPanel
	 * @param g
	 */
	@Override
	public void draw(DrawingPanel drawingPanel, Graphics g) {
		if (!drawable()) {
			return;
		}
		Graphics2D g2 = (Graphics2D) (markerShape == NO_MARKER || markerShape == AREA ? g : g.create());
		if (markerShape != NO_MARKER && markerShape != AREA) {
			drawClip(g2, drawingPanel, markerSize);
		}
		drawData(drawingPanel, g2);
		if (g2 != g)
			g2.dispose();
	}
	
	/**
	 * Expand the clip a bit to allow the marker to extend over the axes if necessary.
	 * 
	 * @param g2
	 * @param drawingPanel
	 * @param offset
	 */
	protected static void drawClip(Graphics2D g2, DrawingPanel drawingPanel, int offset) {
		if (!OSPRuntime.allowDatasetClip)
			return;
		g2.setClip(drawingPanel.leftGutter - offset - 1, drawingPanel.topGutter - offset - 1,
				drawingPanel.getWidth() - drawingPanel.leftGutter - drawingPanel.rightGutter + 2 + 2 * offset,
				drawingPanel.getHeight() - drawingPanel.bottomGutter - drawingPanel.topGutter + 2 + 2 * offset);
		Rectangle viewRect = drawingPanel.getViewRect();
		if (viewRect != null) { // decrease the clip if we are in a scroll pane
			g2.clipRect(viewRect.x, viewRect.y, viewRect.x + viewRect.width, viewRect.y + viewRect.height);
		}
	}

	/**
	 * 
	 * @return true if there is something to draw
	 */
	protected boolean drawable() {
		if (visible)
			for (int i = 0; i < index; i++) {
				if (!Double.isNaN(ypoints[i]))
					return true;
			}
		return false;
	}

	/**
	 * Draw the data in the appropriate fashion.
	 * 
	 * @param drawingPanel
	 * @param g2
	 */
	protected void drawData(DrawingPanel drawingPanel, Graphics2D g2) {		
		if (!drawingPanel.getPixelTransform().equals(pixelTransform)) {
			myShape = null;
			pixelTransform = drawingPanel.getPixelTransform();
		}
		// BH why the try/catch here? What kind of "bad data"?
		try {
			if (myShape == null && (connected || markerShape == AREA))
				myShape = drawingPanel.transformPath(generalPath);
			switch (markerShape) {
			case NO_MARKER:
				break;
			case AREA:
				g2.setColor(fillColor);
				g2.fill(myShape);
				g2.setColor(edgeColor);
				g2.draw(myShape);
				break;
			default:
				drawScatterPlot(drawingPanel, g2);
				break;
			}
			if (connected) {
				g2.setColor(lineColor);
				g2.draw(myShape);
			}
		} catch (Exception ex) {
		} // abort drawing if we have bad data
	}

	/**
	 * Draw the markers at the data points.
	 *
	 * @param drawingPanel
	 * @param g2
	 */
	protected void drawScatterPlot(DrawingPanel drawingPanel, Graphics2D g2) {
		g2.setColor(markerShape == PIXEL ? edgeColor : fillColor);
		double bottom = (markerShape == BAR || markerShape == POST
				? Math.min(drawingPanel.yToPix(0), drawingPanel.yToPix(drawingPanel.getYMin()))
				: 0);
		int width = markerSize * 2 + 1;
		for (int i = 0; i < index; i++) {
			double x = xpoints[i];
			double y = getY(i);
			if (Double.isNaN(y) || x <= 0 && drawingPanel.isLogScaleX() || y <= 0 && drawingPanel.isLogScaleY()) {
				continue;
			}
			int xp = drawingPanel.xToPix(x);
			int yp = drawingPanel.yToPix(y);
			Shape shape = tmpRect;
			switch (markerShape) {
			case POST:
				g2.setColor(edgeColor);
				g2.drawLine(xp, yp, xp, (int) bottom);
				g2.setColor(fillColor);
				break;
			default:
			case SQUARE:
				tmpRect.setRect(xp - markerSize, yp - markerSize, width, width);
				break;
			case PIXEL:
				// draw and center the point
				tmpRect.setRect(xp, yp, 1, 1);
				g2.draw(tmpRect);
				continue;
			case BAR: // draw a bar graph.
				double barHeight = bottom - yp;
				if (barHeight > 0) {
					tmpRect.setRect(xp - markerSize, yp, width, barHeight);
				} else {
					tmpRect.setRect(xp - markerSize, bottom, width, -barHeight);
				}
				break;
			case CIRCLE:
				shape = new Ellipse2D.Double(xp - markerSize, yp - markerSize, width, width);
				break;
			case CUSTOM:
				shape = getTranslateInstance(xp, yp).createTransformedShape(customMarker);
				break;
			}
			g2.fill(shape);
			if (edgeColor != fillColor) {
				g2.setColor(edgeColor);
				g2.draw(shape);
				g2.setColor(fillColor);
			}
		}
		if (errorBars.size() > 0) {
			for (int i = errorBars.size(); --i >= 0;)
				errorBars.get(i).draw(drawingPanel, g2);
		}
	}

	/**
	 * Clear all data from this Dataset.
	 */
	public void clear() {
		index = 0;
		// BH I thought we would need this here:
		//updateID();
		// but with that, we lose the highlight when the 
		// analysis curve fit panel is pulled up.
		
		// BH note that we do not need to clear x or y arrays
		// because index is set to 0. It doesn't hurt to 
		// let these arrays increase in size a bit. They will 
		// never be enormous, and this clearing operation is 
		// carried out A LOT. 
		generalPath.reset();
		errorBars.clear();
		resetXYMinMax(true);
		myShape = null;
	}

	/**
	 * Creates a string representation of the data.
	 *
	 * @return the data
	 */
	@Override
	public String toString() {
		
		String name =  "(" + xColumnName + "," +  getYColumnName() + ") " + bsColVis + " " ;

		if (index == 0) {
			return name + "No data in dataset."; //$NON-NLS-1$
		}
		String s = xpoints[0] + " " + ypoints[0] + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		StringBuffer b = new StringBuffer(index * s.length());
		for (int i = 0; i < index; i++) {
			b.append(xpoints[i]);
			String eol = "\n"; // end of line //$NON-NLS-1$
			try { // system properties may not be readable!
				eol = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (SecurityException ex) {
			}
			b.append(" "); //$NON-NLS-1$
			// changed by D.Brown
			if (Double.isNaN(ypoints[i])) {
				b.append("null"); //$NON-NLS-1$
			} else {
				b.append(ypoints[i]);
			}
			b.append(eol);
			// s += xpoints[i] + "\t" + ypoints[i] + "\n";
		}
		return name + b.toString();
	}

	/**
	 * Counts the number of columns visible
	 *
	 * @param visible array of column visibilities
	 * @return number of visible columns
	 * @deprecated by Bob Hanson -- see bsColVis
	 */
	public static int countColumnsVisible(boolean visible[]) {
		int count = 0;
		for (int i = 0; i < visible.length; i++) {
			if (visible[i]) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Sets the visibility of the x column of this Dataset in a table view.
	 * 
	 * @param b new visibility
	 */
	public void setXColumnVisible(boolean b) {
		bsColVis.set(0, b);
	}

	/**
	 * Sets the visibility of the y column of this Dataset in a table view.
	 * 
	 * @param b new visibility
	 */
	public void setYColumnVisible(boolean b) {
		bsColVis.set(1, b);
	}

	/**
	 * Sets the visibility of this Dataset in a DrawingPanel. Only visible panels
	 * affect autoscaled panels.
	 *
	 * @param b new visibility
	 */
	public void setVisible(boolean b) {
		visible = b;
	}

	/**
	 * Gets the visibility of this dataset in the DrawingPanel.
	 * 
	 * @return boolean
	 */
	public boolean getVisible() {
		return visible;
	}

	/**
	 * Sets the stride of this Dataset in a table view.
	 */
	public void setStride(int stride) {
		model.setStride(stride);
	}

	/**
	 * Gets the visibility of the x column of this Dataset in a table view.
	 * 
	 * @return the x column visibility
	 */
	public boolean isXColumnVisible() {
		return bsColVis.get(0);
	}

	/**
	 * Gets the visibility of the y column of this Dataset in a table view.
	 * 
	 * @return the x column visibility
	 */
	public boolean isYColumnVisible() {
		return bsColVis.get(1);
	}

	/**
	 * Recalculate the general path.
	 */
	protected void recalculatePath() {
		myShape = null;
		generalPath.reset();
		if (index < 1) {
			return;
		}
		int i = 0;
		double[] xValues = xpoints;
		double[] yValues = ypoints;
		double shift = (isShifted() ? this.shift : 0);
		for (; i < index; i++) {
			if (!Double.isNaN(yValues[i])) {
				generalPath.moveTo(xValues[i], yValues[i] + shift);
				break;
			}
		}
		for (++i; i < index; i++) {
			if (!Double.isNaN(yValues[i])) {
				generalPath.lineTo(xValues[i], yValues[i] + shift);
			}
		}
	}

	/**
	 * Perform an insertion sort of the data set. Since data will be partially
	 * sorted this should be fast. Added by W. Christian.
	 */
	protected void insertionSort() {
		// need at least two points to sort.
		if (index >= 2 && sort(xpoints, ypoints, index)) {
			recalculatePath();
		}
	}

	private static boolean sort(double[] xpoints, double[] ypoints, int index) {
		boolean dataChanged = false;
		for (int i = 1; i < index; i++) {
			// Move the i-th datum if it is smaller than the one before it.
			if (xpoints[i - 1] > xpoints[i]) {
				moveDatum(xpoints, ypoints, i);
				dataChanged = true;
			}
		}
		return dataChanged;
	}

	/**
	 * Move an out-of-place datum into its correct position.
	 *
	 * @param loc the datum
	 */
	private static void moveDatum(double[] xpoints, double[] ypoints, int loc) {
		if (loc < 1) {
			// zero-th point cannot be out-of-place
			return;
		}
		// save the old values
		double x = xpoints[loc];
		double y = ypoints[loc];
		// find the insertion point
		int i = loc;
		while (--i >= 0 && xpoints[i] > x) {};
		if (++i == loc)
			return;
		System.arraycopy(xpoints, i, xpoints, i + 1, loc - i);
		System.arraycopy(ypoints, i, ypoints, i + 1, loc - i);
		xpoints[i] = x;
		ypoints[i] = y;
	}

//	static {
//		double[] x, y;
//		x = new double[] {1,2,3,4,5,2.6};
//		y = new double[] {0,1,2,3,4,5};
//		moveDatum(x, y, 5);
//		OSPLog.debug(Arrays.toString(x));
//		OSPLog.debug(Arrays.toString(y));
//		OSPLog.debug("----");
//
//		x = new double[] {1,5,4,6,7,0};
//		y = new double[] {0,1,2,3,4,5};
//		moveDatum(x, y, 2);
//		OSPLog.debug(Arrays.toString(x));
//		OSPLog.debug(Arrays.toString(y));
//		OSPLog.debug("----");
//
//		moveDatum(x, y, 5);
//		OSPLog.debug(Arrays.toString(x));
//		OSPLog.debug(Arrays.toString(y));
//		OSPLog.debug("----");
//		
//		x = new double[] {1,5,4,3,6,0};
//		y = new double[] {0,1,2,3,4,5};
//		sort(x, y, 6);
//		OSPLog.debug(Arrays.toString(x));
//		OSPLog.debug(Arrays.toString(y));
//		OSPLog.debug("----");
//		
//		x = new double[] {6,5,4,3,2,1};
//		y = new double[] {0,1,2,3,4,5};
//		sort(x, y, 6);
//		OSPLog.debug(Arrays.toString(x));
//		OSPLog.debug(Arrays.toString(y));
//		OSPLog.debug("----");
//	}
	
	
	private AffineTransform trD = new AffineTransform();

	protected AffineTransform getTranslateInstance(double tx, double ty) {
		trD.setToTranslation(tx, ty);
		return trD;
	}

	/**
	 * Efficiently removes infinities and NaN (x only) from the dataset.
	 */
	private static int removeBadData(double[] ax, double[] ay, int n) {
		int off = 0;
		for (int i = 0; i < n; i++) {
			double x = ax[i];
			double y = ay[i];
			if (Double.isNaN(x) || Double.isInfinite(x) || Double.isInfinite(y)) {
				off++;
				continue;
			}
			if (off > 0) {
				ax[i - off] = x;
				ay[i - off] = y;
			}
		}
		return n - off;
	}

//	static {
//		double[] xp = new double[] {1,2,3,Double.NaN, 4,5, Double.POSITIVE_INFINITY, 6, 0, 7};
//		double[] yp = new double[] {1,2,3,0,          4,5,        0,                 6, Double.NEGATIVE_INFINITY, 7};
//		OSPLog.debug(removeBadData(xp, yp, xp.length) + Arrays.toString(xp) + Arrays.toString(yp));
//		System.out.println("");
//	}

	/**
	 * Increase the array size up to a maximum size.
	 *
	 * @param newCapacity
	 */
	private synchronized void increaseCapacity(int newCapacity) {
		int pointsAdded = newCapacity - xpoints.length;
		int maxPts = maxPoints == defaultMaxPoints ? (int) (maxPoints * maxPointsMultiplier) : maxPoints;
		newCapacity = Math.min(newCapacity, maxPts); // do not let the number of data points exceed maxPoints
		int newIndex = Math.min(index, (3 * newCapacity) / 4); // drop 1/4 of the old data if the capacity is no longer
																// increasing
		newIndex = Math.min(newIndex, newCapacity - pointsAdded); // drop 1/4 of the old data if the capacity is no
																	// longer increasing
		if (newIndex < 0) {
			newIndex = 0;
		}
		double[] tempx = xpoints;
		xpoints = new double[newCapacity];
		System.arraycopy(tempx, index - newIndex, xpoints, 0, newIndex);
		double[] tempy = ypoints;
		ypoints = new double[newCapacity];
		System.arraycopy(tempy, index - newIndex, ypoints, 0, newIndex);
		if (index != newIndex) { // data was dropped
			index = newIndex;
			resetXYMinMax(false);
			recalculatePath();
		}
		index = newIndex;
	}

	/**
	 * Reset the minimum and maximum values.
	 */
	private void resetXYMinMax(boolean isCleared) {
		xmaxLogscale = ymaxLogscale = -Double.MAX_VALUE;
		xminLogscale = yminLogscale = Double.MAX_VALUE;
		xmax = ymax = -Double.MAX_VALUE;
		xmin = ymin = Double.MAX_VALUE;
		if (isCleared) {
//			xmin = xmax = ymin = ymax = 0;
			return;
		}

		double[] xValues = xpoints;
		double[] yValues = ypoints;
		double shift = (isShifted() ? this.shift : 0);
		for (int i = 0; i < index; i++) {
			if (Double.isNaN(xValues[i]) || Double.isInfinite(xValues[i]) || Double.isInfinite(yValues[i])) {
				continue;
			}
			double xp = xValues[i];
			xmax = Math.max(xp, xmax);
			xmin = Math.min(xp, xmin);
			if (xp > 0) {
				xmaxLogscale = Math.max(xp, xmaxLogscale);
				xminLogscale = Math.min(xp, xminLogscale);
			}
			double yp = yValues[i] + shift;
			if (!Double.isNaN(yp)) {
				ymax = Math.max(yp, ymax);
				ymin = Math.min(yp, ymin);
				if (yp > 0) {
					ymaxLogscale = Math.max(yp, ymaxLogscale);
					yminLogscale = Math.min(yp, yminLogscale);
				}
			}
		}
	}

	/**
	 * Returns an array of valid points. 
	 * 
	 * A point is valid if the ypoint for that
	 * index is not Double.NaN.
	 *
	 * @return valid points
	 */
	private double[] getValidPoints(double[] pts) {
		pts = Arrays.copyOf(pts, pts.length);
		// eliminate NaN values, if any
		int nans = 0;
		for (int i = 0; i < index; i++) {
			if (nans > 0) {
				pts[i - nans] = pts[i];
			}
			if (Double.isNaN(ypoints[i])) {
				nans++;
			}
		}
		if (index - nans == pts.length) {
			return pts;
		}
		double[] temp = new double[index - nans];
		System.arraycopy(pts, 0, temp, 0, index - nans);
		return temp;
	}

	public static Dataset findDataSet(ArrayList<Dataset> datasets, Data newData) {
		int id = newData.getID();
		for (int i = 0, n = datasets.size(); i < n; i++) {
			Dataset ds = datasets.get(i); 
			if (ds.getID() == id) {
				return ds;
			}
		}
		return null;
	}

	protected static int getNaNCount(double[] pts, int index) {
		int nans = 0;
		for (int i = 0; i < index; i++) {
			if (Double.isNaN(pts[i])) {
				nans++;
			}
		}
		return nans;
	}

	/**
	 * ErrorBar for datapoints.
	 */
	class ErrorBar implements Drawable {
		double x, y, delx, dely; // the position and uncertainty of the data point
		int tick = 3;

		ErrorBar(double _x, double _y, double _delx, double _dely) {
			x = _x;
			y = _y;
			delx = _delx;
			dely = _dely;
		}

		/**
		 * Draws the error bars for a data point.
		 *
		 * @param panel
		 * @param g
		 */
		@Override
		public void draw(DrawingPanel panel, Graphics g) {
			// changed by D.Brown
			if (Double.isNaN(y)) {
				return;
			}
			int xpix = panel.xToPix(x);
			int xpix1 = panel.xToPix(x - delx);
			int xpix2 = panel.xToPix(x + delx);
			int ypix = panel.yToPix(y);
			int ypix1 = panel.yToPix(y - dely);
			int ypix2 = panel.yToPix(y + dely);
			g.setColor(errorBarColor);
			g.drawLine(xpix1, ypix, xpix2, ypix);
			g.drawLine(xpix, ypix1, xpix, ypix2);
			g.drawLine(xpix1, ypix - tick, xpix1, ypix + tick);
			g.drawLine(xpix2, ypix - tick, xpix2, ypix + tick);
			g.drawLine(xpix - tick, ypix1, xpix + tick, ypix1);
			g.drawLine(xpix - tick, ypix2, xpix + tick, ypix2);
		}

	}

	/**
	 * Returns the XML.ObjectLoader for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load Dataset data in an XMLControl.
	 */
	protected static class Loader extends XMLLoader {
		@Override
		public void saveObject(XMLControl control, Object obj) {
			Dataset data = (Dataset) obj;
			control.setValue("points", data.getPoints()); //$NON-NLS-1$
			control.setValue("index", data.index); //$NON-NLS-1$
			// control.setValue("x_points", data.getXPoints());
			// control.setValue("y_points", data.getYPoints());
			control.setValue("marker_shape", data.getMarkerShape()); //$NON-NLS-1$
			control.setValue("marker_size", data.getMarkerSize()); //$NON-NLS-1$
			control.setValue("sorted", data.isSorted()); //$NON-NLS-1$
			control.setValue("connected", data.isConnected()); //$NON-NLS-1$
			control.setValue("name", data.name); //$NON-NLS-1$
			control.setValue("x_name", data.xColumnName); //$NON-NLS-1$
			control.setValue("y_name", data.yColumnName); //$NON-NLS-1$
			control.setValue("x_description", data.xColumnDescription); //$NON-NLS-1$
			control.setValue("y_description", data.yColumnDescription); //$NON-NLS-1$
			control.setValue("line_color", data.lineColor); //$NON-NLS-1$
			control.setValue("fill_color", data.fillColor); //$NON-NLS-1$
			control.setValue("edge_color", data.edgeColor); //$NON-NLS-1$
			control.setValue("errorbar_color", data.errorBarColor); //$NON-NLS-1$
			control.setValue("datasetID", data.datasetID); //$NON-NLS-1$
			control.setValue("visible", toBoolArray(data.bsColVis)); //$NON-NLS-1$
		}

		@Override
		public Object createObject(XMLControl control) {
			Class<?> type = control.getObjectClass();
			// handle subclasses separately
			if (Dataset.class.isAssignableFrom(type) && !Dataset.class.equals(type)) {
				try {
					return type.newInstance();
				} catch (InstantiationException ex) {
				} catch (IllegalAccessException ex) {
				}
			}
			return new Dataset();
		}

		@Override
		public Object loadObject(XMLControl control, Object obj) {
			Dataset data = (Dataset) obj;
			double[][] points = (double[][]) control.getObject("points"); //$NON-NLS-1$
			int n;
			double[] xPoints = null, yPoints = null;
			if (points != null && (n = points.length) > 0 && points[0] != null) {
				xPoints = new double[n];
				yPoints = new double[n];
				for (int i = 0; i < n; i++) {
					xPoints[i] = points[i][0];
					yPoints[i] = points[i][1];
				}
			} else {
				// for backward compatibility
				xPoints = (double[]) control.getObject("x_points"); //$NON-NLS-1$
				yPoints = (double[]) control.getObject("y_points"); //$NON-NLS-1$
			}
			if (xPoints != null && yPoints != null) {
				data.clear();
				data.append(xPoints, yPoints);
			}
			data.index = control.getInt("index"); //$NON-NLS-1$
			if (control.getPropertyNamesRaw().contains("marker_shape")) { //$NON-NLS-1$
				data.setMarkerShape(control.getInt("marker_shape")); //$NON-NLS-1$
			}
			if (control.getPropertyNamesRaw().contains("marker_size")) { //$NON-NLS-1$
				data.setMarkerSize(control.getInt("marker_size")); //$NON-NLS-1$
			}
			data.setSorted(control.getBoolean("sorted")); //$NON-NLS-1$
			data.setConnected(control.getBoolean("connected")); //$NON-NLS-1$
			data.name = control.getString("name"); //$NON-NLS-1$
			data.xColumnName = control.getString("x_name"); //$NON-NLS-1$
			data.yColumnName = control.getString("y_name"); //$NON-NLS-1$
			data.xColumnDescription = control.getString("x_description"); //$NON-NLS-1$
			data.yColumnDescription = control.getString("y_description"); //$NON-NLS-1$
			Color color = (Color) control.getObject("line_color"); //$NON-NLS-1$
			if (color != null) {
				data.lineColor = color;
			}
			color = (Color) control.getObject("fill_color"); //$NON-NLS-1$
			if (color != null) {
				data.fillColor = color;
			}
			color = (Color) control.getObject("edge_color"); //$NON-NLS-1$
			if (color != null) {
				data.edgeColor = color;
			}
			color = (Color) control.getObject("errorbar_color"); //$NON-NLS-1$
			if (color != null) {
				data.errorBarColor = color;
			}
			data.setID(control.getInt("datasetID")); //$NON-NLS-1$
			boolean[] colVisible = (boolean[]) control.getObject("visible"); //$NON-NLS-1$
			if (colVisible != null) {
				for (int i = 0; i < colVisible.length; i++)
					data.bsColVis.set(i, colVisible[i]);
			}
			return obj;
		}

	}

	public static boolean[] toBoolArray(BitSet bs) {
		// DB should always be boolean[2]: x and y-columns
		boolean[] b = new boolean[] {false, false}; // false by default
		for (int i = bs.nextSetBit(0); i >= 0 && i < 2; i = bs.nextSetBit(i + 1))
			b[i] = true; // only BitSet elements are true
		return b;
	}

	/**
	 * Converts a table column in a table model to the appropriate table column.
	 * 
	 * <code>
	 * x   y  index  ret
	 * 
	 * best:
	 * 
	 * 1   1    0     0 (index when x)
	 * 1   0    0     0 (index when x)
	 * 1   1    1     1 (index when y)
	 * 0   1    1     1 (index when y)
	 * 0   1    0     1 (1 when !x)
	 * 1   0    1     0 (0 when !y)
	 * 
	 * equivalent
	 * 
	 * 0   1    0     1 (1 when !x)
	 * 0   1    1     1 (1 when !x)
	 * 1   0    0     0 (0 when !y)
	 * 1   0    1     0 (0 when !y)
	 * 1   1    0     0 (index)
	 * 1   1    1     1 (index)
	 * 
	 * same as original:
	 * 0   1    0     1 (1 when index==0 and !x)
	 * 1   0    1     0 (0 when index==1 and !y)
	 * 1   0    0     0 (index)
	 * 0   1    1     1 (index)
	 * 1   1    0     0 (index)
	 * 1   1    1     1 (index)
	 * </code>
	 * 
	 * @param visible     array of column visibilities
	 * @param columnIndex table column index to convert --- 0 or 1
	 * @return converted table column index
	 */
	public static int convertTableColumnIndex(BitSet visible, int columnIndex) {
		// simplest
		return (visible.get(columnIndex) ? columnIndex : 1 - columnIndex);
// original:
//	if (columnIndex == 0 && !visible.get(0)) {
//		columnIndex++;
//	} else if (columnIndex == 1 && !visible.get(1)) {
//		columnIndex--;
//	}
//	return columnIndex;

// equivalent:
//		if (columnIndex == 0 && !visible.get(0)) {
//			return 1;
//		} 
//		if (columnIndex == 1 && !visible.get(1)) {
//			return 0;
//		}
//		return columnIndex;
	}

	public static void loadDatasets(ArrayList<Dataset> datasets, Iterator<Dataset> it) {
		while (it.hasNext()) {
			Dataset newData = it.next();
			Dataset ds = findDataSet(datasets, newData);
			if (ds != null) {
				// convert the source to xml
				// and copy the data to the destination
				getLoader().loadObject(new XMLControlElement(newData), ds); 
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
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
