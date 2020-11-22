/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;

import java.util.Collection;
import java.util.List;

/**
 * This defines methods for storing data in an xml property element.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public interface XMLProperty {

	public final int TYPE_UNKNOWN = -1;	
	public final int TYPE_INT = 0;
	public final int TYPE_DOUBLE = 1;
	public final int TYPE_BOOLEAN = 2;
	public final int TYPE_STRING = 3;
	public final int TYPE_ARRAY = 4;
	public final int TYPE_COLLECTION = 5;
	public final int TYPE_OBJECT = 6;

	public final static String[] types = { 
			"int", // 0
			"double", // 1
			"boolean", // 2
			"string", // 3
			"array", // 4
			"collection", // 5
			"object" // 6
	};

	public static String getTypeName(int type) {
		return (type == TYPE_UNKNOWN ? "object" : types[type]);
	}

	public static int getTypeCode(String type) {
		switch (type) {
		case "int":
			return TYPE_INT;
		case "double":
			return TYPE_DOUBLE;
		case "boolean":
			return TYPE_BOOLEAN;
		case "string":
			return TYPE_STRING;
		case "array":
			return TYPE_ARRAY;
		case "collection":
			return TYPE_COLLECTION;
		case "object":
			return TYPE_OBJECT;
		default:
			return TYPE_UNKNOWN;
		}
	}

	/**
	 * Gets the XMLProperty type for a Java object.
	 *
	 * @param obj the object
	 * @return the type
	 */
	static int getDataType(Object obj) {
		if (obj == null) {
			return TYPE_UNKNOWN;
		}
		if (obj instanceof String) {
			return TYPE_STRING; //$NON-NLS-1$
		} else if (obj instanceof Collection<?>) {
			return TYPE_COLLECTION; //$NON-NLS-1$
		} else if (obj.getClass().isArray()) {
			// make sure ultimate component class is acceptable
			Class<?> componentType = obj.getClass().getComponentType();
			while (componentType.isArray()) {
				componentType = componentType.getComponentType();
			}
			String type = componentType.getName();
			if ((type.indexOf(".") == -1) && ("intdoubleboolean".indexOf(type) == -1)) { //$NON-NLS-1$ //$NON-NLS-2$
				return TYPE_UNKNOWN;
			}
			return TYPE_ARRAY; //$NON-NLS-1$
		} else if (obj instanceof Double) {
			return TYPE_DOUBLE; //$NON-NLS-1$
		} else if (obj instanceof Integer) {
			return TYPE_INT; //$NON-NLS-1$
		} else {
			return TYPE_OBJECT; //$NON-NLS-1$
		}
	}

	/**
	 * Gets the property name.
	 *
	 * @return a name
	 */
	public String getPropertyName();

	/**
	 * Gets the property type.
	 *
	 * @return the type
	 */
	public int getPropertyType();

	/**
	 * Gets the property class.
	 *
	 * @return the class
	 */
	public Class<?> getPropertyClass();

	/**
	 * Gets the immediate parent property.
	 *
	 * @return the type
	 */
	public XMLProperty getParentProperty();

	/**
	 * Gets the level of this property relative to root.
	 *
	 * @return the non-negative integer level
	 */
	public int getLevel();

	/**
	 * Clone the property content of this property.
	 *
	 * @return a list of strings and XMLProperties
	 */
	public List<Object> getPropertyContent();

	/**
	 * Gets the named XMLControl child of this property. May return null.
	 *
	 * @param name the property name
	 * @return the XMLControl
	 */
	public XMLControl getChildControl(String name);

	/**
	 * Gets the XMLControl children of this property.
	 *
	 * @return an array of XMLControls
	 */
	public XMLControl[] getChildControls();

	/**
	 * Sets the value of this property if property type is primitive or string. This
	 * does nothing for other property types.
	 *
	 * @param stringValue the string value of a primitive or string property
	 */
	public void setValue(String stringValue);

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
