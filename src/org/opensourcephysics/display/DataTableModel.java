/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;
import javax.swing.table.TableModel;

/**
 * Interface DataTableModel
 */
public interface DataTableModel extends TableModel {
  /**
   * Sets the visibility of a column of a TableModel in the DataTable.
   * @param tableModel
   * @param columnIndex
   * @param b
   */
  public void setColumnVisible(TableModel tableModel, int columnIndex, boolean b);

  /**
   * Remove a TableModel object from the table model list.
   * @param tableModel
   */
  public void remove(TableModel tableModel);

  /**
   * Remove all TableModels from the table model list.
   */
  public void clear();

  /**
   * Add a TableModel object to the table model list.
   * @param tableModel
   */
  public void add(TableModel tableModel);

  /**
   * Sets the stride of a TableModel in the DataTable.
   * @param tableModel
   * @param stride
   */
  public void setStride(TableModel tableModel, int stride);

  /**
   * Sets the display row number flag.  Table displays row number.
   * @param rowNumberVisible
   */
  public void setRowNumberVisible(boolean rowNumberVisible);

  /**
   * Gets the display row number flag.
   *
   * @return visible flag
   */
  public boolean isRowNumberVisible();

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
