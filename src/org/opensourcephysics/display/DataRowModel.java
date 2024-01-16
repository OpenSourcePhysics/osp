/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;


/**
 * BH 2020.03.30 note:
 * 
 * DataRowTable is used in ThreeStateNuclearDecay, where it tracks the three
 * nuclear states over time.
 * 
 * The symptom in SwingJS was that it was taking an inordinant amount of time to
 * append to the table and to scroll it, and there was a huge amount of activity
 * just in adding a new row.
 * 
 * The problem turned out to be calls to DataRowTable.refreshTable(), which was
 * firing a full table rebuild -- basically a clean sweep of all rows and
 * columns -- every time a row was added.
 * 
 * The solution was to disambiguate the calls to refreshTable() as
 * refreshTable(String type), allowing a more nuanced approach. In addition,
 * that functionality, arising from DataRowModel, was moved into DataRowModel as
 * refreshModel(String type).
 * 
 * This solved the basic problem of too many, too full, updates.
 * 
 * The second problem was that in SwingJS we don't necessarily have to repaint a
 * cell if nothing has changed, particularly during scrolling. "Repainting"
 * actually amounts to rebuilding the HTML5 structure of the table, leading to
 * expensive restyling of the DOM.
 * 
 * To avoid this, SwingJS was modified to allow for a null return from
 * DataRowTable.getCellRenderer(row, column). The new method is
 * getCellRendererOrNull(int row, int column, boolean isScrolling), which is
 * called whenever JSTableUI does not absolutely have to recreate a cell from
 * scratch -- during scrolling particularly.
 * 
 * 
 * @author hansonr and others
 *
 */
public class DataRowModel extends AbstractTableModel {
	public static final String FINAL_UPDATE = "CDT.finalUpdate";
	ArrayList<Object> rowList = new ArrayList<Object>();
	ArrayList<String> colNames = new ArrayList<String>();
	boolean rowNumberVisible = true;
	int colCount = 0, maxRows = -1;
	int firstRowIndex = 0;
	int stride = 1;
	private String updateType;

	/**
	 * Constructor DataRowModel
	 */
	public DataRowModel() {
		colNames.add(0, "row"); // default for zero column //$NON-NLS-1$
	}

	/**
	 * Sets the stride between rows.
	 *
	 * @param tableModel
	 * @param stride
	 */
	public void setStride(int stride) {
		this.stride = stride;
	}

	/**
	 * Sets the maximum number of rows the data can hold
	 */
	public void setMaxPoints(int max) {
		maxRows = max;
		if ((maxRows <= 0) || (rowList.size() <= max)) {
			return;
		}
		// Reset the table to that size
		for (int j = 0, n = rowList.size() - max; j < n; j++) {
			rowList.remove(0);
		}
		colCount = 0;
		for (int j = 0, n = rowList.size(); j < n; j++) {
			Object r = rowList.get(j);
			if (!r.getClass().isArray()) {
				continue;
			}
			int length = 0;
			if (r instanceof double[]) {
				length = ((double[]) r).length;
			} else if (r instanceof byte[]) {
				length = ((byte[]) r).length;
			} else if (r instanceof int[]) {
				length = ((int[]) r).length;
			} else if (r instanceof String[]) {
				length = ((String[]) r).length;
			}
			colCount = Math.max(colCount, length);
		}
	}

	/**
	 * Clear the data
	 */
	public void clear() { // Paco added this method
		rowList.clear();
		colCount = 0;
	}

	/**
	 * Appends a row to this table.
	 *
	 * @param obj Object
	 * @throws IllegalArgumentException
	 */
	public synchronized void appendRow(Object obj) throws IllegalArgumentException {
		if (!obj.getClass().isArray()) {
			throw new IllegalArgumentException("A TableData row must be an array."); //$NON-NLS-1$
		}
		// make sure ultimate component class is acceptable
		Class<?> componentType = obj.getClass().getComponentType();
		if (componentType == Double.TYPE) { //$NON-NLS-1$
			appendDoubles((double[]) obj);
		} else if (componentType == Integer.TYPE) { //$NON-NLS-1$
			appendInts((int[]) obj);
		} else if (componentType == Byte.TYPE) { //$NON-NLS-1$
			appendBytes((byte[]) obj);
		} else if (componentType == String.class) { //$NON-NLS-1$
			appendStrings((String[]) obj);
		} else {
			Object[] row = (Object[]) obj;
			String[] strings = new String[row.length];
			for (int i = 0, n = row.length; i < n; i++) {
				strings[i] = row[i].toString();
			}
			appendStrings(strings);
		}
	}

	/**
	 * Appends a row of data.
	 *
	 * @param x double[]
	 */
	void appendDoubles(double[] x) {
		int n = getRowCount();
		double[] row;
		if (x == null) {
			return;
		}
		row = new double[x.length];
		System.arraycopy(x, 0, row, 0, x.length);
		if ((maxRows > 0) && (rowList.size() >= maxRows)) {
			rowList.remove(0); // Paco added this line
		}
		rowList.add(row);
		colCount = Math.max(colCount, row.length + 1);
		updateTable(n);
	}

	/**
	 * Appends a row of data.
	 *
	 * @param x double[]
	 */
	void appendInts(int[] x) {
		int n = getRowCount();
		int[] row;
		if (x == null) {
			return;
		}
		row = new int[x.length];
		System.arraycopy(x, 0, row, 0, x.length);
		if ((maxRows > 0) && (rowList.size() >= maxRows)) {
			rowList.remove(0); // Paco added this line
		}
		rowList.add(row);
		colCount = Math.max(colCount, row.length + 1);
		updateTable(n);
	}

	/**
	 * Appends a row of data.
	 *
	 * @param x double[]
	 */
	void appendBytes(byte[] x) {
		int n = getRowCount();
		byte[] row;
		if (x == null) {
			return;
		}
		row = new byte[x.length];
		System.arraycopy(x, 0, row, 0, x.length);
		if ((maxRows > 0) && (rowList.size() >= maxRows)) {
			rowList.remove(0); // Paco added this line
		}
		rowList.add(row);
		colCount = Math.max(colCount, row.length + 1);
		updateTable(n);
	}

	/**
	 * Appends a row of data.
	 *
	 * @param x double[]
	 */
	void appendStrings(String[] x) {
		int n = getRowCount();
		String[] row;
		if (x == null) {
			return;
		}
		row = new String[x.length];
		System.arraycopy(x, 0, row, 0, x.length);
		if ((maxRows > 0) && (rowList.size() >= maxRows)) {
			rowList.remove(0); // Paco added this line
		}
		colCount = Math.max(colCount, row.length + 1);
		rowList.add(row);
		updateTable(n);
	}

//	private int rowInserted;

	private void updateTable(int n) {
		pointCount++;
//		rowInserted = -1;
//		int n1 = getRowCount();
//		if (n != n1) {
//			rowInserted = n;
//		}
	}

	/**
	 * Sets the display row number flag. Table displays row number.
	 *
	 * @param vis <code>true<\code> if table display row number
	 * @return true if table display changed
	 */
	public boolean setRowNumberVisible(boolean vis) {
		if (rowNumberVisible == vis) {
			return false;
		}
		rowNumberVisible = vis;
		return true;
	}

	/**
	 * Sets the column names in this table.
	 *
	 * @param column the column index
	 * @param name
	 * @return true if name changed or added
	 */
	public boolean setColumnNames(int column, String name) {
		name = TeXParser.parseTeX(name);
		if ((colNames == null)
				|| (column < colNames.size()) && colNames.get(column) != null && colNames.get(column).equals(name)) { // W.
																														// Christian
																														// added
																														// null
																														// check
			return false;
		}
		while (column >= colNames.size()) {
			colNames.add("" + (char) ('A' + column)); //$NON-NLS-1$
		}
		colNames.set(column, name);
		return true;
	}

	/**
	 * Sets the first row's index.
	 *
	 * @param index
	 */
	public void setFirstRowIndex(int index) {
		firstRowIndex = index;
	}

	/**
	 * Gets the number of columns being shown.
	 *
	 * @return the column count
	 */
	@Override
	public int getColumnCount() {
		int offset = rowNumberVisible ? 0 : 1;
		if (getRowCount() == 0) {
			return (colNames == null) ? 0 : colNames.size() - offset;
			// return 0;
		}
		int count = (rowNumberVisible) ? colCount : colCount - 1;
		return count;
	}

	/**
	 * Gets the name of the specified column.
	 *
	 * @param column the column index
	 * @return the column name
	 */
	@Override
	public String getColumnName(int column) {
		if ((column == 0) && rowNumberVisible) {
			return colNames.get(0);
		}
		if (!rowNumberVisible) {
			column++;
		}
		if (column < colNames.size()) {
			return colNames.get(column);
		}
		return "" + (char) ('A' + column - 1); //$NON-NLS-1$
	}

	/**
	 * Gets the number of rows.
	 *
	 * @return the row count
	 */
	@Override
	public int getRowCount() {
		return (rowList.size() + stride - 1) / stride;
		// return rowList.size();
	}

	/**
	 * Gets the value at the given cell.
	 *
	 * @param row    the row index
	 * @param column the column index
	 * @return the value
	 */
	@Override
	public Object getValueAt(int row, int column) {
		row = row * stride;
		if ((column == 0) && rowNumberVisible) {
			return Integer.valueOf(row + firstRowIndex);
		}
		if (!rowNumberVisible) {
			column++;
		}
		if (row >= rowList.size()) {
			return ""; //$NON-NLS-1$
		}
		Object r = rowList.get(row);
		if (!r.getClass().isArray()) {
			return ""; //$NON-NLS-1$
		}
		if (r instanceof double[]) {
			double[] array = (double[]) r;
			if (column > array.length) {
				return ""; //$NON-NLS-1$
			}
			return new Double(array[column - 1]);
		}
		if (r instanceof byte[]) {
			byte[] array = (byte[]) r;
			if (column > array.length) {
				return ""; //$NON-NLS-1$
			}
			return Byte.valueOf(array[column - 1]);
		}
		if (r instanceof int[]) {
			int[] array = (int[]) r;
			if (column > array.length) {
				return ""; //$NON-NLS-1$
			}
			return Integer.valueOf(array[column - 1]);
		}
		if (r instanceof String[]) {
			String[] array = (String[]) r;
			if (column > array.length) {
				return ""; //$NON-NLS-1$
			}
			return array[column - 1];
		}
		return ""; //$NON-NLS-1$
	}

	private int lastRowAppended = -1;
	private int pointCount;

	public boolean mustPaint(int row, int column) {
		boolean b = lastRowAppended == -2 ? true : updateType != "appendRow" || row == lastRowAppended;
		////System.out.println("DRM mustPaint " + row + "  "+ column + " " + updateType + " " + lastRowAppended + " " + b);
		return b;
	}

	/**
	 * Called from DataRowTable.refreshTable(from), this method handles all
	 * communication directing repaints via the JTable's UI class. There is no need
	 * to create events ourselves, unless those are intended for in-house use.
	 * 
	 * @param table
	 * 
	 * @param type
	 */
	void refreshModel(DataRowTable table, String type) {
		if (type == null)
			type = FINAL_UPDATE;
		//System.out.println("DataRowModel.refreshModel " + type + " " + lastRowAppended);
		switch (type) {
		default:
			updateType = "?";
			
			//System.out.println("DataRowModel.refreshModel " + type + " not processed");
			return;
		case "columnFormat":
		case "setStride":
		case "setRowNumberVis":
			lastRowAppended = -2;
			updateType = type;
			return;
		case "appendRow":
			if (lastRowAppended == -2)
				fireUpdate();
			int n = getRowCount() - 1;
			if (n == lastRowAppended) {
				if (pointCount >= maxRows && (pointCount + stride) % stride == 0) {
					updateType = "maxRows";
				} else {
					updateType = null;
				}
				return;
			}
			lastRowAppended = n;
			updateType = "rowAppended";
			return;
		case "clearData":
			pointCount = 0;
			lastRowAppended = -1;
			updateType = "dataCleared";
			fireUpdate();
			break;
		case "setColumnName":
		case "setColumnNames":
			updateType = "columnNamed";
			return;
		case "_pause":
			// must add to application's pause button.
			updateType = "paused";
			return;
		case "TF.visible":
			updateType = "visible";
			fireUpdate();
			break;
		case FINAL_UPDATE:
			fireUpdate();
		}

//  if(refreshDelay>0) {
//    refreshTimer.start();
//  } else {
//    if(SwingUtilities.isEventDispatchThread()) {
//      doRefreshTable.run();
//    } else {
//      SwingUtilities.invokeLater(doRefreshTable);
//    }
//  }

	}

	private void fireUpdate() {
		String update = updateType;
		if (update == null)
			return;
		updateType = null;
		//System.out.println("fireUpdate " + update);
		switch (update) {
		case "rowAppended":
			fireTableRowsInserted(lastRowAppended, lastRowAppended);
			//table.scrollToEnd();
			return;
		case "columnNamed":
			lastRowAppended = -2;
			return;
		case "?":
		case "visible":
		case "maxRows":
		case "paused":
		case "formatColumn":
		case "setStride":
		case "setRowNumberVis":
		case "dataCleared":
		case "TF.visible":
			fireTableStructureChanged();
			return;
		default:
			return;
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
