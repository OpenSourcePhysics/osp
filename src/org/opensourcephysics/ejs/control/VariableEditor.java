/*
 * The control package contains utilities to build and control
 * simulations using a central control.
 * Copyright (c) Jan 2004 F. Esquembre
 * @author F. Esquembre (http://fem.um.es).
 */

package org.opensourcephysics.ejs.control;

import org.opensourcephysics.ejs.control.value.Value;

/**
 * An interface to get the editor of properties in Ejs
 */
public interface VariableEditor {

  public void updateControlValues(boolean showErrors);
  public void updateTableValues (PropertyEditor _editor, String _variable, String _value, Value _theValue);

} // End of class

