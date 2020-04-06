/*
 * The value package contains utilities to work with primitives
 * Copyright (c) June 2002 F. Esquembre
 * @author F. Esquembre (http://fem.um.es).
 */

package org.opensourcephysics.ejs.control.value;

import org.opensourcephysics.ejs.control.PropertyEditor;

 /**
  * A <code>ExpressionValue</code> is a <code>Value</code> object that
  * holds an expression is parsed into a double.
  * <p>
  * @see     Value
  */
public class InterpretedValue extends Value {
  private String expression;
  private PropertyEditor myEjsPropertyEditor;

  public InterpretedValue(String _expression, PropertyEditor _editor) {
	  super(TYPE_EXPRESSION);
    myEjsPropertyEditor = _editor;
    expression = new String(_expression.trim());
  }
  
  //public String getExpression() { return expression; }

  public boolean getBoolean() {
    try { return ((Boolean) getObject()).booleanValue(); }
    catch (Exception exc) { return false; }
  }

  public int getInteger() { 
    try { return ((Integer) getObject()).intValue(); }
    catch (Exception exc) {
      try { return (int) Math.round(((Double) getObject()).doubleValue()); }
      catch (Exception exc2) { return 0; }
    }
  }
  
  public double  getDouble()  {
    try { return ((Double) getObject()).doubleValue(); }
    catch (Exception exc) { return 0.0; }
  }

  public String  getString()  { return getObject().toString(); }

  public Object  getObject()  { return myEjsPropertyEditor.evaluateExpression(expression); }

  public void copyValue(Value _source) {
    if (_source instanceof InterpretedValue) expression = new String(((InterpretedValue)_source).expression);
    else expression = new String(_source.getString());
  }

  public Value cloneValue() { return new InterpretedValue(expression,myEjsPropertyEditor); }

}

