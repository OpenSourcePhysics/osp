/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This defines methods for storing data in an xml property element.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public final class XMLPropertyElement extends XMLNode implements XMLProperty {
	// static fields
	@SuppressWarnings("javadoc")
	public static boolean defaultWriteNullFinalArrayElements = true;

	// instance fields

    List<Object> content = new ArrayList<Object>();
	
    private boolean writeNullFinalElement;

	/**
	 * Constructs an empty property element.
	 *
	 * @param mother the parent
	 */
	public XMLPropertyElement(XMLProperty mother) {
		parent = mother;
	}

	/**
	 * Constructs a property element with the specified value.
	 *
	 * @param mother       the parent
	 * @param propertyName the name
	 * @param propertyType the type
	 * @param value        the value
	 */
	public XMLPropertyElement(XMLProperty mother, String propertyName, int propertyType, Object value) {
		this(mother, propertyName, propertyType, value, defaultWriteNullFinalArrayElements);
	}

	/**
	 * Constructs a property element with the specified value.
	 *
	 * @param mother                     the parent
	 * @param propertyName               the name
	 * @param propertyType               the type
	 * @param value                      the value
	 * @param writeNullFinalArrayElement true to write a final null array element
	 *                                   (if needed)
	 */
	public XMLPropertyElement(XMLProperty mother, String propertyName, int propertyType, Object value,
			boolean writeNullFinalArrayElement) {
		this(mother);
		name = propertyName;
		type = propertyType;
		writeNullFinalElement = writeNullFinalArrayElement;
		switch (type) {
		case XMLProperty.TYPE_STRING: //$NON-NLS-1$
			if (XML.requiresCDATA((String) value)) {
				content.add(XML.CDATA_PRE + value + XML.CDATA_POST);
			} else {
				content.add(value.toString());
			}
			break;
		case XMLProperty.TYPE_INT:
		case XMLProperty.TYPE_DOUBLE:
		case XMLProperty.TYPE_BOOLEAN:
			content.add(value.toString());
			break;
		case XMLProperty.TYPE_OBJECT:
			if (value == null) {
				content.add("null"); //$NON-NLS-1$
			} else {
				className = value.getClass().getName();
				XMLControl control = new XMLControlElement(this);
				control.saveObject(value);
				content.add(control);
			}
			break;
		case XMLProperty.TYPE_COLLECTION: //$NON-NLS-1$
			className = value.getClass().getName();
			Iterator<?> it = ((Collection<?>) value).iterator();
			while (it.hasNext()) {
				Object next = it.next();
				int type = XMLProperty.getDataType(next);
				if (type != XMLProperty.TYPE_UNKNOWN) {
					content.add(new XMLPropertyElement(this, "item", type, next, writeNullFinalElement)); //$NON-NLS-1$
				}
			}
			break;
		case XMLProperty.TYPE_ARRAY: //$NON-NLS-1$
			className = value.getClass().getName();
			// determine if base component type is primitive and count array elements
			Class<?> baseType = value.getClass().getComponentType();
			Object array = value;
			int count = Array.getLength(array);
			while ((count > 0) && (baseType.getComponentType() != null)) {
				baseType = baseType.getComponentType();
				array = Array.get(array, 0);
				if (array == null) {
					break;
				}
				count = count * Array.getLength(array);
			}
			boolean primitive = (baseType == Integer.TYPE || baseType == Double.TYPE || baseType == Boolean.TYPE);
			if (primitive && (count > XMLControlElement.compactArraySize)) {
				// write array as string if base type is primitive
				String s = getArrayString(value);
				content.add(new XMLPropertyElement(this, "array", XMLProperty.TYPE_STRING, s, writeNullFinalElement)); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				int length = Array.getLength(value);
				int last = writeNullFinalElement ? length - 1 : length;
				for (int j = 0; j < length; j++) {
					Object next = Array.get(value, j);
					int type = XMLProperty.getDataType(next);
					if (type == XMLProperty.TYPE_UNKNOWN) {
						if (j < last)
							continue;
						type = XMLProperty.TYPE_OBJECT; //$NON-NLS-1$
					}
					content.add(new XMLPropertyElement(this, "[" + j + "]", type, next, writeNullFinalElement)); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			break;
		}
	}

	/**
	 * Gets the property name.
	 *
	 * @return a name
	 */
	@Override
	public String getPropertyName() {
		return name;
	}


	/**
	 * Gets the property class.
	 *
	 * @return the class
	 */
	@Override
	public Class<?> getPropertyClass() {
		switch (type) {
		case XMLProperty.TYPE_INT:
			return Integer.TYPE;
		case XMLProperty.TYPE_DOUBLE:
			return Double.TYPE;
		case XMLProperty.TYPE_BOOLEAN:
			return Boolean.TYPE;
		case XMLProperty.TYPE_STRING:
			return String.class;
		default:
			try {
				return Class.forName(className);
			} catch (Exception ex) {
				return null;
			}
		}
	}


	/**
	 * Gets the level of this property relative to root.
	 *
	 * @return the non-negative integer level
	 */
	@Override
	public int getLevel() {
		return parent.getLevel() + 1;
	}

	/**
	 * Gets the xml content for this property. Content items may be strings,
	 * XMLControls or XMLProperties.
	 *
	 * @return a list of content items
	 */
	@Override
	public List<Object> getPropertyContent() {
		return content;
	}

	/**
	 * Gets the named XMLControl child of this property. May return null.
	 *
	 * @param name the property name
	 * @return the XMLControl
	 */
	@Override
	public XMLControl getChildControl(String name) {
	XMLControl[] children = getChildControls();
		for (int i = 0; i < children.length; i++) {
			if (children[i].getPropertyName().equals(name)) {
				return children[i];
			}
		}
		return null;
	}

	/**
	 * Gets the XMLControl children of this property. The returned array has length
	 * for type "object" = 1, "collection" and "array" = 0+, other types = 0.
	 *
	 * @return an XMLControl array
	 */
	@Override
	public XMLControl[] getChildControls() {
		switch (type) {
		case XMLProperty.TYPE_OBJECT:
			if (!content.isEmpty()) {
				return new XMLControl[] { (XMLControl) content.get(0) };				
			}
			break;
		case XMLProperty.TYPE_ARRAY:
		case XMLProperty.TYPE_COLLECTION:
			ArrayList<XMLControl> list = new ArrayList<XMLControl>();
			Iterator<Object> it = content.iterator();
			while (it.hasNext()) {
				XMLProperty prop = (XMLProperty) it.next();
				if (prop.getPropertyType() == XMLProperty.TYPE_OBJECT 
						&& !prop.getPropertyContent().isEmpty()) { //$NON-NLS-1$
					list.add((XMLControl) prop.getPropertyContent().get(0));
				}
			}
			return list.toArray(new XMLControl[0]);
		}
		return new XMLControl[0];
	}

	/**
	 * Sets the value of this property if property type is primitive or string. This
	 * does nothing for other property types.
	 *
	 * @param stringValue the string value of a primitive or string property
	 */
	@Override
	public void setValue(String stringValue) {
		try {
			switch (type) {
			case XMLProperty.TYPE_INT:
				Integer.parseInt(stringValue);
				break;
			case XMLProperty.TYPE_DOUBLE:
				Double.parseDouble(stringValue);
				break;
			case XMLProperty.TYPE_BOOLEAN:
				stringValue = stringValue.equals("true") ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				break;
			case XMLProperty.TYPE_STRING:
				stringValue = XML.CDATA_PRE + stringValue + XML.CDATA_POST;
				break;
			case XMLProperty.TYPE_OBJECT:
			case XMLProperty.TYPE_ARRAY:
			case XMLProperty.TYPE_COLLECTION:
				return;
			}
		} catch (NumberFormatException ex) {
			return;
		}
		content.clear();
		content.add(stringValue);
	}

	/**
	 * Returns the xml string representation of this property.
	 *
	 * @return the xml string
	 */
	@Override
	public String toString() {
		// write the opening tag with attributes
		StringBuffer xml = new StringBuffer(
				XML.NEW_LINE + indent(getLevel()) + "<property name=\"" + name + "\" type=\"" + XMLProperty.getTypeName(type) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (type == XMLProperty.TYPE_ARRAY 
				|| type == XMLProperty.TYPE_COLLECTION) { 
			xml.append(" class=\"" + className + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// write the content
		List<Object> c = content;
		// special case: null object
		if (type == XMLProperty.TYPE_OBJECT && c.isEmpty()) { //$NON-NLS-1$
			// BH! Q this was getPropertyContents(), but that is NOT a clone!
			c = new ArrayList<>();
			c.add("null"); //$NON-NLS-1$
		}
		// if no content, write closing tag and return
		if (c.isEmpty()) {
			xml.append("/>"); //$NON-NLS-1$
			return xml.toString();
		}
		// else write content
		xml.append(">"); //$NON-NLS-1$
		boolean hasChildren = false;
		Iterator<Object> it = c.iterator();
		while (it.hasNext()) {
			Object next = it.next();
			hasChildren = hasChildren || (next instanceof XMLProperty);
			xml.append(next);
		}
		// write the closing tag
		if (hasChildren) {
			xml.append(XML.NEW_LINE + indent(getLevel()));
		}
		xml.append("</property>"); //$NON-NLS-1$
		return xml.toString();
	}

	/**
	 * Returns a space for indentation.
	 *
	 * @param level the indent level
	 * @return the space
	 */
	protected String indent(int level) {
		String space = ""; //$NON-NLS-1$
		for (int i = 0; i < XML.INDENT * level; i++) {
			space += " "; //$NON-NLS-1$
		}
		return space;
	}

	/**
	 * Returns a string representation of a primitive array.
	 *
	 * @param array the array
	 * @return the array string
	 */
	protected String getArrayString(Object array) {
		StringBuffer sb = new StringBuffer("{"); //$NON-NLS-1$
		int length = Array.getLength(array);
		for (int j = 0; j < length; j++) {
			// add separator except for first element
			if (j > 0) {
				sb.append(','); // s += ",";
			}
			Object element = Array.get(array, j);
			if ((element != null) && element.getClass().isArray()) {
				sb.append(getArrayString(element));
			} else {
				sb.append(element);
			}
		}
		sb.append('}');
		return sb.toString();
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
