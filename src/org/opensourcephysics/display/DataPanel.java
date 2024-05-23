/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class DataPanel extends JPanel {
  public DataRowTable dataRowTable = new DataRowTable();
  JScrollPane scrollPane = new JScrollPane(dataRowTable);

  /**
   * Constructor DataRowPanel
   */
  public DataPanel() {
    setLayout(new BorderLayout());
    add(scrollPane, BorderLayout.CENTER);
  }
  

  
  
  @Override
public void paintComponent(Graphics g) {
	  super.paintComponent(g);
  }
  /**
   * Sets the font for this component.
   *
   * @param font the desired <code>Font</code> for this component
   * @see java.awt.Component#getFont
   */
  @Override
public void setFont(Font font){
	  super.setFont(font);
	  if(dataRowTable!=null)dataRowTable.setFont(font);
  }
  
  /**
   * Sets the foreground color of this component.  It is up to the
   * look and feel to honor this property, some may choose to ignore
   * it.
   *
   * @param fg  the desired foreground <code>Color</code> 
   * @see java.awt.Component#getForeground
   */
  @Override
public void setForeground(Color color){
	  super.setForeground(color);
	  if(dataRowTable!=null)dataRowTable.setForeground(color);
  }

  /**
   * Refresh the data in the tables.
   */
  public void refreshTable(String from) {
    dataRowTable.refreshTable(from);
  }

  /**
   * Gets the Table.  Used by EJS to access the table.
   * @return
   */
  public java.awt.Component getVisual() {
    return dataRowTable;
  }

  /**
   *  Sets the given column name in this table.
   *
   * @param  column  the index
   * @param  name
   */
  public void setColumnNames(int column, String name) {
    if(dataRowTable.rowModel.setColumnNames(column, name)) { // refresh if the table changed
      refreshTable("setColumnName");
    }
  }

  /**
   * Sets all column names in this table.
   *
   * @param names
   */
  public void setColumnNames(String[] names) {
    boolean changed = false;
    for(int i = 0, n = names.length; i<n; i++) {
      if(dataRowTable.rowModel.setColumnNames(i, names[i])) {
        changed = true;
      }
    }
    if(changed) {
      refreshTable("setColumnNames");
    }
  }

  /**
   *  Sets the display row number flag. Table displays row number.
   *
   * @param  vis  <code>true<\code> if table display row number
   */
  public void setRowNumberVisible(boolean vis) {
    if(dataRowTable.rowModel.setRowNumberVisible(vis)) { // refresh if the table changed
      refreshTable("setRowNumberVis " + vis);
    }
  }

  /**
   * Sets the first row's index.
   *
   * @param index
   */
  public void setFirstRowIndex(int index) {
    if(dataRowTable.rowModel.firstRowIndex!=index) { // refresh if the table changed
      dataRowTable.rowModel.firstRowIndex = index;
      refreshTable("setFirstRowIndex " + index);
    }
  }

  /**
   *  Sets the delay time for table refresh timer.
   *
   * @param  delay  the delay in millisecond
   */
  public void setRefreshDelay(int delay) {
    dataRowTable.setRefreshDelay(delay);
  }

  /**
   * Appends a two dimensional array to this table.
   *
   * @param obj Object
   * @throws IllegalArgumentException
   */
  public synchronized void appendArray(Object obj) throws IllegalArgumentException {
    if(!obj.getClass().isArray()) {
      throw new IllegalArgumentException(""); //$NON-NLS-1$
    }
    // make sure ultimate component class is acceptable
    Class<?> componentType = obj.getClass().getComponentType();
    while(componentType.isArray()) {
      componentType = componentType.getComponentType();
    }
    if(componentType == Double.TYPE) {      //$NON-NLS-1$
      double[][] array = (double[][]) obj;
      double[] row = new double[array.length];
      for(int i = 0, n = array[0].length; i<n; i++) {
        for(int j = 0, m = row.length; j<m; j++) {
          row[j] = array[j][i];
        }
        appendRow(row);
      }
    } else if(componentType == Integer.TYPE) {  //$NON-NLS-1$
      int[][] array = (int[][]) obj;
      int[] row = new int[array.length];
      for(int i = 0, n = array[0].length; i<n; i++) {
        for(int j = 0, m = row.length; j<m; j++) {
          row[j] = array[j][i];
        }
        appendRow(row);
      }
    } else if(componentType == Byte.TYPE) { //$NON-NLS-1$
      byte[][] array = (byte[][]) obj;
      byte[] row = new byte[array.length];
      for(int i = 0, n = array[0].length; i<n; i++) {
        for(int j = 0, m = row.length; j<m; j++) {
          row[j] = array[j][i];
        }
        appendRow(row);
      }
    } else {
      Object[][] array = (Object[][]) obj;
      Object[] row = new Object[array.length];
      for(int i = 0, n = array[0].length; i<n; i++) {
        for(int j = 0, m = row.length; j<m; j++) {
          row[j] = array[j][i];
        }
        appendRow(row);
      }
    }
  }

  /**
   * Appends a row of data with the given values to the table.
   * @param x double[]
   */
  public synchronized void appendRow(double[] x) {
    dataRowTable.rowModel.appendDoubles(x);
    if(isShowing()) {
      dataRowTable.refreshTable("appendRow");
    }
  }

  /**
   * Appends a row of data with the given values to the table.
   * @param x double[]
   */
  public synchronized void appendRow(int[] x) {
    dataRowTable.rowModel.appendInts(x);
    if(isShowing()) {
      dataRowTable.refreshTable("appendRow");
    }
  }

  /**
   * Appends a row of data with the given values to the table.
   * @param x double[]
   */
  public synchronized void appendRow(Object[] x) {
    dataRowTable.rowModel.appendRow(x);
    if(isShowing()) {
      dataRowTable.refreshTable("appendRow");
    }
  }

  /**
   * Appends a row of data with the given values to the table.
   * @param x double[]
   */
  public synchronized void appendRow(byte[] x) {
    dataRowTable.rowModel.appendBytes(x);
    if(isShowing()) {
      dataRowTable.refreshTable("appendRow");
    }
  }

  /**
   * True if row number numbers are visible.
   * @return
   */
  public boolean isRowNumberVisible() {
    return dataRowTable.rowModel.rowNumberVisible;
  }

  /**
   * Gets the number of columns currently shown.  The row number column is included in the counting if it is visible.
   *
   * @return the column count
   */
  public int getColumnCount() {
    return dataRowTable.rowModel.getColumnCount();
  }

  /**
   * Gets the number of rows currently being shown.
   *
   * @return the row count
   */
  public int getRowCount() {
    return dataRowTable.rowModel.getRowCount();
  }

  /**
   * Gets the total number of rows in the table.
   *
   * @return the row count
   */
  public int getTotalRowCount() {
    return dataRowTable.rowModel.rowList.size();
  }

  /**
   * Gets the number of rows shown.
   *
   * @return the stride
   */
  public int getStride() {
    return dataRowTable.rowModel.stride;
  }

  /**
   *  Sets the format for displaying decimals.
   *
   * @param  column  the index
   * @param  format
   */
  public void setColumnFormat(int column, String format) {
    dataRowTable.setColumnFormat(column, format);
  }

  /**
   * Clears any previous format
   */
  public void clearFormats() {
	dataRowTable.clearFormats();
  }
  
  /**
   *  Sets the default format pattern for displaying decimals.
   *
   * @param  pattern
   */
  public void setNumericFormat(String pattern) {
    dataRowTable.setNumericFormat(pattern);
  }

  /**
   *  Sets the maximum number of points to display
   *
   * @param  max
   */
  public void setMaxPoints(int max) {
    dataRowTable.rowModel.setMaxPoints(max);
  }

  /**
   * Shows or hides this TableFrame depending on the value of parameter
   * <code>vis</code>.
   * @param vis  if <code>true</code>, shows this component;
   * otherwise, hides this component
   */
  @Override
public void setVisible(boolean vis) {
    if(vis) {
      dataRowTable.refreshTable(" vis " + vis); // make sure the table shows the current values
    }
    super.setVisible(vis);
  }

  /**
   *  Sets the stride between successive rows.
   *
   * @param  tableModel
   * @param  stride
   */
  public void setStride(int stride) {
    dataRowTable.setStride(stride);
  }

  /**
   * Clears data from this table.  Column names and format patterns are not affected.
   */
  public void clearData() {
    dataRowTable.clearData();
  }

  /**
   * Clears data, column names and format patterns.
   */
  public void clear() {
    dataRowTable.clear();
  }
  
  /**
   * Sets the table's auto resize mode when the table is resized.
   *
   * @param   mode One of 5 legal values:
   *                   AUTO_RESIZE_OFF,
   *                   AUTO_RESIZE_NEXT_COLUMN,
   *                   AUTO_RESIZE_SUBSEQUENT_COLUMNS,
   *                   AUTO_RESIZE_LAST_COLUMN,
   *                   AUTO_RESIZE_ALL_COLUMNS
   */
  public void setAutoResizeMode(int mode) {
    dataRowTable.setAutoResizeMode(mode); // make sure the table shows the current values
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
