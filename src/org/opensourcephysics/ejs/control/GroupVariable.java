/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.ejs.control;
import java.util.ArrayList;
import java.util.List;

import org.opensourcephysics.ejs.control.value.DoubleValue;
import org.opensourcephysics.ejs.control.value.Value;

/**
 * A utility class that holds information about a value that can be shared
 * by different ControlElement internal variables and that can also trigger
 * methods of a class
 */
public class GroupVariable {
  private String name;
  private Value value;
  private List<Item> elementList;
  private List<MethodWithOneParameter> methodList;

  // A GroupVariable should be created with a non-null value
  // that matches the type it is going to be used.

  /**
   * Constructor GroupVariable
   * @param _aName
   * @param _aValue
   */
  public GroupVariable(String _aName, Value _aValue) {
    name = _aName;
    elementList = new ArrayList<Item>();
    methodList = new ArrayList<MethodWithOneParameter>();
    // value = _aValue.cloneValue();
    if(_aValue!=null) {
      value = _aValue.cloneValue();
    } else {
      value = new DoubleValue(0.0);
    }
    // else value = null; // This is rather dangerous if one doesn't follow the instructions above
  }

  public String getName() {
    return name;
  }

  @Override
public String toString() {
    return name;
  }

  public void setValue(Value _aValue) {
    // This can be optimized by removing the check
    // Again, this forces the instantiation to hold a non-null Value
    // which must hold the right subclass of Value
    // Unfortunately Ejs' users tend to modify the variable class
    if(value.getClass()!=_aValue.getClass()) {
      value = _aValue.cloneValue();
    } else {
      value.copyValue(_aValue);
    }
  }

  public Value getValue() {
    return value;
  }

  // --------------------------------------------------------
  // Adding and removing control elements
  // --------------------------------------------------------
  public void addElementListener(ControlElement _element, int _index) {
    elementList.add(new Item(_element, _index));
  }

  public void removeElementListener(ControlElement _element, int _index) {
	  // BH 2020.03.27 optimization
	  for (int i = elementList.size(); --i >= 0;) {
		 Item item = elementList.get(i);
	      if((item.element==_element)&&(item.index==_index)) {
	          elementList.remove(i);
	          return;
	        }		 
	  }
//    for(Enumeration<Item> e = elementList.elements(); e.hasMoreElements(); ) {
//      Item item = e.nextElement();
//      if((item.element==_element)&&(item.index==_index)) {
//        elementList.removeElement(item);
//        return;
//      }
//    }
  }

	public void propagateValue(ControlElement _element) {
		for (int i = elementList.size(); --i >= 0;) {
			Item item = elementList.get(i);
			if (item.element != _element) {
				item.element.setActive(false);
				if (item.element.myMethodsForProperties[item.index] != null) { // AMAVP (See note in ControlElement)
					// System.out.println ("I call the method
					// "+item.element.myMethodsForProperties[item.index].toString()+ "first!");
					item.element.setValue(item.index, item.element.myMethodsForProperties[item.index]
							.invoke(ControlElement.METHOD_FOR_VARIABLE, null)); // null = no calling object
				} else if (item.element.myExpressionsForProperties[item.index] != null) { // AMAVP (See note in
																							// ControlElement)
					// System.out.println ("I call the expression
					// "+item.element.myExpressionsForProperties[item.index].expression+ "first!");
					item.element.setValue(item.index, item.element.myExpressionsForProperties[item.index]);
				} else {
					item.element.setValue(item.index, value);
				}
				item.element.setActive(true);
			}
		}
	}

  // --------------------------------------------------------
  // Adding and removing method elements
  // --------------------------------------------------------
  public void addListener(Object _target, String _method) {
    addListener(_target, _method, null);
  }

  public void addListener(Object _target, String _method, Object _anObject) {
    methodList.add(new MethodWithOneParameter(ControlElement.VARIABLE_CHANGED, _target, _method, null, null, _anObject));
  }

	public void removeListener(Object _target, String _method) {
		for (int i = methodList.size(); --i >= 0;) {
			MethodWithOneParameter method = methodList.get(i);
			if (method.equals(ControlElement.VARIABLE_CHANGED, _target, _method)) {
				methodList.remove(method);
				return;
			}
		}
	}

	public void invokeListeners(ControlElement _element) {
		for (int i = 0, n = methodList.size(); i < n; i++) {
			methodList.get(i).invoke(ControlElement.VARIABLE_CHANGED, _element);
		}
	}

  // --------------------------------------------------------
  // Internal classes
  // --------------------------------------------------------
  private class Item {
    public ControlElement element;
    public int index;

    Item(ControlElement _anElement, int _anIndex) {
      element = _anElement;
      index = _anIndex;
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
 * Copyright (c) 2017  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
