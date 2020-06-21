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
import java.util.BitSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLLoader;
import org.opensourcephysics.display.DataTable.OSPTableModel;

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
@SuppressWarnings("serial")
public class Dataset extends OSPTableModel implements Measurable, LogMeasurable, Data {
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
	
	protected double[] xpoints;
	// array of x points

	protected double[] ypoints;
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

	private int initialSize;
	// the initial size of the points array

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

	private BitSet colVisible = new BitSet();
	// column visibilities for table view

	protected boolean visible = true;
	// visible in drawing panel; only visible dataset affect autoscale

	private int stride = 1;
	// stride for table view

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
		initialSize = 10;
		xColumnName = "x"; //$NON-NLS-1$
		yColumnName = "y"; //$NON-NLS-1$
		generalPath = new GeneralPath();
		index = 0;
		colVisible.set(0, 2);
		clear();
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
		
		//OSPLog.debug("getXPointsRaw " + ++nRaw);
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
		//OSPLog.debug("getYPointsRaw " + ++nRaw);
			return ypoints;
	}

	public double getY(int i) {
		return ypoints[i];
	}

	/**
	 * Gets an array of valid xpoints. A point is valid if the ypoint for that index
	 * is not Double.NaN.
	 *
	 * @return valid xpoints[]
	 */
	public double[] getValidXPoints() {
		return getValidPoints(xpoints, index);
	}

	/**
	 * Gets an array of valid ypoints. A point is valid if the ypoint for that index
	 * is not Double.NaN.
	 *
	 * @return valid ypoints[]
	 */
	public double[] getValidYPoints() {
		return getValidPoints(ypoints, index);
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
		return colVisible.cardinality();
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
		return (index + stride - 1) / stride;
	}

	/**
	 * Gets the name of the colummn for rendering in a JTable
	 *
	 * @param columnIndex
	 * @return the name
	 */
	@Override
	public String getColumnName(int columnIndex) {
		return (convertTableColumnIndex(colVisible, columnIndex) == 0 ? xColumnName : yColumnName);
	}

	/**
	 * Gets an x or y value for rendering in a JTable.
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @return the datum
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		columnIndex = convertTableColumnIndex(colVisible, columnIndex);
		rowIndex = rowIndex * stride;
		double[] xValues = xpoints;
		// conversionFactor added by D Brown Dec 2010
		if (columnIndex == 0) {
			return Double.valueOf(xValues[rowIndex]);
		}
		double y = ypoints[rowIndex];
		return (Double.isNaN(y) ? null : Double.valueOf(y + shift));
	}

	/**
	 * Gets the type of object for JTable entry.
	 *
	 * @param columnIndex
	 * @return the class
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return Double.class;
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
		myShape = null;
		if (index >= xpoints.length) {
			increaseCapacity(xpoints.length * 2);
		}
		xpoints[index] = x;
		ypoints[index] = y;
		// generalPath.append(new Rectangle2D.Double(x, y, 0, 0), true);
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
		index++;
		// move the new datum if x is less than the last value.
		if (sorted && (index > 1) && (x < xpoints[index - 2])) {
			moveDatum(index - 1);
			// the new datum is out of place so move it
			recalculatePath();
		}
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
		append(xpoints, ypoints);
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
	 * @param _xpoints
	 * @param _ypoints
	 * @param len
	 */
	public void append(double[] _xpoints, double[] _ypoints, int len) {

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
			String s;
			while ((s = reader.readLine()) != null) {
				s = s.trim();
				if ((s.length() == 0) || (s.charAt(0) == '#')) { // ignore lines beginning with #
					continue;
				}
				StringTokenizer st = new StringTokenizer(s, "\t"); //$NON-NLS-1$
				switch (st.countTokens()) {
				case 0:
					continue;
				case 2:
					append(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
					break;
				default:
					throw new IOException();
				}
			}
			reader.close();
		} catch (java.io.FileNotFoundException fnfe) {
			System.err.println("File " + inputFile + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (java.io.IOException ioe) {
			System.err.println("Error reading file " + inputFile); //$NON-NLS-1$
		} catch (NumberFormatException nfe) {
			System.err.println("Error reading file " + inputFile); //$NON-NLS-1$
		}
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

	private static int testCount = 0;

	/**
	 * Draw this Dataset in the drawing panel.
	 *
	 * @param drawingPanel
	 * @param g
	 */
	@Override
	public void draw(DrawingPanel drawingPanel, Graphics g) {
		if (!visible) {
			return;
		}
//		OSPLog.debug(Performance.timeCheckStr("Dataset.draw " + ++testCount + " " + (/** @j2sNative drawingPanel.ui.id || */null), Performance.TIME_MARK));
		if (!drawingPanel.getPixelTransform().equals(pixelTransform)) {
			myShape = null;
			pixelTransform = drawingPanel.getPixelTransform();
		}
		try {
			Graphics2D g2 = (Graphics2D) g;
			if (markerShape != NO_MARKER) {
				drawScatterPlot(drawingPanel, g2);
			}
			if (connected) {
				drawLinePlot(drawingPanel, g2);
			}
		} catch (Exception ex) {
		} // abort drawing if we have bad data
	}

	/**
	 * Clear all data from this Dataset.
	 */
	public void clear() {
		index = 0;
		xpoints = new double[initialSize];
		ypoints = new double[initialSize];
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
		if (index == 0) {
			return "No data in dataset."; //$NON-NLS-1$
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
		return b.toString();
	}

	/**
	 * Counts the number of columns visible
	 *
	 * @param visible array of column visibilities
	 * @return number of visible columns
	 * @deprecated
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
	 * Converts a table column in a table model to the appropriate table column. 
	 * 
	 * <code>
	 * x   y  index  ret
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
	 * @param columnIndex table column index to convert
	 * @return converted table column index
	 */
	public static int convertTableColumnIndex(BitSet visible, int columnIndex) {
		return (!visible.get(0) ? 1 : visible.get(1) ? columnIndex : 0);
//		if (columnIndex == 0 && !visible.get(0)) {
//			columnIndex++;
//		} else if (columnIndex == 1 && !visible.get(1)) {
//			columnIndex--;
//		}
//		return columnIndex;
	}

	/**
	 * Sets the visibility of the x column of this Dataset in a table view.
	 * 
	 * @param b new visibility
	 */
	public void setXColumnVisible(boolean b) {
		colVisible.set(0, b);
	}

	/**
	 * Sets the visibility of the y column of this Dataset in a table view.
	 * 
	 * @param b new visibility
	 */
	public void setYColumnVisible(boolean b) {
		colVisible.set(1, b);
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
	 * 
	 * @param _stride the stride
	 */
	public void setStride(int _stride) {
		stride = _stride;
	}

	/**
	 * Gets the visibility of the x column of this Dataset in a table view.
	 * 
	 * @return the x column visibility
	 */
	public boolean isXColumnVisible() {
		return colVisible.get(0);
	}

	/**
	 * Gets the visibility of the y column of this Dataset in a table view.
	 * 
	 * @return the x column visibility
	 */
	public boolean isYColumnVisible() {
		return colVisible.get(1);
	}

	/**
	 * Perform an insertion sort of the data set. Since data will be partially
	 * sorted this should be fast. Added by W. Christian.
	 */
	protected void insertionSort() {
		boolean dataChanged = false;
		if (index < 2) {
			return;
			// need at least two points to sort.
		}
		for (int i = 1; i < index; i++) {
			if (xpoints[i] < xpoints[i - 1]) {
				// is the i-th datum smaller?
				dataChanged = true;
				moveDatum(i);
			}
		}
		if (dataChanged) {
			recalculatePath();
		}
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
	 * Move an out-of-place datum into its correct position.
	 *
	 * @param loc the datum
	 */
	protected void moveDatum(int loc) {
		if (loc < 1) {
			return;
			// zero-th point cannot be out-of-place
		}
		double x = xpoints[loc];
		// save the old values
		double y = ypoints[loc];
		for (int i = 0; i < index; i++) {
			if (xpoints[i] > x) {
				// find the insertion point
				System.arraycopy(xpoints, i, xpoints, i + 1, loc - i);
				xpoints[i] = x;
				System.arraycopy(ypoints, i, ypoints, i + 1, loc - i);
				ypoints[i] = y;
				return;
			}
		}
	}

	/**
	 * Draw the lines connecting the data points.
	 *
	 * @param drawingPanel
	 * @param g2
	 */
	protected void drawLinePlot(DrawingPanel drawingPanel, Graphics2D g2) {
		// check that at least one ypoints element is a number
		boolean noNumbers = true;
		for (int i = 0; i < index; i++) {
			noNumbers = Double.isNaN(ypoints[i]);
			if (!noNumbers) {
				break;
			}
		}
		if (noNumbers) {
			return;
		}
		g2.setColor(lineColor);
		if (myShape == null)
			myShape = drawingPanel.transformPath(generalPath);
		g2.draw(myShape);
	}

	private AffineTransform trD = new AffineTransform();

	/**
	 * Fills the line connecting the data points.
	 *
	 * @param drawingPanel
	 * @param g2
	 */
	protected void drawFilledPlot(DrawingPanel drawingPanel, Graphics2D g2) {
		// check that at least one ypoints element is a number
		boolean noNumbers = true;
		for (int i = 0; i < index; i++) {
			noNumbers = Double.isNaN(ypoints[i]);
			if (!noNumbers) {
				break;
			}
		}
		if (noNumbers) {
			return;
		}
		if (myShape == null)
			myShape = drawingPanel.transformPath(generalPath);
		g2.setColor(fillColor);
		g2.fill(myShape);
		g2.setColor(edgeColor);
		g2.draw(myShape);
	}

	/**
	 * Draw the markers at the data points.
	 *
	 * @param drawingPanel
	 * @param g2
	 */
	protected void drawScatterPlot(DrawingPanel drawingPanel, Graphics2D g2) {
		if (markerShape == AREA) {
			this.drawFilledPlot(drawingPanel, g2);
			return;
		}
		double xp = 0;
		double yp = 0;
		Shape shape = null;
		int size = markerSize * 2 + 1;
		g2 = (Graphics2D) g2.create();
		// Shape clipShape = g2.getClip();
		// increase the clip so as to include the entire marker
		g2.setClip(drawingPanel.leftGutter - markerSize - 1, drawingPanel.topGutter - markerSize - 1,
				drawingPanel.getWidth() - drawingPanel.leftGutter - drawingPanel.rightGutter + 2 + 2 * markerSize,
				drawingPanel.getHeight() - drawingPanel.bottomGutter - drawingPanel.topGutter + 2 + 2 * markerSize);
		Rectangle viewRect = drawingPanel.getViewRect();
		if (viewRect != null) { // decrease the clip if we are in a scroll pane
			g2.clipRect(viewRect.x, viewRect.y, viewRect.x + viewRect.width, viewRect.y + viewRect.height);
		}
//		double[] tempX = getXPoints();
//		double[] tempY = getYPoints();
		for (int i = 0; i < index; i++) {
			double x = xpoints[i];
			double y = getY(i);
			if (Double.isNaN(y)) {
				continue;
			}
			if (drawingPanel.isLogScaleX() && (x <= 0)) {
				continue;
			}
			if (drawingPanel.isLogScaleY() && (y <= 0)) {
				continue;
			}
			xp = drawingPanel.xToPix(x);
			yp = drawingPanel.yToPix(y);
			switch (markerShape) {
			case BAR: // draw a bar graph.
				double bottom = Math.min(drawingPanel.yToPix(0), drawingPanel.yToPix(drawingPanel.getYMin()));
				double barHeight = bottom - yp;
				if (barHeight > 0) {
					shape = new Rectangle2D.Double(xp - markerSize, yp, size, barHeight);
				} else {
					shape = new Rectangle2D.Double(xp - markerSize, bottom, size, -barHeight);
				}
				g2.setColor(fillColor);
				g2.fill(shape);
				if (edgeColor != fillColor) {
					g2.setColor(edgeColor);
					g2.draw(shape);
				}
				break;
			case POST:
				bottom = Math.min(drawingPanel.yToPix(0), drawingPanel.yToPix(drawingPanel.getYMin()));
				shape = new Rectangle2D.Double(xp - markerSize, yp - markerSize, size, size);
				g2.setColor(edgeColor);
				g2.drawLine((int) xp, (int) yp, (int) xp, (int) bottom);
				g2.setColor(fillColor);
				g2.fill(shape);
				if (edgeColor != fillColor) {
					g2.setColor(edgeColor);
					g2.draw(shape);
				}
				break;
			case SQUARE:
				shape = new Rectangle2D.Double(xp - markerSize, yp - markerSize, size, size);
				g2.setColor(fillColor);
				g2.fill(shape);
				if (edgeColor != fillColor) {
					g2.setColor(edgeColor);
					g2.draw(shape);
				}
				break;
			case CIRCLE:
				shape = new Ellipse2D.Double(xp - markerSize, yp - markerSize, size, size);
				g2.setColor(fillColor);
				g2.fill(shape);
				if (edgeColor != fillColor) {
					g2.setColor(edgeColor);
					g2.draw(shape);
				}
				break;
			case PIXEL:
				shape = new Rectangle2D.Double(xp, yp, 1, 1); // this produces a one pixel shape
				g2.setColor(edgeColor);
				g2.draw(shape);
				// draw and center the point
				break;
			case CUSTOM:
				Shape temp = getTranslateInstance(xp, yp).createTransformedShape(customMarker);
				g2.setColor(fillColor);
				g2.fill(temp);
				if (edgeColor != fillColor) {
					g2.setColor(edgeColor);
					g2.draw(temp);
				}
				break;
			default:
				shape = new Rectangle2D.Double(xp - markerSize, yp - markerSize, size, size);
				g2.setColor(fillColor);
				g2.fill(shape);
				if (edgeColor != fillColor) {
					g2.setColor(edgeColor);
					g2.draw(shape);
				}
				break;
			}
		}
		Iterator<ErrorBar> it = errorBars.iterator();
		while (it.hasNext()) { // copy only the obejcts of the correct type
			(it.next()).draw(drawingPanel, g2);
		}
		g2.dispose();
		// BH 2020.02.26 can't do this in JavaScript
		// g2.setClip(clipShape); // restore the original clipping
	}

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
		if (isCleared) {
			xmin = xmax = ymin = ymax = 0;
			return;
		}
		xmax = ymax = -Double.MAX_VALUE;
		xmin = ymin = Double.MAX_VALUE;

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
	 * Returns an array of valid points. A point is valid if the ypoint for that
	 * index is not Double.NaN.
	 *
	 * @return valid points
	 */
	private double[] getValidPoints(double[] pts, int len) {
		// eliminate NaN values, if any
		int nans = 0;
		for (int i = 0; i < pts.length; i++) {
			if (nans > 0) {
				pts[i - nans] = pts[i];
			}
			if (Double.isNaN(ypoints[i])) {
				nans++;
			}
		}
		if (nans == 0) {
			return pts;
		}
		double[] temp = new double[index - nans];
		System.arraycopy(pts, 0, temp, 0, index - nans);
		return temp;
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
			control.setValue("visible", toBoolArray(data.colVisible)); //$NON-NLS-1$
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
			if ((points != null) && (points.length > 0) && (points[0] != null)) {
				data.clear();
				for (int i = 0; i < points.length; i++) {
					data.append(points[i][0], points[i][1]);
				}
			}
			// for backward compatibility
			double[] xPoints = (double[]) control.getObject("x_points"); //$NON-NLS-1$
			double[] yPoints = (double[]) control.getObject("y_points"); //$NON-NLS-1$
			if ((xPoints != null) && (yPoints != null)) {
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
					data.colVisible.set(i, colVisible[i]);
			}
			return obj;
		}

	}

	public static boolean[] toBoolArray(BitSet bs) {
		boolean[] b = new boolean[bs.length()];
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
			b[i] = true;
		return b;
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
