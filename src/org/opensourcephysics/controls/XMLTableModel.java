/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.table.AbstractTableModel;

import org.opensourcephysics.tools.ArrayInspector;

/**
 * A table model for an XMLTable.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class XMLTableModel extends AbstractTableModel {
  // instance fields
  XMLControl control;
  boolean editable = true;
  Collection<String> uneditablePropNames = new HashSet<String>();

  /**
   * Constructor.
   *
   * @param control an xml control
   */
  public XMLTableModel(XMLControl control) {
    this.control = control;
  }

  /**
   * Gets the number of columns.
   *
   * @return the column count
   */
  @Override
public int getColumnCount() {
	  if (control == null)
		  return 0;
    return 2;
  }

  /**
   * Gets the name of the specified column.
   *
   * @param column the column index
   * @return the column name
   */
  @Override
public String getColumnName(int column) {
    return (column==0) ? ControlsRes.XML_NAME : ControlsRes.XML_VALUE;
  }

  /**
   * Gets the number of rows.
   *
   * @return the row count
   */
  @Override
public int getRowCount() {
	  if (control == null)
		  return 0;
    return control.getPropsRaw().size();
  }

  /**
   * Gets the value at the given cell.
   * Column 0 = property name
   * Column 1 = property content (String for int, double, boolean, string types,
   * XMLControl for object type, XMLProperty for array, collection types)
   *
   * @param row the row index
   * @param column the column index
   * @return the value
   */
  @Override
public Object getValueAt(int row, int column) {
    try {
      XMLProperty val = control.getPropsRaw().get(row);
      Object content = val.getPropertyContent().get(0);
      if(content.toString().indexOf("![CDATA[")>-1) { //$NON-NLS-1$
        content = control.getString(val.getPropertyName());
      }
      return (column==0) ? val.getPropertyName() : content;
    } catch(Exception ex) {
      return null;
    }
  }

  /**
   * Determines whether the given cell is editable.
   *
   * @param row the row index
   * @param col the column index
   * @return true if editable
   */
  @Override
public boolean isCellEditable(int row, int col) {
    // not editable if column 0
    if(col==0) {
      return false;
    }
    // not editable if property name is in uneditable collection
    String propName = (String) getValueAt(row, 0);
    if(uneditablePropNames.contains(propName)) {
      return false;
    }
    // not editable if value is uninspectable array or collection
    Object value = getValueAt(row, col);
    if(value instanceof XMLControl) {
      return true;
    } else if(value instanceof XMLProperty) {
      XMLProperty prop = (XMLProperty) value;
      XMLProperty parent = prop.getParentProperty();
      switch (parent.getPropertyType()) {
      case XMLProperty.TYPE_ARRAY:
    	  return ArrayInspector.canInspect(parent);
      case XMLProperty.TYPE_COLLECTION:
    	  return true;
      default:
    	  return false;
      }
    }
    // otherwise editable
    return true;
  }

	/**
	 * Sets the value at the given cell. This method only sets values for int,
	 * double, boolean and string types.
	 *
	 * @param value the value
	 * @param row   the row index
	 * @param col   the column index
	 */
	@Override
	public void setValueAt(Object value, int row, int col) {
		if (value == null) {
			return;
		}
		boolean changed = false;
		if (value instanceof String) {
			String s = (String) value;
			// determine class type at row and column
			XMLProperty prop = (XMLProperty) control.getPropsRaw().get(row);
			changed = !s.equals(control.getString(prop.getPropertyName()));
			switch (prop.getPropertyType()) {
			case XMLProperty.TYPE_STRING: //$NON-NLS-1$
				control.setValue(prop.getPropertyName(), s);
				break;
			case XMLProperty.TYPE_INT:
				try {
					control.setValue(prop.getPropertyName(), Integer.parseInt(s));
				} catch (NumberFormatException ex) {

					/** empty block */
				}
				break;
			case XMLProperty.TYPE_DOUBLE: //$NON-NLS-1$
				try {
					control.setValue(prop.getPropertyName(), Double.parseDouble(s));
				} catch (NumberFormatException ex) {

					/** empty block */
				}
				break;
			case XMLProperty.TYPE_BOOLEAN: //$NON-NLS-1$
				boolean bool = s.toLowerCase().startsWith("t"); //$NON-NLS-1$
				changed = bool != control.getBoolean(prop.getPropertyName());
				control.setValue(prop.getPropertyName(), bool);
				break;
			case XMLProperty.TYPE_OBJECT: //$NON-NLS-1$
				XMLControl childControl = control.getChildControl(prop.getPropertyName());
				if ((childControl.getObjectClass() == Character.class) && (s.length() == 1)) {
					Character c = new Character(s.charAt(0));
					changed = !c.equals(control.getObject(prop.getPropertyName()));
					control.setValue(prop.getPropertyName(), c);
				}
				break;
			}
		}
		if (changed) {
			fireTableCellUpdated(row, col);
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
